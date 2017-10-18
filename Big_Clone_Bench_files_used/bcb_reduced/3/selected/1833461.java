package com.volantis.charset.xml;

import com.volantis.charset.configuration.Alias;
import com.volantis.charset.configuration.Charset;
import com.volantis.charset.configuration.Charsets;
import com.volantis.charset.configuration.xml.CharsetDigesterDriver;
import com.volantis.xml.xerces.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import junit.framework.*;
import org.apache.log4j.Category;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import our.apache.commons.digester.*;

/**
 *
 * @author mat                       
 */
public class CharsetDigesterDriverTestCase extends TestCase {

    public CharsetDigesterDriverTestCase(java.lang.String testName) {
        super(testName);
    }

    /** Test of digest method, of class com.volantis.charset.configuration.xml.DigesterDriver. */
    public void testDigest() {
        CharsetDigesterDriver dd = new CharsetDigesterDriver();
        Charsets css = dd.digest();
        assertNotNull("No charsets digested", css);
        ArrayList charset = css.getCharsets();
        assertNotNull("No charset information", charset);
        Charset UTF8 = new Charset();
        UTF8.setName("utf-8");
        UTF8.setMIBenum(106);
        UTF8.setPreload(true);
        UTF8.setComplete(true);
        int utf8Index = charset.indexOf(UTF8);
        Charset cs = (Charset) charset.get(utf8Index);
        assertNotNull("No utf8 charset in map", cs);
        assertEquals("UTF-8 charset not correctly configured", cs.getName(), "utf-8");
        assertEquals("MIBenum not correct", cs.getMIBenum(), 106);
        assertTrue("Complete attribute is false", cs.isComplete());
        assertTrue("Preload attribute is false", cs.isPreload());
        ArrayList aliases = cs.getAlias();
        assertNotNull("No aliases found", aliases);
        Alias UTF8Alias = new Alias();
        UTF8Alias.setName("UTF8");
        int utf8AliasIndex = aliases.indexOf(UTF8Alias);
        Alias a = (Alias) aliases.get(utf8AliasIndex);
        assertNotNull("No aliases found", a);
        assertEquals("Incorrect alias in list", a.getName(), "utf8");
    }
}
