package org.skillsdemo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.skillsdemo.common.AppUtil;
import org.skillsdemo.common.Page;
import org.skillsdemo.common.ValidatorUtil;
import org.skillsdemo.dao.PersonDao;
import org.skillsdemo.exception.CustomValidationException;
import org.skillsdemo.model.GridSubmitData;
import org.skillsdemo.model.Person;
import org.skillsdemo.model.PersonCredential;
import org.skillsdemo.model.PersonProject;
import org.skillsdemo.model.ValueAndText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {
  @Autowired PersonDao personDao;
  @Autowired ValidatorUtil validatorUtil;

  public Page<Person> fetchPaginatedPersons(Map<String, String> params) {
    return personDao.fetchPaginatedPersons(params);
  }

  public List<PersonProject> getPersonProjects(Integer personId) {
    return personDao.fetchPersonProjects(personId);
  }

  public List<ValueAndText> getPersonProjectsDropdownList(Integer personId) {
    List<PersonProject> list = personDao.fetchPersonProjects(personId);
    List<ValueAndText> dropdownList = new ArrayList<>();
    for (PersonProject pp : list) {
      dropdownList.add(new ValueAndText(pp.getProjectId().toString(), pp.getProjectName()));
    }
    return dropdownList;
  }

  public List<ValueAndText> getManagerDropdownList() {
    List<Person> list = personDao.getManagerList();
    List<ValueAndText> dropdownList = new ArrayList<>();
    for (Person p : list) {
      dropdownList.add(new ValueAndText(p.getId().toString(), p.getFullName()));
    }
    dropdownList.add(0, new ValueAndText("", ""));
    return dropdownList;
  }

  public List<ValueAndText> findManageAutocomplete(String autocompleteValue) {
    List<Person> list = personDao.findManagerAutocomplete(autocompleteValue);
    List<ValueAndText> responseList = new ArrayList<>();
    for (Person p : list) {
      responseList.add(new ValueAndText(p.getId().toString(), p.getFullName()));
    }
    return responseList;
  }

  @Transactional
  public void processPersonsSubmit(GridSubmitData<Person> gridSubmitData) {
    // if validation fails an exception is thrown and framework code will handle message to user.
    validatorUtil.validateList(gridSubmitData.getNewAndUpdatedRows());

    for (Person person : gridSubmitData.getCreated()) {
      if (personDao.existsUsername(person.getUsername())) {
        throw new CustomValidationException(
            "Save failed. Username " + person.getUsername() + " already exists.");
      }
      personDao.insert(person);
      PersonCredential cred = new PersonCredential(person.getId(), "pass");
      personDao.insert(cred);
    }
    
    for (Person person : gridSubmitData.getUpdatedAndDeletedRows()) {
	  if(AppUtil.getLoggedInPersonId().equals(person.getId())) {
		throw new CustomValidationException(
	            "Save failed. You cannot edit your own record.");
	  }
    }

    gridSubmitData.getUpdated().stream().forEach(p -> personDao.update(p));

    for (Person person : gridSubmitData.getDestroyed()) {
      try {
        personDao.delete(person);
      } catch (DataIntegrityViolationException e) {
        throw new CustomValidationException(
            "Delete failed. User " + person.getFullName() + " is being used in the application.");
      }
    }
  }

  @Transactional
  public GridSubmitData<PersonProject> processPersonProjectsSubmit(
      Integer personId, GridSubmitData<PersonProject> gridSubmitData) {
    // if validation fails an exception is thrown and framework code will handle message to user.
    validatorUtil.validateList(gridSubmitData.getNewAndUpdatedRows());

    for (PersonProject person : gridSubmitData.getNewAndUpdatedRows()) {
      person.setPersonId(personId);
    }

    for (PersonProject pp : gridSubmitData.getCreated()) {
      if (personDao.existsPersonProject(pp.getPersonId(), pp.getProjectId())) {
        throw new CustomValidationException("Save failed. Duplicate Project assigned");
      }
    }
    gridSubmitData.getCreated().stream().forEach(p -> personDao.insert(p));

    gridSubmitData.getUpdated().stream().forEach(p -> personDao.update(p));
    gridSubmitData.getDestroyed().stream().forEach(p -> personDao.delete(p));

    return gridSubmitData;
  }
}
