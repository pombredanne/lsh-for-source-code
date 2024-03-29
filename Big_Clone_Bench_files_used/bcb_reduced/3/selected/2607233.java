package pl.bristleback.server.bristle.engine.netty;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder;
import org.jboss.netty.util.CharsetUtil;
import pl.bristleback.server.bristle.api.DataController;
import pl.bristleback.server.bristle.api.WebsocketConnector;
import pl.bristleback.server.bristle.engine.WebsocketVersions;
import pl.bristleback.server.bristle.utils.ExtendedHttpHeaders;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;

/**
 * //@todo class description
 * <p/>
 * Created on: 2011-07-18 13:52:09 <br/>
 *
 * @author Wojciech Niemiec
 */
public class HttpRequestHandler {

    private static Logger log = Logger.getLogger(HttpRequestHandler.class.getName());

    private static final String WEBSOCKET_ACCEPT_HYBI_10_PARAMETER = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static final int HIXIE_BUFFER_SIZE = 16;

    private static final String WEBSOCKET_PATH = "/websocket";

    private static final String NULL_VALUE = "null";

    private NettyServerEngine engine;

    public HttpRequestHandler(NettyServerEngine engine) {
        this.engine = engine;
    }

    public void handleHttpRequest(ChannelHandlerContext context, HttpRequest request) {
        if (isNotHttpGetRequest(request)) {
            sendHttpResponse(context, request, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
        }
        try {
            HttpResponse response = processHandshake(context, request);
            if (!response.getStatus().equals(HttpResponseStatus.SWITCHING_PROTOCOLS)) {
                sendHttpResponse(context, request, response);
                return;
            }
            initializeWebsocketConnector(context, request, response);
            replaceListeners(context, request);
        } catch (NoSuchAlgorithmException e) {
            context.getChannel().close();
        }
    }

    private void replaceListeners(ChannelHandlerContext context, HttpRequest request) {
        ChannelPipeline pipeline = context.getChannel().getPipeline();
        pipeline.remove("aggregator");
        int maxFrameSize = engine.getEngineConfiguration().getMaxFrameSize();
        if (hasHeaderWithValue(request, ExtendedHttpHeaders.Names.SEC_WEBSOCKET_VERSION, WebsocketVersions.HYBI_13.getVersionCode())) {
            pipeline.replace("decoder", "wsDecoder", new HybiFrameDecoder(maxFrameSize));
            pipeline.replace("encoder", "wsEncoder", new HybiFrameEncoder());
        } else {
            pipeline.replace("decoder", "wsDecoder", new WebSocketFrameDecoder(maxFrameSize));
            pipeline.replace("encoder", "wsEncoder", new WebSocketFrameEncoder());
        }
    }

    private void initializeWebsocketConnector(final ChannelHandlerContext context, final HttpRequest request, HttpResponse response) {
        String controllerName = request.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL);
        final DataController controllerUsed = engine.getConfiguration().getDataController(controllerName);
        ChannelFuture future = context.getChannel().write(response);
        future.addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    WebsocketConnector<Channel> connector = new NettyConnector(future.getChannel(), engine, controllerUsed);
                    connector.setWebsocketVersion(request.getHeader(ExtendedHttpHeaders.Names.SEC_WEBSOCKET_VERSION));
                    if (future.getChannel().isConnected()) {
                        context.setAttachment(connector);
                        engine.startConnector(connector);
                    }
                }
            }
        });
    }

    private HttpResponse processHandshake(ChannelHandlerContext context, HttpRequest request) throws NoSuchAlgorithmException {
        HttpResponse response;
        if (!hasHeaderWithValue(request, HttpHeaders.Names.CONNECTION, HttpHeaders.Values.UPGRADE) || !hasHeaderWithValue(request, HttpHeaders.Names.UPGRADE, HttpHeaders.Values.WEBSOCKET)) {
            return new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        }
        if (hasBadProtocolHeader(request)) {
            return new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_ACCEPTABLE);
        }
        if (request.containsHeader(ExtendedHttpHeaders.Names.SEC_WEBSOCKET_VERSION)) {
            response = processHybiHandshake(request);
        } else {
            response = processHixieHandshake(request);
        }
        return response;
    }

    private HttpResponse processHixieHandshake(HttpRequest request) throws NoSuchAlgorithmException {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS);
        addStandardResponseHeaders(request, response);
        response.addHeader(HttpHeaders.Names.SEC_WEBSOCKET_ORIGIN, request.getHeader(HttpHeaders.Names.ORIGIN));
        ChannelBuffer responseContent = createHixieHandshakeContent(request);
        response.setContent(responseContent);
        return response;
    }

    private HttpResponse processHybiHandshake(HttpRequest request) throws NoSuchAlgorithmException {
        String websocketKey = request.getHeader(ExtendedHttpHeaders.Names.SEC_WEBSOCKET_KEY);
        if (StringUtils.isEmpty(websocketKey)) {
            return new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS);
        addStandardResponseHeaders(request, response);
        String websocketAcceptValue = computeHybiAcceptValue(websocketKey);
        response.addHeader(ExtendedHttpHeaders.Names.SEC_WEBSOCKET_ACCEPT, websocketAcceptValue);
        return response;
    }

    private String computeHybiAcceptValue(String websocketKey) throws NoSuchAlgorithmException {
        String concatenatedKey = websocketKey + WEBSOCKET_ACCEPT_HYBI_10_PARAMETER;
        byte[] result = MessageDigest.getInstance("SHA").digest(concatenatedKey.getBytes());
        result = Base64.encodeBase64(result);
        return new String(result);
    }

    private void addStandardResponseHeaders(HttpRequest request, HttpResponse response) {
        response.addHeader(HttpHeaders.Names.UPGRADE, HttpHeaders.Values.WEBSOCKET);
        response.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.UPGRADE);
        response.addHeader(HttpHeaders.Names.LOCATION, HttpHeaders.Values.UPGRADE);
        response.addHeader(HttpHeaders.Names.SEC_WEBSOCKET_LOCATION, getHttpClientLocation(request));
        String protocolRequested = request.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL);
        if (StringUtils.isNotEmpty(protocolRequested)) {
            response.addHeader(HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL, protocolRequested);
        }
    }

    private boolean hasBadProtocolHeader(HttpRequest request) {
        String protocol = request.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_PROTOCOL);
        if (protocol == null) {
            return false;
        }
        if (protocol.isEmpty()) {
            return true;
        }
        return !engine.getConfiguration().getDataControllers().hasController(protocol);
    }

    private ChannelBuffer createHixieHandshakeContent(HttpRequest request) throws NoSuchAlgorithmException {
        String key1 = request.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_KEY1);
        String key2 = request.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_KEY2);
        int keyA = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1.replaceAll("[^ ]", "").length());
        int keyB = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2.replaceAll("[^ ]", "").length());
        long keyC = request.getContent().readLong();
        ChannelBuffer responseBuffer = ChannelBuffers.buffer(HIXIE_BUFFER_SIZE);
        responseBuffer.writeInt(keyA);
        responseBuffer.writeInt(keyB);
        responseBuffer.writeLong(keyC);
        return ChannelBuffers.wrappedBuffer(MessageDigest.getInstance("MD5").digest(responseBuffer.array()));
    }

    private String getHttpClientLocation(HttpRequest request) {
        String location = request.getHeader(HttpHeaders.Names.ORIGIN);
        if (StringUtils.isEmpty(location) || NULL_VALUE.equals(location)) {
            return "ws://" + request.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
        }
        return location;
    }

    private boolean hasHeaderWithValue(HttpRequest request, String headerName, String expectedHeaderValue) {
        String headerValue = request.getHeader(headerName);
        return (headerValue != null) && (expectedHeaderValue.equalsIgnoreCase(headerValue));
    }

    private boolean isNotHttpGetRequest(HttpRequest aReq) {
        return aReq.getMethod() != HttpMethod.GET;
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        if (res.getStatus().getCode() != HttpResponseStatus.OK.getCode()) {
            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }
        ChannelFuture f = ctx.getChannel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != HttpResponseStatus.OK.getCode()) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
