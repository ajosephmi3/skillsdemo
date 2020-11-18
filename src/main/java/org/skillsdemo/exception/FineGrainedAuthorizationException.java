package org.skillsdemo.exception;

public class FineGrainedAuthorizationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public FineGrainedAuthorizationException(String payload) {
    super(payload);
  }

  public FineGrainedAuthorizationException(Throwable e) {
    super(e);
  }
}
