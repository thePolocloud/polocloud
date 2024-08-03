package dev.httpmarco.polocloud.node.platforms.util;

import com.google.gson.*;
import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.platforms.PlatformType;
import dev.httpmarco.polocloud.node.platforms.PlatformVersion;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Set;

public class PlatformTypeAdapter implements JsonDeserializer<Platform>, JsonSerializer<Platform> {

    public static PlatformTypeAdapter INSTANCE = new PlatformTypeAdapter();

    @Override
    public Platform deserialize(JsonElement json, Type typeOfT, @NotNull JsonDeserializationContext context) throws JsonParseException {
        var object = (JsonObject) json;

        var name = object.get("platform").getAsString();

        //todo remove try catch
        PlatformType type;
        try {
            type = PlatformType.valueOf(object.get("type").getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            type = PlatformType.SERVER;
        }

        PlatformVersion[] versions = context.deserialize(object.get("versions"), PlatformVersion[].class);
        var platform = new Platform(name, type, Set.of(versions));

        if (object.has("patcher")) {
            //todo
            platform.platformPatcher(null);
        }

        if (object.has("startArguments")) {
            platform.startArguments(context.deserialize(object.getAsJsonArray("startArguments"), String[].class));
        }


        return platform;
    }

    @Override
    public JsonElement serialize(Platform src, Type typeOfSrc, JsonSerializationContext context) {
        var object = new JsonObject();
        object.addProperty("platform", src.platform());
        object.addProperty("type", src.type().name());

        if (src.platformPatcher() != null) {
            //todo add the platform patcher
        }

        if (src.startArguments() != null) {
            object.add("startArguments", context.serialize(src.startArguments()));
        }

        object.add("versions", context.serialize(src.versions()));
        return object;
    }
}
