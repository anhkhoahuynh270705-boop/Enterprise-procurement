package com.example.shopping.workflow.camunda.dto.response;

import java.util.Date;
import java.util.Map;

import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CamundaTaskDto {

    private String taskId;

    private String taskName;

    private String processInstanceId;

    private String assignee;

    private Date createTime;

    private ProcurementTicketResponseDto ticket;

    private Map<String, Object> processVariables;
}
