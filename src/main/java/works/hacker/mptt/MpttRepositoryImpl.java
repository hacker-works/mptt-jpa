package works.hacker.mptt;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Transactional
public abstract class MpttRepositoryImpl<T extends MpttEntity> implements MpttRepository<T> {
  @PersistenceContext
  EntityManager entityManager;

  protected Class<T> entityClass;

  @Override
  public void setEntityClass(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @Override
  public void startTree(T node, Long treeId) throws NodeAlreadyAttachedToTree, TreeIdAlreadyUsed {
    ensureNodeIsNotAttachedToAnyTree(node);
    ensureTreeIdIsNotUsed(treeId);

    node.setTreeId(treeId);
    node.setLft(1L);
    node.setRgt(2L);

    entityManager.persist(node);
  }

  protected void ensureNodeIsNotAttachedToAnyTree(T node) throws NodeAlreadyAttachedToTree {
    if (node.hasTreeId()) {
      throw new NodeAlreadyAttachedToTree(
          String.format("Node already has treeId set to %d", node.getTreeId()));
    }
  }

  protected void ensureTreeIdIsNotUsed(Long treeId) throws TreeIdAlreadyUsed {
    try {
      findTreeRoot(treeId);
      throw new TreeIdAlreadyUsed(String.format("%d already used in another tree", treeId));
    } catch (NoResultException e) {
      // do nothing;
    }
  }

  @Override
  public T findTreeRoot(Long treeId) throws NoResultException {
    var query = String.format(
        "SELECT node FROM %s node" +
            " WHERE node.treeId = :treeId AND node.lft = 1",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", treeId)
        .getSingleResult();
  }

  @Override
  public void addChild(T parent, T child) throws NodeNotInTree, NodeAlreadyAttachedToTree {
    ensureParentIsAttachedToTree(parent);
    ensureNodeIsNotAttachedToAnyTree(child);

    long childLft;
    long childRgt;

    var rightMostChild = findRightMostChild(parent);

    if (rightMostChild == null) {
      childLft = parent.getLft() + 1;

      findByTreeIdAndLftGreaterThanEqual(parent.getTreeId(), childLft)
          .forEach(n -> n.setLft(n.getLft() + 2L));
      findByTreeIdAndRgtGreaterThan(parent.getTreeId(), parent.getLft())
          .forEach(n -> n.setRgt(n.getRgt() + 2L));
    } else {
      childLft = rightMostChild.getRgt() + 1;

      findByTreeIdAndLftGreaterThan(parent.getTreeId(), rightMostChild.getRgt())
          .forEach(n -> n.setLft(n.getLft() + 2L));
      findByTreeIdAndRgtGreaterThan(parent.getTreeId(), rightMostChild.getRgt())
          .forEach(n -> n.setRgt(n.getRgt() + 2L));
    }
    childRgt = childLft + 1;

    child.setTreeId(parent.getTreeId());
    child.setLft(childLft);
    child.setRgt(childRgt);

    entityManager.persist(child);
  }

  @Override
  public List<T> removeChild(T parent, T child) throws NodeNotInTree, NodeNotChildOfParent {
    ensureParentIsAttachedToTree(parent);
    ensureChildOfParent(parent, child);

    var removed = findSubTree(child);

    var decrement = child.getRgt() - child.getLft() + 1;
    findByTreeIdAndLftGreaterThan(parent.getTreeId(), child.getRgt())
        .forEach(n -> n.setLft(n.getLft() - decrement));
    findByTreeIdAndRgtGreaterThan(parent.getTreeId(), child.getRgt())
        .forEach(n -> n.setRgt(n.getRgt() - decrement));

    removed.forEach(this::removeNode);
    return removed;
  }

  protected void ensureParentIsAttachedToTree(T parent) throws NodeNotInTree {
    if (!parent.hasTreeId()) {
      throw new NodeNotInTree(String.format("Parent node not attached to any tree: %s", parent));
    }
  }

  protected void ensureChildOfParent(T parent, T child) throws NodeNotChildOfParent, NodeNotInTree {
    if (parent.getLft() < child.getLft() && child.getRgt() < parent.getRgt()) {
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
  public T findRightMostChild(T node) {
    var query = String.format(
        "SELECT node FROM %s node" +
            " WHERE node.treeId = :treeId AND node.rgt = :rgt",
        entityClass.getSimpleName());
    return getSingleResultOrNull(
        entityManager.createQuery(query, entityClass)
            .setParameter("treeId", node.getTreeId())
            .setParameter("rgt", node.getRgt() - 1));
  }

  protected T getSingleResultOrNull(TypedQuery<T> query) {
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public List<T> findByTreeIdAndLftGreaterThanEqual(Long treeId, Long lft) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.lft >= :lft",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", treeId)
        .setParameter("lft", lft)
        .getResultList();
  }

  @Override
  public List<T> findByTreeIdAndLftGreaterThan(Long treeId, Long lft) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.lft > :lft",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", treeId)
        .setParameter("lft", lft)
        .getResultList();
  }

  @Override
  public List<T> findByTreeIdAndRgtGreaterThan(Long treeId, Long rgt) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.rgt > :rgt",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", treeId)
        .setParameter("rgt", rgt)
        .getResultList();
  }

  @Override
  public List<T> findChildren(T node) {
    var query = String.format(
        "SELECT child" +
            " FROM %s child" +
            " WHERE child.treeId = :treeId" +
            " AND :lft < child.lft AND child.rgt < :rgt",
        entityClass.getSimpleName());
    var allChildren = entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .getResultList();
    return allChildren.stream()
        .filter(child -> depth(child, allChildren) == 0L)
        .collect(Collectors.toList());
  }

  // TODO: consider using a depth property, instead of computing it on the flight
  protected Long depth(T child, List<T> allChildren) {
    return allChildren.stream()
        .filter(node -> node.getLft() < child.getLft() && child.getRgt() < node.getRgt())
        .count();
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
            " AND node.lft < :lft AND :rgt < node.rgt" +
            " ORDER BY node.lft ASC",
        entityClass.getSimpleName());
    return entityManager.createQuery(query, entityClass)
        .setParameter("treeId", node.getTreeId())
        .setParameter("lft", node.getLft())
        .setParameter("rgt", node.getRgt())
        .getResultList();
  }

  @Override
  public T findParent(T node) {
    var query = String.format(
        "SELECT node" +
            " FROM %s node" +
            " WHERE node.treeId = :treeId" +
            " AND node.lft < :lft AND :rgt < node.rgt" +
            " ORDER BY node.lft DESC",
        entityClass.getSimpleName());
    return getSingleResultOrNull(
        entityManager.createQuery(query, entityClass)
            .setParameter("treeId", node.getTreeId())
            .setParameter("lft", node.getLft())
            .setParameter("rgt", node.getRgt())
            .setMaxResults(1));
  }

  @Override
  public String printTree(T node) {
    var rootString = printRootNode(node);

    var children = findChildren(node);
    var levels = Collections.unmodifiableList(Collections.singletonList(0));
    var childrenString = children.isEmpty() ? "" :
        "\n" +
            IntStream.range(0, children.size())
                .mapToObj(i -> i < children.size() - 1 ?
                    printSubTree(children.get(i), levels, false) :
                    printSubTree(children.get(i), levels, true))
                .collect(Collectors.joining("\n"));

    return rootString + childrenString;
  }

  protected String printSubTree(T node, List<Integer> levels, boolean isLast) {
    var children = findChildren(node);
    var nextLevels = concatLevel(levels, isLast ? 0 : 1);
    return
        (isLast ? printLastChildNode(node, levels) : printChildNode(node, levels)) +
            (children.isEmpty() ? "" : "\n" +
                IntStream.range(0, children.size())
                    .mapToObj(i -> i < children.size() - 1 ?
                        printSubTree(children.get(i), nextLevels, false) :
                        printSubTree(children.get(i), nextLevels, true))
                    .collect(Collectors.joining("\n")));
  }

  protected List<Integer> concatLevel(List<Integer> levels, Integer level) {
    return Stream.concat(levels.stream(), Stream.of(level))
        .collect(Collectors.toList());
  }

  protected String printRootNode(T node) {
    return String.format(
        // @formatter:off
        ".\n" +
            "└── %s",
        // @formatter:on
        node.toString());
  }

  protected String printChildNode(T node, List<Integer> levels) {
    return String.format("%s├── %s",
        printLevelPrefix(levels),
        node.toString());
  }

  protected String printLastChildNode(T node, List<Integer> levels) {
    return String.format("%s└── %s",
        printLevelPrefix(levels),
        node.toString());
  }

  protected String printLevelPrefix(List<Integer> levels) {
    return levels.stream()
        .map(i -> i == 0 ? "    " : "│   ")
        .collect(Collectors.joining());
  }
}
