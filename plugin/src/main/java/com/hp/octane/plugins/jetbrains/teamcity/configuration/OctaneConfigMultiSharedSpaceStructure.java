package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "configs")
@XmlAccessorType(XmlAccessType.FIELD)
public class OctaneConfigMultiSharedSpaceStructure {

    @XmlElement(name = "octane-config")
    private List<OctaneConfigStructure> multiConfigStructure = new ArrayList<>();


    public List<OctaneConfigStructure> getMultiConfigStructure() {
        return multiConfigStructure;
    }

    public void setMultiConfigStructure(List<OctaneConfigStructure> multiConfigStructure) {
        this.multiConfigStructure = multiConfigStructure;
    }
}
