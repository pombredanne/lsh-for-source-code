package gui;

import java.awt.Cursor;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import exception.MyHibernateException;
import exception.OldVersionException;
import facade.Facade;
import bean.user.User;
import bean.user.AccessLevel;
import util.CriptUtil;
import util.Refreshable;
import util.Util;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 * @author  w4m
 */
public class DetailUserPanel extends javax.swing.JPanel implements Refreshable {

    private final String TITLE_WARNIG = "AVISO";

    private final String TITLE_SUCESS = "SUCESSO";

    private final String TITLE_REQUIRED_FIELDS = "CAMPOS REQUERIDOS";

    private final String MSG_INVALID_PASS = "Senhas não conferem!";

    private final String MSG_INVALID_CURRENT_PASS = "Senha inválida!";

    private final String MSG_SUCESS_UPDATE = "Usuário atualizado com sucesso!";

    private final String EMPLOYEE_CB_INITIAL_MSG = "<SELECIONE UM FUNCIONÁRIO>";

    private final String ACCESS_LEVEL_CB_INITIAL_MSG = "<SELECIONE UM NÍVEL>";

    private final String REQUIRED_FIELDS_LOGIN = "\n *Login";

    private final String REQUIRED_FIELDS_ACCESS_LEVEL = "\n *Nível de acesso";

    private static DetailUserPanel instance;

    private static Facade facade;

    private static MainFrame mainFrame;

    private User user;

    private DetailUserPanel() {
        initComponents();
        init();
    }

    private void init() {
        facade = Facade.getInstance();
        mainFrame = MainFrame.getInstance();
        AutoCompleteDecorator.decorate(accessLevelCB);
    }

    public static DetailUserPanel getInstance(User user) {
        if (instance == null) {
            instance = new DetailUserPanel();
        }
        instance.user = user;
        instance.fillComboBox(instance.accessLevelCB);
        return instance;
    }

    private void fillPanel() {
        if (user != null) {
            accessLevelCB.setSelectedItem(user.getAccessLevel().toString());
            loginTF.setText(user.getLogin());
        }
    }

    private void fillComboBox(JComboBox comboBox) {
        comboBox.removeAllItems();
        List<String> itens = new ArrayList<String>();
        comboBox.addItem(ACCESS_LEVEL_CB_INITIAL_MSG);
        for (AccessLevel al : AccessLevel.values()) {
            if (al.compareTo(mainFrame.getUserLoggedAccessLevel()) <= 0) {
                itens.add(al.toString());
            }
        }
        for (String item : itens) {
            comboBox.addItem(item);
        }
        if (comboBox.getItemCount() == 2) {
            comboBox.removeItem(ACCESS_LEVEL_CB_INITIAL_MSG);
        }
    }

    private boolean validatePass() throws NoSuchAlgorithmException {
        String newPass = new String(newPassTF.getPassword());
        String confirmPass = new String(confirmPassTF.getPassword());
        return (newPass.equals(confirmPass) && !newPass.equals(""));
    }

    private boolean validateCurrentPass() throws NoSuchAlgorithmException, MyHibernateException {
        String currentPass = new String(passTF.getPassword());
        byte[] b = CriptUtil.digest(currentPass.getBytes(), CriptUtil.SHA);
        user = facade.loadUser(loginTF.getText(), CriptUtil.byteArrayToHexString(b));
        return (user != null) ? true : false;
    }

    private void clearAllFields() {
        accessLevelCB.setSelectedIndex(0);
        Util.clearAllFields(passPanel);
        accessLevelCB.setSelectedIndex(Util.FIRST_INDEX);
    }

    private void initComponents() {
        userPanel = new javax.swing.JPanel();
        accessLevelLabel = new javax.swing.JLabel();
        accessLevelCB = new javax.swing.JComboBox();
        loginLB = new javax.swing.JLabel();
        loginTF = new javax.swing.JTextField();
        updateBtn = new javax.swing.JButton();
        clearAllBtn = new javax.swing.JButton();
        backBtn = new javax.swing.JButton();
        passPanel = new javax.swing.JPanel();
        confirmPassLB = new javax.swing.JLabel();
        confirmPassTF = new javax.swing.JPasswordField();
        newPassTF = new javax.swing.JPasswordField();
        passTF = new javax.swing.JPasswordField();
        passLabel = new javax.swing.JLabel();
        newPassLB = new javax.swing.JLabel();
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, " Detalhes do Usuário ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12)));
        userPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(" Usuário "));
        accessLevelLabel.setText("Nível de acesso:");
        accessLevelCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "<SELECIONE UM NÍVEL>" }));
        loginLB.setText("Login:");
        loginTF.setEditable(false);
        loginTF.setEnabled(false);
        javax.swing.GroupLayout userPanelLayout = new javax.swing.GroupLayout(userPanel);
        userPanel.setLayout(userPanelLayout);
        userPanelLayout.setHorizontalGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(userPanelLayout.createSequentialGroup().addContainerGap().addComponent(loginLB).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(loginTF, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(accessLevelLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(accessLevelCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(0, 10, Short.MAX_VALUE)));
        userPanelLayout.setVerticalGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(userPanelLayout.createSequentialGroup().addGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(loginLB).addComponent(loginTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(accessLevelLabel).addComponent(accessLevelCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        userPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { accessLevelCB, loginTF });
        updateBtn.setText("Atualizar");
        updateBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateBtnActionPerformed(evt);
            }
        });
        clearAllBtn.setText("Limpar tudo");
        clearAllBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllBtnActionPerformed(evt);
            }
        });
        backBtn.setText("«« Voltar");
        backBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backBtnActionPerformed(evt);
            }
        });
        passPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Alterar senha"));
        confirmPassLB.setText("Confirma senha:");
        passLabel.setText("Senha atual:");
        newPassLB.setText("Nova senha:");
        javax.swing.GroupLayout passPanelLayout = new javax.swing.GroupLayout(passPanel);
        passPanel.setLayout(passPanelLayout);
        passPanelLayout.setHorizontalGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(passPanelLayout.createSequentialGroup().addGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(newPassLB).addComponent(passLabel).addComponent(confirmPassLB)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(passTF, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(confirmPassTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(newPassTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(258, 258, 258)));
        passPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { confirmPassTF, newPassTF, passTF });
        passPanelLayout.setVerticalGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(passPanelLayout.createSequentialGroup().addGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER).addComponent(passLabel).addComponent(passTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER).addComponent(newPassLB).addComponent(newPassTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(passPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER).addComponent(confirmPassLB).addComponent(confirmPassTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(passPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE).addComponent(userPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(backBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(clearAllBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(updateBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { backBtn, clearAllBtn, updateBtn });
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(userPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(passPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(updateBtn).addComponent(clearAllBtn).addComponent(backBtn)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {
        mainFrame.backPanel();
    }

    private void clearAllBtnActionPerformed(java.awt.event.ActionEvent evt) {
        clearAllFields();
    }

    private void updateBtnActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String pass = new String(passTF.getPassword());
            StringBuffer requiredFields = new StringBuffer("");
            if (accessLevelCB.getSelectedIndex() < Util.FIRST_INDEX) {
                requiredFields.append(REQUIRED_FIELDS_ACCESS_LEVEL);
            }
            if (requiredFields.toString().equals("")) {
                if (validateCurrentPass()) {
                    if (!pass.equals("") && !validatePass()) {
                        JOptionPane.showMessageDialog(instance, MSG_INVALID_PASS, TITLE_WARNIG, JOptionPane.WARNING_MESSAGE);
                        return;
                    } else if (!pass.equals("") && validatePass()) {
                        byte[] b = CriptUtil.digest(new String(newPassTF.getPassword()).getBytes(), CriptUtil.SHA);
                        user.setPass(CriptUtil.byteArrayToHexString(b));
                    }
                } else {
                    JOptionPane.showMessageDialog(instance, MSG_INVALID_CURRENT_PASS, TITLE_WARNIG, JOptionPane.WARNING_MESSAGE);
                    return;
                }
                user.setAccessLevel(AccessLevel.get(accessLevelCB.getSelectedItem().toString()));
                facade.updateUser(user);
                JOptionPane.showMessageDialog(instance, MSG_SUCESS_UPDATE, TITLE_SUCESS, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(instance, requiredFields.toString(), TITLE_REQUIRED_FIELDS, JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (OldVersionException ex) {
            Logger.getLogger(DetailUserPanel.class.getName()).log(Level.SEVERE, null, ex);
            if (Util.transactionErroDialog()) {
                try {
                    user = facade.loadUser(user.getId());
                    refresh();
                } catch (MyHibernateException ex1) {
                    Logger.getLogger(DetailUserPanel.class.getName()).log(Level.SEVERE, null, ex1);
                    Util.errorSQLPane(instance, ex1);
                }
            }
        } catch (MyHibernateException ex) {
            Logger.getLogger(DetailUserPanel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(DetailUserPanel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private javax.swing.JComboBox accessLevelCB;

    private javax.swing.JLabel accessLevelLabel;

    private javax.swing.JButton backBtn;

    private javax.swing.JButton clearAllBtn;

    private javax.swing.JLabel confirmPassLB;

    private javax.swing.JPasswordField confirmPassTF;

    private javax.swing.JLabel loginLB;

    private javax.swing.JTextField loginTF;

    private javax.swing.JLabel newPassLB;

    private javax.swing.JPasswordField newPassTF;

    private javax.swing.JLabel passLabel;

    private javax.swing.JPanel passPanel;

    private javax.swing.JPasswordField passTF;

    private javax.swing.JButton updateBtn;

    private javax.swing.JPanel userPanel;

    public void refresh() {
        clearAllFields();
        fillPanel();
        loginTF.requestFocus();
    }
}
