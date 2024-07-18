package dev.httpmarco.polocloud.rest.endpoints.impl.v1.service;

import com.google.gson.JsonArray;
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
        if (!tokenProvided(context)) return;
        var user = handelAuth(context, getToken(context));
        if (user == null) return;
        if (!hasPermission(user, "polocloud.rest.endpoint.services", context)) return;

        var response = new JsonObject();
        var services = new JsonArray();

        CloudAPI.instance().serviceProvider().services().forEach(service -> services.add(service.toJsonObject()));
        response.add("services", services);

        context.status(200);
        context.json(response.toString());
    }
}
