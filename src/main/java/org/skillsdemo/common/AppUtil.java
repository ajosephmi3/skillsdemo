package org.skillsdemo.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.beanutils.PropertyUtils;
import org.jboss.logging.MDC;
import org.skillsdemo.model.Person;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;
/**
 * Some application utility methods
 * 
 * @author ajoseph
 */
@Slf4j
public class AppUtil {

  public static DateTimeFormatter defaultDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

  public static DateTimeFormatter usDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  public static DateTimeFormatter usDateTimeFormat =
      DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss");

  public static Person getLoggedInPerson() {
    AppUserPrincipal appUserPrincipal =
        (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return appUserPrincipal.getPerson();
  }

  public static Integer getLoggedInPersonId() {
    AppUserPrincipal appUserPrincipal =
        (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return appUserPrincipal.getPerson().getId();
  }

  public static String getFormattedDate(LocalDate date) {
    if (date != null) {
      return date.format(usDateFormat);
    } else {
      return "";
    }
  }

  public static String getFormattedDateTime(LocalDateTime dt) {
    if (dt != null) {
      return dt.format(usDateTimeFormat);
    } else {
      return "";
    }
  }

  public static LocalDate parseDate(String val, String format) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
    try {
      // Take a try
      return LocalDate.parse(val, formatter);
    } catch (Exception e) {
    }
    log.debug("Could not parse date from : {}", val);
    return null;
  }

  public static Object getProperty(Object obj, String propertyName) {
    Object val = null;
    try {
      val = PropertyUtils.getSimpleProperty(obj, propertyName);
    } catch (Exception e) {
      log.warn("Exception was thrown when trying to get property {}.", propertyName);
    }
    return val;
  }

  public static void setProperty(Object obj, String propertyName, Object value) {
    try {
      PropertyUtils.setProperty(obj, propertyName, value);
    } catch (Exception e) {
      log.warn("Exception was thrown when trying to set property {}.", propertyName);
    }
  }

  public static String getShortCorrelationId() {
    String val = "";
    String correlationId = (String) MDC.get(AppConstants.CORRELATION_ID_HEADER);
    if (correlationId != null) {
      String[] arr = correlationId.split("-");
      if (arr != null) {
        val = arr[0];
      }
    }
    return val;
  }
}
