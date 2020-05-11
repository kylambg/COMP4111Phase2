package EventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

public enum ObjectMap {
    INSTANCE;
    private ObjectMapper objectMapper = new ObjectMapper();

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
