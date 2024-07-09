package dev.httpmarco.polocloud.rest;

import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.api.dependencies.Dependency;
import dev.httpmarco.polocloud.api.module.CloudModule;
import dev.httpmarco.polocloud.rest.endpoints.EndpointServiceProvider;
import io.javalin.Javalin;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class RestAPI implements CloudModule {

    private Javalin javalin;
    private EndpointServiceProvider endpointService;

    @Override
    public void onEnable() {
        loadDependency();
        //TODO port
        //TODO ip
        //TODO ssl
        this.javalin = Javalin.createAndStart(config -> config.showJavalinBanner = false);
        (this.endpointService = new EndpointServiceProvider(this)).load();

        this.javalin.exception(Exception.class, (e, ctx) -> {
            CloudAPI.instance().logger().error("Module: REST API, error occurred: ", e);
            ctx.status(500); //TODO 500 Fehler kommt, muss fixed werden (Dafür muss logging fehler behoben werden)
        });
    }

    @Override
    public void onDisable() {
        this.javalin.stop();
    }

    private void loadDependency() {
        //TODO better way to load dependency
        Dependency.load("io.javalin", "javalin", "6.1.6");
        Dependency.load("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.9.24");
        Dependency.load("org.jetbrains.kotlin", "kotlin-reflect", "1.9.24");
        Dependency.load("org.jetbrains.kotlin", "kotlin-stdlib-common", "1.9.24");
        Dependency.load("org.jetbrains.kotlin", "kotlin-stdlib", "1.9.24");
        Dependency.load("org.jetbrains", "annotations", "24.1.0");
        Dependency.load("org.slf4j", "slf4j-api", "2.0.9");
        Dependency.load("ch.qos.logback", "logback-classic", "1.4.11");
        Dependency.load("io.github.hakky54", "sslcontext-kickstart", "8.3.6");
        Dependency.load("io.github.hakky54", "sslcontext-kickstart-for-jetty", "8.3.6");
        Dependency.load("io.github.hakky54", "sslcontext-kickstart-for-pem", "8.3.6");
        Dependency.load("com.aayushatharva.brotli4j", "brotli4j", "1.16.0");
        Dependency.load("com.fasterxml.jackson.core", "jackson-core", "2.17.1");
        Dependency.load("com.fasterxml.jackson.core", "jackson-databind", "2.17.1");
        Dependency.load("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.17.1");
        Dependency.load("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", "2.17.1");
        Dependency.load("com.squareup.okhttp3", "okhttp", "4.12.0");
        Dependency.load("com.squareup.okhttp3", "okhttp-tls", "4.12.0");
        //Todo alle dependecys oben durchschauen ob nötig

        // These dependecys are important to use
        Dependency.load("org.eclipse.jetty.toolchain", "jetty-jakarta-servlet-api", "5.0.2");
        Dependency.load("org.eclipse.jetty", "jetty-http", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-util", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-server", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-servlet", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-security", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-io", "11.0.21");
    }
}
