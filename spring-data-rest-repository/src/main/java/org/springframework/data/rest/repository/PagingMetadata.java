package org.springframework.data.rest.repository;

/**
 * @author Jon Brisbin
 */
public class PagingMetadata {

  private int current = 0;
  private int total   = 0;

  public PagingMetadata(int current, int total) {
    this.current = current;
    this.total = total;
  }

  public int getCurrent() {
    return current;
  }

  public PagingMetadata setCurrent(int current) {
    this.current = current;
    return this;
  }

  public int getTotal() {
    return total;
  }

  public PagingMetadata setTotal(int total) {
    this.total = total;
    return this;
  }

}
