/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.security;

import com.emberone.platform.db.EmberOneDbProperties;
import com.emberone.platform.logging.RequestIdFilter;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TenantContextAutoConfiguration {

    @Bean
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    TenantContextWebFilter tenantContextWebFilter() {
        return new TenantContextWebFilter();
    }

    @Bean
    HibernatePropertiesCustomizer transactionRequiredStatementInspectorCustomizer(
            EmberOneDbProperties dbProperties) {
        return (Map<String, Object> properties) ->
                properties.put(
                        AvailableSettings.STATEMENT_INSPECTOR,
                        new TransactionRequiredStatementInspector(
                                dbProperties.isRequireTransaction()));
    }

    @Bean(name = "transactionManager")
    @Primary
    @ConditionalOnClass(JpaTransactionManager.class)
    PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        TenantContextJpaTransactionManager manager = new TenantContextJpaTransactionManager();
        manager.setEntityManagerFactory(entityManagerFactory);
        manager.setDataSource(dataSource);
        return manager;
    }
}
