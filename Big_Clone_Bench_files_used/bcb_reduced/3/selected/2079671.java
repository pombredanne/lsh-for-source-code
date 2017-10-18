package edu.vt.middleware.password;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>PasswordHistoryRule</code> contains methods for determining if a
 * password matches one of any previous password a user has choosen. If no
 * password history has been set or an empty history has been set, then
 * passwords will meet this rule.
 *
 * @author  Middleware Services
 * @version  $Revision: 1253 $ $Date: 2010-04-20 10:54:19 -0400 (Tue, 20 Apr 2010) $
 */
public class PasswordHistoryRule extends AbstractDigestRule {

    /** password history. */
    private List<String> history = new ArrayList<String>();

    /**
   * This will add the supplied password to the list of history passwords.
   *
   * @param  password  <code>String</code> to add to history
   */
    public void addHistory(final String password) {
        synchronized (this.history) {
            doAddHistory(password);
        }
    }

    private void doAddHistory(final String password) {
        if (password != null) {
            synchronized (this.history) {
                this.history.add(password);
            }
        }
    }

    /**
   * This will add the supplied passwords to the list of history passwords.
   *
   * @param  passwords  <code>String[]</code> to add to history
   */
    public void addHistory(final String[] passwords) {
        if (passwords != null) {
            synchronized (this.history) {
                for (String s : passwords) {
                    this.doAddHistory(s);
                }
            }
        }
    }

    /**
   * This will add the supplied passwords to the list of history passwords.
   *
   * @param  passwords  <code>List</code> to add to history
   */
    public void addHistory(final List<String> passwords) {
        if (passwords != null) {
            synchronized (this.history) {
                for (String s : passwords) {
                    this.doAddHistory(s);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public boolean verifyPassword(final Password password) {
        boolean success = false;
        synchronized (this.history) {
            if (this.history.size() == 0) {
                success = true;
            } else {
                for (String p : this.history) {
                    if (this.digest != null) {
                        final String hash = this.digest.digest(password.getText().getBytes(), this.converter);
                        if (p.equals(hash)) {
                            success = false;
                            this.setMessage(String.format("Password matches one of %s previous passwords", this.history.size()));
                            break;
                        } else {
                            success = true;
                        }
                    } else {
                        if (p.equals(password.getText())) {
                            success = false;
                            this.setMessage(String.format("Password matches one of %s previous passwords", this.history.size()));
                            break;
                        } else {
                            success = true;
                        }
                    }
                }
            }
        }
        return success;
    }
}
