package org.springframework.data.rest.webmvc;

import java.util.Collections;

import org.springframework.hateoas.Resources;

/**
 * @author Jon Brisbin
 */
public class RepositoryLinksResource extends Resources<Object> {

	public RepositoryLinksResource() {
		super(Collections.emptyList());
	}
}
