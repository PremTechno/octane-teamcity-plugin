/*
 *     2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.plugins.jetbrains.teamcity.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.causes.CIEventCause;
import com.hp.octane.integrations.dto.causes.CIEventCauseType;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.PhaseType;
import com.hp.octane.plugins.jetbrains.teamcity.OctaneTeamCityPlugin;
import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.ParametersFactory;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gullery on 13/03/2016.
 * Team City Events listener for the need of publishing CI Server events to NGA server
 */

public class ProgressEventsListener extends BuildServerAdapter {
    private static final Logger logger = LogManager.getLogger(ProgressEventsListener.class);
    private static final DTOFactory dtoFactory = DTOFactory.getInstance();
    private static final String TRIGGER_BUILD_TYPE_KEY = "buildTypeId";

    @Autowired
    private ProjectManager projectManager;
    @Autowired
    private ModelCommonFactory modelCommonFactory;
    @Autowired
    private ParametersFactory parametersFactory;
    @Autowired
    private SBuildServer buildServer;

    private ProgressEventsListener(EventDispatcher<BuildServerListener> dispatcher) {
        dispatcher.addListener(this);
    }

    @Override
    public void buildTypeAddedToQueue(@NotNull SQueuedBuild queuedBuild) {
        TriggeredBy triggeredBy = queuedBuild.getTriggeredBy();
        if (!triggeredBy.getParameters().containsKey(TRIGGER_BUILD_TYPE_KEY)) {

            CIEvent event = dtoFactory.newDTO(CIEvent.class)
                    .setEventType(CIEventType.STARTED)
                    .setBuildCiId(queuedBuild.getItemId())
                    .setProject(queuedBuild.getBuildType().getExternalId())
                    .setProjectDisplayName(queuedBuild.getBuildType().getName())
                    .setStartTime(System.currentTimeMillis())
                    .setCauses(new ArrayList<>());
            OctaneSDK.getClients().forEach(client -> client.getEventsService().publishEvent(event));
        }
    }

    @Override
    public void buildStarted(@NotNull SRunningBuild build) {
        TriggeredBy triggeredBy = build.getTriggeredBy();
        List<CIEventCause> causes = new ArrayList<>();

        updateBuildTriggerCause(triggeredBy, causes);

        CIEvent event = dtoFactory.newDTO(CIEvent.class)
                .setEventType(CIEventType.STARTED)
                .setProject(build.getBuildTypeExternalId())
                .setProjectDisplayName(build.getBuildTypeName())
                .setBuildCiId(String.valueOf(build.getBuildId()))
                .setNumber(build.getBuildNumber())
                .setParameters(parametersFactory.obtainFromBuild(build))
                .setCauses(causes)
                .setStartTime(build.getStartDate().getTime())
                .setEstimatedDuration(build.getDurationEstimate() * 1000);
        OctaneSDK.getClients().forEach(client -> client.getEventsService().publishEvent(event));
    }

    @Override
    public void changesLoaded(@NotNull SRunningBuild build) {
        TriggeredBy triggeredBy = build.getTriggeredBy();
        List<CIEventCause> causes = new ArrayList<>();

        updateBuildTriggerCause(triggeredBy, causes);
        CIEvent scmEvent = dtoFactory.newDTO(CIEvent.class)
                .setEventType(CIEventType.SCM)
                .setCauses(causes)
                .setProject(build.getBuildTypeExternalId())
                .setProjectDisplayName(build.getBuildTypeName())
                .setBuildCiId(String.valueOf(build.getBuildId()))
                .setNumber(build.getBuildNumber())
                .setEstimatedDuration(build.getDurationEstimate() * 1000)
                .setStartTime(System.currentTimeMillis())
                .setPhaseType(PhaseType.INTERNAL)
                .setScmData(ScmUtils.getScmData(build));

        OctaneSDK.getClients().forEach(client -> client.getEventsService().publishEvent(scmEvent));
    }

    @Override
    public void buildFinished(@NotNull SRunningBuild build) {
        TriggeredBy triggeredBy = build.getTriggeredBy();
        List<CIEventCause> causes = new ArrayList<>();

        updateBuildTriggerCause(triggeredBy, causes);

        CIEvent event = dtoFactory.newDTO(CIEvent.class)
                .setEventType(CIEventType.FINISHED)
                .setProject(build.getBuildTypeExternalId())
                .setProjectDisplayName(build.getBuildTypeName())
                .setBuildCiId(String.valueOf(build.getBuildId()))
                .setNumber(build.getBuildNumber())
                .setParameters(parametersFactory.obtainFromBuild(build))
                .setCauses(causes)
                .setStartTime(build.getStartDate().getTime())
                .setEstimatedDuration(build.getDurationEstimate() * 1000)
                .setDuration(build.getDuration() * 1000)
                .setResult(modelCommonFactory.resultFromNativeStatus(build.getBuildStatus()));
        OctaneSDK.getClients().forEach(client -> client.getEventsService().publishEvent(event));
    }

    private void updateBuildTriggerCause(TriggeredBy triggeredBy, List<CIEventCause> causes) {
        if (triggeredBy.getParameters().containsKey(TRIGGER_BUILD_TYPE_KEY)) {
            String rootBuildTypeId = triggeredBy.getParameters().get(TRIGGER_BUILD_TYPE_KEY);
            SQueuedBuild rootBuild = getTriggerBuild(rootBuildTypeId);
            if (rootBuild != null) {
                causes.add(causeFromBuild(rootBuild));
            }
        }
    }

    private SQueuedBuild getTriggerBuild(String triggerBuildTypeId) {
        SQueuedBuild result = null;
        SBuildType triggerBuildType = projectManager.findBuildTypeById(triggerBuildTypeId);
        if (triggerBuildType != null) {
            List<SQueuedBuild> queuedBuildsOfType = triggerBuildType.getQueuedBuilds(null);
            if (!queuedBuildsOfType.isEmpty()) {
                result = queuedBuildsOfType.get(0);
            }
        }
        return result;
    }

    private CIEventCause causeFromBuild(SQueuedBuild build) {
        return dtoFactory.newDTO(CIEventCause.class)
                .setType(CIEventCauseType.UPSTREAM)
                .setProject(build.getBuildType().getExternalId())
                .setBuildCiId(String.valueOf(build.getItemId()));
    }


}
