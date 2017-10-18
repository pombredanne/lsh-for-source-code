package com.kongur.star.venus.web.action.systemanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springmodules.validation.valang.ValangValidator;
import com.kongur.star.venus.common.password.PasswordValidator;
import com.kongur.star.venus.domain.courses.UserProfilesDO;
import com.kongur.star.venus.domain.system.SystemUser;
import com.kongur.star.venus.domain.system.query.SystemUserQuery;
import com.kongur.star.venus.enums.EnumGender;
import com.kongur.star.venus.enums.EnumUserStatus;
import com.kongur.star.venus.enums.EnumUserType;
import com.kongur.star.venus.manager.system.SystemUserManager;
import com.kongur.star.venus.web.action.BaseAction;

/**
 * ר�ҹ���
 * 
 * @author zhengwei
 */
@Controller
@RequestMapping("/user/expert/")
public class ExpertManageAction extends BaseAction {

    @Autowired
    private SystemUserManager systemUserManager;

    @Autowired
    private ValangValidator addExpertUserValidator;

    @Autowired
    private ValangValidator addExpertUserExtValidator;

    @Autowired
    private PasswordValidator passwordValidator;

    /**
     * ��ѯ
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "list.htm")
    public String list(@ModelAttribute("query") SystemUserQuery query) {
        query.setUserType(EnumUserType.EXPERT);
        query.setIncludeProfiles(true);
        systemUserManager.query(query);
        return "user/expert/list";
    }

    /**
     * ��ʼ��
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "add.htm", method = RequestMethod.GET)
    public String addUser(@ModelAttribute("systemUser") SystemUser systemUser, @ModelAttribute("userProfile") UserProfilesDO userProfile, Model model) {
        model.addAttribute("genderList", EnumGender.values());
        return "user/expert/add";
    }

    /**
     * ���ѧУ����Ա, �ύ
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "add.htm", method = RequestMethod.POST)
    public String doAddUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, @ModelAttribute("userProfile") UserProfilesDO userProfile, BindingResult userProfileResult, @RequestParam(value = "isEnableUser", required = false) String isEnableUser, Model model) {
        addExpertUserValidator.validate(systemUser, result);
        if (result.hasErrors()) {
            model.addAttribute("genderList", EnumGender.values());
            return "user/expert/add";
        }
        addExpertUserExtValidator.validate(userProfile, userProfileResult);
        if (userProfileResult.hasErrors()) {
            model.addAttribute("genderList", EnumGender.values());
            return "user/expert/add";
        }
        systemUser.setUserProfiles(userProfile);
        if (EnumUserStatus.isEnable(isEnableUser)) {
            systemUser.setUserStatus(EnumUserStatus.ENABLE.getValue());
        }
        systemUser.setPassword(passwordValidator.digest("123456", 1));
        Long userId = systemUserManager.addExpert(systemUser);
        model.addAttribute("url", "/user/expert/list.htm");
        if (userId == null || userId < 1) {
            return "error";
        }
        return "success";
    }

    /**
     * �޸�ר�ҳ�ʼ��
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "edit.htm", method = RequestMethod.GET)
    public String editUser(@RequestParam("id") Long id, @ModelAttribute("systemUser") SystemUser systemUser, @ModelAttribute("userProfile") UserProfilesDO userProfile, Model model) {
        systemUser = systemUserManager.getUserById(id);
        model.addAttribute("systemUser", systemUser);
        model.addAttribute("userProfile", systemUser.getUserProfiles());
        model.addAttribute("genderList", EnumGender.values());
        return "user/expert/edit";
    }

    /**
     * �޸�ѧУ����Ա, �ύ
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "edit.htm", method = RequestMethod.POST)
    public String doEditUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, @ModelAttribute("userProfile") UserProfilesDO userProfile, BindingResult userProfileResult, Model model) {
        addExpertUserValidator.validate(systemUser, result);
        if (result.hasErrors()) {
            model.addAttribute("genderList", EnumGender.values());
            return "user/expert/edit";
        }
        addExpertUserExtValidator.validate(userProfile, userProfileResult);
        if (userProfileResult.hasErrors()) {
            model.addAttribute("genderList", EnumGender.values());
            return "user/expert/edit";
        }
        systemUser.setUserProfiles(userProfile);
        boolean isSuccess = systemUserManager.editExpert(systemUser);
        model.addAttribute("url", "/user/expert/list.htm");
        if (!isSuccess) {
            return "error";
        }
        return "success";
    }

    /**
     * ɾ��
     * 
     * @param id
     * @param model
     * @return
     */
    @RequestMapping(value = "delete.htm", method = RequestMethod.GET)
    public String deleteUser(@RequestParam("id") Long id, Model model) {
        int rows = systemUserManager.delSystemUserById(id);
        model.addAttribute("url", "/user/expert/list.htm");
        if (rows < 1) {
            return "error";
        }
        return "success";
    }
}
