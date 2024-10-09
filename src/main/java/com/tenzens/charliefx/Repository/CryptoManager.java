package com.tenzens.charliefx.Repository;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class CryptoManager {

    private SecretKeySpec symmetricKey;

    public void generateKeyFromPassword(String password) throws Exception {
        int iterations = 65536;
        int keyLength = 256; // Key length for AES (128, 192, or 256 bits)

        // Generate secret key from password using PBKDF2
        this.symmetricKey = deriveKeyFromPassword(String.format("%s-charlie", password), iterations, keyLength);
    }

    private SecretKeySpec deriveKeyFromPassword(String sequence, int iterations, int keyLength) throws Exception {
        // Use PBKDF2 with HMAC-SHA256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        // Set up the password-based key specification
        KeySpec spec = new PBEKeySpec(sequence.toCharArray(), "charlie".getBytes(StandardCharsets.UTF_8), iterations, keyLength);

        // Generate the secret key
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        // Return the key as a SecretKeySpec for AES
        return new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16]; // AES block size is 16 bytes
        random.nextBytes(iv);
        return iv;
    }

    public String encrypt(String plainText) throws Exception {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        // Initialize the cipher for AES encryption with CBC mode
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, ivParameterSpec);

        // Encrypt the plaintext
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());

        // Prepend the IV to the encrypted bytes
        byte[] ivAndEncrypted = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.length, encryptedBytes.length);

        // Return the result as a Base64 string
        return Base64.getEncoder().encodeToString(ivAndEncrypted);
    }

    public String decrypt(String encryptedData) throws Exception {
        byte[] ivAndEncrypted = Base64.getDecoder().decode(encryptedData);

        // Extract the IV from the first 16 bytes
        byte[] iv = new byte[16];
        System.arraycopy(ivAndEncrypted, 0, iv, 0, iv.length);

        // Extract the encrypted data (after the IV)
        byte[] encryptedBytes = new byte[ivAndEncrypted.length - iv.length];
        System.arraycopy(ivAndEncrypted, iv.length, encryptedBytes, 0, encryptedBytes.length);

        // Decrypt using the extracted IV
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, symmetricKey, ivParameterSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }

}
