package daycare.service;

public class DaycareResult<T> {
    private final boolean success;
    private final String message;
    private final T value;

    private DaycareResult(boolean success, String message, T value) {
        this.success = success;
        this.message = message;
        this.value = value;
    }

    public static <T> DaycareResult<T> success(String message, T value) {
        return new DaycareResult<>(true, message, value);
    }

    public static <T> DaycareResult<T> failure(String message) {
        return new DaycareResult<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getValue() {
        return value;
    }
}
