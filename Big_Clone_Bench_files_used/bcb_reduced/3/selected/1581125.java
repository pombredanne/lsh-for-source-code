package wikisquilter;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Statement;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Class corresponding to the thread objects devoted to process the log files
 * containing the log lines from the Squid server systems to be analysed.
 * @author ajreinoso
 */
public class SquidLogFileProcessor implements Runnable {

    /** Scanner object to read the log lines. */
    private Scanner squidLogFileSC;

    /** File object corresponding the log file to parse. */
    private File squidLogFile;

    /** File object corresponding the log file to parse. */
    private PrintWriter parsingLog;

    /** 
     * Names for the tables storing the different information elements from
     * the URLs.
     */
    private String cliReqTableName, filteredTableName, searchesTableName;

    /** Counter for the corresponding parsed lines and for the ones failed
     *  to be parsed.
     */
    private long totalWrongLines, totalRightLines, rightLines;

    private long totalFtWrongLines, totalFtRightLines, rightFtLines, searchLines;

    /** String object to store a SQL statement. */
    private StringBuilder insertStm, insertFtStm, searchFtStm;

    /** Connection to the database. */
    private Connection mysqldbcon;

    /** Statement object. */
    private Statement stm;

    /** Number of the database rows inserted per sentence. */
    private int numValInsert, mode;

    /** Flag for promiscuous mode. */
    private boolean promiscuous;

    /** Filter object specifying the information elements to be filtered. */
    private Filter filter;

    /** Object managing the connection to the database. */
    private DBManager db;

    /** Must match the length of the cliReqTable table field cr_ch_word. */
    private int CHWORDLENGTH = 30;

    /** Must match the length of the cliReqTable table field cr_action. */
    private int SPACTIONLENGTH = 30;

    /** Must match the length of the filteredTable table field f_title. */
    private final int URLLENGTH = 512;

    /** Does not write the information to the database. */
    public static final int SIMUL = 0;

    /** Writes the information to the database.  */
    public static final int REAL = 1;

    /** Parsed reference number.  */
    private String refNumber;

    /** Parsed date time.  */
    private String dateTime;

    /** Parsed project name.  */
    private int month;

    private String projectName;

    /** Database code for the filtered Wikimedia Foundaation project.  */
    private String projectDBCode;

    /** Parsed namespace.  */
    private String nameSpace;

    /** Database code for the filtered namespace. */
    private String nameSpaceDBCode;

    /** Parsed language.  */
    private String language;

    /** Database code for the filtered language. */
    private String languageDBCode;

    /** Parsed action. */
    private String spAction;

    /** Database code for the filtered action. */
    private String spActionDBCode;

    /** Parsed request method. */
    private String reqMethod;

    /** Database code for the filtered request method. */
    private String reqMethodDBCode;

    /** Parsed URL. */
    private String url;

    /** Parsed article title. */
    private String title;

    /** Parsed searched string. */
    private String searched;

    /** Parsed response time. */
    private String responseTime;

    /** Diggest object. */
    private MessageDigest md5hash;

    private Calendar cal;

    SimpleDateFormat sdf;

    /**
     * Instantiates a new thread to process and analyse the log lines from the
     * Squid server systems contained in a given log file. The analysis of the
     * log lines consist on both a parsing and filtering process to extract
     * a set of information elements from the log lines and to validate them
     * according to the analysis directives.
     * @param SquidlogFileNamep the name of the file containing the Squid log
     * lines to process.
     * @param cliReqtableNamep the name for the table storing information about
     * all the URLs without applying any sort of filter.
     * @param filteredTableNamep the name for the table storing the filtered 
     * information elements found in the log lines whose fields suit TODO MIRAR the analysis
     * directives.
     * @param searchesTableNamep the name for the table storing the information
     * elements from the filtered URLs which, moreover, correspond to search
     * operations.
     * @param filterp the object specifying the information elements to be
     * filtered.
     * @param dbp {@link DBManager DBManager} object providing a connection to
     * the database.
     * @param numValInsertp number of items to insert per sentence.
     * @param modep specifies whether the SQL statements are executed by the
     * database connection manager (real mode) or just written to the standard
     * output (simulation mode).
     * @param promiscuousp puts the application in promiscuous mode in order to
     * register information about all the URLs without applying any filter.
     * @param monthp month whose lines are going to be processed.
     * @throws IOException if an I/O error occurs.
     * @throws SQLException if the execution of the SQL statement fails.
     */
    public SquidLogFileProcessor(String SquidlogFileNamep, String cliReqtableNamep, String filteredTableNamep, String searchesTableNamep, Filter filterp, DBManager dbp, int numValInsertp, int modep, boolean promiscuousp, int monthp) throws IOException, SQLException {
        db = dbp;
        stm = db.getConnection().createStatement();
        squidLogFileSC = new Scanner(new GZIPInputStream(new BufferedInputStream(new FileInputStream(SquidlogFileNamep))));
        cliReqTableName = cliReqtableNamep;
        filteredTableName = filteredTableNamep;
        searchesTableName = searchesTableNamep;
        totalWrongLines = totalRightLines = rightLines = searchLines = 0;
        totalFtWrongLines = totalFtRightLines = rightFtLines = 0;
        numValInsert = numValInsertp;
        mode = modep;
        promiscuous = promiscuousp;
        squidLogFile = new File(SquidlogFileNamep);
        cal = Calendar.getInstance();
        sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String todayDateTime = sdf.format(cal.getTime());
        String parsingFileName = "logs/" + squidLogFile.getName().substring(0, squidLogFile.getName().indexOf(".gz")) + "_" + todayDateTime + ".log";
        parsingLog = new PrintWriter(new BufferedWriter(new FileWriter(parsingFileName)));
        filter = filterp;
        refNumber = null;
        dateTime = null;
        month = monthp;
        projectName = null;
        projectDBCode = null;
        nameSpace = null;
        nameSpaceDBCode = null;
        language = null;
        languageDBCode = null;
        spAction = null;
        spActionDBCode = null;
        reqMethod = null;
        reqMethodDBCode = null;
        url = null;
        title = null;
        responseTime = null;
        try {
            md5hash = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SquidLogFileProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a given date and time in the format "YYYYMMDDHHMMSS"
     * @param month the month of the date.
     * @param day the day of the month.
     * @param hour the time.
     * @return the result of converting the date and the time into the
     * "YYYYMMDDHHMMSS" format
     *
     */
    public static String parseDate(String month, int day, String hour) {
        String sqlDate = "" + Calendar.getInstance().get(Calendar.YEAR);
        int pos = "JANFEBMARAPRMAYJUNJULAUGSEPOCTNOVDEC".indexOf(month.toUpperCase()) / 3 + 1;
        sqlDate += pos < 10 ? "0" + pos : pos;
        sqlDate += day < 10 ? "0" + day : "" + day;
        sqlDate += hour.substring(0, 2);
        sqlDate += hour.substring(3, 5);
        sqlDate += hour.substring(6, 8);
        return sqlDate;
    }

    /**
     * Parses the Wikimedia Foundation project from the URL and lets it in the
     * corresponding attribute.
     */
    public void parseWMProjectFromURL() {
        projectName = "NULL";
        if (Pattern.matches("http://[a-z[A-Z]]{2,3}\\.wiktionary.org/.*", url)) {
            projectName = "wiktionary";
        } else if (url.indexOf("http://species.wiki") != -1) {
            projectName = "species";
        } else if (url.startsWith("http://commons")) {
            projectName = "commons";
        } else if (url.startsWith("http://upload.wikimedia.org")) {
            projectName = "upload";
        } else if (url.startsWith("http://meta.wiki")) {
            projectName = "meta";
        } else if (url.startsWith("http://wikimediafoundation.org/") || url.startsWith("http://www.wikimedia.org/") || url.startsWith("http://wikimedia.org/")) {
            projectName = "wikiMediaFoundation";
        } else if (url.startsWith("http://www.mediawiki.org") || url.startsWith("http://mediawiki.org")) {
            projectName = "mediaWiki";
        } else if (url.startsWith("http://www.wikipedia.") || url.startsWith("http://wikipedia.")) {
            projectName = "wikipediaorg";
        } else if (Pattern.matches("http://[a-z[A-Z]]{2,3}\\.wiki[a-z[A-Z]]*\\.((org)|(com))/.*", url)) {
            projectName = url.substring(url.indexOf('.') + 1, url.indexOf('.', url.indexOf('.') + 1));
        }
        projectName = projectName.toUpperCase();
    }

    /** Parses the type of the url by obtaining the following characters to the
     *  language domain. Basically the method allows to classify URLs requesting
     *  article contents (the "WIKI" string will be returned) and the ones 
     *  requesting any kind of action (the "INDEX" string will be returned)
     *  @return a string of characters describing the type of the URL.
     */
    private String parseURLMainCharac() {
        int i = url.indexOf(".org/"), j = 0;
        String result = "NULL";
        if (url.indexOf("index.php?") != -1) {
            result = "INDEX";
        } else {
            if ((j = url.indexOf('/', i + 5)) == -1) {
                j = url.indexOf('.', i + 5);
            }
            try {
                result = url.substring(i + 5, Math.min(i + 5 + CHWORDLENGTH, j)).toUpperCase();
            } catch (Exception e) {
                return ("NULL");
            }
        }
        return result;
    }

    /**
     * Parses the language from the URL.
     */
    public void parseLanguage() {
        language = "NULL";
        if (projectName.equalsIgnoreCase("Wiktionary") || Pattern.matches("http://[a-z[A-Z]]{2,3}\\.wiki[a-z[A-Z]]*\\.((org)|(com))/.*", url)) {
            language = url.substring(7, 9).toUpperCase();
        }
    }

    /**
     * Parses the namespace from the URL. URLs requesting actions are assigned
     * to the fictitious INDEX namespace.
     * @param url the submitted url.
     */
    public void parseNameSpace(String url) {
        nameSpace = nameSpaceDBCode = "NULL";
        if (url.indexOf("index.php?") != -1) {
            nameSpace = "INDEX";
        } else if (Pattern.matches(".*/wiki/[a-zA-Z_0-9%]*:.*", url)) {
            nameSpace = url.substring(url.indexOf("/wiki/") + 6, url.indexOf(":", url.indexOf("/wiki/") + 5));
        } else if (Pattern.matches(".*/wiki/[a-zA-Z_0-9%]*%3A.*", url)) {
            nameSpace = url.substring(url.indexOf("/wiki/") + 6, url.indexOf("%3A", url.indexOf("/wiki/") + 5));
        } else if (Pattern.matches("http://.*/wiki/([^\\.?/=&:])+", url)) {
            nameSpace = "ARTICLE";
        }
        nameSpace = nameSpace.toUpperCase();
        if (!nameSpace.equals("NULL")) {
            nameSpaceDBCode = filter.filterProjectLangNS(projectName, language, nameSpace);
        }
    }

    /**
     * Parses the title of the article requested by the URL. If the URL
     * specifies an action, the title will refer to the article over which
     * the action is requested. In this case, the title may include a namespace
     * name and it may correpond to a filtered namespace. If this occurs, the
     * previously assigned INDEX namespace will changed to the one included
     * in the URL.
     * @return
     * the string of characters corresponding to the title of the requested
     * article (if any).
     * <br>the <code>"NULL"</code> string if no article is requested.
     */
    public String parseTitle() {
        title = "NULL";
        int i = 0, j = 0;
        if (nameSpaceDBCode.equals(filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "ARTICLE"))) {
            title = url.substring(url.lastIndexOf('/') + 1, url.length());
        } else if ((nameSpaceDBCode.equals(filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "ARTICLE_TALK"))) || (nameSpaceDBCode.equals(filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "USER"))) || (nameSpaceDBCode.equals(filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "USER_TALK")))) {
            if ((i = url.indexOf(':', 5)) != -1) {
                title = url.substring(i + 1, url.length());
            } else if ((i = url.indexOf("%3A")) != -1) {
                title = url.substring(i + 3, url.length());
            } else {
                title = "NOTITLE";
            }
        } else if (nameSpaceDBCode.equals(filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "SPECIAL"))) {
            if ((i = url.indexOf(':', 5)) != -1) {
                if ((j = url.indexOf('/', i + 1)) != -1 || (j = url.indexOf('?', i + 1)) != -1 || (j = url.indexOf('&', i + 1)) != -1) {
                    title = url.substring(i + 1, j);
                    if (title.indexOf("?search=") != -1) {
                        title = "Search";
                    }
                } else {
                    title = url.substring(i + 1, url.length());
                }
            } else if ((i = url.indexOf("%3A")) != -1) {
                if ((j = url.indexOf('/', i + 3)) != -1 || (j = url.indexOf('?', i + 3)) != -1 || (j = url.indexOf('&', i + 3)) != -1) {
                    title = url.substring(i + 3, j);
                    if (title.indexOf("?search=") != -1) {
                        title = "Search";
                    }
                } else {
                    title = url.substring(i + 3, url.length());
                }
            } else {
                title = "NOTITLE";
            }
        } else if (nameSpaceDBCode.equals(filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "INDEX"))) {
            i = -1;
            if ((i = url.indexOf("title=")) != -1) {
                if ((j = url.indexOf('&', i + 6)) != -1) {
                    title = url.substring(i + 6, j);
                }
            }
            if ((i = title.indexOf(':')) != -1) {
                this.parseNameSpace("/wiki/" + title.substring(0, i + 1));
                title = title.substring(i + 1, title.length());
            } else if ((i = title.indexOf("%3A")) != -1) {
                this.parseNameSpace("/wiki/" + title.substring(0, i + 3));
                title = title.substring(i + 3, title.length());
            }
            if (title.indexOf("?search=") != -1) {
                title = "Search";
            }
            if (i != -1) {
                if (nameSpaceDBCode.equals("NULL")) {
                    nameSpaceDBCode = filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "INDEX");
                } else {
                    filter.decreaseProjectLangNS(projectName, language, "INDEX");
                }
            } else {
                nameSpaceDBCode = filter.filterProjectLangNS(projectName, language, "ARTICLE");
                filter.decreaseProjectLangNS(projectName, language, "INDEX");
            }
        }
        title = title.length() > URLLENGTH ? title.substring(0, URLLENGTH) : title;
        return title;
    }

    /**
     * Parses the action from the URL. It may consist on a search operation,
     * on an image or thumbnail request or on another common action. The parsed
     * action will be stored in the corresponding attribute.
     */
    public void parseURLSpecialAction() {
        int i = 0, j = 0;
        spAction = "NULL";
        searched = "NULL";
        if ((i = url.indexOf("search=")) != -1) {
            spAction = "search";
            if ((j = url.indexOf('&', i + 7)) != -1 || (j = url.indexOf('?', i + 7)) != -1 || (j = url.indexOf('/', i + 7)) != -1) {
                searched = url.substring(i + 7, Math.min(i + 7 + SPACTIONLENGTH, j));
            } else {
                searched = url.substring(i + 7, Math.min(i + 7 + SPACTIONLENGTH, url.length()));
            }
        } else if ((i = url.indexOf("action=")) != -1) {
            if ((j = url.indexOf('&', i + 7)) != -1 || (j = url.indexOf('?', i + 7)) != -1 || (j = url.indexOf('/', i + 7)) != -1) {
                spAction = url.substring(i + 7, Math.min(i + 7 + SPACTIONLENGTH, j));
            } else {
                spAction = url.substring(i + 7, Math.min(i + 7 + SPACTIONLENGTH, url.length()));
            }
        } else if (projectName.equals("COMMONS") || projectName.equals("UPLOAD")) {
            String img = "";
            img += url.indexOf("/commons/") != -1 ? "COMMONS" : "";
            img += url.indexOf("/thumb/") != -1 ? "THUMB" : "";
            img = img.length() > 0 ? img : "NULL";
            spAction = img;
        }
        spAction = spAction.toUpperCase();
    }

    /**
     * Builds the SQL insert sentence to insert a set of rows into the table
     * devoted to store the information from the URLs which complain the analysis
     * directives and correspond to search operations. The application uses a
     * multiple insert sentence that tries to store into the table several rows
     * instead of use a single sentence for each insertion operation. In this way,
     * when the number of rows per insert sentence is reached, the sentence is
     * executed.
     */
    public void insertSearch() {
        if (searchLines == 0) {
            searchFtStm = new StringBuilder("INSERT INTO " + searchesTableName + " (" + "f_ref_number, " + "f_date_time, " + "f_lang_id, " + "f_search, " + "f_md5_hash) VALUES \n");
        }
        String dbInsert = "( " + "'" + refNumber + "'," + "'" + dateTime + "', " + "'" + language + "', " + (searched.equals("NULL") ? searched : "'" + searched + "'") + ",";
        String md5h = "NULL";
        if (!searched.equals("NULL")) {
            md5hash.update(searched.getBytes(), 0, searched.length());
            md5h = new BigInteger(1, md5hash.digest()).toString(16);
            while (md5h.length() != 32) {
                md5h = "0" + md5h;
            }
            md5h = "'" + md5h + "'";
        }
        dbInsert += md5h + ")";
        searchFtStm.append(dbInsert);
        searchLines++;
        if (searchLines == 5) {
            searchFtStm.append(';');
            if (mode == SquidLogFileProcessor.SIMUL) {
                System.out.println(searchFtStm.toString());
            } else if (mode == SquidLogFileProcessor.REAL) {
                try {
                    stm.executeUpdate(searchFtStm.toString());
                } catch (SQLException sql) {
                    parsingLog.println("insertSearched: Error Inserting searched row");
                    parsingLog.println(sql.getMessage());
                    parsingLog.println("Sentence: " + searchFtStm);
                } finally {
                    searchLines = 0;
                }
            }
        } else {
            searchFtStm.append(",\n ");
        }
    }

    /**
     * Builds the SQL insert sentence to insert a set of rows into the table
     * devoted to store the information elements from the URLs whose fields
     * fit the analysis directives. The application uses a
     * multiple insert sentence that tries to store into the table several rows
     * instead of use a single sentence for each insertion operation. In this way,
     * when the number of rows per insert sentence is reached, the sentence is
     * executed.
     * @throws SQLException if the execution of the insert sentence fails.
     */
    public void insertFilteredDataRow() throws SQLException {
        if (rightFtLines == 0) {
            insertFtStm = new StringBuilder("INSERT INTO " + filteredTableName + " (" + "f_ref_number, " + "f_date_time, " + "f_wpr_id," + "f_lang_id, " + "f_ns_id, " + "f_title, " + "f_action_id, " + "f_resp_time, " + "f_rm_id," + "f_md5_hash) VALUES \n");
        }
        String dbInsert = "( " + "'" + refNumber + "'," + "'" + dateTime + "', " + (projectDBCode.equals("NULL") ? projectDBCode : "'" + projectDBCode + "'") + ", " + (languageDBCode.equals("NULL") ? languageDBCode : "'" + languageDBCode + "'") + ", " + (nameSpaceDBCode.equals("NULL") ? nameSpaceDBCode : "'" + nameSpaceDBCode + "'") + ", " + "'" + title + "'," + (spActionDBCode.equals("NULL") ? spActionDBCode : "'" + spActionDBCode + "'") + "," + "'" + responseTime + "'," + (reqMethodDBCode.equals("NULL") ? reqMethodDBCode : "'" + reqMethodDBCode + "'") + ", ";
        String md5h = "NULL";
        if (!title.equals("NULL")) {
            md5hash.update(title.getBytes(), 0, title.length());
            md5h = new BigInteger(1, md5hash.digest()).toString(16);
            while (md5h.length() != 32) {
                md5h = "0" + md5h;
            }
            md5h = "'" + md5h + "'";
        }
        dbInsert += md5h + ")";
        rightFtLines++;
        insertFtStm.append(dbInsert);
        if (rightFtLines == numValInsert) {
            insertFtStm.append(';');
            if (mode == SquidLogFileProcessor.SIMUL) {
                System.out.println(insertFtStm.toString());
            } else if (mode == SquidLogFileProcessor.REAL) {
                stm.executeUpdate(insertFtStm.toString());
            }
            totalFtRightLines += rightFtLines;
            rightFtLines = 0;
        } else {
            insertFtStm.append(",\n ");
        }
        if (filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.ACTION, "SEARCH").equals(spActionDBCode)) {
            insertSearch();
        }
    }

    /**
     * Parses each Squid log line to be analysed.
     * @param line the Squid log line.
     * @param mode specifies whether the information elements from the log line
     * will be written to the database or to the standard output.
     */
    public void parseLine(String line, int mode) {
        if (rightLines == 0) {
            insertStm = new StringBuilder("INSERT INTO " + cliReqTableName + "(cr_ref_number, cr_date_time, cr_wpr_id, cr_lang_id," + "cr_content_type, cr_url" + ") VALUES \n");
        }
        String dbInsert = "( ";
        Scanner lineSC = new Scanner(line);
        try {
            lineSC.useDelimiter(Pattern.compile("\\s+"));
            lineSC.next();
            lineSC.next();
            lineSC.next();
            lineSC.next();
            refNumber = lineSC.next();
            dbInsert += refNumber + ", '";
            dateTime = lineSC.next();
            dbInsert += dateTime + "', ";
            try {
                sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
                cal.setTime(sdf.parse(dateTime));
                if (cal.get(Calendar.MONTH) != month - 1) {
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error checking date: " + e.getMessage());
                return;
            }
            url = lineSC.next();
            url = url.replace("\\", "\\\\");
            url = url.replace("\'", "\\\'");
            url = url.replace("\"", "\\\"");
            url = url.length() > 512 ? url.substring(0, 512) : url;
            while (url.endsWith("\\")) {
                url = url.substring(0, url.length() - 1);
                url += '@';
            }
            String save = lineSC.next();
            String content = "NULL";
            if ((url.length() - url.lastIndexOf('.')) >= 4) {
                content = url.substring(url.lastIndexOf('.') + 1, url.lastIndexOf('.') + 1 + 3);
            }
            if (content.indexOf("\'") != -1 || content.indexOf("\\") != -1 || content.toUpperCase().equals("ORG")) {
                content = "NULL";
            }
            if (!content.equals("NULL")) {
                content = "'" + content + "'";
            }
            responseTime = "NULL";
            if (lineSC.hasNext()) {
                responseTime = lineSC.next();
            }
            reqMethod = "NULL";
            if (lineSC.hasNext()) {
                reqMethod = lineSC.next();
            }
            projectDBCode = "NULL";
            languageDBCode = "NULL";
            nameSpaceDBCode = "NULL";
            spActionDBCode = "NULL";
            reqMethodDBCode = "NULL";
            parseWMProjectFromURL();
            projectDBCode = filter.filterProject(projectName);
            dbInsert += projectDBCode.equals("NULL") ? "NULL" : "'" + projectDBCode + "'";
            dbInsert += ",";
            parseLanguage();
            languageDBCode = filter.filterProyectItem(projectName, FiltrableItems.LANG, language);
            dbInsert += languageDBCode.equals("NULL") ? languageDBCode : "'" + languageDBCode + "'";
            dbInsert += ", ";
            if (!projectDBCode.equals("NULL") && !(language.equals("NULL"))) {
                parseNameSpace(url);
                if (save.toUpperCase().equals("SAVE")) {
                    spAction = "SAVE";
                } else if (filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "INDEX").equals(nameSpaceDBCode) || filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "SPECIAL").equals(nameSpaceDBCode)) {
                    parseURLSpecialAction();
                } else {
                    spAction = "NULL";
                }
                spActionDBCode = filter.filterProyectItem(projectName, FiltrableItems.ACTION, spAction);
                reqMethodDBCode = filter.filterProyectItem(projectName, FiltrableItems.METHOD, reqMethod);
                if (((filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "INDEX").equals(nameSpaceDBCode) && !spActionDBCode.equals("NULL")) || (!filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "INDEX").equals(nameSpaceDBCode) && !nameSpaceDBCode.equals("NULL"))) && !reqMethodDBCode.equals("NULL")) {
                    title = parseTitle();
                    if (!filter.getProjectFilteredItemDBCode(projectName, FiltrableItems.NNSS, "INDEX").equals(nameSpaceDBCode)) {
                        try {
                            insertFilteredDataRow();
                        } catch (SQLException sqlf) {
                            totalFtWrongLines += numValInsert;
                            rightFtLines = 0;
                            if (totalFtWrongLines % 1000 == 0) {
                                parsingLog.println("SQLError: Filtered" + sqlf.getMessage());
                                System.err.println("SQLError inserting filtered data row Processing file: " + squidLogFile.getName() + "Table: " + filteredTableName);
                                parsingLog.println(insertFtStm.toString());
                                parsingLog.flush();
                            }
                        }
                    } else {
                        if (!spActionDBCode.equals("NULL")) {
                            filter.decreaseProyectItem(projectName, FiltrableItems.ACTION, spAction);
                        }
                        if (!reqMethodDBCode.equals("NULL")) {
                            filter.decreaseProyectItem(projectName, FiltrableItems.METHOD, reqMethod);
                        }
                    }
                } else {
                    if (!spActionDBCode.equals("NULL")) {
                        filter.decreaseProyectItem(projectName, FiltrableItems.ACTION, spAction);
                    }
                    if (!reqMethodDBCode.equals("NULL")) {
                        filter.decreaseProyectItem(projectName, FiltrableItems.METHOD, reqMethod);
                    }
                }
            }
            dbInsert += content + ",";
            dbInsert += "'" + url + "' ";
            dbInsert += ") ";
            insertStm.append(dbInsert);
            rightLines++;
            if (rightLines == numValInsert) {
                insertStm.append(';');
                if (mode == SquidLogFileProcessor.SIMUL) {
                    System.out.println(insertStm.toString());
                } else if (promiscuous) {
                    stm.executeUpdate(insertStm.toString());
                }
                totalRightLines += rightLines;
                rightLines = 0;
            } else {
                insertStm.append(",\n ");
            }
        } catch (SQLException sql) {
            totalWrongLines += numValInsert;
            rightLines = 0;
            if (totalWrongLines % 1000 == 0) {
                parsingLog.println("SQLError: " + sql.getMessage());
                parsingLog.println(insertStm.toString());
                System.err.println("SQLError inserting in promiscous mode Processing file: " + squidLogFile.getName() + "Table: " + cliReqTableName);
                parsingLog.flush();
            }
        } catch (Exception e) {
            totalWrongLines++;
            if (totalWrongLines % 1000 == 0) {
                parsingLog.println("Line Error: " + e.getMessage());
                parsingLog.println("Line: " + line);
            }
        }
    }

    /**
     * The method called when the thread object is started.
     */
    public void run() {
        boolean seguir = true;
        long k = 0;
        Calendar cal0 = Calendar.getInstance(), cal1;
        parsingLog.println("Starting parsing file: " + squidLogFile.getName() + " at: " + cal0.getTime());
        while (squidLogFileSC.hasNextLine()) {
            String line = squidLogFileSC.nextLine();
            if (k % 50000 == 0) {
                cal1 = Calendar.getInstance();
                parsingLog.println("Processing line " + k + " at: " + cal1.getTime());
                parsingLog.flush();
            }
            parseLine(line, mode);
            k++;
        }
        try {
            if (rightLines != 0) {
                insertStm.setCharAt(insertStm.length() - 3, ';');
                if (mode == SquidLogFileProcessor.SIMUL) {
                    parsingLog.println(insertStm.toString());
                } else if (promiscuous) {
                    stm.executeUpdate(insertStm.toString());
                }
                totalRightLines += rightLines;
            }
        } catch (SQLException sql) {
            totalWrongLines += rightLines;
            parsingLog.println("Main: SQLError in final lines: " + sql.getMessage());
            parsingLog.println("Main: SQLError in final lines: " + insertStm.toString());
            System.err.println("Main: SQLError inserting in promiscous mode (final lines) Processing file: " + (squidLogFile == null ? "FILE NOT FOUND" : squidLogFile.getName()) + "Table: " + cliReqTableName);
        }
        try {
            if (rightFtLines != 0) {
                insertFtStm.setCharAt(insertFtStm.length() - 3, ';');
                if (mode == SquidLogFileProcessor.SIMUL) {
                    parsingLog.println(insertFtStm.toString());
                } else if (mode == SquidLogFileProcessor.REAL) {
                    stm.executeUpdate(insertFtStm.toString());
                }
                totalFtRightLines += rightFtLines;
            }
        } catch (SQLException sql) {
            totalFtWrongLines += rightFtLines;
            parsingLog.println("Main: SQLError in final lines: " + insertFtStm.toString());
            parsingLog.println("Main: SQLError in final lines: " + sql.getMessage());
            System.err.println("Main: SQLError filtered rows (final lines) Processing file: " + (squidLogFile == null ? "FILE NOT FOUND" : squidLogFile.getName()) + "Table: " + filteredTableName);
        }
        try {
            if (searchLines != 0) {
                searchFtStm.setCharAt(searchFtStm.length() - 3, ';');
                if (mode == SquidLogFileProcessor.SIMUL) {
                    parsingLog.println(searchFtStm.toString());
                } else if (mode == SquidLogFileProcessor.REAL) {
                    stm.executeUpdate(searchFtStm.toString());
                }
            }
        } catch (SQLException sql) {
            parsingLog.println("SQLError in final lines: " + sql.getMessage());
            parsingLog.println("SQLError in final lines: " + searchFtStm.toString());
            System.err.println("SQLError search lines (final lines) Processing file: " + (squidLogFile == null ? "FILE NOT FOUND" : squidLogFile.getName()) + "Table: " + filteredTableName);
        }
        parsingLog.println("Total processed log lines: " + totalRightLines);
        parsingLog.println("Total failed to process log lines: " + totalWrongLines);
        parsingLog.println("Total inserted articles: " + totalFtRightLines);
        parsingLog.println("Total failed to insert articles: " + totalFtWrongLines);
        Calendar cal2 = Calendar.getInstance();
        parsingLog.println("Finishing Parsing File at: " + cal2.getTime());
        Calendar cal3 = Calendar.getInstance();
        cal3.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal3.setTimeInMillis(cal2.getTimeInMillis() - cal0.getTimeInMillis());
        parsingLog.println("Total Parsing Time: " + (cal3.get(Calendar.DAY_OF_MONTH) - 1) + " days " + cal3.get(Calendar.HOUR_OF_DAY) + " h. " + cal3.get(Calendar.MINUTE) + " min. " + cal3.get(Calendar.SECOND) + " sec. ");
        parsingLog.close();
    }
}
