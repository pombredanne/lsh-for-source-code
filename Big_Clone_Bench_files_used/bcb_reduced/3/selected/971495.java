package lc.ui.request;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import lc.init.TLCConstants;
import lc.init.config.Cfg;
import lc.ui.util.FormatUtil;
import lc.ui.util.LoanPurpose;
import lc.ui.util.TLCUtil;

/**
 * Frankly I don't know how useful this class will be, but let's see
 * @author Tom
 *
 */
public final class RequestUtils {

    private static final String dobpattern = "^\\d{1,2}/\\d{1,2}/\\d{4}$";

    private static final String actorIdPatten = "^[0-9]*[1-9][0-9]*$";

    private static final String zippattern = "^\\d{5}$";

    private static final String phonepattern1 = "\\(\\d{3}\\)\\W?\\d{3}\\W?\\d{4}$";

    private static final String phonepattern2 = "^\\d{3}\\W?\\d{3}\\W?\\d{4}$";

    private static final String ssnpattern = "^\\d{3}-?\\d{2}-?\\d{4}$";

    private static final String ssnshortpattern = "^\\d{4}$";

    private static final String statepattern = "[A-Z]{2}";

    private static final String routingnumberpattern = "^[0-9]{9}$";

    private static final String passwordpattern = "(?=.*\\d)(?=.*[A-Z,a-z]).{8,}";

    private static final String bankaccountnumberpattern = "\\d+";

    private static final String loanidnumberpattern = "^[0-9]{20}$";

    private static final String feesApplyPatten = "^[+|\\-]?\\d+(.\\d+)?$";

    public static final String dollaramountpattern = "^\\$?(\\d{1,3}(,\\d{3})*|(\\d*))(\\.\\d{1,2})?$";

    private static final int[] ROUTING_MASK = { 3, 7, 1, 3, 7, 1, 3, 7 };

    private static final String cityPattern = "([a-zA-Z]+|[a-zA-Z]+\\s[a-zA-Z]+)";

    public static final String accountPINpattern = "\\.?\\d{2}";

    public static final String emailpattern = "\\S+@\\S+\\.\\S+";

    public static final String screennamepattern = "[-\\w_.]+";

    public static final String numericpattern = "[0-9]+";

    public static final String portfolionamepattern = "[-\\w _.#]+";

    public static final String urlpattern = "^http(s{0,1})://[a-zA-Z0-9_/\\-\\.]+\\.([A-Za-z/]{2,5})[a-zA-Z0-9_/\\&\\?\\=\\-\\.\\~\\%]*";

    private static final String posnumberpattern = "^\\d{1,2}$";

    public static final Pattern[] dobRE = new Pattern[] { Pattern.compile(dobpattern) };

    public static final Pattern[] zipRE = new Pattern[] { Pattern.compile(zippattern) };

    public static final Pattern[] phoneRE = new Pattern[] { Pattern.compile(phonepattern1), Pattern.compile(phonepattern2) };

    public static final Pattern[] ssnRE = new Pattern[] { Pattern.compile(ssnpattern) };

    public static final Pattern[] ssnShortRE = new Pattern[] { Pattern.compile(ssnshortpattern) };

    public static final Pattern[] stateRE = new Pattern[] { Pattern.compile(statepattern) };

    public static final Pattern[] cityRE = new Pattern[] { Pattern.compile(cityPattern) };

    public static final Pattern[] routingNumberRE = new Pattern[] { Pattern.compile(routingnumberpattern) };

    public static final Pattern[] passwordPatternRE = new Pattern[] { Pattern.compile(passwordpattern) };

    public static final Pattern[] bankAccountNumberRE = new Pattern[] { Pattern.compile(bankaccountnumberpattern) };

    public static final Pattern[] loanIdNumberRE = new Pattern[] { Pattern.compile(loanidnumberpattern) };

    public static final Pattern[] accountPINRE = new Pattern[] { Pattern.compile(accountPINpattern) };

    public static final Pattern[] feesNumberRE = new Pattern[] { Pattern.compile(feesApplyPatten) };

    public static final String ratecriteriapattern = "AA|A|B|C|D|E|F|G";

    public static final Pattern[] emailRE = new Pattern[] { Pattern.compile(emailpattern) };

    public static final Pattern[] screennameRE = new Pattern[] { Pattern.compile(screennamepattern) };

    public static final Pattern[] numericRE = new Pattern[] { Pattern.compile(numericpattern) };

    public static final Pattern[] portfolionameRE = new Pattern[] { Pattern.compile(portfolionamepattern) };

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final Pattern[] urlpatternRE = new Pattern[] { Pattern.compile(urlpattern) };

    public static final Pattern[] dollaramountRE = new Pattern[] { Pattern.compile(dollaramountpattern) };

    public static final Pattern[] posnumberRE = new Pattern[] { Pattern.compile(posnumberpattern) };

    public static final Pattern[] ratecriteriapatternRE = new Pattern[] { Pattern.compile(ratecriteriapattern) };

    public static final Pattern[] actorIdRe = new Pattern[] { Pattern.compile(actorIdPatten) };

    /**
	 * Return whether or not the string is empty, where empty is either null,
	 * an empty string, or a string containing only whitespace.
	 * @param s
	 */
    public static final boolean isEmpty(String s) {
        return s == null || "".equals(s.trim());
    }

    /**
	 * Checks whether the input string matches the given regular expression pattern
	 * @param input String to test
	 * @param pattern regular expression to match
	 * @return true if the String matches the regular expression
	 */
    public static boolean matches(String input, Pattern pattern) {
        return pattern.matcher(input).matches();
    }

    /**
	 * This method tests whether a value is empty or not; if it is, the
	 * specified error key is set on the current RequestErrorObject
	 * and 'true' is returned.
	 * 
	 * If either the value to be tested or the request is null, 'false' is returned.
	 * 
	 * @param value value to test
	 * @param req HttpServletRequest object for current request
	 * @param errkey error key to set if empty
	 * @return true if value is empty
	 */
    public static boolean testValueEmpty(String value, HttpServletRequest req, RequestErrorKey errkey) {
        if (value == null || req == null) return false;
        if (isEmpty(value)) {
            RequestErrorObject.setError(req, errkey);
            return true;
        }
        return false;
    }

    public static boolean testValueEmptyReturnBool(String value) {
        if (value == null) return false;
        if (isEmpty(value)) {
            return true;
        }
        return false;
    }

    /**
	 * This method tests whether a value is valid or not; if it is invalid, the
	 * specified error key is set on the current RequestErrorObject
	 * and 'true' is returned.
	 * 
	 * If the value, patterns, or request is null, 'false' is returned.
	 * 
	 * @param value value to test
	 * @param patterns regexp pattern object(s) which, if matches, indicates a valid value
	 * @param req HttpServletRequest object for current request
	 * @param errkey error key to set if empty
	 * @return true if value is invalid or null
	 */
    public static boolean testValueInvalidFormat(String value, Pattern[] patterns, HttpServletRequest req, RequestErrorKey errkey) {
        if (value == null || patterns == null || req == null) return false;
        for (Pattern p : patterns) {
            if (matches(value, p)) {
                return false;
            }
        }
        RequestErrorObject.setError(req, errkey);
        return true;
    }

    /**
	 * This method tests whether a routing number is valid or not; if it is invalid, the
	 * specified error key is set on the current RequestErrorObject
	 * and 'true' is returned.
	 * 
	 * If the value, or request is null, 'true' is returned.
	 * 
	 * @param value value to test
	 * @param req HttpServletRequest object for current request
	 * @param errkey error key to set if empty
	 * @return true if value is invalid or null
	 */
    public static boolean isRoutingNumInvalid(String value, HttpServletRequest req, RequestErrorKey errkey) {
        if (value == null || req == null) return true;
        if (value.length() != 9) {
            RequestErrorObject.setError(req, errkey);
            return true;
        }
        int chksm = 0;
        for (int i = 0; i < 8; i++) chksm += (value.charAt(i) - '0') * ROUTING_MASK[i];
        chksm = (10 - (chksm % 10)) % 10;
        if (chksm != value.charAt(8) - '0') {
            RequestErrorObject.setError(req, errkey);
            return true;
        }
        return false;
    }

    /**
     * Common utility to handle request parameters. Extraneous whitespace surrounding any values
     * will be removed.
     * 
     * Note: If the parameter was not specified, empty string is returned. This is indistinguishable from
     * the situation where the parameter was specified but empty.
     * @param httpServletRequest
     * @param param
     * @return a non-null value
     */
    public static String getRequestParameter(HttpServletRequest httpServletRequest, String param) {
        String s = httpServletRequest.getParameter(param);
        if (s == null) s = ""; else s = s.trim();
        return s;
    }

    /**
     * Common utility to handle request parameters. Extraneous whitespace surrounding any values
     * will be removed.
     * 
     * Note: If the parameter was not specified, empty string is returned. This is indistinguishable from
     * the situation where the parameter was specified but empty.
     * @param req
     * @param param
     * @return
     */
    public static String getRequestParameter(HttpServletRequest req, RequestParam param) {
        return getRequestParameter(req, param.toString());
    }

    /**
     * Common utility to handle request parameters. A String[] will be returned of all equal parameters;
     * if the parameter was unspecified, String[0] will be returned.
     * 
     * Note: If the parameter was not specified, empty string is returned. This is indistinguishable from
     * the situation where the parameter was specified but empty.
     * @param httpServletRequest
     * @param param parameter to retrieve
     * @return value of the parameter, or "" if empty/null
     */
    public static String[] getRequestParameterValues(HttpServletRequest httpServletRequest, String param) {
        String[] s = httpServletRequest.getParameterValues(param);
        if (s == null) s = EMPTY_STRING_ARRAY;
        return s;
    }

    /**
     * Common utility to handle request parameters. A String[] will be returned of all equal parameters;
     * if the parameter was unspecified, String[0] will be returned.
     * 
     * Note: If the parameter was not specified, empty string is returned. This is indistinguishable from
     * the situation where the parameter was specified but empty.
     * @param httpServletRequest
     * @param param parameter to retrieve
     * @return value of the parameter, or "" if empty/null
     */
    public static String[] getRequestParameterValues(HttpServletRequest httpServletRequest, RequestParam param) {
        String[] s = httpServletRequest.getParameterValues(param.toString());
        if (s == null) s = EMPTY_STRING_ARRAY;
        return s;
    }

    /**
     * Tests if a given parameter is equivalent to a selected checkbox.
     * @param value
     * @param req
     * @param errkey
     * @return false if the value equates to a checked checkbox
     */
    public static boolean testInvalidCheckboxValue(String value, HttpServletRequest req, RequestErrorKey errkey) {
        if ("on".equals(value)) {
            return false;
        }
        RequestErrorObject.setError(req, errkey);
        return true;
    }

    /**
     * Tests if a given parameter is equivalent to a selected checkbox.
     * @param param the name of the parameter to retrieve from the request URL
     * @param req
     * @param errkey
     * @return true if the value equates to a checked checkbox
     */
    public static boolean testInvalidCheckboxValue(RequestParam param, HttpServletRequest req, RequestErrorKey errkey) {
        return testInvalidCheckboxValue(RequestUtils.getRequestParameter(req, param), req, errkey);
    }

    /**
     * Converts a value to an int. If the value is not an int, an error is reported
     * via RequestErrorKey and NumberFormatException is thrown.
     * @param value value to convert
     * @param _request
     * @param err
     * @return int value
     * @throws NumberFormatException if the value is not an int
     */
    public static int getIntValue(String value, HttpServletRequest _request, RequestErrorKey err) throws NumberFormatException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            RequestErrorObject.setError(_request, err);
            throw e;
        }
    }

    /**
     * Converts a value to a double. If the value is not a double, an error is reported
     * via RequestErrorKey and NumberFormatException is thrown.
     * @param value value to convert
     * @param _request
     * @param err RequestErrorKey to set; if null, no error is set
     * @return double value
     * @throws NumberFormatException if the value is not a double
     */
    public static double getDoubleValue(String value, HttpServletRequest _request, RequestErrorKey err) throws NumberFormatException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            RequestErrorObject.setError(_request, err);
            throw e;
        }
    }

    /**
	* Validate the form of an email address.
	* 
	* <br/><br/>
	* Return true only if
	* <ul>
	* <li> aEmailAddress can successfully construct an
	* {@link javax.mail.internet.InternetAddres s}
	* <li> when parsed with "@" as delimiter, aEmailAddress contains
	* two tokens which satisfy
	* {@link hirondelle.web4j.util.Util#textHas Content}.
	* </ul>
	* 
	* <br/><br/>
	* The second condition arises since local email addresses, simply of the
	* form "albert", for example, are valid for
	* {@link javax.mail.internet.InternetAddres s}, but almost always
	* undesired.
	*/
    public static boolean isValidEmailAddress(String aEmailAddress) {
        if (aEmailAddress == null) return false;
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(aEmailAddress, true);
            if (!hasNameAndDomain(aEmailAddress)) {
                result = false;
            }
            if (emailAddr.toString().indexOf("?") != -1) {
                result = false;
            }
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    private static boolean hasNameAndDomain(String aEmailAddress) {
        String[] tokens = aEmailAddress.split("@");
        return (tokens.length == 2 && tokens[0].length() > 0 && tokens[1].length() > 0);
    }

    public static boolean testValueInvalidFormatReturnBool(String value, Pattern[] patterns) {
        if (value == null || patterns == null) return false;
        for (Pattern p : patterns) {
            if (matches(value, p)) {
                return false;
            }
        }
        return true;
    }

    public static boolean testInvalidCheckboxValueReturnBool(String value) {
        if ("on".equals(value)) {
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a value is valid or not; if it is invalid, 
	 * 'true' is returned.
	 * 
	 * If the value, patterns is null, 'false' is returned.
	 * 
	 * @param value value to test
	 * @param patterns regexp pattern object(s) which, if matches, indicates a valid value
	 * @param req HttpServletRequest object for current request
	 * @param errkey error key to set if empty
	 * @return true if value is invalid or null
	 */
    public static boolean testValueInvalidFormat(String value, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (matches(value, p)) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Common utility to handle replacing all groups of whitespace into a single whitespace.
	 * @param s
	 */
    public static final String reduceBlank(String s) {
        return s.replaceAll("( )+", " ");
    }

    /**
	 * This method tests whether a password is valid or not; if it is invalid, the
	 * corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param req HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validatePassword(String value, HttpServletRequest req) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(req, RequestErrorKey.PASSWORD_EMPTY);
            return false;
        }
        if (RequestUtils.testValueInvalidFormat(value, RequestUtils.passwordPatternRE, req, RequestErrorKey.PASSWORD_NOT_VALID)) {
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a social security number is valid or not; if it is invalid, the
	 * corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param req HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateSsn(String value, HttpServletRequest req) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(req, RequestErrorKey.MISSING_SSN);
            return false;
        }
        if (RequestUtils.testValueInvalidFormat(value, RequestUtils.ssnRE, req, RequestErrorKey.INVALID_SSN)) {
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a short ssn(last 4 digits) is valid or not; 
	 * if it is valid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty or invalid format, it is invalid.
	 * 
	 * @author emi@scaleoptions.com
	 * @param  value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateShortSsn(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_SSN);
            return false;
        }
        if (RequestUtils.testValueInvalidFormat(value, RequestUtils.ssnShortRE, request, RequestErrorKey.INVALID_SSN_SHORT)) {
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a bank name is valid or not; if it is invalid, the
	 * corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param req HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateBankName(String value, HttpServletRequest req) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(req, RequestErrorKey.MISSING_PRIMARY_ACCT_INSTITUTION);
            return false;
        }
        if (value.length() > 75) {
            RequestErrorObject.setError(req, RequestErrorKey.PRIMARY_ACCT_INSTITUTION_LENGTH_ERROR);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a bank account holder's first name is valid or not; 
	 * if it is invalid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param req HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateBankAccountHolderFirstName(String value, HttpServletRequest req) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(req, RequestErrorKey.MISSING_PRIMARY_ACCT_HOLDER_FIRSTNAME);
            return false;
        } else if (value.length() > 35) {
            RequestErrorObject.setError(req, RequestErrorKey.PRIMARY_ACCT_HOLDER_LENGTH_ERROR_FIRSTNAME);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a bank account holder's last name is valid or not; 
	 * if it is invalid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateBankAccountHolderLastName(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_PRIMARY_ACCT_HOLDER_LASTNAME);
            return false;
        } else if (value.length() > 35) {
            RequestErrorObject.setError(request, RequestErrorKey.PRIMARY_ACCT_HOLDER_LENGTH_ERROR_LASTNAME);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a routing number is valid or not; 
	 * if it is invalid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateRoutingNumber(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_ROUTING_NUMBER);
            return false;
        } else if (RequestUtils.isRoutingNumInvalid(value, request, RequestErrorKey.INVALID_ROUTING_NUMBER)) {
            return false;
        } else if (!validateABAChecksum(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.INVALID_ROUTING_NUMBER_CHECKSUM);
            return false;
        }
        return true;
    }

    /**
	 * Calculates and verifies if a number matches the ABA checksum formula.
	 * Assumption is that the number is 9 digits and has already been pre-checked for this.
	 * 
	 * Formula is implemented as detailed at http://www.brainjar.com/js/validation/
	 * @param number number to test
	 * @return true if checksum calculation succeeds; false otherwise
	 */
    public static boolean validateABAChecksum(String number) {
        int checksum = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = 0;
            try {
                digit = Integer.parseInt(number.substring(i, i + 1));
            } catch (NumberFormatException e) {
                return false;
            }
            switch(i % 3) {
                case 0:
                    checksum += digit * 3;
                    break;
                case 1:
                    checksum += digit * 7;
                    break;
                case 2:
                    checksum += digit * 1;
                    break;
                default:
                    return false;
            }
        }
        if (checksum != 0 && checksum % 10 == 0) return true;
        return false;
    }

    /**
	 * This method tests whether a bank account number is valid or not; 
	 * if it is invalid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateBankAccountNumber(String value, HttpServletRequest request) {
        if (value == null) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_ACCOUNT_NUMBER);
            return false;
        }
        String temp = value.replace("-", "");
        if (RequestUtils.isEmpty(temp)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_ACCOUNT_NUMBER);
            return false;
        } else if (RequestUtils.testValueInvalidFormat(temp, RequestUtils.bankAccountNumberRE, request, RequestErrorKey.INVALID_BANK_ACCOUNT_NUMBER)) {
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a loan title is valid or not; 
	 * if it is invalid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author mike@scaleoptions.com
	 * @param value value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateLoanTitle(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_LOAN_TITLE);
            return false;
        } else if (value.length() < 2) {
            RequestErrorObject.setError(request, RequestErrorKey.LOAN_TITLE_TOO_SHORT);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a loan amount number is valid or not; 
	 * if it is invalid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty or invalid format, it is invalid.
	 * 
	 * @author emi@scaleoptions.com
	 * @param  value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty or invalid format, otherwise true.
	 */
    public static boolean validateLoanAmount(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_LOAN_AMOUNT);
            return false;
        }
        if (!RequestUtils.isEmpty(value)) {
            try {
                value = value.trim();
                if (RequestUtils.testValueInvalidFormat(value, RequestUtils.dollaramountRE, request, RequestErrorKey.INVALID_LOAN_AMOUNT)) return false;
                double amount = Double.parseDouble(FormatUtil.cleanAmount(value));
                if (amount < TLCConstants.MINIMUM_AMOUNT_TO_BORROW) {
                    RequestErrorObject.setError(request, RequestErrorKey.LOAN_AMOUNT_MINIMUM);
                    return false;
                }
                if (amount > Cfg.getLoanProcessingMaximum()) {
                    RequestErrorObject.setError(request, RequestErrorKey.LOAN_AMOUNT_MAXIMUM);
                    return false;
                }
                if (amount % TLCConstants.ROUNDING_UNIT != 0) {
                    amount = TLCUtil.floor25(amount);
                }
            } catch (Exception e) {
                RequestErrorObject.setError(request, RequestErrorKey.INVALID_LOAN_AMOUNT);
                return false;
            }
        }
        return true;
    }

    /**
	 * This method tests whether a loan purpose is valid or not; 
	 * if it is valid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty, it is invalid.
	 * 
	 * @author emi@scaleoptions.com
	 * @param  value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateLoanPurpose(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value) || LoanPurpose.getPurpose(value) == null) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_LOAN_PURPOSE);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a email is valid or not; 
	 * if it is valid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty or invalid format, it is invalid.
	 * 
	 * @author emi@scaleoptions.com
	 * @param  value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateEmail(String value, HttpServletRequest request) {
        if (RequestUtils.testValueEmpty(value, request, RequestErrorKey.EMAIL_EMPTY)) {
            return false;
        }
        if (RequestUtils.testValueInvalidFormat(value, RequestUtils.emailRE, request, RequestErrorKey.INVALID_EMAIL)) {
            return false;
        } else if (!RequestUtils.isValidEmailAddress(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.INVALID_EMAIL);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a yearly income is valid or not; 
	 * if it is valid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is null or empty or invalid format, it is invalid.
	 * 
	 * @author emi@scaleoptions.com
	 * @param  value to test
	 * @param request HttpServletRequest object for current request
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateYealyIncome(String value, HttpServletRequest request) {
        if (RequestUtils.isEmpty(value)) {
            RequestErrorObject.setError(request, RequestErrorKey.MISSING_TOTAL_INCOME);
            return false;
        }
        value = value.trim();
        if (RequestUtils.testValueInvalidFormat(value, RequestUtils.dollaramountRE, request, RequestErrorKey.INVALID_TOTAL_INCOME)) return false; else if (Double.parseDouble(FormatUtil.cleanAmount(value)) < 0) {
            RequestErrorObject.setError(request, RequestErrorKey.INCOME_NEGATIVE);
            return false;
        } else if (Double.parseDouble(FormatUtil.cleanAmount(value)) > 9999999d) {
            RequestErrorObject.setError(request, RequestErrorKey.MAX_TOTAL_INCOME);
            return false;
        }
        return true;
    }

    /**
	 * This method tests whether a agreement is accepted or not. 
	 * if it is valid, the corresponding error key is set on the current RequestErrorObject
	 * and 'false' is returned.
	 * 
	 * If the value is not "on", it is invalid.
	 * 
	 * @author emi@scaleoptions.com
	 * @param  value to test
	 * @param request HttpServletRequest object for current request
	 * @param requestErrorKey object for error message.
	 * @return false if value is invalid, null or empty, otherwise true.
	 */
    public static boolean validateAgreement(String value, HttpServletRequest request, RequestErrorKey key) {
        if (!("on".equals(value))) {
            RequestErrorObject.setError(request, key);
            return false;
        }
        return true;
    }

    public static void printHeaders(HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();
        sb.append("### Request Headers ###\n");
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            sb.append("Name: " + name + "; Value: " + request.getHeader(name));
            sb.append("\n");
        }
        sb.append("#######################\n");
    }

    public static void printMd5(HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        sb.append(request.getRemoteAddr()).append(";");
        sb.append(request.getHeader("USER-AGENT"));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] dg = md.digest(sb.toString().getBytes("utf-8"));
            final StringBuilder sb2 = new StringBuilder();
            sb2.append("\n#######################################");
            sb2.append("string: ").append(sb).append("\n");
            sb2.append("md5: ").append(new String(dg, "utf-8"));
            sb2.append("\n#######################################");
            System.out.println(sb2);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("127.0.0.1;Mozilla/5.0 (Windows NT 6.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.77 Safari/534.24 ChromePlus/1.6.2.0");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("127.0.0.1;Mozilla/5.0 (Windows NT 6.1) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.77 Safari/534.24 ChromePlus/1.6.2.0");
        final long st = System.currentTimeMillis();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] dg = md.digest(sb.toString().getBytes("utf-8"));
            byte[] dg2 = md.digest(sb2.toString().getBytes("utf-8"));
            final StringBuilder pm = new StringBuilder();
            pm.append("\n#######################################\n");
            pm.append("string: ").append(sb).append("\n");
            pm.append("string: ").append(sb2).append("\n");
            pm.append("md5: ").append(Arrays.toString(dg)).append("\n");
            pm.append("md5: ").append(Arrays.toString(dg2)).append("\n");
            pm.append("#######################################");
            final long et = System.currentTimeMillis();
            System.out.println(pm);
            System.out.println("Time: " + (et - st) / 1000.0);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
