package com.github.paicoding.forum.test.mysql1;

import com.github.paicoding.forum.test.BasicTest;
import com.github.paicoding.forum.web.QuickForumApplication;
import com.github.paicoding.forum.web.config.init.DbChangeSetLoader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;


@Slf4j
public class ForumDataSourceInitializerTest extends BasicTest {
    private final String databaseName = "paicoding_initializer_" + UUID.randomUUID().toString().replace("-", "");

    @Value("classpath:liquibase/data/init_schema_221209.sql")
    private Resource schemaSql;
    @Value("classpath:liquibase/data/init_data_221209.sql")
    private Resource initData;

    @Test
    public void dataSourceInitializer() throws SQLException {
        DataSource dataSource = createCustomDataSource();
        log.info(dataSource.getConnection().getMetaData().getURL());

        final DataSourceInitializer initializer = new DataSourceInitializer();
        // 设置数据源
        initializer.setDataSource(dataSource);
        initializer.setEnabled(true);

        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(schemaSql);
        populator.addScript(initData);
        initializer.setDatabasePopulator(populator);
        initializer.afterPropertiesSet();
    }

    @BeforeEach
    void createTestDatabase() throws SQLException {
        executeOnServer("CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4");
    }

    @AfterEach
    void dropTestDatabase() throws SQLException {
        executeOnServer("DROP DATABASE IF EXISTS `" + databaseName + "`");
    }

    private void executeOnServer(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", databaseUsername(), databasePassword());
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private DataSource createCustomDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/" + databaseName);
        dataSource.setUsername(databaseUsername());
        dataSource.setPassword(databasePassword());
        return dataSource;
    }

    private String databaseUsername() {
        return System.getenv().getOrDefault("DB_USERNAME", "root");
    }

    private String databasePassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }
}
