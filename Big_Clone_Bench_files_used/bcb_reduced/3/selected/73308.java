package com.ingenta.howto;

import com.ingenta.clownbike.DatabaseTransaction;
import com.ingenta.clownbike.odb.User;
import com.ingenta.clownbike.odb.UserManager;
import com.ingenta.clownbike.Shell;
import com.ingenta.common.security.MD5;
import com.ingenta.howto.odb.HowTo;
import com.ingenta.howto.odb.HowToManager;
import java.util.Date;

public class HowToInit extends Shell {

    public static void main(String[] args) {
        try {
            new HowToInit(args);
        } catch (Throwable t) {
            t.printStackTrace();
            Throwable cause = t.getCause();
            while (cause != null) {
                System.err.println(cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
        }
    }

    public HowToInit(String[] args) throws Exception {
        args = initialize(args);
        DatabaseTransaction transaction = getDatabase().createTransaction(getIndex());
        HowToManager howToManager = (HowToManager) transaction.getManager(HowTo.class);
        UserManager userManager = (UserManager) transaction.getManager(User.class);
        Date createdOn = new Date();
        User createdBy = userManager.create(transaction, "John Smith", "jsmith", MD5.digest(""), "jsmith@ingenta.com", createdOn, null);
        for (int i = 0; i < 100; i++) {
            getLogger().info("creating how-to #" + i);
            String title = "title " + i;
            String content = CONTENT + "<p>content " + i + "</p>";
            HowTo howto = howToManager.create(transaction, title, content, createdOn, createdBy);
        }
        transaction.commit();
    }

    private static String CONTENT = "<p>Upscaling the resurgent networking exchange solutions, achieving a breakaway systemic electronic data interchange system synchronization, thereby exploiting technical environments for mission critical broad based capacity constrained systems.</p>" + "<p>Fundamentally transforming well designed actionable information whose semantic content is virtually null.</p>";
}
