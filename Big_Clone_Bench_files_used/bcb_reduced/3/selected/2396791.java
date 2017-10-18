package lt.bsprendimai.ddesk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map.Entry;
import lt.bsprendimai.ddesk.dao.Company;
import lt.bsprendimai.ddesk.dao.CompanyContract;
import lt.bsprendimai.ddesk.dao.Person;
import lt.bsprendimai.ddesk.dao.SessionHolder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.Query;

/**
 * User session and information related functioanlity.
 * Permissions, apssword and other functionality.
 *
 *
 * @author Aleksandr Panzin (JAlexoid) alex@activelogic.eu
 */
@SuppressWarnings("unchecked")
public class UserHandler implements Serializable {

    /**
	 *
	 */
    private static final long serialVersionUID = -5373796467908583521L;

    private Person user = new Person();

    private Company company = new Company();

    private CompanyContract contract = new CompanyContract();

    private TicketFilter filterSort = new TicketFilter();

    private boolean loggedIn = false;

    private Locale userLocale;

    private String originalPwd;

    private String pwd1;

    private String pwd2;

    private String email;

    private String phone;

    private Integer language = 1;

    private Date lastLogin;

    /** Creates a new instance of UserHandler */
    public UserHandler() {
    }

    public Person getUser() {
        return user;
    }

    public void setUser(Person user) {
        this.user = user;
    }

    public String logout() {
        saveFilter();
        loggedIn = false;
        user = new Person();
        ParameterAccess.sessionClose();
        return StandardResults.LOGOUT;
    }

    public String login() {
        try {
            char[] pwdMD5 = Hex.encodeHex(MessageDigest.getInstance("MD5").digest(user.getPassword().getBytes()));
            String password = new String(pwdMD5);
            if (password.length() < 32) {
                for (int i = (32 - password.length()); i > 0; i--) {
                    password = "0" + password;
                }
            }
            Query q = SessionHolder.currentSession().getSess().createQuery(" FROM " + Person.class.getName() + "  WHERE lower(loginCode) = lower(?) AND password = ? ");
            q.setString(0, user.getLoginCode().trim());
            q.setString(1, password.trim());
            List l = q.list();
            if (q.list().isEmpty()) {
                loggedIn = false;
                user.setName(null);
                user.setPassword(null);
                if (this.userLocale == null) this.userLocale = Locale.getDefault();
                String message = UIMessenger.getMessage(this.userLocale, "application.login.error");
                UIMessenger.addErrorMessage(message, "");
                return StandardResults.FAIL;
            } else {
                loggedIn = true;
                user = (Person) l.get(0);
                lastLogin = user.getLastLogin();
                user.setLastLogin(new Date());
                user.update();
                this.userLocale = ParameterAccess.getLocale(user.getLanguage());
                new ParameterAccess().setLanguage(user.getLanguage());
                email = user.getEmail();
                phone = user.getPhoneNo();
                for (Entry<Integer, String> c : ParameterAccess.getLanguages().entrySet()) {
                    if (c.getValue().equals(user.getLanguage())) this.setLanguage(c.getKey());
                }
                restoreFilter();
                if (user.getCompany() == Company.OWNER || user.getLoginLevel() == Person.PARTNER) {
                    return StandardResults.INTRANET;
                } else {
                    company = (Company) SessionHolder.currentSession().getSess().createQuery(" FROM " + Company.class.getName() + "  WHERE id = ?").setInteger(0, user.getCompany()).uniqueResult();
                    contract = (CompanyContract) SessionHolder.currentSession().getSess().createQuery(" FROM " + CompanyContract.class.getName() + "  WHERE company = ?").setInteger(0, user.getCompany()).uniqueResult();
                    return StandardResults.SUCCESS;
                }
            }
        } catch (Exception ex) {
            SessionHolder.endSession();
            UIMessenger.addFatalKeyMessage("error.transaction.abort", getUserLocale());
            ex.printStackTrace();
            return StandardResults.FAIL;
        }
    }

    public void loginNoPw() {
        try {
            Query q = SessionHolder.currentSession().getSess().createQuery(" FROM " + Person.class.getName() + "  WHERE lower(loginCode) = lower(?) ");
            q.setString(0, user.getLoginCode().trim());
            List l = q.list();
            if (q.list().isEmpty()) {
            } else {
                loggedIn = true;
                user = (Person) l.get(0);
                email = user.getEmail();
                phone = user.getPhoneNo();
                for (Entry<Integer, String> c : ParameterAccess.getLanguages().entrySet()) {
                    if (c.getValue().equals(user.getLanguage())) this.setLanguage(c.getKey());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loginNoPw(int uid) {
        loggedIn = false;
        changeUser(uid);
    }

    public void changeUser(int uid) {
        try {
            Query q = SessionHolder.currentSession().getSess().createQuery(" FROM " + Person.class.getName() + "  WHERE id = ? ");
            q.setInteger(0, uid);
            List l = q.list();
            if (q.list().isEmpty()) {
            } else {
                loggedIn = true;
                user = (Person) l.get(0);
                email = user.getEmail();
                phone = user.getPhoneNo();
                for (Entry<Integer, String> c : ParameterAccess.getLanguages().entrySet()) {
                    if (c.getValue().equals(user.getLanguage())) this.setLanguage(c.getKey());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void changeUser(String to) {
        user.setLoginCode(to);
        loginNoPw();
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public Locale getUserLocale() {
        if (this.userLocale == null) return Locale.getDefault();
        return userLocale;
    }

    public void setUserLocale(Locale userLocale) {
        this.userLocale = userLocale;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getOriginalPwd() {
        return "";
    }

    public void setOriginalPwd(String original) {
        this.originalPwd = original;
    }

    public String getPwd1() {
        return "";
    }

    public void setPwd1(String pwd1) {
        this.pwd1 = pwd1;
    }

    public String getPwd2() {
        return "";
    }

    public void setPwd2(String pwd2) {
        this.pwd2 = pwd2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String changeEmail() {
        try {
            String lang = ParameterAccess.getLanguages().get(getLanguage()).toLowerCase();
            user.setLanguage(lang);
            this.userLocale = ParameterAccess.getLocale(user.getLanguage());
            String ret = user.update();
            if (ret.equals(StandardResults.SUCCESS)) {
                String message = UIMessenger.getMessage(this.userLocale, "application.login.saved");
                UIMessenger.addInfoMessage(message, "");
            }
            return StandardResults.SUCCESS;
        } catch (Exception ex) {
            SessionHolder.endSession();
            UIMessenger.addFatalKeyMessage("error.transaction.abort", getUserLocale());
            ex.printStackTrace();
            return StandardResults.FAIL;
        }
    }

    public String changePassword() {
        try {
            String password = new BigInteger(1, MessageDigest.getInstance("MD5").digest(originalPwd.getBytes())).toString(16);
            if (password.length() < 32) {
                for (int i = (32 - password.length()); i > 0; i--) {
                    password = "0" + password;
                }
            }
            String message;
            if (user.getPassword().equals(password)) {
                if (!pwd1.equals(pwd2)) {
                    message = UIMessenger.getMessage(this.userLocale, "application.login.passwordsDoNotMatch");
                    UIMessenger.addErrorMessage(message, "");
                    return StandardResults.FAIL;
                }
                password = new BigInteger(1, MessageDigest.getInstance("MD5").digest(pwd1.getBytes())).toString(16);
                if (password.length() < 32) {
                    for (int i = (32 - password.length()); i > 0; i--) {
                        password = "0" + password;
                    }
                }
                user.setPassword(password);
                String ret = user.update();
                if (ret.equals(StandardResults.SUCCESS)) {
                    message = UIMessenger.getMessage(this.userLocale, "application.login.saved");
                    UIMessenger.addInfoMessage(message, "");
                }
            } else {
                message = UIMessenger.getMessage(this.userLocale, "application.login.wrongPassword");
                UIMessenger.addErrorMessage(message, "");
                return StandardResults.FAIL;
            }
            return StandardResults.SUCCESS;
        } catch (Exception ex) {
            SessionHolder.endSession();
            UIMessenger.addFatalKeyMessage("error.transaction.abort", getUserLocale());
            ex.printStackTrace();
            return StandardResults.FAIL;
        }
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getLanguage() {
        return language;
    }

    public void setLanguage(Integer language) {
        this.language = language;
    }

    public CompanyContract getContract() {
        return contract;
    }

    public void setContract(CompanyContract contract) {
        this.contract = contract;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public TicketFilter getFilterSort() {
        return filterSort;
    }

    public void setFilterSort(TicketFilter filterSort) {
        this.filterSort = filterSort;
    }

    public void restoreFilter() {
        try {
            ByteArrayInputStream bos = new ByteArrayInputStream(Base64.decodeBase64(this.user.getSearchFilter().getBytes()));
            ObjectInputStream oos = new ObjectInputStream(bos);
            filterSort = (TicketFilter) oos.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void saveFilter() {
        if (this.user.getId() == null) return;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(filterSort);
            byte[] arr = bos.toByteArray();
            this.user.setSearchFilter(new String(Base64.encodeBase64(arr)));
            this.user.update();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isOwner() {
        if (user.getCompany() == Company.OWNER) return true; else return false;
    }

    public boolean isPartner() {
        if (user.getLoginLevel() == Person.PARTNER) return true; else return false;
    }

    public String getTimeZone() {
        return TimeZone.getDefault().getID();
    }
}
