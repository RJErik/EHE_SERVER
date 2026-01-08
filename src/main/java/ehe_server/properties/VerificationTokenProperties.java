package ehe_server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.verification.token")
public class VerificationTokenProperties {
    private long tokenExpiryHours;

    public long getTokenExpiryHours() {
        return tokenExpiryHours;
    }

    public void setTokenExpiryHours(long tokenExpiryHours) {
        this.tokenExpiryHours = tokenExpiryHours;
    }
}
