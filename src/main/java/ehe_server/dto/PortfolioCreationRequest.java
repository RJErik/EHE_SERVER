package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.annotation.validation.RegexPattern;
import ehe_server.exception.custom.InvalidPortfolioName;
import ehe_server.exception.custom.MissingApiKeyIdException;
import ehe_server.exception.custom.MissingPortfolioNameException;

public class PortfolioCreationRequest {
    @NotEmptyString(exception = MissingPortfolioNameException.class)
    @RegexPattern(pattern = "^[a-zA-Z0-9_]{1,100}$",
    exception = InvalidPortfolioName.class)
    private String portfolioName;
    @NotNullField(exception = MissingApiKeyIdException.class)
    private Integer apiKeyId;

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public Integer getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(Integer apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
}
