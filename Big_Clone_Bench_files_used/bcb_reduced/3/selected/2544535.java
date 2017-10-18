package net.infordata.ifw2.web.view;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.Tag;
import net.infordata.ifw2.util.Stack;
import net.infordata.ifw2.web.bnds.IField;
import net.infordata.ifw2.web.bnds.IFieldDecorator;
import net.infordata.ifw2.web.bnds.IForm;
import net.infordata.ifw2.web.bnds.IFormDecorator;
import net.infordata.ifw2.web.ctrl.FlowContext;
import net.infordata.ifw2.web.ctrl.IFlow;
import net.infordata.ifw2.web.ctrl.IFlowAsDialog;
import net.infordata.ifw2.web.ctrl.IFlowComponent;
import net.infordata.ifw2.web.ctrl.IFlowPersonality;
import net.infordata.ifw2.web.ctrl.IFlowRequestProcessor;
import net.infordata.ifw2.web.tags.FormTag;
import net.infordata.ifw2.web.util.WEBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps state context for the current rendering process.<br>
 * The context is maintained as a {@link ThreadLocal} instance.  
 * @author valentino.proietti
 */
public class RendererContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(RendererContext.class);

    private static final ThreadLocal<RendererContext> cvContext = new ThreadLocal<RendererContext>();

    private static volatile int cvCounter = Integer.MIN_VALUE;

    static RendererContext start(IFlow mainFlow, ContentPart main, boolean keepChangedContent) {
        RendererContext ctx = cvContext.get();
        if (ctx != null) throw new IllegalStateException();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("start " + keepChangedContent);
        ctx = new RendererContext(++cvCounter, keepChangedContent);
        ctx.ivElapsedTime = System.currentTimeMillis();
        cvContext.set(ctx);
        ctx.pushFlow(mainFlow);
        ctx.pushContent(main);
        return ctx;
    }

    static RendererContext end() throws IllegalStateException {
        RendererContext ctx = cvContext.get();
        cvContext.set(null);
        if (ctx == null) throw new IllegalStateException();
        if (ctx.ivCPStack == null || ctx.ivCPStack.size() != 1) throw new IllegalStateException("" + (ctx.ivCPStack == null));
        if (ctx.ivFlowStack == null || ctx.ivFlowStack.size() != 1) throw new IllegalStateException("" + (ctx.ivFlowStack == null));
        if (!ctx.ivTagStack.isEmpty()) throw new IllegalStateException("" + (ctx.ivTagStack.size()));
        ctx.ivElapsedTime = System.currentTimeMillis() - ctx.ivElapsedTime;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("CP: total time for rendering phase " + ctx.getId() + " is " + ctx.ivElapsedTime + "ms, " + " digest total time is " + ctx.ivDigestElapsedTime + "ms");
        }
        return ctx;
    }

    public static final RendererContext get() {
        RendererContext ctx = cvContext.get();
        return ctx;
    }

    /**
   * @return true if the rendering phase is running.
   */
    public static final boolean isRendering() {
        return cvContext.get() != null;
    }

    private final FlowContext ivFlowContext;

    private Stack<ContentPart> ivCPStack = new Stack<ContentPart>();

    private Stack<IFlow> ivFlowStack = new Stack<IFlow>();

    private Stack<Map<String, Object>> ivScopeStack = new Stack<Map<String, Object>>();

    private Stack<Map<String, Object>> ivModalScopeStack = new Stack<Map<String, Object>>();

    /**
   * This isn't like the {@link Tag#getParent()} method which returns the parent tag
   * up to the first element of the same jsp.<br>
   */
    private Stack<WrapBodyTag> ivTagStack = new Stack<WrapBodyTag>();

    private List<WrapBodyTag> ivROTagStack = Collections.unmodifiableList(ivTagStack);

    private final Set<String> ivProcessed = new HashSet<String>();

    private final boolean ivKeepChangedContent;

    private final int ivId;

    private MessageDigest ivMessageDigest;

    private long ivElapsedTime;

    private long ivDigestElapsedTime;

    private final String ivContextPath;

    private String ivBrowser;

    private static final String UNKNOWN_BROWSERPLATFORM = "UNKNOWN_BROWSERPLATFORM";

    private String ivBrowserPlatform = UNKNOWN_BROWSERPLATFORM;

    private Integer ivWebViewVersion = -1;

    private boolean ivFocusRequired;

    private RendererContext(int id, boolean keepChangedContent) {
        ivId = id;
        ivKeepChangedContent = keepChangedContent;
        ivScopeStack.push(new HashMap<String, Object>());
        ivModalScopeStack.push(new HashMap<String, Object>());
        ivFlowContext = FlowContext.get();
        if (ivFlowContext == null) throw new NullPointerException("null flow context");
        HttpServletRequest request = getServletRequest();
        ivContextPath = request.getContextPath() == null ? "" : request.getContextPath();
    }

    public final ServletContext getServletContext() {
        return ivFlowContext.getServletContext();
    }

    public final HttpServletRequest getServletRequest() {
        return ivFlowContext.getServletRequest();
    }

    public final int getId() {
        return ivId;
    }

    public final boolean getKeepChangedContentIndicator() {
        return ivKeepChangedContent;
    }

    /**
   * @return see {@link WEBUtil#checkBrowser(HttpServletRequest)}
   */
    public final String getBrowser() {
        if (ivBrowser != null) return ivBrowser;
        try {
            ivBrowser = WEBUtil.checkBrowser(getServletRequest());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return ivBrowser;
    }

    /**
   * @return see {@link WEBUtil#checkBrowserPlatform(HttpServletRequest)
   */
    public final String getBrowserPlatform() {
        if (ivBrowserPlatform == UNKNOWN_BROWSERPLATFORM) {
            try {
                ivBrowserPlatform = WEBUtil.checkBrowserPlatform(getServletRequest());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return ivBrowserPlatform;
    }

    /**
   * @return see {@link WEBUtil#checkWebView(HttpServletRequest)
   */
    public final Integer getWebViewVersion() {
        if (ivWebViewVersion != null && ivWebViewVersion == -1) {
            try {
                ivWebViewVersion = WEBUtil.checkWebView(getServletRequest());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return ivWebViewVersion;
    }

    public final boolean needsFakeSpan() {
        String browser = getBrowser();
        return browser != null && (browser.startsWith("IE") || browser.startsWith("XE"));
    }

    /**
   * @return true if the focus event is delivered after a call to the focus() method is ended 
   */
    public final boolean focusEventDeliveredAsync() {
        String browser = getBrowser();
        return browser != null && (browser.startsWith("IE") || browser.startsWith("XE"));
    }

    /**
   * @return never returns null but an empty string if no context path is present.
   */
    public final String getContextPath() {
        return ivContextPath;
    }

    /**
   * see {@link FlowContext#idToExtId(String)}
   */
    public final String idToExtId(String id) {
        return ivFlowContext.idToExtId(id);
    }

    /**
   * @return the currently rendered {@link ContentPart}
   */
    public final ContentPart getCurrentContentPart() {
        return ivCPStack.peek();
    }

    /**
   * @return the currently rendered {@link IFlow}
   */
    public final IFlow getCurrentFlow() {
        return ivFlowStack.peek();
    }

    /**
   * @return The flow id which is used to serve resource in a get request or
   *     to process flow requests.<br>
   */
    public final String getCurrentFlowId() {
        IFlow flow = getCurrentFlow();
        String res = "";
        while (flow != null) {
            IFlowPersonality pers = flow.getFlowPersonality();
            IFlow parent = pers.getParent();
            if (parent == null) break;
            res = pers.getId() + ContentPart.UID_SEPARATOR + res;
            flow = parent;
        }
        return res;
    }

    /**
   * @param resourceName - the resource name
   * @return the url to be used to get resources directly from an active flow
   */
    public final String getScopedResourceUri(String resourceName) {
        return getScopedResourceUri(null, resourceName);
    }

    /**
   * @param fileName - optional fileName, it is appended to the url, useful to
   *   generate correct file-names for plugins save-as functionalities.
   * @param resourceName - the resource name (mandatory)
   * @return the url to be used to get resources directly from an active flow
   */
    public final String getScopedResourceUri(String fileName, String resourceName) {
        if (resourceName == null) throw new NullPointerException("null resourceName");
        String cfid = getCurrentFlowId();
        if (cfid == null) throw new IllegalStateException("Not in a flow scope");
        String uri = WEBUtil.getCurrentPageUri(getServletRequest());
        uri += (fileName == null ? "" : "/" + WEBUtil.string2Url(fileName)) + "?$ifw$flow=" + FlowContext.get().idToExtId(cfid) + "&$ifw$resource=" + WEBUtil.string2Url(resourceName) + "&";
        return uri;
    }

    public final int getBrowserWidth() {
        return ivFlowContext.getBrowserWidth();
    }

    public final int getBrowserHeight() {
        return ivFlowContext.getBrowserHeight();
    }

    /**
   * @return true if a flow is in the "stack" of current states and its current state is not 
   *   an {@link IFlowRequestProcessor} (ie. a sub-flow or a modal state).
   */
    public final boolean isCurrentFlowActive() {
        boolean res;
        {
            IFlow currentFlow = getCurrentFlow();
            res = !(currentFlow.getCurrentState() instanceof IFlowRequestProcessor) && (currentFlow.getFlowPersonality().getCurrentSystemDialog() == null) && (currentFlow.getFlowPersonality().getCurrentDialog() == null);
        }
        IFlow subFlow = null;
        for (int i = ivFlowStack.size() - 1; res && i >= 0; i--) {
            IFlow flow = ivFlowStack.get(i);
            if (subFlow != null) {
                if (subFlow.getFlowPersonality() instanceof IFlowComponent) {
                    res = ((IFlowComponent) subFlow.getFlowPersonality()).isEnabled() && !(flow.getCurrentState() instanceof IFlowRequestProcessor) && (flow.getFlowPersonality().getCurrentDialog() == null);
                } else if (subFlow.getFlowPersonality() == flow.getFlowPersonality().getCurrentDialog()) ; else if (flow.getCurrentState() != subFlow.getFlowPersonality()) res = false;
            }
            subFlow = flow;
        }
        return res;
    }

    public IFieldDecorator getFieldDecorator(IField field) {
        if (field instanceof IFieldDecorator) return (IFieldDecorator) field;
        return null;
    }

    public IFormDecorator getFormDecorator(IForm form) {
        if (form instanceof IFormDecorator) return (IFormDecorator) form;
        return null;
    }

    /**
   * @return A map usable to store data scoped to the current flow.<br>
   *   A flow scope inherits values from ancestors flow scopes.<br>
   */
    public Map<String, Object> getCurrentFlowScope() {
        return ivScopeStack.peek();
    }

    /**
   * @return A map usable to store data scoped to the current modal dialog.<br>
   *   If a modal dialog is not active, then a default scope is provided.<br>
   *   A modal scope does not inherit values from parent modal scopes.
   */
    public Map<String, Object> getCurrentModalScope() {
        return ivModalScopeStack.peek();
    }

    void pushContent(ContentPart content) {
        if (ivCPStack == null) throw new IllegalStateException();
        String uid = content.getUniqueId();
        if (ivProcessed.contains(uid)) throw new IllegalStateException(uid + " already processed.");
        if (LOGGER.isTraceEnabled()) LOGGER.trace("pushing content " + uid);
        ivProcessed.add(uid);
        ivCPStack.push(content);
    }

    /**
   * First search in its ancestors then in its sub-childs.
   * @param id
   * @return true if the element with the given id is in enabled chain.
   * @throws NullPointerException
   */
    public boolean isChainEnabled(String id) {
        return getContentPart(id, 0).isChainEnabled();
    }

    /**
   * This is the same result of the csac tag.<br>
   * First search in its ancestors then in its sub-childs.
   * @param id
   * @return script code usable to access the element on the client side
   */
    public String getClientScriptAccessCode(String id) {
        return getClientScriptAccessCode(id, 0);
    }

    /**
   * @param id
   * @param upSteps - number of steps up of the {@link ContentPart} tree
   * @return script code usable to access the element on the client side
   */
    String getClientScriptAccessCode(String id, int upSteps) {
        ContentPart cp = getContentPart(id, upSteps);
        return cp == null ? null : cp.getClientScriptAccessCode();
    }

    private ContentPart getContentPart(String id, int upSteps) {
        if (id == null) throw new IllegalArgumentException();
        ContentPart cp = getCurrentContentPart();
        for (int i = 0; i < upSteps; i++) {
            cp = cp.getParent();
            if (cp == null) {
                throw new IllegalArgumentException("Too many upSteps: " + upSteps + " " + getCurrentContentPart());
            }
        }
        if (id.equals(cp.getId()) && cp.getClientScriptAccessCode() != null) return cp;
        ContentPart ancestor = cp.getAncestor(id);
        if (ancestor != null && ancestor.getClientScriptAccessCode() != null) return ancestor;
        cp = cp.getSubChild(id);
        return cp;
    }

    /**
   * @param id
   * @return The unique id, relative to the current {@link ContentPart}, 
   *   no control is made to check if the element really exists
   */
    public String getUniqueId(String id) {
        if (id == null) throw new IllegalArgumentException();
        ContentPart cp = getCurrentContentPart();
        return cp.getUniqueId() + ContentPart.UID_SEPARATOR + id;
    }

    ContentPart popContent() {
        if (ivCPStack == null) throw new IllegalStateException();
        ContentPart res = ivCPStack.pop();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("popped content " + res.getUniqueId());
        if (ivCPStack.empty()) {
            ivCPStack = null;
        }
        return res;
    }

    /**
   * Always pushes the flow and not its personality.
   * @param flow
   */
    void pushFlow(final IFlow flow) {
        if (ivFlowStack == null) throw new IllegalStateException();
        flow.ensureStartup();
        if (LOGGER.isTraceEnabled()) LOGGER.debug("pushing flow " + flow);
        ivFlowStack.push(flow);
        ivScopeStack.push(new HashMap<String, Object>(ivScopeStack.peek()));
        if (flow.getFlowPersonality() instanceof IFlowComponent) {
            if (LOGGER.isTraceEnabled()) LOGGER.debug("pushing component " + flow.getFlowPersonality());
        } else if (flow.getFlowPersonality() instanceof IFlowAsDialog) {
            if (LOGGER.isTraceEnabled()) LOGGER.debug("pushing modal scope " + flow.getFlowPersonality());
            ivModalScopeStack.push(new HashMap<String, Object>());
        }
        if (!ivFocusRequired) {
            ivFocusRequired = ivFlowContext.isRequiringFocus(flow);
            if (LOGGER.isTraceEnabled() && ivFocusRequired) {
                LOGGER.debug("requiring focus: " + flow);
            }
        }
    }

    IFlow popFlow() {
        if (ivFlowStack == null) throw new IllegalStateException();
        IFlow res = ivFlowStack.pop();
        ivScopeStack.pop();
        if (LOGGER.isTraceEnabled()) LOGGER.debug("popped flow " + res);
        if (ivFlowStack.empty()) ivFlowStack = null;
        if (res.getFlowPersonality() instanceof IFlowComponent) {
            if (LOGGER.isTraceEnabled()) LOGGER.debug("popped component ");
        } else if (res.getFlowPersonality() instanceof IFlowAsDialog) {
            ivModalScopeStack.pop();
            if (LOGGER.isTraceEnabled()) LOGGER.debug("popped modal scope " + res.getFlowPersonality());
        }
        return res;
    }

    /**
   * For internal use only. 
   */
    public boolean checkAndResetRequiringFocusIndicator() {
        if (ivFocusRequired) {
            ivFocusRequired = false;
            return true;
        }
        return false;
    }

    /** */
    final byte[] digest(byte[] buf, int offset, int size) {
        if (ivMessageDigest == null) {
            try {
                ivMessageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException(ex);
            }
        } else ivMessageDigest.reset();
        if (LOGGER.isDebugEnabled()) {
            long tt = System.currentTimeMillis();
            ivMessageDigest.update(buf, offset, size);
            byte[] digest = ivMessageDigest.digest();
            ivDigestElapsedTime += (System.currentTimeMillis() - tt);
            return digest;
        }
        ivMessageDigest.update(buf, offset, size);
        byte[] digest = ivMessageDigest.digest();
        return digest;
    }

    private Boolean ivRenderDisabledTextAsReadOnly;

    private Boolean ivRenderDisabledTextAreaAsReadOnly;

    private Boolean ivEnhanceCurrentFormVisibility;

    public boolean renderDisabledTextAsReadOnly() {
        if (ivRenderDisabledTextAsReadOnly == null) {
            ivRenderDisabledTextAsReadOnly = "false".equalsIgnoreCase(getServletContext().getInitParameter("ifw2.disabledTextAsReadOnly")) ? Boolean.FALSE : Boolean.TRUE;
        }
        return ivRenderDisabledTextAsReadOnly.booleanValue();
    }

    public boolean renderDisabledTextAreaAsReadOnly() {
        if (ivRenderDisabledTextAreaAsReadOnly == null) {
            ivRenderDisabledTextAreaAsReadOnly = "false".equalsIgnoreCase(getServletContext().getInitParameter("ifw2.disabledTextAreaAsReadOnly")) ? Boolean.FALSE : Boolean.TRUE;
        }
        return ivRenderDisabledTextAreaAsReadOnly.booleanValue();
    }

    public boolean enhanceCurrentFormVisibility() {
        if (ivEnhanceCurrentFormVisibility == null) {
            ivEnhanceCurrentFormVisibility = "false".equalsIgnoreCase(getServletContext().getInitParameter("ifw2.enhanceCurrentFormVisibility")) ? Boolean.FALSE : Boolean.TRUE;
        }
        return ivEnhanceCurrentFormVisibility.booleanValue();
    }

    /**
   */
    void pushTag(final WrapBodyTag tag) {
        if (ivTagStack == null) throw new IllegalStateException();
        if (LOGGER.isTraceEnabled()) LOGGER.debug("pushing tag " + tag);
        ivTagStack.push(tag);
    }

    WrapBodyTag popTag() {
        if (ivTagStack == null) throw new IllegalStateException();
        WrapBodyTag res = ivTagStack.pop();
        if (LOGGER.isTraceEnabled()) LOGGER.debug("popped tag " + res);
        return res;
    }

    public List<WrapBodyTag> getTagStack() {
        return ivROTagStack;
    }

    public FormTag getCurrentParentFormTag() {
        for (int i = ivTagStack.size() - 1; i >= 0; i--) {
            WrapBodyTag tag = ivTagStack.get(i);
            if (tag instanceof FormTag) return (FormTag) tag;
        }
        return null;
    }
}
