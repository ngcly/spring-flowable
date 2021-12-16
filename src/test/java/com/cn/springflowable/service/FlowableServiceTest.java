package com.cn.springflowable.service;

import net.bytebuddy.utility.RandomString;
import org.flowable.engine.*;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.FlowableTest;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 单元测试
 * @author ngcly
 */
@FlowableTest
@ConfigurationResource
public class FlowableServiceTest {
    private static ProcessEngine processEngine;
    private static RuntimeService runtimeService;
    private static TaskService taskService;
    private static HistoryService historyService;

    @BeforeAll
    static void setUp(ProcessEngine processEngine) {
        processEngine = processEngine;
        runtimeService = processEngine.getRuntimeService();
        taskService = processEngine.getTaskService();
        historyService = processEngine.getHistoryService();

        processEngine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("./processes/holiday-request.bpmn20.xml")
                .deploy();
    }

    @AfterAll
    static void tearDown() {

    }

    @Test
    void startProcesses(){
        Map<String,Object> variables = Map.of("employee","test_"+ new RandomString(4).nextString(),
                "nrOfHolidays", new Random().nextInt(10),"description","take holiday");
        runtimeService.startProcessInstanceByKey("holiday-request",variables);
    }

    @Test
    void testHolidayProcess() {
        Task task = taskService.createTaskQuery().singleResult();
        System.out.println("task info => "+ task.getId()+": "+task.getName());

        taskService.complete(task.getId(), Map.of("approved",false));
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());
    }

    @Test
    void getTaskTest(){
        var list = historyService.createHistoricTaskInstanceQuery().list();
        list.forEach(HistoricTaskInstance::getDeleteReason);
        List<Task> tasks = taskService.createTaskQuery().list();
        Assertions.assertNotNull(tasks);
        tasks.forEach(task -> System.out.println(task.getId()+": "+task.getName()));
    }

}
