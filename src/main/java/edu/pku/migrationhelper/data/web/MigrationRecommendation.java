package edu.pku.migrationhelper.data.web;

import java.util.List;

/**
 * This class is used during RESTful service with the intention to be more human readable
 *    than LibraryMigrationCandidate
 */
public class MigrationRecommendation {

    private final String fromLib;
    private final String toLib;
    private final double confidence;
    private final int ruleCount;
    private final int messageCount;
    private final int apiCount;
    private final double ruleSupport;
    private final double messageSupport;
    private final double distanceSupport;
    private final double apiSupport;
    private final List<VersionControlReference> refs;

    public MigrationRecommendation(
            String fromLib,
            String toLib,
            double confidence,
            int ruleCount,
            int messageCount,
            int apiCount,
            double ruleSupport,
            double messageSupport,
            double distanceSupport,
            double apiSupport,
            List<VersionControlReference> refs
    ) {
        this.fromLib = fromLib;
        this.toLib = toLib;
        this.confidence = confidence;
        this.ruleCount = ruleCount;
        this.messageCount = messageCount;
        this.apiCount = apiCount;
        this.ruleSupport = ruleSupport;
        this.messageSupport = messageSupport;
        this.distanceSupport = distanceSupport;
        this.apiSupport = apiSupport;
        this.refs = refs;
    }

    public String getFromLib() {
        return fromLib;
    }

    public String getToLib() {
        return toLib;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getRuleCount() {
        return ruleCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public int getApiCount() {
        return apiCount;
    }

    public double getRuleSupport() {
        return ruleSupport;
    }

    public double getMessageSupport() {
        return messageSupport;
    }

    public double getDistanceSupport() {
        return distanceSupport;
    }

    public double getApiSupport() {
        return apiSupport;
    }

    public List<VersionControlReference> getRefs() {
        return refs;
    }
}
