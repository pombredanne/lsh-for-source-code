package base.user;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import base.ICXmlTags;
import base.IXMLLoadable;
import base.IXMLSaveable;

public class User extends Observable implements IXMLSaveable, IXMLLoadable {

    private static final transient Logger logger = Logger.getLogger(User.class.getName());

    private int id;

    private String name = "";

    private String surname = "";

    private base.user.Document document = new base.user.Document();

    ;

    private String imagePath = null;

    private String credential = UserCredential.USER_CREDENTIAL[0];

    private String nickname = "";

    private String password = "";

    private Date birthday = new Date();

    public Date creationDate = new Date();

    private String gender = UserGender.USER_GENDER[0];

    private HashSet<NAddress> nAddress = new HashSet<NAddress>();

    private HashSet<EAddress> eAddress = new HashSet<EAddress>();

    private HashSet<PhoneNumber> phoneNumber = new HashSet<PhoneNumber>();

    private int currentNAddressId = 0;

    private int currentEAddressId = 0;

    private int currentPhoneNumberId = 0;

    /**
	 * @param id
	 *            The user's id.
	 * @param name
	 *            The user's name.
	 * @param surname
	 *            The user's surname.
	 * @param gender
	 *            The user's gender.
	 * @param birthday
	 *            The user's birthday.
	 * @param nickname
	 *            The user's nickname.
	 * @param password
	 *            The user's password.
	 * @param credential
	 *            The user's id.
	 * @param document
	 *            The user's document.
	 */
    protected User(final int id, String name, String surname, String gender, Date birthday, String nickname, String password, String credential, base.user.Document document) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.gender = gender;
        this.birthday = birthday;
        this.nickname = nickname;
        this.password = password;
        this.credential = credential;
        this.document = document;
        this.currentNAddressId = retrieveMaxNAddressId();
        this.currentEAddressId = retrieveMaxEAddressId();
        this.currentPhoneNumberId = retrieveMaxPhoneNumberId();
    }

    protected User(int id) {
        this.id = id;
    }

    /**
	 * @return Returns the imagePath.
	 */
    public String getImagePath() {
        return imagePath;
    }

    /**
	 * @param imagePath
	 *            The imagePath to set.
	 */
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /**
	 * @return Returns the password.
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * @param password
	 *            The password to set.
	 */
    public void setPassword(String password) {
        if (!this.getPassword().equals(password)) {
            this.password = buildUserPassword(password);
        } else this.password = password;
    }

    /**
	 * @param birthday
	 *            The birthday to set.
	 */
    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    /**
	 * @param credential
	 *            The credential to set.
	 */
    public void setCredential(String credential) {
        this.credential = credential;
    }

    /**
	 * @param document
	 *            The document to set.
	 */
    public void setDocument(base.user.Document document) {
        this.document = document;
    }

    /**
	 * @param name
	 *            The name to set.
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * @param nickname
	 *            The nickname to set.
	 */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
	 * @param surname
	 *            The surname to set.
	 */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
	 * @return Returns the document.
	 */
    public base.user.Document getDocument() {
        return document;
    }

    /**
	 * @return Returns the name.
	 */
    public String getName() {
        return name;
    }

    /**
	 * @return Returns the nickname.
	 */
    public String getNickname() {
        return nickname;
    }

    /**
	 * @return Returns the surname.
	 */
    public String getSurname() {
        return surname;
    }

    /**
	 * @return Returns the credential.
	 */
    public String getCredential() {
        return credential;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("USER");
        sb.append("\n");
        sb.append("id: " + id);
        sb.append("\n");
        sb.append("name: " + name);
        sb.append("\n");
        sb.append("surname: " + surname);
        sb.append("\n");
        sb.append("gender: " + gender);
        sb.append("\n");
        sb.append("birthday: " + birthday);
        sb.append("\n");
        sb.append("document: \n" + document);
        sb.append("\n");
        sb.append("nAddress: \n");
        Iterator iterator = nAddress.iterator();
        while (iterator.hasNext()) sb.append(iterator.next() + "\n");
        sb.append("eAddress: \n");
        iterator = eAddress.iterator();
        while (iterator.hasNext()) sb.append(iterator.next() + "\n");
        sb.append("phoneNumber: \n");
        iterator = phoneNumber.iterator();
        while (iterator.hasNext()) sb.append(iterator.next() + "\n");
        sb.append("image: " + imagePath);
        sb.append("\n");
        sb.append("credential: " + credential);
        sb.append("\n");
        sb.append("nickname: " + nickname);
        sb.append("\n");
        sb.append("password: " + password);
        return sb.toString();
    }

    /**
	 * @return Returns the birthday.
	 */
    public Date getBirthday() {
        return birthday;
    }

    /**
	 * @return Returns the id.
	 */
    public int getId() {
        return id;
    }

    /**
	 * @return Returns the gender.
	 */
    public String getGender() {
        return gender;
    }

    /**
	 * @param gender
	 *            The gender to set.
	 */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
	 * @return Returns the creationDate.
	 */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
	 * @param password
	 *            The User's password.
	 * @return Returns an MD5 digest of the user's password.
	 */
    public static String buildUserPassword(String password) {
        String result = "";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes("UTF8"));
            byte[] hash = md.digest();
            for (int i = 0; i < hash.length; i++) {
                int hexValue = hash[i] & 0xFF;
                if (hexValue < 16) {
                    result = result + "0";
                }
                result = result + Integer.toString(hexValue, 16);
            }
            logger.debug("Users'password MD5 Digest: " + result);
        } catch (NoSuchAlgorithmException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }

    /**
	 * @return Returns the eAddress.
	 */
    public EAddress[] getEAddress() {
        if (eAddress == null) {
            eAddress = new HashSet<EAddress>();
        }
        return eAddress.toArray(new EAddress[0]);
    }

    /**
	 * @return Returns the nAddress.
	 */
    public NAddress[] getNAddress() {
        if (nAddress == null) {
            nAddress = new HashSet<NAddress>();
        }
        return nAddress.toArray(new NAddress[0]);
    }

    /**
	 * @return Returns the phoneNumber.
	 */
    public PhoneNumber[] getPhoneNumber() {
        if (phoneNumber == null) {
            phoneNumber = new HashSet<PhoneNumber>();
        }
        return phoneNumber.toArray(new PhoneNumber[0]);
    }

    public void addEAddress(EAddress eAddress) {
        eAddress.setId(getNextEAddressId());
        this.eAddress.add(eAddress);
    }

    public void addNAddress(NAddress nAddress) {
        nAddress.setId(getNextNAddressId());
        this.nAddress.add(nAddress);
    }

    public void addPhoneNumber(PhoneNumber phoneNumber) {
        phoneNumber.setId(getNextPhoneNumberId());
        this.phoneNumber.add(phoneNumber);
    }

    private int getNextNAddressId() {
        return ++currentNAddressId;
    }

    private int getNextEAddressId() {
        return ++currentEAddressId;
    }

    private int getNextPhoneNumberId() {
        return ++currentPhoneNumberId;
    }

    private int retrieveMaxNAddressId() {
        int max = -1;
        NAddress[] address = getNAddress();
        for (int i = 0; i < address.length; i++) if (address[i].getId() > max) max = address[i].getId();
        return max;
    }

    private int retrieveMaxEAddressId() {
        int max = -1;
        EAddress[] address = getEAddress();
        for (int i = 0; i < address.length; i++) if (address[i].getId() > max) max = address[i].getId();
        return max;
    }

    private int retrieveMaxPhoneNumberId() {
        int max = -1;
        PhoneNumber[] phoneNumber = getPhoneNumber();
        for (int i = 0; i < phoneNumber.length; i++) if (phoneNumber[i].getId() > max) max = phoneNumber[i].getId();
        return max;
    }

    public NAddress newNAddress(String city, String nation, String street, String region, String postalCode, String description) {
        return new NAddress(0, city, nation, street, region, postalCode, description);
    }

    public EAddress newEAddress(String eAddress) {
        return new EAddress(0, eAddress);
    }

    public PhoneNumber newPhoneNumber(String areaCode, String exchange, String number, String description) {
        return new PhoneNumber(0, areaCode, exchange, number, description);
    }

    /**
	 * @param addressId
	 *            The id associated to the NAddress
	 * @return The NAddress whose id is addressId, null if such nAddressId is
	 *         not contained in the user's nAddress set.
	 * 
	 */
    public NAddress getNAddressById(int addressId) {
        NAddress[] address = getNAddress();
        for (int i = 0; i < address.length; i++) if (address[i].getId() == addressId) return address[i];
        return null;
    }

    /**
	 * @param addressId
	 *            The id associated to an EAddress.
	 * @return The EAddress whose id is addressId, null if such eAddressId is
	 *         not contained in the user's eAddress set.
	 */
    public EAddress getEAddressById(int addressId) {
        EAddress[] address = getEAddress();
        for (int i = 0; i < address.length; i++) if (address[i].getId() == addressId) return address[i];
        return null;
    }

    /**
	 * @param phoneNumberId
	 *            The id associated to a PhoneNumber.
	 * @return The PhoneNumber whose id is phoneNumberId, null if such
	 *         phoneNumberId is not contained in the user's phoneNumber set.
	 */
    public PhoneNumber getPhoneNumberById(int phoneNumberId) {
        PhoneNumber[] phoneNumber = getPhoneNumber();
        for (int i = 0; i < phoneNumber.length; i++) if (phoneNumber[i].getId() == phoneNumberId) return phoneNumber[i];
        return null;
    }

    /**
	 * @param nAddress
	 *            Deletes the nAddress form the user's nAddress set.
	 */
    public void deleteNAddress(NAddress nAddress) {
        this.nAddress.remove(nAddress);
    }

    /**
	 * @param eAddress
	 *            Deletes the eAddress from the user's eAddress set.
	 */
    public void deleteEAddress(EAddress eAddress) {
        this.eAddress.remove(eAddress);
    }

    /**
	 * @param phoneNumber
	 *            Deletes the phoneNumber from the user's phoneNumber set.
	 */
    public void deletePhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber.remove(phoneNumber);
    }

    public Node toXml(Document document) {
        Element userElement = document.createElement(ICXmlTags.IC_USER_TAG);
        userElement.setAttribute(ICXmlTags.IC_USER_ID_ATTRIBUTE, "" + this.id);
        userElement.setAttribute(ICXmlTags.IC_USER_CURRENT_NADDRESS_ID_ATTRIBUTE, "" + this.currentNAddressId);
        userElement.setAttribute(ICXmlTags.IC_USER_CURRENT_EADDRESS_ID_ATTRIBUTE, "" + this.currentEAddressId);
        userElement.setAttribute(ICXmlTags.IC_USER_CURRENT_PHONE_NUMBER_ID_ATTRIBUTE, "" + this.currentPhoneNumberId);
        Element nameElement = document.createElement(ICXmlTags.IC_USER_NAME_TAG);
        nameElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.name);
        userElement.appendChild(nameElement);
        Element surnameElement = document.createElement(ICXmlTags.IC_USER_SURNAME_TAG);
        surnameElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.surname);
        userElement.appendChild(surnameElement);
        Element genderElement = document.createElement(ICXmlTags.IC_USER_GENDER_TAG);
        genderElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.gender);
        userElement.appendChild(genderElement);
        Element credentialElement = document.createElement(ICXmlTags.IC_USER_CREDENTIAL_TAG);
        credentialElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.credential);
        userElement.appendChild(credentialElement);
        Element nicknameElement = document.createElement(ICXmlTags.IC_USER_NICKNAME_TAG);
        nicknameElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.nickname);
        userElement.appendChild(nicknameElement);
        Element passwordElement = document.createElement(ICXmlTags.IC_USER_PASSWORD_TAG);
        passwordElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.password);
        userElement.appendChild(passwordElement);
        Element imagePathElement = document.createElement(ICXmlTags.IC_USER_IMAGE_PATH_TAG);
        imagePathElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.imagePath);
        userElement.appendChild(imagePathElement);
        Element birthdayElement = document.createElement(ICXmlTags.IC_USER_BIRTHDAY_TAG);
        birthdayElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.birthday.toString());
        birthdayElement.setAttribute(ICXmlTags.IC_LONG_VALUE_ATTRIBUTE, "" + this.birthday.getTime());
        userElement.appendChild(birthdayElement);
        Element creationDateElement = document.createElement(ICXmlTags.IC_USER_CREATION_DATE_TAG);
        creationDateElement.setAttribute(ICXmlTags.IC_VALUE_ATTRIBUTE, this.creationDate.toString());
        creationDateElement.setAttribute(ICXmlTags.IC_LONG_VALUE_ATTRIBUTE, "" + this.creationDate.getTime());
        userElement.appendChild(creationDateElement);
        userElement.appendChild(this.document.toXml(document));
        Element nAddressElement = document.createElement(ICXmlTags.IC_USER_NADDRESS_LIST_TAG);
        Iterator iterator = this.nAddress.iterator();
        while (iterator.hasNext()) nAddressElement.appendChild(((IXMLSaveable) iterator.next()).toXml(document));
        userElement.appendChild(nAddressElement);
        Element eAddressElement = document.createElement(ICXmlTags.IC_USER_EADDRESS_LIST_TAG);
        iterator = this.eAddress.iterator();
        while (iterator.hasNext()) eAddressElement.appendChild(((IXMLSaveable) iterator.next()).toXml(document));
        userElement.appendChild(eAddressElement);
        Element phoneNumberElement = document.createElement(ICXmlTags.IC_USER_PHONE_NUMBER_LIST_TAG);
        iterator = this.phoneNumber.iterator();
        while (iterator.hasNext()) phoneNumberElement.appendChild(((IXMLSaveable) iterator.next()).toXml(document));
        userElement.appendChild(phoneNumberElement);
        return userElement;
    }

    public Object fromXml(Document document) {
        Node userNode = document.getElementsByTagName(ICXmlTags.IC_USER_TAG).item(0);
        int id = Integer.parseInt(userNode.getAttributes().getNamedItem(ICXmlTags.IC_USER_ID_ATTRIBUTE).getNodeValue());
        int currentNAddressId = Integer.parseInt(userNode.getAttributes().getNamedItem(ICXmlTags.IC_USER_CURRENT_NADDRESS_ID_ATTRIBUTE).getNodeValue());
        int currentEAddressId = Integer.parseInt(userNode.getAttributes().getNamedItem(ICXmlTags.IC_USER_CURRENT_EADDRESS_ID_ATTRIBUTE).getNodeValue());
        int currentPhoneNumberId = Integer.parseInt(userNode.getAttributes().getNamedItem(ICXmlTags.IC_USER_CURRENT_PHONE_NUMBER_ID_ATTRIBUTE).getNodeValue());
        String name = "";
        String surname = "";
        String gender = "";
        String credential = "";
        Date birthday = null;
        Date creationDate = null;
        String nickname = "";
        String password = "";
        base.user.Document userDocument = null;
        String imagePath = "";
        NAddress[] nAddress = null;
        EAddress[] eAddress = null;
        PhoneNumber[] phoneNumber = null;
        for (int i = 0; i < userNode.getChildNodes().getLength(); i++) {
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_NAME_TAG)) name = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_SURNAME_TAG)) surname = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_GENDER_TAG)) gender = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_CREDENTIAL_TAG)) credential = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_NICKNAME_TAG)) nickname = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_PASSWORD_TAG)) password = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_IMAGE_PATH_TAG)) imagePath = userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_VALUE_ATTRIBUTE).getNodeValue();
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_BIRTHDAY_TAG)) birthday = new Date(Long.parseLong(userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_LONG_VALUE_ATTRIBUTE).getNodeValue()));
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_CREATION_DATE_TAG)) creationDate = new Date(Long.parseLong(userNode.getChildNodes().item(i).getAttributes().getNamedItem(ICXmlTags.IC_LONG_VALUE_ATTRIBUTE).getNodeValue()));
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_NADDRESS_LIST_TAG)) {
                nAddress = new NAddress[userNode.getChildNodes().item(i).getChildNodes().getLength()];
                for (int k = 0; k < userNode.getChildNodes().item(i).getChildNodes().getLength(); k++) {
                    nAddress[k] = new NAddress().fromXml(userNode.getChildNodes().item(i).getChildNodes().item(i));
                }
            }
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_EADDRESS_LIST_TAG)) {
                eAddress = new EAddress[userNode.getChildNodes().item(i).getChildNodes().getLength()];
                for (int k = 0; k < userNode.getChildNodes().item(i).getChildNodes().getLength(); k++) {
                    eAddress[k] = new EAddress().fromXml(userNode.getChildNodes().item(i).getChildNodes().item(i));
                }
            }
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_USER_PHONE_NUMBER_LIST_TAG)) {
                phoneNumber = new PhoneNumber[userNode.getChildNodes().item(i).getChildNodes().getLength()];
                for (int k = 0; k < userNode.getChildNodes().item(i).getChildNodes().getLength(); k++) {
                    phoneNumber[k] = new PhoneNumber().fromXml(userNode.getChildNodes().item(i).getChildNodes().item(i));
                }
            }
            if (userNode.getChildNodes().item(i).getNodeName().equalsIgnoreCase(ICXmlTags.IC_DOCUMENT_TAG)) {
                userDocument = new base.user.Document().fromXml(userNode.getChildNodes().item(i));
            }
        }
        User result = new User(id);
        result.currentEAddressId = currentEAddressId;
        result.currentNAddressId = currentNAddressId;
        result.currentPhoneNumberId = currentPhoneNumberId;
        result.creationDate = creationDate;
        result.name = name;
        result.surname = surname;
        result.gender = gender;
        result.birthday = birthday;
        result.nickname = nickname;
        result.password = password;
        result.credential = credential;
        result.document = userDocument;
        result.imagePath = imagePath;
        return result;
    }

    /**
	 * @param id
	 *            The id to set.
	 */
    protected void setId(int id) {
        this.id = id;
    }
}
