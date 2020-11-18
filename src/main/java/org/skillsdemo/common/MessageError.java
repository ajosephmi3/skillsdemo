package org.skillsdemo.common;

import java.io.Serializable;

import lombok.Data;

@Data
public class MessageError implements Serializable {

  private static final long serialVersionUID = 1L;

  private String objectName;
  private String field;
  private String message;
  private String rejectedValue;

  MessageError(String message) {
    this.message = message;
  }

  MessageError(String field, String message) {
    this.field = field;
    this.message = message;
  }

  MessageError(String objectName, String field, String message) {
    this(field, message);
    this.objectName = objectName;
  }

  MessageError(String objectName, String field, String message, String rejectedValue) {
    this(objectName, field, message);
    this.rejectedValue = rejectedValue;
  }
}
