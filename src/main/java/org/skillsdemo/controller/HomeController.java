package org.skillsdemo.controller;

import org.skillsdemo.common.AppUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String home() {
	if(AppUtil.getLoggedInPerson().isManager()) {
		return "redirect:/timesheet/employeetimesheetlist";
	}
    return "redirect:/timesheet/mytimesheetlist";
  }

  @GetMapping("/design")
  public String design() {
    return "design";
  }
}
