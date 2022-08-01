package com.ollymonger.dynmap.portals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class LocationUtils {
    static Location getAverageLocation(List<Location> locations) {
        double averageX = locations.parallelStream().mapToDouble(l -> l.getX()).average().getAsDouble();
        double averageY = locations.parallelStream().mapToDouble(l -> l.getY()).average().getAsDouble();
        double averageZ = locations.parallelStream().mapToDouble(l -> l.getZ()).average().getAsDouble();
        return new Location(locations.get(0).getWorld(), averageX, averageY, averageZ);
    }
}

public class DynmapPortalsPlugin extends JavaPlugin implements Listener {
    static final String DYNMAP_PLUGIN_NAME = "dynmap";

    static final String SET_ID_PORTALS = "nether_portals";
    static final String SET_NAME_PORTALS = "Nether Portals";

    static final String SET_ID_PORTAL_EXCLUSIONS = "nether_portal_exclusions";
    static final String SET_NAME_PORTAL_EXCLUSIONS = "Nether Portal Exclusions";

    private MarkerAPI markerApi;
    private MarkerSet portalSet;
    private MarkerSet portalExclusionSet;
    private ArrayList<RegisteredPortal> registeredPortals = new ArrayList<RegisteredPortal>();

    private Type registeredPortalListType = new TypeToken<List<RegisteredPortal>>() {
    }.getType();
    private Gson gson =
            new GsonBuilder()
                    .registerTypeAdapter(registeredPortalListType, new RegisteredPortalSerializer())
                    .registerTypeAdapter(registeredPortalListType, new RegisteredPortalDeserializer())
                    .setPrettyPrinting()
                    .create();


    @Override
    public void onEnable() {
        getLogger().info("DynmapPortalsPlugin is now enabled");

        this.getServer().getPluginManager().registerEvents(this, this);
        loadPortals();
        this.initialiseMarkerApi();
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) throws IOException {
        getLogger().info("Portal created");
        List<BlockState> blocks = event.getBlocks();

        List<Location> obsidianLocations = blocks.parallelStream()
                .filter(b -> b.getType().equals(Material.OBSIDIAN))
                .map(b -> b.getLocation())
                .collect(Collectors.toList());

        RegisteredPortal portal = RegisteredPortal.create(obsidianLocations);
        this.registeredPortals.add(portal);

        String portalId = portal.getPortalId();
        String portalExclusion = portalId + "_exclusion";
        Location centralPoint = portal.getCentralPoint();

        String worldName = centralPoint.getWorld().getName();
        double x = centralPoint.getX();
        double y = centralPoint.getY();
        double z = centralPoint.getZ();

        this.portalSet.createMarker(
                portalId,
                "Nether Portal",
                worldName,
                x,
                y,
                z,
                markerApi.getMarkerIcon("portal"),
                true
        );

        CircleMarker exclusion = this.portalExclusionSet.createCircleMarker(
                portalExclusion,
                "Nether Portal Zone",
                true,
                worldName,
                x,
                y,
                z,
                512,
                512,
                true
        );

        exclusion.setFillStyle(0.3, 0x7931b0);
        exclusion.setLineStyle(1, 1, 0x7f09d9);

        getLogger().info("Created Nether Portal: " + portalId);
        getLogger().info("Created Nether Portal Exclusion: " + portalExclusion);

        writePortals();
    }

    private void writePortals() {
        try {
            Path path = Paths.get(getDataFolder().getAbsolutePath(), "portals.json");
            File file = new File(path.toString());
            file.getParentFile().mkdirs();
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

            os.write(gson.toJson(this.registeredPortals, registeredPortalListType));
            getLogger().info("portal done");

            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPortals() {
        try {
            Path path = Paths.get(getDataFolder().getAbsolutePath(), "portals.json");
            File file = new File(path.toString());
            JsonReader reader = new JsonReader(new FileReader(file));
            List<RegisteredPortal> data = gson.fromJson(reader, registeredPortalListType);

            getLogger().info(String.valueOf(data.size()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType().equals(Material.OBSIDIAN) == false) {
            // we only care about obsidian being broken
            return;
        }

        Location location = block.getLocation();

        Optional<RegisteredPortal> portal = this.registeredPortals.parallelStream()
                .filter(p -> p.isPartOfFrame(location))
                .findFirst();

        portal.ifPresent(p -> {
            String portalId = p.getPortalId();

            event.getPlayer().sendMessage("You destroyed portal " + portalId);

            Marker marker = this.portalSet.findMarker(portalId);
            CircleMarker exclusionMarker = this.portalExclusionSet.findCircleMarker(portalId + "_exclusion");

            marker.deleteMarker();
            exclusionMarker.deleteMarker();

            this.registeredPortals.remove(p);
            writePortals();
        });
    }

    private void initialiseMarkerApi() {
        if (Bukkit.getPluginManager().isPluginEnabled(DYNMAP_PLUGIN_NAME) == false) {
            throw new IllegalStateException("No Dynmap plugin found");
        }

        DynmapCommonAPI plugin = (DynmapCommonAPI) Bukkit.getPluginManager().getPlugin(DYNMAP_PLUGIN_NAME);

        if (plugin == null) {
            throw new IllegalStateException("No Dynmap plugin found");
        }

        this.markerApi = plugin.getMarkerAPI();
        getLogger().info("Marker API retrieved");

        this.portalSet = this.markerApi.getMarkerSet(SET_ID_PORTALS);

        if (this.portalSet == null) {
            getLogger().info("Portals Set not found, creating new set");
            this.portalSet = this.markerApi.createMarkerSet(SET_ID_PORTALS, SET_NAME_PORTALS, null, true);
        }
        this.portalExclusionSet = this.markerApi.getMarkerSet(SET_ID_PORTAL_EXCLUSIONS);

        if (this.portalExclusionSet == null) {
            getLogger().info("Exclusions Set not found, creating new set");
            this.portalExclusionSet = this.markerApi.createMarkerSet(SET_ID_PORTAL_EXCLUSIONS, SET_NAME_PORTAL_EXCLUSIONS, null, true);
        }

        this.portalSet.setHideByDefault(false);
        getLogger().info("Portals Set initialised");

        this.portalExclusionSet.setHideByDefault(true);
        getLogger().info("Exclusions set initialised");

    }
}
