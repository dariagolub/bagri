package com.bagri.server.hazelcast.task.doc;

import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.core.api.DocumentManagement;
import com.bagri.core.server.api.SchemaRepository;
import com.bagri.core.system.Permission;
import com.bagri.server.hazelcast.impl.SchemaRepositoryImpl;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class DocumentBeanProvider extends com.bagri.client.hazelcast.task.doc.DocumentBeanProvider {

	private transient DocumentManagement docMgr;
    
    @Autowired
	public void setRepository(SchemaRepository repo) {
		this.repo = repo;
		this.docMgr = repo.getDocumentManagement();
	}

    @Override
	public Object call() throws Exception {
    	
    	((SchemaRepositoryImpl) repo).getXQProcessor(clientId);
    	checkPermission(Permission.Value.read);
    	
		return docMgr.getDocumentAsBean(uri, props);
	}
}