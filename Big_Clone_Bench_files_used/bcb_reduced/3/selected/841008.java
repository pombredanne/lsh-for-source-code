package com.liferay.portal.ejb;

import java.util.Date;
import java.util.Iterator;
import com.liferay.counter.ejb.CounterManagerUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.PasswordTracker;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.Encryptor;
import com.liferay.util.GetterUtil;
import com.liferay.util.Time;

/**
 * <a href="PasswordTrackerLocalManagerImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.2 $
 *
 */
public class PasswordTrackerLocalManagerImpl implements PasswordTrackerLocalManager {

    public void deleteAll(String userId) throws SystemException {
        PasswordTrackerUtil.removeByUserId(userId);
    }

    public boolean isValidPassword(String userId, String password) throws PortalException, SystemException {
        int passwordsRecycle = GetterUtil.getInteger(PropsUtil.get(PropsUtil.PASSWORDS_RECYCLE));
        if (passwordsRecycle > 0) {
            String newEncPwd = Encryptor.digest(password);
            User user = UserUtil.findByPrimaryKey(userId);
            String oldEncPwd = user.getPassword();
            if (!user.isPasswordEncrypted()) {
                oldEncPwd = Encryptor.digest(user.getPassword());
            }
            if (oldEncPwd.equals(newEncPwd)) {
                return false;
            }
            Date now = new Date();
            Iterator itr = PasswordTrackerUtil.findByUserId(userId).iterator();
            while (itr.hasNext()) {
                PasswordTracker passwordTracker = (PasswordTracker) itr.next();
                Date recycleDate = new Date(passwordTracker.getCreateDate().getTime() + Time.DAY * passwordsRecycle);
                if (recycleDate.after(now)) {
                    if (passwordTracker.getPassword().equals(newEncPwd)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void trackPassword(String userId, String encPwd) throws PortalException, SystemException {
        String passwordTrackerId = Long.toString(CounterManagerUtil.increment(PasswordTracker.class.getName()));
        PasswordTracker passwordTracker = PasswordTrackerUtil.create(passwordTrackerId);
        passwordTracker.setUserId(userId);
        passwordTracker.setCreateDate(new Date());
        passwordTracker.setPassword(encPwd);
        PasswordTrackerUtil.update(passwordTracker);
    }
}
