package ow.messaging;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ow.util.ClassProcessor;
import ow.util.ClassTraverser;

public final class MessageDirectory {

    private static final Logger logger = Logger.getLogger("messaging");

    public static final Color DEFAULT_COLOR = Color.GRAY;

    private static final Map<Integer, String> nameTable = new HashMap<Integer, String>();

    private static final Map<Class<? extends Message>, Integer> classToTagTable = new HashMap<Class<? extends Message>, Integer>();

    private static final Map<Integer, Class<? extends Message>> tagToClassTable = new HashMap<Integer, Class<? extends Message>>();

    private static final Map<Integer, Boolean> toBeReportedTable = new HashMap<Integer, Boolean>();

    private static final Map<Integer, Color> colorTable = new HashMap<Integer, Color>();

    private static MessageDigest md = null;

    private static final String mdAlgoName = "SHA1";

    static {
        try {
            md = MessageDigest.getInstance(mdAlgoName);
        } catch (NoSuchAlgorithmException e) {
        }
        Class[] rootClasses = { ow.routing.RoutingServiceFactory.class, ow.routing.RoutingAlgorithmFactory.class, ow.dht.DHTFactory.class, ow.mcast.McastFactory.class, ow.stat.StatFactory.class };
        MessageDirectory.registerMessageReferredBy(rootClasses);
    }

    private static void registerMessageReferredBy(Class[] clazzes) {
        ClassProcessor proc = new ClassProcessor() {

            public void process(String className) {
                if (className.endsWith("Message")) {
                    try {
                        registerMessage(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        };
        String[] classNameArray = new String[clazzes.length];
        int i = 0;
        for (Class clazz : clazzes) {
            classNameArray[i++] = clazz.getName();
        }
        ClassTraverser traverser = new ClassTraverser("^ow\\.", proc);
        traverser.traversal(classNameArray);
    }

    private static void registerMessage(Class clazz) {
        if (classToTagTable.containsKey(clazz) || !(Message.class.isAssignableFrom(clazz)) || Modifier.isAbstract(clazz.getModifiers())) return;
        String name = null;
        boolean toBeReported = false;
        Color color = null;
        try {
            Field f = clazz.getDeclaredField("NAME");
            name = (String) f.get(clazz);
        } catch (Exception e) {
        }
        try {
            Field f = clazz.getDeclaredField("TO_BE_REPORTED");
            toBeReported = f.getBoolean(clazz);
        } catch (Exception e) {
        }
        try {
            Field f = clazz.getDeclaredField("COLOR");
            color = (Color) f.get(clazz);
        } catch (Exception e) {
        }
        if (color == null) color = DEFAULT_COLOR;
        int tag = getSHA1BasedIntFromString(name);
        tag = (tag ^ (tag >>> 8) ^ (tag >>> 16) ^ (tag >>> 24)) & 0x7f;
        int repeat = 10;
        while (true) {
            String existingName = nameTable.get(tag);
            if (existingName == null) break;
            logger.log(Level.INFO, "A tag " + tag + " was duplicated for message " + name + " and " + existingName);
            if (repeat-- <= 0) System.exit(1);
            tag = (tag + 1) & 0x7f;
        }
        synchronized (MessageDirectory.class) {
            nameTable.put(tag, name);
            tagToClassTable.put(tag, clazz);
            classToTagTable.put(clazz, tag);
            toBeReportedTable.put(tag, toBeReported);
            colorTable.put(tag, color);
        }
    }

    private static int getSHA1BasedIntFromString(String input) {
        byte[] bytes = null;
        try {
            bytes = input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        byte[] value;
        synchronized (md) {
            value = md.digest(bytes);
        }
        int ret = 0;
        int i = 0;
        for (byte b : value) {
            ret ^= (b & 0xff) << (3 - i);
            if (++i > 3) i = 0;
        }
        return ret;
    }

    public static Class<? extends Message> getClassByTag(int tag) {
        return tagToClassTable.get(tag);
    }

    public static int getTagByClass(Class<? extends Message> c) {
        return classToTagTable.get(c);
    }

    public static String getName(int tag) {
        return nameTable.get(tag);
    }

    static boolean getToBeReported(int tag) {
        return toBeReportedTable.get(tag);
    }

    public static Color getColor(int tag) {
        return colorTable.get(tag);
    }
}
