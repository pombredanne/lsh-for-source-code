package com.gittigidiyor.payment.garanti.api;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

class GarantiApiUtil {

    private static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public static String getDefaultModeIfNecessary(String mode) {
        String validMode = GarantiApiConstants.MODE_DEFAULT;
        if (mode != null || GarantiApiConstants.MODE_LIST.contains(mode)) {
            validMode = mode;
        }
        return validMode;
    }

    public static List<String> arrangeCommenList(List<String> commentList) {
        List<String> arrangedCommentList = new ArrayList<String>();
        for (int i = 0; i < commentList.size() && i < GarantiApiConstants.COMMENT_LIST_SIZE_MAX; i++) {
            String comment = commentList.get(i);
            if (comment.length() > GarantiApiConstants.COMMENT_TEXT_LENGTH_MAX) {
                comment = comment.substring(0, GarantiApiConstants.COMMENT_TEXT_LENGTH_MAX);
            }
            arrangedCommentList.add(comment);
        }
        return arrangedCommentList;
    }

    public static String getInstallmentAsString(Integer installment) {
        String installmentStr = "";
        if (installment != null && GarantiApiConstants.INSTALLMENT_MIN < installment && installment <= GarantiApiConstants.INSTALLMENT_MAX) {
            installmentStr = installment.toString();
        }
        return installmentStr;
    }

    public static double round(double number) {
        return Double.parseDouble(decimalFormat.format(number));
    }

    public static String getAmountAsString(Double amount) {
        String amountStr = "0";
        if (amount != null) {
            amount = GarantiApiUtil.round(100 * amount);
            amountStr = String.valueOf(amount.intValue());
        }
        return amountStr;
    }

    public static String getArrangedTerminalId(String terminalId) {
        String arrangedId = terminalId;
        if (!StringUtil.isNullOrZeroLength(arrangedId)) {
            int length = GarantiApiConstants.TERMINAL_ID_LENGTH - arrangedId.length();
            while (length > 0) {
                arrangedId = "0" + arrangedId;
                length--;
            }
        }
        return arrangedId;
    }

    private static String byteArray2HexaDecimal(byte[] hashBytes) {
        Formatter formatter = new Formatter();
        for (byte b : hashBytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static String calculateHash(String data, String algorithm, String charset) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] databytes = data.getBytes(charset);
        md.update(databytes);
        byte[] hashBytes = md.digest();
        return byteArray2HexaDecimal(hashBytes);
    }

    public static String calculateHashAsDefaultAndUpperCase(String data) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return calculateHash(data, "SHA-1", "ISO-8859-9").toUpperCase();
    }
}
