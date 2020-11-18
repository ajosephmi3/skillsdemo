/**
 *  dropdowns used by Kendo grid are managed here.
 *  
 *  See person_grid.js for example usage
 *  
 *  @author ajoseph
 */

function GridDropdownHelper(gridName) {
  let dropdowns = {};

  this.addPropertyDropdown = function (valuePropertyName, textPropertyName, dropdownArray, requiredMessage) {
    dropdowns[valuePropertyName] =
    {
      textPropertyName: textPropertyName, dropdownArray: dropdownArray,
      requiredMessage: requiredMessage
    };
  }

  this.dropdownEditor = function (container, options) {
    let ddArray = dropdowns[options.field].dropdownArray;
    let inputStr = '<input name="' + options.field + '"/>';

    // if required field
    if (dropdowns[options.field].requiredMessage) {
      inputStr = '<input required validationMessage="' + dropdowns[options.field].requiredMessage + '" name="' + options.field + '"/>'
    }

    $(inputStr).appendTo(container)
      .kendoDropDownList({
        autoBind: false,
        valuePrimitive: true,
        dataTextField: "text",
        dataValueField: "value",
        // Once value is changed need to show text (not value) in the grid
        change: function (e) {
          if (dropdowns[options.field].textPropertyName) {
            var ddl = this;
            var text = ddl.dataItem().text;
            var editedRow = ddl.element.closest("tr");
            var model = $("#" + gridName).data("kendoGrid").dataItem(editedRow);
            model.set(dropdowns[options.field].textPropertyName, text);
          }
        },
        dataSource: ddArray,
      });
  }
}