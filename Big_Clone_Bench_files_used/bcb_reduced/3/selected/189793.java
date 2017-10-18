package org.opendht;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;
import org.p2s.lib.Base64;
import org.p2s.lib.XmlLoader;
import org.p2s.lib.XmlNode;

public class OpenDht {

    public static String host = "opendht.nyuld.net";

    public static int port = 5851;

    public static void selectHost() throws Exception {
        ArrayList servers = new ArrayList();
        BufferedReader in = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("org/opendht/servers.txt")));
        String l = in.readLine();
        while (l != null) {
            String fields[] = l.split("\t");
            if (fields.length == 3) {
                servers.add(fields[1]);
            }
            l = in.readLine();
        }
        in.close();
        Random random = new Random();
        int rnd = random.nextInt(servers.size());
        String server = (String) servers.get(rnd);
        String a[] = server.split(":");
        host = a[0];
        port = Integer.parseInt(a[1]);
    }

    public static void selectAndTestHost() throws Exception {
        if (host == null) {
            boolean found = false;
            int maxtry = 5;
            int tries = 0;
            while (!found && tries < maxtry) {
                tries++;
                try {
                    selectHost();
                    System.out.println("trying opendht server " + host);
                    Random r = new Random();
                    String key = "" + r.nextInt();
                    String value = "" + r.nextInt();
                    put(key.getBytes(), value.getBytes());
                    ArrayList ls = get(key.getBytes());
                    if (ls.size() > 0) {
                        String test = new String((byte[]) ls.get(0));
                        found = value.equals(test);
                        if (found) System.out.println("opendht server found");
                    }
                } catch (Exception e) {
                    System.out.println("opendht test failed " + host);
                }
            }
            if (tries == maxtry) throw new Exception("opendht maxtries reached");
        }
    }

    public static void put(byte key[], byte value[]) throws Exception {
        selectAndTestHost();
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(key);
        byte[] keysha = md.digest();
        Socket s = new Socket();
        s.setSoTimeout(10000);
        s.connect(new InetSocketAddress(host, port));
        PrintStream out = new PrintStream(s.getOutputStream());
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0'?>");
        buf.append("<methodCall>");
        buf.append("<methodName>put</methodName>");
        buf.append("<params>");
        buf.append("<param><value><base64>");
        buf.append(Base64.encodeBytes(keysha));
        buf.append("</base64></value></param>");
        buf.append("<param><value><base64>");
        buf.append(Base64.encodeBytes(value));
        buf.append("</base64></value></param>");
        buf.append("<param><value><int>3600</int></value></param>");
        buf.append("<param><value><string>p2s</string></value></param>");
        buf.append("</params>");
        buf.append("</methodCall>");
        out.println("POST / HTTP/1.0");
        out.println("Host: " + host + ":" + port);
        out.println("User-Agent: p2s");
        out.println("Content-Type: text/xml");
        out.println("Content-Length: " + buf.length());
        out.println("");
        out.print(buf);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String l = in.readLine();
        while (l != null) {
            l = in.readLine();
        }
    }

    public static ArrayList get(byte key[]) throws Exception {
        selectAndTestHost();
        ArrayList ret = new ArrayList();
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(key);
        byte[] keysha = md.digest();
        Socket s = new Socket();
        s.setSoTimeout(10000);
        s.connect(new InetSocketAddress(host, port));
        PrintStream out = new PrintStream(s.getOutputStream());
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0'?>");
        buf.append("<methodCall>");
        buf.append("<methodName>get</methodName>");
        buf.append("<params>");
        buf.append("<param><value><base64>");
        buf.append(Base64.encodeBytes(keysha));
        buf.append("</base64></value></param>");
        buf.append("<param><value><int>10</int></value></param>");
        buf.append("<param><value><base64></base64></value></param>");
        buf.append("<param><value><string>p2s</string></value></param>");
        buf.append("</params>");
        buf.append("</methodCall>");
        out.println("POST / HTTP/1.0");
        out.println("Host: " + host + ":" + port);
        out.println("User-Agent: p2s");
        out.println("Content-Type: text/xml");
        out.println("Content-Length: " + buf.length());
        out.println("");
        out.print(buf);
        out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String l = in.readLine();
        while (l != null && l.length() > 0) {
            l = in.readLine();
        }
        StringBuffer xml = new StringBuffer();
        while (l != null) {
            xml.append(l);
            l = in.readLine();
        }
        ByteArrayInputStream str = new ByteArrayInputStream(xml.toString().getBytes());
        XmlNode node = XmlLoader.load(str);
        XmlNode nodes[] = node.getFirstChild("params").getFirstChild("param").getFirstChild("value").getFirstChild("array").getFirstChild("data").getChild("value");
        XmlNode values[] = nodes[0].getFirstChild("array").getFirstChild("data").getChild("value");
        String placemark = nodes[1].getFirstChild("base64").getText();
        for (int i = 0; i < values.length; i++) {
            String data = values[i].getFirstChild("base64").getText();
            byte d[] = Base64.decode(data);
            ret.add(d);
        }
        return ret;
    }
}
