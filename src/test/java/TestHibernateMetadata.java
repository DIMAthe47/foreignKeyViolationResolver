import core.DataUtil;
import core.hibernateMetadata.HibernateMetadataUtil;
import core.hibernateMetadata.MetadataExtractorIntegrator;
import model.B;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.properties;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHibernateMetadata {

    @BeforeEach
    public void initialize() {
        DataUtil.clean();
        DataUtil.populate();
    }

    @Test
    void printAllTables() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();

        for (Namespace namespace : MetadataExtractorIntegrator.INSTANCE.getDatabase().getNamespaces()) {
            for (Table table : namespace.getTables()) {
                System.out.println(String.format("Table %s has the following columns: %s",
                        table,
                        StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(table.getColumnIterator(), Spliterator.ORDERED), false)
                                .collect(Collectors.toList())));
                System.out.println(String.format("Table %s has the following foreignKeys: %s",
                        table,
                        StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(table.getForeignKeyIterator(), Spliterator.ORDERED), false)
                                .collect(Collectors.toList())));
                for (org.hibernate.mapping.ForeignKey foreignKey : table.getForeignKeys().values()) {
                    System.out.println(foreignKey);
                }
            }
        }
        em.close();
        emf.close();
    }

    @Test
    public void testFindPropertiesWithForeignKeysReferencingEntity() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        Set<Property> propertiesWithForeignKeys = HibernateMetadataUtil.findPropertiesWithForeignKeysReferencingEntity(B.class);
        System.out.println(propertiesWithForeignKeys);
        assertEquals(DataUtil.foreignKeysReferencingBCount, propertiesWithForeignKeys.size());
        em.close();
        emf.close();
    }

    @Test
    public void testFindForeignKeys() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        Set<ForeignKey> foreignKeys = HibernateMetadataUtil.findForeignKeysReferencingEntity(B.class);
        System.out.println(foreignKeys);
        assertEquals(DataUtil.foreignKeysReferencingBCount, foreignKeys.size());
        em.close();
        emf.close();
    }

    @Test
    public void testFindForeignKeysValueCounts() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        Set<ForeignKey> foreignKeys = HibernateMetadataUtil.findForeignKeysReferencingEntity(B.class);
        Long oldValue = 1L;
        Map<ForeignKey, Long> foreignKeyCounts = new HashMap<>();
        foreignKeys.forEach((fk) -> foreignKeyCounts.put(fk, HibernateMetadataUtil.foreignKeyValueCount(fk, oldValue, em)));
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
        Set<ForeignKey> foreignKeys = HibernateMetadataUtil.findForeignKeysReferencingEntity(B.class);
        Long oldValue = 1L;
        Long newValue = oldValue + 1L;
        em.getTransaction().begin();
        final int[] updatedCount = {0};
        foreignKeys.forEach((fk) -> {
            updatedCount[0] += HibernateMetadataUtil.replaceForeignKeyValue(fk, oldValue, newValue, em);
        });
        System.out.println(updatedCount[0]);
        assertEquals(DataUtil.foreignKeysReferencingBCount, updatedCount[0]);
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
}
