package org.skillsdemo.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.skillsdemo.model.Person;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
/**
 * UserDetails used by spring security
 * 
 * @author ajoseph
 */
public class AppUserPrincipal implements UserDetails {
  private static final long serialVersionUID = 1L;
  
  private Person person;
  private String password;

  public AppUserPrincipal(Person person, String password) {
    this.person = person;
    this.password = password;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<SimpleGrantedAuthority> grantedAuthorities = new HashSet<>();
    grantedAuthorities.add(new SimpleGrantedAuthority(person.getRole().toUpperCase()));
    return grantedAuthorities;
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Override
  public String getUsername() {
    return person.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return !person.isAccountDisabled();
  }

  @Override
  public boolean isAccountNonLocked() {
    return !person.isAccountDisabled();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return !person.isAccountDisabled();
  }

  @Override
  public boolean isEnabled() {
    return !person.isAccountDisabled();
  }
  
  public Person getPerson() {
	  return this.person;
  }
  
}
