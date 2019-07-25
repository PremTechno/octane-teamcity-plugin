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

package com.hp.octane.plugins.jetbrains.teamcity.configuration;

import com.hp.octane.integrations.OctaneConfiguration;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.connectivity.OctaneResponse;
import com.hp.octane.plugins.jetbrains.teamcity.TeamCityPluginServicesImpl;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.buildResponseStringEmptyConfigs;
import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.buildResponseStringEmptyConfigsWithError;
import static com.hp.octane.plugins.jetbrains.teamcity.utils.Utils.setMessageFont;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 * Created by lazara.
 * Created by gadiel.
 */

public class TCConfigurationService {
	private static final Logger logger = LogManager.getLogger(TCConfigurationService.class);
	private static final String CONFIG_FILE = "octane-config.xml";
	private static final String OLD_ROOT_ELEMENT = "octane-config";
	@Autowired
	private SBuildServer buildServer;
	@Autowired
	private PluginDescriptor pluginDescriptor;

	public String checkConfiguration(OctaneConfiguration octaneConfiguration, String impersonatedUser) {
		String resultMessage;
		OctaneResponse result;
		try {
			result = OctaneSDK.testOctaneConfiguration(octaneConfiguration.getUrl(),
					octaneConfiguration.getSharedSpace(),
					octaneConfiguration.getClient(),
					octaneConfiguration.getSecret(),
					TeamCityPluginServicesImpl.class);
		} catch (Exception e) {
			return buildResponseStringEmptyConfigsWithError("Connection failed: " + e.getMessage());
		}

		if (result.getStatus() == HttpStatus.SC_OK) {
			resultMessage = setMessageFont("Connection succeeded", "green");
		} else if (result.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
			resultMessage = setMessageFont("Authentication failed","red");
		} else if (result.getStatus() == HttpStatus.SC_FORBIDDEN) {
			resultMessage = octaneConfiguration.getClient() + " not authorized to shared space " + octaneConfiguration.getSharedSpace();
			resultMessage = setMessageFont(resultMessage, "red");
		} else if (result.getStatus() == HttpStatus.SC_NOT_FOUND) {
			resultMessage = "Shared space " + octaneConfiguration.getSharedSpace() + " not exists";
			resultMessage = setMessageFont(resultMessage, "red");
		} else {
			resultMessage = "Validation failed for unknown reason; status code: " + result.getStatus();
			resultMessage = setMessageFont(resultMessage, "red");
		}
		resultMessage = checkImpersonatedUser(resultMessage, impersonatedUser);
		return buildResponseStringEmptyConfigs(resultMessage);
	}

	private String checkImpersonatedUser(String resultMessage, String impersonatedUser) {
		if (impersonatedUser == null || impersonatedUser.isEmpty()) {
			return resultMessage;
		}
		UserModel userModel = buildServer.getUserModel();
		List<SUser> users = new ArrayList<>(userModel.getAllUsers().getUsers());
		SUser user = users.stream().filter(u -> impersonatedUser.equals(u.getUsername())).findAny()
				.orElse(null);
		if (user == null) {
			resultMessage = resultMessage + "<br><font color=\"red\">Warning! </font>User '" + impersonatedUser + "' is not defined in TeamCity";
		}
		return resultMessage;
	}

	public List<OctaneConfigStructure> readConfig() {
		OctaneConfigMultiSharedSpaceStructure multiSharedSpaceStructure;
		try {
			JAXBContext context = JAXBContext.newInstance(OctaneConfigMultiSharedSpaceStructure.class);
			Unmarshaller un = context.createUnmarshaller();
			multiSharedSpaceStructure = (OctaneConfigMultiSharedSpaceStructure) un.unmarshal(getConfigurationResource());
		} catch (JAXBException jaxbe) {
			logger.error("failed to read Octane configuration", jaxbe);
			return null;
		}
		return multiSharedSpaceStructure.getMultiConfigStructure();
	}

	public void upgradeConfig() {
		OctaneConfigStructure result;
		try {
			JAXBContext context = JAXBContext.newInstance(OctaneConfigStructure.class);
			Unmarshaller un = context.createUnmarshaller();
			result = (OctaneConfigStructure) un.unmarshal(getConfigurationResource());
		} catch (JAXBException jaxbe) {
			logger.error("failed to read Octane configuration", jaxbe);
			return;
		}
		List<OctaneConfigStructure> multiSharedSpaceStructure = new ArrayList<>();
		multiSharedSpaceStructure.add(result);
		OctaneConfigMultiSharedSpaceStructure configs = new OctaneConfigMultiSharedSpaceStructure();
		configs.setMultiConfigStructure(multiSharedSpaceStructure);
		saveConfig(configs);
		logger.info("ALM Octane CI Plugin configuration was upgraded");
	}

	public String saveConfig(OctaneConfigMultiSharedSpaceStructure configs) {
		try {
			JAXBContext context = JAXBContext.newInstance(OctaneConfigMultiSharedSpaceStructure.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(configs, getConfigurationResource());
			int index = 0;
			String result = "{\"configs\":{";
			for (OctaneConfigStructure conf : configs.getMultiConfigStructure()) {
				result += "\"" + index + "\" : \"" + conf.getIdentity() + "\",";
				index++;
			}
			result = configs.getMultiConfigStructure().isEmpty() ? result : result.substring(0, result.length() - 1);
			result += "}, \"status\":\"" + escapeHtml4(setMessageFont("Configurations updated successfully", "green"))+ "\"}";
			return result;
		} catch (JAXBException jaxbe) {
			logger.error("failed to save Octane configurations", jaxbe);
			return buildResponseStringEmptyConfigsWithError("failed to save Octane configurations");
		} catch (IllegalStateException e) {
			logger.error("failed to publish Octane configurations", e);
			return buildResponseStringEmptyConfigsWithError("failed to publish Octane configurations");
		}
	}

	private File getConfigurationResource() {
		return new File(buildServer.getServerRootPath() + pluginDescriptor.getPluginResourcesPath(CONFIG_FILE));
	}

	public boolean isEmptyConfig() {
		File file = getConfigurationResource();
		return !file.exists() || file.length() == 0;
	}

	public boolean isOldConfiguration() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(getConfigurationResource());
			Element rootElement = document.getDocumentElement();
			if (OLD_ROOT_ELEMENT.equalsIgnoreCase(rootElement.getTagName())) {
				return true;
			}
			return false;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
}
