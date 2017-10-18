package com.funambol.jajah.items.manager;

import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.SIFC;
import com.funambol.common.pim.converter.ContactToSIFC;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.jajah.exceptions.JajahException;
import com.funambol.jajah.items.dao.JajahContactDAO;
import com.funambol.jajah.items.dao.SyncItemInfo;
import com.funambol.jajah.items.model.ContactModel;
import com.funambol.jajah.items.utils.Utils;
import com.funambol.jajah.www.holders.ArrayOfContactHolder;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;

/**
 * Bridge between the SyncSource and the JajahContactDAO
 * @author Marco Gomes
 *
 */
public class JajahContactManager {

    JajahContactDAO dao;

    /**
     * Array of Jajah contacts used to performs sync operations.
     */
    private ArrayList<ContactModel> jajahContacts = null;

    private FunambolLogger log = FunambolLoggerFactory.getLogger("funambol.jajah");

    private String countryCode;

    public JajahContactManager(String jajahUser, String jajahPassword) {
        dao = new JajahContactDAO(jajahUser, jajahPassword);
        getMemberCountryCode();
    }

    public int addContact(ContactModel newContact) throws JajahException {
        int id;
        com.funambol.jajah.www.Contact jajahContact;
        jajahContact = convertContactToJajahContact(newContact);
        id = dao.addContactToAddressBook(jajahContact.getName(), jajahContact.getEmail(), jajahContact.getLandline(), jajahContact.getMobile(), jajahContact.getOffice());
        if (log.isDebugEnabled()) log.debug("added to jajah sever id: " + id);
        return id;
    }

    public void getAllContacts() throws JajahException {
        if (log.isDebugEnabled()) {
            log.debug("Get All Contacts from Jajah");
        }
        ArrayOfContactHolder holder = dao.getAdressBook();
        if (log.isDebugEnabled()) {
            log.debug("Nr of contacts: " + holder.value.length);
        }
        jajahContacts = new ArrayList<ContactModel>();
        ContactModel temp;
        for (int i = 0; i < holder.value.length; i++) {
            temp = convertJajahContactToContact(holder.value[i]);
            jajahContacts.add(temp);
            if (log.isDebugEnabled()) {
                log.debug("Contact -> " + temp.getName());
            }
        }
    }

    /**
     * Get country code from member's numbers from Jajah
     */
    private void getMemberCountryCode() {
        try {
            int i = 0;
            String[] numbers = dao.getMemberPersonalNumbers();
            String number;
            if (!numbers[0].equals("")) i = 0; else if (!numbers[1].equals("")) i = 1; else i = 2;
            for (int j = 0; j < Utils.countryCodes.length; j++) {
                number = String.valueOf(Utils.countryCodes[j]);
                if (numbers[i].startsWith(number)) {
                    this.countryCode = "+" + number;
                    break;
                }
            }
        } catch (JajahException ex) {
            if (log.isDebugEnabled()) log.debug("Error getting member personal numbers from jajah - " + ex.getMessage());
        }
    }

    /**
     * Get a contact from Jajah with the given id
     * @param id of the contact to get
     * @return the contact in the Foundation Contact format
     */
    public ContactModel getContactFromId(String id) throws JajahException {
        for (int i = 0; i < jajahContacts.size(); i++) {
            if (jajahContacts.get(i).getId().equals(id)) {
                return jajahContacts.get(i);
            }
        }
        throw new JajahException("Jajah - getSyncItemFromId error. item id:" + id + " not found.");
    }

    public void updateContact(ContactModel newContact) throws JajahException {
        com.funambol.jajah.www.Contact jajahContact;
        jajahContact = convertContactToJajahContact(newContact);
        dao.updateContactFromAddressBook(jajahContact.getId(), jajahContact.getName(), jajahContact.getEmail(), jajahContact.getLandline(), jajahContact.getMobile(), jajahContact.getOffice());
    }

    public void removeContact(int id) throws JajahException {
        dao.deleteContactFromAddressBook(id);
    }

    public void removeContacts(Integer[] ids) {
        for (int i = 0; i < ids.length; i++) {
            try {
                dao.deleteContactFromAddressBook((ids[i]).intValue());
            } catch (JajahException ex) {
                if (log.isDebugEnabled()) log.debug("Error remove contact from jajah id: " + ids[i] + " - " + ex.getMessage());
            }
        }
    }

    public void removeAllContacts() {
        String[] ids = new String[jajahContacts.size()];
        for (int i = 0; i < jajahContacts.size(); i++) {
            ids[i] = jajahContacts.get(i).getId();
        }
        dao.deleteAllContacts(ids);
        jajahContacts.clear();
    }

    public String[] getAllSyncItemKeys() throws JajahException {
        String[] keys = new String[jajahContacts.size()];
        try {
            for (int i = 0; i < jajahContacts.size(); i++) {
                keys[i] = jajahContacts.get(i).getId();
            }
            return keys;
        } catch (Exception e) {
            throw new JajahException("Jajah - getAllSyncItemKeys error - " + e.getMessage());
        }
    }

    /**
     * Gets the SyncInfo (Id and Md5 Hash) of Jajah Contacts
     * @return array of all Jajah contacts SyncItemInfo
     */
    public SyncItemInfo[] getAllSyncItemInfo() {
        SyncItemInfo[] syncItemInfoArray = new SyncItemInfo[jajahContacts.size()];
        for (int i = 0; i < jajahContacts.size(); i++) {
            syncItemInfoArray[i] = new SyncItemInfo(jajahContacts.get(i).getId(), jajahContacts.get(i).getMd5Hash());
        }
        return syncItemInfoArray;
    }

    /**
     * Convert a JajahContact into a Foundation Contact
     * @param jajahContact
     * @return contact
     */
    private ContactModel convertJajahContactToContact(com.funambol.jajah.www.Contact jajahContact) throws JajahException {
        String temp;
        if (log.isTraceEnabled()) {
            log.trace("Converting Jajah contact to Foundation contact: Name:" + jajahContact.getName() + " Email:" + jajahContact.getEmail());
        }
        try {
            ContactModel contactModel;
            Contact contact = new Contact();
            if (jajahContact.getName() != null && jajahContact.getName().equals("") == false) {
                if (log.isDebugEnabled()) {
                    log.debug("NAME: " + jajahContact.getName());
                }
                contact.getName().getFirstName().setPropertyValue(jajahContact.getName());
            }
            if (jajahContact.getEmail() != null && jajahContact.getEmail().equals("") == false) {
                if (log.isDebugEnabled()) {
                    log.debug("EMAIL1_ADDRESS: " + jajahContact.getEmail());
                }
                Email email1 = new Email();
                email1.setEmailType(SIFC.EMAIL1_ADDRESS);
                email1.setPropertyValue(jajahContact.getEmail());
                contact.getPersonalDetail().addEmail(email1);
            }
            if (jajahContact.getMobile() != null && jajahContact.getMobile().equals("") == false) {
                if (log.isDebugEnabled()) {
                    log.debug("MOBILE_TELEPHONE_NUMBER: " + jajahContact.getMobile());
                }
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.MOBILE_TELEPHONE_NUMBER);
                temp = jajahContact.getMobile().replace("-", "");
                if (!(temp.startsWith("+") || temp.startsWith("00"))) temp = "+".concat(temp);
                phone.setPropertyValue(temp);
                contact.getPersonalDetail().addPhone(phone);
            }
            if (jajahContact.getLandline() != null && jajahContact.getLandline().equals("") == false) {
                if (log.isDebugEnabled()) {
                    log.debug("HOME_TELEPHONE_NUMBER: " + jajahContact.getLandline());
                }
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.HOME_TELEPHONE_NUMBER);
                temp = jajahContact.getLandline().replace("-", "");
                if (!(temp.startsWith("+") || temp.startsWith("00"))) temp = "+".concat(temp);
                phone.setPropertyValue(temp);
                contact.getPersonalDetail().addPhone(phone);
            }
            if (jajahContact.getOffice() != null && jajahContact.getOffice().equals("") == false) {
                if (log.isDebugEnabled()) {
                    log.debug("BUSINESS_TELEPHONE_NUMBER: " + jajahContact.getOffice());
                }
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.BUSINESS_TELEPHONE_NUMBER);
                temp = jajahContact.getOffice().replace("-", "");
                if (!(temp.startsWith("+") || temp.startsWith("00"))) temp = "+".concat(temp);
                phone.setPropertyValue(temp);
                contact.getBusinessDetail().addPhone(phone);
            }
            if (log.isDebugEnabled()) {
                log.debug("CONTACT_ID: " + jajahContact.getId());
            }
            contactModel = new ContactModel(String.valueOf(jajahContact.getId()), contact);
            ContactToSIFC convert = new ContactToSIFC(null, null);
            String sifObject = convert.convert(contactModel);
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(sifObject.getBytes());
            String md5Hash = (new BigInteger(m.digest())).toString();
            contactModel.setMd5Hash(md5Hash);
            return contactModel;
        } catch (Exception e) {
            throw new JajahException("JAJAH  - convertJajahContactToContact error: " + e.getMessage());
        }
    }

    /**
     * Convert a Foundation Contact into a JajahContact
     * @param contact
     * @return jajahContact
     */
    private com.funambol.jajah.www.Contact convertContactToJajahContact(ContactModel contactModel) throws JajahException {
        boolean homePhone = false;
        boolean homeMobile = false;
        boolean businessPhone = false;
        String temp;
        if (log.isTraceEnabled()) {
            log.trace("Converting Foundation contact to Jajah contact: Name:" + contactModel.getName().getFirstName().getPropertyValueAsString());
        }
        try {
            com.funambol.jajah.www.Contact jajahContact = new com.funambol.jajah.www.Contact();
            jajahContact.setId(Integer.parseInt(contactModel.getId()));
            Name name = contactModel.getName();
            if (name != null) {
                if (name.getFirstName() != null && name.getFirstName().getPropertyValueAsString() != null) {
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(name.getFirstName().getPropertyValueAsString()).append(" ");
                    if (name.getMiddleName() != null && name.getMiddleName().getPropertyValueAsString() != null) {
                        buffer.append(name.getMiddleName().getPropertyValueAsString()).append(" ");
                    }
                    if (name.getLastName() != null && name.getLastName().getPropertyValueAsString() != null) {
                        buffer.append(name.getLastName().getPropertyValueAsString()).append(" ");
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("NAME: " + buffer.toString().trim());
                    }
                    jajahContact.setName(buffer.toString().trim());
                }
            }
            if (contactModel.getPersonalDetail() != null) {
                if (contactModel.getPersonalDetail().getEmails() != null && contactModel.getPersonalDetail().getEmails().size() > 0) {
                    if (contactModel.getPersonalDetail().getEmails().get(0) != null) {
                        Email email1 = (Email) contactModel.getPersonalDetail().getEmails().get(0);
                        if (email1.getPropertyValueAsString() != null && email1.getPropertyValueAsString().equals("") == false) {
                            if (log.isDebugEnabled()) {
                                log.debug("EMAIL1: " + email1.getPropertyValueAsString());
                            }
                            jajahContact.setEmail(email1.getPropertyValueAsString());
                        }
                    }
                }
                if (contactModel.getPersonalDetail().getPhones() != null && contactModel.getPersonalDetail().getPhones().size() > 0) {
                    for (int i = 0; i < contactModel.getPersonalDetail().getPhones().size(); i++) {
                        Phone phone = (Phone) contactModel.getPersonalDetail().getPhones().get(i);
                        if (log.isDebugEnabled()) {
                            log.debug("PERSONAL_PHONE: " + phone.getPropertyValueAsString() + " type:" + phone.getPhoneType());
                        }
                        if (phone.getPhoneType().equals(SIFC.HOME_TELEPHONE_NUMBER) && homePhone == false) {
                            temp = phone.getPropertyValueAsString();
                            if (!(temp.startsWith("+") || temp.startsWith("00"))) temp = countryCode.concat(temp);
                            jajahContact.setLandline(temp);
                            homePhone = true;
                        } else if ((phone.getPhoneType().equals(SIFC.MOBILE_TELEPHONE_NUMBER) || phone.getPhoneType().equals(SIFC.MOBILE_HOME_TELEPHONE_NUMBER)) && homeMobile == false) {
                            temp = phone.getPropertyValueAsString();
                            if (!(temp.startsWith("+") || temp.startsWith("00"))) temp = countryCode.concat(temp);
                            jajahContact.setMobile(temp);
                            homeMobile = true;
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("JAJAH - Whoops - Personal Phones UNKNOWN TYPE:" + phone.getPhoneType() + " VALUE:" + phone.getPropertyValueAsString());
                            }
                        }
                    }
                }
            }
            if (contactModel.getBusinessDetail().getPhones() != null && contactModel.getBusinessDetail().getPhones().size() > 0) {
                for (int i = 0; i < contactModel.getBusinessDetail().getPhones().size(); i++) {
                    Phone phone = (Phone) contactModel.getBusinessDetail().getPhones().get(i);
                    if (log.isDebugEnabled()) {
                        log.debug("BUSINESS_PHONE: " + phone.getPropertyValueAsString() + " type:" + phone.getPhoneType());
                    }
                    if (phone.getPhoneType().equals(SIFC.BUSINESS_TELEPHONE_NUMBER) && businessPhone == false) {
                        temp = phone.getPropertyValueAsString();
                        if (!(temp.startsWith("+") || temp.startsWith("00"))) temp = countryCode.concat(temp);
                        jajahContact.setOffice(temp);
                        businessPhone = true;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("JAJAH - Whoops - Business Phones UNKNOWN TYPE:" + phone.getPhoneType() + " VALUE:" + phone.getPropertyValueAsString());
                        }
                    }
                }
            }
            return jajahContact;
        } catch (Exception e) {
            throw new JajahException("JAJAH - convertContactToJahahContact error: " + e.getMessage());
        }
    }

    public String calculateMd5Hash(ContactModel contact) throws JajahException {
        ContactModel temp = convertJajahContactToContact(convertContactToJajahContact(contact));
        return temp.getMd5Hash();
    }
}
