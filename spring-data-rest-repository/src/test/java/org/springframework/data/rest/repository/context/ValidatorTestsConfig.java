package org.springframework.data.rest.repository.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.repository.RepositoryTestsConfig;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import({RepositoryTestsConfig.class})
public class ValidatorTestsConfig {

  @Bean public ValidatingRepositoryEventListener validatingListener() {
    return new ValidatingRepositoryEventListener();
  }

}
