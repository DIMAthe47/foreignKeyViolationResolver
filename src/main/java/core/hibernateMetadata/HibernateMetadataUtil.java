package core.hibernateMetadata;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;
import java.util.Map;
import java.util.Set;

public class HibernateMetadataUtil {

    public static Set<ForeignKey> findForeignKeysReferencingEntity(Class<?> referencedClass) {
        Set<ForeignKey> foreignKeys = new HashSet<>();
        for (Namespace namespace : MetadataExtractorIntegrator.INSTANCE.getDatabase().getNamespaces()) {
            for (Table table : namespace.getTables()) {
                for (org.hibernate.mapping.ForeignKey foreignKey : table.getForeignKeys().values()) {
                    if (foreignKey.getReferencedEntityName().equals(referencedClass.getName())) {
                        foreignKeys.add(foreignKey);
                    }
                }
            }
        }
        return foreignKeys;
    }

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

    //expecting many foreign keys in one table - so we need foreignKeyName
    public static ForeignKey findForeignKeyInTableForManyToOne(String foreignKeyName, Table table) {
        for (org.hibernate.mapping.ForeignKey foreignKey : table.getForeignKeys().values()) {
            if (foreignKey.getName().equals(foreignKeyName)) {
                return foreignKey;
            }
        }
        return null;
    }

    //expecting only one FK per Entity for regular ManyToMany table
    public static ForeignKey findForeignKeyInTableForManyToMany(String referencedEntityName, Table table) {
        for (org.hibernate.mapping.ForeignKey foreignKey : table.getForeignKeys().values()) {
            if (foreignKey.getReferencedEntityName().equals(referencedEntityName)) {
                return foreignKey;
            }
        }
        return null;
    }

    public static ForeignKey extractForeignKeyFromProperty(Class<?> referencedClass, Property property) {
        String referencedEntityName = referencedClass.getName();
        Value value = property.getValue();
        if (value instanceof ManyToOne) {
            ManyToOne manyToOne = (ManyToOne) value;
            ForeignKey foreignKey = findForeignKeyInTableForManyToOne(manyToOne.getForeignKeyName(), manyToOne.getTable());
            return foreignKey;
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            Table table = collection.getCollectionTable();
            ForeignKey foreignKey = findForeignKeyInTableForManyToMany(referencedEntityName, table);
            return foreignKey;
        }
        return null;
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
            for (org.hibernate.mapping.ForeignKey foreignKey : table.getForeignKeys().values()) {
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

    public static Long foreignKeyValueCount(ForeignKey foreignKey, Object value, EntityManager entityManager) {
        Map<ForeignKey, Long> counts = new HashMap<>();
        String tableName = foreignKey.getTable().getName();
        String schemaName = foreignKey.getTable().getSchema();
        String columnName = foreignKey.getColumn(0).getQuotedName();
        String queryPattern = String.format("SELECT COUNT(*) FROM %s.%s WHERE %s=?", schemaName, tableName, columnName);
        Query q = entityManager.createNativeQuery(queryPattern);
        q.setParameter(1, value);
        Long count = ((Number) q.getSingleResult()).longValue();
        return count;
    }

    public static int replaceForeignKeyValue(ForeignKey foreignKey, Object oldValue, Object newValue, EntityManager entityManager) {
        String queryPattern = queryPattern(foreignKey, true);
        Query q = entityManager.createNativeQuery(queryPattern);
        q.setParameter(1, newValue);
        q.setParameter(2, oldValue);
        int count = q.executeUpdate();
        return count;
    }

    public static int deleteRowsWithForeignKeyValue(ForeignKey foreignKey, Object oldValue, EntityManager entityManager) {
        String queryPattern = queryPattern(foreignKey, false);
        Query q = entityManager.createNativeQuery(queryPattern);
        q.setParameter(1, oldValue);
        int count = q.executeUpdate();
        return count;
    }

    public static int replacePropertyValue(Class<?> referencedClass, Property property, Object oldValue, Object newValue, EntityManager entityManager) {
        ForeignKey foreignKey = extractForeignKeyFromProperty(referencedClass, property);
        int count = replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
        return count;
    }

    public static int deletePropertyValue(Class<?> referencedClass, Property property, Object oldValue, EntityManager entityManager) {
        ForeignKey foreignKey = extractForeignKeyFromProperty(referencedClass, property);
        int count = deleteRowsWithForeignKeyValue(foreignKey, oldValue, entityManager);
        return count;
    }

    public static int deleteIfCollectionElseReplacePropertyValue(Class<?> referencedClass, Property property, Object oldValue, Object newValue, EntityManager entityManager) {
        ForeignKey foreignKey = extractForeignKeyFromProperty(referencedClass, property);
        int count;
        if (isCollection(property)) {
            count = deleteRowsWithForeignKeyValue(foreignKey, oldValue, entityManager);
        } else {
            count = replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
        }
        return count;
    }

    public static String queryPattern(ForeignKey foreignKey, boolean updateElseDelete) {
        String tableName = foreignKey.getTable().getName();
        String schemaName = foreignKey.getTable().getSchema();
        String columnName = foreignKey.getColumn(0).getQuotedName();
        String queryPattern;
        if (updateElseDelete)
            queryPattern = String.format("UPDATE %s.%s SET %s=? WHERE %s=?", schemaName, tableName, columnName, columnName);
        else
            queryPattern = String.format("DELETE FROM %s.%s WHERE %s=?", schemaName, tableName, columnName, columnName);
        return queryPattern;
    }

}
