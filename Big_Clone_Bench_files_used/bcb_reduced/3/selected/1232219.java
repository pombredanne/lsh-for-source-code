package hu.organum.lilypondwave.servlet;

import hu.organum.lilypondwave.common.HexUtil;
import hu.organum.lilypondwave.common.Settings;
import hu.organum.lilypondwave.renderer.Renderer;
import hu.organum.lilypondwave.renderer.RendererConfiguration;
import hu.organum.lilypondwave.renderer.RenderingException;
import hu.organum.lilypondwave.renderer.RenderingResult;
import hu.organum.lilypondwave.renderer.ResultFileType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LilyWaveServlet. This servlet throws various error codes if something goes wrong: SC_BAD_REQUEST (400) - For errors
 * that should be fixed by the client, ie. change parameters, change the code, make it shorter etc. SC_REQUEST_TIMEOUT (408) - The request could not
 * be handled in the configured time. SC_SERVICE_UNAVAILABLE (503) - If the request should be processed by a different server, eg. the request can not
 * fit in the queue. SC_NOT_FOUND (404) - If the requested rendered file could not be served.
 */
public class LilyWaveServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LilyWaveServlet.class.getName());

    private static final String PARAM_SOURCE = "q";

    private static final String PARAM_STAFF_SIZE = "s";

    private static final String PARAM_TYPE = "t";

    /**
	 * If this parameter is set then only the hash code is returned upon success.
	 */
    private static final String PARAM_RETURNHASH = "rh";

    private static final String PARAM_HASHCODE = "h";

    private static final String PARAM_JSONP_CALLBACK = "jsonp";

    private static final int DEFAULT_RESOLUTION = 101;

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 64;

    private Settings settings;

    private BlockingQueue<QueueElement> processingQueue;

    private Map<String, RenderingResult> cacheIndex;

    /**
	 * TODO: take more than one request a time (configurable)
	 */
    private class ProcessorWorker implements Runnable {

        @Override
        public void run() {
            LOG.info("Processor worker started");
            while (true) {
                try {
                    QueueElement queueElement = processingQueue.take();
                    synchronized (queueElement) {
                        LOG.info("Code taken from queue, start rendering");
                        renderCode(queueElement);
                        LOG.info("rendering finished");
                        queueElement.notifyAll();
                    }
                } catch (InterruptedException e) {
                    LOG.warning(e.getMessage());
                }
            }
        }
    }

    @Override
    public void init() throws ServletException {
        settings = new Settings();
        processingQueue = new ArrayBlockingQueue<QueueElement>(Integer.valueOf(settings.get("QUEUE_CAPACITY")));
        LinkedHashMap<String, RenderingResult> cacheMap = new LinkedHashMap<String, RenderingResult>(settings.getInteger("CACHE_SIZE"), 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, RenderingResult> eldest) {
                if (size() > settings.getInteger("CACHE_SIZE")) {
                    eldest.getValue().delete();
                    return true;
                } else {
                    return false;
                }
            }
        };
        cacheIndex = Collections.synchronizedMap(cacheMap);
        LOG.info("Servlet initialized");
        new Thread(new ProcessorWorker()).start();
    }

    private Renderer createRenderer(String lilypondCode, String featureName, int requestedSize) {
        int size;
        if (requestedSize > MAX_SIZE) {
            size = MAX_SIZE;
        } else {
            size = requestedSize;
        }
        int resolution = DEFAULT_RESOLUTION * size / DEFAULT_SIZE;
        RendererConfiguration config = new RendererConfiguration();
        config.setUniqueName(getUniqueFileName(lilypondCode, size));
        config.setLilypondCode(lilypondCode);
        config.setFeatureName(featureName);
        config.setResolution(resolution);
        Renderer renderer = new Renderer(settings, config);
        return renderer;
    }

    private String getUniqueFileName(String lilypondCode, int size) {
        String digest = null;
        try {
            digest = HexUtil.getHexString(MessageDigest.getInstance("MD5").digest((lilypondCode + size).getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            LOG.warning(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            LOG.warning(e.getMessage());
        }
        if (digest == null) {
            digest = UUID.randomUUID().toString();
        }
        return digest;
    }

    private void returnResult(RenderingResult renderingResult, HttpServletResponse response, ResultFileType requestedType) {
        boolean success = false;
        BufferedInputStream inputStream;
        try {
            File file = new File(renderingResult.getFile().getParent(), renderingResult.getHash() + "." + requestedType.getExtension());
            inputStream = new BufferedInputStream(new FileInputStream(file));
            response.setContentType(requestedType.getMimeType());
            ServletOutputStream outputStream = response.getOutputStream();
            int b = -1;
            while ((b = inputStream.read()) != -1) {
                outputStream.write(b);
            }
            outputStream.close();
            success = true;
        } catch (FileNotFoundException e) {
            LOG.warning(e.getMessage());
        } catch (IOException e) {
            LOG.warning(e.getMessage());
        }
        if (!success) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }
        }
    }

    private void renderCachedContent(String hash, HttpServletResponse response, ResultFileType requestedType) {
        RenderingResult renderingResult = cacheIndex.get(hash);
        returnResult(renderingResult, response, requestedType);
    }

    private String createJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<String, Object> entry = iterator.next();
            Object value = entry.getValue();
            String jsonValue;
            if (value instanceof String) {
                jsonValue = "\"" + value + "\"";
            } else {
                jsonValue = String.valueOf(value);
            }
            sb.append(String.format("%s:%s", entry.getKey(), jsonValue));
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private void renderCode(QueueElement element) {
        Renderer renderer = element.getRenderer();
        RenderingResult renderingResult = null;
        try {
            renderingResult = renderer.render();
        } catch (RenderingException renderingException) {
            String msg = "Rendering failed:" + renderingException.getMessage() + " verbose message: " + renderingException.getVerboseMessage();
            LOG.severe(msg);
            try {
                element.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }
        }
        if (renderingResult != null && renderingResult.exists()) {
            cacheIndex.put(renderer.getUniqueName(), renderingResult);
            if (!element.isHashOnly()) {
                returnResult(renderingResult, element.getResponse(), element.getResultFileType());
            } else {
                element.getResponse().setContentType("text/plain");
                try {
                    Map<String, Object> values = new HashMap<String, Object>();
                    values.put("hash", renderer.getUniqueName());
                    for (ResultFileType resultFileType : ResultFileType.values()) {
                        if (renderer.resultExists(resultFileType)) {
                            values.put(resultFileType.name(), true);
                        }
                    }
                    PrintWriter out = element.getResponse().getWriter();
                    String json = createJson(values);
                    if (element.getJsonpCallback() != null) {
                        json = String.format("%s(%s)", element.getJsonpCallback(), json);
                    }
                    out.write(json);
                    element.getResponse().getWriter().close();
                } catch (IOException e) {
                    LOG.warning(e.getMessage());
                }
            }
        } else {
            try {
                element.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }
        }
    }

    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String lilypondCode = request.getParameter(PARAM_SOURCE);
        if (lilypondCode == null && request.getParameter(PARAM_HASHCODE) == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (request.getParameter(PARAM_HASHCODE) != null) {
            renderCachedContent(request.getParameter(PARAM_HASHCODE), response, ResultFileType.valueOf(request.getParameter(PARAM_TYPE)));
            return;
        }
        lilypondCode = "\\header { tagline = \"\" }" + lilypondCode;
        int size;
        if (request.getParameter(PARAM_STAFF_SIZE) != null) {
            size = Integer.parseInt(request.getParameter(PARAM_STAFF_SIZE));
        } else {
            size = DEFAULT_SIZE;
        }
        String featureName;
        if ("true".equals(settings.get("TEST"))) {
            featureName = "testPng";
        } else {
            featureName = "firstPagePng";
        }
        Renderer renderer = createRenderer(lilypondCode, featureName, size);
        ResultFileType resultFileType;
        if (request.getParameter(PARAM_TYPE) != null) {
            resultFileType = ResultFileType.valueOf(request.getParameter(PARAM_TYPE));
        } else {
            resultFileType = ResultFileType.PNG;
        }
        if (renderer.getAlreadyDone()) {
            QueueElement queueElement = new QueueElement(renderer, response, request.getParameter(PARAM_RETURNHASH) != null, resultFileType, request.getParameter(PARAM_JSONP_CALLBACK));
            renderCode(queueElement);
        } else {
            QueueElement queueElement = new QueueElement(createRenderer(lilypondCode, featureName, size), response, request.getParameter(PARAM_RETURNHASH) != null, resultFileType, request.getParameter(PARAM_JSONP_CALLBACK));
            try {
                synchronized (queueElement) {
                    boolean insertedInQueue = processingQueue.offer(queueElement);
                    if (!insertedInQueue) {
                        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        return;
                    } else {
                        queueElement.wait(settings.getInteger("REQUEST_TIMEOUT"));
                    }
                }
            } catch (InterruptedException e) {
                LOG.warning("Given up waiting for element to be handled");
                response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }
}
