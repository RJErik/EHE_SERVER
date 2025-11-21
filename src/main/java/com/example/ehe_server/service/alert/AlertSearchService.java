package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertSearchResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.InvalidConditionTypeException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertSearchService implements AlertSearchServiceInterface {

    private final AlertRepository alertRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AlertSearchService(
            AlertRepository alertRepository,
            LoggingServiceInterface loggingService) {
        this.alertRepository = alertRepository;
        this.loggingService = loggingService;
    }

    @LogMessage(
            messageKey = "log.message.alert.search",
            params = {
                    "#platform",
                    "#symbol",
                    "#conditionTypeStr",
                    "#result.size()"}
    )
    @Override
    public List<AlertSearchResponse> searchAlerts(Integer userId, String platform, String symbol, String conditionTypeStr) {
        // Parse condition type if provided
        Alert.ConditionType conditionType = null;
        if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
            try {
                conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidConditionTypeException(conditionTypeStr);
            }
        }

        // Log search parameters
        StringBuilder searchParams = new StringBuilder("Searching alerts with filters: ");
        if (platform != null && !platform.trim().isEmpty()) searchParams.append("platform=").append(platform).append(", ");
        if (symbol != null && !symbol.trim().isEmpty()) searchParams.append("symbol=").append(symbol).append(", ");
        if (conditionType != null) searchParams.append("conditionType=").append(conditionType).append(", ");

        String logMessage = searchParams.toString();
        if (logMessage.endsWith(", ")) {
            logMessage = logMessage.substring(0, logMessage.length() - 2);
        }
        loggingService.logAction(logMessage);

        // Execute single database query with all filters
        List<Alert> alerts = alertRepository.searchAlerts(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null,
                conditionType
        );

        // Transform to AlertSearchResponse
        List<AlertSearchResponse> alertSearchResponses = alerts.stream()
                .map(alert -> {
                    AlertSearchResponse alertSearchResponse = new AlertSearchResponse();
                    alertSearchResponse.setId(alert.getAlertId());
                    alertSearchResponse.setPlatform(alert.getPlatformStock().getPlatformName());
                    alertSearchResponse.setSymbol(alert.getPlatformStock().getStockSymbol());
                    alertSearchResponse.setConditionType(alert.getConditionType().name());
                    alertSearchResponse.setThresholdValue(alert.getThresholdValue());
                    alertSearchResponse.setDateCreated(alert.getDateCreated().format(DATE_FORMATTER));
                    alertSearchResponse.setActive(alert.isActive());
                    return alertSearchResponse;
                })
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Alert search successful, found " + alertSearchResponses.size() + " alerts");

        return alertSearchResponses;
    }
}