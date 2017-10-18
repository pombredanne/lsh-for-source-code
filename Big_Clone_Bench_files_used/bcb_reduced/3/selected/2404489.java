package com.coltrane.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import com.coltrane.dao.CustomerDAO;
import com.coltrane.dao.CustomerMaterialDAO;
import com.coltrane.dao.RevisionHistoryDAO;
import com.coltrane.domain.ComboBoxOption;
import com.coltrane.domain.Customer;
import com.coltrane.domain.CustomerMaterial;
import com.coltrane.domain.CustomerMaterialSearch;
import com.coltrane.domain.CustomerSearch;
import com.coltrane.domain.RevisionHistory;
import com.coltrane.domain.UserSession;

public class CustomerService {

    private CustomerDAO customerDAO;

    private CustomerMaterialDAO customerMaterialDAO;

    private RevisionHistoryDAO revisionHistoryDAO;

    private UserSession userSession;

    @Autowired
    public void setCustomerDAO(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Autowired
    public void setCustomerMaterialDAO(CustomerMaterialDAO customerMaterialDAO) {
        this.customerMaterialDAO = customerMaterialDAO;
    }

    @Autowired
    public void setRevisionHistoryDAO(RevisionHistoryDAO revisionHistoryDAO) {
        this.revisionHistoryDAO = revisionHistoryDAO;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

    public Customer getCustomerById(long id) {
        return customerDAO.getById(id);
    }

    public void addCustomer(Customer customer) {
        long id = customerDAO.create(customer);
        revisionHistoryDAO.create(new RevisionHistory("Customer", id, userSession.getCode(), "NEW", ""));
    }

    public void updateCustomer(Customer customer) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("Id", customer.getId());
        parameters.addValue("Name", customer.getName());
        parameters.addValue("Contact", customer.getContact());
        parameters.addValue("Phone", customer.getPhone());
        parameters.addValue("Fax", customer.getFax());
        parameters.addValue("Email", customer.getEmail());
        parameters.addValue("Address", customer.getAddress());
        parameters.addValue("PortalAccount", customer.getPortalAccount().trim().toUpperCase());
        parameters.addValue("PortalPassword", digestPassword(customer.getPortalPassword().trim()));
        parameters.addValue("Sales", customer.getSales());
        parameters.addValue("Note", customer.getNote());
        customerDAO.update(parameters);
        revisionHistoryDAO.create(new RevisionHistory("Customer", customer.getId(), userSession.getCode(), "Change", ""));
    }

    public void deleteCustomer(long id) {
        Customer customer = customerDAO.getById(id);
        customerDAO.deleteById(id);
        revisionHistoryDAO.create(new RevisionHistory("Customer", id, userSession.getCode(), "Delete", "Customer: " + customer.getCode() + " has been deleted by " + userSession.getCode()));
    }

    public List<Customer> searchCustomers(CustomerSearch search) {
        String order = userSession.getCustomerSort() + (userSession.isCustomerDesc() ? " DESC" : "");
        return customerDAO.search(processSearchCondition(search), order, userSession.getCurrentCustomerPage() - 1, userSession.getRecordsPerPage());
    }

    public int countSearch(CustomerSearch search) {
        return customerDAO.countSearchResult(processSearchCondition(search));
    }

    public List<ComboBoxOption> getCustomerOptions() {
        return this.customerDAO.getOptions("", "Code");
    }

    public CustomerMaterial getCustomerMaterialById(long id) {
        return customerMaterialDAO.getById(id);
    }

    public void addCustomerMaterial(CustomerMaterial customerMaterial) {
        long id = customerMaterialDAO.create(customerMaterial);
        revisionHistoryDAO.create(new RevisionHistory("CustomerMaterial", id, userSession.getCode(), "NEW", ""));
    }

    public void updateCustomerMaterial(CustomerMaterial customerMaterial) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("Id", customerMaterial.getId());
        parameters.addValue("Customer", customerMaterial.getCustomer());
        parameters.addValue("CustomerCode", customerMaterial.getCustomerCode());
        parameters.addValue("LocalCode", customerMaterial.getLocalCode());
        parameters.addValue("Description", customerMaterial.getDescription());
        customerMaterialDAO.update(parameters);
        revisionHistoryDAO.create(new RevisionHistory("CustomerMaterial", customerMaterial.getId(), userSession.getCode(), "Change", ""));
    }

    public void deleteCustomerMaterial(long id) {
        CustomerMaterial customerMaterial = customerMaterialDAO.getById(id);
        customerMaterialDAO.deleteById(id);
        revisionHistoryDAO.create(new RevisionHistory("CustomerMaterial", id, userSession.getCode(), "Delete", "CustomerMaterial Id: " + customerMaterial.getId() + " has been deleted by " + userSession.getCode()));
    }

    public List<CustomerMaterial> searchCustomerMaterials(CustomerMaterialSearch search) {
        String order = userSession.getCustomerMaterialSort() + (userSession.isCustomerMaterialDesc() ? " DESC" : "");
        return customerMaterialDAO.search(processSearchCondition(search), order, userSession.getCurrentCustomerMaterialPage() - 1, userSession.getRecordsPerPage());
    }

    public int countSearch(CustomerMaterialSearch search) {
        return customerMaterialDAO.countSearchResult(processSearchCondition(search));
    }

    public List<RevisionHistory> getRevisionHistoryForCustomer(long customerId) {
        return revisionHistoryDAO.search("Module = 'Customer' AND ParentMarker = '" + customerId + "'", "EnteredTime DESC", 0, 0);
    }

    public List<RevisionHistory> getRevisionHistoryForDeletedCustomers() {
        return revisionHistoryDAO.search("Module = 'Customer' AND Action = 'Delete'", "EnteredTime DESC", 0, 0);
    }

    public List<RevisionHistory> getRevisionHistoryForCustomerMaterial(long customerMaterialId) {
        return revisionHistoryDAO.search("Module = 'CustomerMaterial' AND ParentMarker = '" + customerMaterialId + "'", "EnteredTime DESC", 0, 0);
    }

    public List<RevisionHistory> getRevisionHistoryForDeletedCustomerMaterials() {
        return revisionHistoryDAO.search("Module = 'CustomerMaterial' AND Action = 'Delete'", "EnteredTime DESC", 0, 0);
    }

    private String processSearchCondition(CustomerSearch search) {
        String condition = "1";
        if (search.getCodeLike() != null && !search.getCodeLike().equals("")) {
            condition += " AND Code LIKE '%" + search.getCodeLike() + "%'";
        }
        if (search.getNameLike() != null && !search.getNameLike().equals("")) {
            condition += " AND Name LIKE '%" + search.getNameLike() + "%'";
        }
        if (search.getContactLike() != null && !search.getContactLike().equals("")) {
            condition += " AND Contact LIKE '%" + search.getContactLike() + "%'";
        }
        if (search.getPhoneLike() != null && !search.getPhoneLike().equals("")) {
            condition += " AND Phone LIKE '%" + search.getPhoneLike() + "%'";
        }
        if (search.getFaxLike() != null && !search.getFaxLike().equals("")) {
            condition += " AND Fax LIKE '%" + search.getFaxLike() + "%'";
        }
        if (search.getEmailLike() != null && !search.getEmailLike().equals("")) {
            condition += " AND Email LIKE '%" + search.getEmailLike() + "%'";
        }
        if (search.getAddressLike() != null && !search.getAddressLike().equals("")) {
            condition += " AND Address LIKE '%" + search.getAddressLike() + "%'";
        }
        if (search.getSalesLike() != null && !search.getSalesLike().equals("")) {
            condition += " AND Sales LIKE '%" + search.getSalesLike() + "%'";
        }
        if (search.getNoteLike() != null && !search.getNoteLike().equals("")) {
            condition += " AND Note LIKE '%" + search.getNoteLike() + "%'";
        }
        return condition;
    }

    private String processSearchCondition(CustomerMaterialSearch search) {
        String condition = "1";
        if (search.getCustomerLike() != null && !search.getCustomerLike().equals("")) {
            condition += " AND Customer LIKE '%" + search.getCustomerLike() + "%'";
        }
        if (search.getCustomerCodeLike() != null && !search.getCustomerCodeLike().equals("")) {
            condition += " AND CustomerCode LIKE '%" + search.getCustomerCodeLike() + "%'";
        }
        if (search.getLocalCodeLike() != null && !search.getLocalCodeLike().equals("")) {
            condition += " AND LocalCode LIKE '%" + search.getLocalCodeLike() + "%'";
        }
        if (search.getDescriptionLike() != null && !search.getDescriptionLike().equals("")) {
            condition += " AND Description LIKE '%" + search.getDescriptionLike() + "%'";
        }
        return condition;
    }

    private String digestPassword(String password) {
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(password.getBytes());
            byte[] messageDigest = algorithm.digest();
            for (byte b : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & b));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }
}
