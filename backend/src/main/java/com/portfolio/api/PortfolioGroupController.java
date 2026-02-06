package com.portfolio.api;

import com.portfolio.portfolio.entity.PortfolioGroup;
import com.portfolio.portfolio.repository.PortfolioGroupRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/portfolio-groups")
@RequiredArgsConstructor
public class PortfolioGroupController {
    
    private final PortfolioGroupRepository groupRepository;
    
    // 개발 모드: 임시 workspace ID
    private static final String DEFAULT_WORKSPACE_ID = "default-workspace";
    
    @GetMapping
    public ResponseEntity<?> listGroups() {
        try {
            List<PortfolioGroup> groups = groupRepository.findByWorkspaceIdOrderBySortOrder(DEFAULT_WORKSPACE_ID);
            
            List<Map<String, Object>> groupList = groups.stream()
                    .map(this::toGroupDto)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", groupList);
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        try {
            if (groupRepository.existsByNameAndWorkspaceId(request.getName(), DEFAULT_WORKSPACE_ID)) {
                return createErrorResponse("Group with name '" + request.getName() + "' already exists", 
                        HttpStatus.BAD_REQUEST);
            }
            
            PortfolioGroup group = PortfolioGroup.builder()
                    .workspaceId(DEFAULT_WORKSPACE_ID)
                    .name(request.getName())
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                    .build();
            
            group = groupRepository.save(group);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", toGroupDto(group));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateGroup(@PathVariable String id, @RequestBody UpdateGroupRequest request) {
        try {
            PortfolioGroup group = groupRepository.findByIdAndWorkspaceId(id, DEFAULT_WORKSPACE_ID)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
            
            if (request.getName() != null) {
                group.setName(request.getName());
            }
            if (request.getSortOrder() != null) {
                group.setSortOrder(request.getSortOrder());
            }
            
            group = groupRepository.save(group);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", toGroupDto(group));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable String id) {
        try {
            PortfolioGroup group = groupRepository.findByIdAndWorkspaceId(id, DEFAULT_WORKSPACE_ID)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
            
            groupRepository.delete(group);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", Map.of("message", "Group deleted successfully"));
            response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
            response.put("error", null);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private Map<String, Object> toGroupDto(PortfolioGroup group) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", group.getId());
        dto.put("name", group.getName());
        dto.put("sortOrder", group.getSortOrder());
        dto.put("createdAt", group.getCreatedAt().toString());
        return dto;
    }
    
    private ResponseEntity<?> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", status.name());
        error.put("message", message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        response.put("meta", Map.of("timestamp", java.time.Instant.now().toString()));
        response.put("error", error);
        
        return ResponseEntity.status(status).body(response);
    }
    
    @Data
    public static class CreateGroupRequest {
        private String name;
        private Integer sortOrder;
    }
    
    @Data
    public static class UpdateGroupRequest {
        private String name;
        private Integer sortOrder;
    }
}
