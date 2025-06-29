package com.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class CheckerApproval implements JavaDelegate {
  @Override
  public void execute(DelegateExecution delegateExecution) throws Exception {
    System.out.println("Checker Approval task ........");
  }
}
