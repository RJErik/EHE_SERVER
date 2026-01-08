package ehe_server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {

    private String url;

    private PathConfig user = new PathConfig();

    private PathConfig admin = new PathConfig();

    private String verifyRegistrationPath;
    private String resetPasswordPath;
    private String verifyEmailChangePath;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public PathConfig getUser() {
        return user;
    }

    public void setUser(PathConfig user) {
        this.user = user;
    }

    public PathConfig getAdmin() {
        return admin;
    }

    public void setAdmin(PathConfig admin) {
        this.admin = admin;
    }

    public String getVerifyRegistrationPath() { return verifyRegistrationPath; }
    public void setVerifyRegistrationPath(String verifyRegistrationPath) { this.verifyRegistrationPath = verifyRegistrationPath; }
    public String getResetPasswordPath() { return resetPasswordPath; }
    public void setResetPasswordPath(String resetPasswordPath) { this.resetPasswordPath = resetPasswordPath; }
    public String getVerifyEmailChangePath() { return verifyEmailChangePath; }
    public void setVerifyEmailChangePath(String verifyEmailChangePath) { this.verifyEmailChangePath = verifyEmailChangePath; }

    public static class PathConfig {
        private String suffix;

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }
    }
}