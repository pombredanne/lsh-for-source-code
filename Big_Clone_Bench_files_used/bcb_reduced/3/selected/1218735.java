package org.rapla.storage.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.Assert;
import org.rapla.components.util.DateTools;
import org.rapla.components.util.Tools;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.Category;
import org.rapla.entities.DependencyException;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.Named;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.UniqueKeyException;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.internal.PreferencesImpl;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.internal.ModifiableTimestamp;
import org.rapla.entities.storage.CannotExistWithoutTypeException;
import org.rapla.entities.storage.DynamicTypeDependant;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.IdTable;
import org.rapla.storage.LocalCache;
import org.rapla.storage.RaplaSecurityException;
import org.rapla.storage.ReferenceNotFoundException;
import org.rapla.storage.StorageUpdateListener;
import org.rapla.storage.UpdateEvent;
import org.rapla.storage.UpdateResult;

/** An abstract implementation of the StorageOperator-Interface.
    It operates on a LocalCache-Object and doesn't implement
    connect, isConnected, refresh, createIdentifier and disconnect.
    <b>!!WARNING!!</b> This operator is not thread-safe.
    {@link org.rapla.server.internal.ServerServiceImpl} for an example
    of an thread-safe wrapper around this storageoperator.
    @see LocalCache
 */
public abstract class AbstractCachableOperator implements CachableStorageOperator {

    private RaplaLocale raplaLocale;

    private ArrayList<StorageUpdateListener> storageUpdateListeners = new ArrayList<StorageUpdateListener>();

    private Object lock = new Object();

    private MessageDigest md;

    protected LocalCache cache;

    /** set encryption if you want to enable password encryption. Possible values
     are "sha" or "md5".*/
    protected String encryption = "md5";

    protected I18nBundle i18n;

    protected RaplaContext serviceManager;

    Date today;

    Logger logger;

    protected IdTable idTable = new IdTable();

    public AbstractCachableOperator(RaplaContext context) throws RaplaException {
        this.logger = (Logger) context.lookup(Logger.class.getName());
        this.serviceManager = context;
        raplaLocale = (RaplaLocale) context.lookup(RaplaLocale.ROLE);
        i18n = (I18nBundle) context.lookup(I18nBundle.ROLE + "/org.rapla.RaplaResources");
        try {
            if (encryption != null) md = MessageDigest.getInstance(encryption);
        } catch (NoSuchAlgorithmException ex) {
            throw new RaplaException(ex);
        }
        Assert.notNull(raplaLocale.getLocale());
        LocalCache newCache = new LocalCache(raplaLocale.getLocale());
        setCache(newCache);
        updateToday();
    }

    protected Logger getLogger() {
        return logger;
    }

    public String getEncryption() {
        return encryption;
    }

    /** returns the current time that is used for storing. The default
        implementation returns System.currentTimeMillis(). You can override
        it to use another time setting (for example the time on the server).
     */
    protected long getCurrentTime() throws RaplaException {
        return System.currentTimeMillis();
    }

    /** this method is called by the update-timer thread once a day.
        You need to call it manualy if you changed the system-time that
        should be used as base (server/client).
     */
    protected void updateToday() {
        try {
            long currentTime = getCurrentTime();
            today = new Date(DateTools.cutDate(currentTime));
            long delay = DateTools.cutDate(currentTime) + DateTools.MILLISECONDS_PER_DAY - currentTime;
            TimerTask updateTask = new TimerTask() {

                public void run() {
                    updateToday();
                }
            };
            Timer timer = new Timer(true);
            timer.schedule(updateTask, delay);
            SimpleDateFormat sdfHHMMSS = new SimpleDateFormat("HH:mm:ss");
            getLogger().info("Update Today Service: within " + sdfHHMMSS.format(delay));
        } catch (RaplaException ex) {
            getLogger().error("Can't get time. ", ex);
            today = new Date(today.getTime() + DateTools.MILLISECONDS_PER_DAY);
        }
    }

    protected I18nBundle getI18n() {
        return i18n;
    }

    public Entity editObject(Entity o, User user) throws RaplaException {
        checkConnected();
        if (!(o instanceof RefEntity && o instanceof Mementable)) throw new RaplaException("Can only edit objects implementing the Entity and the Mementable interface.");
        Object clone;
        try {
            RefEntity persistant = resolveId(((RefEntity) o).getId());
            clone = ((Mementable) persistant).deepClone();
        } catch (EntityNotFoundException ex) {
            clone = ((Mementable) o).deepClone();
        }
        if (clone instanceof ModifiableTimestamp) {
            ((ModifiableTimestamp) clone).setLastChanged(today);
            if (user != null) {
                ((ModifiableTimestamp) clone).setLastChangedBy(user);
            }
        }
        ((SimpleEntity) clone).setReadOnly(false);
        return (Entity) clone;
    }

    public void storeAndRemove(final Entity[] storeObjects, final Entity[] removeObjects, final User user) throws RaplaException {
        checkConnected();
        UpdateEvent evt = new UpdateEvent();
        if (user != null) {
            evt.setUserId(((RefEntity) user).getId());
        }
        for (int i = 0; i < storeObjects.length; i++) {
            if (!(storeObjects[i] instanceof RaplaObject)) throw new RaplaException("Can only store objects of type " + RaplaObject.class.getName() + ". Class not supported " + storeObjects[i].getClass());
            evt.putStore((RefEntity) storeObjects[i]);
        }
        for (int i = 0; i < removeObjects.length; i++) {
            if (!(removeObjects[i] instanceof RaplaObject)) throw new RaplaException("Can only remove objects of type " + RaplaObject.class.getName() + ". Class not supported " + removeObjects[i].getClass());
            RefEntity entity = (RefEntity) removeObjects[i];
            RaplaType type = entity.getRaplaType();
            if (Appointment.TYPE.equals(type) || Category.TYPE.equals(type) || Attribute.TYPE.equals(type)) {
                throw new RaplaException("You can't remove an object of class " + entity.getClass().getName() + ". Please remove it from the parent object and store the parent.");
            }
            evt.putRemove(entity);
        }
        dispatch(evt);
    }

    public Collection getObjects(final RaplaType raplaType) throws RaplaException {
        checkConnected();
        return cache.getCollection(raplaType);
    }

    public List getVisibleEntities(final User user) throws RaplaException {
        checkConnected();
        ArrayList list = new ArrayList();
        Iterator it = cache.getVisibleEntities();
        while (it.hasNext()) list.add(it.next());
        return list;
    }

    public User getUser(final String username) throws RaplaException {
        checkConnected();
        return cache.getUser(username);
    }

    public Preferences getPreferences(final User user) throws RaplaException {
        checkConnected();
        if (user != null) {
            resolveId(((RefEntity) user).getId());
        }
        Preferences pref = cache.getPreferences(user);
        if (pref == null) {
            PreferencesImpl newPref = new PreferencesImpl();
            newPref.setOwner(user);
            newPref.setId(createIdentifier(Preferences.TYPE));
            pref = newPref;
            cache.put(newPref);
        }
        return pref;
    }

    public SortedSet<Appointment> getAppointments(final User user, final Date start, final Date end) throws RaplaException {
        checkConnected();
        return cache.getAppointments(user, start, end);
    }

    public List<Reservation> getReservations(final User user, final Date start, final Date end) throws RaplaException {
        return cache.getReservations(user, start, end);
    }

    public Category getSuperCategory() {
        return cache.getSuperCategory();
    }

    public void addStorageUpdateListener(StorageUpdateListener listener) {
        storageUpdateListeners.add(listener);
    }

    public void removeStorageUpdateListener(StorageUpdateListener listener) {
        storageUpdateListeners.remove(listener);
    }

    public StorageUpdateListener[] getStorageUpdateListeners() {
        return storageUpdateListeners.toArray(new StorageUpdateListener[] {});
    }

    protected void fireStorageUpdated(final UpdateResult evt) {
        StorageUpdateListener[] listeners = getStorageUpdateListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].objectsUpdated(evt);
        }
    }

    protected void fireUpdateError(final RaplaException ex) {
        if (storageUpdateListeners.size() == 0) return;
        StorageUpdateListener[] listeners = getStorageUpdateListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].updateError(ex);
        }
    }

    protected void fireStorageDisconnected() {
        if (storageUpdateListeners.size() == 0) return;
        StorageUpdateListener[] listeners = getStorageUpdateListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].storageDisconnected();
        }
    }

    public Date today() {
        return today;
    }

    protected void checkConnected() throws RaplaException {
        if (!isConnected()) throw new RaplaException(getI18n().format("error.connection_closed", ""));
    }

    public void setCache(final LocalCache cache) throws RaplaException {
        this.cache = cache;
        idTable.setCache(cache);
    }

    public LocalCache getCache() {
        return cache;
    }

    /** Override this method to implement the persistent mechanism.
        By default it calls
        <li>check()</li>
        <li>update()</li>
        <li>fireStorageUpdate()</li>
        <li>fireTriggerEvents()</li>
        You should not call dispatch directly from the client.
        Use storeObjects and removeObjects instead.
    */
    public void dispatch(final UpdateEvent evt) throws RaplaException {
        UpdateEvent closure = createClosure(evt);
        check(closure);
        UpdateResult result = update(closure, true);
        fireStorageUpdated(result);
    }

    /** performs Integrety constraints check */
    protected void check(final UpdateEvent evt) throws RaplaException {
        Set<RefEntity> storeObjects = new HashSet(evt.getStoreObjects());
        Set<RefEntity> removeObjects = new HashSet(evt.getRemoveObjects());
        checkUnique(storeObjects);
        checkConsistency(storeObjects);
        checkReferences(storeObjects);
        checkNoDependencies(removeObjects, storeObjects);
        checkVersions(storeObjects);
    }

    /** Writes the UpdateEvent in the cache */
    protected UpdateResult update(final UpdateEvent evt, final boolean increaseVersion) throws RaplaException {
        User user = null;
        if (evt.getUserId() != null) {
            user = (User) resolveId(evt.getUserId());
        }
        UpdateResult result = new UpdateResult(user);
        List<RefEntity> resolvableEntities = new ArrayList();
        HashMap<RefEntity, RefEntity> oldEntities = new HashMap();
        for (RefEntity entity : evt.getStoreObjects()) {
            RefEntity persistantEntity = findPersistantVersion(entity);
            if (persistantEntity == entity) {
                continue;
            }
            if (persistantEntity != null) {
                if (getLogger().isDebugEnabled()) getLogger().debug("Storing old: " + entity);
                RefEntity oldEntity = ((Mementable<RefEntity>) persistantEntity).deepClone();
                oldEntities.put(persistantEntity, oldEntity);
            }
        }
        for (RefEntity entity : evt.getStoreObjects()) {
            if (increaseVersion) increaseVersion(entity);
            RefEntity persistantEntity = findPersistantVersion(entity);
            if (persistantEntity == entity) {
                continue;
            }
            if (persistantEntity != null) {
                if (getLogger().isDebugEnabled()) getLogger().debug("Changing: " + entity);
                ((Mementable) persistantEntity).copy(entity);
            } else {
                if (getLogger().isDebugEnabled()) getLogger().debug("Adding entity: " + entity);
                persistantEntity = ((Mementable<RefEntity>) entity).deepClone();
            }
            ((SimpleEntity) persistantEntity).setReadOnly(true);
            cache.put(persistantEntity);
            resolvableEntities.add(persistantEntity);
        }
        for (Iterator it = resolvableEntities.iterator(); it.hasNext(); ) {
            SimpleEntity persistantEntity = (SimpleEntity) it.next();
            persistantEntity.resolveEntities(getCache());
            RefEntity newEntity = (RefEntity) persistantEntity.deepClone();
            RefEntity oldEntity = (RefEntity) oldEntities.get(persistantEntity);
            if (oldEntity != null) {
                result.addOperation(new UpdateResult.Change(persistantEntity, newEntity, oldEntity));
            } else {
                result.addOperation(new UpdateResult.Add(persistantEntity, newEntity));
            }
        }
        for (RefEntity entity : evt.getRemoveObjects()) {
            if (increaseVersion) increaseVersion(entity);
            RefEntity persistantVersion = findPersistantVersion(entity);
            if (persistantVersion != null) {
                cache.remove(persistantVersion);
                ((SimpleEntity) persistantVersion).setReadOnly(true);
                result.addOperation(new UpdateResult.Remove(persistantVersion));
            }
        }
        return result;
    }

    /** Create a closure for all objects that should be updated. The closure
        contains all objects that are sub-entities of the entities and all objects
        and all other objects that are affected by the update: e.g.
        Classifiables when the DynamicType changes.
        The method will recursivly proceed with all discovered objects.
    */
    protected UpdateEvent createClosure(final UpdateEvent evt) throws RaplaException {
        UpdateEvent closure = evt.clone();
        Iterator<RefEntity> it = evt.getStoreObjects().iterator();
        while (it.hasNext()) {
            addStoreOperationsToClosure(closure, it.next());
        }
        it = evt.getRemoveObjects().iterator();
        while (it.hasNext()) {
            addRemoveOperationsToClosure(closure, it.next());
        }
        return closure;
    }

    private void increaseVersion(RefEntity e) throws RaplaException {
        e.setVersion(e.getVersion() + 1);
        if (getLogger().isDebugEnabled()) getLogger().debug("Increasing Version for " + e + " to " + e.getVersion());
    }

    private void addStoreOperationsToClosure(UpdateEvent evt, RefEntity entity) throws RaplaException {
        if (getLogger().isDebugEnabled() && !evt.getStoreObjects().contains(entity)) getLogger().debug("Adding " + entity + " to store closure");
        evt.putStore(entity);
        if (DynamicType.TYPE.equals(entity.getRaplaType())) {
            DynamicType dynamicType = (DynamicType) entity;
            addChangedDynamicTypeDependant(evt, dynamicType, false);
        }
        Iterator<RefEntity> it = entity.getSubEntities();
        while (it.hasNext()) {
            RefEntity subEntity = it.next();
            addStoreOperationsToClosure(evt, subEntity);
        }
        it = getRemovedEntities(entity).iterator();
        while (it.hasNext()) {
            addRemoveOperationsToClosure(evt, it.next());
        }
    }

    protected void addChangedDynamicTypeDependant(UpdateEvent evt, DynamicType type, boolean toRemove) throws RaplaException {
        Iterator it = getReferencingEntities((RefEntity) type).iterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (!(next instanceof DynamicTypeDependant)) {
                continue;
            }
            DynamicTypeDependant dependant = (DynamicTypeDependant) next;
            if (!dependant.needsChange(type) && !toRemove) continue;
            if (getLogger().isDebugEnabled()) getLogger().debug("Classifiable " + dependant + " needs change!");
            if (evt.getStoreObjects().contains(dependant)) {
                dependant = (DynamicTypeDependant) evt.findEntity((RefEntity) dependant);
            } else {
                User user = null;
                if (evt.getUserId() != null) {
                    user = (User) resolveId(evt.getUserId());
                }
                dependant = (DynamicTypeDependant) editObject((Entity) dependant, user);
                addStoreOperationsToClosure(evt, (RefEntity) dependant);
            }
            if (toRemove) {
                try {
                    dependant.commitRemove(type);
                } catch (CannotExistWithoutTypeException ex) {
                }
            } else {
                dependant.commitChange(type);
            }
        }
    }

    private void addRemoveOperationsToClosure(UpdateEvent evt, RefEntity entity) throws RaplaException {
        if (getLogger().isDebugEnabled() && !evt.getRemoveObjects().contains(entity)) getLogger().debug("Adding " + entity + " to remove closure");
        evt.putRemove(entity);
        if (DynamicType.TYPE.equals(entity.getRaplaType())) {
            DynamicType dynamicType = (DynamicType) entity;
            addChangedDynamicTypeDependant(evt, dynamicType, true);
        }
        Iterator<RefEntity> it = entity.getSubEntities();
        while (it.hasNext()) {
            addRemoveOperationsToClosure(evt, it.next());
        }
        it = getRemovedEntities(entity).iterator();
        while (it.hasNext()) {
            addRemoveOperationsToClosure(evt, it.next());
        }
        if (User.TYPE.equals(entity.getRaplaType())) {
            Preferences preferences = cache.getPreferences((User) entity);
            if (preferences != null) addRemoveOperationsToClosure(evt, (RefEntity) preferences);
        }
    }

    private Collection<RefEntity> getRemovedEntities(RefEntity entity) {
        RefEntity original = findPersistantVersion(entity);
        List<RefEntity> result = null;
        if (original != null) {
            Iterator<RefEntity> it = original.getSubEntities();
            while (it.hasNext()) {
                RefEntity subEntity = it.next();
                if (!entity.isParentEntity(subEntity)) {
                    if (result == null) {
                        result = new ArrayList<RefEntity>();
                    }
                    result.add(subEntity);
                }
            }
        }
        if (result != null) {
            return result;
        } else {
            return Collections.emptySet();
        }
    }

    private void throwNotUnique(RefEntity entity, String name) throws UniqueKeyException {
        throw new UniqueKeyException(i18n.format("error.not_unique", name));
    }

    public Entity getPersistant(Entity entity) throws EntityNotFoundException {
        RefEntity persistant = findPersistantVersion((RefEntity) entity);
        if (persistant == null) {
            throw new EntityNotFoundException("There is no persistant version of " + entity);
        }
        return persistant;
    }

    protected void resolveEntities(Iterator entities, EntityResolver resolver) throws RaplaException {
        List readOnlyList = new ArrayList();
        for (Iterator it = entities; it.hasNext(); ) {
            Object obj = it.next();
            if (obj instanceof RefEntity) {
                RefEntity entity = (RefEntity) obj;
                entity.resolveEntities(resolver);
                readOnlyList.add(obj);
            }
        }
        for (Iterator it = readOnlyList.iterator(); it.hasNext(); ) {
            ((SimpleEntity) it.next()).setReadOnly(true);
        }
    }

    /** check if we find an object with the same name.
     * If a different object (different id) with the same unique
     * attributes is found a UniqueKeyException will be thrown.
     */
    protected final void checkUnique(Collection<RefEntity> entities) throws RaplaException {
        for (RefEntity entity : entities) {
            String name = "";
            RefEntity entity2 = null;
            if (DynamicType.TYPE.equals(entity.getRaplaType())) {
                DynamicType type = (DynamicType) entity;
                name = type.getElementKey();
                entity2 = (RefEntity) cache.getDynamicType(name);
                if (entity2 != null && !entity2.equals(entity)) throwNotUnique(entity, name);
            }
            if (Category.TYPE.equals(entity.getRaplaType())) {
                Category category = (Category) entity;
                Category[] categories = category.getCategories();
                for (int i = 0; i < categories.length; i++) {
                    String key = categories[i].getKey();
                    for (int j = i + 1; j < categories.length; j++) {
                        String key2 = categories[j].getKey();
                        if (key == key2 || (key != null && key.equals(key2))) {
                            throwNotUnique((RefEntity) categories[i], key);
                        }
                    }
                }
            }
            if (User.TYPE.equals(entity.getRaplaType())) {
                name = ((User) entity).getUsername();
                entity2 = (RefEntity) cache.getUser(name);
                if (entity2 != null && !entity2.equals(entity)) throwNotUnique(entity, name);
            }
        }
    }

    /** Check if the objects are consistent, so that they can be safely stored.*/
    protected void checkConsistency(Collection<RefEntity> entities) throws RaplaException {
        for (RefEntity entity : entities) {
            if (Category.TYPE.equals(entity.getRaplaType())) {
                if (entity.equals(cache.getSuperCategory())) {
                    Category userGroups = ((Category) entity).getCategory(Permission.GROUP_CATEGORY_KEY);
                    if (userGroups == null) {
                        throw new RaplaException("The category with the key '" + Permission.GROUP_CATEGORY_KEY + "' is missing.");
                    }
                } else {
                    Category category = (Category) entity;
                    if (category.getParent() == null) {
                        throw new RaplaException("The category " + category + " needs a parent.");
                    }
                }
            }
        }
    }

    /** Check if the references of each entity refers to an object in cache or in the passed collection.*/
    protected final void checkReferences(Collection<RefEntity> entities) throws RaplaException {
        Iterator it = entities.iterator();
        while (it.hasNext()) {
            SimpleEntity entity = (SimpleEntity) it.next();
            Iterator it2 = entity.getReferences();
            while (it2.hasNext()) {
                RefEntity reference = (RefEntity) it2.next();
                if (reference instanceof Preferences) {
                    throw new RaplaException("The current version of Rapla doesnt allow references to preferences.");
                }
                if (reference instanceof Reservation) {
                    if (!(entity instanceof Appointment)) {
                        throw new RaplaException("The current version of Rapla doesnt allow references to events");
                    }
                }
                if (reference instanceof Appointment) {
                    if (!(entity instanceof Reservation)) {
                        throw new RaplaException("The current version of Rapla doesnt allow references to appointment");
                    }
                }
                if (findPersistantVersion(reference) != null) continue;
                if (entities.contains(reference)) continue;
                throw new ReferenceNotFoundException(i18n.format("error.reference_not_stored", getName(reference)));
            }
        }
    }

    private String getName(Object object) {
        if (object == null) return (String) null;
        if (object instanceof Named) return (((Named) object).getName(i18n.getLocale()));
        return object.toString();
    }

    /** returns all entities that depend one the passed entities. In most cases
     one object depends on an other object if it has a reference to it.
     * @param entities */
    protected final Set getDependencies(RefEntity entity) throws RaplaException {
        HashSet dependencyList = new HashSet();
        RaplaType type = entity.getRaplaType();
        final Collection referencingEntities;
        if (Category.TYPE.equals(type) || DynamicType.TYPE.equals(type) || Allocatable.TYPE.equals(type) || User.TYPE.equals(type)) {
            referencingEntities = getReferencingEntities(entity);
        } else {
            referencingEntities = cache.getReferers(Preferences.TYPE, entity);
        }
        dependencyList.addAll(referencingEntities);
        return dependencyList;
    }

    protected List getReferencingReservations(RefEntity entity) throws RaplaException {
        ArrayList result = new ArrayList();
        Iterator it = this.getReservations(null, null, null).iterator();
        while (it.hasNext()) {
            RefEntity referer = (RefEntity) it.next();
            if (referer != null && referer.isRefering(entity)) {
                result.add(referer);
            }
        }
        return result;
    }

    protected List<Entity> getReferencingEntities(RefEntity entity) throws RaplaException {
        ArrayList<Entity> list = new ArrayList();
        list.addAll(getReferencingReservations(entity));
        list.addAll(cache.getReferers(Allocatable.TYPE, entity));
        list.addAll(cache.getReferers(Preferences.TYPE, entity));
        list.addAll(cache.getReferers(User.TYPE, entity));
        list.addAll(cache.getReferers(DynamicType.TYPE, entity));
        return list;
    }

    private int countDynamicTypes(Collection entities, String classificationType) {
        Iterator it = entities.iterator();
        int count = 0;
        while (it.hasNext()) {
            RaplaObject entity = (RaplaObject) it.next();
            if (!DynamicType.TYPE.equals(entity.getRaplaType())) continue;
            DynamicType type = (DynamicType) entity;
            if (type.getAnnotation(DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE).equals(classificationType)) {
                count++;
            }
        }
        return count;
    }

    private void checkDynamicType(Collection entities, String classificationType) throws RaplaException {
        Collection allTypes = cache.getCollection(DynamicType.TYPE);
        int count = countDynamicTypes(entities, classificationType);
        if (count >= 0 && count >= countDynamicTypes(allTypes, classificationType)) {
            throw new RaplaException(i18n.getString("error.one_type_requiered"));
        }
    }

    protected void checkNoDependencies(Collection<RefEntity> entities, Set<RefEntity> storeObjects) throws RaplaException {
        HashSet dep = new HashSet();
        for (RefEntity entity : entities) {
            for (RefEntity obj : storeObjects) {
                if (obj.isRefering(entity)) {
                    dep.add(obj);
                }
            }
            Set dependencies = getDependencies(entity);
            for (Object dependency : dependencies) {
                if (!storeObjects.contains(dependency)) {
                    dep.add(dependency);
                }
            }
        }
        if (dep.size() > 0) {
            Collection names = new ArrayList();
            Iterator entityIt = dep.iterator();
            while (entityIt.hasNext()) {
                RaplaObject obj = (RaplaObject) entityIt.next();
                StringBuffer buf = new StringBuffer();
                if (obj instanceof Reservation) {
                    buf.append(getString("reservation"));
                } else if (obj instanceof Preferences) {
                    buf.append(getString("preferences"));
                } else if (obj instanceof Category) {
                    buf.append(getString("categorie"));
                } else if (obj instanceof Allocatable) {
                    buf.append(getString("resources_persons"));
                } else if (obj instanceof User) {
                    buf.append(getString("user"));
                }
                if (obj instanceof Named) {
                    Locale locale = i18n.getLocale();
                    final String string = ((Named) obj).getName(locale);
                    buf.append(": " + string);
                } else {
                    buf.append(obj.toString());
                }
                if (obj instanceof RefEntity) {
                    final Object idFull = ((RefEntity) obj).getId();
                    if (idFull != null) {
                        String idShort = idFull.toString();
                        int dot = idShort.lastIndexOf('.');
                        buf.append(" (" + idShort.substring(dot + 1) + ")");
                    }
                }
                names.add(buf.toString());
            }
            throw new DependencyException(names);
        }
        checkDynamicType(entities, DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
        checkDynamicType(entities, DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION);
        checkDynamicType(entities, DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION);
    }

    private String getString(String key) {
        return getI18n().getString(key);
    }

    /** Entities will be resolved against resolveableEntities.
        If not found the ParentResolver will be used.
     */
    public EntityResolver createEntityResolver(Collection resolveableEntities, LocalCache parent) {
        EntityStore resolver = new EntityStore(parent, cache.getSuperCategory());
        resolver.addAll(resolveableEntities);
        return resolver;
    }

    public RefEntity resolveId(Object id) throws EntityNotFoundException {
        return cache.resolve(id);
    }

    /** compares the version of the cached entities with the versions of the new entities.
     * Throws an Exception if the newVersion != cachedVersion
     */
    protected void checkVersions(Collection entities) throws RaplaException {
        Iterator it = entities.iterator();
        while (it.hasNext()) {
            RefEntity entity = (RefEntity) it.next();
            RefEntity persistantVersion = findPersistantVersion(entity);
            if (persistantVersion != null && persistantVersion != entity && entity.getVersion() < persistantVersion.getVersion()) {
                getLogger().warn("There is a newer  version for: " + entity.getId() + " stored version :" + persistantVersion.getVersion() + " version to store :" + entity.getVersion());
                throw new RaplaException(getI18n().format("error.new_version", entity.toString()));
            }
        }
    }

    private RefEntity findPersistantVersion(RefEntity entity) {
        return (RefEntity) cache.get(entity.getId());
    }

    public void authenticate(String username, String password) throws RaplaException {
        if (getLogger().isInfoEnabled()) getLogger().info("Check password for User " + username);
        User user = cache.getUser(username);
        if (user != null && checkPassword(((RefEntity) user).getId(), password)) {
            return;
        }
        getLogger().error("Login " + username);
        throw new RaplaSecurityException(i18n.getString("error.login"));
    }

    public boolean canChangePassword() {
        return true;
    }

    public void changePassword(User user, char[] oldPassword, char[] newPassword) throws RaplaException {
        if (getLogger().isInfoEnabled()) getLogger().info("Change password for User " + user.getUsername());
        Object userId = ((RefEntity) user).getId();
        String password = new String(newPassword);
        if (encryption != null) password = encrypt(password);
        cache.putPassword(userId, password);
        storeAndRemove(new Entity[] { editObject(user, null) }, Entity.ENTITY_ARRAY, user);
    }

    public Object getLock() {
        return lock;
    }

    protected String encrypt(String password) {
        synchronized (md) {
            md.reset();
            md.update(password.getBytes());
            return encryption + ":" + Tools.convert(md.digest());
        }
    }

    private boolean checkPassword(Object userId, String password) {
        if (userId == null) return false;
        String correct_pw = (String) cache.getPassword(userId);
        if (correct_pw == null) {
            return false;
        }
        if (encryption != null && correct_pw.indexOf(encryption + ":") >= 0) {
            password = encrypt(password);
        }
        if (correct_pw.equals(password)) {
            return true;
        }
        return false;
    }
}
