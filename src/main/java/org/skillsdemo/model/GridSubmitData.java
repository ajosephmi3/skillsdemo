package org.skillsdemo.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
/*
 * Kendo grid sends data in separate array for new/update/deleted records
 * 
 * The controllers bind that incoming kendo data to this class.
 */
@Data
public class GridSubmitData<T> {
  private List<T> created = new ArrayList<>();
  private List<T> updated = new ArrayList<>();
  private List<T> destroyed = new ArrayList<>();

  @JsonIgnore
  public List<T> getNewAndUpdatedRows() {
    List<T> list = new ArrayList<>();
    list.addAll(this.created);
    list.addAll(this.updated);
    return list;
  }
  
  @JsonIgnore
  public List<T> getAllRows() {
    List<T> list = new ArrayList<>();
    list.addAll(this.created);
    list.addAll(this.updated);
    list.addAll(this.destroyed);
    return list;
  }
  
  @JsonIgnore
  public List<T> getUpdatedAndDeletedRows() {
    List<T> list = new ArrayList<>();
    list.addAll(this.updated);
    list.addAll(this.destroyed);
    return list;
  }
  
}
