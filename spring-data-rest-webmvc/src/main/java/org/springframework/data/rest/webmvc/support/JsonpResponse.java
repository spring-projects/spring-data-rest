package org.springframework.data.rest.webmvc.support;

import org.springframework.http.ResponseEntity;

/**
 * @author Jon Brisbin
 */
public class JsonpResponse<T> {

  private final ResponseEntity<T> responseEntity;
  private final String            callbackParam;
  private final String            errbackParam;

  public JsonpResponse(ResponseEntity<T> responseEntity,
                       String callbackParam,
                       String errbackParam) {
    this.responseEntity = responseEntity;
    this.callbackParam = callbackParam;
    this.errbackParam = errbackParam;
  }

  public ResponseEntity<T> getResponseEntity() {
    return responseEntity;
  }

  public String getCallbackParam() {
    return callbackParam;
  }

  public String getErrbackParam() {
    return errbackParam;
  }

}
