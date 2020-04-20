package org.springframework.data.rest.webmvc.util;

import java.util.function.Predicate;

/**
 * Various extensions of AssertJ to improve testing.
 *
 * @author Greg Turnquist
 */
public final class AssertionUtils {


    /**
     * {@link Predicate} to verify a given string ends in a certain pattern.
     *
     * @param pattern
     * @return
     */
    public static Predicate<String> endsWith(String pattern) {
        return s -> s.endsWith(pattern);
    }
}
