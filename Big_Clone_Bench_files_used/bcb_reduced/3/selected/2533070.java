package com.dokumentarchiv.plugins.export;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.Plugin;
import de.inovox.AdvancedMimeMessage;
import com.dokumentarchiv.plugins.IArchive;
import com.dokumentarchiv.search.ListEntry;
import com.dokumentarchiv.search.Search;
import com.dokumentarchiv.search.SearchFunction;

/**
 * Archiving plugin that simply exports all emails to a directory
 * 
 * @author Carsten Burghardt
 * @version $Id: ExportPlugin.java 618 2008-03-12 21:56:45Z carsten $
 */
public class ExportPlugin extends Plugin implements IArchive {

    private static final long serialVersionUID = 2260952990764720008L;

    private static Log log = LogFactory.getLog(ExportPlugin.class);

    private String exportDir = null;

    /**
     * Constructor
     * @param manager
     * @param descr
     */
    public ExportPlugin() {
        super();
    }

    /**
     * Constructor for JUnit
     * @param init
     * @throws Exception
     */
    public ExportPlugin(boolean init) throws Exception {
        if (init) {
            doStart();
        }
    }

    /**
     * @see org.java.plugin.Plugin#doStart()
     */
    protected void doStart() throws Exception {
        try {
            URL configUrl = getManager().getPathResolver().resolvePath(getDescriptor(), CONFIGNAME);
            Configuration config = new PropertiesConfiguration(configUrl);
            exportDir = config.getString("directory") + File.separator;
            log.info("Using directory " + exportDir);
        } catch (ConfigurationException e) {
            log.error("Can not read properties", e);
            getManager().disablePlugin(getDescriptor());
            return;
        }
    }

    /**
     * @see org.java.plugin.Plugin#doStop()
     */
    protected void doStop() throws Exception {
    }

    /**
     * Archives the complete email
     * @see com.dokumentarchiv.core.IArchive#archiveEMail
     */
    public boolean archiveEMail(AdvancedMimeMessage msg) {
        try {
            if (messageAlreadyExported(msg)) {
                log.info("File already exists in the directory");
                return true;
            }
            String fileName = getUniqueFilename(msg);
            FileOutputStream fileout = new FileOutputStream(fileName, false);
            msg.writeTo(fileout);
            fileout.close();
            log.debug("Exported message to " + fileName);
        } catch (Exception e) {
            log.error("Error writing to export dir", e);
            return false;
        }
        return true;
    }

    /**
     * @param msg
     * @return
     */
    private boolean messageAlreadyExported(AdvancedMimeMessage msg) {
        if (msg.getSource() != null) {
            File check = new File(msg.getSource());
            if (check != null && check.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a unique filename
     * @param msg
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws NoSuchAlgorithmException
     */
    private String getUniqueFilename(MimeMessage msg) throws IOException, MessagingException, NoSuchAlgorithmException {
        byte[] bytes = msg.getContent().toString().getBytes();
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha.digest(bytes);
        return exportDir + System.currentTimeMillis() + hexEncode(digest);
    }

    /**
     * hex encode the string to make a nice string representation
     * @param aInput
     * @return hex encoded string
     */
    private String hexEncode(byte[] aInput) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

    /**
     * @see com.dokumentarchiv.core.IArchive#findDocuments
     */
    public List findDocuments(Search search) {
        File folder = new File(exportDir);
        FileFilter filter = new DateFilter(search);
        File[] emails = folder.listFiles(filter);
        List entries = new Vector();
        for (int i = 0; i < emails.length; ++i) {
            File file = emails[i];
            ListEntry entry = new ListEntry();
            entry.setDate(new Date(file.lastModified()));
            entry.setId(file.getName());
            entries.add(entry);
        }
        log.debug("Found " + entries);
        return entries;
    }

    /**
     * @see com.dokumentarchiv.core.IArchive#getDocumentByID
     */
    public InputStream getDocumentByID(String id) {
        String fileName = exportDir + id;
        try {
            InputStream is = new FileInputStream(fileName);
            log.debug("Return stream for " + id);
            return is;
        } catch (FileNotFoundException e) {
            log.error("File not found", e);
        }
        return null;
    }

    public HashMap getSupportedFunctions() {
        HashMap map = new HashMap();
        Vector parts = new Vector();
        parts.add(SearchFunction.getStringForFunction(SearchFunction.EQUALS));
        parts.add(SearchFunction.getStringForFunction(SearchFunction.EQUALSNOT));
        parts.add(SearchFunction.getStringForFunction(SearchFunction.ISGREATER));
        parts.add(SearchFunction.getStringForFunction(SearchFunction.ISGREATEROREQUAL));
        parts.add(SearchFunction.getStringForFunction(SearchFunction.ISLESS));
        parts.add(SearchFunction.getStringForFunction(SearchFunction.ISLESSOREQUAL));
        map.put(ISearchField.DATE, parts);
        return map;
    }
}
