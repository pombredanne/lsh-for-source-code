package org.eralyautumn.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 常用方法
 * 
 * @author <a href="mailto:fmlou@163.com">HongzeZhang</a>
 * 
 * @version 1.0
 * 
 *          2009-3-2
 */
public class Util {

    public static Calendar calendar = Calendar.getInstance();

    /**
	 * 获取类名
	 * 
	 * @param clazz
	 *            类
	 * @return
	 */
    public static String getClassName(Class<? extends Object> clazz) {
        if (clazz == null) return null;
        String classname = clazz.getName();
        return classname.substring(classname.lastIndexOf(".") + 1);
    }

    public static String getLowerCaseClassName(Class<? extends Object> clazz) {
        return getClassName(clazz).toLowerCase();
    }

    /**
	 * 获取对象类名
	 * 
	 * @param obj
	 * @return
	 */
    public static String getModelName(Object obj) {
        if (obj == null) return null;
        String classname = obj.getClass().getName();
        return classname.substring(classname.lastIndexOf(".") + 1);
    }

    public static String getLowerCaseModelName(Object obj) {
        return toLowerFirstCase(getModelName(obj));
    }

    public static String toLowerFirstCase(String modelName) {
        if (modelName == null || modelName.trim().equals("")) return modelName;
        if (modelName.length() > 1) return modelName.substring(0, 1).toLowerCase() + modelName.substring(1);
        return modelName.toUpperCase();
    }

    public static String getLowerCaseModelName(List<Object> list) {
        if (list == null || list.size() == 0) return null;
        String classname = list.get(0).getClass().getName();
        return toLowerFirstCase(classname.substring(classname.lastIndexOf(".") + 1));
    }

    /**
	 * md5算法加密
	 * 
	 * @param s
	 *            要加密的字符串
	 * @return 新的字符串
	 */
    public static String MD5(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] upFirstString(String[] string) {
        String[] string2 = new String[string.length];
        int i = 0;
        for (String str : string) {
            String s1 = str.trim().substring(0, 1);
            String s2 = str.trim().substring(1, str.trim().length());
            string2[i] = s1.toUpperCase() + s2;
            i++;
        }
        return string2;
    }

    public static String upSignString(String sign, String str) {
        StringBuilder strRe = new StringBuilder();
        String[] strs = str.split(sign);
        for (String strSign : strs) {
            String s1 = strSign.trim().substring(0, 1);
            String s2 = strSign.trim().substring(1, strSign.trim().length());
            strRe.append(s1.toUpperCase()).append(s2);
        }
        return strRe.toString();
    }

    public static String turnType(String type) {
        if (type.toLowerCase().startsWith("int") || type.toLowerCase().startsWith("tinyint") || type.toLowerCase().startsWith("integer") || type.toLowerCase().startsWith("smallint") || type.toLowerCase().startsWith("bigint")) return "int";
        if (type.toLowerCase().startsWith("double") || type.toLowerCase().startsWith("float") || type.toLowerCase().startsWith("decimal") || type.toLowerCase().startsWith("numeric")) return "double";
        if (type.toLowerCase().startsWith("char") || type.toLowerCase().startsWith("varchar") || type.toLowerCase().startsWith("longvarchar")) return "String";
        if (type.toLowerCase().startsWith("time") || type.toLowerCase().startsWith("date") || type.toLowerCase().startsWith("datetime") || type.toLowerCase().startsWith("timestamp")) return "java.util.Date";
        if (type.toLowerCase().startsWith("real")) return "float";
        if (type.equals("java.lang.String")) return "String";
        return type;
    }

    public static Map<String, String>[] getTableFieldMap(String table, String driverClassName, String url, String username, String password) {
        Map<String, String>[] fieldMap = new HashMap[2];
        fieldMap[0] = new HashMap<String, String>();
        fieldMap[1] = new HashMap<String, String>();
        if (driverClassName == null || url == null || username == null || password == null) return fieldMap;
        Statement st = null;
        Connection c = null;
        try {
            Class.forName(driverClassName);
            c = DriverManager.getConnection(url, username, password);
            st = c.createStatement();
            ResultSet rs = st.executeQuery("SHOW FULL COLUMNS FROM " + table);
            while (rs.next()) {
                String name = rs.getString("Field").toLowerCase();
                String type = rs.getString("Type");
                String comment = rs.getString("Comment");
                type = Util.turnType(type);
                fieldMap[0].put(name, type);
                fieldMap[1].put(name, comment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (c != null) try {
                c.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return fieldMap;
    }

    public static List<String> getTableFieldList(String table, String driverClassName, String url, String username, String password) {
        List<String> fieldList = new ArrayList<String>();
        if (driverClassName == null || url == null || username == null || password == null) return fieldList;
        Statement st = null;
        Connection c = null;
        try {
            Class.forName(driverClassName);
            c = DriverManager.getConnection(url, username, password);
            st = c.createStatement();
            ResultSet rs = st.executeQuery("desc " + table);
            while (rs.next()) {
                String name = rs.getString(1);
                fieldList.add(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (c != null) try {
                c.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return fieldList;
    }

    public static String getGBK(String s) {
        if (s == null || s.equals("")) return s;
        String ss = s;
        try {
            ss = new String(s.getBytes(), "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ss;
    }

    public static String getGBK(String s, String code) {
        if (s == null || s.equals("")) return s;
        String ss = s;
        try {
            ss = new String(s.getBytes(code), "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ss;
    }

    public static String getISO(String s) {
        if (s == null || s.equals("")) return s;
        String ss = s;
        try {
            ss = new String(s.getBytes(), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ss;
    }

    public static String getISO(String s, String code) {
        if (s == null || s.equals("")) return s;
        String ss = s;
        try {
            ss = new String(s.getBytes(code), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ss;
    }

    public static String upFirstString(String string) {
        return upSignString("_", string);
    }

    public static void set(Method method, Object bean, Object[] param) {
        try {
            method.invoke(bean, param);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void setParameterToPOJO(Object bean, Class paramClass, String field, String value) throws SecurityException, NoSuchMethodException, ParseException {
        Method method = null;
        method = bean.getClass().getMethod("set" + Util.upFirstString(field), new Class[] { paramClass });
        if (paramClass.equals(String.class)) {
            Util.set(method, bean, new Object[] { value });
        } else if (paramClass.equals(int.class) || paramClass.equals(Integer.class)) {
            int intvalue = Integer.parseInt(value);
            Util.set(method, bean, new Object[] { intvalue });
        } else if (paramClass.equals(float.class) || paramClass.equals(Float.class)) {
            float fvalue = Float.parseFloat(value);
            Util.set(method, bean, new Object[] { fvalue });
        } else if (paramClass.equals(double.class) || paramClass.equals(Double.class)) {
            double dvalue = Double.parseDouble(value);
            Util.set(method, bean, new Object[] { dvalue });
        } else if (paramClass.equals(long.class) || paramClass.equals(Long.class)) {
            long lvalue = Long.parseLong(value);
            Util.set(method, bean, new Object[] { lvalue });
        } else if (paramClass.equals(Date.class)) {
            Date date = null;
            SimpleDateFormat sdf = null;
            if (value.length() > 11) sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss"); else sdf = new SimpleDateFormat("yy-MM-dd");
            date = sdf.parse(value);
            Util.set(method, bean, new Object[] { date });
        }
    }

    public static void setParameterToPOJO(Object bean, String field, Object value) throws SecurityException, NoSuchMethodException, ParseException {
        Method method = null;
        method = bean.getClass().getMethod("set" + Util.upFirstString(field), new Class[] { value.getClass() });
        Util.set(method, bean, new Object[] { value });
    }

    public static Object getParameterToPOJO(Object bean, String field) {
        Method method = null;
        try {
            method = bean.getClass().getMethod("get" + Util.upFirstString(field), null);
            return method.invoke(bean, null);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRootPackage(String packageName) {
        String rootPackage = null;
        String[] key = { "dao", "service", "Impl", "controller", "model", "action" };
        for (int i = 0; i < key.length; i++) {
            int location = 0;
            if ((location = packageName.indexOf(key[i])) > 0) {
                String tmpStr = packageName.substring(0, location);
                rootPackage = tmpStr.substring(0, packageName.lastIndexOf("."));
            }
        }
        return rootPackage;
    }

    public static String getRootPackage(String[] packageName) {
        String rootPackage = null;
        String[] key = { "dao", "service", "Impl", "controller", "model", "action" };
        for (int i = 0; i < key.length; i++) {
            int location = 0;
            if ((location = packageName[0].indexOf(key[i])) > 0) {
                String tmpStr = packageName[0].substring(0, location);
                rootPackage = tmpStr.substring(0, packageName[0].lastIndexOf("."));
                break;
            }
        }
        return rootPackage;
    }

    public static String changeFirstChar(String str, boolean up) {
        if (str == null || str.trim().equals("")) return str;
        String s1 = up ? str.trim().substring(0, 1).toUpperCase() : str.trim().substring(0, 1).toLowerCase();
        String s2 = str.trim().substring(1, str.trim().length());
        return s1 + s2;
    }

    public static String getFieldFromTableName(String tableName) {
        if (tableName == null || tableName.trim().equals("")) return tableName;
        String str = Util.upSignString("_", tableName);
        return Util.changeFirstChar(str, false);
    }

    public static String getNowDate() {
        Date date = new Date();
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public static String getNowDateTime() {
        Date date = new Date();
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date);
    }

    public static void checkFile(File file) throws IOException {
        if (!file.exists()) {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            file.createNewFile();
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean isIpUser(String key, Hashtable<String, Long> filter, int maxHashSize, long maxTimeSize) {
        boolean isSuccess = false;
        long now = System.currentTimeMillis();
        if (!filter.containsKey(key) || (now - filter.get(key) > maxTimeSize)) {
            filter.put(key, now);
            isSuccess = true;
        }
        if (filter.size() >= maxHashSize) {
            for (Enumeration e = filter.keys(); e.hasMoreElements(); ) {
                String name = (String) e.nextElement();
                Long value = filter.get(name);
                if (now - value > maxTimeSize) filter.remove(name);
            }
        }
        return isSuccess;
    }

    public static String[] getROrderChars(String str) {
        if (null == str) {
            return null;
        }
        str = str.trim();
        int j = str.length();
        String[] rC = new String[j];
        for (char tempC : str.toCharArray()) {
            rC[--j] = String.valueOf(tempC);
        }
        return rC;
    }

    public static Date getStringToDate(String dateStr, String dateFormat) {
        if (null == dateFormat) {
            dateFormat = "yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        Date date = null;
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getISOForYui(String s) {
        if (s == null) return s;
        String s2 = null;
        try {
            s2 = new String(s.getBytes("ISO8859_1"), "UTF-8");
            s2 = new String(s2.getBytes("GBK"), "ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s2;
    }

    public static String inputStream2String(InputStream in) throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    public static boolean isNotEmpty(String str) {
        return (str != null && !str.trim().equals(""));
    }

    public static boolean isEmpty(String str) {
        return (str == null || str.trim().equals(""));
    }

    public static boolean isZero(int str) {
        return str == 0;
    }

    /**
	 * 得到未来几天的月-日字符串
	 * 
	 * @param format
	 * @param next
	 * @param type
	 *            :1:"YYYY-MM-dd",2:"MM-dd"
	 * @return
	 */
    public static final String getNextDate(String format, int next, int type) {
        if (format == null || format.length() == 0) {
            format = "yyyy-MM-dd";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        long dayLong = 24 * 60 * 60 * 1000;
        String nextDate = null;
        nextDate = sdf.format(new Date().getTime() + next * dayLong);
        if (type == 1) {
            return nextDate;
        }
        return nextDate.substring(5);
    }

    /**
	 * 将MM-dd类型的字符串转成中文显示形式
	 * 
	 * @param date
	 * @return
	 */
    public static final String getDateChinese(String date) {
        return date.replaceAll("-", "月") + "日";
    }

    /**
	 * 将日期字符串转换成日期类型
	 * 
	 * @param text
	 * @param pattern
	 * @return
	 */
    public static java.util.Date text2Date(String text, String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            date = formatter.parse(text);
        } catch (Exception e) {
        }
        return date;
    }

    /**
	 * 获取两个时间之间的天数，小时，分，秒
	 * 
	 * @param bigDate
	 * @return
	 */
    public static int[] getBetweenDate(String bigDate) {
        if (bigDate == null) return null;
        Date date = Util.formartDateTime(bigDate);
        Long durTime = 0L;
        durTime = date.getTime() - System.currentTimeMillis();
        int[] days = new int[4];
        if (durTime <= 0) {
            return null;
        }
        days[0] = (int) (durTime / (1000 * 3600 * 24));
        days[1] = (int) (durTime - new Long(days[0] * (1000 * 3600 * 24))) / (1000 * 3600);
        days[2] = (int) (durTime - new Long(days[0] * (1000 * 3600 * 24) + days[1] * (1000 * 3600))) / (1000 * 60);
        days[3] = (int) (durTime - new Long(days[0] * (1000 * 3600 * 24) + days[1] * (1000 * 3600) + days[2] * (1000 * 60))) / 1000;
        return days;
    }

    public static SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

    public static Date formartDate(String date) {
        try {
            return sdf2.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date formartDateTime(String date) {
        try {
            return sdf1.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * 得到某个对象public 属性为null 的数量
	 * 
	 * @param object
	 * @return
	 */
    public static int classFieldIsNullNum(Object object) {
        int num = 0;
        Field fields[] = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                System.out.println(field.getName());
                System.out.println("field.isAccessible():" + field.isAccessible());
                if (field.isAccessible()) {
                    System.out.println(field.getName());
                    System.out.println(field.get(object).getClass());
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return num;
    }

    /**
	 * 得到两个数的百分比
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
    public static String percent(double p1, double p2) {
        String str;
        double p3 = p1 / p2;
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(0);
        str = nf.format(p3);
        return str;
    }

    /**
	 * 获得随机列表
	 * 
	 * @param num
	 *            随机数量
	 * @param fromList
	 *            从那个列表取
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static List getRandList(int num, List fromList) {
        List newList = new ArrayList();
        if (fromList.size() <= num) {
            return fromList;
        } else {
            List randNumsList = getRandNum(num, fromList.size());
            for (int i = 0; i < num; i++) {
                newList.add(fromList.get((Integer) randNumsList.get(i)));
            }
        }
        return newList;
    }

    /**
	 * 随机获取几个数字
	 * 
	 * @param num
	 *            获取几个
	 * @param size
	 *            从多少中获取
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static List getRandNum(int num, int size) {
        List list = new ArrayList();
        Random rand = new Random();
        int randNum = 1;
        for (int i = 0; i < num; i++) {
            boolean same = false;
            randNum = rand.nextInt(size);
            for (int j = 0; j < list.size(); j++) {
                if (randNum == (Integer) list.get(j)) {
                    same = true;
                }
            }
            if (same == true) {
                i--;
                continue;
            }
            list.add(randNum);
        }
        return list;
    }

    /**
	 * 获取本地IP
	 * 
	 * @return
	 */
    public static String getLocalIP() {
        String ip = "";
        try {
            Enumeration<?> e1 = (Enumeration<?>) NetworkInterface.getNetworkInterfaces();
            while (e1.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) e1.nextElement();
                if (!ni.getName().equals("eth0")) {
                    continue;
                } else {
                    Enumeration<?> e2 = ni.getInetAddresses();
                    while (e2.hasMoreElements()) {
                        InetAddress ia = (InetAddress) e2.nextElement();
                        if (ia instanceof Inet6Address) continue;
                        ip = ia.getHostAddress();
                    }
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        if ("192.168.233.1".equals(ip)) return "172.16.0.173";
        return ip;
    }

    public static String byteHEX(byte ib) {
        char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0F];
        ob[1] = Digit[ib & 0X0F];
        String s = new String(ob);
        return s;
    }

    /**
	 * 首字母变小写
	 * @param str
	 * @return
	 */
    public static String lowerFirstString(String str) {
        if (str == null || str.trim().equals("")) return str;
        if (str.length() < 2) return str.toLowerCase();
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
	 * 获取当前天数
	 * @return
	 */
    public static int getNowDays() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    /**
	 * 获取指定时间的天数
	 * @return
	 */
    public static int getDays(long time) {
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        sw.flush();
        String s = sw.toString();
        pw.close();
        try {
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static void printStackTrace(Throwable t) {
        t.printStackTrace();
        System.out.println("111111111111");
    }

    public static void main(String[] args) {
        printStackTrace(new Throwable("is null"));
    }
}
