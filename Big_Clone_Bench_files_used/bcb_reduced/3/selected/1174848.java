package com.lb.trac.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import net.sf.beanlib.hibernate.HibernateBeanReplicator;
import net.sf.beanlib.hibernate3.Hibernate3BeanReplicator;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import com.lb.trac.pojo.Profilo;
import com.lb.trac.pojo.comparator.OrderBy;

public class TracSetupUtil {

    public static ServletContext SERVLET_CONTEXT = null;

    private List<String> pojoObjects = new ArrayList<String>();

    public String md5(String string) throws GeneralSecurityException {
        MessageDigest algorithm = MessageDigest.getInstance("MD5");
        algorithm.reset();
        algorithm.update(string.getBytes());
        byte messageDigest[] = algorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        return hexString.toString();
    }

    public void setPojoObjects(List<String> pojoObjects) {
        this.pojoObjects = pojoObjects;
    }

    /**
	 * Crea una copia di un oggetto persistente, senza tenerlo legato alla
	 * sessione hibernate. Così facendo per fare riferimento ad oggetti interni
	 * al POJO non caricati (lazy="true") occorre ripassare dal SearchService
	 * 
	 * @param source
	 * @param targetClass
	 * @return
	 */
    public <T> T copyPojoProperties(T source) {
        Assert.notNull(source, "Source must not be null");
        HibernateBeanReplicator r = new Hibernate3BeanReplicator();
        return r.copy(source);
    }

    public static void main(String[] args) throws GeneralSecurityException {
        TracSetupUtil t = new TracSetupUtil();
        System.out.println(t.md5("ADMINadmin"));
    }

    public String getMD5Checksum(byte[] contents) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[2048];
        int ch = 0;
        ByteArrayInputStream bin = new ByteArrayInputStream(contents);
        while ((ch = bin.read(buffer)) > 0) {
            md5.update(buffer, 0, ch);
        }
        bin.close();
        buffer = md5.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buffer.length; i++) {
            byte b = buffer[i];
            sb.append(Integer.toString((b & 0xff) + 0x100, 16));
        }
        return sb.toString();
    }

    public static String getLoginUrl(HttpServletRequest request) {
        String protocol = "http";
        String host = request.getServerName();
        String context = request.getContextPath();
        int port = request.getServerPort();
        if (!context.substring(0, 1).equals("/")) {
            context = "/" + context;
        }
        String url = protocol + "://" + host + ":" + port + context + "/login.do";
        return url;
    }

    public static Criteria createCriteria4Property(Criteria criteria, String propertyName) {
        String[] columnNames = StringUtils.split(propertyName, ".");
        Criteria[] criterias = new Criteria[columnNames.length];
        criterias[0] = criteria;
        for (int i = 0; i < columnNames.length; i++) {
            String property = columnNames[i];
            if (i > 0) {
                criterias[i] = criterias[i - 1].createCriteria(columnNames[i - 1]);
            }
            if (i == columnNames.length - 1) {
                criterias[i].createCriteria(property);
            }
        }
        return criteria;
    }

    public static <T> T copyProperties(Object source, T target, String... excludeProperties) {
        BeanUtils.copyProperties(source, target, excludeProperties);
        return target;
    }

    public static <T> T copyProperties(Object source, T target) {
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
