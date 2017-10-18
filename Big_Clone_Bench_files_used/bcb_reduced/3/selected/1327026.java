package org.yournamehere.client.admin;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.*;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.yournamehere.client.ScrumManagerPanel;
import org.yournamehere.client.ServerException;

/**
 * Generate a pannel with the users list.
 * @author David
 */
public class AdminUserPanel extends ScrumManagerPanel {

    private Window windowPerson = new Window();

    private Menu menu = new Menu();

    private ListGrid personGrid = new ListGrid();

    private static LinkedHashMap<String, String> personMap = new LinkedHashMap<String, String>();

    private static List<String> drawn = new ArrayList<String>();

    private final int widthPersonWin = 502;

    private final int heightPersonWin = 185;

    /**
     * constructor of the class.
     */
    public AdminUserPanel(Canvas canvasPrincipal) throws ServerException {
        super();
        this.canvasPrincipal = canvasPrincipal;
        setSize("85%", "85%");
        init();
    }

    /**
     * start the panel. Create the window, and load it of information.
     */
    private void init() {
        createPrincipalWindow();
        loadPersons();
    }

    /**
     * configure the principal window by adding the grid indicated.
     */
    private void createPrincipalWindow() {
        createWindowCenter(windowPrincipal, 880, 530, constants.personMenu());
        windowPrincipal.addItem(createGridPersons());
        configurePrincipalWindow();
        windowPrincipal.addCloseClickHandler(new CloseClickHandler() {

            public void onCloseClick(CloseClientEvent event) {
                windowPrincipal.destroy();
            }
        });
        canvasPrincipal.addChild(windowPrincipal);
        canvasPrincipal.show();
    }

    /**
     * create a grid with the appropriate structure.
     * @return
     */
    private Canvas createGridPersons() {
        Canvas canvas = new Canvas();
        personGrid.setWidth100();
        personGrid.setHeight100();
        personGrid.setEmptyCellValue("-");
        personGrid.setDataSource(GridFieldAdmin.getInstance());
        ListGridField nameField = new ListGridField("Name", constants.name());
        ListGridField groupField = new ListGridField("Group", constants.group());
        ListGridField activeField = new ListGridField("Active", constants.active());
        personGrid.setFields(nameField, groupField, activeField);
        personGrid.setSortField(0);
        personGrid.setDataPageSize(50);
        personGrid.setAutoFetchData(true);
        popupPersons();
        personGrid.setContextMenu(menu);
        personGrid.addRecordClickHandler(new RecordClickHandler() {

            public void onRecordClick(RecordClickEvent event) {
                ListGridRecord record = (ListGridRecord) event.getRecord();
                idItemSelected = record.getAttribute("id");
                popupPersons();
                personGrid.setContextMenu(menu);
            }
        });
        personGrid.addRowContextClickHandler(new RowContextClickHandler() {

            @Override
            public void onRowContextClick(RowContextClickEvent event) {
                ListGridRecord record = (ListGridRecord) event.getRecord();
                idItemSelected = record.getAttribute("id");
                popupPersons();
                personGrid.setContextMenu(menu);
            }
        });
        personGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {

            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
                ListGridRecord record = (ListGridRecord) event.getRecord();
                idItemSelected = record.getAttribute("id");
                popupPersons();
                personGrid.setContextMenu(menu);
                managePerson(idItemSelected, SHOW);
            }
        });
        canvas.addChild(personGrid);
        return canvas;
    }

    /**
     * Configure the menu that will be the context menu.
     */
    private void popupPersons() {
        menu.clear();
        menu = new Menu();
        MenuItem personItem = new MenuItem(constants.addPerson(), "person.png");
        com.smartgwt.client.widgets.menu.events.ClickHandler aux_pers = new com.smartgwt.client.widgets.menu.events.ClickHandler() {

            @Override
            public void onClick(MenuItemClickEvent event) {
                createPersonForm();
            }
        };
        personItem.addClickHandler(aux_pers);
        if (!idItemSelected.equals("")) {
            MenuItem detailsItem = new MenuItem(constants.viewPerson(), "info.png");
            com.smartgwt.client.widgets.menu.events.ClickHandler aux_details = new com.smartgwt.client.widgets.menu.events.ClickHandler() {

                @Override
                public void onClick(MenuItemClickEvent event) {
                    managePerson(idItemSelected, SHOW);
                }
            };
            detailsItem.addClickHandler(aux_details);
            MenuItemSeparator separator = new MenuItemSeparator();
            MenuItem deleteItem = new MenuItem(constants.delete(), "cancel.png");
            com.smartgwt.client.widgets.menu.events.ClickHandler aux_delete_theme = new com.smartgwt.client.widgets.menu.events.ClickHandler() {

                @Override
                public void onClick(MenuItemClickEvent event) {
                    if (com.google.gwt.user.client.Window.confirm(constants.sureDeletePerson())) {
                        deletePerson(idItemSelected);
                        personGrid.removeSelectedData();
                        personMap.remove(idItemSelected);
                    }
                }
            };
            deleteItem.addClickHandler(aux_delete_theme);
            MenuItem modifyItem = new MenuItem(constants.modify(), "configure.png");
            com.smartgwt.client.widgets.menu.events.ClickHandler modify_handler = new com.smartgwt.client.widgets.menu.events.ClickHandler() {

                @Override
                public void onClick(MenuItemClickEvent event) {
                    managePerson(idItemSelected, MODIFY);
                }
            };
            modifyItem.addClickHandler(modify_handler);
            menu.setVisible(false);
            menu.addItem(personItem);
            menu.addItem(detailsItem);
            menu.addItem(separator);
            menu.addItem(deleteItem);
            menu.addItem(modifyItem);
        } else {
            menu.setVisible(false);
            menu.addItem(personItem);
        }
    }

    /**
     * delete a given person.
     * @param id
     */
    private void deletePerson(final String id) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                deletePerson(id);
            }

            public void onSuccess(Object result) {
            }
        };
        getService().deletePerson(id, callback);
    }

    /**
     * retrieves information from a given person.
     * @param id
     * @param action
     */
    public void managePerson(final String id, final int action) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                managePerson(id, action);
            }

            public void onSuccess(Object result) {
                String name = (String) result;
                if (name == null) {
                    managePerson(id, action);
                } else {
                    managePerson(id, name, action);
                }
            }
        };
        getService().getName(id, PERSONS, callback);
    }

    private void managePerson(final String id, final String name, final int action) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                managePerson(id, name, action);
            }

            public void onSuccess(Object result) {
                String id_group = (String) result;
                if ((id_group == null) || (id_group.equals("0"))) managePerson(id, name, id_group, "-", action); else managePerson(id, name, id_group, action);
            }
        };
        getService().getFatherPerson(id, callback);
    }

    private void managePerson(final String id, final String name, final String id_group, final int action) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                managePerson(id, name, id_group, action);
            }

            public void onSuccess(Object result) {
                String group = (String) result;
                if (group == null) managePerson(id, name, id_group, action); else managePerson(id, name, id_group, group, action);
            }
        };
        getService().getName(id_group, PERSONS, callback);
    }

    private void managePerson(final String id, final String name, final String id_group, final String group, final int action) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                managePerson(id, name, id_group, group, action);
            }

            public void onSuccess(Object result) {
                Boolean stored = (Boolean) result;
                if (stored == null) managePerson(id, name, id_group, group, action); else {
                    if (action == SHOW) {
                        new UserPanel(name, group, stored, canvasPrincipal);
                    } else if (action == MODIFY) modifyPersonForm(id, name, id_group, group, stored); else if (action == LOADPRINCIPAL) addInformationPerson(id, name, group, stored);
                }
            }
        };
        getService().getPersonStored(id, callback);
    }

    /**
     * adds the information given to the grid.
     * @param id
     * @param name
     * @param group
     * @param stored
     */
    private void addInformationPerson(String id, String name, String group, Boolean stored) {
        if (!exist(id)) {
            ListGridRecord rec = new ListGridRecord();
            rec.setAttribute("Name", name);
            rec.setAttribute("Group", group);
            rec.setAttribute("Active", stored);
            rec.setAttribute("id", id);
            personMap.put(id, "<b>" + name + "</b>");
            drawn.add(id);
            personGrid.addData(rec);
        }
    }

    /**
     * create a window with a empty form. Will add a new person.
     */
    private void createPersonForm() {
        windowPerson = new Window();
        createWindowCenter(windowPerson, widthPersonWin, heightPersonWin, constants.person());
        windowPerson.addItem(addFormPerson("[" + constants.name() + "]", "-", "-", true, ADD));
        configureFormWindow(windowPerson);
        errorMessage.setVisible(false);
        windowPerson.addItem(errorMessage);
    }

    /**
     * create a window with a completed form with the information given. Will modify a person
     * @param idItemSelected
     * @param name
     * @param group
     * @param stored
     */
    private void modifyPersonForm(String idItemSelected, String name, String id_group, String group, Boolean stored) {
        windowPerson = new Window();
        createWindowCenter(windowPerson, widthPersonWin, heightPersonWin, constants.modifyPerson());
        windowPerson.addItem(addFormPerson(name, id_group, group, stored, MODIFY));
        configureFormWindow(windowPerson);
        errorMessage.setVisible(false);
        windowPerson.addItem(errorMessage);
    }

    /**
     * creates a form with the information received
     * @param name
     * @param group
     * @param store
     * @param action
     * @return
     */
    private DynamicForm addFormPerson(String name, String id_group, String group, boolean store, final int action) {
        final DynamicForm form = configureForm(430, 85);
        final TextItem nameItem = new TextItem("fName");
        configureFormsItems(nameItem, 250, 5, constants.namePerson(), name);
        SelectItem groupItem = new SelectItem("fGroup");
        groupItem.setTitle(constants.group());
        groupItem.setWidth(250);
        personMap.put("-", "-");
        groupItem.setValueMap(personMap);
        groupItem.setDefaultToFirstOption(true);
        if (id_group.equals("0")) groupItem.setDefaultValue("-"); else groupItem.setDefaultValue(id_group);
        CheckboxItem activeItem = new CheckboxItem("fActive");
        configureFormsCheckboxItem(activeItem, 50, constants.active(), store);
        if (action == SHOW) form.setFields(nameItem, groupItem, activeItem); else {
            final PasswordItem passwordItem = new PasswordItem("fPassword");
            if (action == MODIFY) passwordItem.setTitle(constants.newPassword() + "\n"); else if (action == ADD) passwordItem.setTitle(constants.password());
            passwordItem.setSelectOnFocus(true);
            passwordItem.setWrapTitle(false);
            passwordItem.setWidth(250);
            ButtonItem button = createButton();
            button.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {

                @Override
                public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                    if (action == MODIFY) {
                        try {
                            String cifrado = "";
                            if (!form.getValueAsString("fPassword").equals("null")) {
                                cifrado = encryptPassword(form.getValueAsString("fPassword"));
                            }
                            String id_group = form.getValueAsString("fGroup");
                            if (id_group.equals("-")) modifyPerson(form.getValueAsString("fName"), "0", "-", (Boolean) form.getValue("fActive"), cifrado, idItemSelected); else modifyPerson(form.getValueAsString("fName"), id_group, (Boolean) form.getValue("fActive"), cifrado, idItemSelected);
                        } catch (NoSuchAlgorithmException ex) {
                            showError(widthPersonWin, heightPersonWin + 35, constants.errorPassword(), windowPerson, ERROR);
                        } catch (Exception ex) {
                            showError(widthPersonWin, heightPersonWin + 35, constants.errorModifyUser(), windowPerson, ERROR);
                        }
                    } else if (action == ADD) if (form.getValueAsString("fPassword").equals("null")) {
                        showError(widthPersonWin, heightPersonWin + 35, constants.advicePassword(), windowPerson, ERROR);
                    } else {
                        try {
                            String cifrado = encryptPassword(form.getValueAsString("fPassword"));
                            String id_group = form.getValueAsString("fGroup");
                            if (id_group.equals("-")) addPerson(form.getValueAsString("fName"), "0", "-", (Boolean) form.getValue("fActive"), cifrado); else addPerson(form.getValueAsString("fName"), id_group, (Boolean) form.getValue("fActive"), cifrado);
                            restartWinModal(windowPerson);
                            errorMessage.setVisible(false);
                        } catch (NoSuchAlgorithmException ex) {
                            showError(widthPersonWin, heightPersonWin + 35, constants.errorPassword(), windowPerson, ERROR);
                        }
                    }
                }
            });
            form.setFields(nameItem, passwordItem, groupItem, activeItem, button);
        }
        return form;
    }

    /**
     * modify a person
     * @param name
     * @param id_group
     * @param stored
     * @param password
     * @param id
     */
    private void modifyPerson(final String name, final String id_group, final boolean stored, final String password, final String id) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                modifyPerson(name, id_group, stored, password, id);
            }

            public void onSuccess(Object result) {
                String group = (String) result;
                if ((group == null) || (id_group == id)) {
                    showError(widthPersonWin, heightPersonWin + 35, constants.errorGroup(), windowPerson, ERROR);
                } else {
                    modifyPerson(name, id_group, group, stored, password, id);
                }
            }
        };
        getService().getName(id_group, PERSONS, callback);
    }

    private void modifyPerson(final String name, final String id_group, final String group, final boolean stored, final String password, final String id) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                modifyPerson(name, id_group, group, stored, password, id);
            }

            public void onSuccess(Object result) {
                Boolean ok = (Boolean) result;
                if (ok) {
                    personGrid.removeSelectedData();
                    personMap.remove(id);
                    drawn.remove(id);
                    addInformationPerson(id, name, group, stored);
                    restartWinModal(windowPerson);
                    errorMessage.setVisible(false);
                } else {
                    SC.say(constants.errorModifyUser());
                }
            }
        };
        getService().modifyPerson(id, name, id_group, stored, password, callback);
    }

    /**
     * creates a person
     * @param name
     * @param id_group
     * @param stored
     * @param password
     */
    private void addPerson(final String name, final String id_group, final boolean stored, final String password) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                addPerson(name, id_group, stored, password);
            }

            public void onSuccess(Object result) {
                String group = (String) result;
                if (group == null) {
                    showError(widthPersonWin, heightPersonWin + 35, constants.errorGroup(), windowPerson, ERROR);
                } else {
                    addPerson(name, id_group, group, stored, password);
                }
            }
        };
        getService().getName(id_group, PERSONS, callback);
    }

    private void addPerson(final String name, final String id_group, final String group, final boolean stored, final String password) {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                addPerson(name, id_group, group, stored, password);
            }

            public void onSuccess(Object result) {
                String id = (String) result;
                if ((id == null) || (id.equals("0"))) com.google.gwt.user.client.Window.confirm(constants.errorName()); else addInformationPerson(id, name, group, stored);
            }
        };
        getService().addPerson(name, id_group, stored, password, callback);
    }

    /**
     * retrieves a list of people that should appear in the grid.
     */
    private void loadPersons() {
        AsyncCallback callback = new AsyncCallback() {

            public void onFailure(Throwable arg0) {
                loadPersons();
            }

            public void onSuccess(Object result) {
                List<String> list = (List<String>) result;
                String id;
                Iterator iterThemes = list.iterator();
                while (iterThemes.hasNext()) {
                    id = (String) iterThemes.next();
                    managePerson(id, LOADPRINCIPAL);
                }
            }
        };
        getService().getPersons(callback);
    }

    /**
     * encrypt the password received.
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
     * check if a id has been drawn in the grid.
     * @param id
     * @return
     */
    private boolean exist(String id) {
        return drawn.contains(id);
    }
}
