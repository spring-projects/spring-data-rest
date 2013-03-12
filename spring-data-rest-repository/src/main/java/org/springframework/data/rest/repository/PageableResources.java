package org.springframework.data.rest.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;

/**
 * @author Jon Brisbin
 */
public class PageableResources<T> extends Resources<T> {

	private Pageable page;

	protected PageableResources() {
		super();
	}

	public PageableResources(Iterable<T> content, Pageable page, Link... links) {
		super(content, links);
		this.page = page;
	}

	public PageableResources(Iterable<T> content, Pageable page, Iterable<Link> links) {
		super(content, links);
		this.page = page;
	}

	public Pageable getPage() {
		return page;
	}

	public PageableResources<T> setPage(Pageable page) {
		this.page = page;
		return this;
	}

}
