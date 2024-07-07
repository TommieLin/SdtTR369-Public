package com.sdt.diagnose.common;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * @Author Outis
 * @Date 2023/11/30 13:52
 * @Version 1.0
 */
public class AESUtil {
    private String aesKey;
    private static final String ALGORITHMS = "AES/ECB/PKCS5Padding";
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    // 加密算法和填充模式
    private static final String ALGORITHM = "AES";
    private static final String AES_CBC_PADDING = "AES/CBC/PKCS5Padding";

    // 初始向量和密码
    private static final byte[] DEFAULT_KEY_VI = "Sdt@lonely#9527@".getBytes(StandardCharsets.UTF_8);
    public static final byte[] DEFAULT_SECRET_KEY = "SDt@sDT%14#2022!".getBytes(StandardCharsets.UTF_8);


    /**
     * Aes加密(CBC工作模式)
     * <p>
     * 说明：
     * DEFAULT_SECRET_KEY密钥,长度必须等于16
     * DEFAULT_KEY_VI 初始化向量,keyIv长度必须等于16
     *
     * @param data 明文
     * @return 密文
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data) {
        try {
            //获取SecretKey对象,也可以使用getSecretKey()方法
            Key secretKey = new SecretKeySpec(DEFAULT_SECRET_KEY, ALGORITHM);
            //获取指定转换的密码对象Cipher（参数：算法/工作模式/填充模式）
            Cipher cipher = Cipher.getInstance(AES_CBC_PADDING);
            //创建向量参数规范也就是初始化向量
            IvParameterSpec ips = new IvParameterSpec(DEFAULT_KEY_VI);
            //用密钥和一组算法参数规范初始化此Cipher对象（加密模式）
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ips);
            //执行加密操作
            return cipher.doFinal(data);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Aes解密(ECB工作模式)
     * <p>
     * DEFAULT_SECRET_KEY密钥,长度必须等于16
     * DEFAULT_KEY_VI 初始化向量,keyIv长度必须等于16
     *
     * @param data 密文
     * @return 明文
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data) {
        try {
            //获取SecretKey对象,也可以使用getSecretKey()方法
            Key secretKey = new SecretKeySpec(DEFAULT_SECRET_KEY, ALGORITHM);
            //获取指定转换的密码对象Cipher（参数：算法/工作模式/填充模式）
            Cipher cipher = Cipher.getInstance(AES_CBC_PADDING);
            //创建向量参数规范也就是初始化向量
            IvParameterSpec ips = new IvParameterSpec(DEFAULT_KEY_VI);
            //用密钥和一组算法参数规范初始化此Cipher对象（加密模式）
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ips);
            //执行加密操作
            return cipher.doFinal(data);
        } catch (Exception e) {
            return null;
        }
    }

//    public void init() {
//        KeyGenerator keyGenerator = null;
//        try {
//            keyGenerator = KeyGenerator.getInstance("AES");
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//            return;
//        }
//        keyGenerator.init(128);     // 设置密钥长度为128
//        try {
//            encryptCipher = Cipher.getInstance(ALGORITHMS);   // 创建密码器
//            encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey.getBytes(), "AES"));
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
//            e.printStackTrace();
//            return;
//        }
//        try {
//            decryptCipher = Cipher.getInstance(ALGORITHMS);   // 创建密码器
//            decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey.getBytes(), "AES"));
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
//            e.printStackTrace();
//        }
//    }

//    // 加密
//    public byte[] encrypt(byte[] src) {
//        byte[] bytes = null;
//        try {
//            bytes = encryptCipher.doFinal(src);
//        } catch (IllegalBlockSizeException | BadPaddingException e) {
//            e.printStackTrace();
//        }
//        return bytes;
//    }
//
//    public byte[] encrypt(byte[] src, int pos, int length) {
//        byte[] bytes = null;
//        try {
//            bytes = encryptCipher.doFinal(src, pos, length);
//        } catch (IllegalBlockSizeException | BadPaddingException e) {
//            e.printStackTrace();
//        }
//        return bytes;
//    }
//
//    // 解密
//    public byte[] decrypt(byte[] src) {
//        byte[] bytes = null;
//        try {
//            bytes = decryptCipher.doFinal(src);
//        } catch (IllegalBlockSizeException | BadPaddingException e) {
//            e.printStackTrace();
//        }
//        return bytes;
//    }
//
//    public byte[] decrypt(byte[] src, int pos, int length) {
//        byte[] bytes = null;
//        try {
//            bytes = decryptCipher.doFinal(src, pos, length);
//        } catch (IllegalBlockSizeException | BadPaddingException e) {
//            e.printStackTrace();
//        }
//        return bytes;
//    }
//
//    // 补全不足16整数倍的数 因为128位加密需要位数是128整数倍的字节数组，一个字节8位 8*16=128
//    public int countPadding(int src) {
//        return (src + 16) / 16 * 16;
//    }


}
