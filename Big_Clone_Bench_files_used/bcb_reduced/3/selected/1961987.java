package com.sts.webmeet.server;

import com.sts.webmeet.server.interfaces.*;
import java.security.*;
import org.apache.log4j.Logger;

public class PasswordUtil {

    private static final String CHAR_ENCODING = "UTF-8";

    private static final String HASH_ALGORITHM = "SHA";

    private static SecureRandom sr = new SecureRandom();

    private static final Logger logger = Logger.getLogger(PasswordUtil.class);

    public static boolean checkMeetingPassword(String strMeetingID, String strPassword) {
        return checkMeetingPassword(new Integer(strMeetingID), strPassword);
    }

    public static MeetingData getMeetingDataForOwner(CustomerData customer, String strMeetingID) {
        MeetingData dataRet = null;
        try {
            MeetingLocal meeting = MeetingUtil.getLocalHome().findByPrimaryKey(new Integer(strMeetingID));
            if (null == meeting) {
                logger.error("meeting " + strMeetingID + " not found.");
            } else if (customer.getCustomerId().intValue() != meeting.getCustomer().getCustomerId().intValue()) {
                logger.error("SECURITY: meeting " + strMeetingID + " not owned by " + customer.getCustomerId());
            } else {
                dataRet = meeting.getMeetingData();
            }
        } catch (Exception e) {
            logger.error("problem getting meeting key", e);
        }
        return dataRet;
    }

    public static boolean checkMeetingPassword(Integer meetingID, String strPassword) {
        boolean bCorrect = false;
        try {
            MeetingLocal meeting = MeetingUtil.getLocalHome().findByPrimaryKey(meetingID);
            if (null == meeting) {
                logger.error("meeting " + meetingID + " not found.");
                return bCorrect;
            }
            String strStoredPassword = meeting.getPassword();
            bCorrect = strStoredPassword.equals(strPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!bCorrect) {
            logger.info("password check failed");
        }
        return bCorrect;
    }

    public static boolean checkMeetingKey(String strMeetingID, String strKey) {
        return checkMeetingKey(new Integer(strMeetingID), strKey);
    }

    public static boolean checkMeetingKey(Integer meetingID, String strKey) {
        boolean bCorrect = false;
        try {
            MeetingLocal meeting = MeetingUtil.getLocalHome().findByPrimaryKey(meetingID);
            if (null == meeting) {
                logger.error("meeting " + meetingID + " not found.");
                return bCorrect;
            } else {
                bCorrect = strKey.equals(meeting.getMeetingKey());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!bCorrect) {
            logger.error("key check failed");
        }
        return bCorrect;
    }

    public static boolean checkParticipantID(String meetingID, String strParticipantID) {
        boolean bCorrect = false;
        try {
            MeetingLocal meeting = MeetingUtil.getLocalHome().findByPrimaryKey(new Integer(meetingID));
            ParticipationLocal participation = ParticipationUtil.getLocalHome().findByPrimaryKey(strParticipantID);
            if (null == meeting || null == participation) {
                logger.error("meeting " + meetingID + " or participation " + strParticipantID + " not found.");
                return bCorrect;
            } else {
                bCorrect = meeting.getMeetingId().equals(participation.getMeeting().getMeetingId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!bCorrect) {
            logger.error("key check failed");
        }
        return bCorrect;
    }

    public static String generatePassword() {
        return RandomStringUtil.nextString(8).toLowerCase();
    }

    public static byte[] generateHash(String strPassword, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(strPassword.getBytes(CHAR_ENCODING));
            md.update(salt);
            return md.digest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getSalt(int iLength) {
        byte[] ba = new byte[iLength];
        sr.nextBytes(ba);
        return ba;
    }

    private static boolean compareByteArrays(byte[] baFirst, byte[] baSecond) {
        if (baFirst.length == baSecond.length) {
            for (int i = 0; i < baFirst.length; i++) {
                if (baFirst[i] != baSecond[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
