package com.wash.iot.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 数据加密工具类
 * 提供敏感数据的加密和解密功能
 */
@Slf4j
@Component
public class DataEncryptionUtil {

    @Value("${app.encryption.key:XiYiJi2024SecretKey}")
    private String encryptionKey;
    
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;

    /**
     * 加密敏感数据
     */
    public String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        try {
            // 创建密钥
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), AES);
            
            // 创建初始化向量
            IvParameterSpec ivSpec = new IvParameterSpec(generateIv());
            
            // 创建加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            // 加密数据
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // 将IV和加密数据组合
            byte[] combined = new byte[IV_LENGTH + encryptedData.length];
            System.arraycopy(ivSpec.getIV(), 0, combined, 0, IV_LENGTH);
            System.arraycopy(encryptedData, 0, combined, IV_LENGTH, encryptedData.length);
            
            // 返回Base64编码的结果
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("数据加密失败", e);
            throw new RuntimeException("数据加密失败", e);
        }
    }

    /**
     * 解密敏感数据
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            // 解码Base64
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            
            // 提取IV和加密数据
            byte[] iv = new byte[IV_LENGTH];
            byte[] data = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, data, 0, data.length);
            
            // 创建密钥和IV
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), AES);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // 创建解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // 解密数据
            byte[] decryptedData = cipher.doFinal(data);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("数据解密失败", e);
            throw new RuntimeException("数据解密失败", e);
        }
    }
    
    /**
     * 手机号脱敏处理
     */
    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 身份证号脱敏处理
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 15) {
            return idCard;
        }
        int length = idCard.length();
        return idCard.substring(0, 6) + "********" + idCard.substring(length - 4);
    }
    
    /**
     * 姓名脱敏处理
     */
    public String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        } else if (name.length() == 2) {
            return name.charAt(0) + "*";
        } else {
            return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
        }
    }
    
    /**
     * 生成随机IV
     */
    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * 获取密钥字节数组
     */
    private byte[] getKeyBytes() {
        // 确保密钥长度为32字节（256位）
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32];
        System.arraycopy(keyBytes, 0, result, 0, Math.min(keyBytes.length, 32));
        return result;
    }
    
    /**
     * 生成随机密钥
     */
    public String generateRandomKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
            keyGenerator.init(KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            log.error("生成随机密钥失败", e);
            throw new RuntimeException("生成随机密钥失败", e);
        }
    }
}