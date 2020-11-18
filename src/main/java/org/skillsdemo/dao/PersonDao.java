package org.skillsdemo.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.MutablePair;
import org.skillsdemo.common.KendoQueryBuilder;
import org.skillsdemo.common.Page;
import org.skillsdemo.common.QueryColumnOverride;
import org.skillsdemo.model.Person;
import org.skillsdemo.model.PersonCredential;
import org.skillsdemo.model.PersonProject;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PersonDao extends BaseDao {
  /**
   * Generates a dynamic query depending on filter/sort/pagination parameters sent from UI. 
   * Does 2 queries. The first gets the count and the second gets a Page full of records.
   * 
   * @param params - Kendo query parameters. Will be something similar to:
   * http://localhost:8080/person/api/persons?take=3&skip=0&page=1&pageSize=3
   *                                            &filter[logic]=and
   *                                            &filter[filters][0][field]=firstName
   *                                            &filter[filters][0][operator]=contains
   *                                            &filter[filters][0][value]=joe
   * @return a Page for persons
   */
  public Page<Person> fetchPaginatedPersons(Map<String, String> params) {
    String fromClause =
        String.join(
            " ",
            " from person p",
            "left join person_project pp on p.id = pp.person_id",
            "left join project proj on pp.project_id = proj.id",
            "left join person manager on p.reports_to_id = manager.id");

    // the property name from UI send by kendo in some cases dont match sql select column name.
    // Override them here so the query builder can build the correct where/orderby clauses.
    List<QueryColumnOverride> columnOverrides = new ArrayList<>();
    columnOverrides.add(new QueryColumnOverride("projectNames", "proj.id", "integer"));
    columnOverrides.add(new QueryColumnOverride("role", "p.role", "string"));
    columnOverrides.add(new QueryColumnOverride("reportsToId", "manager.id", "integer"));
    columnOverrides.add(new QueryColumnOverride("username", "p.username", "string"));
    columnOverrides.add(new QueryColumnOverride("firstName", "p.first_name", "string"));
    columnOverrides.add(new QueryColumnOverride("lastName", "p.last_name", "string"));
    columnOverrides.add(new QueryColumnOverride("accountStatus", "p.account_status", "integer"));
    
    KendoQueryBuilder queryBuilder = new KendoQueryBuilder(params, columnOverrides);
    
    MutablePair<String, Map<String, Object>> whereInfo = queryBuilder.getWhereClause();
    String whereClause = whereInfo.getLeft();
    Map<String, Object> sqlParams = whereInfo.getRight();

    String sqlCount = "select count(distinct(p.id)) " + fromClause + whereClause;
    int count = npJdbcTemplate.queryForObject(sqlCount, sqlParams, Integer.class);

    String orderByClause = queryBuilder.getOrderByClause();
    String offsetLimitClause = queryBuilder.getOffsetLimitClause();
    String sql =
        "select p.id, p.username, p.last_name, p.first_name, p.role, p.phone_num, p.email, p.reports_to_id, p.account_status, p.updated_on, p.updated_by,"
            + "concat(manager.first_name,' ', manager.last_name) reports_to_full_name, string_agg(proj.name, ',') project_names"
            + fromClause
            + whereClause
            + " group by p.id, p.username, p.last_name, p.first_name, p.role, p.phone_num, p.email, "
            + "p.reports_to_id, p.account_status, p.updated_on, p.updated_by,reports_to_full_name"
            + orderByClause
            + offsetLimitClause;

    RowMapper<Person> mapper = BeanPropertyRowMapper.newInstance(Person.class);
    List<Person> persons = npJdbcTemplate.query(sql, sqlParams, mapper);

    return new Page<Person>(count, persons);
  }

  public List<PersonProject> fetchPersonProjects(Integer personId) {
    String sql =
        String.join(
            " ",
            "select pp.*, p.name project_name",
            "from person_project pp",
            "join project p on pp.project_id = p.id",
            "where pp.person_id = ?",
            "order by p.name");

    RowMapper<PersonProject> mapper = BeanPropertyRowMapper.newInstance(PersonProject.class);
    return jdbcTemplate.query(sql, mapper, personId);
  }

  public boolean existsPersonProject(Integer personId, Integer projectId) {
    String sql = "select count(*) from person_project where person_id = ? and project_id = ?";
    int count = jdbcTemplate.queryForObject(sql, Integer.class, personId, projectId);
    return (count > 0) ? true : false;
  }

  public boolean existsUsername(String username) {
    String sql = "select count(*) from person where lower(username) = lower(?)";
    int count = jdbcTemplate.queryForObject(sql, Integer.class, username);
    return (count > 0) ? true : false;
  }

  public List<Person> getManagerList() {
    String sql = "select * from person where role = 'ROLE_MANAGER' order by first_name, last_name";
    RowMapper<Person> mapper = BeanPropertyRowMapper.newInstance(Person.class);
    return jdbcTemplate.query(sql, mapper);
  }
  
  public List<Person> findManagerAutocomplete(String value){
	    String sql =  String.join(" ",
	    		                 "select id, first_name, last_name, username from person",
	    		                 "where role = 'ROLE_MANAGER'",
	    		                 "and concat(first_name , ' ' ,last_name) ilike ?",
	    		                 "order by first_name, last_name");
	    RowMapper<Person> mapper = BeanPropertyRowMapper.newInstance(Person.class);
	    return jdbcTemplate.query(sql, mapper, "%" + value + "%");
  }
  
  public Person findByUsername(String username) {
    String sql = "select * from person where username = ?";
    RowMapper<Person> mapper = BeanPropertyRowMapper.newInstance(Person.class);
    try {
      return jdbcTemplate.queryForObject(sql, mapper, username);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public String getPassword(Integer personId) {
    String sql = "select * from person_credential where person_id = ?";
    RowMapper<PersonCredential> mapper = BeanPropertyRowMapper.newInstance(PersonCredential.class);
    try {
      PersonCredential credential = jdbcTemplate.queryForObject(sql, mapper, personId);
      return credential.getPassword();
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }
}
