package com.kongur.star.venus.web.action.system;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.eyeieye.melody.util.StringUtil;
import com.kongur.star.venus.common.password.PasswordValidator;
import com.kongur.star.venus.domain.system.ModifyPasswordDO;
import com.kongur.star.venus.domain.system.SystemAgent;
import com.kongur.star.venus.domain.system.SystemUser;
import com.kongur.star.venus.domain.system.query.SystemUserQuery;
import com.kongur.star.venus.enums.EnumUserType;
import com.kongur.star.venus.manager.system.SystemRoleManager;
import com.kongur.star.venus.manager.system.SystemUserManager;
import com.kongur.star.venus.manager.system.SystemUserRoleManager;
import com.kongur.star.venus.web.action.BaseAction;
import com.kongur.star.venus.web.validator.system.SystemUserValidator;

/**
 * @author gaojf
 * @version $Id: SystemUserAction.java,v 0.1 2012-3-21 ����02:10:59 gaojf Exp $
 */
@Controller
public class SystemUserAction extends BaseAction {

    SystemUserValidator systemUserValidator = new SystemUserValidator();

    @Autowired
    private SystemUserManager systemUserManager;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private SystemUserRoleManager systemUserRoleManager;

    @Autowired
    private SystemRoleManager systemRoleManager;

    @RequestMapping(value = "/system/user/list")
    public String listUser(@ModelAttribute("query") SystemUserQuery query) throws Exception {
        query.setUserType(EnumUserType.ADMIN);
        systemUserManager.query(query);
        return "/system/user/list";
    }

    @RequestMapping(value = "/system/user/add", method = RequestMethod.GET)
    public String addUser(@ModelAttribute("systemUser") SystemUser systemUser) throws Exception {
        return "/system/user/add";
    }

    @RequestMapping(value = "/system/user/add", method = RequestMethod.POST)
    public String saveUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, SystemAgent systemAgent, ModelMap model) throws Exception {
        systemUserValidator.validate(systemUser, result);
        if (result.hasErrors()) {
            return "/system/user/add";
        }
        systemUser.setPassword(passwordValidator.digest(systemUser.getPassword(), 1));
        Long id = systemUserManager.addSystemUser(systemUser);
        if (null == id) {
            model.put("result", "�������Աʧ�ܣ�");
            return "/error";
        }
        return "redirect:/system/user/list.htm";
    }

    @RequestMapping(value = "/system/user/delete", method = RequestMethod.GET)
    public String deleteUser(@RequestParam("id") Long id) {
        systemUserManager.delSystemUserById(id);
        return "redirect:/system/user/list.htm";
    }

    /**
     * ��ʾ�޸Ĺ���Ա��Ϣ
     * 
     * @param id
     * @param model
     * @return
     */
    @RequestMapping(value = "/system/user/edit", method = RequestMethod.GET)
    public String editUser(@RequestParam(value = "id", required = false) Long id, @RequestParam(value = "account", required = false) String account, ModelMap model) throws Exception {
        SystemUser user = null;
        if (id != null) {
            user = systemUserManager.getSystemUserById(id);
        } else if (account != null) {
            user = systemUserManager.getSystemUserByAccount(account);
        } else {
            throw new RuntimeException("can not find parameter id or account!");
        }
        model.put("systemUser", user);
        return "/system/user/edit";
    }

    @RequestMapping(value = "/system/user/edit", method = RequestMethod.POST)
    public String saveEditUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, SystemAgent systemAgent, ModelMap model) throws Exception {
        systemUserValidator.validate(systemUser, result);
        if (result.hasErrors()) {
            return "/system/user/edit";
        }
        int rs = systemUserManager.update(systemUser);
        if (rs <= 0) {
            model.put("result", "�������ʧ��");
            return "/system/user/edit";
        }
        return "redirect:/system/user/list.htm";
    }

    /**
     * ��������
     * 
     * @param id
     * @param systemAgentAccount
     * @return
     */
    @RequestMapping(value = "/system/user/resetpasswd", method = RequestMethod.GET)
    public String resetPassword(@RequestParam("id") Long id, @RequestParam("operator") String systemAgentAccount) {
        systemUserManager.initionPassword(id, passwordValidator.digest("123456", 1), systemAgentAccount);
        return "redirect:/system/user/list.htm";
    }

    /**
     * ��ʾ�û���ɫ
     * 
     * @param id
     * @param model
     * @return
     */
    @RequestMapping(value = "/system/user/role", method = RequestMethod.GET)
    public String userRole(@RequestParam("id") Long userId, ModelMap model) {
        model.put("systemUser", systemUserManager.getSystemUserById(userId));
        model.put("listSystemUserRole", systemUserRoleManager.getUserRoleByUserId(userId));
        model.put("listSystemRole", systemRoleManager.getRoles());
        return "/system/user/role";
    }

    /**
     * �����ɫ����
     * 
     * @param systemUser
     * @param roleId
     * @param systemAgent
     * @param model
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/system/user/role", method = RequestMethod.POST)
    public String saveUserRole(@ModelAttribute("systemUser") SystemUser systemUser, @RequestParam("roleId") String roleId, SystemAgent systemAgent, ModelMap model) throws Exception {
        systemUserManager.saveSystemUserRole(systemUser, convertToListRoleId(roleId), systemAgent.getAccount());
        model.put("url", "/system/user/list.htm");
        return "success";
    }

    /**
     * �Խ�ɫ�ַ����ת��
     * 
     * @param roleId
     * @return
     */
    private static List<Long> convertToListRoleId(String roleId) {
        List<Long> list = new ArrayList<Long>();
        if (null == roleId) {
            return list;
        }
        String[] ids = roleId.split(",");
        for (String each : ids) {
            if (!StringUtils.isEmpty(each)) {
                list.add(Long.valueOf(each));
            }
        }
        return list;
    }

    /**
     * ��ʾ������Ϣҳ��
     * 
     * @param systemUser
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "system/user/self_edit", method = RequestMethod.GET)
    public String modifyPersonalInfo(SystemAgent systemAgent, Model model) throws Exception {
        SystemUser user = systemUserManager.getSystemUserById(systemAgent.getAccountId());
        model.addAttribute("systemUser", user);
        return "/system/user/selfEdit";
    }

    /**
     * ��ʾ������Ϣҳ��
     * 
     * @param systemUser
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "system/user/self_edit", method = RequestMethod.POST)
    public String doModifyPersonalInfo(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, SystemAgent systemAgent, ModelMap model) throws Exception {
        if (StringUtils.isEmpty(systemUser.getRealName())) {
            result.rejectValue("realName", null, null, "����������");
        } else {
            if (systemUser.getRealName().length() > 25) {
                result.rejectValue("realName", null, new Integer[] { 25 }, "�����ܳ���{0}���ַ�");
            }
        }
        if (StringUtils.isNotBlank(systemUser.getEmail())) {
            if (systemUser.getEmail().length() > 50) {
                result.rejectValue("email", null, null, "���ܳ���50���ַ�");
            }
        }
        if (result.hasErrors()) {
            return "/system/user/selfEdit";
        }
        int rs = systemUserManager.update(systemUser);
        if (rs <= 0) {
            model.put("message", "�������ʧ��");
            return "error";
        }
        return "success";
    }

    /**
     * �޸�����
     * 
     * @return
     */
    @RequestMapping(value = "/system/user/modify_password", method = RequestMethod.GET)
    public String modifyPassword(@ModelAttribute("modifyPassword") ModifyPasswordDO modifyPassword) {
        return "/system/user/modify_password";
    }

    /**
     * ִ���޸ĸ������룬���ڵ�ǰ��¼�û�
     * 
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/system/user/modify_password", method = RequestMethod.POST)
    public String doModifyPassword(@ModelAttribute("modifyPassword") ModifyPasswordDO modifyPassword, BindingResult result, SystemAgent systemAgent, ModelMap model) throws Exception {
        if (StringUtils.isEmpty(modifyPassword.getOriPassword())) {
            result.rejectValue("oriPassword", null, null, "������ԭ����");
        }
        if (StringUtils.isEmpty(modifyPassword.getNewPassword())) {
            result.rejectValue("newPassword", null, null, "������������");
        }
        if (StringUtils.isEmpty(modifyPassword.getConfirmPassword())) {
            result.rejectValue("confirmPassword", null, null, "������������ȷ��");
        }
        if (!modifyPassword.getNewPassword().equals(modifyPassword.getConfirmPassword())) {
            result.rejectValue("confirmPassword", null, null, "������ȷ�ϲ���ȷ");
        }
        if (result.hasErrors()) {
            model.put("message", "");
            model.put("error", "");
            return "/system/user/modify_password";
        }
        try {
            int rows = systemUserManager.resetpasswd(systemAgent.getAccountId(), passwordValidator.digest(modifyPassword.getNewPassword(), 1), systemAgent.getAccount());
            if (rows < 1) {
                throw new Exception("modifyPasswdDo error!");
            }
            model.put("message", "�޸ĳɹ���");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "/error";
        }
        return "success";
    }

    /**
     * �����û�
     * 
     * @param id
     * @param model
     * @return
     */
    @RequestMapping(value = "system/user/enable.htm", method = RequestMethod.GET)
    public String enableUser(@RequestParam("id") Long id, @RequestParam(value = "type", required = false) String type, Model model) {
        boolean isSuccess = systemUserManager.enableUser(id);
        if (StringUtil.isNotBlank(type)) {
            Integer intType = Integer.valueOf(type);
            if (EnumUserType.isExpert(intType)) {
                model.addAttribute("url", "/user/expert/list.htm");
            } else if (EnumUserType.isCoursesManager(intType)) {
                model.addAttribute("url", "/user/course/list.htm");
            } else if (EnumUserType.isSchoolManager(intType)) {
                model.addAttribute("url", "/user/school/list.htm");
            }
        } else {
            model.addAttribute("url", "/system/user/list.htm");
        }
        if (!isSuccess) {
            return "error";
        }
        return "success";
    }

    /**
     * ͣ��
     * 
     * @param id
     * @param model
     * @return
     */
    @RequestMapping(value = "system/user/disable.htm", method = RequestMethod.GET)
    public String disableUser(@RequestParam("id") Long id, @RequestParam(value = "type", required = false) String type, Model model) {
        boolean isSuccess = systemUserManager.disableUser(id);
        if (StringUtil.isNotBlank(type)) {
            Integer intType = Integer.valueOf(type);
            if (EnumUserType.isExpert(intType)) {
                model.addAttribute("url", "/user/expert/list.htm");
            } else if (EnumUserType.isCoursesManager(intType)) {
                model.addAttribute("url", "/user/course/list.htm");
            } else if (EnumUserType.isSchoolManager(intType)) {
                model.addAttribute("url", "/user/school/list.htm");
            }
        } else {
            model.addAttribute("url", "/system/user/list.htm");
        }
        if (!isSuccess) {
            return "error";
        }
        return "success";
    }
}
