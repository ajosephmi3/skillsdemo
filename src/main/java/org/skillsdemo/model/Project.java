package org.skillsdemo.model;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Project {
  private Integer id;

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Description is required")
  private String description;

  private String createdBy;
  private String updatedBy;

  @JsonFormat(pattern = "MM/dd/yy HH:mm")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime createdOn;

  @JsonFormat(pattern = "MM/dd/yy HH:mm")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime updatedOn;

  // table person_project
  private Integer personId;

  public Project(Integer id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }
}
