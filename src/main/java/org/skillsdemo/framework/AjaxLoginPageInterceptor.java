package org.skillsdemo.framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * When the user session times out the application needs to redirect to the login page even when it
 * is an ajax request.
 *
 * <p>This intercepter adds a response header when accessing the login page using ajax
 *
 * <p>On the client side, the javascript (main.js) checks for the specific header after completion
 * of each ajax request and forces a browser reload when header is found which in turn logs out
 * the user.
 *
 * @author ajoseph
 */
public class AjaxLoginPageInterceptor extends HandlerInterceptorAdapter {
  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView)
      throws Exception {
    if (modelAndView != null
        && "login".equals(modelAndView.getViewName())
        && "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
      response.setHeader("ajax_login_page", "true");
    }
  }
}
