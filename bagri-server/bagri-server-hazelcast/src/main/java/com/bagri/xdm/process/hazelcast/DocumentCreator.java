package com.bagri.xdm.process.hazelcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.xdm.access.api.XDMDocumentManagement;
import com.bagri.xdm.domain.XDMDocument;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentCreator extends com.bagri.xdm.access.hazelcast.process.DocumentCreator {

	private static final transient Logger logger = LoggerFactory.getLogger(DocumentCreator.class);
    
	private XDMDocumentManagement xdmProxy;
    
    @Autowired
	public void setXdmProxy(XDMDocumentManagement xdmProxy) {
		this.xdmProxy = xdmProxy;
		logger.trace("setXdmProxy; got proxy: {}", xdmProxy); 
	}
    
	@Override
	public XDMDocument call() throws Exception {
		logger.trace("call.enter; xdm: {} ", xdmProxy);

		XDMDocument doc = xdmProxy.storeDocument(docId, uri, xml);
		
		logger.trace("process.exit; returning: {}", doc);
		return doc;
	}
	
}