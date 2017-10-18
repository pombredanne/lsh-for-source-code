package net.sf.vorg.vorgautopilot.parsers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import net.sf.vorg.core.enums.InputTypes;
import net.sf.vorg.core.models.VORGURLRequest;

public class HTTPInputParser extends XMLFileParser {

    private static Logger logger = Logger.getLogger("net.sf.vorg.vorgautopilot.parsers");

    public HTTPInputParser(final String targetInput) {
        super(targetInput);
    }

    @Override
    public InputTypes getType() {
        return InputTypes.HTTP;
    }

    /**
	 * Computes a new file message digest and compares it to the current stored hash key. Returns true is both
	 * keys are the same that means that the file has not changed sinde the last verification.
	 */
    @Override
    protected boolean checkHashCode() {
        final byte[] newHash = computeHash();
        return MessageDigest.isEqual(newHash, hash);
    }

    @Override
    protected byte[] computeHash() {
        try {
            final MessageDigest inputHash = MessageDigest.getInstance("SHA");
            inputHash.update(bufferFileData().getBytes());
            return inputHash.digest();
        } catch (final NoSuchAlgorithmException nsae) {
            lastException = nsae;
            return new byte[0];
        } catch (final IOException ioe) {
            lastException = ioe;
            return new byte[0];
        }
    }

    private String bufferFileData() throws IOException {
        final URL url = new URL(inputReference);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return VORGURLRequest.getResourceData(conn);
    }
}
