package org.springframework.data.rest.webmvc;

/**
 * Indicates a resource was not found.
 *
 * @author Jon Brisbin
 */
public class ResourceNotFoundException extends Exception {
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
