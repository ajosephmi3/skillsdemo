package org.jdbctemplatemapper.dbutil;

import java.beans.PropertyDescriptor;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

/**
 * 1) Simple CRUD one liners using spring's JDBC template. 2) Methods to map relationships (toOne,
 * toMany etc) 3) Uses springsecurity's Principal to populate createdBy, updateBy .. fields.
 *
 * <p>TODO 1)Boolean/BigDecimal conversion, 2)make record audit property names configurable. 3) Need
 * a userResolver to populate created/updated user info
 *
 * @author ajoseph
 */
public class JdbcUtil {
  private NamedParameterJdbcTemplate npJdbcTemplate;
  private JdbcTemplate jdbcTemplate;
  private IAuditOperatorResolver auditOperatorResolver;

  private String createdByPropertyName;
  private String createdOnPropertyName;
  private String updatedByPropertyName;
  private String updatedOnPropertyName;
  private String schemaName;
  private String versionPropertyName;

  private static int IN_CLAUSE_CHUNK_SIZE = 100;

  // Convert camel case to snake case regex pattern
  private static Pattern TO_SNAKE_CASE_PATTERN = Pattern.compile("(.)(\\p{Upper})");

  // Inserts use SimpleJdbcInsert. Since SimpleJdbcInsert is thread safe, cache it
  // Map key - table name,
  //     value - SimpleJdcInsert object for the specific table
  private Map<String, SimpleJdbcInsert> simpleJdbcInsertCache = new ConcurrentHashMap<>();

  // update sql cache
  // Map key   - table name or sometimes tableName-updatePropertyName1-updatePropertyName2
  //     value - the update sql
  private Map<String, String> updateSqlCache = new ConcurrentHashMap<>();

  // Map key - table name,
  //     value - the list of database column names
  private Map<String, List<String>> tableColumnNamesCache = new ConcurrentHashMap<>();

  // Map key - simple Class name
  //     value - list of property names
  private Map<String, List<String>> objectPropertyNamesCache = new ConcurrentHashMap<>();

  // Map key - snake case string,
  //     value - camel case string
  private Map<String, String> snakeToCamelCache = new ConcurrentHashMap<>();

  // Map key - camel case string,
  //     value - snake case string
  private Map<String, String> camelToSnakeCache = new ConcurrentHashMap<>();

  public JdbcUtil(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    this.npJdbcTemplate = namedParameterJdbcTemplate;
    this.jdbcTemplate = namedParameterJdbcTemplate.getJdbcTemplate();
  }

  public JdbcUtil withAuditOperatorResolver(IAuditOperatorResolver auditOperatorResolver) {
    this.auditOperatorResolver = auditOperatorResolver;
    return this;
  }

  public JdbcUtil withSchemaName(String schemaName) {
    this.schemaName = schemaName;
    return this;
  }

  public JdbcUtil withCreatedByPropertyName(String propName) {
    this.createdByPropertyName = propName;
    return this;
  }

  public JdbcUtil withCreatedOnPropertyName(String propName) {
    this.createdOnPropertyName = propName;
    return this;
  }

  public JdbcUtil withUpdatedByPropertyName(String propName) {
    this.updatedByPropertyName = propName;
    return this;
  }

  public JdbcUtil withUpdatedOnPropertyName(String propName) {
    this.updatedOnPropertyName = propName;
    return this;
  }

  public JdbcUtil withVersionPropertyName(String propName) {
    this.versionPropertyName = propName;
    return this;
  }

  /**
   * Returns the object by Id. Return null if not found
   *
   * @param id - Id of object
   * @param type - Class of object
   * @return - The object of the specific type
   */
  public <T> T findById(Object id, Class<T> clazz) {
    if (!(id instanceof Integer || id instanceof Long)) {
      throw new IllegalArgumentException("id has to be type of Integer or Long");
    }
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + tableName + " where id = ?";
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    try {
      Object obj = jdbcTemplate.queryForObject(sql, mapper, id);
      return clazz.cast(obj);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  /**
   * Inserts an object. For objects which have auto increment database id, after the insert the
   * object will get assigned the id. Also assigns createdBy, createdOn, updatedBy, updatedOn values
   * if these properties exist for the object
   *
   * @param pojo - The object to be saved
   */
  public void insert(Object pojo) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    LocalDateTime now = LocalDateTime.now();

    if (createdOnPropertyName != null && PropertyUtils.isReadable(pojo, createdOnPropertyName)) {
      setSimpleProperty(pojo, createdOnPropertyName, now);
    }
    if (createdByPropertyName != null
        && auditOperatorResolver != null
        && PropertyUtils.isReadable(pojo, createdByPropertyName)) {
      setSimpleProperty(pojo, createdByPropertyName, auditOperatorResolver.getAuditOperator());
    }
    if (updatedOnPropertyName != null && PropertyUtils.isReadable(pojo, updatedOnPropertyName)) {
      setSimpleProperty(pojo, updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && auditOperatorResolver != null
        && PropertyUtils.isReadable(pojo, updatedByPropertyName)) {
      setSimpleProperty(pojo, updatedByPropertyName, auditOperatorResolver.getAuditOperator());
    }
    if (versionPropertyName != null && PropertyUtils.isReadable(pojo, versionPropertyName)) {
      setSimpleProperty(pojo, versionPropertyName, 1);
    }

    Map<String, Object> attributes = convertToDbColumnAttributes(pojo);
    Object idValue = getSimpleProperty(pojo, "id");

    SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(tableName);
    if (jdbcInsert == null) {
      if (idValue == null) {
        // object whose id in database is auto increment
        if (schemaName != null) {
          jdbcInsert =
              new SimpleJdbcInsert(jdbcTemplate)
                  .withSchemaName(schemaName)
                  .withTableName(tableName)
                  .usingGeneratedKeyColumns("id");
        } else {
          new SimpleJdbcInsert(jdbcTemplate)
              .withTableName(tableName)
              .usingGeneratedKeyColumns("id");
        }
      } else {
        // object has already assigned id; ie NOT auto increment in database
        if (schemaName != null) {
          jdbcInsert =
              new SimpleJdbcInsert(jdbcTemplate)
                  .withSchemaName(schemaName)
                  .withTableName(tableName);
        } else {
          jdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName(tableName);
        }
      }
      simpleJdbcInsertCache.put(tableName, jdbcInsert);
    }

    if (idValue == null) {
      // object whose id in database is auto increment
      Number idNumber = jdbcInsert.executeAndReturnKey(attributes);
      // set id on pojo
      Class<?> clazz = getPropertyType(pojo, "id");
      if ("Integer".equals(clazz.getSimpleName())) {
        setSimpleProperty(pojo, "id", idNumber.intValue());
      } else {
        // id is of type long
        setSimpleProperty(pojo, "id", idNumber.longValue());
      }
    } else {
      // object with id that is NOT auto increment in database
      jdbcInsert.execute(attributes);
    }
  }

  /**
   * Updates object. Will also set updatedBy and updatedOn values if these properties exist for the
   * object. if 'version' property exists for object throws an OptimisticLockingException if fails
   * to update the record
   *
   * @param pojo - object to be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object pojo) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    String updateSql = updateSqlCache.get(tableName);
    if (updateSql == null) {
      updateSql = buildUpdateSql(pojo);
    }

    LocalDateTime now = LocalDateTime.now();

    if (updatedOnPropertyName != null && PropertyUtils.isReadable(pojo, updatedOnPropertyName)) {
      setSimpleProperty(pojo, updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && auditOperatorResolver != null
        && PropertyUtils.isReadable(pojo, updatedByPropertyName)) {
      setSimpleProperty(pojo, updatedByPropertyName, auditOperatorResolver.getAuditOperator());
    }

    Map<String, Object> attributes = convertToSqlProperties(pojo);
    // if object has property version throw OptimisticLockingException
    // update fails. version gets incremented
    if (versionPropertyName != null && PropertyUtils.isReadable(pojo, versionPropertyName)) {
      Integer versionVal = (Integer) getSimpleProperty(pojo, versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + pojo.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", versionVal++);
      }
      int cnt = npJdbcTemplate.update(updateSql, attributes);
      if (cnt == 0) {
        throw new OptimisticLockingException(
            "Update failed for "
                + pojo.getClass().getSimpleName()
                + " for id:"
                + attributes.get("id")
                + " and "
                + versionPropertyName
                + ":"
                + attributes.get(versionPropertyName));
      }
      return cnt;
    } else {
      return npJdbcTemplate.update(updateSql, attributes);
    }
  }

  /**
   * Updates the propertyNames of the object. Will also set updatedBy and updatedOn values if these
   * properties exist for the object
   *
   * @param pojo - object to be updated
   * @param propertyNames - array of property names that should be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object pojo, String... propertyNames) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    // cachekey ex: className-propertyName1-propertyName2
    String cacheKey = tableName + "-" + String.join("-", propertyNames);
    String updateSql = updateSqlCache.get(cacheKey);
    if (updateSql == null) {
      StringBuilder sqlBuilder = new StringBuilder("update ");
      sqlBuilder.append(tableName);
      sqlBuilder.append(" set ");

      List<String> dbColumnNameList = new ArrayList<>();
      for (String propertyName : propertyNames) {
        dbColumnNameList.add(convertCamelToSnakeCase(propertyName));
      }
      boolean first = true;
      for (String columnName : dbColumnNameList) {
        if (!first) {
          sqlBuilder.append(", ");
        } else {
          first = false;
        }
        sqlBuilder.append(columnName);
        sqlBuilder.append(" = :");

        sqlBuilder.append(convertSnakeToCamelCase(columnName));
      }
      // the set assignment for the incremented version
      if (versionPropertyName != null && PropertyUtils.isReadable(pojo, versionPropertyName)) {
        sqlBuilder.append(", ").append(versionPropertyName).append(" = :incrementedVersion");
      }
      // the where clause
      sqlBuilder.append(" where id = :id");
      if (versionPropertyName != null && PropertyUtils.isReadable(pojo, versionPropertyName)) {
        sqlBuilder
            .append(" and ")
            .append(versionPropertyName)
            .append(" = :")
            .append(versionPropertyName);
      }

      updateSql = sqlBuilder.toString();
      updateSqlCache.put(cacheKey, updateSql);
    }

    LocalDateTime now = LocalDateTime.now();
    if (updatedOnPropertyName != null && PropertyUtils.isReadable(pojo, updatedOnPropertyName)) {
      setSimpleProperty(pojo, updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && auditOperatorResolver != null
        && PropertyUtils.isReadable(pojo, updatedByPropertyName)) {
      setSimpleProperty(pojo, updatedByPropertyName, auditOperatorResolver.getAuditOperator());
    }

    Map<String, Object> attributes = convertToSqlProperties(pojo);
    // if object has property version throw OptimisticLockingException
    // update fails. The version gets incremented
    if (versionPropertyName != null && PropertyUtils.isReadable(pojo, versionPropertyName)) {
      Integer versionVal = (Integer) getSimpleProperty(pojo, versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + pojo.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", versionVal++);
      }
      int cnt = npJdbcTemplate.update(updateSql, attributes);
      if (cnt == 0) {
        throw new OptimisticLockingException(
            "Update failed for "
                + pojo.getClass().getSimpleName()
                + " id:"
                + attributes.get("id")
                + " "
                + versionPropertyName
                + ":"
                + attributes.get("version"));
      }
      return cnt;
    } else {
      return npJdbcTemplate.update(updateSql, attributes);
    }
  }

  /**
   * Physically Deletes the object from the database
   *
   * @param pojo - Object to be deleted
   * @return 0 if no records were deleted
   */
  public Integer delete(Object pojo) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    String sql = "delete from " + tableName + " where id = ?";
    Object id = getSimpleProperty(pojo, "id");
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Physically Deletes the object from the database by id
   *
   * @param id - Id of object to be deleted
   * @param clazz - Type of object to be deleted.
   * @return 0 if no records were deleted
   */
  public <T> Integer deleteById(Integer id, Class<T> clazz) {
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "delete from " + tableName + " where id = ?";
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Get the next sequence number for the sequence name
   *
   * @param sequenceName - The name of the sequence
   * @return the next sequence number
   */
  public Integer getNextSequence(String sequenceName) {
    String sql = "select nextval('" + sequenceName + "')";
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }

  /**
   * Find all objects
   *
   * @param clazz - Type of object
   * @return List of objects
   */
  public <T> List<T> findAll(Class<T> clazz) {
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + tableName;
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  /**
   * Find all objects
   *
   * @param clazz - Type of object
   * @return List of objects
   */
  public <T> List<T> findAll(Class<T> clazz, String orderByClause) {
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + tableName + " " + orderByClause;
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  public <T, U> void toOne(T mainObj, String relationshipPropertyName, Class<U> relationshipClazz) {
    List<T> mainObjList = new ArrayList<>();
    mainObjList.add(mainObj);
    toOne(mainObjList, relationshipPropertyName, relationshipClazz);
  }

  public <T, U> void toOne(
      List<T> mainObjList, String relationshipPropertyName, Class<U> relationshipClazz) {
    String tableName = convertCamelToSnakeCase(relationshipClazz.getSimpleName());
    if (Util.isNotEmpty(mainObjList)) {
      String joinColumnName = tableName + "_id";
      String joinPropertyName = convertSnakeToCamelCase(joinColumnName);

      List<Integer> allColumnIds = new ArrayList<>();
      for (T mainObj : mainObjList) {
        Integer joinPropertyValue = (Integer) getSimpleProperty(mainObj, joinPropertyName);
        if (joinPropertyValue != null && joinPropertyValue > 0) {
          allColumnIds.add((Integer) getSimpleProperty(mainObj, joinPropertyName));
        }
      }
      List<U> list = new ArrayList<>();
      // to avoid query being issued with large number of ids
      // for the 'IN (:columnIds) clause the list is chunked by IN_CLAUSE_CHUNK_SIZE
      // and multiple queries issued if needed.
      Collection<List<Integer>> chunkedColumnIds = chunkList(allColumnIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Integer> columnIds : chunkedColumnIds) {
        String sql = "select * from " + tableName + " where id in (:columnIds)";

        MapSqlParameterSource params = new MapSqlParameterSource("columnIds", columnIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(relationshipClazz);
        list.addAll(npJdbcTemplate.query(sql, params, mapper));
      }
      Map<Integer, U> idToObjectMap =
          list.stream()
              .collect(Collectors.toMap(e -> (Integer) getSimpleProperty(e, "id"), obj -> obj));

      for (T mainObj : mainObjList) {
        Integer joinPropertyValue = (Integer) getSimpleProperty(mainObj, joinPropertyName);
        if (joinPropertyValue != null && joinPropertyValue > 0) {
          setSimpleProperty(
              mainObj, relationshipPropertyName, idToObjectMap.get(joinPropertyValue));
        }
      }
    }
  }

  @SuppressWarnings("all")
  public <T, U> T toOneMapperForObject(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String relationshipPropertyName,
      SelectMapper<U> relatedObjMapper) {
    List<T> list = toOneMapper(rs, mainObjMapper, relationshipPropertyName, relatedObjMapper);
    return Util.isNotEmpty(list) ? list.get(0) : null;
  }

  @SuppressWarnings("all")
  public <T, U> List<T> toOneMapper(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String relationshipPropertyName,
      SelectMapper<U> relatedObjMapper) {
    try {
      List<T> list = new ArrayList<>();
      List<String> resultSetColumnNames = getResultSetColumnNames(rs);
      while (rs.next()) {
        T mainObj =
            newInstance(
                mainObjMapper.getClazz(),
                rs,
                mainObjMapper.getSqlColumnPrefix(),
                resultSetColumnNames);
        Integer relatedObjId = rs.getInt(relatedObjMapper.getSqlColumnPrefix() + "id");
        if (relatedObjId != null && relatedObjId > 0) {
          Object relatedObj =
              newInstance(
                  relatedObjMapper.getClazz(),
                  rs,
                  relatedObjMapper.getSqlColumnPrefix(),
                  resultSetColumnNames);
          setSimpleProperty(mainObj, relationshipPropertyName, relatedObj);
        }
        list.add(mainObj);
      }
      return list;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public <T, U> void toOneMerge(
      List<T> mainObjList,
      List<U> relatedObjList,
      String relationshipPropertyName,
      String joinPropertyName) {
    if (Util.isNotEmpty(mainObjList) && Util.isNotEmpty(relatedObjList)) {
      Map<Integer, U> idToObjectMap =
          relatedObjList
              .stream()
              .collect(Collectors.toMap(e -> (Integer) getSimpleProperty(e, "id"), obj -> obj));

      for (T mainObj : mainObjList) {
        Integer joinPropertyValue = (Integer) getSimpleProperty(mainObj, joinPropertyName);
        if (joinPropertyValue != null && joinPropertyValue > 0) {
          setSimpleProperty(
              mainObj, relationshipPropertyName, idToObjectMap.get(joinPropertyValue));
        }
      }
    }
  }

  public <T, U> void toMany(
      T mainObj, String collectionPropertyName, Class<U> manySideClazz, String orderByClause) {
    List<T> mainObjList = new ArrayList<>();
    mainObjList.add(mainObj);
    toMany(mainObjList, collectionPropertyName, manySideClazz, orderByClause);
  }

  /**
   * When provided a list of one side objects populates the many side list.
   *
   * <p>Executes a query with 'IN' clause.
   *
   * @param mainObjList - the main object list
   * @param collectionPropertyName - The collection property name on mainObj
   * @param manySideClass - The many side class
   * @param orderByClause - The order by clause for the many side query
   */
  public <T, U> void toMany(
      List<T> mainObjList,
      String collectionPropertyName,
      Class<U> manySideClazz,
      String orderByClause) {
    String tableName = convertCamelToSnakeCase(manySideClazz.getSimpleName());
    if (Util.isNotEmpty(mainObjList)) {
      Set<Integer> allIds = new LinkedHashSet<>();
      for (T mainObj : mainObjList) {
        Integer idVal = (Integer) getSimpleProperty(mainObj, "id");
        if (idVal != null && idVal > 0) {
          allIds.add((idVal));
        } else {
          throw new RuntimeException("id property in mainObjList cannot be null");
        }
      }

      List<Integer> uniqueIds = new ArrayList<>(allIds);

      String joinColumnName =
          convertCamelToSnakeCase(mainObjList.get(0).getClass().getSimpleName()) + "_id";
      List<U> manySideList = new ArrayList<>();
      // to avoid query being issued with large number of
      // records for the 'IN (:columnIds) clause the list is chunked by IN_CLAUSE_CHUNK_SIZE
      // and multiple queries issued
      Collection<List<Integer>> chunkedColumnIds = chunkList(uniqueIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Integer> columnIds : chunkedColumnIds) {
        String sql = "select * from " + tableName + " where " + joinColumnName + " in (:columnIds)";
        if (Util.isNotEmpty(orderByClause)) {
          sql += " " + orderByClause;
        } else {
          sql += " order by id";
        }
        MapSqlParameterSource params = new MapSqlParameterSource("columnIds", columnIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(manySideClazz);
        manySideList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      if (Util.isNotEmpty(manySideList)) {
        String joinPropertyName = convertSnakeToCamelCase(joinColumnName);

        // map: key - joinPropertyName, value - List of manyside for the join property
        Map<Integer, List<U>> mapColumnIdToManySide =
            manySideList
                .stream()
                .collect(
                    Collectors.groupingBy(e -> (Integer) getSimpleProperty(e, joinPropertyName)));

        // assign the manyside list to the mainobj
        for (T mainObj : mainObjList) {
          Integer idValue = (Integer) getSimpleProperty(mainObj, "id");
          List<U> relatedList = mapColumnIdToManySide.get(idValue);
          setSimpleProperty(mainObj, collectionPropertyName, relatedList);
        }
      }
    }
  }

  @SuppressWarnings("all")
  public <T, U> List<T> toManyMapper(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String collectionPropertyName,
      SelectMapper<U> relatedObjMapper) {
    try {
      Map<Integer, T> resultMap = new LinkedHashMap<>();
      List<String> resultSetColumnNames = getResultSetColumnNames(rs);
      String colMainObjId = mainObjMapper.getSqlColumnPrefix() + "id";
      String colRelatedObjId = relatedObjMapper.getSqlColumnPrefix() + "id";
      while (rs.next()) {
        Integer mainObjId = rs.getInt(colMainObjId);
        T mainObj =
            resultMap.getOrDefault(
                mainObjId,
                newInstance(
                    mainObjMapper.getClazz(),
                    rs,
                    mainObjMapper.getSqlColumnPrefix(),
                    resultSetColumnNames));
        Integer relatedObjId = rs.getInt(colRelatedObjId);
        if (relatedObjId != null && relatedObjId != 0) {
          Object relatedObj =
              newInstance(
                  relatedObjMapper.getClazz(),
                  rs,
                  relatedObjMapper.getSqlColumnPrefix(),
                  resultSetColumnNames);
          List list = (List) getSimpleProperty(mainObj, collectionPropertyName);
          if (list == null) {
            list = new ArrayList<>();
            setSimpleProperty(mainObj, collectionPropertyName, list);
          }
          list.add(relatedObj);
        }
        resultMap.put(mainObjId, mainObj);
      }
      return new ArrayList<T>(resultMap.values());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("all")
  public <T, U> void toManyMerge(
      List<T> mainObjList,
      List<U> manySideList,
      String collectionPropertyName,
      String joinPropertyName) {
    try {
      Map<Integer, T> resultMap = new LinkedHashMap<>();
      if (Util.isNotEmpty(manySideList)) {
        Map<Integer, List<U>> mapColumnIdToManySide =
            manySideList
                .stream()
                .collect(
                    Collectors.groupingBy(e -> (Integer) getSimpleProperty(e, joinPropertyName)));

        // assign the manyside list to the mainobj
        for (T mainObj : mainObjList) {
          Integer idValue = (Integer) getSimpleProperty(mainObj, "id");
          List<U> relatedList = mapColumnIdToManySide.get(idValue);
          setSimpleProperty(mainObj, collectionPropertyName, relatedList);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns lists for each mapper passed in as an argument. The values in the list are UNIQUE and
   * in same order as the ResultSet values. The map key is the SqlMapper columnPrefix.
   *
   * @param rs - The result set
   * @param selectMappers - The variable number of sql mappers.
   * @return Map - key: 'sqlColumnPrefix' of sqlmapper, value - unique list
   */
  @SuppressWarnings("all")
  public Map<String, List> multipleModelMapper(ResultSet rs, SelectMapper... selectMappers) {
    try {
      Map<String, List> resultMap = new HashMap();
      Map<String, List> tempMap = new HashMap<>();
      for (SelectMapper selectMapper : selectMappers) {
        tempMap.put(selectMapper.getSqlColumnPrefix(), new ArrayList());
      }
      List<String> resultSetColumnNames = getResultSetColumnNames(rs);
      while (rs.next()) {
        for (SelectMapper selectMapper : selectMappers) {
          Integer id = rs.getInt(selectMapper.getSqlColumnPrefix() + "id");
          if (id == null || id == 0) {
            new RuntimeException(
                "Mapper expects '" + selectMapper.getSqlColumnPrefix() + "id' in select statement");
          }
          Object obj =
              newInstance(
                  selectMapper.getClazz(),
                  rs,
                  selectMapper.getSqlColumnPrefix(),
                  resultSetColumnNames);
          tempMap.get(selectMapper.getSqlColumnPrefix()).add(obj);
        }
      }

      // each list should only have elements unique by 'id'
      for (String key : tempMap.keySet()) {
        List<Object> list = tempMap.get(key);
        resultMap.put(key, uniqueByIdList(list));
      }
      return resultMap;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates a string which can be used in a sql select statement with all columns of the table.
   *
   * @param tableName - the Table name
   * @param tableAlias - the alias being used in the sql statement for the table.
   * @param includeCommaAtEnd - whether to include a comma at end of string
   * @return a string for the select columns. Example if tableAlias is 'emp' returns something like:
   *     "emp.id emp_id, emp.last_name emp_last_name, emp.first_name emp_first_name ....."
   */
  public String selectCols(String tableName, String tableAlias, boolean includeCommaAtEnd) {
    List<String> dbColumnNames = getDbColumnNames(tableName);
    StringBuilder sb = new StringBuilder();
    for (String colName : dbColumnNames) {
      sb.append(tableAlias)
          .append(".")
          .append(colName)
          .append(" ")
          .append(tableAlias)
          .append("_")
          .append(colName)
          .append(",");
    }
    String str = sb.toString();
    if (str != null) {
      return includeCommaAtEnd ? str : str.substring(0, str.length() - 1);
    } else {
      return null;
    }
  }

  /**
   * Builds sql update statement with named parameters for the object.
   *
   * @param pojo - the object that needs to be update.
   * @return sql update string
   */
  private String buildUpdateSql(Object pojo) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());

    // database columns for the tables
    List<String> dbColumnNameList = getDbColumnNames(tableName);

    // ignore these attributes when generating the sql 'SET' command
    List<String> ignoreAttrs = new ArrayList<>();
    ignoreAttrs.add("id");
    if (createdByPropertyName != null) {
      ignoreAttrs.add(createdByPropertyName);
    }
    if (createdOnPropertyName != null) {
      ignoreAttrs.add(createdOnPropertyName);
    }

    List<String> updateColumnNameList = new ArrayList<>();
    PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(pojo);
    for (PropertyDescriptor pd : propertyDescriptors) {
      String columnName = convertCamelToSnakeCase(pd.getName());
      // skips non db columns and ignore fields like 'id' etc for SET
      if (!ignoreAttrs.contains(columnName) && dbColumnNameList.contains(columnName)) {
        updateColumnNameList.add(columnName);
      }
    }

    StringBuilder sqlBuilder = new StringBuilder("update ");
    sqlBuilder.append(tableName);
    sqlBuilder.append(" set ");
    boolean first = true;
    // the dbColumnNameList is the driver because we want the update statement column order to
    // reflect the table column order in database.
    for (String columnName : dbColumnNameList) {
      if (updateColumnNameList.contains(columnName)) {
        if (!first) {
          sqlBuilder.append(", ");
        } else {
          first = false;
        }
        sqlBuilder.append(columnName);
        sqlBuilder.append(" = :");

        if (versionPropertyName != null && versionPropertyName.equals(columnName)) {
          sqlBuilder.append("incrementedVersion");
        } else {
          sqlBuilder.append(convertSnakeToCamelCase(columnName));
        }
      }
    }
    // build where clause
    sqlBuilder.append(" where id = :id");
    if (versionPropertyName != null && updateColumnNameList.contains(versionPropertyName)) {
      sqlBuilder
          .append(" and ")
          .append(versionPropertyName)
          .append(" = :")
          .append(versionPropertyName);
    }

    String updateSql = sqlBuilder.toString();
    updateSqlCache.put(tableName, updateSql);
    return updateSql;
  }

  /**
   * Used by mappers to instantiate object from the result set
   *
   * @param clazz - Class of object to be instantiated
   * @param rs - Sql result set
   * @param prefix - The sql alias in the query (if any)
   * @param resultSetColumnNames - the column names in the sql statement.
   * @return Object of type T populated from the data in the result set
   */
  private <T> T newInstance(
      Class<T> clazz, ResultSet rs, String prefix, List<String> resultSetColumnNames) {
    try {
      Object obj = clazz.newInstance();
      List<String> propertyNames = getPropertyNames(obj);
      for (String propName : propertyNames) {
        String columnName = convertCamelToSnakeCase(propName);
        if (Util.isNotEmpty(prefix)) {
          columnName = prefix + columnName;
        }
        if (resultSetColumnNames.contains(columnName)) {
          Object columnVal = rs.getObject(columnName);

          if (columnVal instanceof java.sql.Timestamp) {
            setSimpleProperty(obj, propName, ((java.sql.Timestamp) (columnVal)).toLocalDateTime());
          } else if (columnVal instanceof java.sql.Date) {
            setSimpleProperty(obj, propName, ((java.sql.Date) columnVal).toLocalDate());
          } else {
            setSimpleProperty(obj, propName, columnVal);
          }
        }
      }
      return clazz.cast(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts an object to a map with key as database column names. ie the camel case property names
   * are converted to snake case. For example 'userLastName' will get converted to 'user_last_name'
   * Also converts values of type LocalDate, LocalDateTime to their corresponding sql types
   *
   * @param pojo - The object to convert
   * @return A map with keys that are in snake case to match database column names and values
   *     converted to sql types
   */
  private Map<String, Object> convertToDbColumnAttributes(Object pojo) {
    Map<String, Object> camelCaseAttrs = convertToSqlProperties(pojo);
    Map<String, Object> snakeCaseAttrs = new HashMap<>();
    for (String key : camelCaseAttrs.keySet()) {
      // lastName will get converted to last_name
      String snakeCaseKey = convertCamelToSnakeCase(key);
      snakeCaseAttrs.put(snakeCaseKey, camelCaseAttrs.get(key));
    }
    return snakeCaseAttrs;
  }

  /**
   * Converts an object to a Map and the property values are type converted to work with sql
   *
   * @param pojo - The object to be converted.
   * @return Map with key: property name, value: type conversion date fields to sql types
   */
  private Map<String, Object> convertToSqlProperties(Object pojo) {
    Map<String, Object> camelCaseAttrs = new HashMap<>();
    List<String> propertyNames = getPropertyNames(pojo);
    for (String propName : propertyNames) {
      // for LocalDateTime and LocalDate fields convert to their corresponding sql
      // types
      Object propValue = getSimpleProperty(pojo, propName);
      if (propValue instanceof LocalDateTime) {
        camelCaseAttrs.put(propName, java.sql.Timestamp.valueOf((LocalDateTime) propValue));
      } else if (propValue instanceof LocalDate) {
        camelCaseAttrs.put(propName, java.sql.Date.valueOf((LocalDate) propValue));
      } else {
        camelCaseAttrs.put(propName, propValue);
      }
    }
    return camelCaseAttrs;
  }

  /**
   * Gets the table column names from Databases MetaData. The column names are cached
   *
   * @param table - table name
   * @return the list of columns of the table
   */
  private List<String> getDbColumnNames(String table) {
    List<String> columns = tableColumnNamesCache.get(table);
    if (columns == null) {
      columns = new ArrayList<>();
      try {
        DatabaseMetaData metadata = jdbcTemplate.getDataSource().getConnection().getMetaData();
        ResultSet resultSet = metadata.getColumns(null, schemaName, table, null);
        while (resultSet.next()) {
          columns.add(resultSet.getString("COLUMN_NAME"));
        }
        tableColumnNamesCache.put(table, columns);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return columns;
  }

  /**
   * Gets the resultSet column names ie the column names in the 'select' statement of the sql
   *
   * @param rs - ResultSet
   * @return List of strings with column name
   */
  private List<String> getResultSetColumnNames(ResultSet rs) {
    List<String> rsColNames = new ArrayList<>();
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numberOfColumns = rsmd.getColumnCount();
      // jdbc indexes start at 1
      for (int i = 1; i <= numberOfColumns; i++) {
        rsColNames.add(rsmd.getColumnName(i));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return rsColNames;
  }

  private List<String> getPropertyNames(Object pojo) {
    List<String> list = objectPropertyNamesCache.get(pojo.getClass().getSimpleName());
    if (list == null) {
      list = new ArrayList<>();
      PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(pojo);
      for (PropertyDescriptor pd : propertyDescriptors) {
        String propName = pd.getName();
        // log.debug("Property name:{}" + propName);
        if ("class".equals(propName)) {
          continue;
        } else {
          list.add(propName);
        }
      }
      objectPropertyNamesCache.put(pojo.getClass().getSimpleName(), list);
    }
    return list;
  }

  private Object getSimpleProperty(Object obj, String propertyName) {
    try {
      return PropertyUtils.getSimpleProperty(obj, propertyName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setSimpleProperty(Object obj, String propertyName, Object val) {
    try {
      PropertyUtils.setProperty(obj, propertyName, val);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Class<?> getPropertyType(Object obj, String propertyName) {
    try {
      return PropertyUtils.getPropertyType(obj, propertyName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts camel case to snake case. Ex: userLastName gets converted to user_last_name
   *
   * @param property - property name in camel case
   * @return the snake case string
   */
  private String convertCamelToSnakeCase(String str) {
    String snakeCase = camelToSnakeCache.get(str);
    if (snakeCase == null) {
      if (str != null) {
        snakeCase = TO_SNAKE_CASE_PATTERN.matcher(str).replaceAll("$1_$2").toLowerCase();
       // snakeCase = RegExUtils.replaceAll(str, TO_SNAKE_CASE_PATTERN, "$1_$2").toLowerCase();
        camelToSnakeCache.put(str, snakeCase);
      }
    }
    return snakeCase;
  }

  private String convertSnakeToCamelCase(String str) {
    String camelCase = snakeToCamelCache.get(str);
    if (camelCase == null) {
      camelCase = Util.toCamelCase(str, false, new char[] {'_'});
      snakeToCamelCache.put(str, camelCase);
    }
    return camelCase;
  }

  private Collection<List<Integer>> chunkList(List<Integer> list, Integer chunkSize) {
    AtomicInteger counter = new AtomicInteger();
    Collection<List<Integer>> result =
        list.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values();
    return result;
  }

  private List<Object> uniqueByIdList(List<Object> list) {
    if (Util.isNotEmpty(list)) {
      Map<Integer, Object> idToObjectMap = new LinkedHashMap<>();
      for (Object obj : list) {
        Integer id = (Integer) getSimpleProperty(obj, "id");
        if (!idToObjectMap.containsKey(id)) {
          idToObjectMap.put(id, obj);
        }
      }
      return new ArrayList<Object>(idToObjectMap.values());
    } else {
      return list;
    }
  }
  


}
