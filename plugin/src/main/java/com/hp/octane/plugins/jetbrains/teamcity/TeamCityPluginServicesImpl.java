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

package com.hp.octane.plugins.jetbrains.teamcity;

import com.hp.octane.integrations.CIPluginServices;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.parameters.CIParameter;
import com.hp.octane.integrations.dto.parameters.CIParameters;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.tests.BuildContext;
import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunResult;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.exceptions.PermissionException;
import com.hp.octane.integrations.utils.CIPluginSDKUtils;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigStructure;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.TCConfigurationHolder;
import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import com.hp.octane.plugins.jetbrains.teamcity.testrunner.TeamCityTestsToRunConverterBuilder;
import com.hp.octane.plugins.jetbrains.teamcity.utils.SpringContextBridge;
import jetbrains.buildServer.Build;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.serverSide.impl.RunningBuildState;
import jetbrains.buildServer.serverSide.parameters.ParameterFactory;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Created by gullery on 21/01/2016.
 * TeamCity CI Server oriented extension of CI Data Provider
 */

public class TeamCityPluginServicesImpl extends CIPluginServices {
	private static final Logger log = LogManager.getLogger(TeamCityPluginServicesImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private final int MAX_SIZE = 255;
	private ProjectManager projectManager;
	private BuildServerEx buildServerEx;
	private ModelCommonFactory modelCommonFactory;
	private SnapshotsFactory snapshotsFactory;
	private PluginDescriptor pluginDescriptor;
	private UserModel userModel;
	private TCConfigurationHolder holder;
	private ParameterFactory parameterFactory;

	public TeamCityPluginServicesImpl() {
		projectManager = SpringContextBridge.services().getProjectManager();
		buildServerEx = SpringContextBridge.services().getSBuildServer();
		modelCommonFactory = SpringContextBridge.services().getModelCommonFactory();
		snapshotsFactory = SpringContextBridge.services().getSnapshotsFactory();
		pluginDescriptor = SpringContextBridge.services().getPluginDescriptor();
		userModel = buildServerEx.getUserModel();
		holder = SpringContextBridge.services().getTCConfigurationHolder();
		parameterFactory = SpringContextBridge.services().getParameterFactory();
	}

	private SUser getImpersonatedUser() {
		OctaneConfigStructure conf = holder.getConfigs().stream().filter(confStr -> getInstanceId().equals(confStr.getIdentity())).findAny()
				.orElse(null);
		if (conf != null && (conf.getImpersonatedUser() != null && !conf.getImpersonatedUser().isEmpty())) {
			SUser impersonatedUser = userModel.findUserAccount(null, conf.getImpersonatedUser());
			if (impersonatedUser == null) {
				log.error("Impersonated user '" + conf.getImpersonatedUser() + "' does not exist in TeamCity");
				throw new PermissionException(HttpStatus.SC_UNAUTHORIZED);
			}
			return impersonatedUser;
		}
		return null;
	}

	@Override
	public CIServerInfo getServerInfo() {
		return dtoFactory.newDTO(CIServerInfo.class)
				.setSendingTime(System.currentTimeMillis())
				.setType(CIServerTypes.TEAMCITY.value())
				.setUrl(buildServerEx.getRootUrl())
				.setVersion(pluginDescriptor.getPluginVersion());
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return dtoFactory.newDTO(CIPluginInfo.class)
				.setVersion(pluginDescriptor.getPluginVersion());
	}

	@Override
	public File getAllowedOctaneStorage() {
		return new File(buildServerEx.getServerRootPath(), "logs");
	}

	@Override
	public CIProxyConfiguration getProxyConfiguration(URL targetHostUrl) {
		log.info("get proxy configuration");
		CIProxyConfiguration result = null;
		if (isProxyNeeded(targetHostUrl)) {
			log.info("proxy is required for host " + targetHostUrl.getHost());
			Map<String, String> propertiesMap = parseProperties(System.getenv("TEAMCITY_SERVER_OPTS"));
			String protocol = "D" + targetHostUrl.getProtocol();
			result = dtoFactory.newDTO(CIProxyConfiguration.class)
					.setHost(propertiesMap.get(protocol + ".proxyHost"))
					.setPort(Integer.parseInt(propertiesMap.get(protocol + ".proxyPort")))
					.setUsername(propertiesMap.get(protocol + ".proxyUser"))
					.setPassword(propertiesMap.get(protocol + ".proxyPassword"));
		}
		return result;
	}

	@Override
	public CIJobsList getJobsList(boolean includeParameters) {
		SUser impersonatedUser = getImpersonatedUser();
		if (impersonatedUser == null) {
			return modelCommonFactory.createProjectList();
		}
		try {
			return buildServerEx.getSecurityContext().runAs(impersonatedUser, () -> modelCommonFactory.createProjectList());
		} catch (Throwable throwable) {
			if (throwable instanceof AccessDeniedException) {
				log.error(throwable.getMessage(), throwable);
				throw new PermissionException(HttpStatus.SC_FORBIDDEN);
			}
			ExceptionUtil.rethrowAsRuntimeException(throwable);
		}
		return null;
	}

	@Override
	public PipelineNode getPipeline(String rootJobCiId) {
		return modelCommonFactory.createStructure(rootJobCiId);
	}

	@Override
	public SnapshotNode getSnapshotLatest(String jobCiId, boolean subTree) {
		return snapshotsFactory.createSnapshot(jobCiId);
	}

	//  TODO: implement
	@Override
	public SnapshotNode getSnapshotByNumber(String jobCiId, String buildCiId, boolean subTree) {
		return null;
	}

	@Override
	public void runPipeline(String jobCiId, String originalBody) {
		SBuildType buildType = findBuildType(jobCiId);
		if (buildType != null) {
			setJobParams(originalBody, buildType);
			buildType.addToQueue("ngaRemoteExecution");
		}
	}

	@Override
	public void stopPipelineRun(String jobCiId, String originalBody) {
		SBuildType buildType = findBuildType(jobCiId);
		if (buildType != null) {
			SUser impersonatedUser = getImpersonatedUser();
			List<SRunningBuild> runningBuilds = buildType.getRunningBuilds(impersonatedUser);
			for (SRunningBuild runningBuild : runningBuilds) {
				String interruptedMessage = "build number [" + runningBuild.getBuildNumber() + "] of project "
						+ runningBuild.getFullName() + " was canceled";
				runningBuild.setInterrupted(RunningBuildState.INTERRUPTED_BY_USER, impersonatedUser, interruptedMessage);
				runningBuild.stop(impersonatedUser, interruptedMessage);
			}
		}
	}

	private void setJobParams(String originalBody, SBuildType buildType) {
		if (originalBody != null && !originalBody.isEmpty() && originalBody.contains("parameters") &&
				!buildType.getParameters().isEmpty()) {
			CIParameters ciParameters = DTOFactory.getInstance().dtoFromJson(originalBody, CIParameters.class);
			for (CIParameter param : ciParameters.getParameters()) {
				String value;
				if (TeamCityTestsToRunConverterBuilder.TESTS_TO_RUN_PARAMETER.equals(param.getName())) {
					value = setTestsToRun(ciParameters, param, buildType.getConfigParameters().get(TeamCityTestsToRunConverterBuilder.TESTING_FRAMEWORK_PARAMETER));
				} else {
					value = (String) (param.getValue() == null ? param.getDefaultValue() : param.getValue());
				}
				Parameter buildParam = parameterFactory.createSimpleParameter("build.my." + param.getName(), value);
				buildType.addBuildParameter(buildParam);
			}
		}
	}

	private String setTestsToRun(CIParameters ciParameters, CIParameter testsToRun, String defaultFramework) {
		CIParameter testingFramework = ciParameters.getParameters().stream().filter(ciparam -> TeamCityTestsToRunConverterBuilder.TESTING_FRAMEWORK_PARAMETER.equals(ciparam.getName()))
				.findAny()
				.orElse(null);
		TeamCityTestsToRunConverterBuilder builder;
		if (testingFramework != null) {
			String framework = (String) testingFramework.getValue();
			builder = new TeamCityTestsToRunConverterBuilder(framework);
		} else if (defaultFramework != null && !defaultFramework.isEmpty()) {
			builder = new TeamCityTestsToRunConverterBuilder(defaultFramework);
		} else {
			builder = new TeamCityTestsToRunConverterBuilder();
		}
		return builder.convert((String) testsToRun.getValue(), null).getConvertedTestsString();
	}

	@Override
	public InputStream getTestsResult(String jobId, String buildId) {
		TestsResult result = null;
		if (jobId != null && buildId != null) {
			SBuildType buildType = findBuildType(jobId);
			if (buildType != null) {
				Build build = buildServerEx.findBuildInstanceById(Long.valueOf(buildId));
				if (build instanceof SFinishedBuild) {
					List<TestRun> tests = createTestList((SFinishedBuild) build);
					if (tests != null && !tests.isEmpty()) {
						BuildContext buildContext = dtoFactory.newDTO(BuildContext.class)
								.setJobId(build.getBuildTypeExternalId())
								.setJobName(build.getBuildTypeName())
								.setBuildId(String.valueOf(build.getBuildId()))
								.setBuildName(build.getBuildNumber())
								.setServerId(getInstanceId());
						result = dtoFactory.newDTO(TestsResult.class)
								.setBuildContext(buildContext)
								.setTestRuns(tests);
					}
				}
			}
		}
		return result == null ? null : dtoFactory.dtoToXmlStream(result);
	}

	private SBuildType findBuildType(String jobId) {
		SUser impersonatedUser = getImpersonatedUser();
		if (impersonatedUser == null) {
			return projectManager.findBuildTypeByExternalId(jobId);
		} else {
			try {
				return buildServerEx.getSecurityContext().runAs(impersonatedUser, () -> projectManager.findBuildTypeByExternalId(jobId));
			} catch (Throwable throwable) {
				if (throwable instanceof AccessDeniedException) {
					log.error(throwable.getMessage(), throwable);
					throw new PermissionException(HttpStatus.SC_FORBIDDEN);
				}
				ExceptionUtil.rethrowAsRuntimeException(throwable);
			}
		}
		return null;
	}

	private List<TestRun> createTestList(SFinishedBuild build) {
		List<TestRun> result = new ArrayList<TestRun>();
		BuildStatistics stats = build.getBuildStatistics(new BuildStatisticsOptions());
		for (STestRun testRun : stats.getTests(null, BuildStatistics.Order.NATURAL_ASC)) {
			TestRunResult testResultStatus = null;
			if (testRun.isIgnored()) {
				testResultStatus = TestRunResult.SKIPPED;
			} else if (testRun.getStatus().isFailed()) {
				testResultStatus = TestRunResult.FAILED;
			} else if (testRun.getStatus().isSuccessful()) {
				testResultStatus = TestRunResult.PASSED;
			}
			TestName fqTestName = testRun.getTest().getName();
			String pkgName = fqTestName.getPackageName();
			String className = fqTestName.getClassName();
			String testName = fqTestName.getTestMethodName();
			if (pkgName.length() > MAX_SIZE ||
					className.length() > MAX_SIZE ||
					testName.length() > MAX_SIZE) {
				log.error("Test [" + fqTestName.toString() + "] excluded from test results sending to ALM Octane. One of its parameters (package name, class name or test name) exceeds max size of 255 chars length.");
			} else {
				TestRun tr = dtoFactory.newDTO(TestRun.class)
						.setModuleName("")
						.setPackageName(pkgName)
						.setClassName(className)
						.setTestName(testName)
						.setResult(testResultStatus)
						.setStarted(build.getStartDate().getTime())
						.setDuration((long) testRun.getDuration());
				result.add(tr);
			}
		}

		return result;
	}

	private boolean isProxyNeeded(URL targetHost) {
		boolean result = false;
		Map<String, String> propertiesMap = parseProperties(System.getenv("TEAMCITY_SERVER_OPTS"));

		String proxyHost = "D" + targetHost.getProtocol() + ".proxyHost";
		if (propertiesMap.get(proxyHost) != null) {

			String nonProxyHostsStr = propertiesMap.get("D" + targetHost.getProtocol() + ".nonProxyHosts");

			if (targetHost != null && !CIPluginSDKUtils.isNonProxyHost(targetHost.getHost(), nonProxyHostsStr)) {
				result = true;
			}
		}
		return result;
	}

	private Map<String, String> parseProperties(String internalProperties) {
		Map<String, String> propertiesMap = new HashMap<String, String>();
		if (internalProperties != null) {
			String[] properties = internalProperties.split(" -");
			for (String str : Arrays.asList(properties)) {
				String[] split = str.split("=");
				if (split.length == 2) {
					propertiesMap.put(split[0], split[1]);
				}
			}
		}
		return propertiesMap;
	}
}
