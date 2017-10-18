package ru.yep.forum.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import ru.yep.forum.core.Part;
import ru.yep.forum.core.Task;
import ru.yep.forum.core.Tasks;
import ru.yep.forum.core.TimePoint;
import ru.yep.forum.core.Topic;
import ru.yep.forum.core.Topics;
import ru.yep.forum.core.User;
import ru.yep.forum.core.Users;

/**
 * @author Oleg Orlov
 */
public class ForumUtil {

    private static final Logger logger = Logger.getLogger(ForumUtil.class.getName());

    public interface URLs {

        public static final String LOGIN_PAGE = "login.jsp";

        public static final String LOGOUT_PAGE = "logout";

        public static final String MAIN_PAGE = "forum.jsp";

        public static final String MAIN_PAGE_SERVLET = "forum";

        public static final String NEW_TOPIC_PAGE = "newtopic.jsp";

        public static final String TOPIC_PAGE = "topic.jsp";

        public static final String WORKSHOP_PAGE = "workshop.jsp";

        public static final String WORKSHOP_PAGE_SERVLET = "workshop";

        public static final String ADMIN_PAGE = "admin.jsp";

        public static final String ADMIN_PAGE_SERVLET = "admin";

        public static final String ADMIN_INVITATIONS = "admin_invitations.jsp";

        public static final String PROFILE_PAGE = "profile.jsp";

        public static final String PROFILE_SERVLET = "profile";

        public static final String NEW_ACCOUNT_PAGE = "new_account.jsp";

        public static final String TASKLIST_PAGE = "tasklist.jsp";

        public static final String TASKLIST_SERVLET = "tasks";

        public static final String TASK_PAGE = "task.jsp";

        public static final String NEW_TASK_SERVLET = "newtask";

        public static final String NEW_TASK_PAGE = "newtask.jsp";

        public static final String PART_PAGE = "part.jsp";

        public static final String NOREFERENCE = "javascript:void(0)";

        public static final String LOGIN_REQUEST_PAGE = "loginreq.jsp";

        public static final String ADMIN_USERLIST_PAGE = "admin_users.jsp";

        public static final String USERLIST_SERVLET = "users";

        public static final String FILES_SERLVLET = "files";

        public static final String FILES_PAGE = "files.jsp";
    }

    public interface MessageCommands {

        public static final String OK = "OK";

        public static final String POSTPONED = "POSTPONED";

        public static final String SKIPPED = "SKIPPED";
    }

    public interface SessionAttributes {

        public static final String REDIRECTED_FROM_URI = "redirectedfromuri";

        public static final String REDIRECTED_REQUEST_BEAN = "redirectedrequestmap";

        public static final String FORUM_SESSION = "forumsession";

        public static final String ADMIN_PAGE_ACCOUNT_REQUEST_MAP = "admin_page_account_request_map";
    }

    /**
	 * is used to pass params from jsp to jspf fragments
	 */
    public interface RequestAttributes {

        public static final String SHOW_TOPICS = "showtopics";

        public static final String SHOW_COMMENTS = "showcomments";

        public static final String SKIP_AVATAR = "skipavatar";

        public static final String TOPIC = "topic";

        public static final String PART = "part";

        public static final String COMMENT = "comment";

        public static final String COMMENT_INC_USE_TABLE = "comment.inc.usetable";

        public static final String PAGE_BEAN = "pagebean";

        public static final String PAGE_LOGINREQ_ERROR = "page_loginreq_error";

        public static final String FOOTER_MESSAGE = "footer_message";

        public static final String PROFILE_PAGE_ACCOUNT_REQUEST = "profile_page_account_request";
    }

    public interface ConfigKeys {

        public static final String MAIL_SMTP_HOST = "mail.smtp.host";

        public static final String MAIL_SMTP_PORT = "mail.smtp.port";

        public static final String MAIL_SMTP_LOGIN = "mail.smtp.login";

        public static final String MAIL_SMTP_PASSWD = "mail.smtp.passwd";

        public static final String MAIL_FORUM_NAME = "mail.forum";

        public static final String MAIL_FORUM_MAILADDR = "mail.forum.addr";

        public static final String NOTIFYSUPPORT_CHECKTIMEOUT = "notifysupport.checktimeout";
    }

    public static final String PARTS_PATH_DELIMITER = "/";

    /** <b>Important:</b> it's used for parameters parsing. Keep it untouched.<br>
     * format is: <code>YYYY-MM-DD</code>*/
    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    public static final SimpleDateFormat SDF_DECORATE_DATE = new SimpleDateFormat("dd.MM.yyyy");

    public static final SimpleDateFormat SDF_DECORATE = new SimpleDateFormat("dd.MM.yyyy, HH:mm");

    public static final String SESSION_COOKIE = "sid";

    public static String createHref(String uri, String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=\"").append(uri).append("\">").append(text).append("</a>");
        return sb.toString();
    }

    ;

    /** @return blank href with specified text */
    public static String createHref(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=\"").append(URLs.NOREFERENCE).append("\">").append(text).append("</a>");
        return sb.toString();
    }

    ;

    /** For UI purposes. Return anchor with proper href.<br>
	 * if (user == null || NOBODY) return user.getName()*/
    public static final String createHrefToUser(User user) {
        if (user == Users.NOBODY || user == null) return Users.NOBODY.getName();
        String textToShow = (user.getName() != null && user.getName().trim().length() > 0) ? user.getName() : user.getEmail();
        return createHref(getURIForUser(user), textToShow);
    }

    public static final String createHrefToUser(int uid) {
        User user = Users.getDefault().getUser(uid);
        return createHrefToUser(user);
    }

    public static final String createHrefToTopic(Topic topic) {
        return createHref(topic.getURI(), topic.name);
    }

    public static final String createHrefToPart(Part part) {
        return createHref(part.getURI(), part.name);
    }

    public static final String createHrefToTaskList(User user) {
        if (user == null || user == Users.NOBODY) return createHref(URLs.TASKLIST_SERVLET, "Без исполнителя");
        return createHref(URLs.TASKLIST_SERVLET + "?uid=" + user.getId(), user.getName());
    }

    public static final String createHrefToTask(Task task) {
        return createHref(getURIForTask(task), task.getKey());
    }

    public static String getURIForUser(User user) {
        return URLs.PROFILE_SERVLET + "?uid=" + user.getId();
    }

    public static String getURIForTask(Task task) {
        return URLs.TASKLIST_SERVLET + "?taskid=" + task.getId();
    }

    public static String createPathHref(Part part) {
        StringBuilder sb = new StringBuilder();
        sb.append(createHref(URLs.MAIN_PAGE, "ГЛАВНАЯ"));
        sb.append(" - ");
        ArrayList parentsList = new ArrayList();
        Part parentPart = part.getParent();
        while (parentPart.getId() != Topics.ID_ROOT_PART) {
            parentsList.add(parentPart);
            parentPart = parentPart.getParent();
        }
        Part[] parents = (Part[]) parentsList.toArray(new Part[0]);
        for (int iPart = parents.length - 1; iPart >= 0; iPart--) {
            Part nextParent = parents[iPart];
            sb.append(createHrefToPart(nextParent));
            sb.append(" - ");
        }
        sb.append(createHrefToPart(part));
        return sb.toString();
    }

    public static String escape(String content) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '<') buffer.append("&lt;"); else if (c == '>') buffer.append("&gt;"); else if (c == '&') buffer.append("&amp;"); else if (c == '"') buffer.append("&quot;"); else if (c == '\'') buffer.append("&apos;"); else buffer.append(c);
        }
        return buffer.toString();
    }

    public static void prepareHeader(HttpServletResponse response) throws IOException {
        prepareHeader(response, "");
    }

    public static void prepareHeader(HttpServletResponse response, String title) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setDateHeader("Expires", -1);
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        out.println("<HTML>");
        out.println("<HEAD><TITLE>" + title + "</TITLE></HEAD>");
        out.println("<BODY>");
    }

    public static void prepareFooter(HttpServletResponse response) throws IOException {
        response.getWriter().println("</BODY>\n</HTML>");
    }

    /** @return value from cookie with specified name if cookie exists. <b>null</b> otherwise. */
    public static String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (int i = 0; i < cookies.length; i++) {
            if (cookies[i].getName().equals(name)) return cookies[i].getValue();
        }
        return null;
    }

    public static String printCookies(Cookie[] cookies) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        formatter.format("%20s  %20s  %20s  %20s  %3s  %5s\n", new String[] { "Name", "Value", "Domain", "Path", "Sec", "Age" });
        if (cookies != null) for (int i = 0; (i < cookies.length); i++) {
            Cookie coo = cookies[i];
            formatter.format("%20s  %20s  %20s  %20s  %3b  %5d\n", new Object[] { coo.getName(), coo.getValue(), coo.getDomain(), coo.getPath(), Boolean.valueOf(coo.getSecure()), Integer.valueOf(coo.getMaxAge()) });
        }
        return sb.toString();
    }

    public static String getSessionId(HttpServletRequest request) {
        return ForumUtil.getCookie(request, ForumUtil.SESSION_COOKIE);
    }

    /**
     * decorates inputed text as html
     * @param text - plain text
     */
    public static String decorateUrls(String text) {
        if (text == null) return "";
        StringBuffer sb = new StringBuffer();
        Pattern urlPattern = Pattern.compile("(\\w+://\\S*[\\w\\?\\.,:;\\&\\%/=]+)");
        Matcher urlMatcher = urlPattern.matcher(text);
        while (urlMatcher.find()) urlMatcher.appendReplacement(sb, createHref("$1", "$1"));
        urlMatcher.appendTail(sb);
        return sb.toString();
    }

    public static String decorateTextAsHtml(String text) {
        if (text == null) return "";
        if (BBCoder.bbCodePattern.matcher(text).find()) {
            text = new BBCoder().render(text);
        } else text = decorateUrls(text);
        return text.replaceAll("\n", "<br>");
    }

    private static final Pattern PATTERN_WO_QUOTES = Pattern.compile("\\[quote.*?\\].*\\[/quote\\]", Pattern.DOTALL);

    private static final Pattern PATTERN_WO_IMAGES = Pattern.compile("\\[img.*?\\].*\\[/img\\]", Pattern.DOTALL);

    public static String decorateAsShortHtml(String text) {
        if (text == null) return "";
        String woQuotes = PATTERN_WO_QUOTES.matcher(text).replaceFirst("");
        String woImages = PATTERN_WO_IMAGES.matcher(woQuotes).replaceFirst("");
        String woWhiteSpace = woImages.replaceFirst("[\n\r]+", "");
        String decorated = decorateTextAsHtml(woWhiteSpace);
        int indexOfBR = decorated.indexOf("<br>");
        String firstLine = indexOfBR == -1 ? decorated : decorated.substring(0, indexOfBR);
        int lastIndex = firstLine.indexOf(" ", 50) != -1 ? firstLine.indexOf(" ", 50) : firstLine.length();
        return firstLine.substring(0, lastIndex);
    }

    /** Is used for UI purposes  */
    public static String decorate(Date date) {
        if (date == null) return "";
        return SDF_DECORATE.format(date);
    }

    /** Is used for UI purposes  */
    public static String decorateOnlyDate(Date date) {
        if (date == null) return "";
        return SDF_DECORATE_DATE.format(date);
    }

    public static String decorate(TimePoint point) {
        if (point.getDate() != Tasks.FUTURE) return SDF_DECORATE.format(point.getDate()); else return "Unsheduled";
    }

    public static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes());
            return ForumUtil.bufferToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bufferToHex(byte buffer[]) {
        return ForumUtil.bufferToHex(buffer, 0, buffer.length);
    }

    public static String bufferToHex(byte buffer[], int startOffset, int length) {
        StringBuffer hexString = new StringBuffer(2 * length);
        int endOffset = startOffset + length;
        for (int i = startOffset; i < endOffset; i++) ForumUtil.appendHexPair(buffer[i], hexString);
        return hexString.toString();
    }

    private static void appendHexPair(byte b, StringBuffer hexString) {
        char highNibble = kHexChars[(b & 0xF0) >> 4];
        char lowNibble = kHexChars[b & 0x0F];
        hexString.append(highNibble);
        hexString.append(lowNibble);
    }

    private static final char kHexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static class HexOutputStream extends OutputStream {

        StringWriter sw;

        public HexOutputStream(StringWriter sw) {
            this.sw = sw;
        }

        public void write(int b) throws IOException {
            int big = kHexChars[((b >> 4) & 0xF)];
            sw.write(big);
            int little = kHexChars[((b >> 0) & 0xF)];
            sw.write(little);
        }
    }

    public static class HexInputStream extends InputStream {

        StringReader sr;

        public HexInputStream(StringReader sr) {
            this.sr = sr;
        }

        private int unhex(int b) {
            for (int i = 0; i < kHexChars.length; i++) if (b == kHexChars[i]) return i;
            throw new IllegalArgumentException("" + b);
        }

        public int read() throws IOException {
            int big = unhex(sr.read()) << 4;
            int little = unhex(sr.read()) << 0;
            return big + little;
        }
    }

    public static final String[] MONTHNAMES = { "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь" };

    public static final String[] DAYS_SHORTS = { "вс", "пн", "вт", "ср", "чт", "пт", "сб" };

    public static int daysBetween(Date fromDate, Date toDate) {
        Calendar sourceCal = Calendar.getInstance();
        Calendar compareCal = Calendar.getInstance();
        sourceCal.setTime(fromDate);
        compareCal.set(sourceCal.get(Calendar.YEAR), sourceCal.get(Calendar.MONTH), sourceCal.get(Calendar.DAY_OF_MONTH));
        Date calFromDate = compareCal.getTime();
        sourceCal.setTime(toDate);
        compareCal.set(sourceCal.get(Calendar.YEAR), sourceCal.get(Calendar.MONTH), sourceCal.get(Calendar.DAY_OF_MONTH));
        Date calToDate = compareCal.getTime();
        long millisDiff = calToDate.getTime() - calFromDate.getTime();
        logger.finest("diff of dates " + (millisDiff));
        long secondsDiff = millisDiff / 1000;
        return Math.round(secondsDiff / (3600 * 24));
    }

    public static Date getToday() {
        Calendar cal = getCalendar(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Calendar getCalendar(Date dateFor) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateFor);
        return cal;
    }

    public static Calendar getBeginOfWeekBefore(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, dayOfWeek - 9);
        return cal;
    }

    public static Calendar getEndOfNextWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DAY_OF_MONTH, 8 - dayOfWeek);
        return cal;
    }

    public static Calendar getLastDayOfMonth(Date date) {
        Calendar monthEnd = Calendar.getInstance();
        monthEnd.setTime(date);
        monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        return monthEnd;
    }

    public static Calendar getFirstDayOfMonth(Date date) {
        Calendar monthStart = Calendar.getInstance();
        monthStart.setTime(date);
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        return monthStart;
    }

    public static String toString(String[] strings) {
        if (strings.length == 0) return "";
        StringBuilder sb = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            sb.append(", ").append(strings[i]);
        }
        return sb.toString();
    }

    public static Date parseDateSDF(String param) {
        if (param == null || param.trim().length() == 0) return null;
        try {
            return SDF.parse(param);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String documentToString(Document doc) {
        StringWriter sw = new StringWriter();
        try {
            Transformer transfer = TransformerFactory.newInstance().newTransformer();
            transfer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transfer.transform(new DOMSource(doc), new StreamResult(sw));
        } catch (Exception e) {
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
        }
        return sw.toString();
    }

    /**
	 * extracts application URL from request.
	 * @return full URL <b>without</b> last "/" ;
	 */
    public static String getAppURL(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String hostPath = requestURL.substring(0, requestURL.indexOf(request.getContextPath()));
        String appPath = hostPath + request.getContextPath();
        return appPath;
    }

    /**
	 * extracts servlet's URL from request.
	 * <b>Rules:</b><br>
	 * <code>http://host.ru/forum/topics/12345</code>
	 * should be handled as<br> 
	 * <code>http://host.ru/forum/topics?id=12345</code>
	 * 
	 * - last resource part is parameter
	 * - one before - resourceName 
	 * @return resourceName
	 */
    public static String getResourceName(HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String hostPath = requestURL.substring(0, requestURL.indexOf(request.getContextPath()));
        String appPath = hostPath + request.getContextPath();
        String contextPath = request.getRequestURI();
        Pattern regex = Pattern.compile("/");
        String[] parts = splitServed(request.getRequestURI(), "/");
        if (parts.length == 0) return null;
        return appPath;
    }

    /**
     * redirect to any page by sendRedirect
     * TODO: support FOOTER_MESSAGE persistent 
     * @param page - any page from URLs.
     */
    public static void redirectTo(String page, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.finest("redirect it to page. from_uri:" + request.getRequestURL());
        response.sendRedirect(page);
    }

    /**
     * redirect to any page by RequestDispatcher 
     * @param page - any page from URLs.
     */
    public static void forwardTo(String page, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.finest("redirect it to page. from_uri:" + request.getRequestURL());
        request.getRequestDispatcher("/" + page).forward(request, response);
    }

    public static void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.finest(" redirect it to loginPage. from_uri:" + request.getRequestURL());
        String requestedUri = extractUriToForward(request);
        request.getSession().setAttribute(SessionAttributes.REDIRECTED_FROM_URI, requestedUri);
        request.getSession().setAttribute(SessionAttributes.REDIRECTED_REQUEST_BEAN, new PageBean(request));
        request.getRequestDispatcher("/" + URLs.LOGIN_PAGE).forward(request, response);
    }

    public static PageBean getRequestBean(HttpServletRequest request) {
        if (!request.getRequestURI().endsWith("login")) {
            PageBean previousPageBean = (PageBean) request.getSession().getAttribute(SessionAttributes.REDIRECTED_REQUEST_BEAN);
            if (previousPageBean != null) {
                request.getSession().setAttribute(SessionAttributes.REDIRECTED_REQUEST_BEAN, null);
                return previousPageBean;
            }
        }
        PageBean result;
        if ((result = (PageBean) request.getAttribute(RequestAttributes.PAGE_BEAN)) == null) {
            result = new PageBean(request);
        }
        return result;
    }

    public static String extractUriToForward(HttpServletRequest request) {
        String requestedUri = request.getRequestURI().replaceAll(request.getContextPath(), "");
        return requestedUri;
    }

    /**
     * To be used by LoginServlet after redirectToLogin() call.  
     */
    public static void redirectToPrevious(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String requestedUri = (String) request.getSession().getAttribute(SessionAttributes.REDIRECTED_FROM_URI);
        logger.finest(" redirect back to previousPage. to_uri:" + requestedUri);
        if (requestedUri == null) {
            response.sendRedirect(URLs.MAIN_PAGE);
            return;
        }
        String relativeUrl = requestedUri.replaceAll("/", "");
        response.sendRedirect(relativeUrl);
    }

    /**
	 * Appends errorText to request's attribute to be printed in footer by footer.inc
	 */
    public static void sayError(String errorText, ServletRequest request) {
        request.setAttribute(RequestAttributes.FOOTER_MESSAGE, errorText);
    }

    /**
	 * Appends confirmationText to request's attribute to be printed in footer by footer.inc
	 */
    public static void sayConfirmation(String confirmationText, ServletRequest request) {
        request.setAttribute(RequestAttributes.FOOTER_MESSAGE, confirmationText);
    }

    /**
	 * split string and return all none empty parts
	 * @param what
	 * @param delim
	 * @return
	 */
    public static String[] splitServed(String what, String delim) {
        String[] parts = what.split(delim);
        ArrayList list = new ArrayList();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() == 0) continue;
            list.add(parts[i]);
        }
        return (String[]) list.toArray(new String[0]);
    }

    public static String genNextPath(String sourcePath) {
        return sourcePath + PARTS_PATH_DELIMITER + System.currentTimeMillis() / 1000;
    }

    public static String wrapPart(Part part, String content, String ident) {
        StringBuilder sb = new StringBuilder();
        int deep = part.getDeep();
        for (int i = 0; i < deep; i++) sb.append(ident);
        sb.append(content);
        return sb.toString();
    }

    public static String wrapPartName(Part part) {
        return wrapPart(part, part.getName(), "&nbsp;&nbsp;&nbsp;");
    }

    public static String wrapPartDesc(Part part) {
        return wrapPart(part, part.getDescription(), "&nbsp;&nbsp;&nbsp;");
    }

    public static class BBCoder {

        public static final Pattern bbCodePattern = Pattern.compile("\\[[biu(quote)(img)]{1}.*?\\]");

        public String render(String text) {
            text = text.replaceAll("\\[bbcode/\\]", "");
            text = text.replaceAll("\\[b\\]", "<b>");
            text = text.replaceAll("\\[/b\\]", "</b>");
            text = text.replaceAll("\\[i\\]", "<i>");
            text = text.replaceAll("\\[/i\\]", "</i>");
            text = text.replaceAll("\\[u\\]", "<u>");
            text = text.replaceAll("\\[/u\\]", "</u>");
            text = text.replaceAll("\\[quote.*?\\]", "" + "<div class=\"border\"><span class=\"smallgreyfont\">Цитата:</span><br>");
            text = text.replaceAll("\\[/quote\\]", "</div>");
            text = text.replaceAll("\\[img\\]", "<img src=\"");
            text = text.replaceAll("\\[/img\\]", "\" alt=\"\"></img>");
            return text;
        }
    }

    public static String toString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
