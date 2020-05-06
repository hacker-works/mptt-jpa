package works.hacker.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import works.hacker.model.TagTree;

public interface TagTreeRepo extends JpaRepository<TagTree, Long> {
}
