package com.cn.springflowable.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * @author chenning
 */
public class CallExternalSystemDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("All variable: "+execution.getVariables());
        System.out.println("Calling the external system for employee "
                + execution.getVariable("employee"));
    }
}
