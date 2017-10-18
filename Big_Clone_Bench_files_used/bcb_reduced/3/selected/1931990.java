package com.kongur.star.venus.web.action.systemanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.kongur.star.venus.common.password.PasswordValidator;
import com.kongur.star.venus.domain.courses.UserProfilesDO;
import com.kongur.star.venus.domain.system.SystemAgent;
import com.kongur.star.venus.domain.system.SystemUser;
import com.kongur.star.venus.domain.system.query.SystemUserQuery;
import com.kongur.star.venus.enums.EnumUserType;
import com.kongur.star.venus.manager.courses.UserProfilesManager;
import com.kongur.star.venus.manager.system.SystemUserManager;
import com.kongur.star.venus.web.action.BaseAction;
import com.kongur.star.venus.web.validator.system.SystemUserValidator;

@Controller
@RequestMapping("user/course/")
public class CourseManagerAction extends BaseAction {

    @Autowired
    private SystemUserManager systemUserManager;

    @Autowired
    private UserProfilesManager userProfilesManager;

    SystemUserValidator systemUserValidator = new SystemUserValidator();

    @Autowired
    private PasswordValidator passwordValidator;

    /**
     * ��ѯ��Ŀ������ ���ѧУID ��ѯ����Ϊ��Ŀ������
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "list.htm")
    public String list(@ModelAttribute("query") SystemUserQuery query) {
        query.setUserType(EnumUserType.COURCE_MANAGER);
        UserProfilesDO profilesDO = userProfilesManager.selectUserProfilesByUserId(getLoginUser().getId());
        if (profilesDO != null) {
            query.setSchoolId(profilesDO.getSchoolId().intValue());
        }
        systemUserManager.queryCourseManager(query);
        return "/user/course/list";
    }

    /**
     * ��ʼ����Ŀ������
     * 
     * @param query
     * @return
     */
    @RequestMapping(value = "add.htm", method = RequestMethod.GET)
    public String addUser(@ModelAttribute("systemUser") SystemUser systemUser) {
        return "/user/course/add";
    }

    /**
     * ִ��������Ŀ������
     * 
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "add.htm", method = RequestMethod.POST)
    public String saveUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, SystemAgent systemAgent, ModelMap model) throws Exception {
        SystemUser existsUser = systemUserManager.getSystemUserByAccount(systemUser.getLoginName());
        if (existsUser != null) {
            model.addAttribute("loginNameError", "��ǰ�û��Ѵ���!");
            return "/user/course/add";
        }
        systemUser.setPassword(passwordValidator.digest(systemUser.getPassword(), 1));
        UserProfilesDO profilesDO = userProfilesManager.selectUserProfilesByUserId(getLoginUser().getId());
        if (profilesDO != null) {
            UserProfilesDO newProfilesDO = new UserProfilesDO();
            newProfilesDO.setSchoolId(profilesDO.getSchoolId());
            systemUser.setUserProfiles(newProfilesDO);
        }
        Long id = systemUserManager.addCourseManager(systemUser);
        if (null == id) {
            model.put("result", "������Ŀ������ʧ�ܣ�");
            return "/error";
        }
        return "redirect:/user/course/list.htm";
    }

    /**
     * ��ʾ��Ŀ��������Ϣ
     * 
     * @param id
     * @param model
     * @return
     */
    @RequestMapping(value = "edit", method = RequestMethod.GET)
    public String editUser(@RequestParam("id") Long id, ModelMap model) throws Exception {
        model.put("systemUser", systemUserManager.getSystemUserById(id));
        return "/user/course/edit";
    }

    @RequestMapping(value = "edit", method = RequestMethod.POST)
    public String saveEditUser(@ModelAttribute("systemUser") SystemUser systemUser, BindingResult result, SystemAgent systemAgent, ModelMap model) throws Exception {
        int rs = systemUserManager.update(systemUser);
        if (rs <= 0) {
            model.put("result", "�������ʧ��");
            return "/user/course/edit";
        }
        return "redirect:/user/course/list.htm";
    }

    /**
     * ɾ����Ŀ��������Ϣ
     * 
     * @param id
     * @return
     */
    @RequestMapping(value = "delete", method = RequestMethod.GET)
    public String deleteCourseManager(@RequestParam("id") Long id) {
        systemUserManager.deleteCourseManager(id);
        return "redirect:/user/course/list.htm";
    }
}
