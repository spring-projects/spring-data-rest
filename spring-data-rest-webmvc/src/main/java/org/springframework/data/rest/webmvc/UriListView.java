package org.springframework.data.rest.webmvc;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.rest.core.Link;
import org.springframework.data.rest.core.SimpleLink;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.AbstractView;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class UriListView extends AbstractView {

  public UriListView() {
    setContentType("text/uri-list");
  }

  @SuppressWarnings({"unchecked"})
  @Override
  protected void renderMergedOutputModel(Map<String, Object> model,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {

    Object resource = model.get("resource");
    response.setContentType(getContentType());

    HttpStatus status = (HttpStatus) model.get("status");
    HttpHeaders headers = (HttpHeaders) model.get("headers");
    List<SimpleLink> links = null;
    if (resource instanceof List) {
      links = (List<SimpleLink>) resource;
    } else if (resource instanceof Map) {
      Map m = (Map) resource;
      Object o = m.get("_links");
      if (null != o && o instanceof List) {
        links = (List<SimpleLink>) o;
      } else {
        response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        return;
      }
    } else if (resource instanceof Links) {
      links = ((Links) resource).getLinks();
    }

    if (null != status) {
      response.setStatus(status.value());
    }

    if (null != headers) {
      for (Map.Entry<String, String> entry : headers.toSingleValueMap().entrySet()) {
        response.setHeader(entry.getKey(), entry.getValue());
      }
    }

    PrintWriter out = response.getWriter();
    if (null != links) {
      for (Link l : links) {
        out.println(l.href().toString());
      }
    }
    out.flush();

  }

}
