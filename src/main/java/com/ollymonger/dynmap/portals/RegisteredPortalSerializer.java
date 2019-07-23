package com.ollymonger.dynmap.portals;

import com.google.gson.*;
import org.bukkit.Location;

import java.lang.reflect.Type;
import java.util.List;

public class RegisteredPortalSerializer implements JsonSerializer<List<RegisteredPortal>> {
    @Override
    public JsonElement serialize(List<RegisteredPortal> src, Type typeOfSrc, JsonSerializationContext context) {
        try {
            JsonArray portalArray = new JsonArray();

            for (RegisteredPortal portal : src) {
                JsonObject portalObject = new JsonObject();

                portalObject.addProperty("id", portal.getPortalId());

                JsonArray frameBlockArray = new JsonArray();

                for (Location block : portal.getFrameBlocks()) {
                    JsonObject frameBlock = new JsonObject();
                    frameBlock.addProperty("x", block.getX());
                    frameBlock.addProperty("Y", block.getY());
                    frameBlock.addProperty("Z", block.getZ());
                    frameBlockArray.add(frameBlock);
                }
                portalArray.add(frameBlockArray);

                JsonObject centralBlock = new JsonObject();
                Location centralPoint = portal.getCentralPoint();
                centralBlock.addProperty("x", centralPoint.getX());
                centralBlock.addProperty("y", centralPoint.getY());
                centralBlock.addProperty("z", centralPoint.getZ());

                portalArray.add(centralBlock);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
