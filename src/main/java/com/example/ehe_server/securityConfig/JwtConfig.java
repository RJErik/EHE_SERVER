package com.example.ehe_server.securityConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

    @Value("${jwt.keystore.path}")
    private String keystorePath;

    @Value("${jwt.keystore.password}")
    private String keystorePassword;

    @Value("${jwt.key.alias}")
    private String keyAlias;

    @Value("${jwt.key.private.password}")
    private String privateKeyPassword;

    @Bean
    public KeyStore keyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream resourceAsStream = new FileInputStream(keystorePath)) {
            keyStore.load(resourceAsStream, keystorePassword.toCharArray());
        }
        return keyStore;
    }

    @Bean
    public RSAPrivateKey jwtSigningKey() {
        try {
            // Create a File object from the path
            File keystoreFile = new File(keystorePath);

            // Check if the file exists
            if (!keystoreFile.exists()) {
                throw new FileNotFoundException("Keystore file not found: " + keystorePath);
            }

            // Load the keystore
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream is = new FileInputStream(keystoreFile)) {
                keystore.load(is, keystorePassword.toCharArray());
            }

            // Get the private key
            Key key = keystore.getKey(keyAlias, keystorePassword.toCharArray());
            if (key instanceof RSAPrivateKey) {
                return (RSAPrivateKey) key;
            } else {
                throw new RuntimeException("Key is not an RSA private key");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JWT signing key", e);
        }
    }

    @Bean
    public RSAPublicKey jwtValidationKey(KeyStore keyStore) throws Exception {
        Certificate cert = keyStore.getCertificate(keyAlias);
        PublicKey publicKey = cert.getPublicKey();
        if (publicKey instanceof RSAPublicKey) {
            return (RSAPublicKey) publicKey;
        }
        throw new IllegalStateException("Unable to load RSA public key");
    }
}
