package org.connotea;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.logging.LogFactory.getLog;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.restlet.data.Reference;
import org.w3c.dom.Document;

/**
 * Connotea API client facade. This class provides convenient encapsulations of
 * Connotea API methods and is the only part of the client you should have any
 * need to call from your own application code.
 * <p>
 * Note that, at present, this client does not support handling of timeouts or
 * retries that may be required in a future, or wrapper, version in order to
 * cope with the throttling that is implemented on Connotea. Currently, a
 * request that is refused on the basis of throttling will cause a
 * {@link ConnoteaException} to be thrown in which
 * {@link ConnoteaException#getApiMessage()} should return
 * {@link Message#INVALID}.
 * 
 * @author <a href="mailto:christopher.townson@googlemail.com">Christopher
 *         Townson</a>
 */
public class Connotea {

    /**
     * Simple enumeration of Connotea API commands; i.e. "add", "edit", "noop",
     * "remove".
     * 
     * @author <a href="mailto:christopher@christophertownson.com">Christopher
     *         Townson</a>
     */
    private enum Command {

        ADD("/add"), EDIT("/edit"), NOOP("/noop"), REMOVE("/remove");

        private String value;

        private Command(String value) {
            this.value = value;
        }

        /**
         * Returns the command as a string which can be appended to a URI to
         * build the required path.
         * 
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Simple enumeration of Connotea API entities; i.e. "bookmark", "message",
     * "post", "tag".
     * 
     * @author <a href="mailto:christopher@christophertownson.com">Christopher
     *         Townson</a>
     */
    private enum Entity {

        BOOKMARK("/bookmarks"), MESSAGE(""), POST(""), TAG("/tags");

        private String value;

        private Entity(String value) {
            this.value = value;
        }

        /**
         * Returns the entity as a string which can be appended to a URI to
         * build the required path.
         * 
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Simple enumeration of Connotea API fields; i.e. "date", "tag", "uri",
     * "user".
     * 
     * @author <a href="mailto:christopher@christophertownson.com">Christopher
     *         Townson</a>
     */
    private enum Field {

        DATE("/date/"), TAG("/tag/"), URI("/uri/"), USER("/user/");

        private String value;

        private Field(String value) {
            this.value = value;
        }

        /**
         * Returns the field as a string which can be appended to a URI to build
         * the required path.
         * 
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * The base URI for all calls to the Connotea API.
     */
    public static final String BASE_API_URI = "http://www.connotea.org/data";

    /**
     * When instantiating {@link Connotea} with {@link Properties} or
     * {@link Map}, you can use this key to set your Connotea API password.
     */
    public static final String CONNOTEA_PASSWORD_KEY = "connotea.password";

    /**
     * When instantiating {@link Connotea} with {@link Properties} or
     * {@link Map}, you can use this key to set a connection timeout in seconds.
     */
    public static final String CONNOTEA_TIMEOUT_KEY = "connotea.timeout";

    /**
     * When instantiating {@link Connotea} with {@link Properties} or
     * {@link Map}, you can use this key to set your Connotea API username.
     */
    public static final String CONNOTEA_USERNAME_KEY = "connotea.username";

    private static final int DEFAULT_TIMEOUT = 0;

    private static final Log LOG = getLog(Connotea.class);

    private Parser<Document, List<Bookmark>> bookmarkParser;

    private Connector connector;

    private Parser<Document, Message> messageParser;

    private Parser<Document, List<Post>> postParser;

    private Parser<Document, List<Tag>> tagParser;

    /**
     * Instantiate a Connotea API client, passing your Connotea username and
     * password in the provided {@link Map} under the
     * {@link #CONNOTEA_USERNAME_KEY} (
     * <code>{@value #CONNOTEA_USERNAME_KEY}</code>) and
     * {@link #CONNOTEA_PASSWORD_KEY} (
     * <code>{@value #CONNOTEA_PASSWORD_KEY}</code>).
     * 
     * @param map the map containing your Connotea username and password under
     *            the specified keys
     */
    public Connotea(Map<String, String> map) {
        this(map.get(CONNOTEA_USERNAME_KEY), map.get(CONNOTEA_PASSWORD_KEY), Integer.parseInt(map.get(CONNOTEA_TIMEOUT_KEY)));
    }

    /**
     * Instantiate a Connotea API client, passing your Connotea username and
     * password in the provided {@link Properties} under the
     * {@link #CONNOTEA_USERNAME_KEY} (
     * <code>{@value #CONNOTEA_USERNAME_KEY}</code>) and
     * {@link #CONNOTEA_PASSWORD_KEY} (
     * <code>{@value #CONNOTEA_PASSWORD_KEY}</code>).
     * 
     * @param properties the properties containing your Connotea username and
     *            password under the specified keys
     * @throws ClassCastException if the property values under the specified
     *             keys could not be cast to {@link String}s
     */
    public Connotea(Properties properties) throws ClassCastException {
        this(properties.getProperty(CONNOTEA_USERNAME_KEY), properties.getProperty(CONNOTEA_PASSWORD_KEY), Integer.parseInt(properties.getProperty(CONNOTEA_TIMEOUT_KEY)));
    }

    /**
     * Instantiate a Connotea API client, passing your Connotea username and
     * password.
     * 
     * @param username your Connotea username
     * @param password your Connotea password
     */
    public Connotea(String username, String password) {
        this(username, password, DEFAULT_TIMEOUT);
    }

    /**
     * Instantiate a Connotea API client with the given timeout, passing your
     * connotea username and password.
     * 
     * @param username the username
     * @param password the password
     * @param timeout the timeout
     */
    public Connotea(String username, String password, int timeout) {
        connector = new RestletConnector(username, password, timeout);
    }

    /**
     * Add the provided {@link Post} to Connotea for the user identified by the
     * username with which this client was instantiated. The {@link Post} must,
     * at the very least, contain <code>non-null</code> values for the
     * following:
     * <ul>
     * <li>{@link Bookmark#getLink()} (on {@link Post#getBookmark()})</li>
     * </ul>
     * Additionally, one <em>or</em> both of the following must be
     * <strong>both</strong> <code>non-null</code> <strong>and</strong> return
     * <code>false</code> for {@link List#isEmpty()}:
     * <ul>
     * <li>{@link Bookmark#getTags()} (on {@link Post#getBookmark()})</li>
     * <li>{@link Post#getSubjects()}</li>
     * </ul>
     * Note that this client will not prevent you from attempting to pass an
     * empty tag string (via having only empty strings in {@link Tag#getValue()}
     * in either of the above lists), but the Connotea API
     * <strong>will</strong>! (i.e. you will get back an error with HttpStatus
     * code 400 and a message <q>Missing tags, malformed tags, or use of a
     * reserved keyword as a tag</q> and your request will fail.) The Connotea
     * API requires the submission of at least one tag with a value when adding
     * a new {@link Post}.
     * 
     * @param post the {@link Post} to add
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if the {@link Post} could not be added (see the
     *             {@link Message} returned as part of the exception by calling
     *             {@link ConnoteaException#getApiMessage()}
     */
    public Message add(Post post) throws ConnoteaException {
        String uri = uri(Command.ADD);
        Map<String, String> parameters = parameters(post);
        Response response = connector.post(uri, parameters);
        Document document = response.getDocument();
        return messageParser.parse(document);
    }

    /**
     * Convenient shorthand version of {@link #add(Post)} which adds a new
     * {@link Post} to Connotea using the provided URI and tags.
     * 
     * @param uri the {@link Bookmark} URI
     * @param tags the tags
     * @return the {@link Message} return by Connotea
     * @throws ConnoteaException if the {@link Post} could not be added (see the
     *             {@link Message} returned as part of the exception by calling
     *             {@link ConnoteaException#getApiMessage()}
     */
    public Message add(String uri, List<Tag> tags) throws ConnoteaException {
        return add(new Post(new Bookmark(new Reference(uri), tags)));
    }

    /**
     * Convenient shorthand version of {@link #add(Post)} which adds a new
     * {@link Post} to Connotea using the provided fields.
     * 
     * @param uri the {@link Bookmark} URI
     * @param tags the tags
     * @param title the {@link Post} title
     * @param description the {@link Post} description
     * @param comment a {@link Post} comment
     * @param myWork true if the user identified by the username with which this
     *            client was instantiated is claiming authorship over the
     *            resource identified by the supplied URI
     * @param privato true if the user identified by the username with which
     *            this client was instantiated wants this {@link Post} to be
     *            private to their account
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if the {@link Post} could not be added (see the
     *             {@link Message} returned as part of the exception by calling
     *             {@link ConnoteaException#getApiMessage()}
     */
    public Message add(String uri, List<Tag> tags, String title, String description, String comment, boolean myWork, boolean privato) throws ConnoteaException {
        List<Comment> comments = new ArrayList<Comment>();
        comments.add(new Comment(comment));
        return add(new Post(new Bookmark(new Reference(uri), tags), comments, title, description, myWork, privato));
    }

    /**
     * Useful initialisation operation that allows you to check the
     * authentication credentials with which this client has been initialised to
     * ensure that you <em>can</em> actually connect to Connotea.
     * 
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if your credentials could not be checked (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public Message checkCredentials() throws ConnoteaException {
        String uri = uri(Command.NOOP);
        Response response = connector.get(uri);
        Document document = response.getDocument();
        return messageParser.parse(document);
    }

    /**
     * Edit the provided {@link Post} on Connotea for the user identified by the
     * username with which this client was instantiated. The {@link Post} must,
     * at the very least, contain <code>non-null</code> values for the
     * following:
     * <ul>
     * <li>{@link Bookmark#getLink()} (on {@link Post#getBookmark()})</li>
     * </ul>
     * Additionally, one <strong>or</strong> both of the following must be
     * <strong>both</strong> <code>non-null</code> <strong>and</strong> return
     * <code>false</code> for {@link List#isEmpty()}:
     * <ul>
     * <li>{@link Bookmark#getTags()} (on {@link Post#getBookmark()})</li>
     * <li>{@link Post#getSubjects()}</li>
     * </ul>
     * Note that this client will not prevent you from attempting to pass an
     * empty tag string (via having only empty strings in {@link Tag#getValue()}
     * in either of the above lists), but the Connotea API
     * <strong>will</strong>! (i.e. you will get back an error with HttpStatus
     * code 400 and a message <q>Missing tags, malformed tags, or use of a
     * reserved keyword as a tag</q> and your request will fail.) The Connotea
     * API requires the submission of at least one tag with a value when editing
     * a {@link Post}.
     * 
     * @param post the {@link Post} to edit
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if the {@link Post} could not be edited (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public Message edit(Post post) throws ConnoteaException {
        String uri = uri(Command.EDIT);
        Map<String, String> parameters = parameters(post);
        Response response = connector.post(uri, parameters);
        Document document = response.getDocument();
        return messageParser.parse(document);
    }

    /**
     * Convenient shorthand version of {@link #edit(Post)} which edits the
     * {@link Post} on Connotea which has the specified URI with the provided
     * fields.
     * 
     * @param uri the {@link Bookmark} URI
     * @param tags the tags
     * @return the {@link Message} return by Connotea
     * @throws ConnoteaException if the {@link Post} could not be edited (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public Message edit(String uri, List<Tag> tags) throws ConnoteaException {
        return edit(new Post(new Bookmark(new Reference(uri), tags)));
    }

    /**
     * Convenient shorthand version of {@link #edit(Post)} which edits the
     * {@link Post} on Connotea which has the specified URI with the provided
     * fields.
     * 
     * @param uri the {@link Bookmark} URI
     * @param tags the tags
     * @param title the {@link Post} title
     * @param description the {@link Post} description
     * @param comment a {@link Post} comment
     * @param myWork true if the user identified by the username with which this
     *            client was instantiated is claiming authorship over the
     *            resource identified by the provided uri
     * @param privato if the user identified by the username with which this
     *            client was instantiated wants this {@link Post} to be private
     *            to their account
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if the {@link Post} could not be edited (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public Message edit(String uri, List<Tag> tags, String title, String description, String comment, boolean myWork, boolean privato) throws ConnoteaException {
        List<Comment> comments = new ArrayList<Comment>();
        comments.add(new Comment(comment));
        return edit(new Post(new Bookmark(new Reference(uri), tags), comments, title, description, myWork, privato));
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * {@link Date}.
     * 
     * @param date the {@link Date}
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByDate(Date date) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.DATE, format(date));
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by
     * provided parameters.
     * 
     * @param date the {@link Date}
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByDate(Date date, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.DATE, format(date), limit, start);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param date the {@link Date}
     * @param query the query
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByDate(Date date, String query) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.DATE, format(date), query);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param date the {@link Date}
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByDate(Date date, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.DATE, format(date), query, limit, start);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea containing the
     * specified {@link Tag}.
     * 
     * @param tag the {@link Tag}
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByTag(Tag tag) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.TAG, tag.getValue());
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea containing the
     * specified {@link Tag}. Return a subset from the total result set
     * delimited by provided parameters.
     * 
     * @param tag the {@link Tag}
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByTag(Tag tag, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.TAG, tag.getValue(), limit, start);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea containing the
     * specified {@link Tag}. Return a subset from the total result set
     * delimited by the provided free text query.
     * 
     * @param tag the {@link Tag}
     * @param query the query
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByTag(Tag tag, String query) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.TAG, tag.getValue(), query);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea containing the
     * specified {@link Tag}. Return a subset from the total result set
     * delimited by the provided parameters and free text query.
     * 
     * @param tag the {@link Tag}
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByTag(Tag tag, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.TAG, tag.getValue(), query, limit, start);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the given URI.
     * 
     * @param uri the URI
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUri(String uri) throws ConnoteaException {
        String u = uri(Entity.BOOKMARK, Field.URI, md5(uri));
        Response response = connector.get(u);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by provided
     * parameters.
     * 
     * @param uri the URI
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUri(String uri, int limit, int start) throws ConnoteaException {
        String u = uri(Entity.BOOKMARK, Field.URI, md5(uri), limit, start);
        Response response = connector.get(u);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by the provided free
     * text query.
     * 
     * @param uri the URI
     * @param query the query
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUri(String uri, String query) throws ConnoteaException {
        String u = uri(Entity.BOOKMARK, Field.URI, md5(uri), query);
        Response response = connector.get(u);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * {@link org.restlet.data.Reference} URI. Return a subset from the total
     * result set delimited by the provided parameters and free text query.
     * 
     * @param uri the URI
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUri(String uri, String query, int limit, int start) throws ConnoteaException {
        String u = uri(Entity.BOOKMARK, Field.URI, md5(uri), query, limit, start);
        Response response = connector.get(u);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * username.
     * 
     * @param username the username
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUsername(String username) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.USER, username);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by provided
     * parameters.
     * 
     * @param username the username
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUsername(String username, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.USER, username, limit, start);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param username the username
     * @param query the query
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUsername(String username, String query) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.USER, username, query);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all {@link Bookmark}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param username the username
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Bookmark}s
     * @throws ConnoteaException if the {@link Bookmark}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Bookmark> getBookmarksByUsername(String username, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.USER, username, query, limit, start);
        Response response = connector.get(uri);
        return bookmarks(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Date}.
     * 
     * @param date the {@link Date}
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByDate(Date date) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.DATE, format(date));
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by
     * provided parameters.
     * 
     * @param date the {@link Date}
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByDate(Date date, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.DATE, format(date), limit, start);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param date the {@link Date}
     * @param query the query
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByDate(Date date, String query) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.DATE, format(date), query);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param date the {@link Date}
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByDate(Date date, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.DATE, format(date), query, limit, start);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Tag}.
     * 
     * @param tag the {@link Tag}
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByTag(Tag tag) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.TAG, tag.getValue());
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Tag}. Return a subset from the total result set delimited by
     * provided parameters.
     * 
     * @param tag the {@link Tag}
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByTag(Tag tag, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.TAG, tag.getValue(), limit, start);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Tag}. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param tag the {@link Tag}
     * @param query the query
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByTag(Tag tag, String query) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.TAG, tag.getValue(), query);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * {@link Tag}. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param tag the {@link Tag}
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByTag(Tag tag, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.TAG, tag.getValue(), query, limit, start);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the given URI.
     * 
     * @param uri the URI
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUri(String uri) throws ConnoteaException {
        String u = uri(Entity.POST, Field.URI, md5(uri));
        Response response = connector.get(u);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by provided
     * parameters.
     * 
     * @param uri the URI
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUri(String uri, int limit, int start) throws ConnoteaException {
        String u = uri(Entity.POST, Field.URI, md5(uri), limit, start);
        Response response = connector.get(u);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by the provided free
     * text query.
     * 
     * @param uri the URI
     * @param query the query
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUri(String uri, String query) throws ConnoteaException {
        String u = uri(Entity.POST, Field.URI, md5(uri), query);
        Response response = connector.get(u);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by the provided
     * parameters and free text query.
     * 
     * @param uri the URI
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUri(String uri, String query, int limit, int start) throws ConnoteaException {
        String u = uri(Entity.POST, Field.URI, md5(uri), query, limit, start);
        Response response = connector.get(u);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * username.
     * 
     * @param username the username
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUsername(String username) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.USER, username);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by provided
     * parameters.
     * 
     * @param username the username
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUsername(String username, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.BOOKMARK, Field.USER, username, limit, start);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param username the username
     * @param query the query
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUsername(String username, String query) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.USER, username, query);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Retrieve a list of all the {@link Post}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param username the username
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Post}s
     * @throws ConnoteaException if the {@link Post}s could not be retrieved
     *             (see the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Post> getPostsByUsername(String username, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.POST, Field.USER, username, query, limit, start);
        Response response = connector.get(uri);
        return posts(response);
    }

    /**
     * Obtain a fully populated {@link Tag} instance for the specified tag
     * <code>value</code>.
     * 
     * @param tagValue the tagValue
     * @return the {@link Tag} or <code>null</code> if the tag does not
     *         currently exist on Connotea
     * @throws ConnoteaException if the {@link Tag} could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     * @see Tag#getValue()
     */
    public Tag getTag(String tagValue) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.TAG, tagValue);
        Response response = connector.get(uri);
        List<Tag> tags = tags(response);
        if (isNotEmpty(tags)) {
            return tags.get(0);
        }
        return null;
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * {@link Date}.
     * 
     * @param date the {@link Date}
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByDate(Date date) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.DATE, format(date));
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by
     * provided parameters.
     * 
     * @param date the {@link Date}
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByDate(Date date, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.DATE, format(date), limit, start);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param date the {@link Date}
     * @param query the query
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByDate(Date date, String query) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.DATE, format(date), query);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * {@link Date}. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param date the {@link Date}
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByDate(Date date, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.DATE, format(date), query, limit, start);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the given URI.
     * 
     * @param uri the URI
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUri(String uri) throws ConnoteaException {
        String u = uri(Entity.TAG, Field.URI, md5(uri));
        Response response = connector.get(u);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by provided
     * parameters.
     * 
     * @param uri the URI
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUri(String uri, int limit, int start) throws ConnoteaException {
        String u = uri(Entity.TAG, Field.URI, md5(uri), limit, start);
        Response response = connector.get(u);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by the provided free
     * text query.
     * 
     * @param uri the URI
     * @param query the query
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUri(String uri, String query) throws ConnoteaException {
        String u = uri(Entity.TAG, Field.URI, md5(uri), query);
        Response response = connector.get(u);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the given URI.
     * Return a subset from the total result set delimited by the provided
     * parameters and free text query.
     * 
     * @param uri the URI
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUri(String uri, String query, int limit, int start) throws ConnoteaException {
        String u = uri(Entity.TAG, Field.URI, md5(uri), query, limit, start);
        Response response = connector.get(u);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * username.
     * 
     * @param username the username
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUsername(String username) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.USER, username);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by provided
     * parameters.
     * 
     * @param username the username
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUsername(String username, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.USER, username, limit, start);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by the
     * provided free text query.
     * 
     * @param username the username
     * @param query the query
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUsername(String username, String query) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.USER, username, query);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Retrieve a list of all the {@link Tag}s on Connotea for the specified
     * username. Return a subset from the total result set delimited by the
     * provided parameters and free text query.
     * 
     * @param username the username
     * @param query the query
     * @param limit the subset size from the total result set to return
     * @param start the index within the total result set from which to begin
     *            the subset
     * @return the {@link Tag}s
     * @throws ConnoteaException if the {@link Tag}s could not be retrieved (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public List<Tag> getTagsByUsername(String username, String query, int limit, int start) throws ConnoteaException {
        String uri = uri(Entity.TAG, Field.USER, username, query, limit, start);
        Response response = connector.get(uri);
        return tags(response);
    }

    /**
     * Remove the provided {@link Post} from Connotea for the user identified by
     * the username with which this client was instantiated. The {@link Post}
     * must, at the very least, contain <code>non-null</code> values for the
     * following:
     * <ul>
     * <li>{@link Bookmark#getLink()} (on {@link Post#getBookmark()})</li>
     * </ul>
     * 
     * @param post the {@link Post} to remove
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if the {@link Post} could not be removed (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public Message remove(Post post) throws ConnoteaException {
        String uri = uri(Command.REMOVE);
        Map<String, String> parameters = parameters(post, false, false);
        Response response = connector.post(uri, parameters);
        Document document = response.getDocument();
        return messageParser.parse(document);
    }

    /**
     * Remove the {@link Post} from Connotea identified by the provided
     * {@link org.restlet.data.Reference} URI for the user identified by the
     * username with which this client was instantiated.
     * 
     * @param uri the URI
     * @return the {@link Message} returned by Connotea
     * @throws ConnoteaException if the {@link Post} could not be removed (see
     *             the {@link Message} returned as part of the exception by
     *             calling {@link ConnoteaException#getApiMessage()}
     */
    public Message remove(String uri) throws ConnoteaException {
        return remove(new Post(new Bookmark(new Reference(uri))));
    }

    /**
     * Sets the bookmarkParser.
     * 
     * @param bookmarkParser the bookmarkParser to set
     */
    public void setBookmarkParser(Parser<Document, List<Bookmark>> bookmarkParser) {
        this.bookmarkParser = bookmarkParser;
    }

    /**
     * Sets the connector.
     * <p>
     * <em>Note: authentication and timeout handling is delegated by this class
     * to the connector. If you set your own connector, this will override your
     * Connotea username/password and any timeout values that have been
     * supplied during the instantiation of this class. It is recommended that
     * you call {@link #checkCredentials()} after setting the new connector has
     * been set.</em>
     * 
     * @param connector the connector to set
     */
    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    /**
     * Sets the messageParser.
     * 
     * @param messageParser the messageParser to set
     */
    public void setMessageParser(Parser<Document, Message> messageParser) {
        this.messageParser = messageParser;
    }

    /**
     * Sets the postParser.
     * 
     * @param postParser the postParser to set
     */
    public void setPostParser(Parser<Document, List<Post>> postParser) {
        this.postParser = postParser;
    }

    /**
     * Sets the tagParser.
     * 
     * @param tagParser the tagParser to set
     */
    public void setTagParser(Parser<Document, List<Tag>> tagParser) {
        this.tagParser = tagParser;
    }

    private List<Bookmark> bookmarks(Response response) throws ConnoteaException {
        Document document = response.getDocument();
        if (response.getStatus() == 200) {
            return bookmarkParser.parse(document);
        }
        Message message = messageParser.parse(document);
        throw new ConnoteaException(message);
    }

    private String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (Exception e) {
            throw new ConnoteaRuntimeException(e);
        }
    }

    private String format(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String md5(String uri) throws ConnoteaRuntimeException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(uri.getBytes());
            byte[] bytes = messageDigest.digest();
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    stringBuffer.append('0');
                }
                stringBuffer.append(hex);
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ConnoteaRuntimeException(e);
        }
    }

    private Map<String, String> parameters(Post p) {
        return parameters(p, true, true);
    }

    private Map<String, String> parameters(Post p, boolean tagsRequired, boolean includeOptionalFields) {
        if (p == null) {
            throw new ConnoteaRuntimeException("post is null");
        }
        String uri = null;
        try {
            uri = p.getBookmark().getLink().getTargetRef().toString();
        } catch (Exception e) {
            throw new ConnoteaRuntimeException("could not required field URI from post: " + p);
        }
        if (isBlank(uri)) {
            throw new ConnoteaRuntimeException("required field URI is blank for post: " + p);
        }
        Map<String, String> params = new HashMap<String, String>();
        LOG.debug("adding URI to params: " + uri);
        params.put("uri", uri);
        if (tagsRequired) {
            if (isEmpty(p.getSubjects()) || ((p.getBookmark() == null) || isEmpty(p.getBookmark().getTags()))) {
                throw new ConnoteaRuntimeException("both post subjects and bookmark tags are null or empty: at least one valid element in either is required for post: " + p);
            }
            String tags = tags(p);
            params.put("tags", tags);
            LOG.debug("added tags to params: " + tags);
        }
        if (includeOptionalFields) {
            String usertitle = p.getTitle();
            if (usertitle != null) {
                if (isBlank(usertitle) && LOG.isWarnEnabled()) {
                    LOG.warn("empty string provided for post title: " + "current field value *may* be deleted!");
                }
                params.put("usertitle", usertitle);
                LOG.debug("added usertitle param: " + usertitle);
            }
            String description = p.getDescription();
            if (description != null) {
                if (isBlank(description) && LOG.isWarnEnabled()) {
                    LOG.warn("empty string provided for post description: " + "current field value *may* be deleted!");
                }
                params.put("description", description);
                LOG.debug("added description param: " + description);
            }
            String mywork = (p.isMyWork()) ? "1" : "0";
            params.put("mywork", mywork);
            LOG.debug("added mywork param: " + mywork);
            String privato = (p.isPrivate()) ? "1" : "0";
            params.put("private", privato);
            LOG.debug("added private param: " + privato);
            if (isNotEmpty(p.getComments())) {
                LOG.debug("attempting to use comment at index 0 as post comment field value");
                String comment = p.getComments().get(0).getEntry();
                if (comment != null) {
                    if (isBlank(comment) && LOG.isWarnEnabled()) {
                        LOG.warn("empty string provided for comment entry: " + "current field value *may* be deleted");
                    }
                    params.put("comment", comment);
                    LOG.debug("added comment param: " + comment);
                }
            }
        }
        return null;
    }

    private List<Post> posts(Response response) throws ConnoteaException {
        Document document = response.getDocument();
        if (response.getStatus() == 200) {
            return postParser.parse(document);
        }
        Message message = messageParser.parse(document);
        throw new ConnoteaException(message);
    }

    private String quote(String tag) {
        if (tag.contains(" ")) {
            return "\"" + tag + "\"";
        }
        return tag;
    }

    private String tags(Post p) {
        List<String> tags = new ArrayList<String>();
        if (isNotEmpty(p.getSubjects())) {
            for (Tag t : p.getSubjects()) {
                String value = quote(t.getValue());
                tags.add(value);
                LOG.debug("added tag: " + value);
            }
        }
        if ((p.getBookmark() != null) && isNotEmpty(p.getBookmark().getTags())) {
            List<Tag> bookmarkTags = p.getBookmark().getTags();
            for (Tag t : bookmarkTags) {
                String value = quote(t.getValue());
                tags.add(value);
                LOG.debug("added tag: " + value);
            }
        }
        return join(tags, ",");
    }

    private List<Tag> tags(Response response) throws ConnoteaException {
        Document document = response.getDocument();
        if (response.getStatus() == 200) {
            return tagParser.parse(document);
        }
        Message message = messageParser.parse(document);
        throw new ConnoteaException(message);
    }

    private String uri(Command command) {
        return BASE_API_URI + command.toString();
    }

    private String uri(Entity entity, Field field, String argument) {
        return BASE_API_URI + entity + field + argument;
    }

    private String uri(Entity entity, Field field, String argument, int limit, int start) {
        return BASE_API_URI + entity + field + argument + "?num=" + limit + "&start=" + start;
    }

    private String uri(Entity entity, Field field, String argument, String query) {
        return BASE_API_URI + entity + field + argument + "?q=" + encode(query);
    }

    private String uri(Entity entity, Field field, String argument, String query, int limit, int start) {
        return BASE_API_URI + entity + field + argument + "?num=" + limit + "&start=" + start + "q=" + encode(query);
    }
}
