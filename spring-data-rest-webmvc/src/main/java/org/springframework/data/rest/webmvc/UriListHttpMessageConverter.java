package org.springframework.data.rest.webmvc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.LinkList;
import org.springframework.data.rest.core.Resource;
import org.springframework.data.rest.core.ResourceLink;
import org.springframework.data.rest.repository.invoke.RepositoryMethodResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;

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
        || List.class.isAssignableFrom(clazz)
        || Map.class.isAssignableFrom(clazz)
        || LinkList.class.isAssignableFrom(clazz)
        || Resource.class.isAssignableFrom(clazz));
  }

  @SuppressWarnings({"unchecked"})
  @Override
  protected Object readInternal(Class<?> clazz,
                                HttpInputMessage inputMessage)
      throws IOException,
             HttpMessageNotReadableException {

    String rel = inputMessage.getHeaders().getFirst("x-spring-data-urilist-rel");
    if(null == rel && inputMessage instanceof ServletServerHttpRequest) {
      rel = ((ServletServerHttpRequest)inputMessage).getURI().getPath().substring(1).replaceAll("/", ".");
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputMessage.getBody()));
    String line;
    Object links;
    try {
      links = clazz.newInstance();
    } catch(InstantiationException e) {
      throw new HttpMessageNotReadableException(e.getMessage(), e);
    } catch(IllegalAccessException e) {
      throw new HttpMessageNotReadableException(e.getMessage(), e);
    }
    while(null != (line = reader.readLine())) {
      Link l = new ResourceLink(rel, URI.create(line.trim()));
      if(links instanceof LinkList) {
        ((LinkList)links).add(l);
      } else if(links instanceof List) {
        ((List)links).add(l);
      } else if(links instanceof Map) {
        List linksFromMap = (List)((Map)links).get("links");
        if(null == linksFromMap) {
          linksFromMap = new ArrayList();
          ((Map)links).put("links", linksFromMap);
        }
        linksFromMap.add(l);
      } else if(links instanceof Resource) {
        ((Resource)links).addLink(l);
      }
    }
    return links;
  }

  @Override
  protected void writeInternal(Object links, HttpOutputMessage outputMessage)
      throws IOException,
             HttpMessageNotWritableException {
    OutputStream body = outputMessage.getBody();
    if(links instanceof LinkList) {
      for(Link link : ((LinkList)links).getLinks()) {
        body.write(link.href().toASCIIString().getBytes());
        body.write('\n');
      }
    } else if(links instanceof List) {
      for(Object o : (List)links) {
        if(o instanceof Link) {
          body.write(((Link)o).href().toASCIIString().getBytes());
        } else {
          body.write(o.toString().getBytes());
        }
        body.write('\n');
      }
    } else if(links instanceof Map) {
      writeInternal(((Map)links).get("links"), outputMessage);
    } else if(links instanceof RepositoryMethodResponse) {
      writeInternal(((RepositoryMethodResponse)links).getLinks(), outputMessage);
    } else if(links instanceof Resource) {
      writeInternal(((Resource)links).getLinks(), outputMessage);
    }
  }

}
