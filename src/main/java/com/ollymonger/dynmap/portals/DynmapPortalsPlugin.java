package com.ollymonger.dynmap.portals;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.List;


public class DynmapPortalsPlugin extends JavaPlugin implements Listener {
    static final String DYNMAP_PLUGIN_NAME = "dynmap";

    static final String SET_ID_PORTALS = "nether_portals";
    static final String SET_NAME_PORTALS = "Nether Portals";

    static final String SET_ID_PORTAL_EXCLUSIONS = "nether_portal_exclusions";
    static final String SET_NAME_PORTAL_EXCLUSIONS = "Nether Portal Exclusions";

    private MarkerAPI markerApi;
    private MarkerSet portalSet;
    private MarkerSet portalExclusionSet;
    private List<BlockState> blockList;

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
        this.blockList = blocks;
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
            this.portalExclusionSet.createCircleMarker(portalExclusion, "Nether Portal Zone", true, worldName, x, y, z, 100, 100, true);
            getLogger().info("Created Nether Portal: " + portalID);
            getLogger().info("Created Nether Portal Exclusion: " + portalExclusion);
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType().equals(Material.NETHER_PORTAL) == false) {
            return;
        }

        // nether portal being affected by physics means it's broken
        //getLogger().info("Portal destroyed");
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

        this.portalExclusionSet.setHideByDefault(false);
        getLogger().info("Exclusions set initialised");

    }
}