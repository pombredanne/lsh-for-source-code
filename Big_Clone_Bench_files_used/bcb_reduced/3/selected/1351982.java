package org.openeye.jbpm;

import org.hibernate.validator.Email;
import org.hibernate.validator.NotEmpty;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.log.Log;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.Token;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.SeamResourceBundle;
import org.openeye.console.TaskForm;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.Serializable;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.jboss.seam.util.Hex;

@Name("customerService")
@Scope(ScopeType.SESSION)
public class CustomerService implements Serializable {

    private static final long serialVersionUID = 1487558952791439917L;

    @Logger
    private static Log log;

    @In
    private JbpmServiceLocal jbpmService;

    @In
    private FacesMessages facesMessages;

    @In(create = true)
    private AttachmentHandler attachmentHandler;

    @Out(required = false)
    private TaskForm unprotectedTaskForm = null;

    @Out(required = false)
    private List<ProcessInstance> customerProcessInstances = null;

    @In
    private FacesContext facesContext;

    @NotEmpty
    private String ticket;

    private String contactInfo;

    @NotEmpty
    private String title;

    @NotEmpty
    private String description;

    private String actorId;

    private String customerId;

    @NotEmpty
    private String searchString;

    @NotEmpty
    private String comment;

    @Email
    @NotEmpty
    private String email;

    private String url;

    private boolean sendEmailConfirmation = false;

    public void find() {
        unprotectedTaskForm = null;
        customerProcessInstances = null;
        if ((searchString == null) || searchString.isEmpty()) {
            return;
        } else {
            ProcessInstance processInstance = jbpmService.getProcessInstanceByTicket(searchString);
            if (processInstance != null) {
                unprotectedTaskForm = setupTaskForm(processInstance);
                return;
            } else {
                customerProcessInstances = jbpmService.getProcessInstancesByEmail(searchString);
                if ((customerProcessInstances != null) && customerProcessInstances.isEmpty()) customerProcessInstances = null;
                return;
            }
        }
    }

    public void createTicket() {
        unprotectedTaskForm = null;
        ticket = "";
        if ((title == null) || (contactInfo == null) || (description == null) || (email == null)) {
            return;
        } else {
            try {
                ProcessInstance processInstance = jbpmService.startProcess("Request Fulfillment");
                processInstance.setKey(title);
                processInstance.getContextInstance().setVariable(SeamResourceBundle.getBundle().getString("openeye.description"), description);
                processInstance.getContextInstance().setVariable(SeamResourceBundle.getBundle().getString("openeye.createdBy"), email);
                processInstance.getContextInstance().setVariable("Assignee", email);
                processInstance.getContextInstance().setVariable(SeamResourceBundle.getBundle().getString("openeye.email"), email);
                processInstance.getContextInstance().setVariable(SeamResourceBundle.getBundle().getString("openeye.contactInfo"), contactInfo);
                ticket = generateTicketId();
                processInstance.getContextInstance().setVariable(SeamResourceBundle.getBundle().getString("openeye.ticket"), ticket);
                TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance().getTaskInstances().iterator().next();
                if ((actorId != null) && !actorId.isEmpty()) {
                    taskInstance.setActorId(actorId);
                }
                String message = SeamResourceBundle.getBundle().getString("openeye.processInstanceCreated") + " - Id: " + processInstance.getId() + " key: " + processInstance.getKey();
                facesMessages.add(message);
            } finally {
            }
            if (isSendEmailConfirmation()) {
                try {
                    getServerURL();
                    if ((url != null) && !url.isEmpty()) {
                        Events.instance().raiseEvent("serviceRequestConfirmation");
                    }
                } catch (Exception exc) {
                    facesMessages.add("Send email failed because no SMTP server was found!");
                }
            }
            contactInfo = "";
            title = "";
            description = "";
            customerId = "";
            actorId = "";
            searchString = "";
            email = "";
        }
        log.info("Exiting: createTicket");
    }

    @SuppressWarnings("unchecked")
    private TaskForm setupTaskForm(ProcessInstance processInstance) {
        TaskForm unprotectedTaskForm = null;
        facesMessages.add("");
        unprotectedTaskForm = new TaskForm();
        TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance().getTaskInstances().iterator().next();
        unprotectedTaskForm.setTaskInstance(taskInstance);
        unprotectedTaskForm.setProcessInstance(processInstance);
        ProcessDefinition processDefinition = processInstance.getProcessDefinition();
        if (processDefinition != null) unprotectedTaskForm.setProcessDefinition(processDefinition);
        Token rootToken = processInstance.getRootToken();
        if (rootToken != null) {
            unprotectedTaskForm.setRootToken(rootToken);
        }
        Node node = processInstance.getRootToken().getNode();
        if (node != null) {
            unprotectedTaskForm.setNode(node);
        }
        Map<String, Object> map = taskInstance.getVariables();
        if (map == null) map = new HashMap();
        unprotectedTaskForm.setVariableMap(map);
        Collection<TaskInstance> allProcessTasks = processInstance.getTaskMgmtInstance().getTaskInstances();
        List<Comment> comments = new ArrayList<Comment>();
        for (TaskInstance task : allProcessTasks) {
            comments.addAll(task.getComments());
        }
        unprotectedTaskForm.setComments(comments);
        List<String> attachments = new ArrayList<String>(attachmentHandler.getAttachmentsList(processInstance.getId()).size());
        attachments = attachmentHandler.getAttachmentsList(processInstance.getId());
        if (attachments != null) {
            log.info("Adding attachments, size = " + attachments.size());
            unprotectedTaskForm.setAttachments(attachments);
        }
        return unprotectedTaskForm;
    }

    @SuppressWarnings("unchecked")
    public void addComment() {
        if ((actorId != null) && !actorId.isEmpty() && (unprotectedTaskForm != null) && (unprotectedTaskForm.getTaskInstance() != null)) {
            Comment newComment = new Comment();
            newComment.setMessage(comment);
            newComment.setTime(new Date());
            newComment.setActorId(actorId);
            TaskInstance updateTask = jbpmService.getTaskInstance(unprotectedTaskForm.getTaskInstance().getId());
            updateTask.getComments().add(newComment);
            unprotectedTaskForm.setTaskInstance(updateTask);
            unprotectedTaskForm.setLastComment(newComment);
            Collection<TaskInstance> allProcessTasks = unprotectedTaskForm.getProcessInstance().getTaskMgmtInstance().getTaskInstances();
            List<Comment> comments = new ArrayList<Comment>();
            for (TaskInstance task : allProcessTasks) {
                comments.addAll(task.getComments());
            }
            unprotectedTaskForm.setComments(comments);
        }
    }

    @SuppressWarnings("unused")
    private String getMD5Hash(final String msg) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            return new String(Hex.encodeHex(md5.digest(msg.getBytes("UTF-8"))));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private String generateTicketId() {
        UUID ticket = UUID.randomUUID();
        return ticket.toString();
    }

    private String getServerURL() {
        url = "";
        try {
            ExternalContext extCtxt = facesContext.getExternalContext();
            url = ((javax.servlet.http.HttpServletRequest) extCtxt.getRequest()).getRequestURL().toString();
        } catch (Exception exc) {
        }
        if ((url == null) || url.isEmpty()) url = "";
        return url;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getTicket() {
        return ticket;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setUnprotectedTaskForm(TaskForm unprotectedTaskForm) {
        this.unprotectedTaskForm = unprotectedTaskForm;
    }

    public TaskForm getUnprotectedTaskForm() {
        if (unprotectedTaskForm == null) unprotectedTaskForm = new TaskForm();
        return unprotectedTaskForm;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public String getUrl() {
        if (url == null) url = new String("");
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCustomerProcessInstances(List<ProcessInstance> processInstances) {
        this.customerProcessInstances = processInstances;
    }

    public List<ProcessInstance> getCustomerProcessInstances() {
        return customerProcessInstances;
    }

    public void setSendEmailConfirmation(boolean sendEmailConfirmation) {
        this.sendEmailConfirmation = sendEmailConfirmation;
    }

    public boolean isSendEmailConfirmation() {
        return sendEmailConfirmation;
    }
}
