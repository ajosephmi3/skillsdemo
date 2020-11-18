package org.skillsdemo.model;

import lombok.Data;

@Data
public class TimesheetPayload {
  private String action;
  private String userComments;
  private String approverComments;
  private GridSubmitData<TimesheetRow> gridSubmitData;
}
