package org.nodevision.portal.struts.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.nodevision.portal.hibernate.om.NvCustomAttrs;
import org.nodevision.portal.hibernate.om.NvCustomValues;
import org.nodevision.portal.hibernate.om.NvCustomValuesId;
import org.nodevision.portal.hibernate.om.NvRoles;
import org.nodevision.portal.hibernate.om.NvUserRoles;
import org.nodevision.portal.hibernate.om.NvUserRolesId;
import org.nodevision.portal.hibernate.om.NvUsers;
import org.nodevision.portal.struts.user.forms.AddEditUserForm;
import org.nodevision.portal.utils.HibernateUtil;

public class AddEditUser extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ArrayList groups = new ArrayList();
        try {
            Session hbsession = HibernateUtil.currentSession();
            Query query = hbsession.createQuery("select c.RoleName from org.nodevision.portal.hibernate.om.NvRoles as c");
            Vector groupsList = new Vector(query.list());
            request.setAttribute("allgroups", groupsList);
        } catch (Exception e) {
            getServlet().log("Error ", e);
        } finally {
            HibernateUtil.closeSession();
        }
        AddEditUserForm addform = (AddEditUserForm) form;
        if ("true".equalsIgnoreCase(addform.getSave())) {
            ActionErrors errors = new ActionErrors();
            try {
                if ("new".equalsIgnoreCase(request.getParameter("action"))) {
                    errors = addform.validate(mapping, request);
                    if (errors.size() > 0) {
                        saveErrors(request, errors);
                        fillCustomHash(addform.getLogin(), request);
                        return mapping.getInputForward();
                    }
                    createUser(addform, request, mapping);
                }
                saveInfos(addform, request, mapping);
                return mapping.findForward("success");
            } catch (Exception e) {
                errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("users.error", e.getMessage()));
                saveErrors(request, errors);
                ActionForward map = new ActionForward();
                map.setRedirect(false);
                map.setPath("/admin/user/user.do");
                e.printStackTrace();
                return map;
            } finally {
                getUsers(request);
            }
        } else {
            if ("edit".equalsIgnoreCase(request.getParameter("action"))) {
                addform.setLogin(request.getParameter("user"));
                fillForm(addform, request);
            }
        }
        this.fillCustomHash(addform.getLogin(), request);
        return mapping.getInputForward();
    }

    private void saveInfos(AddEditUserForm addform, HttpServletRequest request, ActionMapping mapping) throws Exception {
        updateUser(addform, request);
    }

    private void createUser(AddEditUserForm addform, HttpServletRequest request, ActionMapping mapping) throws Exception {
        MessageDigest md = (MessageDigest) MessageDigest.getInstance("MD5").clone();
        md.update(addform.getPassword().getBytes("UTF-8"));
        byte[] pd = md.digest();
        StringBuffer app = new StringBuffer();
        for (int i = 0; i < pd.length; i++) {
            String s2 = Integer.toHexString(pd[i] & 0xFF);
            app.append((s2.length() == 1) ? "0" + s2 : s2);
        }
        Session hbsession = HibernateUtil.currentSession();
        try {
            Transaction tx = hbsession.beginTransaction();
            NvUsers user = new NvUsers();
            user.setLogin(addform.getLogin());
            user.setPassword(app.toString());
            hbsession.save(user);
            hbsession.flush();
            if (!hbsession.connection().getAutoCommit()) {
                tx.commit();
            }
        } finally {
            HibernateUtil.closeSession();
        }
    }

    private void updateUser(AddEditUserForm addform, HttpServletRequest request) throws Exception {
        Session hbsession = HibernateUtil.currentSession();
        try {
            Transaction tx = hbsession.beginTransaction();
            NvUsers user = (NvUsers) hbsession.load(NvUsers.class, addform.getLogin());
            if (!addform.getPassword().equalsIgnoreCase("")) {
                MessageDigest md = (MessageDigest) MessageDigest.getInstance("MD5").clone();
                md.update(addform.getPassword().getBytes("UTF-8"));
                byte[] pd = md.digest();
                StringBuffer app = new StringBuffer();
                for (int i = 0; i < pd.length; i++) {
                    String s2 = Integer.toHexString(pd[i] & 0xFF);
                    app.append((s2.length() == 1) ? "0" + s2 : s2);
                }
                user.setPassword(app.toString());
            }
            ActionErrors errors = new ActionErrors();
            HashMap cAttrs = addform.getCustomAttrs();
            Query q1 = hbsession.createQuery("from org.nodevision.portal.hibernate.om.NvCustomAttrs as a");
            Iterator attrs = q1.iterate();
            HashMap attrInfos = new HashMap();
            while (attrs.hasNext()) {
                NvCustomAttrs element = (NvCustomAttrs) attrs.next();
                attrInfos.put(element.getAttrName(), element.getAttrType());
                NvCustomValuesId id = new NvCustomValuesId();
                id.setNvUsers(user);
                NvCustomValues value = new NvCustomValues();
                id.setNvCustomAttrs(element);
                value.setId(id);
                if (element.getAttrType().equalsIgnoreCase("String")) {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream serializer = new ObjectOutputStream(bout);
                    serializer.writeObject(cAttrs.get(element.getAttrName()).toString());
                    value.setAttrValue(Hibernate.createBlob(bout.toByteArray()));
                } else if (element.getAttrType().equalsIgnoreCase("Boolean")) {
                    Boolean valueBoolean = Boolean.FALSE;
                    if (cAttrs.get(element.getAttrName()) != null) {
                        valueBoolean = Boolean.TRUE;
                    }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream serializer = new ObjectOutputStream(bout);
                    serializer.writeObject(valueBoolean);
                    value.setAttrValue(Hibernate.createBlob(bout.toByteArray()));
                } else if (element.getAttrType().equalsIgnoreCase("Date")) {
                    Date date = new Date(0);
                    if (!cAttrs.get(element.getAttrName()).toString().equalsIgnoreCase("")) {
                        String bdate = cAttrs.get(element.getAttrName()).toString();
                        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                        date = df.parse(bdate);
                    }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream serializer = new ObjectOutputStream(bout);
                    serializer.writeObject(date);
                    value.setAttrValue(Hibernate.createBlob(bout.toByteArray()));
                }
                hbsession.saveOrUpdate(value);
                hbsession.flush();
            }
            String bdate = addform.getUser_bdate();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            Date parsedDate = df.parse(bdate);
            user.setTimezone(addform.getTimezone());
            user.setLocale(addform.getLocale());
            user.setBdate(new BigDecimal(parsedDate.getTime()));
            user.setGender(addform.getUser_gender());
            user.setEmployer(addform.getEmployer());
            user.setDepartment(addform.getDepartment());
            user.setJobtitle(addform.getJobtitle());
            user.setNamePrefix(addform.getName_prefix());
            user.setNameGiven(addform.getName_given());
            user.setNameFamily(addform.getName_famliy());
            user.setNameMiddle(addform.getName_middle());
            user.setNameSuffix(addform.getName_suffix());
            user.setHomeName(addform.getHome_name());
            user.setHomeStreet(addform.getHome_street());
            user.setHomeStateprov(addform.getHome_stateprov());
            user.setHomePostalcode(addform.getHome_postalcode().equalsIgnoreCase("") ? new Integer(0) : new Integer(addform.getHome_postalcode()));
            user.setHomeOrganization(addform.getHome_organization_name());
            user.setHomeCountry(addform.getHome_country());
            user.setHomeCity(addform.getHome_city());
            user.setHomePhoneIntcode((addform.getHome_phone_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_phone_intcode()));
            user.setHomePhoneLoccode((addform.getHome_phone_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_phone_loccode()));
            user.setHomePhoneNumber((addform.getHome_phone_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_phone_number()));
            user.setHomePhoneExt((addform.getHome_phone_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_phone_ext()));
            user.setHomePhoneComment(addform.getHome_phone_commment());
            user.setHomeFaxIntcode((addform.getHome_fax_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_fax_intcode()));
            user.setHomeFaxLoccode((addform.getHome_fax_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_fax_loccode()));
            user.setHomeFaxNumber((addform.getHome_fax_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_fax_number()));
            user.setHomeFaxExt((addform.getHome_fax_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_fax_ext()));
            user.setHomeFaxComment(addform.getHome_fax_commment());
            user.setHomeMobileIntcode((addform.getHome_mobile_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_mobile_intcode()));
            user.setHomeMobileLoccode((addform.getHome_mobile_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_mobile_loccode()));
            user.setHomeMobileNumber((addform.getHome_mobile_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_mobile_number()));
            user.setHomeMobileExt((addform.getHome_mobile_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_mobile_ext()));
            user.setHomeMobileComment(addform.getHome_mobile_commment());
            user.setHomePagerIntcode((addform.getHome_pager_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_pager_intcode()));
            user.setHomePagerLoccode((addform.getHome_pager_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_pager_loccode()));
            user.setHomePagerNumber((addform.getHome_pager_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_pager_number()));
            user.setHomePagerExt((addform.getHome_pager_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getHome_pager_ext()));
            user.setHomePagerComment(addform.getHome_pager_commment());
            user.setHomeUri(addform.getHome_uri());
            user.setHomeEmail(addform.getHome_email());
            user.setBusinessName(addform.getBusiness_name());
            user.setBusinessStreet(addform.getBusiness_street());
            user.setBusinessStateprov(addform.getBusiness_stateprov());
            user.setBusinessPostalcode((addform.getBusiness_postalcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_postalcode()));
            user.setBusinessOrganization(addform.getBusiness_organization_name());
            user.setBusinessCountry(addform.getBusiness_country());
            user.setBusinessCity(addform.getBusiness_city());
            user.setBusinessPhoneIntcode((addform.getBusiness_phone_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_phone_intcode()));
            user.setBusinessPhoneLoccode((addform.getBusiness_phone_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_phone_loccode()));
            user.setBusinessPhoneNumber((addform.getBusiness_phone_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_phone_number()));
            user.setBusinessPhoneExt((addform.getBusiness_phone_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_phone_ext()));
            user.setBusinessPhoneComment(addform.getBusiness_phone_commment());
            user.setBusinessFaxIntcode((addform.getBusiness_fax_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_fax_intcode()));
            user.setBusinessFaxLoccode((addform.getBusiness_fax_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_fax_loccode()));
            user.setBusinessFaxNumber((addform.getBusiness_fax_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_fax_number()));
            user.setBusinessFaxExt((addform.getBusiness_fax_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_fax_ext()));
            user.setBusinessFaxComment(addform.getBusiness_fax_commment());
            user.setBusinessMobileIntcode((addform.getBusiness_mobile_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_mobile_intcode()));
            user.setBusinessMobileLoccode((addform.getBusiness_mobile_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_mobile_loccode()));
            user.setBusinessMobileNumber((addform.getBusiness_mobile_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_mobile_number()));
            user.setBusinessMobileExt((addform.getBusiness_mobile_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_mobile_ext()));
            user.setBusinessMobileComment(addform.getBusiness_mobile_commment());
            user.setBusinessPagerIntcode((addform.getBusiness_pager_intcode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_pager_intcode()));
            user.setBusinessPagerLoccode((addform.getBusiness_pager_loccode().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_pager_loccode()));
            user.setBusinessPagerNumber((addform.getBusiness_pager_number().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_pager_number()));
            user.setBusinessPagerExt((addform.getBusiness_pager_ext().equalsIgnoreCase("")) ? new Integer(0) : Integer.valueOf(addform.getBusiness_pager_ext()));
            user.setBusinessPagerComment(addform.getBusiness_pager_commment());
            user.setBusinessUri(addform.getBusiness_uri());
            user.setBusinessEmail(addform.getBusiness_email());
            String hqlDelete = "delete org.nodevision.portal.hibernate.om.NvUserRoles where login = :login";
            int deletedEntities = hbsession.createQuery(hqlDelete).setString("login", user.getLogin()).executeUpdate();
            String[] selectedGroups = addform.getSelectedGroups();
            Set newGroups = new HashSet();
            for (int i = 0; i < selectedGroups.length; i++) {
                NvUserRolesId userroles = new NvUserRolesId();
                userroles.setNvUsers(user);
                userroles.setNvRoles((NvRoles) hbsession.load(NvRoles.class, selectedGroups[i]));
                NvUserRoles newRole = new NvUserRoles();
                newRole.setId(userroles);
                newGroups.add(newRole);
            }
            user.setSetOfNvUserRoles(newGroups);
            hbsession.update(user);
            hbsession.flush();
            if (!hbsession.connection().getAutoCommit()) {
                tx.commit();
            }
        } finally {
            HibernateUtil.closeSession();
        }
    }

    private void getUsers(HttpServletRequest request) {
        Session hbsession = HibernateUtil.currentSession();
        try {
            Query query = hbsession.createQuery("from org.nodevision.portal.hibernate.om.NvUsers");
            Vector list = new Vector(query.list());
            request.setAttribute("users", list);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    private void fillForm(AddEditUserForm addform, HttpServletRequest request) throws Exception {
        Session hbsession = HibernateUtil.currentSession();
        try {
            NvUsers user = (NvUsers) hbsession.load(NvUsers.class, addform.getLogin());
            addform.setName_prefix(user.getNamePrefix());
            addform.setName_given(user.getNameGiven());
            addform.setName_famliy(user.getNameFamily());
            addform.setName_middle(user.getNameMiddle());
            addform.setName_suffix(user.getNameSuffix());
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            Date parsedDate = new Date(user.getBdate() == null ? 0 : user.getBdate().longValue());
            addform.setUser_bdate(df.format(parsedDate));
            addform.setUser_gender(user.getGender());
            addform.setEmployer(user.getEmployer());
            addform.setDepartment(user.getDepartment());
            addform.setJobtitle(user.getJobtitle());
            addform.setTimezone(user.getTimezone());
            addform.setLocale(user.getLocale());
            addform.setBusiness_name(user.getBusinessName());
            addform.setBusiness_organization_name(user.getBusinessOrganization());
            addform.setBusiness_street(user.getBusinessStreet());
            addform.setBusiness_postalcode(user.getBusinessPostalcode() == null ? "0" : String.valueOf(user.getBusinessPostalcode()));
            addform.setBusiness_city(user.getBusinessCity());
            addform.setBusiness_stateprov(user.getBusinessStateprov());
            addform.setBusiness_country(user.getBusinessCountry());
            addform.setBusiness_phone_intcode(user.getBusinessPhoneIntcode() == null ? "0" : String.valueOf(user.getBusinessPhoneIntcode()));
            addform.setBusiness_phone_loccode(user.getBusinessPhoneLoccode() == null ? "0" : String.valueOf(user.getBusinessPhoneLoccode()));
            addform.setBusiness_phone_number(user.getBusinessPhoneNumber() == null ? "0" : String.valueOf(user.getBusinessPhoneNumber()));
            addform.setBusiness_phone_ext(user.getBusinessPhoneExt() == null ? "0" : String.valueOf(user.getBusinessPhoneExt()));
            addform.setBusiness_phone_commment(user.getBusinessPhoneComment());
            addform.setBusiness_fax_intcode(user.getBusinessFaxIntcode() == null ? "0" : String.valueOf(user.getBusinessFaxIntcode()));
            addform.setBusiness_fax_loccode(user.getBusinessFaxLoccode() == null ? "0" : String.valueOf(user.getBusinessFaxLoccode()));
            addform.setBusiness_fax_number(user.getBusinessFaxNumber() == null ? "0" : String.valueOf(user.getBusinessFaxNumber()));
            addform.setBusiness_fax_ext(user.getBusinessFaxExt() == null ? "0" : String.valueOf(user.getBusinessFaxExt()));
            addform.setBusiness_fax_commment(user.getBusinessFaxComment());
            addform.setBusiness_mobile_intcode(user.getBusinessMobileIntcode() == null ? "0" : String.valueOf(user.getBusinessMobileIntcode()));
            addform.setBusiness_mobile_loccode(user.getBusinessMobileLoccode() == null ? "0" : String.valueOf(user.getBusinessMobileLoccode()));
            addform.setBusiness_mobile_number(user.getBusinessMobileNumber() == null ? "0" : String.valueOf(user.getBusinessMobileNumber()));
            addform.setBusiness_mobile_ext(user.getBusinessMobileExt() == null ? "0" : String.valueOf(user.getBusinessMobileExt()));
            addform.setBusiness_mobile_commment(user.getBusinessMobileComment());
            addform.setBusiness_pager_intcode(user.getBusinessPagerIntcode() == null ? "0" : String.valueOf(user.getBusinessPagerIntcode()));
            addform.setBusiness_pager_loccode(user.getBusinessPagerLoccode() == null ? "0" : String.valueOf(user.getBusinessPagerLoccode()));
            addform.setBusiness_pager_number(user.getBusinessPagerNumber() == null ? "0" : String.valueOf(user.getBusinessPagerNumber()));
            addform.setBusiness_pager_ext(user.getBusinessPagerExt() == null ? "0" : String.valueOf(user.getBusinessPagerExt()));
            addform.setBusiness_pager_commment(user.getBusinessPagerComment());
            addform.setBusiness_email(user.getBusinessEmail());
            addform.setBusiness_uri(user.getBusinessUri());
            addform.setHome_name(user.getHomeName());
            addform.setHome_organization_name(user.getHomeOrganization());
            addform.setHome_street(user.getHomeStreet());
            addform.setHome_postalcode(user.getHomePostalcode() == null ? "0" : String.valueOf(user.getHomePostalcode()));
            addform.setHome_city(user.getHomeCity());
            addform.setHome_stateprov(user.getHomeStateprov());
            addform.setHome_country(user.getHomeCountry());
            addform.setHome_phone_intcode(user.getHomePhoneIntcode() == null ? "0" : String.valueOf(user.getHomePhoneIntcode()));
            addform.setHome_phone_loccode(user.getHomePhoneLoccode() == null ? "0" : String.valueOf(user.getHomePhoneLoccode()));
            addform.setHome_phone_number(user.getHomePhoneNumber() == null ? "0" : String.valueOf(user.getHomePhoneNumber()));
            addform.setHome_phone_ext(user.getHomePhoneExt() == null ? "0" : String.valueOf(user.getHomePhoneExt()));
            addform.setHome_phone_commment(user.getHomePhoneComment());
            addform.setHome_fax_intcode(user.getHomeFaxIntcode() == null ? "0" : String.valueOf(user.getHomeFaxIntcode()));
            addform.setHome_fax_loccode(user.getHomeFaxLoccode() == null ? "0" : String.valueOf(user.getHomeFaxLoccode()));
            addform.setHome_fax_number(user.getHomeFaxNumber() == null ? "0" : String.valueOf(user.getHomeFaxNumber()));
            addform.setHome_fax_ext(user.getHomeFaxExt() == null ? "0" : String.valueOf(user.getHomeFaxExt()));
            addform.setHome_fax_commment(user.getHomeFaxComment());
            addform.setHome_mobile_intcode(user.getHomeMobileIntcode() == null ? "0" : String.valueOf(user.getHomeMobileIntcode()));
            addform.setHome_mobile_loccode(user.getHomeMobileLoccode() == null ? "0" : String.valueOf(user.getHomeMobileLoccode()));
            addform.setHome_mobile_number(user.getHomeMobileNumber() == null ? "0" : String.valueOf(user.getHomeMobileNumber()));
            addform.setHome_mobile_ext(user.getHomeMobileExt() == null ? "0" : String.valueOf(user.getHomeMobileExt()));
            addform.setHome_mobile_commment(user.getHomeMobileComment());
            addform.setHome_pager_intcode(user.getHomePagerIntcode() == null ? "0" : String.valueOf(user.getHomePagerIntcode()));
            addform.setHome_pager_loccode(user.getHomePagerLoccode() == null ? "0" : String.valueOf(user.getHomePagerLoccode()));
            addform.setHome_pager_number(user.getHomePagerNumber() == null ? "0" : String.valueOf(user.getHomePagerNumber()));
            addform.setHome_pager_ext(user.getHomePagerExt() == null ? "0" : String.valueOf(user.getHomePagerExt()));
            addform.setHome_pager_commment(user.getHomePagerComment());
            addform.setHome_email(user.getHomeEmail());
            addform.setHome_uri(user.getHomeUri());
            addform.setCustomAttrs(fillCustomHash(addform.getLogin(), request));
            Iterator roles = user.getSetOfNvUserRoles().iterator();
            ArrayList groups = new ArrayList();
            String[] h = new String[user.getSetOfNvUserRoles().size()];
            int count = 0;
            while (roles.hasNext()) {
                NvUserRoles element = (NvUserRoles) roles.next();
                h[count] = element.getId().getNvRoles().getRoleName();
                count++;
            }
            addform.setSelectedGroups(h);
        } finally {
            HibernateUtil.closeSession();
        }
    }

    private HashMap fillCustomHash(String login, HttpServletRequest request) throws Exception {
        Session hbsession = HibernateUtil.currentSession();
        Query q1 = hbsession.createQuery("from org.nodevision.portal.hibernate.om.NvCustomAttrs as a");
        Iterator it = q1.iterate();
        ArrayList attrList = new ArrayList();
        HashMap cAttrs = new HashMap();
        HashMap types = new HashMap();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        while (it.hasNext()) {
            NvCustomAttrs element = (NvCustomAttrs) it.next();
            HashMap subtypes = new HashMap();
            subtypes.put("title", element.getAttrTitle());
            subtypes.put("type", element.getAttrType());
            types.put(element.getAttrName(), subtypes);
            Query q2 = hbsession.createQuery("from org.nodevision.portal.hibernate.om.NvCustomValues as c where login= :login and attr_name = :attrname").setString("login", login).setString("attrname", element.getAttrName());
            if (q2.list().size() > 0) {
                NvCustomValues value = (NvCustomValues) q2.list().get(0);
                if (value.getAttrValue().length() > 0) {
                    DataInputStream in = new DataInputStream(value.getAttrValue().getBinaryStream());
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    int c;
                    while ((c = in.read()) != -1) {
                        bout.write(c);
                    }
                    final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                    final ObjectInputStream oin = new ObjectInputStream(bin);
                    Object valueObj = oin.readObject();
                    if (valueObj instanceof Boolean) {
                        Boolean formBool = (Boolean) valueObj;
                        cAttrs.put(element.getAttrName(), (formBool.booleanValue() ? "true" : "false"));
                    } else if (valueObj instanceof String) {
                        cAttrs.put(element.getAttrName(), valueObj.toString());
                    } else if (valueObj instanceof Date) {
                        Date date = (Date) valueObj;
                        cAttrs.put(element.getAttrName(), df.format(date));
                    } else {
                        cAttrs.put(element.getAttrName(), valueObj.toString());
                    }
                } else {
                    cAttrs.put(element.getAttrName(), "");
                }
            } else {
                cAttrs.put(element.getAttrName(), "");
            }
        }
        request.setAttribute("attrs", cAttrs);
        request.setAttribute("types", types);
        return cAttrs;
    }
}
