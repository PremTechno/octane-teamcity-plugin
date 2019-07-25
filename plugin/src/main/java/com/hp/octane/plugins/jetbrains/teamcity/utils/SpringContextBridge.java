package com.hp.octane.plugins.jetbrains.teamcity.utils;

import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationHolder;
import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import jetbrains.buildServer.serverSide.BuildServerEx;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.parameters.ParameterFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringContextBridge implements SpringContextBridgedServices, ApplicationContextAware {
	private static ApplicationContext context;
	@Autowired
	ProjectManager projectManager;
	@Autowired
	BuildServerEx sBuildServer;
	@Autowired
	ModelCommonFactory modelCommonFactory;
	@Autowired
	SnapshotsFactory snapshotsFactory;
	@Autowired
	private PluginDescriptor pluginDescriptor;
	@Autowired
	private TCConfigurationHolder holder;
	@Autowired
	private ParameterFactory parameterFactory;

	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	@Override
	public ProjectManager getProjectManager() {
		return projectManager;
	}

	@Override
	public BuildServerEx getSBuildServer() {
		return sBuildServer;
	}

	@Override
	public ModelCommonFactory getModelCommonFactory() {
		return modelCommonFactory;
	}

	@Override
	public SnapshotsFactory getSnapshotsFactory() {
		return snapshotsFactory;
	}

	@Override
	public PluginDescriptor getPluginDescriptor() {
		return pluginDescriptor;
	}

	@Override
	public TCConfigurationHolder getTCConfigurationHolder() {
		return holder;
	}

	@Override
	public ParameterFactory getParameterFactory() {
		return parameterFactory;
	}

	public static SpringContextBridgedServices services() {
		return context.getBean(SpringContextBridgedServices.class);
	}
}