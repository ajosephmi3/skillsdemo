/*
 * The app uses turbolinks so it is kind of a Single Page App.
 * Because of turbolinks the DOM needs to be kept clean programatically.
 * Javascript widgets like grids, datepickers, modals etc need to be removed from the DOM before 
 * rendering of a new page.
 * To facilitate this, widgets are tagged with specific attributes and are identified and 
 * removed from DOM using the teardown() method. The teardown() method is invoked from main.js 
 * on the 'turbolinks:before-render' event
 * 
 * Example of attribute tags: all kendo grids are tagged with attributes 'data-js-kendo-grid'
 *  
 */

let TeardownWidgets = (function () {
  let publicApi = {};

  // destroys javascript widgets to keep the DOM clean. Invoked from main.js
  publicApi.teardown = function () {
    console.log("START teardown");
    destroyKendoGrids();
    destroyDatePickers();
    destroyModals();
    //destroySidebar();
    console.log("END teardown");
  }

  // All kendo grids should be tagged with marker data attribute 'data-js-kendo-grid' so they
  // can be identified and destroyed.
  function destroyKendoGrids() {
    $('[data-js-kendo-grid]').each(function () {
      let grid = $(this).data("kendoGrid");
      if (grid) {
        console.log("destroying grid " + $(this).attr("id"));
        grid.destroy();
      }
    })
  }

  // All date pickers should be tagged with marker data attribute 'data-js-datepicker' so they
  // can be identified and destroyed.
  function destroyDatePickers() {
    $('[data-js-datepicker]').each(function () {
      var datepicker = $(this).data("kendoDatePicker");
      if (datepicker) {
        console.log("destroying datepicker " + $(this).attr("id"));
        datepicker.destroy(); // detaches all events.
        $(this).remove(); // remove from dom
      }
    })
  }
  
  function destroyModals() {
	$('[data-js-modal]').each(function () {
	  console.log("destroying modal " + $(this).attr("id"));
	  $(this).remove();
	})
  }
    
  // destroys a kendo grid by name
  publicApi.destroyGridByName = function (gridName) {
	let grid = $("#" + gridName).data("kendoGrid");
	if (grid) {
	  console.log("destroying grid by name: " + gridName);
	  grid.destroy();
	}
  }

  // expose the public api.
  return publicApi;
})();