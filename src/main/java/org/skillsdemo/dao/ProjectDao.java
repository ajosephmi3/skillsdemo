package org.skillsdemo.dao;

import java.util.List;

import org.skillsdemo.model.Project;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectDao extends BaseDao {

  public List<Project> findAllOrderByName() {
    String sql = "select * from project order by name";
    RowMapper<Project> mapper = BeanPropertyRowMapper.newInstance(Project.class);
    return jdbcTemplate.query(sql, mapper);
  }
}
