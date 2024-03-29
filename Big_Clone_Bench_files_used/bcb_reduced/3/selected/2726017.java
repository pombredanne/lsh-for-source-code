package com.burgess.rtd.model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.burgess.rtd.constants.API;
import com.burgess.rtd.constants.Program;

/**
 * Represents a Request string that needs to be submitted to RTM.
 */
public class Request {

    public static String ID = "_id";

    public static String QUERY = "query";

    public static String TYPE = "type";

    public static String LOCAL_ID = "local_id";

    public static String CREATED = "created";

    public static String SYNCED = "synced";

    public static String TABLE = "requests";

    public static String CREATE = "create table requests (" + ID + " integer primary key autoincrement, " + QUERY + " text, " + TYPE + " integer, " + LOCAL_ID + " integer default null, " + CREATED + " datetime, " + SYNCED + " boolean default 0);";

    public static String DESTROY = "drop table if exists requests";

    public String url = null;

    /**
	 * Stores the Request parameters in a key/value structure
	 */
    private Hashtable<String, Object> parameters;

    public Request() {
        parameters = new Hashtable<String, Object>();
    }

    /**
	 * Constructor which uses the default JSON format
	 *
	 * @param name	Request method name
	 */
    public Request(String name) {
        this(name, "json");
    }

    /**
	 * Constructor which allows the user to pick a format name
	 *
	 * @param name		Request method name
	 * @param format	Format: json or xml
	 */
    public Request(String name, String format) {
        Log.d(Program.LOG, "Request created (" + name + ", " + format + ")");
        parameters = new Hashtable<String, Object>();
        if (!name.equals("")) {
            setParameter("method", name);
        }
        setParameter("api_key", API.API_KEY);
        setParameter("format", format);
    }

    /**
	 * Sets a parameter in the key/value structure
	 *
	 * @param key
	 * @param value
	 */
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public void removeParameter(String key) {
        parameters.remove(key);
    }

    /**
	 * Builds a request URI
	 */
    @Override
    public String toString() {
        if (url == null) {
            String ret = "";
            for (String key : parameters.keySet()) {
                ret += key + "=" + parameters.get(key).toString() + "&";
            }
            ret += "api_sig=" + signMethod();
            return ret.replace(" ", "%20");
        } else {
            return url;
        }
    }

    /**
	 * Signs the method using MD5 for the api_sig parameter
	 *
	 * @return	An MD5 encoded string for the request
	 */
    private String signMethod() {
        String str = API.SHARED_SECRET;
        Vector<String> v = new Vector<String>(parameters.keySet());
        Collections.sort(v);
        for (String key : v) {
            str += key + parameters.get(key);
        }
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(str.getBytes(), 0, str.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public void save(SQLiteDatabase db, long localId, int dataType) {
        ContentValues cv = new ContentValues();
        cv.put(Request.CREATED, Program.DATE_FORMAT.format(Calendar.getInstance().getTime()));
        cv.put(Request.LOCAL_ID, localId);
        cv.put(Request.QUERY, toString());
        cv.put(Request.SYNCED, false);
        cv.put(Request.TYPE, Program.Data.LIST);
        db.insert(Request.TABLE, null, cv);
    }

    public static Request parse(String url) {
        Request r = new Request();
        String[] pairs = url.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String[] keyvalue = pairs[i].split("=");
            r.setParameter(keyvalue[0], keyvalue[1]);
        }
        r.removeParameter("api_sig");
        return r;
    }
}
