package com.Veibai.VNable.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class PackCrypto {

    private static final byte[] MAGIC = {'V', 'N', 'E', 'N', 'C', '0', '1'};
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int KEY_BITS = 256;
    private static final int TAG_BITS = 128;
    private static final int ITERATIONS = 65536;

    private PackCrypto() {
    }

    public static String decryptText(String password, byte[] data)
            throws GeneralSecurityException {
        return new String(decrypt(password, data), StandardCharsets.UTF_8);
    }

    public static byte[] decrypt(String password, byte[] data)
            throws GeneralSecurityException {
        int head = MAGIC.length + SALT_LEN + IV_LEN;
        if (password == null || password.isEmpty()
                || data == null || data.length <= head + TAG_BITS / 8
                || !hasMagic(data)) {
            throw new GeneralSecurityException("invalid encrypted pack file format");
        }
        int p = MAGIC.length;
        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(data, p, salt, 0, SALT_LEN);
        p += SALT_LEN;
        byte[] iv = new byte[IV_LEN];
        System.arraycopy(data, p, iv, 0, IV_LEN);
        p += IV_LEN;
        byte[] cipherBytes = new byte[data.length - p];
        System.arraycopy(data, p, cipherBytes, 0, cipherBytes.length);

        SecretKeySpec key = new SecretKeySpec(deriveKey(password, salt), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(cipherBytes);
    }

    private static byte[] deriveKey(String password, byte[] salt)
            throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);
        mac.init(new SecretKeySpec(passBytes, "HmacSHA256"));
        Arrays.fill(passBytes, (byte) 0);
        int dkLen = KEY_BITS / 8;
        int hLen = mac.getMacLength();
        int blocks = (dkLen + hLen - 1) / hLen;
        byte[] dk = new byte[dkLen];
        byte[] block = new byte[salt.length + 4];
        System.arraycopy(salt, 0, block, 0, salt.length);
        for (int i = 1; i <= blocks; i++) {
            block[salt.length] = (byte) (i >>> 24);
            block[salt.length + 1] = (byte) (i >>> 16);
            block[salt.length + 2] = (byte) (i >>> 8);
            block[salt.length + 3] = (byte) i;
            byte[] u = mac.doFinal(block);
            byte[] t = new byte[hLen];
            System.arraycopy(u, 0, t, 0, hLen);
            for (int j = 1; j < ITERATIONS; j++) {
                u = mac.doFinal(u);
                for (int k = 0; k < hLen; k++) t[k] ^= u[k];
            }
            int off = (i - 1) * hLen;
            System.arraycopy(t, 0, dk, off, Math.min(hLen, dkLen - off));
        }
        return dk;
    }

    private static boolean hasMagic(byte[] data) {
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) return false;
        }
        return true;
    }
}
