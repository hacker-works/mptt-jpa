package works.hacker.mptt;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

@Transactional
public abstract class MpttRepositoryImpl<T extends MpttEntity, ID> implements MpttRepository<T, ID> {
  @PersistenceContext
  EntityManager entityManager;

  private Class<T> entityClass;

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

  private void ensureNodeIsNotAttachedToAnyTree(T node) throws NodeAlreadyAttachedToTree {
    if (node.hasTreeId()) {
      throw new NodeAlreadyAttachedToTree();
    }
  }

  private void ensureTreeIdIsNotUsed(Long treeId) throws TreeIdAlreadyUsed {
    try {
      findTreeRoot(treeId);
      throw new TreeIdAlreadyUsed();
    } catch (NoResultException e) {
      return;
    }
  }

  @Override
  public MpttEntity findTreeRoot(Long treeId) throws NoResultException {
    String query = String.format(
        "SELECT node FROM %s node " +
        "WHERE node.treeId = :treeId AND node.lft = 1",
        entityClass.getSimpleName());
    T root = entityManager.createQuery(query, entityClass)
        .setParameter("treeId", treeId)
        .getSingleResult();
    return root;
  }
}
