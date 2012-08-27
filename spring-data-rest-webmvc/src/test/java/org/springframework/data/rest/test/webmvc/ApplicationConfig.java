package org.springframework.data.rest.test.webmvc;

import java.util.ArrayList;
import java.util.List;
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
    return cs;
  }

}
