package org.skillsdemo.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.skillsdemo.model.TimeEntry;
import org.skillsdemo.model.Timesheet;
import org.skillsdemo.model.TimesheetLine;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class TimesheetDao extends BaseDao {

  public List<Timesheet> getMyTimesheets(Integer personId) {
    String sql =
        String.join(
            " ",
            "select t.id, t.start_date, t.end_date, t.status, p.last_name, p.first_name, sum(te.entry_hours) total_hours",
            "from timesheet t",
            "join person p on t.person_id = p.id",
            "left join timesheet_line tl on t.id = tl.timesheet_id",
            "left join time_entry te on tl.id = te.timesheet_line_id",
            "where t.person_id = ? ",
            "group by t.id, t.start_date, t.end_date, t.status, p.last_name, p.first_name",
            "order by t.start_date desc");
    RowMapper<Timesheet> mapper = BeanPropertyRowMapper.newInstance(Timesheet.class);
    return jdbcTemplate.query(sql, mapper, personId);
  }

  public List<Timesheet> getEmployeeTimesheets(Integer reportsToId) {
    String sql =
        String.join(
            " ",
            "select t.id, t.start_date, t.end_date, t.status, p.last_name, p.first_name, sum(te.entry_hours) total_hours",
            "from timesheet t",
            "join person p on t.person_id = p.id",
            "left join timesheet_line tl on t.id = tl.timesheet_id",
            "left join time_entry te on tl.id = te.timesheet_line_id",
            "where p.reports_to_id = ?",
            "and t.status != 'Draft' ",
            "group by t.id, t.start_date, t.end_date, t.status, p.last_name, p.first_name",
            "order by t.start_date desc");
    RowMapper<Timesheet> mapper = BeanPropertyRowMapper.newInstance(Timesheet.class);
    return jdbcTemplate.query(sql, mapper, reportsToId);
  }

  public Timesheet fetchFullTimesheet(Integer timesheetId) {
    Timesheet timesheet = fetchTimesheetAndRelatedLines(timesheetId);
    if (timesheet == null) {
      return null;
    }
    List<TimesheetLine> timesheetLines = timesheet.getTimesheetLines();
    if (CollectionUtils.isNotEmpty(timesheetLines)) {
      // get the list of timesheetLineIds of timesheet
      List<Integer> timesheetLineIds =
          timesheetLines.stream().map(TimesheetLine::getId).collect(Collectors.toList());
      List<TimeEntry> timeEntries = fetchEntriesByLineIds(timesheetLineIds);
      if (CollectionUtils.isNotEmpty(timeEntries)) {
        // Map the timesheetLineIds to their corresponding timesheet entries
        // map with key: timesheetLineId and value: List<TimesheetEntry>
        Map<Integer, List<TimeEntry>> timesheetLineIdEntries =
            timeEntries.stream().collect(Collectors.groupingBy(TimeEntry::getTimesheetLineId));
        // assign the tasks to the projects
        timesheetLines.forEach(l -> l.setTimeEntries(timesheetLineIdEntries.get(l.getId())));
      }
    }
    return timesheet;
  }

  // fetches the timesheet and its TimesheetLines. Does NOT fetch the time entries
  private Timesheet fetchTimesheetAndRelatedLines(Integer timesheetId) {
    String sql =
        String.join(
            " ",
            "select t.*,p.last_name, p.first_name,",
            "tl.id timesheetline_id, tl.timesheet_id timesheetline_timesheet_id, tl.project_id timesheetline_project_id, proj.name timesheetline_project_name",
            "from timesheet t",
            "left join timesheet_line tl on t.id = tl.timesheet_id",
            "join person p on t.person_id = p.id",
            "left join project proj on tl.project_id = proj.id",
            "where t.id = ?",
            "order by t.id, tl.id");

    List<Timesheet> timesheetList =
        jdbcTemplate.query(
            sql,
            new Object[] {timesheetId},
            rs -> {
              return jdbcUtil.oneToManyMapper(
                  rs, Timesheet.class, TimesheetLine.class, "timesheetline_");
            });

    return CollectionUtils.isNotEmpty(timesheetList) ? timesheetList.get(0) : null;
  }

  public boolean timesheetExists(Integer personId, LocalDate timesheetStartDate) {
    String sql = "select * from timesheet  where person_id = ? and start_date = ?";
    RowMapper<Timesheet> mapper = BeanPropertyRowMapper.newInstance(Timesheet.class);
    List<Timesheet> list =
        jdbcTemplate.query(sql, mapper, personId, java.sql.Date.valueOf(timesheetStartDate));
    return CollectionUtils.isNotEmpty(list);
  }

  public Integer updateTimesheetLine(TimesheetLine timesheetLine) {
    String sql = "update timesheet_line set project_id = ? where id = ? and timesheet_id = ?";
    return jdbcTemplate.update(
        sql, timesheetLine.getProjectId(), timesheetLine.getId(), timesheetLine.getTimesheetId());
  }

  public Integer deleteTimesheetLine(Integer timesheetId, Integer timesheetLineId) {
    String sql = "delete from timesheet_line where timesheet_id = ? and id = ?";
    return jdbcTemplate.update(sql, timesheetId, timesheetLineId);
  }

  public void deleteTimeEntriesByLineId(Integer timesheetLineId) {
    String sql = "delete from time_entry where timesheet_line_id = ?";
    jdbcTemplate.update(sql, timesheetLineId);
  }

  public List<TimeEntry> fetchEntriesByLineIds(List<Integer> timesheetLineIds) {
    MapSqlParameterSource params = new MapSqlParameterSource("timesheetLineIds", timesheetLineIds);
    String sql =
        String.join(
            " ",
            "select *",
            "from time_entry",
            "where timesheet_line_id in (:timesheetLineIds)",
            "order by timesheet_line_id, entry_date");
    RowMapper<TimeEntry> mapper = BeanPropertyRowMapper.newInstance(TimeEntry.class);
    return npJdbcTemplate.query(sql, params, mapper);
  }

  // batch inserts for performance reasons
  public int[][] batchInsertTimeEntries(List<TimeEntry> entries, int batchSize) {
    String insertSql =
        String.join(
            " ",
            "insert into time_entry",
            "(timesheet_line_id, entry_date, entry_hours)",
            "values(?,?,?)");

    int[][] insertCounts =
        jdbcTemplate.batchUpdate(
            insertSql,
            entries,
            batchSize,
            new ParameterizedPreparedStatementSetter<TimeEntry>() {
              public void setValues(PreparedStatement ps, TimeEntry entry) throws SQLException {
                ps.setInt(1, entry.getTimesheetLineId());
                ps.setDate(2, java.sql.Date.valueOf(entry.getEntryDate()));
                ps.setObject(3, entry.getEntryHours(), java.sql.Types.DOUBLE);
              }
            });
    return insertCounts;
  }

  // batch updates for performance reasons
  public int[][] batchUpdateTimeEntries(List<TimeEntry> entries, int batchSize) {
    String updateSql =
        String.join(
            " ",
            "update time_entry",
            "set entry_hours = ?",
            "where timesheet_line_id = ?",
            "and entry_date = ?");

    int[][] updateCounts =
        jdbcTemplate.batchUpdate(
            updateSql,
            entries,
            batchSize,
            new ParameterizedPreparedStatementSetter<TimeEntry>() {
              public void setValues(PreparedStatement ps, TimeEntry entry) throws SQLException {
                ps.setObject(1, entry.getEntryHours(), java.sql.Types.DOUBLE);
                ps.setInt(2, entry.getTimesheetLineId());
                ps.setDate(3, java.sql.Date.valueOf(entry.getEntryDate()));
              }
            });

    return updateCounts;
  }
}
