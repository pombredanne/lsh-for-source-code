package com.funambol.google.items.dao;

import com.funambol.framework.logging.FunambolLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.google.exception.GmailException;
import com.funambol.google.exception.GmailLoginException;
import com.funambol.google.items.model.ContactField;
import com.funambol.google.items.model.ContactSection;
import com.funambol.google.items.model.GmailContact;
import com.funambol.google.utils.CSVReader;
import com.funambol.google.utils.LanguagesLoader;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Data Access Object that performs operations of insert, update, delete and retrieve of Gmail contacts.
 * @author Tiago Conde
 * @version $Id: GmailContactDAO.java,v 1.0.0 2007/07/30 15:00:00 Tiago Conde Exp $
 *
 */
public class GmailContactDAO {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.3) Gecko/20070309 Firefox/2.0.0.3";

    /**
     * Charset returned by Gmail used for insert, update and retrieve operations.
     * (charset of the language defined in Gmail Settings by the user).
     */
    private String GMAIL_OUTPUT_CHARSET = null;

    /**
     * Charset used in insert and update operations of Gmail contacts.
     */
    private String GMAIL_INPUT_CHARSET = "UTF-8";

    private String email = null;

    private String password = null;

    private HttpClient client;

    private static Random random = new Random();

    private String proxyHost = null;

    private int proxyPort;

    /**
     * Flag that tells if an account use the SETSID operation at login.
     */
    private boolean isLoginWithSID = false;

    /**
     * List with the word 'Personal' in the languages supported by Gmail
     */
    private Hashtable<String, String> personalTable = null;

    /**
     * List with the word 'Work' in the languages supported by Gmail
     */
    private Hashtable<String, String> workTable = null;

    /**
     * List with the word 'Other' in the languages supported by Gmail
     */
    private Hashtable<String, String> otherTable = null;

    static {
        System.getProperties().setProperty("httpclient.useragent", USER_AGENT);
    }

    private ResourceBundle properties = ResourceBundle.getBundle("config/GmailContact");

    private FunambolLogger log = FunambolLoggerFactory.getLogger("funambol.google");

    public GmailContactDAO(String email, String password, String proxyHost, int proxyPort) {
        this(email, password);
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        client.getHostConfiguration().setProxy(this.proxyHost, this.proxyPort);
    }

    public GmailContactDAO(String email, String password) {
        if (log.isInfoEnabled()) log.info("GmailContactDAO created...");
        this.email = email;
        this.password = password;
        client = new HttpClient();
        loadLanguages();
    }

    /**
     * Performs the login operation in the Gmail service.
     * @throws GmailLoginException
     */
    public void login() throws GmailLoginException {
        int statusCode = -1;
        PostMethod post = new PostMethod(properties.getString("login_page"));
        NameValuePair[] data = { new NameValuePair("service", "mail"), new NameValuePair("Email", email), new NameValuePair("Passwd", password), new NameValuePair("null", "Sign in"), new NameValuePair("continue", "https://gmail.google.com/gmail") };
        if (log.isInfoEnabled()) log.info("Gmail: Starting login on gmail service...");
        post.setRequestBody(data);
        try {
            statusCode = client.executeMethod(post);
            if (statusCode == 200) throw new GmailLoginException("Gmail: Invalid authentication");
            if (log.isInfoEnabled()) log.info("Gmail: valid authentication...");
        } catch (HttpException e) {
            throw new GmailLoginException("Gmail: HttpException on 'login post': " + e.getMessage());
        } catch (IOException e) {
            throw new GmailLoginException("Gmail: IOException on 'login post': " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("login post successful... starting 1st redirect...");
        GetMethod get = null;
        String resultGet = null;
        String redirectLocation;
        Header locationHeader = post.getResponseHeader("location");
        if (locationHeader != null) {
            redirectLocation = locationHeader.getValue();
            get = new GetMethod(redirectLocation);
            get.setFollowRedirects(true);
            try {
                statusCode = client.executeMethod(get);
                resultGet = getResponseAsStringFromStream(get.getResponseBodyAsStream());
                if (log.isTraceEnabled()) log.trace("1st redirect successful...");
            } catch (HttpException e) {
                throw new GmailLoginException("Gmail: HttpException on '1st redirect login': " + e.getMessage());
            } catch (IOException e) {
                throw new GmailLoginException("Gmail: IOException on '1st redirect login': " + e.getMessage());
            } catch (GmailException e) {
                throw new GmailLoginException(e.getMessage());
            } finally {
                get.releaseConnection();
            }
        }
        if (log.isTraceEnabled()) log.trace("checking login type...");
        try {
            String GMAIL_AT = getCookie("GMAIL_AT");
            if (GMAIL_AT == null) {
                if (log.isInfoEnabled()) log.info("Gmail: login with SID... starting SETSID method...");
                isLoginWithSID = true;
                resultGet = redirectToMetaUrl(resultGet, true);
            } else {
                if (log.isInfoEnabled()) log.info("Gmail: login without SID...");
            }
        } catch (Exception e) {
            throw new GmailLoginException("Gmail: Exception on login SETSID: " + e.getMessage());
        }
        if (resultGet.contains("Lockdown in sector 4")) throw new GmailLoginException("Gmail: Account disabled -> Lockdown in sector 4");
        if (log.isInfoEnabled()) log.info("Gmail: login process successful...");
    }

    /**
     * Performs the logout operation in the Gmail service.
     * @throws GmailException
     */
    public void logout() throws GmailException {
        GetMethod get = null;
        String resultGet = null;
        if (log.isInfoEnabled()) log.info("Gmail: starting logout process...");
        try {
            String query = properties.getString("logout_page");
            query = query.replace("[RANDOM_INT]", "" + random.nextInt());
            get = new GetMethod(query);
            get.setFollowRedirects(true);
            get.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            client.executeMethod(get);
            resultGet = getResponseAsStringFromStream(get.getResponseBodyAsStream());
        } catch (IOException e) {
            if (log.isInfoEnabled()) log.info("Gmail: IOException on 'logout': " + e.getMessage());
        } finally {
            get.releaseConnection();
        }
        try {
            if (isLoginWithSID == true) {
                if (log.isTraceEnabled()) log.trace("starting 1st redirect logout with sid...");
                resultGet = redirectToMetaUrl(resultGet, true);
                if (log.isTraceEnabled()) log.trace("starting 2nd redirect logout with sid...");
                redirectToMetaUrl(resultGet, false);
            }
            isLoginWithSID = false;
        } catch (Exception e) {
            if (log.isInfoEnabled()) log.info("Gmail: Exception on logout CLEARSID: " + e.getMessage());
        }
    }

    /**
     * Get the list of all gmail contacts by getting the csv file from the export option in Gmail.
     * (Each GmailContact has only information for synchronization)
     * @return List of Gmail Contacts
     * @throws GmailException
     */
    @SuppressWarnings("unchecked")
    public ArrayList<GmailContact> getAllContacts() throws GmailException {
        String query = properties.getString("export_page");
        query = query.replace("[RANDOM_INT]", "" + random.nextInt());
        int statusCode = -1;
        GetMethod get = new GetMethod(query);
        if (log.isInfoEnabled()) log.info("getting all contacts ...");
        try {
            statusCode = client.executeMethod(get);
            if (statusCode != 200) throw new GmailException("In contacts export page: Status code expected: 200 -> Status code returned: " + statusCode);
        } catch (HttpException e) {
            throw new GmailException("HttpException in contacts export page:" + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in contacts export page:" + e.getMessage());
        } finally {
            get.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("accessing contacts export page successful...");
        String query_post = properties.getString("outlook_export_page");
        PostMethod post = new PostMethod(query_post);
        post.addRequestHeader("Accept-Encoding", "gzip,deflate");
        post.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.8");
        NameValuePair[] data = { new NameValuePair("at", getCookie("GMAIL_AT")), new NameValuePair("ecf", "o"), new NameValuePair("ac", "Export Contacts") };
        post.setRequestBody(data);
        if (log.isTraceEnabled()) log.trace("getting contacts csv file...");
        try {
            statusCode = client.executeMethod(post);
            if (statusCode != 200) throw new GmailException("In csv file post: Status code expected: 200 -> Status code returned: " + statusCode);
            if (log.isTraceEnabled()) log.trace("Gmail: csv charset: " + post.getResponseCharSet());
            GMAIL_OUTPUT_CHARSET = post.getResponseCharSet();
            InputStreamReader isr = new InputStreamReader(new GZIPInputStream(post.getResponseBodyAsStream()), post.getResponseCharSet());
            CSVReader reader = new CSVReader(isr);
            List csvEntries = reader.readAll();
            reader.close();
            ArrayList<GmailContact> contacts = new ArrayList<GmailContact>();
            MessageDigest m = MessageDigest.getInstance("MD5");
            if (log.isTraceEnabled()) log.trace("creating Gmail contacts...");
            for (int i = 1; i < csvEntries.size(); i++) {
                GmailContact contact = new GmailContact();
                String[] value = (String[]) csvEntries.get(i);
                for (int j = 0; j < value.length; j++) {
                    switch(j) {
                        case 0:
                            contact.setName(value[j]);
                            break;
                        case 1:
                            contact.setEmail(value[j]);
                            if (contact.getName() == null) contact.setIdName(value[j]); else contact.setIdName(contact.getName() + value[j]);
                            break;
                        case 2:
                            contact.setNotes(value[j]);
                            break;
                        case 3:
                            contact.setEmail2(value[j]);
                            break;
                        case 4:
                            contact.setEmail3(value[j]);
                            break;
                        case 5:
                            contact.setMobilePhone(value[j]);
                            break;
                        case 6:
                            contact.setPager(value[j]);
                            break;
                        case 7:
                            contact.setCompany(value[j]);
                            break;
                        case 8:
                            contact.setJobTitle(value[j]);
                            break;
                        case 9:
                            contact.setHomePhone(value[j]);
                            break;
                        case 10:
                            contact.setHomePhone2(value[j]);
                            break;
                        case 11:
                            contact.setHomeFax(value[j]);
                            break;
                        case 12:
                            contact.setHomeAddress(value[j]);
                            break;
                        case 13:
                            contact.setBusinessPhone(value[j]);
                            break;
                        case 14:
                            contact.setBusinessPhone2(value[j]);
                            break;
                        case 15:
                            contact.setBusinessFax(value[j]);
                            break;
                        case 16:
                            contact.setBusinessAddress(value[j]);
                            break;
                        case 17:
                            contact.setOtherPhone(value[j]);
                            break;
                        case 18:
                            contact.setOtherFax(value[j]);
                            break;
                        case 19:
                            contact.setOtherAddress(value[j]);
                            break;
                    }
                }
                m.update(contact.toString().getBytes());
                if (log.isTraceEnabled()) log.trace("setting Md5 Hash...");
                contact.setMd5Hash(new BigInteger(m.digest()).toString());
                contacts.add(contact);
            }
            if (log.isTraceEnabled()) log.trace("Mapping contacts uid...");
            Collections.sort(contacts);
            ArrayList<GmailContact> idList = getAllContactsID();
            for (int i = 0; i < idList.size(); i++) {
                contacts.get(i).setId(idList.get(i).getId());
            }
            if (log.isInfoEnabled()) log.info("getting all contacts info successful...");
            return contacts;
        } catch (HttpException e) {
            throw new GmailException("HttpException in csv file post:" + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in csv file post:" + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new GmailException("No such md5 algorithm " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * Insert a new contact in Gmail Service
     * @param newcontact
     * @return the inserted contact (GmailContact).
     * @throws GmailException
     */
    public GmailContact insertContact(GmailContact newcontact) throws GmailException {
        if (log.isInfoEnabled()) log.info("Inserting Contact...");
        accessContactPage();
        PostMethod post = new PostMethod(properties.getString("insert_page"));
        post.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        post.setRequestBody(buildPostDataInsert(newcontact));
        int statusCode = -1;
        String result = null;
        try {
            statusCode = client.executeMethod(post);
            if (statusCode != 200) throw new GmailException("In insertContact post method: Status code expected: 200 -> Status code returned: " + statusCode);
            result = getResponseAsStringFromStream(post.getResponseBodyAsStream());
            if (log.isTraceEnabled()) log.trace("Insert Contact post method successful.");
        } catch (HttpException e) {
            throw new GmailException("HttpException in Insert Contact post method " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in Insert Contact post method " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("Checking Insert response...");
        String status = null;
        try {
            status = getStatusCode(result);
            if (status.equalsIgnoreCase("1") == true) {
                Pattern idPattern = Pattern.compile("(?<=D\\(\\[\"cov\",\\[\"ce\",\")(.*?)(?=\",\")");
                Matcher idMatcher = idPattern.matcher(result);
                idMatcher.find();
                newcontact.setId(idMatcher.group());
                if (log.isInfoEnabled()) log.info("Contact insert successful...");
                return newcontact;
            } else {
                throw new GmailException("Contact not inserted -> " + getErrorMessage(result));
            }
        } catch (Exception e) {
            throw new GmailException("Exception checking Insert response: " + e.getMessage());
        }
    }

    /**
     * Updates an existing contact in Gmail Service
     * (Before updating, the new Sync updated info is merged with the nonSync info of the contact in Gmail)
     * @param contact
     * @return the updated contact (GmailContact).
     * @throws GmailException
     */
    public GmailContact editContact(GmailContact contact) throws GmailException {
        if (log.isInfoEnabled()) log.info("Updating Contact with id: " + contact.getId());
        if (contact.getId() == null || contact.getId().equals("")) {
            throw new GmailException("Update contact with invalid id");
        }
        accessContactPage();
        contact = mergeCientAndServerContact(contact, getContact(contact.getId()));
        PostMethod post = new PostMethod(properties.getString("update_page"));
        post.setRequestBody(buildPostDataUpdate(contact));
        String result = null;
        int statusCode = -1;
        try {
            statusCode = client.executeMethod(post);
            if (statusCode != 200) throw new GmailException("In update Contact post method: Status code expected: 200 -> Status code returned: " + statusCode);
            result = getResponseAsStringFromStream(post.getResponseBodyAsStream());
            if (log.isTraceEnabled()) log.trace("Update Contact post method successful.");
        } catch (HttpException e) {
            throw new GmailException("HttpException Update Contact post method " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException Update Contact post method " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("Checking Update response...");
        String status = null;
        try {
            status = getStatusCode(result);
            if (status.equalsIgnoreCase("1") == true) {
                if (log.isInfoEnabled()) log.info("Contact update successful...");
                return contact;
            } else {
                throw new GmailException("Contact not updated, id:" + contact.getId() + " -> " + getErrorMessage(result));
            }
        } catch (Exception e) {
            throw new GmailException("Update Contact Operation Status - " + e.getMessage(), e);
        }
    }

    /**
     * Remove a contact from Gmail Service.
     * (Old Method: Not being used. The contacts are removed in one operation)
     * @param id of the contact to remove
     * @throws GmailException
     */
    public void removeContact(String id) throws GmailException {
        if (log.isInfoEnabled()) log.info("Removing Contact with id: " + id);
        if (id == null || id.length() < 1) {
            throw new GmailException("contact with invalid id");
        }
        int statusCode = -1;
        String resultPost = null;
        PostMethod post = new PostMethod(properties.getString("delete_page"));
        post.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        post.addRequestHeader("Accept-Language", "en-us,en;q=0.5");
        NameValuePair[] data = { new NameValuePair("act", "dcal"), new NameValuePair("at", getCookie("GMAIL_AT")), new NameValuePair("cpt", null), new NameValuePair("cl_nw", null), new NameValuePair("cl_nm", null), new NameValuePair("c", id) };
        post.setRequestBody(data);
        try {
            statusCode = client.executeMethod(post);
            if (statusCode != 200) throw new GmailException("In remove contact post method: Status code expected: 200 -> Status code returned: " + statusCode);
            resultPost = getResponseAsStringFromStream(post.getResponseBodyAsStream());
            if (log.isTraceEnabled()) log.trace("Remove Contact post method successful.");
        } catch (HttpException e) {
            throw new GmailException("HttpException in remove contact: " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in remove contact: " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("Checking remove response...");
        String status = null;
        try {
            status = getStatusCode(resultPost);
            if (status.equalsIgnoreCase("0")) {
                throw new GmailException("Contact not removed, id:" + id + " -> " + getErrorMessage(resultPost));
            }
            if (log.isInfoEnabled()) log.info("Contact remove successful...");
        } catch (Exception e) {
            throw new GmailException("Error deleting Contact: " + e.getMessage());
        }
    }

    /**
     * Remove a list of contacts from Gmail Service
     * @param List of the ids of the contacts to remove
     * @throws GmailException
     */
    public void removeContacts(String[] ids) throws GmailException {
        if (log.isInfoEnabled()) log.info("Removing Contacts...");
        int statusCode = -1;
        String resultPost = null;
        PostMethod post = new PostMethod(properties.getString("delete_page"));
        post.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        post.addRequestHeader("Accept-Language", "en-us,en;q=0.5");
        if (log.isTraceEnabled()) log.trace("Creating post data for removing contacts...");
        ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new NameValuePair("act", "dcal"));
        data.add(new NameValuePair("at", getCookie("GMAIL_AT")));
        data.add(new NameValuePair("cpt", null));
        data.add(new NameValuePair("cl_nw", null));
        data.add(new NameValuePair("cl_nm", null));
        for (int i = 0; i < ids.length; i++) {
            data.add(new NameValuePair("c", ids[i]));
        }
        post.setRequestBody(data.toArray(new NameValuePair[data.size()]));
        try {
            statusCode = client.executeMethod(post);
            if (statusCode != 200) throw new GmailException("In remove contacts post method: Status code expected: 200 -> Status code returned: " + statusCode);
            resultPost = getResponseAsStringFromStream(post.getResponseBodyAsStream());
            if (log.isTraceEnabled()) log.trace("Remove Contacts post method successful.");
        } catch (HttpException e) {
            throw new GmailException("HttpException in remove contacts: " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in remove contacts: " + e.getMessage());
        } finally {
            post.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("Checking remove response...");
        String status = null;
        try {
            status = getStatusCode(resultPost);
            log.info("status:" + status);
            if (status.equalsIgnoreCase("0")) {
                throw new GmailException("Contacts not removed -> " + getErrorMessage(resultPost));
            }
            if (log.isInfoEnabled()) log.info("Contacts remove successful...");
        } catch (Exception e) {
            throw new GmailException("Delete Contacts Operation Status - " + e.getMessage(), e);
        }
    }

    /**
     * Get a Gmail Contact from Gmail Service.
     * (The information is stored both in the GmailContact sections and on the GmailContact sync properties)
     * @param id
     * @return GmailContact
     * @throws GmailException
     */
    private GmailContact getContact(String id) throws GmailException {
        String query = properties.getString("contact_page");
        query = query.replace("[CONTACT_ID]", id);
        query = query.replace("[RANDOM_INT]", "" + random.nextInt());
        int statusCode = -1;
        String resultGet = null;
        GetMethod get = new GetMethod(query);
        if (log.isInfoEnabled()) log.info("Getting contact info with id: " + id);
        if (id == null || id.equals("")) {
            throw new GmailException("Contact id null or empty.");
        }
        try {
            statusCode = client.executeMethod(get);
            if (statusCode != 200) throw new GmailException("In getContact: Status code expected: 200 -> Status code returned: " + statusCode);
            resultGet = getResponseAsStringFromStream(get.getResponseBodyAsStream());
            if (resultGet.contains("D([\"cf\",\"The requested contact no longer exists.\"]);")) {
                throw new GmailException("The requested contact no longer exists (id:+" + id + ")");
            }
            return parseGmailDataPackInfo(resultGet);
        } catch (HttpException e) {
            throw new GmailException("HttpException in get contact get method: " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in get contact get method: " + e.getMessage());
        } finally {
            get.releaseConnection();
        }
    }

    /**
     * Gets all the Gmail Contacts id's. Used in GetAllContacts() to identify the contacts retrieved in the csv file.
     * @return List of Gmail Contacts
     * @throws GmailException
     */
    @SuppressWarnings("unchecked")
    private ArrayList<GmailContact> getAllContactsID() throws GmailException {
        int statusCode = -1;
        String query = properties.getString("contactsid_page");
        query = query.replace("[RANDOM_INT]", "" + random.nextInt());
        String input = null;
        GetMethod get = new GetMethod(query);
        get.setFollowRedirects(true);
        try {
            statusCode = client.executeMethod(get);
            if (statusCode != 200) throw new GmailException("In getAllContactsIds get method: Status code expected: 200 -> Status code returned: " + statusCode);
            input = getResponseAsStringFromStream(get.getResponseBodyAsStream());
            if (log.isTraceEnabled()) log.trace("getAllContactsIds get method successful.");
        } catch (HttpException e) {
            throw new GmailException("Get Method Error - status code:" + statusCode + " - " + e.getMessage(), e);
        } catch (IOException e) {
            throw new GmailException("Get Method Error - status code:" + statusCode + " - " + e.getMessage(), e);
        } finally {
            get.releaseConnection();
        }
        if (log.isTraceEnabled()) log.trace("creating Gmail contacts...");
        ArrayList<GmailContact> contacts = new ArrayList<GmailContact>();
        try {
            input = input.replaceAll("\n", "");
            String[] contactsSplited = input.split("\\[\"ce\",");
            for (int i = 1; i < contactsSplited.length; i++) {
                char[] resultGetArray = contactsSplited[i].toCharArray();
                ArrayList<String> parseList = new ArrayList<String>();
                StringBuffer sb = new StringBuffer();
                for (int j = 1; j < resultGetArray.length; j++) {
                    if (resultGetArray[j] == '"' && resultGetArray[j - 1] != '\\') {
                        parseList.add(sb.toString());
                        sb = new StringBuffer();
                    } else {
                        sb.append(resultGetArray[j]);
                    }
                }
                GmailContact contact = new GmailContact();
                contact.setId(parseList.get(0));
                contact.setName(StringEscapeUtils.unescapeJavaScript(parseList.get(4)));
                contact.setEmail(StringEscapeUtils.unescapeJavaScript(parseList.get(6)));
                contact.setIdName(StringEscapeUtils.unescapeJavaScript(parseList.get(4) + parseList.get(6)));
                contacts.add(contact);
            }
            Collections.sort(contacts);
            return contacts;
        } catch (Exception e) {
            throw new GmailException("Error parsing Gmail Contact: " + e.getMessage());
        }
    }

    /**
     * Parses the result of an operation in Gmail Service and retrieved its status code.
     * (0 - unsuccessful ; 1 - successful)
     * @param result - String that contains the status code to be parsed.
     * @return Status Code
     * @throws GmailException
     */
    private String getStatusCode(String result) throws GmailException {
        if (log.isTraceEnabled()) log.trace("Getting operation status code...");
        try {
            result = result.replaceAll("\n", "");
            Pattern statusPattern = Pattern.compile("(?<=D\\(\\[\"ar\",)(.*?)(?=,\")");
            Matcher statusMatcher = statusPattern.matcher(result);
            statusMatcher.find();
            return statusMatcher.group();
        } catch (Exception e) {
            throw new GmailException("Can't get operation status code: " + e.getMessage());
        }
    }

    /**
     * Parses the result of an operation in Gmail Service and retrieved its error message in case of failure.
     * @param result - String that contains the error message to be parsed.
     * @return Error Message
     */
    private String getErrorMessage(String result) {
        int begin = result.indexOf("D([\"ar\"");
        int end = result.lastIndexOf("]);//-->");
        return StringEscapeUtils.unescapeJavaScript(result.substring(begin + 10, end));
    }

    /**
     * Converts an http response from InputStream into a String encoded in UTF-8 charset.
     * @param is - InputStream.
     * @return response in String format.
     * @throws GmailException
     */
    private String getResponseAsStringFromStream(InputStream is) throws GmailException {
        StringBuffer sb = new StringBuffer();
        String aux = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((aux = br.readLine()) != null) {
                sb.append(aux);
            }
        } catch (IOException e) {
            throw new GmailException("IOException in getting response as string from stream: " + e.getMessage());
        } finally {
            try {
                br.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }

    /**
     * Redirects into the hyperlink present in the resultGet parameter.
     * @param resultGet - http response that contais the hyperlink to redirect.
     * @param returnGetResult - true if we want to return the response of the http redirect operation.
     * @return the response of the http redirect.
     * @throws GmailException
     */
    private String redirectToMetaUrl(String resultGet, boolean returnGetResult) throws GmailException {
        GetMethod get = null;
        if (log.isTraceEnabled()) log.trace("starting redirect to meta url...");
        try {
            resultGet = resultGet.replaceAll("\n", "");
            Pattern pattern = Pattern.compile("='(.+?)'\"");
            Matcher matcher = pattern.matcher(resultGet);
            if (matcher.find() == false) throw new GmailException("cannot get meta url");
            String link = matcher.group().substring(2, matcher.group().length() - 2);
            link = link.replace("amp;", "");
            if (log.isTraceEnabled()) log.trace("redirecting to: " + link);
            get = new GetMethod(link);
            client.executeMethod(get);
            if (log.isTraceEnabled()) log.trace("redirect successful...");
            if (returnGetResult) return getResponseAsStringFromStream(get.getResponseBodyAsStream());
        } catch (HttpException e) {
            throw new GmailException("HttpException on redirectToMetaUrl: " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException on redirectToMetaUrl: " + e.getMessage());
        } catch (Exception e) {
            throw new GmailException("Exception on redirectToMetaUrl: " + e.getMessage());
        } finally {
            get.releaseConnection();
        }
        return null;
    }

    /**
     * Parses a Gmail contact from its Javascript DataPack structure into a GmailContact object.
     * @param info - the Gmail contact in the DataPack format.
     * @return GmailContact with info in the sections field only.
     * @throws GmailException
     */
    private GmailContact parseGmailDataPackInfo(String info) throws GmailException {
        try {
            if (log.isTraceEnabled()) log.trace("Parsing Gmail DataPack...");
            info = info.replaceAll("\n", "");
            int start = info.indexOf("D([\"cov\",[\"ce\",");
            int end = info.lastIndexOf("]]]);");
            info = info.substring(start, end);
            char[] resultGetArray = info.toCharArray();
            ArrayList<String> parseList = new ArrayList<String>();
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < resultGetArray.length; i++) {
                if (resultGetArray[i] == '"' && resultGetArray[i - 1] != '\\') {
                    parseList.add(sb.toString());
                    sb = new StringBuffer();
                } else {
                    sb.append(resultGetArray[i]);
                }
            }
            parseList.remove(14);
            parseList.remove(13);
            parseList.remove(12);
            parseList.remove(10);
            parseList.remove(8);
            parseList.remove(7);
            parseList.remove(6);
            parseList.remove(4);
            parseList.remove(3);
            parseList.remove(2);
            parseList.remove(1);
            parseList.remove(0);
            if (log.isDebugEnabled()) {
                for (int i = 0; i < parseList.size(); i++) {
                    log.debug("->" + parseList.get(i));
                }
            }
            GmailContact contact = new GmailContact();
            contact.setId(StringEscapeUtils.unescapeJavaScript(parseList.get(0)));
            contact.setName(StringEscapeUtils.unescapeJavaScript(parseList.get(1)));
            contact.setEmail(StringEscapeUtils.unescapeJavaScript(parseList.get(2)));
            int begin = 4;
            if (parseList.get(3).equals("n") && parseList.get(4).equals(",")) {
                contact.setNotes(StringEscapeUtils.unescapeJavaScript(parseList.get(5)));
                begin = 8;
            }
            int order = 1;
            String section = null;
            for (int i = begin; i < parseList.size(); i += 2) {
                if (parseList.get(i - 2).equals("]]],[")) break;
                if (parseList.get(i).equals(",[")) {
                    section = null;
                    section = parseList.get(i - 1);
                    if (personalTable.containsKey(section)) {
                        contact.addSection(section, "1", order++);
                        section = "1";
                    } else if (workTable.containsKey(section)) {
                        contact.addSection(section, "2", order++);
                        section = "2";
                    } else if (otherTable.containsKey(section)) {
                        contact.addSection(section, "3", order++);
                        section = "3";
                    } else {
                        contact.addSection(StringEscapeUtils.unescapeJavaScript(section), "x", order++);
                    }
                } else if (section != null) {
                    String value = StringEscapeUtils.unescapeJavaScript(parseList.get(i + 1));
                    String type = parseList.get(i - 1);
                    setContactFields(contact, section, type, value);
                    contact.addField(type, value);
                    i += 2;
                }
            }
            if (log.isTraceEnabled()) log.trace("Parse successful...");
            return contact;
        } catch (Exception e) {
            throw new GmailException("Error parsing Gmail Data Pack: " + e.getMessage());
        }
    }

    /**
     * Sets a GmailContact field into its sync properties.
     * @param contact - the Gmail Contact to set the information.
     * @param section - the type of the section (1: personal / 2: work / 3:other)
     * @param type - type of the field (e:email / p:phone / m:mobile / b:pager / f:fax / d:company / t:title / a - address)
     * @param value - the value of the field
     */
    private void setContactFields(GmailContact contact, String section, String type, String value) {
        switch(type.charAt(0)) {
            case 'e':
                if (contact.getEmail2() == null) contact.setEmail2(value); else if (contact.getEmail3() == null) contact.setEmail3(value);
                break;
            case 'i':
                break;
            case 'p':
                if (section.equalsIgnoreCase("1")) {
                    if (contact.getHomePhone() == null) contact.setHomePhone(value); else if (contact.getHomePhone2() == null) contact.setHomePhone2(value);
                } else if (section.equalsIgnoreCase("2")) {
                    if (contact.getBusinessPhone() == null) contact.setBusinessPhone(value); else if (contact.getBusinessPhone2() == null) contact.setBusinessPhone2(value);
                } else if (section.equalsIgnoreCase("3")) {
                    if (contact.getOtherPhone() == null) contact.setOtherPhone(value);
                }
                break;
            case 'm':
                if (contact.getMobilePhone() == null) contact.setMobilePhone(value);
                break;
            case 'b':
                if (contact.getPager() == null) contact.setPager(value);
                break;
            case 'f':
                if (section.equalsIgnoreCase("1")) {
                    if (contact.getHomeFax() == null) contact.setHomeFax(value);
                } else if (section.equalsIgnoreCase("2")) {
                    if (contact.getBusinessFax() == null) contact.setBusinessFax(value);
                } else if (section.equalsIgnoreCase("3")) {
                    if (contact.getOtherFax() == null) contact.setOtherFax(value);
                }
                break;
            case 'd':
                if (contact.getCompany() == null) contact.setCompany(value);
                break;
            case 't':
                if (contact.getJobTitle() == null) contact.setJobTitle(value);
                break;
            case 'o':
                break;
            case 'a':
                if (section.equalsIgnoreCase("1")) {
                    if (contact.getHomeAddress() == null) contact.setHomeAddress(value);
                } else if (section.equalsIgnoreCase("2")) {
                    if (contact.getBusinessAddress() == null) contact.setBusinessAddress(value);
                } else if (section.equalsIgnoreCase("3")) {
                    if (contact.getOtherAddress() == null) contact.setOtherAddress(value);
                }
                break;
        }
    }

    /**
     * Merges the updated GmailContact fields with the nonSync fields of the GmailContact present in Gmail Service.
     * @param contact - GmailContact from the client with the updated information.
     * @param serverContact - GmailContact from the Gmail Service.
     * @return merged GmailContact
     */
    private GmailContact mergeCientAndServerContact(GmailContact contact, GmailContact serverContact) {
        boolean email2 = false, email3 = false, mobile = false, pager = false, company = false, title = false;
        boolean homeAddress = false, businessAddress = false, otherAddress = false;
        boolean homeFax = false, businessFax = false, otherFax = false;
        boolean homePhone = false, businessPhone = false, otherPhone = false;
        boolean homePhone2 = false, businessPhone2 = false;
        boolean hasPersonalSection = false, hasWorkSection = false, hasOtherSection = false;
        if (log.isTraceEnabled()) log.trace("Merging cliente and server gmail contacts for update...");
        for (int i = 0; i < serverContact.getSections().size(); i++) {
            ContactSection section = serverContact.getSections().get(i);
            if (section.getFields() != null && section.getFields().size() != 0) {
                contact.addSection(section.getName(), section.getTag(), i + 1);
                if (log.isTraceEnabled()) log.trace("Merging contact section: " + section.getName());
                for (int j = 0; j < section.getFields().size(); j++) {
                    ContactField field = section.getFields().get(j);
                    switch(field.getType().charAt(0)) {
                        case 'e':
                            if (email2 == false && contact.getEmail2() != null && contact.getEmail2().trim().length() > 0) {
                                email2 = true;
                                contact.addField(field.getType(), contact.getEmail2().trim());
                            } else if (email3 == false && contact.getEmail3() != null && contact.getEmail3().trim().length() > 0) {
                                email3 = true;
                                contact.addField(field.getType(), contact.getEmail3().trim());
                            } else {
                                contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 'i':
                            contact.addField("i", field.getValue().trim());
                            break;
                        case 'p':
                            switch(section.getTag().charAt(0)) {
                                case '1':
                                    if (homePhone == false && contact.getHomePhone() != null && contact.getHomePhone().trim().length() > 0) {
                                        homePhone = true;
                                        contact.addField(field.getType(), contact.getHomePhone().trim());
                                    } else if (homePhone2 == false && contact.getHomePhone2() != null && contact.getHomePhone2().trim().length() > 0) {
                                        homePhone2 = true;
                                        contact.addField(field.getType(), contact.getHomePhone2().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                case '2':
                                    if (businessPhone == false && contact.getBusinessPhone() != null && contact.getBusinessPhone().trim().length() > 0) {
                                        businessPhone = true;
                                        contact.addField(field.getType(), contact.getBusinessPhone());
                                    } else if (businessPhone2 == false && contact.getBusinessPhone2() != null && contact.getBusinessPhone2().trim().length() > 0) {
                                        businessPhone2 = true;
                                        contact.addField(field.getType(), contact.getBusinessPhone2().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                case '3':
                                    if (otherPhone == false && contact.getOtherPhone() != null && contact.getOtherPhone().trim().length() > 0) {
                                        otherPhone = true;
                                        contact.addField(field.getType(), contact.getOtherPhone().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                default:
                                    contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 'm':
                            if (mobile == false && contact.getMobilePhone() != null && contact.getMobilePhone().trim().length() > 0) {
                                mobile = true;
                                contact.addField(field.getType(), contact.getMobilePhone().trim());
                            } else {
                                contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 'b':
                            if (pager == false && contact.getPager() != null && contact.getPager().trim().length() > 0) {
                                pager = true;
                                contact.addField(field.getType(), contact.getPager().trim());
                            } else {
                                contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 'f':
                            switch(section.getTag().charAt(0)) {
                                case '1':
                                    if (homeFax == false && contact.getHomeFax() != null && contact.getHomeFax().trim().length() > 0) {
                                        homeFax = true;
                                        contact.addField(field.getType(), contact.getHomeFax().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                case '2':
                                    if (businessFax == false && contact.getBusinessFax() != null && contact.getBusinessFax().trim().length() > 0) {
                                        businessFax = true;
                                        contact.addField(field.getType(), contact.getBusinessFax().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                case '3':
                                    if (otherFax == false && contact.getOtherFax() != null && contact.getOtherFax().trim().length() > 0) {
                                        otherFax = true;
                                        contact.addField(field.getType(), contact.getOtherFax().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                default:
                                    contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 'd':
                            if (company == false && contact.getCompany() != null && contact.getCompany().trim().length() > 0) {
                                company = true;
                                contact.addField(field.getType(), contact.getCompany().trim());
                            } else {
                                contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 't':
                            if (title == false && contact.getJobTitle() != null && contact.getJobTitle().trim().length() > 0) {
                                title = true;
                                contact.addField(field.getType(), contact.getJobTitle().trim());
                            } else {
                                contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                        case 'o':
                            contact.addField(field.getType(), field.getValue().trim());
                            break;
                        case 'a':
                            switch(section.getTag().charAt(0)) {
                                case '1':
                                    if (homeAddress == false && contact.getHomeAddress() != null && contact.getHomeAddress().trim().length() > 0) {
                                        homeAddress = true;
                                        contact.addField(field.getType(), contact.getHomeAddress().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                case '2':
                                    if (businessAddress == false && contact.getBusinessAddress() != null && contact.getBusinessAddress().trim().length() > 0) {
                                        businessAddress = true;
                                        contact.addField(field.getType(), contact.getBusinessAddress().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                case '3':
                                    if (otherAddress == false && contact.getOtherAddress() != null && contact.getOtherAddress().trim().length() > 0) {
                                        otherAddress = true;
                                        contact.addField(field.getType(), contact.getOtherAddress().trim());
                                    } else {
                                        contact.addField(field.getType(), field.getValue().trim());
                                    }
                                    break;
                                default:
                                    contact.addField(field.getType(), field.getValue().trim());
                            }
                            break;
                    }
                }
            }
        }
        for (int i = 0; i < contact.getSections().size(); i++) {
            ContactSection section = contact.getSections().get(i);
            if (section.getTag().equalsIgnoreCase("1")) {
                hasPersonalSection = true;
            } else if (section.getTag().equalsIgnoreCase("2")) {
                hasWorkSection = true;
            } else if (section.getTag().equalsIgnoreCase("3")) {
                hasOtherSection = true;
            }
        }
        if (hasPersonalSection == false) if ((contact.getHomeAddress() != null && contact.getHomeAddress().trim().length() > 0) || (contact.getHomeFax() != null && contact.getHomeFax().trim().length() > 0) || (contact.getHomePhone() != null && contact.getHomePhone().trim().length() > 0) || (contact.getHomePhone2() != null && contact.getHomePhone2().trim().length() > 0) || (contact.getEmail2() != null && contact.getEmail2().trim().length() > 0) || (contact.getMobilePhone() != null && contact.getMobilePhone().trim().length() > 0)) {
            contact.addSection("Personal", "1", contact.getSections().size());
        }
        if (hasWorkSection == false) if ((contact.getBusinessAddress() != null && contact.getBusinessAddress().trim().length() > 0) || (contact.getBusinessFax() != null && contact.getBusinessFax().trim().length() > 0) || (contact.getBusinessPhone() != null && contact.getBusinessPhone().trim().length() > 0) || (contact.getBusinessPhone2() != null && contact.getBusinessPhone2().trim().length() > 0) || (contact.getEmail3() != null && contact.getEmail3().trim().length() > 0) || (contact.getJobTitle() != null && contact.getJobTitle().trim().length() > 0) || (contact.getPager() != null && contact.getPager().trim().length() > 0) || (contact.getCompany() != null && contact.getCompany().trim().length() > 0)) {
            contact.addSection("Work", "2", contact.getSections().size());
        }
        if (hasOtherSection == false) if ((contact.getOtherAddress() != null && contact.getOtherAddress().trim().length() > 0) || (contact.getOtherFax() != null && contact.getOtherFax().trim().length() > 0) || (contact.getOtherPhone() != null && contact.getOtherPhone().trim().length() > 0)) {
            contact.addSection("Other", "3", contact.getSections().size());
        }
        for (int i = 0; i < contact.getSections().size(); i++) {
            ContactSection section = contact.getSections().get(i);
            if (section.getTag().equalsIgnoreCase("1")) {
                if (contact.getHomeFax() != null && contact.getHomeFax().trim().length() > 0 && homeFax == false) {
                    contact.addField("f", contact.getHomeFax().trim(), i);
                }
                if (contact.getHomePhone() != null && contact.getHomePhone().trim().length() > 0 && homePhone == false) {
                    contact.addField("p", contact.getHomePhone().trim(), i);
                }
                if (contact.getHomePhone2() != null && contact.getHomePhone2().trim().length() > 0 && homePhone2 == false) {
                    contact.addField("p", contact.getHomePhone2().trim(), i);
                }
                if (contact.getEmail2() != null && contact.getEmail2().trim().length() > 0 && email2 == false) {
                    contact.addField("e", contact.getEmail2().trim(), i);
                }
                if (contact.getMobilePhone() != null && contact.getMobilePhone().trim().length() > 0 && mobile == false) {
                    contact.addField("m", contact.getMobilePhone().trim(), i);
                }
                if (contact.getHomeAddress() != null && contact.getHomeAddress().trim().length() > 0 && homeAddress == false) {
                    contact.addField("a", contact.getHomeAddress().trim(), i);
                }
            } else if (section.getTag().equalsIgnoreCase("2")) {
                if (contact.getBusinessFax() != null && contact.getBusinessFax().trim().length() > 0 && businessFax == false) {
                    contact.addField("f", contact.getBusinessFax().trim(), i);
                }
                if (contact.getBusinessPhone() != null && contact.getBusinessPhone().trim().length() > 0 && businessPhone == false) {
                    contact.addField("p", contact.getBusinessPhone().trim(), i);
                }
                if (contact.getBusinessPhone2() != null && contact.getBusinessPhone2().trim().length() > 0 && businessPhone2 == false) {
                    contact.addField("p", contact.getBusinessPhone2().trim(), i);
                }
                if (contact.getEmail3() != null && contact.getEmail3().trim().length() > 0 && email3 == false) {
                    contact.addField("e", contact.getEmail3().trim(), i);
                }
                if (contact.getPager() != null && contact.getPager().trim().length() > 0 && pager == false) {
                    contact.addField("b", contact.getPager().trim(), i);
                }
                if (contact.getCompany() != null && contact.getCompany().trim().length() > 0 && company == false) {
                    contact.addField("d", contact.getCompany().trim(), i);
                }
                if (contact.getJobTitle() != null && contact.getJobTitle().trim().length() > 0 && title == false) {
                    contact.addField("t", contact.getJobTitle().trim(), i);
                }
                if (contact.getBusinessAddress() != null && contact.getBusinessAddress().trim().length() > 0 && businessAddress == false) {
                    contact.addField("a", contact.getBusinessAddress().trim(), i);
                }
            } else if (section.getTag().equalsIgnoreCase("3")) {
                if (contact.getOtherFax() != null && contact.getOtherFax().trim().length() > 0 && otherFax == false) {
                    contact.addField("f", contact.getOtherFax().trim(), i);
                }
                if (contact.getOtherPhone() != null && contact.getOtherPhone().trim().length() > 0 && otherPhone == false) {
                    contact.addField("p", contact.getOtherPhone().trim(), i);
                }
                if (contact.getOtherAddress() != null && contact.getOtherAddress().trim().length() > 0 && otherAddress == false) {
                    contact.addField("a", contact.getOtherAddress().trim(), i);
                }
            }
        }
        if (log.isTraceEnabled()) log.trace("Merge completed.");
        return contact;
    }

    /**
     * Parses the fields in GmailContact into the http post structure that Gmail understands.
     * @param newcontact - The new contact to insert
     * @return the array of fields to be inserted, structured in the Gmail format.
     * @throws GmailException
     */
    private NameValuePair[] buildPostDataInsert(GmailContact newcontact) throws GmailException {
        if (log.isTraceEnabled()) log.trace("Creating post data for insert...");
        try {
            ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new NameValuePair("act", "ec"));
            data.add(new NameValuePair("at", getCookie("GMAIL_AT")));
            data.add(new NameValuePair("ct_id", "-1"));
            if (newcontact.getName() != null) data.add(new NameValuePair("ct_nm", new String(newcontact.getName().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ct_nm", null));
            if (newcontact.getEmail() != null) data.add(new NameValuePair("ct_em", new String(newcontact.getEmail().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ct_em", null));
            if (newcontact.getNotes() != null) data.add(new NameValuePair("ctf_n", new String(newcontact.getNotes().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ctf_n", null));
            data.add(new NameValuePair("ct_pd", "1"));
            data.add(new NameValuePair("ct_ph", null));
            int sectionCount = 0;
            int fieldCount = 0;
            if (newcontact.getHomePhone() != null || newcontact.getHomePhone2() != null || newcontact.getHomeFax() != null || newcontact.getHomeAddress() != null || newcontact.getMobilePhone() != null || newcontact.getPager() != null || newcontact.getEmail2() != null) {
                data.add(new NameValuePair("ctsn_0" + sectionCount, "Personal"));
                fieldCount = 0;
                if (newcontact.getEmail2() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_e", new String(newcontact.getEmail2().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getHomePhone() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_p", new String(newcontact.getHomePhone().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getHomePhone2() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_p", new String(newcontact.getHomePhone2().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getHomeFax() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_f", new String(newcontact.getHomeFax().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getMobilePhone() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_m", new String(newcontact.getMobilePhone().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getPager() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_b", new String(newcontact.getPager().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getHomeAddress() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_a", new String(newcontact.getHomeAddress().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                sectionCount++;
            }
            if (newcontact.getBusinessPhone() != null || newcontact.getBusinessPhone2() != null || newcontact.getBusinessFax() != null || newcontact.getBusinessAddress() != null || newcontact.getCompany() != null || newcontact.getJobTitle() != null || newcontact.getEmail3() != null) {
                data.add(new NameValuePair("ctsn_0" + sectionCount, "Work"));
                fieldCount = 0;
                if (newcontact.getEmail3() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_e", new String(newcontact.getEmail3().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getBusinessPhone() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_p", new String(newcontact.getBusinessPhone().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getBusinessPhone2() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_p", new String(newcontact.getBusinessPhone2().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getBusinessFax() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_f", new String(newcontact.getBusinessFax().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getCompany() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_d", new String(newcontact.getCompany().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getJobTitle() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_t", new String(newcontact.getJobTitle().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getBusinessAddress() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_a", new String(newcontact.getBusinessAddress().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                sectionCount++;
            }
            if (newcontact.getOtherPhone() != null || newcontact.getOtherFax() != null || newcontact.getOtherAddress() != null) {
                data.add(new NameValuePair("ctsn_0" + sectionCount, "Other"));
                fieldCount = 0;
                if (newcontact.getOtherPhone() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_p", new String(newcontact.getOtherPhone().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getOtherFax() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_f", new String(newcontact.getOtherFax().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
                if (newcontact.getOtherAddress() != null) {
                    data.add(new NameValuePair("ctsf_0" + sectionCount + "_0" + (fieldCount++) + "_a", new String(newcontact.getOtherAddress().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET)));
                }
            }
            if (log.isTraceEnabled()) log.trace("Post data created successfully...");
            return data.toArray(new NameValuePair[data.size()]);
        } catch (UnsupportedEncodingException e) {
            throw new GmailException("UnsupportedEncodingException in buildPostDataInsert()");
        } catch (Exception e) {
            throw new GmailException("Exception in buildPostDataInsert: " + e.getMessage());
        }
    }

    /**
     * Parses the fields in GmailContact into the http post structure that Gmail understands.
     * @param contact - The contact to update
     * @return the array of fields to be updated, structured in the Gmail format.
     * @throws GmailException
     */
    private NameValuePair[] buildPostDataUpdate(GmailContact contact) throws GmailException {
        if (log.isTraceEnabled()) log.trace("Creating post data for update...");
        log.info("CLIENT_buildPostDataUpdate: " + contact.toString());
        log.info("CLIENT2_buildPostDataUpdate_: " + contact.toString2());
        try {
            ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
            data.add(new NameValuePair("act", "ec"));
            data.add(new NameValuePair("at", getCookie("GMAIL_AT")));
            data.add(new NameValuePair("ct_id", contact.getId()));
            if (contact.getName() != null) data.add(new NameValuePair("ct_nm", new String(contact.getName().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ct_nm", null));
            if (contact.getEmail() != null) data.add(new NameValuePair("ct_em", new String(contact.getEmail().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ct_em", null));
            if (contact.getNotes() != null) data.add(new NameValuePair("ctf_n", new String(contact.getNotes().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ctf_n", null));
            data.add(new NameValuePair("ct_pd", "1"));
            data.add(new NameValuePair("ct_ph", null));
            String sectionN = null;
            String fieldN = null;
            for (int i = 0; i < contact.getSections().size(); i++) {
                ContactSection section = contact.getSections().get(i);
                sectionN = i < 10 ? "0" + i : "" + i;
                data.add(new NameValuePair("ctsn_" + sectionN, section.getName()));
                for (int j = 0; j < section.getFields().size(); j++) {
                    ContactField field = section.getFields().get(j);
                    fieldN = j < 10 ? "0" + j : "" + j;
                    if (field.getValue() != null) data.add(new NameValuePair("ctsf_" + sectionN + "_" + fieldN + "_" + field.getType(), new String(field.getValue().getBytes(GMAIL_INPUT_CHARSET), GMAIL_OUTPUT_CHARSET))); else data.add(new NameValuePair("ctsf_" + sectionN + "_" + fieldN + "_" + field.getType(), null));
                }
            }
            if (log.isTraceEnabled()) log.trace("Post data created successfully...");
            return data.toArray(new NameValuePair[data.size()]);
        } catch (UnsupportedEncodingException e) {
            throw new GmailException("UnsupportedEncodingException in buildPostDataUpdate()");
        }
    }

    /**
     * Access the contacts web page used for insert and update a contact
     * @throws GmailException
     */
    private void accessContactPage() throws GmailException {
        int statusCode = -1;
        String query = properties.getString("contacts_page");
        query = query.replace("[RANDOM_INT]", "" + random.nextInt());
        GetMethod get = new GetMethod(query);
        get.setFollowRedirects(true);
        try {
            statusCode = client.executeMethod(get);
            if (statusCode != 200) throw new GmailException("In Gmail contact details page: Status code expected: 200 -> Status code returned: " + statusCode);
            if (log.isInfoEnabled()) log.info("Access Gmail contact details page successful...");
        } catch (HttpException e) {
            throw new GmailException("HttpException in Gmail contact details page: " + e.getMessage());
        } catch (IOException e) {
            throw new GmailException("IOException in Gmail contact details page: " + e.getMessage());
        } finally {
            get.releaseConnection();
        }
    }

    /**
     * Gets the cookie specified by the parameter name.
     * @param name of the cookie.
     * @return the value of the cookie.
     */
    private String getCookie(String name) {
        Cookie[] logoncookies = client.getState().getCookies();
        if (logoncookies.length != 0) {
            for (int i = 0; i < logoncookies.length; i++) {
                if (logoncookies[i].getName().equalsIgnoreCase(name)) {
                    return logoncookies[i].getValue();
                }
            }
        }
        return null;
    }

    /**
     * Loads the contacts sections names translated in the languages supported by Gmail.
     */
    private void loadLanguages() {
        if (log.isTraceEnabled()) log.trace("Loading languages...");
        personalTable = new Hashtable<String, String>();
        workTable = new Hashtable<String, String>();
        otherTable = new Hashtable<String, String>();
        personalTable = LanguagesLoader.getPersonalTable();
        workTable = LanguagesLoader.getWorkTable();
        otherTable = LanguagesLoader.getOtherTable();
    }
}
