package org.springframework.data.rest.webmvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.data.rest.webmvc.gemfire.GemfireRepositoryConfig;
import org.springframework.data.rest.webmvc.jpa.JpaRepositoryConfig;
import org.springframework.data.rest.webmvc.mongodb.MongoDbRepositoryConfig;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import({ JpaRepositoryConfig.class, MongoDbRepositoryConfig.class, GemfireRepositoryConfig.class })
public class RepositoryRestMvcTestConfig extends RepositoryRestMvcConfiguration {}
