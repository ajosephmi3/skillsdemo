package org.skillsdemo.config;

import org.skillsdemo.common.TwitterSnowflake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure the twittersnowflake sequence generator which can generate unique sequences
 * of type 'Long' in a distributed environment.
 * 
 * See class TwitterSnowflake.java for more info
 * 
 * @author ajoseph
 *
 */
@Configuration
public class SequenceGeneratorConfig {

  @Bean(name = "SequenceGenerator")
  public TwitterSnowflake sequenceGenerator() {
    return new TwitterSnowflake();
  }
}
