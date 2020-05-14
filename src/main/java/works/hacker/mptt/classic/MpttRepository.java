package works.hacker.mptt.classic;

import works.hacker.mptt.TreeRepository;

import java.util.List;

public interface MpttRepository<T extends MpttEntity> extends TreeRepository<T> {
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
}
