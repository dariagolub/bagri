package com.bagri.xdm.api;

/**
 * XDM health check state options; provided for the client side
 * 
 * @author Denis Sukhoroslov
 */
public enum XDMHealthCheckState {
	
	/**
	 * do not check health state
	 */
	skip,

	/**
	 * log warning in case of wrong state but perform operations further 
	 */
	log,

	/**
	 * throw exception in case of wrong state, prevent any operation until state became healthy
	 */
	raise;

}
