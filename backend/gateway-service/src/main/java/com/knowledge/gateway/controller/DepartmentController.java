package com.knowledge.gateway.controller;

import com.knowledge.api.dto.DepartmentDTO;
import com.knowledge.api.service.DepartmentService;
import com.knowledge.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @DubboReference(check = false, timeout = 10000)
    private DepartmentService departmentService;

    @PostMapping
    public Result<DepartmentDTO> createDepartment(@RequestBody DepartmentDTO departmentDTO) {
        log.info("创建部门 - 部门名称: {}", departmentDTO.getName());
        try {
            DepartmentDTO result = departmentService.createDepartment(departmentDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建部门失败 - 部门名称: {}, 错误: {}", departmentDTO.getName(), e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<DepartmentDTO> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDTO departmentDTO) {
        log.info("更新部门 - 部门ID: {}", id);
        try {
            departmentDTO.setId(id);
            DepartmentDTO result = departmentService.updateDepartment(departmentDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新部门失败 - 部门ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        log.info("删除部门 - 部门ID: {}", id);
        try {
            departmentService.deleteDepartment(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除部门失败 - 部门ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<DepartmentDTO> getDepartment(@PathVariable Long id) {
        try {
            DepartmentDTO departmentDTO = departmentService.getDepartmentById(id);
            if (departmentDTO == null) {
                return Result.error("部门不存在");
            }
            return Result.success(departmentDTO);
        } catch (Exception e) {
            log.error("获取部门信息失败 - 部门ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    public Result<List<DepartmentDTO>> getAllDepartments() {
        try {
            List<DepartmentDTO> departments = departmentService.getAllDepartments();
            return Result.success(departments);
        } catch (Exception e) {
            log.error("获取部门列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}

