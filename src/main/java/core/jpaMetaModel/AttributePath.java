package core.jpaMetaModel;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.stream.Collectors;

public class AttributePath {
    private ManagedType managedType;
    private final List<Attribute> attributeList;

    public ManagedType getManagedType() {
        return managedType;
    }

    public List<Attribute> getAttributeList() {
        return attributeList;
    }

    public void setManagedType(ManagedType managedType) {
        this.managedType = managedType;
    }

    @Override
    public String toString() {
        String managedTypeString = managedType != null ? managedType.getJavaType().toString() : "";
        String attributeListString = attributeList != null ? attributeList.stream().map((att) -> att.getName()).collect(Collectors.joining(",")) : "";
//            String attributeString = attribute != null ? attribute.getName() : "";
//            return String.format("AttributePath: %s %s %s", managedTypeString, attributeListString, attributeString);
        return String.format("AttributePath: %s %s", managedTypeString, attributeListString);
    }

    public AttributePath(List<Attribute> attributeList) {
        this.attributeList = attributeList;
    }
}
