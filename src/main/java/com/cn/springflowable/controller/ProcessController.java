package com.cn.springflowable.controller;

import com.cn.springflowable.service.FlowableService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.rest.service.api.engine.CommentResponse;
import org.flowable.rest.service.api.history.HistoricActivityInstanceResponse;
import org.flowable.rest.service.api.history.HistoricProcessInstanceResponse;
import org.flowable.rest.service.api.runtime.process.ExecutionResponse;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.flowable.rest.service.api.runtime.task.TaskResponse;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.*;

/**
 * @author chenning
 */
@RestController
@RequestMapping("/flowable")
@RequiredArgsConstructor
public class ProcessController {
    private final FlowableService flowableService;

    /**
     * 开始流程
     * @param processKey 流程key
     * @param map 提交内容
     * @return String 流程实例Id
     */
    @PostMapping("/process/start/{processKey}")
    public String addProcess(@PathVariable String processKey, @RequestBody Map<String,Object> map){
        return flowableService.startProcess(processKey,map);
    }

    /**
     * 获取流程实例信息
     * @param instanceId 流程id
     * @return ProcessInstanceResponse
     */
    @GetMapping("/process/info/{instanceId}")
    public ProcessInstanceResponse getProcessInstance(@PathVariable String instanceId) {
        return flowableService.getProcessInstance(instanceId);
    }

    /**
     * 获取当前任务记录
     * @param instanceId 流程实例id
     * @return List
     */
    @GetMapping("/task/logs/{instanceId}")
    public List<TaskResponse> getTaskList(@PathVariable String instanceId) {
        return flowableService.getTaskList(instanceId);
    }

    /**
     * 获取当前执行列表
     * @param instanceId 流程id
     * @return List<ExecutionResponse>
     */
    @GetMapping("/execution/info/{instanceId}")
    public List<ExecutionResponse> getExecutionList(@PathVariable String instanceId) {
        return flowableService.getExecution(instanceId);
    }

    /**
     * 获取当前活动列表
     * @param instanceId 流程id
     * @return List<ActivityInstance>
     */
    @GetMapping("/activity/instance/{instanceId}")
    public List<ActivityInstance> getActivityList(@PathVariable String instanceId) {
        return flowableService.getActivity(instanceId);
    }

    /**
     * 任务处理
     * @param taskId 任务id
     * @param map 相关处理内容
     * @return String
     */
    @PostMapping("/task/deal/{taskId}")
    public String dealTask(@PathVariable String taskId, @RequestBody Map<String,Object> map) {
        flowableService.dealTask(taskId,map);
        return "操作成功";
    }

    /**
     * 直接跳到指定任务环节
     * @param instanceId 实例id
     * @param newKey 指定任务key
     * @param map 设置任务的相关参数
     * @return String
     */
    @PutMapping("/task/skip/{instanceId}/{newKey}")
    public String skipTask(@PathVariable String instanceId, @PathVariable String newKey, @RequestBody Map<String,Object> map){
        flowableService.skipTask(instanceId, newKey, map);
        return "操作成功";
    }

    /**
     * 添加流程审批意见
     * @param taskId 任务id
     * @param instanceId 实例id
     * @param type 类型
     * @param message 意见信息
     */
    @PostMapping("/process/comment")
    public void addComment(String instanceId, String taskId, String type, String message) {
        flowableService.addProcessComment(taskId, instanceId, type, message);
    }

    /**
     * 获取流程审批意见
     * @param instanceId 流程id
     * @return List<CommentResponse>
     */
    @GetMapping("/process/comment")
    public List<CommentResponse> getComments(String instanceId) {
        return flowableService.getProcessComments(instanceId);
    }

    /**
     * 查询用户发起的流程历史记录
     * @param userId 用户id
     * @return List
     */
    @GetMapping("/process/history/{userId}")
    public List<HistoricProcessInstanceResponse> getProcessInstanceHistory(@PathVariable String userId) {
        return flowableService.getProcessInstanceHistory(userId);
    }

    /**
     * 查看历史活动记录
     * @param instanceId 流程实例id
     * @return List
     */
    @GetMapping("/activity/history/{instanceId}")
    public List<HistoricActivityInstanceResponse> getActivityHistory(@PathVariable String instanceId) {
        return flowableService.getActivityHistory(instanceId);
    }

    /**
     * 查看历史任务记录
     * @param instanceId 流程实例id
     * @return List
     */
    @GetMapping("/task/history/{instanceId}")
    public List<HistoricTaskInstance> getTaskHistory(@PathVariable String instanceId){
        return flowableService.getTaskHistory(instanceId);
    }

    /**
     * 查询流程任务进程图
     * @param instanceId 流程实例id
     * @return StreamingResponseBody
     */
    @GetMapping("/process/diagram/{instanceId}")
    public StreamingResponseBody getProcessDiagram(@PathVariable String instanceId) throws IOException {
        return flowableService.getProcessDiagram(instanceId);
    }

    /**
     * 查询流程图
     * @param definitionKey 模版key
     * @return StreamingResponseBody
     * @throws IOException IO异常
     */
    @GetMapping("/diagram/{definitionKey}")
    public StreamingResponseBody getDiagram(@PathVariable String definitionKey) throws IOException {
        return flowableService.getResourceDiagram(definitionKey);
    }

    /**
     * 查询流程定义信息
     * @param definitionId 流程定义id
     * @param definitionKey 流程定义key
     * @param definitionName 流程定义名称
     * @return List<ProcessDefinition>
     */
    @GetMapping("/process/definition")
    public List<ProcessDefinition> getProcessDefinitions(String definitionId, String definitionKey, String definitionName) {
        return flowableService.getProcessDefinitionList(definitionId, definitionKey, definitionName);
    }

    /**
     * 删除流程定义
     * @param definitionId 流程定义id
     */
    @DeleteMapping("/process/definition/{definitionId}")
    public void deleteProcessDefinitions(@PathVariable String definitionId) {
        flowableService.deleteProcessDefinition(definitionId);
    }

    /**
     * 获取流程定义xml信息
     * @param definitionId 流程定义id
     * @return String
     * @throws IOException IO异常
     */
    @GetMapping("/process/definition/xml")
    public String getProcessDefinitions(String definitionId) throws IOException {
        return flowableService.getXmlResource(definitionId);
    }

    /**
     * 停止流程
     * @param instanceId 流程id
     */
    @PostMapping("/process/stop")
    public void stopProcess(String instanceId) {
        flowableService.stopProcess(instanceId);
    }
}
