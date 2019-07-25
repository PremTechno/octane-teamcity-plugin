package com.hp.octane.plugins.jetbrains.teamcity.utils;

import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationHolder;
import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import jetbrains.buildServer.serverSide.BuildServerEx;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.parameters.ParameterFactory;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public interface SpringContextBridgedServices {

	ProjectManager getProjectManager();

	BuildServerEx getSBuildServer();

	ModelCommonFactory getModelCommonFactory();

	SnapshotsFactory getSnapshotsFactory();

	PluginDescriptor getPluginDescriptor();

	TCConfigurationHolder getTCConfigurationHolder();

	ParameterFactory getParameterFactory();

}
