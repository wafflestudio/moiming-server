package com.wafflestudio.spring2025.config

import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import javax.sql.DataSource

@Configuration
@EnableJdbcRepositories(basePackages = ["com.wafflestudio.spring2025"])
@EnableJdbcAuditing
class DatabaseConfig(
    private val env: Environment,
) : AbstractJdbcConfiguration() {
    @Bean
    fun dataSource(): DataSource =
        DataSourceBuilder
            .create()
            .url(env.getProperty("spring.datasource.url"))
            .username(env.getProperty("spring.datasource.username"))
            .password(env.getProperty("spring.datasource.password"))
            .driverClassName(env.getProperty("spring.datasource.driver-class-name"))
            .build()
}
