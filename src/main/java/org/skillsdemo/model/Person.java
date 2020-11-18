package org.skillsdemo.model;

import java.time.LocalDateTime;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.skillsdemo.common.FixedDropdowns;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

@Data
public class Person {
  private Integer id;

  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "First Name is required")
  private String firstName;

  @NotBlank(message = "Last Name is required")
  private String lastName;

  private String phoneNum;

  @Email(message = "Email format is not valid")
  private String email;

  @JsonSerialize(using = ToStringSerializer.class)
  private Integer reportsToId;

  private String createdBy;
  private String updatedBy;

  @JsonFormat(pattern = "MM/dd/yyyy HH:mm:ss")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime createdOn;

  @JsonFormat(pattern = "MM/dd/yyyy HH:mm:ss")
  @JsonProperty(access = Access.READ_ONLY)
  private LocalDateTime updatedOn;

  @Pattern(regexp = "(ROLE_ADMIN|ROLE_USER|ROLE_MANAGER)", message = "Role is required")
  private String role;

  private Integer accountStatus;

  private String reportsToFullName;

  private String projectNames;

  public boolean isAccountDisabled() {
    return accountStatus == null || accountStatus == 0 ? true : false;
  }

  public String getFullName() {
    return firstName + " " + lastName;
  }

  public String getRoleText() {
    return FixedDropdowns.getRoleText(role);
  }

  public String getAccountStatusText() {
    return FixedDropdowns.getAccountStatusText(accountStatus);
  }

  public boolean isAdmin() {
    return StringUtils.equals(this.role, "ROLE_ADMIN");
  }

  public boolean isManager() {
    return StringUtils.equals(this.role, "ROLE_MANAGER");
  }
}
