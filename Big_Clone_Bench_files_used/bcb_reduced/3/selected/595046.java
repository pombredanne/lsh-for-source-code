package net.sf.hippopotam.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;
import java.awt.Component;
import net.sf.hippopotam.model.KeyNamePair;

/**
 *
 */
public class ObjectUtil {

    public static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9]+[a-zA-Z0-9_-]*(\\.[a-zA-Z0-9_-]+)*(\\.[a-zA-Z0-9][a-zA-Z0-9-]{0,10}[a-zA-Z0-9])$");

    public static boolean isEmailValid(final String v) {
        if (v == null) {
            return true;
        } else {
            return EMAIL_PATTERN.matcher(v).matches();
        }
    }

    public static String getEmptyInsteadOfNull(String s) {
        if (s == null) {
            return "";
        } else {
            return s;
        }
    }

    public static String formatInteger(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }

    public static String trimString(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if ("".equals(s)) {
            return null;
        }
        return s;
    }

    public static String toString(Object o) {
        return o == null ? "" : o.toString();
    }

    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof String) {
            String s = (String) o;
            return "".equals(s.trim());
        }
        return false;
    }

    public static boolean areEqual(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static boolean isOnlyOneEmpty(Object o1, Object o2) {
        boolean empty1 = isEmpty(o1);
        boolean empty2 = isEmpty(o2);
        return (empty1 || empty2) && (!(empty1 && empty2));
    }

    public static boolean areBothEmpty(Object o1, Object o2) {
        boolean empty1 = isEmpty(o1);
        boolean empty2 = isEmpty(o2);
        return empty1 && empty2;
    }

    public static int calculateAge(Date birthday) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        int currentYear = gregorianCalendar.get(GregorianCalendar.YEAR);
        gregorianCalendar.setTime(birthday);
        int birthdayYear = gregorianCalendar.get(GregorianCalendar.YEAR);
        return currentYear - birthdayYear;
    }

    public static <T> T paramIfNull(T ref, T param) {
        if (ref == null) {
            return param;
        } else {
            return ref;
        }
    }

    public static byte[] objectToBytes(Object inData) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(inData);
        objectOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    public static Object bytesToObject(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return objectInputStream.readObject();
    }

    public static Object clone(Object o) {
        try {
            byte[] bytes = objectToBytes(o);
            return bytesToObject(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String getClassNameWithoutPackage(Class aClass) {
        return getClassNameWithoutPackage(aClass.getName());
    }

    public static String getClassNameWithoutPackage(String className) {
        if (!className.contains(".")) {
            return className;
        }
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public static Class[] createParameterClassesArray(List inDataList) {
        Class[] result = new Class[inDataList.size()];
        for (int i = 0; i < inDataList.size(); i++) {
            Object parameter = inDataList.get(i);
            if (parameter == null) {
                return null;
            }
            result[i] = parameter.getClass();
        }
        return result;
    }

    public static Method findMethodByParameters(Object object, String methodName, Class[] parameterClassesArray) throws NoSuchMethodException {
        return object.getClass().getMethod(methodName, parameterClassesArray);
    }

    public static Method findMethodByName(Object object, String methodName) throws NoSuchMethodException {
        Method[] methodArray = object.getClass().getMethods();
        for (Method method : methodArray) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException("methodName=" + methodName);
    }

    public static Object getProperty(Object bean, String name) {
        try {
            return findGetter(bean, name).invoke(bean);
        } catch (Exception e) {
            throw new RuntimeException("getProperty '" + name + "' for bean " + bean, e);
        }
    }

    public static Object getPropertyOrEmptyBean(Object entity, String name) {
        Object result = getProperty(entity, name);
        if (result == null) {
            Method getter = findGetter(entity, name);
            Class returnType = getter.getReturnType();
            try {
                Constructor constructor = returnType.getConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static void setProperty(Object bean, String propertyName, Object value) {
        Method setter = findSetterByArgValue(bean, propertyName, value);
        if (setter == null) {
            throw new RuntimeException("Method not found: " + setterSignatureAsString(bean, propertyName, value));
        }
        try {
            setter.invoke(bean, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Possible bad method args: " + setterSignatureAsString(bean, propertyName, value));
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static String setterSignatureAsString(Object bean, String propertyName, Object value) {
        String message;
        message = bean.getClass() + "#" + setterName(propertyName);
        if (value != null) {
            message += "(" + value.getClass() + ")";
        }
        return message;
    }

    public static boolean isPropertyExists(Object bean, String propertyName) {
        try {
            return bean.getClass().getMethod(getterName(propertyName)) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static Method findGetter(Object bean, String propertyName) {
        try {
            return bean.getClass().getMethod(getterName(propertyName));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method findSetterByArgValue(Object bean, String propertyName, Object value) {
        if (value == null) {
            return findSetterIfSingle(bean, propertyName);
        }
        Method setter = findSetterByArgType(bean, propertyName, value.getClass());
        if (setter != null) {
            return setter;
        }
        return findSetterIfSingle(bean, propertyName);
    }

    public static Method findSetterByArgType(Object bean, String propertyName, Class clazz) {
        try {
            return bean.getClass().getMethod(setterName(propertyName), clazz);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findSetterIfSingle(Object bean, String propertyName) {
        String setterName = setterName(propertyName);
        Method method = null;
        for (Method m : bean.getClass().getMethods()) {
            if (m.getParameterTypes().length != 1) {
                continue;
            }
            if (m.getName().equals(setterName)) {
                if (method != null) {
                    return null;
                }
                method = m;
            }
        }
        return method;
    }

    public static String getterName(String propertyName) {
        return "get" + toUppercaseFirstLetter(propertyName);
    }

    public static String setterName(String propertyName) {
        return "set" + toUppercaseFirstLetter(propertyName);
    }

    public static String toUppercaseFirstLetter(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    public static String toLowercaseFirstLetter(String propertyName) {
        return propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
    }

    public static List<String> getStringListByToken(String source, String delim) {
        List<String> result = new ArrayList<String>();
        if (trimString(source) == null) {
            return result;
        }
        for (StringTokenizer stringTokenizer = new StringTokenizer(source, delim, false); stringTokenizer.hasMoreTokens(); ) {
            String nextToken = trimString(stringTokenizer.nextToken());
            if (nextToken != null) {
                result.add(nextToken);
            }
        }
        return result;
    }

    public static boolean areBothNotEmpty(Object object1, Object object2) {
        return !isEmpty(object1) && !isEmpty(object2);
    }

    public static boolean entityFilterMatches(String entityValue, String filterValue) {
        return isEmpty(filterValue) || (entityValue != null && entityValue.toUpperCase().startsWith(filterValue.toUpperCase()));
    }

    public static int[] toIntArray(Collection<Integer> c) {
        int[] result = new int[c.size()];
        int i = 0;
        for (Integer value : c) {
            if (value == null) {
                throw new IllegalArgumentException("Collection element suggested not null");
            }
            result[i++] = value;
        }
        return result;
    }

    public static List<Integer> toIntegerList(int[] array) {
        List<Integer> result = new ArrayList<Integer>();
        for (int value : array) {
            result.add(value);
        }
        return result;
    }

    public static int getZeroInsteadOfNull(Integer value) {
        return value != null ? value : 0;
    }

    public static boolean isPhoneValid(String phone) {
        if (ObjectUtil.isEmpty(phone)) {
            return true;
        }
        return phone.replaceAll("[()+\\s]", "").matches("[0-9]+");
    }

    public static boolean isKeyNamePairListModified(Collection<KeyNamePair> newKeyNamePairList, Collection<KeyNamePair> oldKeyNamePairList) {
        for (KeyNamePair keyNamePair : newKeyNamePairList) {
            if (!oldKeyNamePairList.contains(keyNamePair)) {
                return true;
            }
        }
        for (KeyNamePair keyNamePair : oldKeyNamePairList) {
            if (!newKeyNamePairList.contains(keyNamePair)) {
                return true;
            }
        }
        return false;
    }

    public static String truncate(String s, int len) {
        if (s == null) return s;
        if (s.length() <= len) return s;
        if (len <= 0) return "";
        return s.substring(0, len);
    }

    public static void setField(Object o, String fieldName, Object fieldValue) {
        try {
            Field f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(o, fieldValue);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getField(Object o, String fieldName) {
        try {
            Field f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(o);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static String digestByMd5(byte[] bytes) {
        return digest(bytes, "MD5");
    }

    public static String digestBySha(byte[] bytes) {
        return digest(bytes, "SHA");
    }

    private static String digest(byte[] arg, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] res = md.digest(arg);
            return toHexadecimalString(res);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toHexadecimalString(byte[] bytes) {
        StringBuffer strbuf = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            if (((int) b & 0xff) < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Integer.toString((int) b & 0xff, 16));
        }
        return strbuf.toString();
    }

    public static byte[] toBytesFromHexadecimal(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i + i, i + i + 2), 16);
        }
        return bytes;
    }

    public static Method findMethod(Object object, String methodName, List args) throws NoSuchMethodException {
        Class[] parameterClassesArray = createParameterClassesArray(args);
        if (parameterClassesArray == null) {
            return findMethodByName(object, methodName);
        }
        try {
            return findMethodByParameters(object, methodName, parameterClassesArray);
        } catch (Throwable t) {
            return findMethodByName(object, methodName);
        }
    }

    public static Object createNewInstance(Object object) {
        try {
            Class aClass = object.getClass();
            Constructor constructor = aClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] splitSting(String s) {
        List<String> result = new ArrayList<String>();
        for (String string : s.split("\\s")) {
            string = trimString(string);
            if (string != null) {
                result.add(string);
            }
        }
        return result.toArray(new String[] {});
    }

    public static String concatenateStrings(String[] strings) {
        StringBuffer stringBuffer = new StringBuffer();
        for (String s : strings) {
            if (stringBuffer.length() > 0) {
                stringBuffer.append(" ");
            }
            stringBuffer.append(s);
        }
        return stringBuffer.toString();
    }

    public static boolean isGetter(String methodName) {
        if (methodName.length() < 4) {
            return false;
        }
        if (!methodName.startsWith("get")) {
            return false;
        }
        if (!methodName.substring(3, 4).toUpperCase().equals(methodName.substring(3, 4))) {
            return false;
        }
        return true;
    }
}
