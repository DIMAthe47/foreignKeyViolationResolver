package core;

import model.A;
import model.B;
import model.E;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.properties;
import static core.hibernateMetadata.MetadataExtractorIntegratorProvider.propertiesWithDropCreate;

public class DataUtil {

    public static final int entitiesCount = 20;
    public static final int foreignKeysReferencingBCount = 6;

    public static void populate() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", properties());
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        for (int i = 0; i < entitiesCount; i++) {
            A a = new A();
            B b = new B();
            a.setB(b);
            a.setB2(b);
            a.getE().setBInEmbeddable(b);
            a.getBSet().add(b);
            a.getB2Set().add(b);
            E e = new E();
            e.setBInEmbeddable(b);
            a.getESet().add(e);
            em.persist(a);
            em.persist(b);
        }

        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    public static void clean() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit", propertiesWithDropCreate());
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("DELETE FROM a___e").executeUpdate();
        em.createNativeQuery("DELETE FROM a___b").executeUpdate();
        em.createQuery("DELETE FROM A").executeUpdate();
        em.createQuery("DELETE FROM B").executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
}
