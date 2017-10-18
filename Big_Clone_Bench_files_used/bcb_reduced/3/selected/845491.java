package com.sun.jini.outrigger;

import com.sun.jini.landlord.LeasedResource;
import com.sun.jini.logging.Levels;
import com.sun.jini.proxy.MarshalledWrapper;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.rmi.MarshalException;
import java.rmi.UnmarshalException;
import java.rmi.server.RMIClassLoader;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Comparator;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.io.MarshalledInstance;
import net.jini.loader.ClassLoading;

/**
 * An <code>EntryRep</code> object contains a packaged <code>Entry</code> object
 * for communication between the client and a <code>JavaSpace</code>.
 * 
 * @author Sun Microsystems, Inc.
 * 
 * @see JavaSpace
 * @see Entry
 */
class EntryRep implements StorableResource, LeasedResource, Serializable {

    static final long serialVersionUID = 3L;

    /**
	 * The fields of the entry in marshalled form. Use <code>null</code> for
	 * <code>null</code> fields.
	 */
    private MarshalledInstance[] values;

    private String[] superclasses;

    private long[] hashes;

    private long hash;

    private String className;

    private String codebase;

    private Uuid id;

    private transient long expires;

    /**
	 * <code>true</code> if the last time this object was unmarshalled integrity
	 * was being enforced, <code>false</code> otherwise.
	 */
    private transient boolean integrity;

    /** Comparator for sorting fields */
    private static final FieldComparator comparator = new FieldComparator();

    /**
	 * This object represents the passing of a <code>null</code> parameter as a
	 * template, which is designed to match any entry. When a <code>null</code>
	 * is passed, it is replaced with this rep, which is then handled specially
	 * in a few relevant places.
	 */
    private static final EntryRep matchAnyRep;

    static {
        try {
            matchAnyRep = new EntryRep(new Entry() {

                static final long serialVersionUID = -4244768995726274609L;
            }, false);
        } catch (MarshalException e) {
            throw new AssertionError(e);
        }
    }

    /**
	 * The realClass object is transient because we neither need nor want it
	 * reconstituted on the other side. All we want is to be able to recreate it
	 * on the receiving client side. If it were not transient, not only would an
	 * unnecessary object creation occur, but it might force the download of the
	 * actual class to the server.
	 */
    private transient Class realClass;

    /**
	 * Logger for logging information about operations carried out in the
	 * client. Note, we hard code "com.sun.jini.outrigger" so we don't drag in
	 * OutriggerServerImpl to outrigger-dl.jar.
	 */
    private static final Logger logger = Logger.getLogger("com.sun.jini.outrigger.proxy");

    /**
	 * Set this entry's generic data to be shared with the <code>other</code>
	 * object. Those fields that are object references that will be the same for
	 * all objects of the same type are shared this way.
	 * <p>
	 * Note that <code>codebase</code> is <em>not</em> shared. If it were, then
	 * the failure of one codebase could make all entries inaccessible. Each
	 * entry is usable insofar as the codebase under which it was written is
	 * usable.
	 */
    void shareWith(EntryRep other) {
        className = other.className;
        superclasses = other.superclasses;
        hashes = other.hashes;
        hash = other.hash;
    }

    /**
	 * Get the entry fields associated with the passed class and put them in a
	 * canonical order. The fields are sorted so that fields belonging to a
	 * superclasses are before fields belonging to subclasses and within a class
	 * fields are ordered lexicographically by their name.
	 */
    private static Field[] getFields(Class cl) {
        final Field[] fields = cl.getFields();
        Arrays.sort(fields, comparator);
        return fields;
    }

    /**
	 * Cached hash values for all classes we encounter. Weak hash used in case
	 * the class is GC'ed from the client's VM.
	 */
    private static WeakHashMap classHashes;

    /**
	 * Lookup the hash value for the given class. If it is not found in the
	 * cache, generate the hash for the class and save it.
	 */
    private static synchronized Long findHash(Class clazz, boolean marshaling) throws MarshalException, UnusableEntryException {
        if (classHashes == null) classHashes = new WeakHashMap();
        Long hash = (Long) classHashes.get(clazz);
        if (hash == null) {
            try {
                Field[] fields = getFields(clazz);
                MessageDigest md = MessageDigest.getInstance("SHA");
                DataOutputStream out = new DataOutputStream(new DigestOutputStream(new ByteArrayOutputStream(127), md));
                Class c = clazz.getSuperclass();
                if (c != Object.class) out.writeLong(findHash(c, marshaling).longValue());
                for (int i = 0; i < fields.length; i++) {
                    if (!usableField(fields[i])) continue;
                    out.writeUTF(fields[i].getName());
                    out.writeUTF(fields[i].getType().getName());
                }
                out.flush();
                byte[] digest = md.digest();
                long h = 0;
                for (int i = Math.min(8, digest.length); --i >= 0; ) {
                    h += ((long) (digest[i] & 0xFF)) << (i * 8);
                }
                hash = new Long(h);
            } catch (Exception e) {
                if (marshaling) throw throwNewMarshalException("Exception calculating entry class hash for " + clazz, e); else throw throwNewUnusableEntryException("Exception calculating entry class hash for " + clazz, e);
            }
            classHashes.put(clazz, hash);
        }
        return hash;
    }

    /**
	 * Create a serialized form of the entry. If <code>validate</code> is
	 * <code>true</code>, basic sanity checks are done on the class to ensure
	 * that it meets the requirements to be an <code>Entry</code>.
	 * <code>validate</code> is <code>false</code> only when creating the
	 * stand-in object for "match any", which is never actually marshalled on
	 * the wire and so which doesn't need to be "proper".
	 */
    private EntryRep(Entry entry, boolean validate) throws MarshalException {
        realClass = entry.getClass();
        if (validate) ensureValidClass(realClass);
        className = realClass.getName();
        codebase = RMIClassLoader.getClassAnnotation(realClass);
        final Field[] fields = getFields(realClass);
        int numFields = fields.length;
        MarshalledInstance[] vals = new MarshalledInstance[numFields];
        int nvals = 0;
        for (int fnum = 0; fnum < fields.length; fnum++) {
            final Field field = fields[fnum];
            if (!usableField(field)) continue;
            final Object fieldValue;
            try {
                fieldValue = field.get(entry);
            } catch (IllegalAccessException e) {
                final IllegalArgumentException iae = new IllegalArgumentException("Couldn't access field " + field);
                iae.initCause(e);
                throw throwRuntime(iae);
            }
            if (fieldValue == null) {
                vals[nvals] = null;
            } else {
                try {
                    vals[nvals] = new MarshalledInstance(fieldValue);
                } catch (IOException e) {
                    throw throwNewMarshalException("Can't marshal field " + field + " with value " + fieldValue, e);
                }
            }
            nvals++;
        }
        this.values = new MarshalledInstance[nvals];
        System.arraycopy(vals, 0, this.values, 0, nvals);
        try {
            hash = findHash(realClass, true).longValue();
        } catch (UnusableEntryException e) {
            throw new AssertionError(e);
        }
        ArrayList sclasses = new ArrayList();
        ArrayList shashes = new ArrayList();
        for (Class c = realClass.getSuperclass(); c != Object.class; c = c.getSuperclass()) {
            try {
                sclasses.add(c.getName());
                shashes.add(findHash(c, true));
            } catch (ClassCastException cce) {
                break;
            } catch (UnusableEntryException e) {
                throw new AssertionError(e);
            }
        }
        superclasses = (String[]) sclasses.toArray(new String[sclasses.size()]);
        hashes = new long[shashes.size()];
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = ((Long) shashes.get(i)).longValue();
        }
    }

    /**
	 * Create a serialized form of the entry with our object's relevant fields
	 * set.
	 */
    public EntryRep(Entry entry) throws MarshalException {
        this(entry, true);
    }

    /** Used in recovery */
    EntryRep() {
    }

    /** Used to look up no-arg constructors. */
    private static Class[] noArg = new Class[0];

    /**
	 * Ensure that the entry class is valid, that is, that it has appropriate
	 * access. If not, throw <code>IllegalArgumentException</code>.
	 */
    private static void ensureValidClass(Class c) {
        boolean ctorOK = false;
        try {
            if (!Modifier.isPublic(c.getModifiers())) {
                throw throwRuntime(new IllegalArgumentException("entry class " + c.getName() + " not public"));
            }
            Constructor ctor = c.getConstructor(noArg);
            ctorOK = Modifier.isPublic(ctor.getModifiers());
        } catch (NoSuchMethodException e) {
            ctorOK = false;
        } catch (SecurityException e) {
            ctorOK = false;
        }
        if (!ctorOK) {
            throw throwRuntime(new IllegalArgumentException("entry class " + c.getName() + " needs public no-arg constructor"));
        }
    }

    /**
	 * The <code>EntryRep</code> that marks a ``match any'' request. This is
	 * used to represent a <code>null</code> template.
	 */
    static EntryRep matchAnyEntryRep() {
        return matchAnyRep;
    }

    /**
	 * Return <code>true</code> if the given rep is that ``match any''
	 * <code>EntryRep</code>.
	 */
    private static boolean isMatchAny(EntryRep rep) {
        return matchAnyRep.equals(rep);
    }

    /**
	 * Return the class name that is used by the ``match any'' EntryRep
	 */
    static String matchAnyClassName() {
        return matchAnyRep.classFor();
    }

    /**
	 * Return an <code>Entry</code> object built out of this
	 * <code>EntryRep</code> This is used by the client-side proxy to convert
	 * the <code>EntryRep</code> it gets from the space server into the actual
	 * <code>Entry</code> object it represents.
	 * 
	 * @throws UnusableEntryException
	 *             One or more fields in the entry cannot be deserialized, or
	 *             the class for the entry type itself cannot be deserialized.
	 */
    Entry entry() throws UnusableEntryException {
        ObjectInputStream objIn = null;
        try {
            ArrayList badFields = null;
            ArrayList except = null;
            realClass = ClassLoading.loadClass(codebase, className, null, integrity, null);
            if (findHash(realClass, false).longValue() != hash) throw throwNewUnusableEntryException(new IncompatibleClassChangeError(realClass + " changed"));
            Entry entryObj = (Entry) realClass.newInstance();
            Field[] fields = getFields(realClass);
            int nvals = 0;
            for (int i = 0; i < fields.length; i++) {
                Throwable nested = null;
                try {
                    if (!usableField(fields[i])) continue;
                    final MarshalledInstance val = values[nvals++];
                    Object value = (val == null ? null : val.get(integrity));
                    fields[i].set(entryObj, value);
                } catch (Throwable e) {
                    nested = e;
                }
                if (nested != null) {
                    if (badFields == null) {
                        badFields = new ArrayList(fields.length);
                        except = new ArrayList(fields.length);
                    }
                    badFields.add(fields[i].getName());
                    except.add(nested);
                }
            }
            if (nvals < values.length) {
                throw throwNewUnusableEntryException(entryObj, null, new Throwable[] { new IncompatibleClassChangeError("A usable field has been removed from " + entryObj.getClass().getName() + " since this EntryRep was created") });
            }
            if (badFields != null) {
                String[] bf = (String[]) badFields.toArray(new String[badFields.size()]);
                Throwable[] ex = (Throwable[]) except.toArray(new Throwable[bf.length]);
                throw throwNewUnusableEntryException(entryObj, bf, ex);
            }
            return entryObj;
        } catch (InstantiationException e) {
            throw throwNewUnusableEntryException(e);
        } catch (ClassNotFoundException e) {
            throw throwNewUnusableEntryException("Encountered a " + "ClassNotFoundException while unmarshalling " + className, e);
        } catch (IllegalAccessException e) {
            throw throwNewUnusableEntryException(e);
        } catch (RuntimeException e) {
            throw throwNewUnusableEntryException("Encountered a " + "RuntimeException while unmarshalling " + className, e);
        } catch (MalformedURLException e) {
            throw throwNewUnusableEntryException("Malformed URL " + "associated with entry of type " + className, e);
        } catch (MarshalException e) {
            throw new AssertionError(e);
        }
    }

    public int hashCode() {
        return className.hashCode();
    }

    /**
	 * To be equal, the other object must by an <code>EntryRep</code> for an
	 * object of the same class with the same values for each field. This is
	 * <em>not</em> a template match -- see <code>matches</code>.
	 * 
	 * @see matches
	 */
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof EntryRep)) return false;
        EntryRep other = (EntryRep) o;
        if (hash != other.hash) return false;
        if (values.length != other.values.length) return false;
        for (int i = 0; i < values.length; i++) {
            if ((values[i] == null) && (other.values[i] != null)) return false;
            if ((values[i] != null) && (other.values[i] == null)) return false;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].equals(other.values[i])) return false;
        }
        return true;
    }

    /**
	 * Return <code>true</code> if the field is to be used for the entry. That
	 * is, return <code>true</code> if the field isn't <code>transient</code>,
	 * <code>static</code>, or <code>final</code>.
	 * 
	 * @throws IllegalArgumentException
	 *             The field is not <code>transient</code>, <code>static</code>,
	 *             or <code>final</code>, but is primitive and hence not a
	 *             proper field for an <code>Entry</code>.
	 */
    private static boolean usableField(Field field) {
        final int ignoreMods = (Modifier.TRANSIENT | Modifier.STATIC | Modifier.FINAL);
        if ((field.getModifiers() & ignoreMods) != 0) return false;
        if (field.getType().isPrimitive()) {
            throw throwRuntime(new IllegalArgumentException("primitive field, " + field + ", not allowed in an Entry"));
        }
        return true;
    }

    /**
	 * Return the ID.
	 */
    Uuid id() {
        return id;
    }

    /**
	 * Pick a random <code>Uuid</code> and set our id field to it.
	 * 
	 * @throws IllegalStateException
	 *             if this method has already been called.
	 */
    void pickID() {
        if (id != null) throw new IllegalStateException("pickID called more than once");
        id = UuidFactory.generate();
    }

    /**
	 * Return the <code>MarshalledObject</code> for the given field.
	 */
    public MarshalledInstance value(int fieldNum) {
        return values[fieldNum];
    }

    /**
	 * Return the number of fields in this kind of entry.
	 */
    public int numFields() {
        if (values != null) {
            return values.length;
        } else {
            return 0;
        }
    }

    /**
	 * Return the class name for this entry.
	 */
    public String classFor() {
        return className;
    }

    /**
	 * Return the array names of superclasses of this entry type.
	 */
    public String[] superclasses() {
        return superclasses;
    }

    /**
	 * Return the hash of this entry type.
	 */
    long getHash() {
        return hash;
    }

    /**
	 * Return the array of superclass hashes of this entry type.
	 */
    long[] getHashes() {
        return hashes;
    }

    /**
	 * See if the other object matches the template object this represents.
	 * (Note that even though "this" is a template, it may have no wildcards --
	 * a template can have all values.)
	 */
    boolean matches(EntryRep other) {
        if (EntryRep.isMatchAny(this)) return true;
        for (int f = 0; f < values.length; f++) {
            if (values[f] == null) {
                continue;
            }
            if (!values[f].equals(other.values[f])) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return ("EntryRep[" + className + "]");
    }

    /**
	 * Return <code>true</code> if this entry represents an object that is at
	 * least the type of the <code>otherClass</code>.
	 */
    boolean isAtLeastA(String otherClass) {
        if (otherClass.equals(matchAnyClassName())) return true;
        if (className.equals(otherClass)) return true;
        for (int i = 0; i < superclasses.length; i++) if (superclasses[i].equals(otherClass)) return true;
        return false;
    }

    /** Comparator for sorting fields. Cribbed from Reggie */
    private static class FieldComparator implements Comparator {

        public FieldComparator() {
        }

        /** Super before subclass, alphabetical within a given class */
        public int compare(Object o1, Object o2) {
            Field f1 = (Field) o1;
            Field f2 = (Field) o2;
            if (f1 == f2) return 0;
            if (f1.getDeclaringClass() == f2.getDeclaringClass()) return f1.getName().compareTo(f2.getName());
            if (f1.getDeclaringClass().isAssignableFrom(f2.getDeclaringClass())) return -1;
            return 1;
        }
    }

    /**
	 * Use <code>readObject</code> method to capture whether or not integrity
	 * was being enforced when this object was unmarshalled, and to perform
	 * basic integrity checks.
	 */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (className == null) throw new InvalidObjectException("null className");
        if (values == null) throw new InvalidObjectException("null values");
        if (superclasses == null) throw new InvalidObjectException("null superclasses");
        if (hashes == null) throw new InvalidObjectException("null hashes");
        if (hashes.length != superclasses.length) throw new InvalidObjectException("hashes.length (" + hashes.length + ") does not equal  superclasses.length (" + superclasses.length + ")");
        integrity = MarshalledWrapper.integrityEnforced(in);
    }

    /**
	 * We should always have data in the stream, if this method gets called
	 * there is something wrong.
	 */
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("SpaceProxy should always have data");
    }

    public void setExpiration(long newExpiration) {
        expires = newExpiration;
    }

    public long getExpiration() {
        return expires;
    }

    public Uuid getCookie() {
        return id;
    }

    public void store(ObjectOutputStream out) throws IOException {
        final long bits0;
        final long bits1;
        if (id == null) {
            bits0 = 0;
            bits1 = 0;
        } else {
            bits0 = id.getMostSignificantBits();
            bits1 = id.getLeastSignificantBits();
        }
        out.writeLong(bits0);
        out.writeLong(bits1);
        out.writeLong(expires);
        out.writeObject(codebase);
        out.writeObject(className);
        out.writeObject(superclasses);
        out.writeObject(values);
        out.writeLong(hash);
        out.writeObject(hashes);
    }

    public void restore(ObjectInputStream in) throws IOException, ClassNotFoundException {
        final long bits0 = in.readLong();
        final long bits1 = in.readLong();
        if (bits0 == 0 && bits1 == 0) {
            id = null;
        } else {
            id = UuidFactory.create(bits0, bits1);
        }
        expires = in.readLong();
        codebase = (String) in.readObject();
        className = (String) in.readObject();
        superclasses = (String[]) in.readObject();
        values = (MarshalledInstance[]) in.readObject();
        hash = in.readLong();
        hashes = (long[]) in.readObject();
    }

    /** Log and throw a runtime exception */
    private static RuntimeException throwRuntime(RuntimeException e) {
        if (logger.isLoggable(Levels.FAILED)) {
            logger.log(Levels.FAILED, e.getMessage(), e);
        }
        throw e;
    }

    /** Construct, log, and throw a new MarshalException */
    private static MarshalException throwNewMarshalException(String msg, Exception nested) throws MarshalException {
        final MarshalException me = new MarshalException(msg, nested);
        if (logger.isLoggable(Levels.FAILED)) {
            logger.log(Levels.FAILED, msg, me);
        }
        throw me;
    }

    /**
	 * Construct, log, and throw a new UnusableEntryException
	 */
    private UnusableEntryException throwNewUnusableEntryException(Entry partial, String[] badFields, Throwable[] exceptions) throws UnusableEntryException {
        final UnusableEntryException uee = new UnusableEntryException(partial, badFields, exceptions);
        if (logger.isLoggable(Levels.FAILED)) {
            logger.log(Levels.FAILED, "failure constructing entry of type " + className, uee);
        }
        throw uee;
    }

    /**
	 * Construct, log, and throw a new UnusableEntryException, that raps a given
	 * exception.
	 */
    private static UnusableEntryException throwNewUnusableEntryException(Throwable nested) throws UnusableEntryException {
        final UnusableEntryException uee = new UnusableEntryException(nested);
        if (logger.isLoggable(Levels.FAILED)) {
            logger.log(Levels.FAILED, nested.getMessage(), uee);
        }
        throw uee;
    }

    /**
	 * Construct, log, and throw a new UnusableEntryException, that will rap a
	 * newly constructed UnmarshalException (that optional wraps a given
	 * exception).
	 */
    private static UnusableEntryException throwNewUnusableEntryException(String msg, Exception nested) throws UnusableEntryException {
        final UnmarshalException ue = new UnmarshalException(msg, nested);
        final UnusableEntryException uee = new UnusableEntryException(ue);
        if (logger.isLoggable(Levels.FAILED)) {
            logger.log(Levels.FAILED, msg, uee);
        }
        throw uee;
    }
}
