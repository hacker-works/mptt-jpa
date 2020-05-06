package works.hacker.repo;

import org.springframework.stereotype.Repository;
import works.hacker.model.TagTree;
import works.hacker.mptt.MpttRepositoryImpl;

@Repository
public class TagTreeRepositoryImpl extends MpttRepositoryImpl<TagTree, Long> implements
    TagTreeRepositoryCustom {
}
