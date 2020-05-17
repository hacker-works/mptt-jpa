package works.hacker.mptt.dyadic;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Random;

public abstract class DyadicRepositoryImpl<T extends DyadicEntity> implements
    DyadicRepository<T> {
  @PersistenceContext
  EntityManager entityManager;

  protected Class<T> entityClass;

  @Override
  public void setEntityClass(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @Override
  public Long startTree(T node) throws NodeAlreadyAttachedToTree {
    ensureNodeIsNotAttachedToAnyTree(node);

    var treeId = generateTreeId();
    node.setTreeId(treeId);
    node.setNodeDefaults();

    entityManager.persist(node);
    return treeId;
  }

  protected void ensureNodeIsNotAttachedToAnyTree(T node) throws NodeAlreadyAttachedToTree {
    if (node.hasTreeId()) {
      throw new NodeAlreadyAttachedToTree(
          String.format("Node already has treeId set to %d", node.getTreeId()));
    }
  }

  protected Long generateTreeId() {
    Long treeId = new Random().nextLong();
    var query = String.format(
        "SELECT node FROM %s node WHERE node.treeId = :treeId",
        entityClass.getSimpleName());
    try {
      entityManager.createQuery(query, entityClass)
          .setParameter("treeId", treeId)
          .setMaxResults(1)
          .getSingleResult();
    } catch (NoResultException e) {
      return treeId;
    }
    return generateTreeId();
  }

  @Override
  public T findTreeRoot(Long treeId) throws NoResultException {
    var query = String.format(
        "SELECT node FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.head = 0 AND node.tail = 1",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", treeId)
        .getSingleResult();
  }

  @Override
  public void addChild(T parent, T child) throws NodeNotInTree, NodeAlreadyAttachedToTree {
    ensureParentIsAttachedToTree(parent);
    ensureNodeIsNotAttachedToAnyTree(child);


    entityManager.persist(child);
  }

  protected void addFirstChild(T parent, T child) {
    child.setTreeId(parent.getTreeId());
    child.setHeadN(parent.getHeadN());
    child.setHeadD(parent.getHeadD());
    child.setTailN(parent.getHeadN() + parent.getTailN());
    child.setTailD(parent.getHeadD() + parent.getTailD());
  }

  protected void ensureParentIsAttachedToTree(T parent) throws NodeNotInTree {
    if (!parent.hasTreeId()) {
      throw new NodeNotInTree(String.format("Parent node not attached to any tree: %s", parent));
    }
  }

  @Override
  public List<T> removeChild(T parent, T child) throws NodeNotInTree, NodeNotChildOfParent {
    return null;
  }

  @Override
  public List<T> findChildren(T node) {
    return null;
  }

  @Override
  public List<T> findSubTree(T node) {
    return null;
  }

  @Override
  public List<T> findAncestors(T node) {
    return null;
  }

  @Override
  public T findParent(T node) {
    return null;
  }
}
