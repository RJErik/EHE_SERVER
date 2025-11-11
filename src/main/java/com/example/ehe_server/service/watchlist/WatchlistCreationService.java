package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistCreationResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.PlatformStockNotFoundException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.exception.custom.WatchlistItemAlreadyExistsException;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.watchlist.WatchlistCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class WatchlistCreationService implements WatchlistCreationServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistCreationService(
            WatchlistItemRepository watchlistItemRepository,
            PlatformStockRepository platformStockRepository,
            UserRepository userRepository,
            LoggingServiceInterface loggingService) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.platformStockRepository = platformStockRepository;
        this.userRepository = userRepository;
        this.loggingService = loggingService;
    }

    @Override
    public WatchlistCreationResponse createWatchlistItem(Integer userId, String platform, String symbol) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Check if platform and symbol combination exists
        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        PlatformStock platformStock = platformStocks.get(0);

        // Check if item already exists for this user and platform stock
        Optional<WatchlistItem> existingItem = watchlistItemRepository.findByUser_UserIdAndPlatformStock(userId, platformStock);
        if (existingItem.isPresent()) {
            throw new WatchlistItemAlreadyExistsException(platform, symbol);
        }

        // Create new watchlist item
        WatchlistItem newItem = new WatchlistItem();
        newItem.setUser(user);
        newItem.setPlatformStock(platformStock);
        newItem.setDateAdded(LocalDateTime.now());
        WatchlistItem savedItem = watchlistItemRepository.save(newItem);

        // Log success
        loggingService.logAction("Added item to watchlist: " + platform + "/" + symbol + " for user: " + userId);

        // Prepare and return success DTO
        return new WatchlistCreationResponse(
                savedItem.getWatchlistItemId(),
                platform,
                symbol,
                savedItem.getDateAdded().format(DATE_FORMATTER)
        );
    }
}