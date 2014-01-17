/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.hamcrest.Matcher;
import org.springframework.data.rest.core.Path;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.util.Assert;
import org.springframework.web.util.UriTemplate;

/**
 * Simple wrapper for {@link Resource}s to allow easy assertions on it.
 * 
 * @author Oliver Gierke
 */
public class ResourceTester {

	private final ResourceSupport resource;

	public static ResourceTester of(Object object) {
		assertThat(object, is(instanceOf(ResourceSupport.class)));
		return new ResourceTester((ResourceSupport) object);
	}

	/**
	 * Creates a new {@link ResourceTester} for the given {@link ResourceSupport}.
	 * 
	 * @param resource must not be {@literal null}.
	 */
	private ResourceTester(ResourceSupport resource) {
		Assert.notNull(resource, "Resource must not be null!");
		this.resource = resource;
	}

	/**
	 * Asserts that the {@link Resource} contains the given number of {@link Link}s.
	 * 
	 * @param number
	 */
	public void assertNumberOfLinks(int number) {
		assertThat(resource.getLinks().size(), is(number));
	}

	/**
	 * Asserts that the {@link Resource} has a link with the given rel and href.
	 * 
	 * @param rel must not be {@literal null}.
	 * @param href can be {@literal null}, if so, only the presence of a {@link Link} with the given rel is checked.
	 */
	public Link assertHasLink(String rel, String href) {
		return assertHasLinkMatching(rel, href == null ? null : is(href));
	}

	/**
	 * Asserts that the {@link Resource} has a link with the given rel and ending with the given href.
	 * 
	 * @param rel must not be {@literal null}.
	 * @param href can be {@literal null}, if so, only the presence of a {@link Link} with the given rel is checked.
	 */
	public Link assertHasLinkEndingWith(String rel, String hrefEnd) {
		return assertHasLinkMatching(rel, hrefEnd == null ? null : endsWith(hrefEnd));
	}

	private final Link assertHasLinkMatching(String rel, Matcher<String> hrefMatcher) {

		Link link = resource.getLink(rel);
		assertThat("Expected link with rel '" + rel + "' but didn't find it in " + resource.getLinks(), link,
				is(notNullValue()));

		if (hrefMatcher != null) {
			assertThat(link.getHref(), is(hrefMatcher));
		}

		return link;
	}

	@SuppressWarnings("unchecked")
	public <T> PagedResources<T> assertIsPage() {

		assertThat(resource, is(instanceOf(PagedResources.class)));
		return (PagedResources<T>) resource;
	}

	public ResourceTester getContentResource() {

		assertThat(resource, is(instanceOf(Resources.class)));
		Object next = ((Resources<?>) resource).getContent().iterator().next();

		assertThat(next, is(instanceOf(ResourceSupport.class)));
		return new ResourceTester((ResourceSupport) next);
	}

	public void withContentResource(ContentResourceHandler handler) {

		assertThat(resource, is(instanceOf(Resources.class)));

		for (Object element : ((Resources<?>) resource).getContent()) {
			assertThat(element, is(instanceOf(ResourceSupport.class)));
			handler.doWith(of(element));
		}
	}

	public interface ContentResourceHandler {

		void doWith(ResourceTester content);
	}

	public static class HasSelfLink implements ContentResourceHandler {

		private final Path template;

		public HasSelfLink(Path template) {
			this.template = template;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.rest.webmvc.ResourceTester.ContentResourceHandler#doWith(org.springframework.data.rest.webmvc.ResourceTester)
		 */
		@Override
		public void doWith(ResourceTester content) {

			String href = content.assertHasLink("self", null).getHref();

			UriTemplate uriTemplate = new UriTemplate(template.toString());
			assertThat(String.format("Expected %s to match %s!", href, uriTemplate.toString()), uriTemplate.matches(href),
					is(true));
		}
	}
}
