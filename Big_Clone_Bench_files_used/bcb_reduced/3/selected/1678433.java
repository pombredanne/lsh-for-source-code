package com.lullabysoft.demo.spring.ioc.factorybean;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Ryan
 *
 */
public class MessageDigestExample {

    public static void main(String[] args) {
        BeanFactory factory = new XmlBeanFactory(new FileSystemResource("WebRoot/WEB-INF/classes/com/lullabysoft/demo/spring/beans.xml"));
        MessageDigester digester = (MessageDigester) factory.getBean("digester");
        digester.digest("Hello World!");
    }
}
