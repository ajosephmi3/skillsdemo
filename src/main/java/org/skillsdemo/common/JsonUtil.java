package org.skillsdemo.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/** 
 * Utility class to convert java class to json and vice versa 
 */
@Slf4j
public final class JsonUtil {

  private static ObjectMapper mapper;

  static {
    log.info("Initializing ObjectMapper");
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    log.debug("ObjectMapper initialized");
  }

  private JsonUtil() {}

  /**
   * Convert String to Object of type T.
   *
   * @param jsonString to convert to object of type T
   * @param clazz of object
   * @return Object of type T
   * @throws IllegalArgumentException if an IOException occurs during conversion
   */
  public static <T> T toObject(String jsonString, Class<T> clazz) {
    try {
      return mapper.readValue(jsonString, clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Error converting Json to " + clazz.getName() + ", json string = " + jsonString, e);
    }
  }

  /**
   * Convert String to Object of type T.
   *
   * @param jsonString to convert to object of type T
   * @param typeRef of TypeReference (to handle generic types)
   * @return Object of type T
   * @throws IllegalArgumentException if an IOException occurs during conversion
   */
  public static <T> T toObject(String jsonString, TypeReference<T> typeRef) {
    try {
      return mapper.readValue(jsonString, typeRef);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Error converting Json to "
              + typeRef.getClass().getName()
              + ", json string = "
              + jsonString,
          e);
    }
  }

  /**
   * Convert Inputstream to Object of type T.
   *
   * @param is to convert to object of type T
   * @param clazz of the object
   * @return Object of type T
   * @throws IllegalArgumentException if an IOException occurs during conversion
   */
  public static <T> T toObject(InputStream is, Class<T> clazz) {
    try {
      return mapper.readValue(is, clazz);
    } catch (IOException e) {
      throw new IllegalArgumentException("Error converting Json to " + clazz.getName(), e);
    }
  }

  /**
   * Convert String to Object of type T.
   *
   * @param is to convert to object of type T
   * @param typeRef of TypeReference (to handle generic types)
   * @return Object of type T
   * @throws IllegalArgumentException if an IOException occurs during conversion
   */
  public static <T> T toObject(InputStream is, TypeReference<T> typeRef) {
    try {
      return mapper.readValue(is, typeRef);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Error converting Json to " + typeRef.getClass().getName() + ", json string = " + is, e);
    }
  }

  /**
   * Convert Object of type T to Json String.
   *
   * @param object of type T
   * @return json string
   * @throws IllegalArgumentException if an IOException occurs during conversion
   */
  public static <T> String toJson(T object) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Error converting " + object.getClass().getName() + " to Json", e);
    }
  }

  public static <T> String toUnformattedJson(T object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Error converting " + object.getClass().getName() + " to Json", e);
    }
  }

  public static JsonNode toJsonNode(String jsonStr) {
    try {
      return mapper.readTree(jsonStr);

    } catch (IOException e) {
      throw new IllegalArgumentException("Error converting " + jsonStr + " to JsonNode", e);
    }
  }

  public static <T> Map<String, Object> toMap(T t) {
    return mapper.convertValue(t, new TypeReference<Map<String, Object>>() {});
  }
}
