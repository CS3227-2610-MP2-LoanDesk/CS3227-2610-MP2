final class ReturnService {
    boolean returnItem(boolean saveSnapshot, String condition) {
        return saveSnapshot && "GOOD".equals(condition);
    }
}
