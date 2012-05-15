package org.springframework.data.rest.webmvc;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class RepositoryRestViewResolver implements ViewResolver {

  private View view;
  private Map<String, View> customViewMappings = Collections.emptyMap();

  public RepositoryRestViewResolver(View view) {
    this.view = view;
  }

  public RepositoryRestViewResolver setCustomViewMappings(Map<String, View> customViewMappings) {
    this.customViewMappings = customViewMappings;
    return this;
  }

  @Override public View resolveViewName(String viewName, Locale locale) throws Exception {
    if (customViewMappings.containsKey(viewName)) {
      return customViewMappings.get(viewName);
    }
    return view;
  }

}
