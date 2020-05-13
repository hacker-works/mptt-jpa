package works.hacker.repo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import works.hacker.config.TagTreeJpaConfig;
import works.hacker.model.TagTree;
import works.hacker.mptt.MpttEntity;
import works.hacker.mptt.MpttRepository;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TagTreeJpaConfig.class}, loader = AnnotationConfigContextLoader.class)
@Transactional
@DirtiesContext
public class TagTreeRepoTest {
  private final Logger LOG = LoggerFactory.getLogger(TagTreeRepoTest.class);

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Resource
  TagTreeRepository tagTreeRepo;

  @Before
  public void init() {
    tagTreeRepo.setEntityClass(TagTree.class);
  }

  @Test
  public void giveSaved_whenFindByName_thenOK() {
    assertThat(tagTreeRepo.count(), is(0L));

    TagTree expected = new TagTree("test-01");
    tagTreeRepo.save(expected);
    assertThat(tagTreeRepo.count(), is(1L));

    TagTree actual = tagTreeRepo.findByName(expected.getName());
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getName(), is(expected.getName()));
  }

  @Test
  public void givenNoTree_whenConstructed_thenHasNoTreeId() {
    TagTree actual = new TagTree("test");
    assertThat(actual.hasTreeId(), is(false));
  }

  @Test
  public void givenNoTree_whenStartTree_thenOK() throws MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");

    Long treeId = tagTreeRepo.startTree(root);

    assertThat(tagTreeRepo.count(), is(1L));

    TagTree actual = tagTreeRepo.findByName(root.getName());
    assertThat(actual.getTreeId(), not(MpttEntity.NO_TREE_ID));
    assertThat(actual.getTreeId(), is(treeId));
    assertThat(actual.getLft(), is(MpttEntity.DEFAULT_LFT));
    assertThat(actual.getRgt(), is(MpttEntity.DEFAULT_RGT));
  }

  @Test
  public void givenTree_whenStartTreeWithUsedRootNode_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree {
    Long treeId = tagTreeRepo.startTree(new TagTree("root"));

    exceptionRule.expect(MpttRepository.NodeAlreadyAttachedToTree.class);
    exceptionRule.expectMessage(String.format("Node already has treeId set to %d", treeId));
    TagTree root = tagTreeRepo.findByName("root");
    tagTreeRepo.startTree(root);
  }

  @Test
  public void givenTree_whenFindTreeRoot_thenOK() throws MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    Long treeId = tagTreeRepo.startTree(root);

    TagTree actual = tagTreeRepo.findTreeRoot(treeId);
    assertThat(actual, is(root));
  }

  @Test
  public void givenParentNodeNotAttachedToTree_whenAddChild_thenError()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree {
    TagTree parent = new TagTree("parent");
    TagTree child = new TagTree("child");

    exceptionRule.expect(MpttRepository.NodeNotInTree.class);
    exceptionRule.expectMessage(String.format("Parent node not attached to any tree: %s", parent));
    tagTreeRepo.addChild(parent, child);
  }

  @Test
  public void givenChildIsTreeRoot_whenAddChild_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree parent = new TagTree("parent");
    TagTree child = new TagTree("child");

    tagTreeRepo.startTree(parent);
    Long treeId = tagTreeRepo.startTree(child);

    exceptionRule.expect(MpttRepository.NodeAlreadyAttachedToTree.class);
    exceptionRule.expectMessage(String.format("Node already has treeId set to %d", treeId));
    tagTreeRepo.addChild(parent, child);
  }

  @Test
  public void givenEmptyTree_whenFindRightMostChild_thenNull()
      throws MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree actual = tagTreeRepo.findRightMostChild(root);
    assertThat(actual, is(nullValue()));
  }

  @Test
  public void givenEmptyTree_whenAddChild_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    assertThat(tagTreeRepo.count(), is(2L));

    TagTree actualRoot = tagTreeRepo.findByName("root");
    TagTree actualChild = tagTreeRepo.findByName("child");

    assertThat(actualRoot.getLft(), is(1L));
    assertThat(actualRoot.getRgt(), is(4L));
    assertThat(actualChild.getTreeId(), is(root.getTreeId()));
    assertThat(actualChild.getLft(), is(root.getLft() + 1));
    assertThat(actualChild.getRgt(), is(root.getRgt() - 1));
  }

  @Test
  public void givenTreeRoot_whenPrintTree_thenOK() throws MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 2]",
        root.getId(), root.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithChild_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 4]\n"+
        "    └── child (id: %d) [treeId: %d | lft: 2 | rgt: 3]",
        root.getId(), root.getTreeId(),
        child.getId(), child.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithOneChild_whenFindChildren_thenContainsOneChild()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    List<TagTree> actual = tagTreeRepo.findChildren(root);
    assertThat(actual, containsInRelativeOrder(child));
  }

  @Test
  public void givenTreeWithTwoChildren_whenFindChildren_thenContainsTwoChildren()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree child2 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child2);

    List<TagTree> actual = tagTreeRepo.findChildren(root);
    assertThat(actual, containsInRelativeOrder(child1, child2));
  }

  @Test
  public void givenTreeWithTwoChildren_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 6]\n" +
        "    ├── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 3]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 4 | rgt: 5]",
        root.getId(), root.getTreeId(),
        child1.getId(), child1.getTreeId(),
        child2.getId(), child2.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithChildAndSubChild_whenFindChildren_thenContainsOneChild()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    List<TagTree> actual = tagTreeRepo.findChildren(root);
    assertThat(actual.size(), is(1));
    assertThat(actual, containsInRelativeOrder(child));
  }

  @Test
  public void givenTreeWithChildAndSubChild_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 6]\n" +
        "    └── child (id: %d) [treeId: %d | lft: 2 | rgt: 5]\n" +
        "        └── subChild (id: %d) [treeId: %d | lft: 3 | rgt: 4]",
        root.getId(), root.getTreeId(),
        child.getId(), child.getTreeId(),
        subChild.getId(), subChild.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree1_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child1, subChild);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 8]\n" +
        "    ├── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 5]\n" +
        "    │   └── subChild (id: %d) [treeId: %d | lft: 3 | rgt: 4]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 6 | rgt: 7]",
        root.getId(), root.getTreeId(),
        child1.getId(), child1.getTreeId(),
        subChild.getId(), subChild.getTreeId(),
        child2.getId(), child2.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree1_whenfindChildren_thenContainsTwoChildren()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child1, subChild);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    List<TagTree> actual = tagTreeRepo.findChildren(root);
    assertThat(actual.size(), is(2));
    assertThat(actual, containsInRelativeOrder(child1, child2));
  }

  @Test
  public void givenComplexTree2_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child1, subChild);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild, subSubChild);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 10]\n" +
        "    ├── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 7]\n" +
        "    │   └── subChild (id: %d) [treeId: %d | lft: 3 | rgt: 6]\n" +
        "    │       └── subSubChild (id: %d) [treeId: %d | lft: 4 | rgt: 5]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 8 | rgt: 9]",
        root.getId(), root.getTreeId(),
        child1.getId(), child1.getTreeId(),
        subChild.getId(), subChild.getTreeId(),
        subSubChild.getId(), subSubChild.getTreeId(),
        child2.getId(), child2.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree2_whenFindChildren_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child1, subChild);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild, subSubChild);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    List<TagTree> actual1 = tagTreeRepo.findChildren(root);
    assertThat(actual1.size(), is(2));
    assertThat(actual1, containsInRelativeOrder(child1, child2));

    List<TagTree> actual2 = tagTreeRepo.findChildren(child1);
    assertThat(actual2.size(), is(1));
    assertThat(actual2, contains(subChild));

    List<TagTree> actual3 = tagTreeRepo.findChildren(subChild);
    assertThat(actual3.size(), is(1));
    assertThat(actual3, contains(subSubChild));
  }

  @Test
  public void givenComplexTree3_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 14]\n" +
        "    ├── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 9]\n" +
        "    │   ├── subChild-1 (id: %d) [treeId: %d | lft: 3 | rgt: 6]\n" +
        "    │   │   └── subSubChild (id: %d) [treeId: %d | lft: 4 | rgt: 5]\n" +
        "    │   └── subChild-2 (id: %d) [treeId: %d | lft: 7 | rgt: 8]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 10 | rgt: 13]\n" +
        "        └── lastSubChild (id: %d) [treeId: %d | lft: 11 | rgt: 12]",
        root.getId(), root.getTreeId(),
        child1.getId(), child1.getTreeId(),
        subChild1.getId(), subChild1.getTreeId(),
        subSubChild.getId(), subSubChild.getTreeId(),
        subChild2.getId(), subChild2.getTreeId(),
        child2.getId(), child2.getTreeId(),
        lastSubChild.getId(), lastSubChild.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));


    // @formatter:off
    String expectedPartial = String.format(
        ".\n" +
        "└── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 9]\n" +
        "    ├── subChild-1 (id: %d) [treeId: %d | lft: 3 | rgt: 6]\n" +
        "    │   └── subSubChild (id: %d) [treeId: %d | lft: 4 | rgt: 5]\n" +
        "    └── subChild-2 (id: %d) [treeId: %d | lft: 7 | rgt: 8]",
        child1.getId(), child1.getTreeId(),
        subChild1.getId(), subChild1.getTreeId(),
        subSubChild.getId(),  subSubChild.getTreeId(),
        subChild2.getId(), subChild2.getTreeId());
    // @formatter:on
    String actualPartial = tagTreeRepo.printTree(child1);
    assertThat(actualPartial, is(expectedPartial));
  }

  @Test
  public void givenComplexTree3_whenFindChildren_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);

    List<TagTree> actual1 = tagTreeRepo.findChildren(root);
    assertThat(actual1.size(), is(2));
    assertThat(actual1, containsInRelativeOrder(child1, child2));

    List<TagTree> actual2 = tagTreeRepo.findChildren(child1);
    assertThat(actual2.size(), is(2));
    assertThat(actual2, containsInRelativeOrder(subChild1, subChild2));
  }

  @Test
  public void givenParentNotAttachedToTree_whenRemoveChild_thenError()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeNotChildOfParent {
    TagTree parent = new TagTree("parent");
    TagTree child = new TagTree("child");

    exceptionRule.expect(MpttRepository.NodeNotInTree.class);
    exceptionRule.expectMessage(String.format("Parent node not attached to any tree: %s", parent));
    tagTreeRepo.removeChild(parent, child);
  }

  @Test
  public void givenParentAndChildInDifferentTrees_whenRemoveChild_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree parent1 = new TagTree("parent-1");
    tagTreeRepo.startTree(parent1);
    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(parent1, child1);

    TagTree parent2 = new TagTree("parent-2");
    tagTreeRepo.startTree(parent2);
    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(parent2, child2);

    exceptionRule.expect(MpttRepository.NodeNotInTree.class);
    exceptionRule
        .expectMessage(String.format("Nodes not in same tree - parent: %s; child %s", parent1, child2));
    tagTreeRepo.removeChild(parent1, child2);
  }

  @Test
  public void givenParentAndChild_whenRemoveChildReverseParentAndChild_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree parent = new TagTree("parent");
    tagTreeRepo.startTree(parent);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(parent, child);

    exceptionRule.expect(MpttRepository.NodeNotChildOfParent.class);
    tagTreeRepo.removeChild(child, parent);
  }

  @Test
  public void givenParentAndChild_whenRemoveChild_thenOK()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree parent = new TagTree("parent");
    tagTreeRepo.startTree(parent);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(parent, child);

    LOG.debug(String.format("before:\n%s", tagTreeRepo.printTree(parent)));
    List<TagTree> removed = tagTreeRepo.removeChild(parent, child);
    LOG.debug(String.format("after\n%s", tagTreeRepo.printTree(parent)));

    TagTree actual = tagTreeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(2L));

    assertThat(tagTreeRepo.findChildren(actual), is(emptyIterable()));

    assertThat(tagTreeRepo.count(), is(1L));

    assertThat(removed.size(), is(1));
    assertThat(removed, contains(child));
  }

  @Test
  public void givenParentChildAndSubChild_whenRemoveChild_thenOK()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree parent = new TagTree("parent");
    tagTreeRepo.startTree(parent);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(parent, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    LOG.debug(String.format("before:\n%s", tagTreeRepo.printTree(parent)));
    List<TagTree> removed = tagTreeRepo.removeChild(parent, child);
    LOG.debug(String.format("after:\n%s", tagTreeRepo.printTree(parent)));

    TagTree actual = tagTreeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(2L));

    assertThat(tagTreeRepo.findChildren(actual), is(emptyIterable()));

    assertThat(tagTreeRepo.count(), is(1L));

    assertThat(removed.size(), is(2));
    assertThat(removed, contains(child, subChild));
  }

  @Test
  public void givenParentAndTwoChildren_whenRemoveChild_thenOK()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree parent = new TagTree("parent");
    tagTreeRepo.startTree(parent);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(parent, child1);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(parent, child2);

    LOG.debug(String.format("before:\n%s", tagTreeRepo.printTree(parent)));
    List<TagTree> removed = tagTreeRepo.removeChild(parent, child1);
    LOG.debug(String.format("after:\n%s", tagTreeRepo.printTree(parent)));

    TagTree actual = tagTreeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(4L));
    assertThat(child2.getLft(), is(2L));
    assertThat(child2.getRgt(), is(3L));

    List<TagTree> actualChildren = tagTreeRepo.findChildren(parent);
    assertThat(actualChildren.size(), is(1));
    assertThat(actualChildren, contains(child2));

    assertThat(tagTreeRepo.count(), is(2L));

    assertThat(removed.size(), is(1));
    assertThat(removed, contains(child1));
  }

  @Test
  public void givenParentChildAndSubChild_whenRemoveSubChild_thenOK()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree parent = new TagTree("parent");
    tagTreeRepo.startTree(parent);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(parent, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    LOG.debug(String.format("before:\n%s", tagTreeRepo.printTree(parent)));
    List<TagTree> removed = tagTreeRepo.removeChild(parent, subChild);
    LOG.debug(String.format("after:\n%s", tagTreeRepo.printTree(parent)));

    TagTree actual = tagTreeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(4L));
    assertThat(child.getLft(), is(2L));
    assertThat(child.getRgt(), is(3L));

    List<TagTree> actualChildren = tagTreeRepo.findChildren(parent);
    assertThat(actualChildren.size(), is(1));
    assertThat(actualChildren, contains(child));

    assertThat(tagTreeRepo.count(), is(2L));

    assertThat(removed.size(), is(1));
    assertThat(removed, contains(subChild));

    assertThat(tagTreeRepo.findChildren(child), is(empty()));
  }

  @Test
  public void givenComplexTree3_whenRemoveChild1_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);

    LOG.debug(String.format("before:\n%s", tagTreeRepo.printTree(root)));
    tagTreeRepo.removeChild(root, child1);
    LOG.debug(String.format("after:\n%s", tagTreeRepo.printTree(root)));

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 6]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 2 | rgt: 5]\n" +
        "        └── lastSubChild (id: %d) [treeId: %d | lft: 3 | rgt: 4]",
        root.getId(), root.getTreeId(),
        child2.getId(), child2.getTreeId(),
        lastSubChild.getId(), lastSubChild.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree3_whenRemoveChild2_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree,
      MpttRepository.NodeNotChildOfParent {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);

    LOG.debug(String.format("before:\n%s", tagTreeRepo.printTree(root)));
    tagTreeRepo.removeChild(root, child2);
    LOG.debug(String.format("after:\n%s", tagTreeRepo.printTree(root)));

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 10]\n" +
        "    └── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 9]\n" +
        "        ├── subChild-1 (id: %d) [treeId: %d | lft: 3 | rgt: 6]\n" +
        "        │   └── subSubChild (id: %d) [treeId: %d | lft: 4 | rgt: 5]\n" +
        "        └── subChild-2 (id: %d) [treeId: %d | lft: 7 | rgt: 8]",
        root.getId(), root.getTreeId(),
        child1.getId(), child1.getTreeId(),
        subChild1.getId(), subChild1.getTreeId(),
        subSubChild.getId(), subSubChild.getTreeId(),
        subChild2.getId(), subChild2.getTreeId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree3_whenFindRoot_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);

    LOG.debug(String.format("tree to search for root:\n%s", tagTreeRepo.printTree(root)));

    var actual = tagTreeRepo.findTreeRoot(root.getTreeId());
    assertThat(actual, is(root));
  }

  @Test
  public void givenRoot_whenFindAncestorsOfRoot_thenEmptyList()
      throws MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    List<TagTree> actual = tagTreeRepo.findAncestors(root);
    assertThat(actual, is(empty()));
  }

  @Test
  public void givenRootAndChild_whenFindAncestorsOfChild_thenListOfRoot()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    List<TagTree> actual = tagTreeRepo.findAncestors(child);
    assertThat(actual.size(), is(1));
    assertThat(actual, contains(root));
  }

  @Test
  public void givenRootChildAndSubChild_whenFindAncestors_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    List<TagTree> ancestorsOfRoot = tagTreeRepo.findAncestors(root);
    assertThat(ancestorsOfRoot, is(empty()));

    List<TagTree> ancestorsOfChild = tagTreeRepo.findAncestors(child);
    assertThat(ancestorsOfChild.size(), is(1));
    assertThat(ancestorsOfChild, contains(root));

    List<TagTree> ancestorsOfSubChild = tagTreeRepo.findAncestors(subChild);
    assertThat(ancestorsOfSubChild.size(), is(2));
    assertThat(ancestorsOfSubChild, containsInRelativeOrder(root, child));
  }

  @Test
  public void givenComplexTree3_whenFindAncestors_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);
    /*
    .
    └── root (id: %d) [treeId: 100 | lft: 1 | rgt: 14]
        ├── child-1 (id: %d) [treeId: 100 | lft: 2 | rgt: 9]
        │   ├── subChild-1 (id: %d) [treeId: 100 | lft: 3 | rgt: 6]
        │   │   └── subSubChild (id: %d) [treeId: 100 | lft: 4 | rgt: 5]
        │   └── subChild-2 (id: %d) [treeId: 100 | lft: 7 | rgt: 8]
        └── child-2 (id: %d) [treeId: 100 | lft: 10 | rgt: 13]
            └── lastSubChild (id: %d) [treeId: 100 | lft: 11 | rgt: 12]
    */
    assertThat(tagTreeRepo.findAncestors(subChild1), containsInRelativeOrder(root, child1));
    assertThat(tagTreeRepo.findAncestors(subChild2), containsInRelativeOrder(root, child1));
    assertThat(tagTreeRepo.findAncestors(subSubChild), containsInRelativeOrder(root, child1, subChild1));
  }

  @Test
  public void givenRoot_whenFindParentOfRoot_thenNull() throws MpttRepository.NodeAlreadyAttachedToTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    assertThat(tagTreeRepo.findParent(root), is(nullValue()));
  }

  @Test
  public void givenRootAndChild_whenFindParentOfChild_thenRoot()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    assertThat(tagTreeRepo.findParent(root), is(nullValue()));
    assertThat(tagTreeRepo.findParent(child), is(root));
  }

  @Test
  public void givenRootChildAndSubChild_whenFindParent_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    assertThat(tagTreeRepo.findParent(root), is(nullValue()));
    assertThat(tagTreeRepo.findParent(child), is(root));
    assertThat(tagTreeRepo.findParent(subChild), is(child));
  }

  @Test
  public void givenRootAndTwoChildren_whenFindParent_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    assertThat(tagTreeRepo.findParent(root), is(nullValue()));
    assertThat(tagTreeRepo.findParent(child1), is(root));
    assertThat(tagTreeRepo.findParent(child2), is(root));
  }

  @Test
  public void givenComplexTree3_whenFindParent_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild1 = new TagTree("subChild-1");
    tagTreeRepo.addChild(child1, subChild1);

    TagTree subSubChild = new TagTree("subSubChild");
    tagTreeRepo.addChild(subChild1, subSubChild);

    TagTree subChild2 = new TagTree("subChild-2");
    tagTreeRepo.addChild(child1, subChild2);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    TagTree lastSubChild = new TagTree("lastSubChild");
    tagTreeRepo.addChild(child2, lastSubChild);
    /*
    .
    └── root (id: %d) [treeId: 100 | lft: 1 | rgt: 14]
        ├── child-1 (id: %d) [treeId: 100 | lft: 2 | rgt: 9]
        │   ├── subChild-1 (id: %d) [treeId: 100 | lft: 3 | rgt: 6]
        │   │   └── subSubChild (id: %d) [treeId: 100 | lft: 4 | rgt: 5]
        │   └── subChild-2 (id: %d) [treeId: 100 | lft: 7 | rgt: 8]
        └── child-2 (id: %d) [treeId: 100 | lft: 10 | rgt: 13]
            └── lastSubChild (id: %d) [treeId: 100 | lft: 11 | rgt: 12]
    */
    assertThat(tagTreeRepo.findParent(root), is(nullValue()));
    assertThat(tagTreeRepo.findParent(child1), is(root));
    assertThat(tagTreeRepo.findParent(child2), is(root));
    assertThat(tagTreeRepo.findParent(subChild1), is(child1));
    assertThat(tagTreeRepo.findParent(subChild2), is(child1));
    assertThat(tagTreeRepo.findParent(subSubChild), is(subChild1));
    assertThat(tagTreeRepo.findParent(lastSubChild), is(child2));
  }
}
