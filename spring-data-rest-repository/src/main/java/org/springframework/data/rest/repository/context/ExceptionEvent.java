package org.springframework.data.rest.repository.context;

/**
 * An event to encapsulate an exception occurring anywhere within the REST exporter.
 *
 * @author Jon Brisbin
 */
public class ExceptionEvent extends RepositoryEvent {
  public ExceptionEvent(Throwable t) {
    super(t);
  }

  /**
   * Get the source of this exception event.
   *
   * @return The {@link Throwable} that is the source of this exception event.
   */
  public Throwable getException() {
    return (Throwable)getSource();
  }
}
