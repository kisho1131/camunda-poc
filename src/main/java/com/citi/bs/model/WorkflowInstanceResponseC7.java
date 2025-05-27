package com.citi.bs.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


public class WorkflowInstanceResponseC7 {
  private String processInstanceId;

  public WorkflowInstanceResponseC7(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }
}