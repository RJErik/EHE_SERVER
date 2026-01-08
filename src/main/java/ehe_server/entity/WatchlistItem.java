package ehe_server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_item")
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_item_id")
    private Integer watchlistItemId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "platform_stock_id", nullable = false)
    private PlatformStock platformStock;

    @CreationTimestamp
    @Column(name = "date_added", nullable = false, updatable = false)
    private LocalDateTime dateAdded;

    // Getters and setters
    public Integer getWatchlistItemId() {
        return watchlistItemId;
    }

    public void setWatchlistItemId(Integer watchlistItemId) {
        this.watchlistItemId = watchlistItemId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PlatformStock getPlatformStock() {
        return platformStock;
    }

    public void setPlatformStock(PlatformStock platformStock) {
        this.platformStock = platformStock;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }
}
