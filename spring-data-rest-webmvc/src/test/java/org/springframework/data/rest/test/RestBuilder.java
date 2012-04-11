package org.springframework.data.rest.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Closure;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class RestBuilder {

  private static final String[] DATE_FORMATS = new String[]{
      "EEE, dd MMM yyyy HH:mm:ss z",
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
      "yyyy-MM-dd HH:mm:ss"
  };

  private ConversionService conversionService = new DefaultConversionService();
  private ClientHttpRequestFactory requestFactory;
  private RestTemplate restTemplate;
  private HttpHeaders headers = new HttpHeaders();
  private MediaType contentType;
  private Class<?> responseType = byte[].class;
  private Map uriParams;
  private Object body;
  private Closure errorHandler;

  public RestBuilder() {
    this.restTemplate = new RestTemplate();
  }

  public RestBuilder(ClientHttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    this.restTemplate = new RestTemplate(requestFactory);
  }

  public Object call(Closure cl) {
    RestBuilder b = null != requestFactory ? new RestBuilder(requestFactory) : new RestBuilder();
    if (null != errorHandler) {
      b.setErrorHandler(errorHandler);
    }
    b.conversionService = conversionService;
    cl.setDelegate(b);

    return cl.call();
  }

  public Object delete(String url) {
    restTemplate.delete(url);
    return this;
  }

  @SuppressWarnings({"unchecked"})
  public Object get(String url) {
    return restTemplate.getForEntity(maybeAddParams(url), responseType);
  }

  @SuppressWarnings({"unchecked"})
  public Object post(String url) {
    if (responseType == URI.class) {
      return restTemplate.postForLocation(maybeAddParams(url), new HttpEntity(body, headers));
    } else {
      return restTemplate.postForEntity(maybeAddParams(url), new HttpEntity(body, headers), responseType);
    }
  }

  @SuppressWarnings({"unchecked"})
  public Object put(String url) {
    if (null != uriParams) {
      restTemplate.put(maybeAddParams(url), new HttpEntity(body, headers), uriParams);
    } else {
      restTemplate.put(maybeAddParams(url), new HttpEntity(body, headers));
    }
    return this;
  }

  public Object accept(String accept) {
    headers.setAccept(MediaType.parseMediaTypes(accept));
    return this;
  }

  public Object body(Object body) {
    this.body = body;
    return this;
  }

  public Object contentType(String contentType) {
    this.contentType = MediaType.parseMediaType(contentType);
    headers.setContentType(this.contentType);
    return this;
  }

  public Object date(Date date) {
    headers.setDate(date.getTime());
    return this;
  }

  @SuppressWarnings({"unchecked"})
  public Object date(String date) {
    for (String fmt : DATE_FORMATS) {
      try {
        Date dte = new SimpleDateFormat(fmt).parse(date);
        headers.setDate(dte.getTime());
        break;
      } catch (ParseException e) {}
    }
    return this;
  }

  @SuppressWarnings({"unchecked"})
  public Object header(String key, Object val) {
    if (null != val) {
      if (val instanceof List) {
        headers.put(key, (List) val);
      } else if (ClassUtils.isAssignable(val.getClass(), String.class)) {
        headers.set(key, (String) val);
      } else {
        headers.set(key, conversionService.convert(val, String.class));
      }
    } else {
      headers.remove(key);
    }
    return this;
  }

  @SuppressWarnings({"unchecked"})
  public Object headers(Map headers) {
    this.headers.putAll(headers);
    return this;
  }

  public Date now() {
    return Calendar.getInstance().getTime();
  }

  @SuppressWarnings({"unchecked"})
  public Object param(String key, String value) {
    if (null == uriParams) {
      uriParams = new HashMap();
    }
    uriParams.put(key, value);
    return this;
  }

  public Object params(Map params) {
    this.uriParams = params;
    return this;
  }

  public Object responseType(Class<?> responseType) {
    this.responseType = responseType;
    return this;
  }

  public Object setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
    return this;
  }

  public Object setErrorHandler(Closure errorHandler) {
    this.errorHandler = errorHandler;
    if (null != errorHandler) {
      this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
        @Override public void handleError(ClientHttpResponse response) throws IOException {
          RestBuilder.this.errorHandler.call(response);
        }
      });
    }
    return this;
  }

  public Object setMessageConverters(List<HttpMessageConverter<?>> converters) {
    restTemplate.setMessageConverters(converters);
    return this;
  }

  @SuppressWarnings({"unchecked"})
  private String maybeAddParams(String url) {
    StringBuffer buff = new StringBuffer(url);
    if (null != uriParams) {
      buff.append("?");
      for (Map.Entry<String, String> entry : ((Map<String, String>) uriParams).entrySet()) {
        try {
          buff.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new IllegalStateException(e);
        }
      }
    }
    return buff.toString();
  }

  @Override public String toString() {
    return "RestBuilder{" +
        "requestFactory=" + requestFactory +
        ", restTemplate=" + restTemplate +
        ", headers=" + headers +
        ", params=" + uriParams +
        ", contentType=" + contentType +
        ", errorHandler=" + errorHandler +
        '}';
  }

}
