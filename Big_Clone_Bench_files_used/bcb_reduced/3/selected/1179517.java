package fairVote.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import fairVote.core.MyException;
import java.util.Iterator;
import org.apache.commons.codec.binary.Base64;

public class Basic {

    public static <T> String join(final Iterable<T> objs, final String delimiter) {
        Iterator<T> iter = objs.iterator();
        if (!iter.hasNext()) return "";
        StringBuffer buffer = new StringBuffer(String.valueOf(iter.next()));
        while (iter.hasNext()) buffer.append(delimiter).append(String.valueOf(iter.next()));
        return buffer.toString();
    }

    public static String escapeXML(String data) {
        return data.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static byte[] md5(byte[] m) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(m);
            return algorithm.digest();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static String md5AsHex(String smsg) {
        return Basic.byte2HexString(md5(smsg));
    }

    public static byte[] md5(String smsg) {
        String m = smsg.replaceAll("[\r\n]+", "");
        try {
            return Basic.md5(m.getBytes());
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static String byte2String(byte[] bi) {
        return new String(bi);
    }

    public static String byte2HexString(byte[] bi) {
        String m = "";
        for (int i = 0; i < bi.length; i++) {
            int k = bi[i];
            if (k < 0) k += 256;
            if (k < 15) m += "0";
            m += Integer.toHexString(k);
        }
        return m;
    }

    public static void printAsHex(byte[] sig) {
        for (int i = 0; i < sig.length; i++) {
            if (i % 16 == 0) System.out.print(" ");
            String a = "0" + Integer.toHexString(sig[i]);
            System.out.print(a.substring(a.length() - 2));
            if ((i % 16 == 15) || (i == sig.length - 1)) System.out.println(); else System.out.print(":");
        }
    }

    public static void printAsHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (i % 16 == 0) System.out.print(" ");
            String a = "0" + Integer.toHexString((int) s.charAt(i));
            System.out.print(a.substring(a.length() - 2));
            if ((i % 16 == 15) || (i == s.length() - 1)) System.out.println(); else System.out.print(":");
        }
    }

    public static void printAsHex(byte[] sig, String title) {
        System.out.println(title);
        printAsHex(sig);
    }

    public static void printAsHex(String s, String title) {
        System.out.println(title);
        printAsHex(s);
    }

    public static byte[] loadUrlRaw(String surl) {
        try {
            URL url = new URL(surl);
            return Basic.loadUrlRaw(url);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static byte[] loadUrlRaw(URL url) {
        try {
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            return loadInputStream(in);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static byte[] loadFileRaw(String filename) {
        try {
            FileInputStream in = new FileInputStream(filename);
            return loadInputStream(in);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static byte[] loadInputStream(InputStream in) {
        try {
            byte[] buffer = new byte[1000];
            byte[] response = null;
            byte[] oldresponse = null;
            int numRead;
            int numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                if (numWritten == 0) {
                    response = new byte[numRead];
                } else {
                    oldresponse = response;
                    response = new byte[numWritten + numRead];
                    for (int j = 0; j < numWritten; j++) {
                        response[j] = oldresponse[j];
                    }
                }
                for (int j = 0; j < numRead; j++) response[numWritten + j] = buffer[j];
                numWritten += numRead;
            }
            return response;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public static String loadFile(String s, boolean nohigherchar) throws MyException {
        String cr = "\n";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(s), "UTF-8"));
            String response = new String();
            String line = null;
            while ((line = in.readLine()) != null) {
                response += new String(line) + cr;
            }
            return response;
        } catch (FileNotFoundException e) {
            throw new MyException(MyException.ERROR_FILENOTFOUND, e.getMessage());
        } catch (IOException e) {
            throw new MyException(MyException.ERROR_IO, e.getMessage());
        }
    }

    public static String loadUrl(String surl, String charset) throws MyException {
        try {
            URL url = new URL(surl);
            return Basic.loadUrl(url, charset);
        } catch (MalformedURLException e) {
            throw new MyException(MyException.ERROR_BADURL, surl);
        }
    }

    public static String loadUrl(URL url, String charset) throws MyException {
        try {
            URLConnection conn = url.openConnection();
            InputStream urlin = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlin, charset));
            StringBuffer buff = new StringBuffer();
            char[] cbuf = new char[1028];
            int count;
            while ((count = in.read(cbuf)) != -1) {
                buff.append(new String(cbuf, 0, count));
            }
            return buff.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new MyException(MyException.ERROR_FILENOTFOUND, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new MyException(MyException.ERROR_IO, e.getMessage());
        }
    }

    public static byte[] loadFileCert(String s, boolean flRemoveFooter, boolean fldecode64) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(s));
            boolean trunked = flRemoveFooter;
            boolean started = flRemoveFooter;
            String base64 = new String();
            String line = null;
            while ((line = in.readLine()) != null) {
                if (!flRemoveFooter) {
                    if (!started) {
                        started = line.startsWith("-----");
                        continue;
                    }
                    if (line.startsWith("-----")) {
                        trunked = true;
                        break;
                    }
                }
                base64 += line;
            }
            if ((started == false) || (trunked == false)) throw new IOException("Couldn't find certificate beginning");
            in.close();
            if (fldecode64) return Base64.decodeBase64(base64.getBytes("utf-8"));
            return base64.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readLine(InputStream in) throws IOException {
        boolean eol_is_cr = System.getProperty("line.separator").equals("\r");
        StringBuffer str = new StringBuffer();
        while (true) {
            int i = in.read();
            if (i == -1) {
                if (str.length() > 0) break; else return null;
            } else if (i == '\r') {
                if (eol_is_cr) break;
            } else if (i == '\n') break; else str.append((char) i);
        }
        return str.toString();
    }
}
