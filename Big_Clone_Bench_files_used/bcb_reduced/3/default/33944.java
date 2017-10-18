import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Vector;
import java.security.MessageDigest;

public class hashcash {

    public static void main(String[] args) throws Throwable {
        BufferedReader in = new BufferedReader(new FileReader("hashcash.conf"));
        String s;
        String host = "127.0.0.1";
        int port = 689;
        int bits = 22;
        Vector from = new Vector();
        while (in.ready()) {
            s = in.readLine().trim();
            if (s.length() > 0) switch(Character.toLowerCase(s.charAt(0))) {
                case 'i':
                    host = s.split("=")[1];
                    break;
                case 'p':
                    port = new Integer(s.split("=")[1]).intValue();
                    break;
                case 'b':
                    bits = new Integer(s.split("=")[1]).intValue();
                    break;
                case 'f':
                    from.add(s.split("=")[1].toLowerCase());
                    break;
                case '#':
                    break;
                default:
                    throw new Exception("Error in config file");
            }
        }
        ServerSocket ss = register(host, port, "HashCash");
        Stack freeThreads = HThread.freeThreads = new Stack();
        HThread.host = host;
        HThread.bits = bits;
        HThread.from = from;
        while (!ss.isClosed()) {
            Socket sock = ss.accept();
            try {
                HThread thread = (HThread) freeThreads.pop();
                thread.sock = sock;
                thread.start();
            } catch (EmptyStackException e) {
                new HThread(sock);
            }
        }
    }

    private static final ServerSocket register(String host, int port, String agent) {
        Socket nmap = null;
        ServerSocket ss = null;
        try {
            nmap = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(nmap.getInputStream()));
            String s = in.readLine();
            ss = new ServerSocket(0, 0, nmap.getLocalAddress());
            if (s.startsWith("4242 ")) throw new Exception("Please add " + nmap.getLocalAddress().getHostAddress() + " to NMAP trusted hosts via NWAdmin.");
            if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 1: " + s);
            PrintStream out = new PrintStream(nmap.getOutputStream());
            out.println("QWAIT 4 " + ss.getLocalPort() + " " + agent);
            s = in.readLine();
            if (!s.startsWith("1000 ")) throw new Exception("Failed to register with queue: " + s);
        } catch (Throwable t) {
            System.out.println(t.getMessage());
            System.exit(1);
        }
        try {
            nmap.close();
        } catch (Throwable t) {
        }
        return ss;
    }

    private static final class HThread extends Thread {

        private static Stack freeThreads;

        private static String host;

        private static int bits;

        private static Vector from;

        private final Random r = new Random();

        private final Calendar cal = Calendar.getInstance();

        private final MessageDigest md;

        public Socket sock;

        HThread(Socket s) throws Throwable {
            super();
            md = MessageDigest.getInstance("SHA");
            sock = s;
            super.start();
        }

        public final synchronized void start() {
            this.notify();
        }

        private final synchronized void waitForJob() {
            while (sock == null) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                }
            }
        }

        public final void run() {
            while (true) {
                try {
                    if (!sock.getLocalAddress().getHostAddress().equals(host)) throw new Exception("unauthorized connection from " + sock.getLocalAddress().getHostAddress());
                    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String s = in.readLine();
                    if (!s.startsWith("6020 ")) throw new Exception("NMAP protocol error 2: " + s);
                    String id = s.split(" ")[1];
                    int size = new Integer(s.split(" ")[3]).intValue();
                    Vector env = new Vector();
                    for (int x = new Integer(s.split(" ")[2]).intValue() - (in.readLine().length() + 2); x > 0; x -= (s.length() + 2)) {
                        env.add(s = in.readLine());
                    }
                    s = in.readLine();
                    if (!s.startsWith("6021 ")) throw new Exception("NMAP protocol error 3: " + s);
                    boolean process = false;
                    Stack to = new Stack();
                    for (Enumeration e = env.elements(); e.hasMoreElements(); ) {
                        s = (String) e.nextElement();
                        switch(s.charAt(0)) {
                            case 'F':
                                for (Enumeration n = from.elements(); n.hasMoreElements(); ) {
                                    if (s.split(" ")[0].toLowerCase().endsWith((String) n.nextElement())) process = true;
                                }
                                break;
                            case 'R':
                                to.push(s.split(" ")[1]);
                        }
                    }
                    if (to.empty()) process = false;
                    PrintStream out = new PrintStream(sock.getOutputStream());
                    int header = 0;
                    if (process) {
                        out.println("QHEAD " + id);
                        s = in.readLine();
                        if (!s.startsWith("2023 ")) throw new Exception("NMAP protocol error 4: " + s);
                        header = new Integer(s.split(" ")[1]).intValue();
                        for (int x = header; x > 0; x -= (s.length() + 2)) {
                            s = in.readLine();
                            if (s.startsWith("X-Hashcash: ")) process = false;
                        }
                        s = in.readLine();
                        if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 5: " + s);
                    }
                    if (process) {
                        out.println("QCREA");
                        s = in.readLine();
                        if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 6: " + s);
                        out.println("QADDQ " + id + " 0 " + header);
                        s = in.readLine();
                        if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 7: " + s);
                        do {
                            s = "0:" + timeToYYMMDD(cal) + ":" + (String) to.pop() + ":";
                            byte[] c = new byte[s.length() + Math.max(11, 6 + bits * 100 / 595)];
                            System.arraycopy(s.getBytes(), 0, c, 0, s.length());
                            for (int x = s.length(); x < c.length; x++) c[x] = getRandomChar(r);
                            byte[] h = new byte[20];
                            h = md.digest(c);
                            while (!zeroBits(h, bits)) {
                                for (int x = c.length; x-- > s.length() && (c[x] = incChar(c[x])) == 48; ) ;
                                h = md.digest(c);
                            }
                            out.println("QSTOR MESSAGE " + (c.length + 14));
                            out.println("X-Hashcash: " + new String(c));
                            s = in.readLine();
                            if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 8: " + s);
                        } while (!to.empty());
                        out.println("QADDQ " + id + " " + header + " " + (size - header));
                        s = in.readLine();
                        if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 9: " + s);
                        for (Enumeration e = env.elements(); e.hasMoreElements(); ) {
                            out.println("QSTOR RAW " + e.nextElement());
                            s = in.readLine();
                            if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 10: " + s);
                        }
                        out.println("QRUN");
                        s = in.readLine();
                        if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 11: " + s);
                        out.println("QDELE " + id);
                        s = in.readLine();
                        if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 12: " + s);
                    }
                    out.println("QDONE");
                    s = in.readLine();
                    if (!s.startsWith("1000 ")) throw new Exception("NMAP protocol error 13: " + s);
                } catch (Throwable t) {
                    System.out.println(t.getMessage());
                } finally {
                    try {
                        sock.close();
                    } catch (Throwable t) {
                    }
                }
                sock = null;
                freeThreads.push(this);
                waitForJob();
            }
        }

        /**
	 * @return one random character out of 62 ('A'..'Z','a'..'z','0'..'9')
	 */
        private static final byte getRandomChar(Random rnd) {
            int x = rnd.nextInt(62);
            if (x < 10) return (byte) (48 + x);
            x -= 10;
            if (x < 26) return (byte) (65 + x);
            x -= 26;
            return (byte) (97 + x);
        }

        /**
	 * @param c
	 * @return c+1 (within '0'..'9','A'..'Z','a'..'z')
	 */
        private static final byte incChar(byte c) {
            c++;
            if (c == 48 + 10) return (byte) 65;
            if (c == 65 + 26) return (byte) 97;
            if (c == 97 + 26) return (byte) 48;
            return c;
        }

        /**
	 * @param ba
	 * @param minimum
	 * @return checks whether a bitstring (MSB first) starts at least with [minimum] zeros
	 */
        private static final boolean zeroBits(byte[] ba, int minimum) {
            int o = 0;
            while (minimum >= 8) {
                if (ba[o++] != (byte) 0) return false;
                minimum -= 8;
            }
            if (minimum > 0) {
                int m = (0xFF << (8 - minimum)) & 0xFF;
                if ((ba[o] & m) != 0) return false;
            }
            return true;
        }

        /**
	 * @param i
	 * @return returns a two-digit string (leading zero for i<10)
	 */
        private static final String twoDigits(int i) {
            return i < 10 ? "0" + i : Integer.toString(i);
        }

        /**
	 * @param c
	 * @return returns a 6 character string (YYMMDD) for a given Calendar
	 */
        private static final String timeToYYMMDD(Calendar c) {
            return twoDigits(c.get(Calendar.YEAR) % 100) + twoDigits((c.get(Calendar.MONTH) + 1) % 100) + twoDigits(c.get(Calendar.DAY_OF_MONTH) % 100);
        }
    }
}
