package org.skillsdemo.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Sometimes the data sent from UI will not have match the property/column names
 * of object/table. This allows the KendoQueryBuilder.java to build the appropriate
 * queries. 
 * 
 * @author ajoseph
 */
@Data
@AllArgsConstructor
public class QueryColumnOverride {
	private String fieldName; // field name sent by UI
	private String columnName; // the corresponding sql column name
	private String type; // type used by query builder for conversion. "string", "integer" etc
}
