package edu.pku.migrationhelper.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

// @Configuration
// @MapperScan(value = "edu.pku.migrationhelper.mapper")
@Deprecated
public class DataSourceConfiguration {

    @Bean(name = "dataSource")
    @ConfigurationProperties("spring.datasource.dbcp2")
    public DataSource dataSource() {
        return new BasicDataSource();
    }

    @Bean(name = "transactionManager")
    public DataSourceTransactionManager dbOneTransactionManager(
            @Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
