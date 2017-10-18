package co.edu.unal.ungrid.services.server.db.util;

import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import co.edu.unal.ungrid.services.server.db.base.Record;
import co.edu.unal.ungrid.services.server.db.insert.AbstractInsert;
import co.edu.unal.ungrid.services.server.db.instance.DatabaseLoader;
import co.edu.unal.ungrid.services.server.db.select.AbstractQuery;
import co.edu.unal.ungrid.services.server.db.update.AbstractUpdate;
import co.edu.unal.ungrid.services.server.util.DateUtil;
import co.edu.unal.ungrid.services.server.util.StringUtil;

/**
 * @author Administrator
 *
 */
public class UserAccountDb {

    private UserAccountDb() {
    }

    public static String encode(final String sRawInput) {
        String sBase64Enc = null;
        if (sRawInput != null) {
            try {
                BASE64Encoder encoder = new BASE64Encoder();
                sBase64Enc = encoder.encodeBuffer(sRawInput.getBytes());
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        if (sBase64Enc.charAt(sBase64Enc.length() - 1) == '\n') {
            sBase64Enc = sBase64Enc.substring(0, sBase64Enc.length() - 1);
        }
        return sBase64Enc;
    }

    public static String decode(final String sEncoded) {
        String sRawOut = null;
        if (sEncoded != null) {
            try {
                BASE64Decoder decoder = new BASE64Decoder();
                sRawOut = new String(decoder.decodeBuffer(sEncoded));
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        return sRawOut;
    }

    private static class UserAccountSelect extends AbstractQuery {

        public UserAccountSelect() {
            setQuery(SELECT_ALL_FROM + TABLE);
        }

        private UserAccountSelect(final String sQuery) {
            setQuery(sQuery);
        }

        public static UserAccountSelect getSelectFromAccount(final String sAccount) {
            return new UserAccountSelect(SELECT_ALL_FROM + TABLE + WHERE + ACCOUNT + EQUALS + StringUtil.quote(sAccount));
        }

        public static UserAccountSelect getSelectFromEmail(final String sEmail) {
            return new UserAccountSelect(SELECT_ALL_FROM + TABLE + WHERE + EMAIL + EQUALS + StringUtil.quote(sEmail));
        }

        public UserAccountSelect(final String sAccount, final String sPassword) {
            setQuery(SELECT_ALL_FROM + TABLE + WHERE + ACCOUNT + EQUALS + StringUtil.quote(sAccount) + AND + PASSWORD + EQUALS + StringUtil.quote(encode(sPassword)));
        }

        @SuppressWarnings("unused")
        public UserAccountSelect(int status) {
            setQuery(SELECT_ALL_FROM + TABLE + WHERE + STATUS + EQUALS + status);
        }
    }

    private static class UserAccountInsert extends AbstractInsert {

        public UserAccountInsert(final String sAccount, final String sEncodedPwd, int nType, long nStatus, final String sEmail, final String sOrganization, final String sComments, final String sFullName, long nService) {
            String sNow = DateUtil.now();
            setInsert(INSERT + TABLE + VALUES + "(" + StringUtil.quote(sAccount) + COMMA + StringUtil.quote(sEncodedPwd) + COMMA + StringUtil.quote(sNow) + COMMA + StringUtil.quote(sNow) + COMMA + nType + COMMA + nStatus + COMMA + StringUtil.quote(sEmail) + COMMA + StringUtil.quote(sOrganization) + COMMA + StringUtil.quote(sComments) + COMMA + StringUtil.quote(sFullName) + COMMA + nService + ")");
        }
    }

    private static class UserAccountUpdate extends AbstractUpdate {

        public UserAccountUpdate(final String sAccount, final String sPassword) {
            String sNow = DateUtil.now();
            setUpdate(UPDATE + TABLE + SET + PASSWORD + EQUALS + getStringAttrib(encode(sPassword)) + COMMA + PASSWORD_DATE + EQUALS + StringUtil.quote(sNow) + WHERE + ACCOUNT + EQUALS + getStringAttrib(sAccount));
        }
    }

    public static boolean newUserEncodePwd(final String sAccount, final String sPassword, int nType, long nStatus, final String sEmail, final String sOrganization, final String sComments, final String sFullName, long nService) {
        return SqlUtilities.stdExecute(new UserAccountInsert(sAccount, encode(sPassword), nType, nStatus, sEmail, sOrganization, sComments, sFullName, nService));
    }

    public static boolean newUserEncodedPwd(final String sAccount, final String sEncodedPwd, int nType, long nStatus, final String sEmail, final String sOrganization, final String sComments, final String sFullName, long nService) {
        return SqlUtilities.stdExecute(new UserAccountInsert(sAccount, sEncodedPwd, nType, nStatus, sEmail, sOrganization, sComments, sFullName, nService));
    }

    public static ArrayList<Record> getAllUsers() {
        ArrayList<Record> ra = new ArrayList<Record>();
        try {
            ResultSet rs = SqlUtilities.stdQuery(new UserAccountSelect());
            if (rs != null) {
                while (rs.next()) {
                    ra.add(new Record(rs));
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ra;
    }

    public static Record getUserInfoFromAccount(final String sAccount) {
        return SqlUtilities.stdGetRecord(UserAccountSelect.getSelectFromAccount(sAccount));
    }

    public static Record getUserInfoFromEmail(final String sEmail) {
        return SqlUtilities.stdGetRecord(UserAccountSelect.getSelectFromEmail(sEmail));
    }

    public static boolean isUserActive(final String sAccount) {
        Record r = SqlUtilities.stdGetRecord(UserAccountSelect.getSelectFromAccount(sAccount));
        return (r != null && r.getData(STATUS).equals("" + ACTIVE));
    }

    private static Record getUserInfo(final String sAccount, final String sPassword) {
        return SqlUtilities.stdGetRecord(new UserAccountSelect(sAccount, sPassword));
    }

    public static boolean updateUserPwd(final String sAccount, final String sPasswd) {
        return SqlUtilities.stdExecute(new UserAccountUpdate(sAccount, sPasswd));
    }

    private static final int FIXED_HASH_SIZE = 20;

    private static final int FIXED_SALT_SIZE = 8;

    @SuppressWarnings("unused")
    private static final String SSHA_PREFIX = "{SSHA}";

    @SuppressWarnings("unused")
    private static int chkPasswd(final String sInputPwd, final String sSshaPwd) {
        assert sInputPwd != null;
        assert sSshaPwd != null;
        int r = ERR_LOGIN_ACCOUNT;
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] ba = decoder.decodeBuffer(sSshaPwd);
            assert ba.length >= FIXED_HASH_SIZE;
            byte[] hash = new byte[FIXED_HASH_SIZE];
            byte[] salt = new byte[FIXED_SALT_SIZE];
            System.arraycopy(ba, 0, hash, 0, FIXED_HASH_SIZE);
            System.arraycopy(ba, FIXED_HASH_SIZE, salt, 0, FIXED_SALT_SIZE);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sInputPwd.getBytes());
            md.update(salt);
            byte[] baPwdHash = md.digest();
            if (MessageDigest.isEqual(hash, baPwdHash)) {
                r = ERR_LOGIN_OK;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return r;
    }

    public static int getUserLogin(final String sAccount, final String sPasswrd) {
        int r = ERR_LOGIN_ACCOUNT;
        Record rcd = getUserInfo(sAccount, sPasswrd);
        if (rcd != null) {
            Long s = rcd.getLong(STATUS);
            if (s != null && s == ACTIVE) {
                r = ERR_LOGIN_OK;
            } else {
                r = ERR_LOGIN_STATUS;
            }
        }
        if (m_nDebug > 5) System.out.println("UserAccount::getUserLogin(): r=" + r);
        return r;
    }

    public static void testAllUsers(String[] args) {
        ArrayList<Record> ra = getAllUsers();
        for (Record r : ra) {
            System.out.println(r.getData(ACCOUNT) + "\t\ttype=" + r.getData(TYPE) + " status=" + r.getData(STATUS));
        }
    }

    public static void testUserInsert(String[] args) {
        if (args.length > 8) {
            System.out.println(newUserEncodePwd(args[0], args[1], Integer.parseInt(args[2]), Long.parseLong(args[3]), args[4], args[5], args[6], args[7], Integer.parseInt(args[8])));
        }
    }

    public static void testUserInfo(String[] args) {
        if (args.length > 1) {
            System.out.println(getUserInfo(args[0], args[1]));
        }
    }

    public static void testEncode(String[] args) {
        if (args.length > 0) {
            System.out.println(args[0]);
            String sEncoded = encode(args[0]);
            System.out.println(sEncoded);
            System.out.println(decode(sEncoded));
        }
    }

    public static void testDecode(String[] args) {
        if (args.length > 0) {
            System.out.println(args[0]);
            System.out.println(decode(args[0]));
        }
    }

    public static void main(String[] args) {
        DatabaseLoader.getInstance(DatabaseLoader.STANDARD_MODE);
        testAllUsers(args);
    }

    public static final String TABLE = "user_account";

    public static final String ACCOUNT = "account";

    public static final String PASSWORD = "password";

    public static final String ACCOUNT_DATE = "account_date";

    public static final String PASSWORD_DATE = "password_date";

    public static final String TYPE = "type";

    public static final String STATUS = "status";

    public static final String EMAIL = "email";

    public static final String ORGANIZATION = "organization";

    public static final String COMMENTS = "comments";

    public static final String FULLNAME = "fullname";

    public static final String SERVICE = "service";

    public static final long INACTIVE = 0;

    public static final long ACTIVE = 1;

    public static final int ERR_LOGIN_OK = 0;

    public static final int ERR_LOGIN_ACCOUNT = 1;

    public static final int ERR_LOGIN_STATUS = 2;

    public static final int ML_PASSWORD = 45;

    private static int m_nDebug = 1;
}
