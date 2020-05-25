package works.hacker.model.classic;

import works.hacker.mptt.TreeEntity;
import works.hacker.mptt.TreeRepository;
import works.hacker.mptt.classic.MpttEntity;
import works.hacker.repo.classic.MpttNodeRepository;
import works.hacker.repo.classic.MpttNodeRepositoryCustom;
import works.hacker.repo.classic.MpttNodeRepositoryImpl;

import javax.persistence.Entity;

/**
 * Demo usage of the {@link MpttEntity}.
 * <p>
 * Also used in the unit / integrations tests, as the {@link MpttEntity} is annotated
 * with {@link javax.persistence.MappedSuperclass} and can not be used standalone.
 *
 * @see MpttNodeRepositoryCustom
 * @see MpttNodeRepositoryImpl
 * @see MpttNodeRepository
 * @see TreeEntity
 * @see TreeRepository
 * @see <a href="https://github.com/hacker-works/mptt-jpa">README</a>
 */
@Entity
public class MpttNode extends MpttEntity {
  @SuppressWarnings({"Unused"})
  public MpttNode() {
    super();
  }

  public MpttNode(String name) {
    super(name);
  }
}
