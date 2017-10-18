package org.enerj.enhancer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.enerj.annotations.SchemaAnnotation;
import org.enerj.core.ObjectSerializer;
import org.enerj.core.Persistable;
import org.enerj.core.PersistableHelper;
import org.enerj.core.PersistentAware;
import org.enerj.core.Persister;
import org.enerj.core.SystemCIDMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Class file enhancer/ASM vistor. An instance enhances a single class.
 * <p>
 * This can throw EnhancerException (runtime exception) from any of the ASM visitor methods.  
 *
 * @version $Id: ClassEnhancer.java,v 1.12 2006/06/06 22:41:27 dsyrstad Exp $
 * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad</a>
 */
class ClassEnhancer extends ClassAdapter implements Opcodes {

    private static final String FIELD_ACCESSOR_PREFIX = "enerj_Get_";

    private static final String FIELD_MUTATOR_PREFIX = "enerj_Set_";

    private static final String sPostLoadMethodName = "enerjPostLoad";

    private static final String sPreStoreMethodName = "enerjPreStore";

    private static final String sPostStoreMethodName = "enerjPostStore";

    private static final String sPreHollowMethodName = "enerjPreHollow";

    private static final String sClassIdFieldName = "enerj_sClassId";

    private static final String sPersisterDescr = Type.getDescriptor(Persister.class);

    private static final String sNoArgMethodSignature = "()V";

    private static final String sSchemaAnnotationDescr = Type.getDescriptor(SchemaAnnotation.class);

    private static final String sPersistableClassSlashed = Type.getInternalName(Persistable.class);

    private static final String sPersistableClassDescr = Type.getDescriptor(Persistable.class);

    private static final String sPersistableVoidSignature = '(' + sPersistableClassDescr + ")V";

    private static final String sPersistentAwareClassSlashed = Type.getInternalName(PersistentAware.class);

    private static final String sPersistableHelperClassSlashed = Type.getInternalName(PersistableHelper.class);

    private static final String sObjectSerializerClassNameSlashed = Type.getInternalName(ObjectSerializer.class);

    private static final String sObjectSerializerClassDescr = Type.getDescriptor(ObjectSerializer.class);

    private static final String sDataInputClassNameSlashed = Type.getInternalName(DataInput.class);

    private static final String sDataOutputClassNameSlashed = Type.getInternalName(DataOutput.class);

    private static final String sObjectInputStreamClassNameSlashed = Type.getInternalName(ObjectInputStream.class);

    private static final String sSerializableClassNameSlashed = Type.getInternalName(Serializable.class);

    private static final String sPersisterConstructorSignature = '(' + Type.getDescriptor(Persister.class) + ")V";

    private static final String sReadWriteObjectMethodSignature = '(' + sObjectSerializerClassDescr + ")V";

    private static final String sResolveObjectMethodSignature = '(' + sObjectSerializerClassDescr + "Z)V";

    private byte[] mOriginalClassBytes;

    private MetaData mMetaData;

    private String mClassName;

    private boolean mIsOnlyPersistentAware;

    private boolean mIsPersistable;

    private boolean mIsSerializable;

    private boolean mHasReadObjectMethod;

    private String mSuperClassName;

    private String mSuperClassNameSlashed;

    private boolean mIsTopLevelPersistable;

    private long mClassId;

    private String mThisClassDescr;

    private String mThisClassNameSlashed;

    private boolean mHasNoArgConstructor = false;

    private boolean mHasPostLoad = false;

    private boolean mHasPreStore = false;

    private boolean mHasPostStore = false;

    private boolean mHasPreHollow = false;

    private boolean mEnhancedClone = false;

    private boolean mEnhancedClinit = false;

    private ArrayList<Field> mPersistentFields = new ArrayList<Field>();

    private ArrayList<Field> mTransientFields = new ArrayList<Field>();

    private Map<String, Field> mDeclaredFields = new HashMap<String, Field>();

    /**
     * Construct a ClassEnhancer. 
     *
     * @param aClassVisitor the ClassVisitor that is the delegate (usually a ClassWriter).
     * @param aClassName the fully qualified dotted class name.
     * @param anOriginalClassFile the original class file bytes before enhancement.
     * @param aMetaData the enhancer MetaData.
     */
    ClassEnhancer(ClassVisitor aClassVisitor, String aClassName, byte[] anOriginalClassFile, MetaData aMetaData) {
        super(aClassVisitor);
        mOriginalClassBytes = anOriginalClassFile;
        mMetaData = aMetaData;
        mClassName = aClassName;
        mThisClassNameSlashed = aClassName.replace('.', '/');
        mThisClassDescr = 'L' + mThisClassNameSlashed + ';';
    }

    /** 
     * {@inheritDoc}
     * Enhances at the class level. 
     * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int aVersion, int someAccessModifiers, String aName, String aSignature, String aSuperClassName, String[] someInterfaces) {
        try {
            for (int i = 0; someInterfaces != null && i < someInterfaces.length; i++) {
                String interfaceName = someInterfaces[i];
                if (interfaceName.equals(sPersistableClassSlashed) || interfaceName.equals(sPersistentAwareClassSlashed)) {
                    throw new AlreadyEnhancedException("Class " + mClassName + " has already been enhanced - skipping.", null);
                }
                if (interfaceName.equals(sSerializableClassNameSlashed)) {
                    mIsSerializable = true;
                }
            }
            if ((someAccessModifiers & ACC_INTERFACE) == ACC_INTERFACE) {
                throw new SkipEnhancementException();
            }
            mIsOnlyPersistentAware = mMetaData.isClassOnlyPersistentAware(mClassName);
            mIsPersistable = !mIsOnlyPersistentAware;
            mSuperClassNameSlashed = aSuperClassName;
            mSuperClassName = mSuperClassNameSlashed.replace('/', '.');
            mIsTopLevelPersistable = mIsPersistable && !mMetaData.isClassAFCO(mSuperClassName);
            mClassId = SystemCIDMap.getSystemCIDForClassName(mClassName);
            if (mClassId == ObjectSerializer.NULL_CID) {
                mClassId = generateClassId(mOriginalClassBytes);
            }
            String interfaceName = null;
            if (mIsPersistable) {
                interfaceName = sPersistableClassSlashed;
            } else if (mIsOnlyPersistentAware) {
                interfaceName = sPersistentAwareClassSlashed;
            }
            if (interfaceName != null) {
                String[] newInterfaces = new String[someInterfaces.length + 1];
                System.arraycopy(someInterfaces, 0, newInterfaces, 0, someInterfaces.length);
                newInterfaces[someInterfaces.length] = interfaceName;
                someInterfaces = newInterfaces;
            }
            cv.visit(aVersion, someAccessModifiers, aName, aSignature, aSuperClassName, someInterfaces);
        } catch (MetaDataException e) {
            throw new EnhancerException("Error processing class " + aName, e);
        }
    }

    /** 
     * {@inheritDoc}
     * Enhances methods.
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int someAccessModifiers, String aMethodName, String aDescriptor, String aSignature, String[] someExceptions) {
        MethodVisitor mv = cv.visitMethod(someAccessModifiers, aMethodName, aDescriptor, aSignature, someExceptions);
        if (aDescriptor.equals(sNoArgMethodSignature)) {
            if (aMethodName.equals(sPostLoadMethodName)) {
                mHasPostLoad = true;
            } else if (aMethodName.equals(sPreStoreMethodName)) {
                mHasPreStore = true;
            } else if (aMethodName.equals(sPostStoreMethodName)) {
                mHasPostStore = true;
            } else if (aMethodName.equals(sPreHollowMethodName)) {
                mHasPreHollow = true;
            }
        }
        boolean enhanceConstructor = false;
        boolean enhanceClone = false;
        boolean enhanceExternalOnly = false;
        boolean enhanceReadObject = false;
        if (aMethodName.equals("<init>") && mIsPersistable) {
            enhanceExternalOnly = true;
            if (mIsTopLevelPersistable) {
                enhanceConstructor = true;
            }
            if (aDescriptor.equals(sNoArgMethodSignature)) {
                mHasNoArgConstructor = true;
            }
        } else if (aMethodName.equals("clone") && aDescriptor.equals("()Ljava/lang/Object;")) {
            enhanceExternalOnly = true;
            if (mIsTopLevelPersistable) {
                enhanceClone = true;
                mEnhancedClone = true;
            }
        } else if (mIsTopLevelPersistable && aMethodName.equals("readObject") && aDescriptor.equals("(Ljava/io/ObjectInputStream;)V")) {
            mHasReadObjectMethod = true;
            enhanceReadObject = true;
        }
        if (!aMethodName.startsWith("enerj_") && !Modifier.isAbstract(someAccessModifiers) && !Modifier.isNative(someAccessModifiers)) {
            mv = new MethodEnhancer(mv, aMethodName, enhanceExternalOnly, enhanceConstructor, enhanceClone, enhanceReadObject);
        }
        return mv;
    }

    /** 
     * {@inheritDoc}
     * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    public FieldVisitor visitField(int someAccessModifiers, String aName, String aDescriptor, String aSignature, Object aValue) {
        try {
            Field field = new Field(aName, aDescriptor, someAccessModifiers);
            mDeclaredFields.put(aName, field);
            if (mIsPersistable) {
                if (mMetaData.isFieldPersistent(mClassName, aName, someAccessModifiers)) {
                    mPersistentFields.add(field);
                    emitPersistentFieldMediationMethods(field);
                } else {
                    mTransientFields.add(field);
                    if ((someAccessModifiers & ACC_STATIC) == ACC_STATIC) {
                        emitStaticFieldMediationMethods(field);
                    } else {
                        emitTransientFieldMediationMethods(field);
                    }
                }
            }
            return cv.visitField(someAccessModifiers, aName, aDescriptor, aSignature, aValue);
        } catch (MetaDataException e) {
            throw new EnhancerException("Error processing class " + mClassName, e);
        }
    }

    /** 
     * {@inheritDoc}
     * @see org.objectweb.asm.ClassAdapter#visitEnd()
     */
    public void visitEnd() {
        if (mIsTopLevelPersistable) {
            enhanceTopLevelClass();
        }
        if (mIsPersistable) {
            try {
                mMetaData.validateFieldOverrides(mClassName, mDeclaredFields);
            } catch (MetaDataException e) {
                throw new EnhancerException("Error processing class " + mClassName, e);
            }
        }
        if (mIsTopLevelPersistable && !mEnhancedClone) {
            emitClone();
        }
        if (mIsTopLevelPersistable && mIsSerializable && !mHasReadObjectMethod) {
            emitReadObject();
        }
        if (mIsPersistable) {
            AnnotationVisitor av = cv.visitAnnotation(sSchemaAnnotationDescr, true);
            av.visit("originalByteCodes", mOriginalClassBytes);
            av.visit("classID", Long.valueOf(mClassId));
            AnnotationVisitor av1 = av.visitArray("persistentFieldNames");
            List<Field> somePersistentFields = getPersistentFields();
            for (int i = 0; i < somePersistentFields.size(); i++) {
                Field field = (Field) somePersistentFields.get(i);
                av1.visit(null, field.getName());
            }
            av1.visitEnd();
            av1 = av.visitArray("transientFieldNames");
            List<Field> someTransientFields = getTransientFields();
            for (int i = 0; i < someTransientFields.size(); i++) {
                Field field = (Field) someTransientFields.get(i);
                av1.visit(null, field.getName());
            }
            av1.visitEnd();
            av.visitEnd();
            emitSpecialConstructor();
            emitReadObject(mPersistentFields);
            emitWriteObject(mPersistentFields);
            emitResolveObject(mPersistentFields);
            emitHollow(mPersistentFields);
            emitClassId();
            if (!mEnhancedClinit) {
                emitClassInit();
            }
        }
        cv.visitEnd();
    }

    /**
     * Generate class Id.<p>
     *
     * @param someBytesCodes the unenhanced bytecodes of the class.
     *
     * @return a class Id that does not conflict with the system class Id range
     *  of [ObjectServer.NULL_CID..ObjectServer.LAST_SYSTEM_CID).
     *
     * @throws EnhancerException if an error occurs (e.g., java.security.NoSuchAlgorithmException).
     */
    private static long generateClassId(byte[] someByteCodes) throws EnhancerException {
        try {
            java.security.MessageDigest sha1Digest = java.security.MessageDigest.getInstance("SHA-1");
            byte[] sha1 = sha1Digest.digest(someByteCodes);
            long cid = (long) (sha1[0] & 0xff) | ((long) (sha1[1] & 0xff) << 8) | ((long) (sha1[2] & 0xff) << 16) | ((long) (sha1[3] & 0xff) << 24) | ((long) (sha1[4] & 0xff) << 32) | ((long) (sha1[5] & 0xff) << 40) | ((long) (sha1[6] & 0xff) << 48) | ((long) (sha1[7] & 0xff) << 56);
            if (cid >= ObjectSerializer.NULL_CID && cid <= ObjectSerializer.LAST_SYSTEM_CID) {
                cid += ObjectSerializer.LAST_SYSTEM_CID;
            }
            return cid;
        } catch (NoSuchAlgorithmException e) {
            throw new EnhancerException("Cannot create class ID", e);
        }
    }

    /**
     * Modify a top-level Persistable class to add the core "enerj_" fields and methods.
     * This generally implements the Persistable interface.
     */
    private void enhanceTopLevelClass() {
        String boolDescr = Type.BOOLEAN_TYPE.getDescriptor();
        String longDescr = Type.LONG_TYPE.getDescriptor();
        String intDescr = Type.INT_TYPE.getDescriptor();
        cv.visitField(ACC_PROTECTED | ACC_TRANSIENT, "enerj_mModified", boolDescr, null, null);
        cv.visitField(ACC_PROTECTED | ACC_TRANSIENT, "enerj_mNew", boolDescr, null, null);
        cv.visitField(ACC_PROTECTED | ACC_TRANSIENT, "enerj_mLoaded", boolDescr, null, null);
        cv.visitField(ACC_PROTECTED | ACC_TRANSIENT, "enerj_mAllowNonTransactionalReads", boolDescr, null, null);
        cv.visitField(ACC_PROTECTED | ACC_TRANSIENT, "enerj_mAllowNonTransactionalWrites", boolDescr, null, null);
        cv.visitField(ACC_PRIVATE | ACC_TRANSIENT, "enerj_mVersion", longDescr, null, null);
        cv.visitField(ACC_PRIVATE | ACC_TRANSIENT, "enerj_mOID", longDescr, null, null);
        cv.visitField(ACC_PRIVATE | ACC_TRANSIENT, "enerj_mPersister", sPersisterDescr, null, null);
        cv.visitField(ACC_PRIVATE | ACC_TRANSIENT, "enerj_mLockLevel", intDescr, null, null);
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_IsModified", "enerj_mModified");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_IsNew", "enerj_mNew");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_IsLoaded", "enerj_mLoaded");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_AllowsNonTransactionalRead", "enerj_mAllowNonTransactionalReads");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_AllowsNonTransactionalWrite", "enerj_mAllowNonTransactionalWrites");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, longDescr, "enerj_GetVersion", "enerj_mVersion");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, longDescr, "enerj_GetPrivateOID", "enerj_mOID");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, sPersisterDescr, "enerj_GetPersister", "enerj_mPersister");
        emitAccessorMethod(ACC_PUBLIC | ACC_FINAL, intDescr, "enerj_GetLockLevel", "enerj_mLockLevel");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_SetModified", "enerj_mModified");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_SetNew", "enerj_mNew");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_SetLoaded", "enerj_mLoaded");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_SetAllowNonTransactionalRead", "enerj_mAllowNonTransactionalReads");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, boolDescr, "enerj_SetAllowNonTransactionalWrite", "enerj_mAllowNonTransactionalWrites");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, longDescr, "enerj_SetVersion", "enerj_mVersion");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, longDescr, "enerj_SetPrivateOID", "enerj_mOID");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, sPersisterDescr, "enerj_SetPersister", "enerj_mPersister");
        emitMutatorMethod(ACC_PUBLIC | ACC_FINAL, intDescr, "enerj_SetLockLevel", "enerj_mLockLevel");
    }

    /**
     * Emits a readObject() method on a top-level persistable that implements
     * Serializable.
     */
    private void emitReadObject() {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "readObject", "(Ljava/io/ObjectInputStream;)V", null, new String[] { "java/io/IOException", "java/lang/ClassNotFoundException" });
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "initPersistable", sPersistableVoidSignature);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, sObjectInputStreamClassNameSlashed, "defaultReadObject", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emit a simple, generic accessor method.
     */
    private void emitAccessorMethod(int someAccessFlags, String aDescriptor, String aMethodName, String aFieldName) {
        MethodVisitor mv = cv.visitMethod(someAccessFlags, aMethodName, "()" + aDescriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, aFieldName, aDescriptor);
        mv.visitInsn(getReturnOpcodeForDescriptor(aDescriptor));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emit a simple, generic accessor method.
     */
    private void emitMutatorMethod(int someAccessFlags, String aDescriptor, String aMethodName, String aFieldName) {
        MethodVisitor mv = cv.visitMethod(someAccessFlags, aMethodName, '(' + aDescriptor + ")V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(getLoadOpcodeForDescriptor(aDescriptor), 1);
        mv.visitFieldInsn(PUTFIELD, mThisClassNameSlashed, aFieldName, aDescriptor);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Gets the opcode offset for the specified descriptor.
     *
     * @param aDescriptor a descriptor.
     * 
     * @return the opcode as defined in 
     */
    private static int getOpcodeOffsetForDescriptor(String aDescriptor) {
        char first = aDescriptor.charAt(0);
        switch(first) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
                return 0;
            case 'J':
                return 1;
            case 'F':
                return 2;
            case 'D':
                return 3;
            case '[':
            case 'L':
                return 4;
            default:
                throw new IllegalArgumentException("aDescriptor not recognized: " + aDescriptor);
        }
    }

    /**
     * Gets the xLOAD opcode for the specified descriptor.
     *
     * @param aDescriptor a descriptor.
     * 
     * @return the opcode as defined in 
     */
    private static int getLoadOpcodeForDescriptor(String aDescriptor) {
        return ILOAD + getOpcodeOffsetForDescriptor(aDescriptor);
    }

    /**
     * Gets the xRETURN opcode for the specified descriptor.
     *
     * @param aDescriptor a descriptor.
     * 
     * @return the opcode as defined in 
     */
    private static int getReturnOpcodeForDescriptor(String aDescriptor) {
        if (aDescriptor.charAt(0) == 'V') {
            return RETURN;
        }
        return IRETURN + getOpcodeOffsetForDescriptor(aDescriptor);
    }

    /**
     * Create the method name suffix used for a field getter or setter.
     *
     * @param aClassName the field's fully-qualified dotted class name.
     * @param aFieldName the field's name.
     *
     * @return the method name suffix.
     */
    private String getFieldMethodNameSuffix(String aClassName, String aFieldName) {
        return aClassName.replace('.', '_') + '_' + aFieldName;
    }

    /**
     * Generate the getfield/putfield replacement methods (enerj_Get_* and enerj_Set_*)
     * for persistent fields.
     * Parameters to the generated methods conveniently match the stack frame of
     * getfield and putfield.
     *
     * @param aField a Field for which the methods will be generated.
     */
    private void emitPersistentFieldMediationMethods(Field aField) {
        int fieldScope = aField.getAccessModifiers() & (ACC_PRIVATE | ACC_PUBLIC | ACC_PROTECTED);
        String fieldName = aField.getName();
        String methodNameSuffix = getFieldMethodNameSuffix(mClassName, fieldName);
        String fieldType = aField.getDescriptor();
        int opcodeOffset = getOpcodeOffsetForDescriptor(fieldType);
        MethodVisitor mv = cv.visitMethod(fieldScope | ACC_STATIC, FIELD_ACCESSOR_PREFIX + methodNameSuffix, '(' + mThisClassDescr + ')' + fieldType, null, null);
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, "enerj_mLoaded", "Z");
        Label label1 = new Label();
        mv.visitJumpInsn(IFNE, label1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, "enerj_mNew", "Z");
        mv.visitJumpInsn(IFNE, label1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "checkLoaded", "(Lorg/enerj/core/Persistable;Z)V");
        mv.visitLabel(label1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
        mv.visitInsn(getReturnOpcodeForDescriptor(fieldType));
        Label label3 = new Label();
        mv.visitLabel(label3);
        mv.visitLocalVariable("anInstance", mThisClassDescr, null, label0, label3, 0);
        mv.visitMaxs(0, 0);
        mv = cv.visitMethod(fieldScope | ACC_STATIC, FIELD_MUTATOR_PREFIX + methodNameSuffix, '(' + mThisClassDescr + fieldType + ")V", null, null);
        mv.visitCode();
        Label mutatorStartLabel = new Label();
        mv.visitLabel(mutatorStartLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, "enerj_mLoaded", "Z");
        Label mutatorLabel1 = new Label();
        mv.visitJumpInsn(IFNE, mutatorLabel1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, "enerj_mNew", "Z");
        mv.visitJumpInsn(IFNE, mutatorLabel1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "checkLoaded", "(Lorg/enerj/core/Persistable;Z)V");
        mv.visitLabel(mutatorLabel1);
        Label notModifiedLabel = new Label();
        if (MetaData.isPrimitive(fieldType)) {
            mv.visitVarInsn(ILOAD + opcodeOffset, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
            char type = fieldType.charAt(0);
            switch(type) {
                case 'B':
                case 'Z':
                case 'C':
                case 'S':
                case 'I':
                    mv.visitJumpInsn(IF_ICMPEQ, notModifiedLabel);
                    break;
                case 'F':
                    mv.visitInsn(FCMPL);
                    mv.visitJumpInsn(IFEQ, notModifiedLabel);
                    break;
                case 'J':
                    mv.visitInsn(LCMP);
                    mv.visitJumpInsn(IFEQ, notModifiedLabel);
                    break;
                case 'D':
                    mv.visitInsn(DCMPL);
                    mv.visitJumpInsn(IFEQ, notModifiedLabel);
                    break;
                default:
                    throw new RuntimeException("Unknown primitive type: " + type);
            }
        } else if (fieldType.equals("Ljava/lang/String;") || fieldType.equals("Ljava/lang/Integer;") || fieldType.equals("Ljava/lang/Long;") || fieldType.equals("Ljava/lang/Byte;") || fieldType.equals("Ljava/lang/Boolean;") || fieldType.equals("Ljava/lang/Character;") || fieldType.equals("Ljava/lang/Short;") || fieldType.equals("Ljava/lang/Float;") || fieldType.equals("Ljava/lang/Double;")) {
            mv.visitVarInsn(ALOAD, 1);
            Label imEqualsLabel = new Label();
            mv.visitJumpInsn(IFNONNULL, imEqualsLabel);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
            Label imStoreLabel = new Label();
            mv.visitJumpInsn(IFNONNULL, imStoreLabel);
            mv.visitLabel(imEqualsLabel);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitJumpInsn(IFNULL, notModifiedLabel);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
            mv.visitMethodInsn(INVOKEVIRTUAL, aField.getInternalName(), "equals", "(Ljava/lang/Object;)Z");
            mv.visitJumpInsn(IFNE, notModifiedLabel);
            mv.visitLabel(imStoreLabel);
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
            mv.visitJumpInsn(IF_ACMPEQ, notModifiedLabel);
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, "enerj_mModified", "Z");
        Label mutatorLabel2 = new Label();
        mv.visitJumpInsn(IFNE, mutatorLabel2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "addModified", sPersistableVoidSignature);
        mv.visitLabel(mutatorLabel2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD + opcodeOffset, 1);
        mv.visitFieldInsn(PUTFIELD, mThisClassNameSlashed, fieldName, fieldType);
        mv.visitLabel(notModifiedLabel);
        mv.visitInsn(RETURN);
        Label mutatorEndlabel = new Label();
        mv.visitLabel(mutatorEndlabel);
        mv.visitLocalVariable("anInstance", mThisClassDescr, null, mutatorStartLabel, mutatorEndlabel, 0);
        mv.visitLocalVariable("aValue", fieldType, null, mutatorStartLabel, mutatorEndlabel, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generate the getfield/putfield replacement methods (enerj_Get_* and enerj_Set_*)
     * for non-static transient fields.
     * Parameters to the generated methods conveniently match the stack frame of
     * getfield and putfield.
     *
     * @param aField a Field for which the methods will be generated.
     */
    private void emitTransientFieldMediationMethods(Field aField) {
        int fieldScope = aField.getAccessModifiers() & (ACC_PRIVATE | ACC_PUBLIC | ACC_PROTECTED);
        String fieldName = aField.getName();
        String methodNameSuffix = getFieldMethodNameSuffix(mClassName, fieldName);
        String fieldType = aField.getDescriptor();
        MethodVisitor mv = cv.visitMethod(fieldScope | ACC_STATIC, FIELD_ACCESSOR_PREFIX + methodNameSuffix, '(' + mThisClassDescr + ')' + fieldType, null, null);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
        mv.visitInsn(getReturnOpcodeForDescriptor(fieldType));
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("anInstance", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cv.visitMethod(fieldScope | ACC_STATIC, FIELD_MUTATOR_PREFIX + methodNameSuffix, '(' + mThisClassDescr + fieldType + ")V", null, null);
        mv.visitCode();
        startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(getLoadOpcodeForDescriptor(fieldType), 1);
        mv.visitFieldInsn(PUTFIELD, mThisClassNameSlashed, fieldName, fieldType);
        mv.visitInsn(RETURN);
        endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("anInstance", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitLocalVariable("aValue", fieldType, null, startLabel, endLabel, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generate the getstatic/putstatic replacement methods (enerj_Get_* and enerj_Set_*)
     * for static transient fields. 
     * Parameters to the generated methods conveniently match the stack frame of
     * getstatic and putstatic.
     *
     * @param aField a Field for which the methods will be generated.
     */
    private void emitStaticFieldMediationMethods(Field aField) {
        int fieldScope = aField.getAccessModifiers() & (ACC_PRIVATE | ACC_PUBLIC | ACC_PROTECTED);
        String fieldName = aField.getName();
        String methodNameSuffix = getFieldMethodNameSuffix(mClassName, fieldName);
        String fieldType = aField.getDescriptor();
        MethodVisitor mv = cv.visitMethod(fieldScope | ACC_STATIC, FIELD_ACCESSOR_PREFIX + methodNameSuffix, "()" + fieldType, null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, mThisClassNameSlashed, fieldName, fieldType);
        mv.visitInsn(getReturnOpcodeForDescriptor(fieldType));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cv.visitMethod(fieldScope | ACC_STATIC, FIELD_MUTATOR_PREFIX + methodNameSuffix, '(' + fieldType + ")V", null, null);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(getLoadOpcodeForDescriptor(fieldType), 0);
        mv.visitFieldInsn(PUTSTATIC, mThisClassNameSlashed, fieldName, fieldType);
        mv.visitInsn(RETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("aValue", fieldType, null, startLabel, endLabel, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emit the special clone() method on a top-level persistable. This is
     * generated when a top-level Persistable doesn't have a clone method.
     * It ensures initPersistableClone is called if a sub-class implements clone().
     */
    private void emitClone() {
        String[] exceptions = new String[] { "java/lang/CloneNotSupportedException" };
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "clone", "()Ljava/lang/Object;", null, exceptions);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, mSuperClassNameSlashed, "clone", "()Ljava/lang/Object;");
        mv.visitInsn(DUP);
        mv.visitTypeInsn(CHECKCAST, sPersistableClassSlashed);
        mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "initPersistableClone", sPersistableVoidSignature);
        mv.visitInsn(ARETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emits the enerj_ReadObject method.
     *
     * @param someFields a list of the persistent fields for this class (not including
     *  super-class fields).
     */
    private void emitReadObject(ArrayList<Field> someFields) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "enerj_ReadObject", sReadWriteObjectMethodSignature, null, new String[] { "java/io/IOException" });
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, sObjectSerializerClassNameSlashed, "getDataInput", "()Ljava/io/DataInput;");
        mv.visitVarInsn(ASTORE, 2);
        if (!mIsTopLevelPersistable) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, mSuperClassNameSlashed, "enerj_ReadObject", sReadWriteObjectMethodSignature);
        }
        for (Field field : someFields) {
            String fieldName = field.getName();
            String fieldType = field.getDescriptor();
            String dataInOutSuffix = MetaData.getPrimitiveDataInOutSuffix(fieldType);
            if (dataInOutSuffix != null) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEINTERFACE, sDataInputClassNameSlashed, "read" + dataInOutSuffix, "()" + fieldType);
                mv.visitFieldInsn(PUTFIELD, mThisClassNameSlashed, fieldName, fieldType);
            } else {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, sObjectSerializerClassNameSlashed, "readObject", "(" + sPersistableClassDescr + ")Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, field.getInternalName());
                mv.visitFieldInsn(PUTFIELD, mThisClassNameSlashed, fieldName, fieldType);
            }
        }
        if (mHasPostLoad) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, mThisClassNameSlashed, sPostLoadMethodName, sNoArgMethodSignature);
        }
        mv.visitInsn(RETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitLocalVariable("aContext", "Lorg/enerj/core/ObjectSerializer;", null, startLabel, endLabel, 1);
        mv.visitLocalVariable("stream", "Ljava/io/DataInput;", null, startLabel, endLabel, 2);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emits enerj_WriteObject.
     *
     * @param someFields a list of the persistent fields for this class (not including
     *  super-class fields).
     */
    private void emitWriteObject(ArrayList<Field> someFields) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "enerj_WriteObject", sReadWriteObjectMethodSignature, null, new String[] { "java/io/IOException" });
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, sObjectSerializerClassNameSlashed, "getDataOutput", "()Ljava/io/DataOutput;");
        mv.visitVarInsn(ASTORE, 2);
        if (mHasPreStore) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, mThisClassNameSlashed, sPreStoreMethodName, sNoArgMethodSignature);
        }
        if (!mIsTopLevelPersistable) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, mSuperClassNameSlashed, "enerj_WriteObject", sReadWriteObjectMethodSignature);
        }
        for (Field field : someFields) {
            String fieldName = field.getName();
            String fieldType = field.getDescriptor();
            String dataInOutSuffix = MetaData.getPrimitiveDataInOutSuffix(fieldType);
            if (dataInOutSuffix != null) {
                mv.visitVarInsn(ALOAD, 2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
                char sigChar = fieldType.charAt(0);
                if (sigChar == 'B' || sigChar == 'S' || sigChar == 'C') {
                    sigChar = 'I';
                }
                mv.visitMethodInsn(INVOKEINTERFACE, sDataOutputClassNameSlashed, "write" + dataInOutSuffix, "(" + sigChar + ")V");
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKEVIRTUAL, sObjectSerializerClassNameSlashed, "writeObject", "(Ljava/lang/Object;Lorg/enerj/core/Persistable;)V");
            }
        }
        if (mHasPostStore) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, mThisClassNameSlashed, sPostStoreMethodName, sNoArgMethodSignature);
        }
        mv.visitInsn(RETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitLocalVariable("aContext", sObjectSerializerClassDescr, null, startLabel, endLabel, 1);
        mv.visitLocalVariable("stream", "Ljava/io/DataOutput;", null, startLabel, endLabel, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    /**
     * Emits enerj_ResolveObject.
     *
     * @param someFields a list of the persistent fields for this class (not including
     *  super-class fields).
     */
    private void emitResolveObject(ArrayList<Field> someFields) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "enerj_ResolveObject", sResolveObjectMethodSignature, null, new String[] { "java/io/IOException" });
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        if (!mIsTopLevelPersistable) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, mSuperClassNameSlashed, "enerj_ResolveObject", sResolveObjectMethodSignature);
        }
        for (Field field : someFields) {
            String fieldName = field.getName();
            String fieldType = field.getDescriptor();
            String dataInOutSuffix = MetaData.getPrimitiveDataInOutSuffix(fieldType);
            if (dataInOutSuffix == null) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, mThisClassNameSlashed, fieldName, fieldType);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, sObjectSerializerClassNameSlashed, "resolveObject", "(Ljava/lang/Object;Z)V");
            }
        }
        mv.visitInsn(RETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitLocalVariable("aContext", sObjectSerializerClassDescr, null, startLabel, endLabel, 1);
        mv.visitLocalVariable("shouldDisassociate", "Z", null, startLabel, endLabel, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    /**
     * Generate the enerj_Hollow method.
     *
     * @param someFields a list of the persistent fields for this class (not including
     *  super-class fields).
     *
     * @throws EnhancerException in the event of an error.
     */
    private void emitHollow(ArrayList<Field> someFields) throws EnhancerException {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "enerj_Hollow", sNoArgMethodSignature, null, null);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        if (mHasPreHollow) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, mThisClassNameSlashed, "enerjPreHollow", sNoArgMethodSignature);
        }
        if (!mIsTopLevelPersistable) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, mSuperClassNameSlashed, "enerj_Hollow", sNoArgMethodSignature);
        }
        for (Field field : someFields) {
            String fieldType = field.getDescriptor();
            if (!MetaData.isPrimitive(fieldType)) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(ACONST_NULL);
                mv.visitFieldInsn(PUTFIELD, mThisClassNameSlashed, field.getName(), fieldType);
            }
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "completeHollow", sPersistableVoidSignature);
        mv.visitInsn(RETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emit the special constructor. "&lt;init>(Persister)".
     */
    private void emitSpecialConstructor() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", sPersisterConstructorSignature, null, null);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitVarInsn(ALOAD, 0);
        String constructorClass = mSuperClassNameSlashed;
        String signature;
        if (mIsTopLevelPersistable) {
            signature = sNoArgMethodSignature;
            if (mHasNoArgConstructor) {
                constructorClass = mThisClassNameSlashed;
            }
        } else {
            mv.visitVarInsn(ALOAD, 1);
            signature = sPersisterConstructorSignature;
        }
        mv.visitMethodInsn(INVOKESPECIAL, constructorClass, "<init>", signature);
        mv.visitInsn(RETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emits the class Id static variable and a static and non-static getters for it.
     */
    private void emitClassId() {
        FieldVisitor fv = cv.visitField(ACC_PRIVATE + ACC_STATIC + ACC_TRANSIENT, sClassIdFieldName, "J", null, null);
        fv.visitEnd();
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "enerj_GetClassIdStatic", "()J", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, mThisClassNameSlashed, sClassIdFieldName, "J");
        mv.visitInsn(LRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cv.visitMethod(ACC_PUBLIC, "enerj_GetClassId", "()J", null, null);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);
        mv.visitFieldInsn(GETSTATIC, mThisClassNameSlashed, sClassIdFieldName, "J");
        mv.visitInsn(LRETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", mThisClassDescr, null, startLabel, endLabel, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Emits the &lt;clinit> method when one does not already exist.
     * Initializes the value of the class Id.
     */
    private void emitClassInit() {
        MethodVisitor mv = cv.visitMethod(ACC_STATIC, "<clinit>", sNoArgMethodSignature, null, null);
        mv.visitCode();
        mv.visitLdcInsn((Long) mClassId);
        mv.visitFieldInsn(PUTSTATIC, mThisClassNameSlashed, sClassIdFieldName, "J");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Answers if this class is a Persistable.
     *
     * @return true if it is a Persistable.
     */
    boolean isPersistable() {
        return mIsPersistable;
    }

    /**
     * Gets the Persistent Fields.
     *
     * @return a ArrayList<Field>.
     */
    List<Field> getPersistentFields() {
        return mPersistentFields;
    }

    /**
     * Gets the Transient Fields.
     *
     * @return a ArrayList<Field>.
     */
    List<Field> getTransientFields() {
        return mTransientFields;
    }

    /**
     * Gets the ClassId.
     *
     * @return a class Id.
     */
    long getClassId() {
        return mClassId;
    }

    /**
     * Handles enhancement of existing methods.
     */
    private class MethodEnhancer extends MethodAdapter {

        private String mMethodName;

        /** 
         * If the method is <init> or clone on a Persistable, only enhance
         * access to Persistables external to this one. This eliminates unnecessary
         * modification marking and race conditions on initialization.
         */
        private boolean mShouldEnhanceExternalOnly;

        private boolean mShouldEnhanceConstructor;

        private boolean mShouldEnhanceClone;

        private boolean mShouldEnhanceReadObject;

        private boolean mIsClinit;

        /**
         * Constructs a MethodEnhancer.
         * 
         * @param aMethodVisitor the delegate method visitor.
         * @param enhanceExternalOnly true if only field access external to this class should be replaced.  
         * @param isClone true if this method is a clone()Ljava/lang/Object; method.
         * @param shouldEnhanceConstructor true if the method being visited is a constructor and 
         *  constructor enhancement should be performed.
         * @param shouldEnhanceClone true if the method being visited is clone() and clone enhancement should be performed.
         * @param shouldEnhanceReadObject true if the method being visited is readObject(ObjectInputStream) and it should
         *  be enhanced.  
         */
        MethodEnhancer(MethodVisitor aMethodVisitor, String aMethodName, boolean shouldEnhanceExternalOnly, boolean shouldEnhanceConstructor, boolean shouldEnhanceClone, boolean shouldEnhanceReadObject) {
            super(aMethodVisitor);
            mShouldEnhanceExternalOnly = shouldEnhanceExternalOnly;
            mShouldEnhanceConstructor = shouldEnhanceConstructor;
            mShouldEnhanceClone = shouldEnhanceClone;
            mShouldEnhanceReadObject = shouldEnhanceReadObject;
            mIsClinit = aMethodName.equals("<clinit>");
        }

        /** 
         * {@inheritDoc}
         * Do special first instruction insertion, if necessary.
         * 
         * @see org.objectweb.asm.MethodVisitor#visitCode()
         */
        public void visitCode() {
            mv.visitCode();
            if (mShouldEnhanceReadObject) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "initPersistable", sPersistableVoidSignature);
            }
        }

        /** 
         * {@inheritDoc}
         * Replace any getfield/getstatic/putfield/putstatic instructions in the 
         * specified method which reference fields of persistable classes.
         * 
         * @see org.objectweb.asm.MethodAdapter#visitFieldInsn(int, java.lang.String, java.lang.String, java.lang.String)
         */
        public void visitFieldInsn(int anOpcode, String anOwnerClass, String aFieldName, String aFieldDescriptor) {
            try {
                String ownerClassDotted = anOwnerClass.replace('/', '.');
                if (!mIsClinit && mMetaData.isClassAFCO(ownerClassDotted) && !(mShouldEnhanceExternalOnly && anOwnerClass.equals(mThisClassNameSlashed))) {
                    String ownerClassDescriptor = 'L' + anOwnerClass + ';';
                    switch(anOpcode) {
                        case GETFIELD:
                            mv.visitMethodInsn(INVOKESTATIC, anOwnerClass, FIELD_ACCESSOR_PREFIX + getFieldMethodNameSuffix(ownerClassDotted, aFieldName), '(' + ownerClassDescriptor + ')' + aFieldDescriptor);
                            break;
                        case PUTFIELD:
                            mv.visitMethodInsn(INVOKESTATIC, anOwnerClass, FIELD_MUTATOR_PREFIX + getFieldMethodNameSuffix(ownerClassDotted, aFieldName), '(' + ownerClassDescriptor + aFieldDescriptor + ")V");
                            break;
                        case GETSTATIC:
                            mv.visitMethodInsn(INVOKESTATIC, anOwnerClass, FIELD_ACCESSOR_PREFIX + getFieldMethodNameSuffix(ownerClassDotted, aFieldName), "()" + aFieldDescriptor);
                            break;
                        case PUTSTATIC:
                            mv.visitMethodInsn(INVOKESTATIC, anOwnerClass, FIELD_MUTATOR_PREFIX + getFieldMethodNameSuffix(ownerClassDotted, aFieldName), '(' + aFieldDescriptor + ")V");
                            break;
                        default:
                            throw new EnhancerException("Unexpected field opcode: " + anOpcode, null);
                    }
                } else {
                    mv.visitFieldInsn(anOpcode, anOwnerClass, aFieldName, aFieldDescriptor);
                }
            } catch (MetaDataException e) {
                throw new EnhancerException("Error processing class " + mClassName, e);
            }
        }

        /** 
         * {@inheritDoc}
         * Special enhancement on top-level persistable constructors.
         * @see org.objectweb.asm.MethodAdapter#visitMethodInsn(int, java.lang.String, java.lang.String, java.lang.String)
         */
        public void visitMethodInsn(int anOpcode, String anOwnerClass, String aMethodName, String aMethodDescriptor) {
            mv.visitMethodInsn(anOpcode, anOwnerClass, aMethodName, aMethodDescriptor);
            if (mShouldEnhanceConstructor && anOpcode == INVOKESPECIAL && anOwnerClass.equals(mSuperClassNameSlashed)) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "initPersistable", sPersistableVoidSignature);
            }
        }

        /** 
         * {@inheritDoc}
         * Special enhancement on clone() methods.
         * @see org.objectweb.asm.MethodAdapter#visitInsn(int)
         */
        public void visitInsn(int anOpcode) {
            if (mShouldEnhanceClone && anOpcode == ARETURN) {
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESTATIC, sPersistableHelperClassSlashed, "initPersistableClone", sPersistableVoidSignature);
            } else if (mIsPersistable && mIsClinit && anOpcode == RETURN) {
                mEnhancedClinit = true;
                mv.visitLdcInsn((Long) mClassId);
                mv.visitFieldInsn(PUTSTATIC, mThisClassNameSlashed, sClassIdFieldName, "J");
            }
            mv.visitInsn(anOpcode);
        }
    }
}
