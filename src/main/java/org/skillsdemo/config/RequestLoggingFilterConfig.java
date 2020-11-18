package org.skillsdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
/**
 * Logs the request url and payload. 
 * 
 * See application.properties on how to turning on/off the request logging
 * 
 * @author ajoseph
 */
@Configuration
@Order(2)
public class RequestLoggingFilterConfig {
  /*
   * One drawback of Spring's CommonsRequestLoggingFilter used below is that the request data 
   * gets logged only at end of the request. Good enough for now.
   */
  @Bean
  public CommonsRequestLoggingFilter logFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(10000);
    filter.setIncludeHeaders(false);
    filter.setAfterMessagePrefix("Request Data: ");
    return filter;
  }
}
