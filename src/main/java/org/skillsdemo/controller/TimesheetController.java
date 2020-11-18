package org.skillsdemo.controller;

import static org.skillsdemo.common.AppConstants.TURBOLINKS_REDIRECT_LOCATION;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.skillsdemo.common.AppUtil;
import org.skillsdemo.common.CacheHelper;
import org.skillsdemo.common.JsonUtil;
import org.skillsdemo.model.Timesheet;
import org.skillsdemo.model.TimesheetPayload;
import org.skillsdemo.model.TimesheetPeriod;
import org.skillsdemo.model.TimesheetRow;
import org.skillsdemo.service.PersonService;
import org.skillsdemo.service.TimesheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TimesheetController {
  @Autowired private TimesheetService timesheetService;
  @Autowired private PersonService personService;
  @Autowired private CacheHelper cacheHelper;

  @GetMapping("/timesheet/period")
  public String showTimesheetPeriodPage() {
    return "timesheet_period";
  }

  @GetMapping(value = "/timesheet/{timesheetId}")
  public String showTimesheetPage(@PathVariable("timesheetId") Integer timesheetId, Model model) {
    Timesheet timesheet = timesheetService.getTimesheet(timesheetId);

    model.addAttribute("timesheet", timesheet);
    model.addAttribute("editable", timesheetService.isEditable(timesheet));
    model.addAttribute("allowApproval", timesheetService.allowApproval(timesheet));

    model.addAttribute("datesList", JsonUtil.toJson(timesheet.getDatesList()));
    model.addAttribute(
        "userProjectsDropdownList",
        JsonUtil.toJson(
            personService.getPersonProjectsDropdownList(AppUtil.getLoggedInPersonId())));

    return "timesheet";
  }

  @GetMapping(value = "/timesheet/mytimesheetlist")
  public String showMyTimesheetListPage() {
    return "timesheet_list";
  }

  @RolesAllowed({"ROLE_MANAGER"})
  @GetMapping(value = "/timesheet/employeetimesheetlist")
  public String showEmployeeTimesheetListPage() {
    return "employee_timesheet_list";
  }

  @PostMapping(value = "/timesheet/api/period/next")
  @ResponseBody
  public String periodNext(
      @Valid @RequestBody TimesheetPeriod period, HttpServletResponse response) {
    Integer timesheetId = timesheetService.createTempTimesheet(period.getPeriodDate());
    String url = "/timesheet/" + timesheetId;
    response.addHeader(TURBOLINKS_REDIRECT_LOCATION, url);
    return "";
  }

  @GetMapping(value = "/timesheet/api/timesheetId/{timesheetId}/timesheetrows")
  @ResponseBody
  public List<TimesheetRow> getTimesheetRows(
      @PathVariable("timesheetId") Integer timesheetId, HttpServletResponse response) {
    List<TimesheetRow> list = timesheetService.getTimesheetRows(timesheetId);

    // Handle any redirect messages so that UI can appropriately show the message after a redirect
    String redirectMessage = cacheHelper.getAndEvictRedirectMessageFromCache(timesheetId + "");
    if (StringUtils.isNotBlank(redirectMessage)) {
      response.setHeader("redirect_message", redirectMessage);
    }

    return list;
  }

  @GetMapping(value = "/timesheet/api/mytimesheets")
  @ResponseBody
  public List<Timesheet> getMyTimesheets() {
    return timesheetService.getMyTimesheets();
  }

  @GetMapping(value = "/timesheet/api/employeetimesheets")
  @ResponseBody
  public List<Timesheet> getEmployeeTimesheets() {
    return timesheetService.getEmployeeTimesheets();
  }

  @PostMapping(value = "/timesheet/api/timesheetId/{timesheetId}/submit")
  @ResponseBody
  public TimesheetPayload timesheetSave(
      @PathVariable("timesheetId") Integer timesheetId,
      @RequestBody TimesheetPayload payload,
      HttpServletResponse response) {

    Timesheet timesheet = timesheetService.saveTimesheet(timesheetId, payload);
    if (!"Save".equals(payload.getAction())) {
      String url = "/timesheet/" + timesheetId;
      response.addHeader(TURBOLINKS_REDIRECT_LOCATION, url);
      cacheHelper.putRedirectMessageIntoCache(
          timesheetId + "", payload.getAction() + " Successfull");

    } else {
      if (!"Draft".equals(timesheet.getStatus())) {
        String url = "/timesheet/" + timesheetId;
        response.addHeader(TURBOLINKS_REDIRECT_LOCATION, url);
        cacheHelper.putRedirectMessageIntoCache(
            timesheetId + "", payload.getAction() + " Successfull");
      }
    }
    return payload;
  }
}
