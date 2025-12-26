package com.example.ehe_server.annotation.validation;

import com.example.ehe_server.exception.custom.CustomBaseException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ExceptionInstantiator {

    public Optional<CustomBaseException> instantiate(String className) {
        if (className == null || className.isEmpty()) {
            return Optional.empty();
        }

        try {
            Class<?> clazz = Class.forName(className);

            if (!CustomBaseException.class.isAssignableFrom(clazz)) {
                return Optional.empty();
            }

            CustomBaseException exception = (CustomBaseException) clazz
                    .getDeclaredConstructor()
                    .newInstance();

            return Optional.of(exception);

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}