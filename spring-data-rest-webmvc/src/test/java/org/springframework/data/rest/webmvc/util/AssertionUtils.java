package org.springframework.data.rest.webmvc.util;

import java.util.function.Predicate;

/**
 * Various extensions of AssertJ to improve testing.
 *
 * @author Greg Turnquist
 */
public final class AssertionUtils {

	/**
	 * {@link Predicate} to verify a given string ends in a certain suffix.
	 *
	 * @param suffix
	 * @return
	 */
	public static Predicate<String> suffix(String suffix) {
		return s -> s.endsWith(suffix);
	}
}
