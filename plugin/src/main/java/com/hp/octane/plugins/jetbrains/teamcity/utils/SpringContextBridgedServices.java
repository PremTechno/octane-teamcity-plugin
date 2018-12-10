package com.hp.octane.plugins.jetbrains.teamcity.utils;

import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;

public interface SpringContextBridgedServices {

    public ProjectManager getProjectManager();
    public SBuildServer getSBuildServer();
    public ModelCommonFactory getModelCommonFactory();
    public SnapshotsFactory getSnapshotsFactory();

}
