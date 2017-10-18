package au.edu.archer.metadata.mde.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import javax.servlet.ServletContext;

public final class BuildInfo {

    private static String buildJSPath = "client/Build.js";

    private ServletContext context;

    public BuildInfo(ServletContext context) {
        super();
        this.context = context;
    }

    /**
     * Returns a unique identifier for each product build.
     * @return Unique product build identifier.
     */
    public final String getBuildId() {
        byte[] bytes = new byte[2048];
        InputStream is = null;
        MessageDigest md5 = null;
        int nRead = 0;
        try {
            md5 = MessageDigest.getInstance("MD5");
            if ((is = context.getResourceAsStream(buildJSPath)) != null) {
                while ((nRead = is.read(bytes)) > 0) {
                    md5.update(bytes, 0, nRead);
                }
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        if (md5 == null) {
            return null;
        }
        return new BigInteger(1, md5.digest()).toString(16);
    }
}
