package org.springframework.data.rest.example.neo4j;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * @author Jon Brisbin
 */
@NodeEntity
public class Studio {

	@GraphId
	private Long   id;
	private String name;

	public Studio() {
	}

	public Studio(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
