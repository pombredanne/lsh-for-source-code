package pdfManagement;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import logManagement.Log4k;

/**
 *
 * @author administrator
 */
public class pdfPrinter {

    private static final String font = FontFactory.HELVETICA;

    private static String md5(String input) {
        String res = "";
        try {
            MessageDigest cript = MessageDigest.getInstance("MD5");
            cript.reset();
            cript.update(input.getBytes());
            byte[] md5 = cript.digest();
            String tmp = "";
            for (int i = 0; i < md5.length; i++) {
                tmp = (Integer.toHexString(0xFF & md5[i]));
                if (tmp.length() == 1) {
                    res += "0" + tmp;
                } else {
                    res += tmp;
                }
            }
        } catch (NoSuchAlgorithmException ex) {
            Log4k.error(pdfPrinter.class.getName(), ex.getMessage());
        }
        return res;
    }

    private static void createParagraph(Document doc, String text, float size, int allignament) throws DocumentException {
        Paragraph p = new Paragraph(text, FontFactory.getFont(font, size));
        p.setAlignment(allignament);
        p.setSpacingBefore(1f);
        p.setSpacingAfter(1f);
        doc.add(p);
    }

    ;

    public static synchronized String print(String path, String date, String object, String text, String signature) throws PdfException {
        String fileName = null;
        try {
            Document document = new Document();
            fileName = md5(date + object + text + signature) + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(path + File.separator + fileName));
            Log4k.debug(pdfPrinter.class.getName(), "-------------------> Percorso file: " + path + File.separator + fileName);
            document.open();
            createParagraph(document, date, 10, Element.ALIGN_RIGHT);
            createParagraph(document, object, 14, Element.ALIGN_LEFT);
            createParagraph(document, text, 10, Element.ALIGN_JUSTIFIED);
            createParagraph(document, signature, 10, Element.ALIGN_RIGHT);
            document.close();
        } catch (DocumentException ex) {
            Log4k.error(pdfPrinter.class.getName(), ex.getMessage());
        } catch (FileNotFoundException ex) {
            Log4k.error(pdfPrinter.class.getName(), ex.getMessage());
        }
        if (fileName == null) throw (new PdfException("Error in PDF creation"));
        return fileName;
    }
}
