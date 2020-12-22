package org.skillsdemo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skillsdemo.common.JdbcUtil;
import org.skillsdemo.common.SelectMapper;
import org.skillsdemo.model.Person;
import org.skillsdemo.model.Project;
import org.skillsdemo.model.Timesheet;
import org.skillsdemo.model.TimesheetLine;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcUtilTest {
  @SpyBean private JdbcUtil jdbcUtil;

  @Test
  public void toManyMapperListTest() throws Exception {
    ResultSet rs =
        MockResultSet.create(
            new String[] {
              "id",
              "person_id",
              "timesheetline_id",
              "timesheetline_timesheet_id",
              "timesheetline_project_id",
              "timesheetline_project_name"
            }, // columns
            new Object[][] { // data
              {1, 1, 1, 1, 1, "myworld"},
              {1, 1, 2, 2, 2, "audittrak"},
              {2, 2, 3, 3, 2, "audittrak"}
            });

    SelectMapper<Timesheet> timesheetMapper = new SelectMapper<>(Timesheet.class);
    SelectMapper<TimesheetLine> timesheetLineMapper =
        new SelectMapper<>(TimesheetLine.class, "timesheetline_");

    List<Timesheet> timesheets =
        jdbcUtil.toManyMapper(rs, timesheetMapper,"timesheetLines", timesheetLineMapper);

    assertEquals(2, timesheets.size());
    assertEquals(2, timesheets.get(0).getTimesheetLines().size());
    assertEquals(1, timesheets.get(1).getTimesheetLines().size());
  }

  @Test
  public void toOneMapperSingleObjectTest() throws Exception {
    ResultSet rs =
        MockResultSet.create(
            new String[] {"id", "person_id", "person_first_name", "person_last_name"}, // columns
            new Object[][] { // data
              {1, 1, "tony firstname", "tony lastname"}
            });

    List<Timesheet> timesheets = jdbcUtil.toOneMapper(rs, new SelectMapper<>(Timesheet.class),
    		                                   "person",
    		                                   new SelectMapper<>(Person.class, "person_"));
    Timesheet timesheet = timesheets.get(0);
    assertEquals(1, timesheet.getId());
    assertEquals("tony firstname", timesheet.getPerson().getFirstName());
  }

  @Test
  public void toOneMapperListTest() throws Exception {
    ResultSet rs =
        MockResultSet.create(
            new String[] {"id", "person_id", "person_first_name", "person_last_name"}, // columns
            new Object[][] { // data
              {1, 1, "one firstname", "one lastname"},
              {2, 2, "two firstname", "two lastname"},
            });

    List<Timesheet> timesheets = jdbcUtil.toOneMapper(rs, 
    		                                          new SelectMapper<>(Timesheet.class), 
    		                                          "person",
    		                                          new SelectMapper<>(Person.class, "person_"));

    assertEquals(2, timesheets.size());
    assertEquals("one firstname", timesheets.get(0).getPerson().getFirstName());
    assertEquals("two lastname", timesheets.get(1).getPerson().getLastName());
  }
  
  @Test
  public void multipleModelMapperTest() throws Exception {
	    ResultSet rs =
	            MockResultSet.create(
	                new String[] {
	                  "id", 
	                  "person_id", "person_first_name", "person_last_name",
	                  "timesheetline_id", "timesheetline_timesheet_id","timesheetline_project_id",
	                  "project_id","project_name"
	                }, // columns
	                new Object[][] { // data
	                  {1, 1, "one firstname", "one lastname", 1, 1, 1, 1, "myworld"},
	                  {1, 1, "one firstname", "one lastname", 2, 1, 2, 2, "audittrak"},
	                  {2, 2, "two firstname", "two lastname", 3, 2, 2, 2,"audittrak"}
	                });

      Map<String, List> modelList = jdbcUtil.multipleModelMapper(rs, new SelectMapper<>(Timesheet.class,""), 
	    		                        new SelectMapper<>(Person.class, "person_"),	    
	    		                        new SelectMapper<>(TimesheetLine.class, "timesheetline_"), 
                                        new SelectMapper<>(Project.class, "project_"));

    assertEquals(2, modelList.get("").size());
    assertEquals(2, modelList.get("person_").size());
    assertEquals(3, modelList.get("timesheetline_").size());
    assertEquals(2, modelList.get("project_").size());

  }
    
  @Test
  public void toOneMergeTest() throws Exception {
	    ResultSet rs =
	            MockResultSet.create(
	                new String[] {
	                  "id", 
	                  "person_id", "person_first_name", "person_last_name",
	                  "timesheetline_id", "timesheetline_timesheet_id","timesheetline_project_id",
	                  "project_id","project_name"
	                }, // columns
	                new Object[][] { // data
	                  {1, 1, "one firstname", "one lastname", 1, 1, 1, 1, "myworld"},
	                  {1, 1, "one firstname", "one lastname", 2, 1, 2, 2, "audittrak"},
	                  {2, 2, "two firstname", "two lastname", 3, 2, 2, 2,"audittrak"}
	                });

      Map<String, List> modelList = jdbcUtil.multipleModelMapper(rs, new SelectMapper<>(Timesheet.class,""), 
	    		                        new SelectMapper<>(Person.class, "person_"),	    
	    		                        new SelectMapper<>(TimesheetLine.class, "timesheetline_"), 
                                        new SelectMapper<>(Project.class, "project_"));

     List<Timesheet> timesheetList =  modelList.get("");
     List<Person> personList =  modelList.get("person_");
     List<TimesheetLine> timesheetLineList = modelList.get("timesheetline_");
     List<Project> projectList = modelList.get("project_");

     jdbcUtil.toOneMerge(timesheetList, personList, "person", "personId");
     assertEquals("one firstname", timesheetList.get(0).getPerson().getFirstName());
     assertEquals("two lastname", timesheetList.get(1).getPerson().getLastName());
     
     jdbcUtil.toOneMerge(timesheetLineList, projectList, "project", "projectId");
     assertEquals("myworld", timesheetLineList.get(0).getProject().getName());

  }
  
  @Test
  public void toManyMergeTest() throws Exception {
	    ResultSet rs =
	            MockResultSet.create(
	                new String[] {
	                  "id", 
	                  "person_id", "person_first_name", "person_last_name",
	                  "timesheetline_id", "timesheetline_timesheet_id","timesheetline_project_id",
	                  "project_id","project_name"
	                }, // columns
	                new Object[][] { // data
	                  {1, 1, "one firstname", "one lastname", 1, 1, 1, 1, "myworld"},
	                  {1, 1, "one firstname", "one lastname", 2, 1, 2, 2, "audittrak"},
	                  {2, 2, "two firstname", "two lastname", 3, 2, 2, 2,"audittrak"}
	                });

      Map<String, List> modelList = jdbcUtil.multipleModelMapper(rs, new SelectMapper<>(Timesheet.class,""), 
	    		                        new SelectMapper<>(Person.class, "person_"),	    
	    		                        new SelectMapper<>(TimesheetLine.class, "timesheetline_"), 
                                        new SelectMapper<>(Project.class, "project_"));

     List<Timesheet> timesheetList =  modelList.get("");
     List<TimesheetLine> timesheetLineList =  modelList.get("timesheetline_");
     
     jdbcUtil.toManyMerge(timesheetList, timesheetLineList, "timesheetLines", "timesheetId");
     assertEquals(2, timesheetList.get(0).getTimesheetLines().size());
     assertEquals(1, timesheetList.get(1).getTimesheetLines().size());

  }
}
