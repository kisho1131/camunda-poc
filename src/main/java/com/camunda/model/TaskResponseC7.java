package com.camunda.model;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@RequiredArgsConstructor
public class TaskResponseC7 {
  private String id;
  private String name;
  private String assignee;
  private Date createTime;
  private String processInstanceId;
  private String taskDefinitionKey;

  public TaskResponseC7(String id, String name, String assignee, Date createTime, String processInstanceId, String taskDefinitionKey) {
    this.id = id;
    this.name = name;
    this.assignee = assignee;
    this.createTime = createTime;
    this.processInstanceId = processInstanceId;
    this.taskDefinitionKey = taskDefinitionKey;
  }
}
