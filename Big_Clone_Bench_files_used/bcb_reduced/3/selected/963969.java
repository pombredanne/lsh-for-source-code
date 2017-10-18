package com.angel.common.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.angel.architecture.exceptions.NonBusinessException;

/** The <code>StringHelper.class</code> helps you to manage some string functions.
 *  All its methods are statics.
 *
 * @author William
 * @version
 */
public class StringHelper {

    public static final String CHAR_ENCODING_ISO_8859_1 = "ISO-8859-1";

    public static final String ALGORITHM = "SHA-256";

    private static final char[] DECIMAL_HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /** It is the logger for the class. */
    private static Logger logger = Logger.getLogger(StringHelper.class);

    /** Define a Constant for a empty string. */
    public static final String EMPTY_STRING = "";

    /** Define a Constant for http prefix. */
    public static final String PREFIX_HTTP = "http://";

    /** Define a Constant for invalid web characters. */
    public static final String INVALID_WEB_CHARACTERS = "!\\\"^�$%&/��`��";

    /** Define a Constant for web numbers characters. */
    public static final String NUMBER_WEB_CHARACTERS = "0123456789";

    /** Define a Constant for string web characters. */
    public static final String STRING_WEB_CHARACTERS = "abcdefghijklmn�opqrstuvwxyzABCDEFGHIJKLMN�OPQRSTUVWXYZ";

    private StringHelper() {
        super();
    }

    /** Set current log level.
	 *
	 * @param logLevel
	 */
    public static void setLogLevel(Level logLevel) {
        logger.setLevel(logLevel);
    }

    /** Replace in the string all the charfind by charResult, and then it is returned. The characters are
	 *  replaced in the string object like parameter.
	 *
	 * @param string
	 * @param charFind
	 * @param charResult
	 * @return
	 */
    public static String replace(String string, Character charFind, Character charResult) {
        if (!isEmpty(string)) {
            if (string.contains(String.valueOf(charFind))) {
                string = string.replace(charFind, charResult);
            }
        }
        return string;
    }

    /** The functions is the same like replace.
	 *
	 * @param string
	 * @param charFind
	 * @param charResult
	 * @return
	 */
    public static String replaceInNew(String string, Character charFind, Character charResult) {
        String newString = new String(string);
        return replace(newString, charFind, charResult);
    }

    /** Replace in the string as parameter each character in the Array in "charFind" at position 'i', by the
	 *  character in the Array "charResult" at the same position 'i'.
	 *  It supposes that the two have the same length. If they haven't the same length, the process finalize when
	 *  the shortest array was processed.
	 *
	 * @param string
	 * @param charFind
	 * @param charResult
	 * @return It returns a string with the characters replaced.
	 */
    public static String replaceCharacters(String string, Character[] charFind, Character[] charResult) {
        int charFindLength = charFind.length;
        int charResultLength = charResult.length;
        if (!isEmpty(string)) {
            for (int i = 0; i < charFindLength && i < charResultLength; i++) {
                string = string.replace(charFind[i], charResult[i]);
            }
        }
        return string;
    }

    public static String replaceCharactersInNew(String string, Character[] charFind, Character[] charResult) {
        String newString = new String(string);
        return replaceCharacters(newString, charFind, charResult);
    }

    /** Capitalize the string as parameter. It is an string with the first characters of the string or the first
	 *  character coninuos by a space blank in upper.
	 *
	 * @param string
	 * @return It returns a string capitalized.
	 */
    public static String capitalize(String string) {
        String label = (string != null ? string.toLowerCase() : "");
        String aux = new String();
        int size = label.length();
        for (int i = 0; i < size; i++) {
            if (label.substring(i, i + 1).equals(" ") && i + 2 <= size) {
                aux = aux.concat(" ");
                if (!label.substring(i + 1, i + 2).equals("")) {
                    aux = aux.concat(label.substring(i + 1, i + 2).toUpperCase());
                    i++;
                }
            } else {
                if (i == 0 && !label.substring(i, i + 1).equals("")) {
                    aux = aux.concat(label.substring(i, i + 1).toUpperCase());
                } else {
                    aux = aux.concat(label.substring(i, i + 1));
                }
            }
        }
        return aux;
    }

    /** It is the same process than capitalize method, but it returns a new string capitalized.
	 *
	 * @param string
	 * @return A new string capitalized.
	 */
    public static String capitalizeInNew(String string) {
        String newString = new String(string);
        return capitalize(newString);
    }

    public static List<String> capitalizeAll(List<String> listStrings) {
        List<String> newStrings = null;
        if (listStrings.size() > 0) {
            newStrings = new ArrayList<String>();
        } else {
            newStrings = listStrings;
        }
        for (int i = 0; i < listStrings.size(); i++) {
            String aString = new String();
            aString = capitalize(listStrings.get(i));
            newStrings.add(aString);
        }
        return newStrings;
    }

    public static String[] capitalizeAllToArray(List<String> listStrings) {
        List<String> newStrings = capitalizeAll(listStrings);
        return convertToArray(newStrings);
    }

    public static String[] capitalizeAllFromArray(String[] listStrings) {
        List<String> newStrings = capitalizeAll(convertToList(listStrings));
        return convertToArray(newStrings);
    }

    public static List<String> convertToList(String[] objects) {
        ArrayList<String> newObjects = new ArrayList<String>();
        for (int i = 0; i < objects.length; i++) {
            newObjects.add(objects[i]);
        }
        return newObjects;
    }

    public static Map<String, String> convertToMap(List<String> objects, String separator) {
        Map<String, String> newObjects = new HashMap<String, String>();
        for (String value : objects) {
            String[] splitted = value.split(separator);
            if (splitted != null && splitted.length > 1) {
                newObjects.put(splitted[0], splitted[1]);
            } else {
                if (splitted != null && splitted.length == 1) {
                    newObjects.put(splitted[0], EMPTY_STRING);
                }
            }
        }
        return newObjects;
    }

    private static String[] convertToArray(List<String> objects) {
        String[] newObjects = (String[]) objects.toArray(new String[objects.size()]);
        return newObjects;
    }

    public static String upperCharacterInPosition(String string, int position) {
        if (!isEmpty(string)) {
            if (string.length() < position) {
                return string;
            } else {
                String beginString = string.substring(0, position - 1);
                String charToUpper = string.substring(position - 1, position).toUpperCase();
                String endString = string.substring(position, string.length());
                return beginString + charToUpper + endString;
            }
        }
        return string;
    }

    public static String upperOnlyCharacter(String string, Character character) {
        if (!isEmpty(string)) {
            if (string.contains(String.valueOf(character))) {
                for (int i = 0; i < string.length(); i++) {
                    if (Character.valueOf(string.charAt(i)).equals(character)) {
                        string = upperCharacterInPosition(string, i + 1);
                    }
                }
            }
        }
        return string;
    }

    public static String alternCapitalize(String string) {
        if (!isEmpty(string)) {
            for (int i = 0; i < string.length(); i++) {
                if (NumberHelper.isPair(i)) {
                    string = upperCharacterInPosition(string, i + 1);
                }
            }
        }
        return string;
    }

    public static String invertOrder(String string) {
        String inverted = null;
        if (!isEmpty(string)) {
            inverted = new String();
            for (int i = 0; i < string.length(); i++) {
                inverted = inverted + string.substring(string.length() - i - 1, string.length() - i);
            }
        }
        return inverted;
    }

    public static ArrayList<String> getAllDiferentsPositions(String string) {
        ArrayList<String> strings = new ArrayList<String>();
        int size = string != null ? string.length() : 0;
        for (int i = size; i > 0; i--) {
            string = string.substring(size - 1, size) + string.substring(0, size - 1);
            strings.add(string);
        }
        return strings;
    }

    /** Split the string with the "splitter" string.
	 *
	 * @param string
	 * @param splitter
	 * @return It returns an ArrayList with all splited strings.
	 */
    public static List<String> split(String string, String splitter) {
        return convertToList(string.split(splitter));
    }

    /** Verify if the string contains at least one character in the characterList.
	 *
	 * @param string
	 * @param charactersList
	 * @return It returns true if the string contains at least one character in the characterList. Otherwise
	 * 		   it returns false.
	 */
    public static boolean containsAtLeast(String string, String charactersList) {
        boolean contains = false;
        CharSequence charSeq = charactersList.subSequence(0, charactersList.length());
        int size = charSeq.length();
        for (int i = 0; i < size; i++) {
            if (containsCharacter(string, charSeq.charAt(i))) {
                contains = true;
                i = size;
            }
        }
        return contains;
    }

    public static String trimComplete(String value) {
        String newValue = null;
        if (!isEmpty(value)) {
            newValue = value.trim();
        }
        return newValue;
    }

    public static boolean beginsAtLeast(String string, String charactersList) {
        boolean begins = false;
        if (!isEmpty(string)) {
            String firstChar = string.substring(0, 1);
            begins = containsAtLeast(firstChar, charactersList);
        }
        return begins;
    }

    public static boolean beginsWith(String string, String beginString) {
        boolean begins = false;
        if (!isEmpty(string)) {
            begins = string.startsWith(beginString);
        }
        return begins;
    }

    /** Verify if the string as parameter contains the character "character".
	 *
	 * @param string
	 * @param character
	 * @return Return true if the string contains the character "character". Otherwise it returns false.
	 */
    public static boolean containsCharacter(String string, Character character) {
        boolean contains = false;
        int size = string != null ? string.length() : 0;
        char currentChar;
        for (int i = 0; i < size; i++) {
            currentChar = string.charAt(i);
            if (Character.valueOf(currentChar).equals(character)) {
                contains = true;
                i = size;
            }
        }
        return contains;
    }

    /** Verify if the string length is higher than the max, and lower than the min.
	 *
	 * @param string
	 * @param min
	 * @param max
	 * @return If the string length is between the min and max, it returns true. Otherwise it return false.
	 */
    public static boolean isLengthBetween(String string, int min, int max) {
        return isLengthHigherOrEqualsThan(string, min) && isLengthLowerOrEqualsThan(string, max);
    }

    /** Verify if the string length is lower or equals than the min.
	 *
	 * @param string
	 * @param min
	 * @return It returns true if the string length is lower or equals than the min. Otherwise it returns false.
	 */
    public static boolean isLengthLowerOrEqualsThan(String string, int min) {
        return (string != null ? string.length() : 0) <= min;
    }

    /** Verify if the string length is higher or equals than the max.
	 *
	 * @param string
	 * @param max
	 * @return It returns true if the string length is higher or equals than the max. Otherwise it returns false.
	 */
    public static boolean isLengthHigherOrEqualsThan(String string, int max) {
        return (string != null ? string.length() : 0) >= max;
    }

    /**  Verify it the string is empty. It returns true if the string is null or if string is equals "".
	 *
	 * @param string
	 * @return It returns true if the string is null or equals to "". Otherwise it return false.
	 */
    public static boolean isEmpty(String string) {
        return string == null || string.equals(EMPTY_STRING) || (string != null && string.trim().equals(EMPTY_STRING));
    }

    public static boolean isNull(Object obj) {
        return obj == null || obj.equals(null);
    }

    /** Test a string if empty o not.
	 *
	 * @param value to test if it is not empty.
	 * @return true it value is not empty. Otherwise it returns false.
	 */
    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    /** Replace at the string the first character find by the character to replace.
	 *
	 * @param string
	 * @param find
	 * @param toReplace
	 * @return the string with its characters replaced.
	 */
    public static String replaceFirst(String string, Character find, Character toReplace) {
        String newValue = null;
        if (!isEmpty(find.toString()) && !isEmpty(toReplace.toString())) newValue = replaceFirst(string, find.toString(), toReplace.toString()); else newValue = string;
        return newValue;
    }

    /** Replace at the string the first string find by the string to replace.
	 *
	 * @param string
	 * @param find
	 * @param toReplace
	 * @return The string with its string replaced.
	 */
    public static String replaceFirst(String string, String find, String toReplace) {
        if (!isEmpty(string)) {
            int pos = string.indexOf(find);
            if (pos != -1) {
                String begin = string.substring(0, pos);
                String end = string.substring(pos + find.length(), string.length());
                return begin + toReplace + end;
            }
        }
        return string;
    }

    public static String replaceAll(String string, String find, String toReplace) {
        if (!isEmpty(string)) {
            int pos = string.indexOf(find);
            while (pos != -1) {
                string = replaceFirst(string, find, toReplace);
                pos = string.indexOf(find);
            }
        }
        return string;
    }

    public static String replaceAllRecursively(String string, String find, String replace) {
        if (!isEmpty(string)) {
            while (string.indexOf(find) > -1) {
                string = replaceAll(string, find, replace);
            }
        }
        return string;
    }

    public static String replaceAllFirst(String string, Character find, Character toReplace) {
        return replaceAllFirst(string, String.valueOf(find.charValue()), String.valueOf(toReplace.charValue()));
    }

    public static String replaceAllFirst(String string, String find, String toReplace) {
        boolean begins = StringHelper.beginsAtLeast(string, find);
        while (string.length() > 0 && begins) {
            string = StringHelper.replaceFirst(string, find, toReplace);
            begins = StringHelper.beginsAtLeast(string, find);
        }
        return string;
    }

    /** Test all values are not empty. If one of them is empty, it returns false.
	 *
	 * @param values to test if they are empty.
	 * @return true if they are not empty, otherwise it returns false.
	 * @See isNotEmpty at {@link StringHelper}
	 */
    public static boolean areAllNotEmpty(String... values) {
        boolean areNotEmpty = true;
        if (values != null && values.length > 0) {
            for (String value : values) {
                areNotEmpty = isNotEmpty(value) && areNotEmpty;
            }
        }
        return areNotEmpty;
    }

    /**
     * TODO Write comment.
     * @param s
     * @return
     */
    public static long longHashCode(String s) {
        long h = 0L;
        char val[] = s.toCharArray();
        for (int i = 0; i < val.length; i++) {
            h = 31L * h + (long) val[i];
        }
        return h;
    }

    public static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes());
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer(digest.length * 2);
            for (int i = 0; i < digest.length; ++i) {
                byte b = digest[i];
                int high = (b & 0xF0) >> 4;
                int low = b & 0xF;
                sb.append(DECIMAL_HEX[high]);
                sb.append(DECIMAL_HEX[low]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new NonBusinessException("Error hashing string", e);
        }
    }

    /**
     * TODO Write a comment.
     * @param _list
     * @return
     */
    public static String convertToPlainString(Object[] _list) {
        String strings = "";
        for (Object c : _list) {
            if (_list[_list.length - 1].equals(c)) {
                strings = strings.concat(c.toString());
            } else {
                strings = strings.concat(c.toString());
            }
        }
        return strings;
    }

    public static String convertToPlainString(Object[] _list, String separator) {
        String strings = "";
        int size = _list.length;
        int currentPosition = 0;
        for (Object c : _list) {
            if (currentPosition == size - 1) {
                strings += c.toString();
            } else {
                strings += c.toString().concat(separator);
            }
            currentPosition++;
        }
        return strings;
    }

    /**
	 * TODO Write a comment.
	 *
	 * @param _list
	 * @return
	 */
    public static String convertToPlainString(Collection<?> _list) {
        return convertToPlainString(_list.toArray());
    }

    public static String replaceAtLast(String value, String find, String toReplace) {
        if (StringHelper.isNotEmpty(value)) {
            boolean ends = value.endsWith(find);
            if (ends) {
                String beginValue = value.substring(0, value.lastIndexOf(find));
                String endValue = toReplace;
                value = beginValue + endValue;
            }
        }
        return value;
    }

    public static String replaceAllAtLast(String value, String find, String toReplace) {
        if (StringHelper.isNotEmpty(value)) {
            boolean ends = value.endsWith(find);
            while (value.length() > 0 && ends) {
                value = StringHelper.replaceAtLast(value, find, toReplace);
                ends = StringHelper.endsWith(value, find);
            }
        }
        return value;
    }

    private static boolean endsWith(String value, String find) {
        return value != null ? value.endsWith(find) : false;
    }
}
