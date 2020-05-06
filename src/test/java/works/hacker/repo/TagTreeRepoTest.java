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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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
  public void whenSaved_thenOK() {
    assertThat(tagTreeRepo.count(), is(0L));

    TagTree expected = new TagTree("test-01");
    tagTreeRepo.save(expected);
    assertThat(tagTreeRepo.count(), is(1L));

    TagTree actual = tagTreeRepo.findByName(expected.getName()).get(0);
    assertThat(actual.getId(), is(notNullValue()));
    assertThat(actual.getName(), is(expected.getName()));
  }

  @Test
  public void whenConstructed_thenHasNoTreeId() {
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
    Long treeId2 = 200L;
    TagTree rootNode2 = tagTreeRepo.findByName(rootNode1.getName()).get(0);
    tagTreeRepo.startTree(rootNode2, treeId2);
  }
}
