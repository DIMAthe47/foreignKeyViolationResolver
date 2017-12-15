package model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(schema = "public", name = "a")
@Data
@EqualsAndHashCode(callSuper = false)
public class A {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Id()
    private Long id;

    @Column(name = "name")
    private Long name;

    @ManyToOne
    @JoinColumn(name = "b_id", foreignKey = @ForeignKey(name = "b_fkey"))
    private B b;

    @ManyToOne
    @JoinColumn(name = "b2_id", foreignKey = @ForeignKey(name = "b2_fkey"))
    private B b2;

    @Embedded
    private E e = new E();

    @ManyToMany
    @JoinTable(schema = "public", name = "a___b", inverseJoinColumns = {@JoinColumn(name = "b_id", foreignKey = @ForeignKey(name = "b_fkey"))}, joinColumns = {@JoinColumn(name = "a_id", foreignKey = @ForeignKey(name = "a_fkey"))})
    private Set<B> bSet = new HashSet<>();

    @ManyToMany
    @JoinTable(schema = "public", name = "a___b2", inverseJoinColumns = {@JoinColumn(name = "b_id", foreignKey = @ForeignKey(name = "b_fkey"))}, joinColumns = {@JoinColumn(name = "a_id", foreignKey = @ForeignKey(name = "a_fkey"))})
    private Set<B> b2Set = new HashSet<>();

    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "a_fkey"), schema = "public", name = "a___e", joinColumns = @JoinColumn(name = "a_id", foreignKey = @ForeignKey(name = "a_fkey")))
    private Set<E> eSet = new HashSet<>();
}
