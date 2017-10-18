package org.jgenesis.swing.models;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.text.*;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgenesis.bean.InaccessibleProperty;
import org.jgenesis.beanset.BeanSet;
import org.jgenesis.beanset.BeanManager;
import org.jgenesis.beanset.BeanSetEvent;
import org.jgenesis.beanset.BeanSetException;
import org.jgenesis.beanset.BeanSetListener;
import org.jgenesis.beanset.DefaultBeanSetListener;
import org.jgenesis.beanset.JGInternalBeanSetListener;
import org.jgenesis.helper.DateUtils;
import org.jgenesis.helper.Digest;
import org.jgenesis.helper.MaskFormatException;

/**
 * BeanTextDocument class that serves as BeanSet model for swing text fields and
 * "lookup" text fields.
 */
public class BeanTextDocument extends BaseTextDocument implements Serializable {

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
    private static final long serialVersionUID = -6281540343027205131L;

    private static Log log = LogFactory.getLog(BeanTextDocument.class);

    private BeanManager beanManager;

    private String fieldName;

    private JGInternalBeanSetListener beanSetListener;

    private PropertyChangeListener beanSetPropertyChangeListener;

    private PropertyChangeSupport propertySupport;

    private boolean internalChange;

    private boolean notifyBeanSet = true;

    /**
     * Constructs a new BeanTextDocument.
     */
    public BeanTextDocument() {
        init();
    }

    /**
     * Constructs a new BeanTextDocument that will be used in a Swing text field,
     * defining its BeanManager and BeanManager field name.
     * @param beanManager BeanManager that will be used by this BeanTextDocument.
     * @param fieldName BeanManager field name used by this BeanTextDocument.
     */
    public BeanTextDocument(BeanManager beanManager, String fieldName) {
        this(beanManager, fieldName, -1);
    }

    /**
     * Constructs a new BeanTextDocument that will be used in a Swing text field,
     * defining its BeanManager and BeanManager field name.
     * @param beanManager BeanManager that will be used by this BeanTextDocument.
     * @param fieldName BeanManager field name used by this BeanTextDocument.
     */
    public BeanTextDocument(BeanManager beanManager, String fieldName, int maxLength) {
        this.beanManager = beanManager;
        this.setFieldName(fieldName);
        init();
    }

    /**
     * Constructs a new BeanTextDocument that will be used in a Swing "lookup" text field.
     * Whenever the Master BeanSet is changed the "lookup" BeanTextDocument is also changed,
     * reflecting the relationship between the master and the detail fields.
     * The fields (separated by ";") defined in fieldNamesDisplay will be shown. The delimiter 
     * specified by the user will separate the fields in the BeanTextDocument. 
     * "isCached" is used to tell if the detail BeanSet will be be stored in memory
     * (cached) or not (no cache).
     * @param masterBeanSet Master BeanSet that will be used by this BeanTextDocument.
     * @param fieldName Master BeanSet field name used by this BeanTextDocument.
     * @param detailBeanSet Detail BeanSet used in a "lookup" BeanTextDocument.
     * @param fieldNameData Detail BeanSet field name used as key in a "lookup" BeanTextDocument.
     * @param fieldNamesDisplay Detail BeanSet field name(s) used as value in a "lookup"
     *        BeanTextDocument. These are the field names that will be "displayed" in the
     *        BeanTextDocument.
     * @param delimiter Used to separate field names that will be stored in the BeanTextDocument.
     * @param isCached Used to define a detail BeanSet in a "lookup" BeanTextDocument
     *        that will be completely loaded into memory (cached) or that will not be stored
     *        in memory (not cached).
     * @deprecated Use JGBeanTextDocument.setMaxLength()
     */
    public BeanTextDocument(BeanSet masterBeanSet, String fieldName, int maxLength) {
        this.setFieldName(fieldName);
        init();
    }

    /**
	 * Registers listeners to BeanSet and Document events and updates the BeanTextDocument.
	 */
    private void init() {
        if (this.propertySupport == null) this.propertySupport = new PropertyChangeSupport(this);
        if (this.beanSetListener == null) beanSetListener = new JGInternalBeanSetListener(this) {

            public void beanSetChanged(BeanSetEvent beanSetEvent) {
                cursorMoved(beanSetEvent);
            }

            public void itemChanged(BeanSetEvent beanSetEvent) {
                cursorMoved(beanSetEvent);
            }

            public void cursorMoved(BeanSetEvent beanSetEvent) {
                if (!internalChange) refreshValue(getValue());
            }

            public void stateChanged(BeanSetEvent beanSetEvent) {
                if (!beanManager.isEditState()) cursorMoved(beanSetEvent);
            }

            public void deleteItem(BeanSetEvent beanSetEvent) {
                try {
                    int index = beanManager.getBeanSet().getCurrentPosition();
                    refreshValue(index >= 0 ? beanManager.getBeanSet().getBeanWrapperAt(index).getFieldValue(fieldName) : null);
                } catch (BeanSetException e) {
                    log.error(e.getMessage(), e);
                }
            }
        };
        this.addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent documentevent) {
                if (!internalChange && BeanTextDocument.this.notifyBeanSet) {
                    if (!internalChange && BeanTextDocument.this.notifyBeanSet) try {
                        internalChange = true;
                        updateBeanSet();
                    } finally {
                        internalChange = false;
                    }
                }
            }

            public void removeUpdate(DocumentEvent documentevent) {
                insertUpdate(documentevent);
            }

            public void changedUpdate(DocumentEvent documentevent) {
                insertUpdate(documentevent);
            }
        });
        beanSetPropertyChangeListener = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (BeanTextDocument.this.fieldName == null) return;
                if (!internalChange && BeanTextDocument.this.fieldName.contains(evt.getPropertyName())) {
                    if (log.isDebugEnabled()) log.debug("Editting field: " + BeanTextDocument.this.fieldName);
                    refreshValue(getValue());
                }
            }
        };
        if (beanManager != null) {
            beanManager.getBeanSet().addBeanSetListener(beanSetListener);
            beanManager.getBeanSet().addPropertyChangeListener(this.beanSetPropertyChangeListener);
        }
        refreshValue(getValue());
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public void setBeanManager(BeanManager beanManager) {
        if (this.beanManager == beanManager) return;
        Object oldBeanManager = this.beanManager;
        if (this.beanManager != null) {
            this.beanManager.getBeanSet().removeBeanSetListener(beanSetListener);
            this.beanManager.getBeanSet().removePropertyChangeListener(this.beanSetPropertyChangeListener);
        }
        this.beanManager = beanManager;
        if (this.beanManager != null) {
            this.beanManager.getBeanSet().addBeanSetListener(beanSetListener);
            this.beanManager.getBeanSet().addPropertyChangeListener(this.beanSetPropertyChangeListener);
        }
        refreshValue(getValue());
        this.propertySupport.firePropertyChange("beanManager", oldBeanManager, beanManager);
    }

    /**
    * Returns the field name that will be used in a BeanTextDocument or the master
    * BeanSet field name used as key in a "lookup" BeanTextDocument.
    * @return Field name.
    */
    public String getFieldName() {
        return fieldName;
    }

    /**
	 * Defines field name that will be used in a BeanTextDocument or the master
	 * BeanSet field name used as key in a "lookup" BeanTextDocument.
	 * @param fieldName Field name.
	 */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
        if (this.beanManager != null) refreshValue(getValue());
    }

    /**
	 * Returns the current BeanSet object.
	 * @return Current BeanSet object.
	 */
    public Object getValue() {
        if (this.beanManager == null) return fieldName;
        Object value = null;
        if (fieldName != null) value = beanManager.getCurrentBean().getFieldValue(fieldName);
        return value;
    }

    public void refreshValue() {
        refreshValue(getValue());
    }

    /**
     * Updates the Document with the defined object.
     * @param value Object used to update the Document.
     */
    private void refreshValue(Object value) {
        String strValue;
        if (this.isPasswordEnable()) {
            if (value == null || (value instanceof String && ((String) value).length() == 0)) strValue = ""; else {
                strValue = passwordEchoString;
            }
            if (this.getDigestAlgorithm() == null || this.getDigestAlgorithm().length() == 0) this.setPassword((String) value); else this.setDigestPassword((String) value);
        } else if (value == null || value instanceof InaccessibleProperty) {
            strValue = "";
        } else if (getDateFormat() != null) try {
            Class fieldClass = this.beanManager.getCurrentBean().getFieldClass(fieldName);
            if (!Date.class.isAssignableFrom(fieldClass)) {
                if (fieldClass == int.class || fieldClass == long.class || fieldClass == String.class || fieldClass == Integer.class || fieldClass == Long.class) try {
                    value = DateUtils.stringToDate(String.valueOf(value), ((SimpleDateFormat) this.getDateFormat()).toPattern());
                } catch (IllegalArgumentException ex) {
                    if (log.isDebugEnabled()) log.debug(ex.getMessage());
                    value = "";
                }
            }
            strValue = getDateFormat().format(value);
        } catch (IllegalArgumentException e) {
            strValue = "";
            if (log.isWarnEnabled()) log.warn("The given date pattern (" + ((SimpleDateFormat) getDateFormat()).toPattern() + ") cannot format: " + value);
        } else if (getNumberFormat() != null && (float.class.isAssignableFrom(value.getClass()) || value instanceof Float || double.class.isAssignableFrom(value.getClass()) || value instanceof Double)) try {
            strValue = getNumberFormat().format(value);
        } catch (IllegalArgumentException e) {
            strValue = "";
            if (log.isWarnEnabled()) log.warn("The given number cannot be formatted: " + value);
        } else if (getMaskFormat() != null) try {
            strValue = getMaskFormat().format(value instanceof String ? ((String) value).trim() : value != null ? value.toString() : "");
        } catch (MaskFormatException e) {
            strValue = "";
            if (log.isWarnEnabled()) log.warn("The given mask (" + getMaskFormat().getPattern() + ") cannot format: " + value);
        } else strValue = value.toString();
        this.refreshString(strValue);
    }

    /**
     * Updates the Document with the defined String.
     * @param str String used to update the Document.
     */
    protected void refreshString(String str) {
        try {
            if (!internalChange) if (str != null && !str.equals(this.getText(0, this.getLength()))) {
                boolean oldInternalChange = internalChange;
                try {
                    internalChange = true;
                    this.replace(0, this.getLength(), str, null);
                } finally {
                    internalChange = oldInternalChange;
                }
            }
        } catch (BadLocationException badlocationexception) {
            log.error(badlocationexception.getMessage(), badlocationexception);
        }
    }

    public void replace(int start, int length, String value, AttributeSet atts) throws BadLocationException {
        if (internalChange || (beanManager != null && !beanManager.isReadOnly())) {
            super.replace(start, length, value, atts);
        }
    }

    /**
     * Updates the current master BeanSet object with the BeanTextDocument value.
     */
    protected void updateBeanSet() {
        try {
            if (beanManager != null) {
                Object value = null;
                try {
                    if (this.isPasswordEnable()) {
                        if (this.getDigestAlgorithm() == null || this.getDigestAlgorithm().length() == 0) value = this.getLength() == 0 ? null : this.getPassword(); else value = this.getLength() == 0 ? null : Digest.digest(this.getPassword(), getDigestAlgorithm());
                    } else if (this.getDateFormat() != null) {
                        value = this.getDateFormat().parse(this.getText(0, this.getLength()));
                        Class fieldClass = beanManager.getCurrentBean().getFieldClass(fieldName);
                        if (!Date.class.isAssignableFrom(fieldClass)) {
                            if (fieldClass == int.class || fieldClass == Integer.class) value = Integer.parseInt(DateUtils.dateToString((Date) value, ((SimpleDateFormat) this.getDateFormat()).toPattern())); else if (fieldClass == String.class) value = DateUtils.dateToString((Date) value, ((SimpleDateFormat) this.getDateFormat()).toPattern());
                        }
                    } else if (this.getNumberFormat() != null) {
                        String str = this.getText(0, this.getLength());
                        value = !str.equals("") ? this.getNumberFormat().parse(str) : "0";
                    } else if (this.getMaskFormat() != null) value = this.getMaskFormat().parse(this.getText(0, this.getLength())); else value = this.getText(0, this.getLength());
                    if (fieldName != null) beanManager.getCurrentBean().setFieldValue(fieldName, value);
                } catch (ParseException ex) {
                    if (log.isDebugEnabled()) log.debug(ex.getMessage(), ex);
                }
            }
        } catch (BadLocationException badlocationexception) {
            if (log.isDebugEnabled()) log.debug(badlocationexception.getMessage(), badlocationexception);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertySupport.removePropertyChangeListener(listener);
    }

    public boolean isNotifyBeanSet() {
        return notifyBeanSet;
    }

    public void setNotifyBeanSet(boolean notifyBeanSet) {
        this.notifyBeanSet = notifyBeanSet;
    }
}
