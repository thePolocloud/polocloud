package dev.httpmarco.polocloud.node.platforms.util;

import com.google.gson.*;
import dev.httpmarco.polocloud.node.platforms.Platform;
import dev.httpmarco.polocloud.node.platforms.PlatformType;
import dev.httpmarco.polocloud.node.platforms.PlatformVersion;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class PlatformTypeAdapter implements JsonDeserializer<Platform>, JsonSerializer<Platform> {

    public static PlatformTypeAdapter INSTANCE = new PlatformTypeAdapter();

    @Override
    public Platform deserialize(JsonElement json, Type typeOfT, @NotNull JsonDeserializationContext context) throws JsonParseException {
        var object = (JsonObject) json;

        var name = object.get("platform").getAsString();
        var type = PlatformType.valueOf(object.get("platform").getAsString());

        var platform = new Platform(name, type, context.deserialize(object.get("versions"), PlatformVersion[].class));

        if(object.has("patcher")) {
            //todo
            platform.platformPatcher(null);
        }

        return platform;
    }

    @Override
    public JsonElement serialize(Platform src, Type typeOfSrc, JsonSerializationContext context) {
        var object = new JsonObject();

        return object;
    }
}
