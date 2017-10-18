package org.yournamehere.client.init;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.KeyPressEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyPressHandler;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.yournamehere.client.ScrumManagerPanel;
import org.yournamehere.client.ServerException;

/**
 * Generate a pannel with the tree of themes, stories and tasks.
 * @author David
 */
public class InitPanel extends ScrumManagerPanel {

    private ButtonItem button = new ButtonItem("Accept", constants.accept());

    /**
     * Constructor of the class.
     */
    public InitPanel() throws ServerException {
        super();
        setSize("50%", "50%");
        init();
    }

    /**
     * Start the panel.
     */
    private void init() {
        createWindowCenter(windowPrincipal, 380, 120, constants.authentication());
        windowPrincipal.addItem(addForm());
        windowPrincipal.addItem(errorMessage);
        windowPrincipal.addCloseClickHandler(new CloseClickHandler() {

            public void onCloseClick(CloseClientEvent event) {
                windowPrincipal.destroy();
            }
        });
        windowPrincipal.show();
    }

    /**
     * Create a form that asks the user for credentials
     * @return
     */
    private DynamicForm addForm() {
        final DynamicForm form = new DynamicForm();
        form.setAlign(Alignment.CENTER);
        form.setAutoFocus(true);
        form.setNumCols(2);
        form.setMargin(10);
        final TextItem usernameItem = new TextItem("fName");
        usernameItem.setTitle(constants.username());
        usernameItem.setRequired(true);
        usernameItem.setSelectOnFocus(true);
        usernameItem.setWrapTitle(false);
        usernameItem.setColSpan(1);
        usernameItem.setStartRow(false);
        usernameItem.setEndRow(true);
        PasswordItem passwordItem = new PasswordItem("fPassword");
        passwordItem.setTitle(constants.password());
        passwordItem.setRequired(true);
        passwordItem.setWrapTitle(false);
        passwordItem.setColSpan(1);
        passwordItem.setStartRow(false);
        passwordItem.setEndRow(true);
        passwordItem.addKeyPressHandler(new KeyPressHandler() {

            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getKeyName().equals("Enter")) {
                    clicked(form);
                }
            }
        });
        button = createButton();
        button.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                clicked(form);
            }
        });
        form.setFields(usernameItem, passwordItem, button);
        return form;
    }

    private void clicked(DynamicForm form) {
        if (form.getValueAsString("fPassword").equals("null")) {
            showError(385, 165, constants.errorPass(), windowPrincipal, ERROR);
        } else {
            try {
                String cifrado = encryptPassword(form.getValueAsString("fPassword"));
                checkPassword(form.getValueAsString("fName"), cifrado);
            } catch (NoSuchAlgorithmException ex) {
                init();
            }
        }
    }

    /**
     * Encrypt the password received.
     * @param password
     * @return
     * @throws NoSuchAlgorithmException
     */
    private String encryptPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest encript = MessageDigest.getInstance("MD5");
        encript.update(password.getBytes());
        byte[] b = encript.digest();
        int size = b.length;
        StringBuffer h = new StringBuffer(size);
        for (int i = 0; i < size; i++) {
            h.append(b[i]);
        }
        return h.toString();
    }

    /**
     * Checks the credentials received.
     * @param username
     * @param password
     */
    private void checkPassword(final String username, final String password) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                checkPassword(username, password);
            }

            public void onSuccess(Object result) {
                Boolean access = (Boolean) result;
                if (access) {
                    try {
                        windowPrincipal.destroy();
                        RootPanel.get().add(new ElectionPanel(username));
                    } catch (ServerException ex) {
                        init();
                    }
                } else {
                    showError(385, 165, constants.incorrectPass(), windowPrincipal, ERROR);
                }
            }
        };
        getService().checkPassword(username, password, callback);
    }
}
