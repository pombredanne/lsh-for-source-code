package model;

import java.net.URL;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import controler.http.HttpFetch;

/**
 * Computes the MD5 hash of the favicon
 */
public class ChallengeFaviconMd5 extends Challenge {

    private URL target;

    /**
     * constructor
     *
     * @param target the url to target
     */
    public ChallengeFaviconMd5(URL target) {
        super("FaviconMd5");
        this.target = target;
    }

    /**
     * test logic :
     * retrieve [target]/favicon.ico
     * and, if found, computes its md5
     * This test is very reliable to detect two identical applications,
     * but can completely differs if the applications are two slightly
     * versions of a same one.
     */
    public void perform() {
        state = RUNNING;
        StringBuilder str = new StringBuilder(target.getProtocol());
        str.append("://");
        str.append(target.getAuthority());
        str.append("/favicon.ico");
        System.err.println("Favicon is @ " + str.toString());
        HttpFetch favicon;
        try {
            favicon = new HttpFetch(str.toString());
        } catch (MalformedURLException e) {
            state = FAILED;
            return;
        }
        favicon.setTimeout(2000);
        try {
            MessageDigest hash = MessageDigest.getInstance("MD5");
            byte[] content = favicon.getBytes();
            if (content == null) {
                state = FAILED;
                return;
            }
            hash.update(content);
            byte[] digest = hash.digest();
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < digest.length; ++i) {
                int value = digest[i];
                if (value < 0) {
                    value += 256;
                }
                if (value <= 0x0f) {
                    res.append("0");
                }
                res.append(Integer.toHexString(value));
            }
            if (res.toString().equals("d41d8cd98f00b204e9800998ecf8427e")) {
                state = FAILED;
                testResults = "";
            } else {
                state = SUCCEEDED;
                testResults = res.toString();
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("MD5 algorithm not available on this system, test skipped!");
            state = FAILED;
            return;
        }
    }

    /**
     * Compare this challenge with another successfully runned one.
     * The result is either zero or 100%
     * as md5 comparison is boolean.
     *
     * @param challenge a challenge to be compared to
     * @return a number between zero and one that represents
     * how similar the two challenges' results are
     */
    private double compareTo(ChallengeFaviconMd5 challenge) {
        if (challenge.getState().compareTo(Challenge.SUCCEEDED) == 0 && state.compareTo(Challenge.SUCCEEDED) == 0 && challenge.getTestResults().compareTo(testResults) == 0) {
            return 1.0;
        }
        return 0.0;
    }

    /**
     * This exported method is only a wrapper for the private one
     * that really do the job. This one adds a check to ensure we're comparing
     * two similar challenges.
     *
     * @param challenge a challenge to be compared to
     * @return a number between zero and one that represents
     * @throws model.Challenge.InvalidChallengeComparison
     */
    public double compareTo(Challenge challenge) throws InvalidChallengeComparison {
        if (challenge instanceof ChallengeFaviconMd5) {
            return this.compareTo((ChallengeFaviconMd5) challenge);
        }
        throw new InvalidChallengeComparison("Invalid attempt to compare challenge " + getName() + " with challenge " + challenge.getName());
    }
}
