package org.skillsdemo.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.skillsdemo.common.AppUtil;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Timesheet {
  private Integer id;
  private Integer personId;

  private String status;

  @JsonFormat(pattern = "MM/dd/yyyy")
  private LocalDate startDate;

  @JsonFormat(pattern = "MM/dd/yyyy")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDate endDate;

  private List<TimesheetLine> timesheetLines = new ArrayList<>();

  private String createdBy;
  private String updatedBy;
  private String submittedBy;
  private String approvedBy;

  @JsonFormat(pattern = "MM/dd/yyyy HH:mm:ss")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime createdOn;

  @JsonFormat(pattern = "MM/dd/yyyy HH:mm:ss")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime updatedOn;

  @JsonFormat(pattern = "MM/dd/yyyy HH:mm:ss")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime submittedOn;

  @JsonFormat(pattern = "MM/dd/yyyy HH:mm:ss")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime approvedOn;

  private String userComments;
  private String approverComments;

  // from person table
  private String lastName;
  private String firstName;
  
  // calculated in sql
  private Double totalHours;
  
  private Person person;

  public Timesheet(Integer id, LocalDate startDate, LocalDate endDate, Integer personId) {
    this.id = id;
    this.startDate = startDate;
    this.endDate = endDate;
    this.personId = personId;
  }

  public void addTimesheetLine(TimesheetLine line) {
    timesheetLines.add(line);
  }

  public List<TimesheetRow> getTimesheetRows() {
    List<TimesheetRow> list = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(this.timesheetLines)) {
      for (TimesheetLine line : this.timesheetLines) {
        TimesheetRow row = new TimesheetRow();
        row.setId(line.getId());
        row.setProjectId(line.getProjectId());
        row.setProjectName(line.getProjectName());
        List<TimeEntry> entries = line.getTimeEntries();
        if (CollectionUtils.isNotEmpty(entries)) {
          int idx = 0;
          for (TimeEntry entry : entries) {
            row.setHoursDay("hoursDay" + idx, entry.getEntryHours());
            idx++;
          }
        }
        list.add(row);
      }
    }
    return list;
  }

  public List<String> getDatesList() {
    List<String> list = new ArrayList<>();
    LocalDate startDate = getStartDate();
    if (startDate != null) {
      for (int i = 0; i < 7; i++) {
        LocalDate dt = startDate.plusDays(i);
        String dayName = dt.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        list.add(getDateAsStringShort(dt) + " " + dayName);
      }
    }
    return list;
  }

  public String getTimesheetDateRange() {
    return AppUtil.getFormattedDate(this.startDate)
        + " - "
        + AppUtil.getFormattedDate(this.endDate);
  }

  public String getFormattedSubmittedOn() {
    return AppUtil.getFormattedDateTime(this.submittedOn);
  }

  public String getFormattedApprovedOn() {
    return AppUtil.getFormattedDateTime(this.submittedOn);
  }

  public String getFullName() {
    return this.firstName + " " + this.lastName;
  }

  public static LocalDate getStartDateForPeriod(LocalDate periodDate) {
    LocalDate startDate = null;
    if (periodDate != null) {
      startDate = periodDate.with(DayOfWeek.MONDAY);
    }

    if (startDate == null) {
      throw new RuntimeException("Could not figure out startDate");
    }

    return startDate;
  }

  private String getDateAsStringShort(LocalDate date) {
    return date != null ? date.format(DateTimeFormatter.ofPattern("MM/dd")) : "";
  }
}
