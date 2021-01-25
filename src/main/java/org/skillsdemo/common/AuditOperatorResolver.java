package org.skillsdemo.common;

import org.jdbctemplatemapper.dbutil.IAuditOperatorResolver;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditOperatorResolver implements IAuditOperatorResolver{
	
	public Object getAuditOperator() {
		Object appUserPrincipal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	    if (appUserPrincipal instanceof AppUserPrincipal) {
	      return ((AppUserPrincipal) appUserPrincipal).getPerson().getFullName();
	    } else {
	    	return "anonymous";
	    }
	}
}
