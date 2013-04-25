package org.springframework.data.rest.webmvc;

import org.springframework.hateoas.Resources;

import java.util.Collections;

/**
 * @author Jon Brisbin
 */
public class RepositoryLinksResource extends Resources<Object> {

	public RepositoryLinksResource() {
		super(Collections.emptyList());
	}

}
