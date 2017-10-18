package org.gs.game.gostop.dlg;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.gs.game.gostop.Main;
import org.gs.game.gostop.MainFrame;
import org.gs.game.gostop.Resource;
import org.gs.game.gostop.config.GameConfig;
import org.gs.game.gostop.config.GameUser;
import org.gs.game.gostop.config.UserLocale;
import org.gs.game.gostop.item.PlayerLabelItem;
import org.gs.game.gostop.item.PlayerPointItem;
import org.gs.game.gostop.sound.GameSoundManager;
import org.gs.game.gostop.sound.GameVoiceType;
import org.gs.game.gostop.utils.Base64Coder;

public class SetupUserDlg extends JDialog implements ActionListener {

    private static final long serialVersionUID = -1097984594924692111L;

    public static final String USER_INFO_TITLE = "setup.userinfo.title";

    private static final String DLG_TITLE = "setup.user.title";

    private static final String LOGIN_ID_LBL = "setup.user.login.id";

    private static final String PASSWORD_LBL = "setup.user.password";

    private static final String CREATE_LBL = "setup.user.create";

    private static final String USER_INFO_TITLE_LBL = "setup.user.info.title";

    private static final String USER_NAME_LBL = "setup.user.name";

    private static final String USER_ALIAS_LBL = "setup.user.alias";

    private static final String USER_MONEY_LBL = "setup.user.money";

    private static final String USER_REFILL_LBL = "setup.user.refill";

    private static final String USER_RECORD_LBL = "setup.user.record";

    private static final String USER_HL_MONEY_LBL = "setup.user.hl.money";

    private static final String USER_BW_RECORD_LBL = "setup.user.bw.record";

    private static final String USER_AVATAR_LBL = "setup.user.avatar";

    private static final String AVATAR_FILTER_LBL = "setup.user.avatar.filter";

    private static final String USER_LOCALE_LBL = "setup.user.locale";

    private static final String USER_VOICEID_LBL = "setup.user.voice";

    private static final String E_AVATAR_TITLE_LBL = "setup.user.eavatar.title";

    private static final String E_LOGIN_TITLE_LBL = "setup.user.elogin.title";

    private static final String E_LOGIN_MSG_LBL = "setup.user.elogin.msg";

    private static final String E_NEWUSER_TITLE_LBL = "setup.user.enewuser.title";

    private static final String E_EXIST_MSG_LBL = "setup.user.eexist.msg";

    private static final String E_CREATE_MSG_LBL = "setup.user.ecreate.msg";

    private JTextField loginId;

    private JLabel passwordLabel;

    private JPasswordField password;

    private JCheckBox createUser;

    private JPanel userInfo;

    private JTextField userName;

    private JTextField userAlias;

    private JLabel userMoney;

    private JLabel userRefill;

    private JLabel userRecord;

    private JLabel userHLMoney;

    private JLabel userBWRecord;

    private JButton userAvatar;

    private JComboBox userLocale;

    private JComboBox userVoice;

    private JButton okButton;

    private JButton cancelButton;

    private GameUser gameUser;

    private boolean canEdit;

    private boolean updated;

    public SetupUserDlg(Frame parent) {
        this(parent, null, true);
    }

    public SetupUserDlg(Frame parent, GameUser gameUser, boolean canEdit) {
        super(parent, Resource.getProperty(gameUser == null ? DLG_TITLE : USER_INFO_TITLE), gameUser == null);
        this.gameUser = gameUser;
        this.canEdit = canEdit;
        updated = false;
        initContentPane();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        getRootPane().setDefaultButton(okButton);
    }

    public GameUser getGameUser() {
        return gameUser;
    }

    public void setUser(GameUser gameUser, boolean canEdit) {
        this.gameUser = gameUser;
        this.canEdit = canEdit;
        setUserInfo();
    }

    private void initContentPane() {
        Container contentPane = getContentPane();
        JPanel mainPanel;
        if (contentPane instanceof JPanel) mainPanel = (JPanel) contentPane; else {
            mainPanel = new JPanel();
            setContentPane(mainPanel);
        }
        mainPanel.setBorder(new CompoundBorder(mainPanel.getBorder(), new EmptyBorder(10, 10, 10, 10)));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        Container boxOuter = new Container();
        GridBagLayout gbl = new GridBagLayout();
        boxOuter.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        LineBorder lb = new LineBorder(Color.GRAY);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;
        loginId = (JTextField) addComponentItem(boxOuter, gbl, gbc, lb, LOGIN_ID_LBL, true);
        passwordLabel = new JLabel(Resource.getProperty(PASSWORD_LBL));
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbl.setConstraints(passwordLabel, gbc);
        boxOuter.add(passwordLabel);
        password = new JPasswordField(20);
        password.setBorder(lb);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(password, gbc);
        boxOuter.add(password);
        userInfo = new JPanel();
        userInfo.setVisible(false);
        gbl.setConstraints(userInfo, gbc);
        boxOuter.add(userInfo);
        mainPanel.add(boxOuter);
        mainPanel.add(Box.createVerticalStrut(6));
        Box boxButtons = new Box(BoxLayout.X_AXIS);
        if (gameUser == null) {
            createUser = new JCheckBox(Resource.getProperty(CREATE_LBL));
            createUser.setBorderPaintedFlat(true);
            createUser.addActionListener(this);
            boxButtons.add(createUser);
            boxButtons.add(Box.createHorizontalGlue());
        }
        okButton = new JButton(Resource.getStandardProperty(Resource.STD_OK_BUTTON));
        okButton.addActionListener(this);
        boxButtons.add(okButton);
        cancelButton = new JButton(Resource.getStandardProperty(Resource.STD_CANCEL_BUTTON));
        cancelButton.addActionListener(this);
        cancelButton.setVisible(canEdit);
        boxButtons.add(cancelButton);
        mainPanel.add(boxButtons);
        if (gameUser != null) setUserInfo();
    }

    private void initUserInfoPanel() {
        if (userInfo.getComponentCount() == 0) {
            GridBagLayout gbl = new GridBagLayout();
            GridBagConstraints gbc = new GridBagConstraints();
            LineBorder lb = new LineBorder(Color.GRAY);
            userInfo.setLayout(gbl);
            userInfo.setBorder(new TitledBorder(Resource.getProperty(USER_INFO_TITLE_LBL)));
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.weightx = 1.0;
            userName = (JTextField) addComponentItem(userInfo, gbl, gbc, lb, USER_NAME_LBL, true);
            userAlias = (JTextField) addComponentItem(userInfo, gbl, gbc, lb, USER_ALIAS_LBL, true);
            if (gameUser != null) {
                userMoney = (JLabel) addComponentItem(userInfo, gbl, gbc, lb, USER_MONEY_LBL, false);
                userRefill = (JLabel) addComponentItem(userInfo, gbl, gbc, lb, USER_REFILL_LBL, false);
                userRecord = (JLabel) addComponentItem(userInfo, gbl, gbc, lb, USER_RECORD_LBL, false);
                userHLMoney = (JLabel) addComponentItem(userInfo, gbl, gbc, lb, USER_HL_MONEY_LBL, false);
                userBWRecord = (JLabel) addComponentItem(userInfo, gbl, gbc, lb, USER_BW_RECORD_LBL, false);
            }
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            Component c = new JLabel(Resource.getProperty(USER_AVATAR_LBL));
            gbl.setConstraints(c, gbc);
            userInfo.add(c);
            gbc.fill = GridBagConstraints.VERTICAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.WEST;
            userAvatar = new JButton();
            GameConfig gcfg = MainFrame.getGameConfig();
            userAvatar.setPreferredSize(new Dimension(gcfg.getAvatarMaxWidth(), gcfg.getAvatarMaxHeight()));
            userAvatar.addActionListener(this);
            gbl.setConstraints(userAvatar, gbc);
            userInfo.add(userAvatar);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            c = new JLabel(Resource.getProperty(USER_LOCALE_LBL));
            gbl.setConstraints(c, gbc);
            userInfo.add(c);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            java.util.List<UserLocale> locales = UserLocale.getUserLocales();
            userLocale = new JComboBox(locales.toArray(new UserLocale[locales.size()]));
            userLocale.setBorder(lb);
            userLocale.setEditable(false);
            userLocale.setSelectedItem(GameConfig.getSupportedLocale(null));
            gbl.setConstraints(userLocale, gbc);
            userInfo.add(userLocale);
            gbc.gridwidth = GridBagConstraints.RELATIVE;
            c = new JLabel(Resource.getProperty(USER_VOICEID_LBL));
            gbl.setConstraints(c, gbc);
            userInfo.add(c);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            java.util.List<GameVoiceType> voices = GameSoundManager.getVoiceTypes();
            userVoice = new JComboBox(voices.toArray(new GameVoiceType[voices.size()]));
            userVoice.setBorder(lb);
            userVoice.setEditable(false);
            userVoice.setSelectedItem(GameConfig.getSupportedLocale(null));
            gbl.setConstraints(userVoice, gbc);
            userInfo.add(userVoice);
        }
    }

    private JComponent addComponentItem(Container container, GridBagLayout gbl, GridBagConstraints gbc, Border border, String label, boolean canEdit) {
        Component c = new JLabel(Resource.getProperty(label));
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbl.setConstraints(c, gbc);
        container.add(c);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JComponent valueItem = canEdit ? new JTextField(20) : new JLabel();
        valueItem.setBorder(border);
        gbl.setConstraints(valueItem, gbc);
        container.add(valueItem);
        return valueItem;
    }

    private void setUserInfo() {
        loginId.setText(gameUser.getLoginId());
        loginId.setEditable(false);
        passwordLabel.setVisible(canEdit);
        password.setVisible(canEdit);
        initUserInfoPanel();
        userName.setText(gameUser.getUserName());
        userName.setEditable(canEdit);
        userAlias.setText(gameUser.getUserAlias());
        userAlias.setEditable(canEdit);
        if (gameUser.getAvatarPath() != null) {
            try {
                BufferedImage bi = ImageIO.read(new File(gameUser.getAvatarPath()));
                userAvatar.setIcon(new ImageIcon(bi));
            } catch (IOException e) {
                userAvatar.setIcon(null);
            }
        } else userAvatar.setIcon(null);
        userAvatar.setEnabled(canEdit);
        userLocale.setSelectedItem(UserLocale.getUserLocale(gameUser.getUserLocale()));
        userLocale.setEnabled(canEdit);
        userVoice.setSelectedItem(GameSoundManager.getVoiceType(gameUser.getUserVoiceId()));
        userVoice.setEnabled(canEdit);
        userInfo.setVisible(true);
        cancelButton.setVisible(canEdit);
        userMoney.setText(Resource.format(PlayerLabelItem.MONEY_LBL, gameUser.getMoney()));
        userRefill.setText(Integer.toString(gameUser.getAllInCount()));
        userRecord.setText(Resource.format(PlayerLabelItem.RECORD_LBL, gameUser.getWins(), gameUser.getDraws(), gameUser.getLoses()));
        userHLMoney.setText(Resource.format(PlayerLabelItem.MONEY_LBL, gameUser.getHighestMoney()) + " / " + Resource.format(PlayerLabelItem.MONEY_LBL, gameUser.getLowestMoney()));
        userBWRecord.setText(Resource.format(PlayerPointItem.POINTS_LBL, gameUser.getBestPoints()) + " " + Resource.format(PlayerLabelItem.MONEY_LBL, gameUser.getBestMoney()) + " / " + Resource.format(PlayerPointItem.POINTS_LBL, gameUser.getWorstPoints()) + " " + Resource.format(PlayerLabelItem.MONEY_LBL, gameUser.getWorstMoney()));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            if (gameUser != null) {
                if (canEdit) {
                    updateUserInfo();
                    updated = true;
                }
            } else if (createUser.isSelected() && loginId.getText().length() > 0 && password.getPassword().length > 0) gameUser = createNewUser(); else gameUser = authenticate();
        } else if (e.getSource() == createUser) {
            if (createUser.isSelected()) initUserInfoPanel();
            userInfo.setVisible(createUser.isSelected());
            pack();
            Main.moveToCenter(this);
        } else if (e.getSource() == userAvatar) loadAvatar();
        if (e.getSource() != userAvatar && (gameUser != null || e.getSource() == cancelButton)) dispose();
    }

    public void dispose() {
        super.dispose();
        if (getParent() instanceof MainFrame) ((MainFrame) getParent()).onChildDlgDisposed(this, updated);
    }

    private GameUser createNewUser() {
        GameUser gameUser;
        String lid = loginId.getText();
        if (GameUser.isGameUser(lid)) {
            gameUser = null;
            JOptionPane.showMessageDialog(this, Resource.getProperty(E_EXIST_MSG_LBL), Resource.getProperty(E_NEWUSER_TITLE_LBL), JOptionPane.ERROR_MESSAGE);
        } else {
            String avatarPath = saveAvatarIcon(lid);
            gameUser = GameUser.addGameUser(lid, getEncryptedPassword(), userName.getText(), userAlias.getText(), avatarPath, ((UserLocale) userLocale.getSelectedItem()).getLocaleId(), ((GameVoiceType) userVoice.getSelectedItem()).getVoiceTypeId(), GameUser.GameUserType.HUMAN);
            if (gameUser == null) {
                JOptionPane.showMessageDialog(this, Resource.getProperty(E_CREATE_MSG_LBL), Resource.getProperty(E_NEWUSER_TITLE_LBL), JOptionPane.ERROR_MESSAGE);
            }
        }
        return gameUser;
    }

    private void updateUserInfo() {
        gameUser.updateInfo(password.getPassword().length == 0 ? null : getEncryptedPassword(), userName.getText(), userAlias.getText(), saveAvatarIcon(loginId.getText()), ((UserLocale) userLocale.getSelectedItem()).getLocaleId(), ((GameVoiceType) userVoice.getSelectedItem()).getVoiceTypeId());
    }

    private GameUser authenticate() {
        GameUser gameUser;
        gameUser = GameUser.getGameUser(loginId.getText(), getEncryptedPassword());
        if (gameUser == null) {
            JOptionPane.showMessageDialog(this, Resource.getProperty(E_LOGIN_MSG_LBL), Resource.getProperty(E_LOGIN_TITLE_LBL), JOptionPane.ERROR_MESSAGE);
        }
        return gameUser;
    }

    private void loadAvatar() {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter fnef = new FileNameExtensionFilter(Resource.getProperty(AVATAR_FILTER_LBL), "jpg", "gif", "png", "bmp");
        fc.setFileFilter(fnef);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage bi = ImageIO.read(fc.getSelectedFile());
                GameConfig gc = MainFrame.getGameConfig();
                int width = gc.getAvatarMaxWidth();
                int height = gc.getAvatarMaxHeight();
                if ((float) width / bi.getWidth() > (float) height / bi.getHeight()) width = (int) (bi.getWidth() * ((float) height / bi.getHeight())); else height = (int) (bi.getHeight() * ((float) width / bi.getWidth()));
                BufferedImage th = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = th.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(bi, 0, 0, width, height, null);
                g2.dispose();
                userAvatar.setIcon(new ImageIcon(th));
            } catch (Exception err) {
                JOptionPane.showMessageDialog(this, err.toString(), Resource.getProperty(E_AVATAR_TITLE_LBL), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String saveAvatarIcon(String loginId) {
        String avatarPath = null;
        ImageIcon icon = (ImageIcon) userAvatar.getIcon();
        if (icon != null) {
            File folder = new File(GameUser.getUserConfigFolder(), "avatar");
            folder.mkdir();
            File avatar = new File(folder, loginId + ".jpg");
            try {
                ImageIO.write((BufferedImage) icon.getImage(), "jpg", avatar);
                avatarPath = avatar.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return avatarPath;
    }

    private String getEncryptedPassword() {
        String encrypted;
        char[] pwd = password.getPassword();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(new String(pwd).getBytes("UTF-8"));
            byte[] digested = md.digest();
            encrypted = new String(Base64Coder.encode(digested));
        } catch (Exception e) {
            encrypted = new String(pwd);
        }
        for (int i = 0; i < pwd.length; i++) pwd[i] = 0;
        return encrypted;
    }
}
