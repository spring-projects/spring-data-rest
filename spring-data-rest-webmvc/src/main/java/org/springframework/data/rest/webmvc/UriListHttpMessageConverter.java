package org.springframework.data.rest.webmvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.rest.repository.invoke.RepositoryMethodResponse;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;

/**
 * A special {@link org.springframework.http.converter.HttpMessageConverter} that can take various input formats and
 * produce a plain-text list of URIs (or read the same).
 *
 * @author Jon Brisbin
 */
public class UriListHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

  public UriListHttpMessageConverter() {
    super(MediaTypes.URI_LIST);
  }

  @Override protected boolean supports(Class<?> clazz) {
    return (RepositoryMethodResponse.class.isAssignableFrom(clazz)
        || Resource.class.isAssignableFrom(clazz)
        || Set.class.isAssignableFrom(clazz));
  }

  @SuppressWarnings({"unchecked"})
  @Override
  protected Object readInternal(Class<?> clazz,
                                HttpInputMessage inputMessage) throws IOException,
                                                                      HttpMessageNotReadableException {
    Assert.isTrue((Resource.class.isAssignableFrom(clazz) || Set.class.isAssignableFrom(clazz)),
                  "Cannot read a text/uri-list into a " + clazz);

    String rel = inputMessage.getHeaders().getFirst("x-spring-data-urilist-rel");
    if(null == rel && inputMessage instanceof ServletServerHttpRequest) {
      rel = ((ServletServerHttpRequest)inputMessage).getURI().getPath().substring(1).replaceAll("/", ".");
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()));

    Set<Link> links = new HashSet<Link>();
    String line;
    while(null != (line = reader.readLine())) {
      links.add(new Link(URI.create(line.trim()).toString(), rel));
    }

    return (Set.class.isAssignableFrom(clazz) ? links : new Resource<String>("", links));
  }

  @Override
  protected void writeInternal(Object links, HttpOutputMessage outputMessage)
      throws IOException,
             HttpMessageNotWritableException {
    OutputStream body = outputMessage.getBody();
    if(links instanceof Set) {
      for(Object o : (Set)links) {
        if(o instanceof Link) {
          body.write(((Link)o).getHref().getBytes());
        } else {
          body.write(o.toString().getBytes());
        }
        body.write('\n');
      }
    } else if(links instanceof RepositoryMethodResponse) {
      writeInternal(((RepositoryMethodResponse)links).getLinks(), outputMessage);
    } else if(links instanceof Resource) {
      writeInternal(((Resource)links).getLinks(), outputMessage);
    }
  }

}
