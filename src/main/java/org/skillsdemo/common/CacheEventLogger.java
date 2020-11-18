package org.skillsdemo.common;

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

import lombok.extern.slf4j.Slf4j;
/**
 * Logs ehcache events
 * See application.properties for logging details.
 * 
 * @author ajoseph
 */
@Slf4j
public class CacheEventLogger implements CacheEventListener<Object, Object> {
  @Override
  /*
   * Logs the cache event. 
   */
  public void onEvent(CacheEvent<? extends Object, ? extends Object> cacheEvent) {
    log.debug("Cache event {} for item with key {}", cacheEvent.getType(), cacheEvent.getKey());
  }
}
