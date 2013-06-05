package org.springframework.data.rest.webmvc;

/**
 * Indicates a resource was not found.
 *
 * @author Jon Brisbin
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7992904489502842099L;

	public ResourceNotFoundException() {
    super("Resource not found");
  }

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
