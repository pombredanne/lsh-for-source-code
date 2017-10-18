package net.sf.cryptoluggage.luggage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.tree.MutableTreeNode;
import net.sf.cryptoluggage.crypto.CipherGenerator;
import net.sf.cryptoluggage.crypto.CryptoEnvironment;
import net.sf.cryptoluggage.crypto.EasyKeyGenerator;
import net.sf.cryptoluggage.crypto.FileTransformer;
import net.sf.cryptoluggage.crypto.ParameterSet;
import net.sf.cryptoluggage.crypto.RestrictedEnvironmentException;
import net.sf.cryptoluggage.util.HexRepresentation;
import net.sf.cryptoluggage.util.MiscUtil;

/**
 * Luggage class is used to represent a Luggage and manage it.
 * 
 * Physically, a luggage is a folder with encrypted files and a couple of files
 * with metadata. That folder is self-contained so that one can easily export
 * luggages to external (unsecure) storage media and be able to recover the 
 * files in any other computer with a JRE.
 * 
 * @author Miguel Hernández <mhernandez314@gmail.com>
 */
public class Luggage {

    protected File baseDirectory;

    protected FileHierarchy fileHierarchy;

    protected SecretKey key;

    protected ParameterSet parameters;

    private CipherGenerator cg;

    private FileTransformer fileTransformer;

    private MessageDigest nameDigester;

    private static final String parameterFileName = ".parameters";

    private static final String hierarchyFileName = ".hierarchy";

    /**
     * Create a instance from an existing Luggage folder. A valid key must be
     * specified in order to load and open the luggage from the folder.
     *
     * @param luggageFolder the folder that contains the Luggage.
     * @param password the password used in the creation of the luggage.
     * @see EasyKeyGenerator
     * @throws java.io.IOException if a problem arises while opening the luggage
     */
    public Luggage(File luggageFolder, char[] password) throws IOException, RestrictedEnvironmentException {
        try {
            this.baseDirectory = luggageFolder;
            loadParameters();
            if (!CryptoEnvironment.canHandleParameters(parameters)) {
                throw new RestrictedEnvironmentException();
            }
            nameDigester = MessageDigest.getInstance(parameters.getDigestAlgorithm());
            EasyKeyGenerator kg = new EasyKeyGenerator(parameters);
            key = kg.getKey(password);
            cg = new CipherGenerator(parameters);
            fileTransformer = new FileTransformer(parameters);
            loadHierarchy();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Error creating nameDigester", ex);
        }
    }

    /**
     * Get the base directory of the luggage
     * @return the base directory of the luggage
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Get a tree of directories rooted on the given element, which must be
     * a directory node.
     *
     * @param directory a directory LuggageElement
     * @return a tree of directories rooted on the given directory
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException if the given
     * element is not a directory
     */
    public MutableTreeNode getDirectoryTree(LuggageElement directory) throws NodeNotFoundException {
        if (!isDirectory(directory)) {
            throw new NodeNotFoundException("Element was not a directory");
        }
        return fileHierarchy.getDirectoryNode(directory.getElementName(), directory.getParentDirs());
    }

    /**
     * Get the TransformationInfo instance associated to a given file
     *
     * @return a TransformationInfo instance associated to the given file
     */
    public TransformationInfo getTransformationInfo(LuggageElement file) throws NodeNotFoundException {
        return fileHierarchy.getFile(file.getElementName(), file.getParentDirs());
    }

    /**
     * Get the name of the luggage, which is the name of the direcctory where
     * is based.
     * @return the name of the luggage
     */
    public String getName() {
        return getBaseDirectory().getName();
    }

    /**
     * Insert a file into the luggage root. 
     * The file is read and encrypted and saved in the file hierarchy.
     * Hierarchy file is automatically updated.
     *
     * @param file the file to be added to the luggage
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * 
     * @throws IOException if file couldn't be inserted (wasn't a real file,
     * couldn't write to luggage's base directory...)
     * @throws NodeAlreadyExistsException if file already existed
     * @throws NodeNotFoundException if any of the parent dirs couldn't be found
     * int the luggage hierarchy.
     */
    public void insertFile(File file, String progressMessage) throws IOException, NodeAlreadyExistsException, NodeNotFoundException {
        insertFile(file, new ArrayList(), progressMessage);
    }

    /**
     * Call insertFile with null progressMessage
     */
    public void insertFile(File file) throws IOException, NodeAlreadyExistsException, NodeNotFoundException {
        insertFile(file, new ArrayList(), null);
    }

    /**
     * Insert a file into the luggage in a given directory in the luggage file
     * hierarchy. The directory is specified by a list of directory names which
     * must exist prior to this call, where the last item of the list is the
     * name of the directory where the file is to be inserted.
     * Hierarchy file is automatically updated.
     *
     * @param file the file to be inserted
     * @param parentDirs the list of parent dirs from root to parent dir
     * where the file is to be inserted.
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * 
     * @throws IOException if file couldn't be inserted (wasn't a real file,
     * couldn't write to luggage's base directory...)
     * @throws NodeAlreadyExistsException if file already existed
     * @throws NodeNotFoundException if any of the parent dirs couldn't be found
     * int the luggage hierarchy.
     */
    public synchronized void insertFile(File file, List<String> parentDirs, String progressMessage) throws NodeAlreadyExistsException, IOException, NodeNotFoundException {
        insertFileNoUpdate(file, parentDirs, progressMessage);
        updateHierarchyFile();
    }

    /**
     * Call insertFile with null progressMessage
     */
    public synchronized void insertFile(File file, List<String> parentDirs) throws NodeAlreadyExistsException, IOException, NodeNotFoundException {
        insertFile(file, parentDirs, null);
    }

    /**
     * Insert a directory into the luggage in a given dir in the luggage hierarchy.
     * The directory is specified by a list of directory names which
     * must exist prior to this call, where the last item of the list is the
     * name of the directory where the file is to be inserted.
     * Hierarchy file is automatically updated.
     *
     * @param directory the directory to insert
     * @param parentDirs the list of parent dirs from root to parent dir
     * where the dir is to be inserted.
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     *
     * @throws IOException if directory couldn't be inserted (wasn't a real dir,
     * couldn't write to luggage's base directory...)
     * @throws NodeAlreadyExistsException if the dir or any of its children already
     * existed in the hierarchy
     * @throws NodeNotFoundException if any of the parent dirs couldn't be found
     * int the luggage hierarchy.
     */
    public synchronized void insertDirectory(File directory, List<String> parentDirs, String progressMessage) throws IOException, NodeAlreadyExistsException, NodeNotFoundException {
        if (!directory.isDirectory()) {
            throw new IOException("File '" + directory.getPath() + "' is not a" + "valid directory");
        }
        insertDirectoryNoUpdate(directory, parentDirs, progressMessage);
        updateHierarchyFile();
    }

    /**
     * Call insertdirectory with null progressMessage
     */
    public synchronized void insertDirectory(File directory, List<String> parentDirs) throws IOException, NodeAlreadyExistsException, NodeNotFoundException {
        insertDirectory(directory, parentDirs, null);
    }

    /**
     * Add a new empty directory in the file hierarchy of the luggage.
     * Hierarchy file is automatically updated.
     *
     * @param dirName the name of the directory to be created
     * @param parentDirs the list of parent dirs from root to parent dir
     * where the directory is going to be added.
     * @throws NodeAlreadyExistsException if node already existed
     * @throws NodeNotFoundException if any of the parent dirs couldn't be found
     * @throws IOException if hierarchy file couldn't be updated
     */
    public synchronized void addDirectory(String dirName, List<String> parentDirs) throws NodeAlreadyExistsException, NodeNotFoundException, IOException {
        addDirectoryNoUpdate(dirName, parentDirs);
        updateHierarchyFile();
    }

    /**
     * Add a new empty directory in the file hierarchy of the luggage
     * without updating the hierarchy file.
     *
     * @param dirName the name of the directory to be created
     * @param parentDirs the list of parent dirs from root to parent dir
     * where the directory is going to be added.
     * @throws NodeAlreadyExistsException if node already existed
     * @throws NodeNotFoundException if any of the parent dirs couldn't be found
     */
    protected synchronized void addDirectoryNoUpdate(String dirName, List<String> parentDirs) throws NodeAlreadyExistsException, NodeNotFoundException {
        fileHierarchy.addDirectory(dirName, parentDirs);
    }

    /**
     * Copy the data of an encrypted file (that resides in the root directory of
     * the luggage) to a given file
     * 
     * @param outputFile the file where the data will be writen
     * @param fileName the name of the file in the luggage file hierarchy
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * 
     * @throws java.io.IOException if file couldn't be extracted
     * @throws NodeNotFoundException if the file couldn't be found in the
     * luggage file hierarchy.
     */
    public synchronized void extractFile(File outputFile, String fileName, String progressMessage) throws IOException, NodeNotFoundException {
        extractFile(outputFile, fileName, new ArrayList(), progressMessage);
    }

    /**
     * Call extractFile with null progressMessage
     */
    public synchronized void extractFile(File outputFile, String fileName) throws IOException, NodeNotFoundException {
        extractFile(outputFile, fileName, (String) null);
    }

    /**
     * Copy the data of an encrypted file in the luggage to a given file
     *
     * @param outputFile the file where the data will be writen
     * @param luggageElement file in the luggage to copy outside
     * @throws java.io.IOException if file couldn't be extracted
     * @throws NodeNotFoundException if the file couldn't be found in the
     * luggage file hierarchy.
     */
    public synchronized void extractFile(File outputFile, LuggageElement luggageElement) throws IOException, NodeNotFoundException {
        extractFile(outputFile, luggageElement.getElementName(), luggageElement.getParentDirs());
    }

    /**
     * Copy the data of an encrypted file in the luggage to a given file
     *
     * @param outputFile the file where the data will be writen
     * @param fileName the name of the file in the luggage file hierarchy
     * @param parentDirs the list of parent dirs of the file from root to parent dir
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * @throws java.io.IOException if file couldn't be extracted
     * @throws NodeNotFoundException if the file couldn't be found in the
     * luggage file hierarchy.
     */
    public synchronized void extractFile(File outputFile, String fileName, List<String> parentDirs, String progressMessage) throws IOException, NodeNotFoundException {
        TransformationInfo ti = fileHierarchy.getFile(fileName, parentDirs);
        File inputFile = new File(baseDirectory + File.separator + ti.getEncryptedFileName());
        fileTransformer.decryptFile(inputFile, outputFile, key, progressMessage, ti.getIv());
    }

    /**
     * Call extractFile with null progressMessage
     */
    public synchronized void extractFile(File outputFile, String fileName, List<String> parentDirs) throws IOException, NodeNotFoundException {
        extractFile(outputFile, fileName, parentDirs, null);
    }

    /**
     * Recursively export a directory of the luggage file hierarchy and all of
     * its contents to a given directory.
     *
     * A directory will be created in the outputDir directory (which must
     * exist prior to this call) and then files and directories will be
     * decrypted and added to that outputDir.
     *
     * @param outputDir the directory where the selected dir will be put. Must
     * exist prior to this call.
     * @param dirName the name of the directory to extract, that is implicitly
     * searched in the root directory of the luggage file hierarchy.
     * @param parentDirs a list of directory names that represent a path in the
     * file hierarchy of the luggage.
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * 
     * @throws java.io.IOException If data cannot be extracted
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException if the
     * selected directory didn't exist in the luggage file hierarchy.
     */
    public synchronized void extractDirectory(File outputFile, String fileName, String progressMessage) throws IOException, NodeNotFoundException {
        extractDirectory(outputFile, fileName, new ArrayList(), progressMessage);
    }

    /**
     * Call extractDirectory with null progressMessage
     */
    public synchronized void extractDirectory(File outputFile, String fileName) throws IOException, NodeNotFoundException {
        extractDirectory(outputFile, fileName, new ArrayList(), null);
    }

    /**
     * Recursively export a directory of the luggage file hierarchy and all of
     * its contents to a given directory.
     *
     * A directory will be created in the outputDir directory (which must
     * exist prior to this call) and then files and directories will be
     * decrypted and added to that outputDir.
     *
     * @param outputDir the directory where the selected dir will be put. Must
     * exist prior to this call.
     * @param luggageElement directory in the luggage to extract
     *
     * @throws java.io.IOException If data cannot be extracted
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException if the
     * selected directory didn't exist in the luggage file hierarchy.
     */
    public synchronized void extractDirectory(File outputFile, LuggageElement luggageElement) throws IOException, NodeNotFoundException {
        extractDirectory(outputFile, luggageElement.getElementName(), luggageElement.getParentDirs());
    }

    /**
     * Recursively export a directory of the luggage file hierarchy and all of
     * its contents to a given directory.
     *
     * A directory will be created in the outputDir directory (which must
     * exist prior to this call) and then files and directories will be
     * decrypted and added to that outputDir.
     *
     * @param outputDir the directory where the selected dir will be put. Must
     * exist prior to this call.
     * @param dirName the name of the directory to extract in the luggage file
     * hierarchy
     * @param parentDirs a list of directory names that represent a path in the
     * file hierarchy of the luggage.
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     *
     * @throws java.io.IOException If data cannot be extracted
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException if the
     * selected directory didn't exist in the luggage file hierarchy.
     */
    public synchronized void extractDirectory(File outputDir, String dirName, List<String> parentDirs, String progressMessage) throws IOException, NodeNotFoundException {
        if (!outputDir.isDirectory()) {
            throw new IOException("File '" + outputDir.getPath() + "' is not a" + "valid directory");
        }
        List<String> childDirs = fileHierarchy.getDirectories(dirName, parentDirs);
        List<TransformationInfo> childFiles = fileHierarchy.getFiles(dirName, parentDirs);
        File dirFile;
        if (parentDirs.isEmpty() && (dirName.equals("") || dirName.equals("/"))) {
            dirFile = new File(outputDir, getName());
        } else {
            dirFile = new File(outputDir, dirName);
        }
        if (!dirFile.mkdir()) {
            throw new IOException("Couldn't create directory '" + dirFile.getPath() + "'");
        }
        parentDirs.add(dirName);
        for (TransformationInfo childFile : childFiles) {
            File childOutputFile = new File(dirFile + File.separator + childFile.getOriginalFileName());
            extractFile(childOutputFile, childFile.getOriginalFileName(), parentDirs, progressMessage);
        }
        for (String childDir : childDirs) {
            extractDirectory(dirFile, childDir, parentDirs, progressMessage);
        }
        parentDirs.remove(parentDirs.size() - 1);
    }

    /**
     * Call extractDirectory with null progressMessage
     */
    public synchronized void extractDirectory(File outputDir, String dirName, List<String> parentDirs) throws IOException, NodeNotFoundException {
        extractDirectory(outputDir, dirName, (String) null);
    }

    /**
     * Extract the whole contents of the luggage to the given directory.
     * 
     * A new folder called like the luggage base dir will be created in
     * the selected directory, and contents will be extracted there.
     * 
     * @param outputDir directory where contents will be extracted.
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     */
    public synchronized void extractRoot(File outputDir, String progressMessage) throws IOException {
        if (!outputDir.isDirectory()) {
            throw new IOException("File '" + outputDir.getPath() + "' is not" + "a valid extraction directory");
        }
        String outputName = baseDirectory.getName();
        int counter = 1;
        File newDir = new File(outputDir + File.separator + outputName);
        while (!newDir.mkdir()) {
            newDir = new File(outputDir + File.separator + outputName + "-" + counter);
            counter++;
        }
        List<TransformationInfo> files = fileHierarchy.getFiles();
        List<String> dirs = fileHierarchy.getDirectories();
        for (TransformationInfo file : files) {
            System.err.println("Extracting file" + file);
            try {
                File outputFile = new File(newDir + File.separator + file.getOriginalFileName());
                extractFile(outputFile, file.getOriginalFileName(), progressMessage);
            } catch (NodeNotFoundException ex) {
                throw new RuntimeException(MiscUtil.getI18nString("Files_returned_by_getFiles()_are_") + "not found by extractFile.", ex);
            }
        }
        for (String dir : dirs) {
            System.err.println("Extracting dir" + dir);
            try {
                extractDirectory(newDir, dir, progressMessage);
            } catch (NodeNotFoundException ex) {
                throw new RuntimeException(MiscUtil.getI18nString("Dirs_returned_by_getDirectories()_") + "are not found by extractFile.", ex);
            }
        }
    }

    /**
     * Call extractRoot with null progressMessage
     */
    public synchronized void extractRoot(File outputDir) throws IOException {
        extractRoot(outputDir, null);
    }

    /**
     * Determine wether the given element is a directory
     *
     * @param luggageElement the luggage element to test
     * @return true if and only if the elemnt exists and is a directory
     */
    public synchronized boolean isDirectory(LuggageElement luggageElement) {
        return isDirectory(luggageElement.getElementName(), luggageElement.getParentDirs());
    }

    /**
     * Determine wether the given element is a directory
     * 
     * @param nodeName the name of the node
     * @param path the path to the node
     * @return true if and only if the elemnt exists and is a directory
     */
    public synchronized boolean isDirectory(String nodeName, List<String> path) {
        return fileHierarchy.isDirectory(nodeName, path);
    }

    /**
     * Determine wether the given element is a file
     * 
     * @param luggageElement the luggage element to test
     * @return true if and only if the elemnt exists and is a file
     */
    public synchronized boolean isFile(LuggageElement luggageElement) {
        return isFile(luggageElement.getElementName(), luggageElement.getParentDirs());
    }

    /**
     * Determine wether the given element is a file
     *
     * @param nodeName the name of the node
     * @param path the path to the node
     * @return true if and only if the elemnt exists and is a file
     */
    public boolean isFile(String nodeName, List<String> path) {
        return fileHierarchy.isFile(nodeName, path);
    }

    /**
     * Deletes a file from the luggage file hierarchy, also removing the
     * encrypted file from the luggage base dir.
     * Hierarchy file is automatically updated.
     * 
     * @param luggageElement element to delete from the luggage
     * @throws java.io.IOException if the encrypted file cannot be deleted
     * @throws NodeNotFoundException if the encrypted file cannot be found
     */
    public synchronized void deleteFile(LuggageElement luggageElement) throws NodeNotFoundException, IOException {
        deleteFile(luggageElement.getElementName(), luggageElement.getParentDirs());
    }

    /**
     * Deletes a file from the luggage file hierarchy, also removing the
     * encrypted file from the luggage base dir.
     * Hierarchy file is automatically updated.
     *
     * @param fileName the name of the file in the luggage hierarchy
     * @param parentDirs the list of parent directories of the file in the
     * file hierarchy, starting with the dir hanging from the root node
     * @throws java.io.IOException if the encrypted file cannot be deleted
     * @throws NodeNotFoundException if the encrypted file cannot be found
     */
    public synchronized void deleteFile(String fileName, List<String> parentDirs) throws NodeNotFoundException, IOException {
        deleteFileNoUpdate(fileName, parentDirs);
        updateHierarchyFile();
    }

    /**
     * Delete a directory and all of its contents from the luggage.
     * Hierarchy file is automatically updated.
     *
     * @param luggageElement element
     * @throws NodeNotFoundException if the encrypted file cannot be found
     * @throws java.io.IOException if file cannot be deleted
     */
    public synchronized void deleteDirectory(LuggageElement luggageElement) throws NodeNotFoundException, IOException {
        deleteDirectory(luggageElement.getElementName(), luggageElement.getParentDirs());
    }

    /**
     * Delete a directory and all of its contents from the luggage.
     * Hierarchy file is automatically updated.
     *
     * @param dirName the name of the directory to be deleted
     * @param parentDirs parent directories, listed from root to parent dir
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException if the
     * specified directory cannot be found in the hierarchy
     * @throws java.io.IOException if an error occurs deleting the encryped
     * files under the directory.
     */
    public synchronized void deleteDirectory(String dirName, List<String> parentDirs) throws NodeNotFoundException, IOException {
        deleteFilesRecursively(dirName, parentDirs);
        fileHierarchy.deleteDirectory(dirName, parentDirs);
        updateHierarchyFile();
    }

    /**
     * Create a new empty directory in the luggage.
     * The given element must not exist in the luggage, but all parent dirs must.
     * @param newDir Element that represents the new directory.
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException If any parent
     * dir didn't previously exist in the luggage
     * @throws net.sf.cryptoluggage.luggage.NodeAlreadyExistsException if node
     * already existed in the luggage.
     * @throws IOException if an error occurs while writing new state to disk.
     */
    public synchronized void createDirectory(LuggageElement newDir) throws NodeNotFoundException, NodeAlreadyExistsException, IOException {
        if (isDirectory(newDir) || isFile(newDir)) {
            throw new NodeAlreadyExistsException("Element is already present in the luggage.");
        }
        LuggageElement parentDir = newDir.getParentElement();
        if (!isDirectory(parentDir)) {
            throw new NodeNotFoundException("Parent element is not a directory.");
        }
        fileHierarchy.addDirectory(newDir.getElementName(), newDir.getParentDirs());
        updateHierarchyFile();
    }

    /**
     * List all elements under and including the given directory
     *
     * @param dir the directory whose contents are returned
     * @return a list with all elements under and including the given directory
     */
    public synchronized List<LuggageElement> listElements(LuggageElement dir) {
        ArrayList<LuggageElement> elements = new ArrayList();
        listElementsRecursive(elements, dir);
        return elements;
    }

    /**
     * Move an element from one place of the luggage to another.
     *
     * @param element the element to be moved
     * @param targetDir the target directory node
     *
     * @throws IOException if file hierarchy file couldn't be updated
     * @throws NodeAlreadyExistsException if an element with the same name already
     * existed in targetDir
     * @throws NodeNotFoundException if either element or targetDir couldn't be
     * found in the Luggage or target dir was a successor of the element to be moved
     */
    public synchronized void moveElement(LuggageElement element, LuggageElement targetDir) throws IOException, NodeNotFoundException, NodeAlreadyExistsException {
        if (targetDir.isSuccessorOf(element)) {
            throw new NodeNotFoundException("Target dir is below source element!");
        }
        fileHierarchy.moveElement(element.getElementName(), element.getParentDirs(), targetDir.getElementName(), targetDir.getParentDirs());
        updateHierarchyFile();
    }

    /**
     * Change the name of an element.
     *
     * @param element the element to rename
     * @param newName the new name to be assigned to the element
     *
     * @throws NodeNotFoundException if the element to rename doesn't exis
     * @throws NodeAlreadyExistsException if an element with the same name already
     * existed
     * @throws IOException if an error occurs updating the hierarchy file
     */
    public synchronized void renameElement(LuggageElement element, String newName) throws NodeNotFoundException, IOException, NodeAlreadyExistsException {
        fileHierarchy.renameElement(element.getElementName(), element.getParentDirs(), newName);
        updateHierarchyFile();
    }

    /**
     * Recursively add all elements under and including current dir to elements-
     *
     * First currentDir is added, then all files under currentDir and then
     * recursively all directories under currentDir.
     * 
     * @param elements list where elements will be added
     * @param currentDir current directory being explored
     */
    private void listElementsRecursive(ArrayList<LuggageElement> elements, LuggageElement currentDir) {
        try {
            ArrayList<String> newParentDirs = new ArrayList(currentDir.getParentDirs());
            elements.add(new LuggageElement(currentDir.getElementName(), (List<String>) newParentDirs.clone()));
            newParentDirs.add(currentDir.getElementName());
            List<TransformationInfo> files = fileHierarchy.getFiles(currentDir.getElementName(), currentDir.getParentDirs());
            for (TransformationInfo ti : files) {
                LuggageElement fileElement = new LuggageElement(ti.getOriginalFileName(), (List<String>) newParentDirs.clone());
                elements.add(fileElement);
            }
            List<String> subDirs = fileHierarchy.getDirectories(currentDir.getElementName(), currentDir.getParentDirs());
            for (String subDir : subDirs) {
                LuggageElement subDirElement = new LuggageElement(subDir, newParentDirs);
                listElementsRecursive(elements, subDirElement);
            }
        } catch (NodeNotFoundException ex) {
            Logger.getLogger(Luggage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Physically create a Luggage in the specified folder with the given pass.
     * The folder must not exist prior to the call of this method.
     * 
     * @param luggageFolder destination folder, cannot exist
     * @param the master password for the Luggage
     * @return A new Luggage instance
     * @throws java.io.IOException if method isn't able to create the luggage
     */
    public static Luggage createLuggage(File luggageFolder, char[] password) throws IOException {
        try {
            if (luggageFolder.exists()) {
                throw new IOException(luggageFolder.getPath() + " already exists");
            }
            if (!luggageFolder.mkdirs()) {
                throw new IOException("Error creating directory '" + luggageFolder.getParent() + "' for luggage");
            }
            ParameterSet parameterSet = ParameterSet.getInstance();
            FileOutputStream fos = new FileOutputStream(luggageFolder.getPath() + File.separator + parameterFileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(parameterSet);
            oos.close();
            fos.close();
            CipherGenerator cg = new CipherGenerator(parameterSet);
            EasyKeyGenerator kg = new EasyKeyGenerator(parameterSet);
            SecretKey key = kg.getKey(password);
            Cipher cipher = cg.generateEncryptionCipher(key);
            fos = new FileOutputStream(luggageFolder + File.separator + hierarchyFileName);
            CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            oos = new ObjectOutputStream(cos);
            FileHierarchy fh = new FileHierarchy();
            MutableTreeNode rootNode = fh.getRootNode();
            rootNode.setUserObject(luggageFolder.getCanonicalPath());
            oos.writeObject(fh);
            oos.close();
            cos.close();
            fos.close();
            return new Luggage(luggageFolder, password);
        } catch (RestrictedEnvironmentException ex) {
            throw new RuntimeException("Error opening created Luggage", ex);
        }
    }

    /**
     * Load the set of parameters used in a luggage given its base dir.
     * 
     * @param luggageBaseDir the directory where the luggage is located
     * @return the set of parameters used in a luggage given its base dir.
     * @throws java.io.IOException if the set of parameters cannot be found
     */
    public static ParameterSet loadParameterSet(File luggageBaseDir) throws IOException {
        try {
            File parametersFile = new File(luggageBaseDir + File.separator + parameterFileName);
            if (!parametersFile.isFile()) {
                throw new IOException("File '" + parametersFile.getPath() + "' " + "couldn't be found or read.");
            }
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(parametersFile));
            ParameterSet parameters = (ParameterSet) ois.readObject();
            return parameters;
        } catch (ClassNotFoundException ex) {
            throw new IOException("Error loading parameters from '" + luggageBaseDir + "'");
        } catch (ClassCastException ex) {
            throw new IOException("Error loading parameters from '" + luggageBaseDir + "'");
        }
    }

    /**
     * Get the root node of the file hierarchy, so that it can be displayed
     * with any TreeNode viewer.
     *
     * @return the root node of the file hierarchy
     */
    public synchronized MutableTreeNode getHierarchyTree() {
        return fileHierarchy.getRootNode();
    }

    private void deleteFileNoUpdate(String fileName, List<String> parentDirs) throws IOException, NodeNotFoundException {
        TransformationInfo ti = fileHierarchy.getFile(fileName, parentDirs);
        fileHierarchy.deleteFile(fileName, parentDirs);
        File file = new File(baseDirectory + File.separator + ti.getEncryptedFileName());
        if (!file.delete()) {
            throw new IOException("Error deleting encrypted file '" + file.getPath() + "'");
        }
    }

    /**
     * Delete all encrypted files contained under the specified directories,
     * without altering the file hierarchy.
     *
     * @param dirName the name of the target directory
     * @param parentDirs the path to the target directory
     * @throws NodeNotFoundException if the directory cannot be found
     * @throws IOException if encrypted files couldn't be deleted
     */
    private void deleteFilesRecursively(String dirName, List<String> parentDirs) throws NodeNotFoundException, IOException {
        List<String> childDirs = fileHierarchy.getDirectories(dirName, parentDirs);
        List<TransformationInfo> childFiles = fileHierarchy.getFiles(dirName, parentDirs);
        parentDirs.add(dirName);
        for (TransformationInfo childFile : childFiles) {
            deleteFileNoUpdate(childFile.getOriginalFileName(), parentDirs);
        }
        for (String childDir : childDirs) {
            deleteFilesRecursively(childDir, parentDirs);
        }
        parentDirs.remove(parentDirs.size() - 1);
    }

    /**
     * Generate a file instance for the encrypted file.
     * This file is guaranteed to have a unique name. A keyed hash function is
     * used so that encrypted file names don't reveal the original file name.
     * 
     * @param inputFile the input file
     * @param parentDirs the dir path where the file is going to be inserted
     * @return
     */
    private File getOutputFile(File inputFile, List<String> parentDirs) {
        StringBuilder nameBuilder = new StringBuilder();
        for (Iterator<String> it = parentDirs.iterator(); it.hasNext(); ) {
            nameBuilder.append(it.next() + File.separator);
        }
        nameBuilder.append(inputFile.getName());
        String completeName = nameBuilder.toString();
        String digestName;
        File digestFile;
        do {
            byte[] a = parameters.getKeyGenerationSalt();
            byte[] b = completeName.getBytes();
            byte[] inputBytes = new byte[a.length + b.length];
            System.arraycopy(a, 0, inputBytes, 0, a.length);
            System.arraycopy(b, 0, inputBytes, a.length, b.length);
            byte[] outputBytes = nameDigester.digest(inputBytes);
            digestName = HexRepresentation.get(outputBytes, true);
            digestFile = new File(baseDirectory + File.separator + digestName);
            completeName += File.separator;
        } while (digestFile.isFile());
        return digestFile;
    }

    /**
     * Get the File instance representing the parameters file,
     * or throw an exception if file doesn't exist or cannot be read.
     *
     * @return the File instance representing the parameters file
     * @throws java.io.IOException if file doesn't exist or cannot be read
     */
    private File getParametersFile() throws IOException {
        File f = new File(baseDirectory + File.separator + parameterFileName);
        if (!f.isFile()) {
            throw new IOException("Parameters file '" + f.getPath() + "'" + " does not exist");
        }
        return f;
    }

    /**
     * Get the File instance representing the hierarchy file,
     * or throw an exception if file doesn't exist or cannot be read.
     *
     * @return the File instance representing the hierarchy file
     * @throws java.io.IOException if file doesn't exist or cannot be read
     */
    private File getHierarchyFile() throws IOException {
        File f = new File(baseDirectory + File.separator + hierarchyFileName);
        if (!f.isFile()) {
            throw new IOException("Parameters file '" + f.getPath() + "'" + " does not exist");
        }
        return f;
    }

    /**
     * Perform actual directory insetion, without updating the file
     * representing the hierarchy-
     * 
     * @param directory directory to insert in the luggage
     * @param parentDirs path where the directory is to be inserted
     * @param progressMessage Progress message to show if process takes long enough.
     * If it's null, no message will be shown
     * 
     * @throws net.sf.cryptoluggage.luggage.NodeAlreadyExistsException
     * @throws java.io.IOException
     * @throws net.sf.cryptoluggage.luggage.NodeNotFoundException
     */
    private void insertDirectoryNoUpdate(File directory, List<String> parentDirs, String progressMessage) throws NodeAlreadyExistsException, IOException, NodeNotFoundException {
        addDirectoryNoUpdate(directory.getName(), parentDirs);
        parentDirs.add(directory.getName());
        File[] children = directory.listFiles();
        for (int i = 0; i < children.length; i++) {
            if (children[i].isFile()) {
                insertFileNoUpdate(children[i], parentDirs, progressMessage + " (" + children[i].getName() + ")");
            }
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                insertDirectoryNoUpdate(children[i], parentDirs, progressMessage);
            }
        }
        parentDirs.remove(parentDirs.size() - 1);
    }

    /**
     * Call insertDirectoryNoUpdate with null progressMessage
     */
    private void insertDirectoryNoUpdate(File directory, List<String> parentDirs) throws NodeAlreadyExistsException, IOException, NodeNotFoundException {
        insertDirectoryNoUpdate(directory, parentDirs, null);
    }

    /**
     * Insert a file in the hierarchy and encrypt data, but don't update
     * hierarchy file.
     */
    private void insertFileNoUpdate(File file, List<String> parentDirs, String insertionMessage) throws NodeNotFoundException, NodeAlreadyExistsException, IOException {
        if (fileHierarchy.containsFile(file.getName(), parentDirs)) {
            throw new NodeAlreadyExistsException("File" + file.getName() + " already exists");
        }
        File outputFile = getOutputFile(file, parentDirs);
        IvParameterSpec randomIV = parameters.getRandomIV();
        fileTransformer.encryptFile(file, outputFile, key, insertionMessage, randomIV);
        TransformationInfo ti = new TransformationInfo(file.getName(), outputFile.getName(), file.length(), outputFile.length(), randomIV);
        fileHierarchy.addFile(ti, parentDirs);
    }

    /**
     * Load the FileHierarchy instance previously serialized and then sealed,
     * using the luggage master key.
     *
     * @throws java.io.IOException if the hierarchy file couldn't be loaded
     */
    private void loadHierarchy() throws IOException {
        try {
            Cipher cipher = cg.generateDecryptionCipher(key);
            FileInputStream fis = new FileInputStream(getHierarchyFile());
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            ObjectInputStream ois = new ObjectInputStream(cis);
            fileHierarchy = (FileHierarchy) ois.readObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException("Error loading hierarchy file", ex);
        } catch (ClassCastException ex) {
            throw new IOException("Error loading hierarchy file", ex);
        }
    }

    /**
     * Load a ParameterSet instance from base dir.
     *
     * @throws java.io.IOException if the ParameterSet instance couldn't be
     * loaded
     */
    private void loadParameters() throws IOException {
        try {
            FileInputStream fis = new FileInputStream(getParametersFile());
            ObjectInputStream ois = new ObjectInputStream(fis);
            parameters = (ParameterSet) ois.readObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException("Error loading the parameter set", ex);
        } catch (ClassCastException ex) {
            throw new IOException("Error loading the parameter set", ex);
        }
    }

    /**
     * Update the hierarchy file, overwriting any previous contents.
     */
    private void updateHierarchyFile() throws IOException {
        Cipher cipher = cg.generateEncryptionCipher(key);
        FileOutputStream fos = new FileOutputStream(getHierarchyFile());
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        ObjectOutputStream oos = new ObjectOutputStream(cos);
        oos.writeObject(fileHierarchy);
        oos.close();
        cos.close();
        fos.close();
    }
}
