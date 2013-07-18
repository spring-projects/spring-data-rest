package org.springframework.data.rest.core.domain.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Jon Brisbin
 */
@Document
public class Profile {

	@Id private String id;
	private String name;
	private String type;

	public Profile() {}

	public Profile(String id, String name, String type) {
		this.id = id;
		this.name = name;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Profile setName(String name) {
		this.name = name;
		return this;
	}

	public String getType() {
		return type;
	}

	public Profile setType(String type) {
		this.type = type;
		return this;
	}

}
