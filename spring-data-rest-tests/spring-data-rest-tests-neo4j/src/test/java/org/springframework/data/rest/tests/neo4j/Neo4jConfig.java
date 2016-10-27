/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.tests.neo4j;

import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Luanne Misquitta
 */
@Configuration
@EnableNeo4jRepositories
@EnableTransactionManagement
public class Neo4jConfig {

    @Bean
    public org.neo4j.ogm.config.Configuration configuration() {
        final org.neo4j.ogm.config.Configuration configuration = new org.neo4j.ogm.config.Configuration();
        configuration.driverConfiguration().setDriverClassName(EmbeddedDriver.class.getName());
        return configuration;
    }

    @Bean
    public SessionFactory sessionFactory() {
        return new SessionFactory(configuration(), "org.springframework.data.rest.tests.neo4j");
    }

    @Bean
    public Neo4jTransactionManager transactionManager() {
        return new Neo4jTransactionManager(sessionFactory());
    }

    @Bean
    public TestDataPopulator testDataPopulator() {
        return new TestDataPopulator();
    }
}
