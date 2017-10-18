package co.edu.unal.ungrid.services.server.ldap;

import java.security.MessageDigest;
import javax.swing.JFileChooser;
import sun.misc.BASE64Decoder;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapUtil {

    public static ResultCode checkExistence(final String sUsrAcc) {
        ResultCode rc = ResultCode.UNAVAILABLE;
        if (sUsrAcc != null) {
            try {
                if (search(sUsrAcc, MAIL_ATTR) != null) {
                    rc = ResultCode.SUCCESS;
                }
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }
        return rc;
    }

    private static String search(final String sUsrAcc, final String sAttribute) throws LDAPException {
        String sAttrValue = null;
        if (sUsrAcc != null) {
            LDAPConnection conn = new LDAPConnection(HOST, PORT, BIND_DN, BIND_PW);
            SearchResult sr = conn.search(BASE_DN, SearchScope.SUB, "(uid=" + sUsrAcc + ")", sAttribute);
            if (sr.getEntryCount() > 0) {
                SearchResultEntry entry = sr.getSearchEntries().get(0);
                if (entry != null) {
                    sAttrValue = entry.getAttributeValue(sAttribute);
                }
            }
        } else {
            throw new LDAPException(ResultCode.valueOf(ResultCode.ASSERTION_FAILED_INT_VALUE), "Internal: Null user account!");
        }
        return sAttrValue;
    }

    private static boolean validateSshaPwd(String sSshaPwd, String sUserPwd) {
        boolean b = false;
        if (sSshaPwd != null && sUserPwd != null) {
            if (sSshaPwd.startsWith(SSHA_PREFIX)) {
                sSshaPwd = sSshaPwd.substring(SSHA_PREFIX.length());
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    BASE64Decoder decoder = new BASE64Decoder();
                    byte[] ba = decoder.decodeBuffer(sSshaPwd);
                    byte[] hash = new byte[FIXED_HASH_SIZE];
                    byte[] salt = new byte[FIXED_SALT_SIZE];
                    System.arraycopy(ba, 0, hash, 0, FIXED_HASH_SIZE);
                    System.arraycopy(ba, FIXED_HASH_SIZE, salt, 0, FIXED_SALT_SIZE);
                    md.update(sUserPwd.getBytes());
                    md.update(salt);
                    byte[] baPwdHash = md.digest();
                    b = MessageDigest.isEqual(hash, baPwdHash);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
        return b;
    }

    public static ResultCode validatePwd(String sUsrAcc, String sUsrPwd) {
        ResultCode rc = ResultCode.UNAVAILABLE;
        if (sUsrAcc != null && sUsrPwd != null) {
            try {
                String sLdapPwd = search(sUsrAcc, PWD_ATTR);
                if (sLdapPwd != null) {
                    rc = (validateSshaPwd(sLdapPwd, sUsrPwd) ? ResultCode.SUCCESS : ResultCode.INVALID_CREDENTIALS);
                }
            } catch (LDAPException exc) {
                rc = ResultCode.valueOf(ResultCode.CONNECT_ERROR_INT_VALUE);
            }
        }
        return rc;
    }

    public static void testExistence(String[] args) {
        if (args.length > 0) {
            System.out.println(args[0] + "=" + (checkExistence(args[0]) == ResultCode.SUCCESS));
        }
    }

    public static void testQuery(String[] args) {
        if (args.length >= 2) {
            try {
                String sAttrValue = search(args[0], args[1]);
                System.out.println(args[1] + "=" + sAttrValue);
            } catch (LDAPException exc) {
                exc.printStackTrace();
            }
        }
    }

    public static void testPasswd(String[] args) {
        boolean b = false;
        if (args.length >= 2) {
            try {
                String sLdapPwd = search(args[0], PWD_ATTR);
                if (sLdapPwd != null) {
                    b = validateSshaPwd(sLdapPwd, args[1]);
                }
            } catch (LDAPException exc) {
                exc.printStackTrace();
            }
        } else {
            GetPasswordDlg dlg = new GetPasswordDlg();
            dlg.showDialog();
            if (dlg.getReturn() == JFileChooser.APPROVE_OPTION) {
                try {
                    String sLdapPwd = search(dlg.getAccount(), PWD_ATTR);
                    if (sLdapPwd != null) {
                        b = validateSshaPwd(sLdapPwd, dlg.getPassword());
                    }
                } catch (LDAPException exc) {
                    exc.printStackTrace();
                }
            }
        }
        System.out.println("valid=" + b);
    }

    public static void main(String[] args) {
        testExistence(args);
    }

    private static final String HOST = "undirectorio.unal.edu.co";

    private static final int PORT = 389;

    private static final String BIND_DN = "uid=user_ldap,ou=institucional,o=bogota,o=unal.edu.co";

    private static final String BIND_PW = "consultaldap";

    private static final String BASE_DN = "ou=people,o=bogota,o=unal.edu.co";

    private static final String PWD_ATTR = "userPassword";

    private static final String MAIL_ATTR = "mail";

    private static final int FIXED_HASH_SIZE = 20;

    private static final int FIXED_SALT_SIZE = 8;

    private static final String SSHA_PREFIX = "{SSHA}";
}
