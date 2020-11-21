// toastr configuration
toastr.options.closeButton = true;
toastr.options.preventDuplicates = true;
toastr.options.tapToDismiss = false;

// Force toastr.error messages to be sticky
toastr.error_ = toastr.error;
toastr.error = function (a, b, c) {
  let t = toastr.options.timeOut;
  let et = toastr.options.extendedTimeOut;
  toastr.options.timeOut = 0;
  toastr.options.extendedTimeOut = 0;
  toastr.error_(a, b, c);
  toastr.options.timeOut = t;
  toastr.options.extendedTimeOut = et;
};

// Since turbolinks is kind of a Single Page App need to keep the DOM clean.
// teardown (ie cleanup) the DOM before each turbolinks render
document.addEventListener('turbolinks:before-render', () => {
  TeardownWidgets.teardown();
});

// By default there is no way to identify a turbolinks request from a standard ajax request.
// This event adds a request header to all turbolinks requests.
document.addEventListener("turbolinks:request-start", function (event) {
  var xhr = event.data.xhr
  xhr.setRequestHeader("Application-Turbolinks-Request", 'true');
});

// 1)sets the active link on the sidebar. Note all sidebar links are expected to have 
//   an id starting with 'nav_' and follow the naming convention
// 2) manages the display(show/hide) of sidebar also
// Note: login.html page removes application related cookies.
document.addEventListener('turbolinks:load', function() {
   // for location /timesheet/mytimesheets will return 'mytimesheets'
   var path = window.location.href.split("/").pop();  
   if ($("#nav_" + path).length) {
     $("#nav_" + path).addClass("highlight");
     Cookies.set('app_nav_link',path)
   }
   else {
	 path = Cookies.get('app_nav_link');
	 $("#nav_" + path).addClass("highlight");
   }

   // manage the sidebar display. Note in login.html we 
   var sidebarCollapsed = Cookies.get('app_sidebar_collapsed');
   if(sidebarCollapsed && $('#sidebar').is(":visible")){
	   $("#sidebar").hide();
   }
  
});

// sidebar toggle
$(document).on('click', '.sidebar-toggle', function(){
	var sidebarCollapsed = Cookies.get('app_sidebar_collapsed');
	if(sidebarCollapsed){
		Cookies.remove('app_sidebar_collapsed');
		$("#sidebar").show();
	}
	else {
		$("#sidebar").hide();
		 Cookies.set('app_sidebar_collapsed',"true")
	}
});

/*
 * Does following:
 * 1) When session times out during an ajax call need to take user to the login page. 
 * The application is configured such that when an ajax call to login page is made it sends
 * a 'ajax_login_page' header which this methods looks for.
 * 
 * 2) When a redirect using Turbolinks happens in some cases a message needs to be shown to user
 *    once the redirect request is complete. The backend in these cases will set a response header 'redirect_message'
 *    
 */
$(document).ajaxComplete(function (event, xhr, settings) {
  if (xhr.getResponseHeader('ajax_login_page')) {
    window.location.reload();
  }
  else if (xhr.getResponseHeader('redirect_message')) {
    toastr.success(xhr.getResponseHeader('redirect_message'));
  }
});



