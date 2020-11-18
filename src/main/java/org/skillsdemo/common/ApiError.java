package org.skillsdemo.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * Api error message which is sent to UI as json to be displayed to user.
 * 
 * @author ajoseph
 */
@Data
public class ApiError implements Serializable {

  private static final long serialVersionUID = 1L;

  private String status = HttpStatus.BAD_REQUEST.toString();

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
  private LocalDateTime timestamp = LocalDateTime.now();

  private String apiMessage;
  private List<MessageError> errors = new ArrayList<>();

  public ApiError() {}

  public ApiError(String message) {
    this.apiMessage = message;
  }

  public ApiError(List<MessageError> errors) {
    this();
    if (errors != null) {
      this.errors = new ArrayList<>(errors);
    } else {
      this.errors = null;
    }
  }

  public ApiError(String message, List<MessageError> errors) {
    this(errors);
    this.apiMessage = message;
  }

  public void addError(MessageError error) {
    if (error != null) {
      errors.add(error);
    }
  }

  public void addError(String message) {
    addError(new MessageError(message));
  }

  public void addError(String field, String message) {
    addError(new MessageError(field, message));
  }

  public void addError(String object, String field, String message) {
    addError(new MessageError(object, field, message));
  }

  public void addError(String object, String field, String message, Object rejectedValue) {
    if (rejectedValue != null) {
      addError(new MessageError(object, field, message, rejectedValue.toString()));
    } else {
      addError(new MessageError(object, field, message));
    }
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
