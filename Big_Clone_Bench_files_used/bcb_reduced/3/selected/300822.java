package il.ac.biu.cs.grossmm.foo.protocol;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RegisterRequest extends Request {

    String user;

    String digest;

    static MessageDigest d;

    static Lock lock = new ReentrantLock();

    static {
        try {
            d = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public RegisterRequest(String user, String password) {
        super();
        this.user = user;
        this.digest = encode(password);
    }

    protected RegisterRequest(String tid, String[] params) {
        super(tid);
        this.user = params[0];
        this.digest = params[1];
    }

    static byte toHex(int n) {
        if (n < 10) return (byte) (n + '0');
        return (byte) (n + 'A' - 10);
    }

    /**
	 * @param input
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    private static String encode(String text) {
        byte[] input = text.getBytes();
        byte[] output;
        d.reset();
        output = d.digest(input);
        byte[] hex = new byte[output.length * 2];
        for (int j = 0; j < output.length; j++) {
            byte b = output[j];
            hex[j * 2] = toHex((b & 0xf0) >> 4);
            hex[j * 2 + 1] = toHex(b & 0x0f);
        }
        String encoded = null;
        try {
            encoded = new String(hex, "ascii");
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
        return encoded;
    }

    @Override
    public String getCommand() {
        return "REGISTER";
    }

    @Override
    public String[] getParameters() {
        return new String[] { user, digest };
    }

    public static void understand() {
        RequestFactory fac = new RequestFactory() {

            public Request getRequest(String tid, String[] params) {
                return new RegisterRequest(tid, params);
            }
        };
        factories.put("REGISTER", fac);
    }

    public String getDigest() {
        return digest;
    }

    public String getUser() {
        return user;
    }
}
