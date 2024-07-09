package dev.httpmarco.polocloud.rest.endpoints.impl.v1.service;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.api.CloudAPI;
import dev.httpmarco.polocloud.rest.RestAPI;
import dev.httpmarco.polocloud.rest.endpoints.Endpoint;
import io.javalin.http.Context;

public class ServicesEndpoint extends Endpoint {

    public ServicesEndpoint(RestAPI restAPI) {
        super("services/", restAPI);
    }

    @Override
    public void get(Context context) {
        CloudAPI.instance().logger().info("request");
        var services = CloudAPI.instance().serviceProvider().services();
        CloudAPI.instance().logger().info(GSON.toJsonTree(services).getAsJsonArray().toString());
        var response = new JsonObject();

        response.add("services", GSON.toJsonTree(services).getAsJsonArray());

        context.status(200);
        context.json(response);
    }
}
