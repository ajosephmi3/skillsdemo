package org.skillsdemo.dao;

import java.util.List;

import org.skillsdemo.common.JdbcUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Single line CRUD and other helpful methods.
 *
 * @author ajoseph
 */
public class BaseDao {
  @Autowired protected JdbcUtil jdbcUtil;
  @Autowired protected JdbcTemplate jdbcTemplate;
  @Autowired protected NamedParameterJdbcTemplate npJdbcTemplate;

  public <T> T findById(Object id, Class<T> clazz) {
    return jdbcUtil.findById(id, clazz);
  }

  public void insert(Object pojo) {
    jdbcUtil.insert(pojo);
  }

  public <T> List<T> findAll(Class<T> clazz) {
    return jdbcUtil.findAll(clazz);
  }

  public Integer update(Object pojo) {
    return jdbcUtil.update(pojo);
  }

  public Integer delete(Object pojo) {
    return jdbcUtil.delete(pojo);
  }

  public <T> Integer deleteById(Integer id, Class<T> clazz) {
    return jdbcUtil.deleteById(id, clazz);
  }

  public Integer softDelete(Object pojo) {
    return jdbcUtil.softDelete(pojo);
  }

  public Integer getNextSequence(String sequenceName) {
    return jdbcUtil.getNextSequence(sequenceName);
  }
}
