package org.springframework.data.rest.webmvc.convert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.rest.webmvc.support.JsonpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * @author Jon Brisbin
 */
public class JsonpResponseHttpMessageConverter implements HttpMessageConverter<JsonpResponse<?>> {

  private static final MediaType       APPLICATION_JAVASCRIPT = MediaType.valueOf("application/javascript");
  private static final List<MediaType> SUPPORTED_TYPES        = Arrays.asList(
      APPLICATION_JAVASCRIPT
  );

  private final MappingJackson2HttpMessageConverter jacksonConverter;

  public JsonpResponseHttpMessageConverter(MappingJackson2HttpMessageConverter jacksonConverter) {
    this.jacksonConverter = jacksonConverter;
  }

  @Override public boolean canRead(Class<?> clazz, MediaType mediaType) {
    return false;
  }

  @Override public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    return JsonpResponse.class.isAssignableFrom(clazz) && mediaType.getSubtype().contains("javascript");
  }

  @Override public List<MediaType> getSupportedMediaTypes() {
    return SUPPORTED_TYPES;
  }

  @Override
  public JsonpResponse<?> read(Class<? extends JsonpResponse<?>> clazz,
                               HttpInputMessage inputMessage) throws IOException,
                                                                     HttpMessageNotReadableException {
    throw new HttpMessageNotReadableException("JSONP messages are not readable.");
  }

  @Override
  public void write(JsonpResponse<?> jsonpResponse,
                    MediaType contentType,
                    final HttpOutputMessage outputMessage) throws IOException,
                                                                  HttpMessageNotWritableException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    bytes.write((jsonpResponse.getCallbackParam() + "(").getBytes());

    jacksonConverter.write(jsonpResponse.getResponseEntity().getBody(),
                           MediaType.APPLICATION_JSON,
                           new HttpOutputMessage() {
                             @Override public OutputStream getBody() throws IOException {
                               return bytes;
                             }

                             @Override public HttpHeaders getHeaders() {
                               return outputMessage.getHeaders();
                             }
                           });

    bytes.write(");".getBytes());

    byte[] byteArray = bytes.toByteArray();

    outputMessage.getHeaders().setContentType(APPLICATION_JAVASCRIPT);
    outputMessage.getHeaders().setContentLength(byteArray.length);
    outputMessage.getBody().flush();
    outputMessage.getBody().write(byteArray);
  }

}
