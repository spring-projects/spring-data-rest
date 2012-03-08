# Spring Data Rest Exporter

The Spring Data Rest exporter is a project that aims to make it easy to expose various
services as Rest endpoints. The goal of the project is to provide a flexible and configurable
mechanism for writing simple services that can expose arbitrary services over HTTP.

The first exporter implemented is a JPA Repository exporter. This takes your JPA repositories
and front-ends them with HTTP, allowing you full CRUD capability over your entities, to include
managing associations.

### Installation

To use the Spring Data Rest exporter, first package your domain classes and repositories into a JAR
file. Include some Spring XML configuration files in the `META-INF/spring-data-rest` directory in
that JAR file (including an applicable EntityManager and DataSource).

You can either deploy this JAR file into your Servlet container in a "shared" configuration, or you
can add this JAR file (and any other application dependencies to the exporter WAR file's `WEB-INF/lib`
directory.
