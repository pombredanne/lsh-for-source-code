package info.metlos.jcdc.plugins;

import info.metlos.jcdc.config.ConfigurationException;
import info.metlos.jcdc.config.ConfigurationSection;
import info.metlos.jcdc.config.Configurator;
import info.metlos.jcdc.config.IVisuallyConfigurable;
import info.metlos.jcdc.config.extensionpoints.ConfigurationExtensionPoint;
import info.metlos.jcdc.config.ui.HublistsPluginConfigurationUi;
import info.metlos.plugin.IExtensionPoint;
import info.metlos.plugin.InstanceCreator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.trolltech.qt.gui.QWidget;

/**
 * This is a default hublists plugin.
 * 
 * @author metlos
 * 
 * @version $Id: HubListsPlugin.java 238 2008-09-28 17:43:23Z metlos $
 */
public class HubListsPlugin implements IHubListsPlugin, IVisuallyConfigurable {

    private static final Logger logger = LogManager.getLogger(HubListsPlugin.class);

    private static final String id = "info.metlos.jcdc.plugins.HubListsPlugin";

    private static HubListsPlugin me = new HubListsPlugin();

    private HubListsPlugin() {
        interest = new HashSet<String>();
        interest.add(ConfigurationExtensionPoint.getInstance().getId());
        hublists = new LinkedHashSet<URI>();
        File f = new File(localCacheDir);
        if (!f.exists()) {
            f.mkdir();
        }
    }

    private final Set<String> interest;

    private final Set<URI> hublists;

    private boolean cacheLocally = false;

    private String localCacheDir = Configurator.getApplicationDataDirectory() + File.separator + "hublist-cache";

    /**
	 * This is a special class that handles "download" of a locally cached file.
	 * It just returns the destination file straight away.
	 * 
	 * @author metlos
	 * 
	 */
    private static class CachedHubListDownloadTracker extends HubListDownloadTracker {

        private final int max;

        public CachedHubListDownloadTracker(String dest) throws IOException {
            super("file:///", dest);
            File f = new File(dest);
            max = (int) f.length();
        }

        @Override
        public String getResult() {
            return dest;
        }

        @Override
        public void run() {
            notifyListeners(max);
        }

        @Override
        public int getMaximum() {
            return max;
        }
    }

    @InstanceCreator
    public static HubListsPlugin getInstance() {
        return me;
    }

    public QWidget createControl(QWidget parent) {
        return new HublistsPluginConfigurationUi(parent, this);
    }

    public ConfigurationSection getSection() {
        return ConfigurationSection.getFromPath("hublists");
    }

    public void configure(Element configurationData) throws ConfigurationException {
        hublists.clear();
        cacheLocally = Boolean.valueOf(configurationData.getAttribute("cache"));
        localCacheDir = configurationData.getAttribute("cache-dir");
        loadHublists(configurationData);
    }

    private void loadHublists(Node fromNode) throws ConfigurationException {
        NodeList nodes = fromNode.getChildNodes();
        Node n = nodes.item(0);
        while (n != null) {
            if ("item".equals(n.getNodeName())) {
                Node urlAttr = n.getAttributes().getNamedItem("url");
                if (urlAttr == null) {
                    throw new ConfigurationException("Missing url attribute for hublists item.");
                }
                String urlText = urlAttr.getNodeValue();
                URI uri = null;
                try {
                    uri = new URI(urlText);
                    hublists.add(uri);
                } catch (URISyntaxException e) {
                }
            }
            n = n.getNextSibling();
        }
    }

    public String getId() {
        return id;
    }

    public void saveConfiguration(Element configurationNode) {
        Document doc = configurationNode.getOwnerDocument();
        configurationNode.setAttribute("cache", Boolean.toString(cacheLocally));
        configurationNode.setAttribute("cache-dir", localCacheDir);
        for (URI uri : this.getHubLists()) {
            Element el = doc.createElement("item");
            el.setAttribute("url", uri.toString());
            configurationNode.appendChild(el);
        }
    }

    public Set<String> getInterest() {
        return interest;
    }

    public void initialize(Set<IExtensionPoint> extensionPoints) {
    }

    public Set<IExtensionPoint> offers() {
        return null;
    }

    public Set<URI> getHubLists() {
        return hublists;
    }

    public HubListDownloadTracker getFileNameForHubListUri(URI uri) {
        File f = null;
        try {
            if (this.cacheLocally) {
                f = new File(localCacheDir + "/" + getCacheFileName(uri));
                if (f.exists()) {
                    return new CachedHubListDownloadTracker(f.getAbsolutePath());
                }
            } else {
                f = File.createTempFile("jcdc_hublist", null);
            }
            return new HubListDownloadTracker(new URL(uri.toString()), f.getAbsolutePath());
        } catch (IllegalStateException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to create MD5 digest. Not caching.", e);
            }
            try {
                f = File.createTempFile("hublist", null);
            } catch (IOException e2) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to create temp file after failing to create MD5 digest.", e2);
                }
                return null;
            }
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to create temp file.", e);
            }
            return null;
        }
        return null;
    }

    public boolean isCachingLocally() {
        return cacheLocally;
    }

    public void setCachingLocally(boolean cacheLocally) {
        this.cacheLocally = cacheLocally;
    }

    public void clearCache() {
        File dir = new File(localCacheDir);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (!files[i].delete()) {
                    logger.warn("Failed to delete cached hublist file " + files[i].getName());
                }
            }
        }
    }

    public void removeFromCache(URI uri) {
        File f = new File(localCacheDir + "/" + getCacheFileName(uri));
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Failed to delete cached hublist file " + f.getName());
            }
        }
    }

    /**
	 * Generates the filename to use to save the hublist from given url
	 * 
	 * @param url
	 * @return
	 * @throws IllegalStateException
	 *             if we cannot obtain MD5 hasher from JRE that we could use to
	 *             create the filename.
	 */
    private String getCacheFileName(URI uri) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = uri.toString().getBytes();
            bytes = md.digest(bytes);
            String fn = "";
            for (int i = 0; i < 16; i++) {
                fn += String.format("%02x", bytes[i]);
            }
            return fn;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to create MD5 message digest object.", e);
        }
    }
}
