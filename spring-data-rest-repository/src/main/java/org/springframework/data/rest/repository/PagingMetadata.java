package org.springframework.data.rest.repository;

/**
 * @author Jon Brisbin
 */
public class PagingMetadata {

  private int  number        = 0;
  private int  size          = 0;
  private int  totalPages    = 0;
  private long totalElements = 0;

  public PagingMetadata() {
  }

  public PagingMetadata(int number,
                        int size,
                        int totalPages,
                        long totalElements) {
    this.number = number;
    this.size = size;
    this.totalPages = totalPages;
    this.totalElements = totalElements;
  }

  public int getNumber() {
    return number;
  }

  public PagingMetadata setNumber(int number) {
    this.number = number;
    return this;
  }

  public int getSize() {
    return size;
  }

  public PagingMetadata setSize(int size) {
    this.size = size;
    return this;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public PagingMetadata setTotalPages(int totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  public long getTotalElements() {
    return totalElements;
  }

  public PagingMetadata setTotalElements(long totalElements) {
    this.totalElements = totalElements;
    return this;
  }

}
