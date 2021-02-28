package cloud.gcp.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException() {
        super("Not records found");
    }
}
