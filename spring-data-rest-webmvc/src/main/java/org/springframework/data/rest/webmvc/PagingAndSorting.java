package org.springframework.data.rest.webmvc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jon Brisbin
 */
public class PagingAndSorting implements Pageable {

  final String pageParameter;
  final String limitParameter;
  final String orderParameter;
  private final PageRequest pageRequest;

  public PagingAndSorting(String pageParameter,
                          String limitParameter,
                          String orderParameter,
                          PageRequest pageRequest) {
    this.pageParameter = pageParameter;
    this.limitParameter = limitParameter;
    this.orderParameter = orderParameter;
    this.pageRequest = pageRequest;
  }

  public PagingAndSorting addSortParameters(UriComponentsBuilder urib) {
    Sort sort = pageRequest.getSort();
    if (null != sort) {
      Iterator<Sort.Order> iter = sort.iterator();
      while (iter.hasNext()) {
        Sort.Order order = iter.next();
        urib.queryParam(orderParameter, order.getProperty());
        try {
          urib.queryParam(URLEncoder.encode(order.getProperty() + ".dir", "ISO-8859-1"),
                          order.getDirection().toString().toLowerCase());
        } catch (UnsupportedEncodingException ignored) {
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
