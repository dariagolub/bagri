package com.bagri.client.hazelcast.task.doc;

import static com.bagri.client.hazelcast.serialize.TaskSerializationFactory.cli_CreateDocumentTask;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.bagri.core.api.DocumentAccessor;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;


public class DocumentCreator extends DocumentAwareTask implements Callable<DocumentAccessor> {

	protected Object content;

	public DocumentCreator() {
		super();
	}

	public DocumentCreator(String clientId, long txId, String uri, Properties props, Object content) {
		super(clientId, txId, uri, props);
		this.content = content;
	}

	@Override
	public DocumentAccessor call() throws Exception {
		return null; 
	}

	@Override
	public int getId() {
		return cli_CreateDocumentTask;
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		super.readData(in);
		content = in.readObject();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		super.writeData(out);
		out.writeObject(content);
	}

}
