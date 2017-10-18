package net.kano.joustsim.oscar;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.snaccmd.ExtraInfoData;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.icon.IconRequestListener;
import net.kano.joustsim.oscar.oscar.service.icon.IconService;
import net.kano.joustsim.oscar.oscar.service.icon.IconServiceArbiter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.beans.PropertyChangeEvent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class BuddyIconTracker {

    private static final Logger LOGGER = Logger.getLogger(BuddyIconTracker.class.getName());

    private static final long RE_REQUEST_INTERVAL = 60 * 1000;

    private static final long MAX_REREQUEST_ICON_TIMES = 4;

    private final AimConnection conn;

    private final Map<BuddyIconRequest, Long> pendingRequests = new HashMap<BuddyIconRequest, Long>();

    private final Map<ExtraInfoData, ByteBlock> iconCache = new HashMap<ExtraInfoData, ByteBlock>();

    private final IconRequestListener iconRequestListener = new MyIconRequestListener();

    private Timer rerequestIconsTimer;

    private boolean enabled = true;

    public BuddyIconTracker(AimConnection aconn) {
        this.conn = aconn;
        BuddyInfoManager mgr = conn.getBuddyInfoManager();
        mgr.addGlobalBuddyInfoListener(new MyGlobalBuddyInfoListener());
        conn.addStateListener(new StateListener() {

            public void handleStateChange(StateEvent event) {
                if (event.getNewState().isFinished()) {
                    if ((rerequestIconsTimer != null)) {
                        rerequestIconsTimer.cancel();
                        rerequestIconsTimer = null;
                    }
                    synchronized (BuddyIconTracker.this) {
                        pendingRequests.clear();
                    }
                }
            }
        });
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) iconCache.clear();
    }

    private synchronized void clearRequest(BuddyIconRequest iconRequest) {
        pendingRequests.remove(iconRequest);
        if ((rerequestIconsTimer != null) && pendingRequests.isEmpty()) {
            rerequestIconsTimer.cancel();
            rerequestIconsTimer = null;
        }
    }

    private synchronized void updateRequestTime(BuddyIconRequest iconRequest) {
        if ((rerequestIconsTimer == null) && pendingRequests.isEmpty()) {
            rerequestIconsTimer = new Timer(true);
            rerequestIconsTimer.schedule(new RerequestIconsTask(), RE_REQUEST_INTERVAL, RE_REQUEST_INTERVAL);
        }
        pendingRequests.put(iconRequest, System.currentTimeMillis());
    }

    public synchronized long getRequestTime(ExtraInfoData block, Screenname buddy) {
        Long time = pendingRequests.get(new BuddyIconRequest(buddy, block));
        return time == null ? 0 : time;
    }

    @Nullable
    public synchronized ByteBlock getIconDataForHash(ExtraInfoData hash) {
        return iconCache.get(hash);
    }

    @Nullable
    public ByteBlock getBuddyIconData(Screenname screenname) {
        BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(screenname);
        ExtraInfoData hash = buddyInfo.getIconHash();
        if (hash == null) return null;
        return getIconDataForHash(hash);
    }

    public ExtraInfoData addToCache(Screenname buddy, ByteBlock iconData) {
        DefensiveTools.checkNull(iconData, "iconData");
        ExtraInfoData iconInfo = new ExtraInfoData(ExtraInfoData.FLAG_HASH_PRESENT, computeIconHash(iconData));
        storeInCache(iconInfo, buddy, iconData);
        return iconInfo;
    }

    private synchronized void storeInCache(ExtraInfoData hash, Screenname buddy, @NotNull ByteBlock iconData) {
        LOGGER.fine("Cached icon data for " + hash);
        clearRequest(new BuddyIconRequest(buddy, hash));
        iconCache.put(hash, ByteBlock.wrap(iconData.toByteArray()));
    }

    private static ByteBlock computeIconHash(ByteBlock iconData) {
        ByteBlock hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            hash = ByteBlock.wrap(digest.digest(iconData.toByteArray()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return hash;
    }

    /**
   * Returns whether the request time was updated; returns false if
   * this icon has been re-requested too many times. If this method
   * returns false, this request should not be re-requested again.
   */
    private synchronized boolean updateRequestTimeIfPossible(BuddyIconRequest iconRequest) {
        if (iconRequest.getTimesRequested() < MAX_REREQUEST_ICON_TIMES) {
            updateRequestTime(iconRequest);
            iconRequest.incrementTimesRequested();
            return true;
        } else {
            clearRequest(iconRequest);
            return false;
        }
    }

    private void requestIcon(BuddyIconRequest iconRequest) {
        if (updateRequestTimeIfPossible(iconRequest)) {
            IconServiceArbiter iconArbiter = conn.getExternalServiceManager().getIconServiceArbiter();
            if (iconArbiter != null) {
                if (iconRequest.screenname != null) {
                    LOGGER.fine("Requesting buddy icon for " + iconRequest.screenname);
                }
                iconArbiter.addIconRequestListener(iconRequestListener);
                iconArbiter.requestIcon(iconRequest.screenname, iconRequest.data);
            } else {
                LOGGER.warning("icon arbiter is null!");
            }
        }
    }

    private void storeBuddyIconData(Screenname buddy, ExtraInfoData iconInfo, ByteBlock iconData) {
        BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(buddy);
        buddyInfo.setIconDataIfHashMatches(iconInfo, iconData);
    }

    private static class BuddyIconRequest {

        private final Screenname screenname;

        private final ExtraInfoData data;

        private int timesRequested;

        public BuddyIconRequest(Screenname screenname, ExtraInfoData data) {
            this.screenname = screenname;
            this.data = data;
            timesRequested = 0;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BuddyIconRequest that = (BuddyIconRequest) o;
            return data.equals(that.data) && screenname.equals(that.screenname);
        }

        public int hashCode() {
            return 31 * screenname.hashCode() + data.hashCode();
        }

        public int getTimesRequested() {
            return timesRequested;
        }

        public void incrementTimesRequested() {
            timesRequested++;
        }
    }

    private class MyGlobalBuddyInfoListener implements GlobalBuddyInfoListener {

        public void newBuddyInfo(BuddyInfoManager manager, Screenname buddy, BuddyInfo info) {
            if (!isEnabled()) return;
            handleNewIconHashForBuddy(buddy, info.getIconHash());
        }

        public void buddyInfoChanged(BuddyInfoManager manager, Screenname buddy, BuddyInfo info, PropertyChangeEvent event) {
            if (!isEnabled()) return;
            if (event.getPropertyName().equals(BuddyInfo.PROP_ICON_HASH)) {
                ExtraInfoData newHash = (ExtraInfoData) event.getNewValue();
                handleNewIconHashForBuddy(buddy, newHash);
            }
        }

        private void handleNewIconHashForBuddy(Screenname buddy, ExtraInfoData newHash) {
            LOGGER.fine("Got new icon hash for " + buddy + ": " + newHash);
            if (newHash == null) {
                storeBuddyIconData(buddy, newHash, null);
            } else {
                ByteBlock iconData = getIconDataForHash(newHash);
                if (iconData == null) {
                    requestIcon(new BuddyIconRequest(buddy, newHash));
                } else {
                    LOGGER.finer("Icon data was already cached for " + buddy);
                    storeBuddyIconData(buddy, newHash, iconData);
                }
            }
        }

        public void receivedStatusUpdate(BuddyInfoManager manager, Screenname buddy, BuddyInfo info) {
        }
    }

    private class RerequestIconsTask extends TimerTask {

        public void run() {
            LOGGER.fine("RerequestIconsTask invoked...");
            List<BuddyIconRequest> rereq = new ArrayList<BuddyIconRequest>();
            synchronized (BuddyIconTracker.this) {
                for (Map.Entry<BuddyIconRequest, Long> entry : pendingRequests.entrySet()) {
                    if (System.currentTimeMillis() - entry.getValue() > RE_REQUEST_INTERVAL) {
                        rereq.add(entry.getKey());
                    }
                }
            }
            for (BuddyIconRequest iconRequest : rereq) {
                LOGGER.fine("Re-requesting buddy icon for " + iconRequest.screenname + " (" + iconRequest.data + ")");
                requestIcon(iconRequest);
            }
        }
    }

    private class MyIconRequestListener implements IconRequestListener {

        public void buddyIconCleared(IconService service, Screenname screenname, ExtraInfoData data) {
            if (!isEnabled()) return;
            LOGGER.fine("Buddy icon cleared for " + screenname + ": " + data);
            storeBuddyIconData(screenname, data, null);
        }

        public void buddyIconUpdated(IconService service, Screenname buddy, ExtraInfoData hash, ByteBlock iconData) {
            if (!isEnabled()) return;
            storeInCache(hash, buddy, iconData);
            BuddyInfo buddyInfo = conn.getBuddyInfoManager().getBuddyInfo(buddy);
            LOGGER.fine("Storing buddy icon for " + buddy);
            if (!buddyInfo.setIconDataIfHashMatches(hash, iconData)) {
                LOGGER.info("Buddy icon data for " + buddy + " set too " + "late - hash " + hash + " no longer matches");
            }
        }
    }
}
