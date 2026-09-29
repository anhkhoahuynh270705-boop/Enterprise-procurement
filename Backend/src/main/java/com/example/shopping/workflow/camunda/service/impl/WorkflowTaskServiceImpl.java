package com.example.shopping.workflow.camunda.service.impl;

import com.example.shopping.workflow.camunda.service.WorkflowTaskService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.camunda.bpm.engine.HistoryService;
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
import com.example.shopping.security.TicketAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    public static final String PROCESS_KEY = "procurement-maker-checker";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ProcurementTicketRepository ticketRepository;
    private final ProcurementMapper procurementMapper;

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

    /**
     * Lấy danh sách nhiệm vụ đang chờ Checker phê duyệt.
     *
     * <p>Tối ưu N+1: thay vì gọi DB riêng lẻ cho từng task,
     * ta batch fetch tất cả biến Camunda qua processInstanceId
     * và batch fetch tất cả phiếu DB qua IN query duy nhất.</p>
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
            ticketDtoMap = ticketRepository.findAllByIdIn(ticketIds).stream()
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

            result.add(CamundaTaskDto.builder()
                    .taskId(task.getId())
                    .taskName(task.getName())
                    .processInstanceId(task.getProcessInstanceId())
                    .assignee(task.getAssignee())
                    .createTime(task.getCreateTime())
                    .ticket(ticketDto)
                    .processVariables(vars)
                    .build());
        }

        return result;
    }

    /* Checker review */
    @Transactional
    public void completeCheckerReview(String taskId, String checkerUsername, boolean approved, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).processDefinitionKey(PROCESS_KEY).taskDefinitionKey("UserTask_CheckerReview").active().singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Không tìm thấy nhiệm vụ Camunda với taskId: " + taskId);
        }
        String ticketIdSrt = ( String ) runtimeService.getVariable(task.getProcessInstanceId(), "ticketId");
        if(ticketIdSrt == null ) {
            throw new IllegalArgumentException("Không tìm thấy ticketId cho processInstanceId: " + task.getProcessInstanceId());
        }
        UUID ticketId = UUID.fromString(ticketIdSrt);
        ProcurementTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mua hàng với id: " + ticketId));
        if(!TicketAccess.hasRole("ADMIN") && ticket.getMakerUsername().equals(checkerUsername)){
            throw new IllegalArgumentException("Không được duyệt phiếu cho chính mình tự tạo");
        }
        
        if (ticket.getStatus() != ProcurementStatus.PENDING_APPROVAL || !task.getProcessInstanceId().equals(ticket.getCamundaProcessInstanceId())) {
            throw new IllegalArgumentException("Phi?u kh?ng c? nhi?m v? ch? duy?t h?p l?.");
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
