package works.hacker.mptt;

import javax.persistence.NoResultException;
import java.util.List;

/**
 * JPA repository interface defining the operations of / on an MPTT tree.
 * <p>
 * All mutator operations are assumed to persist the changes.
 *
 * @see MpttRepositoryImpl
 * @see <a href="https://github.com/hacker-works/mptt-jpa">README</a>
 * @see <a href="https://github.com/hacker-works/mptt-jpa/blob/develop/src/test/java/works/hacker/repo/TagTreeRepoTest.java">TagTreeRepoTest</a>
 */
public interface MpttRepository<T extends MpttEntity> {
  /**
   * Sets the class of the entity.
   * <p>
   * The entity class should be stored in the state of the repository-instance.
   * <p>
   * The entity class is typically used when creating JPQL-queries dynamically. For example:
   * <pre>{@code
   * tagTreeRepo.setEntityClass(TagTree.class);
   * }</pre>
   * <p>
   * And then in the repository implementation, you might have something like:
   * <pre>{@code
   *   @Override
   *   public T findTreeRoot(Long treeId) throws NoResultException {
   *     var query = String.format(
   *         "SELECT node FROM %s node" +
   *             " WHERE node.treeId = :treeId AND node.lft = 1",
   *         entityClass.getSimpleName());
   *     return entityManager.createQuery(query, entityClass)
   *         .setParameter("treeId", treeId)
   *         .getSingleResult();
   *   }
   * }</pre>
   * <p>
   * <b>NOTE:</b> The reason for this method in the interface is <b>Java Generics - Type Erasure</b>.
   * <p>
   * Generics are used for tighter type checks at compile time and to provide a generic programming. To
   * implement generic behaviour, java compiler apply type erasure. Type erasure is a process in which
   * compiler replaces a generic parameter with actual class or bridge method, so no way to obtain the
   * type even by using the Reflection API.
   */
  void setEntityClass(Class<T> entityClass);

  /**
   * Starts a new tree.
   *
   * @param node   the node to become the root node of this tree; should be a mint node; must not be null
   * @param treeId the identifier for the new tree, that is not used by another tree; must not be null
   * @throws NodeAlreadyAttachedToTree in case the node is part of another tree
   * @throws TreeIdAlreadyUsed         in case the given {@code treeId} is used by another tree
   */
  void startTree(T node, Long treeId) throws NodeAlreadyAttachedToTree, TreeIdAlreadyUsed;

  /**
   * Finds the tree root node for a given {@code treeId}.
   *
   * @param treeId the identifier of the tree
   * @return the root node of the tree
   * @throws NoResultException in case there's no such tree
   */
  T findTreeRoot(Long treeId) throws NoResultException;

  /**
   * Adds a direct child to a given parent-node.
   *
   * @param parent the parent node; must not be null; must be part of a tree
   * @param child  the child node; must not be null; must not be part of another tree; should be mint
   * @throws NodeNotInTree             in case {@code parent} is not part of a tree
   * @throws NodeAlreadyAttachedToTree in case {@code child} is already part of a tree (incl. the parents)
   */
  void addChild(T parent, T child) throws NodeNotInTree, NodeAlreadyAttachedToTree;

  /**
   * Removes a child and its succeeding sub-tree nodes (if any) from a given parent-node.
   * <p>
   * Removing indirect children is allowed.
   *
   * @param parent the parent node; must not be null; must be part of a tree
   * @param child  the child node; must not be null; must be a direct (or indirect) child of the parent
   * @return the list of removed nodes - the {@code child} and its sub-tree nodes
   * @throws NodeNotInTree        in case {@code parent} is not part of a tree
   * @throws NodeNotChildOfParent in case the {@code child}-node is not the sub-tree of the {@code parent}
   */
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
