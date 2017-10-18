package ru.cybersms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import ru.cybersms.model.response.BalanceResponse;
import ru.cybersms.model.obj.Bulk;
import ru.cybersms.model.response.BulksResponse;
import ru.cybersms.model.obj.Contact;
import ru.cybersms.model.response.ContactsResponse;
import ru.cybersms.model.response.CountResponse;
import ru.cybersms.model.response.HistoryResponse;
import ru.cybersms.model.response.PriceResponse;
import ru.cybersms.model.response.SendResponse;
import ru.cybersms.model.response.StatusResponse;
import ru.cybersms.model.response.IdResponse;
import ru.cybersms.model.JsonDateConverter;
import ru.cybersms.model.obj.Message;
import ru.cybersms.model.response.TagsResponse;
import ru.cybersms.model.obj.Task;
import ru.cybersms.model.request.RequestProcessor;
import ru.cybersms.model.response.TasksResponse;

/**
 * 
 * @author Андрей Шерцингер <support@cybersms.ru>
 */
public class CyberSmsImpl implements CyberSms {

    private RequestProcessor processor;

    private String user;

    private String apiKey;

    private String url;

    private Gson gson;

    private boolean plainKey;

    private DateFormat dateTimeFormat;

    private DateFormat dateFormat;

    /**
     * Default constructor.
     * 
     * @param processor Implementation of {@link RequestProcessor} for request execution.
     * @param user CyberSMS's account name(email address).
     * @param apiKey API key for account.
     * @throws Exception 
     */
    public CyberSmsImpl(RequestProcessor processor, String user, String apiKey) throws Exception {
        init(processor, user, apiKey, (String) null);
    }

    /**
     * Constructor with URL.
     * 
     * @param processor Implementation of {@link RequestProcessor} for request execution.
     * @param user CyberSMS's account name(email address).
     * @param apiKey API key for account.
     * @param url API URL. Must end with '<code>/</code>'. Default: <code>https://api.cybersms.ru/</code>
     * @throws Exception 
     */
    public CyberSmsImpl(RequestProcessor processor, String user, String apiKey, String url) throws Exception {
        init(processor, user, apiKey, url);
    }

    private void init(RequestProcessor processor, String user, String apiKey, String url) throws Exception {
        this.processor = processor;
        this.user = user;
        this.apiKey = apiKey;
        if (StringUtils.isNotEmpty(url)) {
            this.url = url;
        } else {
            this.url = "https://api.cybersms.ru/";
        }
        this.plainKey = this.url.startsWith("https");
        this.processor.init(this.url);
        this.gson = (new GsonBuilder()).registerTypeAdapter(Date.class, new JsonDateConverter()).create();
        this.dateTimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        this.dateFormat = new SimpleDateFormat(DATE_FORMAT);
        this.dateTimeFormat.setTimeZone(TIME_ZONE_UTC);
        this.dateFormat.setTimeZone(TIME_ZONE_UTC);
    }

    /**
     * The getter for plainKey flag.
     * <p>This flag determines how the request is signed.<br>
     * If flag is TRUE request will contain plain '<code>apiKey</code>' param.
     * Otherwise there will be '<code>sign</code>' md5(sha1) hash param. 
     * <p>This param depends on url automatically(for https scheme false, for http true) for security.
     * @return plainKey the flag
     */
    public boolean isPlainKey() {
        return plainKey;
    }

    /**
     * The setter for plainKey flag. 
     * <p>This flag determines how the request is signed.<br>
     * If flag is TRUE request will contain plain '<code>apiKey</code>' param.
     * Otherwise there will be '<code>sign</code>' md5(sha1) hash param. 
     * <p>This param depends on url automatically(for https scheme false, for http true) for security.
     * 
     * @param plainKey the flag
     */
    public void setPlainKey(boolean plainKey) {
        this.plainKey = plainKey;
    }

    @Override
    public BalanceResponse checkBalance() throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        String responseBody = this.makeRequest("user/balance", params);
        return gson.fromJson(responseBody, BalanceResponse.class);
    }

    @Override
    public SendResponse sendSms(Message message, Boolean test) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("recipients", message.getRecipients());
        params.put("recipients", message.getRecipients());
        params.put("message", message.getMessage());
        if (message.getSender() != null) {
            params.put("sender", message.getSender());
        }
        if (message.getLifetime() != null) {
            params.put("lifetime", String.valueOf(message.getLifetime()));
        }
        if (test != null) {
            params.put("test", test ? "1" : "0");
        }
        String responseBody = this.makeRequest("message/send", params);
        return gson.fromJson(responseBody, SendResponse.class);
    }

    @Override
    public StatusResponse getStatus(final Integer[] message_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("messages_id", StringUtils.join(message_id, ','));
        String responseBody = this.makeRequest("message/status", params);
        return gson.fromJson(responseBody, StatusResponse.class);
    }

    @Override
    public PriceResponse getPrice(Message message) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("recipients", message.getRecipients());
        params.put("message", message.getMessage());
        String responseBody = this.makeRequest("message/price", params);
        return gson.fromJson(responseBody, PriceResponse.class);
    }

    @Override
    public HistoryResponse getHistory(Integer[] message_id, String recipient, String sender, String status, Date date_from, Date date_to, Integer bulk_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (message_id != null && message_id.length > 0) {
            params.put("id", StringUtils.join(message_id, ','));
        }
        if (bulk_id != null) {
            params.put("bulk_id", String.valueOf(bulk_id));
        }
        if (StringUtils.isNotBlank(recipient)) {
            params.put("recipient", recipient);
        }
        if (sender != null) {
            params.put("sender", sender);
        }
        if (StringUtils.isNotBlank(status)) {
            params.put("status", status);
        }
        if (date_from != null) {
            params.put("date_from", dateTimeFormat.format(date_from));
        }
        if (date_to != null) {
            params.put("date_to", dateTimeFormat.format(date_to));
        }
        String responseBody = this.makeRequest("message/history", params);
        return gson.fromJson(responseBody, HistoryResponse.class);
    }

    @Override
    public IdResponse createTag(String name) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("name", name);
        String responseBody = this.makeRequest("tag/create", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public CountResponse deleteTag(Integer[] tag_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (tag_id != null && tag_id.length > 0) {
            params.put("id", StringUtils.join(tag_id, ','));
        }
        String responseBody = this.makeRequest("tag/delete", params);
        return gson.fromJson(responseBody, CountResponse.class);
    }

    @Override
    public TagsResponse listTag(Integer[] tag_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (tag_id != null && tag_id.length > 0) {
            params.put("id", StringUtils.join(tag_id, ','));
        }
        String responseBody = this.makeRequest("tag/list", params);
        return gson.fromJson(responseBody, TagsResponse.class);
    }

    @Override
    public IdResponse updateTag(int tag_id, String name) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("id", String.valueOf(tag_id));
        params.put("name", name);
        String responseBody = this.makeRequest("tag/update", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public IdResponse createContact(Contact contact) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (contact.getPhone() != null) {
            params.put("phone", String.valueOf(contact.getPhone()));
        }
        if (contact.getFirstName() != null) {
            params.put("first_name", String.valueOf(contact.getFirstName()));
        }
        if (contact.getLastName() != null) {
            params.put("last_name", String.valueOf(contact.getLastName()));
        }
        if (contact.getSecondName() != null) {
            params.put("patronymic", String.valueOf(contact.getSecondName()));
        }
        if (contact.getBirthDay() != null) {
            params.put("birth_date", dateFormat.format(contact.getBirthDay()));
        }
        if (contact.getDescription() != null) {
            params.put("description", String.valueOf(contact.getDescription()));
        }
        if (contact.getTags() != null && !contact.getTags().isEmpty()) {
            params.put("tags_list", StringUtils.join(contact.getTags(), ','));
        }
        if (contact.getParam1() != null) {
            params.put("param1", String.valueOf(contact.getParam1()));
        }
        if (contact.getParam2() != null) {
            params.put("param2", String.valueOf(contact.getParam2()));
        }
        String responseBody = this.makeRequest("contact/create", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public IdResponse updateContact(Contact contact) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("id", String.valueOf(contact.getId()));
        if (contact.getPhone() != null) {
            params.put("phone", String.valueOf(contact.getPhone()));
        }
        if (contact.getFirstName() != null) {
            params.put("first_name", String.valueOf(contact.getFirstName()));
        }
        if (contact.getLastName() != null) {
            params.put("last_name", String.valueOf(contact.getLastName()));
        }
        if (contact.getSecondName() != null) {
            params.put("patronymic", String.valueOf(contact.getSecondName()));
        }
        if (contact.getBirthDay() != null) {
            params.put("birth_date", dateFormat.format(contact.getBirthDay()));
        }
        if (contact.getDescription() != null) {
            params.put("description", String.valueOf(contact.getDescription()));
        }
        if (contact.getTags() != null && !contact.getTags().isEmpty()) {
            params.put("tags_list", StringUtils.join(contact.getTags(), ','));
        }
        if (contact.getParam1() != null) {
            params.put("param1", String.valueOf(contact.getParam1()));
        }
        if (contact.getParam2() != null) {
            params.put("param2", String.valueOf(contact.getParam2()));
        }
        String responseBody = this.makeRequest("contact/update", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public CountResponse deleteContact(Integer[] contact_id, String[] phone) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (contact_id != null && contact_id.length > 0) {
            params.put("id", StringUtils.join(contact_id, ','));
        }
        if (phone != null && phone.length > 0) {
            params.put("phone", StringUtils.join(phone, ','));
        }
        String responseBody = this.makeRequest("contact/delete", params);
        return gson.fromJson(responseBody, CountResponse.class);
    }

    @Override
    public ContactsResponse listContact(Integer[] contact_id, Integer[] tag_id, String[] phone) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (contact_id != null && contact_id.length > 0) {
            params.put("id", StringUtils.join(contact_id, ','));
        }
        if (tag_id != null && tag_id.length > 0) {
            params.put("tags", StringUtils.join(tag_id, ','));
        }
        if (phone != null && phone.length > 0) {
            params.put("phone", StringUtils.join(phone, ','));
        }
        String responseBody = this.makeRequest("contact/list", params);
        return gson.fromJson(responseBody, ContactsResponse.class);
    }

    @Override
    public IdResponse createBulk(Bulk bulk) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (StringUtils.isNotEmpty(bulk.getName())) {
            params.put("name", String.valueOf(bulk.getName()));
        }
        if (StringUtils.isNotEmpty(bulk.getSender())) {
            params.put("sender", String.valueOf(bulk.getSender()));
        }
        if (StringUtils.isNotEmpty(bulk.getMessage())) {
            params.put("message", String.valueOf(bulk.getMessage()));
        }
        if (bulk.getLifetime() > 0) {
            params.put("lifetime", String.valueOf(bulk.getLifetime()));
        }
        if (bulk.getInclude_tags() != null && bulk.getInclude_tags().length > 0) {
            params.put("include_tags_list", StringUtils.join(bulk.getInclude_tags(), ','));
        }
        if (bulk.getExclude_tags() != null && bulk.getExclude_tags().length > 0) {
            params.put("exclude_tags_list", StringUtils.join(bulk.getExclude_tags(), ','));
        }
        if (bulk.isSend_email_report() && StringUtils.isNotEmpty(bulk.getReport_email())) {
            params.put("send_email_report", bulk.isSend_email_report() ? "1" : "0");
            params.put("report_email", String.valueOf(bulk.getReport_email()));
        }
        if (bulk.isSend_sms_report() && StringUtils.isNotEmpty(bulk.getReport_phone())) {
            params.put("send_sms_report", bulk.isSend_sms_report() ? "1" : "0");
            params.put("report_phone", String.valueOf(bulk.getReport_phone()));
        }
        String responseBody = this.makeRequest("bulk/create", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public IdResponse updateBulk(Bulk bulk) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("id", String.valueOf(bulk.getId()));
        if (StringUtils.isNotEmpty(bulk.getName())) {
            params.put("name", String.valueOf(bulk.getName()));
        }
        if (bulk.getSender() != null) {
            params.put("sender", String.valueOf(bulk.getSender()));
        }
        if (StringUtils.isNotEmpty(bulk.getMessage())) {
            params.put("message", String.valueOf(bulk.getMessage()));
        }
        if (bulk.getLifetime() > 0) {
            params.put("lifetime", String.valueOf(bulk.getLifetime()));
        }
        if (bulk.getInclude_tags() != null && bulk.getInclude_tags().length > 0) {
            params.put("include_tags_list", StringUtils.join(bulk.getInclude_tags(), ','));
        }
        if (bulk.getExclude_tags() != null && bulk.getExclude_tags().length > 0) {
            params.put("exclude_tags_list", StringUtils.join(bulk.getExclude_tags(), ','));
        }
        if (bulk.isSend_email_report() && StringUtils.isNotEmpty(bulk.getReport_email())) {
            params.put("send_email_report", bulk.isSend_email_report() ? "1" : "0");
            params.put("report_email", String.valueOf(bulk.getReport_email()));
        }
        if (bulk.isSend_sms_report() && StringUtils.isNotEmpty(bulk.getReport_phone())) {
            params.put("send_sms_report", bulk.isSend_sms_report() ? "1" : "0");
            params.put("report_phone", String.valueOf(bulk.getReport_phone()));
        }
        String responseBody = this.makeRequest("bulk/update", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public CountResponse deleteBulk(Integer[] bulk_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (bulk_id != null && bulk_id.length > 0) {
            params.put("id", StringUtils.join(bulk_id, ','));
        }
        String responseBody = this.makeRequest("bulk/delete", params);
        return gson.fromJson(responseBody, CountResponse.class);
    }

    @Override
    public IdResponse sendBulk(int bulk_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("id", String.valueOf(bulk_id));
        String responseBody = this.makeRequest("bulk/send", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public IdResponse resumeBulk(int bulk_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("id", String.valueOf(bulk_id));
        String responseBody = this.makeRequest("bulk/resume", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public BulksResponse listBulk(Integer[] bulk_id, String name, String sender, String status, Date date_from, Date date_to) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (bulk_id != null && bulk_id.length > 0) {
            params.put("id", StringUtils.join(bulk_id, ','));
        }
        if (StringUtils.isNotBlank(name)) {
            params.put("name", name);
        }
        if (sender != null) {
            params.put("sender", sender);
        }
        if (StringUtils.isNotBlank(status)) {
            params.put("status", status);
        }
        if (date_from != null) {
            params.put("date_from", dateTimeFormat.format(date_from));
        }
        if (date_to != null) {
            params.put("date_to", dateTimeFormat.format(date_to));
        }
        String responseBody = this.makeRequest("bulk/list", params);
        return gson.fromJson(responseBody, BulksResponse.class);
    }

    @Override
    public IdResponse createTask(Task task) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (StringUtils.isNotEmpty(task.getName())) {
            params.put("name", String.valueOf(task.getName()));
        }
        if ((task.getStart_at() != null) || (task.getExpires_at() != null)) {
            if (task.getStart_at() != null) {
                params.put("starts_at", dateTimeFormat.format(task.getStart_at()));
            }
            if (task.getExpires_at() != null) {
                params.put("expires_at", dateTimeFormat.format(task.getExpires_at()));
            }
        }
        if (task.getRepeat() != null) {
            params.put("repeat", String.valueOf(task.getRepeat()));
        }
        params.put("is_active", task.isActive() ? "1" : "0");
        params.put("type", String.valueOf(task.getType()));
        switch(task.getType()) {
            case message:
                if (StringUtils.isNotEmpty(task.getMessage().getRecipients())) {
                    params.put("recipient", String.valueOf(task.getMessage().getRecipients()));
                }
                if (task.getMessage().getSender() != null) {
                    params.put("sender", String.valueOf(task.getMessage().getSender()));
                }
                if (StringUtils.isNotEmpty(task.getMessage().getMessage())) {
                    params.put("message", String.valueOf(task.getMessage().getMessage()));
                }
                if (task.getMessage().getLifetime() != null) {
                    params.put("lifetime", String.valueOf(task.getMessage().getLifetime()));
                }
                break;
            case bulk:
                if (task.getBulk_id() != null) {
                    params.put("bulk_id", String.valueOf(task.getBulk_id()));
                }
                break;
            case application:
                if (task.getApplication_id() != null) {
                    params.put("application_id", String.valueOf(task.getApplication_id()));
                }
                break;
        }
        String responseBody = this.makeRequest("task/create", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public IdResponse updateTask(Task task) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("id", String.valueOf(task.getId()));
        if (StringUtils.isNotEmpty(task.getName())) {
            params.put("name", String.valueOf(task.getName()));
        }
        if (task.getStart_at() != null) {
            params.put("starts_at", dateTimeFormat.format(task.getStart_at()));
        }
        if (task.getExpires_at() != null) {
            params.put("expires_at", dateTimeFormat.format(task.getExpires_at()));
        }
        if (task.getRepeat() != null) {
            params.put("repeat", String.valueOf(task.getRepeat()));
        }
        params.put("is_active", task.isActive() ? "1" : "0");
        params.put("type", String.valueOf(task.getType()));
        switch(task.getType()) {
            case message:
                if (StringUtils.isNotEmpty(task.getMessage().getRecipients())) {
                    params.put("recipient", String.valueOf(task.getMessage().getRecipients()));
                }
                if (task.getMessage().getSender() != null) {
                    params.put("sender", String.valueOf(task.getMessage().getSender()));
                }
                if (StringUtils.isNotEmpty(task.getMessage().getMessage())) {
                    params.put("message", String.valueOf(task.getMessage().getMessage()));
                }
                if (task.getMessage().getLifetime() > 0) {
                    params.put("lifetime", String.valueOf(task.getMessage().getLifetime()));
                }
                break;
            case bulk:
                if (task.getBulk_id() != null) {
                    params.put("bulk_id", String.valueOf(task.getBulk_id()));
                }
                break;
            case application:
                if (task.getApplication_id() != null) {
                    params.put("application_id", String.valueOf(task.getApplication_id()));
                }
                break;
        }
        String responseBody = this.makeRequest("task/update", params);
        return gson.fromJson(responseBody, IdResponse.class);
    }

    @Override
    public CountResponse deleteTask(Integer[] task_id) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (task_id != null && task_id.length > 0) {
            params.put("id", StringUtils.join(task_id, ','));
        }
        String responseBody = this.makeRequest("task/delete", params);
        return gson.fromJson(responseBody, CountResponse.class);
    }

    @Override
    public TasksResponse listTask(Integer[] task_id, Integer type, Boolean active, Integer repeat, String recipient, String sender) throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        if (task_id != null && task_id.length > 0) {
            params.put("id", StringUtils.join(task_id, ','));
        }
        if (type != null) {
            params.put("type", String.valueOf(type));
        }
        if (active != null) {
            params.put("is_active", active ? "1" : "0");
        }
        if (repeat != null) {
            params.put("repeat", String.valueOf(repeat));
        }
        if (StringUtils.isNotBlank(recipient)) {
            params.put("recipient", recipient);
        }
        if (StringUtils.isNotBlank(sender)) {
            params.put("sender", sender);
        }
        String responseBody = this.makeRequest("task/list", params);
        return gson.fromJson(responseBody, TasksResponse.class);
    }

    private String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private String makeRequest(String action, final Map<String, String> params) throws Exception {
        final Map<String, String> linkedMap = new LinkedHashMap<String, String>();
        linkedMap.put("user", user);
        for (String p : params.keySet()) {
            linkedMap.put(p, params.get(p));
        }
        if (this.plainKey) {
            linkedMap.put("apikey", this.apiKey);
        } else {
            String tosign = this.user + StringUtils.join(params.values(), "") + this.apiKey;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String sign = convertToHex(md5.digest(convertToHex(sha1.digest(tosign.getBytes())).getBytes()));
            linkedMap.put("sign", sign);
        }
        return processor.process(action, linkedMap);
    }
}
