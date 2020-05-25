package works.hacker.mptt.dyadic;

import works.hacker.mptt.TreeRepository;

import java.util.Optional;

public interface DyadicRepository<T extends DyadicEntity> extends TreeRepository<T> {
  /**
   * <b>Internal method:</b> Finds the youngest / last-added child of a given node.
   * <p>
   * This method should not be called directly, but {@link DyadicRepository#addChild} depends on it.
   * <p>
   * Given the following dyadic fractions nested intervals representation:
   * <pre>
   * .
   * └── root [lft: 0/1 | rgt: 1/1]
   *     ├── child1 [lft: 0/1 | rgt: 1/2]
   *     │   ├── subChild1 [lft: 0/1 | rgt: 1/4]
   *     │   │   └── subSubChild [lft: 0/1 | rgt: 1/8]
   *     │   └── subChild2 [lft: 1/4 | rgt: 3/8]
   *     └── child2 [lft: 1/2 | rgt: 3/4]
   *         └── lastSubChild [lft: 1/2 | rgt: 5/8]
   * </pre>
   * When {@code repo.findYoungestChild(child1)}, then the right most child is
   * {@code subChild-2 [lft: 1/4 | rgt: 3/8]}
   * <p>
   * When {@code repo.findYoungestChild(root)}, then the right most child is
   * {@code child2 [lft: 1/2 | rgt: 3/4]}
   *
   * @param parent the parent node for which to find the right most child
   * @return an optional of the youngest / last-added child; or empty optional, if there are no children
   *
   * @see <a href="https://github.com/hacker-works/mptt-jpa">README</a>
   */
  Optional<T> findYoungestChild(T parent);
}
