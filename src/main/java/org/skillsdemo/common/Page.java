package org.skillsdemo.common;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class to support kendo pagination.
 * 
 * @author ajoseph
 */
@Data
@AllArgsConstructor
public class Page<T> {
    private Integer totalRecordCount;
    private List<T> records;
}

