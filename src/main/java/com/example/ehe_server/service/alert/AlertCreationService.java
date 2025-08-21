package com.example.ehe_server.service.alert;

import com.example.ehe_server.dto.AlertCreationResponse;
import com.example.ehe_server.entity.Alert;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.InvalidAlertThresholdException;
import com.example.ehe_server.exception.custom.InvalidConditionTypeException;
import com.example.ehe_server.exception.custom.PlatformStockNotFoundException;
import com.example.ehe_server.repository.AlertRepository;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.alert.AlertCreationServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class AlertCreationService implements AlertCreationServiceInterface {

    private final AlertRepository alertRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public AlertCreationService(
            AlertRepository alertRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    public AlertCreationResponse createAlert(Integer userId, String platform, String symbol, String conditionTypeStr, BigDecimal thresholdValue) {
        // Validate threshold value
        if (thresholdValue == null || thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAlertThresholdException(thresholdValue);
        }

        // Parse condition type
        Alert.ConditionType conditionType;
        try {
            conditionType = Alert.ConditionType.valueOf(conditionTypeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidConditionTypeException(conditionTypeStr);
        }

        // Check if platform and symbol combination exists
        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        PlatformStock platformStock = platformStocks.get(0);

        // Create new alert
        Alert newAlert = new Alert();
        newAlert.setUser(userRepository.findById(userId).get());
        newAlert.setPlatformStock(platformStock);
        newAlert.setConditionType(conditionType);
        newAlert.setThresholdValue(thresholdValue);
        newAlert.setDateCreated(LocalDateTime.now());
        newAlert.setActive(true);
        Alert savedAlert = alertRepository.save(newAlert);

        // Log success
        loggingService.logAction("Added alert: " + platform + "/" + symbol + " " + conditionType + " " + thresholdValue);

        // Prepare success response
        return new AlertCreationResponse(
                savedAlert.getAlertId(),
                platform,
                symbol,
                savedAlert.getConditionType().toString(),
                savedAlert.getThresholdValue(),
                savedAlert.getDateCreated().format(DATE_FORMATTER),
                savedAlert.isActive()
        );
    }
}