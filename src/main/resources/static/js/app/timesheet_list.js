function TimesheetList(gridName, readUrl) {
  var publicApi = {}
  var grid = new GridCrud(gridName, false);
  // kendo columns
  let columns = [{
    field: "startDate",
    filterable: false,
    template: '<a href="/timesheet/#=id#">#=startDate# - #=endDate#</a>',
    headerTemplate: 'Period <span class="k-icon k-i-kpi"></span>'
  },
  { field: "fullName",
	headerTemplate: 'Name <span class="k-icon k-i-kpi"></span>'
  },
  { field: "status",
	headerTemplate: 'Status <span class="k-icon k-i-kpi"></span>'
  },
  { field: "totalHours", title: "Total Hours", filterable: false,
    headerTemplate: 'Total Hours <span class="k-icon k-i-kpi"></span>'
  },
  ]

  publicApi.displayGrid = function () {
    grid.initialize({}, columns, "", readUrl);
  }

  return publicApi;
}