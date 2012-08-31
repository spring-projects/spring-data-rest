package org.springframework.data.rest.test.webmvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.RepositoryRestMvcConfiguration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Jon Brisbin
 */
@Configuration
@Import(RepositoryRestMvcConfiguration.class)
@ComponentScan(basePackageClasses = ApplicationConfig.class)
@EnableJpaRepositories
@EnableTransactionManagement
public class ApplicationConfig {

  @Bean public DataSource dataSource() {
    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    return builder.setType(EmbeddedDatabaseType.HSQL).build();
  }

  @Bean public EntityManagerFactory entityManagerFactory() {
    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setDatabase(Database.HSQL);
    vendorAdapter.setGenerateDdl(true);

    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan(getClass().getPackage().getName());
    factory.setDataSource(dataSource());

    factory.afterPropertiesSet();

    return factory.getObject();
  }

  @Bean public JpaDialect jpaDialect() {
    return new HibernateJpaDialect();
  }

  @Bean public PlatformTransactionManager transactionManager() {
    JpaTransactionManager txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(entityManagerFactory());
    return txManager;
  }

  @Bean public TestRepositoryEventListener testRepositoryEventListener() {
    return new TestRepositoryEventListener();
  }

  @SuppressWarnings({"unchecked"})
  @Bean public ConversionService customConversionService() {
    DefaultFormattingConversionService cs = new DefaultFormattingConversionService();
    cs.addConverter(new Converter<String[], List<Long>>() {
      @Override public List<Long> convert(String[] source) {
        List<Long> longs = new ArrayList<Long>(source.length);
        for(String s : source) {
          longs.add(Long.parseLong(s));
        }
        return longs;
      }
    });
    //    cs.addConverter(new Converter<Person, Resource>() {
    //      @Override public Resource convert(Person person) {
    //        Map<String, Object> m = new HashMap<String, Object>();
    //        m.put("name", person.getName());
    //        CustomResource r = new CustomResource(m);
    //        r.add(new Link("http://localhost:8080/people/1", "self"));
    //        return r;
    //      }
    //    });
    return cs;
  }

  @Bean public ResourceProcessor<Resource<Person>> personProcessor() {
    return new ResourceProcessor<Resource<Person>>() {
      @Override public Resource<Person> process(Resource<Person> resource) {
        System.out.println("\t***** ResourceProcessor for Person: " + resource);
        resource.add(new Link("http://localhost:8080/people", "added-link"));
        return resource;
      }
    };
  }

}
