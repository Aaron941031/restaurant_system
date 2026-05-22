package group19.restaurant_system.dto;

import java.util.List;

public class ExclusionBatchResult {
    private List<String> added;
    private List<String> existing;
    private List<String> notFound;

    public ExclusionBatchResult() {}

    public ExclusionBatchResult(List<String> added, List<String> existing, List<String> notFound) {
        this.added = added;
        this.existing = existing;
        this.notFound = notFound;
    }

    public List<String> getAdded() { return added; }
    public void setAdded(List<String> added) { this.added = added; }

    public List<String> getExisting() { return existing; }
    public void setExisting(List<String> existing) { this.existing = existing; }

    public List<String> getNotFound() { return notFound; }
    public void setNotFound(List<String> notFound) { this.notFound = notFound; }
}
