package com.carbonfive.flashgateway.security;

import java.io.*;
import java.util.*;
import java.util.Date;
import javax.servlet.http.*;
import javax.servlet.*;
import org.apache.commons.logging.*;
import com.carbonfive.flashgateway.security.*;
import com.carbonfive.flashgateway.security.config.*;
import flashgateway.action.message.*;
import flashgateway.*;
import flashgateway.io.*;

/**
 * GatekeeperFilter is a standard Servlet 2.3 Filter that is designed
 * to inspect the AMF messsage sent by a Flash MX client when trying
 * to invoke a service in the servlet container through Macromedia
 * Flash Remoting MX for J2EE. GatekeeperFilter uses classes in the Flash
 * Remoting distribution to parse AMF messages.
 * <p>
 * GatekeeperFilter relies on Gatekeeper to determine if a particular service
 * invocation is allowed.
 *
 * @web.filter name="GatekeeperFilter"
 * @web.filter-mapping url-pattern="/gateway"
 * @web.filter-init-param name="config-file"
 *                        value="flashgatekeeper.xml"
 */
public class GatekeeperFilter implements Filter {

    private static Log log = LogFactory.getLog(GatekeeperFilter.class.getName());

    Gatekeeper gatekeeper;

    public void init(FilterConfig config) throws ServletException {
        String file = config.getInitParameter("config-file");
        if (file == null) throw new ServletException("init-param \"config-file\" is required");
        try {
            gatekeeper = new Gatekeeper();
            gatekeeper.setConfig(ConfigDigester.digest(file));
            log.info("Loaded gatekeeper config from " + file + ": " + gatekeeper.getConfig());
        } catch (Exception e) {
            log.error("error trying to load gatekeeper config from " + file, e);
            throw new ServletException("error trying to load gatekeeper config from " + file, e);
        }
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        BufferedHttpRequestWrapper wrapped = new BufferedHttpRequestWrapper((HttpServletRequest) request);
        BufferedServletInputStream buffered = wrapped.getBufferedInputStream();
        buffered.mark(wrapped.getContentLength() + 1);
        try {
            MessageDeserializer des = new MessageDeserializer(GatewayConstants.SERVER_J2EE);
            des.setInputStream(buffered);
            ActionMessage requestMessage = des.readMessage();
            for (Iterator bodies = requestMessage.getBodies().iterator(); bodies.hasNext(); ) {
                MessageBody requestBody = (MessageBody) bodies.next();
                if (log.isDebugEnabled()) log.debug("Service invocation: " + requestBody.getTargetURI());
                String serviceName = requestBody.getTargetURI().substring(0, requestBody.getTargetURI().lastIndexOf("."));
                String methodName = requestBody.getTargetURI().substring(requestBody.getTargetURI().lastIndexOf(".") + 1);
                if (!gatekeeper.canInvoke((HttpServletRequest) request, serviceName, methodName)) {
                    String msg = serviceName + " is not a permitted service.\n" + "Request Details: " + getRequestDetails(request);
                    log.warn(msg);
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, msg);
                    return;
                }
            }
        } catch (IOException e) {
            log.error("Error inspecting AMF request", e);
        }
        buffered.reset();
        chain.doFilter(wrapped, response);
    }

    private MessageHeader getSecurityHeader(ActionMessage requestMessage) {
        for (Iterator i = requestMessage.getHeaders().iterator(); i.hasNext(); ) {
            MessageHeader header = (MessageHeader) i.next();
            if (GatewayConstants.SECURITY_HEADER_NAME.equals(header.getName())) return header;
        }
        return null;
    }

    private String getRequestDetails(ServletRequest request) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n=============================================");
        msg.append("\nRequest Received at " + new Date());
        msg.append("\n characterEncoding = " + request.getCharacterEncoding());
        msg.append("\n     contentLength = " + request.getContentLength());
        msg.append("\n       contentType = " + request.getContentType());
        msg.append("\n            locale = " + request.getLocale());
        msg.append("\n           locales = ");
        boolean first = true;
        for (Enumeration locales = request.getLocales(); locales.hasMoreElements(); ) {
            Locale locale = (Locale) locales.nextElement();
            if (first) first = false; else msg.append(", ");
            msg.append(locale.toString());
        }
        for (Enumeration names = request.getParameterNames(); names.hasMoreElements(); ) {
            String name = (String) names.nextElement();
            msg.append("         parameter = " + name + " = ");
            String values[] = request.getParameterValues(name);
            for (int i = 0; i < values.length; i++) {
                if (i > 0) msg.append(", ");
                msg.append(values[i]);
            }
        }
        msg.append("\n          protocol = " + request.getProtocol());
        msg.append("\n        remoteAddr = " + request.getRemoteAddr());
        msg.append("\n        remoteHost = " + request.getRemoteHost());
        msg.append("\n            scheme = " + request.getScheme());
        msg.append("\n        serverName = " + request.getServerName());
        msg.append("\n        serverPort = " + request.getServerPort());
        msg.append("\n          isSecure = " + request.isSecure());
        if (request instanceof HttpServletRequest) {
            msg.append("\n---------------------------------------------");
            HttpServletRequest hrequest = (HttpServletRequest) request;
            msg.append("\n       contextPath = " + hrequest.getContextPath());
            Cookie cookies[] = hrequest.getCookies();
            if (cookies == null) cookies = new Cookie[0];
            for (int i = 0; i < cookies.length; i++) {
                msg.append("\n            cookie = " + cookies[i].getName() + " = " + cookies[i].getValue());
            }
            for (Enumeration names = hrequest.getHeaderNames(); names.hasMoreElements(); ) {
                String name = (String) names.nextElement();
                String value = hrequest.getHeader(name);
                msg.append("\n            header = " + name + " = " + value);
            }
            msg.append("\n            method = " + hrequest.getMethod());
            msg.append("\n          pathInfo = " + hrequest.getPathInfo());
            msg.append("\n       queryString = " + hrequest.getQueryString());
            msg.append("\n        remoteUser = " + hrequest.getRemoteUser());
            msg.append("\nrequestedSessionId = " + hrequest.getRequestedSessionId());
            msg.append("\n        requestURI = " + hrequest.getRequestURI());
            msg.append("\n       servletPath = " + hrequest.getServletPath());
        }
        msg.append("\n=============================================");
        return msg.toString();
    }
}
