package org.skillsdemo.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import lombok.extern.slf4j.Slf4j;

/**
 * Kendo query builder for server side pagination/filtering/sorting
 *
 * @author ajoseph
 */
@Slf4j
public class KendoQueryBuilder {
  // filter field parameters look like:
  // filter[filters][0][field]
  public static Pattern FILTER_FIELD_PATTERN =
      Pattern.compile("filter\\[filters\\]\\[(\\d+)\\]\\[field\\]");

  // kendo sort field parameters look like:
  // sort[0][field]
  public static Pattern SORT_FIELD_PATTERN = Pattern.compile("sort\\[(\\d+)\\]\\[field\\]");

  // Convert camel case to snake case pattern
  private static Pattern TO_SNAKE_CASE_PATTERN = Pattern.compile("(.)(\\p{Upper})");

  private Map<String, String> params;
  private List<QueryColumnOverride> columnOverrides = new ArrayList<>();

  public KendoQueryBuilder(Map<String, String> params) {
    this.params = params;
  }

  public KendoQueryBuilder(Map<String, String> params, List<QueryColumnOverride> columnOverrides) {
    this.params = params;
    this.columnOverrides = columnOverrides;
  }

  /*
   * Builds whereClause from the parameters send by kendo grid
   *
   * Example kendo request with pagination and filter:
   *
   * http://localhost:8080/person/api/persons?take=3&skip=0&page=1&pageSize=3
   *                                            &filter[logic]=and
   *                                            &filter[filters][0][field]=firstName
   *                                            &filter[filters][0][operator]=contains
   *                                            &filter[filters][0][value]=joe
   *
   * The where clause is built from the 'filter' parameters above
   *
   * @return MutablePair<whereClause,sqlParams> 
   *      whereClause - named parameter where clause. ex: WHERE last_name ilike :lastName AND first_name ilike :firstName 
   *      sqlParams   - the Sql parameter map for the above where clause.
   */
  public MutablePair<String, Map<String, Object>> getWhereClause() {
    Map<String, Object> sqlParams = new HashMap<>();
    // the where clause build will look something like:
    // WHERE last_name ilike :lastName AND first_name ilike :firstName
    String whereClause = "";
    List<String> filterFieldIndexes = getFilterFieldIndexes();
    boolean isFirst = true;
    if (CollectionUtils.isNotEmpty(filterFieldIndexes)) {
      for (String index : filterFieldIndexes) {
        String type = "string";
        // filter field name ex: filter[filters][0][field]
        String fieldName = params.get("filter[filters][" + index + "][field]");
        // something like 'lastName' will get converted to 'last_name'
        String columnName = convertCamelCaseToSnakeCase(fieldName);
        if (isFirst) {
          isFirst = false;
        } else {
          whereClause += " and ";
        }
        QueryColumnOverride override = getOverrideForField(fieldName, columnOverrides);
        if (override != null) {
          columnName = override.getColumnName();
          type = override.getType();
        }
        if ("integer".equals(type)) {
          whereClause += columnName + "= ";
          String valueStr = params.get("filter[filters][" + index + "][value]");
          Object value = null;
          if (StringUtils.isNotEmpty(valueStr)) {
            try {
              value = Integer.valueOf(valueStr);
            } catch (Exception e) {
            }
          }
          sqlParams.put(fieldName, value);
        } else if ("date".equals(type)) {
          String operator = params.get("filter[filters][" + index + "][operator]");
          if ("gte".equals(operator)) {
            whereClause += columnName + " >= ";
          } else if ("lte".equals(operator)) {
            whereClause += columnName + " <= ";
          } else {
            whereClause += columnName + " = ";
          }

          whereClause += " :" + fieldName + " ";
          // filter field value ex: filter[filters][0][value]="12/31/2020"
          LocalDate dt =
              parseDate(params.get("filter[filters][" + index + "][value]"), "MM/dd/yyyy");
          if (dt != null) sqlParams.put(fieldName, java.sql.Date.valueOf(dt));
          else {
            sqlParams.put(fieldName, null);
          }
        } else {
          whereClause += " " + columnName + " ilike ";
          // filter field value ex: filter[filters][0][value]
          // create bind sqlParamter. Since we are using the 'like' clause the value will be
          // '%something%'
          sqlParams.put(
              fieldName,
              "%" + params.get("filter[filters][" + index + "][value]") + "%");
        }
        whereClause += " :" + fieldName + " ";
      }
    }
    if (StringUtils.isNotEmpty(whereClause)) {
      whereClause = " where " + whereClause;
    }
    MutablePair<String, Map<String, Object>> pair = new MutablePair<>(whereClause, sqlParams);
    return pair;
  }

  /*
   * builds an 'order by' clause from the kendo request:
   *
   * Example kendo request with pagination and sort will look like:
   *
   * http://localhost:8080/person/api/persons?take=3&skip=0&page=1&pageSize=3
   *                                         &sort[0][field]=firstName
   *                                         &sort[0][dir]=desc
   *
   * @param params - kendo sort parameters as above
   * @return The order by clause. Ex: 'order by first_name desc' for the above request
   */
  public String getOrderByClause() {
    String clause = "";
    List<String> sortFieldIndexes = getSortFieldIndexes();
    if (CollectionUtils.isNotEmpty(sortFieldIndexes)) {
      for (String index : sortFieldIndexes) {
        // sort field ex: sort[0][field]
        String fieldName = params.get("sort[" + index + "][field]");
        // something like 'firstName' will get converted to 'first_name'
        String columnName = convertCamelCaseToSnakeCase(fieldName);
        QueryColumnOverride override = getOverrideForField(fieldName, columnOverrides);
        if (override != null) {
          columnName = override.getColumnName();
        }
        if (StringUtils.isNotEmpty(clause)) {
          clause += ",";
        }
        clause += " " + columnName + " ";
        // sort direction ex: sort[0][dir]
        clause += " " + params.get("sort[" + index + "][dir]") + " ";
      }
    } else {
      clause += " id desc"; // default
    }
    return " order by " + clause;
  }

  public String getOffsetLimitClause() {
    // defaults
    Integer skip = 0;
    Integer take = 10;
    if (params != null) {
      if (params.containsKey("skip")) {
        skip = Integer.valueOf(params.get("skip"));
      }
      if (params.containsKey("take")) {
        take = Integer.valueOf(params.get("take"));
      }
    }
    return " OFFSET " + skip + " LIMIT " + take;
  }

  public String getValueForField(String fieldName) {
    String val = "";
    List<String> filterFieldIndexes = getFilterFieldIndexes();
    if (CollectionUtils.isNotEmpty(filterFieldIndexes)) {
      for (String index : filterFieldIndexes) {
        // filter field name ex: filter[filters][0][field]
        String name = params.get("filter[filters][" + index + "][field]");
        if (StringUtils.equals(fieldName, name)) {
          val = params.get("filter[filters][" + index + "][value]");
          break;
        }
      }
    }
    return val;
  }

  /*
   * Get the list of sort indexes from urls like below:
   * http://localhost:8080/person/api/persons?take=3&skip=0&page=1&pageSize=3
   *                                         &sort[0][field]=firstName
   *                                         &sort[0][dir]=desc
   */
  private List<String> getSortFieldIndexes() {
    List<String> indexes = new ArrayList<>();
    for (String key : params.keySet()) {
      Matcher m = SORT_FIELD_PATTERN.matcher(key);
      while (m.find()) {
        indexes.add(m.group(1));
      }
    }
    log.info("Sort field indexes:{}", indexes);
    return indexes;
  }

  /*
   * Get the list of sort indexes from urls like below:
   * http://localhost:8080/person/api/persons?take=3&skip=0&page=1&pageSize=3
   *                                            &filter[logic]=and
   *                                            &filter[filters][0][field]=firstName
   *                                            &filter[filters][0][operator]=contains
   *                                            &filter[filters][0][value]=joe
   */
  private List<String> getFilterFieldIndexes() {
    List<String> indexes = new ArrayList<>();
    for (String key : params.keySet()) {
      Matcher m = FILTER_FIELD_PATTERN.matcher(key);
      while (m.find()) {
        indexes.add(m.group(1));
      }
    }
    log.info("Filter field indexes:{}", indexes);
    return indexes;
  }

  private String convertCamelCaseToSnakeCase(String propertyName) {
    String val = "";
    if (propertyName != null) {
      val = RegExUtils.replaceAll(propertyName, TO_SNAKE_CASE_PATTERN, "$1_$2").toLowerCase();
    }
    return val;
  }

  private QueryColumnOverride getOverrideForField(
      String fieldName, List<QueryColumnOverride> columnOverrides) {
    QueryColumnOverride override = null;
    if (columnOverrides != null) {
      for (QueryColumnOverride qco : columnOverrides) {
        if (StringUtils.equals(fieldName, qco.getFieldName())) {
          override = qco;
          break;
        }
      }
    }
    return override;
  }

  private LocalDate parseDate(String val, String format) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
    try {
      // Take a try
      return LocalDate.parse(val, formatter);
    } catch (Exception e) {
    }
    log.debug("Could not parse date from : {}", val);
    return null;
  }
}
