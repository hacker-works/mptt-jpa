package works.hacker.mptt.classic;

import works.hacker.mptt.TreeEntity;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class MpttEntity extends TreeEntity<Long> {
  public MpttEntity() {
    super();
  }

  public MpttEntity(String name) {
    super(name);
  }

  @Override
  public Long getStartLft() {
    return 1L;
  }

  @Override
  public Long getStartRgt() {
    return 2L;
  }
}
