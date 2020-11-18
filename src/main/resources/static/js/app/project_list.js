function ProjectList(gridName, editable) {
  var publicApi = {};
  var grid = new GridCrud(gridName, editable);
  // kendo model
  var model = {};
  model['id'] = "id";
  model.fields = {
    name: { editable: editable, validation: { required: { message: 'Please enter value' } } },
    description: { editable: editable },
    updatedBy: { editable: false },
    updatedOn: { editable: false },
    updatedInfo: { editable: false },
  }
  // kendo columns
  var columns = [{ field: "name", headerTemplate: 'Project Name <span class="k-icon k-i-kpi"></span>', filterable: true },
  { field: "description", headerTemplate: 'Description <span class="k-icon k-i-kpi"></span>', filterable: true, },
  { field: "updatedInfo", title: "Last Update", filterable: false, sortable: false, template: "#=updatedBy# #=updatedOn#" },]

  var submitUrl = "/project/api/submit";
  var readUrl = "/project/api/projects";

  publicApi.displayGrid = function () {
    grid.initialize(model, columns, submitUrl, readUrl);
  }

  return publicApi;
}