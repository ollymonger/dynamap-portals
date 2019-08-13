package com.ollymonger.dynmap.portals;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RegisteredPortalDeserializer implements JsonDeserializer<List<RegisteredPortal>> {
    @Override
    public List<RegisteredPortal> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<RegisteredPortal> result = new ArrayList<>();
        JsonArray portals = json.getAsJsonArray();

        for(JsonElement portalElement : portals){
            JsonObject portalObject = portalElement.getAsJsonObject();
            String portalId = portalObject.get("id").getAsString();

            JsonObject centralPoint = portalObject.getAsJsonObject("centralPoint");
            double centralPointX = centralPoint.get("x").getAsDouble();
            double centralPointY = centralPoint.get("y").getAsDouble();
            double centralPointZ = centralPoint.get("z").getAsDouble();
            Location centralPointLocation = new Location(Bukkit.getWorld("world"), centralPointX, centralPointY, centralPointZ);

            JsonArray frameBlocks = portalObject.getAsJsonArray("frameBlocks");
            List<Location> allFrameBlocks = new ArrayList<Location>();
            for (JsonElement frameBlockElement : frameBlocks) {
                JsonObject frameBlockObject = frameBlockElement.getAsJsonObject();
                double frameBlockX = frameBlockObject.get("x").getAsDouble();
                double frameBlockY = frameBlockObject.get("y").getAsDouble();
                double frameBlockZ = frameBlockObject.get("z").getAsDouble();
                Location frameBlockLocation = new Location(Bukkit.getWorld("world"), frameBlockX, frameBlockY, frameBlockZ);
                allFrameBlocks.add(frameBlockLocation);

            }
            RegisteredPortal portal = new RegisteredPortal(portalId, centralPointLocation, allFrameBlocks);
            result.add(portal);
        }

        return result;
    }
}
