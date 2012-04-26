package org.springframework.data.rest.core.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class RestHelper<T> {

  public HttpStatus status;
  public HttpHeaders headers = new HttpHeaders();
  public T body;

  private RestHelper(T body) {
    this.body = body;
  }

  public static <T> RestHelper<T> resource(T body) {
    return new RestHelper<T>(body);
  }

  public RestHelper<T> header(String key, String value) {
    headers.add(key, value);
    return this;
  }

  public RestHelper<T> status(HttpStatus status) {
    this.status = status;
    return this;
  }

  public HttpEntity<T> asHttpEntity() {
    return new HttpEntity<T>(body, headers);
  }

  public ResponseEntity<T> asResponseEntity() {
    return new ResponseEntity<T>(body, headers, status);
  }

}
