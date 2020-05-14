package works.hacker.mptt.dyadic;

import works.hacker.mptt.TreeEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class DyadicFractionsEntity extends TreeEntity {
  public static final long START = 0;
  public static final long END = 1;

  @Column(nullable = false)
  private long headN;

  @Column(nullable = false)
  private long headD;

  @Column(nullable = false)
  private long tailN;

  @Column(nullable = false)
  private long tailD;

  @Column(nullable = false)
  private double head;

  @Column(nullable = false)
  private double tail;

  public DyadicFractionsEntity() {
    super();

    this.headN = START;
    this.headD = END;

    this.tailN = END;
    this.tailD = END;

    this.head = (double) headN / (double) headD;
    this.tail = (double) tailN / (double) tailD;
  }

  private void updateHead() {
    this.head = (double) headN / (double) headD;
  }

  private void updateTail() {
    this.tail = (double) tailN / (double) tailD;
  }

  public long getHeadN() {
    return headN;
  }

  public void setHeadN(long headN) {
    this.headN = headN;
    updateHead();
  }

  public long getHeadD() {
    return headD;
  }

  public void setHeadD(long headD) {
    if (headD == 0) throw new IllegalArgumentException("Will lead to division by zero");
    this.headD = headD;
    updateHead();
  }

  public long getTailN() {
    return tailN;
  }

  public void setTailN(long tailN) {
    this.tailN = tailN;
    updateTail();
  }

  public long getTailD() {
    return tailD;
  }

  public void setTailD(long tailD) {
    if (tailD == 0) throw new IllegalArgumentException("Will lead to division by zero");
    this.tailD = tailD;
    updateTail();
  }

  @Override
  public String toString() {
    return String.format("[treeId: %d | lft: %d/%d | rgt: %d/%d]", treeId, headN, headD, tailN, tailD);
  }
}
