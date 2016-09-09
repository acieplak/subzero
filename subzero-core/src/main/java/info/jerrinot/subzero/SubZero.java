package info.jerrinot.subzero;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;

/**
 * Convenient class for dead-simple SubZero injection into Hazelcast configuration.
 *
 * This is the simplest way to use SubZero when you use Hazelcast programmatic configuration.
 *
 *
 */
public final class SubZero {

    private SubZero() {

    }

    /**
     * Use SubZero as a global serializer.
     *
     * This method configures Hazelcast to delegate a class serialization to SubZero when the class
     * has no explicit strategy configured.
     *
     * @param config Hazelcast configuration to inject SubZero into
     * @return Hazelcast configuration.
     */
    public static Config useAsGlobalSerializer(Config config) {
        SerializationConfig serializationConfig = config.getSerializationConfig();
        injectSubZero(serializationConfig);
        return config;
    }

    /**
     * Use SubZero as a global serializer.
     *
     * This method configures Hazelcast to delegate a class serialization to SubZero when the class
     * has no explicit strategy configured.
     *
     * This method it intended to be used to configure {@link ClientConfig} instances, but
     * I do not want to create a hard-dependency on Hazelcast Client module.
     *
     * @param config Hazelcast configuration to inject SubZero into
     * @return Hazelcast configuration.
     */
    public static <T> T useAsGlobalSerializer(T config) {
        String className = config.getClass().getName();
        SerializationConfig serializationConfig;
        if (className.equals("com.hazelcast.client.config.ClientConfig")) {
            ClientConfig clientConfig = (ClientConfig) config;
            serializationConfig = clientConfig.getSerializationConfig();
        } else if (className.equals("com.hazelcast.config.Config")) {
            Config memberConfig = (Config) config;
            serializationConfig = memberConfig.getSerializationConfig();
        } else {
            throw new IllegalArgumentException("Unknown configuration object " + config);
        }
        injectSubZero(serializationConfig);
        return config;
    }

    private static void injectSubZero(SerializationConfig serializationConfig) {
        GlobalSerializerConfig globalSerializerConfig = serializationConfig.getGlobalSerializerConfig();
        if (globalSerializerConfig == null) {
            globalSerializerConfig = new GlobalSerializerConfig();
            serializationConfig.setGlobalSerializerConfig(globalSerializerConfig);
        }
        globalSerializerConfig.setClassName(Serializer.class.getName()).setOverrideJavaSerialization(true);
    }

    /**
     * Use SubZero as a serializer for selected classes only.
     *
     * @param config Hazelcast configuration to inject SubZero into
     * @param classes classes Hazelcast should serialize via SubZero
     * @return Hazelcast configuration
     */
    public static Config useForClasses(Config config, Class<?>...classes) {
        SerializationConfig serializationConfig = config.getSerializationConfig();
        for (Class<?> clazz : classes) {
            SerializerConfig serializerConfig = new SerializerConfig();
            Serializer<?> serializer = new Serializer(clazz);
            serializerConfig.setImplementation(serializer);
            serializerConfig.setTypeClass(clazz);
            serializationConfig.addSerializerConfig(serializerConfig);
        }
        return config;
    }
}
