final class ReturnService {
    boolean returnItem(boolean saveLoan, boolean saveEquipment, String condition) {
        if (!saveLoan) return false;
        return saveEquipment && !"LOST".equals(condition);
    }
}
