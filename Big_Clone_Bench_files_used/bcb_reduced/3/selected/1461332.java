package org.dcm4chex.archive.hsm.spi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import org.dcm4che.util.MD5Utils;
import org.dcm4chex.archive.hsm.spi.utils.HsmUtils;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.apache.commons.compress.tar.TarInputStream;
import org.apache.commons.compress.tar.TarEntry;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;

/**
 * @author Fuad Ibrahimov
 * @since Feb 19, 2007
 */
public class TarServiceTests {

    private static final String FILE2 = "org/dcm4chex/archive/hsm/spi/test2.txt";

    private static final String FILE1 = "org/dcm4chex/archive/hsm/spi/test1.txt";

    private File file1;

    private File file2;

    private TarService tarService;

    private String tempDir;

    private File destDir;

    private File tarFile;

    private FileInfo fileInfo1;

    private FileInfo fileInfo2;

    private List<FileInfo> files;

    @BeforeMethod
    public void setUp() throws Exception {
        file1 = HsmUtils.classpathResource(FILE1);
        file2 = HsmUtils.classpathResource(FILE2);
        tarService = new TarService();
        tempDir = HsmUtils.classpathResource(".").getCanonicalPath();
        destDir = new File(tempDir, "destinationDir");
        destDir.mkdirs();
        fileInfo1 = TestUtils.newFileInfo(1, FILE1, tempDir, "1.33.44.55", file1.length(), "1.55.44.1", new String(md5Digest(file1)), 0);
        fileInfo2 = TestUtils.newFileInfo(2, FILE2, tempDir, "1.33.44.55", file2.length(), "1.55.44.2", new String(md5Digest(file2)), 0);
        files = new ArrayList<FileInfo>() {

            {
                add(fileInfo1);
                add(fileInfo2);
            }
        };
        tarFile = tarService.pack(destDir.getCanonicalPath(), files);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        TestUtils.cleanup(tarFile, tempDir);
        TestUtils.cleanup(new File(destDir, FILE1), tempDir);
        TestUtils.cleanup(new File(destDir, TarService.MD5_SUM), tempDir);
        TestUtils.cleanup(new File(destDir, FILE2), tempDir);
    }

    @Test
    public void canPackGivenFiles() throws Exception {
        assertTrue(tarFile.exists(), "Didn't create expected tar file.");
        assertTrue(tarFile.length() > 0, "Didn't create expected tar file.");
        assertEquals(tarFile.getCanonicalPath(), new File(destDir, "org/dcm4chex/archive/hsm/spi-test1.txt.tar").getCanonicalPath());
        TarInputStream tis = null;
        try {
            tis = new TarInputStream(new FileInputStream(tarFile));
            TarEntry nextEntry = tis.getNextEntry();
            assertTrue(nextEntry != null, "Tar file didn't contain any entries.");
            assertEquals(nextEntry.getName(), TarService.MD5_SUM);
            assertEquals(tis.getNextEntry().getName(), fileInfo1.fileID.replaceAll(File.separator, "/"));
            assertEquals(tis.getNextEntry().getName(), fileInfo2.fileID.replaceAll(File.separator, "/"));
        } finally {
            if (tis != null) tis.close();
        }
    }

    @Test
    public void canUnpackTarFile() throws Exception {
        tarService.unpack(tarFile, destDir.getCanonicalPath());
        assertTrue(new File(destDir, FILE1).exists());
        assertTrue(new File(destDir, FILE2).exists());
    }

    @Test
    public void unpackReplacesExistingFiles() throws Exception {
        File newFile = new File(destDir, FILE1);
        newFile.getParentFile().mkdirs();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(file1);
            fos = new FileOutputStream(newFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
        } finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
        String file1Md5 = new String(md5Digest(file1));
        String newFileMd5 = new String(md5Digest(newFile));
        assertEquals(newFileMd5, file1Md5);
        tarService.unpack(tarFile, destDir.getCanonicalPath());
        File exFile1 = new File(destDir, FILE1);
        File exFile2 = new File(destDir, FILE2);
        assertTrue(exFile1.exists());
        assertTrue(exFile2.exists());
        assertEquals(new String(md5Digest(exFile1)), file1Md5);
    }

    @Test(expectedExceptions = { FailedDigestCheckException.class })
    public void unpackChecksMd5Sums() throws Exception {
        fileInfo2.md5 = "123456789012345678901234567890ce";
        tarService.pack(destDir.getCanonicalPath(), files);
        try {
            tarService.unpack(tarFile, destDir.getCanonicalPath());
        } catch (FailedDigestCheckException e) {
            assertFalse(new File(destDir, FILE1).exists());
            File exFile2 = new File(destDir, FILE2);
            assertFalse(exFile2.exists());
            assertFalse(exFile2.getParentFile().exists());
            assertEquals(e.getMessage(), MessageFormat.format(TarService.FAILED_DIGEST_CHECK, "MD5", FILE2, fileInfo2.md5, new String(md5Digest(file2))));
            throw e;
        }
    }

    @Test(expectedExceptions = { FailedDigestCheckException.class })
    public void unpackThrowsExceptionIfUnexpectedTarEntry() throws Exception {
        fileInfo2.md5 = "";
        tarService.pack(destDir.getCanonicalPath(), files);
        try {
            tarService.unpack(tarFile, destDir.getCanonicalPath());
        } catch (FailedDigestCheckException e) {
            assertFalse(new File(destDir, FILE1).exists());
            assertFalse(new File(destDir, FILE2).exists());
            assertEquals(e.getMessage(), MessageFormat.format(TarService.UNEXPECTED_TAR_ENTRY, FILE2));
            throw e;
        }
    }

    @Test
    public void canUnpackTarFileWithoutMd5Check() throws Exception {
        tarService.setCheckMd5(false);
        tarService.unpack(tarFile, destDir.getCanonicalPath());
        assertTrue(new File(destDir, FILE1).exists());
        assertTrue(new File(destDir, FILE2).exists());
        assertTrue(new File(destDir, TarService.MD5_SUM).exists());
    }

    private char[] md5Digest(File file) throws NoSuchAlgorithmException, IOException {
        DigestInputStream dis = null;
        try {
            dis = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));
            byte[] buf = new byte[32];
            while (dis.read(buf) > 0) {
            }
            return MD5Utils.toHexChars(dis.getMessageDigest().digest());
        } finally {
            if (dis != null) dis.close();
        }
    }
}
