import core.jpaMetaModel.AttributePath;
import core.hibernateMetadata.MetadataExtractorIntegratorProvider;
import core.jpaMetaModel._FiascoWithJpaMetaModel;
import model.B;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.properties;

public class TestJpaMetaModel {

    @Test
    void testGetPathes() {
        Map<String, IntegratorProvider> properties = new HashMap<>();
        properties.put("hibernate.integrator_provider", new MetadataExtractorIntegratorProvider());
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        List<AttributePath> attributePaths = _FiascoWithJpaMetaModel.findReferencingPathes(B.class, em);
        Long oldValue = 1L;
        Long newValue = oldValue + 1L;
        _FiascoWithJpaMetaModel.countReferencingEntities(attributePaths, oldValue, em);
        _FiascoWithJpaMetaModel.replaceReferencingEntities(attributePaths, oldValue, newValue, em);
        em.close();
        emf.close();
    }
}
