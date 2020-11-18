function Timesheet(gridName, editable, allowApproval, timesheetId, datesList, userProjectsDropdownList) {
  grid = new GridCrud(gridName, editable);

  grid.customPayloadCallback(customPayloadCallback);
  grid.editFunctionOverride(editFunctionOverride);
  grid.setDataboundCallback(databoundCallback);

  // dropdowns for the grid
  var dropdownHelper = new GridDropdownHelper(gridName);
  dropdownHelper.addPropertyDropdown('projectId', 'projectName', userProjectsDropdownList, 'Project is required');

  // override the default grid options
  grid.setGridOptions({ filterable: false, sortable: false, pageable: false});

  // kendo model definition
  let model = {};
  model['id'] = "id";
  model.fields = {
    projectId: { editable: editable },
    projectName: { editable: editable },
  }
  // hour fields for model.
  for (let i = 0; i < datesList.length; i++) {
    let dayFieldName = "hoursDay" + i;
    model.fields[dayFieldName] = { editable: editable, validation: { functionvalidation: validateHours } };
  }
  model.fields["rowTotal"] = { editable: true }

  // kendo column definition
  let columns = [
    {
      field: "projectId", title: "project", width: 200, filterable: false,
      editor: dropdownHelper.dropdownEditor,
      template: "#: projectName #"
    }];

  // the hour columns
  for (let i = 0; i < datesList.length; i++) {
    let dayFieldName = "hoursDay" + i;
    columns.push({ field: dayFieldName, title: datesList[i], footerTemplate: "<div style='font-weight: normal' id='" + dayFieldName + "columnTotal" + "'></div>" });
  }

  // the rowTotal column
  columns.push({
    field: "rowTotal", title: "Total",
    template: calcRowTotal,
    footerTemplate: "<div id='grandTotal'></div>",
    headerAttributes: { style: "background-color: #20b2aa; color:black !important;font-weight: bold" }
  })

  let submitUrl = "/timesheet/api/timesheetId/" + timesheetId + "/submit";
  let readUrl = "/timesheet/api/timesheetId/" + timesheetId
    + "/timesheetrows";


  let toolbar = [{ name: "create", text: "Add New Row" },
  { name: "save", text: 'Save As Draft' },
  { name: "Submit", iconClass: "k-icon k-i-check-circle" },
  { name: 'cancel', text: 'Cancel Changes' }];

  if (!editable) {
    if (allowApproval) {
      toolbar = [{ name: "Approve", iconClass: "k-icon k-i-check-circle" },
      { name: "Reject", iconClass: "k-icon k-i-undo" },
      ];
    }
    else {
      toolbar = [];
    }
  }

  //console.log(toolbar)
  grid.setToolbarOptions(toolbar);

  this.displayGrid = function () {
    grid.initialize(model, columns, submitUrl, readUrl);
  }

  this.getGrid = function () {
    return this.grid;
  }

  /*
   * Need to send more than just the Grid data to the backend. This method does that.
   */
  function customPayloadCallback(gridSubmitData) {
    var payload = {};
    payload.userComments = $('#userComments').val();
    payload.approverComments = $('#approverComments').val();
    payload.gridSubmitData = gridSubmitData;
    payload.action = $('#action').val();
    return payload;
  }

  function databoundCallback(e) {
    calcColumnTotalAndDisplay(e)
  }

  /*
   * Does the following:
   * 1) Limits hour fields to 5 chars
   * 2) Does not allow edit of 'rowTotal' column field
   * 3) if an hour field value is changed recalculates column and row totals.
   * 4) For dropdown field makes sure that the dropdown shows on a single click. 
   *         (Default kendo behaviour for dropdowns is 2 seperate clicks to open. 
   *          first click for focus to the field and 2nd to open the dropdown)
   */
  function editFunctionOverride(e) {
    let selectedRows = this.select();
    let dataItem = this.dataItem(selectedRows[0]);
    if (dataItem) {
      var fieldName = this.columns[e.container.index()].field;
      if (fieldName && fieldName.startsWith('hoursDay')) {
        let inputHtml = "input[name='" + fieldName + "']";
        // something like "input[name='hoursDay0']"
        e.container.find(inputHtml).attr('maxlength', '5');
      }

      if (fieldName && fieldName == 'rowTotal') {
        this.closeCell();
      }

      if (fieldName && fieldName == 'projectId' && !e.model.isNew()) {
        dropdown = e.container.find('[data-role=dropdownlist]').data('kendoDropDownList');
        if (dropdown) {
          dropdown.open();
        }
      }
    }

    e.model.bind("change", function (j) {
      if (fieldName.startsWith('hoursDay')) {
        calcColumnTotalAndDisplay(e);
        let rowTotal = calcRowTotal(dataItem);
        e.model.set("rowTotal", rowTotal)
      }
    });

  }

  /*
   * 1) Validates numeric
   * 2) Validates the hours to be less be between 0 and 24
   * 3) Validates max 2 decimal places.
   */
  function validateHours(input) {
    if (input.val()) {
      value = Number(input.val()).toFixed(2);
      if (value == 'NaN') {
        input.attr("data-functionvalidation-msg",
          "Value has to be numeric");
        return false;
      } else if (value < 0.0 || value > 24.0) {
        input.attr("data-functionvalidation-msg",
          "Value has to be between 0 and 24");
        return false;
      }
      else {
        let strVal = input.val();
        let arr = strVal.split('.');
        if (arr.length > 1) {
          if (arr[1].length > 2) {
            input.attr("data-functionvalidation-msg",
              "Value can only have upto 2 decimal places");
            return false;
          }
        }
      }
    }
    return true;
  }

  function calcColumnTotalAndDisplay(e) {
    let grid = $("#timesheet_grid").data("kendoGrid");
    let items = grid.items(); // items in the grid
    let grandTotal = 0;
    for (let i = 0; i < datesList.length; i++) {
      let total = 0.0;
      items.each(function (idx, row) {
        let dataItem = grid.dataItem(row);
        if (dataItem["hoursDay" + i]) {
          let value = Number(dataItem["hoursDay" + i]);
          if (value != 'NaN') {
            total += value;
            grandTotal += value;
          }
        }
      });
      let el = '#hoursDay' + i + "columnTotal";
      $(el).text(total.toFixed(2));
    }

    $('#grandTotal').text(grandTotal.toFixed(2));

  }

  function calcRowTotal(dataItem) {
    let total = 0.0;
    for (let i = 0; i < datesList.length; i++) {
      if (dataItem['hoursDay' + i]) {
        let value = Number(dataItem["hoursDay" + i]);
        if (value != 'NaN') {
          total += value;
        }
      }
    }
    return total.toFixed(2);
  }

  // submit button
  $("#timesheet_grid").on('click', '.k-grid-Submit', function (e) {
    e.preventDefault();
    $('#action').val('Submit');
    $("#timesheet_grid").data("kendoGrid").saveChanges();
    // set it back to the default value
    $('#action').val('Save');
  });

  // Approve button
  $("#timesheet_grid").on('click', '.k-grid-Approve', function (e) {
    e.preventDefault();
    $('#action').val('Approve');
    $("#timesheet_grid").data("kendoGrid").saveChanges();
  });

  //Reject button
  $("#timesheet_grid").on('click', '.k-grid-Reject', function (e) {
    e.preventDefault();
    $('#action').val('Reject');
    $("#timesheet_grid").data("kendoGrid").saveChanges();
  });

}