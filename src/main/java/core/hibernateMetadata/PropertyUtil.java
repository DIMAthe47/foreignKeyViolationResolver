package core.hibernateMetadata;

import org.hibernate.mapping.*;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PropertyUtil {
    public static Set<Property> findPropertiesWithForeignKeysReferencingEntity(Class<?> referencedClass) {
        Set<Property> propertiesWithForeignKeys = new HashSet<>();
        for (PersistentClass persistentClass : MetadataExtractorIntegrator.INSTANCE.getMetadata().getEntityBindings()) {
            for (Iterator<Property> propertyIterator = persistentClass.getPropertyIterator(); propertyIterator.hasNext(); ) {
                Property property = propertyIterator.next();
                propertiesWithForeignKeys.addAll(findPropertiesWithForeignKeysReferencingEntity(referencedClass, property));
            }
        }
        return propertiesWithForeignKeys;
    }

    public static Set<Property> findPropertiesWithForeignKeysReferencingEntity(Class<?> referencedClass, Property property) {
        Set<Property> propertiesWithForeignKeys = new HashSet<>();
        String referencedEntityName = referencedClass.getName();
        Value value = property.getValue();
        if (value instanceof ManyToOne) {
            ManyToOne manyToOne = (ManyToOne) value;
            if (referencedEntityName.equals(manyToOne.getReferencedEntityName())) {
                propertiesWithForeignKeys.add(property);
            }
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            Table table = collection.getCollectionTable();
            for (ForeignKey foreignKey : table.getForeignKeys().values()) {
                if (foreignKey.getReferencedEntityName().equals(referencedClass.getName())) {
                    propertiesWithForeignKeys.add(property);
                }
            }
            propertiesWithForeignKeys.add(property);
        } else if (value instanceof Component) {
            //embeddable properties will not be included
            //but their content will (if they have such associations)
            Component component = (Component) value;
            for (Iterator<Property> propertyIterator = component.getPropertyIterator(); propertyIterator.hasNext(); ) {
                Property property1 = propertyIterator.next();
                propertiesWithForeignKeys.addAll(findPropertiesWithForeignKeysReferencingEntity(referencedClass, property1));
            }
        }
        return propertiesWithForeignKeys;
    }

    public static boolean isCollection(Property property) {
        Value value = property.getValue();
        if (value instanceof Collection) {
            return true;
        }
        return false;
    }

    public static int replacePropertyValue(Class<?> referencedClass, Property property, Object oldValue, Object newValue, EntityManager entityManager) {
        ForeignKey foreignKey = ForeignKeyUtil.extractForeignKeyFromProperty(referencedClass, property);
        int count = ForeignKeyUtil.replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
        return count;
    }

    public static int deletePropertyValue(Class<?> referencedClass, Property property, Object oldValue, EntityManager entityManager) {
        ForeignKey foreignKey = ForeignKeyUtil.extractForeignKeyFromProperty(referencedClass, property);
        int count = ForeignKeyUtil.deleteRowsWithForeignKeyValue(foreignKey, oldValue, entityManager);
        return count;
    }

    public static int deleteIfCollectionElseReplacePropertyValue(Class<?> referencedClass, Property property, Object oldValue, Object newValue, EntityManager entityManager) {
        ForeignKey foreignKey = ForeignKeyUtil.extractForeignKeyFromProperty(referencedClass, property);
        int count;
        if (newValue == null) {
            boolean updateElseDelete = foreignKey.getColumn(0).isNullable();
//            boolean updateElseDelete = isCollection(property);
            if (isCollection(property)) {
                count = ForeignKeyUtil.deleteRowsWithForeignKeyValue(foreignKey, oldValue, entityManager);
            } else {
                count = ForeignKeyUtil.replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
            }
        } else {
            count = ForeignKeyUtil.replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
        }
        return count;
    }
}
