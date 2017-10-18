package com.versant.core.metadata;

import java.util.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import com.versant.core.common.BindingSupportImpl;

/**
 * This knows how to analyze types to detirmine default persistence
 * information e.g. persistence-modifier, default-fetch-group and so on.
 * This is used by the runtime meta data building code and the workbench.
 *
 * @see MetaDataBuilder
 */
public final class MetaDataUtils {

    /**
     * Are we running on jdk 1.5 or higher?
     */
    private static boolean isJDK_1_5 = false;

    static {
        try {
            Class.forName("java.lang.StringBuilder");
            isJDK_1_5 = true;
        } catch (Exception e) {
        }
    }

    /**
     * This is all the class types that are mutable. These must be cloned
     * if copies is maintained.
     */
    private final HashSet MUTABLE_TYPES = new HashSet();

    /**
     * Fields with any of these modifiers may not be persistent.
     */
    private static final int PM_NONE_FIELD_MODS = Modifier.STATIC | Modifier.FINAL;

    /**
     * Fields with types in this set are persistent by default.
     */
    protected final HashSet PM_PERSISTENT_TYPES = new HashSet();

    /**
     * Field types in this set default to the default fetch group.
     */
    protected final HashSet DFG_TYPES = new HashSet();

    /**
     * Arrays of types in this set are embedded by default.
     */
    protected final HashSet EMBEDDED_ARRAY_TYPES = new HashSet();

    protected static Set BASIC_TYPES = new HashSet();

    private Set classIdSet = new HashSet();

    private MessageDigest messageDigest;

    private DataOutputStream hashOut;

    protected HashSet storeTypes = new HashSet(17);

    protected HashSet externalizedTypes = new HashSet(17);

    public MetaDataUtils() {
        Class[] mTypes = new Class[] { java.util.Date.class, java.util.List.class, java.util.ArrayList.class, java.util.LinkedList.class, java.util.Vector.class, java.util.Collection.class, java.util.Set.class, java.util.HashSet.class, java.util.TreeSet.class, java.util.SortedSet.class, java.util.SortedMap.class, java.util.Map.class, java.util.HashMap.class, java.util.TreeMap.class, java.util.Hashtable.class };
        for (int i = 0; i < mTypes.length; i++) {
            MUTABLE_TYPES.add(mTypes[i]);
        }
        Class[] dfg = new Class[] { Boolean.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Character.TYPE, Float.TYPE, Double.TYPE, Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Character.class, Float.class, Double.class, String.class, BigDecimal.class, BigInteger.class, Date.class };
        Class[] t = new Class[] { Locale.class, ArrayList.class, HashMap.class, HashSet.class, Hashtable.class, LinkedList.class, TreeMap.class, TreeSet.class, SortedSet.class, SortedMap.class, Vector.class, Collection.class, Set.class, List.class, Map.class };
        for (int i = dfg.length - 1; i >= 0; i--) {
            DFG_TYPES.add(dfg[i]);
            PM_PERSISTENT_TYPES.add(dfg[i]);
        }
        for (int i = t.length - 1; i >= 0; i--) {
            PM_PERSISTENT_TYPES.add(t[i]);
        }
        Class[] emb = new Class[] { Boolean.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Character.TYPE, Float.TYPE, Double.TYPE, Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Character.class, Float.class, Double.class };
        for (int i = emb.length - 1; i >= 0; i--) {
            if (emb[i].isPrimitive()) EMBEDDED_ARRAY_TYPES.add(emb[i]);
        }
        Class[] basicTypes = new Class[] { Boolean.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Character.TYPE, Float.TYPE, Double.TYPE, Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Character.class, Float.class, Double.class, byte[].class, char[].class, Byte[].class, Character[].class, String.class, BigDecimal.class, BigInteger.class, Date.class, Calendar.class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class };
        for (int i = 0; i < basicTypes.length; i++) {
            BASIC_TYPES.add(basicTypes[i]);
        }
        ByteArrayOutputStream devnull = new ByteArrayOutputStream(512);
        try {
            messageDigest = MessageDigest.getInstance("SHA");
            DigestOutputStream mdo = new DigestOutputStream(devnull, messageDigest);
            hashOut = new DataOutputStream(mdo);
        } catch (NoSuchAlgorithmException complain) {
            throw BindingSupportImpl.getInstance().security(complain.getMessage());
        }
    }

    /**
     * Clear the set of classId's generated so far and any other statefull
     * information.
     */
    public void clear() {
        classIdSet.clear();
        storeTypes.clear();
        externalizedTypes.clear();
    }

    /**
     * Is type a mutable type e.g. java.util.Date etc?
     */
    public boolean isMutableType(Class type) {
        return MUTABLE_TYPES.contains(type);
    }

    /**
     * Do the modifiers indicate a field can be persistent?
     */
    public boolean isPersistentModifiers(int modifiers) {
        return (modifiers & PM_NONE_FIELD_MODS) == 0;
    }

    /**
     * Do the modifiers indicate a field that is persistent by default?
     */
    public boolean isDefaultPersistentModifiers(int modifiers) {
        return isPersistentModifiers(modifiers) && !Modifier.isTransient(modifiers);
    }

    /**
     * Is a field of type that can be persistent?
     *
     * @param type     Type to check
     * @param classMap Map with all persistent classes as keys
     */
    public boolean isPersistentType(Class type, Map classMap) {
        return isDefaultPersistentType(type, classMap) || type == Object.class || type.isInterface() || (type.getComponentType() != null);
    }

    /**
     * Is a field of a type that can only be persisted through externalization?
     */
    public boolean isPersistableOnlyUsingExternalization(Class type, Map classMap) {
        boolean et = externalizedTypes.contains(type);
        if (et) externalizedTypes.remove(type);
        boolean ans = !isPersistentType(type, classMap);
        if (et) externalizedTypes.add(type);
        return ans;
    }

    /**
     * Is a field of type that should be persistent by default?
     *
     * @param type     Type to check
     * @param classMap Map with all persistent classes as keys
     */
    public boolean isDefaultPersistentType(Class type, Map classMap) {
        if (PM_PERSISTENT_TYPES.contains(type) || classMap.containsKey(type) || isTypeRegistered(type) || externalizedTypes.contains(type)) {
            return true;
        }
        type = type.getComponentType();
        if (type == null) return false;
        return PM_PERSISTENT_TYPES.contains(type) || classMap.containsKey(type) || isTypeRegistered(type);
    }

    /**
     * Can a field of type with modifiers be considered persistent?
     *
     * @param type      Type to check
     * @param modifiers Field modifiers
     * @param classMap  Map with all persistent classes as keys
     */
    public boolean isPersistentField(Class type, int modifiers, Map classMap) {
        return isPersistentModifiers(modifiers) && isPersistentType(type, classMap);
    }

    /**
     * Is the field of of those added by the enhancer?
     */
    public boolean isEnhancerAddedField(String fieldName) {
        return fieldName.startsWith("jdo");
    }

    /**
     * Should f be considered persistent by default?
     */
    public boolean isDefaultPersistentField(ClassMetaData.FieldInfo f, Map classMap) {
        return !isEnhancerAddedField(f.getName()) && isDefaultPersistentModifiers(f.getModifiers()) && isDefaultPersistentType(f.getType(), classMap);
    }

    /**
     * Should f be considered persistent by default?
     */
    public boolean isDefaultPersistentField(Field f, Map classMap) {
        return !isEnhancerAddedField(f.getName()) && isDefaultPersistentModifiers(f.getModifiers()) && isDefaultPersistentType(f.getType(), classMap);
    }

    /**
     * Can f be persisted?
     */
    public boolean isPersistableField(Field f, Map classMap) {
        return !isEnhancerAddedField(f.getName()) && isPersistentModifiers(f.getModifiers());
    }

    /**
     * Should this field be part of the default fetch group by default?
     */
    public boolean isDefaultFetchGroupType(Class type) {
        return DFG_TYPES.contains(type) || isTypeRegistered(type) || externalizedTypes.contains(type);
    }

    /**
     * Should a field of type be embedded by default?
     */
    public boolean isEmbeddedType(Class type) {
        if (DFG_TYPES.contains(type) || externalizedTypes.contains(type)) {
            return true;
        }
        type = type.getComponentType();
        return type != null && EMBEDDED_ARRAY_TYPES.contains(type);
    }

    /**
     * What category does a field with the supplied attributes belong to?
     *
     * @param persistenceModifier Transactional fields are CATEGORY_TRANSACTIONAL
     * @param type                Type of field
     * @param classMap            Map with all persistent classes as keys
     * @return One of the MDStatics.CATEGORY_xxx constants
     * @see MDStatics
     */
    public int getFieldCategory(int persistenceModifier, Class type, Map classMap) {
        switch(persistenceModifier) {
            case MDStatics.PERSISTENCE_MODIFIER_PERSISTENT:
                if (externalizedTypes.contains(type)) {
                    return MDStatics.CATEGORY_EXTERNALIZED;
                } else if (type.getComponentType() != null) {
                    return MDStatics.CATEGORY_ARRAY;
                } else if (classMap.containsKey(type)) {
                    if (type.isInterface()) return MDStatics.CATEGORY_POLYREF;
                    return MDStatics.CATEGORY_REF;
                } else if (Collection.class.isAssignableFrom(type)) {
                    return MDStatics.CATEGORY_COLLECTION;
                } else if (Map.class.isAssignableFrom(type)) {
                    return MDStatics.CATEGORY_MAP;
                } else if (type.isInterface() || type == Object.class) {
                    return MDStatics.CATEGORY_POLYREF;
                } else if (!Map.class.isAssignableFrom(type) && !Collection.class.isAssignableFrom(type) && !isPersistentType(type, classMap)) {
                    return MDStatics.CATEGORY_EXTERNALIZED;
                }
                return MDStatics.CATEGORY_SIMPLE;
            case MDStatics.PERSISTENCE_MODIFIER_TRANSACTIONAL:
                return MDStatics.CATEGORY_TRANSACTIONAL;
            case MDStatics.PERSISTENCE_MODIFIER_NONE:
                return MDStatics.CATEGORY_NONE;
        }
        throw BindingSupportImpl.getInstance().internal("Bad persistence-modifier code: " + persistenceModifier);
    }

    /**
     * Generate the classId for the class. This is a positive int computed
     * from a hash of the class name with duplicates resolved by incrementing
     * the id.
     */
    public int generateClassId(String qname) {
        int classId = computeClassId(qname);
        for (; classIdSet.contains(new Integer(classId)); classId = (classId + 1) & 0x7FFFFFFF) {
            ;
        }
        classIdSet.add(new Integer(classId));
        return classId;
    }

    private int computeClassId(String className) {
        int hash = 0;
        try {
            hashOut.writeUTF(className);
            hashOut.flush();
            byte hasharray[] = messageDigest.digest();
            int len = hasharray.length;
            if (len > 8) len = 8;
            for (int i = 0; i < len; i++) {
                hash += (hasharray[i] & 255) << (i * 4);
            }
            hash &= 0x7FFFFFFF;
        } catch (IOException ignore) {
            hash = -1;
        }
        return hash;
    }

    /**
     * Register a store specific persistent type.
     */
    public void registerStoreType(Class type) {
        storeTypes.add(type);
    }

    /**
     * Has a store specific persistent type been registered?
     */
    public boolean isTypeRegistered(Class type) {
        return storeTypes.contains(type);
    }

    /**
     * Get the element type for a Collection if possible using the JDK 1.5
     * generics API or null if not possible.
     */
    public static Class getGenericElementType(Field field) {
        return getType(field, 0);
    }

    /**
     * Get the key type for a Map if possible using the JDK 1.5
     * generics API or null if not possible.
     */
    public static Class getGenericKeyType(Field field) {
        return getType(field, 0);
    }

    /**
     * Get the value type for a Map if possible using the JDK 1.5 generics
     * API or null if not possible.
     */
    public static Class getGenericValueType(Field field) {
        return getType(field, 1);
    }

    /**
     * Get the class type of Collections and Maps with jdk1.5 generics if at all
     * possible
     */
    private static Class getType(Field field, int index) {
        if (isJDK_1_5) {
            Class clazz = null;
            try {
                Method methodGetGenericType = field.getClass().getMethod("getGenericType", new Class[] {});
                if (methodGetGenericType == null) return null;
                Object type = methodGetGenericType.invoke(field, new Object[] {});
                if (type == null) return null;
                Method methodActualTypeArguments = type.getClass().getMethod("getActualTypeArguments", new Class[] {});
                if (methodActualTypeArguments == null) return null;
                Object typeArray = methodActualTypeArguments.invoke(type, new Object[] {});
                if (typeArray == null) return null;
                Object[] types = (Object[]) typeArray;
                clazz = (Class) types[index];
            } catch (Exception e) {
            }
            return clazz;
        } else {
            return null;
        }
    }

    /**
     * Register a type that is persisted using an externalizer.
     */
    public void registerExternalizedType(Class t) {
        externalizedTypes.add(t);
    }

    /**
     * If this is a Basic type as defined by ejb3 spec.
     */
    public boolean isBasicType(Class t) {
        return BASIC_TYPES.contains(t);
    }
}
