package com.cn.springflowable.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author chenning
 */
@Slf4j
public class SendRejectionMail implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        log.info("Send rejection mail to employee ");
        log.info("All variable: {}", execution.getVariables());
    }
}
