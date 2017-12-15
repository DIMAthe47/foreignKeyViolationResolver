package core.hibernateMetadata;

import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataExtractorIntegratorProvider implements IntegratorProvider {

    protected Integrator integrator() {
        return MetadataExtractorIntegrator.INSTANCE;
    }

    @Override
    public List<Integrator> getIntegrators() {
        return Arrays.asList(integrator());
    }

    public static Map<String, Object> properties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.integrator_provider", new MetadataExtractorIntegratorProvider());
        properties.put("javax.persistence.schema-generation.database.action", "none");
        return properties;
    }

    public static Map<String, Object> propertiesWithDropCreate() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.integrator_provider", new MetadataExtractorIntegratorProvider());
        properties.put("javax.persistence.schema-generation.database.action", "drop-and-create");
        return properties;
    }

}
