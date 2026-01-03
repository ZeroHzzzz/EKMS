package com.knowledge.api.service;

import com.knowledge.api.dto.DepartmentDTO;

import java.util.List;

public interface DepartmentService {
    DepartmentDTO createDepartment(DepartmentDTO departmentDTO);
    DepartmentDTO updateDepartment(DepartmentDTO departmentDTO);
    void deleteDepartment(Long id);
    DepartmentDTO getDepartmentById(Long id);
    List<DepartmentDTO> getAllDepartments();
    DepartmentDTO getDepartmentByName(String name);
}

