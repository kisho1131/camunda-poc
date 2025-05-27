package com.citi.bs.model;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class TaskResponseC7 {
  private String id;
  private String name;
  private String assignee;
  private Date createTime;
  private String processInstanceId;
  private String taskDefinitionKey;
}
