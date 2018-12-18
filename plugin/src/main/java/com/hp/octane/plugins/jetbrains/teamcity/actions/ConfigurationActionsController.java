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

package com.hp.octane.plugins.jetbrains.teamcity.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.plugins.jetbrains.teamcity.TeamCityPluginServicesImpl;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigMultiSharedSpaceStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationHolder;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.buildResponseStringEmptyConfigs;

/**
 * Created by lazara on 14/02/2016.
 */

public class ConfigurationActionsController implements Controller {
    private static final Logger logger = LogManager.getLogger(ConfigurationActionsController.class);

    @Autowired
    private TCConfigurationService configurationService;
    @Autowired
    private TCConfigurationHolder holder;

    @Override
    public ModelAndView handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String returnStr = "";
        String action = httpServletRequest.getParameter("action");

        if (!"post".equals(httpServletRequest.getMethod().toLowerCase()) && (action == null || action.isEmpty())) {
            returnStr = reloadConfiguration();
        } else {
            try {
                if ("test".equalsIgnoreCase(action)) {
                    String server = httpServletRequest.getParameter("server");
                    String url = parseUiLocation(server);
                    String apiKey = httpServletRequest.getParameter("username");
                    String secret = httpServletRequest.getParameter("password");
                    String sharedSpace = httpServletRequest.getParameter("sharedSpace");
                    OctaneConfiguration testedOctaneConfiguration = new OctaneConfiguration(UUID.randomUUID().toString(), url, sharedSpace);
                    testedOctaneConfiguration.setClient(apiKey);
                    testedOctaneConfiguration.setSecret(secret);
                    returnStr = configurationService.checkConfiguration(testedOctaneConfiguration);
                } else {
                    //save configuration
                    ObjectMapper objectMapper = new ObjectMapper();
                    TypeFactory typeFactory = objectMapper.getTypeFactory();
                    CollectionType collectionType = typeFactory.constructCollectionType(
                            List.class, OctaneConfigStructure.class);
                    List<OctaneConfigStructure> configs = objectMapper.readValue(httpServletRequest.getInputStream(), collectionType);
                    handleDeletedConfigurations(configs);
                    returnStr = updateConfiguration(configs);
                }
            } catch (Exception e) {
                logger.error("failed to process configuration request (" + (action == null ? "save" : action) + ")", e);
                returnStr = e.getMessage() + ". Failed to process configuration request (" + (action == null ? "save" : action) + ")";
                returnStr = buildResponseStringEmptyConfigs(returnStr);
            }
        }

        PrintWriter writer;
        try {
            writer = httpServletResponse.getWriter();
            writer.write(returnStr);
        } catch (IOException ioe) {
            logger.error("failed to write response", ioe);
        }
        return null;
    }

    private void handleDeletedConfigurations(List<OctaneConfigStructure> newConfigs) {
        Map<String, OctaneConfiguration> origConfigs = holder.getOctaneConfigurations();
        Set<String> configToRemove = new HashSet<>();
        origConfigs.entrySet().forEach(entry -> {
            boolean found = false;
            for (OctaneConfigStructure newConfig : newConfigs) {
                String identity = newConfig.getIdentity();
                //new configuration
                if (identity == null || identity.isEmpty() || "undefined".equalsIgnoreCase(identity)) {
                    found = true;
                    break;
                }

                if (entry.getKey().equals(newConfig.getIdentity())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                OctaneConfiguration octaneConfiguration = entry.getValue();
                if (octaneConfiguration != null) {
                    logger.info("Removing client with instance Id: " + entry.getKey());
                    OctaneSDK.removeClient(OctaneSDK.getClientByInstanceId(entry.getKey()));
                    configToRemove.add(entry.getKey());
                }
            }
        });
        if (!configToRemove.isEmpty()) {
            configToRemove.forEach(instanceId -> origConfigs.remove(instanceId));
            //remove config before save
            for (OctaneConfigStructure octaneConfigStructure : new ArrayList<>(holder.getConfigs())) {
                if (configToRemove.contains(octaneConfigStructure.getIdentity())) {
                    holder.getConfigs().remove(octaneConfigStructure);
                }
            }
            save();
        }
    }

    private String save() {
        logger.info("Saving ALM Octane configurations...");
        OctaneConfigMultiSharedSpaceStructure confs = new OctaneConfigMultiSharedSpaceStructure();
        confs.setMultiConfigStructure(holder.getConfigs());
        return configurationService.saveConfig(confs);
    }

    public String updateConfiguration(List<OctaneConfigStructure> newConfigs) {
        List<OctaneConfigStructure> originalConfigs = holder.getConfigs();
        for (OctaneConfigStructure newConf : newConfigs) {
            OctaneConfigStructure result = originalConfigs.stream()
                    .filter(or_conf -> or_conf.getIdentity().equals(newConf.getIdentity()))
                    .findAny()
                    .orElse(null);
            if (result == null) {
                checkAndUpdateIdentityAndLocationIfNotTheSame(newConf);
                OctaneConfiguration octaneConfiguration = new OctaneConfiguration(newConf.getIdentity(), newConf.getLocation(),
                        newConf.getSharedSpace());
                octaneConfiguration.setClient(newConf.getUsername());
                octaneConfiguration.setSecret(newConf.getSecretPassword());
                try {
                    OctaneSDK.addClient(octaneConfiguration, TeamCityPluginServicesImpl.class);
                }catch (Exception e){
                   return  buildResponseStringEmptyConfigs(e.getMessage());
                }
                holder.getOctaneConfigurations().put(newConf.getIdentity(), octaneConfiguration);
                holder.getConfigs().add(newConf);
            } else {
                //update existing configuration
                OctaneConfiguration octaneConfiguration = holder.getOctaneConfigurations().get(result.getIdentity());
                octaneConfiguration.setUrl(newConf.getUiLocation());
                result.setUiLocation(newConf.getUiLocation());
                octaneConfiguration.setClient(newConf.getUsername());
                result.setUsername(newConf.getUsername());
                octaneConfiguration.setSecret(newConf.getSecretPassword());
                result.setSecretPassword(newConf.getSecretPassword());
                octaneConfiguration.setSharedSpace(newConf.getSharedSpace());
                result.setSharedSpace(newConf.getSharedSpace());
                result.setLocation(parseUiLocation(newConf.getUiLocation()));
             }
        }

        return save();
    }

    private void checkAndUpdateIdentityAndLocationIfNotTheSame(OctaneConfigStructure newConf) {
        String identity = newConf.getIdentity();
        String location = parseUiLocation(newConf.getUiLocation());
        newConf.setLocation(location);
        if (holder.getConfigs().contains(newConf)){
            OctaneConfigStructure matchingObject = holder.getConfigs().stream().
                    filter(c -> c.equals(newConf)).
                    findAny().orElse(null);
            if (matchingObject != null){
                newConf.setIdentity(matchingObject.getIdentity());
                newConf.setIdentityFrom(matchingObject.getIdentityFrom());
                return;
            }
        }
        if (identity == null || identity.equals("") || "undefined".equalsIgnoreCase(identity)) {
            newConf.setIdentity(UUID.randomUUID().toString());
            newConf.setIdentityFrom(String.valueOf(new Date().getTime()));
        }
    }

    public String reloadConfiguration() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<OctaneConfigStructure> cfg = holder.getConfigs();
            return mapper.writeValueAsString(cfg);
        } catch (JsonProcessingException jpe) {
            logger.error("failed to reload configuration", jpe);
            return "failed to reload configuration";
        }
    }

    public String parseUiLocation(String uiLocation) {
        int contextPos = uiLocation.indexOf("/ui");
        if (contextPos < 0) {
            return uiLocation;
        } else {
            return uiLocation.substring(0, contextPos);
        }
    }
}
