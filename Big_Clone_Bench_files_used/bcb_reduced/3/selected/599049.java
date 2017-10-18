package org.datanucleus.enhancer.bcel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import javax.jdo.JDOFatalException;
import javax.jdo.spi.PersistenceCapable;
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.enhancer.AbstractClassEnhancer;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassField;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.DataNucleusEnhancer;
import org.datanucleus.enhancer.bcel.metadata.BCELClassMetaData;
import org.datanucleus.enhancer.bcel.metadata.BCELFieldPropertyMetaData;
import org.datanucleus.enhancer.bcel.metadata.BCELMember;
import org.datanucleus.enhancer.bcel.method.CheckReadMethod;
import org.datanucleus.enhancer.bcel.method.CheckWriteMethod;
import org.datanucleus.enhancer.bcel.method.DefaultConstructor;
import org.datanucleus.enhancer.bcel.method.InitFieldFlags;
import org.datanucleus.enhancer.bcel.method.InitFieldNames;
import org.datanucleus.enhancer.bcel.method.InitFieldTypes;
import org.datanucleus.enhancer.bcel.method.InitPersistenceCapableSuperClass;
import org.datanucleus.enhancer.bcel.method.JdoCopyField;
import org.datanucleus.enhancer.bcel.method.JdoCopyFields;
import org.datanucleus.enhancer.bcel.method.JdoCopyKeyFieldsFromObjectId;
import org.datanucleus.enhancer.bcel.method.JdoCopyKeyFieldsFromObjectId2;
import org.datanucleus.enhancer.bcel.method.JdoCopyKeyFieldsToObjectId;
import org.datanucleus.enhancer.bcel.method.JdoCopyKeyFieldsToObjectId2;
import org.datanucleus.enhancer.bcel.method.JdoGetManagedFieldCount;
import org.datanucleus.enhancer.bcel.method.JdoGetObjectId;
import org.datanucleus.enhancer.bcel.method.JdoGetPersistenceManager;
import org.datanucleus.enhancer.bcel.method.JdoGetTransactionalObjectId;
import org.datanucleus.enhancer.bcel.method.JdoGetVersion;
import org.datanucleus.enhancer.bcel.method.JdoIsDeleted;
import org.datanucleus.enhancer.bcel.method.JdoIsDetached;
import org.datanucleus.enhancer.bcel.method.JdoIsDirty;
import org.datanucleus.enhancer.bcel.method.JdoIsNew;
import org.datanucleus.enhancer.bcel.method.JdoIsPersistent;
import org.datanucleus.enhancer.bcel.method.JdoIsTransactional;
import org.datanucleus.enhancer.bcel.method.JdoMakeDirty;
import org.datanucleus.enhancer.bcel.method.JdoNewInstance1;
import org.datanucleus.enhancer.bcel.method.JdoNewInstance2;
import org.datanucleus.enhancer.bcel.method.JdoNewObjectIdInstance1;
import org.datanucleus.enhancer.bcel.method.JdoNewObjectIdInstance2;
import org.datanucleus.enhancer.bcel.method.JdoPreSerialize;
import org.datanucleus.enhancer.bcel.method.JdoProvideField;
import org.datanucleus.enhancer.bcel.method.JdoProvideFields;
import org.datanucleus.enhancer.bcel.method.JdoReplaceDetachedState;
import org.datanucleus.enhancer.bcel.method.JdoReplaceField;
import org.datanucleus.enhancer.bcel.method.JdoReplaceFields;
import org.datanucleus.enhancer.bcel.method.JdoReplaceFlags;
import org.datanucleus.enhancer.bcel.method.JdoReplaceStateManager;
import org.datanucleus.enhancer.bcel.method.LoadClass;
import org.datanucleus.enhancer.bcel.method.MediateReadMethod;
import org.datanucleus.enhancer.bcel.method.MediateWriteMethod;
import org.datanucleus.enhancer.bcel.method.NormalGetMethod;
import org.datanucleus.enhancer.bcel.method.NormalSetMethod;
import org.datanucleus.enhancer.bcel.method.ParentManagedFieldNum;
import org.datanucleus.enhancer.bcel.method.PropertyGetterMethod;
import org.datanucleus.enhancer.bcel.method.PropertySetterMethod;
import org.datanucleus.enhancer.bcel.method.SuperClone;
import org.datanucleus.enhancer.bcel.method.WriteObject;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.StringUtils;

/**
 * Class enhancer using Apache BCEL (http://jakarta.apache.org/bcel).
 */
public class BCELClassEnhancer extends AbstractClassEnhancer {

    /** Original class */
    public final JavaClass oldClass;

    /** enhancing class */
    public final ClassGen newClass;

    /** constant pool of enhancing class */
    public final ConstantPoolGen constantPoolGen;

    /** class type of enhancing class */
    public final ObjectType classType;

    /** serialVersionUID value */
    protected long addSerialVersionUID;

    /** Field type of jdoFlag */
    public static final Type OT_Flag = Type.BYTE;

    /** Type of Object[] */
    public static final Type OT_ObjectArray = new ArrayType(Type.OBJECT, 1);

    /** Object type of SingleFieldIdentity classes */
    public static final ObjectType OT_LongIdentity = new ObjectType(CN_LongIdentity);

    public static final ObjectType OT_StringIdentity = new ObjectType(CN_StringIdentity);

    public static final ObjectType OT_ShortIdentity = new ObjectType(CN_ShortIdentity);

    public static final ObjectType OT_IntIdentity = new ObjectType(CN_IntIdentity);

    public static final ObjectType OT_CharIdentity = new ObjectType(CN_CharIdentity);

    public static final ObjectType OT_ByteIdentity = new ObjectType(CN_ByteIdentity);

    public static final ObjectType OT_ObjectIdentity = new ObjectType(CN_ObjectIdentity);

    /** Object type of javax.spi.PersistenceManager */
    public static final ObjectType OT_PersistenceManager = new ObjectType(CN_PersistenceManager);

    /** Object type of javax.jdo.spi.PersistenceCapable.ObjectIdFieldConsumer */
    public static final ObjectType OT_ObjectIdFieldConsumer = new ObjectType(CN_ObjectIdFieldConsumer);

    /** Object type of javax.jdo.spi.PersistenceCapable.ObjectIdFieldSupplier */
    public static final ObjectType OT_ObjectIdFieldSupplier = new ObjectType(CN_ObjectIdFieldSupplier);

    /** Object type of java.util.BitSet */
    public static final ObjectType OT_BitSet = new ObjectType(CN_BitSet);

    /** Object type of javax.jdo.spi.StateManager */
    public static final ObjectType OT_StateManager = new ObjectType(CN_StateManager);

    /** Object type of javax.jdo.spi.PersistenceCapable */
    public static final ObjectType OT_PersistenceCapable = new ObjectType(CN_PersistenceCapable);

    /** Object type of javax.jdo.spi.Detachable */
    public static final ObjectType OT_Detachable = new ObjectType(CN_Detachable);

    /** Object type of javax.jdo.spi.JDOImplHelper */
    public static final ObjectType OT_JDOImplHelper = new ObjectType(CN_JDOImplHelper);

    /** Object type of java.lang.Class */
    public static final ObjectType OT_CLASS = new ObjectType(CN_Class);

    /**
     * Constructor.
     * @param cmd MetaData for the class to be enhanced
     * @param clr ClassLoader resolver
     * @param classBytes Bytes of the class (unenhanced)
     */
    public BCELClassEnhancer(ClassMetaData cmd, ClassLoaderResolver clr, byte[] classBytes) {
        this(cmd, clr);
        NucleusRepository rep = (NucleusRepository) Repository.getRepository();
        rep.defineClass(cmd.getFullClassName(), classBytes);
    }

    /**
     * Constructor.
     * @param cmd MetaData for the class to be enhanced
     * @param clr ClassLoader resolver
     */
    public BCELClassEnhancer(ClassMetaData cmd, ClassLoaderResolver clr) {
        super(cmd, clr);
        org.apache.bcel.util.Repository rep = Repository.getRepository();
        if (rep == null || !(rep instanceof NucleusRepository)) {
            Repository.setRepository(new NucleusRepository(clr));
        }
        if (!(cmd instanceof BCELClassMetaData)) {
            throw new RuntimeException("MetaData for class " + cmd.getFullClassName() + " is not BCEL-specific and so cannot be used");
        }
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled()) {
            DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.SetupClass", className));
        }
        this.oldClass = ((BCELClassMetaData) cmd).getEnhanceClass();
        this.newClass = ((BCELClassMetaData) cmd).getClassGen();
        this.constantPoolGen = this.newClass.getConstantPool();
        this.classType = new ObjectType(className);
        for (int i = 0; i < cmd.getNoOfMembers(); i++) {
            AbstractMemberMetaData fmd = (AbstractMemberMetaData) cmd.getMetaDataForMemberAtRelativePosition(i);
            if (!fmd.isFinal() && !fmd.isStatic()) {
                String fieldName = fmd.getName();
                boolean found = false;
                if (fmd instanceof PropertyMetaData) {
                    Method method = BCELUtils.getGetterByName(fieldName, newClass);
                    if (method != null) {
                        found = true;
                    }
                }
                if (!found) {
                    Field field = BCELUtils.getFieldByName(fieldName, newClass);
                    if (field == null) {
                        if (fmd instanceof FieldMetaData) {
                            throw new RuntimeException(LOCALISER.msg("Enhancer.ClassHasNoSuchField", newClass.getClassName(), fieldName));
                        } else {
                            throw new RuntimeException(LOCALISER.msg("Enhancer.ClassHasNoSuchMethod", newClass.getClassName(), ClassUtils.getJavaBeanGetterName(fieldName, fmd.getType() == Boolean.class)));
                        }
                    }
                }
            }
        }
    }

    /**
     * Convenience accessor for the class name that is stored in a particular
     * class.
     * @param filename Name of the file
     * @return The class name
     */
    public static String getClassNameForFileName(String filename) {
        try {
            return new ClassParser(filename).parse().getClassName();
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Initialise the methods that we need to add to this class
     */
    protected void initialiseMethodsList() {
        if (cmd.getPersistenceCapableSuperclass() == null) {
            methodsToAdd.add(JdoCopyKeyFieldsFromObjectId.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsFromObjectId2.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsToObjectId.getInstance(this));
            methodsToAdd.add(JdoCopyKeyFieldsToObjectId2.getInstance(this));
            methodsToAdd.add(JdoGetObjectId.getInstance(this));
            methodsToAdd.add(JdoGetVersion.getInstance(this));
            methodsToAdd.add(JdoPreSerialize.getInstance(this));
            methodsToAdd.add(JdoGetPersistenceManager.getInstance(this));
            methodsToAdd.add(JdoGetTransactionalObjectId.getInstance(this));
            methodsToAdd.add(JdoIsDeleted.getInstance(this));
            methodsToAdd.add(JdoIsDirty.getInstance(this));
            methodsToAdd.add(JdoIsNew.getInstance(this));
            methodsToAdd.add(JdoIsPersistent.getInstance(this));
            methodsToAdd.add(JdoIsTransactional.getInstance(this));
            methodsToAdd.add(JdoMakeDirty.getInstance(this));
            methodsToAdd.add(JdoNewObjectIdInstance1.getInstance(this));
            methodsToAdd.add(JdoNewObjectIdInstance2.getInstance(this));
            methodsToAdd.add(JdoProvideFields.getInstance(this));
            methodsToAdd.add(JdoReplaceFields.getInstance(this));
            methodsToAdd.add(JdoReplaceFlags.getInstance(this));
            methodsToAdd.add(JdoReplaceStateManager.getInstance(this));
        }
        if (requiresDetachable()) {
            methodsToAdd.add(JdoReplaceDetachedState.getInstance(this));
        }
        methodsToAdd.add(JdoIsDetached.getInstance(this));
        methodsToAdd.add(JdoNewInstance1.getInstance(this));
        methodsToAdd.add(JdoNewInstance2.getInstance(this));
        methodsToAdd.add(JdoReplaceField.getInstance(this));
        methodsToAdd.add(JdoProvideField.getInstance(this));
        methodsToAdd.add(JdoCopyField.getInstance(this));
        methodsToAdd.add(JdoCopyFields.getInstance(this));
        methodsToAdd.add(InitFieldNames.getInstance(this));
        methodsToAdd.add(InitFieldTypes.getInstance(this));
        methodsToAdd.add(InitFieldFlags.getInstance(this));
        methodsToAdd.add(ParentManagedFieldNum.getInstance(this));
        methodsToAdd.add(JdoGetManagedFieldCount.getInstance(this));
        methodsToAdd.add(InitPersistenceCapableSuperClass.getInstance(this));
        methodsToAdd.add(LoadClass.getInstance(this));
        methodsToAdd.add(SuperClone.getInstance(this));
        if (checkHasDefaultConstructor() != null) {
            methodsToAdd.add(DefaultConstructor.getInstance(this));
        }
        try {
            if (BCELUtils.isInstanceof(oldClass, Serializable.class) && BCELUtils.findMethod(newClass, "writeObject", "(Ljava/io/ObjectOutputStream;)V") == null) {
                methodsToAdd.add(WriteObject.getInstance(this));
            }
        } catch (ClassNotFoundException e) {
            DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.ErrorEnhancingClass", cmd.getFullClassName(), e));
        }
    }

    /**
     * Initialise the fields that we need to add to this class
     */
    protected void initialiseFieldsList() {
        if (cmd.getPersistenceCapableSuperclass() == null) {
            fieldsToAdd.add(new ClassField(this, FN_StateManager, Constants.ACC_PROTECTED | Constants.ACC_TRANSIENT, OT_StateManager));
            fieldsToAdd.add(new ClassField(this, FN_Flag, Constants.ACC_PROTECTED | Constants.ACC_TRANSIENT, OT_Flag));
        }
        if (requiresDetachable()) {
            fieldsToAdd.add(new ClassField(this, FN_JdoDetachedState, Constants.ACC_PROTECTED, OT_ObjectArray));
        }
        fieldsToAdd.add(new ClassField(this, FN_FieldFlags, Constants.ACC_PRIVATE | Constants.ACC_STATIC | Constants.ACC_FINAL, new ArrayType(Type.BYTE, 1)));
        fieldsToAdd.add(new ClassField(this, FN_PersistenceCapableSuperclass, Constants.ACC_PRIVATE | Constants.ACC_STATIC | Constants.ACC_FINAL, OT_CLASS));
        fieldsToAdd.add(new ClassField(this, FN_FieldTypes, Constants.ACC_PRIVATE | Constants.ACC_STATIC | Constants.ACC_FINAL, new ArrayType(Class.class.getName(), 1)));
        fieldsToAdd.add(new ClassField(this, FN_FieldNames, Constants.ACC_PRIVATE | Constants.ACC_STATIC | Constants.ACC_FINAL, new ArrayType(Type.STRING, 1)));
        fieldsToAdd.add(new ClassField(this, FN_JdoInheritedFieldCount, Constants.ACC_PRIVATE | Constants.ACC_STATIC | Constants.ACC_FINAL, Type.INT));
        try {
            if (BCELUtils.isInstanceof(oldClass, Serializable.class) && BCELUtils.getFieldByName(FN_serialVersionUID, newClass) == null) {
                fieldsToAdd.add(new ClassField(this, FN_serialVersionUID, Constants.ACC_PRIVATE | Constants.ACC_STATIC | Constants.ACC_FINAL, Type.LONG));
                addSerialVersionUID = new SerialVersionUID().computeSerialVersionUID(oldClass);
            }
        } catch (ClassNotFoundException e) {
            DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.ErrorEnhancingClass", cmd.getFullClassName(), e));
        }
    }

    /**
     * Check original class is already enhanced.
     * @return Return true if already enhanced class.
     */
    public boolean validate() {
        if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE && cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_AWARE) {
            return false;
        }
        if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE) {
            return true;
        }
        String interfaceNames[] = newClass.getInterfaceNames();
        if (interfaceNames != null) {
            for (int i = 0; i < interfaceNames.length; i++) {
                if (interfaceNames[i].equals(CN_PersistenceCapable)) {
                    return true;
                }
            }
        }
        Method methods[] = newClass.getMethods();
        if (methods != null) {
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("jdoReplaceField")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Accessor for the AccessFlags for the <code>className</code> inner class
     * @param javaClass The original java class
     * @param className the inner class name
     * @return the access flags
     */
    private int getAccessFlagsForInnerClass(JavaClass javaClass, String className) {
        for (int i = 0; i < javaClass.getAttributes().length; i++) {
            if (javaClass.getAttributes()[i] instanceof InnerClasses) {
                InnerClasses innerClasses = (InnerClasses) javaClass.getAttributes()[i];
                for (int j = 0; j < innerClasses.getInnerClasses().length; j++) {
                    String name = constantPoolGen.getConstantPool().getConstantString(innerClasses.getInnerClasses()[j].getInnerClassIndex(), Constants.CONSTANT_Class);
                    if (name.equals(className)) {
                        return innerClasses.getInnerClasses()[i].getInnerAccessFlags();
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Check original class has default(no arg) constructor. <br>
     * Original class must has default(no arg) constructor.
     * @return Return null if this class has default constructor.
     */
    protected String checkHasDefaultConstructor() {
        Method methods[] = newClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(Constants.CONSTRUCTOR_NAME)) {
                if (methods[i].getSignature().equals("()V")) {
                    return null;
                }
            }
        }
        return LOCALISER.msg("Enhancer.RequiresDefaultConstructor", newClass.getClassName());
    }

    /**
     * Method to enhance the classes
     * @return Whether the class was enhanced successfully
     */
    public boolean enhance() {
        if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE && cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_AWARE) {
            return false;
        }
        initialise();
        if (validate() && cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE) {
            DataNucleusEnhancer.LOGGER.info(LOCALISER.msg("Enhancer.ClassIsAlreadyEnhanced", newClass.getClassName()));
            return true;
        }
        if (ClassUtils.isInnerClass(this.className) && (getAccessFlagsForInnerClass(oldClass, this.className) & Constants.ACC_STATIC) == 0) {
            DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.PersistentInnerClassMustBeStatic", newClass.getClassName()));
            return false;
        }
        try {
            if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE) {
                enhanceOriginalMethods();
                enhanceClass();
                enhanceFields();
                enhanceMethods();
                enhanceStaticInitializers();
            } else if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_AWARE) {
                enhanceOriginalMethods();
            }
        } catch (Exception e) {
            DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.ErrorEnhancingClass", cmd.getFullClassName(), e.getMessage()), e);
            return false;
        }
        update = true;
        return true;
    }

    /**
     * Method to enhance the class as a whole, providing the required interfaces
     * and adding any setters/getters for its fields
     */
    protected void enhanceClass() {
        if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE) {
            class_addInterface(ClassEnhancer.CN_PersistenceCapable);
            if (requiresDetachable()) {
                class_addInterface(ClassEnhancer.CN_Detachable);
            }
            for (int i = 0; i < cmd.getNoOfMembers(); i++) {
                AbstractMemberMetaData f = cmd.getMetaDataForMemberAtRelativePosition(i);
                if (f.fieldBelongsToClass() && f.getPersistenceModifier() != FieldPersistenceModifier.NONE && !((BCELFieldPropertyMetaData) f).getEnhanceField().isSynthetic()) {
                    enhanceSetter((BCELFieldPropertyMetaData) f);
                    enhanceGetter((BCELFieldPropertyMetaData) f);
                }
            }
        }
    }

    /**
     * Method to add the "implements {interface}" to the class description.
     * @param interfaceName Name of the interface to add.
     */
    protected void class_addInterface(String interfaceName) {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled()) {
            DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.AddInterface", interfaceName));
        }
        newClass.addInterface(interfaceName);
        newClass.update();
    }

    /**
     * Method to enhance the fields
     */
    protected void enhanceFields() {
        for (int i = 0; i < fieldsToAdd.size(); i++) {
            ClassField cf = (ClassField) fieldsToAdd.get(i);
            FieldGen gen = new FieldGen(cf.getAccess(), (Type) cf.getType(), cf.getName(), constantPoolGen);
            Field f = gen.getField();
            newClass.addField(f);
            BCELUtils.addSynthetic(f, constantPoolGen);
            if (DataNucleusEnhancer.LOGGER.isDebugEnabled()) {
                DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.AddField", f.getType() + " " + f.getName()));
            }
            newClass.update();
        }
    }

    /**
     * Method to enhance the methods of the class. Processes all methods in methodsToAdd, adding them to the class.
     */
    protected void enhanceMethods() {
        for (int i = 0; i < methodsToAdd.size(); i++) {
            Object o = methodsToAdd.get(i);
            if (o instanceof ClassMethod) {
                ClassMethod method = (ClassMethod) o;
                method.initialise();
                method.execute();
                method.close();
            } else if (o == null) {
                DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.CallbackIsNullError"));
            } else {
                DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.CallbackIsNotMethodBuilderError", o.getClass().getName()));
            }
        }
    }

    protected void enhanceStaticInitializers() {
        InstructionList il = null;
        InstructionFactory factory = new InstructionFactory(newClass);
        Method clinit = null;
        InstructionList ilOriginal = null;
        MethodGen methodGen = null;
        MethodGen methodGenOriginal = null;
        {
            Method methods[] = newClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(Constants.STATIC_INITIALIZER_NAME)) {
                    clinit = methods[i];
                    methodGenOriginal = new MethodGen(clinit, className, constantPoolGen);
                    ilOriginal = methodGenOriginal.getInstructionList();
                    break;
                }
            }
        }
        il = new InstructionList();
        methodGen = new MethodGen(Constants.ACC_STATIC, Type.VOID, Type.NO_ARGS, null, Constants.STATIC_INITIALIZER_NAME, className, il, constantPoolGen);
        if (ilOriginal != null) {
            il.append(ilOriginal);
            InstructionHandle h[] = il.getInstructionHandles();
            if ("return".equalsIgnoreCase(h[h.length - 1].getInstruction().getName())) {
                try {
                    il.delete(h[h.length - 1]);
                } catch (TargetLostException e) {
                    InstructionHandle[] targets = e.getTargets();
                    for (int i2 = 0; i2 < targets.length; i2++) {
                        InstructionTargeter[] targeters = targets[i2].getTargeters();
                        for (int j = 0; j < targeters.length; j++) {
                            targeters[j].updateTarget(targets[i2], h[j]);
                        }
                    }
                }
            }
        }
        if (addSerialVersionUID != 0) {
            int svUidIndex = constantPoolGen.addLong(addSerialVersionUID);
            il.append(new LDC2_W(svUidIndex));
            il.append(factory.createPutStatic(className, FN_serialVersionUID, Type.LONG));
        }
        il.append(factory.createInvoke(className, MN_FieldNamesInitMethod, new ArrayType(Type.STRING, 1), Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(factory.createPutStatic(className, FN_FieldNames, new ArrayType(Type.STRING, 1)));
        il.append(factory.createInvoke(className, MN_FieldTypesInitMethod, new ArrayType(OT_CLASS, 1), Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(factory.createPutStatic(className, FN_FieldTypes, new ArrayType(OT_CLASS, 1)));
        il.append(factory.createInvoke(className, MN_FieldFlagsInitMethod, new ArrayType(Type.BYTE, 1), Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(factory.createPutStatic(className, FN_FieldFlags, new ArrayType(Type.BYTE, 1)));
        il.append(factory.createInvoke(className, MN_JdoGetInheritedFieldCount, Type.INT, Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(factory.createPutStatic(className, FN_JdoInheritedFieldCount, Type.INT));
        il.append(factory.createInvoke(className, MN_JdoPersistenceCapableSuperclassInit, OT_CLASS, Type.NO_ARGS, Constants.INVOKESTATIC));
        il.append(factory.createPutStatic(className, FN_PersistenceCapableSuperclass, OT_CLASS));
        il.append(new LDC(constantPoolGen.addString(className)));
        il.append(factory.createInvoke(className, MN_jdoLoadClass, OT_CLASS, new Type[] { Type.STRING }, Constants.INVOKESTATIC));
        il.append(factory.createGetStatic(className, FN_FieldNames, new ArrayType(Type.STRING, 1)));
        il.append(factory.createGetStatic(className, FN_FieldTypes, new ArrayType(OT_CLASS, 1)));
        il.append(factory.createGetStatic(className, FN_FieldFlags, new ArrayType(Type.BYTE, 1)));
        il.append(factory.createGetStatic(className, FN_PersistenceCapableSuperclass, OT_CLASS));
        if (((BCELClassMetaData) cmd).getClassGen().isAbstract()) {
            il.append(InstructionConstants.ACONST_NULL);
        } else {
            il.append(factory.createNew(new ObjectType(className)));
            il.append(InstructionConstants.DUP);
            il.append(factory.createInvoke(className, Constants.CONSTRUCTOR_NAME, Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
        }
        il.append(factory.createInvoke(CN_JDOImplHelper, "registerClass", Type.VOID, new Type[] { OT_CLASS, new ArrayType(Type.STRING, 1), new ArrayType(OT_CLASS, 1), new ArrayType(Type.BYTE, 1), OT_CLASS, OT_PersistenceCapable }, Constants.INVOKESTATIC));
        staticInitializerAppend(factory, il);
        il.append(InstructionConstants.RETURN);
        if (clinit != null) {
            newClass.removeMethod(clinit);
        }
        methodGen.setMaxLocals();
        methodGen.setMaxStack();
        Method method = methodGen.getMethod();
        {
            Method allMethod[] = newClass.getMethods();
            newClass.replaceMethod(allMethod[0], method);
            newClass.addMethod(allMethod[0]);
        }
        il.dispose();
    }

    protected void staticInitializerAppend(InstructionFactory factory, InstructionList il) {
    }

    protected void enhanceOriginalMethod(Method m) {
        boolean isDebugEnabled = DataNucleusEnhancer.LOGGER.isDebugEnabled();
        if (BCELUtils.isSynthetic(m)) {
            return;
        }
        MethodGen methodGen = new MethodGen(m, className, constantPoolGen);
        InstructionList il = methodGen.getInstructionList();
        if ((il == null) || (il.size() == 0)) {
            return;
        }
        InstructionFactory factory = new InstructionFactory(newClass);
        boolean isCloneMethod = ("clone".equals(m.getName()) && ((m.getArgumentTypes() == null) || (m.getArgumentTypes().length == 0)));
        boolean change = false;
        InstructionHandle ih = il.getStart();
        while (ih != null) {
            Instruction i = ih.getInstruction();
            if ((i instanceof GETFIELD) || (i instanceof PUTFIELD)) {
                Field f;
                FieldInstruction field = (FieldInstruction) i;
                Constant c = m.getConstantPool().getConstant(field.getIndex());
                ConstantFieldref fieldRef = (ConstantFieldref) c;
                ConstantClass cclass = (ConstantClass) m.getConstantPool().getConstant(fieldRef.getClassIndex());
                ConstantUtf8 utfClassName = (ConstantUtf8) m.getConstantPool().getConstant(cclass.getNameIndex());
                String utfClassNameString = StringUtils.replaceAll(utfClassName.getBytes().toString(), "/", ".");
                JavaClass fieldJavaClass = null;
                try {
                    fieldJavaClass = Repository.lookupClass(utfClassNameString);
                } catch (Throwable ex) {
                    DataNucleusEnhancer.LOGGER.error(LOCALISER.msg("Enhancer.ClassNotFound", utfClassNameString, ex));
                    throw new JDOFatalException(LOCALISER.msg("Enhancer.ClassNotFound", utfClassNameString, ex));
                }
                if (fieldJavaClass == null) {
                    throw new JDOFatalException(LOCALISER.msg("Enhancer.ClassNotFound", utfClassNameString, new NullPointerException()));
                }
                f = BCELUtils.getFieldByName(field.getName(constantPoolGen), fieldJavaClass);
                if (f == null) {
                    String message = LOCALISER.msg("Enhancer.FieldIsNull", className, m.getName(), field.getName(constantPoolGen));
                    DataNucleusEnhancer.LOGGER.error(message);
                    throw new NullPointerException(message);
                }
                ClassGen cg = BCELUtils.getClassByFieldByName(field.getName(constantPoolGen), fieldJavaClass);
                BCELFieldPropertyMetaData fieldConfig = null;
                BCELClassMetaData jdoConfigClass = ((BCELClassMetaData) cmd);
                if (!cg.getClassName().equals(newClass.getClassName())) {
                    jdoConfigClass = (BCELClassMetaData) cmd.getPackageMetaData().getFileMetaData().getMetaDataManager().getMetaDataForClass(cg.getClassName(), clr);
                }
                if (jdoConfigClass != null) {
                    AbstractMemberMetaData apmd = jdoConfigClass.findField(f);
                    if (apmd == null) {
                        if (jdoConfigClass.findProperty(f) == null) {
                            String message = LOCALISER.msg("Enhancer.FieldConfigIsNullError", className + "." + f.getName());
                            DataNucleusEnhancer.LOGGER.fatal(message);
                            throw new RuntimeException(message);
                        }
                    }
                    if (apmd != null && apmd.getPersistenceModifier() != FieldPersistenceModifier.NONE) {
                    }
                    if (!isFieldAccessInPersistenceCapableClass(ih, m.getConstantPool())) {
                    } else if (fieldConfig != null && fieldConfig.getJdoFieldFlag() == 0) {
                    } else if (f.isStatic() || f.isFinal()) {
                    } else if (BCELUtils.isSynthetic(f)) {
                    } else {
                        if (isDebugEnabled) {
                            DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.EnhanceOriginalMethod", className + "." + m.getName(), f.getName()));
                        }
                        if (apmd != null && apmd instanceof FieldMetaData && apmd.getPersistenceModifier() != FieldPersistenceModifier.NONE) {
                            if (i instanceof GETFIELD) {
                                ih.setInstruction(factory.createInvoke(cg.getClassName(), "jdo" + BCELUtils.getGetterName(f), field.getType(constantPoolGen), new Type[] { new ObjectType(cg.getClassName()) }, Constants.INVOKESTATIC));
                            } else {
                                ih.setInstruction(factory.createInvoke(cg.getClassName(), "jdo" + BCELUtils.getSetterName(f), Type.VOID, new Type[] { new ObjectType(cg.getClassName()), field.getType(constantPoolGen) }, Constants.INVOKESTATIC));
                            }
                            change = true;
                        }
                    }
                }
            } else if (isCloneMethod && (i instanceof INVOKESPECIAL)) {
                INVOKESPECIAL is = (INVOKESPECIAL) i;
                if ((cmd.getPersistenceCapableSuperclass() == null) && ("clone".equals(is.getMethodName(constantPoolGen))) && ("()Ljava/lang/Object;".equals(is.getSignature(constantPoolGen)))) {
                    ih.setInstruction(factory.createInvoke(className, ClassEnhancer.MN_JdoSuperClone, Type.OBJECT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                    if (isDebugEnabled) {
                        DataNucleusEnhancer.LOGGER.debug(LOCALISER.msg("Enhancer.EnhanceOriginalMethod", className + "." + m.getName(), "super.clone()"));
                    }
                    change = true;
                }
            }
            ih = ih.getNext();
        }
        if (change) {
            methodGen.setMaxLocals();
            methodGen.setMaxStack();
            newClass.replaceMethod(m, methodGen.getMethod());
        }
    }

    /**
     * Takes the original getXXX, setXXX method and takes the code and creates a jdoGetXXX, jdoSetXXX
     * with the exact same code. The jdoGetXXX, jdoSetXXX are not static (unlike for persistent fields).
     * e.g.
     * <code>
     * public String getName()
     * {
     *    return name;
     * }
     * </code>
     * The generated method is:
     * <code>
     * public String jdoGetName()
     * {
     *    return name;
     * }
     * </code>
     * 
     * @param m The method
     */
    protected void enhancePropertyAccessor(Method m) {
        if (BCELUtils.isSynthetic(m)) {
            return;
        }
        String getterName = ClassUtils.getFieldNameForJavaBeanGetter(m.getName());
        String setterName = ClassUtils.getFieldNameForJavaBeanSetter(m.getName());
        String newMethodName = null;
        String propertyName = null;
        if (getterName != null) {
            newMethodName = "jdo" + BCELUtils.getGetterName(getterName);
            propertyName = getterName;
        } else if (setterName != null) {
            newMethodName = "jdo" + BCELUtils.getSetterName(setterName);
            propertyName = setterName;
        } else {
            return;
        }
        AbstractMemberMetaData apmd = cmd.getMetaDataForMember(propertyName);
        if (apmd == null || apmd instanceof FieldMetaData || apmd.getPersistenceModifier() == FieldPersistenceModifier.NONE) {
            return;
        }
        MethodGen methodGen = new MethodGen(m, className, constantPoolGen);
        methodGen.setName(newMethodName);
        methodGen.setMaxLocals();
        methodGen.setMaxStack();
        newClass.addMethod(methodGen.getMethod());
    }

    protected void enhanceOriginalMethods() {
        Method methods[] = newClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if ("jdoPreClear".equals(methods[i].getName()) || "jdoPostLoad".equals(methods[i].getName())) {
            } else if ("readObject".equals(methods[i].getName()) && (methods[i].getSignature().equals("(Ljava/io/ObjectOutputStream;)V") || methods[i].getSignature().equals("(Ljava/io/ObjectInputStream;)V"))) {
            } else {
                enhanceOriginalMethod(methods[i]);
                enhancePropertySetter(methods[i]);
                enhancePropertyGetter(methods[i]);
                enhancePropertyAccessor(methods[i]);
            }
        }
    }

    /**
     * Method to take the method and create a valid setXXX when the method is a setter for a persistent property.
     * @param m The method
     */
    protected void enhancePropertySetter(Method m) {
        String name = ClassUtils.getFieldNameForJavaBeanSetter(m.getName());
        AbstractMemberMetaData apmd = cmd.getMetaDataForMember(name);
        if (apmd != null && apmd.getPersistenceModifier() != FieldPersistenceModifier.NONE && apmd instanceof PropertyMetaData) {
            if (((apmd.getJdoFieldFlag() & PersistenceCapable.MEDIATE_WRITE) == PersistenceCapable.MEDIATE_WRITE) || ((apmd.getJdoFieldFlag() & PersistenceCapable.CHECK_WRITE) == PersistenceCapable.CHECK_WRITE)) {
                PropertySetterMethod setter = new PropertySetterMethod(m, this.className, constantPoolGen, newClass, m.getArgumentTypes(), (BCELFieldPropertyMetaData) apmd, this);
                setter.execute();
            }
        }
    }

    /**
     * Method to take the method and create a valid getXXX when the method is a getter for a persistent property.
     * @param m The method
     */
    protected void enhancePropertyGetter(Method m) {
        String name = ClassUtils.getFieldNameForJavaBeanGetter(m.getName());
        AbstractMemberMetaData apmd = cmd.getMetaDataForMember(name);
        if (apmd != null && apmd.getPersistenceModifier() != FieldPersistenceModifier.NONE && apmd instanceof PropertyMetaData) {
            if (((apmd.getJdoFieldFlag() & PersistenceCapable.MEDIATE_READ) == PersistenceCapable.MEDIATE_READ) || ((apmd.getJdoFieldFlag() & PersistenceCapable.CHECK_READ) == PersistenceCapable.CHECK_READ)) {
                PropertyGetterMethod getter = new PropertyGetterMethod(m, this.className, constantPoolGen, newClass, m.getArgumentTypes(), (BCELFieldPropertyMetaData) apmd, this);
                getter.execute();
            }
        }
    }

    /**
     * check if the getfield/setfield is suitable to be enhanced. The enhancer will change the code from 
     * something like: "fieldA = fieldB" to "jdoSetfieldA(fieldB)" or "jdoSetfieldA(jdoGetfieldB())"
     * "fieldA = fieldB" to "jdoSetfieldA(fieldB)" or "jdoSetfieldA(jdoGetfieldB())"
     * @param ih the getfield or setfield instruction
     * @param cp the constant pool
     * @return
     */
    private String getClassNameForFieldAccess(InstructionHandle ih, ConstantPool cp) {
        Constant c = cp.getConstant(((CPInstruction) ih.getInstruction()).getIndex());
        if (c instanceof ConstantFieldref) {
            ConstantFieldref fieldRef = (ConstantFieldref) c;
            ConstantClass cclass = (ConstantClass) cp.getConstant(fieldRef.getClassIndex());
            ConstantUtf8 className = (ConstantUtf8) cp.getConstant(cclass.getNameIndex());
            return StringUtils.replaceAll(className.getBytes().toString(), "/", ".");
        }
        return null;
    }

    /**
     * Check whether the field access in the instruction argument ih is in a PersistenceCapable class
     * @param ih the getfield or setfield instruction
     * @param cp the constant pool
     * @return true if the access is in a PersistenceCapable class
     */
    private boolean isFieldAccessInPersistenceCapableClass(InstructionHandle ih, ConstantPool cp) {
        String className = getClassNameForFieldAccess(ih, cp);
        if (className == null) {
            return false;
        }
        return isPersistenceCapable(className);
    }

    /**
     * This method adds to the generated class the jdoSet methods
     * @param fieldConfig
     */
    protected void enhanceSetter(BCELFieldPropertyMetaData fieldConfig) {
        BCELMember f = fieldConfig.getEnhanceField();
        String methodName = BCELUtils.getSetterName(f.getName());
        if (f.isMethod()) {
            methodName = f.getName();
        }
        BCELClassMethod callback = null;
        byte jdoFlag = fieldConfig.getJdoFieldFlag();
        if (f.isFinal() || f.isStatic()) {
            return;
        }
        if (FieldPersistenceModifier.NONE.equals(fieldConfig.getPersistenceModifier())) {
            return;
        }
        if (((AbstractMemberMetaData) fieldConfig) instanceof PropertyMetaData) {
            return;
        }
        if ((jdoFlag & PersistenceCapable.MEDIATE_WRITE) == PersistenceCapable.MEDIATE_WRITE) {
            callback = new MediateWriteMethod("jdo" + methodName, (f.isPublic() ? Constants.ACC_PUBLIC : 0) | (f.isProtected() ? Constants.ACC_PROTECTED : 0) | (f.isPrivate() ? Constants.ACC_PRIVATE : 0) | Constants.ACC_STATIC, Type.VOID, new Type[] { this.classType, f.getType() }, new String[] { "objPC", f.getName() + "_m" }, true, this, fieldConfig);
        } else if ((jdoFlag & PersistenceCapable.CHECK_WRITE) == PersistenceCapable.CHECK_WRITE) {
            callback = new CheckWriteMethod("jdo" + methodName, (f.isPublic() ? Constants.ACC_PUBLIC : 0) | (f.isProtected() ? Constants.ACC_PROTECTED : 0) | (f.isPrivate() ? Constants.ACC_PRIVATE : 0) | Constants.ACC_STATIC, Type.VOID, new Type[] { this.classType, f.getType() }, new String[] { "objPC", f.getName() + "_c" }, true, this, fieldConfig);
        } else {
            callback = new NormalSetMethod("jdo" + methodName, (f.isPublic() ? Constants.ACC_PUBLIC : 0) | (f.isProtected() ? Constants.ACC_PROTECTED : 0) | (f.isPrivate() ? Constants.ACC_PRIVATE : 0) | Constants.ACC_STATIC, Type.VOID, new Type[] { this.classType, f.getType() }, new String[] { "objPC", f.getName() + "_n" }, true, this, fieldConfig);
        }
        if (callback != null) {
            methodsToAdd.add(callback);
        }
    }

    /**
     * This method adds to the generated class the jdoGet methods
     * @param fieldConfig
     */
    protected void enhanceGetter(BCELFieldPropertyMetaData fieldConfig) {
        BCELMember f = fieldConfig.getEnhanceField();
        String methodName = BCELUtils.getGetterName(f.getName());
        if (f.isMethod()) {
            methodName = f.getName();
        }
        BCELClassMethod callback = null;
        byte jdoFlag = fieldConfig.getJdoFieldFlag();
        if (f.isFinal() || f.isStatic()) {
            return;
        }
        if (FieldPersistenceModifier.NONE.equals(fieldConfig.getPersistenceModifier())) {
            return;
        }
        if (((AbstractMemberMetaData) fieldConfig) instanceof PropertyMetaData) {
            return;
        }
        if ((jdoFlag & PersistenceCapable.MEDIATE_READ) == PersistenceCapable.MEDIATE_READ) {
            callback = new MediateReadMethod("jdo" + methodName, (f.isPublic() ? Constants.ACC_PUBLIC : 0) | (f.isProtected() ? Constants.ACC_PROTECTED : 0) | (f.isPrivate() ? Constants.ACC_PRIVATE : 0) | Constants.ACC_STATIC, f.getType(), new Type[] { this.classType }, new String[] { "objPC" }, true, this, fieldConfig);
        } else if ((jdoFlag & PersistenceCapable.CHECK_READ) == PersistenceCapable.CHECK_READ) {
            callback = new CheckReadMethod("jdo" + methodName, (f.isPublic() ? Constants.ACC_PUBLIC : 0) | (f.isProtected() ? Constants.ACC_PROTECTED : 0) | (f.isPrivate() ? Constants.ACC_PRIVATE : 0) | Constants.ACC_STATIC, f.getType(), new Type[] { this.classType }, new String[] { "objPC" }, true, this, fieldConfig);
        } else {
            callback = new NormalGetMethod("jdo" + methodName, (f.isPublic() ? Constants.ACC_PUBLIC : 0) | (f.isProtected() ? Constants.ACC_PROTECTED : 0) | (f.isPrivate() ? Constants.ACC_PRIVATE : 0) | Constants.ACC_STATIC, f.getType(), new Type[] { this.classType }, new String[] { "objPC" }, true, this, fieldConfig);
        }
        methodsToAdd.add(callback);
    }

    /**
     * Access the class in byte array format
     * @return the class in byte array format
     */
    public byte[] getBytes() {
        return newClass.getJavaClass().getBytes();
    }

    /**
     * Compute the serialVersionUID
     * @author unknown
     */
    public class SerialVersionUID {

        private final Comparator FIELD_OR_METHOD_COMPARATOR = new FieldOrMethodComparator();

        /**
         * This method computes the serialVersionUID of a BCEL JavaClass in the same way that the
         * java.io.ObjectStreamClass class computes it for a java.lang.Class.
         * <p>
         * This method is a port of version 1.98 of the ObjectStreamClass's computeDefaultSUID method.
         * <p>
         * Compute a hash for the specified class. Incrementally add items to the hash accumulating in the digest
         * stream. Fold the hash into a long. Use the SHA secure hash function.
         * @param javaClass the class to compute the serialversionUID from
         * @return the serial version uid
         * @throws InternalError
         * @throws SecurityException
         */
        public long computeSerialVersionUID(JavaClass javaClass) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                Method[] methods = javaClass.getMethods();
                Field[] fields = javaClass.getFields();
                dout.writeUTF(javaClass.getClassName());
                int classMods = javaClass.getAccessFlags();
                classMods &= (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);
                if ((classMods & Modifier.INTERFACE) != 0) {
                    classMods = (methods.length > 0) ? (classMods | Modifier.ABSTRACT) : (classMods & ~Modifier.ABSTRACT);
                }
                dout.writeInt(classMods);
                if (true) {
                    String[] ifaceNames = javaClass.getInterfaceNames();
                    Arrays.sort(ifaceNames);
                    for (int i = 0; i < ifaceNames.length; i++) {
                        dout.writeUTF(ifaceNames[i]);
                    }
                }
                Arrays.sort(fields, FIELD_OR_METHOD_COMPARATOR);
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    int mods = fields[i].getAccessFlags();
                    if (((mods & Modifier.PRIVATE) == 0) || ((mods & (Modifier.STATIC | Modifier.TRANSIENT)) == 0)) {
                        dout.writeUTF(field.getName());
                        dout.writeInt(mods);
                        dout.writeUTF(field.getSignature());
                    }
                }
                Arrays.sort(methods, FIELD_OR_METHOD_COMPARATOR);
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    int mods = method.getAccessFlags();
                    if ((mods & Modifier.PRIVATE) == 0) {
                        dout.writeUTF(method.getName());
                        dout.writeInt(mods);
                        dout.writeUTF(method.getSignature().replace('/', '.'));
                    }
                }
                dout.flush();
                MessageDigest md = MessageDigest.getInstance("SHA");
                byte[] hashBytes = md.digest(bout.toByteArray());
                long hash = 0;
                for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                    hash = (hash << 8) | (hashBytes[i] & 0xFF);
                }
                return hash;
            } catch (IOException ex) {
                throw new InternalError();
            } catch (NoSuchAlgorithmException ex) {
                throw new SecurityException(ex.getMessage());
            }
        }

        private class FieldOrMethodComparator implements Comparator {

            public int compare(Object o1, Object o2) {
                FieldOrMethod fom1 = (FieldOrMethod) o1;
                FieldOrMethod fom2 = (FieldOrMethod) o2;
                int comp = fom1.getName().compareTo(fom2.getName());
                if (comp == 0) {
                    comp = fom1.getSignature().compareTo(fom2.getSignature());
                }
                return comp;
            }
        }
    }
}
