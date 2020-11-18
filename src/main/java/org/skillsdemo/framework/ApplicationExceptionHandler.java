package org.skillsdemo.framework;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.skillsdemo.common.ApiError;
import org.skillsdemo.common.AppUtil;
import org.skillsdemo.common.JsonUtil;
import org.skillsdemo.exception.CustomValidationException;
import org.skillsdemo.exception.FineGrainedAuthorizationException;
import org.skillsdemo.exception.OptimisticLockingException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Application level exception handler. Converts some exceptions to user friendly responses.
 *
 * @author ajoseph
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  // Reqex to get value which caused a database unique key violation. Spring throws a
  // DuplicateKeyViolation
  // Format of message is like below:
  // .. Duplicate entry 'theValueWhichCausedTheDuplicateKeyException' for key ..
  public static Pattern duplicateValueRegex = Pattern.compile(".*Duplicate entry '(.*?)'.*");

  /**
   * This handler handles the @Valid annotation errors and converts the errors into a json response
   * with status HttpStatus.BAD_REQUEST (400)
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {

    ApiError apiError = new ApiError("Validation Error");

    for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
      apiError.addError(
          fieldError.getObjectName(),
          fieldError.getField(),
          fieldError.getDefaultMessage(),
          fieldError.getRejectedValue());
    }
    log.debug(JsonUtil.toJson(apiError));
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  /**
   * For simple validations the application uses JSR303 validator (hibernate validator) predefined
   * annotations. Anything which are not covered by the standard predefined annotations, custom
   * validation logic is written and a CustomValidationException is thrown on validation failure.
   * This handler converts the exception into a json response with status HttpStatus.BAD_REQUEST
   * (400)
   */
  @ExceptionHandler({CustomValidationException.class})
  public ResponseEntity<Object> handleValidationException(CustomValidationException e) {
    ApiError apiError = e.getApiError();
    log.debug(JsonUtil.toJson(apiError));
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  /**
   * When a duplicate key integrity violation occurs at a database sql level this handler converts
   * it into a friendly json response with status HttpStatus.BAD_REQUEST (400)
   */
  @ExceptionHandler({DuplicateKeyException.class})
  public ResponseEntity<Object> handleValidationException(DuplicateKeyException e) {
    ApiError apiError = new ApiError("Duplicate Error");
    String errorMsg = "Save failed!. Duplicate record ";
    if (e.getMessage() != null) {
      Matcher m = duplicateValueRegex.matcher(e.getMessage());
      if (m.matches()) {
        errorMsg += m.group(1);
      }
    }
    apiError.addError("", errorMsg);
    log.debug(JsonUtil.toJson(apiError));
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handle the case when spring security throws an access denied exception. The url and user is
   * logged. CorrelationId is send to client so we can track the request for troubleshooting
   */
  @ExceptionHandler({AccessDeniedException.class})
  public ResponseEntity<String> handleAccessDeniedException(
      AccessDeniedException e, ServletWebRequest request) {
    // log the user and url
    if (request.getUserPrincipal() != null) {
      log.error(
          "Access Denied: user:{} requestURL:{}",
          request.getUserPrincipal().getName(),
          ((HttpServletRequest) request.getNativeRequest()).getRequestURL());
    }

    log.error(e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            e.getMessage()
                + ". Error code: "
                + AppUtil.getShortCorrelationId()
                + ". <a href='/'>Back to application</a>");
  }

  /**
   * Handle the case when user tries to access/modify records they should not. The url and user is
   * logged along with any message. Sends out a generic system error message back to user.
   */
  @ExceptionHandler({FineGrainedAuthorizationException.class})
  public ResponseEntity<String> handleFineGrainedAuthorizationException(
      FineGrainedAuthorizationException e, ServletWebRequest request) {
    // log the user and url
    if (request.getUserPrincipal() != null) {
      log.error(
          "Fine grained authorization exception: user:{} requestUrl:{} message:{}",
          request.getUserPrincipal().getName(),
          ((HttpServletRequest) request.getNativeRequest()).getRequestURL(),
          e.getMessage());
    }

    log.error(e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(getDefaultErrorText());
  }

  /**
   * The DAO layer will throw an optimistic locking exception if the 'version' of the object is
   * stale. This is applicable only to models which have a 'version' property.
   */
  @ExceptionHandler({OptimisticLockingException.class})
  public ResponseEntity<ApiError> handleOptimisticLockingException(OptimisticLockingException e) {

    ApiError apiError = new ApiError("Stale Data");
    apiError.addError("The data you are working with is stale. Please refresh and try again.");

    log.debug(JsonUtil.toJson(apiError));

    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  /**
   * When unrecoverable Exception is thrown, log it and send the correlationId as error code to
   * client. This will help in troubleshooting since the error code send to user can be matched
   * against the logs
   */
  @ExceptionHandler({Exception.class})
  public ResponseEntity<String> handleException(Exception e, ServletWebRequest request) {
    String url = ((HttpServletRequest) request.getNativeRequest()).getRequestURL().toString();
    log.error(e.getMessage(), e);

    if (StringUtils.contains(url, "/api/")) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              "An application error occurred. Please contact support with error code: "
                  + AppUtil.getShortCorrelationId());
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(getDefaultErrorText());
    }
  }

  private String getDefaultErrorText() {
    return "A application error occurred. Please contact support with error code: <b>"
        + AppUtil.getShortCorrelationId()
        + "</b>.    <a href='/'>Back to application</a>";
  }

}
