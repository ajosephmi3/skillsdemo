package org.skillsdemo.config;

import javax.sql.DataSource;

import org.skillsdemo.service.AppUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Spring security configuration.
 *
 * @author ajoseph
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired AppUserDetailsService userDetailsService;
  @Autowired DataSource sqlDataSource;

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    // provider.setPasswordEncoder(new BCryptPasswordEncoder(11));
    provider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
    provider.setAuthoritiesMapper(authoritiesMapper());
    return provider;
  }

  @Bean
  public GrantedAuthoritiesMapper authoritiesMapper() {
    SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
    authorityMapper.setConvertToUpperCase(true);
    return authorityMapper;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(authenticationProvider());
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers("/css/**", "/img/**", "/js/**", "/logout")
        .permitAll()
        .antMatchers("/actuator/**")
        .hasRole("TECHADMIN") // securing spring actuator endpoints with spring security
        .anyRequest()
        .authenticated()
        .and()
        .formLogin()
        .loginPage("/login")
        // .defaultSuccessUrl("/", true)
        .permitAll()
        .and()
        .logout()
        .permitAll();

    http.csrf().disable();
  }

  @Bean
  /*
   * Takes user to the url they tried to access after the login.
   */
  public SavedRequestAwareAuthenticationSuccessHandler
      savedRequestAwareAuthenticationSuccessHandler() {
    SavedRequestAwareAuthenticationSuccessHandler auth =
        new SavedRequestAwareAuthenticationSuccessHandler();
    auth.setTargetUrlParameter("targetUrl");
    return auth;
  }
}
