package org.springframework.data.rest.repository.domain.mongodb;

import java.net.UnknownHostException;

import com.mongodb.Mongo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * @author Jon Brisbin
 */
@Configuration
@ComponentScan(basePackageClasses = {MongoDbRepositoryConfig.class})
@EnableMongoRepositories
public class MongoDbRepositoryConfig {

  @Bean public MongoDbFactory mongoDbFactory() throws UnknownHostException {
    return new SimpleMongoDbFactory(new Mongo("localhost"), "spring-data-rest");
  }

  @Bean public MongoTemplate mongoTemplate() throws UnknownHostException {
    return new MongoTemplate(mongoDbFactory());
  }

}
