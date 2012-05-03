# Validation in Spring Data REST

Integrating validation with the Spring Data REST Exporter is as easy as simply defining an instance of a [Validator](http://static.springsource.org/spring/docs/3.1.x/javadoc-api/org/springframework/validation/Validator.html). There is an ApplicationListener that that looks for these Validator instances on startup and wires them to the correct RepositoryEvent based on the bean name.

For example, to validate entities before they are saved to the Repository, you only need to define a Validator instance in your ApplicationContext with a name that starts with "beforeSave".

    <!--
      This validator will be picked up automatically. The default configuration is to look at the bean name
      and figure out what event you're interested in. This validator is interested in 'beforeSave' events
      because the word 'beforeSave' appears in the first part of the bean name. It recognizes:

        - beforeSave
        - afterSave
        - beforeDelete
        - afterDelete
        - beforeLinkSave
        - afterLinkSave

      What you put after that doesn't matter, you just need to make the bean name unique, of course.
    -->
    <bean id="beforeSavePersonValidator" class="com.mycompany.domain.validators.PersonValidator"/>

All the events dicussed in [Handling ApplicationEvents in the REST Exporter](wiki/Handling-ApplicationEvents-in-the-REST-Exporter) can be validated.

If any errors are found during validation, a [RepositoryConstraintViolationException](blob/master/spring-data-rest-repository/src/main/java/org/springframework/data/rest/repository/RepositoryConstraintViolationException.java) will be thrown, resulting in a 400 Bad Request.

### Advanced Configuration

If you need a little more control over how the Validators are wired, you can instantiate a [ValidatingRepositoryEventListener](blob/master/spring-data-rest-repository/src/main/java/org/springframework/data/rest/repository/context/ValidatingRepositoryEventListener.java) yourself and use a Map of Validators to their event names:

    <bean class="org.springframework.data.rest.repository.context.ValidatingRepositoryEventListener">
      <property name="validators">
        <map>
          <entry key="beforeSave">
            <list>
              <bean class="org.springframework.data.rest.test.webmvc.PersonValidator"/>
            </list>
          </entry>
        </map>
      </property>
    </bean>
