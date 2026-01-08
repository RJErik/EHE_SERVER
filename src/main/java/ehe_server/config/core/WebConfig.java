package ehe_server.config.core;

import ehe_server.entity.MarketCandle.Timeframe;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToTimeframeConverter());
    }

    public static class StringToTimeframeConverter implements Converter<String, Timeframe> {
        @Override
        public Timeframe convert(String source) {
            if (source.trim().isEmpty()) {
                return null;
            }
            return Timeframe.fromValue(source);
        }
    }
}