package org.swana.daemon.simulator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.swana.common.ConnectionFactory;

/**
 * Simulator to test tracking daemon functionality.
 * @author Wang Yuxing
 * 
 */
public class WebAccessSimulator extends ConnectionFactory {

    private final String DOMAIN_NAME_CHARS = "abcdefghijklmnopqrstuvwxyz";

    private final String[] DOMAIN_CHARS = new String[] { "com", "net", "org", "cn" };

    private int counter = 0;

    private Random random = new Random();

    private WebConversation wc = new WebConversation();

    private WebRequest req = null;

    private List<Integer> pageIdList = null;

    ThreadPoolExecutor threadPool;

    public WebAccessSimulator() {
        super();
        threadPool = new ThreadPoolExecutor(2, 30, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
        Connection conn = this.getConn();
        String sql = "select ID from swana_access_page";
        pageIdList = new ArrayList<Integer>();
        try {
            Statement st = conn.prepareStatement(sql);
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                pageIdList.add(rs.getInt(1));
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void runSimulator(String target) {
        System.out.println("使用Get方式向服务器发送数据，然后获取网页内容：" + target);
        if (pageIdList.size() == 0) {
            System.out.println("ERROR: No web page registered.");
            return;
        }
        for (int i = 0; i < 100000; i++) {
            Runnable thread = new SimuAccessThread(target);
            threadPool.execute(thread);
        }
    }

    private synchronized void increaseCounter() {
        counter++;
    }

    class SimuAccessThread implements Runnable {

        private String target;

        public SimuAccessThread(String target) {
            super();
            this.target = target;
        }

        @Override
        public void run() {
            try {
                simuVisit();
                increaseCounter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void simuVisit() throws Exception {
            StringBuilder sb = new StringBuilder(30);
            sb.append("http://www.");
            int len = random.nextInt(2) + 1;
            for (int i = 0; i < len; i++) {
                int idxName = random.nextInt(26);
                sb.append(DOMAIN_NAME_CHARS.charAt(idxName));
            }
            sb.append(".").append(DOMAIN_CHARS[random.nextInt(3)]).append("/asdf中文");
            String sessionId = getRandomGUID();
            String referrer = sb.toString();
            String pageId = getRegisteredPid();
            req = new GetMethodWebRequest(target);
            req.setParameter("pageId", pageId);
            req.setParameter("trackingSessionId", sessionId);
            referrer = "http://www.google.com/";
            req.setParameter("referrer", referrer);
            System.out.println("URL:" + req.getURL());
            WebResponse resp = wc.getResponse(req);
            System.out.println(resp.getText());
            Thread.sleep(random.nextInt(1000));
        }
    }

    /**
	 * 从注册的Page ID中随机取得ID
	 * 
	 * @return
	 */
    public String getRegisteredPid() {
        Integer pid = pageIdList.get(random.nextInt(pageIdList.size()));
        System.out.print(pid);
        if (pid == null) {
            return "0";
        }
        return String.valueOf(pid);
    }

    /**
	 * 随机取0-1000的数作为Page ID
	 * 
	 * @return
	 */
    public String getRandomPid() {
        return String.valueOf(random.nextInt(1000));
    }

    public String getRandomGUID() {
        MessageDigest md5 = null;
        String valueBeforeMD5 = "";
        String retValue = "";
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: " + e);
        }
        StringBuffer sbValueBeforeMD5 = new StringBuffer();
        try {
            InetAddress id = InetAddress.getLocalHost();
            long time = System.currentTimeMillis();
            long rand = 0;
            rand = random.nextLong();
            sbValueBeforeMD5.append(id.toString());
            sbValueBeforeMD5.append(Long.toString(time));
            sbValueBeforeMD5.append(Long.toString(rand));
            valueBeforeMD5 = sbValueBeforeMD5.toString();
            md5.update(valueBeforeMD5.getBytes());
            byte[] array = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }
            retValue = sb.toString();
        } catch (UnknownHostException e) {
            System.out.println("Error:" + e);
        }
        return retValue;
    }

    /**
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        WebAccessSimulator was = new WebAccessSimulator();
        String target = "http://localhost/tracking";
        if (args.length == 1) {
            System.out.println(args[0]);
            target = args[0];
        }
        System.out.println(was.getRandomGUID());
        was.runSimulator(target);
    }
}
