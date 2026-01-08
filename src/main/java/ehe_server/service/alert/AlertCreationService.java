package ehe_server.service.alert;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.AlertResponse;
import ehe_server.entity.Alert;
import ehe_server.entity.PlatformStock;
import ehe_server.entity.User;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.PlatformStockNotFoundException;
import ehe_server.exception.custom.UserNotFoundException;
import ehe_server.repository.AlertRepository;
import ehe_server.repository.PlatformStockRepository;
import ehe_server.repository.UserRepository;
import ehe_server.service.intf.alert.AlertCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class AlertCreationService implements AlertCreationServiceInterface {

    private final AlertRepository alertRepository;
    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;

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
    public AlertResponse createAlert(Integer userId, String platform, String symbol, Alert.ConditionType conditionType, BigDecimal thresholdValue) {

        // Database integrity checks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformNameAndStockStockSymbol(platform, symbol);
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

        return new AlertResponse(
                savedAlert.getAlertId(),
                platform,
                symbol,
                savedAlert.getConditionType(),
                savedAlert.getThresholdValue(),
                savedAlert.getDateCreated()
        );
    }
}