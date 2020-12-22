package org.skillsdemo.framework;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Configuration;

//@Aspect
@Configuration
public class AspectSqlValidatorForTenant {
	
	@Before("execution(* org.springframework.jdbc.core.JdbcTemplate.query*(..))")
    public void logBeforeQueryMethod(JoinPoint joinPoint) 
    {

        System.out.println("**** Logging before jdbcTemplate query method() : " + joinPoint.getSignature().getName());
		Object[] args = joinPoint.getArgs();
		if (args != null && args.length > 0) {
			System.out.println("******** sql:" + args[0]);
		}
    }
}
