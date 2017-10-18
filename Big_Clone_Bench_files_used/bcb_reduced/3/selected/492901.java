package net.sf.imca.model;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.NodeList;
import java.sql.Connection;
import net.jforum.DBConnection;
import net.jforum.dao.DataAccessDriver;
import net.sf.imca.model.entities.MembershipTypeEntity;
import net.sf.imca.model.entities.PersonEntity;
import net.sf.imca.model.entities.AddressEntity;
import net.sf.imca.model.entities.CommitteeMemberEntity;
import net.sf.imca.model.exceptions.DataCheckingException;
import net.sf.imca.model.exceptions.LogInException;
import net.sf.imca.model.exceptions.RegistrationException;
import net.sf.imca.services.JoinImcaService;
import net.sf.imca.web.backingbeans.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Business Object representing the properties, persistence and functionality of
 * the IMCA domain object.
 * 
 * @author dougculnane
 */
public class PersonBO extends ImcaBO implements ImcaObjectInterface {

    /**
     * Entity for persisting this Business Object.
     */
    private PersonEntity entity;

    public PersonBO() {
    }

    public PersonBO(EntityManager em, long id) {
        this.setEntity(em.find(PersonEntity.class, id));
    }

    public PersonBO(EntityManager em, String email) {
        findEntity(em, email);
    }

    private PersonEntity findPersonByXmlId(EntityManager em, String xmlId) {
        if (!"".equals(xmlId)) {
            Query query = em.createNamedQuery("findPersonByXmlId");
            query.setParameter("xmlId", xmlId);
            List<PersonEntity> list = query.getResultList();
            if (list.size() != 0) {
                return list.get(0);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public PersonBO(EntityManager em, String email, String password) throws LogInException {
        if (email == null || password == null) {
            log.warn("Email or Password value is NULL");
            throw new LogInException("Email or Password value is NULL");
        }
        if (email.trim().length() == 0) {
            log.warn("Email value is empty.");
            throw new LogInException("Email value is empty");
        }
        if (password.length() < 8) {
            log.warn("Password too short.");
            throw new LogInException("Password too short");
        }
        try {
            new InternetAddress(email);
        } catch (AddressException e) {
            log.warn("Invalid email address: " + email, e);
            throw new LogInException("Email Address not valid.");
        }
        Query query = em.createNamedQuery("findPersonByEmail");
        query.setParameter("email", email);
        List<PersonEntity> list = query.getResultList();
        PersonEntity dbPerson = new PersonEntity();
        if (list.size() == 1) {
            dbPerson = list.get(0);
        } else {
            throw new LogInException("Email address not found.");
        }
        try {
            if (dbPerson.getPassword().equals(endcodePassword(password))) {
                setEntity(dbPerson);
            } else if (dbPerson.getResetPassword() != null && dbPerson.getResetPassword().equals(password)) {
                setEntity(dbPerson);
            } else {
                throw new LogInException("Email and password do not match.");
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            throw new LogInException("Error Encoding Password.");
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
            throw new LogInException("Error Encoding Password.");
        }
    }

    @SuppressWarnings("unchecked")
    public PersonBO(EntityManager em, NodeList nodeList) {
        String xmlId = "";
        String name = "";
        String landcode = "";
        String tel = "";
        String mobile = "";
        String url = "";
        String email_username = "";
        String email_domain = "";
        String address_street1 = "";
        String address_street2 = "";
        String address_city = "";
        String address_area = "";
        String address_postcode = "";
        String address_country = "";
        for (int i = 0; i < nodeList.getLength(); i++) {
            if ("id".equals(nodeList.item(i).getNodeName())) {
                xmlId = nodeList.item(i).getChildNodes().item(0).getNodeValue();
            } else if ("name".equals(nodeList.item(i).getNodeName())) {
                name = nodeList.item(i).getChildNodes().item(0).getNodeValue();
            } else if ("landcode".equals(nodeList.item(i).getNodeName())) {
                landcode = PersonBO.convertOldXmlCountryCodesToISO(nodeList.item(i).getChildNodes().item(0).getNodeValue());
            } else if ("tel".equals(nodeList.item(i).getNodeName())) {
                if (nodeList.item(i).getChildNodes().item(0) != null) {
                    tel = nodeList.item(i).getChildNodes().item(0).getNodeValue();
                }
            } else if ("mobile".equals(nodeList.item(i).getNodeName())) {
                mobile = nodeList.item(i).getChildNodes().item(0).getNodeValue();
            } else if ("url".equals(nodeList.item(i).getNodeName())) {
                url = nodeList.item(i).getChildNodes().item(0).getNodeValue();
            } else if ("email".equals(nodeList.item(i).getNodeName())) {
                NodeList emailNodes = nodeList.item(i).getChildNodes();
                for (int j = 0; j < emailNodes.getLength(); j++) {
                    if ("username".equals(emailNodes.item(j).getNodeName())) {
                        email_username = emailNodes.item(j).getChildNodes().item(0).getNodeValue();
                    } else if ("domain".equals(emailNodes.item(j).getNodeName())) {
                        email_domain = emailNodes.item(j).getChildNodes().item(0).getNodeValue();
                    }
                }
            } else if ("address".equals(nodeList.item(i).getNodeName())) {
                NodeList addressNodes = nodeList.item(i).getChildNodes();
                for (int j = 0; j < addressNodes.getLength(); j++) {
                    if ("street1".equals(addressNodes.item(j).getNodeName())) {
                        address_street1 = addressNodes.item(j).getChildNodes().item(0).getNodeValue();
                    } else if ("street2".equals(addressNodes.item(j).getNodeName())) {
                        address_street2 = addressNodes.item(j).getChildNodes().item(0).getNodeValue();
                    } else if ("city".equals(addressNodes.item(j).getNodeName())) {
                        address_city = addressNodes.item(j).getChildNodes().item(0).getNodeValue();
                    } else if ("postcode".equals(addressNodes.item(j).getNodeName())) {
                        address_postcode = addressNodes.item(j).getChildNodes().item(0).getNodeValue();
                    } else if ("area".equals(addressNodes.item(j).getNodeName())) {
                        address_area = addressNodes.item(j).getChildNodes().item(0).getNodeValue();
                    } else if ("country".equals(addressNodes.item(j).getNodeName())) {
                        address_country = PersonBO.convertOldXmlCountryCodesToISO(addressNodes.item(j).getChildNodes().item(0).getNodeValue());
                    }
                }
            }
        }
        setEntity(new PersonEntity());
        if (!"".equals(xmlId)) {
            Query query = em.createNamedQuery("findPersonByXmlId");
            query.setParameter("xmlId", xmlId);
            List<PersonEntity> list = query.getResultList();
            if (list.size() == 0) {
            } else {
                setEntity(list.get(0));
            }
        }
        int nameDivPosition = 0;
        if (name.indexOf(" ") > 0) {
            nameDivPosition = name.indexOf(" ");
        }
        getEntity().setFirstName(name.substring(0, nameDivPosition));
        getEntity().setLastName(name.substring(nameDivPosition).trim());
        getEntity().setXmlId(xmlId);
        getEntity().setMobile(mobile);
        getEntity().setTel(tel);
        getEntity().setUrl(url);
        if (email_username.length() + email_domain.length() > 0) {
            getEntity().setEmail(email_username + "@" + email_domain);
        }
        if (getEntity().getAddress() == null) {
            getEntity().setAddress(new AddressEntity());
        }
        getEntity().getAddress().setStreet1(address_street1);
        getEntity().getAddress().setStreet2(address_street2);
        getEntity().getAddress().setCity(address_city);
        getEntity().getAddress().setArea(address_area);
        getEntity().getAddress().setPostCode(address_postcode);
        if ("".equals(address_country)) {
            getEntity().getAddress().setCountryCode(landcode);
        } else {
            getEntity().getAddress().setCountryCode(address_country);
        }
        em.persist(getEntity());
        em.persist(getEntity().getAddress());
    }

    public PersonBO(PersonEntity personEntity) {
        this.setEntity(personEntity);
    }

    public boolean register(EntityManager em) throws RegistrationException, AddressException {
        if (findEntity(em, getEntity().getEmail())) {
            throw new RegistrationException("Email address already registered.");
        }
        em.persist(getEntity().getAddress());
        em.persist(getEntity());
        return true;
    }

    /**
     * Get the persistence entity.
     *
     * @return The persistence entity for this Business Object.
     */
    public PersonEntity getEntity() {
        return entity;
    }

    /**
     * Set the persistence entity.
     *
     * @param entity The new persistence entity for this Business Object.
     */
    public void setEntity(PersonEntity entity) {
        this.entity = entity;
    }

    public void setPasswordEncodeAndSet(String plainTextPassword) throws UnsupportedEncodingException, NoSuchAlgorithmException, DataCheckingException {
        if (plainTextPassword.length() < 8) {
            throw new DataCheckingException("Password too short.");
        }
        getEntity().setPassword(endcodePassword(plainTextPassword));
    }

    private String endcodePassword(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(password.getBytes("UTF-8"));
        byte raw[] = md.digest();
        Base64 base64 = new Base64();
        String hash = new String(base64.encode(raw));
        return hash;
    }

    /**
     * Send password by mail. TODO: Mailserver config from properties file.
     * 
     * @param em EntityManager for persistence.
     * @param email user supplied email address.
     * @throws MessagingException When problem occurs.
     */
    public void sendPassword(EntityManager em, InternetAddress email) throws MessagingException, DataCheckingException {
        MimeMessage message = createEmailMessage();
        message.setRecipients(MimeMessage.RecipientType.TO, new InternetAddress[] { email });
        message.setSubject(getMessage("PasswordReminderSubject", this.getLocale()), MAIL_ENCODING);
        message.setText(getMessage("YourPasswordIs", this.getLocale()) + " " + resetPassword(em, email), MAIL_ENCODING);
        Transport.send(message);
    }

    public Locale getLocale() {
        if (getEntity() == null) {
            return Locale.ENGLISH;
        }
        return new Locale(getEntity().getLanguageCode(), getEntity().getAddress().getCountryCode());
    }

    /**
     * 
     * @param em
     * @param email
     * @return
     */
    @SuppressWarnings("unchecked")
    private String resetPassword(EntityManager em, InternetAddress email) throws DataCheckingException {
        Query query = em.createNamedQuery("findPersonByEmail");
        query.setParameter("email", email.getAddress());
        List<PersonEntity> list = query.getResultList();
        StringBuffer buf = new StringBuffer();
        if (list.size() == 1) {
            PersonEntity dbPerson = list.get(0);
            Random rnd = new Random();
            while (buf.length() < 30) {
                buf.append(rnd.nextInt(16));
            }
            dbPerson.setResetPassword(buf.toString());
            em.persist(dbPerson);
        } else {
            throw new DataCheckingException("Can not find email in Database");
        }
        return buf.toString();
    }

    /**
     * Human friendly name of person.
     *
     * @return
     */
    public String getName() {
        if (getEntity() == null) {
            return "";
        } else {
            return getEntity().toString();
        }
    }

    public String getXmlId() {
        if (getEntity() == null) {
            return "";
        } else {
            String xmlId = getEntity().getXmlId();
            if (!xmlId.equals("")) {
                return getEntity().getXmlId();
            }
            return getName().trim().toLowerCase().replaceAll(" ", "");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean findEntity(EntityManager em, String email) {
        Query query = em.createNamedQuery("findPersonByEmail");
        query.setParameter("email", email);
        List<PersonEntity> list = query.getResultList();
        if (list.size() >= 1) {
            setEntity(list.get(0));
            return true;
        }
        return false;
    }

    /**
     * Change the email address in the enitity if it is unique.
     *
     * @param email
     */
    public void replaceEmail(EntityManager em, final String email) throws DataCheckingException, SQLException {
        if (!email.equals(this.getEntity().getEmail())) {
            if (findEntity(em, email)) {
                throw new DataCheckingException("Email address already in database.");
            } else {
                if (DataAccessDriver.getInstance() == null) {
                    log.warn("Can not change user email because jFourm not initialiesd");
                } else {
                    log.info("Email to change from: " + this.getEntity().getEmail() + " to " + email);
                    Connection conn = DBConnection.getImplementation().getConnection();
                    conn.createStatement().executeUpdate("UPDATE jforum_users SET user_email=\"" + email + "\" WHERE user_email=\"" + this.getEntity().getEmail() + "\"");
                    this.getEntity().setEmail(email);
                }
            }
        }
    }

    public boolean isOnWorldsCommitte(EntityManager em) {
        AssociationBO[] associations = getCommitteeMemberships(em);
        for (int i = 0; i < associations.length; i++) {
            if (associations[i].getEntity().getIsoCountryCode().equals(AssociationBO.IMCA_WORLD_COUNTRY_CODE)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentMember() {
        JoinImcaService service = new JoinImcaService();
        return service.getActiveMember(Utils.getWebUser().getPerson());
    }

    public boolean getOnCommittee(EntityManager em) {
        if (findCommitteeMemberships(em).size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public AssociationBO[] getCommitteeMemberships(EntityManager em) {
        List<CommitteeMemberEntity> list = findCommitteeMemberships(em);
        AssociationBO[] associations = new AssociationBO[list.size()];
        for (int i = 0; i < associations.length; i++) {
            associations[i] = new AssociationBO();
            associations[i].setEntity(list.get(i).getAssosiation());
        }
        return associations;
    }

    @SuppressWarnings("unchecked")
    private List<CommitteeMemberEntity> findCommitteeMemberships(EntityManager em) {
        Query query = em.createNamedQuery("findCommitteeMemberships");
        query.setParameter("person", this.getEntity());
        return query.getResultList();
    }

    public AssociationBO[] getOficialIMCACommitteeMemberships(EntityManager em) {
        AssociationBO[] associations = getCommitteeMemberships(em);
        Vector<AssociationBO> vec = new Vector<AssociationBO>();
        for (int i = 0; i < associations.length; i++) {
            if (associations[i].getEntity().getIsOfficiallyImca()) {
                vec.add(associations[i]);
            }
        }
        AssociationBO[] imcaAssociations = new AssociationBO[vec.size()];
        for (int i = 0; i < imcaAssociations.length; i++) {
            imcaAssociations[i] = vec.get(i);
        }
        return imcaAssociations;
    }

    public String getDefaultCountryCode() {
        if (this.getEntity() != null) {
            return this.getEntity().getAddress().getCountryCode();
        }
        return "";
    }

    public void sendMembershipRequestMail(EntityManager em, MembershipTypeEntity memType) throws MessagingException {
        MimeMessage message = createEmailMessage();
        InternetAddress toEmail = new InternetAddress(getEntity().getEmail());
        message.setRecipients(MimeMessage.RecipientType.TO, new InternetAddress[] { toEmail });
        AssociationBO association = new AssociationBO(memType.getAssociation());
        InternetAddress[] ccEmail = association.getCommitteeEmails();
        message.setRecipients(MimeMessage.RecipientType.CC, ccEmail);
        message.setSubject(getMessage("mailMembershipRequestSubject", this.getLocale()) + " " + memType.toString(), MAIL_ENCODING);
        message.setText(this.getName() + "\n\n" + getMessage("mailMembershipRequestBody", this.getLocale()) + "\n\n" + buildMembershipTypeEntityTextSummery(memType, true), MAIL_ENCODING);
        Transport.send(message);
    }

    public void sendMembershipConfirmationMail(EntityManager em, MembershipTypeEntity memType) throws MessagingException {
        MimeMessage message = createEmailMessage();
        InternetAddress toEmail = new InternetAddress(getEntity().getEmail());
        message.setRecipients(MimeMessage.RecipientType.TO, new InternetAddress[] { toEmail });
        AssociationBO association = new AssociationBO(memType.getAssociation());
        InternetAddress[] ccEmail = association.getCommitteeEmails();
        message.setRecipients(MimeMessage.RecipientType.CC, ccEmail);
        message.setSubject(getMessage("mailMembershipRequestConfirmationSubject", this.getLocale()) + " " + memType.toString(), MAIL_ENCODING);
        message.setText(this.getName() + "\n\n" + getMessage("mailMembershipRequestConfirmBody", this.getLocale()) + "\n\n" + buildMembershipTypeEntityTextSummery(memType, false), MAIL_ENCODING);
        Transport.send(message);
    }

    private String buildMembershipTypeEntityTextSummery(MembershipTypeEntity memType, boolean withPaymentDetails) {
        if (withPaymentDetails) {
            AssociationBO association = new AssociationBO(memType.getAssociation());
            return memType.getAssociation().toString() + "\n" + ImcaBO.formatDate(memType.getValidFrom()) + " - " + ImcaBO.formatDate(memType.getValidTo()) + "\n" + memType.getCurrentFee().getCurrency() + " " + memType.getCurrentFee().getAmount() + "\n" + association.getTextPaymentExplanation();
        } else {
            return memType.getAssociation().toString() + "\n" + memType.getValidFrom() + " - " + memType.getValidTo() + "\n" + memType.getCurrentFee().getCurrency() + " " + memType.getCurrentFee().getAmount();
        }
    }

    public String getCountryCode() {
        return getEntity().getAddress().getCountryCode();
    }

    public String getCountry() {
        return getCountry(getCountryCode());
    }

    public String getAddressHtml() {
        StringBuffer buf = new StringBuffer();
        if (getEntity().getAddress().getStreet1().length() > 0) {
            buf.append(getEntity().getAddress().getStreet1() + "<br />");
        }
        if (getEntity().getAddress().getStreet2().length() > 0) {
            buf.append(getEntity().getAddress().getStreet2() + "<br />");
        }
        if (getEntity().getAddress().getCity().length() > 0) {
            buf.append(getEntity().getAddress().getCity() + "<br />");
        }
        if (getEntity().getAddress().getPostCode().length() > 0) {
            buf.append(getEntity().getAddress().getPostCode() + "<br />");
        }
        buf.append(getCountry() + "<br />");
        return buf.toString();
    }

    public String getEmailHTML() {
        if (getEntity() == null) {
            return "";
        }
        String email = getEntity().getEmail();
        if (email == null || email.trim().length() == 0) {
            return "";
        }
        if (Utils.getWebUser().getLogedIn()) {
            return "<a href=\"mailto:" + getEntity().getEmail() + "\">" + getEntity().getEmail() + "</a>";
        }
        return getEntity().getEmail().replaceAll("\\@", " AT ");
    }

    public String getSmallPictureUrl() {
        return "/img/riders/" + getXmlId() + ".jpg";
    }

    public String getRssFeedUrl(boolean searchWeb) {
        String rssFeedUrl = null;
        if (entity.getNewsFeedUrl() != null & !entity.getUrl().equals("")) {
            return entity.getNewsFeedUrl();
        } else if (entity.getUrl() == null || entity.getUrl().equals("")) {
            return entity.getNewsFeedUrl();
        } else if (searchWeb) {
            HttpURLConnection con = null;
            InputStream is = null;
            try {
                URL url = new URL(entity.getUrl());
                con = (HttpURLConnection) url.openConnection();
                con.connect();
                is = con.getInputStream();
                InputStreamReader sr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(sr);
                String ln;
                StringBuffer sb = new StringBuffer();
                while ((ln = br.readLine()) != null) {
                    sb.append(ln + "\n");
                }
                rssFeedUrl = extractRssFeedUrl(sb.toString());
            } catch (Exception e) {
                log.error(e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return rssFeedUrl;
    }

    private String extractRssFeedUrl(String html) {
        String url = null;
        try {
            int pos = html.indexOf("application/rss+xml");
            if (pos > 0) {
                pos = html.indexOf("href", pos);
                pos = html.indexOf("\"", pos);
                url = html.substring(pos + 1, html.indexOf("\"", pos + 1));
                if (!url.startsWith("http://")) {
                    url = null;
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        return url;
    }

    /**
     * @see ImcaObjectInterface#toHtmlString()
     */
    public String toHtmlString() {
        return "<a href=\"" + getRelativeUrl() + "\">" + getName() + "</a>";
    }

    /**
     * @see ImcaObjectInterface#getAbsoluteUrl()
     */
    public String getAbsoluteUrl() {
        return IMCA_WEBSITE_URL + "/imca/faces/rider.jsp?id=" + entity.getId();
    }

    public String getHtmlEmail() {
        String email = getEntity().getEmail();
        int atPos = email.indexOf("@");
        if (atPos > 0) {
            return email.substring(0, email.indexOf("@")) + " (at) " + email.substring(email.indexOf("@") + 1);
        } else {
            return "";
        }
    }

    public String getGoogleMapAddress() {
        StringBuffer buf = new StringBuffer();
        if (getEntity().getAddress().getStreet1().length() > 0) {
            buf.append(getEntity().getAddress().getStreet1() + ", ");
        }
        if (getEntity().getAddress().getStreet2().length() > 0) {
            buf.append(getEntity().getAddress().getStreet2() + ", ");
        }
        if (getEntity().getAddress().getCity().length() > 0) {
            buf.append(getEntity().getAddress().getCity() + ", ");
        }
        if (getEntity().getAddress().getPostCode().length() > 0) {
            buf.append(getEntity().getAddress().getPostCode() + ", ");
        }
        buf.append(getCountry());
        return buf.toString();
    }

    public void setXmlId(EntityManager em) {
        if (getEntity().getXmlId() == null || getEntity().getXmlId().equals("")) {
            String xmlId = getXmlId();
            int count = 0;
            while (findPersonByXmlId(em, xmlId) != null) {
                count++;
                xmlId = xmlId + count;
            }
            getEntity().setXmlId(xmlId);
        }
    }

    public String getXML(boolean member) {
        StringBuffer buf = new StringBuffer();
        buf.append("<rider>\n" + "\t<id>" + getXmlId() + "</id>\n" + "\t<name>" + getName() + "</name>\n");
        if (getEntity().getUrl() != null && !getEntity().getUrl().equals("")) {
            buf.append("\t<url>" + getEntity().getUrl().replaceAll("&", "&amp;") + "</url>\n");
        }
        if (getEntity().getNewsFeedUrl() != null && !getEntity().getNewsFeedUrl().equals("")) {
            buf.append("\t<newsFeedUrl>" + getEntity().getNewsFeedUrl().replaceAll("&", "&amp;") + "</newsFeedUrl>\n");
        }
        if (member) {
            if (getEntity().getEmail() != null && !getEntity().getEmail().equals("")) {
                buf.append("\t<email>" + getEntity().getEmail() + "</email>\n");
            }
            if (getEntity().getMobile() != null && !getEntity().getMobile().equals("")) {
                buf.append("\t<mobile>" + getEntity().getMobile() + "</mobile>\n");
            }
            if (getEntity().getTel() != null && !getEntity().getTel().equals("")) {
                buf.append("\t<tel>" + getEntity().getTel() + "</tel>\n");
            }
        }
        buf.append("\t<club>" + getEntity().getClub() + "</club>\n" + "\t<landcode>" + getEntity().getAddress().getCountryCode().toLowerCase() + "</landcode>\n" + "</rider>\n");
        return buf.toString();
    }

    public String getGMarker() {
        if (this.getEntity().getCoordinates() != null && !this.getEntity().getCoordinates().equals("")) {
            String marker = "marker" + this.entity.getId();
            String html = "html" + this.entity.getId();
            String icon = "";
            return "var " + marker + " = new GMarker(new GLatLng(" + entity.getCoordinates() + ")" + icon + ");\n" + "var " + html + " = \"<b>" + getName() + "</b>" + "GEvent.addListener(" + marker + ", 'click', function() {\n" + "  " + marker + ".openInfoWindowHtml(" + html + ");\n" + "});\n" + "map.addOverlay(" + marker + ", true);\n";
        } else {
            return "";
        }
    }

    public void updateCoordinnates() throws IOException {
        if (this.getGoogleMapAddress().trim().length() > 0) {
            String query = getGoogleMapAddress();
            String[] point = doGeoQuery(query);
            if (point.length == 4) {
                if (point[0].equals("200")) {
                    entity.setCoordinates(point[2] + ", " + point[3]);
                } else if (point[0].equals("602") && query.indexOf(",") > 0) {
                    point = doGeoQuery(query.substring(query.indexOf(",") + 1));
                    if (point.length == 4) {
                        if (point[0].equals("200")) {
                            entity.setCoordinates(point[2] + ", " + point[3]);
                        }
                    }
                }
            }
        }
    }

    public String getMobile() {
        String mobile = entity.getMobile();
        if (mobile == null || mobile.trim().length() == 0) {
            return "";
        }
        return mobile;
    }

    public String getTel() {
        String tel = entity.getTel();
        if (tel == null || tel.trim().length() == 0) {
            return "";
        }
        return tel;
    }

    public String getDetailsHTML(boolean showContactData) {
        if (getEntity() == null) {
            return "";
        }
        StringBuffer toHTMLBuf = new StringBuffer();
        if (showContactData || Utils.getWebUser().getPerson().isCurrentMember()) {
            String email = getEmailHTML();
            if (email.length() > 0) {
                toHTMLBuf.append("email: " + email + "<br />\n");
            }
            String mobile = getMobile();
            if (mobile.length() > 0) {
                toHTMLBuf.append("mobile: " + mobile + "<br />\n");
            }
            String tel = getTel();
            if (tel.length() > 0) {
                toHTMLBuf.append("tel: " + tel + "<br />\n");
            }
            toHTMLBuf.append("<br/>\n" + getAddressHtml() + "<br/>\n");
        }
        String url = getEntity().getUrl();
        if (url != null && url.trim().length() > 0) {
            toHTMLBuf.append("website: <a href=\"" + url + "\" target=\"blank\">" + url + "</a><br />");
        }
        String club = getEntity().getClub();
        if (club != null && club.trim().length() > 0) {
            toHTMLBuf.append("club: " + club);
        }
        return toHTMLBuf.toString();
    }

    public String getQRContactHTML() {
        if (getEntity() == null) {
            return "";
        }
        if (Utils.getWebUser().getPerson().isCurrentMember()) {
            try {
                return "<img src=\"http://chart.apis.google.com/chart?cht=qr&chs=230x230&chl=MECARD" + "%3AN%3A" + URLEncoder.encode(getName(), "UTF-8") + "%3BTEL%3A" + URLEncoder.encode(getMobile(), "UTF-8") + "%3BTEL%3A" + URLEncoder.encode(getTel(), "UTF-8") + "%3BEMAIL%3A" + URLEncoder.encode(getEntity().getEmail(), "UTF-8") + "%3BURL%3A" + URLEncoder.encode(getEntity().getUrl(), "UTF-8") + "%3BADR%3A" + URLEncoder.encode(getGoogleMapAddress(), "UTF-8") + "%3B%3B\" width=\"230\" height=\"230\" />";
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
        return "";
    }

    public String getSmallPictureHTML() {
        return "<img alt=\"\" title=\"" + getName() + "\" src=\"" + getSmallPictureUrl() + "\" align=\"right\" width=\"150px\" />\n";
    }
}
