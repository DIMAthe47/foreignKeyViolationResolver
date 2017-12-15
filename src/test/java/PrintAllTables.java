import core.DataUtil;
import core.hibernateMetadata.MetadataExtractorIntegrator;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.properties;

public class PrintAllTables {

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
}
