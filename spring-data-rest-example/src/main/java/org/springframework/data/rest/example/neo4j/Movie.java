package org.springframework.data.rest.example.neo4j;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

/**
 * @author Jon Brisbin
 */
@NodeEntity
public class Movie {

	@GraphId
	private Long   id;
	private String name;
	@RelatedTo(enforceTargetType = true)
	private Studio studio;

	public Movie() {
	}

	public Movie(String name, Studio studio) {
		this.name = name;
		this.studio = studio;
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

	public Studio getStudio() {
		return studio;
	}

	public void setStudio(Studio studio) {
		this.studio = studio;
	}

}
