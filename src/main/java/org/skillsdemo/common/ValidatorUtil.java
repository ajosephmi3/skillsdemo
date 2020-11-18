package org.skillsdemo.common;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.skillsdemo.exception.CustomValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates the object in the list as per the JSR303 annotations of the object
 * 
 * @author ajoseph
 */
@Component
public class ValidatorUtil {
  @Autowired Validator validator;

  /**
   * Validates the object in the list as per the JSR303 annotations of the object
   */
  public <T> void validateList(List<T> list) {
    for (T t : list) {
      Set<ConstraintViolation<T>> violations = validator.validate(t);
      Optional<ConstraintViolation<T>> violation = violations.stream().findFirst();
      if (violation.isPresent()) {
        throw new CustomValidationException(violation.get().getMessage());
      }
    }
  }
}
