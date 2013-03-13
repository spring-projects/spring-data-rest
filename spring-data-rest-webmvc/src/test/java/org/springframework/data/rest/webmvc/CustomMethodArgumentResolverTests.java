package org.springframework.data.rest.webmvc;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.data.rest.repository.PagingAndSorting;
import org.springframework.data.rest.webmvc.annotation.BaseURI;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Jon Brisbin
 */
public class CustomMethodArgumentResolverTests extends AbstractJMockTests {

	static final MethodParameter BASE_URI;
	static final MethodParameter PAGE_SORT;

	static {
		try {
			BASE_URI = MethodParameter.forMethodOrConstructor(
					Methods.class.getDeclaredMethod("baseUri", URI.class),
					0
			);
			PAGE_SORT = MethodParameter.forMethodOrConstructor(
					Methods.class.getDeclaredMethod("pagingAndSorting", PagingAndSorting.class),
					0
			);
		} catch(NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	private final RepositoryRestConfiguration            config           =
			new RepositoryRestConfiguration()
					.setBaseUri(URI.create("http://localhost:8080"));
	private final BaseUriMethodArgumentResolver          baseUriResolver  =
			new BaseUriMethodArgumentResolver(config);
	private final PagingAndSortingMethodArgumentResolver pageSortResolver =
			new PagingAndSortingMethodArgumentResolver(config);
	private ModelAndViewContainer mavContainer;
	private WebDataBinderFactory  webDataBinderFactory;

	@Before
	public void setup() {
		mavContainer = new ModelAndViewContainer();
		webDataBinderFactory = context.mock(WebDataBinderFactory.class);
	}

	@Test
	public void baseUriMethodArgumentResolver() throws Exception {
		assertThat("Finds @BaseURI-annotated java.net.URI parameter",
		           baseUriResolver.supportsParameter(BASE_URI),
		           is(true));

		// Resolve the base URI
		URI baseUri = (URI)baseUriResolver.resolveArgument(
				BASE_URI,
				mavContainer,
				new ServletWebRequest(Requests.ROOT_REQUEST),
				webDataBinderFactory
		);

		assertThat("Base URI should be 'http://localhost:8080'",
		           baseUri.toString(),
		           is("http://localhost:8080"));
	}

	@Test
	public void pagingAndSortingMethodArgumentResolver() throws Exception {
		assertThat("Finds PagingAndSorting parameter",
		           pageSortResolver.supportsParameter(PAGE_SORT),
		           is(true));

		// Resolve Page and Sort information
		PagingAndSorting pageSort = (PagingAndSorting)pageSortResolver.resolveArgument(
				PAGE_SORT,
				mavContainer,
				new ServletWebRequest(Requests.PAGE_REQUEST),
				webDataBinderFactory
		);

		assertThat("Finds page parameter value",
		           pageSort.getPageNumber(),
		           is(1));
		assertThat("Finds limit parameter value",
		           pageSort.getPageSize(),
		           is(5));
	}

	static class Methods {
		void baseUri(@BaseURI URI baseUri) {
		}

		void pagingAndSorting(PagingAndSorting pageSort) {
		}
	}

}
