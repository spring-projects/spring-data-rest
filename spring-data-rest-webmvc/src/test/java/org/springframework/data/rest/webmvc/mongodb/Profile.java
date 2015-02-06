package org.springframework.data.rest.webmvc.mongodb;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jon Brisbin
 */
@Document
public class Profile {

	@Id private String id;
	private Long person;
	private @JsonProperty(required = true) String type;
	private @LastModifiedDate Date lastModifiedDate;
	private @JsonProperty("renamed") String aliased;

	public String getId() {
		return id;
	}

	public Profile setId(String id) {
		this.id = id;
		return this;
	}

	public Long getPerson() {
		return person;
	}

	public Profile setPerson(Long person) {
		this.person = person;
		return this;
	}

	public String getType() {
		return type;
	}

	public Profile setType(String type) {
		this.type = type;
		return this;
	}

	@JsonIgnore
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public String getAliased() {
		return aliased;
	}
}
