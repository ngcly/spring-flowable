package com.cn.springflowable.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author chenning
 */
@Slf4j
public class CallExternalSystemDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        log.info("All variable: {}", execution.getVariables());
        log.info("Calling the external system for employee {}", execution.getVariable("employee"));
    }
}
