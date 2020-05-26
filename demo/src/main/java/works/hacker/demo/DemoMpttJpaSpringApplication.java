package works.hacker.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;
import works.hacker.model.classic.MpttNode;
import works.hacker.model.dyadic.DyadicNode;
import works.hacker.mptt.TreeRepository;
import works.hacker.mptt.TreeUtils;
import works.hacker.repo.classic.MpttNodeRepository;
import works.hacker.repo.dyadic.DyadicNodeRepository;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EntityScan(basePackages = {"works.hacker.model.classic", "works.hacker.model.dyadic"})
@EnableJpaRepositories(basePackages = {"works.hacker.repo.classic", "works.hacker.repo.dyadic"})
public class DemoMpttJpaSpringApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoMpttJpaSpringApplication.class, args);
  }

  @Component
  public static class Demo {
    private final Logger LOG = LoggerFactory.getLogger(Demo.class);

    @Autowired
    private MpttNodeRepository classicRepo;

    @Autowired
    private DyadicNodeRepository dyadicRepo;

    @PostConstruct
    public void runClassicMpttDemo()
        throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree,
        TreeRepository.NodeNotChildOfParent {
      classicRepo.setEntityClass(MpttNode.class);
      TreeUtils<MpttNode> utils = new TreeUtils<>(classicRepo);

      classicRepo.count();

      MpttNode root = new MpttNode("root");
      Long treeId = classicRepo.startTree(root);
      LOG.info("printTree(root)\n" + utils.printTree(root));

      MpttNode child1 = new MpttNode("child-1");
      classicRepo.addChild(root, child1);

      MpttNode subChild1 = new MpttNode("subChild-1");
      classicRepo.addChild(child1, subChild1);

      MpttNode subSubChild = new MpttNode("subSubChild");
      classicRepo.addChild(subChild1, subSubChild);

      MpttNode subChild2 = new MpttNode("subChild-2");
      classicRepo.addChild(child1, subChild2);

      MpttNode child2 = new MpttNode("child-2");
      classicRepo.addChild(root, child2);

      MpttNode lastSubChild = new MpttNode("lastSubChild");
      classicRepo.addChild(child2, lastSubChild);

      LOG.info("printTree(root)\n" + utils.printTree(classicRepo.findByName("root")));
      LOG.info("printTree(root)\n" + utils.printTree(classicRepo.findByName("child-1")));

      LOG.info("findChildren(root)\n" + classicRepo.findChildren(classicRepo.findTreeRoot(treeId)));
      LOG.info("findAncestors(subSubChild)\n" +
          classicRepo.findAncestors(classicRepo.findByName("subSubChild")));
      LOG.info("findParent(subSubChild)\n" +
          classicRepo.findParent(classicRepo.findByName("subSubChild")));
      LOG.info("findSubTree(child1)\n" + classicRepo.findSubTree(classicRepo.findByName("child-1")));

      classicRepo.removeChild(classicRepo.findByName("root"), classicRepo.findByName("child-1"));

      LOG.info("printTree(root) after removeChild(parent, child1)\n" +
          utils.printTree(classicRepo.findByName("root")));
    }

    @PostConstruct
    public void runDyadicMpttDemo()
        throws TreeRepository.NodeAlreadyAttachedToTree, TreeRepository.NodeNotInTree,
        TreeRepository.NodeNotChildOfParent {
      dyadicRepo.setEntityClass(DyadicNode.class);
      TreeUtils<DyadicNode> utils = new TreeUtils<>(dyadicRepo);

      dyadicRepo.count();

      DyadicNode root = new DyadicNode("root");
      Long treeId = dyadicRepo.startTree(root);
      LOG.info("printTree(root)\n" + utils.printTree(root));

      DyadicNode child1 = new DyadicNode("child-1");
      dyadicRepo.addChild(root, child1);

      DyadicNode subChild1 = new DyadicNode("subChild-1");
      dyadicRepo.addChild(child1, subChild1);

      DyadicNode subSubChild = new DyadicNode("subSubChild");
      dyadicRepo.addChild(subChild1, subSubChild);

      DyadicNode subChild2 = new DyadicNode("subChild-2");
      dyadicRepo.addChild(child1, subChild2);

      DyadicNode child2 = new DyadicNode("child-2");
      dyadicRepo.addChild(root, child2);

      DyadicNode lastSubChild = new DyadicNode("lastSubChild");
      dyadicRepo.addChild(child2, lastSubChild);

      LOG.info("printTree(root)\n" + utils.printTree(dyadicRepo.findByName("root")));
      LOG.info("printTree(root)\n" + utils.printTree(dyadicRepo.findByName("child-1")));

      LOG.info("findChildren(root)\n" + dyadicRepo.findChildren(dyadicRepo.findTreeRoot(treeId)));
      LOG.info("findAncestors(subSubChild)\n" +
          dyadicRepo.findAncestors(dyadicRepo.findByName("subSubChild")));
      LOG.info("findParent(subSubChild)\n" +
          dyadicRepo.findParent(dyadicRepo.findByName("subSubChild")));
      LOG.info("findSubTree(child1)\n" + dyadicRepo.findSubTree(dyadicRepo.findByName("child-1")));

      dyadicRepo.removeChild(dyadicRepo.findByName("root"), dyadicRepo.findByName("child-1"));

      LOG.info("printTree(root) after removeChild(parent, child1)\n" +
          utils.printTree(dyadicRepo.findByName("root")));
    }
  }

}
