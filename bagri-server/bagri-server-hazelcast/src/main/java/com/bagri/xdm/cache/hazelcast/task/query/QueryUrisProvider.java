package com.bagri.xdm.cache.hazelcast.task.query;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.xdm.api.XDMQueryManagement;
import com.bagri.xdm.cache.hazelcast.impl.RepositoryImpl;
import com.bagri.xdm.system.Permission;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class QueryUrisProvider extends com.bagri.xdm.client.hazelcast.task.query.QueryUrisProvider {

	private transient XDMQueryManagement queryMgr;
    
    @Autowired
	public void setQueryManager(XDMQueryManagement queryMgr) {
		this.queryMgr = queryMgr;
		//logger.debug("setQueryManager; got QueryManager: {}", queryMgr); 
	}
    
    @Autowired
	public void setRepository(RepositoryImpl repo) {
		this.repo = repo;
	}

    @Override
	public Collection<String> call() throws Exception {
    	((RepositoryImpl) repo).getXQProcessor(clientId);
    	checkPermission(Permission.Value.read);
       	return queryMgr.getDocumentUris(query, params, context);
	}

}
