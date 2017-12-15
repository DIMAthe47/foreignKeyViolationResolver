import model.A;
import model.B;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

public class GenerateForeignKeyViolationException {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NewPersistenceUnit");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        A a = new A();
        B b = new B();
        a.setB(b);

        em.persist(a);
        em.persist(b);
        em.remove(b);

        try {
            em.getTransaction().commit();
        } catch (Exception e) {
            System.out.println(e);
            PersistenceException persistenceException = (PersistenceException) e.getCause();
            ConstraintViolationException constraintViolationException = (ConstraintViolationException) persistenceException.getCause();
            PSQLException psqlException = (PSQLException) constraintViolationException.getSQLException();
            ServerErrorMessage serverErrorMessage = psqlException.getServerErrorMessage();
            System.out.println(serverErrorMessage);
        }
        em.close();
        emf.close();
    }
}
