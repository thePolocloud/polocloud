package dev.httpmarco.polocloud.rest.endpoints.impl.v1.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.httpmarco.polocloud.rest.RestAPI;
import dev.httpmarco.polocloud.rest.endpoints.Endpoint;
import io.javalin.http.Context;

public class NodeEndpoint extends Endpoint {

    public NodeEndpoint(RestAPI restAPI) {
        super("nodes", restAPI);
    }

    @Override
    public void get(Context context) {
        if (!tokenProvided(context)) return;
        var user = handelAuth(context, getToken(context));
        if (user == null) return;
        if (!hasPermission(user, "polocloud.rest.endpoint.nodes", context)) return;

        var response = new JsonObject();
        var nodes = new JsonArray();

        //CloudAPI.instance().nodeService().localNode()..forEach(group -> nodes.add(group.toJsonObject()));
        response.add("nodes", nodes);

        context.status(200);
        context.json(response.toString());
    }
}
