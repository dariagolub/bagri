package com.bagri.server.hazelcast.impl;

import com.bagri.core.api.DocumentAccessor;
import com.bagri.core.api.ResultCollection;
import com.bagri.core.api.ResultCursor;
import com.bagri.core.model.Document;
import com.bagri.core.system.Collection;
import com.bagri.core.system.Library;
import com.bagri.core.system.Module;
import com.bagri.core.system.Schema;
import com.bagri.core.test.BagriManagementTest;
import com.bagri.support.util.JMXUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.bagri.core.Constants.*;
import static com.bagri.core.test.TestUtils.*;
import static org.junit.Assert.*;

public class MapDocumentManagementTest extends BagriManagementTest {

    private static ClassPathXmlApplicationContext context;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		sampleRoot = "../../etc/samples/json/";
		//System.setProperty(pn_log_level, "trace");
		System.setProperty(pn_node_instance, "0");
		System.setProperty("logback.configurationFile", "hz-logging.xml");
		System.setProperty(pn_config_properties_file, "test.properties");
		System.setProperty(pn_config_path, "src/test/resources");
		context = new ClassPathXmlApplicationContext("spring/cache-test-context.xml");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		context.close();
	}

	@Before
	public void setUp() throws Exception {
		xRepo = context.getBean(SchemaRepositoryImpl.class);
		SchemaRepositoryImpl xdmRepo = (SchemaRepositoryImpl) xRepo; 
		Schema schema = xdmRepo.getSchema();
		if (schema == null) {
			Properties props = loadProperties("src/test/resources/test.properties");
			schema = new Schema(1, new java.util.Date(), "test", "test", "test schema", true, props);
			schema.setProperty(pn_schema_format_default, "MAP");
			Collection collection = new Collection(1, new Date(), JMXUtils.getCurrentUser(), 1, "maps", "", "custom", true);
			schema.addCollection(collection);
			xdmRepo.setSchema(schema);
			xdmRepo.setDataFormats(getBasicDataFormats());
			xdmRepo.setLibraries(new ArrayList<Library>());
			xdmRepo.setModules(new ArrayList<Module>());
		}
		// set bdb.document.format to JSON !
		//XQProcessor xqp = xdmRepo.getXQProcessor("test_client");
		//xqp.getProperties().setProperty("bdb.document.format", "JSON");
	}

	@After
	public void tearDown() throws Exception {
		// remove documents here!
		removeDocumentsTest();
	}

	//protected String getFileName(String original) {
	//	return original.substring(0, original.indexOf(".")) + ".json";
	//}
	
	@Override
	protected Properties getDocumentProperties() {
		Properties props = new Properties();
		props.setProperty(pn_document_data_format, "MAP");
		return props;
	}
	
	@Test
	public void createMapDocumentTest() throws Exception {
		long txId = xRepo.getTxManagement().beginTransaction();
		Map<String, Object> m1 = new HashMap<>();
		m1.put("intProp", 1); 
		m1.put("boolProp", Boolean.FALSE);
		m1.put("strProp", "XYZ");
	    Properties props = new Properties();
		props.setProperty(pn_document_collections, "maps");
		props.setProperty(pn_document_data_format, "MAP");
		DocumentAccessor mDoc = xRepo.getDocumentManagement().storeDocument("map_test1", m1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		
		uris.add(mDoc.getUri());
		xRepo.getTxManagement().commitTransaction(txId);
		
		props.setProperty(pn_document_headers, String.valueOf(DocumentAccessor.HDR_CONTENT));
		Map<String, Object> m2 = xRepo.getDocumentManagement().getDocument(mDoc.getUri(), props).getContent();
		assertEquals(m1.get("intProp"), m2.get("intProp"));
		assertEquals(m1.get("boolProp"), m2.get("boolProp"));
		assertEquals(m1.get("strProp"), m2.get("strProp"));
	}
	
	private String qDoc = "declare namespace m=\"http://www.w3.org/2005/xpath-functions/map\";\n" +
			"for $doc in fn:collection('maps')\n" +
			"where m:get($doc, '@intProp') = 2\n" +
			"return fn:serialize($doc, map{'method': 'json'})";
	
	@Test
	public void queryMapDocumentTest() throws Exception {
		long txId = xRepo.getTxManagement().beginTransaction();
		Map<String, Object> m1 = new HashMap<>();
		m1.put("intProp", 1); 
		m1.put("boolProp", Boolean.TRUE);
		m1.put("strProp", "ABC");
	    Properties props = new Properties();
		props.setProperty(pn_document_collections, "maps");
		props.setProperty(pn_document_data_format, "MAP");
		DocumentAccessor mDoc = xRepo.getDocumentManagement().storeDocument("map_test2", m1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());

		m1.clear();
		m1.put("intProp", 2); 
		m1.put("boolProp", Boolean.FALSE);
		m1.put("strProp", "CDE");
		mDoc = xRepo.getDocumentManagement().storeDocument("map_test3", m1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		uris.add(mDoc.getUri());
		
		m1.clear();
		m1.put("intProp", 3); 
		m1.put("boolProp", Boolean.FALSE);
		m1.put("strProp", "EFGH");
		mDoc = xRepo.getDocumentManagement().storeDocument("map_test4", m1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		uris.add(mDoc.getUri());

		xRepo.getTxManagement().commitTransaction(txId);
		
		try (ResultCursor results = query(qDoc, null, null)) {
			assertTrue(results.next());
			Object value = results.getObject();
			assertNotNull(value);
			assertFalse(results.next());
		}
	}
	
	@Test
	public void queryJsonDocumentTest() throws Exception {
		long txId = xRepo.getTxManagement().beginTransaction();
		String s1 = "{\"boolProp\":true,\"intProp\":1,\"strProp\":\"ABC\"}";
	    Properties props = new Properties();
		props.setProperty(pn_document_collections, "maps");
		props.setProperty(pn_document_data_format, "JSON");
		DocumentAccessor mDoc = xRepo.getDocumentManagement().storeDocument("map_test2", s1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		uris.add(mDoc.getUri());

		s1 = "{\"boolProp\":false,\"intProp\":2,\"strProp\":\"CDE\"}";
		mDoc = xRepo.getDocumentManagement().storeDocument("json_test3", s1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		uris.add(mDoc.getUri());
		
		s1 = "{\"boolProp\":false,\"intProp\":3,\"strProp\":\"EFGH\"}";
		mDoc = xRepo.getDocumentManagement().storeDocument("json_test4", s1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		uris.add(mDoc.getUri());

		xRepo.getTxManagement().commitTransaction(txId);
		
		String query = "declare namespace m=\"http://www.w3.org/2005/xpath-functions/map\";\n" +
				"for $doc in fn:collection('maps')\n" +
				"where m:get($doc, 'intProp') = 2\n" +
				"return fn:serialize($doc, map{'method': 'json'})";
		
		try (ResultCursor results = query(query, null, null)) {
			assertTrue(results.next());
			Object value = results.getObject();
			assertNotNull(value);
			assertFalse(results.next());
		}
	}
	
	@Test
	public void updateMapDocumentTest() throws Exception {
		queryMapDocumentTest();
		
		long txId = xRepo.getTxManagement().beginTransaction();
		Map<String, Object> m1 = new HashMap<>();
		m1.put("intProp", 2); 
		m1.put("boolProp", Boolean.TRUE);
		m1.put("strProp", "CDE10");
	    Properties props = new Properties();
		props.setProperty(pn_document_collections, "maps");
		props.setProperty(pn_document_data_format, "MAP");
		DocumentAccessor mDoc = xRepo.getDocumentManagement().storeDocument("map_test3", m1, props);
		assertNotNull(mDoc);
		assertEquals(txId, mDoc.getTxStart());
		//uris.add(mDoc.getUri());
		xRepo.getTxManagement().commitTransaction(txId);
		
		try (ResultCursor results = query(qDoc, null, null)) {
			assertTrue(results.next());
			String value = results.getString();
			assertNotNull(value);
			assertTrue(value.indexOf("\"boolProp\":true") > 0);
			assertTrue(value.indexOf("\"strProp\":\"CDE10\"") > 0);
			assertFalse(results.next());
		}
	}
	
	private static String qDelete = "declare namespace bgdb=\"http://bagridb.com/bdb\";\n" +
			"declare variable $uri external;\n" + 
			"let $uri := bgdb:remove-document($uri)\n" + 
			"return $uri";

	private static String qStore = "declare namespace bgdb=\"http://bagridb.com/bdb\";\n" +
			"declare variable $uri external;\n" + 
			"declare variable $content external;\n" + 
			"declare variable $props external;\n" + 
			"let $uri := bgdb:store-document-map($uri, $content, $props)\n" +
			"return $uri";

	@Test
	public void storeMapDocumentTest() throws Exception {

		String uri = "map_test_0";
		long txId = xRepo.getTxManagement().beginTransaction();
		Map<String, Object> m1 = new HashMap<>();
		m1.put("intProp", 1); 
		m1.put("boolProp", Boolean.FALSE);
		m1.put("strProp", "XYZ");
	    Properties props = new Properties();
		props.setProperty(pn_document_collections, "maps");
		props.setProperty(pn_document_data_format, "MAP");

		Map<String, Object> params = new HashMap<>();
		params.put("uri", URI.create(uri));
		params.put("content", m1);
		params.put("props", props);

		try (ResultCursor cursor = xRepo.getQueryManagement().executeQuery(qStore, params, props)) {
			assertTrue(cursor.next());
			assertEquals(uri, cursor.getString());
			assertFalse(cursor.next());
		}
		
		//assertNotNull(mDoc);
		//assertEquals(txId, mDoc.getTxStart());
		uris.add(uri);
		xRepo.getTxManagement().commitTransaction(txId);
		
		props.setProperty(pn_document_headers, String.valueOf(DocumentAccessor.HDR_CONTENT));
		Map<String, Object> m2 = xRepo.getDocumentManagement().getDocument(uri, props).getContent();
		assertEquals(m1.get("intProp"), m2.get("intProp"));
		assertEquals(m1.get("boolProp"), m2.get("boolProp"));
		assertEquals(m1.get("strProp"), m2.get("strProp"));
	}
	
	@Test
	public void queryMapDocumentsTest() throws Exception {
	    Properties props = new Properties();
		props.setProperty(pn_document_collections, "maps");
		props.setProperty(pn_document_data_format, "MAP");
		props.setProperty(pn_client_txLevel, pv_client_txLevel_skip);
		//long txId = xRepo.getTxManagement().beginTransaction();
		for (int i=0; i < 100; i++) {
			Map<String, Object> m1 = new HashMap<>();
			m1.put("intProp", i); 
			m1.put("boolProp", i % 2 == 0);
			m1.put("strProp", "xyz" + 32*i);
			DocumentAccessor mDoc = xRepo.getDocumentManagement().storeDocument("map_test" + i, m1, props);
			assertNotNull(mDoc);
			//assertEquals(txId, mDoc.getTxStart());
			uris.add(mDoc.getUri());
		}
		//xRepo.getTxManagement().commitTransaction(txId);
		
		ResultCollection<String> uris2 = (ResultCollection<String>) xRepo.getDocumentManagement().getDocumentUris("uri >= map_test50, txFinish = 0", props);
		assertEquals(54, uris2.size());
		
		props.setProperty(pn_client_fetchSize, "25");
		ResultCollection<DocumentAccessor> results = (ResultCollection<DocumentAccessor>) xRepo.getDocumentManagement().getDocuments("uri >= map_test50, txFinish = 0", props);
		assertEquals(25, results.size());
	}
	
}
