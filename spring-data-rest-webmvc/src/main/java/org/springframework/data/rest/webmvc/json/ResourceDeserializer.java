/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.json;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.support.RepositoryUriResolver;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * 'Hal -> Json -> Domain Object' deserializer registered by {@link PersistentEntityJackson2Module}.
 * 
 * This Json deserializer parses Hal '_links' structures, retrieving the associated objects
 * based upon the link URL.
 * 
 * The deserialization is a two stage process.
 * 
 * The first stage involves pre-processing the Json data while building a token buffer that will upon completion
 * of this stage, contains the complete domain object including the linked assoications. During this stage, 
 * when a _links section is encountered the parser transforms each link into an object by retrieving it from 
 * the associated repository. Once the object is retrieve it is then serialized into the token buffer at the point
 * where it is encountered.
 * 
 * The second stage simply involves deserializing this token buffer as the 'domain type' being deserialized. Since
 * this uses the default Jackson deserializer this means that all Jackson annotations will be honored. 
 * 
 * @author Nick Weedon
 */
class ResourceDeserializer<T extends Object> extends StdDeserializer<T> {

	private static final long serialVersionUID = 8195592798684027681L;
	private final PersistentEntity<?, ?> rootEntity;
	private final ConditionalGenericConverter uriDomainClassConverter;
	private final RepositoryUriResolver repositoryUriResolver;

	private static final TypeDescriptor URI_TYPE = TypeDescriptor.valueOf(URI.class);
	private static final Logger LOG = LoggerFactory.getLogger(PersistentEntityJackson2Module.class);

	ResourceDeserializer(PersistentEntity<?, ?> rootEntity,
			ConditionalGenericConverter uriDomainClassConverter, 
			RepositoryUriResolver repositoryUriResolver) {
		super(rootEntity.getType());
		this.uriDomainClassConverter = uriDomainClassConverter;
		this.rootEntity = rootEntity;
		this.repositoryUriResolver = repositoryUriResolver;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	@Override
	public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonToken tok = null;
		
		HalToJsonConverter halToJsonConverter = new HalToJsonConverter();
		
		TokenBuffer domainObjectBuffer = halToJsonConverter.convertToJsonBuffer(rootEntity, jp);

		if(LOG.isDebugEnabled()) {
			dumpTokenBuffer(domainObjectBuffer);
		}
		
		// Use a standard object mapper to read the token buffer
		// Reading the value through filteredJp will cause infinite recursion of this method
		ObjectMapper objectMapper = new ObjectMapper();
		JsonParser filteredJp = domainObjectBuffer.asParser();
		Object entity = objectMapper.readValue(filteredJp, handledType());

		return (T) entity;
	}
	
	private String getErrorMsgPrefix(JsonParser jp) {
		int JsonLineNo = jp.getCurrentLocation().getLineNr();
		return "While parsing JSON document at line " + JsonLineNo + ": ";
	}
	
	private void logDebug(int depth, String msg) {
		if(LOG.isDebugEnabled()) {
			StringBuilder fullMsg = new StringBuilder();
			for(int i = 1; i < depth; i++) {
				fullMsg.append("\t");
			}
			fullMsg.append(msg);
			LOG.debug(fullMsg.toString());
		}
	}

	private void logWarning(int depth, String msg) {
		if(LOG.isWarnEnabled()) {
			StringBuilder fullMsg = new StringBuilder();
			for(int i = 1; i < depth; i++) {
				fullMsg.append("\t");
			}
			fullMsg.append(msg);
			LOG.warn(fullMsg.toString());
		}
	}
	
	private class HalToJsonConverter {
		
		TokenBuffer domainObjectBuffer;
		JsonParser jp;
		
		public TokenBuffer convertToJsonBuffer(PersistentEntity<?, ?> entity, JsonParser halJsonParser) throws JsonParseException, IOException {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Transforming '" + rootEntity.getName() + "' HAL resource into regular JSON.");
				LOG.debug("==== Original HAL JSON tokens ====");
			}
			domainObjectBuffer = new TokenBuffer(halJsonParser);
			jp = halJsonParser;
			
			parseJsonObject(entity, 1);
			return domainObjectBuffer;
		}
		
		private void parseJsonObject(PersistentEntity<?, ?> entity, int depth) throws JsonParseException, IOException {
			Set<String> processedFields = new HashSet<String>();
			JsonToken token = jp.getCurrentToken();
	
			String name = jp.getCurrentName();
	
			if(token == null) {
				throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
						+ "Malformed JSON encountered. Cannot parse object.");
			}
			
			logDebug(depth, name + ": " + token.toString());
			
			domainObjectBuffer.copyCurrentEvent(jp);
	
			depth++;
	
			do {
				token = jp.nextToken();
				name = jp.getCurrentName();
	
				if(token == JsonToken.START_OBJECT) {
					
					PersistentEntity<?, ?> propertyEntity = null; 
					if(name != null) {
						PersistentProperty<?> associationInverse = getAssociationInverse(entity, name);
						if(associationInverse != null) {
							propertyEntity = associationInverse.getOwner();
						}
					}
					parseJsonObject(propertyEntity, depth);
					continue;
				}
	
				if(token == JsonToken.END_ARRAY || token == JsonToken.END_OBJECT) {
					depth--;
				}
				
				if(token == JsonToken.FIELD_NAME) {
					if(processedFields.contains(name)) {
						throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
							+ "Encountered field '" + name + " more than once in the same object."
							+ "Note that a relation field may not appear inline if it is contained in the '_links' section of that object.");
					}
					processedFields.add(name);
					
					if(name.equals("_links")) {
						logDebug(depth, "<Processing a '_links' section>");
						token = jp.nextToken();
						if(token != JsonToken.START_OBJECT) {
							throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
									+ "Expected '_links' to be of type object.");
						}
						processLinksObject(entity, depth, processedFields);
						continue;
					}
				}
	
				logDebug(depth, name + ": " + token.toString());
	
				if(token == JsonToken.START_ARRAY) {
					depth++;
				}
	
				domainObjectBuffer.copyCurrentEvent(jp);
			}
			while(token != JsonToken.END_OBJECT); 
		}
	
		private void processLinkToken(PersistentEntity<?,?> entity, int depth, String linkName, 
				Set<String> processedFields) throws JsonParseException, IOException {
			
			if(!jp.getCurrentName().equals("href")) {
	
				// We only understand 'href' at the moment so ignore
				logWarning(depth, "Encountered unsupported link field '" + jp.getCurrentName() + "'.");
				jp.nextToken();
				return;
			}
			String uriString = jp.nextTextValue();
	
			URI uri = URI.create(uriString);
			
			ResourceMetadata repoInfo = repositoryUriResolver.findRepositoryInfoForUri(uri);
					
			if(linkName.equals("self")) {
				
				// Simply extract the id from the link
				PersistentProperty<?> idProperty = entity.getIdProperty();
				if(idProperty == null) {
					throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
							+ "Cannot use id in 'self' link uri '" + uriString + "' as there is no metadata for this entity.");
				}
				String id = getIdFromUri(uriString, jp);
				logDebug(depth, "Set id '" + idProperty.getName() + "' to '" + id + "' <-- (self) '" + uriString + "'");
						
				domainObjectBuffer.writeStringField(idProperty.getName(), id); 
			} else if(repoInfo != null) {
				TypeDescriptor linkDomainType = TypeDescriptor.valueOf(repoInfo.getDomainType());
				
				Object linkObject = uriDomainClassConverter.convert(uri, URI_TYPE, linkDomainType);
				
				if(linkObject == null) {
					throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
							+ "No data object for URI '" + uriString + "'.");
				}
				
				// Add the created association object to the Json buffer representing 
				// the transformed domain object
				domainObjectBuffer.writeObject(linkObject);
				logDebug(depth, "new '" + linkName + "' <-- '" + uriString + "'");
				processedFields.add(linkName);
			} else {
				throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
						+ "Could not create object from uri '" + uriString 
						+ "' as there is no exported repository mapped to this URI.");
			}
		}
		
		private void processLinksObject(PersistentEntity<?, ?> entity, 
				int depth, Set<String> processedFields) throws JsonParseException, IOException {
			// Inside _links object
	
			JsonToken token = jp.nextToken();
			depth++;
	
			// For each link field
			do {
				if(token != JsonToken.FIELD_NAME) {
					throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
							+ "Expected field but encountered json token type '" + token.toString() + "'.");					
				}
				String linkName = jp.getCurrentName();
	
				if(processedFields.contains(linkName)) {
					throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
						+ "Encountered link field '" + linkName + " more than once in the same object."
						+ "Note that a relation field may not appear inline if it is contained in the '_links' section of that object.");
				}
				
				if(token == JsonToken.FIELD_NAME && linkName.equals("_links")) {
					throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
							+ "Encountered '_links' property while already inside a '_links' object.");
				}
				
				if(linkName != "self") {
					domainObjectBuffer.copyCurrentEvent(jp);
				}
				
				// Process the link field's value, could be either a single object or an array
				token = jp.nextToken();
				boolean isArray = false;
				switch(token) {
					case START_OBJECT:
						break;
					case START_ARRAY:
						isArray = true;
						//domainObjectBuffer.writeArrayFieldStart(linkName);
						domainObjectBuffer.copyCurrentEvent(jp);
						// Consume the next token so we are in a state that is consistent
						// with that of processing a single link value.
						token = jp.nextToken();
						break;
					default:
						throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
								+ "Expected link property to be of 'object' or 'array' type.");
				}
				
				// Continue to process the value(s) of the link until we reach:
				// END_OBJECT (for an object) or END_ARRAY (for an array)
				boolean moreLinkValues = true;
				while(moreLinkValues) {
					token = jp.nextToken();
					switch(token) {
						case FIELD_NAME:
							processLinkToken(entity, depth, linkName, processedFields);
							break;
						case END_OBJECT:
							if(!isArray) {
								moreLinkValues = false;
							}
							break;
						case END_ARRAY:
							if(!isArray) {
								throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
										+ "Expected end of array but encountered end of object.");
							} 
							domainObjectBuffer.copyCurrentEvent(jp);
							//domainObjectBuffer.writeEndArray();
							moreLinkValues = false;
							break;
						case START_OBJECT:
							if(!isArray) {
								throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
										+ "Encountered more than one object in a non-array type '_links' object.");
							} 
							break;
						default:
							throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
									+ "Expected field or end of object/array.");
					}
				}
				// Read the next link field
				token = jp.nextToken();
			} while(token != JsonToken.END_OBJECT);
		}
		
		private String getIdFromUri(String uri, JsonParser jp) {
			String[] parts = uri.split("/");
	
			if (parts.length < 2) {
				throw new HttpMessageNotReadableException(getErrorMsgPrefix(jp) 
						+ "Cannot extract id from uri '" + uri + "'.");
			}
	
			return parts[parts.length - 1];
		}
	
		// TODO: Add some kind of constant or log time association lookup method to PersistentEntity.
		// This linear search isn't too bad based on the fact that it is unlikely that there would be more than 5 or so
		// relations in most entities. It would be nice however to have some kind of lookup method as part of PersistentEntiy
		// that could be used instead.
		private PersistentProperty<?> getAssociationInverse(PersistentEntity<?, ?> entity, final String inverseName) {
			
			final PersistentProperty<?>[] retAssociation = {null};
			
			// Add associations as links
			entity.doWithAssociations(new SimpleAssociationHandler() {
	
				/*
				 * (non-Javadoc)
				 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
				 */
				@Override
				public void doWithAssociation(Association<? extends PersistentProperty<?>> association) {
					if(association.getInverse().getName().equals(inverseName)) {
						retAssociation[0] = association.getInverse();
					}
				}
			});
			return retAssociation[0];
		}
	}
	
	private void dumpTokenBuffer(TokenBuffer tokenBuffer) throws JsonParseException, IOException {

		LOG.warn("==== Transformed regular JSON tokens ====");
		
		JsonParser jp = tokenBuffer.asParser();
		JsonToken token;
		int depth = 1;
		
		do {
			token = jp.nextToken();
			String name = jp.getCurrentName();

			if(token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
				depth--;
			}
			
			logDebug(depth, name + ": " + token.toString());
			
			if(token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
				depth++;
			}
			
		}
		while(!(token == JsonToken.END_OBJECT  && depth == 1)); 		
	}
		
}