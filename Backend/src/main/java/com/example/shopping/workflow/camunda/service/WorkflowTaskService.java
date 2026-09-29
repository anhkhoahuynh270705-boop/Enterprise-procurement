package com.example.shopping.workflow.camunda.service;

import java.util.List;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.workflow.camunda.dto.response.CamundaTaskDto;

public interface WorkflowTaskService {
    String startProcurementProcess(ProcurementTicket ticket);
    List<CamundaTaskDto> getPendingTasksForChecker();
    void completeCheckerReview(String taskId, String checkerUsername, boolean approved, String comment);
    String getActiveTaskIdForTicket(String processInstanceId);
}
