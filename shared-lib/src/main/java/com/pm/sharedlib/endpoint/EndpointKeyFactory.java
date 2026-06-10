package com.pm.sharedlib.endpoint;

public final class EndpointKeyFactory {

    private EndpointKeyFactory() {
    }

    public static String exposedHttp(String method, String path) {
        return "EXPOSED:HTTP:" + normalize(method) + ":" + path;
    }

    public static String exposedMq(String topic, String handlerClass, String handlerMethod) {
        return "EXPOSED:MQ:" + topic + ":" + handlerClass + "#" + handlerMethod;
    }

    public static String clientHttp(String method, String destinationUrl) {
        return "CLIENT:HTTP:" + normalize(method) + ":" + destinationUrl;
    }

    public static String clientMq(String topic) {
        return "CLIENT:MQ:" + topic;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toUpperCase();
    }
}
