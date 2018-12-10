package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import com.hp.octane.integrations.OctaneConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCConfigurationHolder {
    private List<OctaneConfigStructure> configs = new ArrayList<>();
    private transient Map<String, OctaneConfiguration> octaneConfigurations = new HashMap<>();


    public List<OctaneConfigStructure> getConfigs() {
        return configs;
    }

    public void setConfigs(List<OctaneConfigStructure> configs) {
        this.configs = configs;
    }

    public Map<String, OctaneConfiguration> getOctaneConfigurations() {
        return octaneConfigurations;
    }
}
