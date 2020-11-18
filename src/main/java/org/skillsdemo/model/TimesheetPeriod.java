package org.skillsdemo.model;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class TimesheetPeriod {
  @NotNull(message = "Period is required")
  @JsonFormat(pattern = "MM/dd/yyyy")
  private LocalDate periodDate;
}
