package com.bagri.server.hazelcast.task.schema;

import static com.bagri.server.hazelcast.serialize.TaskSerializationFactory.cli_factory_id;
import static com.bagri.core.server.api.CacheConstants.CN_XDM_DOCUMENT;
import static com.bagri.core.server.api.CacheConstants.CN_XDM_ELEMENT;
import static com.bagri.server.hazelcast.serialize.TaskSerializationFactory.cli_AggregateSchemaInfoTask;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bagri.core.DataKey;
import com.bagri.core.model.Document;
import com.bagri.core.model.Element;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spring.context.SpringAware;

@SpringAware
public class SchemaStatsAggregator implements Callable<Long>, IdentifiedDataSerializable { 
	
	private static final transient Logger logger = LoggerFactory.getLogger(SchemaStatsAggregator.class);
    
	private transient HazelcastInstance hzInstance;

	@Override
	public int getId() {
		return cli_AggregateSchemaInfoTask;
	}

	@Override
	public int getFactoryId() {
		return cli_factory_id;
	}

    @Autowired
	public void setHzInstance(HazelcastInstance hzInstance) {
		this.hzInstance = hzInstance;
		logger.trace("setHzInstance; got instance: {}", hzInstance); 
	}
    
	@Override
	public Long call() throws Exception {
		
		IMap<Long, Document> xddCache = hzInstance.getMap(CN_XDM_DOCUMENT);
		IMap<DataKey, Element> xdmCache = hzInstance.getMap(CN_XDM_ELEMENT);
		
    	LocalMapStats stats = xddCache.getLocalMapStats();
    	long size = stats.getBackupEntryMemoryCost() + stats.getOwnedEntryMemoryCost();
    	stats = xdmCache.getLocalMapStats();
    	size += stats.getBackupEntryMemoryCost() + stats.getOwnedEntryMemoryCost();
        return size; 
    }
    
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		// nothing to read
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		// nothing to write
	}

}




