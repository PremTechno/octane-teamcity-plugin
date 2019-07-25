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

package com.hp.octane.plugins.jetbrains.teamcity.factories;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameterType;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by gullery on 22/03/2016.
 */

public class TCPluginParametersFactory {
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	public List<CIParameter> obtainFromBuildType(SBuildType buildType) {
		List<CIParameter> result = new LinkedList<>();
		CIParameter tmp;

		if (buildType != null && !buildType.getParameters().isEmpty()) {
			for (Map.Entry<String, String> parameter : buildType.getParameters().entrySet()) {
				tmp = dtoFactory.newDTO(CIParameter.class)
						.setType(CIParameterType.STRING)
						.setName(parameter.getKey())
						.setDescription("Value location: " + parameter.getValue())
						.setValue(parameter.getValue())
						.setDefaultValue(parameter.getValue());
				result.add(tmp);
			}
		}

		return result;
	}

	public List<CIParameter> obtainFromBuild(SBuild build) {
		List<CIParameter> result = new LinkedList<>();
		CIParameter tmp;

		if (build != null && !build.getBuildOwnParameters().isEmpty()) {
			for (Map.Entry<String, String> parameter : build.getBuildOwnParameters().entrySet()) {
				String name = parameter.getKey();
				if (parameter.getKey().startsWith("build.my")) {
					name = parameter.getKey().substring("build.my.".length(), parameter.getKey().length());
				}
				if (isNotVisibleParam(name)) {
					continue;
				}
				tmp = dtoFactory.newDTO(CIParameter.class)
						.setType(CIParameterType.STRING)
						.setName(name)
						.setValue(parameter.getValue());
				result.add(tmp);
			}
		}

		return result;
	}

	private boolean isNotVisibleParam(String name) {
		if (name.startsWith("system.") ||
				name.startsWith("build.") ||
				name.startsWith("env.") ||
				name.startsWith("teamcity.")) {
			return true;
		}
		return false;
	}
}
