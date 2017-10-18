package grouprecommendations.Database;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import grouprecommendations.model.FeedSource;
import grouprecommendations.model.Link;
import grouprecommendations.model.LinkSource;
import grouprecommendations.model.ServicePublisher;
import grouprecommendations.model.UserRatings;
import grouprecommendations.model.WebPage;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import constants.IDatabaseConstants;
import database.DBAccessPool;
import grouprecommendations.util.IConstants;
import grouprecommendations.Twitter.TwitterFunctions;
import grouprecommendations.experiment.SystemConfigurator;

/**
 *
 * @author harsha
 */
public class DBFunctions {

    private static DBAccessPool dbAccess = new DBAccessPool(IConstants.DB_IP_ADDRESS, IConstants.DB_SCHEMA, "", IConstants.DB_USERNAME, IConstants.DB_PASSWORD, IConstants.DB_DRIVER, IConstants.DB_SERVER);

    private static Logger logger = Logger.getLogger(DBFunctions.class.getName());

    public DBFunctions() {
        logger.setLevel(Level.WARN);
        logger.info("logging initialized");
    }

    public static DBFunctions getDBFunctions() {
        if (ref == null) ref = new DBFunctions();
        return ref;
    }

    private static DBFunctions ref;

    public DBAccessPool getDBAccess() {
        return this.dbAccess;
    }

    /**
     * The method name says for itself.
     * @param publishingStatus
     * @return
     */
    public Set<String> getWebPageTitleByPublishingStatusAndNotRetweeted(int publishingStatus) {
        Set<String> pageTitles = new HashSet<String>();
        String sql = "select pageTitle from webpage where published=" + publishingStatus + " and docID not in (select retweet_id from retweets)";
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        try {
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                pageTitles.add(rs.getString("pageTitle"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            dbAccess.closeConnection(ps);
        }
        return pageTitles;
    }

    /**
     * The method name says for itself.
     * @param publishingStatus
     * @return
     */
    public Set<String> getWebPageTitleByPublishingStatus(int publishingStatus) {
        Set<String> pageTitles = new HashSet<String>();
        String sql = "select the pageTitle from webpage where published=" + publishingStatus;
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        try {
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                pageTitles.add(rs.getString("pageTitle"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            dbAccess.closeConnection(ps);
        }
        return pageTitles;
    }

    /**
     * @param url
     * @return
     */
    public WebPage getWebPageByURL(String url) {
        return new WebPage(this.getWebPageId(url));
    }

    /**
     * @param userName
     * @return
     */
    public long getTwitterIdByTwitterName(String userName) {
        long twitter_id = 0;
        String sql = "select twitter_id from twitter_followers where twitter_name = '" + userName + "'";
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        try {
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                twitter_id = rs.getLong("twitter_id");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            dbAccess.closeConnection(ps);
        }
        return twitter_id;
    }

    /**
     * @param twitterName
     * @param userId
     * @param tweetId
     * @param linkId
     * @param rate
     * @param comment
     * @return
     */
    public boolean saveUserEvaluationRating(String twitterName, String linkId, String rate, String comment) {
        long twitterId = getTwitterIdByTwitterName(twitterName);
        if (twitterId > 0) {
            String sql = "insert into users_link_rate(twitter_id, linkId, rate, comments) " + "   values (" + twitterId + ",'" + linkId + "','" + rate + "','" + comment + "')";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            dbAccess.ExecUpdate(ps);
            return true;
        } else {
            System.out.println("Twitter name " + twitterName + " does not exist!");
            return false;
        }
    }

    /**
	 * It calculates the group strategy rate score
	 */
    public boolean isGroupStrategyRateScoreCalculated() {
        long count = 0;
        String sql = "select count(*) from group_strategy_rate";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        try {
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                count = rs.getLong("count(*)");
            }
            if (count != 0) {
                return true;
            }
        } catch (SQLException ex) {
            logger.info(ex);
        } finally {
            dbAccess.closeConnection(ps);
        }
        return false;
    }

    /**
     * Saves the groups strategy rate score for a given method
     * @param linkId
     * @param rate
     * @param comment
     * @return
     */
    public boolean saveGroupStrategyRateScore(Long linkId, Double finalRate, String method) {
        String sql = "insert into group_strategy_rate(linkId, final_rate, method) " + "   values (" + linkId + "," + finalRate + ",'" + method + "')";
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        int result = dbAccess.ExecUpdate(ps);
        return result > 0;
    }

    /**
     * @return
     */
    public boolean deleteGroupStrategyRateScore() {
        String sql = "delete from group_strategy_rate";
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        int result = dbAccess.ExecUpdate(ps);
        return result > 0;
    }

    /**
     * Get group strategy
     * @param linkId
     * @param rate
     * @param comment
     * @return
     */
    public Double getGroupStrategyRateScore(Long linkId, String method) {
        Double groupStrategyRateScore = null;
        String sql = "select final_rate from group_strategy_rate where linkId=" + linkId + " and  method='" + method + "'";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        try {
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                groupStrategyRateScore = rs.getDouble("final_rate");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            dbAccess.closeConnection(ps);
        }
        return groupStrategyRateScore;
    }

    /**
     * Get group strategy
     * @param linkId
     * @param rate
     * @param comment
     * @return
     */
    public Double getAverageGroupStrategyRateScore(String method) {
        Double avgGroupStrategyRateScore = null;
        try {
            String sql = "select avg(final_rate) from group_strategy_rate where method='" + method + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                avgGroupStrategyRateScore = rs.getDouble("avg(final_rate)");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return avgGroupStrategyRateScore;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<String> getTwitterNames() {
        Set<String> twitterNames = new HashSet<String>();
        try {
            String sql = "select twitter_name from twitter_followers";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                twitterNames.add(rs.getString("twitter_name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<String>(twitterNames);
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<Link> getMecoDocs() {
        Set<Link> mecoDoc = new HashSet<Link>();
        try {
            String sql = "select * from mecodoc";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                mecoDoc.add(new Link(new Integer(rs.getInt("id")).longValue(), null, null, rs.getString("Title"), rs.getString("Link"), rs.getTimestamp("date"), 0F, 0L));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<Link>(mecoDoc);
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public Timestamp getDateOfMostRecentDocumentImportedFromMecoDB() {
        Timestamp timestamp = null;
        try {
            String sql = "SELECT max(date) as date FROM mecodoc";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                timestamp = (Timestamp) rs.getTimestamp("date");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return timestamp;
    }

    public Timestamp getLastImportFromMecoDB() {
        Timestamp timestamp = null;
        try {
            String sql = "SELECT max(lastImport) as date FROM mecodoc";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                timestamp = (Timestamp) rs.getTimestamp("date");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return timestamp;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public Link getMecoDocById(long mecoDocId) {
        Link mecoDoc = null;
        try {
            String sql = "select * from mecodoc where id = " + new Long(mecoDocId).intValue();
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                mecoDoc = new Link(new Integer(rs.getInt("id")).longValue(), null, null, rs.getString("Title"), rs.getString("link"), rs.getTimestamp("date"), 0F, 0L);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return mecoDoc;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public Link getMecoDocByURL(String url) {
        Link mecoDoc = null;
        try {
            String sql = "select * from mecodoc where link = '" + url + "'";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                mecoDoc = new Link(new Integer(rs.getInt("id")).longValue(), null, null, rs.getString("Title"), rs.getString("link"), rs.getTimestamp("date"), 0F, 0L);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return mecoDoc;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<Long> getTwitterIdsInEvaluation() {
        Set<Long> twitterIds = new HashSet<Long>();
        try {
            String sql = "select distinct(twitter_id) from users_link_rate";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                twitterIds.add(rs.getLong("twitter_id"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<Long>(twitterIds);
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<Long> getTwitterIdsInEvaluationByRound(int round) {
        Set<Long> twitterIds = new HashSet<Long>();
        try {
            String sql = "select distinct(twitter_id) from users_link_rate where round=" + round;
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                twitterIds.add(rs.getLong("twitter_id"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<Long>(twitterIds);
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<UserRatings> getUserRatings() {
        List<UserRatings> userRatingsCol = new ArrayList<UserRatings>();
        Map<String, UserRatings> controlUsersMap = new HashMap<String, UserRatings>();
        try {
            String sql = "select * from users_link_rate";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                Long twitterId = rs.getLong("twitter_id");
                String twitterName = rs.getString("twitter_name");
                Long linkId = rs.getLong("linkId");
                Double rate = rs.getDouble("rate");
                if (!controlUsersMap.keySet().contains(twitterName)) {
                    UserRatings userRatings = new UserRatings(twitterId, twitterName);
                    userRatings.addLinkRatings(linkId.toString(), rate);
                    controlUsersMap.put(twitterName, userRatings);
                    userRatingsCol.add(userRatings);
                } else {
                    UserRatings userRatings = controlUsersMap.get(twitterName);
                    userRatings.addLinkRatings(linkId.toString(), rate);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return userRatingsCol;
    }

    public List<Long> getLinksRatedByRound(int round) {
        Set<Long> linksRated = new HashSet<Long>();
        try {
            String sql = "select distinct(linkId) from users_link_rate where round=" + round;
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                linksRated.add(rs.getLong("linkId"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<Long>(linksRated);
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<Long> getUserUnderEvaluationByRound(int round) {
        Set<Long> userEvalustion = new HashSet<Long>();
        try {
            String sql = "select distinct(twitter_id) from users_link_rate where round=" + round;
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                userEvalustion.add(rs.getLong("twitter_id"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return new ArrayList<Long>(userEvalustion);
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public float getUserRatingAverageByRound(int round) {
        float userRatingAverage = 0;
        try {
            String sql = "select avg(rate) from users_link_rate where round=" + round;
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                userRatingAverage = rs.getFloat("avg(rate)");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return userRatingAverage;
    }

    /**
     * @param round
     * @return
     */
    public List<UserRatings> getUserRatingsByRound(int round) {
        List<UserRatings> userRatingsCol = new ArrayList<UserRatings>();
        Map<String, UserRatings> controlUsersMap = new HashMap<String, UserRatings>();
        try {
            String sql = "select * from users_link_rate where round=" + round;
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                Long twitterId = rs.getLong("twitter_id");
                String twitterName = rs.getString("twitter_name");
                Long linkId = rs.getLong("linkId");
                Double rate = rs.getDouble("rate");
                if (!controlUsersMap.keySet().contains(twitterName)) {
                    UserRatings userRatings = new UserRatings(twitterId, twitterName);
                    userRatings.addLinkRatings(linkId.toString(), rate);
                    controlUsersMap.put(twitterName, userRatings);
                    userRatingsCol.add(userRatings);
                } else {
                    UserRatings userRatings = controlUsersMap.get(twitterName);
                    userRatings.addLinkRatings(linkId.toString(), rate);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return userRatingsCol;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public List<Link> getLinksByPublishingStatusAndSource(int publishedStatus, LinkSource feedSource) {
        List<Link> links = new ArrayList<Link>();
        long feedSourceId = feedSource.getId();
        try {
            String sql = "select l.* " + " from link l, source s " + " where " + "   l.docId_From = s.docId " + "       and s.sourceId = " + feedSourceId + "       and l.published = " + publishedStatus + " order by l.date DESC ";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                WebPage pageTo = this.getWebPage(rs.getLong("docId_To"));
                long id = rs.getLong("linkId");
                float score = rs.getFloat("score");
                long tweetId = rs.getLong("tweet_id");
                String title = rs.getString("title");
                String url = rs.getString("url");
                Date date = rs.getTimestamp("date");
                Link link = new Link(id, feedSource.getFeedPage(), pageTo, title, url, date, score, tweetId);
                links.add(link);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return links;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public void fixLinksByText(String text, Long tweetId) {
        String title = text.substring(0, text.indexOf("- http")).trim();
        String evaluationLink = text.replace(title, "").trim();
        evaluationLink = evaluationLink.replace("-", "").trim();
        String shortenedLink = evaluationLink.substring(evaluationLink.indexOf(" "), evaluationLink.length()).trim();
        System.out.println(shortenedLink);
        System.out.println(tweetId.toString());
        try {
            String sql = "SELECT linkId FROM link where tweet_id != '" + tweetId.toString() + "' and published = true and shortened_url = '" + shortenedLink + "'";
            System.out.println(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                long id = rs.getLong("linkId");
                System.out.println(id);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public boolean existTweetIdInLinkTable(Long tweetId) {
        int amount = 0;
        try {
            String sql = "SELECT count(linkId) FROM link where tweet_id != '" + tweetId.toString() + "'";
            System.out.println(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                amount = rs.getInt("count(linkId)");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return amount > 0;
    }

    /**
     * @param docId
     * @return
     */
    public WebPage getWebPage(long docId) {
        WebPage page = null;
        String pageTitle = "";
        String sql = "select * " + " from webpage p " + " where " + "   p.docId = " + docId;
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        ResultSet rs = dbAccess.ExecQuery(ps);
        try {
            if (rs != null && rs.next()) {
                int id = rs.getInt("docID");
                String url = rs.getString("pageURL");
                pageTitle = rs.getString("pageTitle");
                Date firstAccess = rs.getTimestamp("firstAccess");
                Date lastAccess = rs.getTimestamp("lastAccess");
                page = new WebPage(id, pageTitle, new URL(url), firstAccess, lastAccess);
            }
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(DBFunctions.class.getName()).log(java.util.logging.Level.SEVERE, "URL in database is malformed for page" + pageTitle, ex);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return page;
    }

    public Map<String, Float> getWebPageTermFrequency(long docId) {
        Map<String, Float> tf = new LinkedHashMap<String, Float>();
        String sql = "select word,termFreq from termFrequency where docId = " + docId + " order by word";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        try {
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                tf.put(rs.getString("word"), rs.getFloat("termFreq"));
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return tf;
    }

    /**
     * The method name says for itself.
     * @param publishingStatus
     * @return
     */
    public Set<String> getWebPageTitleRetweeted() {
        Set<String> pageTitles = new HashSet<String>();
        try {
            String sql = "select pageTitle from webpage where docID in (select retweet_id from retweets)";
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                pageTitles.add(rs.getString("pageTitle"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return pageTitles;
    }

    /**
     * This function returns the document ID from the document table that was added into
     * the database. The document ID has to be present in order to calculate the
     * term frequency and document frequency.
     * @param url The url form where the document was downloaded.
     * @return document ID of the webpage, -1 if the id was not found.
     */
    public long getWebPageId(String url) {
        long docID = -1;
        try {
            String sql = "select docID from webpage where pageURL='" + url + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                docID = rs.getLong("docID");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return docID;
    }

    /**
     * @param url
     * @return
     */
    public Timestamp getMecoDateByURL(String url) {
        Timestamp date = null;
        try {
            String sql = "select date from mecodoc where link='" + url + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                date = rs.getTimestamp("date");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return date;
    }

    /**
     * @param pageFromId
     * @param pageToId
     * @return
     */
    public long getLinkId(long pageFromId, long pageToId) {
        long linkID = -1;
        try {
            String sql = "select linkId from link where docId_From=" + pageFromId + " and docId_To = " + pageToId;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                linkID = rs.getLong("linkId");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return linkID;
    }

    /**
     * @param tweetId The status message id given by Twitter
     * @return the link id stored in DB or -1 if not found
     */
    public long getLinkId(long tweetId) {
        long linkID = -1;
        try {
            String sql = "select linkId from link where tweet_id='" + tweetId + "';";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                linkID = rs.getLong("linkId");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return linkID;
    }

    /**
     * This function checks if the word has occured in the same document.
     * term frequency and document frequency.
     * @param docID the document in which the word is being checked.
     * @param word The word being checked.
     * @return true if the word was already found in this document.
     */
    public boolean checkWordDocID(long docID, String word) {
        long count = 0;
        try {
            String sql = "select count(*) from termFrequency where docID=" + docID + " and word='" + word + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                count = rs.getLong("count(*)");
            }
            if (count != 0) {
                return true;
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return false;
    }

    /**
     *  This function checks if the a web page already exist in the database.
     * @param title
     * @param url
     * @return
     */
    public boolean existWebPage(String title, String url) {
        long count = 0;
        try {
            String sql = "select count(*) from webpage where pageTitle = '" + title + "' and pageURL = '" + url + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                count = rs.getLong("count(*)");
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return count > 0;
    }

    public boolean existWebPage(String url) {
        long count = 0;
        try {
            String sql = "select count(*) from webpage where pageURL = '" + url + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                count = rs.getLong("count(*)");
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return count > 0;
    }

    /**
     *  This function selects the ignored links, i.e. those who were posted but not retweeted.
     * @param title
     * @param url
     * @return
     */
    public List<Long> selectIgnoredLinks() {
        List<Long> docIds = new ArrayList<Long>();
        try {
            String sql = "select docId from link where tweet_id not in (select tweet_id from retweets)";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                docIds.add(rs.getLong("docId"));
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return docIds;
    }

    /**
     * Selects all pages tweeted
     * @return
     */
    public List<Long[]> selectTweetedLinks() {
        return selectTweetedLinks(0);
    }

    public List<Long[]> selectTweetedLinks(int limitTweets) {
        List<Long[]> tweetIds = new ArrayList<Long[]>();
        try {
            String sql = "select tweet_id, linkId from link where tweet_id is not null and tweet_id > 0 order by date desc";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            int limit = 0;
            while (rs != null && rs.next() && (limitTweets == 0 || limit++ < limitTweets)) {
                Long[] tweet = new Long[3];
                tweet[0] = rs.getLong("tweet_id");
                tweet[1] = rs.getLong("linkId");
                tweetIds.add(tweet);
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return tweetIds;
    }

    /**
     * @param linkId
     * @param round
     * @return
     */
    public List<Long> getReTweetIdsFromRetweetedLinkIdsAndRound(long linkId, int round) {
        List<Long> reTweetIds = new ArrayList<Long>();
        List<Long> usersUnderEvaluation = getUserUnderEvaluationByRound(round);
        try {
            for (Long tweeterId : usersUnderEvaluation) {
                String sql = "select r.tweet_id from tweets t, retweets r, link l, users_link_rate u where r.docID=l.docID_to and l.linkId = u.linkId  and u.linkId=" + linkId + " and u.round=" + round + " and t.tweet_id = r.tweet_id and t.twitter_id=u.twitter_Id and u.twitter_Id=" + tweeterId;
                PreparedStatement ps = dbAccess.prepareStatement(sql);
                ResultSet rs = dbAccess.ExecQuery(ps);
                while (rs != null && rs.next()) {
                    reTweetIds.add(rs.getLong("tweet_id"));
                }
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return reTweetIds;
    }

    /**
     * @param linkId
     * @param round
     * @return
     */
    public List<Long> getTweetIdsFromRetweetedLinkIdsAndRound(long linkId, int round) {
        List<Long> reTweetIds = new ArrayList<Long>();
        List<Long> usersUnderEvaluation = getUserUnderEvaluationByRound(round);
        try {
            for (Long tweeterId : usersUnderEvaluation) {
                String sql = "select l.tweet_id from tweets t, retweets r, link l, users_link_rate u where r.docID=l.docID_to and l.linkId = u.linkId  and u.linkId=" + linkId + " and u.round=" + round + " and t.tweet_id = r.tweet_id and t.twitter_id=u.twitter_Id and u.twitter_Id=" + tweeterId;
                PreparedStatement ps = dbAccess.prepareStatement(sql);
                ResultSet rs = dbAccess.ExecQuery(ps);
                while (rs != null && rs.next()) {
                    reTweetIds.add(rs.getLong("tweet_id"));
                }
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return reTweetIds;
    }

    public static void main(String[] args) {
        printFullStatisticsByAccountAndRound(ServicePublisher.ROUND_1);
    }

    /**
     * @param linkId
     * @param round
     * @return
     */
    public int getAmountOfFavoriteTweetsByRound(int round) {
        int favoriteAmountByRound = 0;
        Set<Long> tweetIds = new HashSet<Long>();
        Set<String> tweeterIds = new HashSet<String>();
        try {
            String sql = "select distinct(twitter_id) from users_link_rate where round=" + round;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                tweeterIds.add(rs.getString("twitter_id"));
            }
            if (tweeterIds.isEmpty()) {
                throw new Exception("Twitter ids is empty. It must return some values, unless noone had evaluate it. Look the round you trying");
            }
            System.out.println(dbAccess.getDatabaseSchema() + " under evaluation");
            sql = "select distinct(l.tweet_id) from link l, users_link_rate u where u.linkId=l.linkId and u.round=" + round;
            ps = dbAccess.prepareStatement(sql);
            rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                tweetIds.add(rs.getLong("tweet_id"));
            }
            TwitterFunctions twitterFunctions = new TwitterFunctions(this.getDBAccess());
            List<Long> favoriteTweetIds = twitterFunctions.getFavouriteTweetIdsByFollowersIds(new ArrayList<String>(tweeterIds));
            for (Long favoriteTweetId : favoriteTweetIds) {
                if (tweetIds.contains(favoriteTweetId)) {
                    favoriteAmountByRound++;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return favoriteAmountByRound;
    }

    /**
     * @param linkId
     * @param round
     * @return
     */
    public List<Long> getFavoriteTweetsByRound(int round) {
        List<Long> favoriteTweets = new ArrayList<Long>();
        Set<Long> tweetIds = new HashSet<Long>();
        Set<String> tweeterIds = new HashSet<String>();
        try {
            String sql = "select distinct(twitter_id) from users_link_rate where round=" + round;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                tweeterIds.add(rs.getString("twitter_id"));
            }
            if (tweeterIds.isEmpty()) {
                throw new Exception("Twitter ids is empty. It must return some values, unless noone had evaluate it. Look the round you trying");
            }
            System.out.println(dbAccess.getDatabaseSchema() + " under evaluation");
            sql = "select distinct(l.tweet_id) from link l, users_link_rate u where u.linkId=l.linkId and u.round=" + round;
            ps = dbAccess.prepareStatement(sql);
            rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                tweetIds.add(rs.getLong("tweet_id"));
            }
            TwitterFunctions twitterFunctions = new TwitterFunctions(this.getDBAccess());
            List<Long> favoriteTweetIds = twitterFunctions.getFavouriteTweetIdsByFollowersIds(new ArrayList<String>(tweeterIds));
            for (Long favoriteTweetId : favoriteTweetIds) {
                if (tweetIds.contains(favoriteTweetId)) {
                    favoriteTweets.add(favoriteTweetId);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return favoriteTweets;
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public static void printRetweetsByAccountAndRound(int round) {
        Set<String> dbSchemas = getDBSchemaSet();
        Set<String> retweetMessages = new LinkedHashSet<String>();
        for (String dbSchema : dbSchemas) {
            SystemConfigurator.loadConfig(dbSchema);
            int retweetsTotalAccount = 0;
            DBFunctions dbFunctions = new DBFunctions();
            List<Long> linksRated = dbFunctions.getLinksRatedByRound(round);
            for (Long linkRated : linksRated) {
                retweetsTotalAccount = retweetsTotalAccount + dbFunctions.getTweetIdsFromRetweetedLinkIdsAndRound(linkRated, round).size();
            }
            String message = "For TWITTER ACCOUNT " + dbSchema + "	" + retweetsTotalAccount;
            retweetMessages.add(message);
        }
        System.out.println("MAX RETWEETS EXPECTED: 35, i.e. 5 links retweeted by 7 participants");
        for (String retweetMessage : retweetMessages) {
            System.out.println(retweetMessage);
        }
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public static void printIgnoredTweetsByAccountAndRound(int round) {
        Set<String> dbSchemas = getDBSchemaSet();
        Set<String> retweetMessages = new LinkedHashSet<String>();
        for (String dbSchema : dbSchemas) {
            SystemConfigurator.loadConfig(dbSchema);
            DBFunctions dbFunctions = new DBFunctions();
            List<Long> linksRated = dbFunctions.getLinksRatedByRound(round);
            Set<Long> retweetsAndFavorites = new HashSet<Long>();
            for (Long linkRated : linksRated) {
                for (Long tweetIdFromRetweets : dbFunctions.getTweetIdsFromRetweetedLinkIdsAndRound(linkRated, round)) {
                    retweetsAndFavorites.add(tweetIdFromRetweets);
                }
            }
            for (Long favoriteTweets : dbFunctions.getFavoriteTweetsByRound(round)) {
                retweetsAndFavorites.add(favoriteTweets);
            }
            int ignoredMessages = (dbFunctions.getUserUnderEvaluationByRound(round).size() * linksRated.size()) - retweetsAndFavorites.size();
            String message = "For TWITTER ACCOUNT " + dbSchema + ":	" + ignoredMessages;
            retweetMessages.add(message);
        }
        for (String retweetMessage : retweetMessages) {
            System.out.println(retweetMessage);
        }
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public static void printFullStatisticsByAccountAndRound(int round) {
        Set<String> dbSchemas = getDBSchemaSet();
        Set<String> retweetMessages = new LinkedHashSet<String>();
        for (String dbSchema : dbSchemas) {
            int onlyRetweets = 0;
            int onlyFavorite = 0;
            int retweets = 0;
            int favorites = 0;
            List<Long> onlyRetweetsSet = new ArrayList<Long>();
            List<Long> retweetsSet = new ArrayList<Long>();
            List<Long> favoritesSet = new ArrayList<Long>();
            List<Long> onlyFavoritesSet = new ArrayList<Long>();
            int reweetORFavorite = 0;
            int reweetANDFavorite = 0;
            SystemConfigurator.loadConfig(dbSchema);
            DBFunctions dbFunctions = new DBFunctions();
            List<Long> linksRated = dbFunctions.getLinksRatedByRound(round);
            List<Long> retweetsOrFavorites = new ArrayList<Long>();
            for (Long linkRated : linksRated) {
                for (Long tweetIdFromRetweets : dbFunctions.getTweetIdsFromRetweetedLinkIdsAndRound(linkRated, round)) {
                    retweetsOrFavorites.add(tweetIdFromRetweets);
                    onlyRetweetsSet.add(tweetIdFromRetweets);
                    retweetsSet.add(tweetIdFromRetweets);
                }
            }
            retweets = retweetsSet.size();
            for (Long favoriteTweet : dbFunctions.getFavoriteTweetsByRound(round)) {
                retweetsOrFavorites.add(favoriteTweet);
                favoritesSet.add(favoriteTweet);
                if (onlyRetweetsSet.contains(favoriteTweet)) {
                    onlyRetweetsSet.remove(favoriteTweet);
                } else {
                    onlyFavoritesSet.add(favoriteTweet);
                }
            }
            favorites = favoritesSet.size();
            reweetORFavorite = retweetsOrFavorites.size();
            onlyFavorite = onlyFavoritesSet.size();
            onlyRetweets = onlyRetweetsSet.size();
            reweetANDFavorite = (retweetsOrFavorites.size() - (onlyFavorite + onlyRetweets));
            int totalMessages = dbFunctions.getUserUnderEvaluationByRound(round).size() * linksRated.size();
            int ignoredMessages = totalMessages - retweetsOrFavorites.size();
            String message = "For TWITTER ACCOUNT " + dbSchema + "\n Total:" + totalMessages + "\n Ignored Messages:" + ignoredMessages + "\n " + "Retweet:" + retweets + "\n Favorites:" + favorites + "\n " + "Only Retweet:" + onlyRetweets + "\n Only Favorites:" + onlyFavorite + "\n Both Retweets OR Favorites:" + reweetORFavorite + " " + "\n Both Retweets AND Favorites:" + reweetANDFavorite;
            retweetMessages.add(message);
        }
        for (String retweetMessage : retweetMessages) {
            System.out.println(retweetMessage);
        }
    }

    public static void printUserRatingsAverageByAccountAndRound(int round) {
        Set<String> dbSchemas = getDBSchemaSet();
        Set<String> retweetMessages = new LinkedHashSet<String>();
        for (String dbSchema : dbSchemas) {
            SystemConfigurator.loadConfig(dbSchema);
            DBFunctions dbFunctions = new DBFunctions();
            float userRatingAverage = dbFunctions.getUserRatingAverageByRound(round);
            String message = "For TWITTER ACCOUNT " + dbSchema + ":	" + userRatingAverage;
            retweetMessages.add(message);
        }
        for (String retweetMessage : retweetMessages) {
            System.out.println(retweetMessage);
        }
    }

    public static void checkAmountOfEvaluationRatings(int expectedNumberOfRatings, int round) {
        Set<String> dbSchemas = getDBSchemaSet();
        Set<String> setMessages = new LinkedHashSet<String>();
        for (String dbSchema : dbSchemas) {
            SystemConfigurator.loadConfig(dbSchema);
            DBFunctions dbFunctions = new DBFunctions();
            int evaluations = dbFunctions.getUserUnderEvaluationByRound(round).size();
            String message = null;
            if (evaluations == expectedNumberOfRatings) {
                message = "For TWITTER ACCOUNT " + dbSchema + ":	" + (expectedNumberOfRatings);
            } else {
                message = "For TWITTER ACCOUNT " + dbSchema + ": Expected " + (expectedNumberOfRatings) + " but there are " + evaluations;
            }
            setMessages.add(message);
        }
        for (String retweetMessage : setMessages) {
            System.out.println(retweetMessage);
        }
    }

    /**
     * @param publishedStatus
     * @param feedSourceId
     * @return
     */
    public static void printFavoritesByAccountAndRound(int round) {
        Set<String> dbSchemas = getDBSchemaSet();
        Set<String> retweetMessages = new LinkedHashSet<String>();
        int retweetsTotalAccount = 0;
        for (String dbSchema : dbSchemas) {
            SystemConfigurator.loadConfig(dbSchema);
            DBFunctions dbFunctions = new DBFunctions();
            retweetsTotalAccount = dbFunctions.getAmountOfFavoriteTweetsByRound(round);
            String message = "For TWITTER ACCOUNT " + dbSchema + ":	" + retweetsTotalAccount;
            retweetMessages.add(message);
        }
        for (String retweetMessage : retweetMessages) {
            System.out.println(retweetMessage);
        }
    }

    /**
     * @return
     */
    protected static Set<String> getDBSchemaSet() {
        Set<String> dbSchemas = new LinkedHashSet<String>();
        dbSchemas.add(IConstants.DB_SCHEMA_GROUP_MENDER_SINGLE);
        return dbSchemas;
    }

    /**
     *  This function selects the vw_ignored_pages
     * @param title
     * @param url
     * @return
     */
    private Map<String, Float> generateMap(String viewName) {
        Map<String, Float> pages = new LinkedHashMap<String, Float>();
        try {
            String sql = "select word,value from " + viewName + " order by word";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                pages.put(rs.getString("word"), rs.getFloat("value"));
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return pages;
    }

    public void regenerateWebPageTermFrequency() {
        try {
            String sql = "select docId from webpage";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                WebPage page = this.getWebPage(rs.getInt("docId"));
                Source source = new Source(page.getPageUrl());
                source.fullSequentialParse();
                TextExtractor te = source.getTextExtractor();
                String sourceText = te.toString().toLowerCase().replaceAll("[0-9]*", "");
                page.generateTfIdf(sourceText);
                page = null;
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DBFunctions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            logger.info(ex);
        }
    }

    /**
     *  This function selects the pages
     * @param title
     * @param url
     * @return
     */
    public Map<String, Float> getIgnoredPagesMap() {
        return generateMap("vw_ignored_pages");
    }

    public Map<String, Float> getUserPagesMap() {
        return generateMap("vw_user_pages");
    }

    public Map<String, Float> getFavoritePagesMap() {
        return generateMap("vw_favorite_pages");
    }

    /**
     * @param url
     * @return
     */
    public int getFeedSource(String url) {
        int sourceId = 0;
        try {
            String sql = "select sourceId from source where sourceURL = '" + url + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                sourceId = rs.getInt("sourceId");
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return sourceId;
    }

    /**
     * It returns the user with highest retweet. If noone retweet return null.
     * @return
     */
    public Long getHighestRetweeterUnderEvaluation() {
        Long highestRetweeterId = null;
        Long currentRetweeterId = null;
        int highestAmountOfRetweets = 0;
        int currentAmoungOfRetweets = 0;
        try {
            String sql = "SELECT twitter_id FROM users_link_rate";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                currentRetweeterId = new Long(rs.getLong("twitter_id"));
                currentAmoungOfRetweets = this.getRetweetsByUser(highestRetweeterId);
                if (currentAmoungOfRetweets >= highestAmountOfRetweets) {
                    highestRetweeterId = currentRetweeterId;
                }
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return highestRetweeterId;
    }

    /**
     * It returns the users with highest retweets.
     * @return
     */
    public int getRetweetsByUser(Long tweeterId) {
        int retweetsAmount = 0;
        try {
            String sql = "SELECT count(*)  FROM tweets where retweet = 'true' and twitter_id = " + tweeterId;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                retweetsAmount = rs.getInt("count(*)");
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return retweetsAmount;
    }

    /**
     * It returns the users with highest retweets.
     * @return
     */
    public int getRetweetsByDocId(Long docId) {
        int retweetsAmount = 0;
        try {
            String sql = "SELECT count(*) FROM retweets where docID = " + docId;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                retweetsAmount = rs.getInt("count(*)");
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return retweetsAmount;
    }

    /**
     * @param url
     * @return
     */
    public List<FeedSource> getAllFeedSources() {
        List<FeedSource> feedSources = new ArrayList<FeedSource>();
        try {
            String sql = "select * from source";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                FeedSource feedSource = new FeedSource(rs.getString("sourceURL"));
                feedSource.updateLinkList(true);
                feedSources.add(feedSource);
            }
        } catch (SQLException ex) {
            logger.info(ex);
        }
        return feedSources;
    }

    /**
     * Returns the document frequency of the given word.
     * @param word the word for which the document frequency needs to be looked up.
     * @return the document frequency or -1 if the word is not found.
     */
    public double getDF(String word) {
        double docFreq = -1;
        if (word.equals("")) {
            return docFreq;
        }
        try {
            String sql = "select docFreq from documentFrequency where word = '" + word + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                docFreq = rs.getDouble("docFreq");
            }
        } catch (SQLException ex) {
            logger.info(ex);
            return docFreq;
        } finally {
        }
        return docFreq;
    }

    /**
     * Return the count of words that are added into database i.e. the words which
     * were unique to the document.
     * @return
     */
    public int getUniqueWordCount() {
        int uniqueCount = -1;
        try {
            String sql = "select count(word) from documentFrequency";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                uniqueCount = rs.getInt("count(word)");
            }
        } catch (SQLException ex) {
            logger.info(ex);
            return uniqueCount;
        } finally {
        }
        return uniqueCount;
    }

    /**
     * @param rankScore
     */
    public void updateWebPageRankingScore(double rankScore) {
        String sql = "update webpage set rankScore = " + rankScore;
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        logger.info(sql);
        dbAccess.ExecUpdate(ps);
    }

    @Deprecated
    public void updateWebPagePublishing(Link link) {
        String sql = "update webpage set published = " + link.isPublished() + " where docID =" + link.getPageTo().getId();
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    /**
     * @param docID
     * @param word
     * @param termFreq
     */
    public void updateTF(long docID, String word, double termFreq) {
        if (word.equals("")) {
            return;
        }
        String sql = "update termFrequency set termFreq = " + termFreq + " where word = '" + word + "' and docID =" + docID;
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    /**
     * @param word
     * @param docFreq
     */
    public void updateDF(String word, double docFreq) {
        if (word.equals("")) {
            return;
        }
        String sql = "update documentFrequency set docFreq = " + docFreq + " where word = '" + word + "'";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    /**
     * @param score
     * @param linkId
     */
    public void updateLinkScore(float score, long linkId) {
        String sql = "update link set score = " + score + " where linkId = " + linkId;
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    /**
     * @param score
     * @param userScore
     * @param favoritedScore
     * @param ignoredScore
     * @param linkId
     */
    public void updateAllLinkScores(float score, float userScore, float favoritedScore, float ignoredScore, long linkId) {
        try {
            String sql = "insert into link_score_history (linkId, score, userpreference_score, favorited_score, ignored_score, published, date) " + "   select linkid, score, userpreference_score, favorited_score, ignored_score, published, ? " + "     from link " + "     where linkid = ? ";
            PreparedStatement pstmt = dbAccess.prepareStatement(sql);
            pstmt.setLong(2, linkId);
            Date now = new Date();
            pstmt.setTimestamp(1, new Timestamp(now.getTime()));
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(TwitterFunctions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        String sql = "update link set score = " + score + ", " + "   userpreference_score = " + userScore + ", " + "   favorited_score = " + favoritedScore + ", " + "   ignored_score = " + ignoredScore + " where linkId = " + linkId;
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    /**
     * @param word
     */
    public void incrementDF(String word) {
        if (word.equals("")) {
            return;
        }
        double docFreq = getDF(word);
        docFreq++;
        updateDF(word, docFreq);
    }

    /**
     * @param word
     * @return
     */
    public boolean existsWord(String word) {
        int wordCount = 0;
        if (word.equals("")) {
            return false;
        }
        try {
            String sql = "select count(*) from documentFrequency where word = '" + word + "'";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                wordCount = rs.getInt("count(*)");
            }
            if (wordCount != 0) {
                return true;
            }
        } catch (SQLException ex) {
            logger.info(ex);
        } finally {
        }
        return false;
    }

    /**
     * This is the main insert function for inserting a webpage into the database. It also
     * maintains a SHA1 hash of the page data. The SHA1 can be used to very the data change
     * and updated when new data is added by using the MessageDigest.update.
     * Note that the pageTitle and pageSource are escaping the ' and " characters.
     * The same has to be reversed for reading the data.
     * @param pageTitle
     * @param pageURL
     * @param firstAccess
     * @param lastAccess
     * @param pageSource
     * @return
     */
    public int insertWebPage(String pageTitle, String pageURL, Date firstAccess, Date lastAccess, String pageSource, boolean published) {
        int pub = 0;
        int docId = -1;
        if (published) {
            pub = 1;
        }
        java.sql.Timestamp fAccess = new Timestamp(firstAccess.getTime());
        java.sql.Timestamp LAccess = new Timestamp(lastAccess.getTime());
        try {
            MessageDigest hashGenerator = MessageDigest.getInstance("SHA1");
            byte[] theDigest = hashGenerator.digest(pageSource.getBytes("UTF-8"));
            pageTitle = pageTitle.replace("'", "\\'");
            pageTitle = pageTitle.replace("\"", "\\\"");
            pageSource = pageSource.replace("'", "\\'");
            pageSource = pageSource.replace("\"", "\\\"");
            String sql = "insert into webpage(pageTitle, pageURL, firstAccess, lastAccess, pageSource, SHA1, published) values ('" + pageTitle + "','" + pageURL + "','" + fAccess + "','" + LAccess + "','" + pageSource + "','" + theDigest + "'," + pub + " )";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
        } catch (NoSuchAlgorithmException ex) {
            logger.info(ex);
        } catch (UnsupportedEncodingException ex) {
            logger.info(ex);
        } finally {
        }
        return docId;
    }

    /**
     * @param feedUrl
     * @param feedPageId
     * @return
     */
    public int insertFeedSource(String feedUrl, long feedPageId) {
        feedUrl = feedUrl.replace("'", "\\'");
        feedUrl = feedUrl.replace("\"", "\\\"");
        String sql = "insert into source(sourceURL, docID) values ('" + feedUrl + "', " + feedPageId + ")";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        int feedId = dbAccess.ExecUpdate(ps);
        return feedId;
    }

    /**
     * @param docId_From
     * @param docId_To
     * @param title
     * @param url
     * @param published
     * @param date
     * @return
     */
    public int insertLink(long docId_From, long docId_To, String title, String url, int published, Date date) {
        title = title.replace("'", "\\'");
        title = title.replace("\"", "\\\"");
        if (date == null) {
            date = new Date();
        }
        java.sql.Timestamp tDate = new Timestamp(date.getTime());
        String sql = "insert into link(docID_From, docID_To, title, url, published, date) " + "   values (" + docId_From + ", " + docId_To + ", '" + title + "', " + "'" + url + "', " + published + ", '" + tDate + "')";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        int linkId = dbAccess.ExecUpdate(ps);
        return linkId;
    }

    /**
     * @param docId_From
     * @param docId_To
     * @param title
     * @param url
     * @param published
     * @param date
     * @return
     */
    public void insertIntoMecoDocsTable(Link link) {
        if (getMecoDocById(link.getIdKey()) != null) {
            return;
        }
        try {
            link.setLinkUrl(new URL(link.getUrlString().replace("'", "\\'")));
            link.setLinkUrl(new URL(link.getUrlString().replace("\"", "\\\"")));
            link.setLinkTitle(link.getLinkTitle().replace("'", "\\'"));
            link.setLinkTitle(link.getLinkTitle().replace("\"", "\\\""));
            link.setSignalMessage(link.getSignalMessage().replace("'", "\\'"));
            link.setSignalMessage(link.getSignalMessage().replace("\"", "\\\""));
            if (link.getDate() == null) {
                System.out.println("problem no date set");
            }
            String sql = "insert into mecodoc(id, title, link, date,signalMessage) " + "   values (" + link.getIdKey() + ",'" + link.getTitle() + "', " + "'" + link.getUrlString() + "', '" + link.getDate() + "', '" + link.getSignalMessage() + "')";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            dbAccess.ExecUpdate(ps);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void updateLastImportMecoDocsTable(int daysInterval) {
        String sql = null;
        Timestamp dateToUpdate = getLastImportFromMecoDB();
        if (dateToUpdate != null) {
            sql = "update mecodoc set lastImport = DATE_ADD('" + dateToUpdate + "',INTERVAL " + daysInterval + " DAY)";
        } else {
            sql = "update mecodoc set lastImport = DATE_ADD('" + this.getDateOfMostRecentDocumentImportedFromMecoDB() + "',INTERVAL " + daysInterval + "  DAY)";
        }
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    /**
     * Insert link with ranking
     * @param docId_From
     * @param docId_To
     * @param title
     * @param url
     * @param published
     * @param date
     * @param round
     * @return
     */
    public int insertLink(long docId_From, long docId_To, String title, String url, int published, Date date, int round) {
        title = title.replace("'", "\\'");
        title = title.replace("\"", "\\\"");
        if (date == null) {
            date = new Date();
        }
        java.sql.Timestamp tDate = new Timestamp(date.getTime());
        String sql = "insert into link(docID_From, docID_To, title, url, published, date, round) " + "   values (" + docId_From + ", " + docId_To + ", '" + title + "', " + "'" + url + "', " + published + ", '" + tDate + "'," + round + ")";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        int linkId = dbAccess.ExecUpdate(ps);
        return linkId;
    }

    /**
     * @param docID
     * @param word
     * @param termFreq
     * @return
     */
    public boolean insertTF(long docID, String word, double termFreq) {
        if (word.equals("")) {
            return false;
        }
        String sql = "insert into termFrequency values (" + docID + ",'" + word + "'," + termFreq + ")";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
        return true;
    }

    /**
     * @param word
     * @param docFreq
     * @return
     */
    public boolean insertDF(String word, double docFreq) {
        if (word.equals("")) {
            return false;
        }
        String sql = "insert into documentFrequency values ('" + word + "'," + docFreq + ")";
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
        return true;
    }

    public void updatePublishedLink(int published, long tweetId, long linkId) {
        try {
            String sql = "update link set tweet_id = ?, published = ?, date_published = ? where linkId = ?";
            PreparedStatement pstmt = dbAccess.prepareStatement(sql);
            pstmt.setLong(1, tweetId);
            pstmt.setInt(2, published);
            pstmt.setLong(4, linkId);
            Date now = new Date();
            pstmt.setTimestamp(3, new Timestamp(now.getTime()));
            dbAccess.ExecUpdate(pstmt);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(TwitterFunctions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public List<String> getPublishedUrlsShortened() {
        List<String> urls = new ArrayList<String>();
        try {
            String sql = "select shortened_url from link where published = 1 and shortened_url is not null";
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            while (rs != null && rs.next()) {
                urls.add(rs.getString("shortened_url"));
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(DBFunctions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return urls;
    }

    public void updateShortenedUrl(String shortUrl, long linkId) {
        String sql = "update link set shortened_url = '" + shortUrl + "' where linkId = " + linkId;
        logger.info(sql);
        PreparedStatement ps = dbAccess.prepareStatement(sql);
        dbAccess.ExecUpdate(ps);
    }

    public int getLinkNumFavorites(long linkId) {
        int nFav = 0;
        try {
            String sql = "select count(*) as n_fav from favorites where linkId = " + linkId;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                nFav = rs.getInt("n_fav");
            }
        } catch (SQLException ex) {
            logger.info(ex);
            return nFav;
        } finally {
        }
        return nFav;
    }

    public void updateLinkStatistic(Link link) {
        try {
            String sql = "insert into link_statistic_history (linkId, url_clicks, n_favorites, n_retweets, date) " + "   values (?, ?, ?, ?, ?) ";
            PreparedStatement pstmt = dbAccess.prepareStatement(sql);
            pstmt.setLong(1, link.getIdKey());
            pstmt.setLong(2, link.getNumClicks());
            pstmt.setLong(3, link.getNumFavorites());
            pstmt.setLong(4, link.getNumRetweets());
            Date now = new Date();
            pstmt.setTimestamp(5, new Timestamp(now.getTime()));
            dbAccess.ExecUpdate(pstmt);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(TwitterFunctions.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public int getLinkNumClicks(long linkId) {
        int nClicks = 0;
        try {
            String sql = "select url_clicks from link where linkId = " + linkId;
            logger.info(sql);
            PreparedStatement ps = dbAccess.prepareStatement(sql);
            ResultSet rs = dbAccess.ExecQuery(ps);
            if (rs != null && rs.next()) {
                nClicks = rs.getInt("url_clicks");
            }
        } catch (SQLException ex) {
            logger.info(ex);
            return nClicks;
        } finally {
        }
        return nClicks;
    }
}
