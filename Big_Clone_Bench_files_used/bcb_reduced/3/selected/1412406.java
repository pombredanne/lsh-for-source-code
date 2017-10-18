package oscript.classwrap;

import oscript.data.*;
import oscript.exceptions.*;
import oscript.compiler.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Constants;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.security.*;

/**
 * The <code>classwrap</code> package is used to generate a "wrapper" for
 * a java class.  A wrapper is just a subclass of a given java class, where
 * for each public non-final, non-static method, a wrapper method and an
 * "orig" method are generated.  The wrapper method looks up a property
 * with the same name in the script object, and if it exists, and is a
 * function that takes a compatible number of arguments, calls it,
 * otherwise it calls the same method in the parent (original) java class.
 * The orig method simply calls the same method in the parent class.
 * <p>
 * The "wrapper" class is used any place where a script objects extends
 * a java class (ie. java class, java interface, or builtin-type).  The
 * purpose is to allow the script object to override methods in a java
 * class, or implement methods in a java interface.
 * <p>
 * The "wrapper" class is generated using the Byte Code Engineering Library
 * (BCEL.jar).
 * 
 * @author Rob Clark (rob@ti.com)
 * <!--$Format: " * @version $Revision$"$-->
 * @version 1.34
 */
public class ClassWrapGen {

    private static java.util.Hashtable wrapperClassTable = new java.util.Hashtable();

    /**
   * The class that is being built.
   */
    private ClassGen cg;

    /**
   * The constant-pool of the class that is being built.
   */
    private ConstantPoolGen cp;

    /**
   */
    private Class origClass;

    private String origClassName;

    private String className;

    /**
   * Table mapping symbol name to static member idx
   */
    private Hashtable symbolTable = new Hashtable();

    /**
   * Note, conflict between org.apache.bcel.generic.Type and oscript.data.Type.
   */
    private static final org.apache.bcel.generic.Type SCOPE_TYPE = new ObjectType("oscript.data.Scope");

    private static final ObjectType EXCEPTION_TYPE = new ObjectType("oscript.exceptions.PackagedScriptObjectException");

    /**
   * Used to load a class from a <code>JavaClass</code>.
   */
    private static final CompilerClassLoader loader = CompilerClassLoader.getCompilerClassLoader();

    /**
   * Check if we can make a wrapper class for the specified class... this
   * is needed by JavaClassWrapper, which needs to know if it can construct
   * a wrapper class... but it wants to defer the act of actually making
   * the wrapper class.
   * 
   * @param origClass    the original class
   * @return <code>true</code> if we can make a wrapper (ie. class isn't
   * final, etc)
   */
    public static boolean canMakeWrapperClass(Class origClass) {
        int m = origClass.getModifiers();
        if (origClass.isPrimitive() || origClass.isArray()) throw new ProgrammingErrorException(origClass + " is primitive or array"); else if (origClass.getName().endsWith("_wrapper")) throw new ProgrammingErrorException(origClass + " is already a wrapper class"); else if (Modifier.isFinal(m) || !Modifier.isPublic(m)) return false; else return true;
    }

    /**
   * Make the wrapper class.  Wrap all public non-final instance methods.
   * 
   * @param origClass    the original class
   * @return the auto-generated wrapper class, or if a wrapper class cannot
   * be generated, the <code>origClass</code>.  This should never return
   * <code>null</code>.
   */
    public static synchronized Class makeWrapperClass(Class origClass) {
        Class tmp;
        if (!canMakeWrapperClass(origClass)) {
            return origClass;
        } else if ((tmp = (Class) (wrapperClassTable.get(origClass))) != null) {
            return tmp;
        } else {
            String className = "wrap" + oscript.OscriptInterpreter.CACHE_VERSION + "." + origClass.getName() + "_wrapper";
            try {
                tmp = CompilerClassLoader.forName(className, false, null);
            } catch (ClassNotFoundException e) {
                tmp = null;
            }
            if (tmp != null) {
                try {
                    if (computeInterfaceVersionUID(origClass) != ((Long) (tmp.getDeclaredField("__interfaceVersionUID").get(null))).longValue()) tmp = null;
                } catch (Throwable t) {
                    tmp = null;
                }
            }
            if (tmp == null) {
                tmp = (new ClassWrapGen(origClass, className)).makeWrapperClassImpl();
            }
            if (tmp != null) {
                wrapperClassTable.put(origClass, tmp);
            }
            return tmp;
        }
    }

    /**
   * Compute the interface version uid... this works in a manner similar 
   * to the <code>serialVersionUID</code> that java's serialization uses,
   * but since the code to compute the <code>serialVersionUID</code> isn't
   * available to us, we have to recalculate it ourselves.  Since we have
   * to calculate it anyways, we simplify a little by only taking into
   * account things that would change the wrapper class itself (ie. public
   * constructors and public methods), and thus reducing the number of 
   * false negatives w.r.t. changes to the original java class.
   * <p>
   * NOTE: synchronized under the umbrella of {@link #makeWrapperClass}
   */
    private static final long computeInterfaceVersionUID(Class c) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            DataOutputStream dos = new DataOutputStream(new DigestOutputStream(new ByteArrayOutputStream(512), md));
            Constructor[] constructors = c.getConstructors();
            Arrays.sort(constructors, new Comparator() {

                public int compare(Object o1, Object o2) {
                    int rc = ((Constructor) o1).getName().compareTo(((Constructor) o2).getName());
                    if (rc == 0) rc = getSignature((Constructor) o1).compareTo(getSignature((Constructor) o2));
                    return rc;
                }
            });
            for (int i = 0; i < constructors.length; i++) {
                dos.writeUTF("<init>");
                dos.writeUTF(getSignature(constructors[i]));
            }
            Method[] methods = c.getMethods();
            Arrays.sort(methods, new Comparator() {

                public int compare(Object o1, Object o2) {
                    int rc = ((Method) o1).getName().compareTo(((Method) o2).getName());
                    if (rc == 0) rc = getSignature((Method) o1).compareTo(getSignature((Method) o2));
                    return rc;
                }
            });
            for (int i = 0; i < methods.length; i++) {
                int mod = methods[i].getModifiers();
                if (!(Modifier.isStatic(mod) || Modifier.isFinal(mod))) {
                    dos.writeUTF(methods[i].getName());
                    dos.writeUTF(getSignature(methods[i]));
                }
            }
            dos.flush();
            byte[] hash = md.digest();
            long uid = 0;
            for (int i = 0; i < Math.min(8, hash.length); i++) uid += (long) (hash[i] & 0xff) << (i * 8);
            return uid;
        } catch (IOException e) {
            throw new ProgrammingErrorException("shouldn't happen: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new ProgrammingErrorException("shouldn't happen: " + e.getMessage());
        }
    }

    private static final Hashtable constructorSignatureCache = new Hashtable();

    private static final String getSignature(Constructor constructor) {
        String str = (String) (constructorSignatureCache.get(constructor));
        if (str == null) {
            StringBuffer sb = new StringBuffer();
            sb.append('(');
            Class[] params = constructor.getParameterTypes();
            for (int i = 0; i < params.length; i++) getSignature(params[i], sb);
            sb.append(")V");
            str = sb.toString();
            constructorSignatureCache.put(constructor, str);
        }
        return str;
    }

    private static final Hashtable methodSignatureCache = new Hashtable();

    private static final String getSignature(Method method) {
        String str = (String) (methodSignatureCache.get(method));
        if (str == null) {
            StringBuffer sb = new StringBuffer();
            sb.append('(');
            Class[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) getSignature(params[i], sb);
            sb.append(")V");
            str = sb.toString();
            methodSignatureCache.put(method, str);
        }
        return str;
    }

    private static final void getSignature(Class c, StringBuffer sb) {
        if (c.isArray()) {
            sb.append('[');
            getSignature(c.getComponentType(), sb);
        } else if (c.isPrimitive()) {
            if (c == Integer.TYPE) sb.append('I'); else if (c == Byte.TYPE) sb.append('B'); else if (c == Long.TYPE) sb.append('J'); else if (c == Float.TYPE) sb.append('F'); else if (c == Double.TYPE) sb.append('D'); else if (c == Short.TYPE) sb.append('S'); else if (c == Character.TYPE) sb.append('C'); else if (c == Boolean.TYPE) sb.append('Z'); else if (c == Void.TYPE) sb.append('V'); else throw new ProgrammingErrorException("hmm, I don't know what " + c + " is");
        } else {
            sb.append('L');
            sb.append(c.getName().replace('.', '/'));
            sb.append(';');
        }
    }

    /**
   * Because the wrapper method looks to the script object first, before
   * calling the wrapped method, there are times when we want to call the
   * original method directly.  To do this, you call the "orig" method.
   * To hide the naming convention, other code should use this method
   * to get the name of the "orig" method for the named method.
   * 
   * @param javaObj      the java object
   * @param methodName   the name of the method in the parent class
   * @return the mangled name of the method that calls the requested
   * method in the parent class, ie. the "orig" method.
   */
    public static String getOrigMethodName(Object javaObj, String methodName) {
        if ((javaObj == null) || (javaObj instanceof WrappedClass)) return "__orig_" + methodName; else return methodName;
    }

    public static String getOrigMethodName(String methodName) {
        return getOrigMethodName(null, methodName);
    }

    /**
   * Is the specified object an instance of a wrapper class?
   * 
   * @param javaObj      the java object to test
   * @return <code>true</code> if instance of auto-generated wrapper class
   */
    public static boolean isWrapperInstance(Object javaObj) {
        return (javaObj instanceof WrappedClass) || (javaObj instanceof WrappedInterface);
    }

    /**
   * Link the specified java object and script object.  The java object
   * should be an instance of a wrapper class generated by the 
   * <code>makeWrapperClass</code> method.
   * 
   * @param javaObj      the java object
   * @param scriptObj    the script object
   */
    public static void linkObjects(Object javaObj, Scope scriptObj) {
        ((Scope) scriptObj).__setJavaObject(javaObj);
        if (javaObj instanceof WrappedClass) ((WrappedClass) javaObj).__setScriptObject(scriptObj); else if (javaObj instanceof WrappedInterface) ((WrappedInterface) javaObj).__setScriptObject(scriptObj); else if (Value.DEBUG) System.err.println("could not link:  javaObj=" + javaObj + " (" + javaObj.getClass() + "), scriptObj=" + scriptObj + " (" + scriptObj.getType() + ")");
    }

    /**
   * Given a java object, which may be linked to a script object, return
   * the linked script object.
   * 
   * @param javaObj      the java object
   * @return the script object, or <code>null</code>
   */
    public static Value getScriptObject(Object javaObj) {
        if (javaObj instanceof WrappedClass) return ((WrappedClass) javaObj).__getScriptObject(); else if (javaObj instanceof WrappedInterface) return ((WrappedInterface) javaObj).__getScriptObject();
        return null;
    }

    /**
   * Given a java class that may or may not be a wrapper class, return a
   * java class that is the closest super-class that is not a wrapper
   * class.
   * 
   * @param javaClass    a java class that might be a wrapper class
   * @return a java class that is not a wrapper class
   */
    public static final Class getNonWrapperClass(Class javaClass) {
        Class tmp;
        if (((tmp = javaClass.getSuperclass()) != null) && ((tmp = (Class) (wrapperClassTable.get(tmp))) != null) && (tmp == javaClass)) {
            return javaClass.getSuperclass();
        } else {
            return javaClass;
        }
    }

    /**
   */
    private ClassWrapGen(Class origClass, String className) {
        this.origClass = origClass;
        this.origClassName = origClass.getName();
        this.className = loader.makeClassName(className, true);
    }

    /**
   */
    private Class makeWrapperClassImpl() {
        synchronized (ClassGen.class) {
            try {
                String superClassName;
                String[] interfaceNames;
                if (origClass.isInterface()) {
                    superClassName = "java.lang.Object";
                    interfaceNames = new String[] { origClass.getName(), "oscript.classwrap.WrappedInterface" };
                } else {
                    superClassName = origClassName;
                    interfaceNames = new String[] { "oscript.classwrap.WrappedClass" };
                }
                cg = new ClassGen(className, superClassName, "<generated>", Constants.ACC_PUBLIC | Constants.ACC_SUPER, interfaceNames);
                cp = cg.getConstantPool();
                Constructor[] constructors = origClass.getConstructors();
                if (constructors.length > 0) for (int i = 0; i < constructors.length; i++) addConstructor(constructors[i]); else addEmptyConstructor();
                Method[] methods = origClass.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    int mod = methods[i].getModifiers();
                    if (!(Modifier.isStatic(mod) || Modifier.isFinal(mod))) addMethod(methods[i]);
                }
                addCommonJunk();
                if (Value.DEBUG && false) {
                    try {
                        org.apache.bcel.util.Class2HTML c2html = new org.apache.bcel.util.Class2HTML(cg.getJavaClass(), "../");
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
                return loader.makeClass(className, cg.getJavaClass());
            } catch (LinkageError e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private void addCommonJunk() {
        {
            FieldGen fg = new FieldGen(Constants.ACC_PUBLIC | Constants.ACC_FINAL | Constants.ACC_STATIC, org.apache.bcel.generic.Type.LONG, "__interfaceVersionUID", cp);
            cg.addField(fg.getField());
        }
        {
            CompilerInstructionList il = new CompilerInstructionList();
            il.append(new PUSH(cp, computeInterfaceVersionUID(origClass)));
            il.append(new PUTSTATIC(cp.addFieldref(className, "__interfaceVersionUID", "J")));
            for (Iterator itr = symbolTable.entrySet().iterator(); itr.hasNext(); ) {
                Entry e = (Entry) (itr.next());
                String name = (String) (e.getKey());
                Integer iidx = (Integer) (e.getValue());
                il.append(new PUSH(cp, name));
                il.append(new INVOKESTATIC(cp.addMethodref("oscript.data.Symbol", "getSymbol", "(Ljava/lang/String;)Loscript/data/Symbol;")));
                il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Symbol", "getId", "()I")));
                il.append(new PUTSTATIC(iidx.intValue()));
            }
            il.append(InstructionConstants.RETURN);
            MethodGen mg = new MethodGen(Constants.ACC_STATIC | Constants.ACC_PUBLIC | Constants.ACC_FINAL, org.apache.bcel.generic.Type.VOID, new org.apache.bcel.generic.Type[] {}, new String[] {}, "<clinit>", className, il, cp);
            mg.setMaxStack();
            cg.addMethod(mg.getMethod());
        }
        {
            FieldGen fg = new FieldGen(Constants.ACC_PRIVATE, SCOPE_TYPE, "__scriptObject", cp);
            cg.addField(fg.getField());
        }
        {
            CompilerInstructionList il = new CompilerInstructionList();
            il.append(InstructionConstants.ALOAD_0);
            il.append(InstructionConstants.ALOAD_1);
            il.append(new PUTFIELD(cp.addFieldref(className, "__scriptObject", "Loscript/data/Scope;")));
            il.append(InstructionConstants.RETURN);
            MethodGen mg = new MethodGen(Constants.ACC_PUBLIC | Constants.ACC_FINAL, org.apache.bcel.generic.Type.VOID, new org.apache.bcel.generic.Type[] { SCOPE_TYPE }, new String[] { "val" }, "__setScriptObject", className, il, cp);
            mg.setMaxStack();
            cg.addMethod(mg.getMethod());
        }
        {
            CompilerInstructionList il = new CompilerInstructionList();
            il.append(InstructionConstants.ALOAD_0);
            il.append(new GETFIELD(cp.addFieldref(className, "__scriptObject", "Loscript/data/Scope;")));
            il.append(InstructionConstants.ARETURN);
            MethodGen mg = new MethodGen(Constants.ACC_PUBLIC | Constants.ACC_FINAL, SCOPE_TYPE, new org.apache.bcel.generic.Type[] {}, new String[] {}, "__getScriptObject", className, il, cp);
            mg.setMaxStack();
            cg.addMethod(mg.getMethod());
        }
    }

    private void addConstructor(Constructor constructor) {
        Class[] paramTypes = constructor.getParameterTypes();
        CompilerInstructionList il = new CompilerInstructionList();
        insertReturnCallSuper(il, "<init>", Void.TYPE, paramTypes);
        MethodGen mg = new MethodGen(getAccessFlags(constructor.getModifiers()), org.apache.bcel.generic.Type.VOID, getParamTypes(paramTypes), getParamNames(paramTypes), "<init>", className, il, cp);
        Class[] exceptionTypes = constructor.getExceptionTypes();
        for (int i = 0; i < exceptionTypes.length; i++) {
            mg.addException(exceptionTypes[i].getName());
        }
        mg.setMaxStack();
        cg.addMethod(mg.getMethod());
    }

    private void addEmptyConstructor() {
        Class[] paramTypes = new Class[0];
        CompilerInstructionList il = new CompilerInstructionList();
        insertReturnCallSuper(il, "<init>", Void.TYPE, paramTypes);
        MethodGen mg = new MethodGen(Constants.ACC_PUBLIC, org.apache.bcel.generic.Type.VOID, getParamTypes(paramTypes), getParamNames(paramTypes), "<init>", className, il, cp);
        mg.setMaxStack();
        cg.addMethod(mg.getMethod());
    }

    private Hashtable methodTable = new Hashtable();

    private void addMethod(Method method) {
        Class[] paramTypes = method.getParameterTypes();
        Class retType = method.getReturnType();
        boolean isAbstract = origClass.isInterface() || Modifier.isAbstract(method.getModifiers());
        {
            String methodSig = method.getName() + "#" + makeMethodSignature(retType, paramTypes);
            if (methodTable.get(methodSig) != null) return;
            methodTable.put(methodSig, method);
        }
        {
            CompilerInstructionList il = new CompilerInstructionList();
            il.append(InstructionConstants.ALOAD_0);
            il.append(new GETFIELD(cp.addFieldref(className, "__scriptObject", "Loscript/data/Scope;")));
            il.append(InstructionConstants.DUP);
            IFNULL ifnull1 = new IFNULL(null);
            il.append(ifnull1);
            pushSymbol(il, method.getName());
            il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Scope", "__getInstanceMember", "(I)Loscript/data/Value;")));
            il.append(InstructionConstants.DUP);
            IFNULL ifnull2 = new IFNULL(null);
            il.append(ifnull2);
            il.append(new PUSH(cp, paramTypes.length));
            il.append(new ANEWARRAY(cp.addClass("oscript.data.Value")));
            for (int i = 0, idx = 1; i < paramTypes.length; i++) {
                il.append(InstructionConstants.DUP);
                il.append(new PUSH(cp, i));
                idx += insertLoad(il, paramTypes[i], idx);
                insertConvertToScriptValue(il, paramTypes[i]);
                il.append(InstructionConstants.AASTORE);
            }
            il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Value", "callAsFunction", "([Loscript/data/Value;)Loscript/data/Value;")));
            if (retType.isPrimitive()) {
                if (retType == Void.TYPE) {
                    il.append(InstructionConstants.RETURN);
                } else if (retType == Double.TYPE) {
                    il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Value", "castToInexactNumber", "()D")));
                    il.append(InstructionConstants.DRETURN);
                } else if (retType == Float.TYPE) {
                    il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Value", "castToInexactNumber", "()D")));
                    il.append(InstructionConstants.D2F);
                    il.append(InstructionConstants.FRETURN);
                } else if (retType == Boolean.TYPE) {
                    il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Value", "castToBoolean", "()Z")));
                    il.append(InstructionConstants.IRETURN);
                } else if (retType == Long.TYPE) {
                    il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Value", "castToExactNumber", "()J")));
                    il.append(InstructionConstants.LRETURN);
                } else {
                    il.append(new INVOKEVIRTUAL(cp.addMethodref("oscript.data.Value", "castToExactNumber", "()J")));
                    il.append(InstructionConstants.L2I);
                    il.append(InstructionConstants.IRETURN);
                }
            } else if (retType == Value.class) {
                il.append(InstructionConstants.ARETURN);
            } else {
                il.append(new PUSH(cp, retType.getName()));
                il.append(new INVOKESTATIC(cp.addMethodref("oscript.data.JavaBridge", "convertToJavaObject", "(Loscript/data/Value;Ljava/lang/String;)Ljava/lang/Object;")));
                il.append(new CHECKCAST(cp.addClass(retType.getName())));
                il.append(InstructionConstants.ARETURN);
            }
            InstructionHandle target = il.append(InstructionConstants.POP);
            ifnull2.setTarget(target);
            ifnull1.setTarget(target);
            if (!isAbstract) {
                insertReturnCallSuper(il, method.getName(), retType, paramTypes);
            } else {
                il.append(new NEW(cp.addClass("oscript.data.ONoSuchMemberException")));
                il.append(InstructionConstants.DUP);
                il.append(new PUSH(cp, "[class " + origClass.getName() + "]: " + method.getName()));
                il.append(new INVOKESPECIAL(cp.addMethodref("oscript.data.ONoSuchMemberException", "<init>", "(Ljava/lang/String;)V")));
                il.append(new INVOKESTATIC(cp.addMethodref("oscript.exceptions.PackagedScriptObjectException", "makeExceptionWrapper", "(Loscript/data/Value;)Loscript/exceptions/PackagedScriptObjectException;")));
                il.append(InstructionConstants.ATHROW);
            }
            MethodGen mg = new MethodGen(getAccessFlags(method.getModifiers()), getParamType(retType), getParamTypes(paramTypes), getParamNames(paramTypes), method.getName(), className, il, cp);
            Class[] exceptionTypes = method.getExceptionTypes();
            for (int i = 0; i < exceptionTypes.length; i++) {
                mg.addException(exceptionTypes[i].getName());
            }
            mg.setMaxStack();
            cg.addMethod(mg.getMethod());
        }
        if (!isAbstract) {
            CompilerInstructionList il = new CompilerInstructionList();
            insertReturnCallSuper(il, method.getName(), retType, paramTypes);
            MethodGen mg = new MethodGen(getAccessFlags(method.getModifiers()), getParamType(retType), getParamTypes(paramTypes), getParamNames(paramTypes), getOrigMethodName(method.getName()), className, il, cp);
            Class[] exceptionTypes = method.getExceptionTypes();
            for (int i = 0; i < exceptionTypes.length; i++) mg.addException(exceptionTypes[i].getName());
            mg.setMaxStack();
            cg.addMethod(mg.getMethod());
        }
    }

    private int identifierCnt = 0;

    private void pushSymbol(CompilerInstructionList il, String name) {
        Integer iidx = (Integer) (symbolTable.get(name));
        if (iidx == null) {
            String fieldName = "_sym_" + (identifierCnt++) + name;
            FieldGen fg = new FieldGen(Constants.ACC_PRIVATE | Constants.ACC_STATIC, org.apache.bcel.generic.Type.INT, fieldName, cp);
            cg.addField(fg.getField());
            iidx = new Integer(cp.addFieldref(className, fieldName, org.apache.bcel.generic.Type.INT.getSignature()));
            symbolTable.put(name, iidx);
        }
        il.append(new GETSTATIC(iidx.intValue()));
    }

    private void insertReturnCallSuper(CompilerInstructionList il, String name, Class retType, Class[] paramTypes) {
        String className;
        if (origClass.isInterface()) className = "java.lang.Object"; else className = origClassName;
        il.append(InstructionConstants.ALOAD_0);
        for (int i = 0, idx = 1; i < paramTypes.length; i++) idx += insertLoad(il, paramTypes[i], idx);
        il.append(new INVOKESPECIAL(cp.addMethodref(className, name, makeMethodSignature(retType, paramTypes))));
        if (retType.isPrimitive()) {
            if (retType == Void.TYPE) il.append(InstructionConstants.RETURN); else if (retType == Double.TYPE) il.append(InstructionConstants.DRETURN); else if (retType == Float.TYPE) il.append(InstructionConstants.FRETURN); else if (retType == Long.TYPE) il.append(InstructionConstants.LRETURN); else il.append(InstructionConstants.IRETURN);
        } else {
            il.append(InstructionConstants.ARETURN);
        }
    }

    private int insertLoad(CompilerInstructionList il, Class paramType, int idx) {
        if (paramType.isPrimitive()) {
            if (paramType == Double.TYPE) {
                il.append(new DLOAD(idx));
                return 2;
            } else if (paramType == Float.TYPE) {
                il.append(new FLOAD(idx));
                return 1;
            } else if (paramType == Long.TYPE) {
                il.append(new LLOAD(idx));
                return 2;
            } else {
                il.append(new ILOAD(idx));
                return 1;
            }
        } else {
            il.append(new ALOAD(idx));
            return 1;
        }
    }

    private void insertConvertToScriptValue(CompilerInstructionList il, Class paramType) {
        String signature;
        if (paramType.isPrimitive()) {
            if (paramType == Double.TYPE) {
                signature = "(D)Loscript/data/Value;";
            } else if (paramType == Float.TYPE) {
                il.append(InstructionConstants.F2D);
                signature = "(D)Loscript/data/Value;";
            } else if (paramType == Long.TYPE) {
                signature = "(J)Loscript/data/Value;";
            } else if (paramType == Integer.TYPE) {
                il.append(InstructionConstants.I2L);
                signature = "(J)Loscript/data/Value;";
            } else if (paramType == Short.TYPE) {
                il.append(InstructionConstants.I2L);
                signature = "(J)Loscript/data/Value;";
            } else if (paramType == Byte.TYPE) {
                il.append(InstructionConstants.I2L);
                signature = "(J)Loscript/data/Value;";
            } else if (paramType == Boolean.TYPE) {
                signature = "(Z)Loscript/data/Value;";
            } else if (paramType == Character.TYPE) {
                il.append(new INVOKESTATIC(cp.addMethodref("java.lang.String", "valueOf", "(C)Ljava/lang/String;")));
                signature = "(Ljava/lang/String;)Loscript/data/Value;";
            } else {
                throw new ProgrammingErrorException("unknown primitive type: " + paramType);
            }
        } else if (paramType == String.class) {
            signature = "(Ljava/lang/String;)Loscript/data/Value;";
        } else {
            signature = "(Ljava/lang/Object;)Loscript/data/Value;";
        }
        il.append(new INVOKESTATIC(cp.addMethodref("oscript.data.JavaBridge", "convertToScriptObject", signature)));
    }

    private int getAccessFlags(int mod) {
        int acc = 0;
        if (Modifier.isPublic(mod)) {
            acc |= Constants.ACC_PUBLIC;
        }
        return acc;
    }

    private org.apache.bcel.generic.Type[] getParamTypes(Class[] paramTypes) {
        org.apache.bcel.generic.Type[] types = new org.apache.bcel.generic.Type[paramTypes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = getParamType(paramTypes[i]);
        }
        return types;
    }

    private org.apache.bcel.generic.Type getParamType(Class paramType) {
        if (paramType.isPrimitive()) {
            if (paramType == Boolean.TYPE) return org.apache.bcel.generic.Type.BOOLEAN; else if (paramType == Character.TYPE) return org.apache.bcel.generic.Type.CHAR; else if (paramType == Double.TYPE) return org.apache.bcel.generic.Type.DOUBLE; else if (paramType == Float.TYPE) return org.apache.bcel.generic.Type.FLOAT; else if (paramType == Integer.TYPE) return org.apache.bcel.generic.Type.INT; else if (paramType == Long.TYPE) return org.apache.bcel.generic.Type.LONG; else if (paramType == Short.TYPE) return org.apache.bcel.generic.Type.SHORT; else if (paramType == Byte.TYPE) return org.apache.bcel.generic.Type.BYTE; else if (paramType == Void.TYPE) return org.apache.bcel.generic.Type.VOID; else throw new ProgrammingErrorException("unknown primitive: " + paramType);
        } else if (paramType.isArray()) {
            return new ArrayType(getParamType(paramType.getComponentType()), 1);
        } else {
            return new ObjectType(paramType.getName());
        }
    }

    private String[] getParamNames(Class[] paramTypes) {
        String[] names = new String[paramTypes.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = "arg" + i;
        }
        return names;
    }

    private String makeMethodSignature(Class retType, Class[] paramTypes) {
        String signature = org.apache.bcel.generic.Type.getMethodSignature(getParamType(retType), getParamTypes(paramTypes));
        return signature;
    }
}
