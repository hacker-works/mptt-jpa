package works.hacker.mptt;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class TreeEntity {
  public static final long NO_TREE_ID = -1L;

  @Column(nullable = false)
  protected long treeId;

  public TreeEntity() {
    this.treeId = NO_TREE_ID;
  }

  public boolean hasTreeId() {
    return treeId != NO_TREE_ID;
  }

  public Long getTreeId() {
    return treeId;
  }

  public void setTreeId(Long treeId) {
    this.treeId = treeId;
  }
}
