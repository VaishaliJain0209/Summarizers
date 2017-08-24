package com.sapient.hack2.ruleengine.main;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sapient.hack2.ruleengine.service.ApplicationService;

public class RuleEngineProcessor {

	public static void main(String[] args) {
		
		ApplicationContext context =
		    	   new ClassPathXmlApplicationContext(new String[] {"application-context.xml"});

		ApplicationService appService = (ApplicationService)context.getBean("applicationService");
		appService.createRuleJson();
		appService.processRules();   	
				
	}	
	
}
