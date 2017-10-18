package org.nigelk.pdfupdateframe;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nigelk.pdfupdateframe.updaters.AddDocumentLanguage;
import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;

public class TestPdfUpdaterBase {

    static File tmpdir = new File("/tmp");

    static File testfile = null;

    @BeforeClass
    public static void runBeforeClass() {
        testfile = new File("testdata/org/nigelk/pdfupdateframe/test1.pdf");
    }

    @AfterClass
    public static void runAfterClass() {
        testfile = null;
    }

    @Before
    public void runBeforeTest() {
    }

    @After
    public void runAfterTest() {
    }

    @Test
    public void noopUpdateIsSameFile() throws Exception {
        byte[] checksumBefore = checksumFile(testfile);
        PdfUpdaterBase pub = new PdfUpdaterBase(testfile.getAbsolutePath());
        File tmpfile = getTmpFile();
        ;
        pub.createUpdate();
        FileOutputStream fos = new FileOutputStream(tmpfile);
        pub.write(fos);
        fos.close();
        byte[] checksumAfter = checksumFile(tmpfile);
        assertArrayEquals(checksumBefore, checksumAfter);
    }

    byte[] checksumFile(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(f);
        byte[] buf = new byte[8192];
        int howMany = 0;
        while (howMany >= 0) {
            howMany = fis.read(buf);
            if (howMany >= 0) {
                md.update(buf, 0, howMany);
            }
        }
        return md.digest();
    }

    @Test
    public void addLanguageCode() throws Exception {
        PdfUpdaterBase pub = new PdfUpdaterBase(testfile.getAbsolutePath());
        PdfDictionary trailer = pub.getTrailer();
        PdfDictionary root = trailer.getAsDict(PdfName.ROOT);
        assertFalse(root.contains(PdfName.LANG));
        pub.addUpdater(new AddDocumentLanguage("es-MX"));
        File tmpfile = getTmpFile();
        pub.createUpdate();
        FileOutputStream fos = new FileOutputStream(tmpfile);
        pub.write(fos);
        fos.close();
        PdfReader pr = new PdfReader(tmpfile.getAbsolutePath());
        trailer = pr.getTrailer();
        root = trailer.getAsDict(PdfName.ROOT);
        assertTrue(root.contains(PdfName.LANG));
        assertEquals("es-MX", root.getAsString(PdfName.LANG).toString());
        pub = new PdfUpdaterBase(tmpfile.getAbsolutePath());
        pub.addUpdater(new AddDocumentLanguage("en-UK"));
        File tmpfile2 = getTmpFile();
        ;
        pub.createUpdate();
        fos = new FileOutputStream(tmpfile2);
        pub.write(fos);
        fos.close();
        pr = new PdfReader(tmpfile2.getAbsolutePath());
        trailer = pr.getTrailer();
        root = trailer.getAsDict(PdfName.ROOT);
        assertTrue(root.contains(PdfName.LANG));
        assertEquals("en-UK", root.getAsString(PdfName.LANG).toString());
    }

    private File getTmpFile() throws IOException {
        File tmp = File.createTempFile(this.getClass().getName(), ".pdf", tmpdir);
        tmp.deleteOnExit();
        return tmp;
    }

    @Test
    public void increaseSizeKey() throws Exception {
        PdfUpdaterBase pub = new PdfUpdaterBase(testfile.getAbsolutePath());
        PdfDictionary trailer = pub.getTrailer();
        assertEquals(12, trailer.getAsNumber(PdfName.SIZE).intValue());
        Updater u = new Updater() {

            public boolean update(PdfUpdaterBase pb) throws UpdateFailedException {
                PRIndirectReference pir = pb.addPdfObject(new PdfArray());
                return true;
            }

            public String getDescription() {
                return "test updater adds empty unused pdfarray";
            }
        };
        pub.addUpdater(u);
        File tmpfile = getTmpFile();
        ;
        pub.createUpdate();
        FileOutputStream fos = new FileOutputStream(tmpfile);
        pub.write(fos);
        fos.close();
        PdfReader pr = new PdfReader(tmpfile.getAbsolutePath());
        trailer = pr.getTrailer();
        assertEquals(13, trailer.getAsNumber(PdfName.SIZE).intValue());
    }

    @Test
    public void testSomeMutabilityCases() throws Exception {
        PdfUpdaterBase pub = new PdfUpdaterBase(testfile.getAbsolutePath());
        pub.addUpdater(new AddDocumentLanguage("es-MX"));
        pub.createUpdate();
        try {
            pub.getNewObjectNumber(13, 0);
            fail();
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testBetterXrefBuilder() throws Exception {
        PdfUpdaterBase pub = new PdfUpdaterBase(testfile.getAbsolutePath());
        PdfDictionary trailer = pub.getTrailer();
        Updater u = new Updater() {

            public boolean update(PdfUpdaterBase pb) throws UpdateFailedException {
                PRIndirectReference pir = pb.addPdfObject(new PdfArray());
                pir = pb.addPdfObject(new PdfDictionary());
                return true;
            }

            public String getDescription() {
                return "test updater adds empty unused pdfarray and pdfdictionary";
            }
        };
        pub.addUpdater(u);
        pub.addUpdater(new AddDocumentLanguage("en-GB"));
        File tmpfile = getTmpFile();
        ;
        pub.createUpdate();
        FileOutputStream fos = new FileOutputStream(tmpfile);
        pub.write(fos);
        fos.close();
        PdfReader pr = new PdfReader(tmpfile.getAbsolutePath());
        int offset = pr.getLastXref();
        int length = pr.getFileLength();
        FileInputStream fis = new FileInputStream(tmpfile);
        fis.skip(offset);
        byte[] arr = new byte[length - offset];
        int howMany = fis.read(arr);
        assertEquals(length - offset, howMany);
        String frag = new String(arr, Charset.forName("ISO-8859-1").name());
        assertTrue(frag.startsWith("xref\n0 2\n0000000000 65535 f \n0000004376 00000 n \n" + "12 2\n0000004465 00000 n \n0000004355 00000 n \ntrailer"));
    }
}
