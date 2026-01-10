package ehe_server;

import org.conscrypt.OpenSSLProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@SpringBootApplication
@EntityScan(basePackages = {"ehe_server.entity"})
@EnableJpaRepositories(basePackages = {"ehe_server.repository"})
@EnableScheduling
@EnableAsync
public class EheServerApplication {

    public static void main(String[] args) {
        Security.insertProviderAt(new OpenSSLProvider(), 1);
        SpringApplication.run(EheServerApplication.class, args);
    }
}
