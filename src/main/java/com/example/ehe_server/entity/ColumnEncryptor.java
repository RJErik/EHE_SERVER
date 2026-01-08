package com.example.ehe_server.entity;

import com.example.ehe_server.properties.ColumnEncryptionProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Component
@Converter
public class ColumnEncryptor implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private final SecretKeySpec keySpec;

    public ColumnEncryptor(ColumnEncryptionProperties properties) {
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException("Database encryption key (spring.column.secret-key) is missing!");
        }
        byte[] keyBytes = HexFormat.of().parseHex(properties.getSecretKey());

        this.keySpec = new SecretKeySpec(keyBytes, AES);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new IllegalStateException("Error decrypting data", e);
        }
    }
}