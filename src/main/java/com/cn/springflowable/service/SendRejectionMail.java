package com.cn.springflowable.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author chenning
 */
public class SendRejectionMail implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("Send rejection mail to employee ");
        System.out.println("All variable: "+execution.getVariables());
    }
}
