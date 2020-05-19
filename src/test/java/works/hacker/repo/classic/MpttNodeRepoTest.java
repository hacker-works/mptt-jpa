package works.hacker.repo.classic;

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
import works.hacker.config.TreesJpaConfig;
import works.hacker.model.classic.MpttNode;
import works.hacker.mptt.TreeEntity;
import works.hacker.mptt.TreeRepository;
import works.hacker.mptt.TreeUtils;
import works.hacker.mptt.classic.MpttEntity;
import works.hacker.mptt.classic.MpttRepository;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TreesJpaConfig.class}, loader = AnnotationConfigContextLoader.class)
@Transactional
@DirtiesContext
public class MpttNodeRepoTest {
  private final Logger LOG = LoggerFactory.getLogger(MpttNodeRepoTest.class);

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Resource
  MpttNodeRepository treeRepo;

  protected TreeUtils<MpttNode> utils;

  @Before
  public void init() {
    treeRepo.setEntityClass(MpttNode.class);
    utils = new TreeUtils<>(treeRepo);
  }

  @Test
  public void giveSaved_whenFindByName_thenOK() {
    assertThat(treeRepo.count(), is(0L));

    var expected = new MpttNode("test-01");
    treeRepo.save(expected);
    assertThat(treeRepo.count(), is(1L));

    var actual = treeRepo.findByName(expected.getName());
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getName(), is(expected.getName()));
  }

  @Test
  public void givenNoTree_whenConstructed_thenHasNoTreeId() {
    var actual = new MpttNode("test");
    assertThat(actual.hasTreeId(), is(false));
  }

  @Test
  public void givenNoTree_whenStartTree_thenOK() throws TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    var treeId = treeRepo.startTree(root);

    assertThat(treeRepo.count(), is(1L));

    var actual = treeRepo.findByName(root.getName());
    assertThat(actual.getTreeId(), not(TreeEntity.NO_TREE_ID));
    assertThat(actual.getTreeId(), is(treeId));

    assertThat(actual.getLft(), is(MpttEntity.DEFAULT_LFT));
    assertThat(actual.getRgt(), is(MpttEntity.DEFAULT_RGT));
  }

  @Test
  public void givenTree_whenStartTreeWithUsedRootNode_thenError()
      throws TreeRepository.NodeAlreadyAttachedToTree {
    var treeId = treeRepo.startTree(new MpttNode("root"));

    exceptionRule.expect(MpttRepository.NodeAlreadyAttachedToTree.class);
    exceptionRule.expectMessage(String.format("Node already has treeId set to %d", treeId));
    var root = treeRepo.findByName("root");
    treeRepo.startTree(root);
  }

  @Test
  public void givenTree_whenFindTreeRoot_thenOK() throws TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    var treeId = treeRepo.startTree(root);

    var actual = treeRepo.findTreeRoot(treeId);
    assertThat(actual, is(root));
  }

  @Test
  public void givenParentNodeNotAttachedToTree_whenAddChild_thenError()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree {
    var parent = new MpttNode("parent");
    var child = new MpttNode("child");

    exceptionRule.expect(TreeRepository.NodeNotInTree.class);
    exceptionRule.expectMessage(String.format("Parent node not attached to any tree: %s", parent));
    treeRepo.addChild(parent, child);
  }

  @Test
  public void givenChildIsTreeRoot_whenAddChild_thenError()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var parent = new MpttNode("parent");
    var child = new MpttNode("child");

    treeRepo.startTree(parent);
    var treeId = treeRepo.startTree(child);

    exceptionRule.expect(MpttRepository.NodeAlreadyAttachedToTree.class);
    exceptionRule.expectMessage(String.format("Node already has treeId set to %d", treeId));
    treeRepo.addChild(parent, child);
  }

  @Test
  public void givenEmptyTree_whenFindRightMostChild_thenNull()
      throws TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var actual = treeRepo.findRightMostChild(root);
    assertThat(actual, is(nullValue()));
  }

  @Test
  public void givenEmptyTree_whenAddChild_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    assertThat(treeRepo.count(), is(2L));

    var actualRoot = treeRepo.findByName("root");
    var actualChild = treeRepo.findByName("child");

    assertThat(actualRoot.getLft(), is(1L));
    assertThat(actualRoot.getRgt(), is(4L));
    assertThat(actualChild.getTreeId(), is(root.getTreeId()));
    assertThat(actualChild.getLft(), is(root.getLft() + 1));
    assertThat(actualChild.getRgt(), is(root.getRgt() - 1));
  }

  @Test
  public void givenTreeRoot_whenPrintTree_thenOK() throws TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    // @formatter:off
    var expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 2]",
        root.getId(), root.getTreeId());
    // @formatter:on
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithOneChild_whenFindChildren_thenContainsOneChild()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    var actual = treeRepo.findChildren(root);
    assertThat(actual, containsInRelativeOrder(child));
  }

  @Test
  public void givenTreeWithChild_whenPrintTree_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    // @formatter:off
    var expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 4]\n"+
        "    └── child (id: %d) [treeId: %d | lft: 2 | rgt: 3]",
        root.getId(), root.getTreeId(),
        child.getId(), child.getTreeId());
    // @formatter:on
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithTwoChildren_whenFindChildren_thenContainsTwoChildren()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var actual = treeRepo.findChildren(root);
    assertThat(actual, containsInRelativeOrder(child1, child2));
  }

  @Test
  public void givenTreeWithTwoChildren_whenPrintTree_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    // @formatter:off
    var expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 6]\n" +
        "    ├── child-1 (id: %d) [treeId: %d | lft: 2 | rgt: 3]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 4 | rgt: 5]",
        root.getId(), root.getTreeId(),
        child1.getId(), child1.getTreeId(),
        child2.getId(), child2.getTreeId());
    // @formatter:on
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithChildAndSubChild_whenFindChildren_thenContainsOneChild()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child, subChild);

    var actual = treeRepo.findChildren(root);
    assertThat(actual.size(), is(1));
    assertThat(actual, containsInRelativeOrder(child));
  }

  @Test
  public void givenTreeWithChildAndSubChild_whenPrintTree_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child, subChild);

    // @formatter:off
    var expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 6]\n" +
        "    └── child (id: %d) [treeId: %d | lft: 2 | rgt: 5]\n" +
        "        └── subChild (id: %d) [treeId: %d | lft: 3 | rgt: 4]",
        root.getId(), root.getTreeId(),
        child.getId(), child.getTreeId(),
        subChild.getId(), subChild.getTreeId());
    // @formatter:on
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree1_whenPrintTree_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child1, subChild);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    // @formatter:off
    var expected = String.format(
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
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree1_whenFindChildren_thenContainsTwoChildren()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child1, subChild);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var actual = treeRepo.findChildren(root);
    assertThat(actual.size(), is(2));
    assertThat(actual, containsInRelativeOrder(child1, child2));
  }

  @Test
  public void givenComplexTree2_whenPrintTree_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child1, subChild);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild, subSubChild);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    // @formatter:off
    var expected = String.format(
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
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree2_whenFindChildren_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child1, subChild);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild, subSubChild);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var actual1 = treeRepo.findChildren(root);
    assertThat(actual1.size(), is(2));
    assertThat(actual1, containsInRelativeOrder(child1, child2));

    var actual2 = treeRepo.findChildren(child1);
    assertThat(actual2.size(), is(1));
    assertThat(actual2, contains(subChild));

    var actual3 = treeRepo.findChildren(subChild);
    assertThat(actual3.size(), is(1));
    assertThat(actual3, contains(subSubChild));
  }

  @Test
  public void givenComplexTree3_whenPrintTree_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);

    // @formatter:off
    var expected = String.format(
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
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));

    // @formatter:off
    var expectedPartial = String.format(
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
    var actualPartial = utils.printTree(child1);
    assertThat(actualPartial, is(expectedPartial));
  }

  @Test
  public void givenComplexTree3_whenFindChildren_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);

    var actual1 = treeRepo.findChildren(root);
    assertThat(actual1.size(), is(2));
    assertThat(actual1, containsInRelativeOrder(child1, child2));

    var actual2 = treeRepo.findChildren(child1);
    assertThat(actual2.size(), is(2));
    assertThat(actual2, containsInRelativeOrder(subChild1, subChild2));
  }

  @Test
  public void givenParentNotAttachedToTree_whenRemoveChild_thenError()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeNotChildOfParent {
    var parent = new MpttNode("parent");
    var child = new MpttNode("child");

    exceptionRule.expect(MpttRepository.NodeNotInTree.class);
    exceptionRule.expectMessage(String.format("Parent node not attached to any tree: %s", parent));
    treeRepo.removeChild(parent, child);
  }

  @Test
  public void givenParentAndChildInDifferentTrees_whenRemoveChild_thenError()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree,
      TreeRepository.NodeNotChildOfParent {
    var parent1 = new MpttNode("parent-1");
    treeRepo.startTree(parent1);
    var child1 = new MpttNode("child-1");
    treeRepo.addChild(parent1, child1);

    var parent2 = new MpttNode("parent-2");
    treeRepo.startTree(parent2);
    var child2 = new MpttNode("child-2");
    treeRepo.addChild(parent2, child2);

    exceptionRule.expect(MpttRepository.NodeNotInTree.class);
    exceptionRule
        .expectMessage(String.format("Nodes not in same tree - parent: %s; child %s", parent1, child2));
    treeRepo.removeChild(parent1, child2);
  }

  @Test
  public void givenParentAndChild_whenRemoveChildReverseParentAndChild_thenError()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree,
      TreeRepository.NodeNotChildOfParent {
    var parent = new MpttNode("parent");
    treeRepo.startTree(parent);

    var child = new MpttNode("child");
    treeRepo.addChild(parent, child);

    exceptionRule.expect(MpttRepository.NodeNotChildOfParent.class);
    treeRepo.removeChild(child, parent);
  }

  @Test
  public void givenParentAndChild_whenRemoveChild_thenOK()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree,
      TreeRepository.NodeNotChildOfParent {
    var parent = new MpttNode("parent");
    treeRepo.startTree(parent);

    var child = new MpttNode("child");
    treeRepo.addChild(parent, child);

    LOG.debug(String.format("before:\n%s", utils.printTree(parent)));
    var removed = treeRepo.removeChild(parent, child);
    LOG.debug(String.format("after\n%s", utils.printTree(parent)));

    var actual = treeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(2L));

    assertThat(treeRepo.findChildren(actual), is(emptyIterable()));

    assertThat(treeRepo.count(), is(1L));

    assertThat(removed.size(), is(1));
    assertThat(removed, contains(child));
  }

  @Test
  public void givenParentChildAndSubChild_whenRemoveChild_thenOK()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree,
      TreeRepository.NodeNotChildOfParent {
    var parent = new MpttNode("parent");
    treeRepo.startTree(parent);

    var child = new MpttNode("child");
    treeRepo.addChild(parent, child);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child, subChild);

    LOG.debug(String.format("before:\n%s", utils.printTree(parent)));
    var removed = treeRepo.removeChild(parent, child);
    LOG.debug(String.format("after:\n%s", utils.printTree(parent)));

    var actual = treeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(2L));

    assertThat(treeRepo.findChildren(actual), is(emptyIterable()));

    assertThat(treeRepo.count(), is(1L));

    assertThat(removed.size(), is(2));
    assertThat(removed, contains(child, subChild));
  }

  @Test
  public void givenParentAndTwoChildren_whenRemoveChild_thenOK()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree,
      TreeRepository.NodeNotChildOfParent {
    var parent = new MpttNode("parent");
    treeRepo.startTree(parent);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(parent, child1);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(parent, child2);

    LOG.debug(String.format("before:\n%s", utils.printTree(parent)));
    var removed = treeRepo.removeChild(parent, child1);
    LOG.debug(String.format("after:\n%s", utils.printTree(parent)));

    var actual = treeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(4L));
    assertThat(child2.getLft(), is(2L));
    assertThat(child2.getRgt(), is(3L));

    var actualChildren = treeRepo.findChildren(parent);
    assertThat(actualChildren.size(), is(1));
    assertThat(actualChildren, contains(child2));

    assertThat(treeRepo.count(), is(2L));

    assertThat(removed.size(), is(1));
    assertThat(removed, contains(child1));
  }

  @Test
  public void givenParentChildAndSubChild_whenRemoveSubChild_thenOK()
      throws TreeRepository.NodeNotInTree, TreeRepository.NodeAlreadyAttachedToTree,
      TreeRepository.NodeNotChildOfParent {
    var parent = new MpttNode("parent");
    treeRepo.startTree(parent);

    var child = new MpttNode("child");
    treeRepo.addChild(parent, child);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child, subChild);

    LOG.debug(String.format("before:\n%s", utils.printTree(parent)));
    var removed = treeRepo.removeChild(parent, subChild);
    LOG.debug(String.format("after:\n%s", utils.printTree(parent)));

    var actual = treeRepo.findByName("parent");
    assertThat(actual.getLft(), is(1L));
    assertThat(actual.getRgt(), is(4L));
    assertThat(child.getLft(), is(2L));
    assertThat(child.getRgt(), is(3L));

    var actualChildren = treeRepo.findChildren(parent);
    assertThat(actualChildren.size(), is(1));
    assertThat(actualChildren, contains(child));

    assertThat(treeRepo.count(), is(2L));

    assertThat(removed.size(), is(1));
    assertThat(removed, contains(subChild));

    assertThat(treeRepo.findChildren(child), is(empty()));
  }

  @Test
  public void givenComplexTree3_whenRemoveChild1_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree,
      TreeRepository.NodeNotChildOfParent {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);

    LOG.debug(String.format("before:\n%s", utils.printTree(root)));
    treeRepo.removeChild(root, child1);
    LOG.debug(String.format("after:\n%s", utils.printTree(root)));

    // @formatter:off
    var expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: %d | lft: 1 | rgt: 6]\n" +
        "    └── child-2 (id: %d) [treeId: %d | lft: 2 | rgt: 5]\n" +
        "        └── lastSubChild (id: %d) [treeId: %d | lft: 3 | rgt: 4]",
        root.getId(), root.getTreeId(),
        child2.getId(), child2.getTreeId(),
        lastSubChild.getId(), lastSubChild.getTreeId());
    // @formatter:on
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree3_whenRemoveChild2_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree,
      TreeRepository.NodeNotChildOfParent {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);

    LOG.debug(String.format("before:\n%s", utils.printTree(root)));
    treeRepo.removeChild(root, child2);
    LOG.debug(String.format("after:\n%s", utils.printTree(root)));

    // @formatter:off
    var expected = String.format(
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
    var actual = utils.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree3_whenFindTreeRoot_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);

    LOG.debug(String.format("tree to search for root:\n%s", utils.printTree(root)));

    var actual = treeRepo.findTreeRoot(root.getTreeId());
    assertThat(actual, is(root));
  }

  @Test
  public void givenRoot_whenFindAncestorsOfRoot_thenEmptyList()
      throws TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var actual = treeRepo.findAncestors(root);
    assertThat(actual, is(empty()));
  }

  @Test
  public void givenRootAndChild_whenFindAncestorsOfChild_thenListOfRoot()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    var actual = treeRepo.findAncestors(child);
    assertThat(actual.size(), is(1));
    assertThat(actual, contains(root));
  }

  @Test
  public void givenRootChildAndSubChild_whenFindAncestors_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child, subChild);

    var ancestorsOfRoot = treeRepo.findAncestors(root);
    assertThat(ancestorsOfRoot, is(empty()));

    var ancestorsOfChild = treeRepo.findAncestors(child);
    assertThat(ancestorsOfChild.size(), is(1));
    assertThat(ancestorsOfChild, contains(root));

    var ancestorsOfSubChild = treeRepo.findAncestors(subChild);
    assertThat(ancestorsOfSubChild.size(), is(2));
    assertThat(ancestorsOfSubChild, containsInRelativeOrder(root, child));
  }

  @Test
  public void givenComplexTree3_whenFindAncestors_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);
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
    assertThat(treeRepo.findAncestors(subChild1), containsInRelativeOrder(root, child1));
    assertThat(treeRepo.findAncestors(subChild2), containsInRelativeOrder(root, child1));
    assertThat(treeRepo.findAncestors(subSubChild), containsInRelativeOrder(root, child1, subChild1));
  }

  @Test
  public void givenRoot_whenFindParentOfRoot_thenNull() throws TreeRepository.NodeAlreadyAttachedToTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    assertThat(treeRepo.findParent(root), is(Optional.empty()));
  }

  @Test
  public void givenRootAndChild_whenFindParentOfChild_thenRoot()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    assertThat(treeRepo.findParent(root), is(Optional.empty()));
    assertThat(treeRepo.findParent(child).get(), is(root));
  }

  @Test
  public void givenRootChildAndSubChild_whenFindParent_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child = new MpttNode("child");
    treeRepo.addChild(root, child);

    var subChild = new MpttNode("subChild");
    treeRepo.addChild(child, subChild);

    assertThat(treeRepo.findParent(root), is(Optional.empty()));
    assertThat(treeRepo.findParent(child).get(), is(root));
    assertThat(treeRepo.findParent(subChild).get(), is(child));
  }

  @Test
  public void givenRootAndTwoChildren_whenFindParent_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    assertThat(treeRepo.findParent(root), is(Optional.empty()));
    assertThat(treeRepo.findParent(child1).get(), is(root));
    assertThat(treeRepo.findParent(child2).get(), is(root));
  }

  @Test
  public void givenComplexTree3_whenFindParent_thenOK()
      throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree {
    var root = new MpttNode("root");
    treeRepo.startTree(root);

    var child1 = new MpttNode("child-1");
    treeRepo.addChild(root, child1);

    var subChild1 = new MpttNode("subChild-1");
    treeRepo.addChild(child1, subChild1);

    var subSubChild = new MpttNode("subSubChild");
    treeRepo.addChild(subChild1, subSubChild);

    var subChild2 = new MpttNode("subChild-2");
    treeRepo.addChild(child1, subChild2);

    var child2 = new MpttNode("child-2");
    treeRepo.addChild(root, child2);

    var lastSubChild = new MpttNode("lastSubChild");
    treeRepo.addChild(child2, lastSubChild);
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
    assertThat(treeRepo.findParent(root), is(Optional.empty()));
    assertThat(treeRepo.findParent(child1).get(), is(root));
    assertThat(treeRepo.findParent(child2).get(), is(root));
    assertThat(treeRepo.findParent(subChild1).get(), is(child1));
    assertThat(treeRepo.findParent(subChild2).get(), is(child1));
    assertThat(treeRepo.findParent(subSubChild).get(), is(subChild1));
    assertThat(treeRepo.findParent(lastSubChild).get(), is(child2));
  }
}
