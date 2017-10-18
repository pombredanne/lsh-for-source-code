package org.jgenesis.swing.models;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.NumberFormat;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgenesis.helper.Digest;
import org.jgenesis.helper.MaskFormat;
import org.jgenesis.helper.Util;

/**
 * @author regis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BaseTextDocument extends PlainDocument implements Serializable {

    static Log log = LogFactory.getLog(BaseTextDocument.class);

    private NumberFormat numberFormat;

    private DateFormat dateFormat;

    private MaskFormat maskFormat;

    private boolean passwordEnable;

    private String password;

    private String digestAlgorithm;

    private String digestPassword;

    private char passwordEchoChar;

    protected String passwordEchoString;

    private int maxLength = -1;

    public BaseTextDocument() {
        super();
        setPasswordEchoChar('●');
    }

    public void refreshValue() {
        try {
            this.replace(0, this.getLength(), this.getText(0, this.getLength()), null);
        } catch (BadLocationException e) {
            log.error("", e);
        }
    }

    public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (maxLength != -1 && text != null && text.length() > maxLength) text = text.substring(0, maxLength);
        super.replace(offset, length, text, attrs);
    }

    /**
	 * @return Returns the dateFormat.
	 */
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
	 * @param dateFormat The dateFormat to set.
	 */
    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
	 * @return Returns the maskFormat.
	 */
    public MaskFormat getMaskFormat() {
        return maskFormat;
    }

    /**
	 * @param maskFormat The maskFormat to set.
	 */
    public void setMaskFormat(MaskFormat maskFormat) {
        this.maskFormat = maskFormat;
    }

    /**
	 * @return Returns the numberFormat.
	 */
    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    /**
	 * @param numberFormat The numberFormat to set.
	 */
    public void setNumberFormat(NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }

    /**
	 * @return Returns the passwordEnable.
	 */
    public boolean isPasswordEnable() {
        return passwordEnable;
    }

    /**
	 * @param passwordEnable The passwordEnable to set.
	 */
    public void setPasswordEnable(boolean passwordEnable) {
        this.passwordEnable = passwordEnable;
    }

    /**
	 * @return Returns the digestAlgorithm.
	 */
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
	 * @param digestAlgorithm The digestAlgorithm to set.
	 */
    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    /**
	 * @return Returns the password.
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * @param password The password to set.
	 */
    public void setPassword(String password) {
        if (!passwordEnable) {
            log.warn("passwordEnable should be true if you intend to set password attribute.");
            return;
        }
        this.password = password;
        try {
            this.replace(0, this.getLength(), Util.fillString(this.getPasswordEchoChar(), password != null ? password.length() : 0), null);
        } catch (BadLocationException e) {
            log.debug(e.getMessage());
        }
        if (this.getDigestAlgorithm() != null && this.getDigestAlgorithm().length() > 0) {
            this.digestPassword = password == null || password.length() == 0 ? password : Digest.digest(this.getPassword(), getDigestAlgorithm());
        }
    }

    /**
	 * @return Returns the passwordEchoChar.
	 */
    public char getPasswordEchoChar() {
        return passwordEchoChar;
    }

    /**
	 * @param passwordEchoChar The passwordEchoChar to set.
	 */
    public void setPasswordEchoChar(char passwordEchoChar) {
        this.passwordEchoChar = passwordEchoChar;
        passwordEchoString = Util.fillString(passwordEchoChar, 10);
    }

    /**
	 * @return Returns the digestPassword.
	 */
    public String getDigestPassword() {
        return digestPassword;
    }

    /**
	 * @param digestPassword The digestPassword to set.
	 */
    public void setDigestPassword(String digestPassword) {
        this.digestPassword = digestPassword;
        this.password = null;
        if (digestPassword != null && digestPassword.length() > 0) {
            try {
                this.replace(0, this.getLength(), passwordEchoString, null);
            } catch (BadLocationException e) {
                log.debug(e.getMessage());
            }
        }
    }

    /**
	 * @return Returns the maxLength.
	 */
    public int getMaxLength() {
        return maxLength;
    }

    /**
	 * @param maxLength The maxLength to set.
	 */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
