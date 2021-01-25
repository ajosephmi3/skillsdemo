package org.skillsdemo.config;

import org.jdbctemplatemapper.dbutil.JdbcUtil;
import org.skillsdemo.common.AuditOperatorResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcUtilConfig {

  @Bean(name = "jdbcUtil")
  public JdbcUtil jdbcUtil(@Qualifier("npJdbcTemplate") NamedParameterJdbcTemplate npJdbcTemplate) {
    JdbcUtil jdbcUtil = new JdbcUtil(npJdbcTemplate);
    jdbcUtil
        .withSchemaName("public")
        .withAuditOperatorResolver(new AuditOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");

    return jdbcUtil;
  }
}
