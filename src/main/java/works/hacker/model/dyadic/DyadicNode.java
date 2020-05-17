package works.hacker.model.dyadic;

import works.hacker.mptt.classic.MpttEntity;
import works.hacker.mptt.dyadic.DyadicEntity;
import works.hacker.repo.classic.MpttNodeRepository;
import works.hacker.repo.classic.MpttNodeRepositoryCustom;
import works.hacker.repo.classic.MpttNodeRepositoryImpl;

import javax.persistence.*;
import java.util.Objects;

/**
 * Demo usage of the {@link DyadicEntity}.
 * <p>
 * Also used in the unit / integrations tests, as the {@link MpttEntity} is annotated
 * with {@link MappedSuperclass} and can not be used standalone.
 *
 * @see MpttNodeRepositoryCustom
 * @see MpttNodeRepositoryImpl
 * @see MpttNodeRepository
 * @see <a href="https://github.com/hacker-works/mptt-jpa">README</a>
 */
@Entity
public class DyadicNode extends DyadicEntity {
  private static final String NO_NAME = "NO_NAME";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private final String name;

  @SuppressWarnings({"Unused"})
  public DyadicNode() {
    super();
    this.name = NO_NAME;
  }

  public DyadicNode(String name) {
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
    return String.format("%s (id: %d) %s", getName(), getId(), super.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }
}
