package hacker.works.mpttjpa;

/**
 * Defines the required attributes for the entities that are going to store
 * Modified Pre-Order Tree Traversal hierarchical data in a relational database.
 */
public interface MpttHierarchicalEntity<T extends MpttHierarchicalEntity<T>> {
  /**
   * This property would allow us to have multiple hierarchies
   * in the same entity without the need to have a share root.
   *
   * Instead of a tree, we would have a forest. Each forest
   * identified with a unique hierarchy id. Thus the indexing
   * between hierarchies can be independent.
   *
   * @return the id of the hierarchy
   */
  void setHierarchyId(Long hierarchyId);
  Long getHierarchyId();

  Long getId();

  void setLft(Long lft);
  Long getLft();
  
  void setRgt(Long rgt);
  Long getRgt();
}
