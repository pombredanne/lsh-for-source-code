package com.entelience.probe.mail;

import com.entelience.util.Config;
import com.entelience.util.DateHelper;
import com.entelience.util.Logs;
import com.entelience.sql.Db;
import com.entelience.sql.DbHelper;
import com.entelience.sql.UsesDbObject;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
* class for running queries for updating the mail schema
*/
public class SpamMailImport extends UsesDbObject {

    protected static final Logger _logger = Logs.getProbeLogger();

    private PreparedStatement pstInsertDomain;

    private PreparedStatement pstUpdateDomain;

    private PreparedStatement pstInsertUser;

    private PreparedStatement pstUpdateUser;

    private PreparedStatement pstInsertUserDaily;

    private PreparedStatement pstUpdateUserDaily;

    private PreparedStatement pstInsertUserInternalDaily;

    private PreparedStatement pstUpdateUserInternalDaily;

    private PreparedStatement pstGetFileId;

    private PreparedStatement pstUpdateFileTypeDay;

    private PreparedStatement pstInsertFileTypeDay;

    private PreparedStatement pstInsertEmailDaily;

    private PreparedStatement pstUpdateEmailDaily;

    private PreparedStatement pstInsertMimeUserDaily;

    private PreparedStatement pstUpdateMimeUserDaily;

    private PreparedStatement pstInsertMessageDaily;

    private PreparedStatement pstUpdateMessageDaily;

    private PreparedStatement pstAddStatus;

    private PreparedStatement pstGetStatus;

    private PreparedStatement pstUpdateSpamDaily;

    private PreparedStatement pstInsertSpamDaily;

    private PreparedStatement pstAddExternalDomainInfo;

    private PreparedStatement pstUpdateExternalDomainInfo;

    private PreparedStatement pstGetExternalDomainInfo;

    private PreparedStatement pstAddCrossDomain;

    private PreparedStatement pstGetCrossDomain;

    private PreparedStatement pstUpdateExternalDomainDaily;

    private PreparedStatement pstAddExternalDomainDaily;

    private PreparedStatement pstAddGlobalDaily;

    private PreparedStatement pstUpdGlobalDaily;

    private final Map<String, Integer> addressesCache = new HashMap<String, Integer>();

    private final Map<String, String> userNameCache = new HashMap<String, String>();

    private final Map<String, String> domainNameCache = new HashMap<String, String>();

    public void prepare() throws Exception {
        Db db = getDb();
        pstUpdateDomain = db.prepareStatement("UPDATE mail.t_domain SET first_occurrence = timestamp_smaller(?, first_occurrence), last_occurrence = timestamp_larger(?, last_occurrence) WHERE domain_name = ?");
        pstInsertDomain = db.prepareStatement("INSERT INTO mail.t_domain (domain_name, first_occurrence, last_occurrence) VALUES (?, ?, ?)");
        pstUpdateUser = db.prepareStatement("UPDATE mail.t_domain_user SET first_occurrence = timestamp_smaller(?, first_occurrence), last_occurrence = timestamp_larger(?, last_occurrence) WHERE user_name = ? AND t_domain_id = (SELECT t_domain_id FROM mail.t_domain WHERE domain_name = ?) RETURNING t_domain_user_id");
        pstInsertUser = db.prepareStatement("INSERT INTO mail.t_domain_user (first_occurrence, last_occurrence, user_name, t_domain_id, domain_name) SELECT ?, ?, ?, t_domain_id, domain_name FROM mail.t_domain WHERE domain_name = ? RETURNING t_domain_user_id");
        pstUpdateUserDaily = db.prepareStatement("UPDATE mail.t_domain_user_daily SET count_to = count_to+?, count_from = count_from+?, count_cc = count_cc+?, count_bcc = count_bcc+?, volume_in = volume_in+?, volume_out = volume_out+?, count_spam_sent = count_spam_sent+?, count_spam_received = count_spam_received +?, count_blocked_in = count_blocked_in+?, count_blocked_out = count_blocked_out+?, attachment_in_volume = attachment_in_volume+?, attachment_out_volume = attachment_out_volume+?, count_attachments_in = count_attachments_in+?, count_attachments_out = count_attachments_out+?, count_failed_in = count_failed_in+?, count_failed_out = count_failed_out+?, count_received_in_quarantine=count_received_in_quarantine+?, count_dropped_from_quarantine=count_dropped_from_quarantine+?, count_released_from_quarantine=count_released_from_quarantine+? WHERE calc_day = ? AND t_domain_user_id = ?");
        pstInsertUserDaily = db.prepareStatement("INSERT INTO mail.t_domain_user_daily (count_to, count_from, count_cc, count_bcc, volume_in, volume_out, count_spam_sent, count_spam_received, count_blocked_in, count_blocked_out, attachment_in_volume, attachment_out_volume, count_attachments_in, count_attachments_out, count_failed_in, count_failed_out, count_received_in_quarantine, count_dropped_from_quarantine, count_released_from_quarantine, calc_day, t_domain_user_id, user_name, domain_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        pstUpdateUserInternalDaily = db.prepareStatement("UPDATE mail.t_domain_user_internal_daily SET count_in = count_in+?, count_out = count_out+?, volume_in = volume_in+?, volume_out = volume_out+?, count_spam_sent = count_spam_sent+?, count_spam_received = count_spam_received +?, count_blocked_in = count_blocked_in+?, count_blocked_out = count_blocked_out+?, volume_attachments_in = volume_attachments_in+?, volume_attachments_out = volume_attachments_out+?, count_attachments_in = count_attachments_in+?, count_attachments_out = count_attachments_out+?, count_failed_in = count_failed_in+?, count_failed_out = count_failed_out+? WHERE calc_day = ? AND t_domain_user_id = ?");
        pstInsertUserInternalDaily = db.prepareStatement("INSERT INTO mail.t_domain_user_internal_daily (count_in, count_out, volume_in, volume_out, count_spam_sent, count_spam_received, count_blocked_in, count_blocked_out, volume_attachments_in, volume_attachments_out, count_attachments_in, count_attachments_out, count_failed_in, count_failed_out, calc_day, t_domain_user_id, user_name, domain_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        pstGetFileId = db.prepareStatement("SELECT mt.mime_id FROM e_mime_type mt, e_external_type et WHERE mt.mime_id = et.mime_type_id AND type_name ILIKE ?");
        pstUpdateFileTypeDay = db.prepareStatement("UPDATE mail.t_email_daily_file_attachments SET count_in = count_in + ?, count_out = count_out + ?, count_internal = count_internal + ?, count_blocked_in = count_blocked_in + ?, count_blocked_out = count_blocked_out + ?, count_blocked_internal = count_blocked_internal + ?, volume_in = volume_in + ?, volume_out = volume_out + ?, volume_internal = volume_internal + ?, volume_blocked_in = volume_blocked_in + ?, volume_blocked_out = volume_blocked_out + ?, volume_blocked_internal = volume_blocked_internal + ? WHERE calc_day = ? AND mime_type_id = ?");
        pstInsertFileTypeDay = db.prepareStatement("INSERT INTO mail.t_email_daily_file_attachments (calc_day, mime_type_id, count_in, count_out, count_internal, count_blocked_in, count_blocked_out, count_blocked_internal, volume_in, volume_out, volume_internal, volume_blocked_in, volume_blocked_out, volume_blocked_internal) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        pstInsertEmailDaily = db.prepareStatement("INSERT INTO mail.t_email_daily (count, count_in, count_out, count_internal, count_inbound_blocked, count_outbound_blocked, count_internal_blocked, volume_in, volume_out, volume_internal, volume, count_attachments_in, count_attachments_out, count_attachments_internal, emails_received_in_quarantine, emails_dropped_from_quarantine, emails_released_from_quarantine, count_spam_in, count_spam_out, count_spam_internal, count_external, count_external_blocked, count_spam_external, volume_external, calc_day) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?)");
        pstUpdateEmailDaily = db.prepareStatement("UPDATE mail.t_email_daily SET count = count + ?, count_in = count_in + ?, count_out = count_out + ?, count_internal = count_internal + ?, count_inbound_blocked = count_inbound_blocked + ?, count_outbound_blocked = count_outbound_blocked + ?, count_internal_blocked = count_internal_blocked + ?, volume_in = volume_in + ?, volume_out = volume_out + ?, volume_internal = volume_internal + ?, volume = volume + ?, count_attachments_in = count_attachments_in + ?, count_attachments_out = count_attachments_out + ?,  count_attachments_internal = count_attachments_internal + ?, emails_received_in_quarantine = emails_received_in_quarantine + ?, emails_dropped_from_quarantine = emails_dropped_from_quarantine + ?, emails_released_from_quarantine = emails_released_from_quarantine + ?, count_spam_in = count_spam_in + ?, count_spam_out = count_spam_out + ?, count_spam_internal = count_spam_internal + ?, count_external = count_external + ?, count_external_blocked = count_external_blocked + ?, count_spam_external = count_spam_external + ?, volume_external = volume_external + ? WHERE calc_day = ?");
        pstInsertMimeUserDaily = db.prepareStatement("INSERT INTO mail.t_user_mime_daily (count, count_in, count_out, volume, volume_in, volume_out, count_internal, count_internal_in, count_internal_out, volume_internal, volume_internal_in, volume_internal_out, calc_day, t_domain_user_id, t_mime_type_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        pstUpdateMimeUserDaily = db.prepareStatement("UPDATE mail.t_user_mime_daily SET count = count + ?, count_in = count_in + ?, count_out = count_out + ?, volume = volume + ?, volume_in = volume_in + ?, volume_out = volume_out + ?, count_internal = count_internal + ?, count_internal_in = count_internal_in + ?, count_internal_out = count_internal_out + ?, volume_internal = volume_internal + ?, volume_internal_in = volume_internal_in + ?, volume_internal_out = volume_internal_out + ? WHERE calc_day = ? AND t_domain_user_id = ? AND t_mime_type_id = ?");
        pstInsertMessageDaily = db.prepareStatement("INSERT INTO mail.t_message_daily (count_msg, volume_msg, calc_day, sender, receiver) VALUES (?, ?, ?, ?, ?)");
        pstUpdateMessageDaily = db.prepareStatement("UPDATE mail.t_message_daily SET count_msg = count_msg + ?, volume_msg = volume_msg + ? WHERE calc_day = ? AND sender = ? AND receiver = ?");
        pstAddStatus = db.prepareStatement("INSERT INTO mail.t_spam_status (status,quarantine) VALUES (?, false) ");
        pstGetStatus = db.prepareStatement("SELECT t_spam_status_id FROM mail.t_spam_status WHERE lower(status) = lower(?) ");
        pstUpdateSpamDaily = db.prepareStatement("UPDATE mail.t_spam_daily SET instances = instances + ? WHERE calc_day = ? AND t_spam_status_id = ?");
        pstInsertSpamDaily = db.prepareStatement("INSERT INTO mail.t_spam_daily (calc_day, t_spam_status_id, instances) VALUES (?, ?, ?)");
        pstAddExternalDomainInfo = db.prepareStatement("INSERT INTO mail.t_external_domain_info (e_cross_domain_id, registration_date, expiration_date, first_message_date) VALUES (?, ?, ?, ?)");
        pstUpdateExternalDomainInfo = db.prepareStatement("UPDATE  mail.t_external_domain_info SET registration_date = ?, expiration_date = ?, first_message_date = ? WHERE e_cross_domain_id = ?");
        pstGetExternalDomainInfo = db.prepareStatement("SELECT registration_date, expiration_date, first_message_date FROM mail.t_external_domain_info WHERE e_cross_domain_id = ?");
        pstAddCrossDomain = db.prepareStatement("INSERT INTO e_cross_domain (domain_name) VALUES (?) RETURNING e_cross_domain_id");
        pstGetCrossDomain = db.prepareStatement("SELECT e_cross_domain_id FROM e_cross_domain WHERE lower(domain_name) = lower(?)");
        pstUpdateExternalDomainDaily = db.prepareStatement("UPDATE mail.t_external_domain_daily SET magnitude = ?, ratio_to_worldwide = ? WHERE e_cross_domain_id = ? AND calc_day = ?");
        pstAddExternalDomainDaily = db.prepareStatement("INSERT INTO mail.t_external_domain_daily (magnitude, ratio_to_worldwide, e_cross_domain_id, calc_day) VALUES (?, ?, ?, ?)");
        pstAddGlobalDaily = db.prepareStatement("INSERT INTO mail.t_global_daily( count_emails, count_spams, calc_day) VALUES (?, ?, ?)");
        pstUpdGlobalDaily = db.prepareStatement("UPDATE mail.t_global_daily SET count_emails = ?, count_spams = ? WHERE calc_day = ?");
        cnilLevel = Config.getProperty(db, "com.entelience.esis.cnilLevel", 2);
        if (cnilLevel == 0) _logger.info("CNIL Level : No obfuscation of users names"); else if (cnilLevel == 1) _logger.info("CNIL Level : SHA obfuscation of users names"); else _logger.info("CNIL Level : No informations by users");
        anonymizer = MessageDigest.getInstance("SHA");
    }

    private int cnilLevel;

    MessageDigest anonymizer;

    public static final String MAIL_REPORT = "com.entelience.report.mail.DomainUsersReport";

    private String obfuscateUsername(String user) {
        if (cnilLevel == 0) {
            return user;
        } else if (cnilLevel == 1) {
            if (user == null) return null;
            anonymizer.update(user.getBytes());
            java.math.BigInteger hash = new java.math.BigInteger(1, anonymizer.digest());
            return hash.toString(16);
        } else {
            return "Anonymized user";
        }
    }

    public void addAttachmentDaily(AttachmentDaily attach) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetFileId.setString(1, attach.fileType);
            Integer fileId = DbHelper.getKey(pstGetFileId);
            if (fileId == null) {
                _logger.warn("Unknown attachment type : " + attach.fileType);
                fileId = Integer.valueOf(0);
            }
            pstUpdateFileTypeDay.setInt(1, attach.count_in);
            pstUpdateFileTypeDay.setInt(2, attach.count_out);
            pstUpdateFileTypeDay.setInt(3, attach.count_internal);
            pstUpdateFileTypeDay.setInt(4, attach.count_blocked_in);
            pstUpdateFileTypeDay.setInt(5, attach.count_blocked_out);
            pstUpdateFileTypeDay.setInt(6, attach.count_blocked_internal);
            pstUpdateFileTypeDay.setLong(7, attach.volume_in);
            pstUpdateFileTypeDay.setLong(8, attach.volume_out);
            pstUpdateFileTypeDay.setLong(9, attach.volume_internal);
            pstUpdateFileTypeDay.setLong(10, attach.volume_blocked_in);
            pstUpdateFileTypeDay.setLong(11, attach.volume_blocked_out);
            pstUpdateFileTypeDay.setLong(12, attach.volume_blocked_internal);
            pstUpdateFileTypeDay.setDate(13, DateHelper.sqld(attach.day));
            pstUpdateFileTypeDay.setInt(14, fileId.intValue());
            if (db.executeUpdate(pstUpdateFileTypeDay) == 0) {
                pstInsertFileTypeDay.setDate(1, DateHelper.sqld(attach.day));
                pstInsertFileTypeDay.setInt(2, fileId.intValue());
                pstInsertFileTypeDay.setInt(3, attach.count_in);
                pstInsertFileTypeDay.setInt(4, attach.count_out);
                pstInsertFileTypeDay.setInt(5, attach.count_internal);
                pstInsertFileTypeDay.setInt(6, attach.count_blocked_in);
                pstInsertFileTypeDay.setInt(7, attach.count_blocked_out);
                pstInsertFileTypeDay.setInt(8, attach.count_blocked_internal);
                pstInsertFileTypeDay.setLong(9, attach.volume_in);
                pstInsertFileTypeDay.setLong(10, attach.volume_out);
                pstInsertFileTypeDay.setLong(11, attach.volume_internal);
                pstInsertFileTypeDay.setLong(12, attach.volume_blocked_in);
                pstInsertFileTypeDay.setLong(13, attach.volume_blocked_out);
                pstInsertFileTypeDay.setLong(14, attach.volume_blocked_internal);
                db.executeUpdate(pstInsertFileTypeDay);
            }
        } finally {
            db.exit();
        }
    }

    private int usrTreated = 0;

    private int domAdded = 0;

    public void addUser(UserInformation ui) throws SQLException {
        Db db = getDb();
        try {
            db.enter();
            pstUpdateDomain.setTimestamp(1, DateHelper.sqlOrNull(ui.firstSeen));
            pstUpdateDomain.setTimestamp(2, DateHelper.sqlOrNull(ui.lastSeen));
            pstUpdateDomain.setString(3, ui.domain);
            if (db.executeUpdate(pstUpdateDomain) == 0) {
                pstInsertDomain.setString(1, ui.domain);
                pstInsertDomain.setTimestamp(2, DateHelper.sqlOrNull(ui.firstSeen));
                pstInsertDomain.setTimestamp(3, DateHelper.sqlOrNull(ui.lastSeen));
                db.executeUpdate(pstInsertDomain);
                domAdded++;
                if (domAdded % 1000 == 0 && domAdded > 0) _logger.info(domAdded + " new domains added");
            }
            String userName = obfuscateUsername(ui.user);
            pstUpdateUser.setTimestamp(1, DateHelper.sqlOrNull(ui.firstSeen));
            pstUpdateUser.setTimestamp(2, DateHelper.sqlOrNull(ui.lastSeen));
            pstUpdateUser.setString(3, userName);
            pstUpdateUser.setString(4, ui.domain);
            Integer userId = DbHelper.getKey(pstUpdateUser);
            if (userId == null) {
                pstInsertUser.setTimestamp(1, DateHelper.sqlOrNull(ui.firstSeen));
                pstInsertUser.setTimestamp(2, DateHelper.sqlOrNull(ui.lastSeen));
                pstInsertUser.setString(3, userName);
                pstInsertUser.setString(4, ui.domain);
                userId = DbHelper.getKey(pstInsertUser);
            }
            usrTreated++;
            if (usrTreated % 1000 == 0 && usrTreated > 0) _logger.info(usrTreated + " users treated");
            if (userId == null) throw new IllegalStateException("No user Id for " + ui.email);
            addressesCache.put(ui.email, userId);
            userNameCache.put(ui.email, userName);
            domainNameCache.put(ui.email, ui.domain);
        } finally {
            db.exit();
        }
    }

    private int usrInternalDailyAdded = 0;

    public void addUserInternalDaily(UserDaily ud) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstUpdateUserInternalDaily.setInt(1, ud.count_to);
            pstUpdateUserInternalDaily.setInt(2, ud.count_from);
            pstUpdateUserInternalDaily.setLong(3, ud.volume_in);
            pstUpdateUserInternalDaily.setLong(4, ud.volume_out);
            pstUpdateUserInternalDaily.setInt(5, ud.count_spam_sent);
            pstUpdateUserInternalDaily.setInt(6, ud.count_spam_received);
            pstUpdateUserInternalDaily.setInt(7, ud.count_blocked_in);
            pstUpdateUserInternalDaily.setInt(8, ud.count_blocked_out);
            pstUpdateUserInternalDaily.setLong(9, ud.attachment_in_volume);
            pstUpdateUserInternalDaily.setLong(10, ud.attachment_out_volume);
            pstUpdateUserInternalDaily.setInt(11, ud.count_attachment_in);
            pstUpdateUserInternalDaily.setInt(12, ud.count_attachment_out);
            pstUpdateUserInternalDaily.setInt(13, ud.count_failed_in);
            pstUpdateUserInternalDaily.setInt(14, ud.count_failed_out);
            pstUpdateUserInternalDaily.setDate(15, DateHelper.sqld(ud.day));
            Integer uid = addressesCache.get(ud.email);
            if (uid == null) _logger.error("No id found for user " + ud.email);
            pstUpdateUserInternalDaily.setInt(16, addressesCache.get(ud.email));
            int res = db.executeUpdate(pstUpdateUserInternalDaily);
            if (res == 0) {
                pstInsertUserInternalDaily.setInt(1, ud.count_to);
                pstInsertUserInternalDaily.setInt(2, ud.count_from);
                pstInsertUserInternalDaily.setLong(3, ud.volume_in);
                pstInsertUserInternalDaily.setLong(4, ud.volume_out);
                pstInsertUserInternalDaily.setInt(5, ud.count_spam_sent);
                pstInsertUserInternalDaily.setInt(6, ud.count_spam_received);
                pstInsertUserInternalDaily.setInt(7, ud.count_blocked_in);
                pstInsertUserInternalDaily.setInt(8, ud.count_blocked_out);
                pstInsertUserInternalDaily.setLong(9, ud.attachment_in_volume);
                pstInsertUserInternalDaily.setLong(10, ud.attachment_out_volume);
                pstInsertUserInternalDaily.setInt(11, ud.count_attachment_in);
                pstInsertUserInternalDaily.setInt(12, ud.count_attachment_out);
                pstInsertUserInternalDaily.setInt(13, ud.count_failed_in);
                pstInsertUserInternalDaily.setInt(14, ud.count_failed_out);
                pstInsertUserInternalDaily.setDate(15, DateHelper.sqld(ud.day));
                pstInsertUserInternalDaily.setInt(16, addressesCache.get(ud.email));
                pstInsertUserInternalDaily.setString(17, userNameCache.get(ud.email));
                pstInsertUserInternalDaily.setString(18, domainNameCache.get(ud.email));
                db.executeUpdate(pstInsertUserInternalDaily);
            }
            usrInternalDailyAdded++;
            if (usrInternalDailyAdded % 1000 == 0 && usrInternalDailyAdded > 0) _logger.info(usrInternalDailyAdded + " internal users/day added or updated");
        } finally {
            db.exit();
        }
    }

    private int usrDailyAdded = 0;

    public void addUserDaily(UserDaily ud) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstUpdateUserDaily.setInt(1, ud.count_to);
            pstUpdateUserDaily.setInt(2, ud.count_from);
            pstUpdateUserDaily.setInt(3, ud.count_cc);
            pstUpdateUserDaily.setInt(4, ud.count_bcc);
            pstUpdateUserDaily.setLong(5, ud.volume_in);
            pstUpdateUserDaily.setLong(6, ud.volume_out);
            pstUpdateUserDaily.setInt(7, ud.count_spam_sent);
            pstUpdateUserDaily.setInt(8, ud.count_spam_received);
            pstUpdateUserDaily.setInt(9, ud.count_blocked_in);
            pstUpdateUserDaily.setInt(10, ud.count_blocked_out);
            pstUpdateUserDaily.setLong(11, ud.attachment_in_volume);
            pstUpdateUserDaily.setLong(12, ud.attachment_out_volume);
            pstUpdateUserDaily.setInt(13, ud.count_attachment_in);
            pstUpdateUserDaily.setInt(14, ud.count_attachment_out);
            pstUpdateUserDaily.setInt(15, ud.count_failed_in);
            pstUpdateUserDaily.setInt(16, ud.count_failed_out);
            pstUpdateUserDaily.setInt(17, ud.count_received_in_quarantine);
            pstUpdateUserDaily.setInt(18, ud.count_dropped_from_quarantine);
            pstUpdateUserDaily.setInt(19, ud.count_released_from_quarantine);
            pstUpdateUserDaily.setDate(20, DateHelper.sqld(ud.day));
            Integer uid = addressesCache.get(ud.email);
            if (uid == null) _logger.error("No id found for user " + ud.email);
            pstUpdateUserDaily.setInt(21, addressesCache.get(ud.email));
            int res = db.executeUpdate(pstUpdateUserDaily);
            if (res == 0) {
                pstInsertUserDaily.setInt(1, ud.count_to);
                pstInsertUserDaily.setInt(2, ud.count_from);
                pstInsertUserDaily.setInt(3, ud.count_cc);
                pstInsertUserDaily.setInt(4, ud.count_bcc);
                pstInsertUserDaily.setLong(5, ud.volume_in);
                pstInsertUserDaily.setLong(6, ud.volume_out);
                pstInsertUserDaily.setInt(7, ud.count_spam_sent);
                pstInsertUserDaily.setInt(8, ud.count_spam_received);
                pstInsertUserDaily.setInt(9, ud.count_blocked_in);
                pstInsertUserDaily.setInt(10, ud.count_blocked_out);
                pstInsertUserDaily.setLong(11, ud.attachment_in_volume);
                pstInsertUserDaily.setLong(12, ud.attachment_out_volume);
                pstInsertUserDaily.setInt(13, ud.count_attachment_in);
                pstInsertUserDaily.setInt(14, ud.count_attachment_out);
                pstInsertUserDaily.setInt(15, ud.count_failed_in);
                pstInsertUserDaily.setInt(16, ud.count_failed_out);
                pstInsertUserDaily.setInt(17, ud.count_received_in_quarantine);
                pstInsertUserDaily.setInt(18, ud.count_dropped_from_quarantine);
                pstInsertUserDaily.setInt(19, ud.count_released_from_quarantine);
                pstInsertUserDaily.setDate(20, DateHelper.sqld(ud.day));
                pstInsertUserDaily.setInt(21, addressesCache.get(ud.email));
                pstInsertUserDaily.setString(22, userNameCache.get(ud.email));
                pstInsertUserDaily.setString(23, domainNameCache.get(ud.email));
                db.executeUpdate(pstInsertUserDaily);
            }
            usrDailyAdded++;
            if (usrDailyAdded % 1000 == 0 && usrDailyAdded > 0) _logger.info(usrDailyAdded + " users/day added or updated");
        } finally {
            db.exit();
        }
    }

    public void addMailDaily(MailDaily md) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstUpdateEmailDaily.setInt(1, md.count_in + md.count_out + md.count_internal + md.count_external);
            pstUpdateEmailDaily.setInt(2, md.count_in);
            pstUpdateEmailDaily.setInt(3, md.count_out);
            pstUpdateEmailDaily.setInt(4, md.count_internal);
            pstUpdateEmailDaily.setInt(5, md.count_inbound_blocked);
            pstUpdateEmailDaily.setInt(6, md.count_outbound_blocked);
            pstUpdateEmailDaily.setInt(7, md.count_internal_blocked);
            pstUpdateEmailDaily.setLong(8, md.volume_in);
            pstUpdateEmailDaily.setLong(9, md.volume_out);
            pstUpdateEmailDaily.setLong(10, md.volume_internal);
            pstUpdateEmailDaily.setLong(11, md.volume_in + md.volume_out + md.volume_internal + md.volume_external);
            pstUpdateEmailDaily.setInt(12, md.count_attachments_in);
            pstUpdateEmailDaily.setInt(13, md.count_attachments_out);
            pstUpdateEmailDaily.setInt(14, md.count_attachments_internal);
            pstUpdateEmailDaily.setLong(15, md.emails_received_in_quarantine);
            pstUpdateEmailDaily.setLong(16, md.emails_dropped_from_quarantine);
            pstUpdateEmailDaily.setLong(17, md.emails_released_from_quarantine);
            pstUpdateEmailDaily.setLong(18, md.count_spam_in);
            pstUpdateEmailDaily.setLong(19, md.count_spam_out);
            pstUpdateEmailDaily.setLong(20, md.count_spam_internal);
            pstUpdateEmailDaily.setLong(21, md.count_external);
            pstUpdateEmailDaily.setLong(22, md.count_spam_external);
            pstUpdateEmailDaily.setLong(23, md.count_external_blocked);
            pstUpdateEmailDaily.setLong(24, md.volume_external);
            pstUpdateEmailDaily.setDate(25, DateHelper.sqld(md.date));
            if (db.executeUpdate(pstUpdateEmailDaily) == 0) {
                pstInsertEmailDaily.setInt(1, md.count_in + md.count_out + md.count_internal + md.count_external);
                pstInsertEmailDaily.setInt(2, md.count_in);
                pstInsertEmailDaily.setInt(3, md.count_out);
                pstInsertEmailDaily.setInt(4, md.count_internal);
                pstInsertEmailDaily.setInt(5, md.count_inbound_blocked);
                pstInsertEmailDaily.setInt(6, md.count_outbound_blocked);
                pstInsertEmailDaily.setInt(7, md.count_internal_blocked);
                pstInsertEmailDaily.setLong(8, md.volume_in);
                pstInsertEmailDaily.setLong(9, md.volume_out);
                pstInsertEmailDaily.setLong(10, md.volume_internal);
                pstInsertEmailDaily.setLong(11, md.volume_in + md.volume_out + md.volume_internal + md.volume_external);
                pstInsertEmailDaily.setInt(12, md.count_attachments_in);
                pstInsertEmailDaily.setInt(13, md.count_attachments_out);
                pstInsertEmailDaily.setInt(14, md.count_attachments_internal);
                pstInsertEmailDaily.setLong(15, md.emails_received_in_quarantine);
                pstInsertEmailDaily.setLong(16, md.emails_dropped_from_quarantine);
                pstInsertEmailDaily.setLong(17, md.emails_released_from_quarantine);
                pstInsertEmailDaily.setLong(18, md.count_spam_in);
                pstInsertEmailDaily.setLong(19, md.count_spam_out);
                pstInsertEmailDaily.setLong(20, md.count_spam_internal);
                pstInsertEmailDaily.setLong(21, md.count_external);
                pstInsertEmailDaily.setLong(22, md.count_spam_external);
                pstInsertEmailDaily.setLong(23, md.count_external_blocked);
                pstInsertEmailDaily.setLong(24, md.volume_external);
                pstInsertEmailDaily.setDate(25, DateHelper.sqld(md.date));
                db.executeUpdate(pstInsertEmailDaily);
            }
        } finally {
            db.exit();
        }
    }

    private int msgDailyAdded = 0;

    public void addMessageDaily(MessageDaily md) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            Integer senderId = addressesCache.get(md.sender);
            Integer receiverId = addressesCache.get(md.receiver);
            pstUpdateMessageDaily.setInt(1, md.count);
            pstUpdateMessageDaily.setLong(2, md.volume);
            pstUpdateMessageDaily.setDate(3, DateHelper.sqld(md.day));
            pstUpdateMessageDaily.setInt(4, senderId);
            pstUpdateMessageDaily.setInt(5, receiverId);
            if (db.executeUpdate(pstUpdateMessageDaily) == 0) {
                pstInsertMessageDaily.setInt(1, md.count);
                pstInsertMessageDaily.setLong(2, md.volume);
                pstInsertMessageDaily.setDate(3, DateHelper.sqld(md.day));
                pstInsertMessageDaily.setInt(4, senderId);
                pstInsertMessageDaily.setInt(5, receiverId);
                db.executeUpdate(pstInsertMessageDaily);
            }
            msgDailyAdded++;
            if (msgDailyAdded % 1000 == 0 && msgDailyAdded > 0) _logger.info(msgDailyAdded + " message/day added or updated");
        } finally {
            db.exit();
        }
    }

    private int usrMimeDailyAdded = 0;

    public void addMimeUserDaily(MimeUserDaily mud) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetFileId.setString(1, mud.fileType);
            Integer fileId = DbHelper.getKey(pstGetFileId);
            if (fileId == null) {
                _logger.warn("Unknown attachment type : " + mud.fileType);
                fileId = Integer.valueOf(0);
            }
            Integer userId = addressesCache.get(mud.email);
            pstUpdateMimeUserDaily.setInt(1, mud.count_in + mud.count_out);
            pstUpdateMimeUserDaily.setInt(2, mud.count_in);
            pstUpdateMimeUserDaily.setInt(3, mud.count_out);
            pstUpdateMimeUserDaily.setLong(4, mud.volume_in + mud.volume_out);
            pstUpdateMimeUserDaily.setLong(5, mud.volume_in);
            pstUpdateMimeUserDaily.setLong(6, mud.volume_out);
            pstUpdateMimeUserDaily.setInt(7, mud.count_internal_in + mud.count_internal_out);
            pstUpdateMimeUserDaily.setInt(8, mud.count_internal_in);
            pstUpdateMimeUserDaily.setInt(9, mud.count_internal_out);
            pstUpdateMimeUserDaily.setLong(10, mud.volume_internal_in + mud.volume_internal_out);
            pstUpdateMimeUserDaily.setLong(11, mud.volume_internal_in);
            pstUpdateMimeUserDaily.setLong(12, mud.volume_internal_out);
            pstUpdateMimeUserDaily.setDate(13, DateHelper.sqld(mud.day));
            pstUpdateMimeUserDaily.setInt(14, userId);
            pstUpdateMimeUserDaily.setInt(15, fileId);
            if (db.executeUpdate(pstUpdateMimeUserDaily) == 0) {
                pstInsertMimeUserDaily.setInt(1, mud.count_in + mud.count_out);
                pstInsertMimeUserDaily.setInt(2, mud.count_in);
                pstInsertMimeUserDaily.setInt(3, mud.count_out);
                pstInsertMimeUserDaily.setLong(4, mud.volume_in + mud.volume_out);
                pstInsertMimeUserDaily.setLong(5, mud.volume_in);
                pstInsertMimeUserDaily.setLong(6, mud.volume_out);
                pstInsertMimeUserDaily.setInt(7, mud.count_internal_in + mud.count_internal_out);
                pstInsertMimeUserDaily.setInt(8, mud.count_internal_in);
                pstInsertMimeUserDaily.setInt(9, mud.count_internal_out);
                pstInsertMimeUserDaily.setLong(10, mud.volume_internal_in + mud.volume_internal_out);
                pstInsertMimeUserDaily.setLong(11, mud.volume_internal_in);
                pstInsertMimeUserDaily.setLong(12, mud.volume_internal_out);
                pstInsertMimeUserDaily.setDate(13, DateHelper.sqld(mud.day));
                pstInsertMimeUserDaily.setInt(14, userId);
                pstInsertMimeUserDaily.setInt(15, fileId);
                db.executeUpdate(pstInsertMimeUserDaily);
            }
            usrMimeDailyAdded++;
            if (usrMimeDailyAdded % 1000 == 0 && usrMimeDailyAdded > 0) _logger.info(usrMimeDailyAdded + " users/mime/day added or updated");
        } finally {
            db.exit();
        }
    }

    public void addSpamDaily(SpamDaily sd) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstGetStatus.setString(1, sd.status);
            Integer statusId = DbHelper.getKey(pstGetStatus);
            if (statusId == null) {
                _logger.warn("Inserting new spam status :" + sd.status);
                pstAddStatus.setString(1, sd.status);
                int res = db.executeUpdate(pstAddStatus);
                if (res != 1) throw new Exception("Error when inserting a new spam status : " + sd.status);
                pstGetStatus.setString(1, sd.status);
                statusId = DbHelper.getKey(pstGetStatus);
                if (statusId == null) throw new Exception("Error when trying to get a newly inserted status :" + sd.status);
            }
            pstUpdateSpamDaily.setInt(1, sd.instances.intValue());
            pstUpdateSpamDaily.setDate(2, DateHelper.sqld(sd.date));
            pstUpdateSpamDaily.setInt(3, statusId.intValue());
            if (db.executeUpdate(pstUpdateSpamDaily) == 0) {
                pstInsertSpamDaily.setDate(1, DateHelper.sqld(sd.date));
                pstInsertSpamDaily.setInt(2, statusId.intValue());
                pstInsertSpamDaily.setInt(3, sd.instances.intValue());
                db.executeUpdate(pstInsertSpamDaily);
            }
        } finally {
            db.exit();
        }
    }

    public int addOrGetCrossDomainId(String _domainName) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            String domainName = DbHelper.nullify(_domainName);
            pstGetCrossDomain.setString(1, domainName);
            Integer domId = DbHelper.getKey(pstGetCrossDomain);
            if (domId != null) return domId;
            pstAddCrossDomain.setString(1, domainName);
            return DbHelper.getIntKey(pstAddCrossDomain);
        } finally {
            db.exit();
        }
    }

    public void addOrUpdateExternalDomainInfo(String domainName, Date registrationDate, Date expirationDate, Date firstMessageDate) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            int domainId = addOrGetCrossDomainId(domainName);
            pstGetExternalDomainInfo.setInt(1, domainId);
            ResultSet rs = db.executeQuery(pstGetExternalDomainInfo);
            if (rs.next()) {
                Date registration = DateHelper.toDateOrNull(rs.getDate(1));
                Date expiration = DateHelper.toDateOrNull(rs.getDate(2));
                Date firstMessage = DateHelper.toDateOrNull(rs.getDate(3));
                if (registration == null) registration = registrationDate; else if (registrationDate != null && registrationDate.before(registration)) registration = registrationDate;
                if (expiration == null) expiration = expirationDate; else if (expirationDate != null && expirationDate.after(expiration)) expiration = expirationDate;
                if (firstMessage == null) firstMessage = firstMessageDate; else if (firstMessageDate != null && firstMessageDate.before(firstMessage)) firstMessage = firstMessageDate;
                pstUpdateExternalDomainInfo.setDate(1, DateHelper.sqldOrNull(registrationDate));
                pstUpdateExternalDomainInfo.setDate(2, DateHelper.sqldOrNull(expirationDate));
                pstUpdateExternalDomainInfo.setDate(3, DateHelper.sqldOrNull(firstMessageDate));
                pstUpdateExternalDomainInfo.setInt(4, domainId);
                db.executeUpdate(pstUpdateExternalDomainInfo);
                _logger.info("External domain infos updated for domain " + domainName);
            } else {
                pstAddExternalDomainInfo = db.prepareStatement("INSERT INTO mail.t_external_domain_info (e_cross_domain_id, registration_date, expiration_date, first_message_date) VALUES (?, ?, ?, ?)");
                pstAddExternalDomainInfo.setInt(1, domainId);
                pstAddExternalDomainInfo.setDate(2, DateHelper.sqldOrNull(registrationDate));
                pstAddExternalDomainInfo.setDate(3, DateHelper.sqldOrNull(expirationDate));
                pstAddExternalDomainInfo.setDate(4, DateHelper.sqldOrNull(firstMessageDate));
                db.executeUpdate(pstAddExternalDomainInfo);
                _logger.info("External domain infos added for domain " + domainName);
            }
        } finally {
            db.exit();
        }
    }

    public void addOrUpdateExternalDomainDaily(Date d, String domainName, double magnitude, double ratio) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            int domainId = addOrGetCrossDomainId(domainName);
            pstUpdateExternalDomainDaily.setDouble(1, magnitude);
            pstUpdateExternalDomainDaily.setDouble(2, ratio);
            pstUpdateExternalDomainDaily.setInt(3, domainId);
            pstUpdateExternalDomainDaily.setDate(4, DateHelper.sqld(d));
            int res = db.executeUpdate(pstUpdateExternalDomainDaily);
            if (res == 0) {
                pstAddExternalDomainDaily.setDouble(1, magnitude);
                pstAddExternalDomainDaily.setDouble(2, ratio);
                pstAddExternalDomainDaily.setInt(3, domainId);
                pstAddExternalDomainDaily.setDate(4, DateHelper.sqld(d));
                db.executeUpdate(pstAddExternalDomainDaily);
            }
        } finally {
            db.exit();
        }
    }

    public void addOrUpdateGlobalDaily(Date d, long spamVolume, long mailVolume) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            pstUpdGlobalDaily.setLong(1, mailVolume);
            pstUpdGlobalDaily.setLong(2, spamVolume);
            pstUpdGlobalDaily.setDate(3, DateHelper.sqld(d));
            if (db.executeUpdate(pstUpdGlobalDaily) == 0) {
                pstAddGlobalDaily.setLong(1, mailVolume);
                pstAddGlobalDaily.setLong(2, spamVolume);
                pstAddGlobalDaily.setDate(3, DateHelper.sqld(d));
                db.executeUpdate(pstAddGlobalDaily);
            }
        } finally {
            db.exit();
        }
    }
}
