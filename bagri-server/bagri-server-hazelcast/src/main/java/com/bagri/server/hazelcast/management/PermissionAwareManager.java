package com.bagri.server.hazelcast.management;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import com.bagri.core.system.Permission;
import com.bagri.core.system.PermissionAware;
import com.bagri.core.system.Role;
import com.bagri.server.hazelcast.task.role.PermissionUpdater;
import com.bagri.server.hazelcast.task.role.RoleUpdater;
import com.bagri.support.util.JMXUtils;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public abstract class PermissionAwareManager<P extends PermissionAware> extends EntityManager {
	
	public PermissionAwareManager() {
		super();
	}
	
	public PermissionAwareManager(HazelcastInstance hzInstance, String entityName) {
		super(hzInstance, entityName);
	}
	
	@Override
	public P getEntity() {
		return (P) super.getEntity();
	}
	
	protected abstract IMap<String, Role> getRoleCache();

	@ManagedAttribute(description="Returns registered Role permissions")
	public CompositeData getDirectPermissions() {
		Map<String, Object> pMap = getEntity().getFlatPermissions();
		return JMXUtils.mapToComposite(entityName, "permissions", pMap);
	}
	
	@ManagedAttribute(description="Returns included (nested) Roles")
	public String[] getDirectRoles() {
		Set<String> roles = getEntity().getIncludedRoles(); 
		return roles.toArray(new String[roles.size()]);
	}
	
	protected void getRecursivePermissions(Map<String, Permission> xPerms, String roleName) {
		Role role = getRoleCache().get(roleName);
		if (role != null) {
			if (role.getIncludedRoles().size() > 0) {
				for (String name: role.getIncludedRoles()) {
					getRecursivePermissions(xPerms, name);
				}
			}
			
			Collection<Permission> perms = role.getPermissions().values();
			if (perms.size() > 0) {
				for (Permission perm: perms) {
					Permission xPerm = xPerms.get(perm.getResource());
					if (xPerm == null) {
						xPerm = new Permission(perm.getResource());
						xPerms.put(perm.getResource(), xPerm);
					}
					for (Permission.Value p: perm.getPermissions()) {
						xPerm.addPermission(p);
					}
				}
			}
		}
	}

	protected void getRecursiveRoles(Set<String> xRoles, String roleName) {
		Role role = getRoleCache().get(roleName);
		if (role != null) {
			if (role.getIncludedRoles().size() > 0) {
				for (String name: role.getIncludedRoles()) {
					getRecursiveRoles(xRoles, name);
				}
			}
			xRoles.add(roleName);
		}
	}
	
	@ManagedOperation(description="Returns access permission for the named Resource")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "resource", description = "A name of the Resource to return")})
	public String[] getResourcePermissions(String resource) {
		Permission perm = getEntity().getPermissions().get(resource);
		if (perm != null) {
			return perm.getPermissionsAsArray();
		}
		return new String[0];
	}
	
	@ManagedOperation(description="Set access permission to the named Resource")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "resource", description = "A name of Resource to add Permissions"),
		@ManagedOperationParameter(name = "permissions", description = "Permisions to add separated by whitespace")})
	public void addPermissions(String resource, String permissions) {
		P role = updatePermissions(resource, permissions, RoleUpdater.Action.add);
	    logger.trace("addPermissions; execution result: {}", role);
	}
	
	@ManagedOperation(description="Removes access permission to the named Reource")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "resource", description = "A name of Resource to remove permissions from"),
		@ManagedOperationParameter(name = "permissions", description = "Permisions to remove separated by whitespace")})
	public void removePermissions(String resource, String permissions) {
		P role = updatePermissions(resource, permissions, RoleUpdater.Action.remove);
	    logger.trace("removePermissions; execution result: {}", role);
	}

	private P updatePermissions(String resource, String permissions, RoleUpdater.Action action) {
		P role = getEntity();
		String[] aPerms = resolvePermissions(permissions);
	    Object result = entityCache.executeOnKey(entityName, new PermissionUpdater(role.getVersion(), 
	    		getCurrentUser(), resource, aPerms, action));
		return (P) result;
	}
	
	private String[] resolvePermissions(String permissions) {
		String[] aPerms = permissions.split(" ");
	    logger.trace("resolvePermissions.enter; got permissions: '{}'; split to array: {} with length: {}", permissions, aPerms, aPerms.length);
	    List<String> lPerms = new ArrayList<String>(aPerms.length);
	    for (String perm: aPerms) {
	    	String xPerm = perm.trim();
	    	if (xPerm.length() > 0) {
	    		try {
	    			if (Permission.Value.valueOf(xPerm) != null) {
	    				lPerms.add(xPerm);
	    			} else {
	    			    logger.info("resolvePermissions; unknown permission: {}, skipping", xPerm);
	    			}
	    		} catch (Exception ex) {
    			    logger.warn("resolvePermissions; error resolving permission: {}, skipping", xPerm);
	    		}
	    	}
	    }
	    logger.trace("resolvePermissions.exit; returning: {}", lPerms);
		return lPerms.toArray(new String[lPerms.size()]);
	}
	
	@ManagedOperation(description="Adds included (nested) Roles")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "roles", description = "Name of the Roles to include separated by whitespace")})
	public void addIncludedRoles(String roles) {
		P role = updateIncludedRoles(roles, RoleUpdater.Action.add);
	    logger.trace("addIncludedRoles; execution result: {}", role);
	}
	
	@ManagedOperation(description="Removes included (nested) Roles")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name = "roles", description = "Name of the Roles to remove separated by whitespace")})
	public void removeIncludedRoles(String roles) {
		P role = updateIncludedRoles(roles, RoleUpdater.Action.remove);
	    logger.trace("removeIncludedRoles; execution result: {}", role);
	}

	private P updateIncludedRoles(String roles, RoleUpdater.Action action) {
		String[] aRoles = roles.split(" ");
		P role = getEntity();
	    Object result = entityCache.executeOnKey(entityName, new RoleUpdater(role.getVersion(), 
	    		getCurrentUser(), aRoles, action));
		return (P) result;
	}
	
	
}
