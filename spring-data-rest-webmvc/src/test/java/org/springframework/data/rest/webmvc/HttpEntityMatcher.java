package org.springframework.data.rest.webmvc;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * @author Jon Brisbin
 */
class HttpEntityMatcher<T> extends BaseMatcher<HttpEntity<T>> {

	private final HttpEntity<T> expected;

	public HttpEntityMatcher(HttpEntity<T> expected) {
		Assert.notNull(expected, "HttpEntity cannot be null");
		this.expected = expected;
	}

	public static <T> HttpEntityMatcher<T> httpEntity(HttpEntity<T> httpEntity) {
		return new HttpEntityMatcher<T>(httpEntity);
	}

	@Override
	public boolean matches(Object item) {
		if (!(item instanceof HttpEntity)) {
			return false;
		}

		if (item instanceof ResponseEntity && expected instanceof ResponseEntity) {
			ResponseEntity<?> left = (ResponseEntity<?>) expected;
			ResponseEntity<?> right = (ResponseEntity<?>) item;

			if (!left.getStatusCode().equals(right.getStatusCode())) {
				return false;
			}
		}

		HttpEntity<?> left = expected;
		HttpEntity<?> right = (HttpEntity<?>) item;

		return left.getBody().equals(right.getBody()) && left.getHeaders().equals(right.getHeaders());
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(expected.toString());
	}

}
