package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.exception.custom.InvalidConditionTypeException;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.alert.AlertSearchServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AlertSearchService implements AlertSearchServiceInterface {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertSearchService(AlertRepository alertRepository,
                              UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
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
    public List<AlertResponse> searchAlerts(Integer userId, String platform, String symbol, String conditionTypeStr) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        // Parse condition logic
        Alert.ConditionType conditionType = null;
        if (conditionTypeStr != null && !conditionTypeStr.trim().isEmpty()) {
            try {
                conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidConditionTypeException(conditionTypeStr);
            }
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Data retrieval
        List<Alert> alerts = alertRepository.searchAlerts(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null,
                (symbol != null && !symbol.trim().isEmpty()) ? symbol : null,
                conditionType
        );

        // Response mapping
        return alerts.stream()
                .map(alert -> {
                    AlertResponse alertSearchResponse = new AlertResponse();
                    alertSearchResponse.setId(alert.getAlertId());
                    alertSearchResponse.setPlatform(alert.getPlatformStock().getPlatform().getPlatformName());
                    alertSearchResponse.setSymbol(alert.getPlatformStock().getStock().getStockName());
                    alertSearchResponse.setConditionType(alert.getConditionType().name());
                    alertSearchResponse.setThresholdValue(alert.getThresholdValue());
                    alertSearchResponse.setDateCreated(alert.getDateCreated().format(DATE_FORMATTER));
                    return alertSearchResponse;
                })
                .collect(Collectors.toList());
    }
}