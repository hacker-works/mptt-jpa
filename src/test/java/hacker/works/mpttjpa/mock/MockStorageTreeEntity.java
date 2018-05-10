package hacker.works.mpttjpa.mock;

import hacker.works.mpttjpa.MpttHierarchicalEntity;
import hacker.works.mpttjpa.exceptions.*;
import hacker.works.mpttjpa.impl.MpttHierarchicalEntityImpl;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MockStorageTreeEntity
    implements MpttHierarchicalEntity<MockStorageTreeEntity> {
  
  private static Set<MockStorageTreeEntity> mockStorage = new HashSet<>();
  
  private Long id;
  private Long hierarchyId;
  private Long lft;
  private Long rgt;
  
  private MpttHierarchicalEntityImpl<MockStorageTreeEntity> mpttImpl =
          new MpttHierarchicalEntityImpl<>(this);
  
  public static void clearMockStorage() {
    mockStorage.clear();
  }

  public void setAsHierarchyRoot(Long hierarchyId) 
      throws HierarchyIdAlreadySetException, HierarchyRootExistsException{
    mpttImpl.setAsHierarchyRoot(hierarchyId);
    this.persist();
  }

  public void addChild(MockStorageTreeEntity child)
          throws HierarchyIdAlreadySetException, HierarchyIdNotSetException {
    mpttImpl.addChild(child);
    child.persist();
  }
  
  public void removeChild(MockStorageTreeEntity child)
      throws NotADescendantException,
          HierarchyIdNotSetException,
          NotInTheSameHierarchyException {
    mpttImpl.removeChild(child);
  }
  
  public MockStorageTreeEntity findHierarchyRoot(Long hierarchyId)
      throws HierarchyRootDoesNotExistException {
    if (hierarchyId == null)
      throw new HierarchyRootDoesNotExistException();
    
    for (MockStorageTreeEntity entity : mockStorage) {
      if (entity.getHierarchyId() == hierarchyId  && entity.lft == 1) {
        return entity;
      }
    }
  
    throw new HierarchyRootDoesNotExistException();
  }
  
  public MockStorageTreeEntity findRightMostChild() {
    
    if (hierarchyId == null)
      return null;
    
    for (MockStorageTreeEntity entity : mockStorage) {
      if (entity.hierarchyId != hierarchyId
        || entity.getLft() == null
        || entity.getRgt() == null) continue;
      
      if (entity.getRgt() == getRgt() - 1)
        return entity;
    }
    return null;
  }
  
  public Set<MockStorageTreeEntity>
  findEntitiesWhichLftIsGreaterThanOrEqualTo(Long value) {
        
    HashSet<MockStorageTreeEntity> result = new HashSet<>();
    
    if (hierarchyId == null)
      return result;
    
    for (MockStorageTreeEntity entity : mockStorage) {
      if (entity.hierarchyId != hierarchyId
        || entity.getLft() == null
        || entity.getRgt() == null) continue;

      if (entity.getLft() >= value)
        result.add(entity);
    }
    
    return result;
  }
  
  public Set<MockStorageTreeEntity>
  findEntitiesWhichLftIsGreaterThan(Long value) {
    
    HashSet<MockStorageTreeEntity> result = new HashSet<>();
    
    if (hierarchyId == null)
      return result;
    
    for (MockStorageTreeEntity entity : mockStorage) {
      if (entity.hierarchyId != hierarchyId
        || entity.getLft() == null
        || entity.getRgt() == null) continue;

      if (entity.getLft() > value)
        result.add(entity);
    }
    
    return result;
  }
  
  public Set<MockStorageTreeEntity>
  findEntitiesWhichRgtIsGreaterThan(Long value) {
    
    HashSet<MockStorageTreeEntity> result = new HashSet<>();
    
    if (hierarchyId == null)
      return result;
    
    for (MockStorageTreeEntity entity : mockStorage) {
      if (entity.hierarchyId != hierarchyId
        || entity.getLft() == null
        || entity.getRgt() == null) continue;

      if (entity.getRgt() > value)
        result.add(entity);
    }
    
    return result;
  }
  
  @Override
  public Set<MockStorageTreeEntity>
  findSubTree() {
    
    HashSet<MockStorageTreeEntity> result = new HashSet<>();
    
    if (hierarchyId == null)
      return result;
    
    for (MockStorageTreeEntity entity : mockStorage) {
      if (entity.hierarchyId != hierarchyId
        || entity.getLft() == null
        || entity.getRgt() == null) continue;

      if (getLft() <= entity.getLft() && entity.getRgt() <= getRgt())
        result.add(entity);
    }
    
    return result;
  }
  
  @Override
  public List<MockStorageTreeEntity>
  findAncestors() {
    return null;
  }
  
  @Override
  public MockStorageTreeEntity findParent() {
    return null;
  }
  
  private void persist() {
    if (id == null) {
      id = (long) mockStorage.size();
      mockStorage.add(this);
    } else {
      throw new EntityExistsException();
    }
  }

  @SuppressWarnings(value = "unused")
  public void remove() {
    if (mockStorage.contains(this)) {
      mockStorage.remove(this);
    } else {
      throw new EntityNotFoundException();
    }
  }

  public void setHierarchyId(Long hierarchyId) {
    this.hierarchyId = hierarchyId;
  }
  public Long getHierarchyId() {
    return hierarchyId;
  }
  
  public void setLft(Long lft) {
    this.lft = lft;
  }
  public Long getLft() {
    return lft;
  }

  public void setRgt(Long rgt) {
    this.rgt = rgt;
  }
  public Long getRgt() {
    return rgt;
  }

}
