package com.hp.octane.plugins.common.model.api;

import com.hp.octane.plugins.jetbrains.teamcity.model.api.ProjectConfig;

/**
 * Created by lazara on 24/12/2015.
 */
public class ProjectsList {

    public ProjectConfig[] jobs;

    public void setJobs(ProjectConfig[] jobs) {
        this.jobs = jobs;
    }

    public ProjectConfig[] getJobs() {

        return jobs;
    }

    public ProjectsList() {}
}