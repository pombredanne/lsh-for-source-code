package com.gcsf.books.utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.gcsf.books.engine.logging.LogHelper;
import com.gcsf.books.utilities.xml.XMLValidatorErrorHandler;

public class FileUtils {

    /**
   * 
   */
    public static final boolean RECURSE = true;

    /**
   * 
   */
    public static final boolean NORECURSE = false;

    /**
   * 
   */
    public static final int BUF_SIZE = 1000;

    private static final String MD5_ALGORITHM = "MD5";

    private static final Logger ourLogger = Logger.getLogger(FileUtils.class);

    private static final Comparator<File> NAME_COMPARE = new Comparator<File>() {

        public int compare(final File aFile1, final File aFile2) {
            return aFile1.getName().compareTo(aFile2.getName());
        }
    };

    /**
   * get a list of files in a given directory matching the given filename
   * extention
   * 
   * for recursive usage set aRecurse=true
   * 
   * @param aFolder
   *          Folder name
   * @param aFilenameExtension
   *          File name extension
   * @param aRecurse
   *          Process subdirectories
   * @return List<File> in the given aFolder matching
   * @throws IOException
   *           when aFolder is not a directory, or cannot be read
   */
    public static List<File> getFiles(String aFolder, final String aFilenameExtension, boolean aRecurse) throws IOException {
        return getFiles(new File(aFolder), aFilenameExtension, aRecurse);
    }

    /**
   * get a list of files in a given directory matching the given filename
   * extention
   * 
   * for recursive usage set aRecurse=true
   * 
   * @param aFolder
   *          Folder name
   * @param aFilenameExtension
   *          File name extension
   * @param aRecurse
   *          Process subdirectories
   * @return List<File> in the given aFolder matching
   * @throws IOException
   *           when aFolder is not a directory, or cannot be read
   */
    public static List<File> getFiles(File aFolder, final String aFilenameExtension, boolean aRecurse) throws IOException {
        try {
            List<File> resultList = new ArrayList<File>();
            if (!aFolder.isDirectory()) {
                throw new IOException(aFolder.getAbsolutePath() + " is not a directory");
            }
            if (!aFolder.canRead()) {
                throw new IOException(aFolder + " cannot be read");
            }
            if (aRecurse) {
                File[] directories = aFolder.listFiles(new FileFilter() {

                    public boolean accept(File aPathname) {
                        return aPathname.isDirectory();
                    }
                });
                Arrays.sort(directories, NAME_COMPARE);
                for (int i = 0; i < directories.length; i++) {
                    if (directories[i].isDirectory()) {
                        resultList.addAll(getFiles(directories[i], aFilenameExtension, aRecurse));
                    }
                }
            }
            File[] files = aFolder.listFiles(new FileFilter() {

                public boolean accept(File aPathname) {
                    return aPathname.getName().toLowerCase().endsWith(aFilenameExtension.toLowerCase());
                }
            });
            Arrays.sort(files, NAME_COMPARE);
            resultList.addAll(Arrays.asList(files));
            return resultList;
        } catch (RuntimeException e) {
            ourLogger.error(String.format("error in getFiles(%s) : ", aFolder), e);
            throw e;
        }
    }

    /**
   * generates a list of files in a system independent manner and a reproducable
   * order, based on the given base file
   * 
   * @param aFile
   *          base folder
   * @return list of files (no directories)
   */
    public static List<File> getFiles(File aFile) {
        List<File> result = new ArrayList<File>();
        getFileListSorted(aFile, result);
        return result;
    }

    /**
   * generates a MD5 checksum for one given file or all files in the given
   * directory
   * 
   * @param aFile
   *          a single file or a base directory
   * @return checksum or null in case of errors
   */
    public static String getMD5String(File aFile) {
        return byteArray2String(FileUtils.getMD5(aFile));
    }

    /**
   * generates a MD5 checksum for one given file or all files in the given
   * directory
   * 
   * @param aFile
   *          a single file or a base directory
   * @return checksum or null in case of errors
   */
    public static byte[] getMD5(File aFile) {
        List<File> list = new ArrayList<File>();
        getFileListSorted(aFile, list);
        File actFile = null;
        byte[] result = null;
        try {
            MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
            md.reset();
            byte[] buffer = new byte[BUF_SIZE];
            int readBytes;
            for (File file : list) {
                actFile = file;
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), BUF_SIZE);
                while ((readBytes = in.read(buffer, 0, BUF_SIZE)) != -1) {
                    md.update(buffer, 0, readBytes);
                }
                System.out.println(file.getAbsolutePath());
            }
            result = md.digest();
        } catch (NoSuchAlgorithmException e) {
            ourLogger.error("cannot find MessageDigest Algorithm:" + MD5_ALGORITHM);
        } catch (FileNotFoundException e) {
            ourLogger.error(String.format("cannot find file <%s>", actFile), e);
        } catch (IOException e) {
            ourLogger.error("cannot read file", e);
        }
        return result;
    }

    private static String byteArray2String(byte[] aArray) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < aArray.length; i++) {
            sb.append(String.format("%02X", aArray[i]));
        }
        return sb.toString();
    }

    /**
   * generates a complete list of Files in a system independent manner and a
   * reproducable order
   * 
   * @param aDirectory
   * @param aResultList
   */
    private static void getFileListSorted(File aDirectory, List<File> aResultList) {
        if (aDirectory.isDirectory()) {
            File[] child = aDirectory.listFiles();
            Arrays.sort(child, NAME_COMPARE);
            if (child != null && child.length > 0) {
                for (int i = 0; i < child.length; i++) {
                    getFileListSorted(child[i], aResultList);
                }
            }
        } else {
            aResultList.add(aDirectory);
        }
    }

    /**
   * Main method
   * 
   * @param aArguments
   *          - usused
   */
    public static void main(String[] aArguments) {
        List<File> files;
        try {
            files = FileUtils.getFiles("../TestApplication/resource/cae/rcs", ".xml", NORECURSE);
            System.out.println(files);
            files = FileUtils.getFiles("../TestApplication/resource/cae/trackdiagram/", ".xml", RECURSE);
            System.out.println(files);
        } catch (IOException aException) {
            ourLogger.error(LogHelper.getStackString(aException));
        }
    }

    /**
   * @param aXmlFilePath
   *          Xml File Path
   * @param aRootNodeNames
   *          Root Node Names
   * @return true if given XML file root node is equal to the given
   *         aRootNodeName
   * @throws ParserConfigurationException
   *           (getXmlRoot method)
   * @throws IOException
   *           (getXmlRoot method)
   * @throws SAXException
   *           (getXmlRoot method)
   */
    public static boolean checkXmlRootNode(File aXmlFilePath, String[] aRootNodeNames) throws SAXException, IOException, ParserConfigurationException {
        boolean result = false;
        Element rootElement = getXmlRoot(aXmlFilePath);
        for (String rootNodeName : aRootNodeNames) {
            if (rootNodeName.equals(rootElement.getNodeName())) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
   * The <CODE>removeDirectory</CODE> method is provided to remove a directory
   * completly, i.e. inclusive all files and subdirectories.
   * 
   * @param aDirectoryToBeRemoved
   *          is the directory incl. all files and subdirectories which shall be
   *          removed
   * @return true if the given directory is removed.
   */
    public static boolean removeDirectory(File aDirectoryToBeRemoved) {
        File[] allFiles = aDirectoryToBeRemoved.listFiles();
        boolean checkFlag = true;
        if (null != allFiles) {
            for (File file : allFiles) {
                if (file.isDirectory()) {
                    checkFlag &= removeDirectory(file);
                } else {
                    checkFlag &= file.delete();
                }
            }
        }
        return checkFlag & aDirectoryToBeRemoved.delete();
    }

    /**
   * @param aXmlFilePath
   *          Xml File Path
   * @return root XML element of the given XML file, or null in case of problems
   * @throws IOException
   *           (parse method)
   * @throws SAXException
   *           (parse method)
   * @throws ParserConfigurationException
   *           (newDocumentBuilder method)
   */
    public static Element getXmlRoot(File aXmlFilePath) throws SAXException, IOException, ParserConfigurationException {
        ourLogger.debug(aXmlFilePath.getAbsolutePath());
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        factory.setValidating(true);
        XMLValidatorErrorHandler errorHandler = new XMLValidatorErrorHandler();
        builder.setErrorHandler(errorHandler);
        try {
            document = builder.parse(aXmlFilePath);
        } catch (Exception e) {
            if (errorHandler.getErrorOccured()) {
                ourLogger.error(errorHandler.getErrorText());
            }
        }
        Element rootElement = document.getDocumentElement();
        return rootElement;
    }

    /**
   * helper method to get XML content, here get the node of a given parent
   * 
   * @param aParentNode
   *          Node
   * @param aNodeName
   *          String
   * @param aIndex
   *          int
   * @return the Node as direct child of aParentNode, with name aNodeName and
   *         index aIndex, or null if not existing
   */
    public static Node getNode(Node aParentNode, String aNodeName, int aIndex) {
        Node result = null;
        int elementIndex = -1;
        NodeList list = aParentNode.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if ((node.getNodeType() == Node.ELEMENT_NODE || node.getNodeType() == Node.TEXT_NODE) && node.getNodeName().equals(aNodeName)) {
                elementIndex++;
                if (aIndex == elementIndex) {
                    result = node;
                    break;
                }
            }
        }
        if (null == result) {
            ourLogger.error(String.format("cannot get node <%s>", aNodeName));
        }
        return result;
    }

    /**
   * helper method to get XML content, here get the content of a given parent
   * 
   * @param aParentNode
   *          Node
   * @param aNodeName
   *          String
   * @param aIndex
   *          int
   * @return the text content of node as direct child of aParentNode, with name
   *         aNodeName and index aIndex, or null if not existing
   */
    public static String getNodeContent(Node aParentNode, String aNodeName, int aIndex) {
        String result = null;
        Node node = getNode(aParentNode, aNodeName, aIndex);
        if (null != node) {
            result = node.getTextContent();
        } else {
            ourLogger.error(String.format("cannot get node content for <%s>", aNodeName));
        }
        return result;
    }

    /**
   * @param aNode
   *          Node
   * @param aAttributeName
   *          String
   * @return the attribute value with the given attribute name of aNode or null
   *         if not existing
   */
    public static String getAttributeValue(Node aNode, String aAttributeName) {
        String result = null;
        NamedNodeMap nodes = aNode.getAttributes();
        if (null != nodes) {
            Node node = nodes.getNamedItem(aAttributeName);
            if (null != node) {
                result = node.getTextContent();
            }
        }
        return result;
    }
}
