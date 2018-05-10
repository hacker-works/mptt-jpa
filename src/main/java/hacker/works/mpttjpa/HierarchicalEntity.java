package hacker.works.mpttjpa;

import hacker.works.mpttjpa.exceptions.*;

import java.util.List;

public interface HierarchicalEntity<T extends HierarchicalEntity<T>> {
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

  void setAsHierarchyRoot(Long hierarchyId)
    throws HierarchyIdAlreadySetException, HierarchyRootExistsException;

  void addChild(T child)
    throws HierarchyIdAlreadySetException, HierarchyIdNotSetException;

  void removeChild(T child)
      throws NotADescendantException,
          HierarchyIdNotSetException,
          NotInTheSameHierarchyException;

  T findHierarchyRoot(Long hierarchyId)
    throws HierarchyRootDoesNotExistException;

  List<T> findAncestors();
  T findParent();

}
