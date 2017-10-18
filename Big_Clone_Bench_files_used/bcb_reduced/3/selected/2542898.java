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
public class RegUserPanel extends javax.swing.JPanel implements Refreshable {

    private final String ACCESS_LEVEL_CB_INITIAL_MSG = "<SELECIONE UM NÍVEL>";

    private final String TITLE_WARNING = "AVISO";

    private final String MSG_REQUIRED_FIELDS = "Campo(s) requerido(s):\n";

    private final String REQUIRED_FIELDS_LOGIN = "* Login";

    private final String REQUIRED_FIELDS_PASS = "\n* Senha";

    private final String REQUIRED_FIELDS_CONFIRMATION_PASS = "\n* Confirmar Senha";

    private final String REQUIRED_FIELDS_ACCESS_LEVEL = "\n* Nível de acesso";

    private final String PASS_DONT_MATCH = "Senhas não conferem";

    private final int TYPE_ACCESS_LEVEL = 1;

    private static RegUserPanel instance;

    private static Facade facade;

    private static MainFrame mainFrame;

    private RegUserPanel() {
        initComponents();
        init();
    }

    private void init() {
        facade = Facade.getInstance();
        mainFrame = MainFrame.getInstance();
        AutoCompleteDecorator.decorate(accessLevelCB);
    }

    public static RegUserPanel getInstance() {
        if (instance == null) {
            instance = new RegUserPanel();
        }
        return instance;
    }

    private void fillComboBox(JComboBox comboBox, int type) {
        comboBox.removeAllItems();
        List<String> itens = new ArrayList<String>();
        comboBox.addItem(ACCESS_LEVEL_CB_INITIAL_MSG);
        if (type == TYPE_ACCESS_LEVEL) {
            for (AccessLevel al : AccessLevel.values()) {
                if (al.compareTo(mainFrame.getUserLoggedAccessLevel()) < 0) {
                    itens.add(al.toString());
                }
            }
        }
        for (String item : itens) {
            comboBox.addItem(item);
        }
        if (comboBox.getItemCount() == 2 && type == TYPE_ACCESS_LEVEL) {
        }
    }

    private boolean validatePass() {
        String pass = new String(passTF.getPassword());
        String confirmPass = new String(confirmPassTF.getPassword());
        if (pass.equals(confirmPass) && !pass.equals("")) {
            return true;
        }
        return false;
    }

    private void clearAllFields() {
        Util.clearAllFields(instance);
        accessLevelCB.setSelectedIndex(Util.FIRST_INDEX);
    }

    private void initComponents() {
        userPanel = new javax.swing.JPanel();
        loginLabel = new javax.swing.JLabel();
        passLabel = new javax.swing.JLabel();
        passTF = new javax.swing.JPasswordField();
        confirmPassTF = new javax.swing.JPasswordField();
        confirmPassLB = new javax.swing.JLabel();
        loginTF = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        accessLevelLabel = new javax.swing.JLabel();
        accessLevelCB = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        regBtn = new javax.swing.JButton();
        clearAllBtn = new javax.swing.JButton();
        backBtn = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, " Cadastro de Usuário ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12)));
        userPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(" Usuário "));
        loginLabel.setText("Login:");
        passLabel.setText("Senha:");
        passTF.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                passTFFocusGained(evt);
            }
        });
        confirmPassTF.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusGained(java.awt.event.FocusEvent evt) {
                confirmPassTFFocusGained(evt);
            }
        });
        confirmPassLB.setText("Confirma senha:");
        jLabel2.setForeground(new java.awt.Color(255, 0, 0));
        jLabel2.setText("*");
        jLabel4.setForeground(new java.awt.Color(255, 0, 0));
        jLabel4.setText("*");
        jLabel6.setForeground(new java.awt.Color(255, 0, 0));
        jLabel6.setText("*");
        accessLevelLabel.setText("Nível de acesso:");
        accessLevelCB.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "<SELECIONE UM NÍVEL>" }));
        jLabel3.setForeground(new java.awt.Color(255, 0, 0));
        jLabel3.setText("*");
        javax.swing.GroupLayout userPanelLayout = new javax.swing.GroupLayout(userPanel);
        userPanel.setLayout(userPanelLayout);
        userPanelLayout.setHorizontalGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(userPanelLayout.createSequentialGroup().addContainerGap().addComponent(jLabel2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(loginLabel).addGap(9, 9, 9).addComponent(loginTF, javax.swing.GroupLayout.DEFAULT_SIZE, 77, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jLabel3).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(accessLevelLabel).addGap(5, 5, 5).addComponent(accessLevelCB, 0, 70, Short.MAX_VALUE).addGap(22, 22, 22).addComponent(jLabel4).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(passLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(passTF, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE).addGap(18, 18, 18).addComponent(jLabel6).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(confirmPassLB).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(confirmPassTF, javax.swing.GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE).addContainerGap()));
        userPanelLayout.setVerticalGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(userPanelLayout.createSequentialGroup().addGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(loginLabel).addComponent(passLabel).addComponent(jLabel2).addComponent(jLabel4).addComponent(passTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(confirmPassLB).addComponent(jLabel6).addComponent(confirmPassTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(loginTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(accessLevelLabel).addComponent(jLabel3).addComponent(accessLevelCB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        regBtn.setText("Cadastrar");
        regBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regBtnActionPerformed(evt);
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
        jLabel1.setForeground(new java.awt.Color(255, 0, 0));
        jLabel1.setText("*");
        jLabel5.setText("Campos requeridos");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(userPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel5).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 157, Short.MAX_VALUE).addComponent(backBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(clearAllBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(regBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { backBtn, clearAllBtn, regBtn });
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(userPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1).addComponent(jLabel5)).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(regBtn).addComponent(clearAllBtn).addComponent(backBtn))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {
        mainFrame.backPanel();
    }

    private void clearAllBtnActionPerformed(java.awt.event.ActionEvent evt) {
        clearAllFields();
    }

    private void regBtnActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String pass = new String(passTF.getPassword());
            String confirmationPass = new String(confirmPassTF.getPassword());
            StringBuffer requiredFields = new StringBuffer("");
            if (loginTF.getText().equals("")) {
                requiredFields.append(REQUIRED_FIELDS_LOGIN);
            }
            if (accessLevelCB.getSelectedIndex() <= Util.FIRST_INDEX) {
                requiredFields.append(REQUIRED_FIELDS_ACCESS_LEVEL);
            }
            if (pass.toString().equals("")) {
                requiredFields.append(REQUIRED_FIELDS_PASS);
            }
            if (confirmationPass.equals("")) {
                requiredFields.append(REQUIRED_FIELDS_CONFIRMATION_PASS);
            }
            if (requiredFields.toString().equals("")) {
                if (!validatePass()) {
                    JOptionPane.showMessageDialog(instance, PASS_DONT_MATCH, TITLE_WARNING, JOptionPane.INFORMATION_MESSAGE);
                    passTF.setText("");
                    confirmPassTF.setText("");
                    return;
                }
                User user = new User();
                user.setLogin(loginTF.getText());
                byte[] b = CriptUtil.digest(pass.getBytes(), CriptUtil.SHA);
                user.setPass(CriptUtil.byteArrayToHexString(b));
                user.setAccessLevel(AccessLevel.get(accessLevelCB.getSelectedItem().toString()));
                facade.saveUser(user);
                JOptionPane.showMessageDialog(instance, "Usuário cadastrado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                clearAllFields();
                refresh();
            } else {
                JOptionPane.showMessageDialog(instance, MSG_REQUIRED_FIELDS + requiredFields.toString(), TITLE_WARNING, JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (MyHibernateException ex) {
            Logger.getLogger(RegUserPanel.class.getName()).log(Level.SEVERE, null, ex);
            Util.errorSQLPane(instance, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(RegUserPanel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void passTFFocusGained(java.awt.event.FocusEvent evt) {
        passTF.selectAll();
    }

    private void confirmPassTFFocusGained(java.awt.event.FocusEvent evt) {
        confirmPassTF.selectAll();
    }

    private javax.swing.JComboBox accessLevelCB;

    private javax.swing.JLabel accessLevelLabel;

    private javax.swing.JButton backBtn;

    private javax.swing.JButton clearAllBtn;

    private javax.swing.JLabel confirmPassLB;

    private javax.swing.JPasswordField confirmPassTF;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel loginLabel;

    private javax.swing.JTextField loginTF;

    private javax.swing.JLabel passLabel;

    private javax.swing.JPasswordField passTF;

    private javax.swing.JButton regBtn;

    private javax.swing.JPanel userPanel;

    public void refresh() {
        fillComboBox(accessLevelCB, TYPE_ACCESS_LEVEL);
        loginTF.requestFocusInWindow();
    }
}
