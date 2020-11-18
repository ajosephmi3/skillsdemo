package org.skillsdemo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.skillsdemo.common.AppUtil;
import org.skillsdemo.common.CacheHelper;
import org.skillsdemo.common.JsonUtil;
import org.skillsdemo.dao.PersonDao;
import org.skillsdemo.dao.TimesheetDao;
import org.skillsdemo.exception.CustomValidationException;
import org.skillsdemo.exception.FineGrainedAuthorizationException;
import org.skillsdemo.model.Person;
import org.skillsdemo.model.PersonProject;
import org.skillsdemo.model.TimeEntry;
import org.skillsdemo.model.Timesheet;
import org.skillsdemo.model.TimesheetLine;
import org.skillsdemo.model.TimesheetPayload;
import org.skillsdemo.model.TimesheetRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TimesheetService {
  @Autowired private TimesheetDao timesheetDao;
  @Autowired private PersonDao personDao;
  @Autowired private CacheHelper cacheHelper;

  public List<Timesheet> getMyTimesheets() {
    return timesheetDao.getMyTimesheets(AppUtil.getLoggedInPersonId());
  }

  public List<Timesheet> getEmployeeTimesheets() {
    return timesheetDao.getEmployeeTimesheets(AppUtil.getLoggedInPersonId());
  }

  @Transactional
  public Timesheet saveTimesheet(Integer timesheetId, TimesheetPayload payload) {
    Integer personId = AppUtil.getLoggedInPersonId();
    boolean isNewTimesheet = false;
    Timesheet timesheet = timesheetDao.fetchFullTimesheet(timesheetId);
    if (timesheet == null) {
      timesheet = cacheHelper.getTempTimesheetFromCache(timesheetId + "-" + personId);
      if (timesheet == null) {
        throw new RuntimeException("Timesheet not found for id:" + timesheetId);
      }
      isNewTimesheet = true;
    }

    authorize(timesheet);

    String userAction = payload.getAction();
    String status = getStatusForAction(userAction);

    if ("Save".equals(userAction) || "Submit".equals(userAction)) {
      validateTimesheet(timesheet, payload);
    }

    if (isNewTimesheet) {
      timesheet.setStatus(status);
      if ("Submit".equals(userAction)) {
        timesheet.setSubmittedOn(LocalDateTime.now());
        timesheet.setSubmittedBy(AppUtil.getLoggedInPerson().getFullName());
      }

      timesheet.setUserComments(payload.getUserComments());
      timesheetDao.insert(timesheet);
      cacheHelper.evictTempTimesheetFromCache(timesheetId + "-" + personId);
    }

    if ("Save".equals(userAction) || "Submit".equals(userAction)) {
      List<TimesheetRow> newRows = payload.getGridSubmitData().getCreated();
      List<TimesheetRow> updatedRows = payload.getGridSubmitData().getUpdated();
      List<TimesheetRow> deletedRows = payload.getGridSubmitData().getDestroyed();

      processNewRows(newRows, timesheet);
      processUpdatedRows(updatedRows, timesheet);
      processDeletedRows(deletedRows, timesheet);
    }

    if (!isNewTimesheet) {
      timesheet.setStatus(status);
      if ("Submit".equals(userAction)) {
        timesheet.setSubmittedOn(LocalDateTime.now());
        timesheet.setSubmittedBy(AppUtil.getLoggedInPerson().getFullName());
        timesheet.setUserComments(payload.getUserComments());
      } else if ("Approve".equals(userAction)) {
        timesheet.setApprovedOn(LocalDateTime.now());
        timesheet.setApprovedBy(AppUtil.getLoggedInPerson().getFullName());
        timesheet.setApproverComments(payload.getApproverComments());
      } else if ("Reject".equals(userAction)) {
        timesheet.setApproverComments(payload.getApproverComments());
      } else {
        // SAVE
        timesheet.setUserComments(payload.getUserComments());
      }
      timesheetDao.update(timesheet);
    }
    // log.debug("{} {} {}", newRows, updatedRows, deletedRows);

    return timesheet;
  }

  public Timesheet getTimesheet(Integer timesheetId) {
    Timesheet timesheet = timesheetDao.fetchFullTimesheet(timesheetId);
    if (timesheet == null) {
      timesheet =
          cacheHelper.getTempTimesheetFromCache(timesheetId + "-" + AppUtil.getLoggedInPersonId());
      if (timesheet == null) {
        throw new RuntimeException("Timesheet not found for id:" + timesheetId);
      }
    }
    authorize(timesheet);

    return timesheet;
  }

  public List<TimesheetRow> getTimesheetRows(Integer timesheetId) {
    List<TimesheetRow> rows = new ArrayList<>();
    Timesheet timesheet = timesheetDao.fetchFullTimesheet(timesheetId);
    if (timesheet != null) {
      rows = timesheet.getTimesheetRows();
    }
    log.debug("rows:{}", rows);
    return rows;
  }

  public Integer createTempTimesheet(LocalDate periodDate) {
    Integer personId = AppUtil.getLoggedInPersonId();
    LocalDate startDate = Timesheet.getStartDateForPeriod(periodDate);
    if (timesheetDao.timesheetExists(personId, startDate)) {
      throw new CustomValidationException(
          "Timesheet for week starting "
              + AppUtil.getFormattedDate(startDate)
              + " already exists.");
    }
    Integer id = timesheetDao.getNextSequence("timesheet_id_seq");
    Timesheet timesheet = new Timesheet(id, startDate, startDate.plusDays(7), personId);
    timesheet.setLastName(AppUtil.getLoggedInPerson().getLastName());
    timesheet.setFirstName(AppUtil.getLoggedInPerson().getFirstName());
    timesheet.setStatus("Draft");
    cacheHelper.putTempTimesheetIntoCache(id + "-" + personId, timesheet);
    return id;
  }

  private void processNewRows(List<TimesheetRow> newRows, Timesheet timesheet) {
    List<TimesheetLine> timesheetLines = new ArrayList<>();
    List<TimeEntry> timeEntries = new ArrayList<>();
    for (TimesheetRow row : newRows) {
      Integer timesheetLineId = timesheetDao.getNextSequence("timesheet_line_id_seq");

      TimesheetLine timesheetLine =
          new TimesheetLine(timesheetLineId, timesheet.getId(), row.getProjectId());
      timesheetLines.add(timesheetLine);

      LocalDate startDt = timesheet.getStartDate();
      Integer dayCount = getTimesheetDayCount();
      for (int i = 0; i < dayCount; i++) {
        timeEntries.add(
            new TimeEntry(timesheetLineId, startDt.plusDays(i), row.getHoursDay("hoursDay" + i)));
      }
      // need this so kendo knows the line was successfully inserted.
      row.setId(timesheetLineId);
    }

    if (CollectionUtils.isNotEmpty(timesheetLines)) {
      for (TimesheetLine timesheetLine : timesheetLines) {
        timesheetDao.insert(timesheetLine);
      }
      if (CollectionUtils.isNotEmpty(timeEntries)) {
        timesheetDao.batchInsertTimeEntries(timeEntries, 100);
      }
    }
  }

  public boolean isEditable(Timesheet timesheet) {
    boolean editable = false;
    if ("Draft".equals(timesheet.getStatus())) {
      if (timesheet.getPersonId().equals(AppUtil.getLoggedInPersonId())) {
        editable = true;
      }
    } else if ("Rejected".equals(timesheet.getStatus())) {
      if (timesheet.getPersonId().equals(AppUtil.getLoggedInPersonId())) {
        editable = true;
      }
    }
    return editable;
  }

  public boolean allowApproval(Timesheet timesheet) {
    boolean allowApproval = false;
    if ("Pending Approval".equals(timesheet.getStatus())) {
      Person timesheetPerson = personDao.findById(timesheet.getPersonId(), Person.class);
      if (AppUtil.getLoggedInPersonId().equals(timesheetPerson.getReportsToId())) {
        allowApproval = true;
      }
    }
    return allowApproval;
  }

  private void processUpdatedRows(List<TimesheetRow> updatedRows, Timesheet timesheet) {
    List<TimesheetLine> timesheetLines = new ArrayList<>();
    List<TimeEntry> timeEntries = new ArrayList<>();
    for (TimesheetRow row : updatedRows) {
      TimesheetLine timesheetLine =
          new TimesheetLine(row.getId(), timesheet.getId(), row.getProjectId());
      timesheetLines.add(timesheetLine);

      LocalDate startDt = timesheet.getStartDate();
      Integer dayCount = getTimesheetDayCount();
      for (int i = 0; i < dayCount; i++) {
        timeEntries.add(
            new TimeEntry(row.getId(), startDt.plusDays(i), row.getHoursDay("hoursDay" + i)));
      }
    }

    if (CollectionUtils.isNotEmpty(timesheetLines)) {
      for (TimesheetLine timesheetLine : timesheetLines) {
        if (timesheetDao.updateTimesheetLine(timesheetLine) != 1) {
          // should only happen if someone is user is hacking system with postman etc
          throw new RuntimeException("update failed for timesheetLine:" + timesheetLine);
        }
      }
      if (CollectionUtils.isNotEmpty(timeEntries)) {
        timesheetDao.batchUpdateTimeEntries(timeEntries, 100);
      }
    }
  }

  private void processDeletedRows(List<TimesheetRow> deletedRows, Timesheet timesheet) {
    for (TimesheetRow row : deletedRows) {
      timesheetDao.deleteTimeEntriesByLineId(row.getId());
      if (timesheetDao.deleteTimesheetLine(timesheet.getId(), row.getId()) != 1) {
        // should only happen if someone is user is hacking system with postman etc
        throw new RuntimeException(
            "delete failed for timesheeLine:" + row.getId() + " timesheetId:" + timesheet.getId());
      }
    }
  }

  /*
   * 1) Makes sure that lines do not have duplicate projects
   * 2) Checks the total for a day is not over 24 hours.
   *
   * While doing the validation it also checks for data integrity to prevent rogue
   * users from hacking the system.
   */
  private void validateTimesheet(Timesheet timesheet, TimesheetPayload payload) {
    // Create a deep clone of timesheet and modify it with the user inputs.
    Timesheet timesheetClone = JsonUtil.toObject(JsonUtil.toJson(timesheet), timesheet.getClass());

    LocalDate startDt = timesheet.getStartDate();
    Integer dayCount = getTimesheetDayCount();

    // get rid of the deleted lines from clone
    for (TimesheetRow row : payload.getGridSubmitData().getDestroyed()) {
      ListIterator<TimesheetLine> iter = timesheetClone.getTimesheetLines().listIterator();
      while (iter.hasNext()) {
        if (iter.next().getId().equals(row.getId())) {
          iter.remove();
        }
      }
    }

    // update lines and entries on clone
    for (TimesheetRow row : payload.getGridSubmitData().getUpdated()) {
      for (TimesheetLine timesheetLine : timesheetClone.getTimesheetLines()) {
        if (timesheetLine.getId().equals(row.getId())) {
          if (row.getProjectId() == null) {
            throw new CustomValidationException("Save failed. Project is required");
          }
          timesheetLine.setProjectId(row.getProjectId());
          List<TimeEntry> timeEntries = timesheetLine.getTimeEntries();
          for (int i = 0; i < dayCount; i++) {
            LocalDate date = startDt.plusDays(i);
            Double hours = row.getHoursDay("hoursDay" + i);
            for (TimeEntry entry : timeEntries) {
              if (entry.getEntryDate().equals(date)) {
                entry.setEntryHours(hours);
              }
            }
          }
        }
      }
    }

    // add new lines and entries to clone
    for (TimesheetRow row : payload.getGridSubmitData().getCreated()) {
      if (row.getProjectId() == null) {
        throw new CustomValidationException("Save failed. Project is required");
      }
      TimesheetLine timesheetLine = new TimesheetLine(null, timesheet.getId(), row.getProjectId());
      timesheetClone.getTimesheetLines().add(timesheetLine);
      List<TimeEntry> timeEntries = timesheetLine.getTimeEntries();
      for (int i = 0; i < dayCount; i++) {
        timeEntries.add(
            new TimeEntry(
                timesheetLine.getId(), startDt.plusDays(i), row.getHoursDay("hoursDay" + i)));
      }
    }

    // get projects for the timesheet user.
    List<PersonProject> personProjects = personDao.fetchPersonProjects(timesheet.getPersonId());
    // map with projectId and projectName
    Map<Integer, String> projectMap =
        personProjects
            .stream()
            .collect(Collectors.toMap(PersonProject::getProjectId, PersonProject::getProjectName));

    // Now that timesheetClone object has been fully populated with the user inputs, check for
    // duplicate projectIds
    // map key - projectId, value - count
    Map<Integer, Long> projectIdCountMap =
        timesheetClone
            .getTimesheetLines()
            .stream()
            .collect(Collectors.groupingBy(TimesheetLine::getProjectId, Collectors.counting()));
    for (Integer projectId : projectIdCountMap.keySet()) {
      if (projectMap.get(projectId) == null) {
        // should never happen unless someone is hacking into system using postman etc.
        throw new FineGrainedAuthorizationException(
            "Project mismatch. " + JsonUtil.toJson(payload));
      }

      if (projectIdCountMap.get(projectId) > 1) {
        throw new CustomValidationException(
            "Save failed. Duplicate line entries for project " + projectMap.get(projectId));
      }
    }

    // check whether day total exceeds 24
    // Map key - date, value - total hours for day
    Map<LocalDate, Double> dateHours = new HashMap<>();
    for (TimesheetLine line : timesheetClone.getTimesheetLines()) {
      for (TimeEntry entry : line.getTimeEntries()) {
        LocalDate date = entry.getEntryDate();
        Double hours = entry.getEntryHours();
        if (dateHours.containsKey(date)) {
          if (hours != null) {
            dateHours.put(date, dateHours.get(date) + hours);
          }
        } else {
          if (hours != null) {
            dateHours.put(date, entry.getEntryHours());
          }
        }
      }
    }

    for (LocalDate date : dateHours.keySet()) {
      Double hours = dateHours.get(date);
      if (hours != null && hours > 24.0) {
        throw new CustomValidationException(
            "Save failed. Total hours entered for date "
                + AppUtil.getFormattedDate(date)
                + " exceeds 24 hours.");
      } else if (hours != null && hours < 0.0) {
        throw new RuntimeException("hours cannot be negative");
      }
    }
  }

  public void authorize(Timesheet timesheet) {
    boolean authorized = false;
    Person person = AppUtil.getLoggedInPerson();
    if (person.getId().equals(timesheet.getPersonId())) {
      authorized = true;
    } else if (person.isManager()) {
      Person timesheetPerson = personDao.findById(timesheet.getPersonId(), Person.class);
      if (person.getId().equals(timesheetPerson.getReportsToId())) {
        authorized = true;
      }
    } else if (person.isAdmin()) {
      authorized = true;
    }

    if (!authorized) {
      throw new FineGrainedAuthorizationException(
          "Timesheet authorization failed: timesheetId:"
              + timesheet.getId()
              + " userId:"
              + AppUtil.getLoggedInPersonId());
    }
  }

  private String getStatusForAction(String action) {
    String status = "";
    switch (action) {
      case "Submit":
        status = "Pending Approval";
        break;
      case "Approve":
        status = "Approved";
        break;
      case "Reject":
        status = "Rejected";
        break;
      default:
        status = "Draft";
    }

    return status;
  }

  // TODO Make this dynamic in future depending on timesheet type WEEKLY/BI-MONTHLY/MONTHLY
  private Integer getTimesheetDayCount() {
    return 7;
  }
}
