package tester;

import LOGIN_SERVIntf.TLoginData;
import java.io.File;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import parser.*;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.Stub;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.IntHolder;
import javax.xml.rpc.holders.StringHolder;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.message.SOAPHeaderElement;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import uExhibitClasses.*;
import uExhibitClasses.holders.*;

/**
 *
 * @author Admin
 */
public class Main {

    public static void main(String[] args) {
        try {
            Stub stub = (Stub) (new org.tempuri.ILOGIN_SERVserviceLocator().getILOGIN_SERVPort());
            org.tempuri.ILOGIN_SERVbindingStub s = (org.tempuri.ILOGIN_SERVbindingStub) stub;
            TLoginData loginData = new TLoginData();
            loginData.setLogin("s.startsev");
            String text = "1";
            String md5 = null;
            try {
                StringBuffer code = new StringBuffer();
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                byte bytes[] = text.getBytes();
                byte digest[] = messageDigest.digest(bytes);
                for (int i = 0; i < digest.length; ++i) {
                    code.append(Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1));
                }
                md5 = code.toString();
            } catch (Exception e) {
                System.out.println(e);
            }
            System.out.println(md5);
            loginData.setPwdHash(md5);
            loginData.setLocalIP("93.85.154.8");
            loginData.setMACAddress("00-53-45-00-00-00");
            StringHolder str = new StringHolder();
            IntHolder res = new IntHolder();
            s.serv_LogIn(loginData, new IntHolder(), str, res);
            System.out.println(str.value);
        } catch (Exception ex) {
            System.out.println(ex);
            if (ex instanceof AxisFault) {
                ((AxisFault) ex).printStackTrace();
            }
        }
    }
}
