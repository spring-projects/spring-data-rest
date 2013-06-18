package org.springframework.data.rest.webmvc.support;

import java.net.URI;

import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin
 */
public class BaseUriLinkBuilder extends LinkBuilderSupport<BaseUriLinkBuilder> {

	public BaseUriLinkBuilder(UriComponentsBuilder builder) {
		super(builder);
	}

	public static BaseUriLinkBuilder create(URI baseUri) {
		return new BaseUriLinkBuilder(UriComponentsBuilder.fromUri(baseUri));
	}

	@Override
	protected BaseUriLinkBuilder getThis() {
		return this;
	}

	@Override
	protected BaseUriLinkBuilder createNewInstance(UriComponentsBuilder builder) {
		return new BaseUriLinkBuilder(builder);
	}

}
