package com.knowledge.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 用于生成BCrypt加密的密码hash
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密密码
     * @param rawPassword 明文密码
     * @return BCrypt加密后的密码hash
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     * @param rawPassword 明文密码
     * @param encodedPassword 加密后的密码hash
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 测试方法：生成密码hash
     * 运行此main方法可以生成测试密码的hash值
     */
    public static void main(String[] args) {
        String[] passwords = {
            "password123",  // 测试账号通用密码
            "admin123",     // admin账号密码
            "auditor123",   // 审核员密码
            "editor123",    // 编辑员密码
            "user123"       // 普通用户密码
        };

        System.out.println("=== 密码Hash生成工具 ===\n");
        for (String password : passwords) {
            String hash = encode(password);
            System.out.println("密码: " + password);
            System.out.println("Hash: " + hash);
            System.out.println("验证: " + matches(password, hash));
            System.out.println("---");
        }
    }
}

