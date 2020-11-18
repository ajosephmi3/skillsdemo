package org.skillsdemo.model;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PersonProject {
  private Integer id;
  private Integer personId;

  @NotNull(message = "Project is required")
  private Integer projectId;

  private String projectName;
}
