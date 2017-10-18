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
import com.kongur.star.venus.domain.courses.SchoolsDO;
import com.kongur.star.venus.domain.system.SystemUser;
import com.kongur.star.venus.domain.system.query.SystemUserQuery;
import com.kongur.star.venus.enums.EnumUserStatus;
import com.kongur.star.venus.enums.EnumUserType;
import com.kongur.star.venus.manager.courses.SchoolsManager;
import com.kongur.star.venus.manager.courses.UserProfilesManager;
import com.kongur.star.venus.manager.system.SystemUserManager;
import com.kongur.star.venus.web.action.BaseAction;

/**
 * ѧУ�û�����
 */
@Controller
@RequestMapping("user/school/")
public class SchoolUserManageAction extends BaseAction {

    @Autowired
    private SystemUserManager systemUserManager;

    @Autowired
    private SchoolsManager schoolsManager;

    @Autowired
    private ValangValidator addSchoolUserValidator;

    @Autowired
    private ValangValidator editSchoolUserValidator;

    @Autowired
    private UserProfilesManager userProfilesManager;

    @Autowired
    private PasswordValidator passwordValidator;

    /**
     * ��ѯѧУ����Ա
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "list.htm")
    public String list(@ModelAttribute("query") SystemUserQuery query) {
        query.setUserType(EnumUserType.SCHOOL_MANAGER);
        systemUserManager.query(query);
        return "user/school/list";
    }

    /**
     * ��ʼ�����ѧУ����Ա
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "add.htm", method = RequestMethod.GET)
    public String addUser(@ModelAttribute("systemUser") SystemUser systemUser, @ModelAttribute("school") SchoolsDO school) {
        school.setSeq(100);
        return "user/school/add";
    }

    /**
     * ���ѧУ����Ա, �ύ
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "add.htm", method = RequestMethod.POST)
    public String doAddUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, @ModelAttribute("school") SchoolsDO school, @RequestParam(value = "isEnableUser", required = false) String isEnableUser, Model model) {
        addSchoolUserValidator.validate(systemUser, result);
        if (result.hasErrors()) {
            return "user/school/add";
        }
        SystemUser existsUser = systemUserManager.getSystemUserByAccount(systemUser.getLoginName());
        if (existsUser != null) {
            model.addAttribute("loginNameError", "��ǰ�û��Ѵ���!");
            return "user/school/add";
        }
        SchoolsDO existsSchool = schoolsManager.getSchoolByName(systemUser.getRealName());
        if (existsSchool != null) {
            model.addAttribute("realNameError", "ѧУ�Ѵ���!");
            return "user/school/add";
        }
        if (EnumUserStatus.isEnable(isEnableUser)) {
            systemUser.setUserStatus(EnumUserStatus.ENABLE.getValue());
        }
        systemUser.setPassword(passwordValidator.digest(systemUser.getPassword(), 1));
        Long userId = systemUserManager.addSchoolUser(systemUser, school);
        model.addAttribute("url", "/user/school/list.htm");
        if (userId == null || userId < 1) {
            return "error";
        }
        return "success";
    }

    /**
     * �޸�ѧУ����Ա
     * 
     * @return
     */
    @RequestMapping(value = "edit.htm", method = RequestMethod.GET)
    public String editUser(@RequestParam("id") Long id, @ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, @ModelAttribute("school") SchoolsDO school, BindingResult schoolResult, Model model) {
        systemUser = systemUserManager.getUserById(id);
        school = schoolsManager.selectSchoolsById(systemUser.getUserProfiles().getSchoolId());
        school.setSchoolId(school.getId());
        systemUser.setPassword(passwordValidator.dectypt(systemUser.getPassword(), 1));
        model.addAttribute("systemUser", systemUser);
        model.addAttribute("school", school);
        return "user/school/edit";
    }

    /**
     * �޸�ѧУ����Ա
     * 
     * @return
     */
    @RequestMapping(value = "edit.htm", method = RequestMethod.POST)
    public String doEditUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, @ModelAttribute("school") SchoolsDO school, BindingResult schoolResult, Model model) {
        editSchoolUserValidator.validate(systemUser, result);
        if (result.hasErrors()) {
            return "user/school/edit";
        }
        SystemUser existsUser = systemUserManager.getSystemUserById(systemUser.getId());
        if (!existsUser.getLoginName().equalsIgnoreCase(systemUser.getLoginName())) {
            existsUser = systemUserManager.getSystemUserByAccount(systemUser.getLoginName());
            if (existsUser != null) {
                model.addAttribute("loginNameError", "��ǰ�û��Ѵ���!");
                return "user/school/edit";
            }
        }
        SchoolsDO existsSchool = schoolsManager.selectSchoolsById(school.getSchoolId());
        if (!existsSchool.getName().equals(systemUser.getRealName())) {
            existsSchool = schoolsManager.getSchoolByName(systemUser.getRealName());
            if (existsSchool != null) {
                model.addAttribute("realNameError", "ѧУ�Ѵ���!");
                return "user/school/edit";
            }
        }
        systemUser.setPassword(passwordValidator.digest(systemUser.getPassword(), 1));
        boolean isSuccess = systemUserManager.editSchoolUser(systemUser, school);
        model.addAttribute("url", "/user/school/list.htm");
        if (!isSuccess) {
            return "error";
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
    @RequestMapping(value = "enable.htm", method = RequestMethod.GET)
    public String enableUser(@RequestParam("id") Long id, Model model) {
        boolean isSuccess = systemUserManager.enableUser(id);
        model.addAttribute("url", "/user/school/list.htm");
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
    @RequestMapping(value = "disable.htm", method = RequestMethod.GET)
    public String disableUser(@RequestParam("id") Long id, Model model) {
        boolean isSuccess = systemUserManager.disableUser(id);
        model.addAttribute("url", "/user/school/list.htm");
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
        SystemUser user = systemUserManager.getUserById(id);
        int count = userProfilesManager.countCourseManagers(user.getUserProfiles().getSchoolId());
        model.addAttribute("url", "/user/school/list.htm");
        if (count > 0) {
            model.addAttribute("message", "��ѧУ����Ա�»��пγ̸����ˣ���˲���ɾ����û�");
            return "error";
        }
        int rows = systemUserManager.delSystemUserById(id, user.isInvisible());
        if (rows < 1) {
            return "error";
        }
        return "success";
    }
}
