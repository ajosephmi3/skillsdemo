function PersonProjectList(gridName, editable, personId, projectDropdownList) {
  var publicApi = {}

  // destroy previous grid if any
  TeardownWidgets.destroyGridByName(gridName);

  let grid = new GridCrud(gridName, editable);
  grid.afterSaveCallback(afterSaveCallback);
  grid.editFunctionOverride(editFunctionOverride);
  
  // override the default grid options
  grid.setGridOptions({ filterable: false, sortable: false, pageable: false});

  var dropdownHelper = new GridDropdownHelper(gridName);
  dropdownHelper.addPropertyDropdown("projectId", "projectName", projectDropdownList, "Project is required");

  let model = {};
  model['id'] = "id";
  model.fields = {
    projectId: { editable: editable },
    projectName: {}
  }

  let columns = [{
    field: "projectId", title: "Project Name", width: 200, filterable: true,
    editor: dropdownHelper.dropdownEditor,
    template: "#: projectName #"
  },]

  let submitUrl = "/person/api/person/" + personId + "/projectssubmit";
  let readUrl = "/person/api/person/" + personId + "/projects";

  publicApi.displayGrid = function () {
    grid.initialize(model, columns, submitUrl, readUrl);
  }

  // refresh the person grid with the updated projects
  // close the popup dialog
  function afterSaveCallback() {
    $("#person_list_grid").data("kendoGrid").dataSource.read();
    $('#popup_modal').modal('hide');
  }

  //existing records are not editable. Only new records where id is not present are editable.
  function editFunctionOverride(e) {
    let dataItem = this.dataItem(this.select());
    if (dataItem && dataItem['id']) {
      this.closeCell();
    }

    // need this for avoid user clicking twice to open dropdowns in grid
    dropdown = e.container.find('[data-role=dropdownlist]').data('kendoDropDownList');
    if (dropdown) {
      dropdown.open();
    }
  }
  return publicApi;

}