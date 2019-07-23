package com.ollymonger.dynmap.portals;


import org.bukkit.Location;

import java.util.List;

public class RegisteredPortal {
    private String portalId;
    private List<Location> frameBlocks;
    private Location centralPoint;

    public RegisteredPortal(String portalId, Location centralPoint, List<Location> frameBlocks) {
        this.portalId = portalId;
        this.centralPoint = centralPoint;
        this.frameBlocks = frameBlocks;
    }

    public static RegisteredPortal create(List<Location> frameBlocks){
        Location centralPoint = LocationUtils.getAverageLocation(frameBlocks);
        String portalId = String.format(
                "portal_%s_%d_%d_%d",
                centralPoint.getWorld().getName(),
                Math.round(centralPoint.getX()),
                Math.round(centralPoint.getY()),
                Math.round(centralPoint.getZ())
        );
        return new RegisteredPortal(portalId, centralPoint, frameBlocks);
    }

    public boolean isPartOfFrame(Location location) {
        return this.frameBlocks.parallelStream()
                .anyMatch(l -> l.equals(location));
    }

    public String getPortalId() {
        return this.portalId;
    }

    public Location getCentralPoint() { return this.centralPoint; }

    public List<Location> getFrameBlocks() {
        return this.frameBlocks;
    }
}
