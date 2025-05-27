package com.citi.bs.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkflowRequestC7 {
  private String requestId;
  private String initialContent;
  private String creator; // Will be used as assignee for maker task
  private String approver; // Will be used as assignee for checker task

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getInitialContent() {
    return initialContent;
  }

  public void setInitialContent(String initialContent) {
    this.initialContent = initialContent;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getApprover() {
    return approver;
  }

  public void setApprover(String approver) {
    this.approver = approver;
  }
}
