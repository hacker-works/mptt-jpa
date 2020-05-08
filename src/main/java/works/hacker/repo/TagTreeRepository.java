package works.hacker.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import works.hacker.model.TagTree;

public interface TagTreeRepository extends JpaRepository<TagTree, Long>, TagTreeRepositoryCustom {
  TagTree findByName(String name);
}
