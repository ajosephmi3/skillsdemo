package org.skillsdemo.service;

import java.util.ArrayList;
import java.util.List;

import org.skillsdemo.common.ValidatorUtil;
import org.skillsdemo.dao.ProjectDao;
import org.skillsdemo.exception.CustomValidationException;
import org.skillsdemo.model.GridSubmitData;
import org.skillsdemo.model.Project;
import org.skillsdemo.model.ValueAndText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
  @Autowired ProjectDao projectDao;
  @Autowired ValidatorUtil validatorUtil;

  public List<Project> getAllProjects() {
    return projectDao.findAllOrderByName();
  }

  public List<ValueAndText> getProjectDropdownList() {
    List<Project> list = getAllProjects();
    List<ValueAndText> dropdownList = new ArrayList<>();
    for (Project project : list) {
      dropdownList.add(new ValueAndText(project.getId().toString(), project.getName()));
    }

    return dropdownList;
  }

  @Transactional
  public GridSubmitData<Project> processProjectsSubmit(GridSubmitData<Project> gridSubmitData) {
    // if validation fails an exception is thrown and framework code will handle it.
    validatorUtil.validateList(gridSubmitData.getNewAndUpdatedRows());

    gridSubmitData.getCreated().stream().forEach(p -> projectDao.insert(p));
    gridSubmitData.getUpdated().stream().forEach(p -> projectDao.update(p));

    for (Project project : gridSubmitData.getDestroyed()) {
      try {
        projectDao.delete(project);
      } catch (DataIntegrityViolationException e) {
        throw new CustomValidationException(
            "Delete failed. Project " + project.getName() + " is being used in the application.");
      }
    }

    return gridSubmitData;
  }
}
