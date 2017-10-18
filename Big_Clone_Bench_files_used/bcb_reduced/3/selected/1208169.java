package musicagent;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.security.MessageDigest;

/**
 *
 * @author Kyro Rear
 */
public class Song {

    public static final int HATE = -2;

    public static final int DISLIKE = -1;

    public static final int MEH = 0;

    public static final int LIKE = 1;

    public static final int LOVE = 2;

    public static final int UNRATED = -42;

    private String title;

    private String artist;

    private String ID;

    private String location;

    private String hash;

    private ArrayList<String> tags;

    private int rating;

    private int overplay;

    private boolean free;

    public Song(String Location, String Title, String Artist) {
        location = Location;
        title = Title;
        artist = Artist;
        rating = UNRATED;
        overplay = 0;
        tags = new ArrayList<String>();
        ID = title.toLowerCase() + artist.toLowerCase();
        free = false;
    }

    /**
     * 
     * @param s takes a string matching the format created by the toSave method
     */
    public Song(String s) {
        StringTokenizer tokenizer = new StringTokenizer(s, ";");
        free = false;
        title = tokenizer.nextToken();
        artist = tokenizer.nextToken();
        location = tokenizer.nextToken();
        rating = Integer.parseInt(tokenizer.nextToken());
        overplay = Integer.parseInt(tokenizer.nextToken());
        String temp = tokenizer.nextToken();
        tokenizer = new StringTokenizer(temp, ",[] ");
        tags = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            tags.add(tokenizer.nextToken());
        }
        byte[] bytes = new byte[40];
        try {
            MessageDigest hasher = MessageDigest.getInstance("SHA-1");
            hasher.update(title.getBytes());
            hasher.update(artist.getBytes());
            bytes = hasher.digest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String toSave() {
        if (title != null) {
            return title + ";" + artist + ";" + location + ";" + rating + ";" + overplay + ";" + tags;
        } else {
            return null;
        }
    }

    public boolean isFree() {
        return free;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getID() {
        return ID;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int Rating) {
        rating = Rating;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String Location) {
        location = Location;
    }

    public int getOverplay() {
        return overplay;
    }

    public String getHash() {
        return hash;
    }

    public void incrementOverplay() {
        switch(rating) {
            case HATE:
                overplay += 16;
                break;
            case DISLIKE:
                overplay += 8;
                break;
            case MEH:
                overplay += 4;
                break;
            case LIKE:
                overplay += 2;
                break;
            case LOVE:
                overplay += 1;
                break;
            default:
                overplay += 3;
        }
    }

    public String getTags() {
        return tags.toString();
    }

    public void addTag(String Tag) {
        tags.add(Tag);
    }

    public void removeTag(String Tag) {
        tags.remove(Tag);
    }
}
