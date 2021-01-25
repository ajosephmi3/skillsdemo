package org.jdbctemplatemapper.dbutil;

import lombok.Data;

@Data
public class SelectMapper<T> {
  private Class<T> clazz;
  private String sqlColumnPrefix;

  public SelectMapper(Class<T> clazz) {
    this.clazz = clazz;
    this.sqlColumnPrefix = "";
  }

  public SelectMapper(Class<T> clazz, String sqlColumnPrefix) {
    this.clazz = clazz;
    this.sqlColumnPrefix = sqlColumnPrefix;
  }
}
