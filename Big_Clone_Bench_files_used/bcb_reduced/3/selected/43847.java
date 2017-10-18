package be.belgium.eid.objects;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import be.belgium.eid.eidcommon.ByteConverter;
import be.belgium.eid.objects.SmartCardReadable;

/**
 * The IDPhoto class contains the photo of the holder of the current beID card.
 * The validity of this photo can be validated against it's hash in the IDData
 * class. The byte code of the picture is structured in JPEG format
 * 
 * @author Kristof Overdulve
 * @version 1.0.0 08 Dec 2007
 */
public class IDPhoto implements SmartCardReadable {

    /** Contains the ID specific file attributes to read from on the smart card */
    public static final char[] fgPHOTO = { fgDataTag, fgDataTagPHOTO };

    /** Contains the maximum size (in number of bytes) that the IDPhoto can take */
    public static final int fgMAX_RAW_LEN = 4096;

    /** Contains the extension of the photo that indicates the format */
    public static final String EXTENSION = ".jpeg";

    /** Contains the data of the photo */
    private final byte[] fPhoto;

    /**
	 * Parses the given stream of characters into a valid IDPhoto object.
	 * 
	 * @param characterStream
	 *            is the stream of characters to parse
	 * @return a fully initialized IDPhoto object
	 */
    public static IDPhoto parse(final byte[] characterStream) {
        return new IDPhoto(characterStream);
    }

    /**
	 * Initializes the IDPhoto object with the given data.
	 * 
	 * @param photo
	 *            contains the byte information of the photo of the ID
	 */
    public IDPhoto(final byte[] photo) {
        fPhoto = photo.clone();
    }

    /**
	 * Returns the photo of the holder of the ID as an array of bytes in JPEG
	 * format.
	 * 
	 * @return the photo
	 */
    public byte[] getPhoto() {
        return fPhoto.clone();
    }

    /**
	 * Returns the photo of the holder of the ID as an image.
	 * 
	 * @return the image of the photo
	 */
    public Image getImage() {
        return Toolkit.getDefaultToolkit().createImage(fPhoto);
    }

    /**
	 * Writes the photo of the holder of the ID to the given file. The image is
	 * saved as a JPEG image so only the name of the file should be given without
	 * the extension. <br/> <b>E.g.:</b> imagine you want to save the file as
	 * 'mypicture.png'. The given filename must be 'mypicture'.
	 * 
	 * @throws IOException
	 *             indicates that the file couldn't be opened or couldn't be
	 *             written to
	 */
    public void writeToFile(final String filename) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename + EXTENSION);
            fos.write(fPhoto);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
	 * Verifies the picture against the given hash code. Only when verified
	 * correctly can the user rely on the validity of the picture.
	 * 
	 * @param hash
	 *            is the hash to verify against
	 * @return whether the verification succeeded
	 */
    public boolean verifyHash(final byte[] hash) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];
            md.update(fPhoto);
            sha1hash = md.digest();
            final String actualHash = ByteConverter.hexify(sha1hash);
            return actualHash.equals(ByteConverter.hexify(hash));
        } catch (NoSuchAlgorithmException e) {
        }
        throw new IllegalStateException();
    }
}
