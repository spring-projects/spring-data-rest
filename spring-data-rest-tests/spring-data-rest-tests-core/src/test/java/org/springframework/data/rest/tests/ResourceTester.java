/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.tests;

import static org.assertj.core.api.Assertions.*;

import java.util.function.Consumer;

import org.springframework.data.rest.core.Path;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;
import org.springframework.web.util.UriTemplate;

/**
 * Simple wrapper for {@link Resource}s to allow easy assertions on it.
 *
 * @author Oliver Gierke
 */
public class ResourceTester {

	private final RepresentationModel resource;

	public static ResourceTester of(Object object) {
		assertThat(object).isInstanceOf(RepresentationModel.class);
		return new ResourceTester((RepresentationModel) object);
	}

	/**
	 * Creates a new {@link ResourceTester} for the given {@link ResourceSupport}.
	 *
	 * @param resource must not be {@literal null}.
	 */
	private ResourceTester(RepresentationModel resource) {
		Assert.notNull(resource, "EntityRepresentationModel must not be null!");
		this.resource = resource;
	}

	/**
	 * Asserts that the {@link Resource} contains the given number of {@link Link}s.
	 *
	 * @param number
	 */
	public void assertNumberOfLinks(int number) {
		assertThat(resource.getLinks()).hasSize(number);
	}

	/**
	 * Asserts that the {@link Resource} has a link with the given rel and href.
	 *
	 * @param rel must not be {@literal null}.
	 * @param href can be {@literal null}, if so, only the presence of a {@link Link} with the given rel is checked.
	 */
	public Link assertHasLink(String rel, String href) {
		return assertHasLinkMatching(rel, it -> {
			if (href == null) {
				return;
			}
			assertThat(it).isEqualTo(href);
		});
	}

	/**
	 * Asserts that the {@link Resource} has a link with the given rel and ending with the given href.
	 *
	 * @param rel must not be {@literal null}.
	 * @param href can be {@literal null}, if so, only the presence of a {@link Link} with the given rel is checked.
	 */
	public Link assertHasLinkEndingWith(String rel, String hrefEnd) {
		return assertHasLinkMatching(rel, it -> {
			if (hrefEnd == null) {
				return;
			}
			assertThat(it).endsWith(hrefEnd);
		});
	}

	private Link assertHasLinkMatching(String rel, Consumer<String> hrefMatcher) {

		Link link = resource.getRequiredLink(rel);
		assertThat(link).as("Expected link with rel '" + rel + "' but didn't find it in " + resource.getLinks())
				.isNotNull();

		if (hrefMatcher != null) {
			assertThat(link.getHref()).satisfies(hrefMatcher);
		}

		return link;
	}

	@SuppressWarnings("unchecked")
	public <T> PagedModel<T> assertIsPage() {

		assertThat(resource).isInstanceOf(PagedModel.class);
		return (PagedModel<T>) resource;
	}

	public ResourceTester getContentResource() {

		assertThat(resource).isInstanceOf(CollectionModel.class);
		Object next = ((CollectionModel<?>) resource).getContent().iterator().next();

		assertThat(next).isInstanceOf(RepresentationModel.class);
		return new ResourceTester((RepresentationModel) next);
	}

	public void withContentResource(ContentResourceHandler handler) {

		assertThat(resource).isInstanceOf(CollectionModel.class);

		for (Object element : ((CollectionModel<?>) resource).getContent()) {
			assertThat(element).isInstanceOf(RepresentationModel.class);
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
			assertThat(uriTemplate.matches(href)).as(String.format("Expected %s to match %s!", href, uriTemplate.toString()))
					.isTrue();
		}
	}
}
