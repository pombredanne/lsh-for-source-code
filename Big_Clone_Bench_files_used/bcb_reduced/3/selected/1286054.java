package com.liferay.portal.kernel.util;

/**
 * <a href="DigesterUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class DigesterUtil {

    public static String digest(String text) {
        return getDigester().digest(text);
    }

    public static String digest(String algorithm, String text) {
        return getDigester().digest(algorithm, text);
    }

    public static Digester getDigester() {
        return _digester;
    }

    public void setDigester(Digester digester) {
        _digester = digester;
    }

    private static Digester _digester;
}
