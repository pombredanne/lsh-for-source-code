package com.msgserver.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.primefaces.model.UploadedFile;
import com.msgserver.entity.Employee;

public class MsgServerUtils {

    private static Logger logger = Logger.getLogger(MsgServerUtils.class);

    public static String generateRandomPassword(int pwd_len) {
        final int maxNum = 36;
        int i;
        int count = 0;
        char[] str = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
        StringBuffer pwd = new StringBuffer("");
        Random r = new Random();
        while (count < pwd_len) {
            i = Math.abs(r.nextInt(maxNum));
            if (i >= 0 && i < str.length) {
                pwd.append(str[i]);
                count++;
            }
        }
        return pwd.toString();
    }

    public static boolean isNum(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**read content of an uploaded txt file
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static List<String> readText(UploadedFile file) throws IOException {
        InputStream is = null;
        List<String> uploadedEmps = new ArrayList<String>();
        try {
            is = file.getInputstream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            String result = writer.toString();
            if (result != "" && result.trim() != "") {
                String[] phoneList = result.split("\r\n");
                for (int i = 0; i < phoneList.length; i++) {
                    String item = phoneList[i];
                    if (item.length() == 11 && MsgServerUtils.isNum(item)) {
                        uploadedEmps.add(item);
                    }
                }
            }
        }
        return uploadedEmps;
    }

    /**read content of a excel file
	 * @param file
	 * @return
	 */
    public static List<Employee> readExcel(UploadedFile file) {
        List<Employee> emps = new ArrayList<Employee>();
        InputStream in = null;
        try {
            in = file.getInputstream();
        } catch (IOException e) {
            logger.error("read excel file error " + e.getMessage());
        }
        POIFSFileSystem fs = null;
        try {
            fs = new POIFSFileSystem(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HSSFWorkbook wb = null;
        try {
            wb = new HSSFWorkbook(fs);
        } catch (IOException e) {
            logger.error("convert to HSSFWorkBook error " + e.getMessage());
        }
        if (checkValidExcelFile(wb)) {
            HSSFSheet sheet = wb.getSheetAt(0);
            Iterator rows = sheet.rowIterator();
            List sheetData = new ArrayList();
            while (rows.hasNext()) {
                HSSFRow row = (HSSFRow) rows.next();
                Iterator cells = row.cellIterator();
                List data = new ArrayList();
                while (cells.hasNext()) {
                    HSSFCell cell = (HSSFCell) cells.next();
                    data.add(cell);
                }
                sheetData.add(data);
            }
            for (int i = 1; i < sheetData.size(); i++) {
                Employee emp = new Employee();
                List list = (List) sheetData.get(i);
                if (list.size() != 2) {
                    continue;
                }
                for (int j = 0; j < list.size(); j++) {
                    HSSFCell cell = (HSSFCell) list.get(j);
                    String value = null;
                    int cellType = cell.getCellType();
                    switch(cellType) {
                        case 0:
                            value = String.valueOf(cell.getNumericCellValue());
                            break;
                        case 1:
                            value = cell.getStringCellValue();
                        default:
                            break;
                    }
                    if (j == 0) {
                        emp.setName(value);
                    } else if (j == 1) {
                        emp.setPhoneNumber(value);
                    }
                    if (j < list.size() - 1) {
                        System.out.print(", ");
                    }
                }
                emps.add(emp);
            }
        }
        return emps;
    }

    private static boolean checkValidExcelFile(HSSFWorkbook wb) {
        try {
            HSSFSheet sheet = wb.getSheetAt(0);
            HSSFRow row = sheet.getRow(1);
            int i = row.getLastCellNum() - row.getFirstCellNum();
            if (i == 2) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static String SendSms(String mobile, String content, String x_eid, String x_uid, String x_pwd_md5, Integer x_gate_id) throws UnsupportedEncodingException {
        Integer x_ac = 10;
        HttpURLConnection httpconn = null;
        String result = "-20";
        String memo = content.length() < 70 ? content.trim() : content.trim().substring(0, 70);
        StringBuilder sb = new StringBuilder();
        sb.append("http://gateway.woxp.cn:6630/utf8/web_api/?x_eid=");
        sb.append(x_eid);
        sb.append("&x_uid=").append(x_uid);
        sb.append("&x_pwd_md5=").append(x_pwd_md5);
        sb.append("&x_ac=").append(x_ac);
        sb.append("&x_gate_id=").append(x_gate_id);
        sb.append("&x_target_no=").append(mobile);
        sb.append("&x_memo=").append(URLEncoder.encode(memo, "utf-8"));
        try {
            URL url = new URL(sb.toString());
            httpconn = (HttpURLConnection) url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(httpconn.getInputStream()));
            result = rd.readLine();
            rd.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpconn != null) {
                httpconn.disconnect();
                httpconn = null;
            }
        }
        return result;
    }

    public static String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
