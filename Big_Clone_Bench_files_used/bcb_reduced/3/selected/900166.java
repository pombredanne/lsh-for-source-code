package org.sbff;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.TransformerException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.xpath.XPathAPI;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DeliciousAPIHttpClientImpl implements DeliciousAPI {

    static final String deliciousurl = "http://delicious.com/";

    static final String xPathDeliciousBookmarkCount = "//*[@id=\"tagScopeCount\"]/text()";

    static final String xPathDeliciousBookmarkUsersCount = "//*[contains(@class,\"savers4\")]/text()";

    static final String xPathDeliciousBookmark = "//*[contains(@class,\"taggedlink\")]";

    static final String xPathDeliciousUser = "//*[contains(@class,\"user user-tag\")]/@href";

    static final String xPathDeliciousNextLink = "//*[text()=\"Next >\"]/text()";

    static final String userAgent = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)";

    HttpClient client;

    javax.swing.JLabel partialJLabel = new javax.swing.JLabel("Start");

    javax.swing.BoundedRangeModel partialrange = new javax.swing.DefaultBoundedRangeModel();

    static String deliciousUserFromURL(String url) {
        String user = url;
        int slashindex = user.lastIndexOf("/");
        if (slashindex != -1) {
            user = user.substring(slashindex + 1, user.length());
        }
        int semicolonindex = user.indexOf(";");
        if (semicolonindex != -1) {
            user = user.substring(0, semicolonindex);
        }
        return user;
    }

    public DeliciousAPIHttpClientImpl() {
        Properties systemSettings = System.getProperties();
        systemSettings.put("http.agent", userAgent);
        systemSettings.put("httpclient.useragent", userAgent);
        System.setProperties(systemSettings);
        client = new HttpClient();
        client.getParams().setParameter("http.useragent", userAgent);
        try {
            int proxyPort = Integer.parseInt(systemSettings.getProperty("http.proxyPort"));
            String proxyHost = systemSettings.getProperty("http.proxyHost");
            client.getHostConfiguration().setProxy(proxyHost, proxyPort);
        } catch (java.lang.NumberFormatException e) {
        }
    }

    @Override
    public javax.swing.JLabel getPartialJLabel() {
        return partialJLabel;
    }

    @Override
    public javax.swing.BoundedRangeModel getPartialRangeModel() {
        return partialrange;
    }

    @Override
    public int getLinksCountForUser(String user) {
        try {
            GetMethod method = new GetMethod(deliciousurl + user);
            client.executeMethod(method);
            DOMParser parser = new DOMParser();
            parser.setFeature("http://xml.org/sax/features/namespaces", false);
            parser.parse(new InputSource(method.getResponseBodyAsStream()));
            Document doc = parser.getDocument();
            NodeIterator nl = XPathAPI.selectNodeIterator(doc, xPathDeliciousBookmarkCount);
            Node n;
            while ((n = nl.nextNode()) != null) {
                return Integer.parseInt(n.getNodeValue());
            }
            org.w3c.dom.bootstrap.DOMImplementationRegistry registry = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();
            org.w3c.dom.ls.DOMImplementationLS domImplementationLS = (org.w3c.dom.ls.DOMImplementationLS) registry.getDOMImplementation("LS");
            org.w3c.dom.ls.LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
            org.w3c.dom.DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
            if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) domConfiguration.setParameter("format-pretty-print", Boolean.TRUE);
            org.w3c.dom.ls.LSOutput lsOutput = domImplementationLS.createLSOutput();
            lsOutput.setByteStream(System.out);
            lsSerializer.write(doc, lsOutput);
            throw new RuntimeException("Retrieved page did not contain tagScopeCount element id with links count");
        } catch (java.lang.IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (java.lang.InstantiationException e) {
            throw new RuntimeException(e);
        } catch (java.lang.ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getLinksForUser(String user) {
        int linkNumber = 0;
        List<String> list = new java.util.ArrayList<String>();
        try {
            int pageNumber = 1;
            Document doc;
            do {
                GetMethod method = new GetMethod(deliciousurl + user + "?setcount=100&page=" + pageNumber++);
                client.executeMethod(method);
                DOMParser parser = new DOMParser();
                parser.setFeature("http://xml.org/sax/features/namespaces", false);
                parser.parse(new InputSource(method.getResponseBodyAsStream()));
                doc = parser.getDocument();
                {
                    NodeIterator nl = XPathAPI.selectNodeIterator(doc, xPathDeliciousBookmarkCount);
                    Node n;
                    while ((n = nl.nextNode()) != null) {
                        partialrange.setMaximum(Integer.parseInt(n.getNodeValue()));
                    }
                }
                NodeIterator nl = XPathAPI.selectNodeIterator(doc, xPathDeliciousBookmark);
                Node n;
                while ((n = nl.nextNode()) != null) {
                    String url = n.getAttributes().getNamedItem("href").getNodeValue();
                    list.add(url);
                    partialrange.setValue(linkNumber++);
                    partialJLabel.setText(url);
                }
            } while (XPathAPI.selectSingleNode(doc, xPathDeliciousNextLink) != null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public static String md5hash(String text) {
        java.security.MessageDigest md;
        try {
            md = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(text.getBytes());
        byte[] md5bytes = md.digest();
        return new String(org.apache.commons.codec.binary.Hex.encodeHex(md5bytes));
    }

    @Override
    public List<String> getUsersForURL(String url) {
        int userNumber = 0;
        List<String> list = new java.util.ArrayList<String>();
        Document doc = null;
        try {
            int pageNumber = 1;
            do {
                String currenturl = deliciousurl + "url/" + md5hash(url) + "?show=all&page=" + pageNumber++;
                boolean correctlyGot = false;
                do {
                    try {
                        GetMethod method = new GetMethod(currenturl);
                        client.executeMethod(method);
                        DOMParser parser = new DOMParser();
                        parser.setFeature("http://xml.org/sax/features/namespaces", false);
                        parser.parse(new InputSource(method.getResponseBodyAsStream()));
                        doc = parser.getDocument();
                        {
                            NodeIterator nl = XPathAPI.selectNodeIterator(doc, xPathDeliciousBookmarkUsersCount);
                            Node n;
                            while ((n = nl.nextNode()) != null) {
                                partialrange.setMaximum(Integer.parseInt(n.getNodeValue()));
                            }
                        }
                        NodeIterator nl = XPathAPI.selectNodeIterator(doc, xPathDeliciousUser);
                        Node n;
                        while ((n = nl.nextNode()) != null) {
                            String userurl = n.getNodeValue();
                            String user = deliciousUserFromURL(userurl);
                            list.add(user);
                            partialrange.setValue(userNumber++);
                            partialJLabel.setText(user);
                        }
                        correctlyGot = true;
                    } catch (java.net.SocketException e) {
                        try {
                            Thread.sleep(1 * 60 * 1000);
                        } catch (InterruptedException ie) {
                        }
                    } catch (org.w3c.dom.DOMException e) {
                        dumpProblematicDOM(doc);
                        try {
                            Thread.sleep(10 * 60 * 1000);
                        } catch (InterruptedException ie) {
                        }
                    }
                } while (!correctlyGot);
            } while (XPathAPI.selectSingleNode(doc, xPathDeliciousNextLink) != null);
        } catch (org.w3c.dom.DOMException e) {
            dumpProblematicDOM(doc);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    static void dumpProblematicDOM(Document doc) {
        if (doc != null) {
            try {
                org.w3c.dom.bootstrap.DOMImplementationRegistry registry = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();
                org.w3c.dom.ls.DOMImplementationLS domImplementationLS = (org.w3c.dom.ls.DOMImplementationLS) registry.getDOMImplementation("LS");
                org.w3c.dom.ls.LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
                org.w3c.dom.DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
                if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) domConfiguration.setParameter("format-pretty-print", Boolean.TRUE);
                org.w3c.dom.ls.LSOutput lsOutput = domImplementationLS.createLSOutput();
                java.io.OutputStream out = new java.io.FileOutputStream(new java.io.File("problematic_page_parsed.html"));
                lsOutput.setByteStream(System.out);
                lsSerializer.write(doc, lsOutput);
            } catch (java.lang.IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (java.lang.InstantiationException e) {
                throw new RuntimeException(e);
            } catch (java.lang.ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
