function PersonList(gridName, editable,
                    roleDropdownList, accountStatusDropdownList,
                    reportsToDropdownList, projectDropdownList) {

  let self = this;
  this.autocompleteSelectedId = null; // used for reportstoId autocomplete filter
  
  let grid = new GridCrud(gridName, editable);
  grid.beforeReadCallback(beforeReadCallback);
  grid.editFunctionOverride(editFunctionOverride);

  let dropdownHelper = new GridDropdownHelper(gridName);
  dropdownHelper.addPropertyDropdown('role', 'roleText', roleDropdownList, "Role is required");
  dropdownHelper.addPropertyDropdown('accountStatus', 'accountStatusText', accountStatusDropdownList);
  dropdownHelper.addPropertyDropdown('reportsToId', 'reportsToText', reportsToDropdownList);
  
  this.displayGrid = function () {	
    // kendo model definition
    let model = {};
    model['id'] = "id";
    model.fields = {
      username: { editable: editable, validation: { required: { message: 'Please enter value' } } },
      firstName: { editable: editable, validation: { required: { message: 'Please enter value' } } },
      lastName: { editable: editable, validation: { required: { message: 'Please enter value' } } },
      role: { editable: editable, validation: { required: { message: 'Please enter value' } } },
      roleText: {},
      projectNames: { editable: false },
      reportsToId: { editable: editable },
      phoneNum: { editable: editable },
      email: { editable: editable },
      accountStatus: { editable: false, defaultValue: 1 },
      accountStatusText: {},
      updatedBy: { editable: false },
      updatedOn: { editable: false },
      updatedInfo: { editable: false },
      projectId: { editable: false },
      reportsToFullName: {},
    }

    //kendo column definition
    let columns = [
      { field: "username", headerTemplate: 'Username <span class="k-icon k-i-kpi"></span>', width: 170, locked: true },
      { field: "firstName", headerTemplate: 'First Name <span class="k-icon k-i-kpi"></span>', width: 175, locked: true },
      { field: "lastName", headerTemplate: 'Last Name <span class="k-icon k-i-kpi"></span>', width: 175, locked: true },
      {
        field: "role", headerTemplate: 'Role <span class="k-icon k-i-kpi"></span>', width: 125,
        editor: dropdownHelper.dropdownEditor,
        template: "#: roleText #",
        filterable: { ui: roleFilter},
      },
      {
        field: "projectNames", title: "Projects Assigned", width: 225, sortable: false,
        filterable: { ui: projectFilter },
      },
      {
        field: "reportsToId", title: "Reports To", width: 200, sortable: false,
        editor: dropdownHelper.dropdownEditor,
        template: "#: reportsToFullName #",
        filterable: {ui: autoCompleteForReportsTo},
      },
      { field: "phoneNum", title: "Phone", width: 125, filterable: false, sortable: false },
      { field: "email", title: "Email", width: 225, filterable: false, sortable: false },
      {
        field: "accountStatus", title: "Account Status", width: 150, sortable: false,
        editor: dropdownHelper.dropdownEditor,
        template: "#: accountStatusText #",
        filterable: {ui: accountStatusFilter },
      },
      {
        field: "updatedInfo", title: "Last Update", width: 225, filterable: false, sortable: false,
        template: "#=updatedBy# #=updatedOn#"
      },
    ]

    // show edit project icon as first column for existing records. New records
    // won't have icon till record is saved
    columns.unshift({
      template: function (dataItem) {
        if (dataItem.id) {
          return " <a onClick=\"showPersonProjectsInModal('"
            + dataItem.id
            + "','" + dataItem.fullName + "')\" style='cursor:pointer'><i class='fa fa-pencil-square-o' aria-hidden='true'></i> </a>";
        } else {
          return "";
        }
      },
      width: 35,
      locked: true
    });
    
    let submitUrl = "/person/api/submit";
    let readUrl = "/person/api/persons";

    let serverPagination = true;
    grid.initialize(model, columns, submitUrl, readUrl, serverPagination);
  }

  function autoCompleteForReportsTo(element) {
	dropdownHelper.filterAutocomplete(element, "/person/api/personsautocomplete", self);
  }

  function roleFilter(element) {
	dropdownHelper.filterDropdown(element, roleDropdownList);
  }

  function projectFilter(element) {
	dropdownHelper.filterDropdown(element, projectDropdownList);
  }

  function accountStatusFilter(element) {
	dropdownHelper.filterDropdown(element, accountStatusDropdownList);
  }

  //existing records are not editable. Only new records where id is not present are editable.
  function editFunctionOverride(e) {
    var fieldName = this.columns[e.container.index()].field;
    let dataItem = this.dataItem(this.select());
    if (dataItem && dataItem['id'] && fieldName == 'username') {
      this.closeCell();
    }

    // need this for avoid user clicking twice to open dropdowns in grid
    dropdown = e.container.find('[data-role=dropdownlist]').data('kendoDropDownList');
    if (dropdown) {
      dropdown.open();
    }
  }

  // massages the query parameters the kendo filters generate for the call to get the backend.
  function beforeReadCallback(data) {
    let dataClone = data;
    if (data.filter) {
      dataClone = JSON.parse(JSON.stringify(data))
      //console.log(dataClone.filter.filters);
      for (var f of dataClone.filter.filters) {
        if (f.field == 'reportsToId') {
          if (self.autocompleteSelectedId) {
            f.value = self.autocompleteSelectedId;
            break;
          }
        }
      }
    }
    return dataClone;
  }

}