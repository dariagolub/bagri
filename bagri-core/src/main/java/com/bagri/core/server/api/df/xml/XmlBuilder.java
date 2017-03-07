package com.bagri.core.server.api.df.xml;

import static com.bagri.support.util.FileUtils.EOL;
import static com.bagri.support.util.FileUtils.def_encoding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import com.bagri.core.DataKey;
import com.bagri.core.api.BagriException;
import com.bagri.core.model.Data;
import com.bagri.core.model.Element;
import com.bagri.core.model.Elements;
import com.bagri.core.model.NodeKind;
import com.bagri.core.model.Path;
import com.bagri.core.server.api.ContentBuilder;
import com.bagri.core.server.api.ModelManagement;
import com.bagri.core.server.api.impl.ContentBuilderBase;

/**
 * XDM Builder implementation for XML format. 
 * 
 * @author Denis Sukhoroslov
 *
 */
public class XmlBuilder extends ContentBuilderBase implements ContentBuilder {
	
	private boolean pretty = false;
	private int ident = 4;

	/**
	 * 
	 * @param model the XDM model management component
	 */
	public XmlBuilder(ModelManagement model) {
		super(model);
	}
	
 	/**
  	 * {@inheritDoc}
  	 */
 	public void init(Properties properties) {
 		logger.trace("init; got properties: {}", properties);
 		String value = properties.getProperty("xml.pretty.print", "false");
 		pretty = Boolean.valueOf(value);
 		value = properties.getProperty("xml.ident.size", "4");
 		ident = Integer.parseInt(value);
 	}
 
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String buildString(Map<DataKey, Elements> elements) throws BagriException {
    	Collection<Data> dataList = buildDataList(elements);
    	return buildString(dataList);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
   	public String buildString(Collection<Data> elements) throws BagriException {
    	
    	StringBuffer buff = new StringBuffer();
    	Stack<Data> dataStack = new Stack<Data>();
    	boolean eltOpen = false;

    	String prefix = "";
    	for (int i=0; i < ident; i++) {
    		prefix += " ";
    	}
    	
    	for (Data data: elements) {
    		eltOpen = writeElement(data, buff, dataStack, eltOpen, prefix);
    	}

    	boolean writeIdent = false;
		while (dataStack.size() > 0) {
			Data top = dataStack.pop();
			if (eltOpen) {
				buff.append("/>");
				eltOpen = false;
			} else {
				if (writeIdent) {
					for (int i=1; i < top.getLevel(); i++) {
						buff.append(prefix);
					}
				}
				buff.append("</").append(top.getName()).append(">");
			}
			if (pretty) {
				buff.append(EOL);
				writeIdent = true;
			}
		}
    	return buff.toString();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream buildStream(Map<DataKey, Elements> elements) throws BagriException {
    	Collection<Data> dataList = buildDataList(elements);
    	return buildStream(dataList);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream buildStream(Collection<Data> elements) throws BagriException {
		String content = buildString(elements);
		if (content != null) {
			try {
				return new ByteArrayInputStream(content.getBytes(def_encoding));
			} catch (UnsupportedEncodingException ex) {
				throw new BagriException(ex, BagriException.ecInOut);
			}
		}
		return null;
	}
	
	private boolean writeElement(Data data, StringBuffer buff, Stack<Data> dataStack, boolean eltOpen, String prefix) {
		switch (data.getNodeKind()) {
			case document: { // this must be the first row..
				buff.append("<?xml version=\"1.0\"?>"); // what about: encoding="utf-8"?>
				if (pretty) {
					buff.append(EOL);
				}
				break;
			}
			case namespace: { 
				buff.append(" ").append("xmlns");
				String name = data.getName();
				if (name != null && name.trim().length() > 0) {
					buff.append(":").append(name);
				}
				buff.append("=\"").append(data.getValue()).append("\"");
				break;
			}
			case element: {
				eltOpen = endElement(dataStack, data, buff, eltOpen, prefix);
				dataStack.add(data);
				if (pretty) {
					for (int i=1; i < data.getLevel(); i++) {
						buff.append(prefix);
					}
				}
   				buff.append("<").append(data.getName()); 
   				eltOpen = true;
   				break;
			}
			case attribute: { 
				if (!dataStack.isEmpty()) {
					buff.append(" ").append(data.getName()).append("=\"").append(data.getValue()).append("\"");
				} else {
					buff.append(data.getValue());
				}
				break;
			}
			case comment: { 
				eltOpen = endElement(dataStack, data, buff, eltOpen, prefix);
	    		// add idents..
				buff.append("<!--").append(data.getValue()).append("-->"); 
				if (dataStack.isEmpty() && pretty) {
					buff.append(EOL);
				}
				break;
			}
			case pi: { 
				eltOpen = endElement(dataStack, data, buff, eltOpen, prefix);
	    		// add idents..
				buff.append("<?").append(data.getName()).append(" ");
				buff.append(data.getValue()).append("?>"); 
				if (dataStack.isEmpty() && pretty) {
					buff.append(EOL);
				}
				break;
			}
			case text: {
				eltOpen = endElement(dataStack, data, buff, eltOpen, prefix);
				buff.append(data.getValue());
				break;
			}
			default: {
				//logger.warn("buildXml; unknown NodeKind: {}", data.getNodeKind());
			}
		}
		return eltOpen;
	}
	
	private boolean endElement(Stack<Data> dataStack, Data data, StringBuffer buff, boolean eltOpen, String prefix) {
    	
		if (dataStack.isEmpty()) {
			//
		} else {
			Data top = dataStack.peek();
			if (data.getLevel() == top.getLevel() + 1 && data.getParentPos() == top.getPos()) {
				// new child element
				if (eltOpen) {
					buff.append(">");
					eltOpen = false;
				}
				if (data.getNodeKind() != NodeKind.text && pretty) {
					buff.append(EOL);
				}
			} else {
				boolean writeIdent = false;
				while (top != null && (data.getParentPos() != top.getPos() || data.getLevel() != top.getLevel() + 1)) {
					if (eltOpen) {
						buff.append("/>");
						eltOpen = false;
					} else {
						if (writeIdent) {
							for (int i=1; i < top.getLevel(); i++) {
								buff.append(prefix);
							}
						}
						buff.append("</").append(top.getName()).append(">");
					}
					if (pretty) {
						buff.append(EOL);
    					writeIdent = true;
					}
    				dataStack.pop();
    				if (dataStack.isEmpty()) {
    					top = null;
    				} else {
    					top = dataStack.peek();
    				}
				}
			}
		}
		return eltOpen;
    }
    
}
