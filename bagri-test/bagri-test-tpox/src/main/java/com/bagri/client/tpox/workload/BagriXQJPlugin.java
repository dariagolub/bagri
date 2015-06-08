package com.bagri.client.tpox.workload;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;

import net.sf.tpox.workload.parameter.ActualParamInfo;
import net.sf.tpox.workload.parameter.Parameter;
import net.sf.tpox.workload.transaction.Transaction;



//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static com.bagri.common.config.XDMConfigConstants.xdm_spring_context;

public class BagriXQJPlugin extends BagriTPoXPlugin {

    //ApplicationContext context; // = new ClassPathXmlApplicationContext(config);
	
	private ThreadLocal<XQConnection> xqc = new ThreadLocal<XQConnection>() {
		
		@Override
		protected XQConnection initialValue() {
	    	String config = System.getProperty(xdm_spring_context);
			ApplicationContext context = new ClassPathXmlApplicationContext(config);
			XQConnection xqc = context.getBean("xqConnection", XQConnection.class);
			try {
				xqc.getStaticContext().setQueryTimeout(60);
			} catch (XQException ex) {
		    	logger.error("initialValue.error; error setting timeout: ", ex);
			}
	    	logger.info("initialValue.exit; XQC: {}", xqc);
			return xqc;
 		}
		
	};
	
    protected XQConnection getConnection() {
    	return xqc.get(); 
    }
    
    public BagriXQJPlugin() {
    	String config = System.getProperty(xdm_spring_context);
    	logger.debug("<init>. Spring context: {}", config);
    	if (config != null) {
    	    //ApplicationContext 
    	    //context = new ClassPathXmlApplicationContext(config);
    		//xqc = context.getBean("xqConnection", XQConnection.class);
    	}
		//logger.trace("<init>. XQConnection: {}", xqc);
    }
	
	@Override
	public void close() throws SQLException {
		XQConnection conn = getConnection();
		logger.info("close; XQC: {}", conn);
		try {
			conn.close();
		} catch (XQException ex) {
			logger.error("close.error; " + ex, ex);
			throw new SQLException(ex);
		}
	}

	@Override
	public int execute() throws SQLException {
		int transNo = wp.getNextTransNumToExecute(rand);
		Transaction tx = wp.getTransaction(transNo);
		int result = 0; 
		logger.trace("execute.enter; transaction: {}; #: {}; ", tx.getTransName(), transNo);

		Vector<Parameter>[] params = wp.getParameterMarkers();
		int size = (params[transNo].size() - 2)/3;
		
		ActualParamInfo param = wp.getParamMarkerActualValue(transNo, 0, rand);
		String query = param.getActualValue();
		param = wp.getParamMarkerActualValue(transNo, 1, rand);
		boolean isQuery = Boolean.parseBoolean(param.getActualValue());
		Map<String, Object> vars = new HashMap<String, Object>(size);
		String value;
		
		//logger.debug("execute; size: {}; rand: {}; transNo: {}", size, rand, transNo);
		try {
			for (int i=0; i < size; i++) {
				param = wp.getParamMarkerActualValue(transNo, i*3+2, rand);
				String name = param.getActualValue();
				param = wp.getParamMarkerActualValue(transNo, i*3+3, rand);
				String type = param.getActualValue();
				param = wp.getParamMarkerActualValue(transNo, i*3+4, rand);
				if (type.equals("document")) {
					value = new String(param.getDocument());
				} else {
					value = param.getActualValue();
				}
				vars.put(name, buildParam(type, value));
			}
			logger.trace("execute; query: {}; params: {}", query, vars);
		
			if (isQuery) {
				// use execQuery
				result = execQuery(query, vars);
			} else {
				// use execCommand
				result = execCommand(query, vars);
			}
		} catch (Throwable ex) {
			logger.error("execute.error", ex);
		}
		
		return result;
	}
	
	protected int execCommand(String query, Map<String, Object> params) throws XQException {
		
		XQExpression xqe = getConnection().createExpression();
	    for (Map.Entry<String, Object> e: params.entrySet()) {
	    	if (e.getValue() instanceof Integer) {
	    		xqe.bindInt(new QName(e.getKey()), (Integer) e.getValue(), null);
	    	} else {
	    		xqe.bindString(new QName(e.getKey()), e.getValue().toString(), null);
	    	}
	    }
	    xqe.executeCommand(query);
	    // do next somehow!
		return 1;
	}
	
	protected int execQuery(String query, Map<String, Object> params) throws XQException {

		logger.trace("execQuery; query: {}; params: {}", query, params);
		
	    XQPreparedExpression xqpe = getConnection().prepareExpression(query);
	    for (Map.Entry<String, Object> e: params.entrySet()) {
	    	if (e.getValue() instanceof Integer) {
	    		xqpe.bindInt(new QName(e.getKey()), (Integer) e.getValue(), null);
	    	} else {
	    		xqpe.bindString(new QName(e.getKey()), e.getValue().toString(), null);
	    	}
	    }
	    XQResultSequence xqs = xqpe.executeQuery();
	    boolean found = false;
	    while (xqs.next()) {
	    	found = true;
	    }
	    xqs.close();
	    xqpe.close();
	    if (found) {
	    	return 1;
	    }
	    return 0;
	}
	
}
