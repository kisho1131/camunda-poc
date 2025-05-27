package com.citi.bs.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;


public class WorkflowInstanceResponseC7 {
  private String processInstanceId;

  public WorkflowInstanceResponseC7() {
  }

  public WorkflowInstanceResponseC7(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }
}