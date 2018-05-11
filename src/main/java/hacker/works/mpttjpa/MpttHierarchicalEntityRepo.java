package hacker.works.mpttjpa;

import java.util.List;
import java.util.Set;

/**
 * Defines the required access and mutate methods needed for implementing the
 * Modified Pre-Order Tree Traversal algorithm for storing and manipulating
 * hierarchical data.
 */
public interface MpttHierarchicalEntityRepo<T extends MpttHierarchicalEntity<T>> {
  void setAsHierarchyRoot(T entity, Long hierarchyId)
      throws HierarchyIdAlreadySetException, HierarchyRootExistsException;

  void setClazz(Class<T> clazzToSet);

  void addChild(T parent, T child)
      throws HierarchyIdAlreadySetException, HierarchyIdNotSetException;
  void removeChild(T parent, T child)
      throws NotADescendantException,
        HierarchyIdNotSetException,
          NotInTheSameHierarchyException;

  T findHierarchyRoot(Long hierarchyId)
      throws HierarchyRootDoesNotExistException;
  T findRightMostChild(T node);

  Set<T> findEntitiesWhichLftIsGreaterThanOrEqualTo(T node, Long value);
  Set<T> findEntitiesWhichLftIsGreaterThan(T node, Long value);
  Set<T> findEntitiesWhichRgtIsGreaterThan(T node, Long value);

  Set<T> findSubTree(T node);
  List<T> findAncestors(T node);
  List<T> findChildren(T node);

  T findParent(T node);

  class HierarchyIdAlreadySetException extends Exception {}
  class HierarchyIdNotSetException extends Exception {}
  class HierarchyRootDoesNotExistException extends Exception {}
  class HierarchyRootExistsException extends Exception {}
  class NotADescendantException extends Exception {}
  class NotInTheSameHierarchyException extends Exception {}
}