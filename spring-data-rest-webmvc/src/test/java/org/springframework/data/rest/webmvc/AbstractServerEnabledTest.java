package org.springframework.data.rest.webmvc;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author Jon Brisbin
 */
public abstract class AbstractServerEnabledTest {

  private Server server;

  @Before
  public void setup() {
    if(null == server) {
      server = new Server(0);

    }
  }

}
