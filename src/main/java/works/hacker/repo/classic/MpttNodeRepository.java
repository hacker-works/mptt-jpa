package works.hacker.repo.classic;

import org.springframework.data.jpa.repository.JpaRepository;
import works.hacker.model.classic.MpttNode;

public interface MpttNodeRepository extends JpaRepository<MpttNode, Long>, MpttNodeRepositoryCustom {
  MpttNode findByName(String name);
}
