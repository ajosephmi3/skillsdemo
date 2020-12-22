package org.skillsdemo.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimesheetLine {
  private Integer id;
  private Integer timesheetId;
  private Integer projectId;
  private String projectName;

  private List<TimeEntry> timeEntries = new ArrayList<>();
  
  private Project project;

  public TimesheetLine(Integer id, Integer timesheetId, Integer projectId) {
    this.id = id;
    this.timesheetId = timesheetId;
    this.projectId = projectId;
  }
}
