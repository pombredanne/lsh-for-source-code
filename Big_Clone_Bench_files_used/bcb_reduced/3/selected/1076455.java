package com.newobjectivity.xbrl.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.digester.Digester;
import org.apache.xmlbeans.XmlObject;
import com.newobjectivity.xbrl.model.Context;
import com.newobjectivity.xbrl.model.ContextDigester;
import com.newobjectivity.xbrl.model.XbrlDigester;
import com.newobjectivity.xbrl.model.exception.XbrlServiceException;
import com.newobjectivity.xbrl.model.usfr.pte.USFinancialReportingTermsContext;
import com.newobjectivity.xbrl.model.usfr.pte.USFinancialReportingTermsDigester;
import com.newobjectivity.xbrl.model.usfr.pte.USFinancialReportingTermsElement;
import com.newobjectivity.xbrl.query.ContextElementsQuery;
import com.newobjectivity.xbrl.query.USFinancialReportingPrimaryTermsElementsQuery;
import com.newobjectivity.xbrl.util.StringUtils;
import com.newobjectivity.xbrl.util.XmlBeansUtils;

/**
 * so the basic idea is we are going to make a map lookup,
 * where there is a list of lookup elements for each context
 * @author cgraham
 *
 */
public class USFinancialReportParserServiceImpl implements USFinancialReportParserService {

    private XbrlDigester contextDigester;

    private Map<String, XbrlDigester> usfrDigesterMap;

    public List<USFinancialReportingTermsContext> makeReportingContextsFromStream(InputStream ios) throws XbrlServiceException {
        XmlObject xml = XmlBeansUtils.parseXml(ios);
        ContextElementsQuery elementsQuery = new ContextElementsQuery();
        XmlObject contextsXml = elementsQuery.collectContexts(xml);
        List<Context> clist = (List<Context>) getContextDigester().digest(StringUtils.makeStreamFromString(contextsXml.toString()));
        USFinancialReportingPrimaryTermsElementsQuery xquerySample = new USFinancialReportingPrimaryTermsElementsQuery();
        List<USFinancialReportingTermsElement> allElements = new ArrayList<USFinancialReportingTermsElement>();
        for (String digesterKey : usfrDigesterMap.keySet()) {
            XbrlDigester digester = usfrDigesterMap.get(digesterKey);
            XmlObject xqCollectAllTerms = xquerySample.collectByTerm(xml, digesterKey);
            List<USFinancialReportingTermsElement> digestedElements = (List<USFinancialReportingTermsElement>) digester.digest(StringUtils.makeStreamFromString(xqCollectAllTerms.toString()));
            if (digestedElements != null) allElements.addAll(digestedElements);
        }
        return correlateContextWithElements(clist, allElements);
    }

    public List<USFinancialReportingTermsContext> correlateContextWithElements(List<Context> contexts, List<USFinancialReportingTermsElement> elements) throws XbrlServiceException {
        Map<String, USFinancialReportingTermsContext> contextMap = new HashMap<String, USFinancialReportingTermsContext>();
        for (Context c : contexts) {
            USFinancialReportingTermsContext utfe = new USFinancialReportingTermsContext(c);
            contextMap.put(utfe.getId(), utfe);
        }
        for (USFinancialReportingTermsElement element : elements) {
            USFinancialReportingTermsContext utfe = contextMap.get(element.getContextRef());
            if (utfe != null) utfe.addUSFinancialReportingTermsElement(element);
        }
        List<USFinancialReportingTermsContext> values = new ArrayList<USFinancialReportingTermsContext>(contextMap.values());
        return filterContextsWitoutFinancialInfo(values);
    }

    private List<USFinancialReportingTermsContext> filterContextsWitoutFinancialInfo(List<USFinancialReportingTermsContext> values) {
        List<USFinancialReportingTermsContext> nvalues = new ArrayList<USFinancialReportingTermsContext>();
        for (USFinancialReportingTermsContext financialReportingTermsContext : values) {
            if (financialReportingTermsContext.elements.size() > 0) nvalues.add(financialReportingTermsContext);
        }
        return nvalues;
    }

    public XbrlDigester getContextDigester() {
        return contextDigester;
    }

    public void setContextDigester(XbrlDigester contextDigester) {
        this.contextDigester = contextDigester;
    }

    public Map<String, XbrlDigester> getUsfrDigesterMap() {
        return usfrDigesterMap;
    }

    public void setUsfrDigesterMap(Map<String, XbrlDigester> usfrDigesterMap) {
        this.usfrDigesterMap = usfrDigesterMap;
    }
}
