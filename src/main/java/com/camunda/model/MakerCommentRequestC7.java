package com.camunda.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
public class MakerCommentRequestC7 {
  private String commentary;

  public String getCommentary() {
    return commentary;
  }

  public void setCommentary(String commentary) {
    this.commentary = commentary;
  }
}
