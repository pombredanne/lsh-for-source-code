package gnu.javax.rmi.CORBA;

import gnu.CORBA.OrbFunctional;
import gnu.CORBA.Minor;
import gnu.CORBA.Unexpected;
import gnu.CORBA.CDR.Vio;
import gnu.CORBA.CDR.gnuRuntime;
import gnu.CORBA.CDR.gnuValueStream;
import gnu.CORBA.CDR.HeadlessInput;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.StringValueHelper;
import org.omg.CORBA.WStringValueHelper;
import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.ObjectImpl;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ValueBase;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.POAManagerPackage.State;
import org.omg.SendingContext.RunTime;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.WeakHashMap;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.Util;

/**
 * Defines methods that must be accessible in several derived classes.
 * 
 * @author Audrius Meskauskas, Lithuania (AudriusA@Bioinformatics.org)
 */
public class RmiUtilities {

    /**
   * The currently used RMI-IIOP version format.
   */
    public static byte VERSION = 1;

    /**
   * The non - writable class fields.
   */
    static final int NON_WRITABLE = Modifier.STATIC | Modifier.TRANSIENT;

    /**
   * The standard String repository Id.
   */
    public static final String RMI_STRING_ID = StringValueHelper.id();

    /**
   * The standard Class repository Id.
   */
    public static final String RMI_CLASS_ID = "RMI:javax.rmi.CORBA.ClassDesc:2BABDA04587ADCCC:CFBF02CF5294176B";

    /**
   * The standard string array repository Id.
   */
    public static final String RMI_STRING_ARRAY_ID = "RMI:[Ljava.lang.String;:071DA8BE7F971128:A0F0A4387A3BB342";

    /**
   * An instance of the wide string value helper for writing strings.
   */
    static WStringValueHelper wStringValueHelper = new WStringValueHelper();

    /**
   * Set of serializable classes that have .writeObject and .readObject defined.
   * Contains weak references to ensure that the classes will be unloadable.
   */
    WeakHashMap io_format = new WeakHashMap();

    /**
   * The standard IO format with no .writeObject and .readObject defined.
   */
    static final Object STANDARD = new Object();

    /**
   * The custom IO format with .writeObject and .readObject defined,
   * defaultWriteObject called.
   */
    static final Object CUSTOM_DWO = new Object();

    /**
   * The custom IO format with .writeObject and .readObject defined,
   * defaultWriteObject has not been called.
   */
    static final Object CUSTOM_NO_DWO = new Object();

    /**
   * The arguments for readObject.
   */
    static final Class[] READ_OBJECT_ARGS = new Class[] { ObjectInputStream.class };

    /**
   * The arguments for writeObject.
   */
    static final Class[] WRITE_OBJECT_ARGS = new Class[] { ObjectOutputStream.class };

    /**
   * The undocumented field that is heading the Sun's object data, written with
   * writeObject.
   */
    static final int S_X = 16908034;

    /**
   * Write all fields of the passed value.
   */
    void writeFields(OutputStream an_output, Serializable object) {
        org.omg.CORBA_2_3.portable.OutputStream output = (org.omg.CORBA_2_3.portable.OutputStream) an_output;
        try {
            Class o_class = object.getClass();
            Field[] fields = getWritableFields(o_class);
            Field f;
            Class fc;
            for (int i = 0; i < fields.length; i++) {
                f = fields[i];
                fc = f.getType();
                Object v = f.get(object);
                if (fc == String.class) {
                    output.write_value((Serializable) v, wStringValueHelper);
                } else if (fc == int.class) output.write_long(((Integer) v).intValue()); else if (fc == long.class) output.write_longlong(((Number) v).longValue()); else if (fc == double.class) output.write_double(((Number) v).doubleValue()); else if (fc == float.class) output.write_float(((Number) v).floatValue()); else if (fc == boolean.class) output.write_boolean(((Boolean) v).booleanValue()); else if (fc == short.class) output.write_short(((Number) v).shortValue()); else if (fc == byte.class) output.write_octet(((Number) v).byteValue()); else if (fc == char.class) output.write_wchar(((Character) v).charValue()); else {
                    if (!fc.isInterface() && Remote.class.isAssignableFrom(fc)) fc = getExportedInterface(fc);
                    writeMember(output, v, fc);
                }
            }
        } catch (Exception ex) {
            MARSHAL m = new MARSHAL("Cannot write " + object);
            m.minor = Minor.ValueFields;
            m.initCause(ex);
            throw m;
        }
    }

    /**
   * Write a memeber (field) of the data structure.
   */
    void writeMember(org.omg.CORBA_2_3.portable.OutputStream output, Object object, Class xClass) {
        if (output instanceof gnuValueStream) {
            gnuRuntime g = ((gnuValueStream) output).getRunTime();
            if (g != null) g.target = null;
        }
        if (Serializable.class.isAssignableFrom(xClass) || Remote.class.isAssignableFrom(xClass)) {
            if (org.omg.CORBA.Object.class.isAssignableFrom(xClass) || Remote.class.isAssignableFrom(xClass)) {
                if (object == null) output.write_Object(null); else if (isTieRequired(object)) exportTie(output, object, xClass); else writeValue(output, (Serializable) object);
            } else output.write_value((Serializable) object, xClass);
        } else {
            MARSHAL m = new MARSHAL(xClass + " is not Serializable");
            m.minor = Minor.NonSerializable;
            throw m;
        }
    }

    /**
   * Check if the object must be wrapped into Tie, connected to the ORB and then
   * the corresponding Stub be written.
   */
    public boolean isTieRequired(Object object) {
        return object instanceof Remote && !(object instanceof Stub);
    }

    /**
   * Get the interface under that the class of this object must be exposed. The
   * interface must be derived from Remote.
   */
    Class getExportedInterface(Object object) throws MARSHAL {
        Class fc = null;
        Class[] interfaces = object.getClass().getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (!Remote.class.equals(interfaces[i])) if (Remote.class.isAssignableFrom(interfaces[i])) {
                if (fc == null) fc = interfaces[i]; else {
                    MARSHAL m = new MARSHAL("Both " + fc + " and " + interfaces[i] + " extends Remote");
                    m.minor = Minor.TargetConversion;
                    throw m;
                }
            }
        }
        if (fc == null) {
            MARSHAL m = new MARSHAL(object.getClass() + " does not implement any interface, derived from Remote");
            m.minor = Minor.TargetConversion;
            throw m;
        }
        return fc;
    }

    /**
   * Get the persistent hash code for the given class, as defined by OMG
   * standard. The inheritance, field names and types (but not the visibility)
   * are taken into consideration as well as the presence of the writeObject
   * method are taken into consideration. The class name and methods, if any,
   * are not taken into consideration.
   */
    public static long getHashCode(Class c) {
        Class of = c.isArray() ? c.getComponentType() : null;
        if (c.isArray() && ((!Serializable.class.isAssignableFrom(of) || of.isPrimitive() || Remote.class.isAssignableFrom(of)))) return 0;
        if (!Serializable.class.isAssignableFrom(c)) return 0;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bout);
            Class superClass = c.getSuperclass();
            if (superClass != null) out.writeLong(getHashCode(superClass));
            int writeObjectPresentCode;
            try {
                c.getDeclaredMethod("writeObject", new Class[] { ObjectOutputStream.class });
                writeObjectPresentCode = 2;
            } catch (NoSuchMethodException e) {
                writeObjectPresentCode = 1;
            }
            out.writeInt(writeObjectPresentCode);
            Field[] fields = c.getDeclaredFields();
            Arrays.sort(fields, new Comparator() {

                public int compare(Object a, Object b) {
                    Field fa = (Field) a;
                    Field fb = (Field) b;
                    return fa.getName().compareTo(fb.getName());
                }
            });
            Field f;
            for (int i = 0; i < fields.length; i++) {
                f = fields[i];
                if ((f.getModifiers() & NON_WRITABLE) == 0) {
                    out.writeUTF(f.getName());
                    out.writeUTF(getDescriptor(f.getType()));
                }
            }
            out.flush();
            out.close();
            MessageDigest shaDigest;
            try {
                shaDigest = MessageDigest.getInstance("SHA");
            } catch (Exception ex) {
                throw new InternalError("SHA digesting algorithm is not available");
            }
            byte[] sha = shaDigest.digest(bout.toByteArray());
            long hash = 0;
            for (int i = 0; i < Math.min(8, sha.length); i++) {
                hash += (long) (sha[i] & 255) << (i * 8);
            }
            return hash;
        } catch (IOException ioex) {
            throw new Unexpected(ioex);
        }
    }

    /**
   * Converts to hexadecimal string, supplementing leading zeros.
   */
    public static String toHex(long l) {
        StringBuffer b = new StringBuffer();
        b.append(Long.toHexString(l).toUpperCase());
        while (b.length() < 16) b.insert(0, '0');
        return b.toString();
    }

    /**
   * Returns a <code>String</code> representing the type-encoding of a class.
   */
    static String getDescriptor(Class type) {
        if (type.equals(boolean.class)) return "Z";
        if (type.equals(byte.class)) return "B";
        if (type.equals(short.class)) return "S";
        if (type.equals(char.class)) return "C";
        if (type.equals(int.class)) return "I";
        if (type.equals(long.class)) return "J";
        if (type.equals(float.class)) return "F";
        if (type.equals(double.class)) return "D";
        if (type.equals(void.class)) return "V"; else if (type.isArray()) {
            StringBuffer l = new StringBuffer("[");
            Class component = type.getComponentType();
            while (component.isArray()) {
                l.append('[');
                component = component.getComponentType();
            }
            l.append('L');
            l.append(component.getName().replace('.', '/'));
            l.append(';');
            return l.toString();
        } else return "L" + type.getName().replace('.', '/') + ';';
    }

    public static Field[] getWritableFields(Class c) {
        TreeSet set = new TreeSet(new Comparator() {

            public int compare(Object a, Object b) {
                return ((Field) a).getName().compareTo(((Field) b).getName());
            }
        });
        while (!c.equals(Object.class)) {
            Field[] f = c.getDeclaredFields();
            for (int i = 0; i < f.length; i++) {
                if ((f[i].getModifiers() & NON_WRITABLE) == 0) {
                    f[i].setAccessible(true);
                    set.add(f[i]);
                }
            }
            c = c.getSuperclass();
        }
        Field[] r = new Field[set.size()];
        int p = 0;
        Iterator it = set.iterator();
        while (it.hasNext()) {
            r[p++] = (Field) it.next();
        }
        return r;
    }

    /**
   * The method is called for Remotes that are not Stubs. It is assumed, that
   * the Remote is an implementation. The method searches for the suitable tie
   * and, if found, exports it by creating and connecting the stub. Such export
   * is supported since jdk 1.5.
   */
    void exportTie(org.omg.CORBA_2_3.portable.OutputStream output, Object implementation, Class interfaceClass) {
        try {
            Tie t = Util.getTie((Remote) implementation);
            if (t instanceof Servant) {
                POA rootPoa = POAHelper.narrow(output.orb().resolve_initial_references("RootPOA"));
                org.omg.CORBA.Object co = rootPoa.servant_to_reference((Servant) t);
                Stub stub = (Stub) PortableRemoteObject.narrow(co, interfaceClass);
                writeRemoteObject(output, stub);
                if (rootPoa.the_POAManager().get_state().value() == State._HOLDING) rootPoa.the_POAManager().activate();
            } else if (t instanceof org.omg.CORBA.Object) {
                org.omg.CORBA.Object co = (org.omg.CORBA.Object) t;
                output.orb().connect(co);
                Stub stub = (Stub) PortableRemoteObject.narrow(co, interfaceClass);
                writeRemoteObject(output, stub);
            }
        } catch (Exception ex) {
            MARSHAL m = new MARSHAL("Unable to export " + implementation);
            m.minor = Minor.TargetConversion;
            m.initCause(ex);
            throw m;
        }
    }

    /**
   * Start the ORB, if it is not already runnning.
   */
    void ensureOrbRunning(org.omg.CORBA_2_3.portable.OutputStream output) {
        if (output.orb() instanceof OrbFunctional) {
            ((OrbFunctional) output.orb()).ensureRunning();
        }
    }

    /**
   * Write data to the CORBA output stream. Writes the object contents only; the
   * header must be already written. For object, containing objects, may be
   * called recursively.
   * 
   * @param an_output a stream to write to, must be
   * org.omg.CORBA_2_3.portable.OutputStream
   * @param object an object to write.
   */
    public void writeRemoteObject(OutputStream an_output, Object object) {
        org.omg.CORBA_2_3.portable.OutputStream output = (org.omg.CORBA_2_3.portable.OutputStream) an_output;
        if (isTieRequired(object)) {
            Class fc = getExportedInterface(object);
            exportTie(output, object, fc);
        } else if (object instanceof org.omg.CORBA.Object) {
            ensureOrbRunning(output);
            an_output.write_Object((org.omg.CORBA.Object) object);
        } else if (object != null && object instanceof Serializable) writeFields(an_output, (Serializable) object);
    }

    /**
   * Write data to the CORBA output stream. Writes the object contents only; the
   * header must be already written. For object, containing objects, may be
   * called recursively.
   * 
   * @param an_output a stream to write to, must be
   * org.omg.CORBA_2_3.portable.OutputStream
   * @param object an object to write.
   */
    public void writeValue(OutputStream an_output, Serializable object) {
        org.omg.CORBA_2_3.portable.OutputStream output = (org.omg.CORBA_2_3.portable.OutputStream) an_output;
        if (isTieRequired(object)) {
            Class fc = getExportedInterface(object);
            exportTie(output, object, fc);
        } else if (object instanceof org.omg.CORBA.Object) {
            ensureOrbRunning(output);
            an_output.write_Object((org.omg.CORBA.Object) object);
        } else if (object instanceof Externalizable) {
            try {
                ObjectOutputStream stream = new CorbaOutput(output, object, this);
                stream.write(VERSION);
                ((Externalizable) object).writeExternal(stream);
            } catch (Exception ex) {
                MARSHAL m = new MARSHAL("writeExternal failed");
                m.minor = Minor.Value;
                m.initCause(ex);
                throw m;
            }
        } else if (object instanceof Serializable) {
            Object mode = null;
            synchronized (io_format) {
                mode = io_format.get(object.getClass());
                if (mode == STANDARD) {
                    writeFields(an_output, (Serializable) object);
                    return;
                }
            }
            try {
                Method m = object.getClass().getDeclaredMethod("writeObject", WRITE_OBJECT_ARGS);
                m.setAccessible(true);
                try {
                    ObjectOutputStream stream = new CorbaOutput(output, object, this);
                    stream.write(VERSION);
                    if (mode == CUSTOM_DWO) stream.write(1); else if (mode == CUSTOM_NO_DWO) stream.write(0); else {
                        DefaultWriteObjectTester tester = new DefaultWriteObjectTester(object);
                        m.invoke(object, new Object[] { tester });
                        synchronized (io_format) {
                            io_format.put(object.getClass(), tester.dwo_called ? CUSTOM_DWO : CUSTOM_NO_DWO);
                            stream.write(tester.dwo_called ? 1 : 0);
                        }
                    }
                    m.invoke(object, new Object[] { stream });
                    stream.flush();
                } catch (Exception ex) {
                    MARSHAL mx = new MARSHAL(object.getClass().getName() + ".writeObject failed");
                    mx.initCause(ex);
                    throw mx;
                }
            } catch (NoSuchMethodException e) {
                writeFields(an_output, (Serializable) object);
                synchronized (io_format) {
                    io_format.put(object.getClass(), STANDARD);
                }
            }
        }
    }

    /**
   * Read value from the input stream in the case when the value is not
   * Streamable or CustomMarshalled.
   */
    public Serializable readValue(InputStream in, int offset, Class clz, String repositoryID, RunTime sender) {
        if (in instanceof HeadlessInput) ((HeadlessInput) in).subsequentCalls = true;
        gnuRuntime g;
        Serializable object = null;
        try {
            g = (gnuRuntime) sender;
            object = g.target;
        } catch (ClassCastException e) {
            g = null;
        }
        org.omg.CORBA_2_3.portable.InputStream input = (org.omg.CORBA_2_3.portable.InputStream) in;
        if (Remote.class.isAssignableFrom(clz) || ValueBase.class.isAssignableFrom(clz)) {
            if (clz.isInterface()) try {
                clz = Util.loadClass(PortableRemoteObjectDelegateImpl.getStubClassName(clz.getName()), null, clz.getClassLoader());
            } catch (ClassNotFoundException e) {
                MARSHAL m = new MARSHAL("Cannot get stub from interface " + clz.getClass().getName());
                m.minor = Minor.TargetConversion;
                m.initCause(e);
                throw m;
            }
            if (ObjectImpl.class.isAssignableFrom(clz)) {
                Object ro = input.read_Object();
                ObjectImpl obj = (ObjectImpl) ro;
                if (obj == null) return null;
                Delegate delegate = obj._get_delegate();
                object = instantiate(offset, clz, g);
                ((ObjectImpl) object)._set_delegate(delegate);
            }
        } else if (org.omg.CORBA.Object.class.isAssignableFrom(clz)) object = (Serializable) input.read_Object();
        if (object == null) object = instantiate(offset, clz, g);
        if (object instanceof ObjectImpl) return object;
        if (object instanceof Externalizable) {
            try {
                CorbaInput stream = new CorbaInput(input, object, this, offset, repositoryID, g);
                byte version = stream.readByte();
                if (version != 1) throw new MARSHAL("Unsuported RMI-IIOP version " + version);
                ((Externalizable) object).readExternal(stream);
            } catch (Exception ex) {
                MARSHAL m = new MARSHAL("readExternal failed");
                m.initCause(ex);
                throw m;
            }
        } else {
            Object mode = null;
            synchronized (io_format) {
                mode = io_format.get(object.getClass());
            }
            if (mode == STANDARD) {
                readFields(offset, repositoryID, object, input, g);
            } else {
                try {
                    Method m = object.getClass().getDeclaredMethod("readObject", READ_OBJECT_ARGS);
                    try {
                        m.setAccessible(true);
                        CorbaInput stream = new CorbaInput(input, object, this, offset, repositoryID, g);
                        byte version = stream.readByte();
                        if (version != 1) throw new MARSHAL("Unsuported RMI-IIOP version " + version);
                        boolean dwo = stream.readByte() != 0;
                        m.invoke(object, new Object[] { stream });
                        synchronized (io_format) {
                            io_format.put(object.getClass(), dwo ? CUSTOM_DWO : CUSTOM_NO_DWO);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MARSHAL mx = new MARSHAL(object.getClass().getName() + ".readObject failed");
                        mx.initCause(ex);
                        throw mx;
                    }
                } catch (NoSuchMethodException e) {
                    synchronized (io_format) {
                        io_format.put(object.getClass(), STANDARD);
                        readFields(offset, repositoryID, object, input, g);
                    }
                }
            }
        }
        return object;
    }

    /**
   * Create an instance.
   */
    Serializable instantiate(int offset, Class clz, gnuRuntime g) throws MARSHAL {
        Serializable object;
        try {
            object = (Serializable) Vio.instantiateAnyWay(clz);
            g.objectWritten(object, offset);
        } catch (Exception e) {
            MARSHAL m = new MARSHAL("Unable to instantiate " + clz);
            m.minor = Minor.Instantiation;
            m.initCause(e);
            throw m;
        }
        return object;
    }

    /**
   * Read fields of the object.
   */
    void readFields(int offset, String repositoryID, Serializable object, org.omg.CORBA_2_3.portable.InputStream input, gnuRuntime r) throws MARSHAL {
        Field f = null;
        Class o_class = object.getClass();
        try {
            Field[] fields = getWritableFields(o_class);
            Class fc;
            for (int i = 0; i < fields.length; i++) {
                if (input instanceof HeadlessInput) ((HeadlessInput) input).subsequentCalls = true;
                f = fields[i];
                fc = f.getType();
                Object v;
                if (fc == String.class) {
                    v = input.read_value(wStringValueHelper);
                } else if (fc == int.class) v = new Integer(input.read_long()); else if (fc == long.class) v = new Long(input.read_longlong()); else if (fc == double.class) v = new Double(input.read_double()); else if (fc == float.class) v = new Float(input.read_float()); else if (fc == boolean.class) v = input.read_boolean() ? Boolean.TRUE : Boolean.FALSE; else if (fc == short.class) v = new Short(input.read_short()); else if (fc == byte.class) v = new Byte(input.read_octet()); else if (fc == char.class) v = new Character(input.read_char()); else if (org.omg.CORBA.Object.class.isAssignableFrom(fc) || Remote.class.isAssignableFrom(fc)) {
                    v = readValue(input, offset, fc, null, r);
                } else {
                    v = Vio.read(input, fc);
                }
                f.set(object, v);
            }
        } catch (Exception ex) {
            MARSHAL m = new MARSHAL("Cannot read " + o_class.getName() + " field " + f);
            m.initCause(ex);
            m.minor = Minor.ValueFields;
            throw m;
        }
    }
}
