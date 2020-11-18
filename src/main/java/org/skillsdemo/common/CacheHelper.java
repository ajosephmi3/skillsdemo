package org.skillsdemo.common;

import org.skillsdemo.model.Timesheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Ehcache helper wrapper for the app.
 * 
 * @author ajoseph
 */
@Component
public class CacheHelper {
  @Autowired private CacheManager cacheManager;

  public void putTempTimesheetIntoCache(String key, Timesheet timesheet) {
    Cache cache = cacheManager.getCache("tempTimesheetCache");
    cache.put(key, timesheet);
  }

  public Timesheet getTempTimesheetFromCache(String key) {
    Cache cache = cacheManager.getCache("tempTimesheetCache");
    ValueWrapper wrapper = cache.get(key);
    return wrapper != null ? (Timesheet) wrapper.get() : null;
  }

  public void evictTempTimesheetFromCache(String key) {
    Cache cache = cacheManager.getCache("tempTimesheetCache");
    cache.evict(key);
  }

  public void putRedirectMessageIntoCache(String key, String message) {
    Cache cache = cacheManager.getCache("flashMessageCache");
    cache.put(key, message);
  }

  public String getAndEvictRedirectMessageFromCache(String key) {
    String message = null;
    Cache cache = cacheManager.getCache("flashMessageCache");
    ValueWrapper wrapper = cache.get(key);
    if (wrapper != null) {
      message = wrapper != null ? (String) wrapper.get() : null;
      cache.evict(key);
    }
    return message;
  }
}
