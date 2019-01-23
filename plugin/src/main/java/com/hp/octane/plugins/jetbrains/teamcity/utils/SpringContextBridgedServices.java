package com.hp.octane.plugins.jetbrains.teamcity.utils;

import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public interface SpringContextBridgedServices {

	ProjectManager getProjectManager();

	SBuildServer getSBuildServer();

	ModelCommonFactory getModelCommonFactory();

	SnapshotsFactory getSnapshotsFactory();

	PluginDescriptor getPluginDescriptor();

}
