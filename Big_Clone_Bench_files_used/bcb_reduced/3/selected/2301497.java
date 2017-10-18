package net.sf.genedator.plugin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.sf.genedator.plugin.utils.PluginCategory;
import net.sf.genedator.plugin.utils.SimplePluginDataGenerator;

/**
 * @author Anca Zapuc, anca.zapuc@gmail.com
 */
public class RandomPasswordGenerator extends SimplePluginDataGenerator {

    /** Constants **/
    private static final String SPECIAL_CHARACTERS = "special characters";

    private static final String SMALL_LETTERS = "small letters";

    private static final String CAPITAL_LETTERS = "capital letters";

    private static final String NONE = "none";

    private static final String SHA1 = "SHA1";

    private static final String MD5 = "MD5";

    private static final String MAX_NUMBER = "MAX_NUMBER";

    public static final String[] ADDITIONAL = { SPECIAL_CHARACTERS, SMALL_LETTERS, CAPITAL_LETTERS };

    public static final String[] HASH = { NONE, SHA1, MD5 };

    public static final String[] SPECIAL_CHARS = { "?", "/", ".", ":", "\\", "@", "%", ";", "-", "_" };

    public static String[] LETTERS = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };

    public static final String MIN_DIGITS = "MIN_DIGITS";

    public static final String ADDITIONAL_LIST = "ADDITIONAL_LIST";

    public static final String MIN_LENGTH = "MIN_LENGTH";

    public static final String MAX_LENGTH = "MAX_LENGTH";

    public static final String HASH_METHOD = "HASH_METHOD";

    private JCheckBox[] additionalCheckbox;

    private JRadioButton[] additionalHashRadio;

    private JSpinner passwordMinLengthSpinner;

    private JSpinner passwordMaxLengthSpinner;

    private JSpinner passwordMinDigitNrSpinner;

    private List<String> additionalList;

    private Integer minLength, maxLength, minDigits;

    private String hashMethod;

    private Integer maxNumber;

    public static final String PASSWORD_SEPARATOR = "###";

    public RandomPasswordGenerator() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 2));
        passwordMinLengthSpinner = new JSpinner();
        passwordMinLengthSpinner.setModel(new SpinnerNumberModel(2, 2, 20, 1));
        JPanel passwordMinLengthSpinnerWrapper = new JPanel();
        passwordMinLengthSpinnerWrapper.setLayout(new FlowLayout(FlowLayout.LEFT));
        passwordMinLengthSpinnerWrapper.add(passwordMinLengthSpinner, BorderLayout.WEST);
        optionsPanel.add(new JLabel("Password Min Length:"));
        optionsPanel.add(passwordMinLengthSpinnerWrapper);
        passwordMaxLengthSpinner = new JSpinner();
        passwordMaxLengthSpinner.setModel(new SpinnerNumberModel(7, 7, 20, 1));
        JPanel passwordMaxLengthSpinnerWrapper = new JPanel();
        passwordMaxLengthSpinnerWrapper.setLayout(new FlowLayout(FlowLayout.LEFT));
        passwordMaxLengthSpinnerWrapper.add(passwordMaxLengthSpinner, BorderLayout.WEST);
        optionsPanel.add(new JLabel("Password Max Length:"));
        optionsPanel.add(passwordMaxLengthSpinnerWrapper);
        passwordMinDigitNrSpinner = new JSpinner();
        passwordMinDigitNrSpinner.setModel(new SpinnerNumberModel(0, 0, 20, 1));
        JPanel passworMinDigitNrSpinnerWrapper = new JPanel();
        passworMinDigitNrSpinnerWrapper.setLayout(new FlowLayout(FlowLayout.LEFT));
        passworMinDigitNrSpinnerWrapper.add(passwordMinDigitNrSpinner, BorderLayout.WEST);
        optionsPanel.add(new JLabel("Password Min Digits Number:"));
        optionsPanel.add(passworMinDigitNrSpinnerWrapper);
        JPanel additionalMainPanel = new JPanel();
        additionalMainPanel.setLayout(new GridLayout(0, 2));
        JPanel additionalPanel = new JPanel();
        additionalPanel.setLayout(new GridLayout(0, 1));
        JLabel additionalLabel = new JLabel("Choose:");
        additionalCheckbox = new JCheckBox[ADDITIONAL.length];
        for (int i = 0; i < ADDITIONAL.length; i++) {
            additionalCheckbox[i] = new JCheckBox(ADDITIONAL[i]);
            additionalCheckbox[i].setSelected(true);
            additionalPanel.add(additionalCheckbox[i]);
        }
        additionalMainPanel.add(additionalLabel);
        additionalMainPanel.add(additionalPanel);
        JPanel additionalHashMainPanel = new JPanel();
        additionalHashMainPanel.setLayout(new GridLayout(0, 2));
        JPanel additionalHashPanel = new JPanel();
        additionalHashPanel.setLayout(new GridLayout(0, 1));
        JLabel additionalHashLabel = new JLabel("Choose encryption type:");
        additionalHashRadio = new JRadioButton[HASH.length];
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < HASH.length; i++) {
            additionalHashRadio[i] = new JRadioButton(HASH[i]);
            additionalHashRadio[i].setSelected(true);
            additionalHashPanel.add(additionalHashRadio[i]);
            bg.add(additionalHashRadio[i]);
        }
        additionalHashMainPanel.add(additionalHashLabel);
        additionalHashMainPanel.add(additionalHashPanel);
        mainPanel.add(optionsPanel);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(additionalMainPanel);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(additionalHashMainPanel);
        mainPanel.add(Box.createVerticalStrut(14));
    }

    @Override
    public void readConfigParameters() {
        super.readConfigParameters();
        additionalList = new ArrayList<String>();
        for (int i = 0; i < ADDITIONAL.length; i++) {
            if (additionalCheckbox[i].isSelected()) {
                additionalList.add(ADDITIONAL[i]);
            }
        }
        getPluginConfiguration().setParameter(ADDITIONAL_LIST, additionalList);
        Integer minLength = Integer.valueOf(passwordMinLengthSpinner.getValue().toString());
        getPluginConfiguration().setParameter(MIN_LENGTH, minLength);
        Integer maxLength = Integer.valueOf(passwordMaxLengthSpinner.getValue().toString());
        getPluginConfiguration().setParameter(MAX_LENGTH, maxLength);
        Integer minDigits = null;
        minDigits = Integer.valueOf(passwordMinDigitNrSpinner.getValue().toString());
        getPluginConfiguration().setParameter(MIN_DIGITS, minDigits);
        getPluginConfiguration().setParameter(MAX_NUMBER, (int) Math.pow(10, 20));
        for (JRadioButton button : additionalHashRadio) {
            if (button.isSelected()) {
                getPluginConfiguration().setParameter(HASH_METHOD, button.getText());
                break;
            }
        }
    }

    @Override
    public void firstStep() {
        additionalList = (List<String>) getPluginConfiguration().getObject(ADDITIONAL_LIST, Arrays.asList(ADDITIONAL));
        minLength = getPluginConfiguration().getInt(MIN_LENGTH, 2);
        maxLength = getPluginConfiguration().getInt(MAX_LENGTH, 20);
        minDigits = getPluginConfiguration().getInt(MIN_DIGITS, 5);
        hashMethod = getPluginConfiguration().getString(HASH_METHOD, HASH[0]);
        maxNumber = getPluginConfiguration().getInt(MAX_NUMBER, Integer.MAX_VALUE - 1);
    }

    private StringBuffer generateComponent(int size, List<String> list) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 1; i <= size; i++) {
            String value = getRand(list);
            buffer.append(value);
        }
        return buffer;
    }

    @Override
    public String getRandomValue() {
        int passwordLength = generateNumberFromIntervalClosed(minLength, maxLength);
        List<StringBuffer> passwordComponents = new ArrayList<StringBuffer>();
        passwordComponents.add(new StringBuffer(String.valueOf(generateNumberFromIntervalOpen((int) Math.pow(10, minDigits - 1), (int) Math.pow(10, minDigits)))));
        int rest = passwordLength - minDigits;
        boolean specialChars = false;
        boolean smallLetters = false;
        boolean capitalLetters = false;
        for (String additional : additionalList) {
            if (SPECIAL_CHARACTERS.equals(additional)) {
                specialChars = true;
            } else if (SMALL_LETTERS.equals(additional)) {
                smallLetters = true;
            } else if (CAPITAL_LETTERS.equals(additional)) {
                capitalLetters = true;
            }
        }
        StringBuffer specialCharsPart = generatePasswordPart(specialChars, rest, Arrays.asList(SPECIAL_CHARS));
        if (specialCharsPart != null) {
            rest -= specialCharsPart.length();
            passwordComponents.add(specialCharsPart);
        }
        StringBuffer smallLettersPart = generatePasswordPart(smallLetters, rest, Arrays.asList(LETTERS));
        if (smallLettersPart != null) {
            rest -= smallLettersPart.length();
            passwordComponents.add(smallLettersPart);
        }
        StringBuffer capitalLettersPart = generatePasswordPart(capitalLetters, rest, Arrays.asList(LETTERS));
        if (capitalLettersPart != null) {
            rest -= capitalLettersPart.length();
            capitalLettersPart = new StringBuffer(capitalLettersPart.toString().toUpperCase());
            passwordComponents.add(capitalLettersPart);
        }
        if (rest > 0) {
            passwordComponents.add(new StringBuffer(String.valueOf(generateNumberFromIntervalOpen((int) Math.pow(10, rest - 1), (int) Math.pow(10, rest)))));
        }
        List<StringBuffer> orderedPasswordComponents = generatePassword(passwordComponents);
        StringBuffer finalPassword = new StringBuffer();
        for (StringBuffer component : orderedPasswordComponents) {
            finalPassword.append(component.toString().trim());
        }
        StringBuffer hashedPassword = new StringBuffer();
        if (!NONE.equals(hashMethod)) {
            hashedPassword = hashPassword(finalPassword, hashMethod).append(PASSWORD_SEPARATOR).append(finalPassword);
            finalPassword = hashedPassword;
        }
        return finalPassword.toString();
    }

    @Override
    public long getUniqueAmount() {
        return maxNumber;
    }

    @Override
    public String getAboutInfo() {
        return "Allows generatinng of a random password";
    }

    @Override
    public String getAuthor() {
        return "Anca Zapuc";
    }

    @Override
    public String getCategory() {
        return PluginCategory.PASSWORD;
    }

    @Override
    public String getHelpPagePath() {
        return "help.html";
    }

    private String getRand(List<String> values) {
        int pos = getRandom().nextInt(values.size());
        return values.get(pos);
    }

    @Override
    public String getName() {
        return "Random Password Generator";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    private static int generateNumberFromIntervalClosed(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    private static int generateNumberFromIntervalOpen(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    private List<StringBuffer> generatePassword(List<StringBuffer> passwordComponents) {
        List<Integer> positions = new ArrayList<Integer>();
        for (int i = 0; i < passwordComponents.size(); i++) positions.add(i);
        StringBuffer[] finalPassword = new StringBuffer[positions.size()];
        for (StringBuffer component : passwordComponents) {
            int pos = getRandom().nextInt(positions.size());
            int newPos = Integer.valueOf(positions.get(pos));
            finalPassword[newPos] = component;
            positions.remove(positions.get(pos));
        }
        return Arrays.asList(finalPassword);
    }

    private StringBuffer generatePasswordPart(boolean condition, int remainingLength, List<String> list) {
        StringBuffer component = null;
        if (condition && remainingLength > 0) {
            int length = 0;
            if (remainingLength == 1) length = 1; else length = generateNumberFromIntervalClosed(1, remainingLength);
            component = generateComponent(length, list);
        }
        return component;
    }

    private StringBuffer hashPassword(StringBuffer password, String mode) {
        MessageDigest m = null;
        StringBuffer hash = new StringBuffer();
        try {
            m = MessageDigest.getInstance(mode);
            m.update(password.toString().getBytes("UTF8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] digest = m.digest();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(digest[i]);
            if (hex.length() == 1) hex = "0" + hex;
            hex = hex.substring(hex.length() - 2);
            hash.append(hex);
        }
        return hash;
    }
}
