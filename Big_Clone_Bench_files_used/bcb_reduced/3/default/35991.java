import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.security.*;
import java.math.*;

public class YahooPhotoFetch {

    Vector<YahooInfo> UrlList;

    HashSet<String> lesmd5;

    public static void main(String args[]) throws Exception {
        YahooPhotoFetch ypf = new YahooPhotoFetch();
        String login;
        String password;
        if (args.length != 2) {
            System.out.println("Usage : java YahooPhotoFetch <F|P> <groupname>");
            return;
        }
        File lefile = new File("ypf.cfg");
        if (!lefile.exists()) return;
        BufferedReader in = new BufferedReader(new FileReader(lefile));
        login = in.readLine();
        password = in.readLine();
        in.close();
        if (!ypf.connectyahoo(login, password)) {
            System.out.println("Connection error");
            return;
        }
        if (!ypf.loadUrlList("group.lst")) {
            ypf.listgroups();
            ypf.saveUrlList("group.lst");
        }
        System.out.println("Liste des groupes chargee");
        ypf.loadmd5("all.md5");
        ypf.loadmd5("new.md5");
        System.out.println("MD5 charges : " + ypf.lesmd5.size());
        if (!args[1].equals("")) {
            if (args[0].equals("P")) ypf.refreshPhotoItem(new YahooInfo(args[1], "", "", ""));
            if (args[0].equals("F")) ypf.refreshFileItem(new YahooInfo(args[1], "", "", ""));
            ypf.downloadItem(new YahooInfo(args[1], "", "", ""));
        }
        System.out.println("MD5 total : " + ypf.lesmd5.size());
    }

    public YahooPhotoFetch() {
        UrlList = new Vector<YahooInfo>();
        lesmd5 = new HashSet<String>();
        CookieHandler.setDefault(new ListCookieHandler());
    }

    public void print() {
        for (int i = 0; i < UrlList.size(); i++) System.out.println(UrlList.get(i).toString());
    }

    public void clearAll() {
        UrlList.clear();
    }

    public void clearItem(YahooInfo legroup) {
        for (int i = 0; i < UrlList.size(); i++) {
            if (UrlList.get(i).equals(legroup)) {
                UrlList.get(i).setStatus(0);
            } else {
                if (UrlList.get(i).getGroup().equals(legroup.getGroup()) && UrlList.get(i).getDir().startsWith(legroup.getDir())) {
                    UrlList.remove(i);
                    i--;
                }
            }
        }
    }

    public void saveUrlList(String fname) throws Exception {
        File lefic = new File(fname);
        FileWriter fw = new FileWriter(lefic);
        for (int i = 0; i < UrlList.size(); i++) {
            fw.write(UrlList.get(i).toString() + "\n", 0, UrlList.get(i).toString().length() + 1);
        }
        fw.close();
    }

    public boolean loadUrlList(String fname) throws Exception {
        Pattern pat;
        Matcher mat;
        YahooInfo yi;
        File lefile = new File(fname);
        if (!lefile.exists()) return false;
        BufferedReader in = new BufferedReader(new FileReader(lefile));
        String str = in.readLine();
        while (str != null) {
            pat = Pattern.compile("(.+?);(.*?);(.*?);(.+?);(.+)");
            mat = pat.matcher(str);
            if (mat.find()) {
                yi = new YahooInfo(mat.group(1), mat.group(2), mat.group(3), mat.group(4));
                yi.setStatus(Integer.parseInt(mat.group(5)));
                UrlList.add(yi);
            }
            str = in.readLine();
        }
        in.close();
        return true;
    }

    public void loadmd5(String fname) throws Exception {
        Pattern pat;
        Matcher mat;
        File lefile = new File(fname);
        if (!lefile.exists()) return;
        BufferedReader in = new BufferedReader(new FileReader(lefile));
        String str = in.readLine();
        int n = 0;
        while (str != null) {
            n++;
            pat = Pattern.compile("(.+?) .*");
            mat = pat.matcher(str);
            mat.find();
            lesmd5.add(mat.group(1));
            str = in.readLine();
        }
        in.close();
    }

    public void downloadItem(YahooInfo legroup) throws Exception {
        String unmd5;
        String str;
        int data;
        URL myurl;
        URLConnection conn;
        InputStream in;
        FileOutputStream out;
        BigInteger hash;
        File lefic;
        File tmpfic = new File("tmpyahoo.jpg");
        File ledir;
        File newmd5 = new File("new.md5");
        MessageDigest md = MessageDigest.getInstance("MD5");
        System.out.print("Downloading images : ");
        for (int i = 0; i < UrlList.size(); i++) {
            if (UrlList.get(i).getGroup().equals(legroup.getGroup()) && UrlList.get(i).getDir().startsWith(legroup.getDir()) && UrlList.get(i).isFile()) {
                myurl = new URL(UrlList.get(i).getUrl());
                conn = myurl.openConnection();
                conn.connect();
                if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
                    System.out.println(conn.getHeaderField(0).toString());
                    return;
                }
                in = conn.getInputStream();
                out = new FileOutputStream(tmpfic);
                md.reset();
                for (data = in.read(); data != -1; data = in.read()) {
                    md.update((byte) data);
                    out.write(data);
                }
                out.close();
                hash = new BigInteger(1, md.digest());
                unmd5 = new String(hash.toString(16));
                for (int j = unmd5.length(); j < 32; j++) unmd5 = "0" + unmd5;
                if (!lesmd5.contains(unmd5)) {
                    str = UrlList.get(i).getGroup() + "/" + unmd5 + UrlList.get(i).getFile();
                    lefic = new File(str);
                    ledir = new File(UrlList.get(i).getGroup());
                    if (!ledir.exists()) ledir.mkdirs();
                    tmpfic.renameTo(lefic);
                    lesmd5.add(unmd5);
                    FileWriter fw = new FileWriter(newmd5, true);
                    fw.write(unmd5 + " " + str + "\n", 0, unmd5.length() + str.length() + 2);
                    fw.close();
                    System.out.print(".");
                } else {
                    System.out.print("D");
                    tmpfic.delete();
                }
            }
        }
        System.out.println("");
    }

    public void refreshPhotoItem(YahooInfo legroup) throws Exception {
        String lapage = new String("");
        String lapage2 = new String("");
        String ledir = new String("");
        Pattern pat;
        Matcher mat;
        Pattern pat2;
        Matcher mat2;
        URL myurl = new URL("http://groups.yahoo.com/mygroups");
        URI myuri = new URI("http://groups.yahoo.com/mygroups");
        YahooInfo yi;
        clearItem(legroup);
        for (int i = 0; i < UrlList.size(); i++) {
            if (UrlList.get(i).getGroup().equals(legroup.getGroup()) && UrlList.get(i).getDir().startsWith(legroup.getDir())) {
                if (UrlList.get(i).isGroup()) {
                    System.out.print(UrlList.get(i).getGroup() + " : ");
                    myuri = new URI(UrlList.get(i).getUrl());
                    lapage = getpage(UrlList.get(i).getUrl());
                    pat = Pattern.compile("<li> <a href=\"(.+?)\".*?>[PF]h?otos</a></li>");
                    mat = pat.matcher(lapage);
                    if (mat.find()) {
                        yi = new YahooInfo(UrlList.get(i).getGroup(), "/", "", myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                        UrlList.add(yi);
                    }
                }
                if (UrlList.get(i).isDir()) {
                    System.out.println(UrlList.get(i).getGroup() + UrlList.get(i).getDir());
                    myuri = new URI(UrlList.get(i).getUrl());
                    myurl = new URL(UrlList.get(i).getUrl());
                    do {
                        lapage = getpage(myurl.toString());
                        pat = Pattern.compile("<td class=\"bold\"><a href=\"(.+?browse.+?)\".*?>(.+?)</a>");
                        mat = pat.matcher(lapage);
                        while (mat.find()) {
                            ledir = new String(UrlList.get(i).getDir());
                            pat2 = Pattern.compile("([A-Za-z0-9]+)");
                            mat2 = pat2.matcher(mat.group(2));
                            while (mat2.find()) {
                                ledir += mat2.group(1);
                            }
                            ledir += "/";
                            yi = new YahooInfo(UrlList.get(i).getGroup(), ledir, "", myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                            UrlList.add(yi);
                        }
                        pat = Pattern.compile("<td class=\"bold\"><a href=\"(.+?view.+?)\".*?>(.+?)</a>");
                        mat = pat.matcher(lapage);
                        while (mat.find()) {
                            lapage2 = getpage(myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString() + "&m=f");
                            pat2 = Pattern.compile("<center><img src=\"(.+?__hr_.+?)\".*?></center>");
                            mat2 = pat2.matcher(lapage2);
                            if (mat2.find()) {
                                System.out.print("h");
                                ledir = mat2.group(1);
                                pat2 = Pattern.compile("r_/(.+?)\\?");
                                mat2 = pat2.matcher(ledir);
                                if (mat2.find()) {
                                    yi = new YahooInfo(UrlList.get(i).getGroup(), UrlList.get(i).getDir(), mat2.group(1), myuri.resolve(HTMLDecoder.decode(ledir)).toURL().toString());
                                    UrlList.add(yi);
                                }
                            } else {
                                lapage2 = getpage(myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                                pat2 = Pattern.compile("<center><img src=\"(.+?__sr_.+?)\".*?></center>");
                                mat2 = pat2.matcher(lapage2);
                                if (mat2.find()) {
                                    System.out.print("s");
                                    ledir = mat2.group(1);
                                    pat2 = Pattern.compile("r_/(.+?)\\?");
                                    mat2 = pat2.matcher(ledir);
                                    if (mat2.find()) {
                                        yi = new YahooInfo(UrlList.get(i).getGroup(), UrlList.get(i).getDir(), mat2.group(1), myuri.resolve(HTMLDecoder.decode(ledir)).toURL().toString());
                                        UrlList.add(yi);
                                    }
                                }
                            }
                        }
                        System.out.println("");
                        pat = Pattern.compile("<a href=\"(.+?)\">Next");
                        mat = pat.matcher(lapage);
                        myurl = null;
                        if (mat.find()) {
                            myurl = myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL();
                        }
                    } while (myurl != null);
                }
            }
        }
    }

    public void refreshFileItem(YahooInfo legroup) throws Exception {
        String lapage = new String("");
        String ledir = new String("");
        Pattern pat;
        Matcher mat;
        Pattern pat2;
        Matcher mat2;
        int data;
        URL myurl = new URL("http://groups.yahoo.com/mygroups");
        URLConnection conn;
        URI myuri = new URI("http://groups.yahoo.com/mygroups");
        YahooInfo yi;
        clearItem(legroup);
        for (int i = 0; i < UrlList.size(); i++) {
            if (UrlList.get(i).getGroup().equals(legroup.getGroup()) && UrlList.get(i).getDir().startsWith(legroup.getDir())) {
                if (UrlList.get(i).isGroup()) {
                    System.out.print(UrlList.get(i).getGroup() + " : ");
                    myuri = new URI(UrlList.get(i).getUrl());
                    myurl = new URL(UrlList.get(i).getUrl());
                    conn = myurl.openConnection();
                    conn.connect();
                    System.out.println(conn.getHeaderField(0).toString());
                    if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
                        System.out.println(conn.getHeaderField(0).toString());
                        return;
                    }
                    InputStream in = conn.getInputStream();
                    lapage = "";
                    for (data = in.read(); data != -1; data = in.read()) lapage += (char) data;
                    pat = Pattern.compile("<li> <a href=\"(.+?)\".*?>Files</a></li>");
                    mat = pat.matcher(lapage);
                    if (mat.find()) {
                        yi = new YahooInfo(UrlList.get(i).getGroup(), "/", "", myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                        UrlList.add(yi);
                    }
                }
                if (UrlList.get(i).isDir()) {
                    System.out.println(UrlList.get(i).getGroup() + UrlList.get(i).getDir());
                    myuri = new URI(UrlList.get(i).getUrl());
                    myurl = new URL(UrlList.get(i).getUrl());
                    do {
                        myurl = new URL(myurl.toString());
                        conn = myurl.openConnection();
                        conn.connect();
                        if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
                            System.out.println(conn.getHeaderField(0).toString());
                            return;
                        }
                        System.out.print("p");
                        InputStream in = conn.getInputStream();
                        lapage = "";
                        for (data = in.read(); data != -1; data = in.read()) lapage += (char) data;
                        pat = Pattern.compile("<span class=\"title\">\n<a href=\"(.+?/)\">(.+?)</a>");
                        mat = pat.matcher(lapage);
                        while (mat.find()) {
                            ledir = new String(UrlList.get(i).getDir());
                            pat2 = Pattern.compile("([A-Za-z0-9]+)");
                            mat2 = pat2.matcher(mat.group(2));
                            while (mat2.find()) {
                                ledir += mat2.group(1);
                            }
                            ledir += "/";
                            yi = new YahooInfo(UrlList.get(i).getGroup(), ledir, "", myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                            UrlList.add(yi);
                        }
                        pat = Pattern.compile("<span class=\"title\">\n<a href=\"(.+?yahoofs.+?)\".*?>(.+?)</a>");
                        mat = pat.matcher(lapage);
                        while (mat.find()) {
                            yi = new YahooInfo(UrlList.get(i).getGroup(), UrlList.get(i).getDir(), mat.group(2), myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                            UrlList.add(yi);
                        }
                        System.out.println("");
                        pat = Pattern.compile("<a href=\"(.+?)\">Next");
                        mat = pat.matcher(lapage);
                        myurl = null;
                        if (mat.find()) {
                            myurl = myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL();
                        }
                    } while (myurl != null);
                }
            }
        }
    }

    public void refreshPhotoAll() throws Exception {
        for (int i = 0; i < UrlList.size(); i++) {
            if (UrlList.get(i).isGroup()) refreshPhotoItem(new YahooInfo(UrlList.get(i).getGroup(), "", "", ""));
        }
    }

    public void refreshFileAll() throws Exception {
        for (int i = 0; i < UrlList.size(); i++) {
            if (UrlList.get(i).isGroup()) refreshFileItem(new YahooInfo(UrlList.get(i).getGroup(), "", "", ""));
        }
    }

    public void listgroups() throws Exception {
        String lapage = new String("");
        Pattern pat;
        Matcher mat;
        int data;
        URL myurl = new URL("http://groups.yahoo.com/mygroups");
        URLConnection conn;
        URI myuri = new URI("http://groups.yahoo.com/mygroups");
        YahooInfo yi;
        clearAll();
        System.out.print("http://groups.yahoo.com/mygroups : ");
        do {
            myurl = new URL(myurl.toString());
            conn = myurl.openConnection();
            conn.connect();
            if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
                System.out.println(conn.getHeaderField(0).toString());
                return;
            }
            System.out.print(".");
            InputStream in = conn.getInputStream();
            lapage = "";
            for (data = in.read(); data != -1; data = in.read()) lapage += (char) data;
            pat = Pattern.compile("<td class=\"grpname selected\"><a href=\"(.+?)\".*?><em>(.+?)</em></a>");
            mat = pat.matcher(lapage);
            while (mat.find()) {
                yi = new YahooInfo(mat.group(2), "", "", myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL().toString());
                UrlList.add(yi);
            }
            pat = Pattern.compile("<a href=\"(.+?)\">Next &gt;</a>");
            mat = pat.matcher(lapage);
            myurl = null;
            if (mat.find()) {
                myurl = myuri.resolve(HTMLDecoder.decode(mat.group(1))).toURL();
            }
        } while (myurl != null);
        System.out.println("");
    }

    public boolean connectyahoo(String login, String password) throws Exception {
        String lapage = new String("");
        String myargs = new String("");
        Pattern pat;
        Matcher mat;
        int data;
        URL myurl = new URL("http://groups.yahoo.com/mygroups");
        URLConnection conn = myurl.openConnection();
        conn.connect();
        if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
            System.out.println(conn.getHeaderField(0).toString());
            return false;
        }
        System.out.print("login : ");
        InputStream in = conn.getInputStream();
        lapage = "";
        for (data = in.read(); data != -1; data = in.read()) lapage += (char) data;
        myargs = "";
        myargs += URLEncoder.encode(".tries", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8") + "&";
        myargs += URLEncoder.encode(".src", "UTF-8") + "=" + URLEncoder.encode("ygrp", "UTF-8") + "&";
        myargs += URLEncoder.encode(".md5", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".hash", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".js", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".last", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode("promo", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".intl", "UTF-8") + "=" + URLEncoder.encode("us", "UTF-8") + "&";
        myargs += URLEncoder.encode(".bypass", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".partner", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        pat = Pattern.compile("<input type=\"hidden\" name=\".u\" value=\"(.+?)\">");
        mat = pat.matcher(lapage);
        mat.find();
        myargs += URLEncoder.encode(".u", "UTF-8") + "=" + URLEncoder.encode(mat.group(1), "UTF-8") + "&";
        myargs += URLEncoder.encode(".v", "UTF-8") + "=" + URLEncoder.encode("0", "UTF-8") + "&";
        pat = Pattern.compile("<input type=\"hidden\" name=\".challenge\" value=\"(.+?)\">");
        mat = pat.matcher(lapage);
        mat.find();
        myargs += URLEncoder.encode(".challenge", "UTF-8") + "=" + URLEncoder.encode(mat.group(1), "UTF-8") + "&";
        myargs += URLEncoder.encode(".yplus", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".emailCode", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode("pkg", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode("stepid", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode(".ev", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
        myargs += URLEncoder.encode("hasMsgr", "UTF-8") + "=" + URLEncoder.encode("0", "UTF-8") + "&";
        myargs += URLEncoder.encode(".chkP", "UTF-8") + "=" + URLEncoder.encode("Y", "UTF-8") + "&";
        myargs += URLEncoder.encode(".done", "UTF-8") + "=" + URLEncoder.encode("http://groups.yahoo.com/mygroups", "UTF-8") + "&";
        myargs += URLEncoder.encode("login", "UTF-8") + "=" + URLEncoder.encode(login, "UTF-8") + "&";
        myargs += URLEncoder.encode("passwd", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8") + "&";
        myargs += URLEncoder.encode(".persistent", "UTF-8") + "=" + URLEncoder.encode("y", "UTF-8") + "&";
        myargs += URLEncoder.encode(".save", "UTF-8") + "=" + URLEncoder.encode("Sign In", "UTF-8");
        myurl = new URL("http://login.yahoo.com/config/login");
        conn = myurl.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        OutputStream output = conn.getOutputStream();
        PrintStream pout = new PrintStream(output);
        pout.print(myargs);
        pout.close();
        if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
            System.out.println(conn.getHeaderField(0).toString());
            return false;
        }
        System.out.println("OK");
        myurl = new URL("http://groups.yahoo.com/adultconf");
        conn = myurl.openConnection();
        conn.connect();
        if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
            System.out.println(conn.getHeaderField(0).toString());
            return false;
        }
        System.out.print("adult : ");
        in = conn.getInputStream();
        lapage = "";
        for (data = in.read(); data != -1; data = in.read()) lapage += (char) data;
        myargs = "";
        pat = Pattern.compile("<input type=\"hidden\" name=\"ycb\" value=\"(.+?)\">");
        mat = pat.matcher(lapage);
        mat.find();
        myargs += URLEncoder.encode("ycb", "UTF-8") + "=" + URLEncoder.encode(mat.group(1), "UTF-8") + "&";
        myargs += URLEncoder.encode("dest", "UTF-8") + "=" + URLEncoder.encode("/mygroups", "UTF-8") + "&";
        myargs += URLEncoder.encode("accept", "UTF-8") + "=" + URLEncoder.encode("I Acce", "UTF-8");
        myurl = new URL("http://groups.yahoo.com/adultconf");
        conn = myurl.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        output = conn.getOutputStream();
        pout = new PrintStream(output);
        pout.print(myargs);
        pout.close();
        if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
            System.out.println(conn.getHeaderField(0).toString());
            return false;
        }
        System.out.println("OK");
        return true;
    }

    public String getpage(String leurl) throws Exception {
        int data;
        StringBuffer lapage = new StringBuffer();
        URL myurl = new URL(leurl);
        URLConnection conn = myurl.openConnection();
        conn.connect();
        if (!Pattern.matches("HTTP/... 2.. .*", conn.getHeaderField(0).toString())) {
            System.out.println(conn.getHeaderField(0).toString());
            return lapage.toString();
        }
        InputStream in = conn.getInputStream();
        for (data = in.read(); data != -1; data = in.read()) lapage.append((char) data);
        return lapage.toString();
    }
}
