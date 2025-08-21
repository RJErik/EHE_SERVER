package com.example.ehe_server.service.watchlist;

import com.example.ehe_server.dto.WatchlistCreationResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.Watchlist;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.PlatformStockNotFoundException;
import com.example.ehe_server.exception.custom.WatchlistItemAlreadyExistsException;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.repository.WatchlistRepository;
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

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public WatchlistCreationService(
            WatchlistRepository watchlistRepository,
            WatchlistItemRepository watchlistItemRepository,
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }

    @Override
    public WatchlistCreationResponse createWatchlistItem(Integer userId, String platform, String symbol) {
        // Check if platform and symbol combination exists
        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformNameAndStockSymbol(platform, symbol);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        PlatformStock platformStock = platformStocks.get(0);

        // Get or create watchlist for the user
        Watchlist watchlist = watchlistRepository.findByUser(userRepository.findById(userId).orElse(null))
                .orElseGet(() -> {
                    Watchlist newWatchlist = new Watchlist();
                    newWatchlist.setUser(userRepository.findById(userId).orElse(null));
                    return watchlistRepository.save(newWatchlist);
                });

        // Check if item already exists in watchlist
        Optional<WatchlistItem> existingItem = watchlistItemRepository.findByWatchlistAndPlatformStock(watchlist, platformStock);
        if (existingItem.isPresent()) {
            throw new WatchlistItemAlreadyExistsException(platform, symbol);
        }

        // Create new watchlist item
        WatchlistItem newItem = new WatchlistItem();
        newItem.setWatchlist(watchlist);
        newItem.setPlatformStock(platformStock);
        newItem.setDateAdded(LocalDateTime.now());
        WatchlistItem savedItem = watchlistItemRepository.save(newItem);

        // Log success
        loggingService.logAction("Added item to watchlist: " + platform + "/" + symbol);

        // Prepare and return success DTO
        return new WatchlistCreationResponse(
                savedItem.getWatchlistItemId(),
                platform,
                symbol,
                savedItem.getDateAdded().format(DATE_FORMATTER)
        );
    }
}