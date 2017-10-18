package subget.osdb;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;
import redstone.xmlrpc.util.Base64;
import subget.ClientHttpRequest;
import subget.Global;
import subget.Language;
import subget.Logging;
import subget.SubtitleFile;
import subget.bundles.Bundles;
import subget.exceptions.BadLoginException;
import subget.exceptions.LanguageNotSupportedException;
import subget.exceptions.OsdbException;
import subget.exceptions.TimeoutException;
import subget.exceptions.SubtitlesNotFoundException;
import subget.osdb.params.TryUploadXmlRpcParam;
import subget.osdb.params.UploadXmlRpcParam;
import subget.osdb.responses.MovieInfoXmlRpcResponse;

public abstract class Osdb {

    public static enum AddUserResponse {

        OSDB_ADD_USER_OK, OSDB_ADD_USER_LOGIN_EXISTS, OSDB_ADD_USER_EMAIL_EXISTS, OSDB_ADD_USER_BAD_LOGIN, OSDB_ADD_USER_BAD_PASS, OSDB_ADD_USER_BAD_EMAIL, OSDB_ADD_USER_BAD_UNKNOWN
    }

    private static final int HASH_CHUNK_SIZE = 64 * 1024;

    private static XmlRpcClient client = null;

    private static String token = "";

    public static boolean isLoggedIn() throws InterruptedException, TimeoutException, XmlRpcException, XmlRpcFault {
        if (client == null) {
            return false;
        } else {
            XmlRpcStruct struct = (XmlRpcStruct) client.invoke("NoOperation", new String[] { token });
            if (struct.getString("status").equals("200 OK") == false) {
                return false;
            } else {
                return true;
            }
        }
    }

    public static void logOut() throws XmlRpcException, XmlRpcFault, TimeoutException, InterruptedException {
        if (isLoggedIn()) {
            client.invoke("LogOut", new Object[] { token });
            token = "";
        }
    }

    public static boolean userLogIn() throws InterruptedException, BadLoginException, TimeoutException, XmlRpcException {
        try {
            boolean logged = false;
            if (Global.getOsdbUserName().equals("") || Global.getOsdbUserPass().equals("")) {
                if (Global.getOsdbSessionUserName().equals("") || Global.getOsdbSessionUserPass().equals("")) {
                    if (isLoggedIn()) {
                        logOut();
                    }
                    do {
                        String user = "";
                        if (Global.getOsdbSessionUserName().equals("") == false) {
                            user = Global.getOsdbSessionUserName();
                        } else {
                            user = Global.getOsdbUserName();
                        }
                        String[] login = Global.dialogs.showUserPasswordDialog(Global.SubDataBase.BASE_OSDB, user);
                        if (login == null) {
                            throw (new BadLoginException("Could not log in to OSDb."));
                        } else {
                            Global.setOsdbSessionUserName(login[0]);
                            logged = Osdb.logIn(login[0], login[1]);
                        }
                        if (logged == false) {
                            Global.dialogs.showErrorDialog(Bundles.subgetBundle.getString("Login_failed."), Bundles.subgetBundle.getString("Wrong_user/password."));
                        } else {
                            Global.setOsdbSessionUserPass(login[1]);
                        }
                    } while (logged == false);
                }
            } else {
                Global.setOsdbSessionUserName(Global.getOsdbUserName());
                Global.setOsdbSessionUserPass(Global.getOsdbUserPass());
                String user = Global.getOsdbSessionUserName();
                String pass = Global.getOsdbSessionUserPass();
                logged = Osdb.logIn(user, pass);
                while (logged == false) {
                    Global.dialogs.showErrorDialog(Bundles.subgetBundle.getString("Login_failed."), Bundles.subgetBundle.getString("Wrong_user/password."));
                    String[] login = Global.dialogs.showUserPasswordDialog(Global.SubDataBase.BASE_OSDB, user);
                    if (login == null) {
                        throw (new BadLoginException("Could not log in to OSDb."));
                    } else {
                        Global.setOsdbSessionUserName(login[0]);
                        logged = Osdb.logIn(login[0], login[1]);
                    }
                    if (logged) {
                        Global.setOsdbSessionUserPass(login[1]);
                    }
                }
            }
            return logged;
        } catch (TimeoutException ex) {
            throw new TimeoutException("Could not log in to OSDb, timeout.", ex.getCause());
        } catch (XmlRpcException ex) {
            throw new XmlRpcException("Could not log in to OSDb, connection error.", ex.getCause());
        } catch (XmlRpcFault ex) {
            throw new XmlRpcException("Could not log in to OSDb, connection error.", ex.getCause());
        }
    }

    public static boolean anonymousLogIn() throws XmlRpcException, XmlRpcFault, TimeoutException, InterruptedException {
        return (logIn("", ""));
    }

    private static boolean logIn(String user, String pass) throws XmlRpcException, XmlRpcFault, TimeoutException, InterruptedException {
        try {
            if (client == null) {
                client = new XmlRpcClient("http://www.opensubtitles.org/xml-rpc", false);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Logging.logger.fine(Bundles.subgetBundle.getString("Logging_in_to_opensubtitles.org..."));
        XmlRpcStruct struct = (XmlRpcStruct) client.invoke("LogIn", new String[] { user, pass, "", Global.USER_AGENT });
        if (struct.getString("status").indexOf("401") == 0) {
            Logging.logger.severe(Bundles.subgetBundle.getString("Login_failed,_wrong_user/password."));
            return false;
        } else {
            Logging.logger.fine(Bundles.subgetBundle.getString("Login_OK"));
            token = struct.getString("token");
            return true;
        }
    }

    public static String getOsdbHash(File file) throws IOException {
        long size = file.length();
        long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);
        FileChannel fileChannel = new FileInputStream(file).getChannel();
        long head = computeHashForChunk(fileChannel, 0, chunkSizeForFile);
        long tail = computeHashForChunk(fileChannel, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile);
        fileChannel.close();
        return String.format("%016x", size + head + tail);
    }

    private static long computeHashForChunk(FileChannel fileChannel, long start, long size) throws IOException {
        MappedByteBuffer byteBuffer = fileChannel.map(MapMode.READ_ONLY, start, size);
        LongBuffer longBuffer = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        long hash = 0;
        while (longBuffer.hasRemaining()) {
            hash += longBuffer.get();
        }
        return hash;
    }

    public static AddUserResponse osdbUserAdd(String user, String pass, String email) throws IOException, TimeoutException, InterruptedException {
        if (user.matches("^[a-zA-Z0-9_-]{3,20}$") == false) {
            return AddUserResponse.OSDB_ADD_USER_BAD_LOGIN;
        }
        if (pass.equals("")) {
            return AddUserResponse.OSDB_ADD_USER_BAD_PASS;
        }
        if (email.matches("^[a-zA-Z0-9\\-\\_]{1,30}@[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)+$") == false) {
            return AddUserResponse.OSDB_ADD_USER_BAD_EMAIL;
        }
        URLConnection conn = null;
        ClientHttpRequest httpPost = null;
        InputStreamReader responseStream = null;
        URL url = new URL("http://www.opensubtitles.org/en/newuser");
        String response = "";
        String line;
        conn = url.openConnection(Global.getProxy());
        httpPost = new ClientHttpRequest(conn);
        httpPost.setParameter("email", "");
        httpPost.setParameter("password", "");
        httpPost.setParameter("UserNickname", user);
        httpPost.setParameter("UserMail", email);
        httpPost.setParameter("UserPassword", pass);
        httpPost.setParameter("UserPassword2", pass);
        httpPost.setParameter("Terms", "on");
        httpPost.setParameter("action", "newuser");
        responseStream = new InputStreamReader(httpPost.post(), "UTF-8");
        BufferedReader responseReader = new BufferedReader(responseStream);
        while ((line = responseReader.readLine()) != null) {
            response += line;
        }
        int index = response.indexOf("<div class=\"msg error\">");
        if (index == -1) {
            return AddUserResponse.OSDB_ADD_USER_OK;
        }
        int index2 = response.indexOf("</div>", index);
        if (index2 == -1) {
            return AddUserResponse.OSDB_ADD_USER_BAD_UNKNOWN;
        }
        response = response.substring(index + 23, index2);
        response = response.replace("<br />", "\n");
        response = response.replaceAll("<.{1,4}>", "");
        if (response.indexOf("is already taken") != -1) {
            return AddUserResponse.OSDB_ADD_USER_LOGIN_EXISTS;
        }
        if (response.indexOf("is already being used") != -1) {
            return AddUserResponse.OSDB_ADD_USER_EMAIL_EXISTS;
        }
        return AddUserResponse.OSDB_ADD_USER_BAD_UNKNOWN;
    }

    public static boolean tryUploadSubtitles(TryUploadXmlRpcParam param) throws XmlRpcException, TimeoutException, XmlRpcFault, BadLoginException, OsdbException, InterruptedException {
        if (!isLoggedIn()) {
            if (Global.getOsdbUploadLogin()) {
                userLogIn();
            } else {
                anonymousLogIn();
            }
        }
        XmlRpcStruct response = (XmlRpcStruct) client.invoke("TryUploadSubtitles", new Object[] { token, param });
        String status = response.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        } else {
            return (response.getInteger("alreadyindb") == 1 ? false : true);
        }
    }

    public static void uploadSubtitles(UploadXmlRpcParam param) throws XmlRpcException, XmlRpcFault, TimeoutException, BadLoginException, OsdbException, InterruptedException {
        if (!isLoggedIn()) {
            if (Global.getOsdbUploadLogin()) {
                userLogIn();
            } else {
                anonymousLogIn();
            }
        }
        XmlRpcStruct response = (XmlRpcStruct) client.invoke("UploadSubtitles", new Object[] { token, param });
        String status = response.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        }
    }

    public static MovieInfoXmlRpcResponse checkMovieHash(String hash) throws XmlRpcException, XmlRpcFault, TimeoutException, OsdbException, BadLoginException, InterruptedException {
        if (!isLoggedIn()) {
            anonymousLogIn();
        }
        XmlRpcArray param = new XmlRpcArray();
        param.add(hash);
        XmlRpcStruct response = (XmlRpcStruct) client.invoke("CheckMovieHash", new Object[] { token, param });
        String status = response.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        }
        Object object = response.getStruct("data").get(hash);
        if (object instanceof XmlRpcStruct) {
            XmlRpcStruct dataStruct = XmlRpcStruct.class.cast(object);
            MovieInfoXmlRpcResponse returnStruct = new MovieInfoXmlRpcResponse();
            returnStruct.setMovieImdbId(dataStruct.getString("MovieImdbID"));
            returnStruct.setMovieName(dataStruct.getString("MovieName"));
            returnStruct.setMovieYear(dataStruct.getString("MovieYear"));
            return returnStruct;
        }
        return null;
    }

    public static MovieInfoXmlRpcResponse getIMDBMovieDetails(String id) throws XmlRpcException, XmlRpcFault, TimeoutException, OsdbException, BadLoginException, InterruptedException {
        if (!isLoggedIn()) {
            anonymousLogIn();
        }
        XmlRpcStruct response = ((XmlRpcStruct) client.invoke("GetIMDBMovieDetails", new Object[] { token, id }));
        String status = response.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        }
        response = response.getStruct("data");
        MovieInfoXmlRpcResponse returnStruct = new MovieInfoXmlRpcResponse();
        returnStruct.setMovieName(response.getString("title"));
        returnStruct.setMovieYear(response.getString("year"));
        return returnStruct;
    }

    public static MovieInfoXmlRpcResponse[] searchMoviesOnIMDB(String query) throws XmlRpcFault, TimeoutException, XmlRpcException, BadLoginException, OsdbException, InterruptedException {
        if (!isLoggedIn()) {
            anonymousLogIn();
        }
        XmlRpcStruct responseStruct = ((XmlRpcStruct) client.invoke("SearchMoviesOnIMDB", new Object[] { token, query }));
        String status = responseStruct.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        }
        XmlRpcArray response = responseStruct.getArray("data");
        MovieInfoXmlRpcResponse[] returnArray;
        if (response.getStruct(0).get("id") == null) {
            returnArray = new MovieInfoXmlRpcResponse[0];
            return returnArray;
        }
        returnArray = new MovieInfoXmlRpcResponse[response.size()];
        for (int i = 0; i < returnArray.length; ++i) {
            returnArray[i] = new MovieInfoXmlRpcResponse();
            XmlRpcStruct responseItem = response.getStruct(i);
            returnArray[i].setId(responseItem.getString("id"));
            returnArray[i].setMovieName(responseItem.getString("title"));
        }
        return returnArray;
    }

    public static ArrayList<SubtitleFile> searchSubtitles(String movieHash, long movieBytesize, String[] languages, String outputSubsFileName, File outputSubsDir) throws XmlRpcException, BadLoginException, XmlRpcFault, TimeoutException, SubtitlesNotFoundException, InterruptedException {
        if (!isLoggedIn()) {
            if (Global.getOsdbDownloadLogin()) {
                userLogIn();
            } else {
                anonymousLogIn();
            }
        }
        XmlRpcArray arrayParam = new XmlRpcArray();
        for (int i = 0; i < languages.length; ++i) {
            XmlRpcStruct struct = new XmlRpcStruct();
            struct.put("sublanguageid", Language.xxToxxx(languages[i]));
            struct.put("moviehash", movieHash);
            struct.put("moviebytesize", movieBytesize);
            arrayParam.add(struct);
        }
        XmlRpcStruct response = (XmlRpcStruct) client.invoke("SearchSubtitles", new Object[] { token, arrayParam });
        Object obj = response.get("data");
        if (obj instanceof Boolean && Boolean.class.cast(obj).equals(false)) {
            throw new SubtitlesNotFoundException();
        } else {
            ArrayList<SubtitleFile> subs = new ArrayList<SubtitleFile>(5);
            XmlRpcArray subArray = response.getArray("data");
            for (int i = 0; i < subArray.size(); ++i) {
                String language = subArray.getStruct(i).getString("ISO639");
                SubtitleFile sub = new SubtitleFile(outputSubsFileName, outputSubsDir.getAbsolutePath(), language);
                sub.setFromBase(Global.SubDataBase.BASE_OSDB);
                sub.setOsdbId(subArray.getStruct(i).getString("IDSubtitleFile"));
                sub.setUrl(subArray.getStruct(i).getString("SubDownloadLink"));
                sub.setFormatFromOSDb(subArray.getStruct(i).getString("SubFormat"));
                sub.setDisplayName(subArray.getStruct(i).getString("SubFileName"));
                String uploader = subArray.getStruct(i).getString("UserNickName");
                if (uploader.equals("") == false) {
                    sub.setUploader(uploader);
                }
                String ratingStr = subArray.getStruct(i).getString("SubRating");
                if (ratingStr.equals("0.0") == false) {
                    if (ratingStr.endsWith(".0")) {
                        ratingStr = ratingStr.substring(0, ratingStr.length() - 2);
                    }
                    sub.setRating(ratingStr + "/10");
                }
                subs.add(sub);
            }
            return subs;
        }
    }

    public static void downloadSubtitle(SubtitleFile sub, int count) throws XmlRpcException, BadLoginException, XmlRpcFault, TimeoutException, OsdbException, IOException, InterruptedException {
        if (!isLoggedIn()) {
            if (Global.getOsdbDownloadLogin()) {
                userLogIn();
            } else {
                anonymousLogIn();
            }
        }
        XmlRpcArray array = new XmlRpcArray();
        array.add(sub.getOsdbId());
        XmlRpcStruct response = (XmlRpcStruct) client.invoke("DownloadSubtitles", new Object[] { token, array });
        String status = response.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        }
        String content = response.getArray("data").getStruct(0).getString("data");
        byte[] decoded = redstone.xmlrpc.util.Base64.decode(content.getBytes());
        ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
        GZIPInputStream gzis = new GZIPInputStream(bis);
        FileOutputStream fos = new FileOutputStream(Global.getPathToTmpDir() + "/osdb" + String.valueOf(count) + ".txt");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = gzis.read(buffer)) != -1) {
            bos.write(buffer, 0, numRead);
        }
        gzis.close();
        bos.close();
        sub.setFile(new File(Global.getPathToTmpDir() + "/osdb" + String.valueOf(count) + ".txt"));
    }

    public static String detectLanguage(File textFile) throws XmlRpcException, XmlRpcFault, TimeoutException, FileNotFoundException, IOException, NoSuchAlgorithmException, LanguageNotSupportedException, OsdbException, InterruptedException {
        if (!isLoggedIn()) {
            anonymousLogIn();
        }
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ByteArrayOutputStream byteTmp = new ByteArrayOutputStream();
        DeflaterOutputStream gzout = new DeflaterOutputStream(byteOut);
        FileInputStream fis = new FileInputStream(textFile);
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = fis.read(buffer)) != -1) {
            byteTmp.write(buffer, 0, numRead);
        }
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(byteTmp.toByteArray());
        byte[] hash = md5.digest();
        StringBuffer hexString = new StringBuffer();
        for (int j = 0; j < hash.length; j++) {
            String hexPart;
            hexPart = Integer.toHexString(0xFF & hash[j]);
            if (hexPart.length() == 1) {
                hexPart = "0" + hexPart;
            }
            hexString.append(hexPart);
        }
        String stringHash = hexString.toString();
        gzout.write(byteTmp.toByteArray());
        gzout.finish();
        gzout.close();
        char[] base = Base64.encode(byteOut.toByteArray());
        String base64String = new String(base);
        XmlRpcArray array = new XmlRpcArray();
        array.add(base64String);
        XmlRpcStruct responseStruct = ((XmlRpcStruct) client.invoke("DetectLanguage", new Object[] { token, array }));
        String status = responseStruct.getString("status");
        if (status.indexOf("20") != 0) {
            throw new OsdbException(status);
        }
        String lang = responseStruct.getStruct("data").getString(stringHash);
        return Language.xxxToxx(lang);
    }
}
