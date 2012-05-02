package org.springframework.data.rest.core;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface Handler<T,V> {
  V handle(T t);
}
