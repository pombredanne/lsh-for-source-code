package de.shandschuh.jaolt.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import de.shandschuh.jaolt.core.apicall.AuctionCallResult;
import de.shandschuh.jaolt.core.apicall.EndAuctionCallResult;
import de.shandschuh.jaolt.core.apicall.GetProductInformationCallResult;
import de.shandschuh.jaolt.core.apicall.GetStoreCategoriesCallResult;
import de.shandschuh.jaolt.core.apicall.ImportAuctionsCallResult;
import de.shandschuh.jaolt.core.apicall.LeaveFeedbackCallResult;
import de.shandschuh.jaolt.core.apicall.SetStoreCategoriesCallResult;
import de.shandschuh.jaolt.core.apicall.SyncObjectCallResult;
import de.shandschuh.jaolt.core.apicall.TransactionCallResult;
import de.shandschuh.jaolt.core.auction.AttributeSet;
import de.shandschuh.jaolt.core.auction.AuctionType;
import de.shandschuh.jaolt.core.auction.Category;
import de.shandschuh.jaolt.core.auction.Condition;
import de.shandschuh.jaolt.core.auction.Counter;
import de.shandschuh.jaolt.core.auction.Currency;
import de.shandschuh.jaolt.core.auction.Enhancement;
import de.shandschuh.jaolt.core.auction.PaymentMethod;
import de.shandschuh.jaolt.core.auction.Picture;
import de.shandschuh.jaolt.core.auction.Price;
import de.shandschuh.jaolt.core.auction.ProductCode;
import de.shandschuh.jaolt.core.auction.ProductCodeType;
import de.shandschuh.jaolt.core.auction.ShippingService;
import de.shandschuh.jaolt.core.auction.Template;
import de.shandschuh.jaolt.core.auctionplatform.AuctionPlatformCallActionListener;
import de.shandschuh.jaolt.core.auctionplatform.AuctionPlatformFeature;
import de.shandschuh.jaolt.core.auctionplatform.StaticData;
import de.shandschuh.jaolt.core.exception.CommonException;
import de.shandschuh.jaolt.core.exception.DatabaseInitiationException;
import de.shandschuh.jaolt.core.exception.NoAuctionPlatformFoundException;
import de.shandschuh.jaolt.core.transaction.Feedback;
import de.shandschuh.jaolt.gui.Lister;
import de.shandschuh.jaolt.launcher.ApplicationLauncher;
import de.shandschuh.jaolt.tools.download.core.SimpleFileDownloader;
import de.shandschuh.jaolt.tools.log.Logger;

/**
 * This class manages all the data, an auction-platform can have and caches them.
 * 
 * To create an own auction-platform-implementation simply extend this class, put all
 * the needed other classes into a jar, set the main class to the auction-platform-class
 * and put it into the plugins/auctionplatforms/ folder.
 * 
 * 
 * @author handschuh
 *
 */
public abstract class AuctionPlatform extends PersistentObject {

    /** Standard auction-platform */
    private static AuctionPlatform standardInstance;

    /** Auth type password */
    public static final int AUTH_TYPE_PASSWORD = 1;

    /** Auth type token */
    public static final int AUTH_TYPE_TOKEN = 2;

    public static final String DATABASE_SORT_ATTRIBUTEVALUES = "jaolt.database.sort.attributevalues";

    /** A map of auction-platforms and the concering class-names*/
    private static Map<String, AuctionPlatform> instances = new HashMap<String, AuctionPlatform>();

    private XMLOutputter xmlOutputter;

    public static final String PICTUREHTML_SEPARATOR = "<!-- external pictures start here -->";

    private List<String> cookies;

    /**
	 * Returns the auction-platform-object from the class-name.
	 * 
	 * @param  className auction-platforms class-name
	 * @return auction-platform-object from the class-name
	 */
    public static AuctionPlatform getInstance(String className) {
        URLClassLoader urlClassLoader = classLoader.get(className);
        try {
            AuctionPlatform auctionPlatform = instances.get(className);
            if (auctionPlatform == null) {
                auctionPlatform = (AuctionPlatform) urlClassLoader.loadClass(className).newInstance();
                instances.put(className, auctionPlatform);
            }
            return auctionPlatform;
        } catch (Exception exception) {
            return null;
        }
    }

    public static AuctionPlatform getInstance(long id) {
        for (int n = 0, i = classLoader.size(); n < i; n++) {
            AuctionPlatform auctionPlatform = getInstance(classLoader.keySet().toArray()[n].toString());
            if (auctionPlatform.getId() == id) {
                return auctionPlatform;
            }
        }
        return null;
    }

    /**
	 * Returns all loaded auction-platforms.
	 * 
	 * @return loaded auction-platforms
	 */
    public static AuctionPlatform[] getInstances() {
        AuctionPlatform[] auctionPlatforms = new AuctionPlatform[classLoader.size()];
        for (int n = 0, i = auctionPlatforms.length; n < i; n++) {
            auctionPlatforms[n] = getInstance(classLoader.keySet().toArray()[n].toString());
        }
        return auctionPlatforms;
    }

    /**
	 * Loads the auction-platforms.
	 * 
	 * @throws NoAuctionPlatformFoundException thrown if no auction-platform was found
	 */
    public static void loadClassLoaders() throws NoAuctionPlatformFoundException {
        System.setProperty(DATABASE_SORT_ATTRIBUTEVALUES, "none");
        loadClassLoaders(Directory.AUCTIONPLATFORMS_DIR);
        if (classLoader.size() == 0) {
            throw new NoAuctionPlatformFoundException();
        }
    }

    /**
	 * Returns and eventually determines the standard auction-platform.
	 * 
	 * @return standard auction-platform
	 * @throws NoAuctionPlatformFoundException 
	 */
    public static AuctionPlatform getStandardAuctionPlatform() throws NoAuctionPlatformFoundException {
        if (standardInstance == null) {
            AuctionPlatform[] auctionPlatforms = getInstances();
            UpdateChannel updateChannel = UpdateChannel.getCurrentChannel();
            for (int n = 0, i = auctionPlatforms.length; n < i; n++) {
                standardInstance = auctionPlatforms[n];
                if (auctionPlatforms[n] != null && updateChannel.getStandardAuctionPlatformName() != null && updateChannel.getStandardAuctionPlatformName().equalsIgnoreCase(auctionPlatforms[n].getName())) {
                    return standardInstance;
                }
            }
            if (standardInstance != null) {
                return standardInstance;
            }
        } else {
            return standardInstance;
        }
        throw new NoAuctionPlatformFoundException();
    }

    /** A Map of class-names of auction-platforms and their concerning class-loaders */
    protected static Map<String, URLClassLoader> classLoader = new HashMap<String, URLClassLoader>();

    private static void loadClassLoader(URL[] urls, File file) throws Exception {
        URLClassLoader urlLoader = new URLClassLoader(urls, ApplicationLauncher.getUrlLoader());
        try {
            String className = new JarFile(file).getManifest().getMainAttributes().getValue("Main-Class").toString();
            if (className != null && className.length() > 0) {
                classLoader.put(className, urlLoader);
            }
        } catch (NullPointerException e) {
        }
    }

    private static void loadClassLoaders(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int n = 0, i = files.length; n < i; n++) {
                if (!files[n].isDirectory() && files[n].toString().endsWith(".jar")) {
                    try {
                        File auctionPlatformDir = new File(files[n].toString().substring(0, files[n].toString().lastIndexOf('.')));
                        File[] auctionPlatformFiles = auctionPlatformDir.listFiles();
                        URL[] urls = new URL[(auctionPlatformFiles != null ? auctionPlatformFiles.length : 0) + 1];
                        urls[0] = files[n].toURI().toURL();
                        for (int k = 1, l = urls.length; k < l; k++) {
                            urls[k] = auctionPlatformFiles[k - 1].toURI().toURL();
                        }
                        loadClassLoader(urls, files[n]);
                    } catch (Exception exception) {
                        Logger.log(exception);
                    }
                }
            }
        }
    }

    private AuctionPlatformFeature[] features = getFeatures();

    /**
	 * Returns the available currencies of the auction-platform-site.
	 * 
	 * @param  auctionPlatformSite auction-platform-site
	 * @return available currencies
	 */
    public Currency[] getAvailableCurrencies(AuctionPlatformSite auctionPlatformSite) {
        return new Currency[] { auctionPlatformSite.getCurrency() };
    }

    public abstract StaticData<Category> fetchSubCategories(long parentCategoryId, int level, AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    public final Vector<Category> getSubCategories(Category parentCategory, int level, AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception {
        Vector<Category> result = Database.getSubCategories(this, auctionPlatformSite, parentCategory);
        if (result == null || result.size() == 0) {
            StaticData<Category> data = fetchSubCategories(parentCategory.getId(), level, auctionPlatformSite, auctionPlatformAccount);
            Database.setCategories(this, auctionPlatformSite, parentCategory, data);
            result = data.getData();
        }
        return result;
    }

    /**
	 * Fetch the complete mapping.
	 */
    public abstract StaticData<Pair<Long, Vector<Integer>>> fetchCategoryAttributeSetIds(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    private Vector<Integer> getCategoryAttributeSetIds(long categoryId, AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception {
        Vector<Integer> result = Database.getCategoryAttributeSetIds(this, auctionPlatformSite, categoryId);
        if (result == null) {
            StaticData<Pair<Long, Vector<Integer>>> data = fetchCategoryAttributeSetIds(auctionPlatformSite, auctionPlatformAccount);
            return Database.setCategoryAttributeSetIdsAndReturnSingle(this, auctionPlatformSite, categoryId, data);
        } else {
            return result;
        }
    }

    public abstract StaticData<AttributeSet> fetchAttributeSets(Vector<Integer> attributeSetIds, AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    public final Vector<AttributeSet> getAttributeSets(long categoryId, AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception {
        Vector<Integer> attributeSetIds = getCategoryAttributeSetIds(categoryId, auctionPlatformSite, auctionPlatformAccount);
        if (attributeSetIds != null) {
            Vector<AttributeSet> result = Database.getAttributeSets(this, auctionPlatformSite, categoryId);
            Vector<Integer> missingAttributeSets;
            if (result == null) {
                missingAttributeSets = attributeSetIds;
            } else {
                missingAttributeSets = new Vector<Integer>();
                for (int id : attributeSetIds) {
                    boolean found = false;
                    for (AttributeSet attributeSet : result) {
                        if (id == attributeSet.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && !missingAttributeSets.contains(id)) {
                        missingAttributeSets.add(id);
                    }
                }
            }
            if (missingAttributeSets.size() > 0) {
                StaticData<AttributeSet> data = fetchAttributeSets(missingAttributeSets, auctionPlatformSite, auctionPlatformAccount);
                Database.setAttributeSets(this, auctionPlatformSite, categoryId, data);
                if (result == null) {
                    result = data.getData();
                } else {
                    for (AttributeSet attributeSet : data.getData()) {
                        if (!result.contains(attributeSet)) {
                            result.add(attributeSet);
                        }
                    }
                }
            }
            return result;
        } else {
            return null;
        }
    }

    public abstract StaticData<Country> fetchShipToLocations(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    public final Vector<Country> getShipToLocations(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception {
        Vector<Country> result = Database.getShipToLocations(this, auctionPlatformSite);
        if (result == null) {
            StaticData<Country> data = fetchShipToLocations(auctionPlatformSite, auctionPlatformAccount);
            Database.setShipToLocations(this, auctionPlatformSite, data);
            result = data.getData();
        }
        return result;
    }

    public abstract StaticData<PaymentMethod> fetchPaymentMethods(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    public final Vector<PaymentMethod> getPaymentMethods(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception {
        Vector<PaymentMethod> result = Database.getPaymentMethods(this, auctionPlatformSite);
        if (result == null) {
            StaticData<PaymentMethod> data = fetchPaymentMethods(auctionPlatformSite, auctionPlatformAccount);
            Database.setPaymentMethods(this, auctionPlatformSite, data);
            result = data.getData();
        }
        return result;
    }

    public abstract StaticData<ShippingService> fetchShippingServices(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    public final Vector<ShippingService> getShippingServices(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception {
        Vector<ShippingService> result = Database.getShippingServices(this, auctionPlatformSite);
        if (result == null) {
            StaticData<ShippingService> data = fetchShippingServices(auctionPlatformSite, auctionPlatformAccount);
            Database.setShippingServices(this, auctionPlatformSite, data);
            result = data.getData();
        }
        return result;
    }

    public abstract StaticData<Pair<Long, Vector<Condition>>> fetchItemConditions(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount) throws Exception;

    public final Vector<Condition> getConditions(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount, long categoryId, long alternateCategoryId) throws Exception {
        Vector<Condition> result = Database.getConditions(this, auctionPlatformSite, categoryId, alternateCategoryId);
        if (result == null) {
            StaticData<Pair<Long, Vector<Condition>>> data = fetchItemConditions(auctionPlatformSite, auctionPlatformAccount);
            Database.setConditions(this, auctionPlatformSite, data);
            result = Database.getConditions(this, auctionPlatformSite, categoryId, alternateCategoryId);
        }
        return result;
    }

    public boolean hasAuctionPlatformSite(AuctionPlatformSite auctionPlatformSite) {
        return true;
    }

    public String toString() {
        return getName();
    }

    public boolean equals(Object object) {
        return object != null ? this.getClass().getName().equals(object.getClass().getName()) : false;
    }

    public Object clone() {
        try {
            return this.getClass().newInstance();
        } catch (Exception exception) {
            Logger.log(exception);
            return null;
        }
    }

    public abstract AuctionCallResult doAuctionCall(Auction auction, Picture[] pictures, int callType, AuctionPlatformCallActionListener auctionPlatformCallActionListener, PictureUploadResult pictureUploadResult);

    public abstract AuctionType[] getAuctionTypes();

    public abstract URL getAuctionURL(Auction auction);

    public abstract int getAuthType();

    public abstract AuctionPlatformSite[] getAvailableAuctionPlatformSites();

    public abstract Enhancement[] getEnhancements();

    public abstract int getTitleLength();

    public abstract int getSubtitleLength();

    public abstract int[] getListingDurations(AuctionType auctionType);

    public abstract URL getLoginURL(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount);

    public abstract int getMaximumPictureCount();

    public abstract int getMaximumNationalShippingServices();

    public abstract int getMaximumInternationalShippingServices();

    public abstract String getName();

    public abstract TransactionCallResult getTransactions(AuctionPlatformAccount auctionPlatformAccount, Date fromDate, Date tillDate, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract ImportAuctionsCallResult importAuctions(AuctionPlatformAccount auctionPlatformAccount, Date fromDate, Date tillDate, boolean startFilter, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract GetStoreCategoriesCallResult getStoreCategories(AuctionPlatformAccount auctionPlatformAccount, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract SetStoreCategoriesCallResult setStoreCategories(AuctionPlatformAccount auctionPlatformAccount, Category categoryTree, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract GetProductInformationCallResult getProductInformation(AuctionPlatformAccount auctionPlatformAccount, AuctionPlatformSite auctionPlatformSite, int attributeSetId, ProductCode productCode, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract EndAuctionCallResult endAuction(Auction auction, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract Counter[] getCounter();

    public abstract boolean isPrivateAuctionAvailable(AuctionType auctionType);

    public abstract SyncObjectCallResult syncAuction(Auction auction, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract SyncObjectCallResult syncTransaction(Transaction transaction, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public abstract URL getCategoryURL(AuctionPlatformSite auctionPlatformSite, Category category);

    /**
	 * Returns the available price-types.
	 * 
	 * @param auctionType
	 *            auction-type
	 * @return available price-types
	 */
    public abstract Price[] getAvailablePrices(AuctionType auctionType);

    /**
	 * Returns the available product-code-types.
	 * 
	 * @return available product-code-types
	 */
    public abstract ProductCodeType[] getAvailableProductCodeTypes();

    /**
	 * Returns the maximum number of pictures that could be included as an
	 * external picture-source and are shown in the layout.
	 * 
	 * @return maximum number of pictures that could be included as an external
	 *         picture-source and are shown in the layout
	 */
    public abstract int getMaximumExternalPictureCount();

    /**
	 * Returns the unique id of the this auction-platform.
	 * 
	 * @return jAOLT-wide unique id
	 */
    public abstract long getId();

    /**
	 * Returns an array of existing non-standard-features. May be null.
	 * 
	 * @return
	 */
    public abstract AuctionPlatformFeature[] getFeatures();

    /**
	 * Returns the maximum length of th feedback comment.
	 * 
	 * @return maximum length of feedback comment
	 */
    public abstract int getMaximumFeedbackLength();

    public abstract LeaveFeedbackCallResult leaveFeedback(Transaction transaction, String text, Feedback.Type type, AuctionPlatformCallActionListener auctionPlatformCallActionListener);

    public boolean hasFeature(AuctionPlatformFeature feature) {
        for (int n = 0, i = features != null ? features.length : 0; n < i; n++) {
            if (feature == features[n]) {
                return true;
            }
        }
        return false;
    }

    public static Picture[] getPictures(URL[] uris) {
        String[] urls = new String[uris != null ? uris.length : 0];
        for (int n = 0, i = urls.length; n < i; n++) {
            urls[n] = uris[n].toString();
        }
        return getPictures(urls);
    }

    public static Picture[] getPictures(Picture[] pictures) {
        String[] urls = new String[pictures != null ? pictures.length : 0];
        for (int n = 0, i = urls.length; n < i; n++) {
            urls[n] = pictures[n].getURL() + "";
        }
        return getPictures(urls);
    }

    public static Picture[] getPictures(String[] urls) {
        if (urls != null && urls.length > 0) {
            String fileName = Lister.getCurrentInstance().getMember().getFileName();
            File pictureDir = new File(fileName.substring(0, fileName.lastIndexOf(".")) + File.separator + "imported" + File.separator);
            pictureDir.mkdirs();
            Vector<File> files = new Vector<File>();
            for (int n = 0, i = urls.length; n < i; n++) {
                try {
                    File file = SimpleFileDownloader.downloadFile(urls[n], pictureDir, null);
                    if (file != null) {
                        files.add(file);
                    }
                } catch (Exception exception) {
                    Lister.getCurrentInstance().addGlobalException(Language.translateStatic("ACTION_DOWNLOADPICTURE").replace("$0", urls[n]), exception);
                }
            }
            Picture[] pictures = new Picture[files.size()];
            for (int n = 0, i = pictures.length; n < i; n++) {
                pictures[n] = new Picture(files.get(n));
            }
            return pictures;
        } else {
            return new Picture[0];
        }
    }

    public static Vector<ExistingStaticData> getExistingStaticData() throws DatabaseInitiationException {
        return Database.getExistingStaticData();
    }

    public final ExistingStaticData getExistingStaticData(AuctionPlatformSite auctionPlatformSite) throws DatabaseInitiationException {
        return Database.getExistingStaticData(this, auctionPlatformSite);
    }

    public abstract Vector<Tripple<Integer, Long, String>> getOnlineStaticData(AuctionPlatformSite auctionPlatformSite, AuctionPlatformAccount auctionPlatformAccount, Vector<Tripple<Integer, Long, String>> types) throws Exception;

    public AuctionPlatformSite getBestFitAuctionlatformSite(AuctionPlatformSite auctionPlatformSite) {
        AuctionPlatformSite[] auctionPlatformSites = getAvailableAuctionPlatformSites();
        for (int n = 0, i = auctionPlatformSites.length; n < i; n++) {
            if (auctionPlatformSite.equals(auctionPlatformSites[n])) {
                return auctionPlatformSite;
            }
        }
        return auctionPlatformSites[0];
    }

    public final void setId(long id) {
        throw new IllegalArgumentException("Permitted");
    }

    public Category getCategory(AuctionPlatformSite auctionPlatformSite, int categoryNumber) throws DatabaseInitiationException {
        return Database.fillCategoryDatas(this, auctionPlatformSite, new Category(categoryNumber));
    }

    protected static final String getMD5Hash(String text) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            StringBuffer buffer = new StringBuffer();
            byte bytes[] = text.replace("\\", "\\\\").replace("\"", "\\\"").getBytes();
            byte[] md5 = messageDigest.digest(bytes);
            for (int i = 0; i < md5.length; i++) {
                String temp = "0" + Integer.toHexString((0xff & md5[i]));
                buffer.append(temp.substring(temp.length() - 2));
            }
            return buffer.toString();
        } catch (Exception exception) {
            return "";
        }
    }

    public static String generatePictureHtml(Picture[] pictures) {
        return generatePictureHtml(pictures, 0);
    }

    public static String generatePictureHtml(Picture[] pictures, int offset) {
        StringBuffer buffer = new StringBuffer("<hr/><div align=\"center\">");
        for (int n = offset, i = pictures.length; n < i; n++) {
            buffer.append("<div class=\"picture\"><img src=\"" + pictures[n].getURL() + "\" /></div><br/>");
        }
        buffer.append("</div>");
        return buffer.toString();
    }

    protected Element doXMLPost(Element xml, URL url, Map<String, String> header) throws Exception {
        String encoding = "UTF-8";
        if (xmlOutputter == null) {
            xmlOutputter = new XMLOutputter();
        }
        String postString = "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" + xmlOutputter.outputString(xml);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
        connection.setRequestProperty("Content-Type", "text/xml; charset=" + encoding);
        if (cookies != null && useCookies()) {
            for (String cookie : cookies) {
                connection.setRequestProperty("Cookie", cookie);
            }
        }
        if (header != null) {
            Set<String> keys = header.keySet();
            for (String key : keys) {
                connection.addRequestProperty(key, header.get(key));
            }
        }
        connection.connect();
        OutputStream stream = connection.getOutputStream();
        stream.write(postString.getBytes(encoding));
        stream.flush();
        Document document = null;
        InputStream inputStream = connection.getInputStream();
        if (sloppyXML()) {
            StringBuffer buffer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader("gzip".equals(connection.getContentEncoding()) ? new GZIPInputStream(inputStream) : inputStream));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.trim().length() > 0) {
                    buffer.append(line.trim());
                    buffer.append('\n');
                }
            }
            document = new SAXBuilder().build(new StringReader(buffer.toString().trim()));
        } else {
            document = new SAXBuilder().build("gzip".equals(connection.getContentEncoding()) ? new GZIPInputStream(inputStream) : inputStream);
        }
        stream.close();
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        if (useCookies() && headerFields != null) {
            cookies = headerFields.get("Set-Cookie");
        } else {
            cookies = null;
        }
        connection.disconnect();
        return document.getRootElement();
    }

    public boolean sloppyXML() {
        return false;
    }

    protected Element doXMLPost(Element xml, URL url) throws Exception {
        return doXMLPost(xml, url, null);
    }

    @SuppressWarnings("unchecked")
    protected static void applyNamespace(Element element, Namespace namespace) {
        element.setNamespace(namespace);
        List<Element> elements = element.getChildren();
        for (int n = 0, i = elements.size(); n < i; n++) {
            applyNamespace(elements.get(n), namespace);
        }
    }

    protected static void setAuctionPlatformCallDone(AuctionPlatformCallActionListener auctionPlatformCallActionListener, String action) {
        if (auctionPlatformCallActionListener != null) {
            auctionPlatformCallActionListener.setStatusText(Language.translateStatic("MESSAGE_" + action + "DONE"));
            auctionPlatformCallActionListener.setPercentageDone(1d);
            auctionPlatformCallActionListener.finished();
        }
    }

    protected static void setAuctionPlatformCallDone(AuctionPlatformCallActionListener auctionPlatformCallActionListener, Exception exception) {
        if (auctionPlatformCallActionListener != null) {
            auctionPlatformCallActionListener.failed(exception);
            auctionPlatformCallActionListener.finished();
        }
    }

    /**
	 * Override to use
	 * 
	 * @param auctionPlatformAccount
	 * @return
	 */
    public boolean revokeToken(AuctionPlatformAccount auctionPlatformAccount) {
        return false;
    }

    public long getTokenValidityTime() {
        return 0;
    }

    /**
	 * Override to use.
	 * 
	 * @return The url where the user can find information about howto get a token
	 */
    public URL getTokenInfoUrl() {
        return null;
    }

    public boolean useCookies() {
        return false;
    }

    public void clearCookies() {
        cookies = null;
    }

    public void checkTemplateBlacklisted(Template template) throws CommonException {
        if (template != null && template.getFile() != null && template.getFile().toString().contains("data" + File.separator + "templates" + File.separator + "standard_")) {
            throw new CommonException(Language.translateStatic("ERROR_TEMPLATEDEPRECATED"));
        }
    }
}
