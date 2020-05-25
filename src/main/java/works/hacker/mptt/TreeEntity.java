package works.hacker.mptt;

import works.hacker.model.classic.MpttNode;
import works.hacker.mptt.classic.MpttEntity;
import works.hacker.mptt.classic.MpttRepository;
import works.hacker.mptt.classic.MpttRepositoryImpl;

import javax.persistence.*;
import java.util.Objects;

/**
 * Provides the properties needed to maintain the MPTT (Modified Preorder Tree Traversal) data structure.
 * <p>
 * In essence MPTT allows fast read operations on hierarchical tree-like data structures stored in an SQL
 * database.
 * <p>
 * Modelling such data structure of nested sets in SQL is done by using the following properties:
 * <ul>
 * <li><b>id</b> generated numeric id of the entity/li>
 * <li><b>lft</b> and <b>rgt</b> to represent the nesting of the nodes</li>
 * <li><b>depth</b></li> to indicate the generation of the children
 * <li><b>treeId</b> to allow growing multiple trees / hierarchies and discriminate between trees</li>
 * </ul>
 * <p>
 * <b>NOTE:</b> left and right are reserved words in MySQL, thus the use of <b>lft</b> and <b>rgt</b>.
 * <p>
 * These properties are managed by the specific implementations of the {@link TreeRepository}.
 *
 * @see MpttEntity
 * @see MpttNode
 * @see MpttRepository
 * @see MpttRepositoryImpl
 * @see <a href="https://github.com/hacker-works/mptt-jpa">README</a>
 */
@MappedSuperclass
public abstract class TreeEntity<T extends Number> {
  private static final String NO_NAME = "NO_NAME";
  public static final long NO_TREE_ID = -1L;
  public static final long START = 0L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  protected long treeId;

  @Column(nullable = false)
  private long depth;

  @Column(nullable = false)
  protected T lft;

  @Column(nullable = false)
  protected T rgt;

  public TreeEntity() {
    this.name = NO_NAME;
    setDefaults();
  }

  public TreeEntity(String name) {
    this.name = name;
    setDefaults();
  }

  public void setDefaults() {
    this.treeId = NO_TREE_ID;
    this.depth = START;
    this.lft = getStartLft();
    this.rgt = getStartRgt();
  }

  public abstract T getStartLft();

  public abstract T getStartRgt();

  public long getId() {
    return id;
  }

  public boolean hasTreeId() {
    return treeId != NO_TREE_ID;
  }

  public long getTreeId() {
    return treeId;
  }

  public void setTreeId(long treeId) {
    this.treeId = treeId;
  }

  public long getDepth() {
    return depth;
  }

  public void setDepth(long depth) {
    this.depth = depth;
  }

  public T getLft() {
    return lft;
  }

  public void setLft(T lft) {
    this.lft = lft;
  }

  public T getRgt() {
    return rgt;
  }

  public void setRgt(T rgt) {
    this.rgt = rgt;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  protected String toNodeString() {
    return String.format("[treeId: %s | lft: %s | rgt: %s]", getTreeId(), getLft(), getRgt());
  }

  @Override
  public String toString() {
    return String.format("%s (id: %d) %s", getName(),  getId(), toNodeString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return this.toString().equals(o.toString());
  }
}
