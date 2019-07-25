package com.hp.octane.plugins.jetbrains.teamcity.testrunner;

import com.hp.octane.integrations.executor.TestsToRunConverterResult;
import com.hp.octane.integrations.executor.TestsToRunConvertersFactory;
import com.hp.octane.integrations.executor.TestsToRunFramework;

public class TeamCityTestsToRunConverterBuilder {
	public final static String TESTS_TO_RUN_PARAMETER = "testsToRun";
	public final static String TESTING_FRAMEWORK_PARAMETER = "Testing_framework";

	TestsToRunFramework testsToRunFramework;

	public TeamCityTestsToRunConverterBuilder(String testsToRunFramework){
		this.testsToRunFramework = TestsToRunFramework.fromValue(testsToRunFramework);
	}
	public TeamCityTestsToRunConverterBuilder(){
		this.testsToRunFramework = TestsToRunFramework.fromValue("mvnSurefire");
	}

	public TestsToRunConverterResult convert(String rawTests, String executingDirectory){
		return TestsToRunConvertersFactory.createConverter(testsToRunFramework).convert(rawTests, executingDirectory);
	}
}
