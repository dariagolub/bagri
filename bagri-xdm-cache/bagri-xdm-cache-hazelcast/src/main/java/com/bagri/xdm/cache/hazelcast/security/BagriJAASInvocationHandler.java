package com.bagri.xdm.cache.hazelcast.security;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.MBeanServerForwarder;
import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BagriJAASInvocationHandler implements InvocationHandler {

    private static final transient Logger logger = LoggerFactory.getLogger(BagriJAASInvocationHandler.class);
	
	private MBeanServer mbs;
	
	public static MBeanServerForwarder newProxyInstance() {

		final InvocationHandler handler = new BagriJAASInvocationHandler();
		final Class[] interfaces = new Class[] {MBeanServerForwarder.class};
		Object proxy = Proxy.newProxyInstance(
				MBeanServerForwarder.class.getClassLoader(), interfaces,
				handler);

		return MBeanServerForwarder.class.cast(proxy);
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		logger.trace("invoke.enter; method: {}; args: {}", method.getName(), args); 

		final String methodName = method.getName();

		if (methodName.equals("getMBeanServer")) {
			return mbs;
		}

		if (methodName.equals("setMBeanServer")) {
			if (args[0] == null)
				throw new IllegalArgumentException("Null MBeanServer");
			if (mbs != null)
				throw new IllegalArgumentException("MBeanServer object "
						+ "already initialized");
			mbs = (MBeanServer) args[0];
			return null;
		}

		// Retrieve Subject from current AccessControlContext
		AccessControlContext acc = AccessController.getContext();
		Subject subject = Subject.getSubject(acc);

		// Allow operations performed locally on behalf of the connector server
		// itself
		if (subject == null) {
			return method.invoke(mbs, args);
		}

		// Restrict access to "createMBean" and "unregisterMBean" to any user
		if (methodName.equals("createMBean")
				|| methodName.equals("unregisterMBean")) {
			throw new SecurityException("Access denied");
		}

		// Retrieve JMXPrincipal from Subject
		Set<JMXPrincipal> principals = subject
				.getPrincipals(JMXPrincipal.class);
		if (principals == null || principals.isEmpty()) {
			throw new SecurityException("Access denied");
		}
		Principal principal = principals.iterator().next();
		String identity = principal.getName();

		// "role1" can perform any operation other than "createMBean" and
		// "unregisterMBean"
		if (identity.equals("role1")) {
			return method.invoke(mbs, args);
		}

		// "role2" can only call "getAttribute" on the MBeanServerDelegate MBean
		if (identity.equals("role2") && methodName.equals("getAttribute")
				&& MBeanServerDelegate.DELEGATE_NAME.equals(args[0])) {
			return method.invoke(mbs, args);
		}

		throw new SecurityException("Access denied");
	}

}
