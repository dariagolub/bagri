package com.bagri.xdm.client.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import javax.xml.transform.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.xdm.api.XDMModelManagement;
import com.bagri.xdm.domain.XDMData;
import com.bagri.xdm.domain.XDMElement;
import com.bagri.xdm.domain.XDMNodeKind;
import com.bagri.xdm.domain.XDMPath;

public class XDMStaxParser {

	private static final Logger logger = LoggerFactory.getLogger(XDMStaxParser.class);

	private static XMLInputFactory factory = XMLInputFactory.newInstance();

	private StringBuilder chars;
	private List<XMLEvent> firstEvents;
	private List<XDMData> dataList;
	private Stack<XDMData> dataStack;
	private XDMModelManagement dict;
	private int docType = -1;
	private long elementId;

	public static List<XDMData> parseDocument(XDMModelManagement dictionary, String xml) throws IOException, XMLStreamException {
		XDMStaxParser parser = new XDMStaxParser(dictionary);
		return parser.parse(xml);
	}
	
	public XDMStaxParser(XDMModelManagement dict) {
		this.dict = dict;
	}

	public List<XDMData> parse(String xml) throws IOException, XMLStreamException {
		try (Reader reader = new StringReader(xml)) {
			return parse(reader);
		}
	}
	
	public List<XDMData> parse(File file) throws IOException, XMLStreamException {
		try (Reader reader = new FileReader(file)) {
			return parse(reader);
		}
	}
	
	public List<XDMData> parse(InputStream stream) throws IOException, XMLStreamException {
		
		XMLEventReader eventReader = null;
		try {
			eventReader = factory.createXMLEventReader(stream); 
			return parse(eventReader);
		} finally {
			if (eventReader != null) {
				eventReader.close();
			}
		}
	}
	
	public List<XDMData> parse(Reader reader) throws IOException, XMLStreamException {
		
		XMLEventReader eventReader = null;
		try {
			eventReader = factory.createXMLEventReader(reader); 
			return parse(eventReader);
		} finally {
			if (eventReader != null) {
				eventReader.close();
			}
		}
	}
	
	public List<XDMData> parse(Source source) throws IOException, XMLStreamException {
		
		XMLEventReader eventReader = null;
		try {
			eventReader = factory.createXMLEventReader(source); 
			return parse(eventReader);
		} finally {
			if (eventReader != null) {
				eventReader.close();
			}
		}
	}
	
	public List<XDMData> parse(XMLEventReader eventReader) throws IOException, XMLStreamException {
		
		init();
		while (eventReader.hasNext()) {
			processEvent(eventReader.nextEvent());
		}

		List<XDMData> result = dataList;
		dataList = null;
		return result;
	}
	
	private void processEvent(XMLEvent xmlEvent) {
		
		//logger.trace("event: {}; type: {}; docType: {}", xmlEvent, xmlEvent.getEventType(), docType);
		if (docType < 0) {
			firstEvents.add(xmlEvent);
			if (xmlEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
				String root = "/" + xmlEvent.asStartElement().getName();
				docType = dict.translateDocumentType(root);
				for (XMLEvent event: firstEvents) {
					processEvent(event);
				}
			}
		} else {
			switch (xmlEvent.getEventType()) {
				case XMLStreamConstants.START_DOCUMENT:
					processDocument((StartDocument) xmlEvent);
					break;
				case XMLStreamConstants.START_ELEMENT:
					processStartElement(xmlEvent.asStartElement());
					//eventReader.nextTag();
					break;
				case XMLStreamConstants.CHARACTERS:
					processCharacters(xmlEvent.asCharacters());
					break;
				case XMLStreamConstants.END_ELEMENT:
					processEndElement(xmlEvent.asEndElement());
					//eventReader.nextTag();
					break;
				case XMLStreamConstants.END_DOCUMENT:
					cleanup();
					break;
				case XMLStreamConstants.ATTRIBUTE:
					processAttribute((Attribute) xmlEvent);
					break;
				case XMLStreamConstants.COMMENT:
					processComment((Comment) xmlEvent);
					break;
				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					processPI((ProcessingInstruction) xmlEvent);
					break;
			}
		}
	}

	private void cleanup() {
		chars = null;
		firstEvents = null;
		dataStack = null;
	}

	private void init() {
		firstEvents = new ArrayList<XMLEvent>();
		dataList = new ArrayList<XDMData>();
		dataStack = new Stack<XDMData>();
		docType = -1;
		elementId = 0;
		chars = new StringBuilder();
	}
	
	private void processDocument(StartDocument document) {

		//logger.trace("document: {}", document);
		XDMElement start = new XDMElement();
		start.setElementId(elementId++);
		//start.setParentId(0); // -1 ?
		XDMPath path = dict.translatePath(docType, "", XDMNodeKind.document);
		XDMData data = new XDMData(path, start);
		dataStack.add(data);
		dataList.add(data);
	}

	@SuppressWarnings("unchecked")
	private void processStartElement(StartElement element) {
		
		XDMData parent = dataStack.peek();
		XDMData current = addData(parent, XDMNodeKind.element, "/" + element.getName(), null); 
		dataStack.add(current);

		for (Iterator<Namespace> itr = element.getNamespaces(); itr.hasNext();) {
			Namespace ns = itr.next();
			// TODO: process default namespace properly
			String prefix = dict.translateNamespace(ns.getValue(), ns.getName().getLocalPart());
			addData(current, XDMNodeKind.namespace, "/#" + prefix, ns.getValue()); 
		}

		for (Iterator<Attribute> itr = element.getAttributes(); itr.hasNext();) {
			Attribute a = itr.next();
			// TODO: process additional (not registered yet) namespaces properly
			addData(current, XDMNodeKind.attribute, "/@" + a.getName(), a.getValue()); //.trim());
		}
	}

	private void processEndElement(EndElement element) {

		XDMData current = dataStack.pop();
		if (chars.length() > 0) {
			String content = chars.toString();
			// normalize xml content.. what if it is already normalized??
			content = content.replaceAll("&", "&amp;");
			// trim left/right ? this is schema-dependent. trim if schema-type 
			// is xs:token, for instance..
			XDMData text = addData(current, XDMNodeKind.text, "/text()", content); 
			chars.delete(0, chars.length());
			//logger.trace("text: {}", text);
		}
	}

	private void processCharacters(Characters characters) {

		if (characters.getData().trim().length() > 0) {
			chars.append(characters.getData());
		}
	}

	private void processComment(Comment comment) {

		//logger.trace("comment: {}", comment);
		addData(dataStack.peek(), XDMNodeKind.comment, "/comment()", comment.getText());
	}

	private void processAttribute(Attribute attribute) {
		// ...
		logger.trace("attribute: {}", attribute);
	}

	private void processPI(ProcessingInstruction pi) {

		//For a processing-instruction node: processing-instruction(local)[position] where local is the name 
		//of the processing instruction node and position is an integer representing the position of the selected 
		//node among its like-named processing-instruction node siblings
		
		XDMData piData = addData(dataStack.peek(), XDMNodeKind.pi, "/?" + pi.getTarget(), pi.getData());
		//logger.trace("piData: {}; target: {}", piData, pi.getTarget());
	}

	private XDMData addData(XDMData parent, XDMNodeKind kind, String name, String value) {

		XDMElement xElt = new XDMElement();
		xElt.setElementId(elementId++);
		xElt.setParentId(parent.getElementId());
		String path = parent.getPath() + name;
		xElt.setValue(value);
		XDMPath xPath = dict.translatePath(docType, path, kind);
		XDMData xData = new XDMData(xPath, xElt);
		dataList.add(xData);
		return xData;
	}
	
}
