package anonymous.migrationhelper.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * Created by xxx on 2020/2/7.
 */
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    public static String writeObjectAsString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("write object as json string fault", e);
        }
    }

    public static byte[] writeObjectAsBytes(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj).getBytes();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("write object as json bytes fault", e);
        }
    }

    public static <T> T readBytesAsObject(byte[] jsonBytes, TypeReference<T> typeReference){
        try {
            return objectMapper.readValue(jsonBytes, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("read json bytes as object fault", e);
        }
    }

    public static <T> T readStringAsObject(String jsonString, TypeReference<T> typeReference){
        try {
            return objectMapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("read json string as object fault", e);
        }
    }

    public static <T> T readBytesAsObject(byte[] jsonBytes, Class<T> clazz){
        try {
            return objectMapper.readValue(jsonBytes, clazz);
        } catch (IOException e) {
            throw new RuntimeException("read json bytes as object fault", e);
        }
    }

    public static <T> T readStringAsObject(String jsonString, Class<T> clazz){
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            throw new RuntimeException("read json string as object fault", e);
        }
    }
}
