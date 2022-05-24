package utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Gabriela Melingerová
 */
public class XMLUtils {
    
    /**
     * Parse the document from the input stream
     * @param is InputStream
     * @return DOM
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document parseDocument(String filePath) throws ParserConfigurationException, SAXException, IOException {
        File file = new File(filePath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(file);
        return document;
    }
    
    /**
     * Finds the elements in the DOM tree
     * @param topElm Top element
     * @param nodeName Node name
     * @return returns the found nodes
     */
    public static List<Element> findElements(Element topElm, String nodeName) {
        List<Element> elements = new ArrayList<Element>();
        if (topElm == null) throw new IllegalArgumentException("topElm cannot be null");
        synchronized(topElm.getOwnerDocument()) {
            Stack<Element> stack = new Stack<Element>();
            stack.push(topElm);
            while (!stack.isEmpty()) {
                Element curElm = stack.pop();
                if (curElm.getNodeName().equals(nodeName)) {
                    elements.add(curElm);
                    continue;
                }
                List<Node> nodesToProcess = new ArrayList<Node>();

                NodeList childNodes = curElm.getChildNodes();
                for (int i = 0, ll = childNodes.getLength(); i < ll; i++) {
                    Node item = childNodes.item(i);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        nodesToProcess.add(item);
                    }
                }
                Collections.reverse(nodesToProcess);
                for (Node node : nodesToProcess) {
                    stack.push((Element) node);
                }
            }
            return elements;
        }
    }
    
    /**
     * Finds the elements in the DOM tree
     * @param topElm Top element
     * @param nodeName Node name
     * @param attributeName Attribute name
     * @param attributeValue Attribute value
     * @return returns the found nodes
     */
    public static List<Element> findElements(Element topElm, String nodeName, String attributeName, String attributeValue) {
        List<Element> elements = new ArrayList<Element>();
        if (topElm == null) throw new IllegalArgumentException("topElm cannot be null");
        synchronized(topElm.getOwnerDocument()) {
            Stack<Element> stack = new Stack<Element>();
            stack.push(topElm);
            while (!stack.isEmpty()) {
                Element curElm = stack.pop();
                if (curElm.getNodeName().equals(nodeName) && curElm.getAttribute(attributeName) != null && curElm.getAttribute(attributeName).startsWith(attributeValue)) {
                    elements.add(curElm);
                    continue;
                }
                List<Node> nodesToProcess = new ArrayList<Node>();

                NodeList childNodes = curElm.getChildNodes();
                for (int i = 0, ll = childNodes.getLength(); i < ll; i++) {
                    Node item = childNodes.item(i);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        nodesToProcess.add(item);
                    }
                }
                Collections.reverse(nodesToProcess);
                for (Node node : nodesToProcess) {
                    stack.push((Element) node);
                }
            }
            return elements;
        }
    }
    
    /**
     * Finds the element in the DOM tree
     * @param topElm Top element
     * @param nodeName Node name
     * @return returns the found node
     */
    public static Element findElement(Element topElm, String nodeName) {
        List<Element> elements = new ArrayList<Element>();
        if (topElm == null) throw new IllegalArgumentException("topElm cannot be null");
        synchronized(topElm.getOwnerDocument()) {
            Stack<Element> stack = new Stack<Element>();
            stack.push(topElm);
            while (!stack.isEmpty()) {
                Element curElm = stack.pop();
                if (curElm.getNodeName().equals(nodeName)) {
                    elements.add(curElm);
                    return curElm;
                }
                List<Node> nodesToProcess = new ArrayList<Node>();

                NodeList childNodes = curElm.getChildNodes();
                for (int i = 0, ll = childNodes.getLength(); i < ll; i++) {
                    Node item = childNodes.item(i);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        nodesToProcess.add(item);
                    }
                }
                Collections.reverse(nodesToProcess);
                for (Node node : nodesToProcess) {
                    stack.push((Element) node);
                }
            }
            return null;
        }
    }
    
    public static void saveDocument(Document doc, String path) throws TransformerConfigurationException, TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        
        DOMSource domSource = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(path));
        transformer.transform(domSource, result);  
    }
    
}
