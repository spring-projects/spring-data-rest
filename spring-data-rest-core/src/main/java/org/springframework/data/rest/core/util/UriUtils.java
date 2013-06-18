package org.springframework.data.rest.core.util;

import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Helper methods for dealing with URIs.
 * 
 * @author Jon Brisbin
 */
public abstract class UriUtils {

	/**
	 * Create a new {@link URI} out of the components.
	 * 
	 * @param baseUri The base URI these path segments are relative to.
	 * @param pathSegments The path segments to add to the given base URI.
	 * @return A new URI built from the given base URI and additional path segments.
	 */
	public static URI buildUri(URI baseUri, String... pathSegments) {
		return UriComponentsBuilder.fromUri(baseUri).pathSegment(pathSegments).build().toUri();
	}

}
