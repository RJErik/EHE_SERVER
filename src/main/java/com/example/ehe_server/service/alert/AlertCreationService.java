package com.example.ehe_server.service.alert;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.AlertCreationResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.alert.AlertCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class AlertCreationService implements AlertCreationServiceInterface {

    private final AlertRepository alertRepository;
    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AlertCreationService(
            AlertRepository alertRepository,
            PlatformStockRepository platformStockRepository,
            UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.platformStockRepository = platformStockRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.alert.add",
            params = {
                    "#result.alertId",
                    "#result.platform",
                    "#result.symbol",
                    "#result.conditionType",
                    "#result.thresholdValue",
                    "#result.dateCreated"
            }
    )
    @Override
    public AlertCreationResponse createAlert(Integer userId, String platform, String symbol, String conditionTypeStr, BigDecimal thresholdValue) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (platform == null || platform.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new MissingStockSymbolException();
        }

        if (conditionTypeStr == null || conditionTypeStr.trim().isEmpty()) {
            throw new MissingConditionTypeException();
        }

        if (thresholdValue == null) {
            throw new MissingThresholdValueException();
        }

        // Threshold logic check
        if (thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidThresholdValueException(thresholdValue);
        }

        // Parse enum before database access
        Alert.ConditionType conditionType;
        try {
            conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidConditionTypeException(conditionTypeStr);
        }

        // Database integrity checks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformNameAndStockStockName(platform, symbol);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        // Construct and save the alert
        Alert newAlert = new Alert();
        newAlert.setUser(user);
        newAlert.setPlatformStock(platformStocks.getFirst());
        newAlert.setConditionType(conditionType);
        newAlert.setThresholdValue(thresholdValue);

        Alert savedAlert = alertRepository.save(newAlert);

        return new AlertCreationResponse(
                savedAlert.getAlertId(),
                platform,
                symbol,
                savedAlert.getConditionType().toString(),
                savedAlert.getThresholdValue(),
                savedAlert.getDateCreated().format(DATE_FORMATTER)
        );
    }
}