package com.cn.springflowable.service;

import net.bytebuddy.utility.RandomString;
import org.flowable.engine.*;
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
public class FlowableServiceTest {
    private static String processInstanceId;

    @BeforeAll
    static void setUp(ProcessEngine processEngine) {

    }

    @AfterAll
    static void tearDown(ProcessEngine processEngine) {

    }

    @Test
    void startProcesses(RuntimeService runtimeService){
        Map<String,Object> variables = Map.of("employee","test_"+ new RandomString(4).nextString(),
                "nrOfHolidays", new Random().nextInt(10),"description","take holiday");
        processInstanceId = runtimeService.startProcessInstanceByKey("holiday-request",variables).getProcessInstanceId();
    }

    @Test
    void testHolidayProcess(RuntimeService runtimeService,TaskService taskService) {
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        System.out.println("task info => "+ task.getId()+": "+task.getName());

        taskService.complete(task.getId(), Map.of("approved",false));
    }

    @Test
    void getTaskTest(HistoryService historyService, TaskService taskService){
        var list = historyService.createHistoricTaskInstanceQuery().list();
        list.forEach(HistoricTaskInstance::getDeleteReason);
        List<Task> tasks = taskService.createTaskQuery().list();
        Assertions.assertNotNull(tasks);
        tasks.forEach(task -> System.out.println(task.getId()+": "+task.getName()));
    }

}
