function TimesheetList(gridName, readUrl) {
  let grid = new GridCrud(gridName, false);
  
  this.displayGrid = function(){
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
    ];
    
    grid.initialize({}, columns, "", readUrl);
  }

}