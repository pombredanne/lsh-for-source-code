package com.gregor.taglibs.rrd;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.gregor.rrd.RRDTool;
import com.gregor.rrd.RRDException;
import com.keypoint.PngEncoderB;

/**
 * A servlet to call "rrdtool graph" to create a graph and return the
 * resulting output.
 *
 * @author <a href="http://www.gregor.com/dgregor/">DJ Gregor</a>
 * @version $Revision: 79 $
 * @since MailPing 0.85
 */
public class GraphServlet extends HttpServlet {

    private static final String s_initparameter_base = "com.gregor.rrd.GraphServlet";

    private static final long s_expire_default = 1000 * 60 * 10;

    private static final String s_rrdclass_default = "com.gregor.rrd.RRDPipe";

    private static final String s_securerandom_algorithm_default = "SHA1PRNG";

    private static final String s_messagedigest_algorithm_default = "SHA";

    private String m_rrdclass = null;

    private String m_properties = null;

    private String m_securerandom_algorithm = null;

    private String m_messagedigest_algorithm = null;

    private long m_expire = 0;

    private RRDTool m_rrd = null;

    private SecureRandom m_random = null;

    private SortedMap m_map = null;

    /**
     * Initialize this servlet.  Instantiates an instance of RRDTool if it
     * has not already been done or if the desired RRDTool class has changed
     * since instantiation last occurred.  Calls RRDTool.newInstanceOf().
     *
     * @throws ServletException If any one of these exceptions occur while
     *                          calling RRDTool.newInstanceOf():
     *                          ClassNotFoundException, InstantiationException,
     *                          IllegalAccessException
     */
    public void init() throws ServletException {
        if ((m_rrdclass = getInitParameter(s_initparameter_base + ".rrdclass")) == null) {
            m_rrdclass = s_rrdclass_default;
        }
        m_properties = getInitParameter(s_initparameter_base + ".rrdproperties");
        if ((m_messagedigest_algorithm = getInitParameter(s_initparameter_base + ".messagedigest_algorithm")) == null) {
            m_messagedigest_algorithm = s_messagedigest_algorithm_default;
        }
        if ((m_securerandom_algorithm = getInitParameter(s_initparameter_base + ".securerandom_algorithm")) == null) {
            m_securerandom_algorithm = s_securerandom_algorithm_default;
        }
        String expire = getInitParameter(s_initparameter_base + ".expire");
        if (expire == null) {
            m_expire = s_expire_default;
        } else {
            try {
                m_expire = Long.parseLong(expire);
            } catch (NumberFormatException e) {
                throw new ServletException("NumberFormatException while " + "attempting to parse value of " + s_initparameter_base + ".expire" + ": \"" + expire + "\": " + e.toString());
            }
        }
        try {
            m_rrd = RRDTool.newInstanceOf(m_rrdclass);
        } catch (ClassNotFoundException e) {
            throw new ServletException("ClassNotFoundException while trying " + "to create newInstanceOf(\"" + m_rrdclass + "\"): " + e.toString());
        } catch (InstantiationException e) {
            throw new ServletException("InstantiationException while trying " + "to create newInstanceOf(\"" + m_rrdclass + "\"): " + e.toString());
        } catch (IllegalAccessException e) {
            throw new ServletException("IllegalAccessException while trying " + "to create newInstanceOf(\"" + m_rrdclass + "\"): " + e.toString());
        }
        if (m_properties != null) {
            try {
                m_rrd.setProperties(m_properties);
            } catch (RRDException e) {
                throw new ServletException("RRDException while attempting " + "setProperties(\"" + m_properties + "\"): " + e.toString());
            }
        }
        try {
            m_random = SecureRandom.getInstance(m_securerandom_algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("NoSuchAlgorithmException while " + "attempting to get instance of \"" + m_securerandom_algorithm + "\": " + e);
        }
        createCommandMap();
    }

    /**
     * Destroy this servlet.  Calls deInit() on the RRDTool instance, if one
     * was instantiated.
     */
    public void destroy() {
        try {
            m_rrd.deInit();
        } catch (IOException e) {
        } catch (RRDException e) {
        }
    }

    /**
     * Handle an HTTP GET request to produce a graph.  storeCommand() should
     * be called for each request to store the command for "rrdtool graph"
     * and to produce an "&lt;IMG SRC=.../&gt;" tag that will properly
     * direct the client to this servlet.
     *
     * @throws ServletException If an error occurs when calling RRDTool.graph()
     * @throws IOException If a an IO error occurs while writing any output
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String command = null;
        if (request.getAttribute("showmap") != null) {
            returnShowMap(request, response);
            return;
        }
        if ((command = (String) request.getAttribute("command")) != null) {
            PrintWriter out = response.getWriter();
            if (request.getAttribute("printonly") != null) {
                String[] printValues = printCommand(request, response, command, true);
                request.setAttribute("printvalues", printValues);
            } else {
                out.println(graphCommand(request, response, command, (String) request.getAttribute("name")));
            }
            out.flush();
            return;
        }
        long startTime = System.currentTimeMillis();
        response.setHeader("Cache-Control", "no-store, private");
        response.setDateHeader("Date", startTime);
        response.setDateHeader("Expires", startTime);
        String p;
        if ((p = request.getParameter("id")) != null) {
            returnGraphOnDemand(request, response, p);
        } else if ((p = request.getParameter("name")) != null) {
            returnGraphByName(request, response, p);
        } else {
            graphError(request, response, "Request missing information", "The request is missing information " + "indicating which data to return.  " + "The parameters \"id\" or \"name\" must be set.", (request.getParameter("debug") != null));
        }
    }

    /**
     * Store a command for later graphing and return an "IMG"
     * tag with a reference to the stored command to be sent to the client.
     *
     * @param request request object from the client
     * @param response response object to the client
     * @param command RRD command string used to generate graph
     * @return IMG tag containing a reference to the
     *         stored graph.  The reference contains a randomly-generated
     *         value and is only intended to work for this one graph and
     *         for a short period of time.  See <code>m_expire</code>.
     * @throws ServletException If <code>insertCommand</code> throws a
     *         <code>ServletException</code>
     */
    protected String graphCommand(HttpServletRequest request, HttpServletResponse response, String command) throws ServletException {
        String id = insertCommand(command);
        return "<IMG SRC=\"" + response.encodeURL(selfURL(request) + "?id=" + id) + "\">";
    }

    /**
     * Graph a command to a temporary file on disk referenced by name and
     * return an "IMG" tag with that reference.
     *
     * If <code>name</code> is null, the three argument version of
     * <code>graphCommand</code> is called.
     *
     * @param request request object from the client
     * @param response response object to the client
     * @param command RRD command string used to generate graph
     * @param name reference name for this graph
     * @return "IMG" tag containing the reference by name to
     *         the generated graph.  Returns error text inside of square
     *         brackets if <code>RRDTool.graph</code> or
     *         <code>RRDTool.parseGraphSizes</code> return
     *         <code>RRDException</code>.
     * @throws ServletException If <code>graphCommand</code> throws a
     *         <code>ServletException</code> or <code>RRDTool.graph</code>
     *         throws an <code>IOException</code>
     */
    protected String graphCommand(HttpServletRequest request, HttpServletResponse response, String command, String name) throws ServletException {
        if (name == null) {
            return graphCommand(request, response, command);
        }
        String ret[];
        int sizes[];
        String path = getGraphPath(name);
        try {
            ret = m_rrd.graph(path, command);
        } catch (IOException e) {
            throw new ServletException("IO exception while working with " + "RRD: " + e);
        } catch (RRDException e) {
            return "[Error from rrdtool: " + e.getMessage() + "]";
        }
        try {
            sizes = m_rrd.parseGraphSize(ret[0]);
        } catch (RRDException e) {
            return "[Error parsing graph sizes: " + e.getMessage() + "]";
        }
        String file_encoded;
        try {
            file_encoded = URLEncoder.encode(name, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new ServletException("Could not create a URLEncoder for" + "US-ASCII: " + e);
        }
        return "<IMG SRC=\"" + response.encodeURL(selfURL(request) + "?name=" + file_encoded) + "\"/>";
    }

    protected String[] printCommand(HttpServletRequest request, HttpServletResponse response, String command, boolean printOnly) throws ServletException {
        String[] printValues;
        int[] sizes;
        try {
            printValues = m_rrd.graph("/dev/null", command);
        } catch (IOException e) {
            throw new ServletException("IO exception while working with " + "RRD: " + e);
        } catch (RRDException e) {
            throw new ServletException("Error from rrdtool: " + e.getMessage());
        }
        try {
            sizes = m_rrd.parseGraphSize(printValues[0]);
        } catch (RRDException e) {
            throw new ServletException("Error parsing graph sizes: " + e.getMessage());
        }
        return printValues;
    }

    /**
     * Creates graph on demand and returns it to the client.  The three
     * argument form of <code>graphCommand</code> must have been previously
     * called to store the graph command.
     * <code>HttpServletResponse.getWriter</code> and
     * <code>HttpServletResponse.getOutputStream</code> must not have been
     * used before this method is called.
     *
     * If an error occurs and the <code>debug</code> parameter is not set,
     * <code>graphError</code> will be used to create an image containing
     * text about the error.  This is used instead of throwing an exception
     * because the client is usually expecting an image to inline within a
     * page, and in this case, the user will never see the error.  If the
     * <code>debug</code> parameter is set, exceptions will be thrown for
     * error conditions instead of generating an image.
     * 
     * @param request request object from the client
     * @param response response object to the client
     * @param id the <code>id</code> parameter passed from the client.  This
     *           is the value that is randomly-generated by
     *           <code>graphCommand</code> and included in the
     *           "IMG" tag that is passed back to the client.
     * @throws ServletException If the <code>debug</code> parameter is set
     *                          by the client and an RRDException thrown from
     *                          <code>RRDTool.graph</code>
     * @throws IOException If an <code>IOException</code> is thrown from
     *                     <code>RRDTool.graph</code> or while writing data
     *                     to the client.
     */
    protected void returnGraphOnDemand(HttpServletRequest request, HttpServletResponse response, String id) throws ServletException, IOException {
        String command = (String) m_map.get(id);
        expireCommandMap();
        if (command == null) {
            graphError(request, response, "Graph not found", "The stored graph command for \"" + id + "\" was not found. " + "Do you need to refresh the calling page?", (request.getParameter("debug") != null));
            return;
        }
        if (request.getParameter("debug") != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                m_rrd.graph(out, "- " + command);
            } catch (RRDException e) {
                throw new ServletException("Error while graphing: " + e);
            }
            response.setContentType("image/png");
            out.writeTo(response.getOutputStream());
        } else {
            try {
                response.setContentType("image/png");
                m_rrd.graph(response.getOutputStream(), "- " + command);
            } catch (RRDException e) {
                graphError(request, response, e.getMessage(), e.toString(), false);
                return;
            }
        }
    }

    /**
     * Return a previously stored graph by name reference.  The four argument
     * version of <code>graphCommand</code> must have been previously
     * called to create the graph into the servlet's temporary directory.
     *
     * <code>HttpServletResponse.getWriter</code> and
     * <code>HttpServletResponse.getOutputStream</code> must not have been
     * used before this method is called.
     *
     * @param request request object from the client
     * @param response response object to the client
     * @param name the <code>name</code> parameter passed from the client.
     *             This is the same value that was passed to
     *             <code>graphCommand</code> and included in the
     *             "IMG" tag that is passed back to the
     *             client.
     * @throws ServletException If a <code>ServletException</code> is thrown
     *                          by <code>getGraphPath</code>
     * @throws IOException If an <code>IOException</code> is thrown while
     *                     reading data from a temporary file or writing data
     *                     to the client.
     */
    protected void returnGraphByName(HttpServletRequest request, HttpServletResponse response, String name) throws ServletException, IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(getGraphPath(name));
        } catch (FileNotFoundException e) {
            graphError(request, response, "Graph not found", "The stored graph file for \"" + name + "\" was not found. " + "Do you need to refresh the calling page?", (request.getParameter("debug") != null));
        }
        OutputStream out = response.getOutputStream();
        response.setContentType("image/png");
        try {
            byte[] buffer = new byte[16 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            in.close();
            throw e;
        }
    }

    /**
     * Provide basic statistics on the graph command map.
     *
     * This should not be accessable except for testing as it would allow
     * anyone who can access the servlet to view any graph, which may be
     * a problem if sensitive information is contained in the graphs.
     *
     * @param request request object from the client
     * @param response response object to the client
     * @throws IOException If an error occurs while sending data to the client
     */
    protected void returnShowMap(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body><p>");
        synchronized (m_map) {
            out.println("Current time: " + System.currentTimeMillis() + "<br/>Expire: " + m_expire + "<br/><br/>");
            out.println("Count: " + m_map.size() + "<br/>");
            Iterator i = m_map.keySet().iterator();
            while (i.hasNext()) {
                out.println((String) i.next() + "<br/>");
            }
            out.println("<br/>");
            long time = System.currentTimeMillis() - m_expire;
            SortedMap head = m_map.headMap(time + "-");
            i = head.keySet().iterator();
            while (i.hasNext()) {
                out.println((String) i.next() + "<br/>");
            }
            out.println("Count: " + head.size() + "<br/>");
        }
        out.println("</p></body></html");
    }

    /**
     * Convert a named graph to an absolute temporary file path.
     */
    protected String getGraphPath(String name) throws ServletException {
        String hash;
        try {
            MessageDigest md = MessageDigest.getInstance(m_messagedigest_algorithm);
            md.update(name.getBytes());
            hash = bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("NoSuchAlgorithmException while " + "attempting to hash file name: " + e);
        }
        File tempDir = (File) getServletContext().getAttribute("javax.servlet.context.tempdir");
        return tempDir.getAbsolutePath() + File.separatorChar + hash;
    }

    /**
     * Return an error to the client as an image or text.
     *
     * @param request request object from the client
     * @param response response object to the client
     * @param error short error text
     * @param details detailed error text, returned if debug is true
     * @param debug false to return an image, true to return text
     * @throws IOException If an error occurs while sending data to the client
     */
    protected void graphError(HttpServletRequest request, HttpServletResponse response, String error, String details, boolean debug) throws IOException {
        if (debug) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println(" <head>");
            out.println("  <title>");
            out.println("   " + error);
            out.println("  </title>");
            out.println(" </head>");
            out.println(" <body>");
            out.println("  <h1>");
            out.println("   " + error);
            out.println("  </h1>");
            out.println("  <p>");
            out.println("   " + details);
            out.println("  </p>");
            out.println(" </body>");
            out.println("</html>");
        } else {
            makeImage("[Error: " + error + "]", response);
        }
    }

    /**
     * Return a self-referencing URL based on request attributes.
     *
     * Used with we are called through an include() and need to
     * generate a link to pass back to a client.  Constructs the
     * URL with javax.servlet.include.{context_path,servlet_path}.
     *
     * @param request request object from the client
     * @return self-referencing URL
     */
    protected String selfURL(HttpServletRequest request) {
        String context_path = (String) request.getAttribute("javax.servlet.include.context_path");
        String servlet_path = (String) request.getAttribute("javax.servlet.include.servlet_path");
        return context_path + servlet_path;
    }

    /**
     * Create the RRDTool graph command map for later use.
     *
     * A custom sort comparator is used to sort based on the ID, which is
     * assumed to be &lt;time&gt;-&lt;randomstring&gt;.  The sort is done
     * in numercial order by time.  This way, headMap(...).keySet().clear()
     * can be used to easily trim old entries from the map.
     */
    protected void createCommandMap() {
        m_map = Collections.synchronizedSortedMap(new TreeMap(new Comparator() {

            public int compare(Object o1, Object o2) {
                String s1 = (String) o1;
                String s2 = (String) o2;
                int i1 = s1.indexOf('-');
                int i2 = s2.indexOf('-');
                if (i1 != -1) {
                    s1 = s1.substring(0, i1);
                }
                if (i2 != -1) {
                    s2 = s2.substring(0, i2);
                }
                try {
                    long l1 = Long.parseLong(s1);
                    long l2 = Long.parseLong(s2);
                    if (l1 < l2) {
                        return -1;
                    } else if (l1 > l2) {
                        return 1;
                    }
                } catch (NumberFormatException e) {
                }
                return s1.compareTo(s2);
            }
        }));
    }

    /**
     * Add a new RRDTool graph command to the command map, returning a
     * newly-created ID to identify this graph.  The returned ID should
     * eventually be passed to returnGraphOnDemand(..) to create the
     * graph on the fly and return it to the user.
     *
     * @param command RRDTool graph command
     * @return ID string that can be passed to returnGraphOnDemand(...)
     */
    protected String insertCommand(String command) throws ServletException {
        String digest;
        try {
            MessageDigest md = MessageDigest.getInstance(m_messagedigest_algorithm);
            md.update(command.getBytes());
            byte bytes[] = new byte[20];
            m_random.nextBytes(bytes);
            md.update(bytes);
            digest = bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("NoSuchAlgorithmException while " + "attempting to generate graph ID: " + e);
        }
        String id = System.currentTimeMillis() + "-" + digest;
        m_map.put(id, command);
        return id;
    }

    /**
     * Convenience method to convert a byte array to a hex string.
     *
     * From http://forum.java.sun.com/thread.jsp?thread=252591&forum=31&message=936765
     *
     * @param data the byte[] to convert
     * @return String the converted byte[]
     */
    public static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]));
        }
        return (buf.toString());
    }

    /**
     * Convenience method to convert a byte to a hex string.
     *
     * From http://forum.java.sun.com/thread.jsp?thread=252591&forum=31&message=936765
     *
     * @param data the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }

    /**
     * Convenience method to convert an int to a hex char.
     *
     * From http://forum.java.sun.com/thread.jsp?thread=252591&forum=31&message=936765
     *
     * @param i the int to convert
     * @return char the converted char
     */
    public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) return (char) ('0' + i); else return (char) ('a' + (i - 10));
    }

    /**
     * Expire entries in the command map that were created m_expire
     * milliseconds ago.
     */
    protected void expireCommandMap() {
        long time = System.currentTimeMillis() - m_expire;
        synchronized (m_map) {
            m_map.headMap(time + "-").keySet().clear();
        }
    }

    /**
     * Return an PNG image to the client containing the provided message.
     * The message will be in a 400x16 pixel image and will be in 12pt
     * italic Serif font.
     *
     * @param message the message to be returned to the user.  It will
     *        be on a single line, so make sure that it isn't more than
     *        400 pixels wide.
     * @param response response object to the client
     * @throws IOException if an error occurs writing to the client
     */
    public void makeImage(String message, HttpServletResponse response) throws IOException {
        final int width = 400;
        final int height = 16;
        Graphics2D g = null;
        ServletOutputStream out = response.getOutputStream();
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            g = image.createGraphics();
            g.setColor(Color.white);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.black);
            g.setFont(new Font("Serif", Font.ITALIC, 12));
            g.drawString(message, 1, height - (height / 4) - 1);
            response.setContentType("image/png");
            PngEncoderB encoder = new PngEncoderB(image);
            out.write(encoder.pngEncode());
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
    }
}
