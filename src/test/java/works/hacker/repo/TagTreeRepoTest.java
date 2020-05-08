package works.hacker.repo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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

    TagTree actual = tagTreeRepo.findByName(expected.getName()).get(0);
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getName(), is(expected.getName()));
  }

  @Test
  public void givenNoTree_whenConstructed_thenHasNoTreeId() {
    TagTree actual = new TagTree("test");
    assertThat(actual.hasTreeId(), is(false));
  }

  @Test
  public void givenNoTree_whenStartTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed {
    Long treeId = 100L;
    TagTree rootNode = new TagTree("root");

    tagTreeRepo.startTree(rootNode, treeId);

    assertThat(tagTreeRepo.count(), is(1L));

    TagTree actual = tagTreeRepo.findByName(rootNode.getName()).get(0);
    assertThat(actual.getTreeId(), is(100L));
    assertThat(actual.getLft(), is(MpttEntity.DEFAULT_LFT));
    assertThat(actual.getRgt(), is(MpttEntity.DEFAULT_RGT));
  }

  @Test
  public void givenTree_whenStartTreeWithUsedRootNode_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed {
    Long treeId1 = 100L;
    TagTree rootNode1 = new TagTree("root");
    tagTreeRepo.startTree(rootNode1, treeId1);

    exceptionRule.expect(MpttRepository.NodeAlreadyAttachedToTree.class);
    exceptionRule.expectMessage(String.format("node already has treeId set to %d", treeId1));
    Long treeId2 = 200L;
    TagTree rootNode2 = tagTreeRepo.findByName(rootNode1.getName()).get(0);
    tagTreeRepo.startTree(rootNode2, treeId2);
  }

  @Test
  public void givenTree_whenStartTreeWithUsedTreeId_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed {
    Long treeId = 100L;
    TagTree rootNode1 = new TagTree("root-1");
    tagTreeRepo.startTree(rootNode1, treeId);

    exceptionRule.expect(MpttRepository.TreeIdAlreadyUsed.class);
    exceptionRule.expectMessage(String.format("%d already used in another tree", treeId));
    TagTree rootNode2 = new TagTree("root-2");
    tagTreeRepo.startTree(rootNode2, treeId);
  }

  @Test
  public void givenParentNodeNotAttachedToTree_whenAddChild_thenError()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree {
    TagTree parent = new TagTree("parent");
    TagTree child = new TagTree("child");

    exceptionRule.expect(MpttRepository.NodeNotInTree.class);
    exceptionRule.expectMessage("Parent node not attached to any tree");
    tagTreeRepo.addChild(parent, child);
  }

  @Test
  public void givenChildIsTreeRoot_whenAddChild_thenError()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree parent = new TagTree("parent");
    TagTree child = new TagTree("child");

    tagTreeRepo.startTree(parent, 100L);
    tagTreeRepo.startTree(child, 200L);

    exceptionRule.expect(MpttRepository.NodeAlreadyAttachedToTree.class);
    exceptionRule.expectMessage(String.format("node already has treeId set to %d", 200L));
    tagTreeRepo.addChild(parent, child);
  }

  @Test
  public void givenEmptyTree_whenFindRightMostChild_thenNull()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree actual = tagTreeRepo.findRightMostChild(root);
    assertThat(actual, is(nullValue()));
  }

  @Test
  public void givenEmptyTree_whenAddChild_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    assertThat(tagTreeRepo.count(), is(2L));

    TagTree actualRoot = tagTreeRepo.findByName("root").get(0);
    TagTree actualChild = tagTreeRepo.findByName("child").get(0);

    assertThat(actualRoot.getLft(), is(1L));
    assertThat(actualRoot.getRgt(), is(4L));
    assertThat(actualChild.getTreeId(), is(root.getTreeId()));
    assertThat(actualChild.getLft(), is(root.getLft() + 1));
    assertThat(actualChild.getRgt(), is(actualChild.getLft() + 1));
  }

  @Test
  public void givenTreeRoot_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 2]",
        root.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithChild_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 4]\n"+
        "    └── child (id: %d) [treeId: 100 | lft: 2 | rgt: 3]",
        root.getId(),
        child.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithOneChild_whenFindChildren_thenContainsOneChild()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree,
      MpttRepository.TreeIdAlreadyUsed {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    List<TagTree> actual = tagTreeRepo.findChildren(root);
    assertThat(actual, containsInRelativeOrder(child));
  }

  @Test
  public void givenTreeWithTwoChildren_whenFindChildren_thenContainsTwoChildren()
      throws MpttRepository.NodeNotInTree, MpttRepository.NodeAlreadyAttachedToTree,
      MpttRepository.TreeIdAlreadyUsed {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree child2 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child2);

    List<TagTree> actual = tagTreeRepo.findChildren(root);
    assertThat(actual, containsInRelativeOrder(child1, child2));
  }

  @Test
  public void givenTreeWithTwoChildren_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    // @formatter:off
    String expected = String.format(
        ".\n" +
            "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 6]\n" +
            "    ├── child-1 (id: %d) [treeId: 100 | lft: 2 | rgt: 3]\n" +
            "    └── child-2 (id: %d) [treeId: 100 | lft: 4 | rgt: 5]",
        root.getId(),
        child1.getId(),
        child2.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenTreeWithChildAndSubChild_whenFindChildren_thenContainsOneChild()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

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
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child = new TagTree("child");
    tagTreeRepo.addChild(root, child);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child, subChild);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 6]\n" +
        "    └── child (id: %d) [treeId: 100 | lft: 2 | rgt: 5]\n" +
        "        └── subChild (id: %d) [treeId: 100 | lft: 3 | rgt: 4]",
        root.getId(),
        child.getId(),
        subChild.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree1_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

    TagTree child1 = new TagTree("child-1");
    tagTreeRepo.addChild(root, child1);

    TagTree subChild = new TagTree("subChild");
    tagTreeRepo.addChild(child1, subChild);

    TagTree child2 = new TagTree("child-2");
    tagTreeRepo.addChild(root, child2);

    // @formatter:off
    String expected = String.format(
        ".\n" +
        "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 8]\n" +
        "    ├── child-1 (id: %d) [treeId: 100 | lft: 2 | rgt: 5]\n" +
        "    │   └── subChild (id: %d) [treeId: 100 | lft: 3 | rgt: 4]\n" +
        "    └── child-2 (id: %d) [treeId: 100 | lft: 6 | rgt: 7]",
        root.getId(),
        child1.getId(),
        subChild.getId(),
        child2.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree2_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

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
        "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 10]\n" +
        "    ├── child-1 (id: %d) [treeId: 100 | lft: 2 | rgt: 7]\n" +
        "    │   └── subChild (id: %d) [treeId: 100 | lft: 3 | rgt: 6]\n" +
        "    │       └── subSubChild (id: %d) [treeId: 100 | lft: 4 | rgt: 5]\n" +
        "    └── child-2 (id: %d) [treeId: 100 | lft: 8 | rgt: 9]",
        root.getId(),
        child1.getId(),
        subChild.getId(),
        subSubChild.getId(),
        child2.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }

  @Test
  public void givenComplexTree3_whenPrintTree_thenOK()
      throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
      MpttRepository.NodeNotInTree {
    TagTree root = new TagTree("root");
    tagTreeRepo.startTree(root, 100L);

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
            "└── root (id: %d) [treeId: 100 | lft: 1 | rgt: 14]\n" +
            "    ├── child-1 (id: %d) [treeId: 100 | lft: 2 | rgt: 9]\n" +
            "    │   ├── subChild-1 (id: %d) [treeId: 100 | lft: 3 | rgt: 6]\n" +
            "    │   │   └── subSubChild (id: %d) [treeId: 100 | lft: 4 | rgt: 5]\n" +
            "    │   └── subChild-2 (id: %d) [treeId: 100 | lft: 7 | rgt: 8]\n" +
            "    └── child-2 (id: %d) [treeId: 100 | lft: 10 | rgt: 13]\n" +
            "        └── lastSubChild (id: %d) [treeId: 100 | lft: 11 | rgt: 12]",
        root.getId(),
        child1.getId(),
        subChild1.getId(),
        subSubChild.getId(),
        subChild2.getId(),
        child2.getId(),
        lastSubChild.getId());
    // @formatter:on
    String actual = tagTreeRepo.printTree(root);
    assertThat(actual, is(expected));
  }
}
