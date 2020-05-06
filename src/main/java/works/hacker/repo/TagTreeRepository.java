package works.hacker.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import works.hacker.model.TagTree;

import java.util.List;

public interface TagTreeRepository extends JpaRepository<TagTree, Long>, TagTreeRepositoryCustom {
  List<TagTree> findByName(String name);
}
