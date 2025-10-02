package com.camunda.service;


import com.camunda.model.*;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowC7Service {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowC7Service.class);
  private static final String BPMN_PROCESS_ID = "chatApprovalProcessC7";

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private TaskService taskService;

  public WorkflowInstanceResponseC7 startWorkflow(StartWorkflowRequestC7 request) {
    // 1. Request Validation (Simplified)
    if (request.getRequestId() == null || request.getRequestId().isBlank()) {
      throw new IllegalArgumentException("Request ID must be provided.");
    }
    if (request.getCreator() == null || request.getCreator().isBlank()) {
      throw new IllegalArgumentException("Creator (maker) must be provided.");
    }
    if (request.getApprover() == null || request.getApprover().isBlank()) {
      throw new IllegalArgumentException("Approver (checker) must be provided.");
    }

    logger.info("Starting C7 workflow for requestId: {} by creator: {}", request.getRequestId(), request.getCreator());

    Map<String, Object> variables = new HashMap<>();
    variables.put("requestId", request.getRequestId());
    variables.put("initialContent", Optional.ofNullable(request.getInitialContent()).orElse(""));
    variables.put("creator", request.getCreator()); // Used for makerTask assignee
    variables.put("approver", request.getApprover()); // Used for checkerTask assignee

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(BPMN_PROCESS_ID, variables);

    logger.info("C7 Workflow instance created with ID: {}", processInstance.getId());
    return new WorkflowInstanceResponseC7(processInstance.getProcessInstanceId());
  }

  public List<TaskResponseC7> getActiveTasksForProcessInstance(String processInstanceId, String taskDefinitionKey) {
    logger.debug("Fetching active C7 tasks for processInstanceId: {} and taskDefinitionKey: {}", processInstanceId, taskDefinitionKey);

    List<Task> tasks = taskService.createTaskQuery()
          .processInstanceId(processInstanceId)
          .taskDefinitionKey(taskDefinitionKey) // BPMN Element ID of the User Task
          .active()
          .list();

    return tasks.stream()
          .map(task -> new TaskResponseC7(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                task.getCreateTime(),
                task.getProcessInstanceId(),
                task.getTaskDefinitionKey()
          ))
          .collect(Collectors.toList());
  }

  public List<TaskResponseC7> getActiveTasksByAssignee(String assignee, String taskDefinitionKey) {
    logger.debug("Fetching active C7 tasks for assignee: {} and taskDefinitionKey: {}", assignee, taskDefinitionKey);

    List<Task> tasks = taskService.createTaskQuery()
          .taskAssignee(assignee)
          .taskDefinitionKey(taskDefinitionKey)
          .active()
          .list();

    return tasks.stream()
          .map(task -> new TaskResponseC7(
                task.getId(),
                task.getName(),
                task.getAssignee(),
                task.getCreateTime(),
                task.getProcessInstanceId(),
                task.getTaskDefinitionKey()
          ))
          .collect(Collectors.toList());
  }

  public void completeMakerTask(String taskId, MakerCommentRequestC7 request) {
    logger.info("Completing C7 Maker task {} with commentary.", taskId);
    Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
    if (task == null) {
      throw new IllegalArgumentException("Task with ID " + taskId + " not found or not active.");
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("makerComment", request.getCommentary());
    taskService.complete(taskId, variables);

    logger.info("C7 Maker task {} completed.", taskId);
  }

  public void completeCheckerTask(String taskId, CheckerDecisionRequestC7 request) {
    logger.info("Completing C7 Checker task {} with decision: {}", taskId, request.getDecision());
    Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
    if (task == null) {
      throw new IllegalArgumentException("Task with ID " + taskId + " not found or not active.");
    }

    if (!"approved".equalsIgnoreCase(request.getDecision()) && !"rejected".equalsIgnoreCase(request.getDecision())) {
      throw new IllegalArgumentException("Decision must be 'approved' or 'rejected'.");
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("checkerDecision", request.getDecision().toLowerCase());
    System.out.println(variables);
    taskService.complete(taskId);
    logger.info("C7 Checker task {} completed with decision: {}.", taskId, request.getDecision());
  }
}