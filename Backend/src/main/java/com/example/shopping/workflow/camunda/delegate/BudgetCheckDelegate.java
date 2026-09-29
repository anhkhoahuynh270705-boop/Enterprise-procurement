package com.example.shopping.workflow.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("budgetCheckDelegate")
public class BudgetCheckDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String ticketCode = (String) execution.getVariable("ticketCode");
        Double totalAmount = (Double) execution.getVariable("totalAmount");

        log.info("Kiểm tra ngân sách cho phiếu: ", ticketCode, totalAmount);

        boolean highBudget = totalAmount != null && totalAmount > 100_000_000.0;
        execution.setVariable("highBudgetFlag", highBudget);

        log.info("Ngân sách hợp lệ. Chuyển sang nhiệm vụ User Task cho Checker duyệt.");
    }
}
