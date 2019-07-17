package com.ollymonger.dynmap.portals;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.CircleMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class DynmapPortalsPlugin extends JavaPlugin implements Listener {
    static final String DYNMAP_PLUGIN_NAME = "dynmap";

    static final String SET_ID_PORTALS = "nether_portals";
    static final String SET_NAME_PORTALS = "Nether Portals";

    static final String SET_ID_PORTAL_EXCLUSIONS = "nether_portal_exclusions";
    static final String SET_NAME_PORTAL_EXCLUSIONS = "Nether Portal Exclusions";

    private MarkerAPI markerApi;
    private MarkerSet portalSet;
    private MarkerSet portalExclusionSet;

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
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i).getBlock();

            if (block.getType().name() != "FIRE") { //if portal block is not fire
                continue;//continue on until fire is hit
            }

            String worldName = block.getWorld().getName();
            float x = block.getX();
            float y = block.getY();
            float z = block.getZ();

            String portalID = String.format("portal_%s_%d_%d_%d", worldName, Math.round(x), Math.round(y), Math.round(z));
            String portalExclusion = String.format("portal_exclusion_%s_%d_%d_%d", worldName, Math.round(x), Math.round(y), Math.round(z));

            this.portalSet.createMarker(portalID, "Nether Portal", worldName, x, y, z, markerApi.getMarkerIcon("portal"), true);
            CircleMarker exclusion = this.portalExclusionSet.createCircleMarker(portalExclusion, "Nether Portal Zone", true, worldName, x, y, z, 512, 512, true);
            int fillcolour = Integer.parseInt("7931b0", 16);
            int linecolour = Integer.parseInt("7f09d9", 16);
            exclusion.setFillStyle(0.3, fillcolour);
            exclusion.setLineStyle(1, 1, linecolour);

            getLogger().info("Created Nether Portal: " + portalID);
            getLogger().info("Created Nether Portal Exclusion: " + portalExclusion);
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

        this.portalSet.setHideByDefault(true);
        getLogger().info("Portals Set initialised");

        this.portalExclusionSet.setHideByDefault(true);
        getLogger().info("Exclusions set initialised");

        }
        }