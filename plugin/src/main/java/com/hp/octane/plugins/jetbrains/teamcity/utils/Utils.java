package com.hp.octane.plugins.jetbrains.teamcity.utils;

public class Utils {
    public static String buildResponseStringEmptyConfigs(String resultMessage){
        return "{\"configs\":{}, \"status\":\"" + resultMessage + "\"}";
    }
}
