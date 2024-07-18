package dev.httpmarco.polocloud.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.httpmarco.polocloud.api.dependencies.Dependency;
import dev.httpmarco.polocloud.api.module.CloudModule;
import dev.httpmarco.polocloud.base.CloudBase;
import dev.httpmarco.polocloud.rest.command.RestCommand;
import dev.httpmarco.polocloud.rest.config.Config;
import dev.httpmarco.polocloud.rest.endpoints.EndpointServiceProvider;
import dev.httpmarco.polocloud.rest.user.UserManager;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.LoggerFactory;

@Getter
@Accessors(fluent = true)
public class RestAPI implements CloudModule {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Config config;
    private Javalin javalin;
    private Logger logger;
    private UserManager userManager;
    private EndpointServiceProvider endpointService;

    @Override
    public void onEnable() {
        loadDependency();
        this.config = new Config();

        this.logger = (Logger) LoggerFactory.getLogger(RestAPI.class);
        // set`s log level of jetty
        var jettyLogger = (Logger) LoggerFactory.getLogger("org.eclipse.jetty");
        jettyLogger.setLevel(Level.INFO);

        startUpJavalin();

        this.userManager = new UserManager(this);
        (this.endpointService = new EndpointServiceProvider(this)).load();

        var commandService = CloudBase.instance().terminal().commandService();
        commandService.registerCommand(new RestCommand(this));
    }

    @Override
    public void onDisable() {
        this.javalin.stop();
    }

    private void startUpJavalin() {
        this.javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;

            config.pvt.javaLangErrorHandler(((httpServletResponse, error) -> {
                httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
                error.printStackTrace();
            }));
        }).start(this.config.javalinConfiguration().port());

        this.javalin.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            e.printStackTrace();
        });
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
        Dependency.load("ch.qos.logback", "logback-core", "1.4.11");
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
        Dependency.load("org.eclipse.jetty.toolchain", "jetty-jakarta-servlet-api", "5.0.2");
        Dependency.load("com.fasterxml.jackson.core", "jackson-annotations", "2.15.2");
        Dependency.load("org.eclipse.jetty", "jetty-http", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-util", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-server", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-servlet", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-security", "11.0.21");
        Dependency.load("org.eclipse.jetty", "jetty-io", "11.0.21");

        Dependency.load("at.favre.lib", "bcrypt", "0.10.2");
        Dependency.load("at.favre.lib", "bytes", "1.5.0");
    }
}
