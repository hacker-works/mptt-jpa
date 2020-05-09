package works.hacker.mptt;

import javax.persistence.NoResultException;
import java.util.List;

public interface MpttRepository<T extends MpttEntity> {
  void setEntityClass(Class<T> entityClass);

  void startTree(T node, Long treeId) throws NodeAlreadyAttachedToTree, TreeIdAlreadyUsed;

  T findTreeRoot(Long treeId) throws NoResultException;

  void addChild(T parent, T child) throws NodeNotInTree, NodeAlreadyAttachedToTree;

  List<T> removeChild(T parent, T child) throws NodeNotInTree, NodeNotChildOfParent;

  T findRightMostChild(T node);

  List<T> findByTreeIdAndLftGreaterThanEqual(Long treeId, Long lft);

  List<T> findByTreeIdAndLftGreaterThan(Long treeId, Long lft);

  List<T> findByTreeIdAndRgtGreaterThan(Long treeId, Long rgt);

  List<T> findChildren(T node);

  List<T> findSubTree(T node);

  List<T> findAncestors(T node);

  T findParent(T node);

  String printTree(T node);

  class TreeIdAlreadyUsed extends Exception {
    public TreeIdAlreadyUsed(String message) {
      super(message);
    }
  }

  class NodeAlreadyAttachedToTree extends Exception {
    public NodeAlreadyAttachedToTree(String message) {
      super(message);
    }
  }

  class NodeNotInTree extends Exception {
    public NodeNotInTree(String message) {
      super(message);
    }
  }

  class NodeNotChildOfParent extends Exception {
    public NodeNotChildOfParent(String message) {
      super(message);
    }
  }
}
