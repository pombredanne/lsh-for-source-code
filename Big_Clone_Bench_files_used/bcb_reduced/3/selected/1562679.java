package net.sourceforge.blogentis.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sourceforge.blogentis.storage.FileResourceFilter.AllFilesFilter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.Turbine;
import org.apache.turbine.services.mimetype.TurbineMimeTypes;
import com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException;

/**
 * Implementation of an AbstractFileResource using local files.
 * 
 * @author abas
 */
public class LocalFileResource implements AbstractFileResource {

    private static final Log log = LogFactory.getLog(LocalFileResource.class);

    private static String currentFileHash;

    public static List getAllFiles(String path, FileResourceFilter filter) {
        List l = new ArrayList(10);
        File file = new File(Turbine.getRealPath("/templates/" + path));
        if (!file.isDirectory()) return l;
        String[] paths = file.list();
        if (path.equals("/")) path = "";
        for (int i = 0; i < paths.length; i++) {
            if ("CVS".equals(paths[i])) continue;
            String f = path + "/" + paths[i];
            if (filter != null && filter.isIgnored(f)) continue;
            l.add(f);
            if (filter == null || filter.descendInto(f)) l.addAll(getAllFiles(f, filter));
        }
        return l;
    }

    private static synchronized void calcLocalFileHash() {
        long startTime = System.currentTimeMillis();
        if (currentFileHash != null) return;
        List fileList = getAllFiles("/", new AllFilesFilter());
        int len = 0;
        byte[] buf = new byte[1024];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            for (Iterator i = fileList.iterator(); i.hasNext(); ) {
                String path = (String) i.next();
                LocalFileResource lfr = new LocalFileResource(path);
                if (lfr.isDirectory()) {
                    digest.update(path.getBytes("UTF-8"));
                    continue;
                }
                InputStream stream = lfr.getFileAsInputStream();
                while ((len = stream.read(buf)) != -1) {
                    digest.update(buf, 0, len);
                }
                stream.close();
            }
            currentFileHash = new String(Hex.encodeHex(digest.digest()));
        } catch (Exception e) {
            log.error("No SHA found ...?", e);
            currentFileHash = "unknown" + System.currentTimeMillis();
        } finally {
            if (log.isDebugEnabled()) log.debug("Needed " + (System.currentTimeMillis() - startTime) + "ms for hash calculation");
        }
    }

    public static String getLocalFileHash() {
        if (currentFileHash == null) calcLocalFileHash();
        return currentFileHash;
    }

    protected File localFile;

    protected String originalPath;

    public LocalFileResource(String path) {
        if (!path.startsWith("/")) path = "/" + path;
        localFile = new File(Turbine.getRealPath("/templates/" + path));
        originalPath = path;
    }

    public String getPath() {
        return originalPath;
    }

    public String getName() {
        return localFile.getName();
    }

    public int getSize() {
        return (int) localFile.length();
    }

    public Date getLastModified() {
        return new Date(localFile.lastModified());
    }

    public String getMimeType() {
        return TurbineMimeTypes.getContentType(localFile);
    }

    public InputStream getFileAsInputStream() throws FileNotFoundException {
        return new FileInputStream(localFile);
    }

    public String getFileAsString() throws FileNotFoundException {
        try {
            return IOUtils.toString(new FileInputStream(localFile), "UTF-8");
        } catch (MalformedByteSequenceException e) {
            String s = "[this file is not a text file: " + localFile.getAbsolutePath() + "]";
            log.error(s, e);
            return s;
        } catch (IOException e) {
            String s = "Could not open for reading " + localFile.getAbsolutePath();
            log.error(s, e);
            return s;
        }
    }

    public boolean exists() {
        return localFile.exists();
    }

    public boolean isOriginal() {
        return true;
    }

    public boolean isDirectory() {
        return localFile.isDirectory();
    }

    public Iterator getProperties() {
        return properties.keySet().iterator();
    }

    public String getProperty(String propertyName) {
        return (String) properties.get(propertyName);
    }

    public void setProperty(String propertyName, String propertyContents) {
        properties.put(propertyName, propertyContents);
    }

    private Map properties = new HashMap();
}
