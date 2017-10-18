package org.opencdspowered.opencds.core.update;

import org.opencdspowered.opencds.core.util.Constants;
import javolution.util.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.math.BigInteger;

/**
 * The update checker checks if there are any updates available.
 * 
 * @author  Lars 'Levia' Wesselius
*/
public class UpdateChecker {

    private Iterator<Update> m_FoundUpdates;

    private int m_NumberOfUpdates;

    /**
     * UpdateChecker constructor.
    */
    public UpdateChecker() {
    }

    /**
     * Check for updates.
     * 
     * @param   ignore  Ignore updates starting with given string.
     * @return  A iterator with all the updates.
    */
    public Iterator<Update> checkForUpdates(String ignore) {
        FastTable<Update> updateVector = new FastTable<Update>();
        try {
            URL scriptUrl = new URL(Constants.UPDATE_SCRIPT);
            URLConnection con = scriptUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String completeLine = "";
            String line = "";
            while ((line = reader.readLine()) != null) {
                completeLine += line;
            }
            String[] stringUpdates = completeLine.split(";");
            for (int i = 0; i != stringUpdates.length; ++i) {
                Update update = parseUpdate(stringUpdates[i]);
                if (update != null && !update.getPath().startsWith("./changelog.html")) {
                    if (ignore != null) {
                        if (!update.getPath().startsWith(ignore)) {
                            updateVector.add(update);
                        }
                    } else {
                        updateVector.add(update);
                    }
                }
            }
        } catch (Exception e) {
            org.opencdspowered.opencds.core.logging.Logger.getInstance().logException(e);
        }
        m_FoundUpdates = updateVector.iterator();
        m_NumberOfUpdates = updateVector.size();
        return m_FoundUpdates;
    }

    /**
     * Check for updates.
     * 
     * @return  A Iterator with all the updates.
    */
    public Iterator<Update> checkForUpdates() {
        return checkForUpdates(null);
    }

    /**
     * Get the number of updates found.
     * 
     * @return  The number of updates found.
    */
    public int getUpdateCount() {
        return m_NumberOfUpdates;
    }

    private Update parseUpdate(String update) {
        System.out.println(update);
        try {
            String[] parts = update.split(":");
            Update dl = new Update(Integer.valueOf(parts[1]), parts[3]);
            File file = new File(parts[3]);
            if (file.exists()) {
                dl.setUpdateExists(true);
                MessageDigest digest = MessageDigest.getInstance("MD5");
                InputStream is = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int read = 0;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();
                BigInteger bigInt = new BigInteger(1, md5sum);
                String output = bigInt.toString(16);
                if (output.length() < 32) {
                    int remaining = 32 - output.length();
                    for (int i = 0; i != remaining; ++i) {
                        output = "0" + output;
                    }
                }
                is.close();
                if (!output.equals(parts[2])) {
                    return dl;
                }
            } else {
                dl.setUpdateExists(false);
                return dl;
            }
            return null;
        } catch (Exception e) {
            org.opencdspowered.opencds.core.logging.Logger.getInstance().logException(e);
            return null;
        }
    }
}
