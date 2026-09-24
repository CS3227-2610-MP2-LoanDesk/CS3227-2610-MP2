package loandesk.domain;

public record Equipment(String id, String name, EquipmentCondition condition) {
    public Equipment {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Equipment ID must not be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Equipment name must not be blank.");
        }
        if (condition == null) {
            throw new IllegalArgumentException("Equipment condition is required.");
        }
    }

    public Equipment(String id, String name) {
        this(id, name, EquipmentCondition.GOOD);
    }
}
