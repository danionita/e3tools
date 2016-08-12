package design.export;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxObjectCodec;
import com.mxgraph.util.mxXmlUtils;
import com.thoughtworks.xstream.XStream;

// From: https://github.com/jgraph/jgraphx/issues/19

public class ObjectXStreamCodec extends mxObjectCodec {
    private XStream m_xstream = new XStream(); // XStreamFactory.newInstance();
    private Object m_dummyTemplate = new Object();

    public ObjectXStreamCodec() {
        super(null);
    }

    @Override
    public String getName() {
        return "XStream";
    }

    @Override
    public Object getTemplate() {
        return m_dummyTemplate;
    }

    @Override
    protected Object cloneTemplate(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = item.getNodeName();
                Class realClass = m_xstream.getMapper().realClass(nodeName);
                Object template = m_xstream.getReflectionProvider().newInstance(realClass);
                return template;
            }
        }
        throw new IllegalStateException("Malformed XML in mxObjectXStreamCodec");
    }

    @Override
    protected void encodeObject(mxCodec enc, Object obj, Node node) {
        String xml = m_xstream.toXML(obj);
        Document parseXml = mxXmlUtils.parseXml(xml);
        Element xStreamElement = parseXml.getDocumentElement();
        Node importedNode = enc.getDocument().importNode(xStreamElement, true);
        node.appendChild(importedNode);
    }

    @Override
    protected void decodeNode(mxCodec dec, Node node, Object obj) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                String xml = mxXmlUtils.getXml(item);
                m_xstream.fromXML(xml, obj);
                return;
            }
        }
        throw new IllegalStateException("Malformed XML in mxObjectXStreamCodec");

    }
}
