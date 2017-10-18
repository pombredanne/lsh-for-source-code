package com.imoresoft.magic.action;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ambitor.grass.sql.dao.SuperDao;
import com.ambitor.grass.sql.dao.impl.SuperDaoImpl;
import com.ambitor.grass.util.data.DataMap;
import com.ambitor.grass.util.data.IData;
import com.imoresoft.magic.top.TopConstants;

public class NotifyAction extends BaseAction {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String nick;

    private String leaseId;

    private String validateDate;

    private String invalidateDate;

    private String factMoney;

    private String subscType;

    private String versionNo;

    private String oldVersionNo;

    private String status;

    private String sign;

    private String gmtCreateDate;

    private String tadgetCode;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }

    public String getValidateDate() {
        return validateDate;
    }

    public void setValidateDate(String validateDate) {
        this.validateDate = validateDate;
    }

    public String getInvalidateDate() {
        return invalidateDate;
    }

    public void setInvalidateDate(String invalidateDate) {
        this.invalidateDate = invalidateDate;
    }

    public String getFactMoney() {
        return factMoney;
    }

    public void setFactMoney(String factMoney) {
        this.factMoney = factMoney;
    }

    public String getSubscType() {
        return subscType;
    }

    public void setSubscType(String subscType) {
        this.subscType = subscType;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getOldVersionNo() {
        return oldVersionNo;
    }

    public void setOldVersionNo(String oldVersionNo) {
        this.oldVersionNo = oldVersionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getGmtCreateDate() {
        return gmtCreateDate;
    }

    public void setGmtCreateDate(String gmtCreateDate) {
        this.gmtCreateDate = gmtCreateDate;
    }

    public String getTadgetCode() {
        return tadgetCode;
    }

    public void setTadgetCode(String tadgetCode) {
        this.tadgetCode = tadgetCode;
    }

    @SuppressWarnings("unchecked")
    public String execute() {
        boolean isSignRight = true;
        if (isSignRight) {
            IData notifyMap = new DataMap();
            notifyMap.put("USER_ID", userId);
            notifyMap.put("NICK", nick);
            notifyMap.put("LEASE_ID", leaseId);
            notifyMap.put("START_TIME", validateDate);
            notifyMap.put("END_TIME", invalidateDate);
            notifyMap.put("FACT_MONEY", factMoney);
            notifyMap.put("SUBSC_TYPE", subscType);
            notifyMap.put("VERSION", versionNo);
            notifyMap.put("PRE_VERSION", oldVersionNo);
            notifyMap.put("STATUS", status);
            notifyMap.put("SUBSC_TIME", gmtCreateDate);
            try {
                SuperDao dao = new SuperDaoImpl(pd.getConn());
                dao.insert("TL_M_USER_SUBSC", notifyMap);
            } catch (Exception e) {
                logger.warn(nick + "保存notify信息出错");
                e.printStackTrace();
            }
        } else {
            logger.warn("notify参数验证不正确");
        }
        return SUCCESS;
    }

    @SuppressWarnings("unused")
    private boolean verify() {
        StringBuilder result = new StringBuilder();
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            result.append(TopConstants.APP_SECRET).append("factMoney").append(factMoney).append("gmtCreateDate").append(gmtCreateDate).append("invalidateDate").append(invalidateDate).append("leaseId").append(leaseId).append("nick").append(nick).append("oldVersionNo").append(oldVersionNo).append("status").append(status).append("subscType").append(subscType).append("tadgetCode").append(tadgetCode).append("userId").append(userId).append("validateDate").append(validateDate).append("versionNo").append(versionNo).append(TopConstants.APP_SECRET);
            logger.error("result==" + result.toString());
            byte[] bytes = md5.digest(result.toString().getBytes());
            String encodedString = new String(bytes).toUpperCase();
            logger.error("encodedString==" + encodedString);
            logger.error("sing==" + sign);
            return encodedString.equals(sign);
        } catch (NoSuchAlgorithmException e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
