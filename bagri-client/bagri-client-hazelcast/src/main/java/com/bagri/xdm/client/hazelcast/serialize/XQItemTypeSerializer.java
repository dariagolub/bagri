package com.bagri.xdm.client.hazelcast.serialize;

import java.io.IOException;
import java.net.URI;

import javax.validation.constraints.NotNull;
import javax.xml.namespace.QName;
import javax.xml.xquery.XQDataFactory;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bagri.xqj.BagriXQUtils.*;
import static javax.xml.xquery.XQItemType.*;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

public class XQItemTypeSerializer implements StreamSerializer<XQItemType> {

    private static final Logger logger = LoggerFactory.getLogger(XQItemTypeSerializer.class);
	
	private XQDataFactory xqFactory;
	
	protected XQDataFactory getXQDataFactory() {
		return xqFactory;
	}

	public void setXQDataFactory(@NotNull XQDataFactory xqDataFactory) {
		this.xqFactory = xqDataFactory;
	}
    
	@Override
	public int getTypeId() {
		return XDMDataSerializationFactory.cli_XQItemType;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}
	
	@Override
	public XQItemType read(ObjectDataInput in) throws IOException {
		try {
			int kind = in.readInt();
			int baseType = 0;
			QName typeName = null;
			if (isBaseTypeSupported(kind)) {
				baseType = in.readInt();
				typeName = in.readObject();
			}
			QName nodeName = null;
			if (isNodeNameSupported(kind)) { // || isPINameSupported(kind)) {
				nodeName = in.readObject();
			}
			URI schemaURI = in.readObject();
			boolean nillable = in.readBoolean();
			XQDataFactory xqFactory = getXQDataFactory();
			if (baseType > 0) {
				if (isAtomicType(baseType)) {
					return xqFactory.createAtomicType(baseType, typeName, schemaURI); 
				} else { 
					switch (kind) {
						case XQITEMKIND_ATTRIBUTE: 
							return xqFactory.createAttributeType(nodeName, baseType, typeName, schemaURI); 
						case XQITEMKIND_DOCUMENT_ELEMENT:
							XQItemType deType = xqFactory.createElementType(nodeName, baseType, typeName, schemaURI, nillable);
							return xqFactory.createDocumentElementType(deType); 
						case XQITEMKIND_DOCUMENT_SCHEMA_ELEMENT: 
							XQItemType seType = xqFactory.createSchemaElementType(nodeName, baseType, schemaURI);
							return xqFactory.createDocumentSchemaElementType(seType);  
						case XQITEMKIND_ELEMENT: 
							return xqFactory.createElementType(nodeName, baseType, typeName, schemaURI, nillable);
						case XQITEMKIND_SCHEMA_ATTRIBUTE: 
							return xqFactory.createSchemaAttributeType(nodeName, baseType, schemaURI);
						case XQITEMKIND_SCHEMA_ELEMENT: 
							return xqFactory.createSchemaElementType(nodeName, baseType, schemaURI);
					}
					// List types are not covered yet! 
				}
			} else {
				switch (kind) {
					case XQITEMKIND_DOCUMENT: 
						return xqFactory.createDocumentType();
					case XQITEMKIND_COMMENT: 
						return xqFactory.createCommentType();
					case XQITEMKIND_NODE: 
						return xqFactory.createNodeType();
					case XQITEMKIND_PI: 
						return xqFactory.createProcessingInstructionType(nodeName == null ? null : nodeName.getLocalPart()); 
					case XQITEMKIND_TEXT: 
						return xqFactory.createTextType();
				}
			}
			logger.info("read; got wrong baseType/kind combination: {}/{}, returning null", baseType, kind);
			return null;
		} catch (XQException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public void write(ObjectDataOutput out, XQItemType type) throws IOException {
		try {
			int kind = type.getItemKind();
			out.writeInt(kind);
			if (isBaseTypeSupported(kind)) {
				out.writeInt(type.getBaseType());
				out.writeObject(type.getTypeName());
			}
			if (isNodeNameSupported(kind)) { // || isPINameSupported(kind)) {
				out.writeObject(type.getNodeName()); // can be issues with wildcards
			}
			out.writeObject(type.getSchemaURI());
			out.writeBoolean(type.isElementNillable());
		} catch (XQException ex) {
			throw new IOException(ex);
		}
	}

}
