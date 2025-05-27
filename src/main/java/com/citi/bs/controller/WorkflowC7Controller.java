package com.citi.bs.controller;

import com.citi.bs.model.*;
import com.citi.bs.service.WorkflowC7Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/c7/workflow")
public class WorkflowC7Controller {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowC7Controller.class);

  @Autowired
  private WorkflowC7Service workflowC7Service;

  @PostMapping("/start")
  public ResponseEntity<WorkflowInstanceResponseC7> startWorkflow(@RequestBody StartWorkflowRequestC7 request) {
    try {
      WorkflowInstanceResponseC7 response = workflowC7Service.startWorkflow(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      logger.warn("Validation error starting C7 workflow: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      logger.error("Error starting C7 workflow", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start C7 workflow");
    }
  }

  @GetMapping("/{processInstanceId}/tasks")
  public ResponseEntity<List<TaskResponseC7>> getActiveTasksForProcessInstance(
        @PathVariable String processInstanceId,
        @RequestParam String taskDefinitionKey) {
    try {
      List<TaskResponseC7> tasks = workflowC7Service.getActiveTasksForProcessInstance(processInstanceId, taskDefinitionKey);
      if (tasks.isEmpty()) {
        return ResponseEntity.noContent().build(); // Or ResponseEntity.ok(Collections.emptyList());
      }
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      logger.error("Error fetching C7 tasks for process instance {}: {}", processInstanceId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not fetch C7 tasks.");
    }
  }

  @GetMapping("/tasks/assigned")
  public ResponseEntity<List<TaskResponseC7>> getActiveTasksByAssignee(
        @RequestParam String assignee,
        @RequestParam String taskDefinitionKey) {
    try {
      List<TaskResponseC7> tasks = workflowC7Service.getActiveTasksByAssignee(assignee, taskDefinitionKey);
      if (tasks.isEmpty()) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(tasks);
    } catch (Exception e) {
      logger.error("Error fetching C7 tasks for assignee {}: {}", assignee, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not fetch C7 tasks by assignee.");
    }
  }

  @PostMapping("/tasks/{taskId}/maker/complete")
  public ResponseEntity<Void> completeMakerTask(@PathVariable String taskId, @RequestBody MakerCommentRequestC7 request) {
    try {
      workflowC7Service.completeMakerTask(taskId, request);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      logger.warn("Validation error completing C7 maker task {}: {}", taskId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      logger.error("Error completing C7 maker task {}: {}", taskId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not complete C7 maker task.");
    }
  }

  @PostMapping("/tasks/{taskId}/checker/complete")
  public ResponseEntity<Void> completeCheckerTask(@PathVariable String taskId, @RequestBody CheckerDecisionRequestC7 request) {
    try {
      workflowC7Service.completeCheckerTask(taskId, request);
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e) {
      logger.warn("Validation error completing C7 checker task {}: {}", taskId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      logger.error("Error completing C7 checker task {}: {}", taskId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not complete C7 checker task.");
    }
  }
}
