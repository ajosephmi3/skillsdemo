package org.skillsdemo.common;

import java.util.Arrays;
import java.util.List;

/**
 * Application constants.
 * 
 * @author ajoseph
 */
public class AppConstants {
  public static final String CORRELATION_ID_HEADER = "CORRELATION_ID";

  // urls for which there is no need to generate the correlation id.
  public static List<String> EXCLUDE_URLS =
      Arrays.asList("/css", "/js", "/img", "/fonts", "/login");

  public static final String DB_SCHEMA = "kira";
  public static final String TURBOLINKS_REDIRECT_LOCATION =
      "Application-Turbolinks-Redirect-Location";

  public static String SAVE = "save";
  public static String SUBMIT = "submit";
  public static String APPROVE = "approve";
  public static String REJECT = "reject";
  public static String DELETE = "delete";
}
