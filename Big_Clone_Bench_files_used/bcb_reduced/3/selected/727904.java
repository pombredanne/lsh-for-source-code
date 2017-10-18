package fr.insa.rennes.pelias.pexecutor;

import com.sun.rave.web.ui.appbase.AbstractPageBean;
import com.sun.webui.jsf.component.Message;
import com.sun.webui.jsf.component.PasswordField;
import com.sun.webui.jsf.component.StaticText;
import fr.insa.rennes.pelias.pexecutor.login.LoginBean;
import fr.insa.rennes.pelias.pexecutor.login.Password;
import fr.insa.rennes.pelias.pexecutor.validators.AlphaNumValidator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * <p>Page bean that corresponds to a similarly named JSP page.  This
 * class contains component definitions (and initialization code) for
 * all components that you have defined on this page, as well as
 * lifecycle methods and event handlers where you may add behavior
 * to respond to incoming events.</p>
 *
 * @version Config.java
 * @version Created on 24 févr. 2009, 20:38:14
 * @author Ju
 */
public class Config extends AbstractPageBean {

    /**
     * <p>Automatically managed component initialization.  <strong>WARNING:</strong>
     * This method is automatically generated, so any user-specified code inserted
     * here is subject to being replaced.</p>
     */
    private void _init() throws Exception {
    }

    private String oldPassword;

    private String newPassword;

    private String confirmationPassword;

    private StaticText errorMessage = new StaticText();

    public StaticText getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(StaticText st) {
        this.errorMessage = st;
    }

    private PasswordField oldPasswordField = new PasswordField();

    public PasswordField getOldPasswordField() {
        return oldPasswordField;
    }

    public void setOldPasswordField(PasswordField pf) {
        this.oldPasswordField = pf;
    }

    private PasswordField newPasswordField = new PasswordField();

    public PasswordField getNewPasswordField() {
        return newPasswordField;
    }

    public void setNewPasswordField(PasswordField pf) {
        this.newPasswordField = pf;
    }

    private PasswordField confirmationPasswordField = new PasswordField();

    public PasswordField getConfirmationPasswordField() {
        return confirmationPasswordField;
    }

    public void setConfirmationPasswordField(PasswordField pf) {
        this.confirmationPasswordField = pf;
    }

    private Message messageOldPassword = new Message();

    public Message getMessageOldPassword() {
        return messageOldPassword;
    }

    public void setMessageOldPassword(Message m) {
        this.messageOldPassword = m;
    }

    /**
     * <p>Construct a new Page bean instance.</p>
     */
    public Config() {
    }

    /**
     * <p>Callback method that is called whenever a page is navigated to,
     * either directly via a URL, or indirectly via page navigation.
     * Customize this method to acquire resources that will be needed
     * for event handlers and lifecycle methods, whether or not this
     * page is performing post back processing.</p>
     *
     * <p>Note that, if the current request is a postback, the property
     * values of the components do <strong>not</strong> represent any
     * values submitted with this request.  Instead, they represent the
     * property values that were saved for this view when it was rendered.</p>
     */
    @Override
    public void init() {
        super.init();
        try {
            _init();
        } catch (Exception e) {
            log("Config Initialization Failure", e);
            throw e instanceof FacesException ? (FacesException) e : new FacesException(e);
        }
    }

    /**
     * <p>Callback method that is called after the component tree has been
     * restored, but before any event processing takes place.  This method
     * will <strong>only</strong> be called on a postback request that
     * is processing a form submit.  Customize this method to allocate
     * resources that will be required in your event handlers.</p>
     */
    @Override
    public void preprocess() {
    }

    /**
     * <p>Callback method that is called just before rendering takes place.
     * This method will <strong>only</strong> be called for the page that
     * will actually be rendered (and not, for example, on a page that
     * handled a postback and then navigated to a different page).  Customize
     * this method to allocate resources that will be required for rendering
     * this page.</p>
     */
    @Override
    public void prerender() {
    }

    /**
     * <p>Callback method that is called after rendering is completed for
     * this request, if <code>init()</code> was called (regardless of whether
     * or not this was the page that was actually rendered).  Customize this
     * method to release resources acquired in the <code>init()</code>,
     * <code>preprocess()</code>, or <code>prerender()</code> methods (or
     * acquired during execution of an event handler).</p>
     */
    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * <p>Return a reference to the scoped data bean.</p>
     *
     * @return reference to the scoped data bean
     */
    protected ApplicationBean1 getApplicationBean1() {
        return (ApplicationBean1) getBean("ApplicationBean1");
    }

    /**
     * <p>Return a reference to the scoped data bean.</p>
     *
     * @return reference to the scoped data bean
     */
    protected RequestBean1 getRequestBean1() {
        return (RequestBean1) getBean("RequestBean1");
    }

    /**
     * <p>Return a reference to the scoped data bean.</p>
     *
     * @return reference to the scoped data bean
     */
    protected SessionBean1 getSessionBean1() {
        return (SessionBean1) getBean("SessionBean1");
    }

    /**
     * Validateur pour le champ password
     * @param context
     * @param component
     * @param value
     */
    public void passwordField_validate(FacesContext context, UIComponent component, Object value) {
        AlphaNumValidator pass = new AlphaNumValidator(value);
        pass.Validate();
    }

    /**
     * Handler lors d'un clique sur le bouton valider
     * @return la chaine utile à la navigation
     */
    public String validate_action() {
        if (getOldPassword() == null) {
            errorMessage.setText("ancien mot de passe erroné");
            return null;
        }
        if (getNewPassword() == null) {
            errorMessage.setText("le nouveau mot de passe ne peut pas être vide");
            return null;
        }
        LoginBean login = (LoginBean) getBean("LoginBean");
        login.setPassword(oldPassword);
        Map<String, String> passwords = login.getPasswords();
        if (login.validate()) {
            if (getNewPassword().equals(getConfirmationPassword())) {
                try {
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    byte[] encoded = messageDigest.digest(newPassword.getBytes());
                    String code = "";
                    for (int i = 0; i < encoded.length; i++) {
                        code += encoded[i];
                    }
                    passwords.put(login.getId(), code);
                    errorMessage.setText("Mot de passe changé avec succès");
                    login.setPasswords(passwords);
                    new Password().savePasswords(passwords);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, null, ex);
                    return "null";
                }
            } else {
                errorMessage.setText("Les deux mots de passe ne sont pas identiques");
            }
        } else {
            errorMessage.setText("ancien mot de passe erroné");
        }
        return "null";
    }

    /**
     * @return the oldPassword
     */
    public String getOldPassword() {
        return oldPassword;
    }

    /**
     * @param oldPassword the oldPassword to set
     */
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    /**
     * @return the newPassword
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * @param newPassword the newPassword to set
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    /**
     * @return the confirmationPassword
     */
    public String getConfirmationPassword() {
        return confirmationPassword;
    }

    /**
     * @param confirmationPassword the confirmationPassword to set
     */
    public void setConfirmationPassword(String confirmationPassword) {
        this.confirmationPassword = confirmationPassword;
    }
}
