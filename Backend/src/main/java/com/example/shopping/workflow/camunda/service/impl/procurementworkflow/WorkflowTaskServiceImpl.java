package com.example.shopping.workflow.camunda.service.impl.procurementworkflow;

import com.example.shopping.workflow.camunda.service.WorkflowTaskService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.workflow.camunda.dto.response.CamundaTaskDto;
import com.example.shopping.workflow.camunda.mapper.WorkflowTaskMapper;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.common.enums.ErrorEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    public static final String PROCESS_KEY = "procurement-maker-checker";

    RuntimeService runtimeService;
    TaskService taskService;
    ProcurementTicketRepository ticketRepository;
    ProcurementMapper procurementMapper;
    WorkflowTaskMapper workflowTaskMapper;

    @Transactional
    public String startProcurementProcess(ProcurementTicket ticket) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ticketId", ticket.getId().toString());
        variables.put("ticketCode", ticket.getTicketCode());
        variables.put("title", ticket.getTitle());
        variables.put("totalAmount", ticket.getTotalAmount() != null ? ticket.getTotalAmount().doubleValue() : 0.0);
        variables.put("makerUsername", ticket.getMakerUsername());

        log.info("Khởi chạy tiến trình cho phiếu:", PROCESS_KEY, ticket.getTicketCode());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, ticket.getTicketCode(), variables);

        ticket.setCamundaProcessInstanceId(instance.getId());
        ticket.setStatus(ProcurementStatus.PENDING_APPROVAL);
        ticketRepository.save(ticket);

        return instance.getId();
    }

    /*
     * Lấy danh sách nhiệm vụ đang chờ Checker phê duyệt.
     * Tối ưu N+1: thay vì gọi DB riêng lẻ cho từng task,
     * ta batch fetch tất cả biến Camunda qua processInstanceId
     * và batch fetch tất cả phiếu DB qua IN query duy nhất.
     */
    @Transactional(readOnly = true)
    public List<CamundaTaskDto> getPendingTasksForChecker() {
        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .active()
                .orderByTaskCreateTime().desc()
                .list();

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch variables theo processInstanceId 
        List<String> processInstanceIds = tasks.stream()
                .map(Task::getProcessInstanceId)
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> variablesByProcessInstance = runtimeService
                .createVariableInstanceQuery()
                .processInstanceIdIn(processInstanceIds.toArray(new String[0]))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        v -> v.getProcessInstanceId(),
                        Collectors.toMap(v -> v.getName(), v -> v.getValue() != null ? v.getValue() : "")));

        Set<UUID> ticketIds = variablesByProcessInstance.values().stream()
                .filter(vars -> vars.containsKey("ticketId"))
                .map(vars -> {
                    try {
                        return UUID.fromString((String) vars.get("ticketId"));
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<UUID, ProcurementTicketResponseDto> ticketDtoMap = Collections.emptyMap();
        if (!ticketIds.isEmpty()) {
            ticketDtoMap = ticketRepository.findAllByIdIn(ticketIds)
                    .stream()
                    .collect(Collectors.toMap(
                            ProcurementTicket::getId,
                            procurementMapper::toDto,
                            (a, b) -> a));
        }

        List<CamundaTaskDto> result = new ArrayList<>(tasks.size());
        final Map<UUID, ProcurementTicketResponseDto> finalTicketMap = ticketDtoMap;

        for (Task task : tasks) {
            Map<String, Object> vars = variablesByProcessInstance.getOrDefault(task.getProcessInstanceId(), Collections.emptyMap());
            String ticketIdStr = (String) vars.get("ticketId");
            ProcurementTicketResponseDto ticketDto = null;
            if (ticketIdStr != null) {
                try {
                    ticketDto = finalTicketMap.get(UUID.fromString(ticketIdStr));
                } catch (IllegalArgumentException ignored) { }
            }
            result.add(workflowTaskMapper.toDto(task, ticketDto, vars));
        }

        return result;
    }

    /* Checker review */
    @Transactional
    public void completeCheckerReview(String taskId, String checkerUsername, boolean approved, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).processDefinitionKey(PROCESS_KEY).taskDefinitionKey("UserTask_CheckerReview").active().singleResult();
        if (task == null) {
            throw ErrorEnum.CAMUNDA_TASK_NOT_FOUND.ex("Không tìm thấy nhiệm vụ Camunda với taskId: " + taskId);
        }
        String ticketIdSrt = ( String ) runtimeService.getVariable(task.getProcessInstanceId(), "ticketId");
        if(ticketIdSrt == null ) {
            throw ErrorEnum.CAMUNDA_TICKET_ID_NOT_FOUND.ex("Không tìm thấy ticketId cho processInstanceId: " + task.getProcessInstanceId());
        }
        UUID ticketId = UUID.fromString(ticketIdSrt);
        ProcurementTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());
        if(!TicketAccess.hasRole("ADMIN") && ticket.getMakerUsername().equals(checkerUsername)){
            throw ErrorEnum.TICKET_SELF_APPROVE.ex();
        }
        
        if (ticket.getStatus() != ProcurementStatus.PENDING_APPROVAL || !task.getProcessInstanceId().equals(ticket.getCamundaProcessInstanceId())) {
            throw ErrorEnum.TICKET_NO_PENDING_TASK.ex();
        }
        Map<String, Object> completeVars = new HashMap<>();
        completeVars.put("approved", approved);
        completeVars.put("checkerUsername", checkerUsername);
        completeVars.put("checkerComment", comment != null ? comment : "");

        log.info("Checker hoàn tất nhiệm vụ- Kết quả: ",
                checkerUsername, taskId, approved);

        taskService.complete(taskId, completeVars);
        ticket.setCheckerUsername(checkerUsername);
        ticket.setCheckerComment(comment);
        if(approved) {
            ticket.setStatus(ProcurementStatus.APPROVED);
            ticket.setApprovedAt(LocalDateTime.now());
        } else{
            ticket.setStatus(ProcurementStatus.REJECTED);
            ticket.setApprovedAt(LocalDateTime.now());
        }
        ticketRepository.save(ticket);
        log.info("Checker hoàn tất phiếu :", checkerUsername, ticket.getTicketCode(), approved);
    }

    public String getActiveTaskIdForTicket(String processInstanceId) {
        if (processInstanceId == null) {
            return null;
        }
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();
        return task != null ? task.getId() : null;
    }
}
