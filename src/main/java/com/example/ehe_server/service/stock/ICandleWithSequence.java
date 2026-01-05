package com.example.ehe_server.service.stock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ICandleWithSequence {
    Integer getMarketCandleId();
    LocalDateTime getTimestamp();
    BigDecimal getOpenPrice();
    BigDecimal getClosePrice();
    BigDecimal getHighPrice();
    BigDecimal getLowPrice();
    BigDecimal getVolume();
    Long getSequence();
}
