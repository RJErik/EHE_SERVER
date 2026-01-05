package com.example.ehe_server.service.alert.websocket;

import com.example.ehe_server.entity.MarketCandle.Timeframe;
import com.example.ehe_server.service.intf.alert.websocket.TimeframeNavigatorInterface;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TimeframeNavigator implements TimeframeNavigatorInterface {

    private static final int M5_BOUNDARY_MINUTES = 5;
    private static final int M15_BOUNDARY_MINUTES = 15;
    private static final int H4_BOUNDARY_HOURS = 4;

    @Override
    public boolean isAtBoundary(LocalDateTime time, Timeframe timeframe) {
        int minute = time.getMinute();
        int hour = time.getHour();

        return switch (timeframe) {
            case M1 -> minute % M5_BOUNDARY_MINUTES == 0;
            case M5 -> minute % M15_BOUNDARY_MINUTES == 0;
            case M15 -> minute == 0;
            case H1 -> hour % H4_BOUNDARY_HOURS == 0 && minute == 0;
            case H4 -> hour == 0 && minute == 0;
            default -> false;
        };
    }

    @Override
    public Optional<Timeframe> getNextHigherTimeframe(Timeframe current) {
        return switch (current) {
            case M1 -> Optional.of(Timeframe.M5);
            case M5 -> Optional.of(Timeframe.M15);
            case M15 -> Optional.of(Timeframe.H1);
            case H1 -> Optional.of(Timeframe.H4);
            case H4 -> Optional.of(Timeframe.D1);
            default -> Optional.empty();
        };
    }
}