package com.bagri.xquery.saxon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemAccessor;
import javax.xml.xquery.XQStaticContext;

import net.sf.saxon.expr.instruct.GlobalParameterSet;
import net.sf.saxon.om.StructuredQName;

import com.bagri.xdm.api.XDMDocumentManagement;
import com.bagri.xdm.api.XDMException;
import com.bagri.xdm.api.XDMQueryManagement;
import com.bagri.xqj.BagriXQUtils;
import com.bagri.xquery.api.XQProcessor;

public class XQProcessorClient extends XQProcessorImpl implements XQProcessor {
	
	private Properties collectProperties(XQStaticContext ctx) throws XQException {
		Properties props = contextToProps(ctx);
		for (String name: properties.stringPropertyNames()) {
			if (!props.containsKey(name)) {
				props.setProperty(name, properties.getProperty(name));
			}
		}
		return props;
	}
	
	@Override
	public Iterator executeXCommand(String command, Map<QName, XQItemAccessor> bindings, XQStaticContext ctx) throws XQException {
		
		return executeXCommand(command, bindings, collectProperties(ctx));
	}

	@Override
	public Iterator executeXCommand(String command, Map<QName, XQItemAccessor> bindings, Properties props) throws XQException {
		
    	//logger.trace("executeXCommand.enter; command: {}", command);
    	XDMQueryManagement qMgr = getQueryManagement();
    	try {
    		return qMgr.executeXCommand(command, bindings, props);
    	} catch (XDMException ex) {
    		throw new XQException(ex.getMessage());
    	}
	}

	@Override
	public Iterator executeXQuery(String query, XQStaticContext ctx) throws XQException {

		return executeXQuery(query, collectProperties(ctx));
	}

	@Override
	public Iterator executeXQuery(String query, Properties props) throws XQException {

    	//logger.trace("executeXQuery.enter; query: {}", query);
    	XDMQueryManagement qMgr = getQueryManagement();
    	GlobalParameterSet params = dqc.getParameters();
    	Map bindings = new HashMap<QName, Object>(params.getNumberOfKeys());
    	for (StructuredQName qName: params.getKeys()) {
    		QName vName = new QName(qName.getURI(), qName.getLocalPart(), qName.getPrefix()); 
    		bindings.put(vName, params.get(qName));
    	}
    	//logger.trace("executeXQuery; bindings: {}", bindings);
    	
    	try {
    		return qMgr.executeXQuery(query, bindings, props);
    	} catch (XDMException ex) {
    		logger.error("executeXQuery.error;", ex);
    		throw new XQException(ex.getMessage());
    	}
	}

	@Override
	public Iterator getResults() {
		return null;
	}

	@Override
	public void setResults(Iterator itr) {
		// no-op
	}

	//@Override
	//public Collection<QName> prepareXQuery(String query, XQStaticContext ctx) throws XQException {
	//	return null;
	//}

}
