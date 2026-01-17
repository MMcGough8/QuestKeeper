package com.questkeeper.campaign;

import java.nio.file.Path;

/**
 * Lightweight record containing basic campaign metadata for discovery and selection.
 *
 * This is used by the campaign selection UI to display available campaigns
 * without loading all campaign content.
 *
 * @author Marc McGough
 * @version 1.0
 */
public record CampaignInfo(
    String id,
    String name,
    String description,
    String author,
    String version,
    Path path
) {
    /**
     * Creates a CampaignInfo with required fields.
     */
    public CampaignInfo {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Campaign ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campaign name cannot be null or blank");
        }
        if (path == null) {
            throw new IllegalArgumentException("Campaign path cannot be null");
        }
    }

    /**
     * Returns a formatted display string for campaign selection.
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (version != null && !version.isBlank()) {
            sb.append(" v").append(version);
        }
        if (author != null && !author.isBlank()) {
            sb.append(" by ").append(author);
        }
        return sb.toString();
    }

    /**
     * Returns a short summary of the campaign.
     */
    public String getSummary() {
        if (description != null && !description.isBlank()) {
            // Truncate long descriptions
            if (description.length() > 100) {
                return description.substring(0, 97) + "...";
            }
            return description;
        }
        return "No description available.";
    }
}
