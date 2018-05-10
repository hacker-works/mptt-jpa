package hacker.works.mpttjpa;

import java.util.Set;

/**
 * Defines the required access and mutate methods for 
 * implementing the Modified Pre-Order Tree Traversal
 * algorithm for storing and manipulating hierarchical data.
 */
public interface MpttHierarchicalEntity<T extends MpttHierarchicalEntity<T>> 
    extends HierarchicalEntity<T> {
  
  void setLft(Long lft);
  Long getLft();
  
  void setRgt(Long rgt);
  Long getRgt();
  
  T findRightMostChild();
  Set<T> findEntitiesWhichLftIsGreaterThanOrEqualTo(Long value);
  Set<T> findEntitiesWhichLftIsGreaterThan(Long value);
  Set<T> findEntitiesWhichRgtIsGreaterThan(Long value);
  Set<T> findSubTree();
  
}
