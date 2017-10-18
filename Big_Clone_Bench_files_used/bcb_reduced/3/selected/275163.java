package flickr.service;

import flickr.service.iface.FlickrMethod;
import flickr.service.iface.FlickrMethodParam;
import flickr.service.api.PeopleService;
import flickr.service.api.CommentsService;
import flickr.service.api.FavoritesService;
import flickr.service.api.GroupsService;
import flickr.service.api.PoolsService;
import flickr.service.iface.AuthenticationInterface;
import flickr.response.Activity;
import flickr.response.Error;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.JAXBContext;
import flickr.response.Auth;
import flickr.response.Auth;
import flickr.response.Comment;
import flickr.response.Comments;
import flickr.response.Contact;
import flickr.response.Contacts;
import flickr.response.Event;
import flickr.response.FlickrResponse;
import flickr.response.Group;
import flickr.response.GroupInfo;
import flickr.response.Groups;
import flickr.response.Item;
import flickr.response.Items;
import flickr.response.Photo;
import flickr.response.PhotoSet;
import flickr.response.PhotoSets;
import flickr.response.Photos;
import flickr.response.ResponseObject;
import flickr.response.User;
import flickr.service.api.ContactsService;
import flickr.service.api.PhotoSetsService;
import flickr.service.api.PhotosService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;

/**
 * @author leon
 */
public class Flickr {

    private static final boolean DEBUG = false;

    private static final String API_KEY_PROD = "e31ff7d4add3ff68dc0986797b715090";

    private static final String SECRET_PROD = "5e1fe1d8a0274be7";

    private static final String API_KEY_DEVEL = "57205bc2377f58f87d20e611df7be895";

    private static final String SECRET_DEVEL = "557961665dd791bf";

    static final String API_KEY = API_KEY_DEVEL;

    static final String SECRET = SECRET_DEVEL;

    private String frob;

    private Auth auth;

    private static Flickr flickr;

    private JAXBContext jaxbContext;

    private FlickrConnector connector;

    private List<AuthorizationListener> listeners = new ArrayList<AuthorizationListener>();

    private Flickr() {
        try {
            connector = new SimpleFlickrConnector();
            jaxbContext = JAXBContext.newInstance(FlickrResponse.class, Auth.class, User.class, Groups.class, Group.class, GroupInfo.class, Photos.class, Photo.class, Comment.class, Comments.class, Error.class, Items.class, Item.class, Activity.class, Event.class, PhotoSet.class, PhotoSets.class, Contacts.class, Contact.class);
            loadAuthentication();
        } catch (Exception ex) {
            throw new FlickrException(ex);
        }
    }

    public static Flickr get() {
        if (flickr == null) {
            flickr = new Flickr();
        }
        return flickr;
    }

    Auth getAuth() {
        if (auth == null) {
            auth = new Auth();
            User user = new User();
            user.setFullName("");
            user.setId("");
            user.setUserName("");
            auth.setUser(user);
            auth.setPermissions("");
            auth.setToken("");
        }
        return auth;
    }

    public User getCurrentUser() {
        return getAuth().getUser();
    }

    public URL startAuthorization() throws FlickrException {
        AuthenticationInterface ai = getInterface(AuthenticationInterface.class, "/rsp/frob");
        frob = ai.getFrob(API_KEY);
        try {
            return new URL("http://www.flickr.com/services/auth/?api_key=" + API_KEY + "&perms=write&frob=" + frob + "&api_sig=" + md5(SECRET, "api_key", API_KEY, "frob", frob, "permswrite"));
        } catch (MalformedURLException ex) {
            throw new FlickrException(ex);
        }
    }

    public PeopleService getPeopleService() {
        return new DefaultPeopleService(this);
    }

    public FavoritesService getFavoritesService() {
        return new DefaultFavoritesService(this);
    }

    public GroupsService getGroupsService() {
        return new DefaultGroupsService(this);
    }

    public CommentsService getCommentsService() {
        return new DefaultCommentsService(this);
    }

    public ContactsService getContactsService() {
        return new DefaultContactsService(this);
    }

    public PoolsService getPoolsService() {
        return new DefaultPoolsService(this);
    }

    public RecentActivityService getRecentActivityService() {
        return new DefaultRecentActivityService(this);
    }

    public PhotoSetsService getPhotoSetsService() {
        return new DefaultPhotoSetsService(this);
    }

    public PhotosService getPhotosService() {
        return new DefaultPhotosService(this);
    }

    public void finishAuthorization() throws FlickrException {
        ValueHolderResponseHandler<Auth> vhh = new ValueHolderResponseHandler<Auth>();
        AuthenticationInterface ai = getInterface(AuthenticationInterface.class, vhh, true);
        ai.finishAuthentication(API_KEY, frob);
        auth = vhh.getValue();
        for (AuthorizationListener al : listeners) {
            al.authorizationFinished();
        }
        storeAuthentication();
    }

    private void loadAuthentication() {
        Preferences prefs = Preferences.userNodeForPackage(Flickr.class);
        byte[] bytes = prefs.getByteArray("flickrauth", null);
        if (bytes == null) {
            return;
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            auth = (Auth) ois.readObject();
        } catch (Exception ex) {
        }
    }

    private void storeAuthentication() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(auth);
            oos.close();
            Preferences prefs = Preferences.userNodeForPackage(Flickr.class);
            prefs.putByteArray("flickrauth", bos.toByteArray());
        } catch (Exception ex) {
        }
    }

    private String md5(String... args) throws FlickrException {
        try {
            StringBuilder sb = new StringBuilder();
            for (String str : args) {
                sb.append(str);
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sb.toString().getBytes());
            byte[] bytes = md.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String hx = Integer.toHexString(0xFF & b);
                if (hx.length() == 1) {
                    hx = "0" + hx;
                }
                result.append(hx);
            }
            return result.toString();
        } catch (Exception ex) {
            throw new FlickrException(ex);
        }
    }

    <T> T getInterface(Class<T> interfaceClass, final String xpath) {
        return _getInterface(interfaceClass, xpath, null, true);
    }

    <T, R extends ResponseObject> T getInterface(Class<T> interfaceClass, FlickrResponseHandler<R> responseHandler, boolean waitForCompletion) {
        return _getInterface(interfaceClass, null, responseHandler, waitForCompletion);
    }

    private <T, R extends ResponseObject> T _getInterface(final Class<T> interfaceClass, final String xpath, final FlickrResponseHandler<R> responseHandler, final boolean waitForCompletion) {
        @SuppressWarnings("unchecked") T t = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass }, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                FlickrMethod fm = method.getAnnotation(FlickrMethod.class);
                if (fm == null) {
                    throw new IllegalStateException("Theres no proper " + FlickrMethod.class.getName() + " annotation on " + method.getName() + " from class " + method.getDeclaringClass().getName());
                }
                TreeMap<String, String> param2Value = new TreeMap<String, String>();
                param2Value.put("method", fm.method());
                Annotation[][] anns = method.getParameterAnnotations();
                int paramIndex = 0;
                for (Annotation[] paramAnnotations : anns) {
                    for (Annotation a : paramAnnotations) {
                        if (a.annotationType().equals(FlickrMethodParam.class)) {
                            String argValue = null;
                            Object arg = args[paramIndex];
                            if (arg != null && (boolean.class.isAssignableFrom(arg.getClass()) || Boolean.class.isAssignableFrom(arg.getClass()))) {
                                argValue = (Boolean) arg ? "1" : "0";
                            } else {
                                argValue = String.valueOf(arg);
                            }
                            param2Value.put(((FlickrMethodParam) a).name(), String.valueOf(argValue));
                        }
                    }
                    paramIndex++;
                }
                StringBuilder sb = new StringBuilder();
                List<String> md5Args = new ArrayList<String>();
                md5Args.add(SECRET);
                for (Map.Entry<String, String> e : param2Value.entrySet()) {
                    if (sb.length() == 0) {
                        sb.append("?");
                    } else {
                        sb.append("&");
                    }
                    String encoded = URLEncoder.encode(e.getValue(), "ISO8859-1");
                    sb.append(e.getKey()).append('=').append(encoded);
                    md5Args.add(e.getKey());
                    md5Args.add(e.getValue());
                }
                sb.append("&api_sig=" + md5(md5Args.toArray(new String[md5Args.size()])));
                if (DEBUG) {
                    System.err.println(sb);
                }
                if (xpath != null) {
                    return connector.invoke(sb.toString(), xpath);
                } else {
                    Future<?> f = connector.invoke(jaxbContext, sb.toString(), responseHandler);
                    if (waitForCompletion) {
                        f.get();
                    }
                    return null;
                }
            }
        });
        return t;
    }

    public void addAuthorizationListener(AuthorizationListener authorizationListener) {
        listeners.add(authorizationListener);
    }

    public void removeAuthorizationListener(AuthorizationListener authorizationListener) {
        listeners.remove(authorizationListener);
    }

    public static interface AuthorizationListener {

        public void authorizationFinished();
    }
}
