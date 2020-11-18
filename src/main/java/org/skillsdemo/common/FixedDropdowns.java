package org.skillsdemo.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.skillsdemo.model.ValueAndText;

/**
 * Hard coded dropdowns.
 * 
 * @author ajoseph
 */
public class FixedDropdowns {
  private static List<ValueAndText> roles = new ArrayList<>();
  private static List<ValueAndText> accountStatuses = new ArrayList<>();

  static {
    roles.add(new ValueAndText("ROLE_ADMIN", "Admin"));
    roles.add(new ValueAndText("ROLE_MANAGER","Manager"));
    roles.add(new ValueAndText("ROLE_USER","User"));

    accountStatuses.add(new ValueAndText("1", "Active"));
    accountStatuses.add(new ValueAndText("0", "Disabled"));
  }

  public static List<ValueAndText> getRoleDropdownList() {
    return roles;
  }

  public static String getRoleText(String value) {
    return getTextFromValue(value, roles);
  }

  public static List<ValueAndText> getAccountStatusDropdownList() {
    return accountStatuses;
  }

  public static String getAccountStatusText(Integer value) {
    return getTextFromValue(value, accountStatuses);
  }

  private static String getTextFromValue(Object value, List<ValueAndText> list) {
    String text = "";
    String val = (value != null) ? value+"" : "";
    for (ValueAndText vat : list) {
      if (StringUtils.equals(val, vat.getValue())) {
        text= vat.getText();
      }
    }
    return text;
  }
}
