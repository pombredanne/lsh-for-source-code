package com.fastaop.advice.matcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class to perform pointcut matching.
 * 
 * @author wiesda00
 * 
 */
public final class MatcherHelper {

    private static MessageDigest algorithm = null;

    private static final String DOT_DOT = "(@*\\@)";

    private static final String STAR = "(@*)";

    private MatcherHelper() {
    }

    /**
	 * Creates a or regex expression.
	 * @param currentExpression the current expression or null
	 * @param expression the new expression
	 * @return the new expression
	 */
    public static String addOrExpression(String currentExpression, String expression) {
        String back = null;
        if (currentExpression != null) {
            back = currentExpression + "|" + makeRegexExpressionFromClassPC(expression);
        } else {
            back = makeRegexExpressionFromClassPC(expression);
        }
        return back;
    }

    public static String makeRegexExpressionFromClassPC(String classPC) {
        if (classPC.endsWith("..")) {
            classPC = classPC + "*";
        }
        classPC = classPC.replace("*", STAR);
        classPC = classPC.replace("..", DOT_DOT);
        classPC = classPC.replace(".", "\\.");
        classPC = classPC.replace('@', '.');
        return classPC;
    }

    public static String makeRegexExpressionFromMethodPC(String classPC) {
        classPC = classPC.replace("*", STAR);
        classPC = classPC.replace('@', '.');
        return classPC;
    }

    public static synchronized String generateMD5(String input) {
        byte[] defaultBytes = input.getBytes();
        try {
            if (algorithm == null) {
                algorithm = MessageDigest.getInstance("MD5");
            }
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }
}
