package de.djuxen.appdigest;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;

public class AppDigestUtils {

    static void compareFolderHashes(String rootFolder, Document currentState, Document savedState, StringWriter sw) throws TransformerException, UnsupportedEncodingException {
        boolean blnSomethingHasChanged = false;
        sw.append("<hr>Comparing folder contents<br>");
        NodeList subFolders = XPathAPI.selectNodeList(savedState.getDocumentElement(), "//folder");
        for (int i = 0; i < subFolders.getLength(); i++) {
            Element subFolder = (Element) subFolders.item(i);
            String name = subFolder.getAttribute("name");
            Element newSubFolder = (Element) XPathAPI.selectSingleNode(currentState.getDocumentElement(), "//folder[@name='" + name + "']");
            if (newSubFolder == null) {
                sw.append("Folder " + name + " has been deleted<br>");
                blnSomethingHasChanged = true;
            }
        }
        NodeList newSubFolders = XPathAPI.selectNodeList(currentState.getDocumentElement(), "//folder");
        for (int i = 0; i < newSubFolders.getLength(); i++) {
            Element newSubFolder = (Element) newSubFolders.item(i);
            String name = newSubFolder.getAttribute("name");
            Element subFolder = (Element) XPathAPI.selectSingleNode(savedState.getDocumentElement(), "//folder[@name='" + name + "']");
            if (subFolder == null) {
                File f = new File(rootFolder + File.separator + name);
                sw.append("Folder " + name + " has been created. (Modified on " + new Date(f.lastModified()) + ")<br>");
                blnSomethingHasChanged = true;
            }
        }
        NodeList subFiles = XPathAPI.selectNodeList(savedState.getDocumentElement(), "//file");
        for (int i = 0; i < subFiles.getLength(); i++) {
            Element subFile = (Element) subFiles.item(i);
            String name = subFile.getAttribute("name");
            Element newSubFile = (Element) XPathAPI.selectSingleNode(currentState.getDocumentElement(), "//file[@name='" + name + "']");
            if (newSubFile != null) {
                String hash = java.net.URLDecoder.decode(subFile.getTextContent(), "UTF-8");
                String newHash = java.net.URLDecoder.decode(newSubFile.getTextContent(), "UTF-8");
                if (!hash.equals(newHash)) {
                    File f = new File(rootFolder + File.separator + name);
                    sw.append(" - <a href=\"?exec=download&file=" + URLEncoder.encode(name, "UTF-8") + "\" target=\"_blank\">" + name + "</a> has changed. (Modified on " + new Date(f.lastModified()) + ")<br>");
                    blnSomethingHasChanged = true;
                }
                subFile.getParentNode().removeChild(subFile);
                newSubFile.getParentNode().removeChild(newSubFile);
            } else {
                sw.append(" - " + name + " has been deleted<br>");
                blnSomethingHasChanged = true;
                subFile.getParentNode().removeChild(subFile);
            }
        }
        NodeList newSubFiles = XPathAPI.selectNodeList(currentState.getDocumentElement(), "//file");
        for (int i = 0; i < newSubFiles.getLength(); i++) {
            Element newSubFile = (Element) newSubFiles.item(i);
            String name = newSubFile.getAttribute("name");
            Element subFile = (Element) XPathAPI.selectSingleNode(savedState.getDocumentElement(), "//file[@name='" + name + "']");
            if (subFile == null) {
                File f = new File(rootFolder + File.separator + name);
                sw.append(" - " + name + " is a new file. (Modified on " + new Date(f.lastModified()) + ")<br>");
                blnSomethingHasChanged = true;
            }
        }
        if (!blnSomethingHasChanged) {
            sw.append(" - No file has been changed<br>");
        }
    }

    static Document hashFolder(String rootFolderPath, MessageDigest digest, boolean verbose, StringWriter sw) throws ParserConfigurationException {
        File rootFolder = new File(rootFolderPath);
        if (!rootFolder.exists() || !rootFolder.isDirectory()) throw new AppDigestException("No valid folder path: " + rootFolder.getAbsolutePath());
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        Document doc = dfactory.newDocumentBuilder().newDocument();
        Element rootNode = doc.createElement("appdigest");
        processFolderContent(digest, rootFolder, rootFolderPath, doc, rootNode, verbose, sw);
        doc.appendChild(rootNode);
        return doc;
    }

    static File saveHashResult(String baseDir, Document doc, StringWriter sw) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        File outputFile = null;
        outputFile = new File(baseDir + File.separator + AppDigest.DIGEST_FILENAME);
        sw.append("<hr>Saving hashresult to: " + outputFile.getAbsolutePath() + "<br>");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        outputFile.createNewFile();
        KeySpec keySpec = new PBEKeySpec(AppDigest.ENCRYPTION_KEY.toCharArray(), AppDigest.ENCRYPTION_SALT, AppDigest.ENCRYPTION_COUNT);
        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(AppDigest.ENCRYPTION_SALT, AppDigest.ENCRYPTION_COUNT);
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
        CipherOutputStream cos = new CipherOutputStream(bos, cipher);
        ObjectOutputStream oos = new ObjectOutputStream(cos);
        oos.writeObject(doc);
        oos.flush();
        oos.close();
        sw.append("Hashresult successfully saved<br>");
        return outputFile;
    }

    static Document loadHashResult(String baseDir, StringWriter sw) throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException {
        Document doc = null;
        File inputFile = new File(baseDir + File.separator + AppDigest.DIGEST_FILENAME);
        sw.append("<hr>Loading old hashresult from " + inputFile.getAbsolutePath() + "<br>");
        if (inputFile.exists() && !inputFile.isDirectory()) {
            KeySpec keySpec = new PBEKeySpec(AppDigest.ENCRYPTION_KEY.toCharArray(), AppDigest.ENCRYPTION_SALT, AppDigest.ENCRYPTION_COUNT);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(AppDigest.ENCRYPTION_SALT, AppDigest.ENCRYPTION_COUNT);
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile));
            CipherInputStream cis = new CipherInputStream(bis, cipher);
            ObjectInputStream ois = new ObjectInputStream(cis);
            doc = (Document) ois.readObject();
            ois.close();
            sw.append("Hashresult successfully loaded<br>");
        } else {
            sw.append("Couldn�t load hashresult<br>");
        }
        return doc;
    }

    private static void processFolderContent(MessageDigest digest, File root, String rootFolderPath, Document doc, Element parentNode, boolean verbose, StringWriter sw) {
        Element folderNode = doc.createElement("folder");
        String relFoldername = File.separator;
        if (!root.getAbsolutePath().equals(rootFolderPath)) relFoldername = getRelativeFilename(root, rootFolderPath);
        if (verbose) sw.append("Hashing folder content: " + relFoldername + "<br>");
        folderNode.setAttribute("name", relFoldername);
        File[] files = root.listFiles();
        if (files.length > 0) {
            for (File f : files) {
                if (f.isDirectory()) {
                    processFolderContent(digest, f, rootFolderPath, doc, folderNode, verbose, sw);
                } else {
                    String filename = getRelativeFilename(f, rootFolderPath);
                    if (filename.equals(AppDigest.DIGEST_FILENAME)) continue;
                    if (verbose) sw.append(" - Hashing file " + filename + "<br>");
                    Element fileNode = doc.createElement("file");
                    fileNode.setAttribute("name", filename);
                    try {
                        String hash = new String(hashFile(f, digest), "UTF-8");
                        hash = java.net.URLEncoder.encode(hash, "UTF-8");
                        if (verbose) sw.append(" - >> Hash: " + hash + "<br>");
                        fileNode.appendChild(doc.createCDATASection(hash));
                    } catch (IOException e) {
                        sw.append(" - >> Couldn�t hash file " + filename + " (" + e.getMessage() + ")<br>");
                    }
                    folderNode.appendChild(fileNode);
                }
            }
        } else {
            if (verbose) sw.append(" - NO FILES<br>");
        }
        parentNode.appendChild(folderNode);
    }

    private static byte[] hashFile(File f, MessageDigest digest) throws IOException {
        digest.reset();
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
        byte[] bytes = new byte[1024];
        int len = 0;
        while ((len = is.read(bytes)) >= 0) {
            digest.update(bytes, 0, len);
        }
        is.close();
        return digest.digest();
    }

    private static String getRelativeFilename(File f, String baseDir) {
        String absFoldername = f.getAbsolutePath();
        return absFoldername.substring(absFoldername.indexOf(baseDir) + baseDir.length() + 1);
    }
}
