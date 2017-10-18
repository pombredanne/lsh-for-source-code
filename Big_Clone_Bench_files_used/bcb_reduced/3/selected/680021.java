package org.apache.harmony.tools.serialver;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.swing.JFrame;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.harmony.tools.ClassProvider;

/**
 * This is a tool that calculates the serialVersionUID of a serializable class.
 */
public class Main {

    public static void main(String args[]) {
        if (args.length < 1) {
            usage();
            System.exit(1);
        }
        String pathSep = null;
        try {
            pathSep = System.getProperty("path.separator");
        } catch (SecurityException e) {
        }
        Vector<String> names = new Vector<String>();
        Map options = new HashMap();
        int i = 0;
        while (i < args.length) {
            if (args[i].equals("-classpath")) {
                try {
                    String path = (String) args[i + 1];
                    if (pathSep != null) {
                        String prevPath = (String) options.get(args[i]);
                        if (prevPath != null) {
                            path = prevPath + pathSep + path;
                        }
                    }
                    options.put(args[i], path);
                    i++;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    System.out.println("Missing argument for -classpath option");
                    usage();
                    System.exit(1);
                }
            } else if (args[i].equals("-show")) {
                options.put(args[i], Boolean.valueOf(true));
            } else if (args[i].charAt(0) == '-') {
                System.out.println("Invalid flag " + args[i]);
                usage();
                System.exit(1);
            } else {
                names.add(args[i]);
            }
            i++;
        }
        int runValue = run(options, names);
        if (runValue != -1) System.exit(runValue);
    }

    /**
     * Runs a tool.
     *
     * @param options
     *            - a <code>java.util.Map</code> of the following key-value
     *            pairs.
     *
     *            <li><i>key</i> - "-classpath" <li><i>value</i> - a
     *            <code>java.lang.String</code> which is a path where classes
     *            are located.
     *
     *            <li><i>key</i> - "-show" <li><i>value</i> - display a simple
     *            GUI with a textfield, where the user can enter the name of the
     *            class he wants to generate the serialversionUID.
     *
     * @param classNames
     *            - a vector of the fully qualified class names.
     * @return <code>0</code> if there is no error; <code>1</code> if there is
     *         error; <code>-1</code> if the runtime has started a GUI.
     */
    public static int run(Map options, Vector<String> classNames) {
        String classpath = getString(options, "-classpath");
        boolean show = getBoolean(options, "-show");
        Iterator namesIter = classNames.iterator();
        ClassProvider classProvider = new ClassProvider(null, classpath, false);
        if ((namesIter.hasNext() == true) && (show == true)) {
            System.out.println("Cannot specify class arguments with the -show option");
            return 1;
        }
        if (show == true) {
            ShowGui gui = new ShowGui(classProvider);
            gui.setSize(600, 120);
            gui.setVisible(true);
            gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            return -1;
        } else {
            return cliRun(classProvider, namesIter);
        }
    }

    /**
     * Cli run of the serialver tool
     *
     * @param classProvider
     *            - a helper that provides the class information from a classpath.
     * @param namesIter
     *            - the list of names to check the serialversionUID.
     * @return 0 if everything worked fine<br/>
     *         1 if it has encountered errors while running
     */
    public static int cliRun(ClassProvider classProvider, Iterator namesIter) {
        int runValue = 0;
        while (namesIter.hasNext()) {
            String className = (String) namesIter.next();
            try {
                Clazz clazz = new Clazz(classProvider, className);
                if (!isSerializable(clazz)) {
                    System.out.println("Class " + clazz.getName() + " is not Serializable.");
                    continue;
                }
                long hash = calculeSUID(clazz);
                printResult(clazz, hash);
            } catch (Exception e) {
                System.out.println("Class " + className + " not found.");
                runValue = 1;
            }
        }
        return runValue;
    }

    /**
     * Calculates the serialversionUID for a given {@link Clazz}.
     *
     * @param clazz
     *            - wrapped class
     * @return the serialversionUID
     * @throws ClassNotFoundException
     *             if a class is not found
     */
    public static long calculeSUID(Clazz clazz) throws ClassNotFoundException {
        try {
            if (clazz.hasSerialVersionUID()) {
                return clazz.getExistantSerialVersionUID();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.writeUTF(clazz.getName());
            int classMask = Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT;
            daos.writeInt(clazz.getModifiers() & classMask);
            for (String clazzInterface : clazz.getSortedInterfaces()) {
                daos.writeUTF(clazzInterface);
            }
            int fieldMask = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT;
            for (Field field : clazz.getSortedValidFields()) {
                daos.writeUTF(field.getName());
                daos.writeInt(field.getModifiers() & fieldMask);
                daos.writeUTF(field.getSignature());
            }
            int methodMask = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT;
            for (Method constructor : clazz.getSortedValidConstructors()) {
                daos.writeUTF(constructor.getName());
                daos.writeInt(constructor.getModifiers() & methodMask);
                daos.writeUTF(constructor.getSignature());
            }
            for (Method methods : clazz.getSortedValidMethods()) {
                daos.writeUTF(methods.getName());
                daos.writeInt(methods.getModifiers() & methodMask);
                daos.writeUTF(methods.getSignature().replace('/', '.'));
            }
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] sha = md.digest(baos.toByteArray());
            long hash = 0;
            for (int i = 7; i >= 0; i--) {
                hash = (hash << 8) | (sha[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            throw new ClassNotFoundException();
        }
    }

    /**
     * Verify if the wrapped class is serializable.
     *
     * @return true - if it's serializable<br />
     *         false otherwise.
     * @throws ClassNotFoundException
     *             if the wrappedclass isn't found.
     */
    public static boolean isSerializable(Clazz wrappedclazz) throws ClassNotFoundException {
        return wrappedclazz.isSerializable();
    }

    /**
     * Prints the usage information.
     */
    public static void usage() {
        System.out.println("Use: serialver [-classpath classpath] [-show] [classname...]");
    }

    /**
     * Prints the serialversionUID of a given class.
     *
     * @param clazz
     *            the class
     * @param suid
     *            the value to be printed
     */
    public static void printResult(Clazz clazz, long suid) {
        System.out.println(clazz.getName() + "\t" + "static final long serialVersionUID = " + suid + "L;");
    }

    /**
     * Get the options from the Map, string options (-classpath <i>path</i>).
     *
     * @param options
     *            map.
     * @param name
     *            option.
     * @return value of the option.
     */
    private static String getString(Map options, String name) {
        try {
            return (String) options.get(name);
        } catch (ClassCastException e) {
            throw new RuntimeException("'" + name + "': expected java.lang.String", e);
        }
    }

    /**
     * Get the options from the Map, boolean options (<i>-show</i>).
     *
     * @param options
     *            map.
     * @param name
     *            option.
     * @return true if option was passed as arg<br />
     *         false otherwise.
     */
    private static boolean getBoolean(Map options, String name) {
        try {
            Object value = options.get(name);
            if (value != null) {
                return ((Boolean) value).booleanValue();
            }
        } catch (ClassCastException e) {
            throw new RuntimeException("'" + name + "': expected java.lang.Boolean", e);
        }
        return false;
    }
}
