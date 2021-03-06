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
import com.hp.octane.integrations.exceptions.OctaneSDKGeneralException;
import com.hp.octane.integrations.utils.OctaneUrlParser;
import com.hp.octane.plugins.jetbrains.teamcity.TeamCityPluginServicesImpl;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.*;
import com.hp.octane.plugins.jetbrains.teamcity.utils.Utils;
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

import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.buildResponseStringEmptyConfigsWithError;

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
					OctaneUrlParser octaneUrlParser;
					try {
						octaneUrlParser = OctaneUrlParser.parse(server);
						String apiKey = httpServletRequest.getParameter("username");
						String secret = httpServletRequest.getParameter("password");

						OctaneConfiguration testedOctaneConfiguration = new OctaneConfiguration(UUID.randomUUID().toString(),
								octaneUrlParser.getLocation(),
								octaneUrlParser.getSharedSpace());
						testedOctaneConfiguration.setClient(apiKey);
						testedOctaneConfiguration.setSecret(secret);
						String impersonatedUser = httpServletRequest.getParameter("impersonatedUser");
						returnStr = configurationService.checkConfiguration(testedOctaneConfiguration, impersonatedUser);
					} catch (OctaneSDKGeneralException error){
						returnStr = buildResponseStringEmptyConfigsWithError(error.getMessage());
					}
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
				returnStr = buildResponseStringEmptyConfigsWithError(returnStr);
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
		Set<String> newConfigIdentities = new HashSet<>();
		for (OctaneConfigStructure conf : newConfigs) {
			String identity = conf.getIdentity() == null || conf.getIdentity().isEmpty() ? "empty" : conf.getIdentity();
			newConfigIdentities.add(identity);
		}
		Set<String> configToRemove = new HashSet<>();
		for (String identity : origConfigs.keySet()) {
			if (!newConfigIdentities.contains(identity)){
				logger.info("Removing client with instance Id: " + identity);
				OctaneSDK.removeClient(OctaneSDK.getClientByInstanceId(identity));
				configToRemove.add(identity);
			}
		}

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
		for (OctaneConfigStructure conf : holder.getConfigs()) {
			conf.setSecretPassword(conf.getSecretPassword());
		}
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

			if (result == null || holder.getOctaneConfigurations().get(result.getIdentity()) == null) {
				OctaneUrlParser octaneUrlParser;
				try {
					octaneUrlParser = checkAndUpdateIdentityAndLocationIfNotTheSame(newConf);
				} catch (OctaneSDKGeneralException error){
					return buildResponseStringEmptyConfigsWithError(error.getMessage());

				}
				OctaneConfiguration octaneConfiguration = new OctaneConfiguration(newConf.getIdentity(), newConf.getLocation(),
						octaneUrlParser.getSharedSpace());
				octaneConfiguration.setClient(newConf.getUsername());
				octaneConfiguration.setSecret(newConf.getSecretPassword());

				try {
					OctaneSDK.addClient(octaneConfiguration, TeamCityPluginServicesImpl.class);
				} catch (Exception e) {
					return buildResponseStringEmptyConfigsWithError(e.getMessage());
				}
				holder.getOctaneConfigurations().put(newConf.getIdentity(), octaneConfiguration);
				newConf.setSharedSpace(octaneUrlParser.getSharedSpace());
				holder.getConfigs().add(newConf);
			} else {
				//update existing configuration
				OctaneConfiguration octaneConfiguration = holder.getOctaneConfigurations().get(result.getIdentity());

				OctaneUrlParser octaneUrlParser;
				try {
					octaneUrlParser = OctaneUrlParser.parse(newConf.getUiLocation());
				} catch (OctaneSDKGeneralException error){
					return buildResponseStringEmptyConfigsWithError(error.getMessage());

				}

				String sp = octaneUrlParser.getSharedSpace();
				String location = octaneUrlParser.getLocation();
				octaneConfiguration.setUrl(newConf.getUiLocation());
				result.setUiLocation(newConf.getUiLocation());
				octaneConfiguration.setClient(newConf.getUsername());
				result.setUsername(newConf.getUsername());
				octaneConfiguration.setSecret(newConf.getSecretPassword());
				result.setSecretPassword(newConf.getSecretPassword());
				octaneConfiguration.setSharedSpace(sp);
				result.setSharedSpace(sp);
				result.setLocation(location);
				result.setImpersonatedUser(newConf.getImpersonatedUser());
			}
		}

		return save();
	}

	private OctaneUrlParser checkAndUpdateIdentityAndLocationIfNotTheSame(OctaneConfigStructure newConf) throws OctaneSDKGeneralException{

	    String identity = newConf.getIdentity();
		OctaneUrlParser octaneUrlParser = OctaneUrlParser.parse(newConf.getUiLocation());
		newConf.setLocation(octaneUrlParser.getLocation());

		if (holder.getConfigs().contains(newConf)) {
			OctaneConfigStructure matchingObject = holder.getConfigs().stream().
					filter(c -> c.equals(newConf)).
					findAny().orElse(null);
			if (matchingObject != null) {
				newConf.setIdentity(matchingObject.getIdentity());
				newConf.setIdentityFrom(matchingObject.getIdentityFrom());
				return octaneUrlParser;
			}
		}
		if (identity == null || identity.equals("") || "undefined".equalsIgnoreCase(identity)) {
			newConf.setIdentity(UUID.randomUUID().toString());
			newConf.setIdentityFrom(String.valueOf(new Date().getTime()));
		}
		return octaneUrlParser;
	}

	public String reloadConfiguration() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			List<OctaneConfigStructure> cfg = holder.getConfigs();
			return mapper.writeValueAsString(cfg);
		} catch (JsonProcessingException jpe) {
			logger.error("failed to reload configuration", jpe);
			return Utils.buildResponseStringEmptyConfigsWithError("failed to reload configuration");
		}
	}
}
