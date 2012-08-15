package org.springframework.data.rest.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class used as a helper for those classes that need access to the exported repositories.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class RepositoryExporterSupport<S extends RepositoryExporterSupport<? super S>> {

  protected List<RepositoryExporter> repositoryExporters = Collections.emptyList();

  /**
   * Get a List of {@link RepositoryExporter}s.
   *
   * @return Exported {@link RepositoryExporter}s.
   */
  public List<RepositoryExporter> getRepositoryExporters() {
    return repositoryExporters;
  }

  /**
   * Set the List of {@link RepositoryExporter}s.
   *
   * @param repositoryExporters
   *     Export this {@link List} of {@link RepositoryExporter}s.
   */
  @Autowired(required = false)
  public void setRepositoryExporters(List<RepositoryExporter> repositoryExporters) {
    this.repositoryExporters = repositoryExporters;
  }

  /**
   * Get a List of {@link RepositoryExporter}s.
   *
   * @return Exported {@link RepositoryExporter}s.
   */
  public List<RepositoryExporter> repositoryExporters() {
    return repositoryExporters;
  }

  /**
   * Set the List of {@link RepositoryExporter}s.
   *
   * @param repositoryExporters
   *     Export this {@link List} of {@link RepositoryExporter}s.
   *
   * @return @this
   */
  @SuppressWarnings({"unchecked"})
  public S repositoryExporters(List<RepositoryExporter> repositoryExporters) {
    setRepositoryExporters(repositoryExporters);
    return (S)this;
  }

  /**
   * Set the {@link RepositoryExporter}s to use.
   *
   * @param repositoryExporter
   *
   * @return
   */
  @SuppressWarnings({"unchecked"})
  public S repositoryExporters(RepositoryExporter... repositoryExporter) {
    setRepositoryExporters(Arrays.asList(repositoryExporter));
    return (S)this;
  }

  /**
   * Does a Repository exist for this name?
   *
   * @param name
   *
   * @return true
   */
  public boolean hasRepositoryMetadataFor(String name) {
    try {
      return (null != repositoryMetadataFor(name));
    } catch(RepositoryNotFoundException ignored) {
      return false;
    }
  }

  /**
   * Is there a Repository responsible for this domain type?
   *
   * @param domainType
   *
   * @return
   */
  public boolean hasRepositoryMetadataFor(Class<?> domainType) {
    try {
      return (null != repositoryMetadataFor(domainType));
    } catch(RepositoryNotFoundException ignored) {
      return false;
    }
  }

  /**
   * Find {@link RepositoryMetadata} for the {@link org.springframework.data.repository.Repository} exported under this
   * name.
   *
   * @param name
   *     URL segment name.
   *
   * @return {@link RepositoryMetadata} or {@literal null} if none found.
   */
  @SuppressWarnings({"unchecked"})
  protected RepositoryMetadata repositoryMetadataFor(String name) {
    for(RepositoryExporter exporter : repositoryExporters) {
      RepositoryMetadata repoMeta = exporter.repositoryMetadataFor(name);
      if(null != repoMeta) {
        return repoMeta;
      }
    }
    throw new RepositoryNotFoundException("No repository found for name " + name);
  }

  /**
   * Find the {@link RepositoryMetadata} for the {@link org.springframework.data.repository.Repository} responsible for
   * the given domain type.
   *
   * @param domainType
   *     Type of the domain class.
   *
   * @return {@link RepositoryMetadata} or {@literal null} if none found.
   */
  @SuppressWarnings({"unchecked"})
  protected RepositoryMetadata repositoryMetadataFor(Class<?> domainType) {
    for(RepositoryExporter exporter : repositoryExporters) {
      RepositoryMetadata repoMeta = exporter.repositoryMetadataFor(domainType);
      if(null != repoMeta) {
        return repoMeta;
      }
    }
    throw new RepositoryNotFoundException("No repository found for type " + domainType.getName());
  }

  /**
   * Find the {@link RepositoryMetadata} for an attribute of an entity which is possibly managed by a {@link
   * org.springframework.data.repository.Repository}.
   *
   * @param attrMeta
   *     {@link AttributeMetadata} of a possibly-managed entity.
   *
   * @return {@link RepositoryMetadata} or {@literal null} if none found.
   */
  @SuppressWarnings({"unchecked"})
  protected RepositoryMetadata repositoryMetadataFor(AttributeMetadata attrMeta) {
    if(null != attrMeta.elementType()) {
      return repositoryMetadataFor(attrMeta.elementType());
    } else {
      return repositoryMetadataFor(attrMeta.type());
    }
  }

}
