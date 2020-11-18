package org.skillsdemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * When sending data to the front end for dropdowns etc this is the structure of the data kendo is
 * expecting.
 *
 * @author ajoseph
 */
@Data
@AllArgsConstructor
public class ValueAndText {
  private String value; // ex: project id
  private String text; // ex: 'project abc';
}
