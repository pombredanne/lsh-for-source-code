package plugin.voting;

import eu.popeye.networkabstraction.communication.message.PopeyeMessage;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class used for exchanging notification messages with other users
 * @author Marcel Arrufat Arias
 */
public class PollSetMessage implements PopeyeMessage {

    private String username;

    private String question;

    private String title;

    private String[] choices;

    private String voteId;

    public PollSetMessage(String username, String question, String title, String[] choices) {
        this.username = username;
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        String id = username + String.valueOf(System.nanoTime());
        m.update(id.getBytes(), 0, id.length());
        voteId = new BigInteger(1, m.digest()).toString(16);
        this.question = question;
        this.title = title;
        this.choices = choices;
    }

    /**
     * @return Returns the type.
     */
    public String getUsername() {
        return username;
    }

    public String getVoteId() {
        return voteId;
    }

    public String getTitle() {
        if (title.trim().length() == 0) return "<No Title>";
        return title;
    }

    /**
     * @return Returns the shuffledWord.
     */
    public String getQuestion() {
        if (question.trim().length() == 0) return "<Empty question>";
        return question;
    }

    /**
     * @return the currentScore
     */
    public String[] getChoices() {
        return choices;
    }

    public String getChoiceAt(int p) {
        return choices[p];
    }

    public String toString() {
        return getTitle() + " by " + getUsername();
    }
}
