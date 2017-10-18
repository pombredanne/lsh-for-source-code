package nz.ng.utilities.dbmanager;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author juanjose.sanchez
 * last revision: 24/09/2010
 */
public class NzDBUtils {

    private static volatile NzDBUtils singleton;

    private NzDBUtils() {
    }

    public static NzDBUtils getSingleton() {
        if (singleton == null) {
            synchronized (NzDBUtils.class) {
                if (singleton == null) {
                    singleton = new NzDBUtils();
                }
            }
        }
        return singleton;
    }

    /**
	 * Returns the SQL type associated to the integer given.
	 * @param jdbcType the integer corresponding to a SQL type
	 * @return the SQL type as a string corresponding to the number given
	 */
    public String getJdbcTypeName(int jdbcType) {
        Field[] fields = java.sql.Types.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Integer value = (Integer) fields[i].get(null);
                if (value.equals(jdbcType)) {
                    return fields[i].getName();
                }
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            }
        }
        return null;
    }

    /**
	 * Returns the integer associated to the SQL type name given.
	 * @param jdbcType the SQL type as a string
	 * @return the integer corresponding to the SQL type given
	 */
    public int getJdbcTypeInt(String jdbcType) {
        Field[] fields = java.sql.Types.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                String value = (String) fields[i].getName();
                if (value.equals(jdbcType)) {
                    return (Integer) fields[i].get(null);
                }
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            }
        }
        return -1;
    }

    public String md5Encode(String pass) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(pass.getBytes());
        byte[] result = md.digest();
        return new String(result);
    }
}
