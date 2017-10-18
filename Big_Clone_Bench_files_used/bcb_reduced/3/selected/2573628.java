package org.monet.modelling.ide.wizards.pages;

import java.security.MessageDigest;
import java.security.Security;
import java.util.UUID;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.monet.modelling.ide.library.LibraryBase64;
import org.monet.modelling.ide.providers.FNVProvider;

public class DefinitionPropertiesPage extends WizardPage implements Listener {

    private String typeDefinition;

    private Text nameText;

    private Text codeText;

    private Combo parentCombo;

    private Composite container_1;

    public DefinitionPropertiesPage() {
        super("");
        setTitle("Attributes and Properties");
    }

    @Override
    public void createControl(Composite parent) {
        container_1 = new Composite(parent, SWT.NULL);
        setControl(container_1);
        GridLayout gl_container_1 = new GridLayout(3, false);
        gl_container_1.verticalSpacing = 10;
        container_1.setLayout(gl_container_1);
        createGroupAttributes(container_1);
        createGroupProperties(container_1);
    }

    public String getPath() {
        GeneralFilePage previousPage = (GeneralFilePage) super.getPreviousPage();
        String filename = previousPage.getFileName();
        return filename;
    }

    public void createGroupAttributes(Composite container) {
        Group grpAttributes = new Group(container, SWT.NONE);
        grpAttributes.setText("Attributes");
        GridLayout gl_grpAttributes = new GridLayout(3, false);
        gl_grpAttributes.verticalSpacing = 10;
        grpAttributes.setLayout(gl_grpAttributes);
        GridData gd_grpAttributes = new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1);
        gd_grpAttributes.heightHint = 107;
        grpAttributes.setLayoutData(gd_grpAttributes);
        Label lblName = new Label(grpAttributes, SWT.NONE);
        lblName.setText("Name");
        nameText = new Text(grpAttributes, SWT.BORDER);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        Label lblCode = new Label(grpAttributes, SWT.NONE);
        lblCode.setText("Code");
        codeText = new Text(grpAttributes, SWT.BORDER);
        codeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        codeText.setEditable(false);
        codeText.setText(generateCode(getPath()));
        Label lblParent = new Label(grpAttributes, SWT.NONE);
        lblParent.setText("Parent");
        parentCombo = new Combo(grpAttributes, SWT.NONE);
        GridData gd_parentCombo = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_parentCombo.widthHint = 345;
        parentCombo.setLayoutData(gd_parentCombo);
        Button btnBrowseParentDef = new Button(grpAttributes, SWT.NONE);
        btnBrowseParentDef.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnBrowseParentDef.setText("Browse");
    }

    public void createGroupProperties(Composite container) {
        Group grpProperties = new Group(container, SWT.NONE);
        GridData gd_grpProperties = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
        gd_grpProperties.heightHint = 50;
        grpProperties.setLayoutData(gd_grpProperties);
        grpProperties.setText("Properties");
        grpProperties.setLayout(new GridLayout(5, false));
        Button abstractCheckBox = new Button(grpProperties, SWT.CHECK);
        abstractCheckBox.setText("Abstract");
        new Label(grpProperties, SWT.NONE);
        Button singletonCheckBox = new Button(grpProperties, SWT.CHECK);
        singletonCheckBox.setText("Singleton");
        new Label(grpProperties, SWT.NONE);
        Button blockedCheckBox = new Button(grpProperties, SWT.CHECK);
        blockedCheckBox.setText("Blocked");
        Button environmentCheckBox = new Button(grpProperties, SWT.CHECK);
        environmentCheckBox.setText("Environment");
        new Label(grpProperties, SWT.NONE);
        Button componentCheckBox = new Button(grpProperties, SWT.CHECK);
        componentCheckBox.setText("Component");
        new Label(grpProperties, SWT.NONE);
        new Label(grpProperties, SWT.NONE);
    }

    private String generateCode(String seed) {
        try {
            Security.addProvider(new FNVProvider());
            MessageDigest digest = MessageDigest.getInstance("FNV-1a");
            digest.update((seed + UUID.randomUUID().toString()).getBytes());
            byte[] hash1 = digest.digest();
            String sHash1 = "m" + (new String(LibraryBase64.encode(hash1))).replaceAll("=", "").replaceAll("-", "_");
            return sHash1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void setTypeDefinition(String typeDefinition) {
        this.typeDefinition = typeDefinition;
    }

    public String getTypeDefinition() {
        return typeDefinition;
    }

    public String getCodeDefinition() {
        return codeText.getText();
    }

    public String getNameDefinition() {
        return nameText.getText();
    }

    public String getParent() {
        return null;
    }

    public String getLocation() {
        return "";
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isSingleton() {
        return false;
    }

    public boolean isComponent() {
        return false;
    }

    public boolean isEnvironment() {
        return false;
    }

    public boolean isBlocked() {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
    }
}
