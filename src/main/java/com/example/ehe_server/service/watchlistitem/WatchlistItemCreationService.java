package com.example.ehe_server.service.watchlistitem;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.WatchlistResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.entity.WatchlistItem;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.repository.WatchlistItemRepository;
import com.example.ehe_server.service.intf.watchlist.WatchlistCreationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional
public class WatchlistItemCreationService implements WatchlistCreationServiceInterface {

    private final WatchlistItemRepository watchlistItemRepository;
    private final PlatformStockRepository platformStockRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public WatchlistItemCreationService(
            WatchlistItemRepository watchlistItemRepository,
            PlatformStockRepository platformStockRepository,
            UserRepository userRepository) {
        this.watchlistItemRepository = watchlistItemRepository;
        this.platformStockRepository = platformStockRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.watchlistItem.add",
            params = {
                    "#result.id",
                    "#result.platform",
                    "#result.symbol",
                    "#result.dateAdded",
            }
    )
    @Override
    public WatchlistResponse createWatchlistItem(Integer userId, String platform, String symbol) {

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

        // Database integrity checks
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Optional<PlatformStock> platformStocks = platformStockRepository.findByStockNameAndPlatformName(platform, symbol);
        if (platformStocks.isEmpty()) {
            throw new PlatformStockNotFoundException(platform, symbol);
        }

        PlatformStock platformStock = platformStocks.get();

        // Duplicate check
        Optional<WatchlistItem> existingItem = watchlistItemRepository.findByUser_UserIdAndPlatformStock(userId, platformStock);
        if (existingItem.isPresent()) {
            throw new WatchlistItemAlreadyExistsException(platform, symbol);
        }

        // Execution and persistence
        WatchlistItem newItem = new WatchlistItem();
        newItem.setUser(user);
        newItem.setPlatformStock(platformStock);

        WatchlistItem savedItem = watchlistItemRepository.save(newItem);

        // Response mapping
        return new WatchlistResponse(
                savedItem.getWatchlistItemId(),
                platform,
                symbol,
                savedItem.getDateAdded().format(DATE_FORMATTER)
        );
    }
}