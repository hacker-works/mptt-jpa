package works.hacker.mptt;

import javax.persistence.NoResultException;

public interface MpttRepository<T extends MpttEntity, ID> {
  void setEntityClass(Class<T> entityClass);

  void startTree(T node, Long treeId) throws NodeAlreadyAttachedToTree, TreeIdAlreadyUsed;
  MpttEntity findTreeRoot(Long treeId) throws NoResultException;

  // @formatter:off
  class TreeIdAlreadyUsed extends Exception {}
  class NodeAlreadyAttachedToTree extends Exception {}
  // @formatter:on
}
