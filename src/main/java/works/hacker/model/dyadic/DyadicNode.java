package works.hacker.model.dyadic;

import works.hacker.mptt.TreeEntity;
import works.hacker.mptt.TreeRepository;
import works.hacker.mptt.dyadic.DyadicEntity;
import works.hacker.repo.dyadic.DyadicNodeRepository;
import works.hacker.repo.dyadic.DyadicNodeRepositoryCustom;
import works.hacker.repo.dyadic.DyadicNodeRepositoryImpl;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

/**
 * Demo usage of the {@link DyadicEntity}.
 * <p>
 * Also used in the unit / integrations tests, as the {@link DyadicEntity} is annotated
 * with {@link MappedSuperclass} and can not be used standalone.
 *
 * @see DyadicNodeRepositoryCustom
 * @see DyadicNodeRepositoryImpl
 * @see DyadicNodeRepository
 * @see TreeEntity
 * @see TreeRepository
 * @see <a href="https://github.com/hacker-works/mptt-jpa">README</a>
 */
@Entity
public class DyadicNode extends DyadicEntity {
  @SuppressWarnings({"Unused"})
  public DyadicNode() {
    super();
  }

  public DyadicNode(String name) {
    super(name);
  }
}
