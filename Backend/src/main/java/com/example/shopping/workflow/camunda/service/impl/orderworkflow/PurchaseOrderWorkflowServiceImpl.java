    package com.example.shopping.workflow.camunda.service.impl.orderworkflow;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.purchaseorder.entity.PurchaseOrder;
import com.example.shopping.workflow.camunda.service.PurchaseOrderWorkflowService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PurchaseOrderWorkflowServiceImpl implements PurchaseOrderWorkflowService {

    public static final String PROCESS_KEY = "purchase-order-maker-checker";
    static final String REVIEW_TASK_KEY = "UserTask_CheckerReviewPurchaseOrder";

    RuntimeService runtimeService;
    TaskService taskService;

    @Override
    @Transactional
    public String startReviewProcess(PurchaseOrder order) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("purchaseOrderId", order.getId().toString());
        variables.put("orderCode", order.getOrderCode());
        variables.put("supplierName", order.getSupplier().getName());
        variables.put("createdBy", order.getCreatedBy());
        variables.put("totalAmount", order.getTotalAmount().doubleValue());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, order.getId().toString(), variables);
        order.setCamundaProcessInstanceId(instance.getId());
        return instance.getId();
    }

    @Override
    @Transactional
    public void completeReview(PurchaseOrder order, String reviewer, boolean approved, String comment) {
        String processInstanceId = order.getCamundaProcessInstanceId();
        if (processInstanceId == null || processInstanceId.isBlank()) {
            startReviewProcess(order);
            processInstanceId = order.getCamundaProcessInstanceId();
        }

        Task task = taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(REVIEW_TASK_KEY)
                .active()
                .singleResult();
        if (task == null) {
            throw ErrorEnum.PURCHASE_ORDER_NO_ACTIVE_TASK.ex();
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        variables.put("reviewer", reviewer);
        variables.put("reviewComment", comment == null ? "" : comment);
        taskService.complete(task.getId(), variables);
    }
}
