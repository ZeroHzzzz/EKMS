package com.knowledge.common.util;

import com.knowledge.common.constant.Constants;
import com.knowledge.common.exception.BusinessException;

/**
 * 用户验证工具类
 * 统一处理用户相关的验证逻辑
 */
public class UserValidationUtil {

    /**
     * 验证角色是否有效
     * @param role 角色
     * @return true表示有效
     */
    public static boolean isValidRole(String role) {
        return Constants.ROLE_ADMIN.equals(role) ||
               Constants.ROLE_EDITOR.equals(role) ||
               Constants.ROLE_USER.equals(role);
    }

    /**
     * 验证是否为系统管理员角色
     * @param role 角色
     * @return true表示是系统管理员
     */
    public static boolean isAdminRole(String role) {
        return Constants.ROLE_ADMIN.equals(role);
    }

    /**
     * 验证部门ID是否必须
     * 规则：系统管理员不需要部门，其他角色必须选择部门
     * 
     * @param role 用户角色
     * @param departmentId 部门ID
     * @throws BusinessException 如果验证失败
     */
    public static void validateDepartment(String role, Long departmentId) {
        if (isAdminRole(role)) {
            // 系统管理员不需要部门，允许departmentId为null
            return;
        } else {
            // 非系统管理员必须选择部门
            if (departmentId == null) {
                throw new BusinessException(400, "非系统管理员必须选择部门");
            }
        }
    }

    /**
     * 验证注册时的角色
     * 规则：允许注册所有有效角色（包括系统管理员）
     * 
     * @param role 角色
     * @throws BusinessException 如果验证失败
     */
    public static void validateRegisterRole(String role) {
        if (role == null || role.isEmpty()) {
            return; // 允许为空，会使用默认值
        }
        if (!isValidRole(role)) {
            throw new BusinessException(400, "无效的用户角色");
        }
        // 系统管理员可以注册，且不需要选择部门（由validateDepartment处理）
    }
}

