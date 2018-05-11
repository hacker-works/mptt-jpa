package hacker.works.mpttjpa.sample;

import hacker.works.mpttjpa.MpttHierarchicalEntity;

import javax.persistence.*;

@Entity
@Table(name = "TAG_TREE")
@NamedQueries(value = {
  @NamedQuery(name = "findAncestors", query =
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND tag.lft < :lft AND :rgt < tag.rgt" +
    " ORDER BY tag.lft ASC"),

  @NamedQuery(name = "findSubtree", query = 
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND :lft <= tag.lft AND tag.rgt <= :rgt"),

  @NamedQuery(name = "findEntitiesWithLeftGreaterThan", query = 
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND :value < tag.lft"),

  @NamedQuery(name = "findEntitiesWithLeftGreaterThanOrEqual", query = 
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND :value <= tag.lft"),

  @NamedQuery(name = "findEntitiesWithRightGreaterThan", query = 
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND :value < tag.rgt"),

  @NamedQuery(name = "findEntityByRightValue", query = 
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND tag.rgt = :value"),

  @NamedQuery(name = "findHierarchyRoot", query = 
    "SELECT tag" +
    " FROM TagTree tag" +
    " WHERE :hierarchyId IS NOT NULL AND :hierarchyId = tag.hierarchyId" +
    " AND tag.lft = 1"),

  @NamedQuery(name = "findChildren", query = 
    "SELECT child" +
    " FROM TagTree child" +
    " WHERE :hierarchyId IS NOT NULL" +
    " AND child.hierarchyId = :hierarchyId" +
    " AND :lft < child.lft AND child.rgt < :rgt" +
    " AND :lft =" +
    " (SELECT MAX(ancestor.lft)" +
    "   FROM TagTree ancestor" +
    "   WHERE ancestor.hierarchyId = child.hierarchyId" +
    "   AND ancestor.lft < child.lft" +
    "   AND child.rgt < ancestor.lft.rgt)" +
    " ORDER BY child.lft ASC")
})
public class TagTree implements MpttHierarchicalEntity<TagTree> {

  public TagTree(String name) {
    this.name = name;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id;

  @Column(nullable = false)
  public String name;

  @Column(nullable = false)
  private Long hierarchyId;

  @Column(nullable = false)
  private Long lft;

  @Column(nullable = false)
  private Long rgt;

  @Override
  public void setLft(Long lft) {
    this.lft = lft;
  }

  @Override
  public Long getLft() {
    return this.lft;
  }

  @Override
  public void setRgt(Long rgt) {
    this.rgt = rgt;
  }

  @Override
  public Long getRgt() {
    return this.rgt;
  }

  @Override
  public void setHierarchyId(Long hierarchyId) {
    this.hierarchyId = hierarchyId;
  }

  @Override
  public Long getHierarchyId() {
    return this.hierarchyId;
  }

  @Override
  public Long getId() {
    return this.id;
  }

  @Override
  public String toString() {
    return "TagTree(" + this.name + ")" + super.toString();
  }
}
