package server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 * 
 * 
 * @author renat
 *         <p>
 *         Copyright: Copyright (c) 2008
 *         </p>
 *         <p>
 *         ��������: NIC
 *         </p>
 */
public class GlobalConfig {

    private static int basePort = 5555;

    public static final int UDP_DOCUMENT_RETR_PORT = basePort++;

    public static final int UDP_TEMPLATE_RETR_PORT = basePort++;

    public static final int UDP_LOGON_PORT = basePort++;

    public static final int UDP_LIST_PORT = basePort++;

    public static final int UDP_REPORTER_PORT = basePort++;

    public static final int TCP_DOCUMENT_RETR_PORT = basePort++;

    public static final int TCP_TEMPLATE_RETR_PORT = basePort++;

    public static final int TCP_LOGON_PORT = basePort++;

    public static final int TCP_LIST_PORT = basePort++;

    public static final int TCP_REPORTER_PORT = basePort++;

    /**
	 * 
	 */
    public static final String TEMPLATE_RETRIEVER = "TemplateRetriever";

    /**
	 * 
	 */
    public static final String DOCUMENT_RETRIEVER = "DocumentRetriever";

    /**
	 * 
	 */
    public static final String TEMPLATE = "Template";

    /**
	 * 
	 */
    public static final String DOCUMENT = "Document";

    private static final MessageDigest MD;

    static {
        try {
            MD = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String shaHash(Object value) {
        return new BASE64Encoder().encode(MD.digest(value.toString().getBytes()));
    }
}
