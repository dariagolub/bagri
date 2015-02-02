package com.bagri.xdm.cache.hazelcast.task.node;

import static com.bagri.xdm.cache.hazelcast.util.HazelcastUtils.hz_instance;
import static com.bagri.xdm.client.hazelcast.serialize.XDMDataSerializationFactory.cli_KillNodeTask;
import static com.bagri.xdm.client.hazelcast.serialize.XDMDataSerializationFactory.factoryId;

import java.io.IOException;
import java.util.Set;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class NodeKiller implements Runnable, IdentifiedDataSerializable {
	
	@Override
	public void run() {
		System.exit(0);
		HazelcastInstance hzSystem = Hazelcast.getHazelcastInstanceByName(hz_instance);
		Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();
		for (HazelcastInstance instance: instances) {
			if (!hz_instance.equals(instance.getName())) {
				instance.shutdown();
			}
		}
		hzSystem.shutdown();
	}

	@Override
	public int getFactoryId() {
		return factoryId;
	}

	@Override
	public int getId() {
		return cli_KillNodeTask;
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
	}

}