package com.example.shopping.workflow.camunda.service.impl.supplierworkflow;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.supplier.proposal.entity.SupplierProposal;
import com.example.shopping.workflow.camunda.service.SupplierProposalWorkflowService;

import lombok.RequiredArgsConstructor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierProposalWorkflowServiceImpl implements SupplierProposalWorkflowService {
    public static final String PROCESS_KEY = "supplier-proposal-maker-checker";
    private static final String REVIEW_TASK_KEY = "UserTask_CheckerReviewProposal";

    RuntimeService runtimeService;
    TaskService taskService;

    @Override
    @Transactional
    public String startReviewProcess(SupplierProposal proposal) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("proposalId", proposal.getId().toString());
        variables.put("supplierName", proposal.getName());
        variables.put("taxCode", proposal.getTaxCode());
        variables.put("submittedBy", proposal.getSubmittedBy());

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                PROCESS_KEY, proposal.getId().toString(), variables);
        proposal.setCamundaProcessInstanceId(instance.getId());
        return instance.getId();
    }

    @Override
    @Transactional
    public void completeCheckerReview(
            SupplierProposal proposal, String checkerUsername, boolean approved, String comment) {
        String processInstanceId = proposal.getCamundaProcessInstanceId();
        if (processInstanceId == null || processInstanceId.isBlank()) {
            startReviewProcess(proposal);
            processInstanceId = proposal.getCamundaProcessInstanceId();
        }

        Task task = taskService.createTaskQuery()
                .processDefinitionKey(PROCESS_KEY)
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(REVIEW_TASK_KEY)
                .active()
                .singleResult();
        if (task == null) {
            throw ErrorEnum.SUPPLIER_PROPOSAL_NO_ACTIVE_TASK.ex();
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        variables.put("checkerUsername", checkerUsername);
        variables.put("reviewComment", comment == null ? "" : comment);
        taskService.complete(task.getId(), variables);
    }
}
