package model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Embeddable;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Data
@EqualsAndHashCode(callSuper = false)
@Embeddable
public class E {

    @ManyToOne
    @JoinColumn(name = "bInEmbeddable_id", foreignKey = @ForeignKey(name = "bInEmbeddable_fkey"))
    private B bInEmbeddable;
}
