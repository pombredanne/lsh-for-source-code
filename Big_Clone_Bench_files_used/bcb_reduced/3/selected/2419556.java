package maudio.extractors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import maudio.exceptions.CannotRetrieveMP3TagException;
import java.io.File;
import java.security.MessageDigest;
import javax.imageio.ImageIO;
import maudio.entities.AudioMetadata;
import maudio.extractors.interfaces.IAudioMetadataExtractor;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import utils.FileUtil;
import utils.GraphicsUtilities;
import utils.ImageUtils;
import utils.Util;

/**
 * Implementation of the AudioMetadataExtractor for extracting Mp3 tags from files.
 * Uses JaudioTagger to extract tags from mp3 files.
 * @author Ricardo
 */
public class MP3AudioMetadataExtractor implements IAudioMetadataExtractor {

    private static String validGenres[] = { "Jazz", "Gospel", "Blues", "Metal", "Rock", "Pop", "Disco", "Funk", "R&B", "Rap", "Hip-Hop", "Electro", "Latin", "Classical", "Soundtrack", "World", "Reggae", "Soul", "African", "Other" };

    private BufferedImage defaultImage;

    private String defaultImageHash;

    public MP3AudioMetadataExtractor() {
        try {
            defaultImage = ImageIO.read(getClass().getResource("/images/not_available.jpg"));
            if (defaultImage.getWidth() > 128) {
                defaultImage = GraphicsUtilities.createThumbnailFast(defaultImage, 128);
            }
            byte[] imageBytes = ImageUtils.toByteArray(defaultImage);
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(imageBytes);
                byte[] hash = md.digest();
                defaultImageHash = Util.returnHex(hash);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method retrieves the metadata of the desired track.
     * @param file the filename of the track we want to retrieve
     * @return
     */
    @Override
    public AudioMetadata getAudioMetadata(String filename) throws CannotRetrieveMP3TagException {
        AudioMetadata metadata = new AudioMetadata();
        File sourceFile = new File(filename);
        AudioFile mp3file;
        String artist, album, title, bitrate, year, genre;
        int duration, trackNumber;
        BufferedImage artwork = null;
        try {
            mp3file = AudioFileIO.read(sourceFile);
            Tag tag = mp3file.getTag();
            AudioHeader audioHeader = mp3file.getAudioHeader();
            artist = validatedArtist(tag.getFirst(FieldKey.ARTIST));
            album = validatedAlbum(tag.getFirst(FieldKey.ALBUM));
            title = validateTitle(tag.getFirst(FieldKey.TITLE));
            duration = audioHeader.getTrackLength();
            bitrate = audioHeader.getBitRate();
            year = validatedYear(tag.getFirst(FieldKey.YEAR));
            genre = validatedGenre(tag.getFirst(FieldKey.GENRE));
            trackNumber = 0;
            try {
                artwork = tag.getFirstArtwork().getImage();
                if (artwork.getWidth() > 128) {
                    artwork = GraphicsUtilities.createThumbnailFast(artwork, 128);
                }
                trackNumber = 0;
            } catch (Exception e) {
                System.out.println("Trying to find artwork in the folder!");
                boolean found = false;
                String parentFolderStr = sourceFile.getParent();
                File parentFolder = new File(parentFolderStr);
                if (parentFolder.isDirectory()) {
                    File[] files = FileUtil.findAllFilesRecursively(parentFolder);
                    for (File f : files) {
                        if (f.getAbsolutePath().toLowerCase().contains("cover") || f.getAbsolutePath().toLowerCase().contains("capa") || f.getAbsolutePath().toLowerCase().contains("fronte")) {
                            BufferedImage cover = ImageIO.read(f);
                            found = true;
                            artwork = cover;
                            if (artwork.getWidth() > 128) {
                                artwork = GraphicsUtilities.createThumbnailFast(artwork, 128);
                            }
                            System.out.println("Artwork found!");
                            break;
                        }
                    }
                    if (!found) {
                        for (File f : files) {
                            if (f.getAbsolutePath().endsWith(".jpg") || f.getAbsolutePath().endsWith(".png")) {
                                BufferedImage cover = ImageIO.read(f);
                                found = true;
                                artwork = cover;
                                if (artwork.getWidth() > 128) {
                                    artwork = GraphicsUtilities.createThumbnailFast(artwork, 128);
                                }
                                System.out.println("Artwork found!");
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    artwork = defaultImage;
                }
            }
            metadata.setAuthor(artist);
            metadata.setAlbum(album);
            metadata.setTitle(title);
            metadata.setDuration(duration);
            metadata.setBitrate(bitrate);
            metadata.setYear(year);
            metadata.setGenre(genre);
            metadata.setTrackNumber(trackNumber);
            metadata.setArtwork(artwork);
        } catch (Exception ex) {
            throw new CannotRetrieveMP3TagException("Can't retrieve tag from MP3 file.\nPossible reason: " + ex, filename);
        }
        return metadata;
    }

    public String getDefaultImageHash() {
        return defaultImageHash;
    }

    private String validateTitle(String firstTitle) {
        if (firstTitle.equals("")) {
            return "Unknown";
        } else {
            return firstTitle;
        }
    }

    private String validatedAlbum(String album) {
        String albumTest = album.toLowerCase();
        if (albumTest.contains("unknown") || albumTest.contains("desconhecido") || albumTest.contains("sconosciuto") || albumTest.contains("desconocido") || albumTest.equals("")) {
            return "Unknown";
        }
        return album;
    }

    private String validatedArtist(String artist) {
        String artistTest = artist.toLowerCase();
        if (artistTest.contains("unknown") || artistTest.contains("desconhecido") || artistTest.contains("sconosciuto") || artistTest.contains("desconocido") || artistTest.equals("")) {
            return "Unknow";
        }
        return artist;
    }

    private String validatedGenre(String genre) {
        String originalGenre;
        String validatedGenre = "";
        String testGenre = genre.toLowerCase();
        for (String validGenre : validGenres) {
            originalGenre = validGenre;
            validGenre = validGenre.toLowerCase();
            if (testGenre.equals(validGenre) || validGenre.contains(testGenre) || testGenre.contains(validGenre)) {
                validatedGenre = originalGenre;
                break;
            } else {
                validatedGenre = "Other";
            }
        }
        return validatedGenre;
    }

    private String validatedYear(String year) {
        String validatedYear = "";
        try {
            Integer.parseInt(year);
            validatedYear = year;
        } catch (NumberFormatException e) {
            validatedYear += Integer.MIN_VALUE;
        }
        return validatedYear;
    }
}
