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
   * <pre><code>
   * tagTreeRepo.setEntityClass(TagTree.class);
   * </code></pre>
   * <p>
   * And then in the repository implementation, you might have something like:
   * <pre><code>
   *  {@literal @}Override
   *   public T findTreeRoot(Long treeId) throws NoResultException {
   *     var query = String.format(
   *         "SELECT node FROM %s node" +
   *             " WHERE node.treeId = :treeId AND node.lft = 1",
   *         entityClass.getSimpleName());
   *     return entityManager.createQuery(query, entityClass)
   *         .setParameter("treeId", treeId)
   *         .getSingleResult();
   *   }
   * </code></pre>
   * <p>
   * <b>NOTE:</b> The reason for this method in the interface is <b>Java Generics - Type Erasure</b>.
   * <p>
   * Generics are used for tighter type checks at compile time and to provide a generic programming. To
   * implement generic behaviour, java compiler apply type erasure. Type erasure is a process in which
   * compiler replaces a generic parameter with actual class or bridge method, so no way to obtain the
   * type even by using the Reflection API.
   *
   * @param entityClass the class type of the entity extending {@link MpttEntity}
   */
  void setEntityClass(Class<T> entityClass);

  /**
   * Starts a new tree.
   *
   * @param node the node to become the root node of this tree; should be a mint node; must not be null
   * @return the generated tree id
   * @throws NodeAlreadyAttachedToTree in case the node is part of another tree
   */
  Long startTree(T node) throws NodeAlreadyAttachedToTree;

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
   * <p>
   * Given the following tree representation:
   * <pre>
   * .
   * └── root
   *     ├── child1
   *     │   ├── subChild1
   *     │   │   └── subSubChild
   *     │   └── subChild2
   *     └── child2
   *         └── lastSubChild
   * </pre>
   * When {@code tagTreeRepo.removeChild(root, child1)}, then the resulting tree should be:
   * <pre>
   * .
   * └── root
   *     └── child2
   *         └── lastSubChild
   * </pre>
   * And the returned result should be a list containing {@code child1, subChild1, subSubChild, subChild2}.
   *
   * @param parent the parent node; must not be null; must be part of a tree
   * @param child  the child node; must not be null; must be a direct (or indirect) child of the parent
   * @return the list of removed nodes - the {@code child} and its sub-tree nodes
   * @throws NodeNotInTree        in case {@code parent} is not part of a tree
   * @throws NodeNotChildOfParent in case the {@code child}-node is not the sub-tree of the {@code parent}
   */
  List<T> removeChild(T parent, T child) throws NodeNotInTree, NodeNotChildOfParent;

  /**
   * <b>Internal method:</b> Finds the right-most child of a given node.
   * <p>
   * This method should not be called directly, but {@link MpttRepository#addChild} depends on it.
   * <p>
   * Given the following MPTT representation:
   * <pre>
   * .
   * └── root [lft: 1 | rgt: 14]
   *     ├── child1 [lft: 2 | rgt: 9]
   *     │   ├── subChild1 [lft: 3 | rgt: 6]
   *     │   │   └── subSubChild [lft: 4 | rgt: 5]
   *     │   └── subChild2 [lft: 7 | rgt: 8]
   *     └── child2 [lft: 10 | rgt: 13]
   *         └── lastSubChild [lft: 11 | rgt: 12]
   * </pre>
   * When {@code tagTreeRepo.findRightMostChild(child1)}, then the right most child is
   * {@code subChild-2 [lft: 7 | rgt: 8]}
   * <p>
   * When {@code tagTreeRepo.findRightMostChild(root)}, then the right most child is
   * {@code child2 [lft: 10 | rgt: 13]}
   *
   * @param node the parent node for which to find the right most child
   * @return the right most child; or null, if there are no children
   */
  T findRightMostChild(T node);

  /**
   * <b>Internal method:</b> Finds the nodes matching the specified criteria.
   * <p>
   * This method should not be called directly, but {@link MpttRepository#addChild} depends on it.
   *
   * @param treeId the value for the {@code treeId}-criteria
   * @param lft    the value for the {@code lft}-criteria
   * @return the matching nodes
   */
  List<T> findByTreeIdAndLftGreaterThanEqual(Long treeId, Long lft);

  /**
   * <b>Internal method:</b> Finds the nodes matching the specified criteria.
   * <p>
   * This method should not be called directly, but {@link MpttRepository#addChild} and
   * {@link MpttRepository#removeChild} depend on it.
   *
   * @param treeId the value for the {@code treeId}-criteria
   * @param lft    the value for the {@code lft}-criteria
   * @return the matching nodes
   */
  List<T> findByTreeIdAndLftGreaterThan(Long treeId, Long lft);

  /**
   * <b>Internal method:</b> Finds the nodes matching the specified criteria.
   * <p>
   * This method should not be called directly, but {@link MpttRepository#addChild} and
   * {@link MpttRepository#removeChild} depend on it.
   *
   * @param treeId the value for the {@code treeId}-criteria
   * @param rgt    the value for the {@code rgt}-criteria
   * @return the matching nodes
   */
  List<T> findByTreeIdAndRgtGreaterThan(Long treeId, Long rgt);

  /**
   * Finds the direct children of a given parent node.
   * <p>
   * Given the following tree representation:
   * <pre>
   * .
   * └── root
   *     ├── child1
   *     │   ├── subChild1
   *     │   │   └── subSubChild
   *     │   └── subChild2
   *     └── child2
   *         └── lastSubChild
   * </pre>
   * When {@code tagTreeRepo.findChildren(root)}, then the returned list of direct children should
   * contain {@code child1, child2}.
   *
   * @param node the parent node; must not be null; must be part of a tree
   * @return the list of the direct children
   */
  List<T> findChildren(T node);

  /**
   * Finds the sub-tree - including the parent and all direct and indirect children - of a given node.
   * <p>
   * Given the following tree representation:
   * <pre>
   * .
   * └── root
   *     ├── child1
   *     │   ├── subChild1
   *     │   │   └── subSubChild
   *     │   └── subChild2
   *     └── child2
   *         └── lastSubChild
   * </pre>
   * When {@code tagTreeRepo.findSubTree(child1)}, then the returned list of sub-tree nodes should
   * contain {@code child1, subChild1, subSubChild, subChild2}.
   *
   * @param node the parent node; must not be null; must be part of a tree
   * @return the list of the parent and all of its direct and indirect children nodes
   */
  List<T> findSubTree(T node);

  /**
   * Finds the list of ancestors of a given node.
   * <p>
   * Given the following tree representation:
   * <pre>
   * .
   * └── root
   *     ├── child1
   *     │   ├── subChild1
   *     │   │   └── subSubChild
   *     │   └── subChild2
   *     └── child2
   *         └── lastSubChild
   * </pre>
   * When {@code tagTreeRepo.findAncestors(subSubChild)}, then the returned list of ancestors nodes
   * should contain {@code root, child1, subChild1}.
   *
   * @param node must not be null; must be part of a tree
   * @return the list of all ancestor nodes; or empty list if the given node is a root node
   */
  List<T> findAncestors(T node);

  /**
   * Finds the direct parent of a given node.
   * <p>
   * Given the following tree representation:
   * <pre>
   * .
   * └── root
   *     ├── child1
   *     │   ├── subChild1
   *     │   │   └── subSubChild
   *     │   └── subChild2
   *     └── child2
   *         └── lastSubChild
   * </pre>
   * When {@code tagTreeRepo.findParent(root)}, should return {@code null}.
   * When {@code tagTreeRepo.findParent(child1)}, should return {@code root}.
   * When {@code tagTreeRepo.findParent(subChild1)}, should return {@code child1}.
   *
   * @param node must not be null; must be part of a tree
   * @return the direct parent node; or null if the given node is a root node
   */
  T findParent(T node);

  /**
   * Prints a string representation of the tree / sub-tree of a given node.
   *
   * @param node must not be null; must be part of a tree
   * @return the string representation of the tree / sub-tree
   */
  String printTree(T node);

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
