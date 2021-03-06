package com.cn.springflowable.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.runtime.process.ExecutionResponse;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.flowable.rest.service.api.runtime.task.TaskResponse;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chenning
 */
@Service
@AllArgsConstructor
public class FlowableService {
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ProcessEngine processEngine;
    private final IdentityService identityService;

    /**
     * 流程启动开始
     * @param processKey 流程定义key
     * @param map 启动相关参数
     * @return String
     */
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(String processKey, Map<String, Object> map) {
        //设置启动用户id
        identityService.setAuthenticatedUserId("cly");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, map);
        return processInstance.getId();
    }

    public ProcessInstanceResponse getProcessInstance(String instanceId) {
        RestResponseFactory restResponse = new RestResponseFactory(new ObjectMapper());
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId)
                .singleResult();
        return restResponse.createProcessInstanceResponse(processInstance);
    }

    public List<TaskResponse> getTaskList(String instanceId) {
        //此处直接这样返回 在controller层 会因为懒加载的问题导致格式化为json而报错 所以采用官方rest包装
        List<Task> list = taskService.createTaskQuery()
                .processInstanceId(instanceId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        RestResponseFactory restResponse = new RestResponseFactory(new ObjectMapper());
        return restResponse.createTaskResponseList(list);
    }

    public TaskResponse getTask(String instanceId) {
        Task task = taskService.createTaskQuery().processInstanceId(instanceId).singleResult();
        if(task == null) {
            return null;
        }
        RestResponseFactory restResponse = new RestResponseFactory(new ObjectMapper());
        return restResponse.createTaskResponse(task);
    }

    public List<ExecutionResponse> getExecution(String instanceId) {
        List<Execution> list = runtimeService.createExecutionQuery().processInstanceId(instanceId).list();
        RestResponseFactory restResponse = new RestResponseFactory(new ObjectMapper());
        return restResponse.createExecutionResponseList(list);
    }

    public List<ActivityInstance> getActivity(String instanceId) {
        return runtimeService.createActivityInstanceQuery()
                .processInstanceId(instanceId)
                .orderByActivityInstanceEndTime()
                .desc()
                .list();
    }

    /**
     * 处理任务
     * @param taskId 任务id
     * @param map 相关处理参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void dealTask(String taskId, Map<String, Object> map) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        //添加审批意见 this.addProcessComment();
        if (Objects.nonNull(task)) {
            if (Objects.isNull(map) || map.isEmpty()) {
                taskService.complete(taskId);
            } else {
                taskService.complete(taskId, map);
            }
        }
    }

    /**
     * 跳到指定任务（主要用于测试）
     * @param instanceId 实例id
     * @param newKey 指定任务的key
     * @param map 相关处理参数
     * @return Boolean
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean skipTask(String instanceId, String newKey, Map<String,Object> map){
        Task task = taskService.createTaskQuery()
                .processInstanceId(instanceId)
                .orderByTaskCreateTime()
                .desc()
                .singleResult();

        if(Objects.isNull(task)) {
            return false;
        }

        if(Objects.isNull(map) || map.isEmpty()){
            taskService.setVariables(task.getId(), map);
        }

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(instanceId)
                .moveActivityIdTo(task.getTaskDefinitionKey(),newKey)
                .changeState();
        return true;
    }

    /**
     * 查询用户的流程实例历史
     * @param userId 用户id
     * @return List
     */
    public List<HistoricProcessInstance> getProcessInstanceHistory(String userId) {
        return historyService.createHistoricProcessInstanceQuery()
                .startedBy(userId)
                .orderByProcessInstanceStartTime()
                .asc()
                .list();
    }

    /**
     * 查询活动历史
     * @param instanceId 实例id
     * @return List
     */
    public List<HistoricActivityInstance> getActivityHistory(String instanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceId)
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .asc()
                .list();
    }

    /**
     * 查询任务历史
     * @param instanceId 实例id
     * @return List
     */
    public List<HistoricTaskInstance> getTaskHistory(String instanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(instanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
    }

    /**
     * 获取流程进程图
     * @param instanceId 实例id
     * @return StreamingResponseBody
     */
    @Transactional(rollbackFor = Exception.class)
    public StreamingResponseBody getProcessDiagram(String instanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId)
                .singleResult();

        String processDefinitionId;
        List<String> activeActivityIds;

        // 2. 获取流程定义id和高亮的节点id
        if (Objects.nonNull(processInstance)) {
            // 2.1 正在运行的流程实例
            processDefinitionId = processInstance.getProcessDefinitionId();
            activeActivityIds = runtimeService.getActiveActivityIds(instanceId);
        } else {
            // 2.2 已经结束的流程实例
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(instanceId).singleResult();
            processDefinitionId = historicProcessInstance.getProcessDefinitionId();

            // 2.3 获取结束节点列表
            List<HistoricActivityInstance> historicEnds = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(instanceId).activityType(BpmnXMLConstants.ELEMENT_EVENT_END).list();

            activeActivityIds = historicEnds.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .collect(Collectors.toList());
        }

        // 3.获取所有的历史轨迹线对象
        List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(instanceId).activityType(BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW).list();

        List<String> highLightedFlows = historicActivityInstances.stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        ProcessEngineConfiguration processEngConfig = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = processEngConfig.getProcessDiagramGenerator();

        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activeActivityIds,
                highLightedFlows, processEngConfig.getActivityFontName(), processEngConfig.getLabelFontName(),
                processEngConfig.getAnnotationFontName(), processEngConfig.getClassLoader(),
                1.0, true);

        return outputStream -> outputStream.write(in.readAllBytes());
    }


    /**
     * 根据条件查询流程定义
     * @param id 流程定义id
     * @param key 流程定义key
     * @param name 流程定义名称
     * @return
     */
    public List<ProcessDefinition> getProcessDefinitionList(String id, String key, String name){
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id)
                .processDefinitionKeyLike(key)
                .processDefinitionNameLike(name)
                .listPage(0,10);
    }

    /**
     * 获取流程定义xml
     * @param id 流程定义id
     * @return String
     * @throws Exception
     */
    public String getXmlResource(String id) throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(id).singleResult();
        InputStream inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
                processDefinition.getResourceName());
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }finally {
            IOUtils.close(inputStream,null);
        }
    }

    /**
     * 获取流程定义图片
     * @param definitionKey 流程定义key
     * @return StreamingResponseBody
     */
    public StreamingResponseBody getResourceDiagram(String definitionKey){
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(definitionKey)
                .list();

        if(CollectionUtils.isEmpty(processDefinitions)) {
            return null;
        }

        ProcessDefinition processDefinition = processDefinitions.get(1);

        InputStream inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(),
                processDefinition.getDiagramResourceName());

        return outputStream -> outputStream.write(inputStream.readAllBytes());
    }

    /**
     * 删除流程定义
     * @param definitionId 流程定义id
     */
    public void deleteProcessDefinition(String definitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId)
                .singleResult();
        // 删除给定的部署和级联删除流程实例、历史流程实例和作业。
        repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
    }

    /**
     * 添加流程审批意见
     * @param taskId 任务id
     * @param instanceId 实例id
     * @param type 类型
     * @param message 意见信息
     */
    public Comment addProcessComment(String taskId, String instanceId, String type, String message){
        return taskService.addComment(taskId, instanceId,type, message);
    }

    /**
     * 获取流程审批意见
     * @param instanceId 实例id
     * @return List
     */
    public List<Comment> getProcessComments(String instanceId){
        return taskService.getProcessInstanceComments(instanceId);
    }

    /**
     * 终止流程实例
     * @param instanceId 实例id
     */
    public void stopProcess(String instanceId){
        // 修改流转执行状态
        runtimeService.setVariable(instanceId, "", "");

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult();

        //添加一条审批记录 this.addProcessComment();

        /// 执行终止
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(instanceId).list();
        List<String> executionIds = executions.stream().map(v -> v.getId()).collect(Collectors.toList());
        // 获取流程结束点
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        List<EndEvent> endNodes = bpmnModel.getMainProcess().findFlowElementsOfType(EndEvent.class);
        String endId = endNodes.get(endNodes.size() - 1).getId();
        // 执行跳转
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, endId)
                .changeState();
    }

}
