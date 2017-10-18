package com.versant.core.jdo.tools.enhancer.utils;

import com.versant.lib.bcel.classfile.JavaClass;
import com.versant.lib.bcel.classfile.Method;
import com.versant.lib.bcel.classfile.Field;
import com.versant.lib.bcel.Repository;
import com.versant.lib.bcel.Constants;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 */
public class SerialUIDHelper {

    /**
     *  Find out if the class has a static class initializer <clinit>
     *
     */
    private static boolean hasStaticInitializer(JavaClass javaClass) {
        Method[] methods = javaClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals("<clinit>")) {
                return true;
            }
        }
        return false;
    }

    private static Comparator compareMethodBySig = new SerialUIDHelper.MethodSortSig();

    private static class MethodSortSig implements Comparator {

        public int compare(Object a, Object b) {
            Method methodA = (Method) a;
            Method methodB = (Method) b;
            String sigA = methodA.getSignature();
            String sigB = methodB.getSignature();
            return sigA.compareTo(sigB);
        }
    }

    private static Comparator compareMethodByName = new SerialUIDHelper.MethodSortName();

    private static class MethodSortName implements Comparator {

        public int compare(Object a, Object b) {
            Method methodA = (Method) a;
            Method methodB = (Method) b;
            String sigA = methodA.getName();
            String sigB = methodB.getName();
            return sigA.compareTo(sigB);
        }
    }

    private static Comparator compareFieldByName = new SerialUIDHelper.FieldSort();

    private static class FieldSort implements Comparator {

        public int compare(Object a, Object b) {
            Field fieldA = (Field) a;
            Field fieldB = (Field) b;
            return fieldA.getName().compareTo(fieldB.getName());
        }
    }

    private static Comparator compareStringByName = new SerialUIDHelper.CompareStringByName();

    private static class CompareStringByName implements Comparator {

        public int compare(Object o1, Object o2) {
            String c1 = (String) o1;
            String c2 = (String) o2;
            return c1.compareTo(c2);
        }
    }

    private static Set removePrivateConstructorsAndSort(JavaClass javaClass) {
        TreeSet set = new TreeSet(compareMethodBySig);
        Method[] methods = javaClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            com.versant.lib.bcel.classfile.Method m = methods[i];
            if (m.getName().equals("<init>") && (!m.isPrivate())) {
                set.add(m);
            }
        }
        return set;
    }

    private static Set removePrivateAndConstructorsAndSort(JavaClass javaClass) {
        TreeSet set = new TreeSet(compareMethodByName);
        Method[] methods = javaClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (!m.getName().startsWith("<")) {
                if (!m.isPrivate()) {
                    set.add(m);
                }
            }
        }
        return set;
    }

    public static long computeSerialVersionUID(JavaClass clazz) {
        ByteArrayOutputStream devnull = new ByteArrayOutputStream(512);
        long h = 0;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            DigestOutputStream mdo = new DigestOutputStream(devnull, md);
            DataOutputStream data = new DataOutputStream(mdo);
            data.writeUTF(clazz.getClassName());
            int classaccess = clazz.getAccessFlags();
            classaccess &= (Constants.ACC_PUBLIC | Constants.ACC_FINAL | Constants.ACC_INTERFACE | Constants.ACC_ABSTRACT);
            Method[] method = clazz.getMethods();
            if ((classaccess & Constants.ACC_INTERFACE) != 0) {
                classaccess &= (~Constants.ACC_ABSTRACT);
                if (method.length > 0) {
                    classaccess |= Constants.ACC_ABSTRACT;
                }
            }
            data.writeInt(classaccess);
            String interfaces[] = clazz.getInterfaceNames();
            Arrays.sort(interfaces, compareStringByName);
            for (int i = 0; i < interfaces.length; i++) {
                data.writeUTF(interfaces[i]);
            }
            com.versant.lib.bcel.classfile.Field[] field = clazz.getFields();
            Arrays.sort(field, compareFieldByName);
            for (int i = 0; i < field.length; i++) {
                Field f = field[i];
                int m = f.getAccessFlags();
                if ((f.isPrivate() && f.isStatic()) || (f.isPrivate() && f.isTransient())) {
                    continue;
                }
                data.writeUTF(f.getName());
                data.writeInt(m);
                data.writeUTF(f.getSignature());
            }
            if (hasStaticInitializer(clazz)) {
                data.writeUTF("<clinit>");
                data.writeInt(Constants.ACC_STATIC);
                data.writeUTF("()V");
            }
            Iterator nonPrivateConstructorsIter = removePrivateConstructorsAndSort(clazz).iterator();
            while (nonPrivateConstructorsIter.hasNext()) {
                Method m = (Method) nonPrivateConstructorsIter.next();
                String mname = "<init>";
                String desc = m.getSignature();
                desc = desc.replace('/', '.');
                data.writeUTF(mname);
                data.writeInt(m.getAccessFlags());
                data.writeUTF(desc);
            }
            Iterator nonPrivateAndNoConstructorsIter = removePrivateAndConstructorsAndSort(clazz).iterator();
            while (nonPrivateAndNoConstructorsIter.hasNext()) {
                Method m = (Method) nonPrivateAndNoConstructorsIter.next();
                String mname = m.getName();
                String desc = m.getSignature();
                desc = desc.replace('/', '.');
                data.writeUTF(mname);
                data.writeInt(m.getAccessFlags());
                data.writeUTF(desc);
            }
            data.flush();
            byte hasharray[] = md.digest();
            for (int i = 0; i < Math.min(8, hasharray.length); i++) {
                h += (long) (hasharray[i] & 255) << (i * 8);
            }
        } catch (IOException ignore) {
            h = -1;
        } catch (NoSuchAlgorithmException complain) {
            throw new SecurityException(complain.getMessage());
        }
        return h;
    }
}
