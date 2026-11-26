package com.optigro.models;

import java.util.Objects;

public class Substitution{
    private final int originalProductId;
    private final int substituteProductId;
    private final String originalName ; 
    private final String substituteName;
    private final boolean accepted;

    public  Substitution(int originalProductId ,int substituteProductId, String originalName , String substituteName , boolean accepted ){
        this.originalProductId = originalProductId;
        this.substituteProductId = substituteProductId;
        this.originalName = originalName;
        this.substituteName = substituteName;
        this.accepted = accepted;
    }

    public int getOriginalProductId() { return originalProductId; }
    public int getSubstituteProductId() { return substituteProductId; }
    public String getOriginalName() { return originalName; }
    public String getSubstituteName() { return substituteName; }
    public boolean isAccepted() { return accepted; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Substitution)) return false;
        Substitution that = (Substitution) o;
        return originalProductId == that.originalProductId &&
               substituteProductId == that.substituteProductId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalProductId, substituteProductId);
    }

    @Override
    public String toString() {
        return String.format("Substitution: %s (ID: %d) -> %s (ID: %d) [%s]",
            originalName, originalProductId, substituteName, substituteProductId,
            accepted ? "ACCEPTED" : "DECLINED");
    }

}