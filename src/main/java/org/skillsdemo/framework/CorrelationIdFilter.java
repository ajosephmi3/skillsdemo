package org.skillsdemo.framework;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.skillsdemo.common.AppConstants;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Assigns a correlationId to each request to identify entries in the logs for a specific request
 * using MDC. The framework also sends the correlationId (short 8 char version) to the front end
 * when an unrecoverable exception occurs. This allows the full request to be traced in the logs
 * when user experiences issues.
 *
 * <p>See application.properties on for the log format to include correlationId
 *
 * @author ajoseph
 */
@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {
  // urls for which there is no need to generate the correlation id.
  private static List<String> EXCLUDE_URLS =
      Arrays.asList("/css", "/js", "/img", "/fonts", "/login");

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    // App/Web servers use pooled threads. Cleanup the MDC.
    MDC.remove(AppConstants.CORRELATION_ID_HEADER);

    final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    String path = httpServletRequest.getRequestURI();
    if (isExcludedUrl(path)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    String corrId = httpServletRequest.getHeader(AppConstants.CORRELATION_ID_HEADER);
    if (corrId == null) {
      corrId = UUID.randomUUID().toString();
    } else {
      // case where a microservice calls this apps endpoint with a correlationid
      log.debug("Found correlationId in Header : " + corrId);
    }

    MDC.put(AppConstants.CORRELATION_ID_HEADER, corrId);

    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void destroy() {}

  private boolean isExcludedUrl(String path) {
    boolean val = false;
    if (StringUtils.isNotBlank(path)) {
      for (String url : EXCLUDE_URLS) {
        if (path.startsWith(url)) {
          val = true;
          break;
        }
      }
    }
    return val;
  }
}
