package works.hacker.mptt;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class MpttEntity {
  public static final Long NO_TREE_ID = 0L;
  public static final Long DEFAULT_LFT = 1L;
  public static final Long DEFAULT_RGT = 2L;

  @Column(nullable = false)
  private Long treeId;

  @Column(nullable = false)
  private long lft;

  @Column(nullable = false)
  private long rgt;

  public MpttEntity() {
    this.treeId = NO_TREE_ID;
    this.lft = DEFAULT_LFT;
    this.rgt = DEFAULT_RGT;
  }

  public boolean hasTreeId() {
    return treeId != null && !treeId.equals(NO_TREE_ID);
  }

  public Long getTreeId() {
    return treeId;
  }

  public void setTreeId(Long treeId) {
    this.treeId = treeId;
  }

  public long getLft() {
    return lft;
  }

  public void setLft(long lft) {
    this.lft = lft;
  }

  public long getRgt() {
    return rgt;
  }

  public void setRgt(long rgt) {
    this.rgt = rgt;
  }

  @Override
  public String toString() {
    return String.format("[treeId: %d | lft: %d | rgt: %d]", getTreeId(), getLft(), getRgt());
  }
}
