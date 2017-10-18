package com.indigen.victor.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.indigen.victor.util.XmlUtils;

/**
 * This class allows to define samples as nodes within a DOM and to generate an XPath
 * matching all the sample nodes, trying to reduce the number of nodes matching this XPath
 */
public class XPathGeneralizer extends LogEnabled {

    private List samples = new Vector();

    private Map indexMap = new Hashtable();

    private Map indexMapAny = new Hashtable();

    private NodePath commonContainer;

    private int bestScore;

    private List bestMatches;

    private Hashtable pageDoms = new Hashtable();

    /**
	 * Object constructor
	 *
	 */
    public XPathGeneralizer() {
    }

    /**
	 * Add a new sample
	 * @param pageId the page identifier where the sample node is located
	 * @param sampleNode the sample node
	 */
    public void addSample(String pageId, Node sampleNode) {
        if (pageDoms.get(pageId) == null) {
            pageDoms.put(pageId, sampleNode.getOwnerDocument().getDocumentElement());
        }
        samples.add(createNodePath(sampleNode));
    }

    /**
	 * Returns the index (in xpath way) of the given node, ie the 1-based index of the given node
	 * through nodes with same parent and same name.
	 * @param node
	 * @param anyNode
	 * @return int
	 */
    int getNodeIndex(Node node, boolean anyNode) {
        Map indexMap0;
        if (anyNode) indexMap0 = indexMapAny; else indexMap0 = indexMap;
        Integer indexObj = (Integer) indexMap0.get(node);
        if (indexObj != null) return indexObj.intValue();
        int index = 1;
        Node node0 = node.getParentNode().getFirstChild();
        while (node0 != node) {
            if (node0.getNodeType() == Node.TEXT_NODE || node0.getNodeType() == Node.ELEMENT_NODE) {
                if (anyNode || node0.getNodeName().equals(node.getNodeName())) {
                    index++;
                }
            }
            node0 = node0.getNextSibling();
        }
        indexMap0.put(node, new Integer(index));
        return index;
    }

    /**
	 * Generate the best possible XPath expression matching all sample nodes
	 * @return
	 */
    public String generateXPath() {
        bestScore = -1;
        bestMatches = null;
        Iterator i = samples.iterator();
        commonContainer = null;
        while (i.hasNext()) {
            NodePath np = (NodePath) i.next();
            if (commonContainer == null) commonContainer = np; else commonContainer = getCommonNodeXPath(commonContainer, np);
        }
        i = samples.iterator();
        while (i.hasNext()) {
            NodePath np = (NodePath) i.next();
            if (np.equals(commonContainer)) {
                commonContainer = commonContainer.parentNodePath;
                break;
            }
        }
        if (samples.size() == 0) {
            getLogger().error("generateXPath: empty set of samples");
            return null;
        }
        solve(samples, new Vector(), false, -1, 0);
        if (bestMatches != null) {
            String bestXPath = null;
            int minMatch = -1;
            String commonXPath = commonContainer.generateXPath();
            i = bestMatches.iterator();
            while (i.hasNext()) {
                String xpath = (String) i.next();
                xpath = commonXPath + xpath;
                int size = 0;
                Iterator j = pageDoms.values().iterator();
                while (j.hasNext()) {
                    Node dom = (Node) j.next();
                    List nodes = XmlUtils.getNodesFromXPath(dom, xpath);
                    size += nodes.size();
                }
                if (minMatch == -1) minMatch = size;
                if (size <= minMatch) {
                    bestXPath = xpath;
                    minMatch = size;
                }
            }
            return bestXPath;
        }
        return null;
    }

    /**
	 * Solving algorithm
	 * @param nodePathes
	 * @param rule
	 * @param wildcard
	 * @param wildcardStarting
	 * @param score
	 */
    private void solve(List nodePathes, List rule, boolean wildcard, int wildcardStarting, int score) {
        if (wildcardStarting < 0) wildcardStarting = nodePathes.size();
        boolean completed = true;
        boolean hasOneCompleted = false;
        boolean sameNodeName = true;
        boolean sameIndex = true;
        boolean sameIndexAny = true;
        Iterator i = nodePathes.iterator();
        String nodeName = null;
        int index = -1;
        int indexAny = -1;
        while (i.hasNext()) {
            NodePath np = (NodePath) i.next();
            if (np.equals(commonContainer) == false) completed = false; else hasOneCompleted = true;
            if (nodeName == null) nodeName = np.nodeName;
            if (!np.nodeName.equals(nodeName)) sameNodeName = false;
            if (index == -1) index = np.tagIndex;
            if (indexAny == -1) indexAny = np.nodeIndex;
            if (np.tagIndex != index) sameIndex = false;
            if (np.nodeIndex != indexAny) sameIndexAny = false;
        }
        if (completed) {
            String xpath = getXPathFromRuleList(rule);
            if (score < bestScore) {
            } else if (score == bestScore) {
                bestMatches.add(xpath);
            } else {
                bestScore = score;
                bestMatches = new Vector();
                bestMatches.add(xpath);
            }
            return;
        }
        if (hasOneCompleted == false) {
            Map attributes = new Hashtable();
            List nodes0 = new Vector();
            i = nodePathes.iterator();
            while (i.hasNext()) {
                NodePath np0 = (NodePath) i.next();
                NamedNodeMap nnm = np0.node.getAttributes();
                for (int j = 0; nnm != null && j < nnm.getLength(); j++) {
                    Node attrNode = nnm.item(j);
                    String attrName = attrNode.getNodeName();
                    String attrValue = attrNode.getNodeValue();
                    String attrValue0 = (String) attributes.get(attrName);
                    if (attrValue0 == null) {
                        attributes.put(attrName, attrValue);
                    } else if (!attrValue.equals(attrValue0)) {
                        attributes.put(attrName, "*");
                    }
                }
                nodes0.add(np0.parentNodePath);
            }
            List rule0 = new Vector(rule);
            int score0 = score + 1;
            String predicate;
            if (sameNodeName) {
                predicate = nodeName;
                if (predicate.equals("#text")) predicate = "text()";
                score0++;
                if (sameIndex) {
                    predicate += "[" + index + "]";
                    score0++;
                }
            } else {
                predicate = "node()";
                if (sameIndexAny) {
                    predicate += "[" + indexAny + "]";
                    score0++;
                }
            }
            Iterator l = attributes.keySet().iterator();
            while (l.hasNext()) {
                String attrName = (String) l.next();
                boolean commonAttribute = true;
                i = nodePathes.iterator();
                while (i.hasNext()) {
                    NodePath np = (NodePath) i.next();
                    if (np.isElementNode) {
                        Element elem = (Element) np.node;
                        if (elem.getAttribute(attrName).length() == 0) commonAttribute = false;
                    } else {
                        commonAttribute = false;
                    }
                }
                if (commonAttribute) {
                    String attrValue = (String) attributes.get(attrName);
                    predicate += "[@" + attrName + "]";
                }
            }
            rule0.add(0, predicate);
            solve(nodes0, rule0, false, 0, score0);
        }
        int d = distanceToCommonContainer(nodePathes);
        if (score + 3 * d < bestScore) return;
        List rule0 = new Vector(rule);
        if (wildcard == false) {
            rule0.add(0, "");
        }
        for (int j = wildcardStarting; j < nodePathes.size(); j++) {
            boolean doSolve = true;
            List nodes0 = new Vector();
            for (int k = 0; k < nodePathes.size(); k++) {
                NodePath np0 = (NodePath) nodePathes.get(k);
                if (k == j) {
                    if (np0.equals(commonContainer) == false) np0 = np0.parentNodePath; else doSolve = false;
                }
                nodes0.add(np0);
            }
            if (doSolve) solve(nodes0, rule0, true, j, score);
        }
    }

    private int distanceToCommonContainer(NodePath np) {
        int n = 0;
        while (!np.equals(commonContainer)) {
            n++;
            np = np.parentNodePath;
        }
        return n;
    }

    private int distanceToCommonContainer(List nodePathes) {
        int n = -1;
        Iterator i = nodePathes.iterator();
        while (i.hasNext()) {
            NodePath np = (NodePath) i.next();
            int n0 = distanceToCommonContainer(np);
            if (n < 0) n = n0;
            if (n0 < n) n = n0;
        }
        return n;
    }

    private String getXPathFromRuleList(List rule) {
        StringBuffer sb = new StringBuffer("/");
        Iterator i = rule.iterator();
        while (i.hasNext()) {
            String predicate = (String) i.next();
            sb.append(predicate);
            if (i.hasNext()) sb.append("/");
        }
        return sb.toString();
    }

    private static String getSignature(String data) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "FFFFFFFFFFFFFFFF";
        }
        md.update(data.getBytes());
        StringBuffer sb = new StringBuffer();
        byte[] sign = md.digest();
        for (int i = 0; i < sign.length; i++) {
            byte b = sign[i];
            int in = (int) b;
            if (in < 0) in = 127 - b;
            String hex = Integer.toHexString(in).toUpperCase();
            if (hex.length() == 1) hex = "0" + hex;
            sb.append(hex);
        }
        return sb.toString();
    }

    private NodePath createNodePath(Node node) {
        NodePath np0 = new NodePath(node);
        NodePath np = np0;
        while (node.getParentNode() != null && node.getParentNode() != node.getOwnerDocument()) {
            node = node.getParentNode();
            NodePath np1 = new NodePath(node);
            np1.setChildNodePath(np);
            np = np1;
        }
        while (np != null) {
            np.computeSignature();
            np = np.childNodePath;
        }
        return np0;
    }

    private NodePath getCommonNodeXPath(NodePath np1, NodePath np2) {
        NodePath common = null;
        NodePath _np1 = np1.getRootNodePath();
        NodePath _np2 = np2.getRootNodePath();
        while (_np1 != null && _np2 != null && _np1.equals(_np2)) {
            common = _np1;
            _np1 = _np1.childNodePath;
            _np2 = _np2.childNodePath;
        }
        return copyNodePath(common);
    }

    private NodePath copyNodePath(NodePath np) {
        NodePath np0 = null;
        NodePath np1 = null;
        while (np != null) {
            NodePath np2 = new NodePath(np);
            if (np1 == null) np1 = np2;
            np2.setChildNodePath(np0);
            np0 = np2;
            np = np.parentNodePath;
        }
        return np1;
    }

    private class NodePath {

        String nodeName;

        int tagIndex;

        int nodeIndex;

        NodePath parentNodePath;

        NodePath childNodePath;

        String signature;

        Node node;

        boolean isElementNode = false;

        boolean isTextNode = false;

        NodePath(Node node) {
            nodeName = node.getNodeName().toLowerCase();
            tagIndex = getNodeIndex(node, false);
            nodeIndex = getNodeIndex(node, true);
            this.node = node;
            if (node.getNodeType() == Node.ELEMENT_NODE) isElementNode = true;
            if (node.getNodeType() == Node.TEXT_NODE) isTextNode = true;
        }

        NodePath(NodePath np) {
            nodeName = np.nodeName;
            tagIndex = np.tagIndex;
            nodeIndex = np.nodeIndex;
            isElementNode = np.isElementNode;
            isTextNode = np.isTextNode;
            node = np.node;
            signature = np.signature;
        }

        void setParentNodePath(NodePath np) {
            if (parentNodePath != null) {
                parentNodePath.childNodePath = null;
            }
            if (np != null) {
                np.childNodePath = this;
            }
            parentNodePath = np;
        }

        void setChildNodePath(NodePath np) {
            if (childNodePath != null) {
                childNodePath.parentNodePath = null;
            }
            if (np != null) {
                np.parentNodePath = this;
            }
            childNodePath = np;
        }

        void computeSignature() {
            StringBuffer sb = new StringBuffer();
            sb.append(nodeName);
            sb.append('|');
            sb.append(tagIndex);
            sb.append('|');
            sb.append(nodeIndex);
            sb.append('|');
            if (parentNodePath != null) sb.append(parentNodePath.signature);
            signature = getSignature(sb.toString());
        }

        NodePath getRootNodePath() {
            NodePath np = this;
            while (np.parentNodePath != null) np = np.parentNodePath;
            return np;
        }

        public boolean equals(NodePath np) {
            return signature.equals(np.signature);
        }

        String generateXPath() {
            StringBuffer sb = new StringBuffer();
            NodePath np = getRootNodePath();
            while (np != null) {
                sb.append('/');
                if (np.isTextNode) sb.append("text()"); else sb.append(np.nodeName);
                sb.append('[');
                sb.append(np.tagIndex);
                sb.append(']');
                np = np.childNodePath;
            }
            return sb.toString();
        }
    }
}
