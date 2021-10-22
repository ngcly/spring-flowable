# Springboot与Flowable的整合

## Flowable 流程图绘画工具  
到官网去下载 flowable-ui 项目，下载地址为：https://flowable.com/open-source/downloads  
运行起来后就可以进行界面化的流程图绘画了。  
flowable-ui 默认相关信息如下：  
端口: 8080  
账号: admin  
密码: test  
绘画完成后将相应的xml文件下载下来放到本项目的 resources/processes 目录下即可

## Flowable 相关使用方法  
1. 部署流程定义（BPM文件），获取流程定义文件对象
```
// resouce：BPMN文件路径，inputStream：该文件的字节流  
DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().addInputStream(resource, inputStream);

/**
 * 根据参数设置流程部署构建器
 * parameter ：部署参数，一个
 * Map<String, Object> deploymentBuilder.category(parameter.get("flowType"))
 *.name(parameter.get("flowName"))
 * .key(parameter.get("flowKey")).tenantId(parameter.get("flowTenantId"));
 */
// 并获取流程定义部署对象 
Deployment deployment = deploymentBuilder.deploy();
String deploymentId = deployment.getId();
ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
// 流程定义ID
String processDefinitionId = processDefinition.getId();
// 流程定义Key
String processDefinitionKey = processDefinition.getKey();

```
2. 挂载与恢复流程定义，挂起后发起流程实例就会抛出异常  
```
// 挂起
repositoryService.suspendProcessDefinitionById(processDefinitionId);
repositoryService.suspendProcessDefinitionByKey(processDefinitionKey);
// 恢复
repositoryService.activateProcessDefinitionById(processDefinitionId);
repositoryService.activateProcessDefinitionByKey(processDefinitionKey);

```
3. 启动流程实例，并获取流程实例对象  
```
// variables：流程变量，Map<String, Object>类型
ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinitionId, variables);
ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, variables);
// 将流程实例与发起人绑定
identityService.setAuthenticatedUserId(userId);

```
4. 对流程实例的操作  
### processInstance  
```
// 流程实例ID
String processInstanceId = processInstance.getId();
// 判断流程是否结束
processInstance.isEnded();
// 判断流程是否挂起
processInstance.isSuspended();
// 获取流程的发起人ID
String startUserId = processInstance.getStartUserId();
```
### runtimeService  
```
// 该流程的执行对象查询
List<Execution> executionList = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).singleResult();
// 该流程实例下的所有活动实例
List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInstanceId).list();
// 更改多个活动实例的状态节点为指定节点 activityId ，比如结束节点终止流程    
runtimeService.createChangeActivityStateBuilder().moveExecutionsToSingleActivityId(executionIds, activityId).changeState();
// 挂起流程实例
runtimeService.suspendProcessInstanceById(processInstanceId);
// 恢复挂起的流程实例
runtimeService.activateProcessInstanceById(processInstanceId);
// 删除一个正在流转的流程 deleteReason：删除原因
HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId);
// 获取该流程实例下的任务数量
long count = query.count();
// 任务数量大于1，则流程已经启动了，不能撤回
if (count > 1) {
    throw new FlowException(ResultEnum.EX_INSTANCE_BEGIN);
}
runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
// 获取流程实例的查询对象
ProcessInstanceQuery instanceQuery = runtimeService.createProcessInstanceQuery();
// 与某个用户相关的
instanceQuery.involvedUser(userId);
// 某个用户开启的
instanceQuery.startedBy(userId);
// 或者查询条件 .or().endOr() ==> (xx or  xx or ... ... or xx)，等于包裹内容的两括号
instanceQuery.or().endOr();
// 挂起的流程
instanceQuery.suspended();
// 在某个时间点之后开始
instanceQuery.startedAfter(Date date);
// 在某个时间点之前开始
instanceQuery.startedBefore(Date date);
// 获取正在流转的一个指定的流程实例
instanceQuery.processInstanceId(processInstanceId);
// 单个的流程实例
ProcessInstance processInstance = instanceQuery.singleResult();
// 多个流程实例 begin : 从第几个开始 ； max : 展示多少个
List<ProcessInstance> processInstances = instanceQuery.list();
List<ProcessInstance> processInstances = instanceQuery.listPage(int begin,int max);
// 流程实例的数量
long count = taskQuery.count();

```  
### historyService  
```
// 获取历史流程实例查询对象
HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
// 已完成的
historicProcessInstanceQuery.finished();
// 未完成的
historicProcessInstanceQuery.unfinished();
// 删除的
historicProcessInstanceQuery.deleted();
// 没有删除的
historicProcessInstanceQuery.notDeleted();
// 在某个时间点之后结束
historicProcessInstanceQuery.finishedAfter(Date date);
// 在某个时间点之前结束
historicProcessInstanceQuery.finishedBefore(Date date);
// 指定父流程ID的流程实例 historicProcessInstanceQuery.superProcessInstanceId(processInstanceId)
// 历史流程实例
HistoricProcessInstance processInstance = historicProcessInstanceQuery.processInstanceId(processInstanceId).singleResult();
// 删除该流程的历史记录
historyService.deleteHistoricProcessInstance(processInstanceId);

```
5. 任务服务的操作  
### 获取task任务对象  
```
// 任务基础查询对象
TaskQuery taskQuery = taskService.createTaskQuery();
// 某个任务
taskQuery.taskId(taskId);
// 某个经办人的任务
taskQuery.taskAssignee(userId);
// 某个委托人的任务
taskQuery.taskOwner(userId);
// 某个或多个流程实例的任务
taskQuery.processInstanceId(String processInstanceId);
taskQuery.processInstanceIdIn(List<String> processInstanceIds);
// 某个或多个部署实例的任务
taskQuery.deploymentId(String deploymentId);
taskQuery.deploymentIdIn(List<String> deploymentIds);
// 某个活动实例的任务
taskQuery.executionId(String executionId);
// 按照任务创建时间倒序
taskQuery.orderByTaskCreateTime().desc();
// 存活的任务
taskQuery.active();
// 挂起的任务
taskQuery.suspended();
// 没有 删除原因 的任务
taskQuery.taskWithoutDeleteReason();
// 没有签收的任务
taskQuery.taskUnassigned();
// 单个的任务对象
Task task = taskQuery.singleResult();
// 多个任务对象 begin : 从第几个开始 ； max : 展示多少个
List<Task> tasks = taskQuery.list();
List<Task> tasks = taskQuery.listPage(int begin,int max);
// 任务的数量
long count = taskQuery.count();
```  
### 变量的设值与取值  
```
// 任务ID
String taskId = task.getId();
// 设置全局变量
taskService.setVariable(taskId,"key1","value1");
// 设置局部变量
taskService.setVariableLocal(taskId,"key2","value2");
// 获取全局变量
Map<String,Object> a = taskService.getVariables(taskId);
// 获取局部变量
Map<String,Object> b = taskService.getVariablesLocal(taskId);
// 流程启动后获取变量（全局变量）
Map<String,Object> variables = runtimeService.getVariables(processInstanceId);
// 设置变量（全局变量）
runtimeService.setVariable(processInstanceId,"key","value");
```  
### 任务的流转  
```
// 任务的执行（委托人）
taskService.resolveTask(taskId);
taskService.complete(taskId);
// 任务的执行（经办人） variables : 下次任务所需要的参数 localScope : 变量的存储范围(true:作用范围为当前任务,false:表示这个变量是全局的)
taskService.complete(taskId);
taskService.complete(String taskId, Map<String, Object> variables);
taskService.complete(String taskId, Map<String, Object> variables, boolean localScope);
// 添加和删除候选人
taskService.addCandidateUser(taskId, userId);
taskService.deleteCandidateUser(taskId, userId);
// 签收
taskService.claim(taskId, userId);
// 委派
taskService.delegateTask(taskId, acceptUserId);
// 转发
taskService.setAssignee(taskId, acceptUserId);
// 驳回 currTaskKeys : 该任务的节点 ； activityId : 上一个节点ID
List<String> currTaskKeys = new ArrayList<>();
List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
for (Task task : tasks) {
    currTaskKeys.add(task.getTaskDefinitionKey());
}
runtimeService.createChangeActivityStateBuilder()
    .processInstanceId(processInstanceId)
    .moveActivityIdsToSingleActivityId(currTaskKeys, activityId)
    .changeState();
// 删除任务
taskService.deleteTask(taskId, deleteReason);
taskService.deleteTasks(List<String> taskIds, deleteReason);

```  
### 绘制流程图   
```
ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
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
    activeActivityIds = new ArrayList<>();
    historicEnds.forEach(historicActivityInstance -> activeActivityIds.add(historicActivityInstance.getActivityId()));
}
List<String> highLightedFlows = new ArrayList<>();
// 3.获取所有的历史轨迹线对象
List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(instanceId).activityType(BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW).list();

historicActivityInstances.forEach(historicActivityInstance -> highLightedFlows.add(historicActivityInstance.getActivityId()));

BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
ProcessEngineConfiguration processEngConfig = processEngine.getProcessEngineConfiguration();
ProcessDiagramGenerator diagramGenerator = processEngConfig.getProcessDiagramGenerator();
InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activeActivityIds, highLightedFlows, processEngConfig.getActivityFontName(), processEngConfig.getLabelFontName(), processEngConfig.getAnnotationFontName(), processEngConfig.getClassLoader(), 1.0, true);
```