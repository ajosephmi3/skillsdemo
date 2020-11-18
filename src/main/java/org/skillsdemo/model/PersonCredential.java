package org.skillsdemo.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PersonCredential {
  private Integer id;
  private Integer personId;
  private String password;

  public PersonCredential(Integer personId, String password) {
    this.personId = personId;
    this.password = password;
  }
}
