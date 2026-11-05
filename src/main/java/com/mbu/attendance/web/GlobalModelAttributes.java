package com.mbu.attendance.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds version / build details to every page footer so you can see each new
 * Jenkins deployment go live (build number is passed in by Ansible).
 */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAttributes {

    private final String version;
    private final String buildNumber;
    private final String environment;

    public GlobalModelAttributes(ObjectProvider<BuildProperties> buildProperties,
                                 @Value("${app.build-number:local}") String buildNumber,
                                 @Value("${app.environment:development}") String environment) {
        BuildProperties props = buildProperties.getIfAvailable();
        this.version = props != null ? props.getVersion() : "dev";
        this.buildNumber = buildNumber;
        this.environment = environment;
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return version;
    }

    @ModelAttribute("buildNumber")
    public String buildNumber() {
        return buildNumber;
    }

    @ModelAttribute("appEnvironment")
    public String appEnvironment() {
        return environment;
    }
}
