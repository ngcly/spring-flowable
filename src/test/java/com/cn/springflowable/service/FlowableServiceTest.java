package com.cn.springflowable.service;

import net.bytebuddy.utility.RandomString;
import org.flowable.engine.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 单元测试
 * @author ngcly
 */
@FlowableTest
public class FlowableServiceTest {
    private String processInstanceId;

    @BeforeAll
    static void setUp(ProcessEngine processEngine) {
        /** 如果在此处采用下面的这种方式部署方式
         *         processEngine.getRepositoryService()
         *                 .createDeployment()
         *                 .addClasspathResource("./processes/holiday-request.bpmn20.xml")
         *                 .deploy();
         *
         * 那么 下面每个方法上面的
         *  @Deployment(resources = { "processes/holiday-request.bpmn20.xml" }) 注解就可以省略掉
         */

    }

    @AfterAll
    static void tearDown(ProcessEngine processEngine) {

    }

    @BeforeEach
    void startProcesses(RuntimeService runtimeService) {
        Map<String,Object> variables = Map.of("employee","test_"+ new RandomString(4).nextString(),
                "nrOfHolidays", new Random().nextInt(10),"description","take holiday");
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("holiday-request",variables);
        assertEquals("holiday-request", instance.getProcessDefinitionKey());
        processInstanceId = instance.getId();
    }

    @Test
    @Deployment(resources = { "processes/holiday-request.bpmn20.xml" })
    void test_startProcesses_success(RuntimeService runtimeService){
        assertNotNull(processInstanceId);
    }

    @Test
    @Deployment(resources = { "processes/holiday-request.bpmn20.xml" })
    void test_completeTask(RuntimeService runtimeService,TaskService taskService) {
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        System.out.println("task info => "+ task.getId()+": "+task.getName());
        Assertions.assertDoesNotThrow(() -> taskService.complete(task.getId(), Map.of("approved",false)));
    }

    @Test
    @Deployment(resources = { "processes/holiday-request.bpmn20.xml" })
    void test_getTask(HistoryService historyService, TaskService taskService){
        var list = historyService.createHistoricTaskInstanceQuery().list();
        list.forEach(HistoricTaskInstance::getDeleteReason);
        List<Task> tasks = taskService.createTaskQuery().list();
        Assertions.assertNotNull(tasks);
        tasks.forEach(task -> System.out.println(task.getId()+": "+task.getName()));
    }

}
