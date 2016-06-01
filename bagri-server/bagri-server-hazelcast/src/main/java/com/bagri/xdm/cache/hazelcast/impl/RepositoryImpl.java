package com.bagri.xdm.cache.hazelcast.impl;

import static com.bagri.common.config.XDMConfigConstants.xdm_schema_format_default;
import static com.bagri.xdm.cache.hazelcast.util.HazelcastUtils.hz_instance;
import static com.hazelcast.core.Hazelcast.getHazelcastInstanceByName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bagri.xdm.api.XDMBindingManagement;
import com.bagri.xdm.api.XDMException;
import com.bagri.xdm.api.XDMModelManagement;
import com.bagri.xdm.api.XDMTransactionManagement;
import com.bagri.xdm.api.impl.RepositoryBase;
import com.bagri.xdm.cache.api.XDMClientManagement;
import com.bagri.xdm.cache.api.XDMIndexManagement;
import com.bagri.xdm.cache.api.XDMRepository;
import com.bagri.xdm.cache.api.XDMTriggerManagement;
import com.bagri.xdm.common.XDMBuilder;
import com.bagri.xdm.common.XDMKeyFactory;
import com.bagri.xdm.common.XDMParser;
import com.bagri.xdm.common.df.xml.XmlBuilder;
import com.bagri.xdm.common.df.xml.XmlStaxParser;
import com.bagri.xdm.domain.XDMPath;
import com.bagri.xdm.system.XDMDataFormat;
import com.bagri.xdm.system.XDMIndex;
import com.bagri.xdm.system.XDMLibrary;
import com.bagri.xdm.system.XDMModule;
import com.bagri.xdm.system.XDMSchema;
import com.bagri.xdm.system.XDMTriggerDef;
import com.bagri.xquery.api.XQProcessor;
import com.hazelcast.core.HazelcastInstance;

public class RepositoryImpl extends RepositoryBase implements ApplicationContextAware, XDMRepository {

	private static final transient Logger logger = LoggerFactory.getLogger(RepositoryImpl.class);
	
	private ThreadLocal<String> thClient = new ThreadLocal<String>() {
		
		@Override
		protected String initialValue() {
			return null;
 		}
	};
	
	private XDMKeyFactory xdmFactory; 
	//private String instanceNum;
	private XDMSchema xdmSchema;
	private Map<String, XDMDataFormat> xdmFormats;
	private Collection<XDMModule> xdmModules;
	private Collection<XDMLibrary> xdmLibraries;
    private XDMClientManagement clientMgr;
    private XDMIndexManagement indexMgr;
    private XDMTriggerManagement triggerMgr;
    private ApplicationContext appContext;
    private HazelcastInstance hzInstance;
	private Map<String, XQProcessor> processors = new ConcurrentHashMap<String, XQProcessor>();
	
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.appContext = context;
	}

	public HazelcastInstance getHzInstance() {
		return hzInstance;
	}
	
    //@Autowired
	public void setHzInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
		logger.debug("setHzInstange; got instance: {}", hzInstance.getName());
	}
	
	@Override
	public void setBindingManagement(XDMBindingManagement bindMgr) {
		super.setBindingManagement(bindMgr);
		((BindingManagementImpl) bindMgr).setRepository(this);
	}

	@Override
	public XDMClientManagement getClientManagement() {
		return clientMgr;
	}
	
	public void setClientManagement(XDMClientManagement clientMgr) {
		this.clientMgr = clientMgr;
		((ClientManagementImpl) clientMgr).setRepository(this);
	}
	
	@Override
	public XDMIndexManagement getIndexManagement() {
		return indexMgr;
	}

	public void setIndexManagement(XDMIndexManagement indexMgr) {
		this.indexMgr = indexMgr;
		((IndexManagementImpl) indexMgr).setRepository(this);
	}
	
	@Override
	public void setTxManagement(XDMTransactionManagement txMgr) {
		super.setTxManagement(txMgr);
		((TransactionManagementImpl) txMgr).setRepository(this);
	}

	@Override
	public XDMTriggerManagement getTriggerManagement() {
		return triggerMgr;
	}

	public void setTriggerManagement(XDMTriggerManagement triggerMgr) {
		this.triggerMgr = triggerMgr;
		((TriggerManagementImpl) triggerMgr).setRepository(this);
	}
	
	@Override
	public String getClientId() {
		return thClient.get();
	}
	
	@Override
	public String getUserName() {
		String user = clientMgr.getCurrentUser();
		//logger.info("getUserName; user: {}", user); 
		if (user == null) {
			user = "unknown";
		}
		return user;
	}
	
	@Override
	public XDMSchema getSchema() {
		return xdmSchema;
	}
	
	public void setSchema(XDMSchema xdmSchema) {
		// TODO: think about run-time updates..
		this.xdmSchema = xdmSchema;
		afterInit();
	}
	
	XQProcessor getXQProcessor() {
		String clientId = thClient.get();
		return getXQProcessor(clientId);
	}
	
	public XQProcessor getXQProcessor(String clientId) {
		XQProcessor result;
		if (clientId == null) {
			result = newXQProcessor();
		} else {
			thClient.set(clientId);
			result = processors.get(clientId);
			if (result == null) {
				result = newXQProcessor();
				processors.put(clientId, result);
			}
		}
		logger.trace("getXQProcessor; returning: {}, factory: {}, repo: {}", 
				result, result.getXQDataFactory(), result.getRepository());
		return result;
	}
	
	private XQProcessor newXQProcessor() {
		XQProcessor result = appContext.getBean(XQProcessor.class, this);
		//XDMQueryManagement qMgr = getQueryManagement();
		//result.setRepository(this);
		return result;
	}
	
	public XDMKeyFactory getFactory() {
		return xdmFactory;
	}
	
	public void setFactory(XDMKeyFactory factory) {
		this.xdmFactory = factory;
	}
	
	@Override
	public XDMParser getParser(String dataFormat) {
		XDMDataFormat df = getDataFormat(dataFormat);
		if (df != null) {
			return instantiateClass(df.getParserClass());
		}
		logger.warn("getParser; no parser found for dataFormat: {}", dataFormat); 
		return new XmlStaxParser(getModelManagement());
	}
	
	@Override
	public XDMBuilder getBuilder(String dataFormat) {
		XDMDataFormat df = getDataFormat(dataFormat);
		if (df != null) {
			return instantiateClass(df.getBuilderClass());
		}
		logger.warn("getBuilder; no builder found for dataFormat: {}", dataFormat); 
		return new XmlBuilder(getModelManagement());
	}
	
	private <T> T instantiateClass(String className) {
		
		try {
			Class clazz = Class.forName(className);
			return (T) clazz.getConstructor(XDMModelManagement.class).newInstance(getModelManagement());
		} catch (Exception ex) {
			logger.error("instantiateClass; cannot instantiate: " + className, ex);
			// throw ex?
		}
		return null;
	}
	
	@Override
	public void close() {
		// TODO: disconnect all clients ?
	}
	
	public XDMDataFormat getDataFormat(String dataFormat) {

		Map<String, XDMDataFormat> formats = xdmFormats;
		if (formats == null) {
			HazelcastInstance dataInstance = getHazelcastInstanceByName(hz_instance);
			if (dataInstance != null) {
				formats = dataInstance.getMap("formats");
			}
		}
		if (formats != null) {
			// find by name
			XDMDataFormat df = formats.get(dataFormat);
			if (df != null) {
				return df;
			} else {
				// find by extension
				for (XDMDataFormat df2: formats.values()) {
					if (df2.getExtensions().contains(dataFormat)) {
						return df2;
					}
				}
			}
			
			// still not found -> get schema default format
			dataFormat = this.xdmSchema.getProperty(xdm_schema_format_default);
			return formats.get(dataFormat);
		}
		return null;
	}

	public void setDataFormats(Collection<XDMDataFormat> cFormats) {
		if (cFormats != null) {
			xdmFormats = new HashMap<>(cFormats.size());
			for (XDMDataFormat df: cFormats) {
				xdmFormats.put(df.getName(), df);
			}
		}
	}
	
	public Collection<XDMLibrary> getLibraries() {
		if (xdmLibraries != null) {
			return xdmLibraries;
		}
		
		HazelcastInstance dataInstance = getHazelcastInstanceByName(hz_instance);
		if (dataInstance != null) {
			Map<String, XDMLibrary> libraries = dataInstance.getMap("libraries");
			return libraries.values();
		}
		return Collections.emptyList(); 
	}

	public void setLibraries(Collection<XDMLibrary> cLibraries) {
		if (cLibraries != null) {
			xdmLibraries = new ArrayList<>(cLibraries);
		}
	}
	
	public Collection<XDMModule> getModules() {
		if (xdmModules != null) {
			return xdmModules;
		}
		
		HazelcastInstance dataInstance = getHazelcastInstanceByName(hz_instance);
		if (dataInstance != null) {
			Map<String, XDMModule> modules = dataInstance.getMap("modules");
			return modules.values();
		}
		return Collections.emptyList(); 
	}
	
	public void setModules(Collection<XDMModule> cModules) {
		if (cModules != null) {
			xdmModules = new ArrayList<>(cModules);
		}
	}
	
	public void afterInit() {
		Set<XDMIndex> indexes = xdmSchema.getIndexes();
		if (indexes.size() > 0) {
			for (XDMIndex idx: indexes) {
				try {
					indexMgr.createIndex(idx);
				} catch (XDMException ex) {
					logger.warn("afterInit.error; index: " + idx, ex);
				}
			}
		}
		
		// now init triggers..
		Set<XDMTriggerDef> triggers = xdmSchema.getTriggers();
		if (triggers.size() > 0) {
			for (XDMTriggerDef trg: triggers) {
				triggerMgr.createTrigger(trg);
			}
		}
	}
	
	public boolean addSchemaIndex(XDMIndex index) {
		
		if (xdmSchema.addIndex(index)) {
			XDMPath[] paths;
			try {
				paths = indexMgr.createIndex(index);
			} catch (XDMException ex) {
				logger.warn("addSchemaIndex.error; index: " + index, ex);
				return false;
			}
			
			DocumentManagementImpl docMgr = (DocumentManagementImpl) getDocumentManagement();
			for (XDMPath xPath: paths) {
				try {
					docMgr.indexElements(xPath.getTypeId(), xPath.getPathId());
				} catch (XDMException ex) {
					logger.warn("addSchemaIndex.error; index: " + index, ex);
				}
			}
			return paths.length > 0;
		}
		logger.info("addSchemaIndex; index {} already exists! do we need to index values?", index);
		return false;
	}

	public boolean dropSchemaIndex(String name) {
		
		XDMIndex index = xdmSchema.removeIndex(name);
		if (index != null) {
			XDMPath[] paths;
			try {
				paths = indexMgr.dropIndex(index);
			} catch (XDMException ex) {
				logger.warn("addSchemaIndex.error; index: " + index, ex);
				return false;
			}
			
			DocumentManagementImpl docMgr = (DocumentManagementImpl) getDocumentManagement();
			int cnt = 0;
			List<Integer> pathIds = new ArrayList<>(paths.length);
			for (XDMPath xPath: paths) {
				pathIds.add(xPath.getPathId());
				cnt += docMgr.deindexElements(xPath.getTypeId(), xPath.getPathId());
			}

			QueryManagementImpl queryMgr = (QueryManagementImpl) getQueryManagement();
			Set<Integer> qKeys = queryMgr.getQueriesForPaths(pathIds, true);
			if (!qKeys.isEmpty()) {
				queryMgr.removeQueries(qKeys);
			}
			
			return cnt > 0;
		}
		logger.info("dropSchemaIndex; index {} does not exist?", index);
		return false;
	}
	
	public boolean addSchemaTrigger(XDMTriggerDef trigger) {
		
		if (xdmSchema.addTrigger(trigger)) {
			return triggerMgr.createTrigger(trigger);
		}
		return false;
	}

	public boolean dropSchemaTrigger(String className) {
		
		XDMTriggerDef trigger = xdmSchema.removeTrigger(className);
		if (trigger != null) {
			return triggerMgr.deleteTrigger(trigger);
		}
		return false;
	}

	
}

