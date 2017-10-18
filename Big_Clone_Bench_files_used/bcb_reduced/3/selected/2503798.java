package cz.hdf.cdnavigator.scan;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.CanonMakernoteDirectory;
import com.drew.metadata.exif.ExifDirectory;
import cz.hdf.cdnavigator.Config;
import cz.hdf.cdnavigator.ConfigFile_v3;
import cz.hdf.cdnavigator.PhotoDataComparator;
import cz.hdf.cdnavigator.SongDataComparator;
import cz.hdf.cdnavigator.Utils;
import cz.hdf.cdnavigator.db.CDData;
import cz.hdf.cdnavigator.db.DataManagement;
import cz.hdf.cdnavigator.db.MusicAlbumData;
import cz.hdf.cdnavigator.db.PhotoAlbumData;
import cz.hdf.cdnavigator.db.PhotoData;
import cz.hdf.cdnavigator.db.SongData;
import cz.hdf.cdnavigator.gui.Global;
import cz.hdf.cdnavigator.gui.MusicWindow;
import cz.hdf.gui.HProgress;
import cz.hdf.gui.HProgressDialog;
import cz.hdf.i18n.I18N;
import cz.hdf.util.ProcessReader;
import cz.hdf.util.ThreadManager;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.io.FileUtils;
import org.jd3lib.Id3v1Tag;
import org.jd3lib.MP3File;
import sun.misc.BASE64Encoder;

/**
 * Scan CD device. Get CD volume, hascode, ...
 *
 * @author  hunter
 */
public class Scan {

    public static final long BYTES_CD_650 = 681984000;

    public static final long BYTES_CD_700 = 737280000;

    public static final long BYTES_CD_800 = 829440000;

    public static final long BYTES_DVD_PLUS_R = 4700372992l;

    public static final long BYTES_DVD_PLUS_R_DL = 8547991552l;

    public static final int DISC_TYPE_UNKNOWN = -1;

    public static final int DISC_TYPE_CD_DATA = DataManagement.DISC_TYPE_CD_DATA_INT;

    public static final int DISC_TYPE_CD_AUDIO = DataManagement.DISC_TYPE_CD_AUDIO_INT;

    public static final int DISC_TYPE_DVD_DATA = DataManagement.DISC_TYPE_DVD_DATA_INT;

    public static final int DISC_TYPE_DVD_MOVIE = DataManagement.DISC_TYPE_DVD_MOVIE_INT;

    /** volume name prefix in isoinfo output */
    private static final String ISOINFO_VOLUME = "Volume id: ";

    protected File discDevice;

    public File discMountDir;

    /** volume name */
    private String volume;

    private String hashcode;

    private int discType;

    /** photos map. Key is photo album and value is a list of photos. */
    private Map<PhotoAlbumData, List<PhotoData>> photoStructure;

    /** music map. Key is music album and value is a list of songs. */
    private Map<MusicAlbumData, List<SongData>> musicStructure;

    /**  */
    private int photoAlbumID;

    /**  */
    private int musicAlbumID;

    /** Logger. Hierarchy is set to name of this class. */
    Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * TODO
	 *
	 * @param  _discDevice    TODO
	 * @param  _discMountDir  TODO
	 */
    public Scan(File _discDevice, File _discMountDir) {
        discDevice = _discDevice;
        discMountDir = _discMountDir;
        discType = DISC_TYPE_UNKNOWN;
        volume = null;
        photoStructure = new HashMap<PhotoAlbumData, List<PhotoData>>();
        musicStructure = new HashMap<MusicAlbumData, List<SongData>>();
    }

    /**
	 * Get primary volume descriptor of CD device. E.g. isoinfo -d -i /dev/cdrom
	 *
	 * @return  isoinfo output
	 * @throws  ScanException  isoinfo end with error, or volume name not found
	 */
    private String scanPrimaryVolumeDescriptor() throws ScanException {
        ProcessReader processInput = null;
        ProcessReader processError = null;
        try {
            Process process = Runtime.getRuntime().exec(new String[] { Config.getProperty(ConfigFile_v3.NODE_SCAN, ConfigFile_v3.NODE_SCAN_ISOINFO), "-d", "-i", discDevice.getAbsolutePath() });
            processInput = new ProcessReader(process.getInputStream());
            processError = new ProcessReader(process.getErrorStream());
            ThreadManager.addTask(processInput, "Isoinfo Read InputStream");
            ThreadManager.addTask(processError, "Isoinfo Read ErrorStream");
            process.waitFor();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                if (exitCode == 2) {
                    throw new ScanException(ScanException.SCAN_MEDIUM_NOT_FOUND, processError.getReadedData());
                } else if (exitCode == 5) {
                    throw new ScanException(ScanException.SCAN_FS_NOT_FOUND, processError.getReadedData());
                }
                throw new ScanException(processError.getReadedData());
            }
            process.destroy();
        } catch (InterruptedException e) {
            throw new ScanException(e.getMessage());
        } catch (IOException e) {
            throw new ScanException(e.getMessage());
        } finally {
            processInput.close();
            processError.close();
        }
        return processInput.getReadedData();
    }

    /**
	 * Get volume filen on CD device. E.g. isoinfo -f -i /dev/cdrom
	 *
	 * @return  isoinfo output
	 * @throws  ScanException  isoinfo end with error, or volume name not found
	 */
    public String scanVolumeFiles() throws ScanException {
        ProcessReader processInput = null;
        ProcessReader processError = null;
        try {
            Process process = Runtime.getRuntime().exec(new String[] { Config.getProperty(ConfigFile_v3.NODE_SCAN, ConfigFile_v3.NODE_SCAN_ISOINFO), "-f", "-i", discDevice.getAbsolutePath() });
            processInput = new ProcessReader(process.getInputStream());
            processError = new ProcessReader(process.getErrorStream());
            ThreadManager.addTask(processInput, "Isoinfo Read InputStream");
            ThreadManager.addTask(processError, "Isoinfo Read ErrorStream");
            process.waitFor();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new ScanException(processError.getReadedData());
            }
        } catch (InterruptedException e) {
            throw new ScanException(e.getMessage());
        } catch (IOException e) {
            throw new ScanException(e.getMessage());
        } finally {
            processInput.close();
            processError.close();
        }
        return processInput.getReadedData();
    }

    /**
	 * Count hashcode. Execute isoinfo command always.
	 *
	 * @return  hashcode
	 * @throws  ScanException
	 */
    public String scanHashcode() throws ScanException {
        return scanHashcode(null, null);
    }

    /**
	 * Count hashcode. Execute isoinfo command only if argument is null.
	 *
	 * @param   _pvd  primary volume descriptor
	 * @param   _vf   volume files
	 * @return  hashcode
	 * @throws  ScanException
	 */
    public String scanHashcode(String _pvd, String _vf) throws ScanException {
        String pvd = _pvd;
        String vf = _vf;
        if (pvd == null) {
            pvd = scanPrimaryVolumeDescriptor();
        }
        if (vf == null) {
            vf = scanVolumeFiles();
        }
        String hashcode = null;
        try {
            vf.getBytes();
            byte[] hash = MessageDigest.getInstance("MD5").digest(pvd.getBytes("UTF-8"));
            BASE64Encoder encoder = new BASE64Encoder();
            hashcode = encoder.encodeBuffer(hash);
            hashcode = hashcode.trim();
        } catch (NoSuchAlgorithmException e) {
            throw new ScanException("Can not found MD5 algorithm. This is necessary to count hashcode. " + e);
        } catch (UnsupportedEncodingException e) {
            throw new ScanException("Can not found UTF-8 charset. This is necessary to count hashcode. " + e);
        }
        return hashcode;
    }

    /**
	 * @return
	 * @throws  ScanException  when CD scanning failed
	 * @throws  SQLException
	 */
    public CDData[] scanCD() throws ScanException, SQLException {
        String pvd = null;
        try {
            pvd = scanPrimaryVolumeDescriptor();
            hashcode = scanHashcode(pvd, null);
            CDData[] cdData = DataManagement.executeQueryCD(DataManagement.SELECT_CD_PREFIX + " where " + DataManagement.TABLE_CD_COLUMN_HASHCODE + " = '" + hashcode + "'");
            if (cdData.length > 0) {
                return cdData;
            }
        } catch (ScanException exc) {
            if (exc.getType() == ScanException.SCAN_FS_NOT_FOUND) {
                discType = DISC_TYPE_CD_AUDIO;
            } else {
                throw exc;
            }
        }
        if (pvd == null) return null;
        if (pvd.contains("NO Joliet present") && pvd.contains("NO Rock Ridge present")) {
            discType = DISC_TYPE_DVD_MOVIE;
        } else {
            Utils.mountCD(discMountDir);
            long discSize = FileUtils.sizeOfDirectory(discMountDir);
            if (discSize < BYTES_CD_800) {
                discType = DISC_TYPE_CD_DATA;
            } else {
                discType = DISC_TYPE_DVD_DATA;
            }
        }
        StringTokenizer st = new StringTokenizer(pvd, "\n");
        String line;
        boolean found = false;
        while ((!found) && st.hasMoreTokens()) {
            line = st.nextToken();
            if (line.startsWith(ISOINFO_VOLUME)) {
                volume = line.substring(ISOINFO_VOLUME.length());
                found = true;
            }
        }
        if (!found) {
            throw new ScanException(ScanException.SCAN_VOLUME_NOT_FOUND, "Can not found volume name in output. Prefix '" + ISOINFO_VOLUME + "' not found.");
        }
        return null;
    }

    /**
	 * @param   _photoExts
	 * @param   _musicExts
	 * @param   _movieExts
	 * @throws  ScanException  when CD scanning failed
	 */
    public void searchFiles(String[] _photoExts, String[] _musicExts, String[] _movieExts) throws ScanException {
        if (Utils.mountCD(discMountDir) != Utils.MOUNT_OK) {
            throw new ScanException("Can not mount CD.");
        }
        photoStructure.clear();
        musicStructure.clear();
        photoAlbumID = -100000;
        musicAlbumID = -100000;
        searchDir(discMountDir, _photoExts, _musicExts, _movieExts);
    }

    /**
	 * Convert folder name to human readable name. Example: AtHome2004 -> At home 2004.
	 *
	 * @param   _folderName  name of folder
	 * @return  human readable name
	 */
    private String readableFolderName(String _folderName) {
        if (_folderName == null) {
            return null;
        }
        if (_folderName.length() < 2) {
            return _folderName;
        }
        StringBuffer sb = new StringBuffer();
        sb.append(_folderName.charAt(0));
        for (int i = 1; i < _folderName.length(); i++) {
            if ((Character.isUpperCase(_folderName.charAt(i)) || Character.isDigit(_folderName.charAt(i))) && !Character.isSpaceChar(_folderName.charAt(i - 1)) && Character.isLowerCase(_folderName.charAt(i - 1))) {
                sb.append(' ');
                sb.append(Character.toLowerCase(_folderName.charAt(i)));
            } else {
                sb.append(_folderName.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
	 * Search folders (depth search)
	 *
	 * @param  _dir
	 * @param  _photoExts
	 * @param  _musicExts
	 * @param  _moviesExt
	 */
    private void searchDir(File _dir, String[] _photoExts, String[] _musicExts, String[] _moviesExt) {
        String[] files = _dir.list();
        File file;
        PhotoAlbumData pad = null;
        MusicAlbumData mad = null;
        int photoID = -100000;
        int songID = -100000;
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            file = new File(_dir, files[i]);
            if (file.isFile()) {
                if (_photoExts != null) {
                    if (Utils.hasFileExtensions(file, _photoExts)) {
                        if (pad == null) {
                            pad = new PhotoAlbumData();
                            pad.setID(photoAlbumID);
                            pad.setName(file.getParentFile().getName());
                            photoAlbumID++;
                            photoStructure.put(pad, new ArrayList<PhotoData>());
                        }
                        List<PhotoData> photos = photoStructure.get(pad);
                        PhotoData pd = new PhotoData();
                        pd.setID(photoID);
                        pd.setAlbumID(pad.getID());
                        photoID++;
                        String relFileName = file.getPath();
                        if (file.getPath().startsWith(discMountDir.getAbsolutePath())) {
                            relFileName = file.getPath().substring(discMountDir.getAbsolutePath().length());
                            if (relFileName.startsWith(Config.FS)) {
                                relFileName = relFileName.substring(1);
                            }
                        }
                        pd.setFile(relFileName);
                        if (pd.getNumber() == PhotoData.BAD_INT_VALUE) {
                            pd.setNumber(i + 1);
                        }
                        photos.add(pd);
                    }
                }
                if (_musicExts != null) {
                    if (Utils.hasFileExtensions(file, _musicExts)) {
                        if (mad == null) {
                            mad = new MusicAlbumData();
                            mad.setID(musicAlbumID);
                            mad.setName(file.getParentFile().getName());
                            musicAlbumID++;
                            musicStructure.put(mad, new ArrayList<SongData>());
                        }
                        List<SongData> songs = musicStructure.get(mad);
                        SongData sd = new SongData();
                        sd.setID(songID);
                        sd.setAlbumID(mad.getID());
                        songID++;
                        String relFileName = file.getPath();
                        if (file.getPath().startsWith(discMountDir.getAbsolutePath())) {
                            relFileName = file.getPath().substring(discMountDir.getAbsolutePath().length());
                            if (relFileName.startsWith(Config.FS)) {
                                relFileName = relFileName.substring(1);
                            }
                        }
                        sd.setFile(relFileName);
                        if (sd.getNumber() == SongData.BAD_INT_VALUE) {
                            sd.setNumber(i + 1);
                        }
                        songs.add(sd);
                    }
                }
                if (_moviesExt != null) {
                }
            }
            if (file.isDirectory()) {
                searchDir(file, _photoExts, _musicExts, _moviesExt);
            }
        }
    }

    public void scanFiles(boolean _exif, boolean _thumb, String _photoNumberRE, String _photoAlbumRE, String _photoYearRE, boolean _id3, String _songNameRE, String _songNumberRE, String _songInterpretRE, String _songAlbumRE, String _songYearRE) throws ScanException {
    }

    /**
	 * Get CD volume name.
	 *
	 * @return  volume name
	 */
    public String getVolume() {
        return volume;
    }

    public int getDiscType() {
        return discType;
    }

    /**
	 * TODO
	 *
	 * @return  TODO
	 */
    public String getHashcode() {
        return hashcode;
    }

    /**
	 * TODO
	 *
	 * @return  TODO
	 */
    public Map<PhotoAlbumData, List<PhotoData>> getPhotoStructure() {
        return photoStructure;
    }

    /**
	 * TODO
	 *
	 * @return  TODO
	 */
    public Map<MusicAlbumData, List<SongData>> getMusicStructure() {
        return musicStructure;
    }
}
