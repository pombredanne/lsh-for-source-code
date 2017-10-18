package com.googlecode.openmpis.action;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;
import com.googlecode.openmpis.dto.Log;
import com.googlecode.openmpis.dto.User;
import com.googlecode.openmpis.form.UserForm;
import com.googlecode.openmpis.persistence.ibatis.dao.impl.LogDAOImpl;
import com.googlecode.openmpis.persistence.ibatis.dao.impl.PersonDAOImpl;
import com.googlecode.openmpis.persistence.ibatis.dao.impl.UserDAOImpl;
import com.googlecode.openmpis.persistence.ibatis.service.LogService;
import com.googlecode.openmpis.persistence.ibatis.service.PersonService;
import com.googlecode.openmpis.persistence.ibatis.service.UserService;
import com.googlecode.openmpis.persistence.ibatis.service.impl.LogServiceImpl;
import com.googlecode.openmpis.persistence.ibatis.service.impl.PersonServiceImpl;
import com.googlecode.openmpis.persistence.ibatis.service.impl.UserServiceImpl;
import com.googlecode.openmpis.util.Configuration;
import com.googlecode.openmpis.util.Constants;
import com.googlecode.openmpis.util.Mail;
import com.googlecode.openmpis.util.Pagination;
import com.googlecode.openmpis.util.Validator;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * The UserAction class provides the methods to list, add, edit, delete and view
 * administrators, encoders and investigators.
 * 
 * @author  <a href="mailto:rvbabilonia@gmail.com">Rey Vincent Babilonia</a>
 * @version 1.0
 */
public class UserAction extends DispatchAction {

    /**
     * The user service
     */
    private UserService userService = new UserServiceImpl(new UserDAOImpl());

    /**
     * The person service
     */
    private PersonService personService = new PersonServiceImpl(new PersonDAOImpl());

    /**
     * The log service
     */
    private LogService logService = new LogServiceImpl(new LogDAOImpl());

    /**
     * The file logger
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * The format for date (e.g. 2009-02-28)
     */
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The pagination context
     */
    private Pagination pagination = new Pagination();

    /**
     * Lists all users.
     * This is the list action called from the Struts framework.
     * 
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    public ActionForward list(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        User currentUser = null;
        if (request.getSession().getAttribute("currentuser") == null) {
            return mapping.findForward(Constants.EXPIRED);
        } else {
            currentUser = (User) request.getSession().getAttribute("currentuser");
        }
        if ((currentUser.getGroupId() == 0) || (currentUser.getGroupId() == 1)) {
            String page = request.getParameter("page");
            if (page != null) {
                if (page.equals("next")) {
                    pagination.nextPage();
                } else if (page.equals("previous")) {
                    pagination.previousPage();
                } else if (page.equals("start")) {
                    pagination.firstPage();
                } else if (page.equals("end")) {
                    pagination.lastPage();
                }
            }
            List<User> userList = userService.getAllUsers(pagination);
            request.setAttribute("userlist", userList);
            request.setAttribute("currentpage", pagination.getCurrentPage());
            request.setAttribute("totalpages", pagination.getTotalPages());
            request.setAttribute("totalresults", pagination.getTotalResults());
            request.setAttribute("maxresults", pagination.getMaxResults());
            request.setAttribute("morepages", pagination.hasMorePages());
            return mapping.findForward(Constants.LIST_USER);
        } else {
            return mapping.findForward(Constants.UNAUTHORIZED);
        }
    }

    /**
     * Prepares the form for user creation.
     * This is the new user action called from the Struts framework.
     * 
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     */
    public ActionForward newUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        User currentUser = null;
        if (request.getSession().getAttribute("currentuser") == null) {
            return mapping.findForward(Constants.EXPIRED);
        } else {
            currentUser = (User) request.getSession().getAttribute("currentuser");
        }
        if ((currentUser.getGroupId() == 0) || (currentUser.getGroupId() == 1)) {
            return mapping.findForward(Constants.ADD_USER);
        } else {
            return mapping.findForward(Constants.UNAUTHORIZED);
        }
    }

    /**
     * Inserts a new user into the database.
     * This is the add user action called from the HTML form.
     * 
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    public ActionForward addUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        User currentUser = null;
        if (request.getSession().getAttribute("currentuser") == null) {
            return mapping.findForward(Constants.EXPIRED);
        } else {
            currentUser = (User) request.getSession().getAttribute("currentuser");
        }
        if ((currentUser.getGroupId() == 0) || (currentUser.getGroupId() == 1)) {
            UserForm userForm = (UserForm) form;
            ActionMessages errors = new ActionMessages();
            if (isValidNewUser(request, form)) {
                User checker = new User();
                checker.setId(userForm.getId());
                checker.setEmail(userForm.getEmail());
                String username = createUsername(userForm);
                checker.setUsername(username);
                if (userService.isUniqueUsername(checker)) {
                    if (userService.isUniqueEmail(checker)) {
                        User user = new User();
                        user.setGroupId(userForm.getGroupId());
                        user.setUsername(username);
                        String password = "p@$$w0rd";
                        user.setPassword(password);
                        user.setFirstName(userForm.getFirstName());
                        user.setLastName(userForm.getLastName());
                        user.setMiddleName(userForm.getMiddleName());
                        user.setBirthMonth(userForm.getBirthMonth());
                        user.setBirthDay(userForm.getBirthDay());
                        user.setBirthYear(userForm.getBirthYear());
                        user.setEmail(userForm.getEmail());
                        user.setDesignation(userForm.getDesignation());
                        user.setAgency(userForm.getAgency());
                        user.setNumber(userForm.getNumber());
                        String date = simpleDateFormat.format(System.currentTimeMillis());
                        user.setDate(date);
                        user.setCreatorId(currentUser.getId());
                        user.setQuestion(0);
                        user.setStatus(userForm.getStatus());
                        boolean isInserted = userService.insertUser(user);
                        if (isInserted) {
                            Log addLog = new Log();
                            addLog.setLog("User " + username + " was created by " + currentUser.getUsername() + ".");
                            addLog.setDate(date);
                            logService.insertLog(addLog);
                            logger.info(addLog.toString());
                            Configuration config = new Configuration("mail.properties");
                            if (Boolean.parseBoolean(config.getProperty("mail.enable"))) {
                                Mail mail = new Mail();
                                mail.send(currentUser.getFirstName(), currentUser.getLastName(), currentUser.getEmail(), user.getEmail(), "Account Creation", "Dear " + user.getFirstName() + "," + "\n\nYou received this email because your account has been created. " + "Your username is " + username + " and your password is " + password + ". Please log in to the system immediately and modify your profile. " + "\n\nIf you have any questions, please feel free to email me." + "\n\nYours truly," + "\n" + currentUser.getFirstName());
                            }
                            request.setAttribute("user", user);
                            request.setAttribute("operation", "add");
                            return mapping.findForward(Constants.ADD_USER_SUCCESS);
                        } else {
                            return mapping.findForward(Constants.FAILURE);
                        }
                    } else {
                        errors.add("email", new ActionMessage("error.email.duplicate"));
                        saveErrors(request, errors);
                        logger.error("Duplicate email.");
                        return mapping.findForward(Constants.ADD_USER_REDO);
                    }
                } else {
                    errors.add("username", new ActionMessage("error.username.duplicate", username));
                    saveErrors(request, errors);
                    logger.error("Duplicate username.");
                    return mapping.findForward(Constants.ADD_USER_REDO);
                }
            } else {
                return mapping.findForward(Constants.ADD_USER_REDO);
            }
        } else {
            return mapping.findForward(Constants.UNAUTHORIZED);
        }
    }

    /**
     * Views the profile of a user.
     * This is the view user action called from the Struts framework.
     * 
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    public ActionForward viewUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        User currentUser = null;
        UserForm userForm = (UserForm) form;
        if (request.getSession().getAttribute("currentuser") == null) {
            return mapping.findForward(Constants.EXPIRED);
        } else {
            currentUser = (User) request.getSession().getAttribute("currentuser");
        }
        if ((currentUser.getGroupId() == 0) || (currentUser.getGroupId() == 1) || (currentUser.getGroupId() == 2)) {
            try {
                int id = 0;
                if (request.getParameter("id") != null) {
                    id = Integer.parseInt(request.getParameter("id"));
                } else if (request.getParameter("id") == null) {
                    id = currentUser.getId();
                }
                System.out.println(id);
                User user = userService.getUserById(id);
                userForm.setId(user.getId());
                userForm.setGroupId(user.getGroupId());
                userForm.setUsername(user.getUsername());
                userForm.setFirstName(user.getFirstName());
                userForm.setMiddleName(user.getMiddleName());
                userForm.setLastName(user.getLastName());
                userForm.setBirthMonth(user.getBirthMonth());
                userForm.setBirthDay(user.getBirthDay());
                userForm.setBirthYear(user.getBirthYear());
                userForm.setDesignation(user.getDesignation());
                userForm.setAgency(user.getAgency());
                userForm.setEmail(user.getEmail());
                userForm.setNumber(user.getNumber());
                userForm.setIpAddress(user.getIpAddress());
                userForm.setLastLogin(user.getLastLogin());
                userForm.setDate(user.getDate());
                userForm.setCreatorId(user.getCreatorId());
                userForm.setStatus(user.getStatus());
                userForm.setQuestion(user.getQuestion());
                userForm.setAnswer((user.getAnswer() != null) ? user.getAnswer() : "");
                if (user.getGroupId() == 2) {
                    request.setAttribute("caseshandled", personService.countPersonsByInvestigatorId(user.getId()));
                }
                if (user.getGroupId() == 1) {
                    request.setAttribute("casesencoded", personService.countPersonsByEncoderId(user.getId()));
                    request.setAttribute("usersencoded", userService.countEncodedUsers(user.getId()));
                }
                if (user.getGroupId() == 0) {
                    request.setAttribute("usersencoded", userService.countEncodedUsers(user.getId()));
                }
                if ((currentUser.getId() == user.getCreatorId()) || (currentUser.getId() == user.getId()) || ((currentUser.getGroupId() == 0) && (user.getGroupId() > 0))) {
                    return mapping.findForward(Constants.EDIT_USER);
                } else {
                    return mapping.findForward(Constants.UNAUTHORIZED);
                }
            } catch (NumberFormatException nfe) {
                return mapping.findForward(Constants.LIST_USER_REDO);
            }
        } else {
            return mapping.findForward(Constants.UNAUTHORIZED);
        }
    }

    /**
     * Updates the user in the database.
     * This is the edit user action called from the HTML form.
     * 
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    public ActionForward editUser(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        User currentUser = null;
        if (request.getSession().getAttribute("currentuser") == null) {
            return mapping.findForward(Constants.EXPIRED);
        } else {
            currentUser = (User) request.getSession().getAttribute("currentuser");
        }
        if ((currentUser.getGroupId() == 0) || (currentUser.getGroupId() == 1) || (currentUser.getGroupId() == 2)) {
            UserForm userForm = (UserForm) form;
            ActionMessages errors = new ActionMessages();
            if (isValidExistingUser(request, form)) {
                User checker = new User();
                checker.setId(userForm.getId());
                checker.setEmail(userForm.getEmail());
                String username = createUsername(userForm);
                checker.setUsername(username);
                if (userService.isUniqueUsername(checker)) {
                    if (userService.isUniqueEmail(checker)) {
                        User user = new User();
                        user.setId(userForm.getId());
                        user.setGroupId(userForm.getGroupId());
                        user.setUsername(username);
                        user.setFirstName(userForm.getFirstName());
                        user.setMiddleName(userForm.getMiddleName());
                        user.setLastName(userForm.getLastName());
                        user.setBirthMonth(userForm.getBirthMonth());
                        user.setBirthDay(userForm.getBirthDay());
                        user.setBirthYear(userForm.getBirthYear());
                        user.setDesignation(userForm.getDesignation());
                        user.setAgency(userForm.getAgency());
                        user.setEmail(userForm.getEmail());
                        user.setNumber(userForm.getNumber());
                        user.setStatus(userForm.getStatus());
                        user.setQuestion(userForm.getQuestion());
                        user.setAnswer(userForm.getAnswer());
                        if ((userForm.getPassword() != null) && (userForm.getRetype() != null) && (userForm.getPassword().equals(userForm.getRetype()))) {
                            user.setPassword(userForm.getPassword());
                            userService.updatePassword(user);
                        }
                        boolean isUpdated = userService.updateUser(user);
                        if (isUpdated) {
                            Log editLog = new Log();
                            if (username.equals(userForm.getUsername())) {
                                editLog.setLog("User " + username + " was updated by " + currentUser.getUsername() + ".");
                            } else {
                                editLog.setLog("User " + userForm.getUsername() + " was renamed to " + username + " by " + currentUser.getUsername() + ".");
                            }
                            editLog.setDate(simpleDateFormat.format(System.currentTimeMillis()));
                            logService.insertLog(editLog);
                            logger.info(editLog.toString());
                            request.setAttribute("username", username);
                            request.setAttribute("operation", "edit");
                            return mapping.findForward(Constants.EDIT_USER_SUCCESS);
                        } else {
                            return mapping.findForward(Constants.FAILURE);
                        }
                    } else {
                        errors.add("email", new ActionMessage("error.email.duplicate"));
                        saveErrors(request, errors);
                        logger.error("Duplicate email.");
                        return mapping.findForward(Constants.EDIT_USER_REDO);
                    }
                } else {
                    errors.add("username", new ActionMessage("error.username.duplicate", username));
                    saveErrors(request, errors);
                    logger.error("Duplicate username.");
                    return mapping.findForward(Constants.EDIT_USER_REDO);
                }
            } else {
                return mapping.findForward(Constants.EDIT_USER_REDO);
            }
        } else {
            return mapping.findForward(Constants.UNAUTHORIZED);
        }
    }

    /**
     * Validates the inputs from the user form.
     * 
     * @param request       the HTTP Request we are processing
     * @param form          the ActionForm bean for this request
     * @return              <code>true</code> if there are no errors in the form; <code>false</code> otherwise
     */
    private boolean isValidNewUser(HttpServletRequest request, ActionForm form) throws Exception {
        ActionMessages errors = new ActionMessages();
        Validator validator = new Validator();
        boolean isValid = true;
        UserForm userForm = (UserForm) form;
        String firstName = userForm.getFirstName();
        String middleName = userForm.getMiddleName();
        String lastName = userForm.getLastName();
        Calendar calendar = Calendar.getInstance();
        calendar.set(userForm.getBirthYear(), (userForm.getBirthMonth() - 1), userForm.getBirthDay());
        String agency = userForm.getAgency();
        String designation = userForm.getDesignation();
        String email = userForm.getEmail();
        String number = userForm.getNumber();
        if (firstName.length() < 1) {
            errors.add("firstname", new ActionMessage("error.firstname.required"));
        } else {
            if (!validator.isValidFirstName(firstName)) {
                errors.add("firstname", new ActionMessage("error.firstname.invalid"));
            }
        }
        if (middleName.length() < 1) {
            errors.add("middlename", new ActionMessage("error.middlename.required"));
        } else {
            if (!validator.isValidLastName(middleName)) {
                errors.add("middlename", new ActionMessage("error.middlename.invalid"));
            }
        }
        if (lastName.length() < 1) {
            errors.add("lastname", new ActionMessage("error.lastname.required"));
        } else {
            if (!validator.isValidLastName(lastName)) {
                errors.add("lastname", new ActionMessage("error.lastname.invalid"));
            }
        }
        if (userForm.getBirthMonth() == calendar.get(Calendar.MONTH)) {
            errors.add("birthdate", new ActionMessage("error.birthdate.invalid"));
        }
        if (email.length() < 1) {
            errors.add("email", new ActionMessage("error.email.required"));
        } else {
            if (!validator.isValidEmailAddress(email)) {
                errors.add("email", new ActionMessage("error.email.invalid"));
            }
        }
        if (designation.length() < 1) {
            errors.add("designation", new ActionMessage("error.designation.required"));
        } else {
            if (!validator.isValidKeyword(designation)) {
                errors.add("designation", new ActionMessage("error.designation.invalid"));
            }
        }
        if (agency.length() < 1) {
            errors.add("agency", new ActionMessage("error.agency.required"));
        } else {
            if (!validator.isValidKeyword(agency)) {
                errors.add("agency", new ActionMessage("error.agency.invalid"));
            }
        }
        if (number.length() < 1) {
            errors.add("number", new ActionMessage("error.number.required"));
        } else {
            if (!validator.isValidNumber(number)) {
                errors.add("number", new ActionMessage("error.number.invalid"));
            }
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            isValid = false;
        }
        return isValid;
    }

    /**
     * Validates the inputs from the user form.
     * 
     * @param request       the HTTP Request we are processing
     * @param form          the ActionForm bean for this request
     * @return              <code>true</code> if there are no errors in the form; <code>false</code> otherwise
     */
    private boolean isValidExistingUser(HttpServletRequest request, ActionForm form) {
        ActionMessages errors = new ActionMessages();
        Validator validator = new Validator();
        boolean isValid = true;
        User currentUser = (User) request.getSession().getAttribute("currentuser");
        UserForm userForm = (UserForm) form;
        String firstName = userForm.getFirstName();
        String middleName = userForm.getMiddleName();
        String lastName = userForm.getLastName();
        Calendar calendar = Calendar.getInstance();
        calendar.set(userForm.getBirthYear(), (userForm.getBirthMonth() - 1), userForm.getBirthDay());
        String agency = userForm.getAgency();
        String designation = userForm.getDesignation();
        String email = userForm.getEmail();
        String number = userForm.getNumber();
        String password = userForm.getPassword();
        String retype = userForm.getRetype();
        String answer = userForm.getAnswer();
        if (currentUser.getId() == userForm.getId()) {
            if (retype.equals("d41d8cd98f00b204e9800998ecf8427e")) {
            } else if ((retype != null) && (retype.length() > 0)) {
                if (password == null) {
                    errors.add("password", new ActionMessage(""));
                } else {
                    if (password.equals("d41d8cd98f00b204e9800998ecf8427e")) {
                        errors.add("password", new ActionMessage("error.password.required"));
                    } else if (!password.equals(retype)) {
                        errors.add("password", new ActionMessage("error.password.mismatch"));
                    }
                }
            }
            if (answer == null) {
                errors.add("answer", new ActionMessage(""));
            } else {
                if (answer.length() < 1) {
                    errors.add("answer", new ActionMessage("error.answer.required"));
                } else {
                    if (!validator.isValidKeyword(answer)) {
                        errors.add("answer", new ActionMessage("error.answer.invalid"));
                    }
                }
            }
        }
        if (firstName == null) {
            errors.add("firstname", new ActionMessage(""));
        } else {
            if (firstName.length() < 1) {
                errors.add("firstname", new ActionMessage("error.firstname.required"));
            } else {
                if (!validator.isValidFirstName(firstName)) {
                    errors.add("firstname", new ActionMessage("error.firstname.invalid"));
                }
            }
        }
        if (middleName == null) {
            errors.add("middlename", new ActionMessage(""));
        } else {
            if (middleName.length() < 1) {
                errors.add("middlename", new ActionMessage("error.middlename.required"));
            } else {
                if (!validator.isValidLastName(middleName)) {
                    errors.add("middlename", new ActionMessage("error.middlename.invalid"));
                }
            }
        }
        if (lastName == null) {
            errors.add("lastname", new ActionMessage(""));
        } else {
            if (lastName.length() < 1) {
                errors.add("lastname", new ActionMessage("error.lastname.required"));
            } else {
                if (!validator.isValidLastName(lastName)) {
                    errors.add("lastname", new ActionMessage("error.lastname.invalid"));
                }
            }
        }
        if (userForm.getBirthMonth() == calendar.get(Calendar.MONTH)) {
            errors.add("birthdate", new ActionMessage("error.birthdate.invalid"));
        }
        if (email == null) {
            errors.add("email", new ActionMessage(""));
        } else {
            if (email.length() < 1) {
                errors.add("email", new ActionMessage("error.email.required"));
            } else {
                if (!validator.isValidEmailAddress(email)) {
                    errors.add("email", new ActionMessage("error.email.invalid"));
                }
            }
        }
        if (designation == null) {
            errors.add("designation", new ActionMessage(""));
        } else {
            if (designation.length() < 1) {
                errors.add("designation", new ActionMessage("error.designation.required"));
            } else {
                if (!validator.isValidKeyword(designation)) {
                    errors.add("designation", new ActionMessage("error.designation.invalid"));
                }
            }
        }
        if (agency == null) {
            errors.add("agency", new ActionMessage(""));
        } else {
            if (agency.length() < 1) {
                errors.add("agency", new ActionMessage("error.agency.required"));
            } else {
                if (!validator.isValidKeyword(agency)) {
                    errors.add("agency", new ActionMessage("error.agency.invalid"));
                }
            }
        }
        if (number == null) {
            errors.add("number", new ActionMessage(""));
        } else {
            if (number.length() < 1) {
                errors.add("number", new ActionMessage("error.number.required"));
            } else {
                if (!validator.isValidNumber(number)) {
                    errors.add("number", new ActionMessage("error.number.invalid"));
                }
            }
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            isValid = false;
        }
        return isValid;
    }

    /**
     * Creates username given the first name, last name, birth month and birth day.
     *
     * @param userForm      the user form
     * @return              the username of the form CCNNNN
     */
    private String createUsername(UserForm userForm) {
        DecimalFormat df = new DecimalFormat("00");
        char firstCharacter = (userForm.getFirstName() != null) ? userForm.getFirstName().charAt(0) : ' ';
        char secondCharacter = ' ';
        for (int i = 0; i < userForm.getLastName().length(); i++) {
            if (Character.isUpperCase(userForm.getLastName().charAt(i))) {
                secondCharacter = userForm.getLastName().charAt(i);
            }
        }
        char letters[] = { firstCharacter, secondCharacter };
        String username = new String(letters);
        username += df.format(userForm.getBirthMonth()) + df.format(userForm.getBirthDay());
        return username;
    }

    /**
     * Counts the number of users in the system.
     *
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    public ActionForward countUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setAttribute("usercount", userService.countAllUsers());
        request.setAttribute("administratorcount", userService.countAdministrators());
        request.setAttribute("encodercount", userService.countEncoders());
        request.setAttribute("investigatorcount", userService.countInvestigators());
        request.setAttribute("activecount", userService.countActiveUsers());
        request.setAttribute("suspendedcount", userService.countSuspendedUsers());
        return mapping.findForward(Constants.USER_STATISTICS);
    }

    /**
     * Writes the users to a PDF file.
     *
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    public ActionForward printUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        User currentUser = null;
        if (request.getSession().getAttribute("currentuser") != null) {
            currentUser = (User) request.getSession().getAttribute("currentuser");
        }
        Document document = new Document(PageSize.LETTER.rotate(), 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.addTitle("Case Statistics");
        document.addAuthor("OpenMPIS");
        document.addSubject("Statistics for All Cases");
        document.addKeywords("OpenMPIS, missing, found, unidentified");
        document.addProducer();
        document.addCreationDate();
        document.addCreator("OpenMPIS version " + Constants.VERSION);
        String date = simpleDateFormat.format(System.currentTimeMillis());
        document.setHeader(new HeaderFooter(new Phrase("List of users as of " + date), false));
        HeaderFooter footer = new HeaderFooter(new Phrase("Page : "), true);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.setFooter(footer);
        document.open();
        Table table = new Table(2);
        table.setBorderWidth(1);
        table.setPadding(2);
        table.setSpacing(0);
        Paragraph paragraph = new Paragraph("Users", FontFactory.getFont(FontFactory.HELVETICA, 24, Font.BOLD));
        paragraph.setAlignment(Paragraph.ALIGN_CENTER);
        Cell cell = new Cell(paragraph);
        cell.setHeader(true);
        cell.setColspan(2);
        table.addCell(cell);
        table.endHeaders();
        table.addCell("Total Administrators");
        table.addCell("" + userService.countAdministrators());
        table.addCell("\t\t\t\t\tActive Administrators");
        table.addCell("\t\t\t\t\t\t\t\t\t\t" + userService.countActiveAdministrators());
        table.addCell("\t\t\t\t\tSuspended Administrators");
        table.addCell("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + userService.countSuspendedAdministrators());
        table.addCell("Total Encoders");
        table.addCell("" + userService.countEncoders());
        table.addCell("\t\t\t\t\tActive Encoders");
        table.addCell("\t\t\t\t\t\t\t\t\t\t" + userService.countActiveEncoders());
        table.addCell("\t\t\t\t\tSuspended Encoders");
        table.addCell("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + userService.countSuspendedEncoders());
        table.addCell("Total Investigators");
        table.addCell("" + userService.countInvestigators());
        table.addCell("\t\t\t\t\tActive Investigators");
        table.addCell("\t\t\t\t\t\t\t\t\t\t" + userService.countActiveInvestigators());
        table.addCell("\t\t\t\t\tSuspended Investigators");
        table.addCell("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + userService.countSuspendedInvestigators());
        table.addCell("Total Users");
        table.addCell("" + userService.countAllUsers());
        table.addCell("\t\t\t\t\tTotal Active Users");
        table.addCell("\t\t\t\t\t\t\t\t\t\t" + userService.countActiveUsers());
        table.addCell("\t\t\t\t\tTotal Suspended Users");
        table.addCell("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + userService.countSuspendedUsers());
        document.add(table);
        if (currentUser != null) {
            if ((currentUser.getGroupId() == 0) || (currentUser.getGroupId() == 1)) {
                document.setHeader(new HeaderFooter(new Phrase("List of administrators as of " + date), false));
                document.newPage();
                float[] widths = { 0.03f, 0.07f, 0.1f, 0.1f, 0.1f, 0.1f, 0.2f, 0.05f };
                PdfPTable pdfptable = new PdfPTable(widths);
                pdfptable.setWidthPercentage(100);
                pdfptable.addCell(new Phrase("ID", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Group", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Last Name", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("First Name", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Agency", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Designation", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("E-mail Address", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Status", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                List<User> administratorList = userService.listAdministrators();
                for (User administrator : administratorList) {
                    pdfptable.addCell(new Phrase("" + administrator.getId(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(getResources(request).getMessage("group." + administrator.getGroupId()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(administrator.getLastName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(administrator.getFirstName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(administrator.getAgency(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(administrator.getDesignation(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(administrator.getEmail(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(getResources(request).getMessage("status.user." + administrator.getStatus()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                }
                document.add(pdfptable);
                document.setHeader(new HeaderFooter(new Phrase("List of encoders as of " + date), false));
                document.newPage();
                pdfptable = new PdfPTable(widths);
                pdfptable.setWidthPercentage(100);
                pdfptable.addCell(new Phrase("ID", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Group", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Last Name", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("First Name", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Agency", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Designation", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("E-mail Address", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Status", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                List<User> encoderList = userService.listEncoders();
                for (User encoder : encoderList) {
                    pdfptable.addCell(new Phrase("" + encoder.getId(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(getResources(request).getMessage("group." + encoder.getGroupId()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(encoder.getLastName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(encoder.getFirstName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(encoder.getAgency(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(encoder.getDesignation(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(encoder.getEmail(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(getResources(request).getMessage("status.user." + encoder.getStatus()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                }
                document.add(pdfptable);
                document.setHeader(new HeaderFooter(new Phrase("List of investigators as of " + date), false));
                document.newPage();
                pdfptable = new PdfPTable(widths);
                pdfptable.setWidthPercentage(100);
                pdfptable.addCell(new Phrase("ID", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Group", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Last Name", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("First Name", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Agency", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Designation", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("E-mail Address", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                pdfptable.addCell(new Phrase("Status", FontFactory.getFont(FontFactory.HELVETICA, 12)));
                List<User> investigatorList = userService.listInvestigators();
                for (User investigator : investigatorList) {
                    pdfptable.addCell(new Phrase("" + investigator.getId(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(getResources(request).getMessage("group." + investigator.getGroupId()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(investigator.getLastName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(investigator.getFirstName(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(investigator.getAgency(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(investigator.getDesignation(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(investigator.getEmail(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    pdfptable.addCell(new Phrase(getResources(request).getMessage("status.user." + investigator.getStatus()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                }
                document.add(pdfptable);
            }
        }
        document.close();
        response.setContentType("application/pdf");
        response.setContentLength(baos.size());
        response.setHeader("Content-disposition", "attachment; filename=User_Statistics.pdf");
        baos.writeTo(response.getOutputStream());
        response.getOutputStream().flush();
        return null;
    }

    /**
     * Creates an MD5-encrypted password.
     * Adapted from http://snipplr.com/view/4321/generate-md5-hash-from-string/.
     *
     * @param password      the password
     * @return              the 32 alphanumeric-equivalent of the password
     * @throws java.security.NoSuchAlgorithmException
     */
    private String encryptPassword(String password) throws NoSuchAlgorithmException {
        StringBuffer encryptedPassword = new StringBuffer();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(password.getBytes());
        byte digest[] = md5.digest();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                encryptedPassword.append('0');
            }
            encryptedPassword.append(hex);
        }
        return encryptedPassword.toString();
    }
}
