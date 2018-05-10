package hacker.works.mpttjpa.impl;

import hacker.works.mpttjpa.MpttHierarchicalEntity;
import hacker.works.mpttjpa.exceptions.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public class MpttHierarchicalEntityImpl<T extends MpttHierarchicalEntity<T>>
    implements MpttHierarchicalEntity<T> {
  
  private T entity;
  
  public MpttHierarchicalEntityImpl(T entity) {
    this.entity = entity;
  }
  
  public void setHierarchyId(Long hierarchyId) {
    entity.setHierarchyId(hierarchyId);
  }
  public Long getHierarchyId() {
    return entity.getHierarchyId();
  }
  
  public void setLft(Long lft) {
    entity.setLft(lft);
  }
  public Long getLft() {
    return entity.getLft();
  }

  public void setRgt(Long rgt) {
    entity.setRgt(rgt);
  }
  public Long getRgt() {
    return entity.getRgt();
  }

  @Transactional
  public void setAsHierarchyRoot(Long hierarchyId) 
      throws HierarchyIdAlreadySetException, HierarchyRootExistsException {
    ensureEntityDoesNotBelongToAnyHierarchy(entity);
    ensureHierarchyRootIsNotSet(hierarchyId);
    
    setHierarchyId(hierarchyId);
    setLft(1L);
    setRgt(2L);
  }

  @Transactional
  public void addChild(T child) 
      throws HierarchyIdAlreadySetException, HierarchyIdNotSetException {
    ensureEntityBelongsToAHierarchy(entity);
    ensureEntityDoesNotBelongToAnyHierarchy(child);
    
    Long childLft;
    Long childRgt;
    
    T rightMostChild = findRightMostChild();
    if (rightMostChild == null) {
      childLft = getLft() + 1;
      childRgt = childLft + 1;
      
      incrementEntitiesLft(findEntitiesWhichLftIsGreaterThanOrEqualTo(childLft), 2L);
      incrementEntitiesRgt(findEntitiesWhichRgtIsGreaterThan(getLft()), 2L);
    } else {
      childLft = rightMostChild.getRgt() + 1;
      childRgt = childLft + 1;
      
      incrementEntitiesLft(findEntitiesWhichLftIsGreaterThan(rightMostChild.getRgt()), 2L);
      incrementEntitiesRgt(findEntitiesWhichRgtIsGreaterThan(rightMostChild.getRgt()), 2L);
    }
    
    child.setLft(childLft);
    child.setRgt(childRgt);
    child.setHierarchyId(getHierarchyId());
  }

  @Transactional
  public void removeChild(T child)
      throws NotADescendantException, HierarchyIdNotSetException, NotInTheSameHierarchyException {
    ensureEntityBelongsToAHierarchy(entity);
    ensureEntityIsDescendant(child);
    
    Set<T> childSubTree = child.findSubTree();
    
    Long decrement = child.getRgt() - child.getLft() + 1;
    decrementEntitiesLftWith(findEntitiesWhichLftIsGreaterThan(child.getRgt()), decrement);
    decrementEntitiesRgtWith(findEntitiesWhichRgtIsGreaterThan(child.getRgt()), decrement);
    
    resetEntities(childSubTree);
  }
  
  public T findHierarchyRoot(Long hierarchyId) throws HierarchyRootDoesNotExistException {
    return entity.findHierarchyRoot(hierarchyId);
  }
  
  public List<T> findAncestors() {
    return entity.findAncestors();
  }
  
  public T findParent() {
    return entity.findParent();
  }
  
  public T findRightMostChild() {
    return entity.findRightMostChild();
  }
  
  public Set<T> findEntitiesWhichLftIsGreaterThanOrEqualTo(Long value) {
    return entity.findEntitiesWhichLftIsGreaterThanOrEqualTo(value);
  }
  
  public Set<T> findEntitiesWhichLftIsGreaterThan(Long value) {
    return entity.findEntitiesWhichLftIsGreaterThan(value);
  }
  
  public Set<T> findEntitiesWhichRgtIsGreaterThan(Long value) {
    return entity.findEntitiesWhichRgtIsGreaterThan(value);
  }
  
  public Set<T> findSubTree() {
    return entity.findSubTree();
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
  
  private void ensureEntityBelongsToAHierarchy(T entity)
      throws HierarchyIdNotSetException {
    if (entity.getHierarchyId() == null) {
      throw new HierarchyIdNotSetException();
    }
  }
  
  private void ensureEntityDoesNotBelongToAnyHierarchy(T entity)
      throws HierarchyIdAlreadySetException {
    if (entity.getHierarchyId() != null) {
      throw new HierarchyIdAlreadySetException();
    }
  }
  
  private void ensureEntityIsInTheSameHierarchy(T entity)
      throws NotInTheSameHierarchyException {
    if (this.getHierarchyId() == null || this.getHierarchyId() != entity.getHierarchyId())
      throw new NotInTheSameHierarchyException();
  }
  
  private void ensureEntityIsDescendant(T entity)
      throws NotADescendantException, NotInTheSameHierarchyException {
    ensureEntityIsInTheSameHierarchy(entity);
    if (!(this.getLft() < entity.getLft() && entity.getRgt() < this.getRgt()))
      throw new NotADescendantException();
  }
  
  private void ensureHierarchyRootIsNotSet(Long hierarchy)
      throws HierarchyRootExistsException {
    try {
      findHierarchyRoot(hierarchy);
      
      throw new HierarchyRootExistsException();
    } catch (HierarchyRootDoesNotExistException e) {
      return;
    }
  }
}
