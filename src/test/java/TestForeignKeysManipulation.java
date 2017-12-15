import core.DataUtil;
import core.hibernateMetadata.ForeignKeyUtil;
import model.B;
import org.hibernate.mapping.ForeignKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.properties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestForeignKeysManipulation {

    @BeforeEach
    public void initialize() {
        DataUtil.clean();
        DataUtil.populate();
    }

    @Test
    public void testFindForeignKeys() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        Set<ForeignKey> foreignKeys = ForeignKeyUtil.findForeignKeysReferencingEntity(B.class);
        System.out.println(foreignKeys);
        assertEquals(DataUtil.foreignKeysReferencingBCount, foreignKeys.size());
        em.close();
        emf.close();
    }

    @Test
    public void testFindForeignKeysValueCounts() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        Set<ForeignKey> foreignKeys = ForeignKeyUtil.findForeignKeysReferencingEntity(B.class);
        Long oldValue = 1L;
        Map<ForeignKey, Long> foreignKeyCounts = new HashMap<>();
        foreignKeys.forEach((fk) -> foreignKeyCounts.put(fk, ForeignKeyUtil.foreignKeyValueCount(fk, oldValue, em)));
        System.out.println(foreignKeyCounts);
        for (Long count : foreignKeyCounts.values()) {
            assertEquals(1L, (long) count);
        }
        em.close();
        emf.close();
    }

    @Test
    public void testReplaceForeignKeysValues() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        int updatedCount = replaceForeignKeysValues(1L, 2L, false, em);
        assertEquals(DataUtil.foreignKeysReferencingBCount, updatedCount);
        em.close();
        emf.close();
    }

    @Test
    public void testReplaceForeignKeysValuesWithNull() {
        Executable executable = () -> {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
            EntityManager em = emf.createEntityManager();
            int updatedCount = replaceForeignKeysValues(1L, null, false, em);
            assertEquals(DataUtil.foreignKeysReferencingBCount, updatedCount);
            em.close();
            emf.close();
        };
        assertThrows(PersistenceException.class, executable);
    }

    @Test
    public void testReplaceOrDeleteForeignKeysValuesWithNull() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        int updatedCount = replaceForeignKeysValues(1L, null, true, em);
        assertEquals(DataUtil.foreignKeysReferencingBCount, updatedCount);
        em.close();
        emf.close();
    }

    int replaceForeignKeysValues(Long oldValue, Long newValue, boolean smart, EntityManager em) {
        Set<ForeignKey> foreignKeys = ForeignKeyUtil.findForeignKeysReferencingEntity(B.class);
        em.getTransaction().begin();
        final int[] updatedCount = {0};
        foreignKeys.forEach((fk) -> {
            updatedCount[0] += smart ? ForeignKeyUtil.replaceOrDeleteForeignKeyValue(fk, oldValue, newValue, em) : ForeignKeyUtil.replaceForeignKeyValue(fk, oldValue, newValue, em);
        });
        System.out.println(updatedCount[0]);
        em.getTransaction().commit();
        return updatedCount[0];
    }
}
