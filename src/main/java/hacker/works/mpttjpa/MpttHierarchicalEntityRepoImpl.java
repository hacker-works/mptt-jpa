package hacker.works.mpttjpa;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MpttHierarchicalEntityRepoImpl<T extends MpttHierarchicalEntity<T>>
    implements MpttHierarchicalEntityRepo<T> {

  private Class<T> clazz;

  public final void setClazz(Class<T> clazzToSet){
    this.clazz = clazzToSet;
  }

  @PersistenceContext
  public EntityManager entityManager;

  @Override
  @Transactional
  public void setAsHierarchyRoot(T node, Long hierarchyId)
      throws HierarchyIdAlreadySetException, HierarchyRootExistsException {
    ensureNodeDoesNotBelongToAnyHierarchy(node);
    ensureHierarchyRootIsNotSet(hierarchyId);

    node.setHierarchyId(hierarchyId);
    node.setLft(1L);
    node.setRgt(2L);

    entityManager.persist(node);
  }

  @Override
  @Transactional
  public void addChild(T parent, T child)
      throws HierarchyIdAlreadySetException, HierarchyIdNotSetException {
    ensureNodeBelongsToAHierarchy(parent);
    ensureNodeDoesNotBelongToAnyHierarchy(child);

    Long childLft;
    Long childRgt;

    T rightMostChild = findRightMostChild(parent);
    if (rightMostChild == null) {
      childLft = parent.getLft() + 1;
      childRgt = childLft + 1;

      incrementEntitiesLft(
          findEntitiesWhichLftIsGreaterThanOrEqualTo(parent, childLft), 2L);
      incrementEntitiesRgt(
          findEntitiesWhichRgtIsGreaterThan(parent, parent.getLft()), 2L);
    } else {
      childLft = rightMostChild.getRgt() + 1;
      childRgt = childLft + 1;

      incrementEntitiesLft(
          findEntitiesWhichLftIsGreaterThan(parent, rightMostChild.getRgt()), 2L);
      incrementEntitiesRgt(
          findEntitiesWhichRgtIsGreaterThan(parent, rightMostChild.getRgt()), 2L);
    }

    child.setLft(childLft);
    child.setRgt(childRgt);
    child.setHierarchyId(parent.getHierarchyId());

    entityManager.persist(child);
  }

  @Override
  @Transactional
  public void removeChild(T parent, T child)
      throws NotADescendantException,
        HierarchyIdNotSetException,
        NotInTheSameHierarchyException {
    Set<T> childSubTree = findSubTree(child);

    ensureNodeBelongsToAHierarchy(parent);
    ensureNodeIsDescendant(parent, child);

    Long decrement = child.getRgt() - child.getLft() + 1;
    decrementEntitiesLftWith(
        findEntitiesWhichLftIsGreaterThan(parent, child.getRgt()), decrement);
    decrementEntitiesRgtWith(
        findEntitiesWhichRgtIsGreaterThan(parent, child.getRgt()), decrement);

    resetEntities(childSubTree);

    removeSubTree(childSubTree);
  }

  private void addValueToEntitiesLft(Set<T> entities, Long value) {
    for (T entity : entities) {
      entity.setLft(entity.getLft() + value);
    }
  }

  private void addValueToEntitiesRgt(Set<T> entities, Long value) {
    for (T entity : entities) {
      entity.setRgt(entity.getRgt() + value);
    }
  }

  private void incrementEntitiesLft(Set<T> entities, Long value) {
    addValueToEntitiesLft(entities, value);
  }

  private void incrementEntitiesRgt(Set<T> entities, Long value) {
    addValueToEntitiesRgt(entities, value);
  }

  private void decrementEntitiesLftWith(Set<T> entities, Long value) {
    addValueToEntitiesLft(entities, -value);
  }

  private void decrementEntitiesRgtWith(Set<T> entities, Long value) {
    addValueToEntitiesRgt(entities, -value);
  }

  private void resetEntities(Set<T> entities) {
    for (T entity : entities) {
      entity.setLft(null);
      entity.setRgt(null);
      entity.setHierarchyId(null);
    }
  }

  private void ensureNodeBelongsToAHierarchy(T node)
      throws HierarchyIdNotSetException {
    if (node.getHierarchyId() == null) {
      throw new HierarchyIdNotSetException();
    }
  }

  private void ensureNodeDoesNotBelongToAnyHierarchy(T node)
      throws HierarchyIdAlreadySetException {
    if (node.getHierarchyId() != null) {
      throw new HierarchyIdAlreadySetException();
    }
  }

  private void ensureNodeIsInTheSameHierarchy(T thisNode, T node)
      throws NotInTheSameHierarchyException {
    if (thisNode.getHierarchyId() == null
        || thisNode.getHierarchyId() != node.getHierarchyId())
      throw new NotInTheSameHierarchyException();
  }

  private void ensureNodeIsDescendant(T thisNode, T node)
      throws NotADescendantException, NotInTheSameHierarchyException {
    ensureNodeIsInTheSameHierarchy(thisNode, node);
    if (!(thisNode.getLft() < node.getLft()
        && node.getRgt() < thisNode.getRgt()))
      throw new NotADescendantException();
  }

  private void ensureHierarchyRootIsNotSet(Long hierarchyId)
      throws HierarchyRootExistsException {
    try {
      findHierarchyRoot(hierarchyId);

      throw new HierarchyRootExistsException();
    } catch (HierarchyRootDoesNotExistException e) {
      return;
    }
  }

  @Override
  public T findHierarchyRoot(Long hierarchyId)
      throws HierarchyRootDoesNotExistException {
    try {
      return entityManager
        .createNamedQuery("findHierarchyRoot", clazz)
        .setParameter("hierarchyId", hierarchyId)
        .getSingleResult();
    } catch (NoResultException e) {
      throw new HierarchyRootDoesNotExistException();
    }
  }

  @Override
  public T findRightMostChild(T node) {
    return getSingleResultWithoutException(entityManager
        .createNamedQuery("findEntityByRightValue", clazz)
        .setParameter("hierarchyId", node.getHierarchyId())
        .setParameter("value", node.getRgt() - 1)
    );
  }

  @Override
  public Set<T> findEntitiesWhichLftIsGreaterThanOrEqualTo(T node, Long value) {
    return new HashSet<T>(entityManager
        .createNamedQuery("findEntitiesWithLeftGreaterThanOrEqual", clazz)
        .setParameter("hierarchyId", node.getHierarchyId())
        .setParameter("value", value)
        .getResultList()
    );
  }

  @Override
  public Set<T> findEntitiesWhichLftIsGreaterThan(T node, Long value) {
    return new HashSet<T>(entityManager
        .createNamedQuery("findEntitiesWithLeftGreaterThan", clazz)
        .setParameter("hierarchyId", node.getHierarchyId())
        .setParameter("value", value)
        .getResultList()
    );
  }

  @Override
  public Set<T> findEntitiesWhichRgtIsGreaterThan(T node, Long value) {
    return new HashSet<T>(entityManager
        .createNamedQuery("findEntitiesWithRightGreaterThan", clazz)
        .setParameter("hierarchyId", node.getHierarchyId())
        .setParameter("value", value)
        .getResultList()
    );
  }

  @Override
  public Set<T> findSubTree(T node) {
    return new HashSet<T>(entityManager
        .createNamedQuery("findSubtree", clazz)
        .setParameter("hierarchyId", node.getHierarchyId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .getResultList()
    );
  }

  @Override
  public List<T> findAncestors(T node) {
    return entityManager
      .createNamedQuery("findAncestors", clazz)
      .setParameter("hierarchyId", node.getHierarchyId())
      .setParameter("lft", node.getLft())
      .setParameter("rgt", node.getRgt())
      .getResultList();
  }

  @Override
  public List<T> findChildren(T node) {
    return entityManager
      .createNamedQuery("findChildren", clazz)
      .setParameter("hierarchyId", node.getHierarchyId())
      .setParameter("lft", node.getLft())
      .setParameter("rgt", node.getRgt())
      .getResultList();
  }

  @Override
  public T findParent(T node) {
    List<T> ancestors = findAncestors(node);
    return (
      (ancestors != null) && ancestors.size() > 0)
        ? ancestors.get(ancestors.size() - 1)
        : null;
  }

  @Transactional
  public void remove(T node) {
    if (entityManager.contains(node)) {
      entityManager.remove(node);
    } else {
      T attached = entityManager.find(clazz, node.getId());
      entityManager.remove(attached);
    }
  }

  @Transactional
  private void removeSubTree(Set<T> subTree) {
    for (T s : subTree) {
      remove(s);
    }
  }

  private static <T> T getSingleResultWithoutException(TypedQuery<T> query) {
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
