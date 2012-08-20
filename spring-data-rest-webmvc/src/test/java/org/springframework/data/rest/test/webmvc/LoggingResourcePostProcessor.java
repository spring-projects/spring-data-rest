package org.springframework.data.rest.test.webmvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.Resource;
import org.springframework.data.rest.webmvc.ResourcePostProcessor;
import org.springframework.http.server.ServerHttpRequest;

/**
 * @author Jon Brisbin
 */
public class LoggingResourcePostProcessor implements ResourcePostProcessor {

  private Logger logger = LoggerFactory.getLogger(LoggingResourcePostProcessor.class);

  @Override public Resource postProcess(ServerHttpRequest request, Resource r) {
    logger.info("    **** post-processing request: " + request + " with resource: " + r);
    return r;
  }

}
