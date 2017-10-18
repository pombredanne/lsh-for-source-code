package subget;

import subget.osdb.Osdb;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.util.Base64;
import subget.bundles.Bundles;
import subget.exceptions.BadLoginException;
import subget.exceptions.BrokenAviHeaderException;
import subget.exceptions.NotSupportedContainerException;
import subget.exceptions.OsdbException;
import subget.exceptions.SevenZipException;
import subget.exceptions.TimeoutException;
import subget.osdb.params.BaseInfoXmlRpcParam;
import subget.osdb.params.CdXmlRpcParam;
import subget.osdb.responses.MovieInfoXmlRpcResponse;
import subget.osdb.params.TryUploadXmlRpcParam;
import subget.osdb.params.UploadXmlRpcParam;

/**
 *
 * @author povder
 */
public class Movie {

    String subLanguage = Global.getDefaultUploadedLanguage();

    ArrayList<String> imdbIDs = new ArrayList<String>(3);

    ArrayList<String> imdbNames = new ArrayList<String>(3);

    String displayName;

    ArrayList<VSpair> vspairs = new ArrayList<VSpair>(3);

    String release = "";

    String comments = "";

    boolean detectLanguage = true;

    int selectedImdb = -1;

    boolean uploadOsdb = true;

    boolean uploadNapi = true;

    boolean napiCorrection = false;

    private ArrayList<String> napiWarnings = new ArrayList<String>(5);

    private ArrayList<String> osdbWarnings = new ArrayList<String>(5);

    public boolean getNapiCorrection() {
        return napiCorrection;
    }

    public void setNapiCorrection(boolean napiCorrection) {
        this.napiCorrection = napiCorrection;
    }

    public void napiUploadSubtitles() throws InterruptedException {
        if (uploadNapi) {
            Logging.logger.finer(Bundles.subgetBundle.getString("Starting_upload_to_NAPI..."));
            for (int i = 0; i < vspairs.size(); ++i) {
                try {
                    VideoFile vid = vspairs.get(i).getVideo();
                    SubtitleFile sub = vspairs.get(i).getSubtitle();
                    if (vid.getNapiHash().equals("")) {
                        vid.setNapiHash();
                    }
                    if (sub.getNapiMd5Sum().equals("")) {
                        sub.setNapiMd5sum();
                    }
                    sub.setNapiMd5sum();
                    sub.napiPack(vid.getNapiMd5sum());
                    if (vid.getHasInfo() == false) {
                        try {
                            vid.getVideoInfo();
                        } catch (BrokenAviHeaderException e) {
                        } catch (NotSupportedContainerException e) {
                        }
                    }
                    URLConnection conn = null;
                    ClientHttpRequest httpPost = null;
                    InputStreamReader responseStream = null;
                    URL url;
                    if (vid.getHasInfo()) {
                        url = new URL("http://www.napiprojekt.pl/unit_napisy/upload.php?" + "m_length=" + vid.getTimeString() + "&m_resolution=" + vid.getResolutionString() + "&m_fps=" + vid.getFpsString() + "&m_hash=" + vid.getNapiMd5sum() + "&m_filesize=" + vid.getFile().length());
                    } else {
                        url = new URL("http://www.napiprojekt.pl/unit_napisy/upload.php?" + "&m_hash=" + vid.getNapiMd5sum() + "&m_filesize=" + vid.getFile().length());
                    }
                    conn = url.openConnection(Global.getProxy());
                    conn.setRequestProperty("User-Agent", Global.USER_AGENT);
                    httpPost = new ClientHttpRequest(conn);
                    httpPost.setParameter("nick", Global.getNapiSessionUserName());
                    httpPost.setParameter("pass", Global.getNapiSessionUserPass());
                    httpPost.setParameter("l", subLanguage);
                    httpPost.setParameter("m_filename", vid.getFile().getName());
                    httpPost.setParameter("t", vid.getNapiHash());
                    httpPost.setParameter("s_hash", sub.getNapiMd5Sum());
                    httpPost.setParameter("v", "other");
                    httpPost.setParameter("kmt", comments);
                    httpPost.setParameter("poprawka", napiCorrection ? "true" : "false");
                    httpPost.setParameter("MAX_FILE_SIZE", "512000");
                    httpPost.setParameter("plik", sub.getCompressedFile(), "subtitles/zip");
                    responseStream = new InputStreamReader(httpPost.post(), "Cp1250");
                    BufferedReader responseReader = new BufferedReader(responseStream);
                    String response = responseReader.readLine();
                    if (response.indexOf("NPc0") != 0 && response.indexOf("NPc2") != 0 && response.indexOf("NPc3") != 0) {
                        napiWarnings.add("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_Could_not_upload_subtitles_to_NAPI_database."));
                    }
                    sub.getCompressedFile().delete();
                } catch (SevenZipException ex) {
                    napiWarnings.add("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_7zip_error._Could_not_upload_subtitles_to_NAPI_database."));
                    Logging.logger.severe("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_7zip_error._Could_not_upload_subtitles_to_NAPI_database."));
                } catch (IOException ex) {
                    napiWarnings.add("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_Connection_error._Could_not_upload_subtitles_to_NAPI_database."));
                    Logging.logger.severe("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_Connection_error._Could_not_upload_subtitles_to_NAPI_database."));
                } catch (NoSuchAlgorithmException ex) {
                    napiWarnings.add("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_MD5_error._Could_not_upload_subtitles_to_NAPI_database."));
                    Logging.logger.severe("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_MD5_error._Could_not_upload_subtitles_to_NAPI_database."));
                } catch (TimeoutException ex) {
                    napiWarnings.add("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_Timeout_error._Could_not_upload_subtitles_to_NAPI_database."));
                    Logging.logger.severe("CD" + String.valueOf(i + 1) + Bundles.subgetBundle.getString(":_Timeout_error._Could_not_upload_subtitles_to_NAPI_database."));
                }
            }
        }
    }

    public ArrayList<String> getNapiWarnings() {
        return napiWarnings;
    }

    public ArrayList<String> getOsdbWarnings() {
        return osdbWarnings;
    }

    public void osdbUploadSubtitles() throws InterruptedException {
        if (uploadOsdb) {
            Logging.logger.finer(Bundles.subgetBundle.getString("Starting_upload_to_OSDb..."));
            CdXmlRpcParam[] cds = new CdXmlRpcParam[vspairs.size()];
            TryUploadXmlRpcParam tryParam = new TryUploadXmlRpcParam();
            UploadXmlRpcParam uploadParam = new UploadXmlRpcParam();
            for (int i = 0; i < cds.length; ++i) {
                FileInputStream fis = null;
                try {
                    byte[] buffer = new byte[1024];
                    fis = new FileInputStream(vspairs.get(i).getSubtitle().getFile());
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    int numread = 0;
                    while ((numread = fis.read(buffer)) != -1) {
                        md5.update(buffer, 0, numread);
                    }
                    fis.close();
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
                    String subhash = hexString.toString();
                    int movietimems = 0;
                    long movieframes = 0;
                    float moviefps = 0;
                    if (vspairs.get(i).getVideo().getHasInfo() == false) {
                        try {
                            vspairs.get(i).getVideo().getVideoInfo();
                        } catch (IOException ex) {
                        } catch (BrokenAviHeaderException ex) {
                        } catch (NotSupportedContainerException ex) {
                        }
                        if (vspairs.get(i).getVideo().getHasInfo()) {
                            movietimems = vspairs.get(i).getVideo().getTime();
                            movieframes = vspairs.get(i).getVideo().getFrames();
                            moviefps = vspairs.get(i).getVideo().getFps();
                        }
                    }
                    cds[i] = new CdXmlRpcParam();
                    cds[i].putSubHash(subhash);
                    cds[i].putSubFilename(vspairs.get(i).getSubtitle().getFile().getName());
                    cds[i].putMovieHash(vspairs.get(i).getVideo().getOsdbHash());
                    cds[i].putMovieBytesize(vspairs.get(i).getVideo().getFile().length());
                    cds[i].putMovieTimeMs(movietimems);
                    cds[i].putMovieFrames(movieframes);
                    cds[i].putMovieFps(moviefps);
                    cds[i].putMovieFilename(vspairs.get(i).getVideo().getFile().getName());
                } catch (IOException ex) {
                    osdbWarnings.add(Bundles.subgetBundle.getString("I/O_error_occured_while_uploading_subtitles."));
                    Logging.logger.severe(Bundles.subgetBundle.getString("I/O_error_occured_while_uploading_subtitles."));
                    return;
                } catch (NoSuchAlgorithmException ex) {
                    osdbWarnings.add(Bundles.subgetBundle.getString("Hashing_error_occured_while_uploading_subtitles."));
                    Logging.logger.severe(Bundles.subgetBundle.getString("Hashing_error_occured_while_uploading_subtitles."));
                    return;
                } finally {
                    try {
                        if (fis != null) fis.close();
                    } catch (IOException ex) {
                        osdbWarnings.add(Bundles.subgetBundle.getString("I/O_error_occured_while_uploading_subtitles."));
                        Logging.logger.severe(Bundles.subgetBundle.getString("I/O_error_occured_while_uploading_subtitles."));
                        return;
                    }
                }
            }
            for (int i = 0; i < cds.length; ++i) {
                tryParam.putCd(i, cds[i]);
            }
            try {
                boolean newSubtitles;
                try {
                    newSubtitles = Osdb.tryUploadSubtitles(tryParam);
                } catch (OsdbException ex) {
                    Global.dialogs.showErrorDialog(Bundles.subgetBundle.getString("Error"), Bundles.subgetBundle.getString("Could_not_upload_subtitles.\n") + ex.getDialogMessage());
                    return;
                }
                if (newSubtitles) {
                    BaseInfoXmlRpcParam baseinfo = new BaseInfoXmlRpcParam();
                    baseinfo.putIdMovieImdb(imdbIDs.get(selectedImdb));
                    baseinfo.putMovieReleaseName(release);
                    baseinfo.putSubLanguageId(Language.xxToxxx(subLanguage));
                    baseinfo.putSubAuthorComment(comments);
                    uploadParam.putBaseInfo(baseinfo);
                    for (int j = 0; j < cds.length; ++j) {
                        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                        DeflaterOutputStream gzout = new DeflaterOutputStream(byteOut);
                        FileInputStream fileIn = new FileInputStream(vspairs.get(j).getSubtitle().getFile());
                        byte[] buffer = new byte[1024];
                        int numRead;
                        while ((numRead = fileIn.read(buffer)) != -1) {
                            gzout.write(buffer, 0, numRead);
                        }
                        gzout.finish();
                        gzout.close();
                        char[] base = Base64.encode(byteOut.toByteArray());
                        String base64String = new String(base);
                        cds[j].putSubContent(base64String);
                        uploadParam.putCd(j, cds[j]);
                    }
                    try {
                        Osdb.uploadSubtitles(uploadParam);
                    } catch (OsdbException ex) {
                        osdbWarnings.add(ex.getDialogMessage());
                        Logging.logger.warning(ex.getDialogMessage());
                        return;
                    }
                } else {
                    osdbWarnings.add(Bundles.subgetBundle.getString("Subtitles_already_are_in_database."));
                    Logging.logger.warning(Bundles.subgetBundle.getString("Subtitles_already_are_in_database."));
                    return;
                }
            } catch (BadLoginException ex) {
                Logger.getLogger(Movie.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                osdbWarnings.add(Bundles.subgetBundle.getString("Connection_error_occured_while_compressing_subtitles."));
                Logging.logger.severe(Bundles.subgetBundle.getString("Connection_error_occured_while_compressing_subtitles."));
            } catch (XmlRpcException ex) {
                osdbWarnings.add(Bundles.subgetBundle.getString("Connection_error_occured_while_uploading_subtitles."));
                Logging.logger.severe(Bundles.subgetBundle.getString("Connection_error_occured_while_compressing_subtitles."));
            } catch (XmlRpcFault ex) {
                osdbWarnings.add(Bundles.subgetBundle.getString("Connection_error_occured_while_uploading_subtitles."));
                Logging.logger.severe(Bundles.subgetBundle.getString("Connection_error_occured_while_compressing_subtitles."));
            } catch (TimeoutException ex) {
                osdbWarnings.add(Bundles.subgetBundle.getString("Connection_error_occured_while_uploading_subtitles,_timeout."));
                Logging.logger.severe(Bundles.subgetBundle.getString("Connection_error_occured_while_compressing_subtitles,_timeout"));
            }
        }
    }

    public boolean getUploadNapi() {
        return uploadNapi;
    }

    public boolean getUploadOsdb() {
        return uploadOsdb;
    }

    public void disableNapi() {
        uploadNapi = false;
    }

    public void disableOsdb() {
        uploadOsdb = false;
    }

    public void enableNapi() {
        uploadNapi = true;
    }

    public void enableOsdb() {
        uploadOsdb = true;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public int getSelectedImdb() {
        return selectedImdb;
    }

    public void setSelectedImdb(int selectedImdb) {
        this.selectedImdb = selectedImdb;
    }

    public ArrayList<String> getImdbIDs() {
        return imdbIDs;
    }

    public ArrayList<String> getImdbNames() {
        return imdbNames;
    }

    public String getSubLanguage() {
        return subLanguage;
    }

    public void setSubLanguage(String subLanguage) {
        this.subLanguage = subLanguage;
    }

    public Movie(String aDisplayName) {
        displayName = aDisplayName;
    }

    public ArrayList<VSpair> getVspairs() {
        return vspairs;
    }

    public void moveUp(int i) {
        if (i > 0) {
            Collections.swap(vspairs, i, i - 1);
            vspairs.get(i - 1).setCd(vspairs.get(i - 1).getCd() - 1);
            vspairs.get(i).setCd(vspairs.get(i).getCd() + 1);
        }
    }

    public void moveDown(int i) {
        if (i < vspairs.size() - 1) {
            Collections.swap(vspairs, i, i + 1);
            vspairs.get(i + 1).setCd(vspairs.get(i + 1).getCd() + 1);
            vspairs.get(i).setCd(vspairs.get(i).getCd() - 1);
        }
    }

    public void removeVspair(int i) {
        vspairs.remove(i);
        for (int j = i + 1; j < vspairs.size(); ++j) {
            vspairs.get(j).setCd(vspairs.get(j).getCd() - 1);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void detectLanguage() throws FileNotFoundException, IOException {
        if (detectLanguage && Global.getDetectUploadedLanguage()) {
            for (int i = 0; i < vspairs.size(); ++i) {
                if (vspairs.get(i).getSubtitle() != null) {
                    subLanguage = Language.detectLanguage(vspairs.get(i).getSubtitle().getFile());
                    detectLanguage = false;
                    break;
                }
            }
        }
    }

    public void forceDetectLanguage() throws FileNotFoundException, IOException {
        try {
            for (int i = 0; i < vspairs.size(); ++i) {
                if (vspairs.get(i).getSubtitle() != null) {
                    subLanguage = Language.detectLanguage(vspairs.get(i).getSubtitle().getFile());
                    detectLanguage = false;
                    break;
                }
            }
        } catch (FileNotFoundException ex) {
            throw new FileNotFoundException(Bundles.subgetBundle.getString("Could_not_detect_language_of_subtitles_to_some_movies,_could_not_read_a_file."));
        } catch (IOException ex) {
            throw new IOException("Could not detect language of subtitles to some movies, I/O error.", ex.getCause());
        }
    }

    public boolean detectImdb(int index) throws InterruptedException, OsdbException, BadLoginException, TimeoutException, XmlRpcException, IOException {
        try {
            if (vspairs.get(index).getVideo() != null) {
                if (Osdb.isLoggedIn() == false) {
                    if (Osdb.anonymousLogIn() == false) {
                        throw (new BadLoginException(Bundles.subgetBundle.getString("Could_not_detect_IMDb_ID_of_some_files,_could_not_login_to_OSDb.")));
                    }
                }
                vspairs.get(index).getVideo().setOsdbHash();
                String year;
                String name;
                String imdbId;
                Logging.logger.fine(String.format(Bundles.subgetBundle.getString("Detecting_IMDb_ID_of_%s..."), vspairs.get(index).getVideo().getFile().getName()));
                MovieInfoXmlRpcResponse response = Osdb.checkMovieHash(vspairs.get(index).getVideo().getOsdbHash());
                if (response != null) {
                    imdbId = response.getMovieImdbId();
                    name = response.getMovieName();
                    year = response.getMovieYear();
                    for (int i = 0; i < imdbIDs.size(); ++i) {
                        if (imdbIDs.get(i).equals(imdbId)) {
                            Logging.logger.fine(Bundles.subgetBundle.getString("Detected_IMDb_ID_already_on_the_list."));
                            Osdb.logOut();
                            return false;
                        }
                    }
                    Logging.logger.finer(Bundles.subgetBundle.getString("Detected_IMDb_ID:_") + name + " (" + year + ")");
                    imdbIDs.add(imdbId);
                    imdbNames.add(name + " (" + year + ")");
                    displayName = imdbNames.get(0);
                    Osdb.logOut();
                    return true;
                }
            }
            Osdb.logOut();
            return false;
        } catch (OsdbException ex) {
            throw new OsdbException(Bundles.subgetBundle.getString("Could_not_detect_IMDb_ID_of_some_files.\n") + ex.getDialogMessage());
        } catch (TimeoutException ex) {
            throw new TimeoutException(Bundles.subgetBundle.getString("Could_not_detect_IMDb_ID_of_some_files,_timeout."), ex.getCause());
        } catch (XmlRpcException ex) {
            if (ex.getCause() instanceof InterruptedException) {
                throw InterruptedException.class.cast(ex.getCause());
            } else throw new XmlRpcException(Bundles.subgetBundle.getString("Could_not_detect_IMDb_ID_of_some_files,_connection_error."), ex.getCause());
        } catch (XmlRpcFault ex) {
            if (ex.getCause() instanceof InterruptedException) {
                throw InterruptedException.class.cast(ex.getCause());
            } else throw new XmlRpcException(Bundles.subgetBundle.getString("Could_not_detect_IMDb_ID_of_some_files,_connection_error."), ex.getCause());
        } catch (IOException ex) {
            throw new IOException(Bundles.subgetBundle.getString("Could_not_detect_IMDb_ID_of_some_files,_connection_error."), ex.getCause());
        }
    }

    public void addImdbId(String id) throws XmlRpcException, XmlRpcFault, TimeoutException, BadLoginException, OsdbException, InterruptedException {
        if (Osdb.isLoggedIn() == false) {
            if (Osdb.anonymousLogIn() == false) {
                throw new subget.exceptions.BadLoginException("anonymous");
            }
        }
        for (int i = 0; i < imdbIDs.size(); ++i) {
            if (imdbIDs.get(i).equals(id)) {
                Osdb.logOut();
                return;
            }
        }
        MovieInfoXmlRpcResponse response = Osdb.getIMDBMovieDetails(id);
        String name = response.getMovieName();
        String year = response.getMovieYear();
        imdbIDs.add(id);
        imdbNames.add(name + " (" + year + ")");
    }

    public void addImdbId(String id, String name) {
        for (int i = 0; i < imdbIDs.size(); ++i) {
            if (imdbIDs.get(i).equals(id)) {
                return;
            }
        }
        imdbIDs.add(id);
        imdbNames.add(name);
    }

    public static String[][] searchImdbByName(String query) throws XmlRpcException, XmlRpcFault, TimeoutException, BadLoginException, OsdbException, InterruptedException {
        if (Osdb.isLoggedIn() == false) {
            if (Osdb.anonymousLogIn() == false) {
                throw new subget.exceptions.BadLoginException("anonymous");
            }
        }
        String[][] ids = null;
        MovieInfoXmlRpcResponse[] responseArray = Osdb.searchMoviesOnIMDB(query);
        if (responseArray.length == 0) {
            Osdb.logOut();
            return null;
        } else {
            ids = new String[2][];
            ids[0] = new String[responseArray.length];
            ids[1] = new String[responseArray.length];
            for (int i = 0; i < responseArray.length; ++i) {
                ids[0][i] = responseArray[i].getId();
                ids[1][i] = responseArray[i].getMovieName();
            }
        }
        Osdb.logOut();
        return ids;
    }
}
