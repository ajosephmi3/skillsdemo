package org.skillsdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * Default spring scheduler which allows scheduling jobs using cron like syntax
 * 
 * @author ajoseph
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {}
