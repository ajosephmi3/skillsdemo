package org.skillsdemo.controller;

import java.util.List;

import javax.annotation.security.RolesAllowed;

import org.skillsdemo.model.GridSubmitData;
import org.skillsdemo.model.Project;
import org.skillsdemo.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RolesAllowed({"ROLE_ADMIN"})
public class ProjectController {
  @Autowired ProjectService projectService;

  @GetMapping("/project/projectlist")
  public String showProjectList() {
    return "project_list";
  }

  @GetMapping("/project/api/projects")
  @ResponseBody
  public List<Project> getAllProjects() {
    return projectService.getAllProjects();
  }

  @PostMapping(value = "/project/api/submit")
  @ResponseBody
  public GridSubmitData<Project> submit(@RequestBody GridSubmitData<Project> gridSubmitData) {
    return projectService.processProjectsSubmit(gridSubmitData);
  }
}
