package org.skillsdemo.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
/**
 * Wires up Ehcache 3.x which supports JSR107
 * 
 * See ehcache.xml for cache configuration
 * 	
 * @author ajoseph
 *
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
