package com.cn.springflowable;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author chenning
 */
@Slf4j
@SpringBootApplication
public class SpringFlowableApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringFlowableApplication.class, args);
    }


    @Bean
    public CommandLineRunner init(final RepositoryService repositoryService,
                                  final RuntimeService runtimeService,
                                  final TaskService taskService) {
        return strings -> {
            log.info("Number of process definitions : {}", repositoryService.createProcessDefinitionQuery().count());
            log.info("Number of deployment: {}", repositoryService.createDeploymentQuery().count());
            log.info("Number of process instance: {}", runtimeService.createProcessInstanceQuery().count());
            log.info("Number of tasks after process start: {}", taskService.createTaskQuery().count());
        };
    }

}
