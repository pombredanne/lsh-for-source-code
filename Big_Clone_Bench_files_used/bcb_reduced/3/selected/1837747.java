package org.pdfclown.files;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.pdfclown.documents.Document;
import org.pdfclown.documents.interchange.metadata.Information;
import org.pdfclown.objects.PdfArray;
import org.pdfclown.objects.PdfDirectObject;
import org.pdfclown.objects.PdfName;
import org.pdfclown.objects.PdfObjectWrapper;
import org.pdfclown.objects.PdfString;
import org.pdfclown.objects.PdfString.SerializationModeEnum;
import org.pdfclown.tokens.CharsetName;
import org.pdfclown.tokens.Writer;
import org.pdfclown.util.NotImplementedException;

/**
  File identifier [PDF:1.7:10.3].

  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @since 0.1.2
  @version 0.1.2, 01/29/12
*/
public final class FileIdentifier extends PdfObjectWrapper<PdfArray> {

    /**
    Gets an existing file identifier.

    @param baseObject Base object to wrap.
  */
    public static FileIdentifier wrap(PdfDirectObject baseObject) {
        return baseObject != null ? new FileIdentifier(baseObject) : null;
    }

    private static PdfArray createBaseDataObject() {
        return new PdfArray(PdfString.Empty, PdfString.Empty);
    }

    private static void digest(MessageDigest digest, Object value) throws UnsupportedEncodingException {
        digest.update(value.toString().getBytes(CharsetName.ISO88591));
    }

    /**
    Creates a new direct file identifier.
  */
    public FileIdentifier() {
        this(createBaseDataObject());
    }

    /**
    Creates a new indirect file identifier.
  */
    public FileIdentifier(File context) {
        super(context, createBaseDataObject());
    }

    /**
    Instantiates an existing file identifier.

    @param baseObject Base object.
  */
    private FileIdentifier(PdfDirectObject baseObject) {
        super(baseObject);
    }

    @Override
    public Object clone(Document context) {
        throw new NotImplementedException();
    }

    /**
    Gets the permanent identifier based on the contents of the file at the time it was originally
    created.
  */
    public String getBaseID() {
        return (String) ((PdfString) getBaseDataObject().get(0)).getValue();
    }

    /**
    Gets the changing identifier based on the file's contents at the time it was last updated.
  */
    public String getVersionID() {
        return (String) ((PdfString) getBaseDataObject().get(1)).getValue();
    }

    /**
    Computes a new version identifier based on the file's contents.
    This method is typically invoked internally during file serialization.

    @param writer File serializer.
  */
    public void update(Writer writer) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm unavailable.", e);
        }
        File file = writer.getFile();
        try {
            digest(md5, new Long(System.currentTimeMillis()));
            if (file.getPath() != null) {
                digest(md5, file.getPath());
            }
            digest(md5, new Long(writer.getStream().getLength()));
            Information information = file.getDocument().getInformation();
            if (information != null) {
                for (Map.Entry<PdfName, PdfDirectObject> informationObjectEntry : information.getBaseDataObject().entrySet()) {
                    digest(md5, informationObjectEntry.getKey());
                    digest(md5, informationObjectEntry.getValue());
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("File identifier digest failed.", e);
        }
        PdfString versionID = new PdfString(md5.digest(), SerializationModeEnum.Hex);
        getBaseDataObject().set(1, versionID);
        if (getBaseDataObject().get(0).equals(PdfString.Empty)) {
            getBaseDataObject().set(0, versionID);
        }
    }
}
