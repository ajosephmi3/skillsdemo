package org.skillsdemo.dao;

import java.sql.ResultSet;
import java.util.List;

import org.skillsdemo.common.JdbcUtil;
import org.skillsdemo.common.SelectMapper;
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

  public <T, U> void toOne(T mainObj, String relationshipPropertyName, Class<U> relationshipClazz) {
    jdbcUtil.toOne(mainObj, relationshipPropertyName, relationshipClazz);
  }

  public <T, U> void toOne(
      List<T> mainObjList, String relationshipPropertyName, Class<U> relationshipClazz) {
    jdbcUtil.toOne(mainObjList, relationshipPropertyName, relationshipClazz);
  }

  public <T, U> List<T> toOneMapper(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      SelectMapper<U> relatedObjMapper,
      String relationshipPropertyName) {
    return jdbcUtil.toOneMapper(rs, mainObjMapper, relationshipPropertyName, relatedObjMapper);
  }

  public <T, U> void toMany(
      T mainObj,
      String collectionPropertyName,
      Class<U> manySideClazz,
      String manySideSortPropertyName) {
    jdbcUtil.toMany(mainObj, collectionPropertyName, manySideClazz, manySideSortPropertyName);
  }

  public <T, U> void toMany(
      List<T> mainObjList,
      String collectionPropertyName,
      Class<U> manySideClazz,
      String manySideSortPropertyName) {
    jdbcUtil.toMany(mainObjList, collectionPropertyName, manySideClazz, manySideSortPropertyName);
  }

  public <T, U> List<T> toManyMapper(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String collectionPropertyName,
      SelectMapper<U> relatedObjMapper) {
    return jdbcUtil.toManyMapper(rs, mainObjMapper, collectionPropertyName, relatedObjMapper);
  }
}
