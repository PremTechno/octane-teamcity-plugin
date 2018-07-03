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

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.configuration.CIProxyConfiguration;
import com.hp.octane.integrations.dto.configuration.OctaneConfiguration;
import com.hp.octane.integrations.dto.general.CIJobsList;
import com.hp.octane.integrations.dto.general.CIPluginInfo;
import com.hp.octane.integrations.dto.general.CIServerInfo;
import com.hp.octane.integrations.dto.general.CIServerTypes;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.integrations.dto.snapshots.SnapshotNode;
import com.hp.octane.integrations.dto.tests.BuildContext;
import com.hp.octane.integrations.dto.tests.TestRun;
import com.hp.octane.integrations.dto.tests.TestRunResult;
import com.hp.octane.integrations.dto.tests.TestsResult;
import com.hp.octane.integrations.spi.CIPluginServicesBase;
import com.hp.octane.integrations.util.CIPluginSDKUtils;
import com.hp.octane.plugins.jetbrains.teamcity.configuration.OctaneConfigStructure;
import com.hp.octane.plugins.jetbrains.teamcity.factories.ModelCommonFactory;
import com.hp.octane.plugins.jetbrains.teamcity.factories.SnapshotsFactory;
import jetbrains.buildServer.Build;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.STestRun;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by gullery on 21/01/2016.
 * TeamCity CI Server oriented extension of CI Data Provider
 */

public class TeamCityPluginServicesImpl extends CIPluginServicesBase {
	private static final Logger log = LogManager.getLogger(TeamCityPluginServicesImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();
	private static final String pluginVersion = "9.1.5";

	@Autowired
	private OctaneTeamCityPlugin octaneTeamCityPlugin;
	@Autowired
	private SBuildServer buildServer;
	@Autowired
	private ModelCommonFactory modelCommonFactory;
	@Autowired
	private SnapshotsFactory snapshotsFactory;

	@Override
	public CIServerInfo getServerInfo() {
		return dtoFactory.newDTO(CIServerInfo.class)
				.setInstanceId(octaneTeamCityPlugin.getConfig().getIdentity())
				.setInstanceIdFrom(octaneTeamCityPlugin.getConfig().getIdentityFromAsLong())
				.setSendingTime(System.currentTimeMillis())
				.setType(CIServerTypes.TEAMCITY.value())
				.setUrl(buildServer.getRootUrl())
				.setVersion(pluginVersion);
	}

	@Override
	public CIPluginInfo getPluginInfo() {
		return dtoFactory.newDTO(CIPluginInfo.class)
				.setVersion(octaneTeamCityPlugin.getDescriptor().getPluginVersion());
	}

	@Override
	public File getAllowedOctaneStorage() {
		return new File(buildServer.getServerRootPath(), "logs");
	}

	@Override
	public OctaneConfiguration getOctaneConfiguration() {
		OctaneConfiguration result = null;
		OctaneConfigStructure config = octaneTeamCityPlugin.getConfig();
		if (config != null && config.getLocation() != null && !config.getLocation().isEmpty() && config.getSharedSpace() != null) {
			result = dtoFactory.newDTO(OctaneConfiguration.class)
					.setUrl(config.getLocation())
					.setSharedSpace(config.getSharedSpace())
					.setApiKey(config.getUsername())
					.setSecret(config.getSecretPassword());
		}
		return result;
	}

	@Override
	public CIProxyConfiguration getProxyConfiguration(String targetHost) {
		log.info("get proxy configuration");
		CIProxyConfiguration result = null;
		try {
			URL targetHostUrl = new URL(targetHost);

			if (isProxyNeeded(targetHostUrl)) {
				log.info("proxy is required for host " + targetHost);
				Map<String, String> propertiesMap = parseProperties(System.getenv("TEAMCITY_SERVER_OPTS"));
				String protocol = "D"+targetHostUrl.getProtocol();
				result = dtoFactory.newDTO(CIProxyConfiguration.class)
						.setHost(propertiesMap.get(protocol+".proxyHost"))
						.setPort(Integer.parseInt(propertiesMap.get(protocol+".proxyPort")))
						.setUsername(propertiesMap.get(protocol+".proxyUser"))
						.setPassword(propertiesMap.get(protocol+".proxyPassword"));
			}
		} catch (MalformedURLException e) {
            log.error("Invalid url", e);
		}
		return result;
	}

	@Override
	public CIJobsList getJobsList(boolean includeParameters) {
		return modelCommonFactory.CreateProjectList();
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
		SBuildType buildType = octaneTeamCityPlugin.getProjectManager().findBuildTypeByExternalId(jobCiId);
		if (buildType != null) {
			buildType.addToQueue("ngaRemoteExecution");
		}
	}

	@Override
	public TestsResult getTestsResult(String jobId, String buildNumber) {
		TestsResult result = null;
		if (jobId != null && buildNumber != null) {
			SBuildType buildType = octaneTeamCityPlugin.getProjectManager().findBuildTypeByExternalId(jobId);
			if (buildType != null) {
				Build build = buildType.getBuildByBuildNumber(buildNumber);
				if (build != null && build instanceof SFinishedBuild) {
					List<TestRun> tests = createTestList((SFinishedBuild) build);
					if (tests != null && !tests.isEmpty()) {
						BuildContext buildContext = dtoFactory.newDTO(BuildContext.class)
								.setJobId(build.getBuildTypeExternalId())
								.setJobName(build.getBuildTypeName())
								.setBuildId(String.valueOf(build.getBuildId()))
								.setBuildName(build.getBuildNumber())
								.setServerId(octaneTeamCityPlugin.getConfig().getIdentity());
						result = dtoFactory.newDTO(TestsResult.class)
								.setBuildContext(buildContext)
								.setTestRuns(tests);
					}
				}
			}
		}
		return result;
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

			TestRun tr = dtoFactory.newDTO(TestRun.class)
					.setModuleName("")
					.setPackageName(testRun.getTest().getName().getPackageName())
					.setClassName(testRun.getTest().getName().getClassName())
					.setTestName(testRun.getTest().getName().getTestMethodName())
					.setResult(testResultStatus)
					.setStarted(build.getStartDate().getTime())
					.setDuration((long) testRun.getDuration());
			result.add(tr);
		}

		return result;
	}

	private boolean isProxyNeeded(URL targetHost) {
		boolean result = false;
		Map<String, String> propertiesMap = parseProperties(System.getenv("TEAMCITY_SERVER_OPTS"));

		String proxyHost = "D" + targetHost.getProtocol() + ".proxyHost";
		if (propertiesMap.get(proxyHost) != null) {

			String nonProxyHostsStr = propertiesMap.get("D" + targetHost.getProtocol() + ".nonProxyHosts");

			if (targetHost != null && !CIPluginSDKUtils.isNonProxyHost(targetHost.getHost(),nonProxyHostsStr)) {
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
