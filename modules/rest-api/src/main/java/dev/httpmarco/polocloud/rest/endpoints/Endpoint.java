package dev.httpmarco.polocloud.rest.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.rest.RestAPI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public abstract class Endpoint implements EndpointMethods {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final String path;
    private final RestAPI restAPI;

    public JsonObject parseJSON(String body) {
        return new JsonObject().getAsJsonObject(body);
    }
}
