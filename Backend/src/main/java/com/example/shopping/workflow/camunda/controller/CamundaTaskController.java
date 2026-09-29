package com.example.shopping.workflow.camunda.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.procurement.dto.request.CheckerReviewRequestDto;
import com.example.shopping.workflow.camunda.dto.response.CamundaTaskDto;
import com.example.shopping.workflow.camunda.service.WorkflowTaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/workflow/camunda")
@RequiredArgsConstructor
public class CamundaTaskController {

    private final WorkflowTaskService workflowTaskService;

    private String extractUsername(Jwt jwt) {
        if (jwt == null) {
            return "system_checker";
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }

    /**
     * Lấy danh sách nhiệm vụ đang chờ Checker phê duyệt
     */
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<CamundaTaskDto>>> getPendingTasks() {
        List<CamundaTaskDto> tasks = workflowTaskService.getPendingTasksForChecker();
        return ResponseEntity.ok(ApiResponse.success(tasks, "Lấy danh sách nhiệm vụ chờ duyệt thành công"));
    }

    /**
     * Checker hoàn tất phê duyệt hoặc từ chối
     */
    @PostMapping("/tasks/{taskId}/review")
    public ResponseEntity<ApiResponse<Void>> reviewTask(
            @PathVariable String taskId,
            @Valid @RequestBody CheckerReviewRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String checkerUsername = extractUsername(jwt);
        workflowTaskService.completeCheckerReview(taskId, checkerUsername, request.getApproved(), request.getComment());
        String actionText = Boolean.TRUE.equals(request.getApproved()) ? "phê duyệt" : "từ chối";
        return ResponseEntity.ok(ApiResponse.success(null, "Đã " + actionText + " phiếu thành công"));
    }
}
