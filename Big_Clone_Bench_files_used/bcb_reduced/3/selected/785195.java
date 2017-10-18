package org.lwjgl.ant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Ant task creating p2 metadata files "content.xml" and "artifact.xml" based
 * on existent plugins and features within an update site.
 * <p>
 * This task is a lighweight (and quick and dirty) version of the ant task
 * "p2.publish.featuresAndBundles"
 * provided by the Eclipse p2 plugin. However, it does not depend on any
 * Eclipse related classes!
 * </p>
 * <p><b>Warning: </b>This task is only tested with the LWJGL update site,
 * if you have a different update site, you better test it by comparing its
 * output with the files created by the original p2 tools.</p>
 *
 * @author 	Jens von Pilgrim (developer@jevopi.de)
 * @since 	Dec 9, 2011
 * @see org.eclipse.equinox.p2.publisher.Publisher
 */
public class StandalonePublisher extends Task {

    public static final int BUFFERSIZE = 2048;

    public static final FileFilter JAR_FILTER = new FileFilter() {

        public boolean accept(File i_pathname) {
            return i_pathname.isFile() && i_pathname.getName().endsWith(".jar");
        }
    };

    protected String updateSiteFolder;

    protected String repositoryName;

    protected String repositoryURI;

    protected Document currentDoc;

    protected boolean compressed = false;

    protected final XPath xpath = XPathFactory.newInstance().newXPath();

    public abstract class XMLBasedInfo {

        public Document doc;

        public abstract String getRootElementName();

        public String xpath(String path) {
            if (path.startsWith("/")) return StandalonePublisher.this.xpath(doc, path); else return StandalonePublisher.this.xpath(doc, "//" + getRootElementName() + "/" + path);
        }

        public List<Node> xpathNods(String path) {
            try {
                XPathExpression expression = xpath.compile(path);
                NodeList nodes = (NodeList) expression.evaluate(doc, XPathConstants.NODESET);
                List<Node> list = new ArrayList<Node>(nodes.getLength());
                for (int i = 0; i < nodes.getLength(); i++) {
                    list.add(nodes.item(i));
                }
                return list;
            } catch (Exception ex) {
                System.err.println(ex);
                return Collections.emptyList();
            }
        }
    }

    public class SiteInfo extends XMLBasedInfo {

        /** 
		 * {@inheritDoc}
		 * @see org.lwjgl.ant.StandalonePublisher.XMLBasedInfo#getRootElementName()
		 */
        @Override
        public String getRootElementName() {
            return "site";
        }
    }

    public static class CategoryInfo {

        String id;

        String name;

        String label;

        String description = "";

        List<FeatureInfo> required = new ArrayList<StandalonePublisher.FeatureInfo>(5);

        public String getVersion() {
            String version = "0.0.0";
            for (FeatureInfo featureInfo : required) {
                String v = featureInfo.getVersion();
                if (version.compareTo(v) < 0) {
                    version = v;
                }
            }
            return version;
        }

        String getID() {
            if (id == null) {
                String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
                id = timeStamp + "." + name;
            }
            return id;
        }

        public String getLabel() {
            if (label == null || label.isEmpty()) return name;
            return label;
        }
    }

    public class FeatureInfo extends XMLBasedInfo {

        public String getClassifier() {
            return "org.eclipse.update.feature";
        }

        public String getContentType() {
            return "application/zip";
        }

        public String getRootElementName() {
            return "feature";
        }

        public int size;

        public File jarFile;

        /**
		 * @return
		 */
        public String getID() {
            return xpath("@id");
        }

        /**
		 * @return
		 */
        public String getVersion() {
            return xpath("@version");
        }
    }

    public class BundleInfo extends FeatureInfo {

        @Override
        public String getID() {
            return mfval("Bundle-SymbolicName");
        }

        @Override
        public String getVersion() {
            return mfval("Bundle-Version");
        }

        @Override
        public String getClassifier() {
            return "osgi.bundle";
        }

        ;

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public String getRootElementName() {
            return "plugin";
        }

        public Manifest manifest;

        public String mf(String attrib) {
            return manifest.getMainAttributes().getValue(attrib);
        }

        public String mfval(String attrib) {
            return mfval(attrib, "");
        }

        /**
		 * @param i_value
		 * @param i_string
		 * @return
		 */
        public String mfval(String attrib, String key) {
            String value = manifest.getMainAttributes().getValue(attrib);
            if (value == null || value.isEmpty()) return "";
            for (String s : value.split(";")) {
                if ("".equals(key)) return s;
                int l = 2;
                int p = s.indexOf(":=");
                if (p < 0) {
                    p = s.indexOf("=");
                    l = 1;
                }
                if (p > 0) {
                    String k = s.substring(0, p);
                    String v = s.substring(p + l);
                    if (key.equals(k)) {
                        v = v.trim();
                        v = v.replaceAll("^\"([^\"]*)\"$", "\\1");
                        return v;
                    }
                }
            }
            return "";
        }

        public String valIndex(String[] attribValues, int index, String key) {
            if (attribValues.length <= index) return "";
            String value = attribValues[index];
            for (String s : value.split(";")) {
                if ("".equals(key)) return s;
                int l = 2;
                int p = s.indexOf(":=");
                if (p < 0) {
                    p = s.indexOf("=");
                    l = 1;
                }
                if (p > 0) {
                    String k = s.substring(0, p);
                    String v = s.substring(p + l);
                    if (key.equals(k)) {
                        v = v.trim();
                        v = v.replaceAll("^\"\\s*", "");
                        v = v.replaceAll("\\s*\"$", "");
                        return v;
                    }
                }
            }
            return "";
        }

        public String mfval(String attrib, String key, String def) {
            String v = mfval(attrib, key);
            if (v == null || v.isEmpty()) return def;
            return v;
        }

        public String valIndex(String[] attribValues, int index, String key, String def) {
            String v = valIndex(attribValues, index, key);
            if (v == null || v.isEmpty()) return def;
            return v;
        }
    }

    protected String xpath(Node node, String path) {
        try {
            XPathExpression expression = xpath.compile(path);
            NodeList nodes = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
            String s = nodes.item(0).getNodeValue();
            s = s.replaceAll("^\\s+", "");
            s = s.replaceAll("\\s+$", "");
            return s;
        } catch (Exception ex) {
            System.err.println(ex);
            return null;
        }
    }

    protected FeatureInfo readFeature(File featureArchive) throws ParserConfigurationException {
        FeatureInfo info = new FeatureInfo();
        info.jarFile = featureArchive;
        info.size = (int) featureArchive.length();
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(featureArchive));
            JarEntry jarEntry;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                if (!jarEntry.isDirectory() && "feature.xml".equals(jarEntry.getName())) {
                    Document doc = readXMLinJar(docBuilder, jis, jarEntry);
                    info.doc = doc;
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (jis != null) try {
                jis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return info;
    }

    protected BundleInfo readBundle(File location) throws ParserConfigurationException {
        BundleInfo info = new BundleInfo();
        info.jarFile = location;
        info.size = (int) location.length();
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(location));
            info.manifest = jis.getManifest();
            JarEntry jarEntry;
            while ((jarEntry = jis.getNextJarEntry()) != null) {
                if (!jarEntry.isDirectory() && "plugin.xml".equals(jarEntry.getName())) {
                    Document doc = readXMLinJar(docBuilder, jis, jarEntry);
                    info.doc = doc;
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (jis != null) try {
                jis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return info;
    }

    protected Collection<CategoryInfo> readSite(DocumentBuilder documentBuilder, FeatureInfo[] featureInfos) throws Exception {
        File f = new File(updateSiteFolder + File.separator + "site.xml");
        if (!f.exists()) {
            f = new File(updateSiteFolder + File.separator + "category.xml");
        }
        Map<String, FeatureInfo> featuresById = new HashMap<String, StandalonePublisher.FeatureInfo>();
        for (FeatureInfo featureInfo : featureInfos) {
            featuresById.put(featureInfo.getID(), featureInfo);
        }
        Map<String, CategoryInfo> categoryInfos = new HashMap<String, CategoryInfo>();
        Map<String, String> featureToGroup = new HashMap<String, String>();
        if (f.exists()) {
            SiteInfo site = new SiteInfo();
            site.doc = readXML(documentBuilder, new FileInputStream(f));
            List<Node> categories = site.xpathNods("//site//category-def");
            for (Node category : categories) {
                CategoryInfo categoryInfo = new CategoryInfo();
                categoryInfo.name = xpath(category, "@name");
                categoryInfo.label = xpath(category, "@label");
                categoryInfo.description = xpath(category, "description/text()");
                categoryInfos.put(categoryInfo.name, categoryInfo);
            }
            List<Node> features = site.xpathNods("//site//feature");
            for (Node feature : features) {
                featureToGroup.put(xpath(feature, "@id"), xpath(feature, "category/@name"));
            }
        }
        List<String> sortedFeatures = new ArrayList<String>();
        for (String id : featuresById.keySet()) {
            String categoryName = featureToGroup.get(id);
            if (categoryName != null) {
                CategoryInfo categoryInfo = categoryInfos.get(categoryName);
                categoryInfo.required.add(featuresById.get(id));
                sortedFeatures.add(id);
            }
        }
        for (String id : sortedFeatures) {
            featuresById.remove(id);
        }
        if (!featuresById.isEmpty()) {
            CategoryInfo categoryInfo = categoryInfos.get(this.repositoryName);
            if (categoryInfo == null) {
                categoryInfo = new CategoryInfo();
                categoryInfo.name = this.repositoryName;
                categoryInfo.label = this.repositoryName;
                categoryInfo.description = "";
                categoryInfos.put(categoryInfo.name, categoryInfo);
            }
            for (FeatureInfo featureInfo : featuresById.values()) {
                categoryInfo.required.add(featureInfo);
            }
        }
        return categoryInfos.values();
    }

    /**
	 * @param i_documentBuilder
	 * @param i_fileInputStream
	 * @return
	 */
    protected Document readXML(DocumentBuilder docBuilder, InputStream is) throws Exception {
        Document doc = docBuilder.parse(is);
        return doc;
    }

    /**
	 * @param docBuilder
	 * @param jis
	 * @param jarEntry
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
    protected Document readXMLinJar(DocumentBuilder docBuilder, JarInputStream jis, JarEntry jarEntry) throws IOException, SAXException {
        try {
            Document doc = docBuilder.parse(jis);
            return doc;
        } catch (IOException ex) {
            System.err.println("Error reading jar entry " + jarEntry + ": ex");
            throw ex;
        }
    }

    protected static void dump(Node doc) {
        System.out.println(doDump(doc));
    }

    protected static String doDump(Node doc) {
        StringWriter stringWriter = new StringWriter();
        StreamResult sr = new StreamResult(stringWriter);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        Properties oprops = new Properties();
        oprops.put(OutputKeys.METHOD, "xml");
        oprops.put(OutputKeys.STANDALONE, "yes");
        oprops.put(OutputKeys.INDENT, "yes");
        try {
            t = tf.newTransformer();
            t.setOutputProperties(oprops);
            t.transform(new DOMSource(doc), sr);
        } catch (Exception e) {
        }
        return stringWriter.toString();
    }

    protected static void dump(Manifest manifest) {
        System.out.println(doDump(manifest));
    }

    protected static String doDump(Manifest manifest) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            manifest.write(os);
            return os.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    protected static String escapeToXMLAttrib(String s) {
        return s.replaceAll("\r", "").replaceAll("\n\\s+", "");
    }

    public void doExecute() throws Exception {
        File f = new File(updateSiteFolder + File.separator + "features");
        File[] locations = f.listFiles(JAR_FILTER);
        FeatureInfo[] featureInfos = new FeatureInfo[locations.length];
        for (int i = 0; i < locations.length; i++) {
            featureInfos[i] = readFeature(locations[i]);
        }
        f = new File(updateSiteFolder + File.separator + "plugins");
        locations = f.listFiles(JAR_FILTER);
        BundleInfo[] bundleInfos = new BundleInfo[locations.length];
        for (int i = 0; i < locations.length; i++) {
            bundleInfos[i] = readBundle(locations[i]);
        }
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Collection<CategoryInfo> categoryInfos = readSite(docBuilder, featureInfos);
        writeContent(featureInfos, bundleInfos, categoryInfos);
        writeArtifacts(featureInfos, bundleInfos);
    }

    /**
	 * @param i_featureInfos
	 * @param i_bundleInfos
	 * @param i_categoryInfos 
	 * @throws Exception 
	 */
    protected void writeContent(FeatureInfo[] i_featureInfos, BundleInfo[] i_bundleInfos, Collection<CategoryInfo> i_categoryInfos) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document content = docBuilder.newDocument();
        content.setXmlStandalone(false);
        content.setTextContent(repositoryName);
        currentDoc = content;
        content.appendChild(content.createProcessingInstruction("metadataRepository", "version='1.1.0'"));
        Element repository = addElement(content, "repository", "name", repositoryName, "type", "org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository", "version", "1");
        addList(repository, "properties", createElement("property", "name", "p2.timestamp", "value", String.valueOf(System.currentTimeMillis())), createElement("property", "name", "p2.compressed", "value", String.valueOf(compressed)));
        addList(repository, "references", createElement("repository", "uri", repositoryURI, "type", "0", "options", "0"));
        List<Element> units = new ArrayList<Element>();
        for (FeatureInfo featureInfo : i_featureInfos) {
            units.add(createUnitForFeature(featureInfo));
            units.add(createUnitForFeatureGroups(featureInfo));
        }
        for (BundleInfo bundleInfo : i_bundleInfos) {
            units.add(createUnitForBundle(bundleInfo));
        }
        for (CategoryInfo categoryInfo : i_categoryInfos) {
            units.add(createUnitForCategory(categoryInfo));
        }
        addList(repository, "units", units);
        writeDocument(content, updateSiteFolder + File.separator + "content.xml");
        currentDoc = null;
    }

    /**
	 * @param i_featureInfos
	 * @param i_bundleInfos
	 * @throws Exception 
	 */
    protected void writeArtifacts(FeatureInfo[] i_featureInfos, BundleInfo[] i_bundleInfos) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document artifact = docBuilder.newDocument();
        artifact.setXmlStandalone(false);
        currentDoc = artifact;
        artifact.appendChild(artifact.createProcessingInstruction("artifactRepository", "version='1.1.0'"));
        Element repository = addElement(artifact, "repository", "name", repositoryName + " - artifacts", "type", "org.eclipse.equinox.p2.artifact.repository.simpleRepository", "version", "1");
        addList(repository, "properties", createElement("property", "name", "p2.timestamp", "value", String.valueOf(System.currentTimeMillis())), createElement("property", "name", "p2.compressed", "value", String.valueOf(compressed)));
        addList(repository, "mappings", createElement("rule", "filter", "(& (classifier=osgi.bundle))", "output", "${repoUrl}/plugins/${id}_${version}.jar"), createElement("rule", "filter", "(& (classifier=binary))", "output", "${repoUrl}/binary/${id}_${version}"), createElement("rule", "filter", "(& (classifier=org.eclipse.update.feature))", "output", "${repoUrl}/features/${id}_${version}.jar"));
        List<Element> artifacts = new ArrayList<Element>();
        for (BundleInfo bundleInfo : i_bundleInfos) {
            artifacts.add(createArtifact(bundleInfo));
        }
        for (FeatureInfo featureInfo : i_featureInfos) {
            artifacts.add(createArtifact(featureInfo));
        }
        addList(repository, "artifacts", artifacts);
        writeDocument(artifact, updateSiteFolder + File.separator + "artifacts.xml");
        currentDoc = null;
    }

    /**
	 * @param i_featureInfo
	 * @return
	 * @throws Exception 
	 */
    protected Element createArtifact(FeatureInfo info) throws Exception {
        Element artifact = createElement("artifact", "classifier", info.getClassifier(), "id", info.getID(), "version", info.getVersion());
        List<Element> properties = new ArrayList<Element>();
        properties.add(createElement("property", "name", "artifact.size", "value", String.valueOf(info.jarFile.length())));
        properties.add(createElement("property", "name", "download.size", "value", String.valueOf(info.jarFile.length())));
        properties.add(createElement("property", "name", "download.md5", "value", String.valueOf(md5(info.jarFile))));
        String contentType = info.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            properties.add(createElement("property", "name", "download.contentType", "value", contentType));
        }
        addList(artifact, "properties", properties);
        return artifact;
    }

    /**
	 * @param i_jarFile
	 * @return
	 * @throws Exception 
	 */
    protected String md5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte data[] = new byte[BUFFERSIZE];
        int count;
        while ((count = fis.read(data, 0, BUFFERSIZE)) != -1) {
            md.update(data, 0, count);
        }
        byte[] digest = md.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        String hashtext = bigInt.toString(16);
        return hashtext;
    }

    /**
	 * @param content
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
    protected void writeDocument(Document content, String targetFile) throws Exception {
        if (compressed) {
            String jarFileNameString = targetFile;
            int pos = jarFileNameString.lastIndexOf('.');
            if (pos > 0) {
                jarFileNameString = jarFileNameString.substring(0, pos) + ".jar";
            } else {
                jarFileNameString += ".jar";
            }
            JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFileNameString));
            pos = targetFile.lastIndexOf(File.separatorChar);
            if (pos > 0) {
                targetFile = targetFile.substring(pos + 1);
            }
            JarEntry entry = new JarEntry(targetFile);
            target.putNextEntry(entry);
            StreamResult sr = new StreamResult(target);
            doWriteDocument(content, sr);
            target.closeEntry();
            target.close();
        } else {
            StreamResult sr = new StreamResult(new File(targetFile));
            doWriteDocument(content, sr);
        }
    }

    /**
	 * @param content
	 * @param sr
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
    private void doWriteDocument(Document content, StreamResult sr) throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        Properties oprops = new Properties();
        oprops.put(OutputKeys.METHOD, "xml");
        oprops.put(OutputKeys.INDENT, "yes");
        t.setOutputProperties(oprops);
        t.transform(new DOMSource(content), sr);
    }

    /**
	 * @param i_site
	 * @return
	 */
    protected Element createUnitForFeatureGroups(FeatureInfo info) {
        String version = info.xpath("@version");
        Element unit = createElement("unit", "id", info.xpath("@id") + ".feature.group", "version", version, "singleton", "false");
        addElement(unit, "update", "id", info.xpath("@id") + ".feature.group", "range", "[0.0.0," + version + ")", "severity", "0");
        addList(unit, "properties", createElement("property", "name", "org.eclipse.equinox.p2.name", "value", info.xpath("@label")), createElement("property", "name", "org.eclipse.equinox.p2.description", "value", info.xpath("description/text()")), createElement("property", "name", "org.eclipse.equinox.p2.description.url", "value", info.xpath("description/@url")), createElement("property", "name", "org.eclipse.equinox.p2.provider", "value", info.xpath("@provider-name")), createElement("property", "name", "org.eclipse.equinox.p2.type.group", "value", "true"));
        addList(unit, "provides", createElement("provided", "namespace", "org.eclipse.equinox.p2.iu", "name", info.xpath("@id") + ".feature.group", "version", version));
        List<Element> requires = new ArrayList<Element>();
        for (Node n : info.xpathNods("//feature//import")) {
            requires.add(createElement("required", "namespace", "org.eclipse.equinox.p2.iu", "name", xpath(n, "@feature") + ".feature.group", "range", "[" + xpath(n, "@version") + "," + xpath(n, "@version") + "]"));
        }
        for (Node n : info.xpathNods("//feature//plugin")) {
            requires.add(createElement("required", "namespace", "org.eclipse.equinox.p2.iu", "name", xpath(n, "@id"), "range", "[" + xpath(n, "@version") + "," + xpath(n, "@version") + "]"));
        }
        Element reqFeature = createElement("required", "namespace", "org.eclipse.equinox.p2.iu", "name", info.xpath("@id") + ".feature.jar", "range", "[" + version + "," + version + "]");
        addElement(reqFeature, "filter").setTextContent("(org.eclipse.update.install.features=true)");
        requires.add(reqFeature);
        addList(unit, "requires", requires);
        addElement(unit, "touchpoint", "id", "null", "version", "0.0.0");
        List<Element> list = new ArrayList<Element>();
        for (Node n : info.xpathNods("//feature//license")) {
            Element l = createElement("license", "uri", xpath(n, "@url"), "url", xpath(n, "@url"));
            l.setTextContent(escapeToXMLAttrib(xpath(n, "text()")));
            list.add(l);
        }
        addList(unit, "licenses", list);
        Element l = createElement("copyright", "uri", info.xpath("copyright/@url"), "url", info.xpath("copyright/@url"));
        l.setTextContent(escapeToXMLAttrib(info.xpath("copyright/text()")));
        unit.appendChild(l);
        return unit;
    }

    /**
	 * @param i_categoryInfo
	 * @return
	 */
    private Element createUnitForCategory(CategoryInfo info) {
        String version = info.getVersion();
        String id = info.getID();
        Element unit = createElement("unit", "id", id, "version", version);
        addList(unit, "properties", createElement("property", "name", "org.eclipse.equinox.p2.name", "value", info.getLabel()), createElement("property", "name", "org.eclipse.equinox.p2.description", "value", info.description), createElement("property", "name", "org.eclipse.equinox.p2.type.category", "value", "true"));
        addList(unit, "provides", createElement("provided", "namespace", "org.eclipse.equinox.p2.iu", "name", id, "version", version));
        List<Element> requires = new ArrayList<Element>();
        for (FeatureInfo featureInfo : info.required) {
            String featureVersion = featureInfo.xpath("@version");
            String featureName = featureInfo.xpath("@id") + ".feature.group";
            Element required = createElement("required", "namespace", "org.eclipse.equinox.p2.iu", "name", featureName, "range", "[" + featureVersion + "," + featureVersion + "]");
            requires.add(required);
        }
        addList(unit, "requires", requires);
        addElement(unit, "touchpoint", "id", "null", "version", "0.0.0");
        return unit;
    }

    /**
	 * @param i_bundleInfo
	 * @return
	 * @throws Exception 
	 */
    protected Element createUnitForFeature(FeatureInfo info) throws Exception {
        String version = info.xpath("@version");
        Element unit = createElement("unit", "id", info.xpath("@id") + ".feature.jar", "version", version);
        addList(unit, "properties", createElement("property", "name", "org.eclipse.equinox.p2.name", "value", info.xpath("@label")), createElement("property", "name", "org.eclipse.equinox.p2.description", "value", info.xpath("description/text()")), createElement("property", "name", "org.eclipse.equinox.p2.description.url", "value", info.xpath("description/@url")), createElement("property", "name", "org.eclipse.equinox.p2.provider", "value", info.xpath("@provider-name")));
        addList(unit, "provides", createElement("provided", "namespace", "org.eclipse.equinox.p2.iu", "name", info.xpath("@id") + ".feature.jar", "version", version), createElement("provided", "namespace", "org.eclipse.equinox.p2.eclipse.type", "name", "feature", "version", "1.0.0"), createElement("provided", "namespace", "org.eclipse.update.feature", "name", info.xpath("@id"), "version", version));
        addElement(unit, "filter").setTextContent("(org.eclipse.update.install.features=true)");
        addList(unit, "artifacts", createElement("artifact", "classifier", "org.eclipse.update.feature", "id", info.xpath("@id"), "version", version));
        addElement(unit, "touchpoint", "id", "org.eclipse.equinox.p2.osgi", "version", "1.0.0");
        Element valueZipped = createElement("instruction", "key", "zipped");
        valueZipped.setTextContent("true");
        addList(unit, "touchpointData", createList("instructions", valueZipped));
        List<Element> list = new ArrayList<Element>();
        for (Node n : info.xpathNods("//feature//license")) {
            Element l = createElement("license", "uri", xpath(n, "@url"), "url", xpath(n, "@url"));
            l.setTextContent(escapeToXMLAttrib(xpath(n, "text()")));
            list.add(l);
        }
        addList(unit, "licenses", list);
        Element l = createElement("copyright", "uri", info.xpath("copyright/@url"), "url", info.xpath("copyright/@url"));
        l.setTextContent(escapeToXMLAttrib(info.xpath("copyright/text()")));
        unit.appendChild(l);
        return unit;
    }

    /**
	 * @param i_bundleInfo
	 * @return
	 */
    protected Element createUnitForBundle(BundleInfo info) {
        String version = info.mfval("Bundle-Version");
        Element unit = createElement("unit", "id", info.mfval("Bundle-SymbolicName"), "version", version, "singleton", info.mfval("Bundle-SymbolicName", "singleton", "false"));
        addElement(unit, "update", "id", info.mfval("Bundle-SymbolicName"), "range", "[0.0.0," + version + ")", "severity", "0");
        List<Element> properties = new ArrayList<Element>();
        mf2properties(info, properties, "Bundle-Name", "org.eclipse.equinox.p2.name", "Bundle-Vendor", "org.eclipse.equinox.p2.provider", "Bundle-Localization", "org.eclipse.equinox.p2.bundle.localization");
        addList(unit, "properties", properties);
        List<Element> elements = new ArrayList<Element>();
        elements.add(createElement("provided", "namespace", "org.eclipse.equinox.p2.iu", "name", info.mfval("Bundle-SymbolicName"), "version", version));
        elements.add(createElement("provided", "namespace", "osgi.bundle", "name", info.mfval("Bundle-SymbolicName"), "version", version));
        elements.add(createElement("provided", "namespace", "org.eclipse.equinox.p2.eclipse.type", "name", "bundle", "version", "1.0.0"));
        String expPackages = info.mf("Export-Package");
        if (expPackages != null) {
            String[] ps = expPackages.split("\\s*,\\s*");
            for (int i = 0; i < ps.length; i++) {
                String name = info.valIndex(ps, i, "");
                elements.add(createElement("provided", "namespace", "java.package", "name", name, "version", "0.0.0"));
            }
        }
        addList(unit, "provides", elements);
        String reqBundles = info.mf("Require-Bundle");
        if (reqBundles != null) {
            elements = new ArrayList<Element>();
            String[] ps = reqBundles.split("\\s*,\\s*");
            for (int i = 0; i < ps.length; i++) {
                String name = info.valIndex(ps, i, "");
                String range = info.valIndex(ps, i, "bundle-version");
                if (range == null || range.isEmpty()) range = "0.0.0";
                elements.add(createElement("required", "namespace", "osgi.bundle", "name", name, "range", range));
            }
            addList(unit, "requires", elements);
        }
        addList(unit, "artifacts", createElement("artifact", "classifier", "osgi.bundle", "id", info.mfval("Bundle-SymbolicName"), "version", version));
        addElement(unit, "touchpoint", "id", "org.eclipse.equinox.p2.osgi", "version", "1.0.0");
        Element valueZipped = createElement("instruction", "key", "zipped");
        valueZipped.setTextContent("true");
        Element manifest = createElement("instruction", "key", "manifest");
        manifest.setTextContent(escapeToXMLAttrib(doDump(info.manifest)));
        addList(unit, "touchpointData", createList("instructions", manifest, valueZipped));
        return unit;
    }

    /**
	 * 
	 */
    protected void mf2properties(BundleInfo info, List<Element> o_properties, String... propName) {
        for (int i = 0; i < propName.length; i += 2) {
            String value = info.mfval(propName[i]);
            if (value != null && !value.isEmpty()) {
                o_properties.add(createElement("property", "name", propName[i + 1], "value", value));
            }
        }
    }

    /**
	 * @param i_content
	 * @param i_string
	 * @param i_createElement
	 * @param i_createElement2
	 */
    protected Element addList(Element parent, String listName, Element... content) {
        return addList(parent, listName, Arrays.asList(content));
    }

    /**
	 * @param i_content
	 * @param i_string
	 * @param i_createElement
	 * @param i_createElement2
	 */
    protected Element addList(Element parent, String listName, List<Element> content) {
        Element list = createList(listName, content);
        parent.appendChild(list);
        return list;
    }

    protected Element createList(String listName, Element... content) {
        return createList(listName, Arrays.asList(content));
    }

    /**
	 * @param listName
	 * @param content
	 * @return
	 */
    protected Element createList(String listName, List<Element> content) {
        Element list = createElement(listName, "size", String.valueOf(content.size()));
        for (Element e : content) {
            list.appendChild(e);
        }
        return list;
    }

    /**
	 * @param i_content
	 * @param i_repository
	 * @return
	 */
    protected Element addElement(Node parent, String name, String... propName_Values) {
        Element child = createElement(name, propName_Values);
        parent.appendChild(child);
        return child;
    }

    protected Element createElement(String name, String... propName_Values) {
        if (propName_Values.length % 2 != 0) {
            throw new IllegalArgumentException("expected key values pairs");
        }
        Element element = currentDoc.createElement(name);
        for (int i = 0; i < propName_Values.length; i += 2) {
            if (propName_Values[i + 1] != null) element.setAttribute(propName_Values[i], propName_Values[i + 1]);
        }
        return element;
    }

    /**
	 * @return the updateSiteFolder
	 */
    public String getUpdateSiteFolder() {
        return updateSiteFolder;
    }

    /**
	 * @param i_updateSiteFolder the updateSiteFolder to set
	 */
    public void setUpdateSiteFolder(String i_updateSiteFolder) {
        updateSiteFolder = i_updateSiteFolder;
    }

    /**
	 * @return the repositoryName
	 */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
	 * @param i_repositoryName the repositoryName to set
	 */
    public void setRepositoryName(String i_repositoryName) {
        repositoryName = i_repositoryName;
    }

    /**
	 * @return the repositoryURI
	 */
    public String getRepositoryURI() {
        return repositoryURI;
    }

    /**
	 * @param i_repositoryURI the repositoryURI to set
	 */
    public void setRepositoryURI(String i_repositoryURI) {
        repositoryURI = i_repositoryURI;
    }

    /**
	 * @return the compressed
	 */
    public boolean isCompressed() {
        return compressed;
    }

    /**
	 * @param i_compressed the compressed to set
	 */
    public void setCompressed(boolean i_compressed) {
        compressed = i_compressed;
    }

    /**
	 * Basically for testing.
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        StandalonePublisher publisher = new StandalonePublisher();
        if (!publisher.parseArgs(args)) {
            help();
        } else {
            try {
                publisher.execute();
                System.out.println("Successfull created p2 metadata in " + publisher.updateSiteFolder);
            } catch (Exception ex) {
                System.err.println("Error creating p2 metadata: ");
                System.err.println(ex);
                ex.printStackTrace();
            }
        }
    }

    /**
	 * 
	 */
    static void help() {
        System.out.println("StandalonePublisher, (C) Jens von Pilgrim 2011 (EPL and BSD license)");
        System.out.println("Ant task generating p2 metadata for old style Eclipse update site.");
        System.out.println("Limited support: only bundles and featues are supported, functionality only");
        System.out.println("  tested for LWJGL update site");
        System.out.println("If run as command line tool, specify update site folder, repository name");
        System.out.println("  and URI as follows:");
        System.out.println("  >java org.ljgl.ant.StandalonePublisher FOLDER NAME URI");
        System.out.println("Example (supposed tool is called from updatesite folder):");
        System.out.println("  >java org.lwjgl.ant.StandalonePublisher . org.lwjgl http://lwjgl.org/update");
        System.out.println("If content.xml and artfact.xml should be compressed, add 'compress'");
    }

    /**
	 * @param i_args
	 * @return
	 */
    boolean parseArgs(String[] i_args) {
        if (i_args == null || i_args.length < 3) {
            return false;
        }
        updateSiteFolder = i_args[0];
        repositoryName = i_args[1];
        repositoryURI = i_args[2];
        if (updateSiteFolder.isEmpty() || repositoryName.isEmpty() || repositoryURI.isEmpty()) {
            return false;
        }
        File f = new File(updateSiteFolder);
        if (!f.exists()) {
            System.err.println("Update site folder does not exist.");
            return false;
        }
        if (!(repositoryURI.startsWith("http") || repositoryURI.startsWith("file"))) {
            System.out.println("Warning: Repository URI should start with protocol, e.g., http: or file:");
        }
        if (i_args.length == 4 && "compress".equalsIgnoreCase(i_args[3])) {
            compressed = true;
        }
        return true;
    }

    /** 
	 * {@inheritDoc}
	 * @see org.apache.tools.ant.Task#execute()
	 */
    @Override
    public void execute() throws BuildException {
        if (updateSiteFolder == null) {
            throw new BuildException("attribute updateSiteFolder missing");
        }
        if (repositoryName == null) {
            throw new BuildException("attribute repositoryName missing");
        }
        if (repositoryURI == null) {
            throw new BuildException("attribute repositoryURI missing");
        }
        try {
            doExecute();
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }
}
