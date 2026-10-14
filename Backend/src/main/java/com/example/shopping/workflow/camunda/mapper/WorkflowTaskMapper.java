package com.example.shopping.workflow.camunda.mapper;

import java.util.Map;

import org.camunda.bpm.engine.task.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.workflow.camunda.dto.response.CamundaTaskDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkflowTaskMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskName", source = "task.name")
    @Mapping(target = "processInstanceId", source = "task.processInstanceId")
    @Mapping(target = "assignee", source = "task.assignee")
    @Mapping(target = "createTime", source = "task.createTime")
    @Mapping(target = "ticket", source = "ticket")
    @Mapping(target = "processVariables", source = "variables")
    CamundaTaskDto toDto(Task task, ProcurementTicketResponseDto ticket, Map<String, Object> variables);
}
