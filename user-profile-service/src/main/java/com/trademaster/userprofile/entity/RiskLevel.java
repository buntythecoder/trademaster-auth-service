package com.trademaster.userprofile.entity;

public enum RiskLevel {
    LOW("Conservative", "Prefers capital preservation over growth", 1, 3),
    MODERATE("Moderate", "Balanced approach to risk and return", 4, 6),
    MEDIUM("Moderate", "Balanced approach to risk and return", 4, 6), // Alias for MODERATE
    HIGH("Aggressive", "Willing to take high risks for potentially high returns", 7, 8),
    VERY_HIGH("Very High", "Maximum risk tolerance for aggressive trading", 9, 10);
    
    private final String displayName;
    private final String description;
    private final int minScore;
    private final int maxScore;
    
    RiskLevel(String displayName, String description, int minScore, int maxScore) {
        this.displayName = displayName;
        this.description = description;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getMinScore() {
        return minScore;
    }
    
    public int getMaxScore() {
        return maxScore;
    }
    
    public boolean isValidScore(int score) {
        return score >= minScore && score <= maxScore;
    }
    
    public static RiskLevel fromScore(int score) {
        for (RiskLevel level : values()) {
            if (level.isValidScore(score)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid risk score: " + score);
    }
}