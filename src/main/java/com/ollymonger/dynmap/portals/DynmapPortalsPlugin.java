package com.ollymonger.dynmap.portals;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

class RegisteredPortal {
    private String portalId;
    private List<Location> frameBlocks;
    private Location centralPoint;

    public RegisteredPortal(List<Location> frameBlocks) {
        this.frameBlocks = frameBlocks;
        this.centralPoint = LocationUtils.getAverageLocation(frameBlocks);

        this.portalId = String.format(
                "portal_%s_%d_%d_%d",
                this.centralPoint.getWorld().getName(),
                Math.round(this.centralPoint.getX()),
                Math.round(this.centralPoint.getY()),
                Math.round(this.centralPoint.getZ())
        );

    }

    public boolean isPartOfFrame(Location location) {
        return this.frameBlocks.parallelStream()
                .anyMatch(l -> l.equals(location));
    }

    public String getPortalId() {
        return this.portalId;
    }

    public Location getCentralPoint() {
        return this.centralPoint;
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

    @Override
    public void onEnable() {
        getLogger().info("DynmapPortalsPlugin is now enabled");

        this.getServer().getPluginManager().registerEvents(this, this);

        this.initialiseMarkerApi();
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        getLogger().info("Portal created");
        List<BlockState> blocks = event.getBlocks();

        List<Location> obsidianLocations = blocks.parallelStream()
                .filter(b -> b.getType().equals(Material.OBSIDIAN))
                .map(b -> b.getLocation())
                .collect(Collectors.toList());

        RegisteredPortal portal = new RegisteredPortal(obsidianLocations);
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
            CircleMarker marker2 = this.portalExclusionSet.findCircleMarker(portalId+"_exclusion");
            this.deleteMarker(marker);
            this.deleteCircleMarker(marker2);

        });
    }

    private void deleteMarker(Marker marker) {
        try {
            Method deleteMarker = marker.getClass().getDeclaredMethod("deleteMarker");
            deleteMarker.setAccessible(true);
            deleteMarker.invoke(marker);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void deleteCircleMarker(CircleMarker marker2) {
        try {
            Method deleteMarker = marker2.getClass().getDeclaredMethod("deleteMarker");
            deleteMarker.setAccessible(true);
            deleteMarker.invoke(marker2);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
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
