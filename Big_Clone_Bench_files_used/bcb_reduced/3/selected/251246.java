package de.sicari.webservice.uddi;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import de.fhg.igd.logging.Logger;
import de.fhg.igd.logging.LoggerFactory;

/**
 * This generator class creates canonical hash codes by means of
 * the <i>Java Reflection Framework</i>.
 *
 * @author Matthias Pressfreund
 * @version "$Id: HashCodeGenerator.java 204 2007-07-11 19:26:55Z jpeters $"
 */
public class HashCodeGenerator {

    /**
     * The <code>Logger</code> instance for this class
     */
    private static Logger log_ = LoggerFactory.getLogger("webservice");

    /**
     * The cache for known hash codes
     */
    protected Map cache_;

    /**
     * Create a new <code>HashCodeGenerator</code>.
     */
    public HashCodeGenerator() {
        cache_ = new HashMap();
    }

    /**
     * Returns the <i>deep</i> string representation of the
     * given <code>object</code>. If object is an array,
     * this method is called recursively with its elements.
     *
     * @param object The object
     * @return The string representation of the object
     */
    protected String deepToString(Object object) {
        StringBuffer strbuf;
        boolean[] za;
        Object[] oa;
        double[] da;
        float[] fa;
        short[] sa;
        long[] la;
        byte[] ba;
        char[] ca;
        int[] ia;
        Class clazz;
        int i;
        strbuf = new StringBuffer("{");
        clazz = object.getClass();
        if (!clazz.isArray()) {
            return object.toString();
        }
        if (!clazz.getComponentType().isArray()) {
            switch(clazz.getName().charAt(1)) {
                case 'Z':
                    za = (boolean[]) object;
                    for (i = 0; i < za.length; i++) {
                        strbuf.append(za[i]);
                        if (i < za.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'B':
                    ba = (byte[]) object;
                    for (i = 0; i < ba.length; i++) {
                        strbuf.append(ba[i]);
                        if (i < ba.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'C':
                    ca = (char[]) object;
                    for (i = 0; i < ca.length; i++) {
                        strbuf.append(ca[i]);
                        if (i < ca.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'D':
                    da = (double[]) object;
                    for (i = 0; i < da.length; i++) {
                        strbuf.append(da[i]);
                        if (i < da.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'F':
                    fa = (float[]) object;
                    for (i = 0; i < fa.length; i++) {
                        strbuf.append(fa[i]);
                        if (i < fa.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'I':
                    ia = (int[]) object;
                    for (i = 0; i < ia.length; i++) {
                        strbuf.append(ia[i]);
                        if (i < ia.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'J':
                    la = (long[]) object;
                    for (i = 0; i < la.length; i++) {
                        strbuf.append(la[i]);
                        if (i < la.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                case 'S':
                    sa = (short[]) object;
                    for (i = 0; i < sa.length; i++) {
                        strbuf.append(sa[i]);
                        if (i < sa.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
                default:
                    oa = (Object[]) object;
                    for (i = 0; i < oa.length; i++) {
                        strbuf.append(oa[i]);
                        if (i < oa.length - 1) {
                            strbuf.append(", ");
                        }
                    }
                    break;
            }
            strbuf.append("}");
            return strbuf.toString();
        }
        oa = (Object[]) object;
        for (i = 0; i < oa.length; i++) {
            strbuf.append(deepToString(oa[i]));
            if (i < oa.length - 1) {
                strbuf.append(", ");
            }
        }
        strbuf.append("}");
        return strbuf.toString();
    }

    /**
     * Create a canonical hash code printout for an interface class.
     *
     * @param className The fully qualified name of the interface class
     * @return The canonical class hash code
     * @throws IllegalArgumentException
     *   if the specified class name is <code>null</code>,
     *   is not an interface or could not be resolved
     * @throws NoSuchAlgorithmException
     *   if the internal <code>MessageDigest</code> could not be initialized
     */
    public String interfaceHash(String className) throws IllegalArgumentException, NoSuchAlgorithmException {
        MessageDigest md;
        StringBuffer field;
        StringBuffer buf;
        SortedSet sorter;
        Object fieldValue;
        Field[] fields;
        String hash;
        Class clazz;
        String msg;
        Class tmp;
        List data;
        byte[] b;
        int val;
        int i;
        if (className == null) {
            log_.error(msg = "Invalid class name: " + className);
            throw new IllegalArgumentException(msg);
        }
        if ((hash = (String) cache_.get(className)) == null) {
            data = new ArrayList();
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                log_.error(msg = className + " could not be resolved");
                throw new IllegalArgumentException(msg);
            }
            if (!clazz.isInterface()) {
                log_.error(msg = className + " is not an interface");
                throw new IllegalArgumentException(msg);
            }
            data.add(new Integer(clazz.getModifiers()));
            data.add(clazz.getName());
            if ((tmp = clazz.getSuperclass()) != null) {
                data.add(tmp.getName());
            }
            sorter = new TreeSet(new Comparator() {

                public int compare(Object o1, Object o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });
            sorter.addAll(Arrays.asList(clazz.getInterfaces()));
            data.add(sorter.toString());
            sorter.clear();
            fields = clazz.getDeclaredFields();
            for (i = 0; i < fields.length; i++) {
                field = new StringBuffer(fields[i].toString());
                try {
                    fieldValue = fields[i].get(null);
                    if (fieldValue != null) {
                        field.append(" = ");
                        field.append(deepToString(fieldValue));
                    }
                } catch (Exception e) {
                }
                sorter.add(field.toString());
            }
            data.add(sorter.toString());
            sorter.clear();
            sorter.addAll(Arrays.asList(clazz.getDeclaredMethods()));
            data.add(sorter.toString());
            md = MessageDigest.getInstance("SHA1");
            md.update(Charset.forName("UTF-8").encode(data.toString()).array());
            b = md.digest();
            buf = new StringBuffer();
            for (i = 0; i < b.length; i++) {
                if ((val = b[i]) < 0) {
                    val += 0x100;
                }
                if (val < 0x10) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(val));
            }
            cache_.put(className, hash = buf.toString());
            log_.debug("Successfully created hash code for " + className + ": " + hash);
        }
        return hash;
    }
}
