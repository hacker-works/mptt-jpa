package works.hacker.mptt.dyadic;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public abstract class DyadicRepositoryImpl<T extends DyadicEntity> implements DyadicRepository<T> {
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

    var youngest = findYoungestChild(parent);
    if (youngest.isEmpty()) {
      addFirstChild(parent, child);
    } else {
      addNextChild(youngest.get(), child);
    }

    entityManager.persist(child);
  }

  protected void addFirstChild(T parent, T child) {
    child.setTreeId(parent.getTreeId());
    child.setDepth(parent.getDepth() + 1);
    child.setHeadN(parent.getHeadN());
    child.setHeadD(parent.getHeadD());
    child.setTailN(2 * parent.getHeadN() + parent.getTailN());
    child.setTailD(2 * parent.getTailD());
  }

  protected void addNextChild(T sibling, T child) {
    child.setTreeId(sibling.getTreeId());
    child.setDepth(sibling.getDepth());
    child.setHeadN(sibling.getTailN());
    child.setHeadD(sibling.getTailD());
    child.setTailN(2 * sibling.getTailN() + 1);
    child.setTailD(2 * sibling.getTailD());
  }

  @Override
  public List<T> removeChild(T parent, T child) throws NodeNotInTree, NodeNotChildOfParent {
    ensureParentIsAttachedToTree(parent);
    ensureChildOfParent(parent, child);

    var removed = findSubTree(child);
    removed.forEach(this::removeNode);
    return removed;
  }

  protected void ensureParentIsAttachedToTree(T parent) throws NodeNotInTree {
    if (!parent.hasTreeId()) {
      throw new NodeNotInTree(String.format("Parent node not attached to any tree: %s", parent));
    }
  }

  protected void ensureChildOfParent(T parent, T child) throws NodeNotChildOfParent, NodeNotInTree {
    if (parent.getHead() <= child.getHead() && child.getTail() <= parent.getTail()) {
      if (!child.getTreeId().equals(parent.getTreeId())) {
        throw new NodeNotInTree(
            String.format("Nodes not in same tree - parent: %s; child %s", parent, child));
      }
    } else {
      throw new NodeNotChildOfParent(String.format("%s not parent of %s", parent, child));
    }
  }

  protected void removeNode(T node) {
    if (entityManager.contains(node)) {
      entityManager.remove(node);
    } else {
      var attached = entityManager.find(entityClass, node);
      entityManager.remove(attached);
    }
  }

  @Override
  public Optional<T> findYoungestChild(T parent) {
    var query = String.format(
        "SELECT youngest FROM %s youngest" +
            " WHERE youngest.treeId = :treeId" +
            " AND youngest.depth = :depth" +
            " AND :head <= youngest.head" +
            " AND youngest.tail <= :tail" +
            " AND youngest.tailD = (" +
            "SELECT MAX(node.tailD) FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.depth = :depth" +
            " AND :head <= node.head" +
            " AND node.tail <= :tail" +
            ")",
        entityClass.getSimpleName(),
        entityClass.getSimpleName());
    return toOptional(
        entityManager.createQuery(query, entityClass)
            .setParameter("treeId", parent.getTreeId())
            .setParameter("head", parent.getHead())
            .setParameter("tail", parent.getTail())
            .setParameter("depth", parent.getDepth() + 1));
  }

  protected Optional<T> toOptional(TypedQuery<T> query) {
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public List<T> findChildren(T node) {
    var query = String.format(
        "SELECT child" +
            " FROM %s child" +
            " WHERE child.treeId = :treeId" +
            " AND :head <= child.head AND child.tail <= :tail" +
            " AND child.depth = :depth",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("head", node.getHead())
        .setParameter("tail", node.getTail())
        .setParameter("depth", node.getDepth() + 1)
        .getResultList();
  }

  @Override
  public List<T> findSubTree(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND :head <= node.head AND node.tail <= :tail",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("head", node.getHead())
        .setParameter("tail", node.getTail())
        .getResultList();
  }

  @Override
  public List<T> findAncestors(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.head <= :head AND :tail <= node.tail" +
            " AND node.depth < :depth" +
            " ORDER BY node.depth ASC",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("head", node.getHead())
        .setParameter("tail", node.getTail())
        .setParameter("depth", node.getDepth())
        .getResultList();
  }

  @Override
  public T findParent(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.head <= :head AND :tail <= node.tail" +
            " AND node.depth = :depth",
        entityClass.getSimpleName());
    return getSingleResultOrNull(
        entityManager.createQuery(query, entityClass)
            .setParameter("treeId", node.getTreeId())
            .setParameter("head", node.getHead())
            .setParameter("tail", node.getTail())
            .setParameter("depth", node.getDepth() - 1));
  }

  protected T getSingleResultOrNull(TypedQuery<T> query) {
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
