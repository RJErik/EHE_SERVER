package com.example.ehe_server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {

    /**
     * Maps to: app.frontend.url
     */
    private String frontEndUrl;

    /**
     * Maps to: app.frontend.user.*
     */
    private PathConfig userUrlSuffix = new PathConfig();

    /**
     * Maps to: app.frontend.admin.*
     */
    private PathConfig adminUrlSuffix = new PathConfig();

    // --- Getters and Setters ---

    public String getFrontEndUrl() {
        return frontEndUrl;
    }

    public void setFrontEndUrl(String frontEndUrl) {
        this.frontEndUrl = frontEndUrl;
    }

    public PathConfig getUserUrlSuffix() {
        return userUrlSuffix;
    }

    public void setUserUrlSuffix(PathConfig userUrlSuffix) {
        this.userUrlSuffix = userUrlSuffix;
    }

    public PathConfig getAdminUrlSuffix() {
        return adminUrlSuffix;
    }

    public void setAdminUrlSuffix(PathConfig adminUrlSuffix) {
        this.adminUrlSuffix = adminUrlSuffix;
    }

    // --- Static Inner Class for Nested Properties ---

    public static class PathConfig {
        /**
         * Maps to: .suffix
         */
        private String suffix;

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }
    }
}