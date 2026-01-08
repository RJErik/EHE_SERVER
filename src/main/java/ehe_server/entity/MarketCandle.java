package ehe_server.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import jakarta.persistence.Converter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_candle")
public class MarketCandle {

    public enum Timeframe {
        M1("1m"),
        M5("5m"),
        M15("15m"),
        H1("1h"),
        H4("4h"),
        D1("1d");

        private final String value;

        Timeframe(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Timeframe fromValue(String value) {
            for (Timeframe tf : Timeframe.values()) {
                if (tf.getValue().equalsIgnoreCase(value)) {
                    return tf;
                }
            }
            throw new IllegalArgumentException("Unknown timeframe: " + value);
        }
    }

    @Converter
    public static class TimeframeConverter implements AttributeConverter<Timeframe, String> {
        @Override
        public String convertToDatabaseColumn(Timeframe attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        @Override
        public Timeframe convertToEntityAttribute(String dbData) {
            return dbData == null ? null : Timeframe.fromValue(dbData);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "market_candle_id")
    private Integer marketCandleId;

    @ManyToOne
    @JoinColumn(name = "platform_stock_id", nullable = false)
    private PlatformStock platformStock;

    @Convert(converter = TimeframeConverter.class)
    @Column(name = "timeframe", nullable = false, length = 50)
    private Timeframe timeframe;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @DecimalMin("0.00000001")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "open_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal openPrice;

    @DecimalMin("0.00000001")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "close_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal closePrice;

    @DecimalMin("0.00000001")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "high_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal highPrice;

    @DecimalMin("0.00000001")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "low_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal lowPrice;

    @DecimalMin("0")
    @Digits(integer = 10, fraction = 8)
    @Column(name = "volume", nullable = false, precision = 18, scale = 8)
    private BigDecimal volume;

    public Integer getMarketCandleId() {
        return marketCandleId;
    }

    public void setMarketCandleId(Integer marketCandleId) {
        this.marketCandleId = marketCandleId;
    }

    public PlatformStock getPlatformStock() {
        return platformStock;
    }

    public void setPlatformStock(PlatformStock platformStock) {
        this.platformStock = platformStock;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
}
