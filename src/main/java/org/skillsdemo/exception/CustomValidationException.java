package org.skillsdemo.exception;

import org.skillsdemo.common.ApiError;

/**
 * When this exception is thrown the ApplicationExceptionHandler.java will retrieve the error and
 * send it to UI as json which is then surfaced in the UI.
 *
 * @author ajoseph
 */
public class CustomValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private ApiError apiError;

  public CustomValidationException(String errorMsg) {
    this.apiError = new ApiError("Validation Exception");
    this.apiError.addError(errorMsg);
  }

  public CustomValidationException(ApiError apiError) {
    this.apiError = apiError;
  }

  public ApiError getApiError() {
    return apiError;
  }
}
