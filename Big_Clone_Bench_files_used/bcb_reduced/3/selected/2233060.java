package gui.windows;

import gui.Resources;
import gui.components.LogoPanel;
import gui.windows.popupdialogs.TosUpdateDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import models.requests.RequestLogin;
import whisper.Config;
import whisper.Whisper;

public class LoginWindow extends JFrame {

    /** The serialisation UID. */
    private static final long serialVersionUID = -6166599183709391201L;

    /** The content panel. */
    private JPanel jContentPane = null;

    /** The center panel. */
    private JPanel jpCenter = null;

    /** The south panel. */
    private JPanel jpSouth = null;

    /** The north panel. */
    private LogoPanel jpNorth = null;

    /** The first name label. */
    private JLabel jlFirstName = null;

    /** The last name label. */
    private JLabel jlLastName = null;

    /** The password label. */
    private JLabel jlPassword = null;

    /** The location label. */
    private JLabel jlLocation = null;

    /** The host label. */
    private JLabel jlHost = null;

    /** The timestamp label. */
    private JLabel jlTimeStamps = null;

    /** The save details label. */
    private JLabel jlSaveDetails = null;

    /** The save password label. */
    private JLabel jlSavePassword = null;

    /** The error label. */
    private JLabel jlError = null;

    /** The first name text field. */
    private JTextField jtfFirstName = null;

    /** The last name text field. */
    private JTextField jtfLastName = null;

    /** The host text field. */
    private JTextField jtfHost = null;

    /** The password field. */
    private JPasswordField jpPassword = null;

    /** The location combo box. */
    private JComboBox jcbLocation = null;

    /** The save details checkbox. */
    private JCheckBox jchkSaveDetails = null;

    /** The save password checkbox. */
    private JCheckBox jchkSavePassword = null;

    /** The timestamp checkbox. */
    private JCheckBox jchkTimeStamps = null;

    /** The login button. */
    private JButton jbLogin = null;

    /** The exit button. */
    private JButton jbExit = null;

    /**
	 * Constructor.
	 */
    public LoginWindow() {
        super();
        initialize();
        this.setIconImage(Resources.IMAGE_WINDOW_ICON);
    }

    /**
	 * Initialise the window.
	 */
    private void initialize() {
        this.setSize(300, 400);
        this.setMinimumSize(new Dimension(300, 420));
        this.setMaximumSize(new Dimension(300, 420));
        this.setResizable(false);
        this.setContentPane(getJContentPane());
        this.setTitle("Whisper - Login (" + Whisper.VERSION + ")");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        validateSettings();
    }

    /**
	 * Get the content pane. If it has not been initialised, it is initialised upon first call.
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            jContentPane.add(getJpCenter(), BorderLayout.CENTER);
            jContentPane.add(getJpSouth(), BorderLayout.SOUTH);
            JPanel northPanel = new JPanel(new FlowLayout());
            northPanel.add(getLogoPanel());
            jContentPane.add(northPanel, BorderLayout.NORTH);
        }
        return jContentPane;
    }

    /**
	 * Get the center content pane. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The center content pane.
	 */
    private JPanel getJpCenter() {
        if (jpCenter == null) {
            jpCenter = new JPanel();
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            jpCenter.setLayout(new GridBagLayout());
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.ipady = 2;
            jpCenter.add(getJlFirstName(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            jpCenter.add(getJlLastName(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            jpCenter.add(getJlPassword(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            jpCenter.add(getJlLocation(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 4;
            jpCenter.add(getJlHost(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 5;
            jpCenter.add(getJlTimeStamps(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 6;
            jpCenter.add(getJlSaveDetails(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 7;
            jpCenter.add(getJlSavePassword(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            jpCenter.add(getJtfFirstName(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            jpCenter.add(getJtfLastName(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            jpCenter.add(getJpPassword(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 3;
            jpCenter.add(getJcbLocation(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 4;
            jpCenter.add(getJtfHost(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 5;
            jpCenter.add(getJchkTimeStamps(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 6;
            jpCenter.add(getJchkSaveDetails(), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 7;
            jpCenter.add(getJchkSavePassword(), gridBagConstraints);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 8;
            gridBagConstraints.gridwidth = 2;
            jpCenter.add(getJlError(), gridBagConstraints);
        }
        return jpCenter;
    }

    /**
	 * Get the south content pane. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The south content pane.
	 */
    private JPanel getJpSouth() {
        if (jpSouth == null) {
            jpSouth = new JPanel();
            jpSouth.setLayout(new GridLayout());
            jpSouth.add(getJbLogin());
            jpSouth.add(getJbExit());
        }
        return jpSouth;
    }

    /**
	 * Get the logo panel. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The logo panel.
	 */
    private LogoPanel getLogoPanel() {
        if (jpNorth == null) {
            jpNorth = new LogoPanel();
            jpNorth.setPreferredSize(new Dimension(128, 128));
        }
        return jpNorth;
    }

    /**
	 * Get the first name label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The first name label.
	 */
    private JLabel getJlFirstName() {
        if (jlFirstName == null) {
            jlFirstName = new JLabel("First name:");
        }
        return jlFirstName;
    }

    /**
	 * Get the last name label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The last name label.
	 */
    private JLabel getJlLastName() {
        if (jlLastName == null) {
            jlLastName = new JLabel("Last name:");
        }
        return jlLastName;
    }

    /**
	 * Get the password label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The password label.
	 */
    private JLabel getJlPassword() {
        if (jlPassword == null) {
            jlPassword = new JLabel("Password:");
        }
        return jlPassword;
    }

    /**
	 * Get the location label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The location label.
	 */
    private JLabel getJlLocation() {
        if (jlLocation == null) {
            jlLocation = new JLabel("Location:");
        }
        return jlLocation;
    }

    /**
	 * Get the host label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The host label.
	 */
    private JLabel getJlHost() {
        if (jlHost == null) {
            jlHost = new JLabel("Host:");
        }
        return jlHost;
    }

    /**
	 * Get the save details label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The save details label.
	 */
    private JLabel getJlSaveDetails() {
        if (jlSaveDetails == null) {
            jlSaveDetails = new JLabel("Save Details:");
        }
        return jlSaveDetails;
    }

    /**
	 * Get the save password label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The save password label.
	 */
    private JLabel getJlSavePassword() {
        if (jlSavePassword == null) {
            jlSavePassword = new JLabel("Save Password:");
        }
        return jlSavePassword;
    }

    /**
	 * Get the timestamp label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The timestamp label.
	 */
    private JLabel getJlTimeStamps() {
        if (jlTimeStamps == null) {
            jlTimeStamps = new JLabel("Time Stamps:");
        }
        return jlTimeStamps;
    }

    /**
	 * Get the first name text field. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The fist name text field.
	 */
    private JTextField getJtfFirstName() {
        if (jtfFirstName == null) {
            jtfFirstName = new JTextField(Config.getConfig().firstName);
            jtfFirstName.setPreferredSize(new Dimension(150, 20));
            jtfFirstName.addCaretListener(new CaretListener() {

                /**
				 * Called when the caret is updated.
				 * 
				 * @param e The CaretEvent.
				 */
                @Override
                public void caretUpdate(CaretEvent e) {
                    Config.getConfig().firstName = getJtfFirstName().getText();
                    validateSettings();
                }
            });
            jtfFirstName.addFocusListener(new FocusAdapter() {

                /**
				 * Called when focus is gained.
				 * 
				 * @param e The FocusEvent.
				 */
                @Override
                public void focusGained(FocusEvent e) {
                    getJtfFirstName().selectAll();
                }
            });
            jtfFirstName.addKeyListener(new KeyAdapter() {

                /**
				 * Called when a key is pressed.
				 * 
				 * @param e The KeyEvent.
				 */
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        getJtfLastName().requestFocus();
                    }
                }
            });
        }
        return jtfFirstName;
    }

    /**
	 * Get the last name text field. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The last name text field.
	 */
    private JTextField getJtfLastName() {
        if (jtfLastName == null) {
            jtfLastName = new JTextField(Config.getConfig().lastName);
            jtfLastName.setPreferredSize(new Dimension(150, 20));
            jtfLastName.addCaretListener(new CaretListener() {

                /**
				 * Called when the caret is updated.
				 * 
				 * @param e The CaretEvent.
				 */
                @Override
                public void caretUpdate(CaretEvent e) {
                    Config.getConfig().lastName = getJtfLastName().getText();
                    validateSettings();
                }
            });
            jtfLastName.addFocusListener(new FocusAdapter() {

                /**
				 * Called when focus is gained.
				 * 
				 * @param e The FocusEvent.
				 */
                @Override
                public void focusGained(FocusEvent e) {
                    getJtfLastName().selectAll();
                }
            });
            jtfLastName.addKeyListener(new KeyAdapter() {

                /**
				 * Called when a key is pressed.
				 * 
				 * @param e The KeyEvent.
				 */
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        getJpPassword().requestFocus();
                    }
                }
            });
        }
        return jtfLastName;
    }

    /**
	 * Get the host text field. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The host text field.
	 */
    private JTextField getJtfHost() {
        if (jtfHost == null) {
            jtfHost = new JTextField(Config.getConfig().hostName + ":" + Config.getConfig().port);
            jtfHost.setPreferredSize(new Dimension(150, 20));
            jtfHost.addCaretListener(new CaretListener() {

                /**
				 * Called when the caret is updated.
				 * 
				 * @param e The CaretEvent.
				 */
                @Override
                public void caretUpdate(CaretEvent e) {
                    validateSettings();
                }
            });
            jtfHost.addFocusListener(new FocusAdapter() {

                /**
				 * Called when focus is gained.
				 * 
				 * @param e The FocusEvent.
				 */
                @Override
                public void focusGained(FocusEvent e) {
                    getJtfHost().selectAll();
                }
            });
        }
        return jtfHost;
    }

    /**
	 * Validate the host control.
	 * 
	 * @return True if valid false if not.
	 */
    private boolean validateHost() {
        String host = getJtfHost().getText();
        String parts[] = host.split(":", 2);
        Config.getConfig().hostName = parts[0];
        if (Config.getConfig().hostName != null && Config.getConfig().hostName.length() > 0) {
            getJlHost().setForeground(SystemColor.textText);
        } else {
            getJlHost().setForeground(Color.RED);
            return false;
        }
        if (parts.length > 1) {
            try {
                Config.getConfig().port = Integer.parseInt(parts[1]);
            } catch (Exception ex) {
                getJlHost().setForeground(Color.RED);
                return false;
            }
        } else {
            Config.getConfig().port = Config.DEFAULT_PORT;
        }
        return true;
    }

    /**
	 * Validate a text component.
	 * 
	 * @param component The component to validate.
	 * @param associatedLabel The label associated with the component.
	 * @return True if valid, otherwise false.
	 */
    private boolean validateField(JTextComponent component, JLabel associatedLabel) {
        if (component instanceof JTextField) {
            if (component.getText() == null || component.getText().trim().length() <= 0 || component.getText().contains(" ")) {
                associatedLabel.setForeground(Color.RED);
                return false;
            }
        } else if (component instanceof JPasswordField) {
            if (((JPasswordField) component).getPassword().length < 0) {
                associatedLabel.setForeground(Color.RED);
            }
        }
        associatedLabel.setForeground(SystemColor.textText);
        return true;
    }

    /**
	 * Get the password field. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The password field.
	 */
    private JPasswordField getJpPassword() {
        if (jpPassword == null) {
            jpPassword = new JPasswordField(Config.getConfig().password);
            jpPassword.setPreferredSize(new Dimension(150, 20));
            jpPassword.addCaretListener(new CaretListener() {

                /**
				 * Called when the caret is updated.
				 * 
				 * @param e The CaretEvent.
				 */
                @Override
                public void caretUpdate(CaretEvent e) {
                    if (validateSettings()) {
                        byte[] data = new String(jpPassword.getPassword()).getBytes();
                        if (data[0] == '$' && data[1] == '1' && data[2] == '$') {
                            Config.getConfig().password = new String(data);
                        } else {
                            try {
                                MessageDigest algorithm = MessageDigest.getInstance("MD5");
                                algorithm.reset();
                                algorithm.update(data);
                                byte messageDigest[] = algorithm.digest();
                                StringBuffer hexString = new StringBuffer();
                                for (int i = 0; i < messageDigest.length; i++) {
                                    String s = Integer.toHexString(0xFF & messageDigest[i]);
                                    if (s.length() <= 1) s = "0" + s;
                                    hexString.append(s);
                                }
                                Config.getConfig().password = "$1$" + hexString.toString();
                            } catch (NoSuchAlgorithmException ex) {
                                getJlPassword().setForeground(Color.RED);
                            }
                        }
                    }
                }
            });
            jpPassword.addFocusListener(new FocusAdapter() {

                /**
				 * Called when focus is gained.
				 * 
				 * @param e The FocusEvent.
				 */
                @Override
                public void focusGained(FocusEvent e) {
                    getJpPassword().selectAll();
                }
            });
            jpPassword.addKeyListener(new KeyAdapter() {

                /**
				 * Called when a key is pressed.
				 * 
				 * @param e The KeyEvent.
				 */
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        doLogin();
                    }
                }
            });
        }
        return jpPassword;
    }

    /**
	 * Get the location combo box. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The location combo box.
	 */
    private JComboBox getJcbLocation() {
        if (jcbLocation == null) {
            jcbLocation = new JComboBox();
            jcbLocation.setPreferredSize(new Dimension(150, 20));
            jcbLocation.addItem("Last");
            jcbLocation.addItem("Home");
            if (Config.getConfig().homeStartLocation) {
                jcbLocation.setSelectedIndex(1);
            }
            jcbLocation.addItemListener(new ItemListener() {

                /**
				 * Called whenever the item state is changed.
				 * 
				 * @param e The ItemEvent.
				 */
                @Override
                public void itemStateChanged(ItemEvent e) {
                    Config.getConfig().homeStartLocation = ((String) getJcbLocation().getSelectedItem()).equals("Home");
                }
            });
        }
        return jcbLocation;
    }

    /**
	 * Get the save details checkbox. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The save details checkbox.
	 */
    private JCheckBox getJchkSaveDetails() {
        if (jchkSaveDetails == null) {
            jchkSaveDetails = new JCheckBox("", Config.getConfig().saveSettings);
            jchkSaveDetails.setPreferredSize(new Dimension(150, 20));
            jchkSaveDetails.addItemListener(new ItemListener() {

                /**
				 * Called whenever the item state is changed.
				 * 
				 * @param e The ItemEvent.
				 */
                @Override
                public void itemStateChanged(ItemEvent e) {
                    Config.getConfig().saveSettings = e.getStateChange() == ItemEvent.SELECTED;
                    getJchkSavePassword().setEnabled(e.getStateChange() == ItemEvent.SELECTED);
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        getJchkSavePassword().setSelected(false);
                    }
                }
            });
        }
        return jchkSaveDetails;
    }

    /**
	 * Get the save password checkbox. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The save password checkbox.
	 */
    private JCheckBox getJchkSavePassword() {
        if (jchkSavePassword == null) {
            jchkSavePassword = new JCheckBox("", Config.getConfig().savePassword);
            jchkSavePassword.setPreferredSize(new Dimension(150, 20));
            jchkSavePassword.addItemListener(new ItemListener() {

                /**
				 * Called whenever the item state is changed.
				 * 
				 * @param e The ItemEvent.
				 */
                @Override
                public void itemStateChanged(ItemEvent e) {
                    Config.getConfig().savePassword = e.getStateChange() == ItemEvent.SELECTED;
                }
            });
            jchkSavePassword.setEnabled(Config.getConfig().saveSettings);
        }
        return jchkSavePassword;
    }

    /**
	 * Get the timestamp checkbox. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The timestamp checkbox.
	 */
    private JCheckBox getJchkTimeStamps() {
        if (jchkTimeStamps == null) {
            jchkTimeStamps = new JCheckBox("", Config.getConfig().timestamp);
            jchkTimeStamps.setPreferredSize(new Dimension(150, 20));
            jchkTimeStamps.addItemListener(new ItemListener() {

                /**
				 * Called whenever the item state is changed.
				 * 
				 * @param e The ItemEvent.
				 */
                @Override
                public void itemStateChanged(ItemEvent e) {
                    Config.getConfig().timestamp = e.getStateChange() == ItemEvent.SELECTED;
                }
            });
        }
        return jchkTimeStamps;
    }

    /**
	 * Get the login button. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The login button.
	 */
    private JButton getJbLogin() {
        if (jbLogin == null) {
            jbLogin = new JButton("Login");
            jbLogin.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed on the button.
				 * 
				 * @param e The action events.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    doLogin();
                }
            });
        }
        return jbLogin;
    }

    /**
	 * Get the exit button. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The exit button.
	 */
    private JButton getJbExit() {
        if (jbExit == null) {
            jbExit = new JButton("Exit");
            jbExit.addActionListener(new ActionListener() {

                /**
				 * Called when an action is performed.
				 * 
				 * @param e The ActionEvent.
				 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    Whisper.exit(0);
                }
            });
        }
        return jbExit;
    }

    /**
	 * Get the error label. If it is not constructed, it is constructed on the first call.
	 * 
	 * @return The error label.
	 */
    private JLabel getJlError() {
        if (jlError == null) {
            jlError = new JLabel();
            jlError.setForeground(Color.RED);
            jlError.setPreferredSize(new Dimension(10, 20));
        }
        return jlError;
    }

    /**
	 * Set the enabled state of all controls.
	 * 
	 * @param enabled The enabled state to set.
	 */
    private void setControlState(final boolean enabled) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is run.
					 */
                    @Override
                    public void run() {
                        setControlState(enabled);
                    }
                });
            } catch (Exception e) {
                if (Whisper.isDebugging()) {
                    e.printStackTrace();
                }
            }
        } else {
            getJbExit().setEnabled(enabled);
            getJbLogin().setEnabled(enabled);
            getJtfFirstName().setEnabled(enabled);
            getJtfHost().setEnabled(enabled);
            getJtfLastName().setEnabled(enabled);
            getJchkSaveDetails().setEnabled(enabled);
            getJcbLocation().setEnabled(enabled);
            getJchkTimeStamps().setEnabled(enabled);
            getJpPassword().setEnabled(enabled);
            getJchkSavePassword().setEnabled(enabled);
        }
    }

    /**
	 * Validate all settings.
	 */
    private boolean validateSettings() {
        boolean valid = true;
        if (!validateField(getJtfFirstName(), getJlFirstName())) {
            valid = false;
        }
        if (!validateField(getJtfLastName(), getJlLastName())) {
            valid = false;
        }
        if (!validateField(getJpPassword(), getJlPassword())) {
            valid = false;
        }
        if (!validateHost()) {
            valid = false;
        }
        getJbLogin().setEnabled(valid && !getLogoPanel().getIsConnecting());
        return valid;
    }

    /**
	 * Set the visibility of the form.
	 * 
	 * @param visible True to be visible, false to be invisible.
	 */
    @Override
    public void setVisible(boolean visible) {
        getLogoPanel().setConnecting(false);
        super.setVisible(visible);
        if (!Config.getConfig().savePassword) getJpPassword().setText("");
        if (getJtfFirstName().getText().trim().length() > 0 && getJtfLastName().getText().trim().length() > 0) getJpPassword().requestFocus();
    }

    /**
	 * Perform the login.
	 */
    private void doLogin() {
        new Thread(new Runnable() {

            /**
			 * Called when the connection thread starts.
			 */
            @Override
            public void run() {
                getLogoPanel().setConnecting(true);
                setControlState(false);
                getJlError().setText("");
                Whisper.getClient().initialise();
                try {
                    Whisper.getClient().connect();
                } catch (Exception ex) {
                    getJlError().setText("Error connecting to transport");
                    getLogoPanel().setConnecting(false);
                    setControlState(true);
                    return;
                }
                int spinTime = 0;
                while (!Whisper.getClient().getConnection().getActive() || !Whisper.getClient().getConnection().getHandShook()) {
                    try {
                        if (spinTime > 10 * Config.CONNECT_TIMEOUT || Whisper.getClient().getConnection().getTimedOut()) {
                            break;
                        }
                        spinTime++;
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
                if (Whisper.getClient().getConnection().getActive() && Whisper.getClient().getConnection().getHandShook()) {
                    new RequestLogin(Whisper.getClient().getConnection(), Config.getConfig().firstName, Config.getConfig().lastName, Config.getConfig().password, Config.getConfig().homeStartLocation).execute();
                    new Thread(new Runnable() {

                        /**
						 * Called when the thread is run.
						 */
                        @Override
                        public void run() {
                            int spinTime = 0;
                            while (!Whisper.getClient().getLoginStatus() && !Whisper.getClient().getAbortLogin() && Whisper.getClient().getConnection().getActive()) {
                                if (spinTime > 10 * Config.LOGIN_TIMEOUT) {
                                    break;
                                }
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                }
                                if (!TosUpdateDialog.dialogIsVisible()) {
                                    spinTime++;
                                }
                            }
                            if (!Whisper.getClient().getLoginStatus() || Whisper.getClient().getAbortLogin()) {
                                getJlError().setText("Error logging in to Second Life");
                                getLogoPanel().setConnecting(false);
                                setControlState(true);
                                Whisper.getClient().disconnect(false);
                            }
                        }
                    }).start();
                } else {
                    Whisper.getClient().disconnect(false);
                    getLogoPanel().setConnecting(false);
                    getJlError().setText("Connection timeout");
                    setControlState(true);
                }
            }
        }).start();
    }
}
