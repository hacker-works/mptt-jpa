package works.hacker.repo.dyadic;

import org.springframework.stereotype.Repository;
import works.hacker.model.dyadic.DyadicNode;
import works.hacker.mptt.dyadic.DyadicRepositoryImpl;

@Repository
public class DyadicNodeRepositoryImpl extends DyadicRepositoryImpl<DyadicNode> implements
    DyadicNodeRepositoryCustom {
}
