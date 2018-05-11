package hacker.works.mpttjpa;

import hacker.works.mpttjpa.sample.TagTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@DataJpaTest
public class MpttHierarchicalEntityRepoImplTest {

  @TestConfiguration
  static class TreeRepoConfiguration {

    @PersistenceContext
    public EntityManager entityManager;

    @SuppressWarnings("unused")
    @Bean
    public TagTreeRepo treeRepo() {
      TagTreeRepo result = new TagTreeRepo();
      result.setClazz(TagTree.class);
      result.entityManager = this.entityManager;
      return result;
    }
  }

  static class TagTreeRepo extends MpttHierarchicalEntityRepoImpl<TagTree> {}

  @Autowired
  public TagTreeRepo repo;

  @Test
  public void
  should_set_root_when_there_is_none() 
      throws MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException,
        MpttHierarchicalEntityRepo.HierarchyRootExistsException {

    TagTree root = new TagTree("root");
    repo.setAsHierarchyRoot(root,0L);
    
    assertThat(root.getHierarchyId(), equalTo(0L));
    assertThat(root.getLft(), equalTo(1l));
    assertThat(root.getRgt(), equalTo(2l));
  }

  @Test public void
  fails_to_set_as_root_when_part_already_belongs_to_other_hierarchy() {
    try {
      TagTree root = new TagTree("root");
      root.setHierarchyId(0L);

      repo.setAsHierarchyRoot(root,1L);

      fail("Should fail to set as hierarchy root when the hierarchy id is already set different");
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException e) {
      // pass
    } catch (MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  fails_to_set_as_root_when_root_already_exists() {
    try {
      TagTree rootA = new TagTree("rootA");
      TagTree rootB = new TagTree("rootB");

      repo.setAsHierarchyRoot(rootA,13L);
      repo.setAsHierarchyRoot(rootB, 13L);

      fail("Should fail to set as hierarchy root when hierarchy root already exists");
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException e) {
      fail();
    } catch (MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      // pass
    }
  }

  @Test public void
  should_add_first_child_to_root() {
    try {
      TagTree root = new TagTree("root");
      TagTree child = new TagTree("child");

      repo.setAsHierarchyRoot(root,13l);
      repo.addChild(root, child);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(4l));

      assertThat(child.getLft(), equalTo(2l));
      assertThat(child.getRgt(), equalTo(3l));
      assertThat(child.getHierarchyId(), equalTo(root.getHierarchyId()));
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  fails_to_add_child_which_is_already_part_of_any_hierarchy() {
    try {
      TagTree rootA = new TagTree("rootA");
      repo.setAsHierarchyRoot(rootA,13l);

      TagTree rootB = new TagTree("rootB");
      repo.setAsHierarchyRoot(rootB, 14l);

      TagTree child = new TagTree("child");

      repo.addChild(rootA, child);
      repo.addChild(rootB, child);

      fail("Should fail to add child which is already part of a hierarchy");
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      // pass
    } catch (MpttHierarchicalEntityRepo.HierarchyIdNotSetException e) {
      fail();
    }
  }

  @Test public void
  should_add_child_to_child_to_root() {
    try {
      TagTree root = new TagTree("root");
      TagTree level1Child = new TagTree("level1");
      TagTree level2Child = new TagTree("level2");

      repo.setAsHierarchyRoot(root,13L);
      repo.addChild(root, level1Child);
      repo.addChild(level1Child, level2Child);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(6l));

      assertThat(level1Child.getLft(), equalTo(2l));
      assertThat(level1Child.getRgt(), equalTo(5l));
      assertThat(level1Child.getHierarchyId(), equalTo(root.getHierarchyId()));

      assertThat(level2Child.getLft(), equalTo(3l));
      assertThat(level2Child.getRgt(), equalTo(4l));
      assertThat(level2Child.getHierarchyId(), equalTo(root.getHierarchyId()));

    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_add_second_child_to_root() {
    try {
      TagTree root = new TagTree("root");
      repo.setAsHierarchyRoot(root,0L);
      TagTree child1 = new TagTree("child1");
      TagTree child2 = new TagTree("child2");

      repo.addChild(root, child1);
      repo.addChild(root, child2);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(6l));

      assertThat(child1.getLft(), equalTo(2l));
      assertThat(child1.getRgt(), equalTo(3l));

      assertThat(child2.getLft(), equalTo(4l));
      assertThat(child2.getRgt(), equalTo(5l));

    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_add_second_level_child_to_first_level_child() {
    try {
      TagTree root = new TagTree("root");
      repo.setAsHierarchyRoot(root,0L);
      TagTree child1Level1 = new TagTree("level1-child1");
      TagTree child1Level2 = new TagTree("level2-child1");
      TagTree child2Level1 = new TagTree("level2-child2");

      repo.addChild(root, child1Level1);
      repo.addChild(root, child2Level1);
      repo.addChild(child1Level1, child1Level2);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(8l));

      assertThat(child1Level1.getLft(), equalTo(2l));
      assertThat(child1Level1.getRgt(), equalTo(5l));

      assertThat(child2Level1.getLft(), equalTo(6l));
      assertThat(child2Level1.getRgt(), equalTo(7l));

      assertThat(child1Level2.getLft(), equalTo(3l));
      assertThat(child1Level2.getRgt(), equalTo(4l));
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  fail_when_adding_child_to_an_entity_which_is_not_in_hierarchy() {
    try {
      TagTree root = new TagTree("root");
      TagTree child = new TagTree("child");

      repo.addChild(root, child);

      fail("Should fail to add child to a entity which is not part of a hierarchy");
    } catch (MpttHierarchicalEntityRepo.HierarchyIdNotSetException e) {
      // pass
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException e) {
      fail();
    }
  }

  @Test public void
  should_remove_the_only_child_of_hierarchy_root() {
    try {
      TagTree root = new TagTree("root");
      TagTree child = new TagTree("child");

      repo.setAsHierarchyRoot(root,13l);
      repo.addChild(root, child);
      repo.removeChild(root, child);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(2l));

      assertThat(child.getHierarchyId(), nullValue());
      assertThat(child.getLft(), nullValue());
      assertThat(child.getRgt(), nullValue());

    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException
            | MpttHierarchicalEntityRepo.NotInTheSameHierarchyException
            | MpttHierarchicalEntityRepo.NotADescendantException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException e) {
      fail();
    }
  }

  @Test public void
  fails_when_trying_to_remove_child_which_belongs_to_other_hierarchy() {
    try {
      TagTree rootA = new TagTree("rootA");
      TagTree rootB = new TagTree("rootB");
      TagTree childB = new TagTree("childB");

      repo.setAsHierarchyRoot(rootA, 13L);
      repo.setAsHierarchyRoot(rootB, 14L);
      repo.addChild(rootB, childB);

      repo.removeChild(rootA, childB);

      fail("Should fail to remove child which belongs to a different hierarchy");
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException
            | MpttHierarchicalEntityRepo.NotADescendantException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException e) {
      fail();
    } catch (MpttHierarchicalEntityRepo.NotInTheSameHierarchyException e) {
      // pass
    }
  }

  @Test public void
  fails_when_trying_to_remove_child_which_is_not_descendant() {
    try {
      TagTree rootA = new TagTree("rootA");
      TagTree childA = new TagTree("childA");
      TagTree childB = new TagTree("ChildB");

      repo.setAsHierarchyRoot(rootA, 13L);
      repo.addChild(rootA, childA);
      repo.addChild(childA, childB);

      repo.removeChild(childB, childA);

      fail("Should fail to remove child which is not a descendant");
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException
            | MpttHierarchicalEntityRepo.NotInTheSameHierarchyException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException e) {
      fail();
    } catch (MpttHierarchicalEntityRepo.NotADescendantException e) {
      // pass
    }
  }

  @Test public void
  should_remove_first_child_in_hierarchy_with_two_children() {
    try {
      TagTree root = new TagTree("root");
      TagTree childA = new TagTree("childA");
      TagTree childB = new TagTree("childB");

      repo.setAsHierarchyRoot(root, 13L);
      repo.addChild(root, childA);
      repo.addChild(root, childB);

      repo.removeChild(root, childA);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(4l));
      assertThat(childB.getLft(), equalTo(2l));
      assertThat(childB.getRgt(), equalTo(3l));

      assertThat(childA.getHierarchyId(), nullValue());
      assertThat(childA.getLft(), nullValue());
      assertThat(childA.getRgt(), nullValue());
    } catch (MpttHierarchicalEntityRepo.NotInTheSameHierarchyException
            | MpttHierarchicalEntityRepo.NotADescendantException
            | MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_remove_second_child_in_hierarchy_with_two_children() {
    try {
      TagTree root = new TagTree("root");
      TagTree childA = new TagTree("childA");
      TagTree childB = new TagTree("chihldB");

      repo.setAsHierarchyRoot(root, 13l);
      repo.addChild(root, childA);
      repo.addChild(root, childB);

      repo.removeChild(root, childB);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(4l));
      assertThat(childA.getLft(), equalTo(2l));
      assertThat(childA.getRgt(), equalTo(3l));

      assertThat(childB.getHierarchyId(), nullValue());
      assertThat(childB.getLft(), nullValue());
      assertThat(childB.getRgt(), nullValue());
    } catch (MpttHierarchicalEntityRepo.NotInTheSameHierarchyException
            | MpttHierarchicalEntityRepo.NotADescendantException
            | MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_remove_first_level_child_from_two_level_hierarchy() {
    try {
      TagTree root = new TagTree("root");
      TagTree child1Level1 = new TagTree("level1-child1");
      TagTree child2Level1 = new TagTree("level1-child2");
      TagTree child1Level2 = new TagTree("level2-child1");

      repo.setAsHierarchyRoot(root, 13L);

      repo.addChild(root, child1Level1);
      repo.addChild(root, child2Level1);
      repo.addChild(child1Level1, child1Level2);

      repo.removeChild(root, child1Level1);

      assertThat(root.getLft(), equalTo(1l));
      assertThat(root.getRgt(), equalTo(4l));
      assertThat(child2Level1.getLft(), equalTo(2l));
      assertThat(child2Level1.getRgt(), equalTo(3l));

      assertThat(child1Level1.getHierarchyId(), nullValue());
      assertThat(child1Level1.getLft(), nullValue());
      assertThat(child1Level1.getRgt(), nullValue());

      assertThat(child1Level2.getHierarchyId(), nullValue());
      assertThat(child1Level2.getLft(), nullValue());
      assertThat(child1Level2.getRgt(), nullValue());
    } catch (MpttHierarchicalEntityRepo.NotInTheSameHierarchyException
            | MpttHierarchicalEntityRepo.NotADescendantException
            | MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  fails_to_find_root_which_does_not_exist() {
    try {
      repo.findHierarchyRoot(15550L);
      fail("Should fail to find unexisting root");
    } catch (MpttHierarchicalEntityRepo.HierarchyRootDoesNotExistException e) {
      // pass
    }
  }

  @Test public void
  should_find_existing_hierarchy_root() {
    try {
      TagTree root = new TagTree("root");
      repo.setAsHierarchyRoot(root, 123L);

      TagTree actual = repo.findHierarchyRoot(123L);
      assertThat(actual, notNullValue());
      assertThat(actual.getHierarchyId(), equalTo(root.getHierarchyId()));
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyRootDoesNotExistException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_not_find_ancestors_for_part_which_is_not_in_hierarchy() {
    TagTree tag = new TagTree("some");

    assertEquals(0, repo.findAncestors(tag).size());
  }

  @Test public void
  should_find_ancestors_for_three_level_deep_hierarchy() {
    try {
      TagTree root = new TagTree("root");
      TagTree childA = new TagTree("childA");
      TagTree childB = new TagTree("childB");
      TagTree childC = new TagTree("childC");

      repo.setAsHierarchyRoot(root, 0L);
      repo.addChild(root, childA);
      repo.addChild(childA, childB);
      repo.addChild(childB, childC);

      ArrayList<TagTree> elements = new ArrayList<>();
      elements.add(root);
      elements.add(childA);
      elements.add(childB);
      elements.add(childC);

      for (int i = 0; i <= 3; i++) {
        assertEquals(elements.subList(0, i), repo.findAncestors(elements.get(i)));
      }
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_find_only_first_child_of_root() {
    try {
      TagTree root = new TagTree("root");
      TagTree child = new TagTree("child");

      repo.setAsHierarchyRoot(root,13l);
      repo.addChild(root, child);

      List<TagTree> actual = repo.findChildren(root);
      assertEquals(
          new ArrayList<TagTree>(){{
            add(child);
          }},
          actual);
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_find_all_level_one_children_of_root() {
    try {
      TagTree root = new TagTree("root");
      TagTree child1 = new TagTree("child1");
      TagTree child2 = new TagTree("child2");
      TagTree child3 = new TagTree("child3");

      repo.setAsHierarchyRoot(root,13l);
      repo.addChild(root, child1);
      repo.addChild(root, child2);
      repo.addChild(root, child3);

      List<TagTree> actual = repo.findChildren(root);
      assertEquals(
          new ArrayList<TagTree>(){{
            add(child1);
            add(child2);
            add(child3);
          }},
          actual);
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_find_only_level_one_children() {
    try {
      TagTree root = new TagTree("root");
      TagTree level1child1 = new TagTree("level1-child1");
      TagTree level1child2 = new TagTree("level1-child2");
      TagTree level1child3 = new TagTree("level1-child3");
      TagTree level2child1 = new TagTree("level2-child1");
      TagTree level2child2 = new TagTree("level2-child2");
      TagTree level2child3 = new TagTree("level2-child3");


      repo.setAsHierarchyRoot(root,13l);
      repo.addChild(root, level1child1);
      repo.addChild(root, level1child2);
      repo.addChild(root, level1child3);
      repo.addChild(level1child1, level2child1);
      repo.addChild(level1child2, level2child2);
      repo.addChild(level1child3, level2child3);

      List<TagTree> actual = repo.findChildren(root);
      assertEquals(
          new ArrayList<TagTree>(){{
            add(level1child1);
            add(level1child2);
            add(level1child3);
          }},
          actual);
      assertEquals(new ArrayList<TagTree>(){{ add(level2child1); }},
          repo.findChildren(level1child1));
      assertEquals(new ArrayList<TagTree>(){{ add(level2child2); }},
          repo.findChildren(level1child2));
      assertEquals(new ArrayList<TagTree>(){{ add(level2child3); }},
          repo.findChildren(level1child3));
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }

  @Test public void
  should_find_parents_for_three_level_deep_hierarchy() {
    try {
      TagTree root = new TagTree("root");
      TagTree childA = new TagTree("childA");
      TagTree childB = new TagTree("childB");
      TagTree childC = new TagTree("childC");

      repo.setAsHierarchyRoot(root, 0L);
      repo.addChild(root, childA);
      repo.addChild(childA, childB);
      repo.addChild(childB, childC);

      ArrayList<TagTree> elements = new ArrayList<>();
      elements.add(root);
      elements.add(childA);
      elements.add(childB);
      elements.add(childC);

      for (int i = 1; i <= 3; i++) {
        assertEquals(elements.get(i-1), repo.findParent(elements.get(i)));
      }
    } catch (MpttHierarchicalEntityRepo.HierarchyIdAlreadySetException
            | MpttHierarchicalEntityRepo.HierarchyIdNotSetException
            | MpttHierarchicalEntityRepo.HierarchyRootExistsException e) {
      fail();
    }
  }
}
