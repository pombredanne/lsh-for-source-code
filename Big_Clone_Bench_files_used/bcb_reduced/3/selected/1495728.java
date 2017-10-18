package supersync.file.filesystems;

import java.util.Date;
import java.io.IOException;
import java.security.DigestInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import supersync.file.AbstractFile;
import supersync.sync.prefs.SystemSetup;
import supersync.sync.prefs.server.Server_FTP;
import javax.swing.JOptionPane;
import supersync.ftp.MyFtpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import supersync.file.SimplePermissions;
import supersync.sync.prefs.GlobalPreferences;
import supersync.ui.ServerSelectionForm;
import static org.junit.Assert.*;

/** This class tests the Ftp Files.
 *
 * @author Brandon Drake
 */
public class FtpFileTest {

    protected static Server_FTP server = null;

    /** Returns true if the hashes are the same, and false if they are different.
     */
    public boolean isHashSame(byte[] l_hash1, byte[] l_hash2) {
        for (int i = 0; i < l_hash1.length; i++) {
            if (l_hash1[i] != l_hash2[i]) {
                return false;
            }
        }
        return true;
    }

    public FtpFileTest() {
    }

    static String password = null;

    public MyFtpClient getFtpClient() throws Exception {
        if (null == server) {
            GlobalPreferences prefs = GlobalPreferences.fromFile();
            ServerSelectionForm selectForm = new ServerSelectionForm(null, true);
            selectForm.setMessage("Select an ftp server to test with.");
            selectForm.setServers(prefs.getServers());
            selectForm.setVisible(true);
            if (null == selectForm.getServer()) {
                throw new Exception("User canceled the operation.");
            }
            server = (Server_FTP) selectForm.getServer();
        }
        supersync.file.filesystems.FileSystemServer fileSystemServer = supersync.file.filesystems.FileSystemServer.getInstance(server.getType());
        supersync.file.AbstractFileSystem tempFileSystem = fileSystemServer.getFileSystem(server, new SystemSetup());
        FtpFileSystem fileSystem = (FtpFileSystem) tempFileSystem;
        MyFtpClient ftpClient = fileSystem.getFtpClient();
        ftpClient.setUsername(server.getUserName());
        if (server.isNoPasswordRequired()) {
            password = "";
        } else if (null == password) {
            password = JOptionPane.showInputDialog("Please enter the password for the FTP Server.");
            if (null == password) {
                throw new Exception("User canceled the operation.");
            }
        }
        ftpClient.setPassword(password);
        ftpClient.login();
        return ftpClient;
    }

    public byte[] getMD5Hash(LocalFile l_file) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
        }
        InputStream is = new FileInputStream(l_file.getFile());
        try {
            is = new DigestInputStream(is, md);
        } finally {
            is.close();
        }
        byte[] digest = md.digest();
        return digest;
    }

    public static File getTestDataDirectory() {
        return new File("Test Data");
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Tests uploading and downloading a file.
     */
    @Test
    public void test1() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test Dir 1");
        if (testFolder.exists()) {
            throw new Exception("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = testFolder.child("testFile.txt");
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        LocalFile tempFile = new LocalFile(File.createTempFile("testfile", ".rtf"));
        AbstractFile.copyFile(testFile, tempFile);
        byte[] hash1 = this.getMD5Hash(testDataFolder.child("Test Description.rtf"));
        byte[] hash2 = this.getMD5Hash(tempFile);
        if (false == this.isHashSame(hash1, hash2)) {
            throw new Exception("Uploaded and Downloaded files differ.");
        }
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }

    /**
     * Tests setting the modification date of a file
     */
    @Test
    public void test2() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        fileSystem.ftpClient.setUseCache(false);
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test Dir 2");
        if (testFolder.exists()) {
            throw new Exception("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = testFolder.child("testFile.txt");
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        Date modTime = new Date();
        modTime.setTime(modTime.getTime() - 60000);
        testFile.setLastModified(modTime);
        FtpFile[] files = testFolder.listFiles();
        for (FtpFile file : files) {
            if (false == file.getName().equals("testFile.txt")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            } else if (file.lastModified().getTime() / 1000 != modTime.getTime() / 1000) {
                throw new Exception("Unexpected modification time: " + file.lastModified().toString());
            }
        }
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }

    /**
     * Test the cache capability to make sure it can deal with creating and deleting files and folders.
     */
    @Test
    public void testCache1() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test Cache Dir 1");
        if (testFolder.exists()) {
            throw new Exception("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = testFolder.child("testFile.txt");
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        FtpFile[] files = testFolder.listFiles();
        for (FtpFile file : files) {
            if (false == file.getName().equals("testFile.txt")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
        }
        FtpFile testDir = testFolder.child("test dir");
        testDir.mkdir();
        files = testFolder.listFiles();
        int filesFound = 0;
        for (FtpFile file : files) {
            if (false == file.getName().equals("test dir") && false == file.getName().equals("testFile.txt")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
            filesFound++;
        }
        if (2 != filesFound) {
            throw new Exception("One of the files could not be located.");
        }
        testFile.delete();
        files = testFolder.listFiles();
        filesFound = 0;
        for (FtpFile file : files) {
            if (false == file.getName().equals("test dir")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
            filesFound++;
        }
        if (1 != filesFound) {
            throw new Exception("One of the files could not be located.");
        }
        testDir.delete();
        files = testFolder.listFiles();
        for (FtpFile file : files) {
            throw new Exception("Unexpected file in test folder: " + file.getName());
        }
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }

    /**
     * Test the cache capability to make sure it can deal with changing the modification date on files.
     */
    @Test
    public void testCache2() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test Cache Dir 2");
        if (testFolder.exists()) {
            throw new Exception("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = testFolder.child("testFile.txt");
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        FtpFile[] files = testFolder.listFiles();
        for (FtpFile file : files) {
            if (false == file.getName().equals("testFile.txt")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
        }
        Date lastModified = new Date();
        lastModified.setTime(1324667151000l);
        testFile.setLastModified(lastModified);
        files = testFolder.listFiles();
        for (FtpFile file : files) {
            if (false == file.getName().equals("testFile.txt")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            } else if (file.lastModified().getTime() / 1000 != lastModified.getTime() / 1000) {
                throw new Exception("Incorrect last modified date.");
            }
        }
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }

    /**
     * Test the cache capability to make sure it can deal with changing the modification date on files.
     */
    @Test
    public void testCache3() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test Cache Dir 3");
        if (testFolder.exists()) {
            throw new Exception("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = testFolder.child("testFile.txt");
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        FtpFile testDir = testFolder.child("test dir");
        testDir.mkdir();
        FtpFile[] files = testFolder.listFiles();
        for (FtpFile file : files) {
            if (false == file.getName().equals("testFile.txt") && false == file.getName().equals("test dir")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
        }
        FtpFile testFile2 = testDir.child("testFile2.txt");
        testFile.renameTo(testFile2);
        files = testFolder.listFiles();
        for (FtpFile file : files) {
            if (false == file.getName().equals("test dir")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
        }
        files = testDir.listFiles();
        int filesFound = 0;
        for (FtpFile file : files) {
            if (false == file.getName().equals("testFile2.txt")) {
                throw new Exception("Unexpected file in test folder: " + file.getName());
            }
            filesFound++;
        }
        if (1 != filesFound) {
            throw new Exception("One of the files could not be located.");
        }
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }

    /**
     * This make sure that if we check if a file exists, that is way down in a directory structure that doesn't exist we don't get an error.
     */
    @Test
    public void testExists() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Does").child("Not").child("Exist");
        if (testFolder.exists()) {
            fail("File should not exist.");
        }
        testFolder = fileSystem.getDefaultDirectory().child("DoesNotExist");
        if (testFolder.exists()) {
            fail("File should not exist.");
        }
        fileSystem.ftpClient.logout();
    }

    /**
     * Tests setting permissions.
     */
    @Test
    public void testPermissions() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test Perm 1");
        if (testFolder.exists()) {
            throw new Exception("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = testFolder.child("testFile.txt");
        testFile.ftpClient.setUseCache(false);
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        testFile.setPermissions(SimplePermissions.fromUnixCode(000));
        testFile.updateFileInfo();
        if (false == String.valueOf(testFile.getPermissions().getUnixCode()).equals("000")) {
            throw new Exception("Permissions were not set correctly to 000.");
        }
        testFile.setPermissions(SimplePermissions.fromUnixCode(777));
        testFile.updateFileInfo();
        if (false == String.valueOf(testFile.getPermissions().getUnixCode()).equals("777")) {
            throw new Exception("Permissions were not set correctly to 777.");
        }
        testFile.setPermissions(SimplePermissions.fromUnixCode(744));
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }

    /** This test tries renaming a file
     */
    @Test
    public void testRename() throws Exception {
        FtpFileSystem fileSystem = new FtpFileSystem(getFtpClient(), server, new SystemSetup());
        fileSystem.login();
        FtpFile testFolder = fileSystem.getDefaultDirectory().child("Test rename");
        if (testFolder.exists()) {
            fail("Please delete all the test files");
        } else {
            testFolder.mkdir();
        }
        FtpFile fold1 = testFolder.child("fold1");
        fold1.mkdir();
        LocalFile testDataFolder = new LocalFile(getTestDataDirectory());
        FtpFile testFile = fold1.child("testFile.txt");
        AbstractFile.copyFile(testDataFolder.child("Test Description.rtf"), testFile);
        FtpFile fold2 = testFolder.child("fold2");
        fold1.renameTo(fold2);
        testFile = fold2.child("testFile.txt");
        FtpFile test2File = fold2.child("test2File.txt");
        testFile.renameTo(test2File);
        fileSystem.ftpClient.setUseCache(false);
        if (false == testFolder.child("fold2").child("test2File.txt").exists()) {
            fail("File not renamed correctly.");
        }
        AbstractFile.deleteDirectoryStructure(testFolder);
        fileSystem.ftpClient.logout();
    }
}
