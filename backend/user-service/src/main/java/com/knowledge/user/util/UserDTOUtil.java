package com.knowledge.user.util;

import com.knowledge.api.dto.UserDTO;
import com.knowledge.common.util.UserValidationUtil;
import com.knowledge.user.entity.Department;
import com.knowledge.user.entity.User;
import com.knowledge.user.mapper.DepartmentMapper;
import org.springframework.beans.BeanUtils;

/**
 * 用户DTO转换工具类
 * 统一处理User和UserDTO之间的转换，包括部门信息的填充
 */
public class UserDTOUtil {

    /**
     * 将User实体转换为UserDTO，并填充部门名称
     * 
     * @param user User实体
     * @param departmentMapper 部门Mapper（用于查询部门信息）
     * @return UserDTO
     */
    public static UserDTO toDTO(User user, DepartmentMapper departmentMapper) {
        if (user == null) {
            return null;
        }
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        userDTO.setPassword(null); // 清除密码字段
        
        // 填充部门名称
        fillDepartmentName(userDTO, user, departmentMapper);
        
        return userDTO;
    }

    /**
     * 填充用户DTO的部门名称
     * 
     * @param userDTO 用户DTO
     * @param user User实体
     * @param departmentMapper 部门Mapper
     */
    public static void fillDepartmentName(UserDTO userDTO, User user, DepartmentMapper departmentMapper) {
        if (user == null || userDTO == null) {
            return;
        }
        
        if (user.getDepartmentId() != null) {
            // 有部门ID，查询部门名称
            Department department = departmentMapper.selectById(user.getDepartmentId());
            if (department != null) {
                userDTO.setDepartment(department.getName());
            }
        } else if (UserValidationUtil.isAdminRole(user.getRole())) {
            // 系统管理员，显示"系统"
            userDTO.setDepartment("系统");
        }
    }

    /**
     * 处理用户部门ID的设置
     * 规则：系统管理员不设置部门（departmentId为null），其他用户必须设置部门
     * 
     * @param user User实体
     * @param role 用户角色
     * @param departmentId 部门ID
     * @throws RuntimeException 如果验证失败
     */
    public static void setDepartmentId(User user, String role, Long departmentId) {
        UserValidationUtil.validateDepartment(role, departmentId);
        
        if (UserValidationUtil.isAdminRole(role)) {
            user.setDepartmentId(null);
        } else {
            user.setDepartmentId(departmentId);
        }
    }
}

