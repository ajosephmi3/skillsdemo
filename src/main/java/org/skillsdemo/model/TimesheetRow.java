package org.skillsdemo.model;

import org.apache.commons.lang3.StringUtils;
import org.skillsdemo.common.AppUtil;

import lombok.Data;

@Data
public class TimesheetRow {
  private Integer id; // the timesheetLineId
  private Integer projectId;
  private String projectName;
  private Double hoursDay0;
  private Double hoursDay1;
  private Double hoursDay2;
  private Double hoursDay3;
  private Double hoursDay4;
  private Double hoursDay5;
  private Double hoursDay6;

  // Kendo will send the fields which are dirty for an update.
  private String dirtyFieldsForRow;

  public Double getHoursDay(String propertyName) {
    if (!StringUtils.startsWith(propertyName, "hoursDay")) {
      throw new IllegalArgumentException("Invalid property name " + propertyName);
    }

    Object val = AppUtil.getProperty(this, propertyName);
    return val != null ? (Double) val : null;
  }

  public void setHoursDay(String propertyName, Double val) {
    if (!StringUtils.startsWith(propertyName, "hoursDay")) {
      throw new IllegalArgumentException("Invalid property name " + propertyName);
    }
    AppUtil.setProperty(this, propertyName, val);
  }
}
