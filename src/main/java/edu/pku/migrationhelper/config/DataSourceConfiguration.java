package edu.pku.migrationhelper.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * Created by xuyul on 2020/1/2.
 */
@Configuration
@MapperScan(value = "edu.pku.migrationhelper.mapper")
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
