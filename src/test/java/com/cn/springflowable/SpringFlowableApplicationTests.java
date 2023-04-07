package com.cn.springflowable;

import com.cn.springflowable.service.FlowableService;
import net.bytebuddy.utility.RandomString;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.rest.service.api.runtime.task.TaskResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 集成测试
 */
@SpringBootTest
class SpringFlowableApplicationTests {
    private String instanceId;

    @Autowired
    private FlowableService flowableService;


    @Test
    void contextLoads() {
    }

    @BeforeEach
    public void startProcess() {
        String processId = "holiday-request";
        Map<String, Object> variables = Map.of("employee", "test_" + new RandomString(4).nextString(),
                "nrOfHolidays", new Random().nextInt(10), "description", "take holiday");
        instanceId = flowableService.startProcess(processId, variables);
    }

    @Test
    void startProcessTest() {
        Assertions.assertNotNull(instanceId);
        System.out.println(instanceId);
    }

    @Test
    void getTaskTest() {
        List<TaskResponse> tasks = flowableService.getTaskList(instanceId);
        Assertions.assertNotNull(tasks);
        tasks.forEach(task -> System.out.println(task.getId() + ": " + task.getName()));
    }

    @Test
    void historyTest() {
        List<HistoricActivityInstance> activities = flowableService.getActivityHistory(instanceId);
        Assertions.assertNotNull(activities);
        activities.forEach(activity -> System.out.println(activity.getActivityId() + " took "
                + activity.getDurationInMillis() + " milliseconds"));
    }

    @Test
    void taskApprovedTest() {
        TaskResponse task = flowableService.getTask(instanceId);
        flowableService.dealTask(task.getId(), Map.of("approved", true));
        TaskResponse approvedTask = flowableService.getTask(instanceId);
        Assertions.assertEquals("Holiday approved", approvedTask.getName());
    }

    @Test
    void taskRejectTest() {
        TaskResponse task = flowableService.getTask(instanceId);
        flowableService.dealTask(task.getId(), Map.of("approved", false));
        TaskResponse approvedTask = flowableService.getTask(instanceId);
        Assertions.assertNull(approvedTask);
    }

    @Test
    void skipTask() {
        String newKey = "holidayApprovedTask";
        Boolean flag = flowableService.skipTask(instanceId, newKey, Map.of("approved", true));
        Assertions.assertTrue(flag);
    }

}
