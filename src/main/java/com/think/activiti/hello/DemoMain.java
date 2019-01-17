package com.think.activiti.hello;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DemoMain {
    private static final Logger logger = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) throws ParseException {
        logger.debug("启动程序");
        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();
        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);
        //启动流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);
        logger.info("启动流程 {}",processInstance.getProcessDefinitionKey());
        //处理流程任务
        Scanner scanner = new Scanner(System.in);
        while(processInstance!=null&&!processInstance.isEnded()){

            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            for (Task task : list) {
                logger.info("待处理任务{}",task.getName());
                FormService formService = processEngine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                Map<String,Object> variables = Maps.newHashMap();
                for (FormProperty formProperty : formProperties) {
                    String line = null;
                    if(StringFormType.class.isInstance(formProperty.getType())){
                        logger.info("请输入{}",formProperty.getName());
                        line = scanner.nextLine();
                        variables.put(formProperty.getId(),line);
                    }else if(DateFormType.class.isInstance(formProperty.getType())){
                        logger.info("请输入{} ? 格式 （yyyy-MM-dd）",formProperty.getName());
                        line =  scanner.nextLine();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date parse = simpleDateFormat.parse(line);
                        variables.put(formProperty.getId(),parse);
                    } else{
                        logger.info("类型暂不支持{}",formProperty.getType());
                    }
                    logger.info("您输入的内容是 【{}】",line);
                }
                taskService.complete(task.getId(),variables);
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }
        }
        logger.debug("结束程序");

    }

    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        return runtimeService.startProcessInstanceById(processDefinition.getId());
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();
        String deploymentId = deploy.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        logger.info("流程定义文件{},流程id{}",processDefinition.getName(),processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        final String version = ProcessEngine.VERSION;
        logger.info(name);
        logger.info(version);
        return processEngine;
    }
}
