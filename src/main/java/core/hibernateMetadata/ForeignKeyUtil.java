package core.hibernateMetadata;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.HashSet;

public class ForeignKeyUtil {
    //expecting many foreign keys in one table - so we need foreignKeyName
    public static ForeignKey findForeignKeyInTableForManyToOne(String foreignKeyName, Table table) {
        for (ForeignKey foreignKey : table.getForeignKeys().values()) {
            if (foreignKey.getName().equals(foreignKeyName)) {
                return foreignKey;
            }
        }
        return null;
    }

    //expecting only one FK per Entity for regular ManyToMany table
    public static ForeignKey findForeignKeyInTableForManyToMany(String referencedEntityName, Table table) {
        for (ForeignKey foreignKey : table.getForeignKeys().values()) {
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

    @Deprecated
    //With foreign keys you cant figure out, is it in many-to-many table.
    //Because if it is, we want to delete this rows when setting null as new value.
    //Use properties+foreignKeys.
    //Another approach - delete if column is not nullable else update
    //Whether column is nullable we can obtain from foreign key. So maybe foreign key is enough.
    public static java.util.Set<ForeignKey> findForeignKeysReferencingEntity(Class<?> referencedClass) {
        java.util.Set<ForeignKey> foreignKeys = new HashSet<>();
        for (Namespace namespace : MetadataExtractorIntegrator.INSTANCE.getDatabase().getNamespaces()) {
            for (Table table : namespace.getTables()) {
                for (ForeignKey foreignKey : table.getForeignKeys().values()) {
                    if (foreignKey.getReferencedEntityName().equals(referencedClass.getName())) {
                        foreignKeys.add(foreignKey);
                    }
                }
            }
        }
        return foreignKeys;
    }

    @Deprecated
    //Use properties+foreignKeys.
    public static Long foreignKeyValueCount(ForeignKey foreignKey, Object value, EntityManager entityManager) {
        java.util.Map<ForeignKey, Long> counts = new HashMap<>();
        String queryPattern = ForeignKeyUtil.queryPattern(foreignKey, "count");
        Query q = entityManager.createNativeQuery(queryPattern);
        q.setParameter(1, value);
        Long count = ((Number) q.getSingleResult()).longValue();
        return count;
    }

    //expecting one column in foreign key
    public static String queryPattern(ForeignKey foreignKey, String count_update_delete_setnull) {
        String tableName = foreignKey.getTable().getName();
        String schemaName = foreignKey.getTable().getSchema();
        String columnName = foreignKey.getColumn(0).getQuotedName();
        String queryPattern;
        if (count_update_delete_setnull.equals("count"))
            queryPattern = String.format("SELECT COUNT(*) FROM %s.%s WHERE %s=?", schemaName, tableName, columnName);
        else if (count_update_delete_setnull.equals("update"))
            queryPattern = String.format("UPDATE %s.%s SET %s=? WHERE %s=?", schemaName, tableName, columnName, columnName);
        else if (count_update_delete_setnull.equals("setnull"))
            queryPattern = String.format("UPDATE %s.%s SET %s=NULL WHERE %s=?", schemaName, tableName, columnName, columnName);
        else
            queryPattern = String.format("DELETE FROM %s.%s WHERE %s=?", schemaName, tableName, columnName, columnName);
        return queryPattern;
    }

    public static int replaceForeignKeyValue(ForeignKey foreignKey, Object oldValue, Object newValue, EntityManager entityManager) {
        String queryPattern;
        Query q;
        if (newValue == null) {
            queryPattern = queryPattern(foreignKey, "setnull");
            q = entityManager.createNativeQuery(queryPattern);
            q.setParameter(1, oldValue);
        } else {
            queryPattern = queryPattern(foreignKey, "update");
            q = entityManager.createNativeQuery(queryPattern);
            q.setParameter(1, newValue);
            q.setParameter(2, oldValue);
        }
        int count = q.executeUpdate();
        return count;
    }

    public static int replaceOrDeleteForeignKeyValue(ForeignKey foreignKey, Object oldValue, Object newValue, EntityManager entityManager) {
        int count;
        if (newValue == null) {
            boolean updateElseDelete = foreignKey.getColumn(0).isNullable();
//            boolean updateElseDelete = isCollection(property);
            if (updateElseDelete) {
                count = ForeignKeyUtil.replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
            } else {
                count = ForeignKeyUtil.deleteRowsWithForeignKeyValue(foreignKey, oldValue, entityManager);
            }
        } else {
            count = ForeignKeyUtil.replaceForeignKeyValue(foreignKey, oldValue, newValue, entityManager);
        }
        return count;
    }

    public static int deleteRowsWithForeignKeyValue(ForeignKey foreignKey, Object oldValue, EntityManager entityManager) {
        String queryPattern = queryPattern(foreignKey, "delete");
        Query q = entityManager.createNativeQuery(queryPattern);
        q.setParameter(1, oldValue);
        int count = q.executeUpdate();
        return count;
    }
}
