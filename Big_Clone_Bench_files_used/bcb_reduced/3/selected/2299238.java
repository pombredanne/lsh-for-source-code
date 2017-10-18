package com.evolution.player.jamendo.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.evolution.player.internal.jamendo.core.json.JSONArray;
import com.evolution.player.internal.jamendo.core.json.JSONException;
import com.evolution.player.internal.jamendo.core.json.JSONObject;

/**
 * @since 0.5
 */
public class JamendoUtils {

    public static class SongInfo {

        public String id;

        public String name;

        public String mbid;

        public SongInfo(String trackId, String songName, String musicbrainzId) {
            id = trackId;
            name = songName;
            mbid = musicbrainzId;
        }
    }

    public static class AlbumInfo {

        public String id;

        public String name;

        public AlbumInfo(String albumId, String albumName) {
            id = albumId;
            name = albumName;
        }
    }

    public static class ArtistInfo {

        public String mbid;

        public String id;

        public String name;

        public ArtistInfo(String id, String artistName, String musicbrainzArtistId) {
            this.id = id;
            name = artistName;
            mbid = musicbrainzArtistId;
        }
    }

    public static URL getAlbumUrl(String trackId) throws IOException {
        JamendoReader albumReader = new JamendoReader(new URL("http://api.jamendo.com/get2/url+id/album/json/?track_id=" + trackId));
        try {
            albumReader.run();
            JSONObject albumResult = albumReader.getJSONResult();
            return new URL(albumResult.getString("url"));
        } catch (JSONException e) {
            return null;
        }
    }

    public static SongInfo getSongInfo(String trackId) throws IOException {
        JamendoReader songReader = new JamendoReader(new URL("http://api.jamendo.com/get2/mbgid+name/track/json/?id=" + trackId));
        try {
            songReader.run();
            JSONObject songResult = songReader.getJSONResult();
            String songName = songResult.getString("name");
            String musicbrainzId = songResult.getString("mbgid").trim();
            if (musicbrainzId.length() == 0) {
                musicbrainzId = null;
            }
            return new SongInfo(trackId, songName, musicbrainzId);
        } catch (JSONException e) {
            return null;
        }
    }

    public static AlbumInfo getAlbumInfo(String trackId) throws IOException {
        JamendoReader albumReader = new JamendoReader(new URL("http://api.jamendo.com/get2/id+name/album/json/?track_id=" + trackId));
        try {
            albumReader.run();
            JSONObject albumResult = albumReader.getJSONResult();
            String albumName = albumResult.getString("name");
            String albumId = albumResult.getString("id");
            return new AlbumInfo(albumId, albumName);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public static ArtistInfo getArtistInfo(String albumId) throws IOException {
        JamendoReader artistReader = new JamendoReader(new URL("http://api.jamendo.com/get2/mbgid+name+id/artist/json/?album_id=" + albumId));
        try {
            artistReader.run();
            JSONObject artistResult = artistReader.getJSONResult();
            String artistName = artistResult.getString("name");
            String musicbrainzArtistId = artistResult.getString("mbgid").trim();
            if (musicbrainzArtistId.length() == 0) {
                musicbrainzArtistId = null;
            }
            String id = artistResult.getString("id");
            return new ArtistInfo(id, artistName, musicbrainzArtistId);
        } catch (JSONException e) {
            return null;
        }
    }

    public static SongInfo[] getAlbumTrackInfos(String albumId) throws IOException {
        JamendoReader trackReader = new JamendoReader(new URL("http://api.jamendo.com/get2/id+mbgid+name/track/json/?album_id=" + albumId));
        try {
            trackReader.run();
            JSONArray tracks = trackReader.getJSONArrayResult();
            SongInfo[] result = new SongInfo[tracks.length()];
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject track = tracks.getJSONObject(i);
                result[i] = new SongInfo(track.getString("id"), track.getString("name"), track.getString("mbgid"));
            }
            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    public static AlbumInfo[] getArtistAlbumInfos(String artistId) throws IOException {
        JamendoReader albumReader = new JamendoReader(new URL("http://api.jamendo.com/get2/id+name/album/json/?artist_id=" + artistId));
        try {
            albumReader.run();
            JSONArray tracks = albumReader.getJSONArrayResult();
            AlbumInfo[] result = new AlbumInfo[tracks.length()];
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject album = tracks.getJSONObject(i);
                result[i] = new AlbumInfo(album.getString("id"), album.getString("name"));
            }
            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    public static String[] getAlbumTracks(String albumId) throws IOException {
        JamendoReader trackReader = new JamendoReader(new URL("http://api.jamendo.com/get2/id/track/json/?album_id=" + albumId));
        trackReader.run();
        return trackReader.getResult();
    }

    public static TrackInfo getInfo(String id) throws IOException {
        SongInfo songInfo = getSongInfo(id);
        if (songInfo == null) return null;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        AlbumInfo albumInfo = getAlbumInfo(id);
        if (albumInfo == null) return null;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        ArtistInfo artistInfo = getArtistInfo(albumInfo.id);
        if (artistInfo == null) return null;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        int trackNumber = -1;
        String[] tracks = getAlbumTracks(albumInfo.id);
        if (tracks != null) trackNumber = indexOf(tracks, id);
        TrackInfo result = new TrackInfo();
        result.songName = songInfo.name;
        result.albumName = albumInfo.name;
        result.artistName = artistInfo.name;
        result.trackNumber = trackNumber;
        result.musicbrainzTrack = songInfo.mbid;
        result.musicbrainzArtist = artistInfo.mbid;
        return result;
    }

    private static int indexOf(String[] ids, String id) {
        for (int i = 0; i < ids.length; i++) {
            if (id.equals(ids[i])) return i + 1;
        }
        return -1;
    }

    public static int getHash(InputStream stream) throws IOException {
        InputStream bufferStream = new BufferedInputStream(stream);
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int totalRead = 0;
            int read = bufferStream.read();
            while (read != -1 && totalRead < 100 * 1000) {
                totalRead++;
                if (totalRead >= 50 * 1000 && totalRead <= 100 * 1000) {
                    digest.update((byte) read);
                }
                read = bufferStream.read();
            }
            return readInteger(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            return -1;
        } finally {
            bufferStream.close();
        }
    }

    private static int readInteger(byte[] bytes) {
        int result = ((bytes[0] & 0xff));
        result <<= 8;
        result |= ((bytes[1] & 0xff));
        result <<= 8;
        result |= ((bytes[2] & 0xff));
        result <<= 8;
        result |= ((bytes[3] & 0xff));
        return result;
    }
}
