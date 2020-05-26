package works.hacker.mptt.dyadic;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Transactional
public abstract class DyadicRepositoryImpl<T extends DyadicEntity> implements DyadicRepository<T> {
  @PersistenceContext
  EntityManager entityManager;

  protected Class<T> entityClass;

  @Override
  public void setEntityClass(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @Override
  public T createNode(String name)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
      InstantiationException {
    return entityClass.getDeclaredConstructor(String.class).newInstance(name);
  }

  @Override
  public Long startTree(T node) throws NodeAlreadyAttachedToTree {
    ensureNodeIsNotAttachedToAnyTree(node);

    var treeId = generateTreeId();
    node.setDefaults();
    node.setTreeId(treeId);

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
            " AND node.lft = 0 AND node.rgt = 1",
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
    child.setLftN(parent.getLftN());
    child.setLftD(parent.getLftD());
    child.setRgtN(2 * parent.getLftN() + parent.getRgtN());
    child.setRgtD(2 * parent.getRgtD());
  }

  protected void addNextChild(T sibling, T child) {
    child.setTreeId(sibling.getTreeId());
    child.setDepth(sibling.getDepth());
    child.setLftN(sibling.getRgtN());
    child.setLftD(sibling.getRgtD());
    child.setRgtN(2 * sibling.getRgtN() + 1);
    child.setRgtD(2 * sibling.getRgtD());
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
    if (parent.getLft() <= child.getLft() && child.getRgt() <= parent.getRgt()) {
      if (child.getTreeId() != parent.getTreeId()) {
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
            " AND :lft <= youngest.lft" +
            " AND youngest.rgt <= :rgt" +
            " AND youngest.rgtD = (" +
            "SELECT MAX(node.rgtD) FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.depth = :depth" +
            " AND :lft <= node.lft" +
            " AND node.rgt <= :rgt" +
            ")",
        entityClass.getSimpleName(),
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", parent.getTreeId())
        .setParameter("lft", parent.getLft())
        .setParameter("rgt", parent.getRgt())
        .setParameter("depth", parent.getDepth() + 1)
        .getResultList().stream().findFirst();
  }

  @Override
  public List<T> findChildren(T node) {
    var query = String.format(
        "SELECT child" +
            " FROM %s child" +
            " WHERE child.treeId = :treeId" +
            " AND :lft <= child.lft AND child.rgt <= :rgt" +
            " AND child.depth = :depth",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .setParameter("depth", node.getDepth() + 1)
        .getResultList();
  }

  @Override
  public List<T> findSubTree(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND :lft <= node.lft AND node.rgt <= :rgt",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .getResultList();
  }

  @Override
  public List<T> findAncestors(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.lft <= :lft AND :rgt <= node.rgt" +
            " AND node.depth < :depth" +
            " ORDER BY node.depth ASC",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .setParameter("depth", node.getDepth())
        .getResultList();
  }

  @Override
  public Optional<T> findParent(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.lft <= :lft AND :rgt <= node.rgt" +
            " AND node.depth = :depth",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .setParameter("depth", node.getDepth() - 1)
        .getResultList().stream().findFirst();
  }
}
