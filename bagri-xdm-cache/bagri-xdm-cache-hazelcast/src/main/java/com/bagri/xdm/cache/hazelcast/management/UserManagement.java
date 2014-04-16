/**
 * 
 */
package com.bagri.xdm.cache.hazelcast.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.bagri.common.manage.JMXUtils;
import com.bagri.xdm.XDMNode;
import com.bagri.xdm.XDMSchema;
import com.bagri.xdm.XDMUser;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;

/**
 * @author Denis Sukhoroslov
 *
 */
@ManagedResource(objectName="com.bagri.xdm:type=Management,name=UserManagement", 
	description="User Management MBean")
public class UserManagement implements InitializingBean {

    private static final transient Logger logger = LoggerFactory.getLogger(UserManagement.class);
	private static final String user_management = "UserManagement";
    
	//private Properties defaults; 
    private HazelcastInstance hzInstance;
	//private IExecutorService execService;
    private IMap<String, XDMUser> userCache;
    private Map<String, UserManager> mgrCache = new HashMap<String, UserManager>();
    
	public UserManagement(HazelcastInstance hzInstance) {
		//super();
		this.hzInstance = hzInstance;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
        Set<String> names = userCache.keySet();
        for (String name: names) {
        	XDMUser user = userCache.get(name);
        	if (user.isActive()) {
        		initUser(user);
        	}
        }
	}
	
	public void setUserCache(IMap<String, XDMUser> userCache) {
		this.userCache = userCache;
	}

	@ManagedAttribute(description="Registered User Names")
	public String[] getUserNames() {
		return userCache.keySet().toArray(new String[0]);
	}
	
	//@Override
	public Collection<XDMUser> getUsers() {
		return new ArrayList<XDMUser>(userCache.values());
	}


	private boolean initUser(XDMUser user) throws Exception {
		if (!userCache.containsKey(user.getLogin())) {
			userCache.put(user.getLogin(), user);
		}

		if (!mgrCache.containsKey(user.getLogin())) {
			UserManager uMgr = new UserManager(hzInstance, user.getLogin()); 
			mgrCache.put(user.getLogin(), uMgr);
			uMgr.afterPropertiesSet();
			return true;
		}
		return false;
	}
	
	private boolean denitUser(XDMUser user) {
		// find and unreg UserManager...
		UserManager uMgr = mgrCache.remove(user.getLogin());
		if (uMgr != null) {
			uMgr.close();
			return true;
		}
		return false;
	}
	
	@ManagedOperation(description="Create new User")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "login", description = "User login"),
		@ManagedOperationParameter(name = "password", description = "User password")})
	public boolean addUser(String login, String password) {
		XDMUser user = new XDMUser(login, password, true, new Date(), user_management);
		try {
			return initUser(user);
		} catch (Exception ex) {
			logger.error("addUser; error: " + ex.getMessage(), ex);
		}
		return false;
	}

	@ManagedOperation(description="Delete User")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "login", description = "User login")})
	public boolean deleteUser(String login) {
		// denit UserManager
		XDMUser user = userCache.get(login);
		if (user != null) {
			return denitUser(user);
		}
		return false;
	}


}
