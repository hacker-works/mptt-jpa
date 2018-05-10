package hacker.works.mpttjpa.impl;

import hacker.works.mpttjpa.exceptions.*;
import hacker.works.mpttjpa.mock.MockStorageTreeEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MpttHierarchicalEntityImplTest {
  
  private MockStorageTreeEntity root;
  
  @Before
  public void setUp() {
    root = new MockStorageTreeEntity();
  }
  
  @After
  public void tearDown() {
    MockStorageTreeEntity.clearMockStorage();
  }

  @Test public void 
  should_set_hierarchy_root_when_there_is_no_other_root() 
      throws HierarchyIdAlreadySetException, HierarchyRootExistsException {
    root.setAsHierarchyRoot(13L);
    
    assertThat(root.getHierarchyId(), equalTo(13L));
    assertThat(root.getLft(), equalTo(1L));
    assertThat(root.getRgt(), equalTo(2L));
  }
  
  @Test public void 
  fails_to_set_as_hierarchy_root_when_entity_belongs_to_other_hierarchy() {
    try {
      root.setHierarchyId(12L);
      root.setAsHierarchyRoot(13L);
      fail("Should fail to set as hierarchy root when the hierarchy id is " +
        "already set different");
    } catch (HierarchyIdAlreadySetException e) {
      // pass
    } catch (HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  fails_to_set_as_hierarchy_root_when_hierarchy_root_already_exists() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity newRootEntity = new MockStorageTreeEntity();
      
      newRootEntity.setAsHierarchyRoot(13L);
      fail("Should fail to set as hierarchy root when hierarchy root already " +
        "exists");
    } catch (HierarchyIdAlreadySetException e) {
      fail();
    } catch (HierarchyRootExistsException e) {
      // pass
    }
  }
  
  @Test public void 
  should_add_first_child_to_hierarchy_root() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity child = new MockStorageTreeEntity();
      
      root.addChild(child);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(4L));
      
      assertThat(child.getLft(), equalTo(2L));
      assertThat(child.getRgt(), equalTo(3L));
      assertThat(child.getHierarchyId(), equalTo(root.getHierarchyId()));
    } catch (HierarchyIdAlreadySetException
        | HierarchyIdNotSetException
        | HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  fails_to_add_child_which_is_already_part_of_any_hierarchy() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity otherRoot = new MockStorageTreeEntity();
      otherRoot.setAsHierarchyRoot(14L);
      
      MockStorageTreeEntity otherChild = new MockStorageTreeEntity();

      otherRoot.addChild(otherChild);
      
      root.addChild(otherChild);
      fail("Should fail to add child which is already part of a hierarchy");
    } catch (HierarchyIdAlreadySetException e) {
      // pass
    } catch (HierarchyIdNotSetException | HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  should_add_child_to_child_to_hierarchy_root() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity childLevel1 = new MockStorageTreeEntity();
      
      MockStorageTreeEntity childLevel2 = new MockStorageTreeEntity();
      
      root.addChild(childLevel1);
      childLevel1.addChild(childLevel2);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(6L));
      
      assertThat(childLevel1.getLft(), equalTo(2L));
      assertThat(childLevel1.getRgt(), equalTo(5L));
      assertThat(childLevel1.getHierarchyId(), equalTo(root.getHierarchyId()));
      
      assertThat(childLevel2.getLft(), equalTo(3L));
      assertThat(childLevel2.getRgt(), equalTo(4L));
      assertThat(childLevel2.getHierarchyId(), equalTo(root.getHierarchyId()));
    } catch (HierarchyIdAlreadySetException
        | HierarchyIdNotSetException
        | HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  should_add_second_child_to_hierarchy_root() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity child1 = new MockStorageTreeEntity();
      
      MockStorageTreeEntity child2 = new MockStorageTreeEntity();
      
      root.addChild(child1);
      root.addChild(child2);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(6L));
      
      assertThat(child1.getLft(), equalTo(2L));
      assertThat(child1.getRgt(), equalTo(3L));
      
      assertThat(child2.getLft(), equalTo(4L));
      assertThat(child2.getRgt(), equalTo(5L));
    } catch (HierarchyIdAlreadySetException
        | HierarchyIdNotSetException
        | HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  should_add_second_level_child_to_first_level_child() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity child1Level1 = new MockStorageTreeEntity();
      
      MockStorageTreeEntity child2Level1 = new MockStorageTreeEntity();
      
      root.addChild(child1Level1);
      root.addChild(child2Level1);
      
      MockStorageTreeEntity childLevel2 = new MockStorageTreeEntity();
      
      child1Level1.addChild(childLevel2);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(8L));
      
      assertThat(child1Level1.getLft(), equalTo(2L));
      assertThat(child1Level1.getRgt(), equalTo(5L));
      
      assertThat(child2Level1.getLft(), equalTo(6L));
      assertThat(child2Level1.getRgt(), equalTo(7L));
      
      assertThat(childLevel2.getLft(), equalTo(3L));
      assertThat(childLevel2.getRgt(), equalTo(4L));
    } catch (HierarchyIdAlreadySetException
        | HierarchyIdNotSetException
        | HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  fail_when_adding_child_to_an_entity_which_is_not_in_hierarchy() {
    try {
      MockStorageTreeEntity child = new MockStorageTreeEntity();
      root.addChild(child);

      fail("Should fail to add child to a entity which is not part of a " +
        "hierarchy");
    } catch (HierarchyIdNotSetException e) {
      // pass
    } catch (HierarchyIdAlreadySetException e) {
      fail();
    }
  }
  
  @Test public void 
  should_remove_the_only_child_of_hierarchy_root() {
    try {
      root.setAsHierarchyRoot(13L);
      MockStorageTreeEntity child = new MockStorageTreeEntity();
      root.addChild(child);
      
      root.removeChild(child);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(2L));
      
      assertThat(child.getHierarchyId(), nullValue());
      assertThat(child.getLft(), nullValue());
      assertThat(child.getRgt(), nullValue());
    } catch (HierarchyIdAlreadySetException
        | HierarchyRootExistsException
        | NotInTheSameHierarchyException
        | NotADescendantException
        | HierarchyIdNotSetException e) {
      fail();
    }
  }
  
  @Test public void 
  fails_when_trying_to_remove_child_which_belongs_to_other_hierarchy() {
    try {
      root.setAsHierarchyRoot(13L);
      MockStorageTreeEntity child = new MockStorageTreeEntity();
      root.addChild(child); 
      
      MockStorageTreeEntity otherRoot = new MockStorageTreeEntity();
      otherRoot.setAsHierarchyRoot(14L);
      MockStorageTreeEntity otherChild = new MockStorageTreeEntity();
      otherRoot.addChild(otherChild);
      
      root.removeChild(otherChild);
      fail("Should fail to remove child which belongs to a different " +
        "hierarchy");
    } catch (HierarchyIdAlreadySetException
        | HierarchyRootExistsException
        | NotADescendantException
        | HierarchyIdNotSetException e) {
      fail();
    } catch (NotInTheSameHierarchyException e) {
      // pass
    }
  }
  
  @Test public void 
  fails_when_trying_to_remove_child_which_is_not_descendant() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity childLevel1 = new MockStorageTreeEntity();
      
      root.addChild(childLevel1); 
      
      MockStorageTreeEntity childLevel2 = new MockStorageTreeEntity();
      
      childLevel1.addChild(childLevel2);
      
      childLevel2.removeChild(childLevel1);
      fail("Should fail to remove child which is not a descendant");
    } catch (HierarchyIdAlreadySetException
        | HierarchyRootExistsException
        | HierarchyIdNotSetException
        | NotInTheSameHierarchyException e) {
      fail();
    } catch (NotADescendantException e) {
      // pass
    }
  }
  
  @Test public void 
  should_remove_first_child_in_hierarchy_with_two_children() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity child1 = new MockStorageTreeEntity();
      MockStorageTreeEntity child2 = new MockStorageTreeEntity();
      
      root.addChild(child1);
      root.addChild(child2);
      
      root.removeChild(child1);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(4L));
      assertThat(child2.getLft(), equalTo(2L));
      assertThat(child2.getRgt(), equalTo(3L));
      
      assertThat(child1.getHierarchyId(), nullValue());
      assertThat(child1.getLft(), nullValue());
      assertThat(child1.getRgt(), nullValue());
    } catch (NotInTheSameHierarchyException
        | NotADescendantException
        | HierarchyIdAlreadySetException
        | HierarchyIdNotSetException
        | HierarchyRootExistsException e) {
      fail();
    }
  }
  
  @Test public void 
  should_remove_second_child_in_hierarchy_with_two_children() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity child1 = new MockStorageTreeEntity();
      MockStorageTreeEntity child2 = new MockStorageTreeEntity();
      
      root.addChild(child1);
      root.addChild(child2);
      
      root.removeChild(child2);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(4L));
      assertThat(child1.getLft(), equalTo(2L));
      assertThat(child1.getRgt(), equalTo(3L));
      
      assertThat(child2.getHierarchyId(), nullValue());
      assertThat(child2.getLft(), nullValue());
      assertThat(child2.getRgt(), nullValue());
    } catch (NotInTheSameHierarchyException | HierarchyIdNotSetException e) {
      fail();
    } catch (NotADescendantException
        | HierarchyIdAlreadySetException
        | HierarchyRootExistsException e) {
      // pass
    }
  }
  
  @Test public void 
  should_remove_first_level_child_from_two_level_hierarchy() {
    try {
      root.setAsHierarchyRoot(13L);
      
      MockStorageTreeEntity child1Level1 = new MockStorageTreeEntity();
      MockStorageTreeEntity child2Level1 = new MockStorageTreeEntity();
      
      root.addChild(child1Level1);
      root.addChild(child2Level1);
      
      MockStorageTreeEntity child1Level2 = new MockStorageTreeEntity();
      
      child1Level1.addChild(child1Level2);
      
      root.removeChild(child1Level1);
      
      assertThat(root.getLft(), equalTo(1L));
      assertThat(root.getRgt(), equalTo(4L));
      assertThat(child2Level1.getLft(), equalTo(2L));
      assertThat(child2Level1.getRgt(), equalTo(3L));
      
      assertThat(child1Level1.getHierarchyId(), nullValue());
      assertThat(child1Level1.getLft(), nullValue());
      assertThat(child1Level1.getRgt(), nullValue());
      
      assertThat(child1Level2.getHierarchyId(), nullValue());
      assertThat(child1Level2.getLft(), nullValue());
      assertThat(child1Level2.getRgt(), nullValue());
    } catch (NotInTheSameHierarchyException
        | NotADescendantException
        | HierarchyIdNotSetException
        | HierarchyRootExistsException
        | HierarchyIdAlreadySetException e) {
      fail();
    }
  }

}
