package com.funambol.google.items.manager;

import java.util.ArrayList;
import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Email;
import com.funambol.common.pim.contact.Name;
import com.funambol.common.pim.contact.Note;
import com.funambol.common.pim.contact.Phone;
import com.funambol.common.pim.contact.SIFC;
import com.funambol.common.pim.contact.Title;
import com.funambol.framework.engine.SyncItemKey;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.google.items.dao.SyncItemInfo;
import com.funambol.google.exception.GmailException;
import com.funambol.google.exception.GmailManagerException;
import com.funambol.google.items.dao.GmailContactDAO;
import com.funambol.google.items.model.GmailContact;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Bridge between the SyncSource and the GmailContactDAO
 * @author Tiago Conde
 * @version $Id: GmailContactManager.java,v 1.0.0 2007/07/30 15:00:00 Tiago Conde Exp $
 *
 */
public class GmailContactManager {

    private FunambolLogger log = FunambolLoggerFactory.getLogger("funambol.google");

    /**
     * Object responsible to interact directly with Gmail Service
     */
    private GmailContactDAO gc = null;

    /**
     * Array of Gmail contacts used to performs sync operations.
     */
    private ArrayList<GmailContact> gmailContacts = null;

    /**
     * GmailContactManager Constructor - Create a GmailContactDAO object
     * @param username
     * @param password
     */
    public GmailContactManager(String username, String password) {
        gc = new GmailContactDAO(username, password);
    }

    /**
     * Gets the SyncInfo (Id and Md5 Hash) of Gmail Contacts
     * @return array of all Gmail contacts SyncItemInfo
     */
    public SyncItemInfo[] getAllSyncItemInfo() {
        SyncItemInfo[] syncItemInfoArray = new SyncItemInfo[gmailContacts.size()];
        for (int i = 0; i < gmailContacts.size(); i++) syncItemInfoArray[i] = new SyncItemInfo(gmailContacts.get(i).getId(), gmailContacts.get(i).getMd5Hash());
        return syncItemInfoArray;
    }

    /**
     * Calls the login operation in the GmailContactDAO object
     * @throws GmailManagerException
     */
    public void login() throws GmailManagerException {
        try {
            gc.login();
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - Begin Sync (Login) error - " + e.getMessage());
        }
    }

    /**
     * Calls the logout operation in the GmailContactDAO object
     * @throws GmailManagerException
     */
    public void logout() throws GmailManagerException {
        try {
            gc.logout();
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - End Sync (Logout) error - " + e.getMessage());
        }
    }

    /**
     * Gets all contacts from Gmail and store it in gmailContacts array
     * @throws GmailManagerException
     */
    public void getAllContactsFromGmail() throws GmailManagerException {
        try {
            gmailContacts = gc.getAllContacts();
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - Begin Sync (getAllContactsFromGmail) error - " + e.getMessage());
        }
    }

    /**
     * Gets the ids of all the Gmail Contacts
     * @return array of SyncItemKey's
     * @throws GmailManagerException
     */
    public String[] getAllSyncItemKeys() throws GmailManagerException {
        String[] keys = new String[gmailContacts.size()];
        try {
            for (int i = 0; i < gmailContacts.size(); i++) {
                keys[i] = gmailContacts.get(i).getId();
            }
            return keys;
        } catch (Exception e) {
            throw new GmailManagerException("Google Gmail - getAllSyncItemKeys error - " + e.getMessage());
        }
    }

    /**
     * Get a contact from Gmail with the given id
     * @param id of the contact to get
     * @return the contact in the Foundation Contact format
     * @throws GmailManagerException
     */
    public Contact getContactFromId(String id) throws GmailManagerException {
        for (int i = 0; i < gmailContacts.size(); i++) {
            if (gmailContacts.get(i).getId().equals(id)) {
                return convertGmailContactToContact(gmailContacts.get(i));
            }
        }
        throw new GmailManagerException("Google Gmail - getSyncItemFromId error. item id:" + id + " not found.");
    }

    /**
     * Performs the insert operation in the GmailContactDAO object
     * @param contact to be inserted
     * @return the inserted contact
     * @throws GmailManagerException
     */
    public Contact addContact(Contact contact) throws GmailManagerException {
        GmailContact gmailContact = convertContactToGmailContact(contact);
        try {
            gmailContact = gc.insertContact(gmailContact);
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - addSyncItem error - " + e.getMessage());
        }
        if (gmailContact != null) return convertGmailContactToContact(gmailContact); else throw new GmailManagerException("Google Gmail - addcontact error ");
    }

    /**
     * Performs the update operation in the GmailContactDAO object
     * @param contact with the updated info
     * @return the updated contact
     * @throws GmailManagerException
     */
    public Contact updateContact(Contact contact) throws GmailManagerException {
        GmailContact gmailContact = convertContactToGmailContact(contact);
        try {
            gmailContact = gc.editContact(gmailContact);
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - updateSyncItem error, item id:" + contact.getUid() + " - " + e.getMessage());
        }
        return convertGmailContactToContact(gmailContact);
    }

    /**
     * Remove a contact from Gmail
     * (Old Method: Not being used. The contacts are removed in one operation)
     * @param id of the contact
     * @throws GmailManagerException
     */
    public void removeContact(String id) throws GmailManagerException {
        try {
            gc.removeContact(id);
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - removeSyncItem (removeContact) error - " + e.getMessage());
        }
        for (int i = 0; i < gmailContacts.size(); i++) if (gmailContacts.get(i).getId().equalsIgnoreCase(id)) gmailContacts.remove(i);
    }

    /**
     * Remove a list of contacts in one operation
     * @param ids array
     * @throws GmailManagerException
     */
    public void removeContacts(String[] ids) throws GmailManagerException {
        try {
            gc.removeContacts(ids);
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - removeSyncItem (removeContacts) error - " + e.getMessage());
        }
        for (int i = 0; i < ids.length; i++) for (int j = 0; j < gmailContacts.size(); j++) if (gmailContacts.get(j).getId().equalsIgnoreCase(ids[i])) gmailContacts.remove(j);
    }

    /**
     * Removes all the contacts from Gmail
     * @throws GmailManagerException
     */
    public void removeAllContacts() throws GmailManagerException {
        String[] ids = new String[gmailContacts.size()];
        for (int i = 0; i < gmailContacts.size(); i++) ids[i] = gmailContacts.get(i).getId();
        try {
            gc.removeContacts(ids);
        } catch (GmailException e) {
            throw new GmailManagerException("Google Gmail - removeSyncItem (removeAllContacts) error - " + e.getMessage());
        }
        gmailContacts.clear();
    }

    /**
     * Gets the keys of the contacts that are twin of the given contact
     * Gmail doesn't alow the same email to be part of two distinct contacts.
     * Therefore a contact is considered a twin if it has the same email of the given contact
     * @param contact to check for twins
     * @return array of SyncItemKey's
     */
    public SyncItemKey[] getKeysFromTwins(Contact contact) {
        SyncItemKey[] keys = new SyncItemKey[1];
        String email1 = null, email2 = null, email3 = null;
        if (log.isTraceEnabled()) log.trace("getKeysFromTwins() for contact with id: " + contact.getUid());
        if (contact.getPersonalDetail().getEmails() != null) {
            if (contact.getPersonalDetail().getEmails().size() == 1 && contact.getPersonalDetail().getEmails().get(0) != null) {
                Email email = (Email) contact.getPersonalDetail().getEmails().get(0);
                if (email.getPropertyValueAsString() != null && email.getPropertyValueAsString().equals("") == false) {
                    email1 = email.getPropertyValueAsString();
                }
            }
            if (contact.getPersonalDetail().getEmails().size() == 2 && contact.getPersonalDetail().getEmails().get(1) != null) {
                Email email = (Email) contact.getPersonalDetail().getEmails().get(1);
                if (email.getPropertyValueAsString() != null && email.getPropertyValueAsString().equals("") == false) {
                    email2 = email.getPropertyValueAsString();
                }
            }
        }
        if (contact.getBusinessDetail().getEmails() != null && contact.getBusinessDetail().getEmails().size() > 0) {
            if (contact.getBusinessDetail().getEmails().get(0) != null) {
                Email email = (Email) contact.getBusinessDetail().getEmails().get(0);
                if (email.getPropertyValueAsString() != null && email.getPropertyValueAsString().equals("") == false) {
                    email3 = email.getPropertyValueAsString();
                }
            }
        }
        ArrayList<SyncItemKey> list = new ArrayList<SyncItemKey>();
        for (int i = 0; i < gmailContacts.size(); i++) {
            if (gmailContacts.get(i).getEmail().equals(email1) || gmailContacts.get(i).getEmail2().equals(email2) || gmailContacts.get(i).getEmail3().equals(email3)) {
                if (log.isTraceEnabled()) log.trace("item with id: " + gmailContacts.get(i).getId() + " is twin of contact with id: " + contact.getUid());
                list.add(new SyncItemKey(gmailContacts.get(i).getId()));
            }
        }
        if (log.isTraceEnabled()) log.trace("twin items found: " + list.size());
        if (list.size() == 0) {
            return new SyncItemKey[0];
        }
        return list.toArray(keys);
    }

    /**
     * Convert a GmailContact into a Foundation Contact
     * @param gmailContact
     * @return contact
     * @throws GmailManagerException
     */
    private Contact convertGmailContactToContact(GmailContact gmailContact) throws GmailManagerException {
        if (log.isTraceEnabled()) log.trace("Converting Gmail contact to Foundation contact: Name:" + gmailContact.getName() + " Email:" + gmailContact.getEmail());
        try {
            Contact contact = new Contact();
            if (gmailContact.getName() != null && gmailContact.getName().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("NAME: " + gmailContact.getName());
                contact.getName().getFirstName().setPropertyValue(gmailContact.getName());
            }
            if (gmailContact.getEmail() != null && gmailContact.getEmail().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("EMAIL1_ADDRESS: " + gmailContact.getEmail());
                Email email1 = new Email();
                email1.setEmailType(SIFC.EMAIL1_ADDRESS);
                email1.setPropertyValue(gmailContact.getEmail());
                contact.getPersonalDetail().addEmail(email1);
            }
            if (gmailContact.getNotes() != null && gmailContact.getNotes().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("BODY: " + gmailContact.getNotes());
                Note note = new Note();
                note.setPropertyValue(gmailContact.getNotes());
                note.setPropertyType(SIFC.BODY);
                contact.addNote(note);
            }
            if (gmailContact.getEmail2() != null && gmailContact.getEmail2().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("EMAIL2_ADDRESS: " + gmailContact.getEmail2());
                Email email2 = new Email();
                email2.setEmailType(SIFC.EMAIL2_ADDRESS);
                email2.setPropertyValue(gmailContact.getEmail2());
                contact.getPersonalDetail().addEmail(email2);
            }
            if (gmailContact.getEmail3() != null && gmailContact.getEmail3().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("EMAIL3_ADDRESS: " + gmailContact.getEmail3());
                Email email3 = new Email();
                email3.setEmailType(SIFC.EMAIL3_ADDRESS);
                email3.setPropertyValue(gmailContact.getEmail3());
                contact.getBusinessDetail().addEmail(email3);
            }
            if (gmailContact.getMobilePhone() != null && gmailContact.getMobilePhone().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("MOBILE_TELEPHONE_NUMBER: " + gmailContact.getMobilePhone());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.MOBILE_TELEPHONE_NUMBER);
                phone.setPropertyValue(gmailContact.getMobilePhone());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getPager() != null && gmailContact.getPager().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("PAGER_NUMBER: " + gmailContact.getPager());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.PAGER_NUMBER);
                phone.setPropertyValue(gmailContact.getPager());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getJobTitle() != null && gmailContact.getJobTitle().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("JOB_TITLE: " + gmailContact.getJobTitle());
                Title title = new Title();
                title.setTitleType(SIFC.JOB_TITLE);
                title.setPropertyValue(gmailContact.getJobTitle());
                contact.getBusinessDetail().addTitle(title);
            }
            if (gmailContact.getCompany() != null && gmailContact.getCompany().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("COMPANY: " + gmailContact.getCompany());
                contact.getBusinessDetail().getCompany().setPropertyValue(gmailContact.getCompany());
            }
            if (gmailContact.getHomePhone() != null && gmailContact.getHomePhone().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("HOME_TELEPHONE_NUMBER: " + gmailContact.getHomePhone());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.HOME_TELEPHONE_NUMBER);
                phone.setPropertyValue(gmailContact.getHomePhone());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getHomePhone2() != null && gmailContact.getHomePhone2().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("HOME2_TELEPHONE_NUMBER: " + gmailContact.getHomePhone2());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.HOME2_TELEPHONE_NUMBER);
                phone.setPropertyValue(gmailContact.getHomePhone2());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getHomeFax() != null && gmailContact.getHomeFax().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("HOME_FAX_NUMBER: " + gmailContact.getHomeFax());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.HOME_FAX_NUMBER);
                phone.setPropertyValue(gmailContact.getHomeFax());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getHomeAddress() != null && gmailContact.getHomeAddress().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("HOME_ADDRESS: " + gmailContact.getHomeAddress());
                contact.getPersonalDetail().getAddress().getStreet().setPropertyValue(gmailContact.getHomeAddress());
            }
            if (gmailContact.getBusinessPhone() != null && gmailContact.getBusinessPhone().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("BUSINESS_TELEPHONE_NUMBER: " + gmailContact.getBusinessPhone());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.BUSINESS_TELEPHONE_NUMBER);
                phone.setPropertyValue(gmailContact.getBusinessPhone());
                contact.getBusinessDetail().addPhone(phone);
            }
            if (gmailContact.getBusinessPhone2() != null && gmailContact.getBusinessPhone2().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("BUSINESS2_TELEPHONE_NUMBER: " + gmailContact.getBusinessPhone2());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.BUSINESS2_TELEPHONE_NUMBER);
                phone.setPropertyValue(gmailContact.getBusinessPhone2());
                contact.getBusinessDetail().addPhone(phone);
            }
            if (gmailContact.getBusinessFax() != null && gmailContact.getBusinessFax().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("BUSINESS_FAX_NUMBER: " + gmailContact.getBusinessFax());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.BUSINESS_FAX_NUMBER);
                phone.setPropertyValue(gmailContact.getBusinessFax());
                contact.getBusinessDetail().addPhone(phone);
            }
            if (gmailContact.getBusinessAddress() != null && gmailContact.getBusinessAddress().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("BUSINESS_ADDRESS: " + gmailContact.getBusinessAddress());
                contact.getBusinessDetail().getAddress().getStreet().setPropertyValue(gmailContact.getBusinessAddress());
            }
            if (gmailContact.getOtherPhone() != null && gmailContact.getOtherPhone().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("OTHER_TELEPHONE_NUMBER: " + gmailContact.getOtherPhone());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.OTHER_TELEPHONE_NUMBER);
                phone.setPropertyValue(gmailContact.getOtherPhone());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getOtherFax() != null && gmailContact.getOtherFax().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("OTHER_FAX_NUMBER: " + gmailContact.getOtherFax());
                Phone phone = new Phone();
                phone.setPhoneType(SIFC.OTHER_FAX_NUMBER);
                phone.setPropertyValue(gmailContact.getOtherFax());
                contact.getPersonalDetail().addPhone(phone);
            }
            if (gmailContact.getOtherAddress() != null && gmailContact.getOtherAddress().equals("") == false) {
                if (log.isDebugEnabled()) log.debug("OTHER_ADDRESS: " + gmailContact.getOtherAddress());
                contact.getPersonalDetail().getOtherAddress().getStreet().setPropertyValue(gmailContact.getOtherAddress());
            }
            if (log.isDebugEnabled()) log.debug("CONTACT_ID: " + gmailContact.getId());
            contact.setUid(gmailContact.getId());
            return contact;
        } catch (Exception e) {
            throw new GmailManagerException("GOOGLE Gmail - convertGmailContactToContact error: " + e.getMessage());
        }
    }

    /**
     * Convert a Foundation Contact into a GmailContact
     * @param contact
     * @return gmailContact
     * @throws GmailManagerException
     */
    private GmailContact convertContactToGmailContact(Contact contact) throws GmailManagerException {
        boolean homePhone = false, homePhone2 = false, homeFax = false, homeMobile = false, homePager = false;
        boolean businessPhone = false, businessPhone2 = false, businessFax = false, businessMobile = false, businessPager = false;
        boolean otherPhone = false, otherFax = false;
        if (log.isTraceEnabled()) log.trace("Converting Foundation contact to Gmail contact: Name:" + contact.getName().getFirstName().getPropertyValueAsString());
        try {
            GmailContact gmailContact = new GmailContact();
            gmailContact.setId(contact.getUid());
            Name name = contact.getName();
            if (name != null) if (name.getFirstName() != null && name.getFirstName().getPropertyValueAsString() != null) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(name.getFirstName().getPropertyValueAsString()).append(" ");
                if (name.getMiddleName() != null && name.getMiddleName().getPropertyValueAsString() != null) buffer.append(name.getMiddleName().getPropertyValueAsString()).append(" ");
                if (name.getLastName() != null && name.getLastName().getPropertyValueAsString() != null) buffer.append(name.getLastName().getPropertyValueAsString()).append(" ");
                if (log.isDebugEnabled()) log.debug("NAME: " + buffer.toString().trim());
                gmailContact.setName(buffer.toString().trim());
            }
            if (contact.getPersonalDetail() != null) {
                if (contact.getPersonalDetail().getEmails() != null && contact.getPersonalDetail().getEmails().size() > 0) {
                    if (contact.getPersonalDetail().getEmails().get(0) != null) {
                        Email email1 = (Email) contact.getPersonalDetail().getEmails().get(0);
                        if (email1.getPropertyValueAsString() != null && email1.getPropertyValueAsString().equals("") == false) {
                            if (log.isDebugEnabled()) log.debug("EMAIL1: " + email1.getPropertyValueAsString());
                            gmailContact.setEmail(email1.getPropertyValueAsString());
                        }
                    }
                    if (contact.getPersonalDetail().getEmails().size() > 1 && contact.getPersonalDetail().getEmails().get(1) != null) {
                        Email email2 = (Email) contact.getPersonalDetail().getEmails().get(1);
                        if (email2.getPropertyValueAsString() != null && email2.getPropertyValueAsString().equals("") == false) {
                            if (log.isDebugEnabled()) log.debug("EMAIL2: " + email2.getPropertyValueAsString());
                            gmailContact.setEmail2(email2.getPropertyValueAsString());
                        }
                    }
                }
                Address address = contact.getPersonalDetail().getAddress();
                if (address != null) if (address.getStreet() != null) if (address.getStreet().getPropertyValueAsString() != null) {
                    StringBuffer addressBuffer = new StringBuffer();
                    addressBuffer.append(address.getStreet().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getPostalCode().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getCity().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getState().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getCountry().getPropertyValueAsString());
                    if (log.isDebugEnabled()) log.debug("HOME_ADDRESS: " + addressBuffer.toString());
                    gmailContact.setHomeAddress(addressBuffer.toString());
                }
                Address addressOther = contact.getPersonalDetail().getOtherAddress();
                if (addressOther != null) if (addressOther.getStreet() != null) if (addressOther.getStreet().getPropertyValueAsString() != null) {
                    StringBuffer addressBuffer = new StringBuffer();
                    addressBuffer.append(addressOther.getStreet().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(addressOther.getPostalCode().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(addressOther.getCity().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(addressOther.getState().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(addressOther.getCountry().getPropertyValueAsString());
                    if (log.isDebugEnabled()) log.debug("OTHER_ADDRESS: " + addressBuffer.toString());
                    gmailContact.setOtherAddress(addressBuffer.toString());
                }
                if (contact.getPersonalDetail().getPhones() != null && contact.getPersonalDetail().getPhones().size() > 0) {
                    for (int i = 0; i < contact.getPersonalDetail().getPhones().size(); i++) {
                        Phone phone = (Phone) contact.getPersonalDetail().getPhones().get(i);
                        if (log.isDebugEnabled()) log.debug("PERSONAL_PHONE: " + phone.getPropertyValueAsString() + " type:" + phone.getPhoneType());
                        if (phone.getPhoneType().equals(SIFC.HOME_TELEPHONE_NUMBER) && homePhone == false) {
                            gmailContact.setHomePhone(phone.getPropertyValueAsString());
                            homePhone = true;
                        } else if (phone.getPhoneType().equals(SIFC.HOME2_TELEPHONE_NUMBER) && homePhone2 == false) {
                            gmailContact.setHomePhone2(phone.getPropertyValueAsString());
                            homePhone2 = true;
                        } else if (phone.getPhoneType().equals(SIFC.HOME_FAX_NUMBER) && homeFax == false) {
                            gmailContact.setHomeFax(phone.getPropertyValueAsString());
                            homeFax = true;
                        } else if ((phone.getPhoneType().equals(SIFC.MOBILE_TELEPHONE_NUMBER) || phone.getPhoneType().equals(SIFC.MOBILE_HOME_TELEPHONE_NUMBER)) && homeMobile == false) {
                            gmailContact.setMobilePhone(phone.getPropertyValueAsString());
                            homeMobile = true;
                        } else if (phone.getPhoneType().equals(SIFC.PAGER_NUMBER) && homePager == false) {
                            gmailContact.setPager(phone.getPropertyValueAsString());
                            homePager = true;
                        } else if (phone.getPhoneType().equals(SIFC.OTHER_TELEPHONE_NUMBER) && otherPhone == false) {
                            gmailContact.setOtherPhone(phone.getPropertyValueAsString());
                            otherPhone = true;
                        } else if (phone.getPhoneType().equals(SIFC.OTHER_FAX_NUMBER) && otherFax == false) {
                            gmailContact.setOtherFax(phone.getPropertyValueAsString());
                            otherFax = true;
                        } else {
                            if (log.isDebugEnabled()) log.debug("GOOGLE - Whoops - Personal Phones UNKNOWN TYPE:" + phone.getPhoneType() + " VALUE:" + phone.getPropertyValueAsString());
                        }
                    }
                }
            }
            if (contact.getBusinessDetail() != null) {
                if (contact.getBusinessDetail().getEmails() != null && contact.getBusinessDetail().getEmails().size() > 0) {
                    if (contact.getBusinessDetail().getEmails().get(0) != null) {
                        Email email3 = (Email) contact.getBusinessDetail().getEmails().get(0);
                        if (email3.getPropertyValueAsString() != null && email3.getPropertyValueAsString().equals("") == false) {
                            if (log.isDebugEnabled()) log.debug("EMAIL3: " + email3.getPropertyValueAsString());
                            gmailContact.setEmail3(email3.getPropertyValueAsString());
                        }
                    }
                }
                Address address = contact.getBusinessDetail().getAddress();
                if (address != null) if (address.getStreet() != null) if (address.getStreet().getPropertyValueAsString() != null) {
                    StringBuffer addressBuffer = new StringBuffer();
                    addressBuffer.append(address.getStreet().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getPostalCode().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getCity().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getState().getPropertyValueAsString()).append(" ");
                    addressBuffer.append(address.getCountry().getPropertyValueAsString());
                    if (log.isDebugEnabled()) log.debug("BUSINESS_ADDRESS: " + addressBuffer.toString());
                    gmailContact.setBusinessAddress(addressBuffer.toString());
                }
                if (contact.getBusinessDetail().getPhones() != null && contact.getBusinessDetail().getPhones().size() > 0) {
                    for (int i = 0; i < contact.getBusinessDetail().getPhones().size(); i++) {
                        Phone phone = (Phone) contact.getBusinessDetail().getPhones().get(i);
                        if (log.isDebugEnabled()) log.debug("BUSINESS_PHONE: " + phone.getPropertyValueAsString() + " type:" + phone.getPhoneType());
                        if (phone.getPhoneType().equals(SIFC.BUSINESS_TELEPHONE_NUMBER) && businessPhone == false) {
                            gmailContact.setBusinessPhone(phone.getPropertyValueAsString());
                            businessPhone = true;
                        } else if (phone.getPhoneType().equals(SIFC.BUSINESS2_TELEPHONE_NUMBER) && businessPhone2 == false) {
                            gmailContact.setBusinessPhone2(phone.getPropertyValueAsString());
                            businessPhone2 = true;
                        } else if (phone.getPhoneType().equals(SIFC.BUSINESS_FAX_NUMBER) && businessFax == false) {
                            gmailContact.setBusinessFax(phone.getPropertyValueAsString());
                            businessFax = true;
                        } else if (phone.getPhoneType().equals(SIFC.MOBILE_BUSINESS_TELEPHONE_NUMBER) && homeMobile == false && businessMobile == false) {
                            gmailContact.setMobilePhone(phone.getPropertyValueAsString());
                            businessMobile = true;
                        } else if (phone.getPhoneType().equals(SIFC.PAGER_NUMBER) && homePager == false && businessPager == false) {
                            gmailContact.setPager(phone.getPropertyValueAsString());
                            businessPager = true;
                        } else {
                            if (log.isDebugEnabled()) log.debug("GOOGLE - Whoops - Business Phones UNKNOWN TYPE:" + phone.getPhoneType() + " VALUE:" + phone.getPropertyValueAsString());
                        }
                    }
                }
                if (contact.getBusinessDetail().getCompany() != null) if (contact.getBusinessDetail().getCompany().getPropertyValueAsString() != null) {
                    if (log.isDebugEnabled()) log.debug("COMPANY: " + contact.getBusinessDetail().getCompany().getPropertyValueAsString());
                    gmailContact.setCompany(contact.getBusinessDetail().getCompany().getPropertyValueAsString());
                }
                if (contact.getBusinessDetail().getTitles() != null && contact.getBusinessDetail().getTitles().size() > 0) {
                    if (contact.getBusinessDetail().getTitles().get(0) != null) {
                        Title title = (Title) contact.getBusinessDetail().getTitles().get(0);
                        if (log.isDebugEnabled()) log.debug("TITLE: " + title.getPropertyValueAsString());
                        gmailContact.setJobTitle(title.getPropertyValueAsString());
                    }
                }
            }
            if (contact.getNotes() != null && contact.getNotes().size() > 0) {
                if (contact.getNotes().get(0) != null) {
                    Note notes = (Note) contact.getNotes().get(0);
                    if (notes.getPropertyValueAsString() != null && notes.getPropertyValueAsString().equals("") == false) {
                        if (log.isDebugEnabled()) log.debug("NOTES: " + notes.getPropertyValueAsString());
                        gmailContact.setNotes(notes.getPropertyValueAsString());
                    }
                }
            }
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(contact.toString().getBytes());
            gmailContact.setMd5Hash(new BigInteger(m.digest()).toString());
            return gmailContact;
        } catch (Exception e) {
            throw new GmailManagerException("GOOGLE Gmail - convertContactToGmailContact error: " + e.getMessage());
        }
    }

    public String calculateMd5Hash(Contact contact) throws GmailManagerException {
        return convertContactToGmailContact(contact).getMd5Hash();
    }
}
