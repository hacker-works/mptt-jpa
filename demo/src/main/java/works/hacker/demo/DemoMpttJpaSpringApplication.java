package works.hacker.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import works.hacker.model.TagTree;
import works.hacker.mptt.MpttEntity;
import works.hacker.mptt.MpttRepository;
import works.hacker.repo.TagTreeRepository;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EntityScan(basePackages = "works.hacker.model", basePackageClasses = {MpttEntity.class})
@EnableJpaRepositories(basePackages = "works.hacker.repo")
public class DemoMpttJpaSpringApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoMpttJpaSpringApplication.class, args);
  }

  @Component
  public static class Demo {
    private final Logger LOG = LoggerFactory.getLogger(Demo.class);

    @Autowired
    private TagTreeRepository tagTreeRepo;

    @PostConstruct
    public void run()
        throws MpttRepository.NodeAlreadyAttachedToTree, MpttRepository.TreeIdAlreadyUsed,
        MpttRepository.NodeNotInTree {
      tagTreeRepo.setEntityClass(TagTree.class);

      tagTreeRepo.count();

      TagTree root = new TagTree("root");
      tagTreeRepo.startTree(root, 100L);
      LOG.info("printTree(root)\n" + tagTreeRepo.printTree(root));

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

      LOG.info("printTree(root)\n" + tagTreeRepo.printTree(tagTreeRepo.findByName("root")));
    }
  }

}
