package com.adela.linky.security.jwt;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the RSA keys used to sign and verify JWTs, from an env var or a PEM file in resources.
 */
public class KeyUtils {

    private KeyUtils() {
    }

    /**
     * Loads the private key (PKCS#8 PEM) used to sign tokens; the env value wins over the file.
     */
    public static PrivateKey loadPrivateKey(String pemPath, String envValue) throws Exception {
        String source = (envValue != null && !envValue.isBlank())
                ? envValue
                : readKeyFromResource(pemPath);

        String key = source
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    /**
     * Loads the public key (X.509 PEM) used to verify tokens; the env value wins over the file.
     */
    public static PublicKey loadPublicKey(String pemPath, String envValue) throws Exception {
        String source = (envValue != null && !envValue.isBlank())
                ? envValue
                : readKeyFromResource(pemPath);

        String key = source
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    /**
     * Reads a PEM file from the classpath, e.g. keys/private_key.pem.
     */
    private static String readKeyFromResource(String path) throws Exception {
        try (InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Key not found: " + path);
            }
            return new String(is.readAllBytes());
        }
    }

}
