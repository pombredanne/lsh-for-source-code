package muvis.database;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import muvis.Environment;
import maudio.entities.AudioMetadata;
import pt.ist.utl.nbtree.keys.Key;
import utils.ImageUtils;
import utils.LRUCache;
import utils.Observable;
import utils.Observer;
import utils.Util;

/**
 * Class that holds the flow of information with the database
 * - java -cp lib/hsqldb.jar org.hsqldb.Server -database.0 file:muvisdb -dbname.0 xdb
 * - Running the database
 * @author Ricardo
 */
public class MusicLibraryDatabaseManager implements Observable {

    private ArrayList<Observer> observers;

    static Connection conn;

    private final Object obsLock = new Object();

    private LRUCache<String, BufferedImage> cache;

    /**
     * Creates a new MusicLibraryDatabaseManager, that can be used for updating the music
     * library database or retrieve information from this object.
     * Attention: must call the methods connect() and initDatabase() for correctly usage.
     */
    public MusicLibraryDatabaseManager() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            observers = new ArrayList<Observer>();
            cache = new LRUCache<String, BufferedImage>(50);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    public void connect() {
        try {
            String dataFolderPath = Environment.getEnvironmentInstance().getDataFolderPath();
            conn = DriverManager.getConnection("jdbc:hsqldb:file:" + dataFolderPath + "db/muvisdb;hsqldb.log_data=false", "sa", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * shutdowns the connection with the database
     * @throws SQLException
     */
    public void shutdown() {
        try {
            conn.commit();
            Statement st = conn.createStatement();
            st.execute("SHUTDOWN");
            conn.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Key getAlbumKey(String artist, String album) {
        ResultSet rs = null;
        try {
            String query = "SELECT albums_table.key " + "FROM albums_table, artists_table " + "WHERE albums_table.artist_id=artists_table.id AND " + "artists_table.artist_name=? AND albums_table.album_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artist);
            st.setString(2, album);
            rs = st.executeQuery();
            if (rs.next()) {
                Key key = (Key) rs.getObject(1);
                st.close();
                return key;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Key(-1, "");
    }

    public Key getAlbumTrackKey(int trackId) {
        ResultSet rs = null;
        try {
            String query = "SELECT albums_table.key " + "FROM albums_table, tracks_table " + "WHERE albums_table.id=tracks_table.album_id AND " + "tracks_table.id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                Key key = (Key) rs.getObject(1);
                st.close();
                return key;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Key(-1, "");
    }

    public ArrayList<String> getAlbumTracks(String artist, String album) {
        ArrayList<String> albumTracks = new ArrayList<String>();
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artist);
            int albumId = getAlbumId(artist, album);
            String query = "SELECT filename " + "FROM tracks_table " + "WHERE artist_id=? AND album_id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, artistId);
            st.setInt(2, albumId);
            rs = st.executeQuery();
            while (rs.next()) {
                String track = rs.getString(1);
                albumTracks.add(track);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return albumTracks;
    }

    public String getAlbumFirstTrack(String artist, String album) {
        String firstTrack = "";
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artist);
            int albumId = getAlbumId(artist, album);
            String query = "SELECT TOP 1 filename " + "FROM tracks_table " + "WHERE artist_id=? AND album_id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, artistId);
            st.setInt(2, albumId);
            rs = st.executeQuery();
            while (rs.next()) {
                firstTrack = rs.getString(1);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return firstTrack;
    }

    public ArrayList<String> getAlbumTracks(String albumName) {
        ArrayList<String> albumTracks = new ArrayList<String>();
        ResultSet rs = null;
        try {
            String query = "SELECT filename " + "FROM tracks_table, albums_table, artists_table " + "WHERE tracks_table.artist_id=artists_table.id AND " + "tracks_table.album_id=albums_table.id AND " + "album_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, albumName);
            rs = st.executeQuery();
            while (rs.next()) {
                String track = rs.getString(1);
                albumTracks.add(track);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return albumTracks;
    }

    public ArrayList<Integer> getAlbumTracksIds(int albumId) {
        ArrayList<Integer> albumTracksIds = new ArrayList<Integer>();
        ResultSet rs = null;
        try {
            String query = "SELECT tracks_table.id " + "FROM tracks_table, albums_table " + "WHERE tracks_table.album_id=albums_table.id AND " + "albums_table.id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, albumId);
            rs = st.executeQuery();
            while (rs.next()) {
                int trackId = rs.getInt(1);
                albumTracksIds.add(trackId);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return albumTracksIds;
    }

    public String getArtistWithMoreTracks() {
        String artistName = "";
        ResultSet rs = null;
        try {
            String query = "SELECT TOP 1 P.artist_name FROM " + "(SELECT artist_name, COUNT(DISTINCT id) as CountTracks " + "FROM information_tracks_table " + "GROUP BY artist_name) as P " + "ORDER BY P.CountTracks DESC ";
            PreparedStatement st = conn.prepareStatement(query);
            rs = st.executeQuery();
            while (rs.next()) {
                artistName = rs.getString(1);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return artistName;
    }

    public ArrayList<String> getArtistAlbums(String artist) {
        ArrayList<String> albums = new ArrayList<String>();
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artist);
            String query = "SELECT album_name " + "FROM albums_table  " + "WHERE artist_id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, artistId);
            rs = st.executeQuery();
            while (rs.next()) {
                String album = rs.getString(1);
                albums.add(album);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return albums;
    }

    public Key getArtistKey(String artist) {
        ResultSet rs = null;
        try {
            String query = "SELECT key " + "FROM artists_table " + "WHERE artist_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artist);
            rs = st.executeQuery();
            if (rs.next()) {
                Key key = (Key) rs.getObject(1);
                return key;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Key(-1, "");
    }

    public ArrayList<String> getArtistTracks(String artist) {
        ArrayList<String> albums = new ArrayList<String>();
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artist);
            String query = "SELECT filename " + "FROM tracks_table, artists_table " + "WHERE tracks_table.artist_id=artists_table.id AND " + "tracks_table.artist_id=?";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, artistId);
            rs = st.executeQuery();
            while (rs.next()) {
                String album = rs.getString(1);
                albums.add(album);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return albums;
    }

    public int getArtistNumTracks(String artist) {
        int numTracks = 0;
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artist);
            String query = "SELECT COUNT(id) " + "FROM tracks_table " + "WHERE artist_id=? ";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, artistId);
            rs = st.executeQuery();
            while (rs.next()) {
                numTracks = rs.getInt(1);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return numTracks;
    }

    public ArrayList<Integer> getArtistTracksIds(String artist) {
        ArrayList<Integer> tracksIds = new ArrayList<Integer>();
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artist);
            String query = "SELECT tracks_table.id " + "FROM tracks_table, artists_table " + "WHERE tracks_table.artist_id=artists_table.id AND " + "tracks_table.artist_id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, artistId);
            rs = st.executeQuery();
            while (rs.next()) {
                int track_id = rs.getInt(1);
                tracksIds.add(track_id);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tracksIds;
    }

    public String getArtistGenre(String artistName) {
        String genre = "";
        ResultSet rs = null;
        try {
            String query = "SELECT genre, COUNT(genre) " + "FROM information_tracks_table " + "WHERE artist_name=? " + "GROUP BY genre " + "ORDER BY genre ASC";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artistName);
            rs = st.executeQuery();
            int count = 0;
            while (rs.next()) {
                if (rs.getInt(2) > count) {
                    count = rs.getInt(2);
                    genre = rs.getString(1);
                }
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return genre;
    }

    public TableRecord getTrackRow(int trackId) {
        ResultSet rs = null;
        TableRecord record = new TableRecord();
        try {
            String query = "SELECT * " + "FROM information_tracks_table WHERE id=?";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                for (int i = 1; i <= 9; i++) {
                    record.setValueColumn(i, rs.getObject(i));
                }
                st.close();
                return record;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new TableRecord();
    }

    public TableRecord getRowAt(int row) {
        ResultSet rs = null;
        int actualRow = 1;
        TableRecord record = new TableRecord();
        try {
            String query = "SELECT * " + "FROM information_tracks_table";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            while (rs.next()) {
                if (actualRow == row) {
                    for (int i = 1; i <= 9; i++) {
                        Object obj = rs.getObject(i);
                        if (obj == null) {
                            if (i == 1) {
                                record.setValueColumn(i, Integer.class.newInstance());
                            } else if (i == 2) {
                                record.setValueColumn(i, String.class.newInstance());
                            } else if (i == 3) {
                                record.setValueColumn(i, String.class.newInstance());
                            } else if (i == 4) {
                                record.setValueColumn(i, String.class.newInstance());
                            } else if (i == 5) {
                                record.setValueColumn(i, Long.class.newInstance());
                            } else if (i == 6) {
                                record.setValueColumn(i, String.class.newInstance());
                            } else {
                                record.setValueColumn(i, String.class.newInstance());
                            }
                        } else {
                            record.setValueColumn(i, rs.getObject(i));
                        }
                    }
                    st.close();
                    return record;
                }
                actualRow++;
            }
            st.close();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new TableRecord();
    }

    public int getTrackId(Key key) {
        try {
            ResultSet rs = null;
            String query = "SELECT id FROM tracks_table WHERE keyValue=? AND keyId=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setDouble(1, key.getValue());
            st.setString(2, key.getId());
            rs = st.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                st.close();
                return id;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Key getArtistTrackKey(int trackId) {
        String artistName = getArtistName(trackId);
        ResultSet rs = null;
        try {
            String query = "SELECT key " + "FROM artists_table " + "WHERE artist_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artistName);
            rs = st.executeQuery();
            if (rs.next()) {
                Key key = (Key) rs.getObject(1);
                st.close();
                return key;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Key(-1, "");
    }

    public Key getTrackKey(int trackId) {
        String filename = getTrackFilename(trackId);
        return getTrackKey(filename);
    }

    public Key getTrackKey(String filename) {
        ResultSet rs = null;
        try {
            String query = "SELECT key " + "FROM tracks_table " + "WHERE filename=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, filename);
            rs = st.executeQuery();
            if (rs.next()) {
                Key key = (Key) rs.getObject("key");
                st.close();
                return key;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Key(-1, "");
    }

    public int getTracksInYearRange(int startYear, int range) {
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) " + "FROM information_tracks_table " + "WHERE year BETWEEN ? AND ?";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, startYear);
            st.setInt(2, startYear + range);
            rs = st.executeQuery();
            if (rs.next()) {
                int tracksTotal = rs.getInt(1);
                st.close();
                return tracksTotal;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTracksWithBeat(String beat) {
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) " + "FROM information_tracks_table " + "WHERE beat=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, beat);
            rs = st.executeQuery();
            if (rs.next()) {
                int tracksTotal = rs.getInt(1);
                st.close();
                return tracksTotal;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTracksWithGenre(String genre) {
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) " + "FROM information_tracks_table " + "WHERE genre=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, genre);
            rs = st.executeQuery();
            if (rs.next()) {
                int tracksTotal = rs.getInt(1);
                st.close();
                return tracksTotal;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getTracksWithMood(String mood) {
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) " + "FROM information_tracks_table " + "WHERE mood=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, mood);
            rs = st.executeQuery();
            if (rs.next()) {
                int tracksTotal = rs.getInt(1);
                st.close();
                return tracksTotal;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void initDatabase() throws SQLException {
        String query = "CREATE CACHED TABLE artists_table ( " + "id INTEGER IDENTITY PRIMARY KEY, " + "artist_name LONGVARCHAR," + "key OTHER," + "keyValue double," + "keyId LONGVARCHAR," + "UNIQUE(id)" + ")";
        PreparedStatement st = conn.prepareCall(query);
        st.execute();
        query = "CREATE CACHED TABLE albums_table ( " + "id INTEGER IDENTITY PRIMARY KEY, " + "album_name LONGVARCHAR," + "artist_id INTEGER," + "key OTHER," + "keyValue double," + "keyId LONGVARCHAR," + "UNIQUE(id)," + "FOREIGN KEY (artist_id) REFERENCES artists_table(id) ON DELETE CASCADE" + ")";
        st = conn.prepareCall(query);
        st.execute();
        query = "CREATE CACHED TABLE tracks_table ( " + "id INTEGER IDENTITY PRIMARY KEY, " + "filename LONGVARCHAR, " + "artist_id INTEGER, " + "album_id INTEGER, " + "key OTHER," + "keyValue double," + "keyId LONGVARCHAR," + "FOREIGN KEY (artist_id) REFERENCES artists_table(id) ON DELETE CASCADE," + "FOREIGN KEY (album_id) REFERENCES albums_table(id) ON DELETE CASCADE" + ")";
        st = conn.prepareCall(query);
        st.execute();
        query = "CREATE CACHED TABLE tracks_cover_table ( " + "id LONGVARCHAR PRIMARY KEY, " + "cover LONGVARBINARY," + "UNIQUE(id)" + ")";
        st = conn.prepareCall(query);
        st.execute();
        query = "CREATE CACHED TABLE information_tracks_table ( " + "id INTEGER, " + "track_title LONGVARCHAR," + "artist_name LONGVARCHAR," + "album_name LONGVARCHAR, " + "duration BIGINT," + "genre LONGVARCHAR," + "year LONGVARCHAR," + "beat LONGVARCHAR, " + "mood LONGVARCHAR, " + "filename LONGVARCHAR, " + "cover_id LONGVARCHAR, " + "FOREIGN KEY (id) REFERENCES tracks_table(id) ON DELETE CASCADE," + "FOREIGN KEY (cover_id) REFERENCES tracks_cover_table(id) ON DELETE CASCADE" + ")";
        st = conn.prepareCall(query);
        st.execute();
    }

    private BufferedImage fromByteArray(byte[] imagebytes) {
        try {
            if (imagebytes != null && (imagebytes.length > 0)) {
                BufferedImage im = ImageIO.read(new ByteArrayInputStream(imagebytes));
                return im;
            }
            return null;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    public AudioMetadata getTrackMetadata(int trackId) {
        AudioMetadata metadata = new AudioMetadata();
        ResultSet rs = null;
        try {
            String query = "SELECT filename, track_title, album_name, artist_name, duration, genre, filename, year " + "FROM information_tracks_table as I, tracks_cover_table as T WHERE id=? AND " + "I.cover_id = T.id";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                metadata.setTrackNumber(trackId);
                metadata.setTitle(rs.getString("track_title"));
                metadata.setAlbum(rs.getString("album_name"));
                metadata.setAuthor(rs.getString("artist_name"));
                metadata.setDuration(rs.getInt("duration"));
                metadata.setGenre(rs.getString("genre"));
                metadata.setFilename(rs.getString("filename"));
                BufferedImage image = cache.get(metadata.getFilename());
                if (image == null) {
                    String query2 = "SELECT cover " + "FROM information_tracks_table as I, tracks_cover_table as T WHERE id=? AND " + "I.cover_id = T.id";
                    PreparedStatement st2 = conn.prepareCall(query2);
                    st2.setInt(1, trackId);
                    ResultSet rs2 = st2.executeQuery();
                    if (rs2.next()) {
                        Blob imageBlob = rs2.getBlob(1);
                        byte[] imageBytes = imageBlob.getBytes(1, (int) imageBlob.length());
                        image = fromByteArray(imageBytes);
                        cache.put(metadata.getFilename(), image);
                    }
                    st2.close();
                }
                metadata.setArtwork(image);
                int year = Integer.parseInt(rs.getString("year"));
                if (year == Integer.MIN_VALUE) {
                    metadata.setYear("Unknown year");
                } else {
                    metadata.setYear(rs.getString("year"));
                }
                metadata.setBitrate("128");
                st.close();
            } else {
                st.close();
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return metadata;
    }

    public AudioMetadata getTrackMetadata(String filename) {
        int trackId = getTrackId(filename);
        return getTrackMetadata(trackId);
    }

    public void removeTrack(String filename) throws SQLException {
        int trackId = getTrackId(filename);
        String query = "DELETE FROM information_tracks_table WHERE id=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setInt(1, trackId);
        st.executeUpdate();
        st.close();
        query = "DELETE FROM tracks_cover_table WHERE id=?";
        st = conn.prepareStatement(query);
        st.setInt(1, trackId);
        st.executeUpdate();
        st.close();
        query = "DELETE FROM tracks_table WHERE id=?";
        st = conn.prepareStatement(query);
        st.setInt(1, trackId);
        st.executeUpdate();
        st.close();
        updateObservers();
    }

    public void setTrackBeat(String filename, String beat) throws SQLException {
        int trackId = getTrackId(filename);
        String query = "UPDATE information_tracks_table " + "SET beat=? " + "WHERE id=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setString(1, beat);
        st.setInt(2, trackId);
        st.executeUpdate();
        st.close();
    }

    public void setTrackMood(String filename, String mood) throws SQLException {
        int trackId = getTrackId(filename);
        String query = "UPDATE information_tracks_table " + "SET mood=? " + "WHERE id=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setString(1, mood);
        st.setInt(2, trackId);
        st.executeUpdate();
        st.close();
    }

    public synchronized void update(String expression) throws SQLException {
        Statement st = null;
        st = conn.createStatement();
        int i = st.executeUpdate(expression);
        if (i == -1) {
            System.out.println("db error : " + expression);
        }
        st.close();
    }

    /**
     * Note: st must be closed after processing the result set
     * @param expression
     * @param st
     * @param rs
     * @throws SQLException
     */
    public synchronized ResultSet query(String expression, Statement st) throws SQLException {
        ResultSet rs = null;
        st = conn.createStatement();
        rs = st.executeQuery(expression);
        return rs;
    }

    /**
     * Add a new track to the database
     * @param filename
     * @param artistName
     * @param albumName
     * @param descriptor
     * @throws SQLException
     */
    public void addNewSong(String filename, String artistName, String albumName, AudioMetadata metadata) throws SQLException {
        insertArtist(artistName);
        insertAlbum(artistName, albumName);
        insertTrack(filename, artistName, albumName);
        insertTrackInformation(filename, metadata);
        updateObservers();
    }

    private boolean existsTrackInformation(String filename) {
        int trackId = getTrackId(filename);
        if (trackId > -1) {
            try {
                ResultSet rs = null;
                String query = "SELECT id FROM information_tracks_table WHERE id=?";
                PreparedStatement st = conn.prepareStatement(query);
                st.setInt(1, trackId);
                rs = st.executeQuery();
                if (rs.next()) {
                    st.close();
                    return true;
                }
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int getTrackId(String filename) {
        try {
            ResultSet rs = null;
            String query = "SELECT id FROM tracks_table WHERE filename=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, filename);
            rs = st.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                st.close();
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String getTrackFilename(int trackId) {
        try {
            ResultSet rs = null;
            String query = "SELECT filename FROM tracks_table WHERE id=?";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                String filename = rs.getString(1);
                st.close();
                return filename;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Inserts/updates a artist in the database, when a new track is added to the database
     * @param artistName
     * @throws SQLException
     */
    private void insertArtist(String artistName) throws SQLException {
        if (!artistExists(artistName)) {
            String query = "INSERT INTO artists_table(artist_name, key, keyValue, keyId) " + "VALUES(?,?,?,?)";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artistName);
            st.setObject(2, new Key(-1, ""));
            st.setDouble(3, -1);
            st.setInt(4, 0);
            st.executeUpdate();
            st.close();
        }
    }

    /**
     * Gets the id for the given artist name
     * @param artistName
     * @return
     */
    private int getArtistId(String artistName) {
        try {
            ResultSet rs = null;
            String query = "SELECT id FROM artists_table WHERE artist_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artistName);
            rs = st.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                st.close();
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getArtistName(int trackId) {
        try {
            ResultSet rs = null;
            String query = "SELECT artist_name " + "FROM artists_table, tracks_table " + "WHERE tracks_table.artist_id=artists_table.id " + "AND tracks_table.id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                String name = rs.getString(1);
                st.close();
                return name;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getArtistName(Key artistKey) {
        try {
            ResultSet rs = null;
            String query = "SELECT artist_name " + "FROM artists_table " + "WHERE keyValue=? AND keyId=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setDouble(1, artistKey.getValue());
            st.setString(2, artistKey.getId());
            rs = st.executeQuery();
            if (rs.next()) {
                String name = rs.getString(1);
                st.close();
                return name;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getAlbumName(int trackId) {
        try {
            ResultSet rs = null;
            String query = "SELECT album_name " + "FROM albums_table, tracks_table, artists_table " + "WHERE artists_table.id=tracks_table.artist_id " + "AND albums_table.id=tracks_table.album_id AND tracks_table.id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                String albumName = rs.getString(1);
                st.close();
                return albumName;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Inserts a new album in the database: if it already exists, updates the existent one
     * @param artistName
     * @param albumName
     * @throws SQLException
     */
    private void insertAlbum(String artistName, String albumName) throws SQLException {
        if (!albumExists(artistName, albumName)) {
            int artistId = getArtistId(artistName);
            String update = "INSERT INTO albums_table(album_name,artist_id,key,keyValue,keyId) VALUES(?,?,?,?,?)";
            PreparedStatement st = conn.prepareStatement(update);
            st.setString(1, albumName);
            st.setInt(2, artistId);
            st.setObject(3, new Key(-1, ""));
            st.setDouble(4, -1);
            st.setInt(5, 0);
            st.executeUpdate();
            st.close();
        }
    }

    /**
     * Gets the album id for a given artistName and albumName
     * (note, different artists can have the same album name)
     * @param artistName
     * @param albumName
     * @return
     */
    public int getAlbumId(String artistName, String albumName) {
        try {
            ResultSet rs = null;
            int artistId = getArtistId(artistName);
            String query = "SELECT id " + "FROM albums_table " + "WHERE album_name=? AND artist_id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, albumName);
            st.setInt(2, artistId);
            rs = st.executeQuery();
            if (rs.next()) {
                int albumId = rs.getInt(1);
                st.close();
                return albumId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getAlbumId(Key albumKey) {
        try {
            ResultSet rs = null;
            String query = "SELECT albums_table.id " + "FROM albums_table " + "WHERE albums_table.keyValue=? AND albums_table.keyId=? ";
            PreparedStatement st = conn.prepareStatement(query);
            st.setDouble(1, albumKey.getValue());
            st.setString(2, albumKey.getId());
            rs = st.executeQuery();
            if (rs.next()) {
                int albumId = rs.getInt(1);
                st.close();
                return albumId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public String getAlbumYear(String artistName, String albumName) {
        String year = "";
        ResultSet rs = null;
        try {
            String query = "SELECT year, COUNT(year) " + "FROM information_tracks_table " + "WHERE artist_name=? AND album_name=? " + "GROUP BY year " + "ORDER BY year ASC";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artistName);
            st.setString(2, albumName);
            rs = st.executeQuery();
            int count = 0;
            while (rs.next()) {
                if (rs.getInt(2) > count) {
                    count = rs.getInt(2);
                    year = rs.getString(1);
                }
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int tempYear = Integer.parseInt(year);
        if (tempYear == Integer.MIN_VALUE) {
            year = "Unknown year";
        }
        return year;
    }

    /**
     * Inserts a new track in the database, if the track isn't already in the database
     * @param filename
     * @param descritor
     * @throws SQLException
     */
    private void insertTrack(String filename, String artistName, String albumName) throws SQLException {
        if (!trackExists(filename)) {
            if (!artistExists(artistName)) {
                insertArtist(artistName);
            }
            int artistId = getArtistId(artistName);
            if (!albumExists(artistName, albumName)) {
                insertAlbum(artistName, albumName);
            }
            int albumId = getAlbumId(artistName, albumName);
            String update = "INSERT INTO tracks_table(filename,artist_id,album_id,key,keyValue,keyId) VALUES(?, ?, ?,?,?,?)";
            PreparedStatement st = conn.prepareStatement(update);
            st.setString(1, filename);
            st.setInt(2, artistId);
            st.setInt(3, albumId);
            st.setObject(4, new Key(-1, ""));
            st.setDouble(5, -1);
            st.setInt(6, 0);
            st.executeUpdate();
            st.close();
        }
    }

    /**
     * Sets the key for this artist
     * @param artistName the name of the artist
     * @param key the key to be set
     * @throws SQLException
     */
    public void setArtistKey(String artistName, Key key) throws SQLException {
        int artistId = getArtistId(artistName);
        setArtistKey(artistId, key);
    }

    public void setArtistKey(int artistId, Key key) throws SQLException {
        String query = "UPDATE artists_table " + "SET key=?, keyValue=?, keyId=? " + "WHERE id=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setObject(1, key);
        st.setDouble(2, key.getValue());
        st.setString(3, key.getId());
        st.setInt(4, artistId);
        st.executeUpdate();
        st.close();
    }

    /**
     * Sets the key for the selected album
     * @param artistName
     * @param albumName
     * @param key
     * @throws SQLException
     */
    public void setAlbumKey(String artistName, String albumName, Key key) throws SQLException {
        int albumId = getAlbumId(artistName, albumName);
        String query = "UPDATE albums_table " + "SET key=?, keyValue=?, keyId=? " + "WHERE id=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setObject(1, key);
        st.setDouble(2, key.getValue());
        st.setString(3, key.getId());
        st.setInt(4, albumId);
        st.executeUpdate();
        st.close();
    }

    public void setAlbumKey(int albumId, Key key) throws SQLException {
        String query = "UPDATE albums_table " + "SET key=?, keyValue=?, keyId=? " + "WHERE id=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setObject(1, key);
        st.setDouble(2, key.getValue());
        st.setString(3, key.getId());
        st.setInt(4, albumId);
        st.executeUpdate();
        st.close();
    }

    public void setTrackKey(String filename, Key key) throws SQLException {
        String query = "UPDATE tracks_table " + "SET key=?, keyValue=?, keyId=? " + "WHERE filename=?";
        PreparedStatement st = conn.prepareStatement(query);
        st.setObject(1, key);
        st.setDouble(2, key.getValue());
        st.setString(3, key.getId());
        st.setString(4, filename);
        st.executeUpdate();
        st.close();
    }

    private boolean artworkExists(String artworkId) {
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) as Result FROM tracks_cover_table WHERE id=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artworkId);
            rs = st.executeQuery();
            if (rs.next()) {
                int result = rs.getInt("Result");
                st.close();
                if (result == 1) {
                    return true;
                }
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void insertTrackInformation(String filename, AudioMetadata metadata) throws SQLException {
        if (!existsTrackInformation(filename)) {
            String title = metadata.getTitle();
            String artistName = metadata.getAuthor();
            String albumName = metadata.getAlbum();
            long duration = metadata.getDuration();
            String genre = metadata.getGenre();
            String year = metadata.getYear();
            BufferedImage artwork = metadata.getArtwork();
            int trackId = getTrackId(filename);
            String update = "INSERT INTO information_tracks_table(id,track_title," + "artist_name,album_name,duration,genre,year,filename) " + "VALUES(?,?,?,?,?,?,?,?)";
            PreparedStatement st = conn.prepareStatement(update);
            st.setInt(1, trackId);
            st.setString(2, title);
            st.setString(3, artistName);
            st.setString(4, albumName);
            st.setLong(5, duration);
            st.setString(6, genre);
            st.setString(7, year);
            st.setString(8, filename);
            st.executeUpdate();
            st.close();
            try {
                byte[] imageBytes = ImageUtils.toByteArray(artwork);
                ByteArrayInputStream inStream = new ByteArrayInputStream(imageBytes);
                MessageDigest md;
                String imageHash = "";
                try {
                    md = MessageDigest.getInstance("MD5");
                    md.update(imageBytes);
                    byte[] hash = md.digest();
                    imageHash = Util.returnHex(hash);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    imageHash = artistName + ":" + albumName + ":" + title;
                }
                if (!artworkExists(imageHash)) {
                    update = "INSERT INTO tracks_cover_table(id,cover) " + "VALUES(?,?)";
                    st = conn.prepareStatement(update);
                    st.setString(1, imageHash);
                    st.setBinaryStream(2, inStream, inStream.available());
                    st.executeUpdate();
                    st.close();
                }
                update = "UPDATE information_tracks_table " + "SET cover_id = ? " + "WHERE id = ? ";
                st = conn.prepareStatement(update);
                st.setString(1, imageHash);
                st.setInt(2, trackId);
                st.executeUpdate();
                st.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("This should not be happening.");
            }
        }
    }

    /**
     * Checks if a given track is already in the database
     * @param filename
     * @return
     */
    public boolean trackExists(String filename) {
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM tracks_table WHERE filename=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, filename);
            rs = st.executeQuery();
            if (rs.next()) {
                st.close();
                return true;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a given artist is already in the database
     * @param artistName
     * @return
     */
    private boolean artistExists(String artistName) {
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM artists_table WHERE artist_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, artistName);
            rs = st.executeQuery();
            if (rs.next()) {
                st.close();
                return true;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if a given album for a given artist is already in the database
     * @param artistName
     * @param albumName
     * @return
     */
    private boolean albumExists(String artistName, String albumName) {
        ResultSet rs = null;
        try {
            int artistId = getArtistId(artistName);
            String query = "SELECT id FROM albums_table WHERE artist_id=? AND album_name=?";
            PreparedStatement st = conn.prepareStatement(query);
            st.setInt(1, artistId);
            st.setString(2, albumName);
            rs = st.executeQuery();
            if (rs.next()) {
                st.close();
                return true;
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getCountTracks() {
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) " + "FROM tracks_table";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            if (rs.next()) {
                int tracksTotal = rs.getInt(1);
                st.close();
                return tracksTotal;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getFilename(int trackId) {
        ResultSet rs = null;
        try {
            String query = "SELECT filename " + "FROM tracks_table " + "WHERE id=?";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, trackId);
            rs = st.executeQuery();
            if (rs.next()) {
                String result = rs.getString(1);
                st.close();
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public int getCountArtists() {
        int countArtists = 0;
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(id) " + "FROM artists_table";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            while (rs.next()) {
                countArtists = rs.getInt(1);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return countArtists;
    }

    public int getCountAlbums() {
        int countAlbums = 0;
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(id) " + "FROM albums_table";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            while (rs.next()) {
                countAlbums = rs.getInt(1);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return countAlbums;
    }

    public ArrayList<String> getAllArtistNames() {
        ArrayList<String> artistNames = new ArrayList<String>();
        ResultSet rs = null;
        try {
            String query = "SELECT artist_name " + "FROM artists_table";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                artistNames.add(name);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return artistNames;
    }

    public ArrayList<String> getAllArtistNamesAscOrder() {
        ArrayList<String> artistNames = new ArrayList<String>();
        ResultSet rs = null;
        try {
            String query = "SELECT artist_name " + "FROM artists_table " + "ORDER BY artist_name ASC";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                artistNames.add(name);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return artistNames;
    }

    public ArrayList<String> getAllArtistNamesDescOrder() {
        ArrayList<String> artistNames = new ArrayList<String>();
        ResultSet rs = null;
        try {
            String query = "SELECT artist_name " + "FROM artists_table " + "ORDER BY artist_name DESC";
            PreparedStatement st = conn.prepareCall(query);
            rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                artistNames.add(name);
            }
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return artistNames;
    }

    public int getTracksBetweenTimeRange(int minTime, int maxTime) {
        int tracksTotal = 0;
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) " + "FROM information_tracks_table " + "WHERE duration BETWEEN ? AND ?";
            PreparedStatement st = conn.prepareCall(query);
            st.setInt(1, minTime);
            st.setInt(2, maxTime);
            rs = st.executeQuery();
            if (rs.next()) {
                tracksTotal = rs.getInt(1);
                st.close();
                return tracksTotal;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tracksTotal;
    }

    @Override
    public void registerObserver(Observer obs) {
        synchronized (obsLock) {
            observers.add(obs);
        }
    }

    @Override
    public void unregisterObserver(Observer obs) {
        synchronized (obsLock) {
            observers.remove(obs);
        }
    }

    @Override
    public void updateObservers() {
        synchronized (obsLock) {
            for (Observer obs : observers) {
                obs.update(this, new Object());
            }
        }
    }
}
