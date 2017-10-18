package net.sf.vorg.vorgautopilot.parsers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import net.sf.core.AbstractXMLHandler;
import net.sf.vorg.core.enums.InputHandlerStates;
import net.sf.vorg.core.enums.InputTypes;
import net.sf.vorg.core.exceptions.DataLoadingException;
import net.sf.vorg.core.models.VORGURLRequest;
import net.sf.vorg.vorgautopilot.core.IInputHandler;
import net.sf.vorg.vorgautopilot.models.PilotBoat;
import net.sf.vorg.vorgautopilot.models.PilotCommand;
import net.sf.vorg.vorgautopilot.models.PilotModelStore;

@SuppressWarnings("deprecation")
public abstract class ABoatRouteParser extends AbstractXMLHandler implements IInputHandler {

    private static Logger logger = Logger.getLogger("net.sf.vorg.vorgautopilot.parsers");

    protected String inputReference = null;

    protected PilotModelStore boatStore = null;

    protected PilotBoat boatConstruction = null;

    protected PilotCommand buildUpCommand = null;

    protected InputHandlerStates state = InputHandlerStates.INVALID;

    protected byte[] hash;

    protected Exception lastException = null;

    protected Calendar lastUpdate = null;

    public ABoatRouteParser(final String targetInput) {
        setInput(targetInput);
    }

    public void clearUpdate() {
        hash = new byte[0];
    }

    public void endElement(final String name) throws SAXException {
        this.endElement(name, name, name);
    }

    @Override
    public void endElement(final String uri, final String localName, final String name) throws SAXException {
        super.endElement(uri, localName, name);
        if (name.toLowerCase().equals("boat")) if (null != boatStore) {
            boatStore.addBoat(boatConstruction);
        }
        if (name.toLowerCase().equals("pilotcommand")) if (null != boatConstruction) {
            boatConstruction.addCommand(buildUpCommand);
        }
    }

    public String getFilePath() {
        return inputReference;
    }

    public InputHandlerStates getState() {
        return state;
    }

    /**
	 * The byte[] returned by MessageDigest does not have a nice textual representation, so some form of
	 * encoding is usually performed.
	 * 
	 * This implementation follows the example of David Flanagan's book "Java In A Nutshell", and converts a
	 * byte array into a String of hex characters.
	 * 
	 * Another popular alternative is to use a "Base64" encoding.
	 */
    public String hexEncode(final byte[] aInput) {
        if (null == aInput) return "INVALID DATA";
        final StringBuilder result = new StringBuilder();
        final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            final byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString().toUpperCase();
    }

    /**
	 * Configures the input to the file path received as the parameter. Checks the input state and updates the
	 * change detection flags and the calculation of the current file hash. During the hash calculation the file
	 * existence is verified.
	 */
    public void setInput(final String fileName) {
        if (null == fileName) {
            this.setState(InputHandlerStates.INVALID);
            inputReference = null;
            lastException = null;
            lastUpdate = null;
        } else {
            inputReference = fileName;
            lastException = null;
            lastUpdate = null;
            this.setState(InputHandlerStates.LINKED);
            if (null != boatStore) {
                this.setState(InputHandlerStates.READY);
            }
            final byte[] localHash = computeHash();
            ABoatRouteParser.logger.fine("Computed hash for file [" + fileName + "] is " + hexEncode(localHash));
            if (null != lastException) {
                this.setState(InputHandlerStates.ERROR);
            } else {
                hash = localHash;
                lastUpdate = null;
            }
        }
    }

    public void setStore(final PilotModelStore newStore) {
        if (null != newStore) {
            boatStore = newStore;
            if (state == InputHandlerStates.LINKED) {
                this.setState(InputHandlerStates.READY);
            }
        }
    }

    /**
	 * Method called during the XML processing of the lines and tags. Most of the tags pass this call to the
	 * PilotCommand for more processing.
	 */
    public void startElement(final String name, final Attributes attributes) throws SAXException {
        if (name.toLowerCase().equals("pilotcommand")) {
            buildUpCommand = new PilotCommand(validateNotNull(attributes, "type"));
            buildUpCommand.startElement(name, attributes);
        }
        if (name.toLowerCase().equals("pilotlimits")) if (null != buildUpCommand) {
            buildUpCommand.startElement(name, attributes);
        }
        if (name.toLowerCase().equals("pilotlimit")) if (null != buildUpCommand) {
            buildUpCommand.startElement(name, attributes);
        }
        if (name.toLowerCase().equals("waypointlist")) if (null != buildUpCommand) {
            buildUpCommand.startElement(name, attributes);
        }
        if (name.toLowerCase().equals("waypoint")) if (null != buildUpCommand) {
            buildUpCommand.startElement(name, attributes);
        }
    }

    /**
	 * Computes a new file message digest and compares it to the current stored hash key. Returns true is both
	 * keys are the same that means that the file has not changed since the last verification.
	 */
    protected boolean checkHashCode() {
        final byte[] newHash = computeHash();
        return MessageDigest.isEqual(newHash, hash);
    }

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

    protected boolean needsReload() {
        if (state != InputHandlerStates.READY) return false;
        if (!checkHashCode()) return true; else if (null == lastUpdate) return true; else {
        }
        return false;
    }

    protected InputStream openInput(final String ref) throws FileNotFoundException, DataLoadingException, IOException {
        InputStream input = null;
        if (getType() == InputTypes.XML) {
            input = new BufferedInputStream(new FileInputStream(ref));
            return input;
        }
        if (getType() == InputTypes.HTTP) {
            try {
                final URL url = new URL(ref);
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                input = new StringBufferInputStream(VORGURLRequest.getResourceData(conn));
                return input;
            } catch (final MalformedURLException e) {
                throw new DataLoadingException("The input URL for resource is not properly specified.");
            }
        }
        throw new DataLoadingException("The input type is not specified or is not supported.");
    }

    protected void setState(final InputHandlerStates newState, final Exception exception) {
        this.setState(newState);
        lastException = exception;
    }

    private String bufferFileData() throws IOException {
        final BufferedReader input = new BufferedReader(new FileReader(inputReference));
        final StringBuffer buffer = new StringBuffer();
        String line = input.readLine();
        while (null != line) {
            buffer.append(line);
            line = input.readLine();
        }
        input.close();
        return buffer.toString();
    }

    private void setState(final InputHandlerStates newState) {
        state = newState;
    }
}
