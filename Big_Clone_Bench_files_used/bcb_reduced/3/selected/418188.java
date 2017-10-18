package de.consolewars.api;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import de.consolewars.api.data.AuthStatus;
import de.consolewars.api.data.AuthenticatedUser;
import de.consolewars.api.data.Blog;
import de.consolewars.api.data.Comment;
import de.consolewars.api.data.Message;
import de.consolewars.api.data.News;
import de.consolewars.api.data.IUnixtime;
import de.consolewars.api.event.BlogUpdateListener;
import de.consolewars.api.event.ListEventObject;
import de.consolewars.api.event.NewsUpdateListener;
import de.consolewars.api.exception.ConsolewarsAPIException;
import de.consolewars.api.parser.SAXAuthStatusParser;
import de.consolewars.api.parser.SAXAuthenticationParser;
import de.consolewars.api.parser.SAXBlogParser;
import de.consolewars.api.parser.SAXCommentParser;
import de.consolewars.api.parser.SAXMessageParser;
import de.consolewars.api.parser.SAXNewsParser;
import de.consolewars.api.util.DateUtil;
import de.consolewars.api.util.URLCreator;

/**
 * java library for API calls on the website http://www.consolewars.de
 * 
 * @author cerpin (arrewk@gmail.com)
 */
public class API {

    private final String BASEURL = "http://www.consolewars.de/api/";

    public static final int BLANK_ARGUMENT = -1;

    private String APIKey;

    private CheckBlogThread bloglistUpdateThread;

    private CheckNewsThread newslistUpdateThread;

    private Thread[] checkUpdateThreads = { bloglistUpdateThread, newslistUpdateThread };

    long minInterval = 1 * DateUtil.MINUTE;

    private ArrayList listeners = new ArrayList();

    public final boolean DEBUG = false;

    public API(String APIKey) {
        this.APIKey = APIKey;
    }

    /**
	 * checking if apikey is valid or not
	 * OK - when apikey is valid
	 * FAILED - when apikey is invalid
	 * INACTIVE when apikey is banned/inactive
	 * 
	 * @return authstatus object
	 */
    public AuthStatus checkAPIToken() throws ConsolewarsAPIException {
        String apiname = "checkapitoken";
        URLCreator APIKeyCheckURL = new URLCreator(BASEURL + apiname + ".php");
        APIKeyCheckURL.addArgument("apitoken", APIKey);
        SAXAuthStatusParser parser = new SAXAuthStatusParser(APIKeyCheckURL.toString());
        parser.parseDocument();
        AuthStatus status = parser.getAuthStatus();
        parser = null;
        return status;
    }

    /**
	 * authentication for e.g. checking private messages
	 * 
	 * @param username username to auth
	 * @param password plain-text password
	 * @return password hash for further use e.g. getMessages 
	 */
    public AuthenticatedUser authenticate(String username, String password) throws ConsolewarsAPIException {
        String apiname = "authenticate";
        URLCreator authenticationURL = new URLCreator(BASEURL + apiname + ".php");
        authenticationURL.addArgument("apitoken", APIKey);
        authenticationURL.addArgument("username", username);
        authenticationURL.addArgument("password", hashPassword(password));
        SAXAuthenticationParser parser = new SAXAuthenticationParser(authenticationURL.toString());
        ArrayList<AuthenticatedUser> authedUser = parser.parseDocument();
        parser = null;
        return authedUser.get(0);
    }

    /**
	 * 
	 * @param uid user-id if you wanna get blogs of one certain user only, with UID_ANY you get recent blogs of all users
	 * @param count number of blogs to retrieve
	 * @param filter what kind of blogs should be returned: 0 normal, 1 newsblogs, 2 other
	 * @return list of blogs
	 */
    public ArrayList<Blog> getBlogsList(int uid, int count, int filter) throws ConsolewarsAPIException {
        ArrayList<Blog> blogs = new ArrayList<Blog>();
        String apiname = "getblogslist";
        URLCreator blogListURL = new URLCreator(BASEURL + apiname + ".php");
        blogListURL.addArgument("apitoken", APIKey);
        if (uid != BLANK_ARGUMENT) {
            blogListURL.addArgument("uid", uid);
        }
        blogListURL.addArgument("count", count);
        if (filter != BLANK_ARGUMENT) blogListURL.addArgument("filter", filter);
        SAXBlogParser parser = new SAXBlogParser(blogListURL.toString());
        blogs = parser.parseDocument();
        parser = null;
        return blogs;
    }

    /**
	 * 
	 * @param uid user id (only if you want blogs of this user only)
	 * @param count maximum number of blogs to return
	 * @param filter see the other getBlogsList
	 * @param since returns only blogs since this date/time
	 * @return list of blogs which are not older than <i>since</i>
	 */
    @SuppressWarnings("unchecked")
    public ArrayList<Blog> getBlogsList(int uid, int count, int filter, Date since) throws ConsolewarsAPIException {
        if (since == null) return getBlogsList(uid, count, filter); else {
            return (ArrayList<Blog>) filterListByDate(getBlogsList(uid, count, filter), since);
        }
    }

    /**
	 * get one blog
	 * 
	 * @param id blog ide
	 * @return one single blog
	 */
    public Blog getBlog(int id) throws ConsolewarsAPIException {
        ArrayList<Blog> blogs = this.getBlogs(new int[] { id });
        return blogs.get(0);
    }

    /**
	 * get several blogs
	 * 
	 * @param id ids of the blogs
	 * @return several blogs as requested by their ids
	 */
    public ArrayList<Blog> getBlogs(int[] id) throws ConsolewarsAPIException {
        ArrayList<Blog> blogs = new ArrayList<Blog>();
        String apiname = "getblogs";
        URLCreator blogsURL = new URLCreator(BASEURL + apiname + ".php");
        blogsURL.addArgument("apitoken", APIKey);
        blogsURL.addArgument("id", id);
        SAXBlogParser parser = new SAXBlogParser(blogsURL.toString());
        blogs = parser.parseDocument();
        parser = null;
        return blogs;
    }

    /**
	 * get comments of news or userblogs
	 *  
	 * @param id
	 * @param area
	 * @param count
	 * @param talkback_viewpage
	 * @param talkback_lastpage
	 * @return list of comments
	 */
    public ArrayList<Comment> getComments(int id, int area, int count, int talkback_viewpage, int talkback_lastpage) throws ConsolewarsAPIException {
        ArrayList<Comment> comments = new ArrayList<Comment>();
        String apiname = "getcomments";
        URLCreator commentsURL = new URLCreator(BASEURL + apiname + ".php");
        commentsURL.addArgument("apitoken", APIKey);
        commentsURL.addArgument("id", id);
        commentsURL.addArgument("area", area);
        commentsURL.addArgument("count", count);
        commentsURL.addArgument("talkback_viewpage", talkback_viewpage);
        if (talkback_lastpage != BLANK_ARGUMENT) {
            commentsURL.addArgument("talkback_lastpage", talkback_lastpage);
        }
        SAXCommentParser parser = new SAXCommentParser(commentsURL.toString());
        comments = parser.parseDocument();
        parser = null;
        return comments;
    }

    /**
	 * @param uid user-id of the 
	 * 
	 * @return the requested private messages
	 */
    public ArrayList<Message> getMessages(int uid, String pass, int folder, int count) throws ConsolewarsAPIException {
        ArrayList<Message> msgs = new ArrayList<Message>();
        String apiname = "getmessages";
        URLCreator commentsURL = new URLCreator(BASEURL + apiname + ".php");
        commentsURL.addArgument("apitoken", APIKey);
        commentsURL.addArgument("user", uid);
        commentsURL.addArgument("pass", pass);
        commentsURL.addArgument("folder", folder);
        commentsURL.addArgument("count", count);
        SAXMessageParser parser = new SAXMessageParser(commentsURL.toString());
        msgs = parser.parseDocument();
        parser = null;
        return msgs;
    }

    public ArrayList<News> getNewsList(int count, int filter) throws ConsolewarsAPIException {
        ArrayList<News> news = new ArrayList<News>();
        String apiname = "getnewslist";
        URLCreator newslistURL = new URLCreator(BASEURL + apiname + ".php");
        newslistURL.addArgument("apitoken", APIKey);
        newslistURL.addArgument("count", count);
        if (filter != BLANK_ARGUMENT) newslistURL.addArgument("filter", filter);
        SAXNewsParser parser = new SAXNewsParser(newslistURL.toString());
        news = parser.parseDocument();
        parser = null;
        return removeNewsTeaser(news);
    }

    public ArrayList<News> getNewsList(int count, int filter, Date since) throws ConsolewarsAPIException {
        if (since == null) return getNewsList(count, filter); else {
            return (ArrayList<News>) filterListByDate(getNewsList(count, filter), since);
        }
    }

    /**
	 * get news by id
	 * 
	 * @param id
	 * @return a list of news
	 */
    public ArrayList<News> getNews(int[] id) throws ConsolewarsAPIException {
        ArrayList<News> news = new ArrayList<News>();
        String apiname = "getnews";
        URLCreator newslistURL = new URLCreator(BASEURL + apiname + ".php");
        newslistURL.addArgument("apitoken", APIKey);
        newslistURL.addArgument("id", id);
        SAXNewsParser parser = new SAXNewsParser(newslistURL.toString());
        news = parser.parseDocument();
        parser = null;
        return news;
    }

    /**
	 * get a single news
	 * 
	 * @param id news-id
	 * @return a single news
	 */
    public News getNews(int id) throws ConsolewarsAPIException {
        ArrayList<News> news = getNews(new int[] { id });
        return news.get(0);
    }

    /**
	 * get event messages when new blogs has been received
	 * 
	 * @param l update listener
	 */
    public void addBlogUpdateListener(BlogUpdateListener l) {
        listeners.add(l);
        if (bloglistUpdateThread == null) {
            bloglistUpdateThread = new CheckBlogThread(minInterval);
            bloglistUpdateThread.start();
        }
    }

    /**
	 * get event messages when new news entries has been received
	 * 
	 * @param l update listener
	 */
    public void addNewsUpdateListener(NewsUpdateListener l) {
        listeners.add(l);
        if (newslistUpdateThread == null) {
            newslistUpdateThread = new CheckNewsThread(minInterval);
            newslistUpdateThread.start();
        }
    }

    /**
	 * relevant only when using {@link BlogUpdateListener} 
	 * set the check frequency for new blogs
	 * 
	 * @param minutes
	 */
    public void setCheckBlogUpdatesInterval(int minutes) {
        setCheckBlogUpdatesInterval(minutes, 0);
    }

    public void setCheckBlogUpdatesInterval(int minutes, int seconds) {
        if (DEBUG) System.out.println("changing interval to " + minutes + " minutes and " + seconds + " seconds");
        bloglistUpdateThread.interrupt();
        long interval = Math.max(minutes * DateUtil.MINUTE + seconds * DateUtil.SECOND, minInterval);
        bloglistUpdateThread.setInterval(interval);
        bloglistUpdateThread.notify();
    }

    public void setCheckNewsUpdatesInterval(int minutes) {
        setCheckNewsUpdatesInterval(minutes, 0);
    }

    public void setCheckNewsUpdatesInterval(int minutes, int seconds) {
        if (DEBUG) System.out.println("changing interval to " + minutes + " minutes and " + seconds + " seconds");
        long interval = Math.max(minutes * DateUtil.MINUTE + seconds * DateUtil.SECOND, minInterval);
        newslistUpdateThread.setInterval(interval);
        newslistUpdateThread.interrupt();
    }

    /**
	 * removing the newsteaser to get the normal news only
	 * 
	 * @param news
	 * @return
	 */
    private ArrayList<News> removeNewsTeaser(ArrayList<News> news) {
        if (news.size() > 0) {
            if (news.get(0).getMode().equals("TEASER")) {
                news.remove(0);
                return news;
            }
        }
        for (int i = news.size() - 1; i >= 0; i--) {
            if (news.get(i).getMode().equals("TEASER")) news.remove(i);
        }
        return news;
    }

    /**
	 * creates a md5-hashed password
	 * 
	 * @param password plaintext password
	 * @return md5-hashed password
	 */
    private String hashPassword(String password) {
        String passwordHash = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            passwordHash = hash.toString(16);
            if (passwordHash.length() == 31) {
                passwordHash = "0" + passwordHash;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return passwordHash;
    }

    /**
	 * filter list by date
	 * 
	 * @param list original list
	 * @param since list items should not be older than this time
	 * @return filtered list by date
	 */
    private ArrayList<? extends IUnixtime> filterListByDate(ArrayList<? extends IUnixtime> list, Date since) {
        ArrayList<IUnixtime> filteredList = new ArrayList<IUnixtime>();
        for (IUnixtime item : list) {
            long sinceTime = since.getTime() / DateUtil.SECOND;
            long itemTime = item.getUnixtime();
            if (itemTime >= sinceTime) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }

    private void fireBlogReceivedEvent(ListEventObject<Blog> event) {
        for (Object listener : listeners) {
            if (listener instanceof BlogUpdateListener) {
                ((BlogUpdateListener) listener).blogsReceived(event);
            }
        }
    }

    private void fireNewsReceivedEvent(ListEventObject<News> event) {
        for (Object listener : listeners) {
            if (listener instanceof NewsUpdateListener) {
                ((NewsUpdateListener) listener).newsReceived(event);
            }
        }
    }

    private abstract class APIThread<T> extends Thread {

        private long checkInterval;

        private int itemCount = 10;

        private Date lastUpdate = null;

        private ArrayList<T> lastItemUpdate = null;

        public APIThread(long checkInterval) {
            this.checkInterval = checkInterval;
        }

        protected void setInterval(long interval) {
            this.checkInterval = interval;
        }

        protected Date getLastUpdate() {
            return lastUpdate;
        }

        protected int getItemCount() {
            return itemCount;
        }

        /**
		 * comparing the lists of the last two updates and removing
		 * double entries
		 * 
		 * @throws ConsolewarsAPIException 
		 */
        protected ArrayList<T> checkUpdateByComparing() throws ConsolewarsAPIException {
            ArrayList<T> currentItemUpdate = getItemList();
            ArrayList<T> cleanItemUpdate = new ArrayList<T>();
            if (lastItemUpdate == null) {
                lastItemUpdate = currentItemUpdate;
                return currentItemUpdate;
            }
            for (T currentUpdateItem : currentItemUpdate) {
                if (!contains(lastItemUpdate, currentUpdateItem)) {
                    cleanItemUpdate.add(currentUpdateItem);
                }
            }
            lastItemUpdate = currentItemUpdate;
            return cleanItemUpdate;
        }

        protected abstract boolean contains(ArrayList<T> a, T item);

        protected abstract void checkUpdate() throws ConsolewarsAPIException;

        protected abstract ArrayList<T> getItemList() throws ConsolewarsAPIException;

        @Override
        public void run() {
            while (true) {
                try {
                    checkUpdate();
                    lastUpdate = new Date();
                    sleep(checkInterval);
                } catch (InterruptedException e) {
                    System.out.println("Wartevorgang vor dem nächsten Update abgebrochen.");
                } catch (ConsolewarsAPIException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class CheckBlogThread extends APIThread<Blog> {

        public CheckBlogThread(long checkInterval) {
            super(checkInterval);
        }

        /**
		 * checking if there are new news entries since the last update
		 * Note: since consolewars allows to set the time by yourself this method is not safe
		 * to receive ALL blogs therefore implemented but not used anywhere
		 * 
		 */
        private ArrayList<Blog> checkUpdateByDate() throws ConsolewarsAPIException {
            return getBlogsList(BLANK_ARGUMENT, getItemCount(), BLANK_ARGUMENT, getLastUpdate());
        }

        @Override
        protected void checkUpdate() throws ConsolewarsAPIException {
            if (DEBUG) System.out.println(new Date() + ": checking for new blogs");
            ArrayList<Blog> blogs = checkUpdateByComparing();
            if (blogs.size() > 0) fireBlogReceivedEvent(new ListEventObject<Blog>(this, blogs));
        }

        @Override
        protected ArrayList<Blog> getItemList() throws ConsolewarsAPIException {
            return getBlogsList(BLANK_ARGUMENT, getItemCount(), BLANK_ARGUMENT);
        }

        @Override
        protected boolean contains(ArrayList<Blog> a, Blog item) {
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i).equals(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class CheckNewsThread extends APIThread<News> {

        public CheckNewsThread(long checkInterval) {
            super(checkInterval);
        }

        /**
		 * checking if there are new news entries since the last update
		 * Note: since consolewars allows to set the time by yourself this method is not safe
		 * to receive ALL news therefore implemented but not used anywhere
		 * 
		 * @throws ConsolewarsAPIException 
		 */
        private void checkUpdateByDate() throws ConsolewarsAPIException {
            ArrayList<News> news = getNewsList(getItemCount(), BLANK_ARGUMENT, getLastUpdate());
            if (news.size() > 0) fireNewsReceivedEvent(new ListEventObject<News>(this, news));
        }

        @Override
        protected void checkUpdate() throws ConsolewarsAPIException {
            if (DEBUG) System.out.println(new Date() + ": checking for news");
            ArrayList<News> news = checkUpdateByComparing();
            if (news.size() > 0) fireNewsReceivedEvent(new ListEventObject<News>(this, news));
        }

        @Override
        protected ArrayList<News> getItemList() throws ConsolewarsAPIException {
            return getNewsList(getItemCount(), BLANK_ARGUMENT);
        }

        @Override
        protected boolean contains(ArrayList<News> a, News item) {
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i).equals(item)) {
                    return true;
                }
            }
            return false;
        }
    }
}
