package org.springframework.data.rest.core;

/**
 * Generic interface used as a callback in any place you need extensibility.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public interface Handler<T, V> {

  /**
   * Accept an argument and possibly produce a result.
   *
   * @param t
   *     arg
   *
   * @return Some object or {@literal null} if no result.
   */
  V handle(T t);

}
