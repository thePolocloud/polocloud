package dev.httpmarco.polocloud.rest.endpoints;

import dev.httpmarco.polocloud.rest.RestAPI;
import dev.httpmarco.polocloud.rest.endpoints.impl.v1.service.ServicesEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public class EndpointServiceProvider implements EndpointService {

    private final List<Endpoint> endpoints = new ArrayList<>();
    private static final String API_PATH = "/api/v1/polocloud/";
    private final RestAPI restAPI;

    @Override
    public void load() {
        registerEndpoint(new ServicesEndpoint(this.restAPI));

        var javalin = this.restAPI.javalin();
        this.endpoints.forEach(endpoint -> {
            //TODO auth
            javalin.post(API_PATH + endpoint.path(), endpoint::post);
            javalin.put(API_PATH + endpoint.path(), endpoint::put);
            javalin.get(API_PATH + endpoint.path(), endpoint::get);
            javalin.delete(API_PATH + endpoint.path(), endpoint::delete);
        });
    }

    @Override
    public void registerEndpoint(Endpoint endpoint) {
        this.endpoints.add(endpoint);
    }
}
