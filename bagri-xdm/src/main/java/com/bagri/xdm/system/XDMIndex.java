package com.bagri.xdm.system;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.bagri.xdm.common.XDMEntity;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://www.bagri.com/xdm/system", propOrder = {
		"name", 
		"docType", 
		"path",
		"dataType",
		"caseSensitive",
		"range",
		"unique",
		"description",
		"enabled"
})
public class XDMIndex extends XDMEntity {
	
	@XmlAttribute(required = true)
	@XmlID
	private String name;
		
	@XmlElement(required = true)
	private String docType;

	@XmlElement(required = true)
	private String path;

	@XmlElement(required = false, defaultValue = "xs:string")
	private String dataType = "xs:string";

	@XmlElement(required = false, defaultValue = "true")
	private boolean caseSensitive = true;

	@XmlElement(required = false, defaultValue = "false")
	private boolean range = false;

	@XmlElement(required = false, defaultValue = "false")
	private boolean unique = false;
	
	@XmlElement(required = false)
	private String description;
		
	@XmlElement(required = false, defaultValue = "true")
	private boolean enabled = true;

	public XDMIndex() {
		// for JAXB
		super();
	}
	
	public XDMIndex(int version, Date createdAt, String createdBy, String name, 
			String docType, String path, String dataType, boolean caseSensitive, boolean range, boolean unique, 
			String description, boolean enabled) {
		super(version, createdAt, createdBy);
		this.name = name;
		this.docType = docType;
		this.path = path;
		this.dataType = dataType;
		this.caseSensitive = caseSensitive;
		this.range = range;
		this.unique = unique;
		this.description = description;
		this.enabled = enabled;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
	
	public String getDocumentType() {
		return docType;
	}

	public String getPath() {
		return path;
	}
	
	public String getDataType() {
		return dataType;
	}
	
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean isRange() {
		return range;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			//this.updateVersion("???");
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

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
		XDMIndex other = (XDMIndex) obj;
		return name.equals(other.name); 
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> result = new HashMap<>();
		result.put("name", name);
		result.put("version", getVersion());
		result.put("created at", getCreatedAt().toString());
		result.put("created by", getCreatedBy());
		result.put("document type", docType);
		result.put("path", path);
		result.put("data type", dataType);
		result.put("case sensitive", caseSensitive);
		result.put("range", range);
		result.put("unique", unique);
		result.put("description", description);
		result.put("enabled", enabled);
		return result;
	}

	@Override
	public String toString() {
		return "XDMIndex [name=" + name + ", version=" + getVersion()
				+ ", docType=" + docType + ", path=" + path + ", dataType=" + dataType
				+ ", caseSensitive=" + caseSensitive + ", range=" + range + ", unique=" + unique
				+ ", created at=" + getCreatedAt() + ", by=" + getCreatedBy()
				+ ", description=" + description + ", enabled=" + enabled + "]";
	}
	
}
