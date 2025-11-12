package com.example.ehe_server.securityConfig;

import com.example.ehe_server.service.audit.UserContextService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Custom DataSource configuration that sets PostgreSQL user context
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Autowired
    @Lazy
    private UserContextService userContextHolder;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        // Connection initialization query to set user context
        config.setConnectionInitSql("SELECT 1"); // Basic connection test

        // Create custom HikariDataSource that sets user context
        return new UserContextAwareDataSource(config, userContextHolder);
    }

    /**
     * Custom HikariDataSource that sets PostgreSQL user context based on Spring Security context
     */
    public static class UserContextAwareDataSource extends HikariDataSource {

        private final UserContextService userContextHolder;

        public UserContextAwareDataSource(HikariConfig configuration, UserContextService userContextHolder) {
            super(configuration);
            this.userContextHolder = userContextHolder;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            setUserContext(connection);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = super.getConnection(username, password);
            setUserContext(connection);
            return connection;
        }

        /**
         * Set the PostgreSQL user context based on Spring Security context
         */
        private void setUserContext(Connection connection) {
            try {
                String currentUser = userContextHolder.getCurrentUserIdAsString();

                // Set the PostgreSQL session variable for audit purposes
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT set_config('ehe.current_user', ?, false)")) {
                    statement.setString(1, currentUser);
                    statement.execute();
                }
            } catch (SQLException e) {
                // Log the error but don't fail the connection
                System.err.println("Failed to set user context in PostgreSQL: " + e.getMessage());
                // In a real application, you might want to use a proper logger here
            }
        }
    }
}