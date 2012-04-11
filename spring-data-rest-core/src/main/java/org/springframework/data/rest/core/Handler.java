package org.springframework.data.rest.core;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface Handler<T,V> {
  V handle(T t);
}
