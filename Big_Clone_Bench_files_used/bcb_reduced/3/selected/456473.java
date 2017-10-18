package netblend;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 * The <code>NetBlendProtocol</code> class represents the communications
 * protocol used between the controller application and each slave.
 * <p>
 * There are two main types of command: Immediate commands and more lengthy
 * commands. Immediate commands execute linearly in the same thread as that used
 * for communication. No other commands can be dealt with until they finish so
 * they must take little time to complete. The following outline is used to
 * construct immediate commands:
 * <p>
 * <table>
 * <tr>
 * <td>--&gt;
 * <td>COMMAND_ID
 * <tr>
 * <td>--&gt;
 * <td>Various fields where any <code>String</code>s must be preceeded by
 * their length.
 * <tr>
 * <td>&lt;--
 * <td>OK_RESPONSE | ERROR at any point where an error could be found with
 * field data. <br>
 * <tr>
 * <td>--&gt;
 * <td>More fields, as required... <br>
 * <tr>
 * <td>&lt;--
 * <td>...with responses when necessary.<br>
 * <tr>
 * <td>&lt;--
 * <td>OK_RESPONSE | ERROR to indicate the success of the command.<br>
 * </table>
 * <p>
 * The more lengthy commands are executed in a separate thread and once started
 * provide no direct feedback on their success. To gain feedback an immediate
 * command to check and report on the status and progress of such a lengthy
 * command must be provided. Only one of these lengthy commands must be allowed
 * to execute at any time. The outline for a lengthy command is as follows:
 * <p>
 * <table>
 * <tr>
 * <td>--&gt;
 * <td>COMMAND_ID <br>
 * <tr>
 * <td>&lt;--
 * <td>OK_RESPONSE | SERVER_BUSY <br>
 * <tr>
 * <td>--&gt;
 * <td>Various fields where any <code>String</code>s must be preceeded by
 * their length. <br>
 * <tr>
 * <td>&lt;--
 * <td>OK_RESPONSE | ERROR at any point where an error could be found with
 * field data. <br>
 * <tr>
 * <td>--&gt;
 * <td>More fields, as required... <br>
 * <tr>
 * <td>&lt;--
 * <td>...with responses when necessary.<br>
 * <tr>
 * <td>&lt;--
 * <td>OK_RESPONSE to indicate that the command has been started.<br>
 * </table>
 * <p>
 * 
 * 
 * @author Ian Thompson
 * 
 */
public class NetBlendProtocol {

    /**
	 * Indicates a request for a connection to the files server.
	 */
    public static final int FILES_SERVER_CONNECTION = -101;

    /**
	 * Indicates a request for a connection to the render server.
	 */
    public static final int RENDER_SERVER_CONNECTION = -102;

    /**
	 * Command ID for listing files in the source directory.
	 */
    public static final int LIST_SOURCE_FILES_COMMAND = -1101;

    /**
	 * Command ID for listing files in the output directory.
	 */
    public static final int LIST_OUTPUT_FILES_COMMAND = -1102;

    /**
	 * Command ID for uploads to the source directory.
	 */
    public static final int UPLOAD_SOURCE_FILE_COMMAND = -1103;

    /**
	 * Command ID for downloads from the output directory.
	 */
    public static final int DOWNLOAD_OUTPUT_FILE_COMMAND = -1104;

    /**
	 * Command ID for deleting a file from the source directory.
	 */
    public static final int DELETE_SOURCE_FILE_COMMAND = -1105;

    /**
	 * Command ID for deleting a file from the output directory.
	 */
    public static final int DELETE_OUTPUT_FILE_COMMAND = -1106;

    /**
	 * Command ID for cancelling a currently running transfer command. Not to be
	 * used as a command on its own.
	 */
    public static final int CANCEL_TRANSFER_COMMAND = -1107;

    /**
	 * Indicates that no more data is to be sent for the current command. Used
	 * for both directions of transfer.
	 */
    public static final int END_OF_DATA_RESPONSE = -1201;

    /**
	 * Indicates that a transfer was cancelled.
	 */
    public static final int TRANSFER_CANCELLED_RESPONSE = -1202;

    /**
	 * Indicates that a file reading operation failed.
	 */
    public static final int FILE_READ_ERROR = -1301;

    /**
	 * Indicates that a file writing operation failed.
	 */
    public static final int FILE_WRITE_ERROR = -1302;

    /**
	 * Indicates that the file deletion operation failed.
	 */
    public static final int DELETE_FAILED_ERROR = -1303;

    /**
	 * Command ID for the retrieval of the current progress of rendering.
	 */
    public static final int GET_RENDER_FINSIHED_COMMAND = -2101;

    /**
	 * Command ID for starting the rendering of a scene.
	 */
    public static final int RENDER_SCENE_COMMAND = -2102;

    /**
	 * Command ID for cancelling a rendering.
	 */
    public static final int TERMINATE_RENDER_COMMAND = -2103;

    /**
	 * Replied if rendering finished normally.
	 */
    public static final int RENDERING_FINISHED_RESPONSE = -2201;

    /**
	 * Replied if rendering was terminated.
	 */
    public static final int RENDERING_TERMINATED_RESPONSE = -2202;

    /**
	 * Replied if part is still rendering.
	 */
    public static final int STILL_RENDERING_RESPONSE = -2203;

    /**
	 * Indicates that an error occurred during rendering.
	 */
    public static final int RENDERING_ERROR = -2301;

    /**
	 * Indicates that no rendering was performed.
	 */
    public static final int NOT_RENDERING_ERROR = -2302;

    /**
	 * Indicates that the server is busy rendering and cannot render another
	 * scene.
	 */
    public static final int BUSY_RENDERING_ERROR = -2303;

    /**
	 * Command ID for disconnecting the client and server, closing all
	 * connections.
	 */
    public static final int DISCONNECT_FROM_SERVER_COMMAND = -3101;

    /**
	 * Command ID for shutting down the server application.
	 */
    public static final int SHUTDOWN_SERVER_COMMAND = -3102;

    /**
	 * Indicates that it is okay to continue with the command.
	 */
    public static final int OK_RESPONSE = -3201;

    /**
	 * Indicates the given password was incorrect.
	 */
    public static final int PASSWORD_INCORRECT_ERROR = -3301;

    /**
	 * Indicates one or more files were not found.
	 */
    public static final int FILE_NOT_FOUND_ERROR = -3302;

    /**
	 * Indicates that a supplied file path was malformed.
	 */
    public static final int BAD_PATH_ERROR = -3303;

    /**
	 * The default port to serve on.
	 */
    public static final int DEFAULT_PORT = 44001;

    /**
	 * The port used for discovery of slave units on the network.
	 */
    public static final String DISCOVERY_ADDRESS = "230.44.0.4";

    /**
	 * The port used for discovery of slave units on the network.
	 */
    public static final int DISCOVERY_PORT = 44004;

    /**
	 * The number of milliseconds to wait for discovery responses.
	 */
    public static final int DISCOVERY_PERIOD = 5000;

    /**
	 * The size of chunks of data when uploading or downloading files.
	 */
    public static final int DATA_CHUNK_SIZE = 4096;

    /**
	 * Obtains a hash from the specified plaintext.
	 * 
	 * @param plaintext
	 *            text to hash.
	 * @return the hashed text.
	 */
    public static String getHash(String plaintext) {
        String hash = null;
        try {
            String text = plaintext;
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-256");
                md.update(text.getBytes("UTF-8"));
                byte[] rawBytes = md.digest();
                hash = new BASE64Encoder().encode(rawBytes);
            } catch (NoSuchAlgorithmException e) {
            }
        } catch (IOException e) {
        }
        return hash;
    }
}
