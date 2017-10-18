package gov.nist.atlas.impl;

import gov.nist.atlas.ATLASClass;
import gov.nist.atlas.Id;
import gov.nist.atlas.type.ATLASType;
import gov.nist.atlas.util.DigestUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version $Revision: 1.21 $
 * @author Christophe Laprun
 */
class IdFactoryImpl {

    public IdFactoryImpl() {
        List idClasses = ATLASClass.getIdentifiableClasses();
        int length = idClasses.size();
        int adjusted = (length % 2 == 0) ? length + 1 : length;
        class2Infos = new HashMap(adjusted);
        prefix2Class = new HashMap(adjusted);
        ATLASClass c = null;
        String prefix = null;
        for (int i = 0; i < length; i++) {
            c = (ATLASClass) idClasses.get(i);
            prefix = createPrefixFor(c.getName());
            class2Infos.put(c, new IdInfo(0, prefix));
            prefix2Class.put(prefix, c);
        }
        idClasses = null;
    }

    private String getPrefixFor(String stringId) {
        MATCHER.reset(stringId);
        if (MATCHER.matches()) return MATCHER.group(1);
        return null;
    }

    private String createPrefixFor(String className) {
        return className.substring(0, 3);
    }

    protected Id createNewIdFor(ATLASType type) {
        ATLASClass clazz = type.getATLASClass();
        if (clazz.equals(ATLASClass.CORPUS)) return createGUID();
        IdInfo info = null;
        if (class2Infos.containsKey(clazz)) {
            info = (IdInfo) class2Infos.get(clazz);
            info.counter++;
            return createNewIdFor(info.toString());
        }
        return createNewIdFor(DEFAULT_PREFIX + DEFAULT_COUNTER);
    }

    protected Id createNewIdFor(String stringId) {
        ATLASClass atlasClass = getATLASClassFor(getPrefixFor(stringId));
        Id id = resolveIdFor(stringId);
        if (id != null) return id;
        updateIdInfoIfNeeded(stringId, atlasClass);
        return createAndAddId(stringId);
    }

    private Id createAndAddId(String stringId) {
        Id id = new IdImpl(stringId);
        addId(id);
        return id;
    }

    protected Id createGUID() {
        StringBuffer guid = new StringBuffer(128);
        try {
            long time = System.currentTimeMillis();
            guid.append(HOST_ID);
            guid.append(":");
            guid.append(Long.toString(time));
            guid.append(":");
            guid.append(Long.toString(CURRENT_INDEX++));
            byte[] array = DigestUtil.digest(guid.toString().getBytes());
            return createAndAddId(DigestUtil.asHexString(array));
        } catch (Exception e) {
            System.out.println("Error:" + e);
        }
        return null;
    }

    private boolean updateIdInfoIfNeeded(String stringId, ATLASClass atlasClass) {
        IdInfo info = (IdInfo) class2Infos.get(atlasClass);
        if (info == null) return false;
        int suffix = Integer.parseInt(stringId.substring(PREFIX_LENGTH));
        int max = Math.max(info.counter, suffix);
        info.counter = max++;
        return true;
    }

    private ATLASClass getATLASClassFor(String prefix) {
        return (ATLASClass) prefix2Class.get(prefix);
    }

    /**
   * Gets the Id object from this factory's internal repository
   * for a given name
   *
   * @param stringId the Id as a string
   * @return the Id for the specified name
   * @since 2.0 Beta 6 Moved from IdFactory
   */
    public final Id resolveIdFor(String stringId) {
        return (Id) ids.get(stringId);
    }

    /**
   * Adds an Id to this factory's internal repository
   *
   * @param id The Id to add
   * @since 2.0 Beta 6 Moved from IdFactory
   */
    protected final void addId(Id id) {
        ids.put(id.getAsString(), id);
    }

    private Map class2Infos;

    private Map prefix2Class;

    private static final String DEFAULT_PREFIX = "DEF";

    private static int DEFAULT_COUNTER = 1;

    private static int PREFIX_LENGTH = 3;

    private static final Pattern ATLAS_ID_PATTERN = Pattern.compile("^(\\w{3})\\d*");

    private static final Matcher MATCHER = ATLAS_ID_PATTERN.matcher("");

    private static long CURRENT_INDEX = new Random(System.currentTimeMillis()).nextLong();

    private static final String HOST_ID;

    /** The map which contains all the existing pair name/Id */
    private Map ids = new HashMap();

    static {
        try {
            HOST_ID = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Couldn't get host Id for GUID generation.", e);
        }
    }

    private static class IdInfo {

        IdInfo(int counter, String prefix) {
            this.counter = counter;
            this.prefix = prefix;
        }

        /**
     * Two IdInfo are equal if their prefix and counter are are equal.
     */
        public boolean equals(Object other) {
            if (other == this) return true;
            if (other instanceof IdInfo) {
                IdInfo idInfo = (IdInfo) other;
                return idInfo.prefix.equals(prefix) && idInfo.counter == counter;
            }
            return false;
        }

        public String toString() {
            return prefix + counter;
        }

        public int setCounter(int newValue) {
            int old = counter;
            counter = newValue;
            return old;
        }

        private String prefix;

        private int counter;
    }
}
