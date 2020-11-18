/*
 * - Ajax POST with redirect where the redirect is handled by turbolinks. 
 * - When the service returns a validation error uses the ApiError object to display error to user using Toaster
 */

let CustomAjax = (function () {
  let publicApi = {};

  publicApi.post = function (url, jsonPayload) {
    $.ajax({
      type: "POST",
      url: url,
      data: jsonPayload,
      contentType: "application/json; charset=utf-8",
      success: function (data, status, xhr) {
        let location = xhr.getResponseHeader('Application-Turbolinks-Redirect-Location');
        if (location) {
          // have turbolinks handle the redirect
          Turbolinks.visit(location);
        }
        else {
          console.log("The ajax POST request did not get a response with header attribute 'Application-Turbolinks-Redirect-Location' ");
          console.log(xhr.responseText);
        }
      },
      error: function (xhr, textStatus, errorString) {
        // if status is 400 its an api error
        if (xhr.status == 400) {
          try {
            let obj = JSON.parse(xhr.responseText);
            //see ApiError.java for details
            if ('apiMessage' in obj) {
              for (error of obj.errors) {
                toastr.warning(error.message);
              }
            }
          }
          catch (e) {
            console.log(e);
            toastr.error(e);
          }
        }
        else {
          console.log(xhr.responseText);
          toastr.error(xhr.responseText);
        }
      },
    });
  }

  // expose the public api.
  return publicApi;
})();