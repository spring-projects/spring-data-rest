package org.springframework.data.rest.webmvc.convert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * @author Jon Brisbin
 */
public class UriListHttpMessageConverter implements HttpMessageConverter<Resource<?>> {

  private static final List<MediaType> MEDIA_TYPES = new ArrayList<MediaType>();

  static {
    MEDIA_TYPES.add(MediaType.parseMediaType("text/uri-list"));
  }

  @Override public boolean canRead(Class<?> clazz, MediaType mediaType) {
    if(null == mediaType) {
      return false;
    }
    return Resource.class.isAssignableFrom(clazz) && mediaType.getSubtype().contains("uri-list");
  }

  @Override public boolean canWrite(Class<?> clazz, MediaType mediaType) {
    return canRead(clazz, mediaType);
  }

  @Override public List<MediaType> getSupportedMediaTypes() {
    return MEDIA_TYPES;
  }

  @Override public Resource<?> read(Class<? extends Resource<?>> clazz,
                                    HttpInputMessage inputMessage)
      throws IOException,
             HttpMessageNotReadableException {
    List<Link> links = new ArrayList<Link>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()));
    String line;
    while(null != (line = reader.readLine())) {
      links.add(new Link(line));
    }
    return new Resource<Object>(Collections.emptyList(), links);
  }

  @Override public void write(Resource<?> resource,
                              MediaType contentType,
                              HttpOutputMessage outputMessage)
      throws IOException,
             HttpMessageNotWritableException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));
    for(Link link : resource.getLinks()) {
      writer.write(link.getHref());
      writer.newLine();
    }
    writer.flush();
  }

}
