function ProjectList(gridName, editable) {
 
  let grid = new GridCrud(gridName, editable);
	
  this.displayGrid = function () {
    // kendo model
    let model = {};
    model['id'] = "id";
    model.fields = {
      name: { editable: editable, validation: { required: { message: 'Please enter value' } } },
      description: { editable: editable },
      updatedBy: { editable: false },
      updatedOn: { editable: false },
      updatedInfo: { editable: false },
    }
    // kendo columns
    let columns = [{ field: "name", headerTemplate: 'Project Name <span class="k-icon k-i-kpi"></span>', filterable: true },
                   { field: "description", headerTemplate: 'Description <span class="k-icon k-i-kpi"></span>', filterable: true, },
                   { field: "updatedInfo", title: "Last Update", filterable: false, sortable: false, template: "#=updatedBy# #=updatedOn#" },]

    let submitUrl = "/project/api/submit";
    let readUrl = "/project/api/projects";

    grid.initialize(model, columns, submitUrl, readUrl);
  }

}