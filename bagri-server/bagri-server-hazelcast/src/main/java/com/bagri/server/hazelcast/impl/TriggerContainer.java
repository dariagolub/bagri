package com.bagri.server.hazelcast.impl;

import com.bagri.core.server.api.DocumentTrigger;

public class TriggerContainer<T> {
	
	private int index;
	private boolean synch;
	private T impl;
	
	public TriggerContainer(int index, boolean synch, T impl) {
		this.index = index;
		this.synch = synch;
		this.impl = impl;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return the synch
	 */
	public boolean isSynchronous() {
		return synch;
	}

	/**
	 * @return the impl
	 */
	public T getImplementation() {
		return impl;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TriggerContainer other = (TriggerContainer) obj;
		if (index != other.index) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TriggerContainer [index=" + index + ", synch=" + synch
				+ ", impl=" + impl + "]";
	}

	
}
