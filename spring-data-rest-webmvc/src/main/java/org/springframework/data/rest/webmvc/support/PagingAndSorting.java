package org.springframework.data.rest.webmvc.support;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.config.RepositoryRestConfiguration;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Implementation of {@link Pageable} that is URL-aware.
 *
 * @author Jon Brisbin
 */
public class PagingAndSorting implements Pageable {

  private final RepositoryRestConfiguration config;
  private final PageRequest                 pageRequest;

  public PagingAndSorting(RepositoryRestConfiguration config,
                          PageRequest pageRequest) {
    this.config = config;
    this.pageRequest = pageRequest;
  }

  /**
   * Add the current sort parameters to the URI.
   *
   * @param urib
   *
   * @return
   */
  public PagingAndSorting addSortParameters(UriComponentsBuilder urib) {
    Sort sort = pageRequest.getSort();
    if(null != sort) {
      Iterator<Sort.Order> iter = sort.iterator();
      while(iter.hasNext()) {
        Sort.Order order = iter.next();
        urib.queryParam(config.getSortParamName(), order.getProperty());
        try {
          urib.queryParam(URLEncoder.encode(order.getProperty() + ".dir", "ISO-8859-1"),
                          order.getDirection().toString().toLowerCase());
        } catch(UnsupportedEncodingException ignored) {
          // this should never happen
        }
      }
    }
    return this;
  }

  @Override public int getPageNumber() {
    return pageRequest.getPageNumber();
  }

  @Override public int getPageSize() {
    return pageRequest.getPageSize();
  }

  @Override public int getOffset() {
    return pageRequest.getOffset();
  }

  @Override public Sort getSort() {
    return pageRequest.getSort();
  }

}
