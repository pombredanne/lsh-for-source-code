package org.telscenter.sail.webapp.presentation.web.controllers;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.sail.webapp.dao.ObjectNotFoundException;
import net.sf.sail.webapp.dao.sds.HttpStatusCodeException;
import net.sf.sail.webapp.dao.sds.impl.AbstractHttpRestCommand;
import net.sf.sail.webapp.domain.User;
import net.sf.sail.webapp.domain.Workgroup;
import net.sf.sail.webapp.domain.impl.CurnitGetCurnitUrlVisitor;
import net.sf.sail.webapp.domain.webservice.http.HttpPostRequest;
import net.sf.sail.webapp.domain.webservice.http.impl.HttpRestTransportImpl;
import net.sf.sail.webapp.presentation.web.controllers.ControllerUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.telscenter.sail.webapp.domain.Run;
import org.telscenter.sail.webapp.domain.attendance.StudentAttendance;
import org.telscenter.sail.webapp.domain.authentication.impl.StudentUserDetails;
import org.telscenter.sail.webapp.domain.authentication.impl.TeacherUserDetails;
import org.telscenter.sail.webapp.domain.project.Project;
import org.telscenter.sail.webapp.presentation.util.json.JSONArray;
import org.telscenter.sail.webapp.presentation.util.json.JSONException;
import org.telscenter.sail.webapp.presentation.util.json.JSONObject;
import org.telscenter.sail.webapp.presentation.web.controllers.run.RunUtil;
import org.telscenter.sail.webapp.presentation.web.filters.TelsAuthenticationProcessingFilter;
import org.telscenter.sail.webapp.service.attendance.StudentAttendanceService;
import org.telscenter.sail.webapp.service.authentication.UserDetailsService;
import org.telscenter.sail.webapp.service.offering.RunService;
import org.telscenter.sail.webapp.service.workgroup.WISEWorkgroupService;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Controller to bridge GET/POST access to the vlewrapper webapp. Validates
 * logged in user, makes sure they're logged in and has the right
 * permissions, etc, before forwarding the request to the appropriate
 * servlet in the vlewrapper webapp.
 * @author hirokiterashima
 * @version $Id:$
 */
public class BridgeController extends AbstractController {

    private WISEWorkgroupService workgroupService;

    private RunService runService;

    private Properties portalProperties;

    private StudentAttendanceService studentAttendanceService;

    /**
	 * @see org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (ControllerUtil.getSignedInUser() == null) {
            response.sendRedirect("/webapp/login.html");
            return null;
        }
        boolean authorized = authorize(request);
        if (!authorized) {
            if (request.getRequestURI().equals("/webapp/bridge/postdata.html")) {
                User signedInUser = ControllerUtil.getSignedInUser();
                if (signedInUser.getUserDetails() instanceof TeacherUserDetails) {
                    response.sendRedirect("/webapp" + TelsAuthenticationProcessingFilter.TEACHER_DEFAULT_TARGET_PATH);
                    return null;
                } else if (signedInUser.getUserDetails() instanceof StudentUserDetails) {
                    response.sendRedirect("/webapp" + TelsAuthenticationProcessingFilter.STUDENT_DEFAULT_TARGET_PATH);
                    return null;
                } else {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to access this page");
                    return null;
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to access this page");
                return null;
            }
        }
        String method = request.getMethod();
        if (method.equals("GET")) {
            return handleGet(request, response);
        } else if (method.equals("POST")) {
            return handlePost(request, response);
        }
        return null;
    }

    private boolean authorize(HttpServletRequest request) {
        String method = request.getMethod();
        User signedInUser = ControllerUtil.getSignedInUser();
        GrantedAuthority[] authorities = signedInUser.getUserDetails().getAuthorities();
        Long signedInUserId = null;
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(UserDetailsService.ADMIN_ROLE)) {
                return true;
            } else if (authority.getAuthority().equals(UserDetailsService.TEACHER_ROLE)) {
                Run run = null;
                try {
                    run = runService.retrieveById(new Long(request.getParameter("runId")));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (ObjectNotFoundException e) {
                    e.printStackTrace();
                }
                if (run == null) {
                    return false;
                } else if (this.runService.hasRunPermission(run, signedInUser, BasePermission.WRITE)) {
                    return true;
                } else if (this.runService.hasRunPermission(run, signedInUser, BasePermission.READ)) {
                    if (method.equals("GET")) {
                        return true;
                    } else if (method.equals("POST")) {
                        return false;
                    }
                }
            }
        }
        if (method.equals("GET")) {
            String workgroupIdStr = "";
            String fromWorkgroupIdStr = "";
            String type = request.getParameter("type");
            String runIdString = request.getParameter("runId");
            Long runId = null;
            if (runIdString != null) {
                runId = Long.parseLong(runIdString);
            }
            String periodString = request.getParameter("periodId");
            Long period = null;
            if (periodString != null) {
                period = Long.parseLong(periodString);
            }
            if (runId != null) {
                try {
                    Run offering = runService.retrieveById(runId);
                    List<Workgroup> workgroupListByOfferingAndUser = workgroupService.getWorkgroupListByOfferingAndUser(offering, signedInUser);
                    Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
                    signedInUserId = workgroup.getId();
                } catch (ObjectNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
            boolean canAccessOtherWorkgroups = false;
            if (type == null) {
                workgroupIdStr = request.getParameter("userId");
            } else if (type.equals("flag")) {
                workgroupIdStr = request.getParameter("userId");
                canAccessOtherWorkgroups = true;
            } else if (type.equals("annotation")) {
                workgroupIdStr = request.getParameter("toWorkgroup");
                fromWorkgroupIdStr = request.getParameter("fromWorkgroup");
                canAccessOtherWorkgroups = true;
            } else if (type.equals("brainstorm")) {
                workgroupIdStr = request.getParameter("userId");
                canAccessOtherWorkgroups = true;
            } else if (type.equals("journal")) {
                workgroupIdStr = request.getParameter("workgroupId");
            } else if (type.equals("peerreview")) {
                return true;
            } else if (type.equals("xlsexport")) {
                return true;
            } else if (type.equals("ideaBasket")) {
                return true;
            } else if (type.equals("studentAssetManager")) {
                return true;
            } else if (type.equals("xmppAuthenticate")) {
                return true;
            } else {
            }
            if (workgroupIdStr == null || workgroupIdStr.equals("")) {
                return false;
            }
            String[] workgroupIds = workgroupIdStr.split(":");
            if (canAccessOtherWorkgroups) {
                try {
                    if (fromWorkgroupIdStr != null && !fromWorkgroupIdStr.equals("") && fromWorkgroupIdStr.equals(signedInUserId)) {
                        return true;
                    } else {
                        Set<Workgroup> classmateWorkgroups = runService.getWorkgroups(runId, period);
                        return elementsInCollection(workgroupIds, classmateWorkgroups);
                    }
                } catch (ObjectNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                List<Workgroup> workgroupsForUser = workgroupService.getWorkgroupsForUser(signedInUser);
                return elementsInCollection(workgroupIds, workgroupsForUser);
            }
            return false;
        } else if (method.equals("POST")) {
            return true;
        }
        return false;
    }

    /**
	 * Checks whether all the elements in the idsAccessing array are
	 * found in the idsAllowed Collection
	 * @param idsAccessing the ids the user is trying to access
	 * @param idsAllowed the ids the user is allowed to access
	 * @return whether all the elements are in the Collection
	 */
    private boolean elementsInCollection(String[] idsAccessing, Collection<Workgroup> idsAllowed) {
        List<String> idsAccessingList = Arrays.asList(idsAccessing);
        List<String> idsAllowedList = new ArrayList<String>();
        Iterator<Workgroup> idsAllowedIter = idsAllowed.iterator();
        while (idsAllowedIter.hasNext()) {
            String idAllowed = idsAllowedIter.next().getId().toString();
            idsAllowedList.add(idAllowed);
        }
        return idsAllowedList.containsAll(idsAccessingList);
    }

    private ModelAndView handleGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = request.getParameter("type");
        ServletContext servletContext2 = this.getServletContext();
        ServletContext vlewrappercontext = servletContext2.getContext("/vlewrapper");
        User user = ControllerUtil.getSignedInUser();
        CredentialManager.setRequestCredentials(request, user);
        String runIdString = request.getParameter("runId");
        Long runId = null;
        if (runIdString != null) {
            runId = Long.parseLong(runIdString);
        }
        Run run = null;
        try {
            if (runId != null) {
                run = runService.retrieveById(runId);
            }
        } catch (ObjectNotFoundException e1) {
            e1.printStackTrace();
        }
        if (type == null) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/getdata.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("brainstorm")) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/getdata.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("flag") || type.equals("annotation")) {
            setUserInfos(run, request);
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/annotations.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("journal")) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/journaldata.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("peerreview")) {
            String periodString = request.getParameter("periodId");
            Long period = null;
            if (periodString != null) {
                period = Long.parseLong(periodString);
            }
            try {
                Set<Workgroup> classmateWorkgroups = runService.getWorkgroups(runId, period);
                request.setAttribute("numWorkgroups", classmateWorkgroups.size());
            } catch (ObjectNotFoundException e) {
                e.printStackTrace();
            }
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/peerreview.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("xlsexport")) {
            setUserInfos(run, request);
            setProjectPath(run, request);
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/getxls.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("ideaBasket")) {
            handleIdeaBasket(request, response);
        } else if (type.equals("studentAssetManager")) {
            handleStudentAssetManager(request, response);
        } else if (type.equals("viewStudentAssets")) {
            handleViewStudentAssets(request, response);
        } else if (type.equals("xmppAuthenticate")) {
            String isXMPPEnabled = portalProperties.getProperty("isXMPPEnabled");
            if (isXMPPEnabled != null && Boolean.valueOf(isXMPPEnabled)) {
                handleWISEXMPPAuthenticate(request, response);
            }
        }
        return null;
    }

    private ModelAndView handlePost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = request.getParameter("type");
        ServletContext servletContext2 = this.getServletContext();
        ServletContext vlewrappercontext = servletContext2.getContext("/vlewrapper");
        User user = ControllerUtil.getSignedInUser();
        CredentialManager.setRequestCredentials(request, user);
        if (type == null) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/postdata.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("flag") || type.equals("annotation")) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/annotations.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("journal")) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/journaldata.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("peerreview")) {
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/peerreview.html");
            requestDispatcher.forward(request, response);
        } else if (type.equals("ideaBasket")) {
            handleIdeaBasket(request, response);
        } else if (type.equals("studentAssetManager")) {
            handleStudentAssetManager(request, response);
        } else if (type.equals("viewStudentAssets")) {
            handleViewStudentAssets(request, response);
        }
        return null;
    }

    private void handleIdeaBasket(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext servletContext2 = this.getServletContext();
        ServletContext vlewrappercontext = servletContext2.getContext("/vlewrapper");
        User user = ControllerUtil.getSignedInUser();
        String action = request.getParameter("action");
        try {
            if (action.equals("getAllIdeaBaskets") && !(user.getUserDetails() instanceof TeacherUserDetails)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to access this page");
            } else {
                String runId = request.getParameter("runId");
                Run run = runService.retrieveById(new Long(runId));
                Project project = run.getProject();
                Serializable projectId = project.getId();
                request.setAttribute("projectId", projectId + "");
                if (!user.isAdmin()) {
                    List<Workgroup> workgroupListByOfferingAndUser = workgroupService.getWorkgroupListByOfferingAndUser(run, user);
                    Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
                    Long workgroupId = workgroup.getId();
                    request.setAttribute("workgroupId", workgroupId + "");
                }
                RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/ideaBasket.html");
                requestDispatcher.forward(request, response);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleViewStudentAssets(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext servletContext2 = this.getServletContext();
        ServletContext vlewrappercontext = servletContext2.getContext("/vlewrapper");
        User user = ControllerUtil.getSignedInUser();
        String studentuploads_base_dir = portalProperties.getProperty("studentuploads_base_dir");
        try {
            String runId = request.getParameter("runId");
            Run run = runService.retrieveById(new Long(runId));
            Project project = run.getProject();
            Serializable projectId = project.getId();
            request.setAttribute("projectId", projectId + "");
            if (studentuploads_base_dir != null) {
                request.setAttribute("studentuploads_base_dir", studentuploads_base_dir);
            }
            String workgroups = request.getParameter("workgroups");
            request.setAttribute("dirName", workgroups);
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/vle/studentassetmanager.html");
            requestDispatcher.forward(request, response);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleStudentAssetManager(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext servletContext2 = this.getServletContext();
        ServletContext vlewrappercontext = servletContext2.getContext("/vlewrapper");
        User user = ControllerUtil.getSignedInUser();
        String studentuploads_base_dir = portalProperties.getProperty("studentuploads_base_dir");
        try {
            String runId = request.getParameter("runId");
            Run run = runService.retrieveById(new Long(runId));
            Project project = run.getProject();
            Serializable projectId = project.getId();
            request.setAttribute("projectId", projectId + "");
            List<Workgroup> workgroupListByOfferingAndUser = workgroupService.getWorkgroupListByOfferingAndUser(run, user);
            Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
            Long workgroupId = workgroup.getId();
            request.setAttribute("dirName", workgroupId + "");
            if (studentuploads_base_dir != null) {
                request.setAttribute("studentuploads_base_dir", studentuploads_base_dir);
            }
            RequestDispatcher requestDispatcher = vlewrappercontext.getRequestDispatcher("/vle/studentassetmanager.html");
            requestDispatcher.forward(request, response);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    class XMPPCreateUserRestCommand extends AbstractHttpRestCommand {

        String runId;

        String workgroupId;

        /**
		 * Create the MD5 hashed password for the xmpp ejabberd user
		 * @param workgroupIdString
		 * @param runIdString
		 * @return
		 */
        private String generateUniqueIdMD5(String workgroupIdString, String runIdString) {
            String passwordUnhashed = workgroupIdString + "-" + runIdString;
            MessageDigest m = null;
            try {
                m = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            m.update(passwordUnhashed.getBytes(), 0, passwordUnhashed.length());
            String uniqueIdMD5 = new BigInteger(1, m.digest()).toString(16);
            return uniqueIdMD5;
        }

        public JSONObject run() {
            String username = workgroupId;
            String password = generateUniqueIdMD5(workgroupId, runId);
            String xmppServerBaseUrl = portalProperties.getProperty("xmppServerBaseUrl");
            String xmppServerHostName = ControllerUtil.getHostNameFromUrl(xmppServerBaseUrl);
            String bodyData = "register \"" + username + "\" \"" + xmppServerHostName + "\" \"" + password + "\"";
            HttpPostRequest httpPostRequestData = new HttpPostRequest(REQUEST_HEADERS_CONTENT, EMPTY_STRING_MAP, bodyData, "/rest", HttpStatus.SC_OK);
            try {
                this.transport.post(httpPostRequestData);
            } catch (HttpStatusCodeException e) {
            }
            JSONObject xmppUserObject = new JSONObject();
            try {
                xmppUserObject.put("xmppUsername", username);
                xmppUserObject.put("xmppPassword", password);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return xmppUserObject;
        }

        /**
		 * @param runId the runId to set
		 */
        public void setRunId(String runId) {
            this.runId = runId;
        }

        /**
		 * @param workgroupId the workgroupId to set
		 */
        public void setWorkgroupId(String workgroupId) {
            this.workgroupId = workgroupId;
        }
    }

    private void handleWISEXMPPAuthenticate(HttpServletRequest request, HttpServletResponse response) {
        String xmppServerBaseUrl = portalProperties.getProperty("xmppServerBaseUrl");
        if (xmppServerBaseUrl == null) {
            return;
        }
        XMPPCreateUserRestCommand restCommand = new XMPPCreateUserRestCommand();
        String runId = request.getParameter("runId");
        String workgroupId = request.getParameter("workgroupId");
        restCommand.setRunId(runId);
        restCommand.setWorkgroupId(workgroupId);
        HttpRestTransportImpl restTransport = new HttpRestTransportImpl();
        restTransport.setBaseUrl(xmppServerBaseUrl);
        restCommand.setTransport(restTransport);
        JSONObject xmppUserObject = restCommand.run();
        try {
            response.getWriter().print(xmppUserObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sets the classmate, teacher and shared teacher user infos
	 * into the request object so they can be retrieved by the
	 * vlewrapper servlets
	 * @param run
	 * @param request
	 */
    private void setUserInfos(Run run, HttpServletRequest request) {
        JSONObject myUserInfoJSONObject = RunUtil.getMyUserInfo(run, workgroupService);
        JSONArray classmateUserInfosJSONArray = RunUtil.getClassmateUserInfos(run, workgroupService, runService);
        JSONObject teacherUserInfoJSONObject = RunUtil.getTeacherUserInfo(run, workgroupService);
        JSONArray sharedTeacherUserInfosJSONArray = RunUtil.getSharedTeacherUserInfos(run, workgroupService);
        JSONObject runInfoJSONObject = RunUtil.getRunInfo(run);
        request.setAttribute("myUserInfo", myUserInfoJSONObject.toString());
        request.setAttribute("classmateUserInfos", classmateUserInfosJSONArray.toString());
        request.setAttribute("teacherUserInfo", teacherUserInfoJSONObject.toString());
        request.setAttribute("sharedTeacherUserInfos", sharedTeacherUserInfosJSONArray.toString());
        request.setAttribute("runInfo", runInfoJSONObject.toString());
        List<StudentAttendance> studentAttendanceList = studentAttendanceService.getStudentAttendanceByRunId(run.getId());
        JSONArray studentAttendanceJSONArray = new JSONArray();
        for (int x = 0; x < studentAttendanceList.size(); x++) {
            StudentAttendance studentAttendance = studentAttendanceList.get(x);
            JSONObject studentAttendanceJSONObj = studentAttendance.toJSONObject();
            studentAttendanceJSONArray.put(studentAttendanceJSONObj);
        }
        request.setAttribute("studentAttendance", studentAttendanceJSONArray.toString());
    }

    /**
	 * Set the project path into the request as an attribute so that we can access
	 * it in other controllers
	 * @param run
	 * @param request
	 */
    private void setProjectPath(Run run, HttpServletRequest request) {
        String curriculumBaseDir = portalProperties.getProperty("curriculum_base_dir");
        String rawProjectUrl = (String) run.getProject().getCurnit().accept(new CurnitGetCurnitUrlVisitor());
        String projectPath = curriculumBaseDir + rawProjectUrl;
        request.setAttribute("projectPath", projectPath);
    }

    /**
	 * @return the workgroupService
	 */
    public WISEWorkgroupService getWorkgroupService() {
        return workgroupService;
    }

    /**
	 * @param workgroupService the workgroupService to set
	 */
    public void setWorkgroupService(WISEWorkgroupService workgroupService) {
        this.workgroupService = workgroupService;
    }

    public RunService getRunService() {
        return runService;
    }

    public void setRunService(RunService runService) {
        this.runService = runService;
    }

    /**
	 * @param portalProperties the portalProperties to set
	 */
    public void setPortalProperties(Properties portalProperties) {
        this.portalProperties = portalProperties;
    }

    /**
	 * 
	 * @return
	 */
    public StudentAttendanceService getStudentAttendanceService() {
        return studentAttendanceService;
    }

    /**
	 * 
	 * @param studentAttendanceService
	 */
    public void setStudentAttendanceService(StudentAttendanceService studentAttendanceService) {
        this.studentAttendanceService = studentAttendanceService;
    }
}
