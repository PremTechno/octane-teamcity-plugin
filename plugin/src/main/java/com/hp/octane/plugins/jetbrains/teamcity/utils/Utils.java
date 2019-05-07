package com.hp.octane.plugins.jetbrains.teamcity.utils;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

public class Utils {
	public static String buildResponseStringEmptyConfigs(String resultMessage) {
		return "{\"configs\":{}, \"status\":\"" + escapeHtml4(resultMessage) + "\"}";
	}

	public static String buildResponseStringEmptyConfigsWithError(String resultMessage) {
		return "{\"configs\":{}, \"status\":\"" + escapeHtml4(setMessageFont(resultMessage, "red")) + "\"}";
	}

	public static String setMessageFont(String messge, String color) {
		messge = "<font color=\"" + color + "\">" + messge + "</font>";
		return messge;
	}
}
