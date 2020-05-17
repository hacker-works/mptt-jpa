package works.hacker.repo.classic;

import org.springframework.stereotype.Repository;
import works.hacker.model.classic.MpttNode;
import works.hacker.mptt.classic.MpttRepositoryImpl;

@Repository
public class MpttNodeRepositoryImpl extends MpttRepositoryImpl<MpttNode> implements
    MpttNodeRepositoryCustom {
}
