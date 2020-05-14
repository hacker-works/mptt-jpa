package works.hacker.repo;

import org.springframework.stereotype.Repository;
import works.hacker.model.TagTree;
import works.hacker.mptt.classic.MpttRepositoryImpl;

@Repository
public class TagTreeRepositoryImpl extends MpttRepositoryImpl<TagTree> implements
    TagTreeRepositoryCustom {
}
