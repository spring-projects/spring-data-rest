package org.springframework.data.rest.webmvc;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Jon Brisbin
 */
public abstract class Requests {

	public static MockHttpServletRequest ROOT_REQUEST = new MockHttpServletRequest("GET", "http://localhost:8080/");
	public static MockHttpServletRequest PAGE_REQUEST = new MockHttpServletRequest("GET", "http://localhost:8080/");

	static {
		PAGE_REQUEST.setParameter("page", "2");
		PAGE_REQUEST.setParameter("size", "10");
	}

	private Requests() {}

}
