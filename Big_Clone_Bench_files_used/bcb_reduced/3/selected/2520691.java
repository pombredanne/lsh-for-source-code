package my.jasper;

import java.util.*;
import java.sql.*;
import javax.swing.*;
import java.security.*;

public class BaseUtil {

    BaseUser user;

    String ssn;

    String visitID;

    int switchBoardX;

    int switchBoardY;

    int switchBoardW;

    int switchBoardH;

    JFrame currentScreen;

    ScreenAccountsPay screenAccountsPay;

    ScreenMonthlyReport screenMonthlyReport;

    ScreenLogin screenLogin;

    ScreenPatientProfile screenPatientProfile;

    ScreenPatientVisit screenPatientVisit;

    ScreenPatientTreatment screenPatientTreatment;

    ScreenEvaluateService screenEvaluateService;

    ScreenStaffMessage screenStaffMessage;

    ScreenPatientQueue screenPatientQueue;

    ScreenStaffUser screenStaffUser;

    ScreenSwitchBoard screenSwitchBoard;

    ShowScreenAccountsPay showScreenAccountsPay;

    ShowScreenMonthlyReport showScreenMonthlyReport;

    ShowScreenLogin showScreenLogin;

    ShowScreenPatientProfile showScreenPatientProfile;

    ShowScreenPatientVisit showScreenPatientVisit;

    ShowScreenPatientTreatment showScreenPatientTreatment;

    ShowScreenEvaluateService showScreenEvaluateService;

    ShowScreenStaffMessage showScreenStaffMessage;

    ShowScreenPatientQueue showScreenPatientQueue;

    ShowScreenStaffUser showScreenStaffUser;

    ShowScreenSwitchBoard showScreenSwitchBoard;

    private SupportMyDBConnection mdbc;

    private Statement stmt;

    Map<String, Object> screen;

    DateTime udt;

    BaseUtil() throws Exception {
        udt = new DateTime();
        mdbc = new SupportMyDBConnection();
        mdbc.init();
        Connection conn = mdbc.getMyConnection();
        stmt = conn.createStatement();
        user = null;
    }

    public boolean isDBHSQLDB() {
        return true;
    }

    public void setPatientSSN(String ssn) {
        this.ssn = ssn;
    }

    public String getPatientSSN() {
        return ssn;
    }

    public void setPatientVisitID(String visitID) {
        this.visitID = visitID;
    }

    public String getPatientVisitID() {
        return visitID;
    }

    public void closeDB() {
        mdbc.close(stmt);
        mdbc.destroy();
    }

    public void buildScreens(ScreenLogin lui) {
        screenAccountsPay = new ScreenAccountsPay(this);
        screenMonthlyReport = new ScreenMonthlyReport(this);
        this.screenLogin = lui;
        screenPatientProfile = new ScreenPatientProfile(this);
        screenPatientVisit = new ScreenPatientVisit(this);
        screenPatientTreatment = new ScreenPatientTreatment(this);
        screenEvaluateService = new ScreenEvaluateService(this);
        screenStaffMessage = new ScreenStaffMessage(this);
        screenPatientQueue = new ScreenPatientQueue(this);
        screenStaffUser = new ScreenStaffUser(this);
        screenSwitchBoard = new ScreenSwitchBoard(this);
        screen = new HashMap<String, Object>();
        screen.put("ScreenAccountsPay", screenAccountsPay);
        screen.put("ScreenGenMonthlyReport", screenMonthlyReport);
        screen.put("ScreenLoginUI", lui);
        screen.put("ScreenPatientProfile", screenPatientProfile);
        screen.put("ScreenPatientVisit", screenPatientVisit);
        screen.put("ScreenStaffContact", screenPatientTreatment);
        screen.put("ScreenStaffEvaluate", screenEvaluateService);
        screen.put("ScreenStaffMessage", screenStaffMessage);
        screen.put("ScreenStaffPatientQueue", screenPatientQueue);
        screen.put("ScreenStaffUser", screenStaffUser);
        screen.put("ScreenSwitchBoard", screenSwitchBoard);
        showScreenAccountsPay = new ShowScreenAccountsPay();
        showScreenMonthlyReport = new ShowScreenMonthlyReport();
        showScreenLogin = new ShowScreenLogin();
        showScreenPatientProfile = new ShowScreenPatientProfile();
        showScreenPatientVisit = new ShowScreenPatientVisit();
        showScreenPatientTreatment = new ShowScreenPatientTreatment();
        showScreenEvaluateService = new ShowScreenEvaluateService();
        showScreenStaffMessage = new ShowScreenStaffMessage();
        showScreenPatientQueue = new ShowScreenPatientQueue();
        showScreenStaffUser = new ShowScreenStaffUser();
        showScreenSwitchBoard = new ShowScreenSwitchBoard();
        currentScreen = null;
    }

    private class ShowScreenAccountsPay implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenAccountsPay scr = (ScreenAccountsPay) getScreen("ScreenAccountsPay");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenMonthlyReport implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenMonthlyReport scr = (ScreenMonthlyReport) getScreen("ScreenGenMonthlyReport");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenLogin implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            screenSwitchBoard.setVisible(false);
            ScreenLogin scr = (ScreenLogin) getScreen("ScreenLoginUI");
            scr.setLocation(screenSwitchBoard.getX(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = null;
            setUser(null);
        }
    }

    private class ShowScreenPatientProfile implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenPatientProfile scr = (ScreenPatientProfile) getScreen("ScreenPatientProfile");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenPatientVisit implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenPatientVisit scr = (ScreenPatientVisit) getScreen("ScreenPatientVisit");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenPatientTreatment implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenPatientTreatment scr = (ScreenPatientTreatment) getScreen("ScreenStaffContact");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenEvaluateService implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenEvaluateService scr = (ScreenEvaluateService) getScreen("ScreenStaffEvaluate");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenStaffMessage implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenStaffMessage scr = (ScreenStaffMessage) getScreen("ScreenStaffMessage");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenPatientQueue implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenPatientQueue scr = (ScreenPatientQueue) getScreen("ScreenStaffPatientQueue");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenStaffUser implements Runnable {

        public void run() {
            if (null != currentScreen) {
                currentScreen.setVisible(false);
            }
            ScreenStaffUser scr = (ScreenStaffUser) getScreen("ScreenStaffUser");
            scr.setLocation(screenSwitchBoard.getX() + screenSwitchBoard.getWidth(), screenSwitchBoard.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            currentScreen = scr;
        }
    }

    private class ShowScreenSwitchBoard implements Runnable {

        public void run() {
            currentScreen = null;
            ScreenSwitchBoard scr = (ScreenSwitchBoard) getScreen("ScreenSwitchBoard");
            scr.setLocation(screenLogin.getX(), screenLogin.getY());
            scr.setVisible(true);
            scr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    public Object getScreen(String screenName) {
        return screen.get(screenName);
    }

    public void setSwitchBoardLoc(int x, int y, int w, int h) {
        switchBoardX = x;
        switchBoardY = y;
        switchBoardW = w;
        switchBoardH = h;
    }

    public int getSwitchBoardX() {
        return switchBoardX;
    }

    public int getSwitchBoardY() {
        return switchBoardY;
    }

    public int getSwitchBoardW() {
        return switchBoardW;
    }

    public int getSwitchBoardH() {
        return switchBoardH;
    }

    public boolean toboolean(String yn) {
        return (null == yn) ? false : yn.equals("Y");
    }

    public boolean sqlExe(String sql) {
        boolean out = false;
        try {
            stmt.execute(sql);
            out = true;
        } catch (SQLException e) {
            int a = 1;
        }
        return out;
    }

    public ResultSet sqlQuery(String sql) {
        ResultSet out = null;
        try {
            out = stmt.executeQuery(sql);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(currentScreen, "SQLException", "Error", JOptionPane.ERROR_MESSAGE);
            int a = 1;
        }
        return out;
    }

    public void stringFilter(String str) {
    }

    public void stringFilter(String str, String regexpr) {
    }

    public void print(String str) {
    }

    public void saveToFile(String filename, String content) {
    }

    public void email(ArrayList<String> addr, String subject, String content) {
    }

    public String getNowDate() {
        return null;
    }

    public String getNowDateTime() {
        return null;
    }

    public void setUser(BaseUser user) {
        this.user = user;
    }

    public BaseUser getUser() {
        return user;
    }

    public String setEncryptedPassword(String rawPassword) {
        String out = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(rawPassword.getBytes());
            byte raw[] = md.digest();
            out = new String();
            for (int x = 0; x < raw.length; x++) {
                String hex2 = Integer.toHexString((int) raw[x] & 0xFF);
                if (1 == hex2.length()) {
                    hex2 = "0" + hex2;
                }
                out += hex2;
                int a = 1;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return out;
    }
}
