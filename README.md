# Modified Pre-order Tree Traversal - Implementation in Java Using JPA

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/works.hacker/mptt-jpa/badge.svg)](https://search.maven.org/artifact/works.hacker/mptt-jpa)

## Introduction

Hierarchical data is a collection of data where each item has a single parent and zero or more children (with the exception of the root item, which has no parent). Hierarchical data can be found in a variety of database applications, but mostly content management categories, and product categories.

There are basically two models for dealing with hierarchical data:

* [the adjacency list model](https://en.wikipedia.org/wiki/Adjacency_list)
* [the nested set model](https://en.wikipedia.org/wiki/Nested_set_model)

Both of the above Wikipedia articles give a good overview, but in order to understand how these are applied to an SQL database I recommend reading [Managing Hierarchical Data in MySQL](http://mikehillyer.com/articles/managing-hierarchical-data-in-mysql/).

MPTT, or *modified preorder tree traversal*, is an efficient way to store hierarchical data in the flat structure of a relational database table. It uses the nested set model as it provides a faster option for read operations compared to the tree traversal operations of an adjacency list.

## The MPTT Structure

The nested set model is using a technique to number the nodes according to a tree traversal, which visits each node twice, assigning numbers in order of visiting, at both visits. This leaves two numbers for each node, which are stored as two attributes. Querying and read operations becomes inexpensive: hierarchy membership can be tested by comparing these numbers. Updating (adding and removal of tree members) requires renumbering and is therefore expensive.

Let's assume the following sample tree structure:

<img src="diagrams/01-sample-tree.svg" alt="Fig. 1: Sample Tree Structure." style="zoom:88%;" />

**Fig. 1: Sample Tree Structure.**

Then the nested set traversal would assign the following `lft` and `rgt` numbers as each node is visited *(NOTE: leaf nodes are visited only once)*:

<img src="diagrams/02-tree-traversal.svg" alt="Fig. 2: MPTT Tree Traversal." style="zoom:100%;" />

**Fig. 2: MPTT Tree Traversal.**

The resulting flat table to persist in the relational database would be:

| ID | Name        | TREE_ID | LFT | RGT |
|----|-------------|--------:|----:|----:|
| 1  | root        | 100     | 1   | 14  |
| 2  | child-1     | 100     | 2   | 9   |
| 3  | subChild-1  | 100     | 3   | 6   |
| 4  | subSubChild | 100     | 4   | 5   |
| 5  | subChild-2  | 100     | 7   | 8   |
| 6  | child-2     | 100     | 10  | 13  |
| 7  | lastChild   | 100     | 11  | 12  |

**Table 1: MPTT Flat Representation (as relational database table).**

## Usage

Even though the MPTT implementation provided in [`works.hacker.mptt`](https://github.com/hacker-works/mptt-jpa/tree/master/src/main/java/works/hacker/mptt) has no dependencies on Spring or other non-standard libraries, the project unit / integration tests are using Spring; and the demo application is a very-simple Spring Boot application too.

[DEMO SPRING BOOT APP - BROWSE SOURCE CODE ON GITHUB](https://github.com/hacker-works/mptt-jpa/tree/develop/demo)

To use `mptt-jpa`:
1. Add the `works.hacker.mptt-jpa`-dependency to the `pom.xml` *(in case Maven is used)*.
2. Add a custom entity-type by extending the [`works.hacker.mptt.classic.MpttEntity`](https://github.com/hacker-works/mptt-jpa/blob/master/src/main/java/works/hacker/mptt/MpttEntity.java)-mapped superclass.
3. Add a custom repository interface by extending the [`works.hacker.works.MpttRepository`](https://github.com/hacker-works/mptt-jpa/blob/master/src/main/java/works/hacker/mptt/MpttRepository.java)-interface.
4. Add a custom repository implementation by extending the reference [`works.hacker.works.MpttRepositoryImpl`](https://github.com/hacker-works/mptt-jpa/blob/master/src/main/java/works/hacker/mptt/MpttRepositoryImpl.java)-implementation.

#### Add `mptt-jpa` to the POM

```xml
<dependency>
    <groupId>works.hacker</groupId>
    <artifactId>mptt-jpa</artifactId>
    <version>0.0.5</version>
</dependency>
``` 

#### Custom Entity Type

```java
@Entity
public class TagTree extends MpttEntity {
  private static final String NO_NAME = "NO_NAME";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private final String name;

  @SuppressWarnings({"Unused"})
  public TagTree() { // IMPORTANT! for some reason Hibernate requires a default constructor
    super(); // make sure to call super()
    this.name = NO_NAME;
  }

  public TagTree(String name) {
    super(); // make sure to call super()
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("%s (id: %d) %s", getName(),  getId(), super.toString());
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
```

#### Custom Repository Interface

Declare you the interface of the custom repository:

```java
public interface TagTreeRepositoryCustom extends MpttRepository<TagTree> {
}
```

In the tests and in the demo, the `MpttRepository`-functionality is added on top the standard `JpaRepository` provided by **Spring Data**. It is mandatory for our interface to follow the naming convention of `${Original Repository name}Custom`.

And then, the **Spring Data** original repository could include in addition to the `MpttRepository`-operations whatever needed query methods with the keywords supported by JPA.

```java
public interface TagTreeRepository extends JpaRepository<TagTree, Long>, TagTreeRepositoryCustom {
  TagTree findByName(String name);
}
```

#### Custom Repository Implementation

Then the repository implementation is as simple as:

```java
@Repository
public class TagTreeRepositoryImpl extends MpttRepositoryImpl<TagTree> implements
    TagTreeRepositoryCustom {
}
```

#### Examples

Inject dependency to your custom repository:

```java
@Autowired
private TagTreeRepository tagTreeRepo;
```

Due to the **Java Generics - Type Erasure**, you need to manually set the entity class prior using the repository:

```java
tagTreeRepo.setEntityClass(TagTree.class);
```

Now you can use the repository as a standard `JpaRepository<TagTree, Long>`:

```java
var persistedTagTreeNodesCount = tagTreeRepo.count();
```

Or create the sample tree from **Fig. 1: Sample Tree Structure.**

```java
TagTree root = new TagTree("root");
Long treeId = tagTreeRepo.startTree(root);

TagTree child1 = new TagTree("child-1");
tagTreeRepo.addChild(root, child1);

TagTree subChild1 = new TagTree("subChild-1");
tagTreeRepo.addChild(child1, subChild1);

TagTree subSubChild = new TagTree("subSubChild");
tagTreeRepo.addChild(subChild1, subSubChild);

TagTree subChild2 = new TagTree("subChild-2");
tagTreeRepo.addChild(child1, subChild2);

TagTree child2 = new TagTree("child-2");
tagTreeRepo.addChild(root, child2);

TagTree lastSubChild = new TagTree("lastSubChild");
tagTreeRepo.addChild(child2, lastSubChild);
```

You can print the tree to a string:

```java
var root = tagTreeRepo.findTreeRoot(treeId);
var fullTree = tagTreeRepo.printTree(root);
```

The result would be a string containing a tree similar to the `tree` command line interface command:

```
.
└── root (id: 1) [treeId: 100 | lft: 1 | rgt: 14]
    ├── child-1 (id: 2) [treeId: 100 | lft: 6 | rgt: 13]
    │   ├── subChild-1 (id: 3) [treeId: 100 | lft: 9 | rgt: 12]
    │   │   └── subSubChild (id: 4) [treeId: 100 | lft: 10 | rgt: 11]
    │   └── subChild-2 (id: 5) [treeId: 100 | lft: 7 | rgt: 8]
    └── child-2 (id: 6) [treeId: 100 | lft: 2 | rgt: 5]
        └── lastSubChild (id: 7) [treeId: 100 | lft: 3 | rgt: 4]
```

You can print also a sub-tree:

```java
var child1 = tagTreeRepo.findByName("child-1");
var partialTree = tagTreeRepo.printTree(child1);
```

...to get:

```
.
└── child-1 (id: 2) [treeId: 100 | lft: 6 | rgt: 13]
    ├── subChild-1 (id: 3) [treeId: 100 | lft: 9 | rgt: 12]
    │   └── subSubChild (id: 4) [treeId: 100 | lft: 10 | rgt: 11]
    └── subChild-2 (id: 5) [treeId: 100 | lft: 7 | rgt: 8]
```

To get a sorted list of the direct children of a node:

```java
var root = tagTreeRepo.findTreeRoot(treeId);
var directChildren = tagTreeRepo.findChildren(root);
```

To get the list of the ancestors of a node:

```java
var subSubChild = tagTreeRepo.findByName("subSubChild");
var ancestors = tagTreeRepo.findAncestors(subSubChild);
```

Works the same for the `findParent` and `findSubTree`-operations.

To remove a child from a parent:

```java
var root = tagTreeRepo.findByName("root");
var child1 = tagTreeRepo.findByName("child-1");
var removed = tagTreeRepo.removeChild(root, child1);
```

The resulting tree should be:

```
.
└── root (id: 1) [treeId: 100 | lft: 1 | rgt: 6]
    └── child-2 (id: 6) [treeId: 100 | lft: 2 | rgt: 5]
        └── lastSubChild (id: 7) [treeId: 100 | lft: 3 | rgt: 4]
```

**HAPPY HACKING! ...AND MAY THE SOURCE BE WITH YOU!**
