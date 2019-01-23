package com.hp.octane.plugins.jetbrains.teamcity.utils;

import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
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
    SBuildServer sBuildServer;
    @Autowired
    ModelCommonFactory modelCommonFactory;
    @Autowired
    SnapshotsFactory snapshotsFactory;
    @Autowired
    private PluginDescriptor pluginDescriptor;

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public ProjectManager getProjectManager() {
        return projectManager;
    }

    @Override
    public SBuildServer getSBuildServer() {
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

	public static SpringContextBridgedServices services() {
        return context.getBean(SpringContextBridgedServices.class);
    }
}