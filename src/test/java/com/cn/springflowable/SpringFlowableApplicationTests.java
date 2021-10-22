package com.cn.springflowable;

import com.cn.springflowable.service.FlowableService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@SpringBootTest
class SpringFlowableApplicationTests {

    @Resource
    private FlowableService flowableService;


    @Test
    void contextLoads() {
    }

    @Test
    public void startProcessTest(){
        String processId = "holiday-request";
        Map<String,Object> variables = Map.of("employee","jack","nrOfHolidays",4,"description","take holiday");
        String instanceId = flowableService.startProcess(processId,variables);
        Assertions.assertNotNull(instanceId);
        System.out.println(instanceId);
    }

    @Test
    public void getTaskTest(){
        String instanceId = "cf6310ef-2d8c-11ec-a762-acde48001122";
        List<Task> tasks = flowableService.getTaskList(instanceId);
        Assertions.assertNotNull(tasks);
        tasks.forEach(task -> System.out.println(task.getId()+": "+task.getName()));
    }

    @Test
    public void historyTest(){
        String processInstanceId = "93ea2e93-2cba-11ec-8e4e-acde48001122";
        List<HistoricActivityInstance> activities = flowableService.getActivityHistory(processInstanceId);
        Assertions.assertNotNull(activities);
        activities.forEach(activity -> System.out.println(activity.getActivityId()+ " took "
                +activity.getDurationInMillis() +" milliseconds"));
    }

    @Test
    public void auditTest(){
        String taskId = "93ecc6ab-2cba-11ec-8e4e-acde48001122";
        flowableService.dealTask(taskId,Map.of("approved",true));
    }

    @Test
    public void skipTask(){
        String instanceId = "cf6310ef-2d8c-11ec-a762-acde48001122";
        String newKey = "holidayApprovedTask";
        Boolean flag = flowableService.skipTask(instanceId,newKey,Map.of("approved",true));
        Assertions.assertTrue(flag);
    }

}