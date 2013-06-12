package org.springframework.data.rest.repository.support;

/**
 * Helper methods aiming at handling String representations of resources.
 *
 * @author Florent Biville
 */
public class ResourceStringUtils {
  /**
   * Checks whether the given input contains actual text (slash excluded).
   * This is a specializing variant of {@link org.springframework.util.StringUtils )}#hasText.
   */
  public static boolean hasText(CharSequence input) {
    int strLen = input.length();
	for (int i = 0; i < strLen; i++) {
		if (!Character.isWhitespace(input.charAt(i)) && !startsWithSlash(input.charAt(i))) {
			return true;
		}
	}
	return false;
  }

    /**
   * Returns a string without the leading slash, if any.
   */
  public static String removeLeadingSlash(String path) {
    if (path.length() == 0) {
        return path;
    }

    boolean hasLeadingSlash = startsWithSlash(path);
    if (path.length() == 1) {
        return hasLeadingSlash ? "" : path;
    }
    return hasLeadingSlash ? path.substring(1) : path;
  }

  private static boolean startsWithSlash(String path) {
    return path.charAt(0) == '/';
  }

  private static boolean startsWithSlash(char c) {
    return c == '/';
  }
}
