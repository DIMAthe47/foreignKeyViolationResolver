package model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Entity
@Table(schema = "public", name = "b")
@Data
@EqualsAndHashCode(callSuper = false)
public class B {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Id()
    private Long id;

    @Column(name = "name")
    private Long name;
}
