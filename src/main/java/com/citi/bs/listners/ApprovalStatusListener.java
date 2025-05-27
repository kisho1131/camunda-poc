package com.citi.bs.listners;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class ApprovalStatusListener implements ExecutionListener {

  private final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ApprovalStatusListener.class);

  @Override
  public void notify(DelegateExecution execution) throws Exception {
    String eventName = execution.getCurrentActivityName(); // Will be "Request Approved" or "Request Rejected"
    String decision = (String) execution.getVariable("checkerDecision"); // Get the decision variable

    if ("Request Approved".equals(eventName)) {
      LOGGER.info("--- Process Instance {} finished. Request was APPROVED! Decision: {} ---", execution.getProcessInstanceId(), decision);
    } else if ("Request Rejected".equals(eventName)) {
      LOGGER.info("--- Process Instance {} finished. Request was REJECTED! Decision: {} ---", execution.getProcessInstanceId(), decision);
    } else {
      LOGGER.info("--- Process Instance {} finished at unknown end event: {} ---", execution.getProcessInstanceId(), eventName);
    }
  }
}
