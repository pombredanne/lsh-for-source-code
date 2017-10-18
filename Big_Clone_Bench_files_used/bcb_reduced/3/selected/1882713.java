package fitnesse.wikitext.widgets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import fitnesse.wikitext.WikiWidget;

public class RandomVariableWidget extends WikiWidget {

    public static final String REGEXP = "!random";

    private static String RandomValue = "";

    public RandomVariableWidget(ParentWidget parent, String text) {
        super(parent);
        RandomValue = randomValue();
    }

    public String render() throws Exception {
        return RandomValue;
    }

    public String randomValue() {
        try {
            Random generator = new Random();
            String randomNum = new Integer(generator.nextInt(100000)).toString();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] result = sha.digest(randomNum.getBytes());
            return hexEncode(result).toString().substring(0, 11);
        } catch (NoSuchAlgorithmException ex) {
            return "notrandom";
        }
    }

    private String hexEncode(byte[] aInput) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }
}
