package org.skillsdemo.service;

import org.skillsdemo.common.AppUserPrincipal;
import org.skillsdemo.dao.PersonDao;
import org.skillsdemo.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
/**
 * Class to support spring security
 *
 * @author ajoseph
 */
public class AppUserDetailsService implements UserDetailsService {
  @Autowired PersonDao personDao;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Person person = personDao.findByUsername(username);
    if (person == null) {
      throw new UsernameNotFoundException("Could not find person by username: " + username);
    }
    String password = personDao.getPassword(person.getId());
    return new AppUserPrincipal(person, password);
  }
}
