package com.tamanderic.smupload;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class SmugMugApi {

    private static final String API_VERSION = "1.2.0";

    private static final String BASE_URL = "://api.smugmug.com/hack/rest/" + API_VERSION + "/";

    private static final String HTTP_BASE_URL = "http" + BASE_URL;

    private static final String HTTPS_BASE_URL = "https" + BASE_URL;

    private static final String UPLOAD_URL = "http://upload.smugmug.com/";

    private String apiKey;

    private boolean debug;

    private String sessionId;

    private byte[] photoBuffer;

    private static final String ELEMENT_RSP = "rsp";

    private static final String ELEMENT_SESSION_ID = "Session";

    private static final String ELEMENT_ALBUM = "Album";

    private static final String ELEMENT_CATEGORY = "Category";

    private static final String ELEMENT_SUBCATEGORY = "SubCategory";

    private static final String ELEMENT_IMAGE = "Image";

    private static final String ATTRNAME_STAT = "stat";

    private static final String ATTRVAL_STAT_OK = "ok";

    private static final String ATTRNAME_KEY = "Key";

    private static final String ATTRNAME_TITLE = "Title";

    private static final String ATTRNAME_ID = "id";

    private static final String ATTRNAME_NAME = "Name";

    private static final String ATTRNAME_URL = "URL";

    public SmugMugApi(String apiKey) {
        this(apiKey, false);
    }

    public SmugMugApi(String apiKey, boolean debug) {
        this.apiKey = apiKey;
        this.debug = debug;
        photoBuffer = new byte[5 * 1024 * 1024];
    }

    public abstract class Resp {

        private String respStat;

        private boolean parseError;

        protected Resp() {
            this.respStat = null;
            this.parseError = false;
        }

        public boolean isOk() {
            return (this.parseError == false && this.respStat != null && this.respStat.equals(ATTRVAL_STAT_OK));
        }

        public String getRespStat() {
            return parseError ? "invalid response message" : respStat;
        }

        public void setRespStat(String respStat) {
            this.respStat = respStat;
        }

        public void setParseError() {
            parseError = true;
        }
    }

    /***********************************************************************
     * smugmug.login.withPassword
     ***********************************************************************/
    public LoginResp loginWithPassword(String loginName, String password) {
        String method = "smugmug.login.withPassword";
        LoginResp resp = new LoginResp();
        String[] params = { "method", method, "APIKey", this.apiKey, "EmailAddress", loginName, "Password", password };
        callApi(new Connection(HTTPS_BASE_URL, params, this.debug), new LoginRespHandler(resp));
        if (resp.isOk()) {
            this.sessionId = resp.getSessionId();
        }
        if (this.debug) {
            System.out.println(method + " sessionId " + this.sessionId + " respStat " + resp.getRespStat());
        }
        return resp;
    }

    public class LoginResp extends Resp {

        private String sid;

        public String getSessionId() {
            return sid;
        }

        public void setSessionId(String sid) {
            this.sid = sid;
        }
    }

    private class LoginRespHandler extends DefaultHandler {

        LoginResp resp;

        public LoginRespHandler(LoginResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_SESSION_ID)) {
                resp.setSessionId(attrs.getValue(ATTRNAME_ID));
            }
        }
    }

    /***********************************************************************
     * smugmug.logout
     ***********************************************************************/
    public void logout() {
        String method = "smugmug.logout";
        String[] params = { "method", method, "sessionId", this.sessionId };
        Connection conn = new Connection(HTTP_BASE_URL, params, this.debug);
        conn.connect();
        this.sessionId = null;
    }

    /***********************************************************************
     * smugmug.categories.create
     ***********************************************************************/
    public CreateCategoryResp createCategory(String categoryName) {
        String method = "smugmug.categories.create";
        CreateCategoryResp resp = new CreateCategoryResp();
        assertLoggedIn();
        String[] params = { "method", method, "SessionID", this.sessionId, "Name", categoryName };
        callApi(new Connection(HTTP_BASE_URL, params, this.debug), new CreateCategoryRespHandler(resp));
        if (this.debug) {
            System.out.println(method + " respstat " + resp.getRespStat());
        }
        return resp;
    }

    public class CreateCategoryResp extends Resp {

        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    private class CreateCategoryRespHandler extends DefaultHandler {

        CreateCategoryResp resp;

        public CreateCategoryRespHandler(CreateCategoryResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_CATEGORY)) {
                try {
                    resp.setId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
            }
        }
    }

    /***********************************************************************
     * smugmug.subcategories.create
     ***********************************************************************/
    public CreateSubcategoryResp createSubcategory(String subcategoryName, int categoryId) {
        String method = "smugmug.subcategories.create";
        CreateSubcategoryResp resp = new CreateSubcategoryResp();
        assertLoggedIn();
        String[] params = { "method", method, "SessionID", this.sessionId, "Name", subcategoryName, "CategoryID", Integer.toString(categoryId) };
        callApi(new Connection(HTTP_BASE_URL, params, this.debug), new CreateSubcategoryRespHandler(resp));
        if (this.debug) {
            System.out.println(method + " respstat " + resp.getRespStat());
        }
        return resp;
    }

    public class CreateSubcategoryResp extends Resp {

        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    private class CreateSubcategoryRespHandler extends DefaultHandler {

        CreateSubcategoryResp resp;

        public CreateSubcategoryRespHandler(CreateSubcategoryResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_SUBCATEGORY)) {
                try {
                    resp.setId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
            }
        }
    }

    /***********************************************************************
     * smugmug.albums.create
     ***********************************************************************/
    public interface SortMethod {

        public static final String POSITION = "Position";

        public static final String CAPTION = "Caption";

        public static final String FILE_NAME = "FileName";

        public static final String DATE = "Date";

        public static final String DATE_TIME = "DateTime";

        public static final String DATE_TIME_ORIGINAL = "DateTimeOriginal";
    }

    public static class AlbumAttributes {

        public Integer categoryId;

        public Integer albumTemplateId;

        public Integer subcategoryId;

        public String description;

        public String keywords;

        public Boolean geography;

        public Integer highlightId;

        public Integer position;

        public Boolean header;

        public Boolean clean;

        public Boolean exif;

        public Boolean filenames;

        public Boolean squareThumbs;

        public Integer templateId;

        public String sortMethod;

        public Boolean sortDirection;

        public String password;

        public String passwordHint;

        public Boolean isPublic;

        public Boolean worldSearchable;

        public Boolean smugSearchable;

        public Boolean external;

        public Boolean isProtected;

        public Boolean watermarking;

        public Integer watermarkId;

        public Boolean hideOwner;

        public Boolean larges;

        public Boolean xlarges;

        public Boolean x2larges;

        public Boolean x3larges;

        public Boolean originals;

        public Boolean canRank;

        public Boolean friendEdit;

        public Boolean familyEdit;

        public Boolean comments;

        public Boolean share;

        public Boolean printable;

        public Boolean defaultColor;

        public Integer proofDays;

        public String backprinting;

        public Float unsharpAmount;

        public Float unsharpRadius;

        public Float unsharpThreshold;

        public Float unsharpSigma;

        public Integer communityId;

        public AlbumAttributes() {
            setDefaults();
        }

        public void setDefaults() {
            categoryId = null;
            albumTemplateId = null;
            subcategoryId = null;
            description = null;
            keywords = null;
            geography = null;
            highlightId = null;
            position = null;
            header = null;
            clean = null;
            exif = null;
            filenames = null;
            squareThumbs = null;
            templateId = null;
            sortMethod = null;
            sortDirection = null;
            password = null;
            passwordHint = null;
            isPublic = null;
            worldSearchable = null;
            smugSearchable = null;
            external = null;
            isProtected = null;
            watermarking = null;
            watermarkId = null;
            hideOwner = null;
            larges = null;
            xlarges = null;
            x2larges = null;
            x3larges = null;
            originals = null;
            canRank = null;
            friendEdit = null;
            familyEdit = null;
            comments = null;
            share = null;
            printable = null;
            defaultColor = null;
            proofDays = null;
            backprinting = null;
            unsharpAmount = null;
            unsharpRadius = null;
            unsharpThreshold = null;
            unsharpSigma = null;
            communityId = null;
        }
    }

    public CreateAlbumResp createAlbum(String albumName, int categoryId, AlbumAttributes attrs) {
        String method = "smugmug.albums.create";
        CreateAlbumResp resp = new CreateAlbumResp();
        assertLoggedIn();
        String[] requiredParams = { "method", method, "SessionID", this.sessionId, "Title", albumName, "CategoryID", Integer.toString(categoryId) };
        String[] params = buildCreateAlbumParams(requiredParams, attrs);
        callApi(new Connection(HTTP_BASE_URL, params, this.debug), new CreateAlbumRespHandler(resp));
        if (this.debug) {
            System.out.println(method + " respstat " + resp.getRespStat());
        }
        return resp;
    }

    public class CreateAlbumResp extends Resp {

        private int id;

        private String key;

        public int getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    private class CreateAlbumRespHandler extends DefaultHandler {

        CreateAlbumResp resp;

        public CreateAlbumRespHandler(CreateAlbumResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_ALBUM)) {
                try {
                    resp.setId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                resp.setKey(attrs.getValue(ATTRNAME_KEY));
            }
        }
    }

    private String[] buildCreateAlbumParams(String[] requiredParams, AlbumAttributes attrs) {
        ArrayList<String> params = new ArrayList<String>(128);
        for (int i = 0; i < requiredParams.length; i++) {
            params.add(requiredParams[i]);
        }
        if (attrs.albumTemplateId != null) {
            params.add("AlbumTemplateID");
            params.add(attrs.albumTemplateId.toString());
        }
        if (attrs.subcategoryId != null) {
            params.add("SubCategoryID");
            params.add(attrs.subcategoryId.toString());
        }
        if (attrs.description != null) {
            params.add("Description");
            params.add(attrs.description);
        }
        if (attrs.keywords != null) {
            params.add("Keywords");
            params.add(attrs.keywords);
        }
        if (attrs.geography != null) {
            params.add("Geography");
            params.add(apiBooleanValue(attrs.geography));
        }
        if (attrs.highlightId != null) {
            params.add("HighlightID");
            params.add(attrs.highlightId.toString());
        }
        if (attrs.position != null) {
            params.add("Position");
            params.add(attrs.position.toString());
        }
        if (attrs.header != null) {
            params.add("Header");
            params.add(apiBooleanValue(attrs.header));
        }
        if (attrs.clean != null) {
            params.add("Clean");
            params.add(apiBooleanValue(attrs.clean));
        }
        if (attrs.exif != null) {
            params.add("EXIF");
            params.add(apiBooleanValue(attrs.exif));
        }
        if (attrs.filenames != null) {
            params.add("Filenames");
            params.add(apiBooleanValue(attrs.filenames));
        }
        if (attrs.squareThumbs != null) {
            params.add("SquareThumbs");
            params.add(apiBooleanValue(attrs.squareThumbs));
        }
        if (attrs.templateId != null) {
            params.add("TemplateID");
            params.add(attrs.templateId.toString());
        }
        if (attrs.sortMethod != null) {
            params.add("SortMethod");
            params.add(attrs.sortMethod);
        }
        if (attrs.sortDirection != null) {
            params.add("SortDirection");
            params.add(apiBooleanValue(attrs.sortDirection));
        }
        if (attrs.password != null) {
            params.add("Password");
            params.add(attrs.password);
        }
        if (attrs.passwordHint != null) {
            params.add("PasswordHint");
            params.add(attrs.passwordHint);
        }
        if (attrs.isPublic != null) {
            params.add("Public");
            params.add(apiBooleanValue(attrs.isPublic));
        }
        if (attrs.worldSearchable != null) {
            params.add("WorldSearchable");
            params.add(apiBooleanValue(attrs.worldSearchable));
        }
        if (attrs.smugSearchable != null) {
            params.add("SmugSearchable");
            params.add(apiBooleanValue(attrs.smugSearchable));
        }
        if (attrs.external != null) {
            params.add("External");
            params.add(apiBooleanValue(attrs.external));
        }
        if (attrs.isProtected != null) {
            params.add("Protected");
            params.add(apiBooleanValue(attrs.isProtected));
        }
        if (attrs.watermarking != null) {
            params.add("Watermarking");
            params.add(apiBooleanValue(attrs.watermarking));
        }
        if (attrs.watermarkId != null) {
            params.add("WatermarkID");
            params.add(attrs.watermarkId.toString());
        }
        if (attrs.hideOwner != null) {
            params.add("HideOwner");
            params.add(apiBooleanValue(attrs.hideOwner));
        }
        if (attrs.larges != null) {
            params.add("Larges");
            params.add(apiBooleanValue(attrs.larges));
        }
        if (attrs.xlarges != null) {
            params.add("XLarges");
            params.add(apiBooleanValue(attrs.xlarges));
        }
        if (attrs.x2larges != null) {
            params.add("X2Larges");
            params.add(apiBooleanValue(attrs.x2larges));
        }
        if (attrs.x3larges != null) {
            params.add("X3Larges");
            params.add(apiBooleanValue(attrs.x3larges));
        }
        if (attrs.originals != null) {
            params.add("Originals");
            params.add(apiBooleanValue(attrs.originals));
        }
        if (attrs.canRank != null) {
            params.add("CanRank");
            params.add(apiBooleanValue(attrs.canRank));
        }
        if (attrs.friendEdit != null) {
            params.add("FriendEdit");
            params.add(apiBooleanValue(attrs.friendEdit));
        }
        if (attrs.familyEdit != null) {
            params.add("FamilyEdit");
            params.add(apiBooleanValue(attrs.familyEdit));
        }
        if (attrs.comments != null) {
            params.add("Comments");
            params.add(apiBooleanValue(attrs.comments));
        }
        if (attrs.share != null) {
            params.add("Share");
            params.add(apiBooleanValue(attrs.share));
        }
        if (attrs.printable != null) {
            params.add("Printable");
            params.add(apiBooleanValue(attrs.printable));
        }
        if (attrs.defaultColor != null) {
            params.add("DefaultColor");
            params.add(apiBooleanValue(attrs.defaultColor));
        }
        if (attrs.proofDays != null) {
            params.add("ProofDays");
            params.add(attrs.proofDays.toString());
        }
        if (attrs.backprinting != null) {
            params.add("Backprinting");
            params.add(attrs.backprinting);
        }
        if (attrs.unsharpAmount != null) {
            params.add("UnsharpAmount");
            params.add(attrs.unsharpAmount.toString());
        }
        if (attrs.unsharpRadius != null) {
            params.add("UnsharpRadius");
            params.add(attrs.unsharpRadius.toString());
        }
        if (attrs.unsharpThreshold != null) {
            params.add("UnsharpThreshold");
            params.add(attrs.unsharpThreshold.toString());
        }
        if (attrs.unsharpSigma != null) {
            params.add("UnsharpSigma");
            params.add(attrs.unsharpSigma.toString());
        }
        if (attrs.communityId != null) {
            params.add("CommunityID");
            params.add(attrs.communityId.toString());
        }
        String[] retParams = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            retParams[i] = params.get(i);
        }
        return retParams;
    }

    /***********************************************************************
     * smugmug.categories.get
     ***********************************************************************/
    public class Category {

        private int id;

        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public GetCategoriesResp getCategories() {
        String method = "smugmug.categories.get";
        GetCategoriesResp resp = new GetCategoriesResp();
        assertLoggedIn();
        String[] params = { "method", method, "SessionID", this.sessionId };
        callApi(new Connection(HTTP_BASE_URL, params, this.debug), new GetCategoriesRespHandler(resp));
        if (this.debug) {
            System.out.println(method + " respstat " + resp.getRespStat());
        }
        return resp;
    }

    public class GetCategoriesResp extends Resp {

        private List<Category> categoryList;

        public GetCategoriesResp() {
            this.categoryList = new LinkedList<Category>();
        }

        public List<Category> getCategoryList() {
            return categoryList;
        }

        public void addCategory(Category category) {
            this.categoryList.add(category);
        }
    }

    private class GetCategoriesRespHandler extends DefaultHandler {

        GetCategoriesResp resp;

        public GetCategoriesRespHandler(GetCategoriesResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_CATEGORY)) {
                Category curCategory = new Category();
                try {
                    curCategory.setId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                curCategory.setName(attrs.getValue(ATTRNAME_TITLE));
                resp.addCategory(curCategory);
            }
        }
    }

    /***********************************************************************
     * smugmug.subcategories.get
     ***********************************************************************/
    public class Subcategory {

        private int id;

        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public GetSubcategoriesResp getSubcategories(int categoryId) {
        String method = "smugmug.subcategories.get";
        GetSubcategoriesResp resp = new GetSubcategoriesResp();
        assertLoggedIn();
        String[] params = { "method", method, "SessionID", this.sessionId, "CategoryID", Integer.toString(categoryId) };
        callApi(new Connection(HTTP_BASE_URL, params, this.debug), new GetSubcategoriesRespHandler(resp));
        if (this.debug) {
            System.out.println(method + " respstat " + resp.getRespStat());
        }
        return resp;
    }

    public class GetSubcategoriesResp extends Resp {

        private List<Subcategory> subcategoryList;

        public GetSubcategoriesResp() {
            this.subcategoryList = new LinkedList<Subcategory>();
        }

        public List<Subcategory> getSubcategoryList() {
            return subcategoryList;
        }

        public void addSubcategory(Subcategory subcategory) {
            this.subcategoryList.add(subcategory);
        }
    }

    private class GetSubcategoriesRespHandler extends DefaultHandler {

        GetSubcategoriesResp resp;

        public GetSubcategoriesRespHandler(GetSubcategoriesResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_SUBCATEGORY)) {
                Subcategory curSubcategory = new Subcategory();
                try {
                    curSubcategory.setId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                curSubcategory.setName(attrs.getValue(ATTRNAME_TITLE));
                resp.addSubcategory(curSubcategory);
            }
        }
    }

    /***********************************************************************
     * smugmug.albums.get
     ***********************************************************************/
    public class Album {

        private int id;

        private String key;

        private String title;

        private int categoryId;

        private String categoryName;

        private int subcategoryId;

        private String subcategoryName;

        public int getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public int getSubcategoryId() {
            return subcategoryId;
        }

        public String getSubcategoryName() {
            return subcategoryName;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setCategoryId(int categoryId) {
            this.categoryId = categoryId;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public void setSubcategoryId(int subcategoryId) {
            this.subcategoryId = subcategoryId;
        }

        public void setSubcategoryName(String subcategoryName) {
            this.subcategoryName = subcategoryName;
        }
    }

    public GetAlbumsResp getAlbums() {
        String method = "smugmug.albums.get";
        GetAlbumsResp resp = new GetAlbumsResp();
        assertLoggedIn();
        String[] params = { "method", method, "SessionID", this.sessionId };
        callApi(new Connection(HTTP_BASE_URL, params, this.debug), new GetAlbumsRespHandler(resp));
        if (this.debug) {
            System.out.println(method + " respstat " + resp.getRespStat());
        }
        return resp;
    }

    public class GetAlbumsResp extends Resp {

        private List<Album> albumList;

        public GetAlbumsResp() {
            this.albumList = new LinkedList<Album>();
        }

        public List<Album> getAlbumList() {
            return albumList;
        }

        public void addAlbum(Album album) {
            this.albumList.add(album);
        }
    }

    private class GetAlbumsRespHandler extends DefaultHandler {

        GetAlbumsResp resp;

        Album curAlbum;

        public GetAlbumsRespHandler(GetAlbumsResp resp) {
            this.resp = resp;
            this.curAlbum = null;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_ALBUM)) {
                curAlbum = new Album();
                try {
                    curAlbum.setId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                curAlbum.setKey(attrs.getValue(ATTRNAME_KEY));
                curAlbum.setTitle(attrs.getValue(ATTRNAME_TITLE));
            } else if (qname.equals(ELEMENT_CATEGORY)) {
                try {
                    curAlbum.setCategoryId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                curAlbum.setCategoryName(attrs.getValue(ATTRNAME_NAME));
            } else if (qname.equals(ELEMENT_SUBCATEGORY)) {
                try {
                    curAlbum.setSubcategoryId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                curAlbum.setSubcategoryName(attrs.getValue(ATTRNAME_NAME));
            }
        }

        public void endElement(String uri, String localname, String qname) {
            if (qname.equals(ELEMENT_ALBUM)) {
                resp.addAlbum(this.curAlbum);
                curAlbum = null;
            }
        }
    }

    /***********************************************************************
     * Photo upload
     *
     * Photo upload is "special" as we use HTTP PUT to send the photo
     * as recommended by SmugMug for performance reasons, in particular
     * so that we can send the photo in binary format.  Thus we do not
     * use the common "class Connection" below but similar things manually.
     ***********************************************************************/
    public class UploadResp extends Resp {

        private int photoId;

        private String key;

        private String url;

        public int getPhotoId() {
            return photoId;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public void setPhotoId(int id) {
            this.photoId = photoId;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public boolean uploadPhoto(File photoFile, int albumId) {
        boolean status = true;
        UploadResp resp = new UploadResp();
        assertLoggedIn();
        String fileName = photoFile.getName();
        String fileLength = Long.toString(photoFile.length());
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (Exception e) {
            System.out.println("uploadPhoto exception: " + e.toString());
            return false;
        }
        if (this.debug) {
            System.out.println("uploadPhoto: " + fileName + " " + encodedFileName + " filelen " + fileLength);
        }
        try {
            URL url = new URL(UPLOAD_URL + fileName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Length", fileLength);
            conn.setRequestProperty("X-Smug-SessionID", this.sessionId);
            conn.setRequestProperty("X-Smug-Version", API_VERSION);
            conn.setRequestProperty("X-Smug-ResponseType", "REST");
            conn.setRequestProperty("X-Smug-AlbumID", Integer.toString(albumId));
            conn.setRequestProperty("X-Smug-FileName", encodedFileName);
            FileInputStream photoStream = new FileInputStream(photoFile);
            MessageDigest md5summer;
            try {
                md5summer = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.toString());
                return false;
            }
            int numRead;
            int totalBytes = 0;
            while ((numRead = photoStream.read(photoBuffer)) != -1) {
                md5summer.update(photoBuffer, 0, numRead);
                totalBytes += numRead;
            }
            byte[] md5sum = md5summer.digest();
            String md5sumStr = "";
            for (int i = 0; i < md5sum.length; i++) {
                md5sumStr += Integer.toHexString((md5sum[i] & 0xff) + 0x100).substring(1);
            }
            conn.setRequestProperty("Content-MD5", md5sumStr);
            if (this.debug) {
                System.out.println("MD5SUM: " + md5sumStr);
            }
            OutputStream connStream = conn.getOutputStream();
            if (numRead == totalBytes) {
                connStream.write(photoBuffer, 0, totalBytes);
            } else {
                photoStream = new FileInputStream(photoFile);
                while ((numRead = photoStream.read(photoBuffer)) != -1) {
                    connStream.write(photoBuffer, 0, numRead);
                }
            }
            connStream.flush();
            try {
                parseResp(conn.getInputStream(), new UploadRespHandler(resp));
                status = resp.isOk();
            } catch (Exception e) {
                if (this.debug) {
                    System.out.println("exception processing upload response: " + e.toString());
                }
                status = false;
            }
            if (this.debug) {
                System.out.println("HTTP Response Code: " + conn.getResponseCode());
                System.out.println("HTTP Response Message: " + conn.getResponseMessage());
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.toString());
            status = false;
        }
        if (this.debug) {
            System.out.println("uploadPhoto: " + fileName + " status " + status);
        }
        return status;
    }

    private class UploadRespHandler extends DefaultHandler {

        UploadResp resp;

        public UploadRespHandler(UploadResp resp) {
            this.resp = resp;
        }

        public void startElement(String uri, String localname, String qname, Attributes attrs) {
            if (qname.equals(ELEMENT_RSP)) {
                resp.setRespStat(attrs.getValue(ATTRNAME_STAT));
            } else if (qname.equals(ELEMENT_IMAGE)) {
                try {
                    resp.setPhotoId(Integer.parseInt(attrs.getValue(ATTRNAME_ID)));
                } catch (NumberFormatException e) {
                    resp.setParseError();
                }
                resp.setKey(attrs.getValue(ATTRNAME_KEY));
                resp.setUrl(attrs.getValue(ATTRNAME_URL));
            }
        }
    }

    /***********************************************************************
     * Private Support Functions
     ***********************************************************************/
    private void assertLoggedIn() {
        if (this.sessionId == null) {
            throw new Error("internal error: not logged in");
        }
    }

    private void callApi(Connection conn, DefaultHandler handler) {
        if (conn.connect()) {
            try {
                parseResp(conn.getInputStream(), handler);
            } catch (Exception e) {
                if (this.debug) {
                    System.out.println("exception processing response: " + e.toString());
                }
            }
        }
    }

    private void parseResp(InputStream inputStream, DefaultHandler handler) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(inputStream, handler);
    }

    private void dumpXml(String qname, Attributes attrs) {
        System.out.println("qname: " + qname);
        for (int i = 0; i < attrs.getLength(); i++) {
            System.out.println("\t" + attrs.getQName(i) + " = " + attrs.getValue(i));
        }
    }

    private String apiBooleanValue(boolean val) {
        return (val) ? "1" : "0";
    }
}

/***************************************************************************
 * Support class for managing server connections
 ***************************************************************************/
class Connection {

    private URL url;

    private URLConnection conn;

    private InputStream inputStream;

    private String urlStr;

    private boolean debug;

    public Connection(String baseUrl, String[] params, boolean debug) {
        this.debug = debug;
        if ((params.length % 2) != 0) {
            throw new Error("internal error: odd-length param list");
        }
        this.urlStr = baseUrl;
        for (int i = 0; i < params.length; i += 2) {
            if (i == 0) {
                this.urlStr += "?" + urlParam(params[i], params[i + 1]);
            } else {
                this.urlStr += "&" + urlParam(params[i], params[i + 1]);
            }
        }
    }

    public boolean connect() {
        if (this.debug) {
            String debugStr = this.urlStr.replaceAll("&Password=[^&]*", "&Password=xxx");
            System.out.println("Connect: " + debugStr);
        }
        try {
            this.url = new URL(this.urlStr);
            this.conn = this.url.openConnection();
            this.conn.setDoOutput(true);
            this.conn.setDoInput(true);
            this.inputStream = conn.getInputStream();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public String toString() {
        return this.urlStr;
    }

    private String urlParam(String name, String value) {
        try {
            return URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error("internal error: URL encoding: " + e.toString());
        }
    }
}
