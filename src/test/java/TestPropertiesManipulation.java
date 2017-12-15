import core.DataUtil;
import core.hibernateMetadata.PropertyUtil;
import model.B;
import org.hibernate.mapping.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Set;

import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.properties;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPropertiesManipulation {

    @BeforeEach
    public void initialize() {
        DataUtil.clean();
        DataUtil.populate();
    }

    @Test
    public void testFindPropertiesWithForeignKeysReferencingEntity() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        Set<Property> propertiesWithForeignKeys = PropertyUtil.findPropertiesWithForeignKeysReferencingEntity(B.class);
        System.out.println(propertiesWithForeignKeys);
        assertEquals(DataUtil.foreignKeysReferencingBCount, propertiesWithForeignKeys.size());
        em.close();
        emf.close();
    }


}
