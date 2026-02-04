package com.befapress.controller;

import com.befapress.dto.response.DepartmentResponse;
import com.befapress.dto.response.MessageResponse;
import com.befapress.entity.Department;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "APIs for managing departments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    @Operation(summary = "List all departments")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return ResponseEntity.ok(departments.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return ResponseEntity.ok(mapToResponse(department));
    }

    @PostMapping
    @Operation(summary = "Create a new department")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new BadRequestException("Department with this name already exists");
        }

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .build();

        department = departmentRepository.save(department);
        return ResponseEntity.ok(mapToResponse(department));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a department")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department = departmentRepository.save(department);

        return ResponseEntity.ok(mapToResponse(department));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a department")
    public ResponseEntity<MessageResponse> deleteDepartment(@PathVariable Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        department.setActive(false);
        departmentRepository.save(department);

        return ResponseEntity.ok(MessageResponse.success("Department deactivated"));
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .isActive(department.isActive())
                .createdAt(department.getCreatedAt())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentRequest {
        private String name;
        private String description;
    }
}
