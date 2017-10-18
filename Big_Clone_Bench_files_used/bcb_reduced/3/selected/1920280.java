package net.sf.warpcore.cms.webfrontend.git;

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.rmi.server.UID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Category;
import org.apache.log4j.Priority;

/**
 * The actual class for attribute objects storage.
 */
public class GitElement {

    private static Category category = Category.getInstance(GitElement.class.getName());

    private String _id;

    private HashMap _attributes = new HashMap();

    private GitComponent _parent;

    GitElement(Random random, GitComponent parent) throws NoSuchAlgorithmException {
        _parent = parent;
        UID uid = new UID();
        String tempid = "TTinitializer [" + uid.toString() + "]42[" + random.nextLong() + "]";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] msg = tempid.getBytes();
            md.update(msg);
            byte[] digest = md.digest();
            _id = "";
            String tempstr;
            byte[] c = new byte[2];
            int j;
            for (int i = 0; i < digest.length; i++) {
                j = digest[i];
                if (j < 0) j = 256 + j;
                c[0] = (byte) ((j / 16) + 48);
                if (c[0] > 57) c[0] += 7;
                c[1] = (byte) ((j % 16) + 48);
                if (c[1] > 57) c[1] += 7;
                tempstr = new String(c);
                _id = _id + tempstr;
            }
        } catch (NoSuchAlgorithmException e) {
            if (category.isEnabledFor(Priority.ERROR)) category.error(e);
            throw new NoSuchAlgorithmException("Could not find hash algorithm SHA-1.");
        }
    }

    /**
 * Return the unique ID of this component.
 *
 * @return		The ID that this element can be found with;
 *			It is guaranteed to be clean for URLs.
 */
    public String getId() {
        return _id;
    }

    /**
 * Store an attribute object.
 *
 * @param		String The key under with this object should be found.
 * @param		Object The object to be stored.
 */
    public void addAttribute(String key, Object o) {
        _attributes.put(key, o);
    }

    /**
 * Retrieve an attribute object by key.
 *
 * @param		String The key under which this object was stored.
 * @return		The retrieved attribute object or null if it
 *			could not be found.
 */
    public Object getAttribute(String key) {
        return _attributes.get(key);
    }

    /**
 * Invalidate this element.
 * This is intended to prevent reloads of the servlet if it should not
 * be possible to launch it multiple times from the same link either
 * by accident or maliciously. Only do this if you are sure what you
 * are doing. You can not invalidate the session global element this way,
 * instead you will have to invalidate the session.
 */
    public void invalidate() {
        _parent.removeElement(this);
    }

    /**
 * Get a set of all keys in this element.
 *
 * @return		A Set of String objects
 */
    public Set getKeys() {
        return _attributes.keySet();
    }
}
