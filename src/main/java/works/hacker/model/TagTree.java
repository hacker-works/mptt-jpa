package works.hacker.model;

import works.hacker.mptt.MpttEntity;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class TagTree extends MpttEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String name;

  public TagTree(String name) {
    super();
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("%s (id: %d) %s", getName(),  getId(), super.toString());
  }

  @Override
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }
}
