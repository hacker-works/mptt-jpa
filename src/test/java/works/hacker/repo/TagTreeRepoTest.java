package works.hacker.repo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import works.hacker.config.TagTreeJpaConfig;
import works.hacker.model.TagTree;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
// import static org.hamcrest.Matchers.not;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TagTreeJpaConfig.class}, loader = AnnotationConfigContextLoader.class)
@Transactional
@DirtiesContext
public class TagTreeRepoTest {

  @Resource
  TagTreeRepo tagTreeRepo;

  @Test
  public void whenSaved_thenOK() {
    TagTree tagTree = new TagTree("test-01");
    tagTreeRepo.save(tagTree);

    assertThat(tagTreeRepo.count(), is(1L));
  }
}
