package works.hacker.model;

import works.hacker.mptt.MpttEntity;

import javax.persistence.*;

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
    return "TagTree{" +
        "id=" + id +
        ", name='" + name + '\'' +
        "} " + super.toString();
  }
}
