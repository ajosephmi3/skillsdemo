package org.skillsdemo.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;

import org.skillsdemo.common.FixedDropdowns;
import org.skillsdemo.common.JsonUtil;
import org.skillsdemo.common.Page;
import org.skillsdemo.model.GridSubmitData;
import org.skillsdemo.model.Person;
import org.skillsdemo.model.PersonProject;
import org.skillsdemo.model.ValueAndText;
import org.skillsdemo.service.PersonService;
import org.skillsdemo.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RolesAllowed({"ROLE_ADMIN"})
public class PersonController {
  @Autowired PersonService personService;
  @Autowired ProjectService projectService;

  @GetMapping("/person/personlist")
  public String showPersonListPage(Model model) {
    // only admins have access to this controller so editable is always true
    model.addAttribute("editable", true);
    model.addAttribute(
        "projectDropdownList", JsonUtil.toJson(projectService.getProjectDropdownList()));
    model.addAttribute("roleDropdownList", JsonUtil.toJson(FixedDropdowns.getRoleDropdownList()));
    model.addAttribute(
        "accountStatusDropdownList",
        JsonUtil.toJson(FixedDropdowns.getAccountStatusDropdownList()));
    model.addAttribute(
        "reportsToDropdownList", JsonUtil.toJson(personService.getManagerDropdownList()));
    return "person_list";
  }

  @GetMapping("/person/api/persons")
  @ResponseBody
  public Page<Person> getAllPersons(@RequestParam(required = false) Map<String, String> params) {
    return personService.fetchPaginatedPersons(params);
  }

  @PostMapping(value = "/person/api/submit")
  @ResponseBody
  public GridSubmitData<Person> submit(@RequestBody GridSubmitData<Person> gridSubmitData) {
    personService.processPersonsSubmit(gridSubmitData);
    return gridSubmitData;
  }

  @GetMapping("/person/api/person/{personId}/projects")
  @ResponseBody
  public List<PersonProject> getProjectsOfUser(@PathVariable("personId") Integer personId) {
    return personService.getPersonProjects(personId);
  }

  @PostMapping(value = "/person/api/person/{personId}/projectssubmit")
  @ResponseBody
  public GridSubmitData<PersonProject> processPersonProjectsSubmit(
      @PathVariable("personId") Integer personId,
      @RequestBody GridSubmitData<PersonProject> gridSubmitData) {
    return personService.processPersonProjectsSubmit(personId, gridSubmitData);
  }
  
  @GetMapping("/person/api/personsautocomplete")
  @ResponseBody
  public List<ValueAndText> findManagerAutocomplete(@RequestParam(required = false) Map<String, String> params) {
	String autocompleteValue = params.get("filter[filters][0][value]");
    return personService.findManageAutocomplete(autocompleteValue);
  }
  
}
