package dev.httpmarco.polocloud.rest.endpoints.impl.v1.user;

import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.rest.RestAPI;
import dev.httpmarco.polocloud.rest.endpoints.Endpoint;
import io.javalin.http.Context;

import java.util.UUID;

public class AuthEndpoint extends Endpoint {

    public AuthEndpoint(RestAPI restAPI) {
        super("login/", restAPI);
    }

    @Override
    public void post(Context context) {
        var result = new JsonObject();
        var body = parseJSON(context.body());
        if (body == null) {
            result.addProperty("message", "Invalid JSON format");
            context.status(400).result(result.toString());
            return;
        }

        if (!body.has("uuid") || !body.has("password")) {
            result.addProperty("message", "Missing required fields");
            context.status(400).result(result.toString());
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(body.get("uuid").getAsString());
        } catch (Exception e) {
            result.addProperty("message", "Can`t parse UUID");
            context.status(400).result(result.toString());
            return;
        }

        var user = restAPI().userManager().findeUserByUUID(uuid);

        if (user == null) {
            result.addProperty("message", "No User found");
            context.status(400).result(result.toString());
            return;
        }


        var ip = context.ip();
        var userAgent = context.userAgent();

        var validToken = restAPI().userManager().checkToken(user, ip);
        if (validToken != null) {
            result.addProperty("token", validToken);
            context.status(200).result(result.toString());
            return;
        }

        var future = restAPI().userManager().login(user, body.get("password").getAsString(), ip, userAgent);

        future.thenAcceptAsync(token -> {
            if (token == null) {
                result.addProperty("message", "Invalid credentials");
                context.status(401).result(result.toString());
                return;
            }

            result.addProperty("token", token);
            context.status(200).result(result.toString());
        }).exceptionally(ex -> {
            result.addProperty("message", "Internal server error");
            context.status(500).result(result.toString());
            return null;
        });

        future.join();
    }
}
