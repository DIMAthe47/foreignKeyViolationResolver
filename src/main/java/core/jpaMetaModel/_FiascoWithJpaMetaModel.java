package core.jpaMetaModel;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.*;
import java.util.*;

public class _FiascoWithJpaMetaModel {
    //too difficult to handle all possibilities: embeddables, collections.
    //Its easy to SELECT, but updateQuery doesn`t permit joins, so how we can update collections?


    public static Path attributePathToPath(AttributePath attributePath, From from) {
        List<Attribute> intermediateAttributes = new ArrayList<>(attributePath.getAttributeList());
//        SingularAttribute lastAttribute = (SingularAttribute) intermediateAttributes.get(intermediateAttributes.size() - 1);
//        intermediateAttributes.remove(intermediateAttributes.size() - 1);
        Path referencingPath = from;
        for (Attribute attribute : intermediateAttributes) {
            //we use only embedded
            if (attribute instanceof SingularAttribute) {
                referencingPath = referencingPath.get((SingularAttribute) attribute);
            } else if (attribute instanceof PluralAttribute) {
                referencingPath = ((From) referencingPath).join(attribute.getName());
            }
        }
//        Path referencingPath = from.get(lastAttribute);

        return referencingPath;
    }

    public static List<AttributePath> findReferencingPathes(Class<?> referencedClass, EntityManager em) {
        List<AttributePath> attributePaths = new ArrayList<>();
        Set<EntityType<?>> allEntityTypes = em.getMetamodel().getEntities();
        for (EntityType entityType : allEntityTypes) {
            List<AttributePath> entityAttributePathes = findReferencingPathes(entityType, referencedClass, em);
            for (AttributePath attributePath : entityAttributePathes) {
                attributePath.setManagedType(entityType);
            }
            attributePaths.addAll(entityAttributePathes);
        }
        return attributePaths;
    }

    public static List<AttributePath> findReferencingPathes(ManagedType managedType, Class<?> referencedClass, EntityManager entityManager) {
        List<AttributePath> attributePaths = new ArrayList<>();
        Set<Attribute<?, ?>> attributes = managedType.getAttributes();
        for (Attribute<?, ?> attribute : attributes) {
            if (Attribute.PersistentAttributeType.MANY_TO_ONE.equals(attribute.getPersistentAttributeType())) {
                if (attribute.getJavaType().isAssignableFrom(referencedClass)) {
                    System.out.println(attribute);
                    attributePaths.add(new AttributePath(Arrays.asList(attribute)));
                }
            } else if (attribute instanceof PluralAttribute) {
                PluralAttribute pluralAttribute = (PluralAttribute) attribute;
                if (Attribute.PersistentAttributeType.ONE_TO_MANY.equals(attribute.getPersistentAttributeType()) ||
                        Attribute.PersistentAttributeType.MANY_TO_MANY.equals(attribute.getPersistentAttributeType())) {
                    Class attrClass = pluralAttribute.getBindableJavaType();
                    if (attrClass.isAssignableFrom(referencedClass)) {
                        System.out.println(attrClass);
                        attributePaths.add(new AttributePath(Arrays.asList(attribute)));
                    }
                } else if (Attribute.PersistentAttributeType.ELEMENT_COLLECTION.equals(attribute.getPersistentAttributeType())) {
                    List<AttributePath> embeddableAttributePathes = buildAttributePathesForEmbeddableAttribute(attribute, referencedClass, entityManager);
                    attributePaths.addAll(embeddableAttributePathes);
                }
            } else if (Attribute.PersistentAttributeType.EMBEDDED.equals(attribute.getPersistentAttributeType())) {
                List<AttributePath> embeddableAttributePathes = buildAttributePathesForEmbeddableAttribute(attribute, referencedClass, entityManager);
                attributePaths.addAll(embeddableAttributePathes);
            }
        }
        return attributePaths;
    }

    public static List<AttributePath> buildAttributePathesForEmbeddableAttribute(Attribute embeddableAtribute, Class<?> referencedClass, EntityManager entityManager) {
        List<AttributePath> attributePaths = new ArrayList<>();
        Class embeddableClass = embeddableAtribute.getJavaType();
        if (embeddableAtribute instanceof Bindable) {
            embeddableClass = ((Bindable) embeddableAtribute).getBindableJavaType();
        } else {
            System.out.println("WTF");
        }

        ManagedType embeddableType = entityManager.getMetamodel().managedType(embeddableClass);
        List<AttributePath> embeddableAttributePathes = findReferencingPathes(embeddableType, referencedClass, entityManager);
        for (AttributePath attributePath : embeddableAttributePathes) {
            List<Attribute> attributePathList = new ArrayList<>(attributePath.getAttributeList());
            attributePathList.add(0, embeddableAtribute);
            AttributePath newAttributePath = new AttributePath(attributePathList);
            attributePaths.add(newAttributePath);
        }
        return attributePaths;
    }

    public static Map<AttributePath, Long> countReferencingEntities(List<AttributePath> attributePaths, Object referencedValue, EntityManager entityManager) {
        Map<AttributePath, Long> counts = new HashMap<>();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        for (AttributePath attributePath : attributePaths) {
            CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
            EntityType entityType = (EntityType) attributePath.getManagedType();
            Root entity = criteriaQuery.from(entityType);
            criteriaQuery.select(criteriaBuilder.count(entity));

            Path referencingPath = attributePathToPath(attributePath, entity);

            criteriaQuery.where(criteriaBuilder.equal(referencingPath, referencedValue));
            Query query = entityManager.createQuery(criteriaQuery);
            Long result = (Long) query.getSingleResult();
            counts.put(attributePath, result);
        }
        return counts;
    }

    public static void replaceReferencingEntities(List<AttributePath> attributePaths, Object oldReferencedValue, Object newReferencedValue, EntityManager entityManager) {
        entityManager.getTransaction().begin();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        for (AttributePath attributePath : attributePaths) {
            CriteriaUpdate update = criteriaBuilder.createCriteriaUpdate(attributePath.getManagedType().getJavaType());
            EntityType entityType = (EntityType) attributePath.getManagedType();
            Root entity = update.from(entityType);

            Path referencingPath = attributePathToPath(attributePath, entity);

            update.set(referencingPath, newReferencedValue);
            update.where(criteriaBuilder.equal(referencingPath, oldReferencedValue));
            entityManager.createQuery(update).executeUpdate();
        }
        entityManager.getTransaction().commit();
    }

}
