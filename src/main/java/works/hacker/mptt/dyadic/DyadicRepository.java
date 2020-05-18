package works.hacker.mptt.dyadic;

import works.hacker.mptt.TreeRepository;

import java.util.Optional;

public interface DyadicRepository<T extends DyadicEntity> extends TreeRepository<T> {
  Optional<T> findYoungestChild(T parent);
}
