package org.skillsdemo.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeEntry {
  private Integer timesheetLineId;
  private LocalDate entryDate;
  private Double entryHours;

  public TimeEntry() {}
}
