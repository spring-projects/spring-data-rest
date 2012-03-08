package org.springframework.data.rest.mvc;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.springframework.data.rest.core.SimpleLink;
import org.springframework.data.rest.core.util.FluentBeanSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.AbstractView;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
@SuppressWarnings({"unchecked"})
public class JsonView extends AbstractView {

  private ObjectMapper mapper = new ObjectMapper();

  {
    CustomSerializerFactory customSerializerFactory = new CustomSerializerFactory();
    customSerializerFactory.addSpecificMapping(SimpleLink.class, new FluentBeanSerializer(SimpleLink.class));
    mapper.setSerializerFactory(customSerializerFactory);
  }

  public JsonView(String mediaType) {
    setContentType(mediaType);
  }

  @Override
  protected void renderMergedOutputModel(Map<String, Object> model,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
    HttpStatus status = status(model);
    response.setStatus(status.value());

    String contentType = getContentType();
    HttpHeaders headers = headers(model);
    if (null != headers) {
      for (Map.Entry<String, String> entry : headers.toSingleValueMap().entrySet()) {
        response.setHeader(entry.getKey(), entry.getValue());
      }
      if (null != headers.getContentType()) {
        contentType = headers.getContentType().toString();
      }
    }
    response.setContentType(contentType);

    Object resource = model.get("resource");
    if (null != resource) {
      if (resource instanceof Throwable) {
        resource = ((Throwable) resource).getMessage();
      }
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      mapper.writerWithDefaultPrettyPrinter().writeValue(bout, resource);

      response.getOutputStream().write(bout.toByteArray());
    } else {
      response.setContentLength(0);
    }
  }


  private HttpStatus status(Map<String, Object> model) {
    Object o = model.get("status");
    if (null != o && o instanceof HttpStatus) {
      return (HttpStatus) o;
    }
    throw new IllegalArgumentException("No status is set in the model.");
  }

  private HttpHeaders headers(Map<String, Object> model) {
    Object o = model.get("headers");
    if (null != o && o instanceof HttpHeaders) {
      return (HttpHeaders) o;
    }
    return null;
  }

}
