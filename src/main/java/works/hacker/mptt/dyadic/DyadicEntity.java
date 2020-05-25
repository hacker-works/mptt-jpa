package works.hacker.mptt.dyadic;

import works.hacker.mptt.TreeEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class DyadicEntity extends TreeEntity<Double> {
  public static final long START = 0;
  public static final long END = 1;

  @Column(nullable = false)
  private long lftN;

  @Column(nullable = false)
  private long lftD;

  @Column(nullable = false)
  private long rgtN;

  @Column(nullable = false)
  private long rgtD;

  public DyadicEntity() {
    super();
  }

  public DyadicEntity(String name) {
    super(name);
  }

  @Override
  public void setDefaults() {
    super.setDefaults();

    this.lftN = START;
    this.lftD = END;

    this.rgtN = END;
    this.rgtD = END;

    this.lft = (double) lftN / (double) lftD;
    this.rgt = (double) rgtN / (double) rgtD;
  }

  @Override
  public Double getStartLft() {
    return (double) START;
  }

  @Override
  public Double getStartRgt() {
    return (double) END;
  }

  private void updateHead() {
    this.lft = (double) lftN / (double) lftD;
  }

  private void updateTail() {
    this.rgt = (double) rgtN / (double) rgtD;
  }

  public long getLftN() {
    return lftN;
  }

  public void setLftN(long lftN) {
    this.lftN = lftN;
    updateHead();
  }

  public long getLftD() {
    return lftD;
  }

  public void setLftD(long lftD) {
    if (lftD == 0) throw new IllegalArgumentException("Will lead to division by zero");
    this.lftD = lftD;
    updateHead();
  }

  public long getRgtN() {
    return rgtN;
  }

  public void setRgtN(long rgtN) {
    this.rgtN = rgtN;
    updateTail();
  }

  public long getRgtD() {
    return rgtD;
  }

  public void setRgtD(long rgtD) {
    if (rgtD == 0) throw new IllegalArgumentException("Will lead to division by zero");
    this.rgtD = rgtD;
    updateTail();
  }

  @Override
  protected String toNodeString() {
    return String.format("[treeId: %d | lft: %d/%d | rgt: %d/%d]", treeId, lftN, lftD, rgtN, rgtD);
  }
}
