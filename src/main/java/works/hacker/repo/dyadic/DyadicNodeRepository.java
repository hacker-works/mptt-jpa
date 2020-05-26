package works.hacker.repo.dyadic;

import org.springframework.data.jpa.repository.JpaRepository;
import works.hacker.model.dyadic.DyadicNode;

public interface DyadicNodeRepository extends JpaRepository<DyadicNode, Long>,
    DyadicNodeRepositoryCustom {
  DyadicNode findByName(String name);
}
