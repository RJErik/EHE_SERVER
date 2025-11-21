package com.example.ehe_server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.alpaca")
public class AlpacaProperties {

    private String apikey;
    private String secret;
    private String baseurl = "https://paper-api.alpaca.markets"; // default to paper trading
    private String dataurl = "https://data.alpaca.markets";
    private String websocketurl = "wss://stream.data.alpaca.markets";

    // Getters and setters
    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getBaseurl() {
        return baseurl;
    }

    public void setBaseurl(String baseurl) {
        this.baseurl = baseurl;
    }

    public String getDataurl() {
        return dataurl;
    }

    public void setDataurl(String dataurl) {
        this.dataurl = dataurl;
    }

    public String getWebsocketurl() {
        return websocketurl;
    }

    public void setWebsocketurl(String websocketurl) {
        this.websocketurl = websocketurl;
    }
}