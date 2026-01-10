package com.knowledge.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.api.dto.DepartmentDTO;
import com.knowledge.api.service.DepartmentService;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.user.entity.Department;
import com.knowledge.user.mapper.DepartmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@DubboService
public class DepartmentServiceImpl implements DepartmentService {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        log.info("创建部门 - 部门名称: {}", departmentDTO.getName());
        
        // 检查部门名称是否已存在
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getName, departmentDTO.getName());
        Department existingDept = departmentMapper.selectOne(wrapper);
        if (existingDept != null) {
            throw new BusinessException(400, "部门名称已存在");
        }
        
        Department department = new Department();
        BeanUtils.copyProperties(departmentDTO, department);
        department.setCreateTime(java.time.LocalDateTime.now());
        department.setUpdateTime(java.time.LocalDateTime.now());
        departmentMapper.insert(department);
        
        log.info("部门创建成功 - 部门ID: {}, 部门名称: {}", department.getId(), department.getName());
        
        DepartmentDTO result = new DepartmentDTO();
        BeanUtils.copyProperties(department, result);
        return result;
    }

    @Override
    @Transactional
    public DepartmentDTO updateDepartment(DepartmentDTO departmentDTO) {
        log.info("更新部门 - 部门ID: {}", departmentDTO.getId());
        
        Department department = departmentMapper.selectById(departmentDTO.getId());
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        
        // 如果修改了部门名称，检查新名称是否已存在
        if (!department.getName().equals(departmentDTO.getName())) {
            LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Department::getName, departmentDTO.getName());
            Department existingDept = departmentMapper.selectOne(wrapper);
            if (existingDept != null) {
                throw new BusinessException(400, "部门名称已存在");
            }
        }
        
        department.setName(departmentDTO.getName());
        department.setDescription(departmentDTO.getDescription());
        department.setUpdateTime(java.time.LocalDateTime.now());
        departmentMapper.updateById(department);
        
        log.info("部门更新成功 - 部门ID: {}", departmentDTO.getId());
        
        DepartmentDTO result = new DepartmentDTO();
        BeanUtils.copyProperties(department, result);
        return result;
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        log.info("删除部门 - 部门ID: {}", id);
        
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        
        // 检查是否有用户使用该部门
        // 这里需要查询user表，但为了解耦，我们暂时不在这里检查
        // 可以在Controller层或通过其他方式检查
        
        departmentMapper.deleteById(id);
        log.info("部门删除成功 - 部门ID: {}", id);
    }

    @Override
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            return null;
        }
        DepartmentDTO result = new DepartmentDTO();
        BeanUtils.copyProperties(department, result);
        return result;
    }

    @Override
    public List<DepartmentDTO> getAllDepartments() {
        List<Department> departments = departmentMapper.selectList(null);
        return departments.stream()
            .map(dept -> {
                DepartmentDTO dto = new DepartmentDTO();
                BeanUtils.copyProperties(dept, dto);
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    public DepartmentDTO getDepartmentByName(String name) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getName, name);
        Department department = departmentMapper.selectOne(wrapper);
        if (department == null) {
            return null;
        }
        DepartmentDTO result = new DepartmentDTO();
        BeanUtils.copyProperties(department, result);
        return result;
    }
}

