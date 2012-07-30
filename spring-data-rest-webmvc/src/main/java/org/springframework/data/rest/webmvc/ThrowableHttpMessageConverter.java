package org.springframework.data.rest.webmvc;

import java.io.IOException;
import java.io.PrintWriter;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * @author Jon Brisbin
 */
public class ThrowableHttpMessageConverter extends AbstractHttpMessageConverter<Throwable> {

  private final ObjectMapper mapper = new ObjectMapper();

  @Override protected boolean supports(Class<?> clazz) {
    throw new IllegalStateException("supports(Class<?> clazz) not used in " + getClass().getName());
  }

  @Override public boolean canRead(Class<?> clazz, MediaType mediaType) {
    return false;
  }

  @Override public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    return (Throwable.class.isAssignableFrom(clazz)
        && (mediaType.getSubtype().contains("json") || mediaType.getSubtype().contains("text")));
  }

  @Override protected Throwable readInternal(Class<? extends Throwable> clazz, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {
    throw new HttpMessageNotReadableException("Cannot read Throwables from input.");
  }

  @Override protected void writeInternal(Throwable throwable, HttpOutputMessage outputMessage)
      throws IOException, HttpMessageNotWritableException {
    if(outputMessage.getHeaders().getContentType().getSubtype().contains("json")) {
      outputMessage.getBody().write(mapper.writeValueAsBytes(throwable));
    } else {
      throwable.printStackTrace(new PrintWriter(outputMessage.getBody()));
    }
  }

}
