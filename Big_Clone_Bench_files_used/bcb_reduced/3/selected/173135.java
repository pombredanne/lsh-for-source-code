package com.hisham.creditcard.touchnet.util;

import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import com.hisham.creditcard.touchnet.*;
import com.hisham.util.UtilityFunctions;
import com.misc.*;

/**
 *
 * <p>Title: TnTransactionResources</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 *
 * @author Ali Hisham Malik
 * @version 2.0
 */
public class TnTransactionResources implements Serializable {

    Logger LOG = Logger.getLogger(TnTransactionResources.class);

    /**
	 * 
	 */
    private static final long serialVersionUID = 1611324905513537440L;

    public static final String AUTHORIZE = "authorize";

    public static final String CREDIT = "credit";

    public static final String DELETE = "delete";

    public static final String SETTLE = "settle";

    public static final String SPLITSETTLE = "splitsettle";

    public static final String touchnetResources = "TouchnetResources";

    private String postingKey;

    private String upaySiteId;

    private static TnTransactionResources tnTransactionResources = null;

    private String remoteAddrStart;

    private Date disallowPostingStartTime;

    private Date disallowPostingEndTime;

    public Date getDisallowPostingEndTime() {
        return disallowPostingEndTime;
    }

    public void setDisallowPostingEndTime(Date disallowPostingEndTime) {
        this.disallowPostingEndTime = disallowPostingEndTime;
    }

    public Date getDisallowPostingStartTime() {
        return disallowPostingStartTime;
    }

    public void setDisallowPostingStartTime(Date disallowPostingStartTime) {
        this.disallowPostingStartTime = disallowPostingStartTime;
    }

    public static TnTransactionResources getInstance() {
        if (tnTransactionResources == null) return tnTransactionResources = new TnTransactionResources();
        return tnTransactionResources;
    }

    private TnTransactionResources() {
        init();
    }

    /**
	 *
	 * @param htmlSerialNumber String
	 */
    public void setPostingKey(String postingKey) {
        this.postingKey = postingKey;
    }

    /**
	 *
	 * @return String
	 */
    public String getPostingKey() {
        return postingKey;
    }

    /**
	 * initialize the provider
	 *
	 */
    public void init() {
        ResourceBundle touchnetBundle = ResourceBundle.getBundle(touchnetResources);
        this.setPostingKey(touchnetBundle.getString("postingKey"));
        this.setUpaySiteId(touchnetBundle.getString("upay.site.id"));
        this.setRemoteAddrStart(touchnetBundle.getString("upay.site.address.start"));
        DateFormat df = new SimpleDateFormat("h:mm a z");
        try {
            this.setDisallowPostingStartTime(df.parse(touchnetBundle.getString("posting.disallow.time.start")));
            this.setDisallowPostingEndTime(df.parse(touchnetBundle.getString("posting.disallow.time.end")));
        } catch (ParseException e) {
            LOG.warn("DisallowPostingTime not set.");
            Date date = Calendar.getInstance().getTime();
            this.setDisallowPostingEndTime(date);
            this.setDisallowPostingStartTime(date);
        }
    }

    /**
	 * AUTHORIZE transaction change status request
	 * @param orderNumber String
	 * @param orderAmount String
	 * @param transactionId String
	 * @param forceSettlement String
	 * @return boolean
	 */
    public boolean changeTransactionStatusAuthorize(String orderNumber, String orderAmount, String transactionId, String forceSettlement) {
        return (this.changeTransactionStatus("AUTHORIZE", orderNumber, orderAmount, transactionId, forceSettlement));
    }

    /**
	 *
	 * @param orderCommand String
	 * @param orderNumber String
	 * @param orderAmount double
	 * @param transactionID String
	 * @param forceSettlement boolean
	 * @return boolean
	 */
    public boolean changeTransactionStatus(String orderCommand, String orderNumber, double orderAmount, String transactionID, boolean forceSettlement) {
        DecimalFormat formatter = new DecimalFormat("#.00");
        String transactionAmount = formatter.format(orderAmount);
        return changeTransactionStatus(orderCommand, orderNumber, transactionAmount, transactionID, forceSettlement ? "1" : "0");
    }

    /**
	 * transaction change status request
	 * @param orderCommand String
	 * @param orderNumber String
	 * @param orderAmount String
	 * @param transactionId String
	 * @param forceSettlement String
	 * @return boolean
	 */
    private boolean changeTransactionStatus(String orderCommand, String orderNumber, String orderAmount, String transactionId, String forceSettlement) {
        throw new java.lang.UnsupportedOperationException("Method not yet implemented");
    }

    /**
	 *
	 * @return String
	 */
    public String getUpaySiteId() {
        return upaySiteId;
    }

    /**
	 *
	 * @param developerSerialNumber String
	 */
    public void setUpaySiteId(String upaySiteId) {
        this.upaySiteId = upaySiteId;
    }

    public void setRemoteAddrStart(String remoteAddrStart) {
        this.remoteAddrStart = remoteAddrStart;
    }

    /**
	 *
	 * @param oos ObjectOutputStream
	 * @throws IOException
	 */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    /**
	 *
	 * @param ois ObjectInputStream
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    public String getRemoteAddrStart() {
        return remoteAddrStart;
    }

    /**
	 * constructs the order string for transaction post
	 * @param itemNumber String
	 * @param itemDescription String
	 * @param itemCost String
	 * @param itemQuantity String
	 * @param itemTaxable String
	 * @return String
	 */
    public String getOrderString(String itemNumber, String itemDescription, String itemCost, String itemQuantity, String itemTaxable) {
        StringBuffer sb = new StringBuffer();
        sb.append(itemNumber);
        sb.append("~");
        sb.append(itemDescription);
        sb.append("~");
        sb.append(itemCost);
        sb.append("~");
        sb.append(itemQuantity);
        sb.append("~");
        sb.append(itemTaxable);
        sb.append("~||");
        return (sb.toString());
    }

    /** authorize credit card
	 *
	 * The method AuthorizeAPI returns two lines of text, as a form post.
	 * The data will be separated by a Carriage return and a line feed.
	 * The field names will be in the first line and their values in the second.
	 * The field names returned are only those provided in the standard Skipjack
	 * response Table.
	 * The field names returned do not include UserDefinedFields.
	 *
	 * @param tnTransactionInfo TnTransactionInfo
	 * @param tnTransactionResponse TnTransactionResponse
	 * @return boolean
	 */
    public boolean approveTransaction(TnTransactionInfo tnTransactionInfo, TnTransactionResponse tnTransactionResponse) {
        throw new java.lang.UnsupportedOperationException("Communication with touchnet not implemented");
    }

    public String getValidationKey(String transactionId, double transactionAmount) {
        try {
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("MD5");
            d.reset();
            String value = this.getPostingKey() + transactionId + transactionAmount;
            d.update(value.getBytes());
            byte[] buf = d.digest();
            return Base64.encodeBytes(buf);
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public ActionMessages allOk() {
        ActionMessages errors = new ActionMessages();
        Calendar cal = Calendar.getInstance();
        if (UtilityFunctions.compareTimeofDay(cal.getTime(), this.getDisallowPostingStartTime()) > 0 && UtilityFunctions.compareTimeofDay(cal.getTime(), this.getDisallowPostingEndTime()) < 0) {
            DateFormat df = new SimpleDateFormat("h:mm a z");
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.touchnet.posting.unavailable", new Object[] { df.format(this.getDisallowPostingStartTime()), df.format(this.getDisallowPostingEndTime()) }));
        }
        return errors;
    }
}
