# Spring Data Rest Exporter

The Spring Data Rest exporter is a project that aims to make it easy to expose various
services as Rest endpoints. The goal of the project is to provide a flexible and configurable
mechanism for writing simple services that can expose arbitrary services over HTTP.

The first exporter implemented is a JPA Repository exporter. This takes your JPA repositories
and front-ends them with HTTP, allowing you full CRUD capability over your entities, to include
managing associations.

### Installation

To use the Spring Data Rest exporter, first package your domain classes and repositories into a JAR
file. Include some Spring XML configuration files in the `META-INF/spring-data-rest` directory (the
file name should end with "-export.xml" to be picked up by the scanner). In that JAR file include
an applicable EntityManager and DataSource and Repository configuration (using the special JPA
Repository namespace).

You can either deploy this JAR file into your Servlet container in a "shared" configuration, or you
can add this JAR file (and any other application dependencies) to the exporter WAR file's `WEB-INF/lib`
directory.

Somewhere in the Spring configuration files you need to define a bean called "baseUri" that is a
`java.net.URI` and is the fully-qualified URI in which the exporter servlet has been deployed. In the
case of the sample below, the servlet is deployed to a context path of `/data`. Using the default
host and port settings, this yields a `baseUri` of `http://localhost:8080/data`. You'll want to change
this to reflect your deployment configuration.

### Sample Configuration

The configuration used in testing looks like this:

##### META-INF/spring-data-rest/shared.xml

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:jdbc="http://www.springframework.org/schema/jdbc"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc.xsd">

      <bean id="baseUri" class="java.net.URI">
        <constructor-arg value="http://localhost:8080/data"/>
      </bean>

      <bean id="entityManagerFactory" class="org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="jpaVendorAdapter">
          <bean class="org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter">
            <property name="generateDdl" value="true"/>
            <property name="database" value="HSQL"/>
          </bean>
        </property>
        <property name="persistenceUnitName" value="jpa.sample"/>
      </bean>

      <bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager">
        <property name="entityManagerFactory" ref="entityManagerFactory"/>
      </bean>

      <jdbc:embedded-database id="dataSource" type="HSQL"/>

    </beans>

##### META-INF/spring-data-rest/repositories-export.xml

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:jpa="http://www.springframework.org/schema/data/jpa"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/data/jpa http://www.springframework.org/schema/data/jpa/spring-jpa.xsd">

      <import resource="shared.xml"/>

      <!-- Search for Repositories under this package name -->
      <jpa:repositories base-package="org.springframework.data.rest.test.webmvc"/>

    </beans>

