# Java SE

## Java 集合框架



![Java Collection框架的层次结构](MarkDown_Java%20SE.assets/java-collection-hierarchy.png)



![image-20210310203841688](MarkDown_Java%20SE.assets/image-20210310203841688.png)





### 面试题

#### **说说常见的集合有哪些吧？**

答：Map接口和Collection接口是所有集合框架的父接口：

1. Collection接口的子接口包括：Set接口和List接口
2. Map接口的实现类主要有：HashMap、TreeMap、Hashtable、ConcurrentHashMap等
3. Set接口的实现类主要有：HashSet、TreeSet、LinkedHashSet等
4. List接口的实现类主要有：ArrayList、LinkedList、Stack（废弃）以及Vector等



### HashMap

Java 8系列之重新认识HashMap - 美团技术团队的文章 - 知乎 https://zhuanlan.zhihu.com/p/21673805

Java为数据结构中的映射定义了一个接口java.util.Map，此接口主要有四个常用的实现类，分别是HashMap、Hashtable、LinkedHashMap和TreeMap，类继承关系如下图所示：

![img](MarkDown_Java%20SE.assets/26341ef9fe5caf66ba0b7c40bba264a5_1440w.png)

下面针对各个实现类的特点做一些说明：

(1) **HashMap**：它根据键的hashCode值存储数据，大多数情况下可以直接定位到它的值，因而具有很快的访问速度，但遍历顺序却是不确定的。 HashMap最多只允许一条记录的键为null，允许多条记录的值为null。HashMap非线程安全，即任一时刻可以有多个线程同时写HashMap，可能会导致数据的不一致。如果需要满足线程安全，可以用 Collections的synchronizedMap方法使HashMap具有线程安全的能力，或者使用ConcurrentHashMap。

(2) **Hashtable**：Hashtable是遗留类，很多映射的常用功能与HashMap类似，不同的是它承自Dictionary类，并且是线程安全的，任一时间只有一个线程能写Hashtable，并发性不如ConcurrentHashMap，因为ConcurrentHashMap引入了分段锁。Hashtable不建议在新代码中使用，不需要线程安全的场合可以用HashMap替换，需要线程安全的场合可以用ConcurrentHashMap替换。

(3) **LinkedHashMap**：LinkedHashMap是HashMap的一个子类，保存了记录的插入顺序，在用Iterator遍历LinkedHashMap时，先得到的记录肯定是先插入的，也可以在构造时带参数，按照访问次序排序。

(4) **TreeMap**：TreeMap实现SortedMap接口，能够把它保存的记录根据键排序，默认是按键值的升序排序，也可以指定排序的比较器，当用Iterator遍历TreeMap时，得到的记录是排过序的。如果使用排序的映射，建议使用TreeMap。在使用TreeMap时，key必须实现Comparable接口或者在构造TreeMap传入自定义的Comparator，否则会在运行时抛出java.lang.ClassCastException类型的异常。

对于上述四种Map类型的类，要求映射中的key是不可变对象。不可变对象是该对象在创建后它的哈希值不会被改变。如果对象的哈希值发生变化，Map对象很可能就定位不到映射的位置了。

通过上面的比较，我们知道了HashMap是Java的Map家族中一个普通成员，鉴于它可以满足大多数场景的使用条件，所以是使用频度最高的一个。下文我们主要结合源码，从存储结构、常用方法分析、扩容以及安全性等方面深入讲解HashMap的工作原理。



#### **内部实现**

搞清楚HashMap，首先需要知道HashMap是什么，即它的存储结构-字段；其次弄明白它能干什么，即它的功能实现-方法。下面我们针对这两个方面详细展开讲解。

#### 存储结构-字段

从结构实现来讲，HashMap是数组+链表+红黑树（JDK1.8增加了红黑树部分）实现的，如下如所示。

![img](MarkDown_Java%20SE.assets/8db4a3bdfb238da1a1c4431d2b6e075c_1440w.png)

这里需要讲明白两个问题：数据底层具体存储的是什么？这样的存储方式有什么优点呢？

(1) 从源码可知，HashMap类中有一个非常重要的字段，就是 Node[] table，即哈希桶数组，明显它是一个Node的数组。我们来看Node[JDK1.8]是何物。

```java
static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;    //用来定位数组索引位置
        final K key;
        V value;
        Node<K,V> next;   //链表的下一个node

        Node(int hash, K key, V value, Node<K,V> next) { ... }
        public final K getKey(){ ... }
        public final V getValue() { ... }
        public final String toString() { ... }
        public final int hashCode() { ... }
        public final V setValue(V newValue) { ... }
        public final boolean equals(Object o) { ... }
}
```

Node是HashMap的一个内部类，实现了Map.Entry接口，本质是就是一个映射(键值对)。上图中的每个黑色圆点就是一个Node对象。

(2) HashMap就是使用哈希表来存储的。哈希表为解决冲突，可以采用开放地址法和链地址法等来解决问题，Java中HashMap采用了链地址法。链地址法，简单来说，就是数组加链表的结合。在每个数组元素上都一个链表结构，当数据被Hash后，得到数组下标，把数据放在对应下标元素的链表上。例如程序执行下面代码：

```java
    map.put("美团","小美");
```

系统将调用"美团"这个key的**hashCode()**方法得到其hashCode 值（该方法适用于每个Java对象），然后再通过Hash算法的后两步运算（高位运算和取模运算，下文有介绍）来定位该键值对的存储位置，有时两个key会定位到相同的位置，表示发生了Hash碰撞。当然Hash算法计算结果越分散均匀，Hash碰撞的概率就越小，map的存取效率就会越高。



如果哈希桶数组很大，即使较差的Hash算法也会比较分散，如果哈希桶数组数组很小，即使好的Hash算法也会出现较多碰撞，所以就需要在空间成本和时间成本之间权衡，其实就是在根据实际情况确定哈希桶数组的大小，并在此基础上设计好的hash算法减少Hash碰撞。那么通过什么方式来控制map使得Hash碰撞的概率又小，哈希桶数组（Node[] table）占用空间又少呢？答案就是好的Hash算法和扩容机制。

在理解Hash和扩容流程之前，我们得先了解下HashMap的几个字段。从HashMap的默认构造函数源码可知，构造函数就是对下面几个字段进行初始化，源码如下：

```
     int threshold;             // 所能容纳的key-value对极限 
     final float loadFactor;    // 负载因子
     int modCount;  
     int size;
```

首先，Node[] table的初始化长度length(默认值是16)，**Load factor为负载因子(默认值是0.75)**，**threshold是HashMap所能容纳的最大数据量的Node(键值对)个数**。threshold = length * Load factor。也就是说，在数组定义好长度之后，负载因子越大，所能容纳的键值对个数越多。



结合负载因子的定义公式可知，threshold就是在此Load factor和length(数组长度)对应下允许的最大元素数目，超过这个数目就重新resize(扩容)，扩容后的HashMap容量是之前容量的两倍。默认的负载因子0.75是对空间和时间效率的一个平衡选择，建议大家不要修改，除非在时间和空间比较特殊的情况下，如果**内存空间很多而又对时间效率要求很高，可以降低负载因子Load factor的值**；相反，如果**内存空间紧张而对时间效率要求不高，可以增加负载因子loadFactor的值**，这个值可以大于1。



**size**这个字段其实很好理解，就是HashMap中**实际存在的键值对数量**。注意和table的长度length、容纳最大键值对数量threshold的区别。而**modCount字段主要用来记录HashMap内部结构发生变化的次数**，主要用于迭代的快速失败。强调一点，内部结构发生变化指的是结构发生变化，例如put新键值对，但是某个key对应的value值被覆盖不属于结构变化。



#### 1. 确定哈希桶数组索引位置

不管增加、删除、查找键值对，定位到哈希桶数组的位置都是很关键的第一步。前面说过HashMap的数据结构是数组和链表的结合，所以我们当然希望这个HashMap里面的元素位置尽量分布均匀些，尽量使得每个位置上的元素数量只有一个，那么当我们用hash算法求得这个位置的时候，马上就可以知道对应位置的元素就是我们要的，不用遍历链表，大大优化了查询的效率。HashMap定位数组索引位置，直接决定了hash方法的离散性能。先看看源码的实现(方法一+方法二):

```java
方法一：
static final int hash(Object key) {   //jdk1.8 & jdk1.7
     int h;
     // h = key.hashCode() 为第一步 取hashCode值
     // h ^ (h >>> 16)  为第二步 高位参与运算
     return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
方法二：
static int indexFor(int h, int length) {  //jdk1.7的源码，jdk1.8没有这个方法，但是实现原理一样的
     return h & (length-1);  //第三步 取模运算
}
```

这里的Hash算法本质上就是三步：**取key的hashCode值、高位运算、取模运算**。

对于任意给定的对象，只要它的hashCode()返回值相同，那么程序调用方法一所计算得到的Hash码值总是相同的。我们首先想到的就是把hash值对数组长度取模运算，这样一来，元素的分布相对来说是比较均匀的。但是，模运算的消耗还是比较大的，在HashMap中是这样做的：调用方法二来计算该对象应该保存在table数组的哪个索引处。

这个方法非常巧妙，它通过h & (table.length -1)来得到该对象的保存位，而HashMap底层数组的长度总是2的n次方，这是HashMap在速度上的优化。当length总是2的n次方时，h& (length-1)运算等价于对length取模，也就是h%length，但是&比%具有更高的效率。

在JDK1.8的实现中，优化了高位运算的算法，通过hashCode()的高16位异或低16位实现的：(h = k.hashCode()) ^ (h >>> 16)，主要是从速度、功效、质量来考虑的，这么做可以在数组table的length比较小的时候，也能保证考虑到高低Bit都参与到Hash的计算中，同时不会有太大的开销。





#### 2. 分析HashMap的put方法

![img](MarkDown_Java%20SE.assets/58e67eae921e4b431782c07444af824e_1440w.png)

①.判断键值对数组table[i]是否为空或为null，否则执行resize()进行扩容；

②.根据键值key计算hash值得到插入的数组索引i，如果table[i]==null，直接新建节点添加，转向⑥，如果table[i]不为空，转向③；

③.判断table[i]的首个元素是否和key一样，如果相同直接覆盖value，否则转向④，这里的相同指的是hashCode以及equals；

④.判断table[i] 是否为treeNode，即table[i] 是否是红黑树，如果是红黑树，则直接在树中插入键值对，否则转向⑤；

⑤.遍历table[i]，判断链表长度是否大于8，大于8的话把链表转换为红黑树，在红黑树中执行插入操作，否则进行链表的插入操作；遍历过程中若发现key已经存在直接覆盖value即可；

⑥.插入成功后，判断实际存在的键值对数量size是否超多了最大容量threshold，如果超过，进行扩容。





#### 3. 扩容机制

扩容(resize)就是重新计算容量，向HashMap对象里不停的添加元素，而HashMap对象内部的数组无法装载更多的元素时，对象就需要扩大数组的长度，以便能装入更多的元素。当然Java里的数组是无法自动扩容的，方法是使用一个新的数组代替已有的容量小的数组，就像我们用一个小桶装水，如果想装更多的水，就得换大水桶。



下面我们讲解下JDK1.8做了哪些优化。经过观测可以发现，我们使用的是2次幂的扩展(指长度扩为原来2倍)，所以，元素的位置要么是在原位置，要么是在原位置再移动2次幂的位置。看下图可以明白这句话的意思，n为table的长度，图（a）表示扩容前的key1和key2两种key确定索引位置的示例，图（b）表示扩容后key1和key2两种key确定索引位置的示例，其中hash1是key1对应的哈希与高位运算结果。

![img](MarkDown_Java%20SE.assets/a285d9b2da279a18b052fe5eed69afe9_1440w.png)

元素在重新计算hash之后，因为n变为2倍，那么n-1的mask范围在高位多1bit(红色)，因此新的index就会发生这样的变化：

![img](MarkDown_Java%20SE.assets/b2cb057773e3d67976c535d6ef547d51_1440w.png)

因此，我们在扩充HashMap的时候，不需要像JDK1.7的实现那样重新计算hash，只需要看看原来的hash值新增的那个bit是1还是0就好了，是0的话索引没变，是1的话索引变成“原索引+oldCap”，可以看看下图为16扩充为32的resize示意图：

![img](MarkDown_Java%20SE.assets/544caeb82a329fa49cc99842818ed1ba_1440w.png)

这个设计确实非常的巧妙，既省去了重新计算hash值的时间，而且同时，由于新增的1bit是0还是1可以认为是随机的，因此resize的过程，均匀的把之前的冲突的节点分散到新的bucket了。这一块就是JDK1.8新增的优化点。有一点注意区别，JDK1.7中rehash的时候，旧链表迁移新链表的时候，如果在新表的数组索引位置相同，则链表元素会倒置，但是从上图可以看出，JDK1.8不会倒置。









#### HashMap是怎么解决哈希冲突的？

##### **什么是哈希？**

简单的说就是一种将任意长度的消息压缩到某一固定长度的消息摘要的函数。**根据同一散列函数计算出的散列值如果不同，那么输入值肯定也不同。但是，根据同一散列函数计算出的散列值如果相同，输入值不一定相同。** **当两个不同的输入值，根据同一散列函数计算出相同的散列值的现象，我们就把它叫做碰撞（哈希碰撞）。**

##### **hash()函数**

上面提到的问题，主要是因为如果使用hashCode取余，那么相当于**参与运算的只有hashCode的低位**，高位是没有起到任何作用的，所以我们的思路就是让hashCode取值出的高位也参与运算，进一步降低hash碰撞的概率，使得数据分布更平均，我们把这样的操作称为**扰动**，在**JDK 1.8**中的hash()函数如下：

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);// 与自己右移16位进行异或运算（高低位异或）
}
```

这比在**JDK 1.7**中，更为简洁，**相比在1.7中的4次位运算，5次异或运算（9次扰动），在1.8中，只进行了1次位运算和1次异或运算（2次扰动）；**

##### **总结**

简单总结一下HashMap是使用了哪些方法来有效解决哈希冲突的：

**1. 使用链地址法（使用散列表）来链接拥有相同hash值的数据；**
**2. 使用2次扰动函数（hash函数）来降低哈希冲突的概率，使得数据分布更平均；**
**3. 引入红黑树进一步降低遍历的时间复杂度，使得遍历更快；**







## 源码分析

### ArrayList

`ArrayList` 的底层是数组队列，相当于动态数组。与 Java 中的数组相比，它的容量能动态增长。在添加大量元素前，应用程序可以使用`ensureCapacity`操作来增加 `ArrayList` 实例的容量。这可以减少递增式再分配的数量。

`ArrayList`继承于 **`AbstractList`** ，实现了 **`List`**, **`RandomAccess`**, **`Cloneable`**, **`java.io.Serializable`** 这些接口。

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable{
  }
```

- `RandomAccess` 是一个标志接口，表明实现这个这个接口的 List 集合是支持**快速随机访问**的。在 `ArrayList` 中，我们即可以通过元素的序号快速获取元素对象，这就是快速随机访问。
- `ArrayList` 实现了 **`Cloneable` 接口** ，即覆盖了函数`clone()`，能被克隆。
- `ArrayList` 实现了 `java.io.Serializable `接口，这意味着`ArrayList`支持序列化，能通过序列化去传输。

####  Arraylist 和 Vector 的区别?

1. `ArrayList` 是 `List` 的主要实现类，底层使用 `Object[ ]`存储，适用于频繁的查找工作，线程不安全 ；
2. `Vector` 是 `List` 的古老实现类，底层使用 `Object[ ]`存储，线程安全的。

####  Arraylist 与 LinkedList 区别?

1. **是否保证线程安全：** `ArrayList` 和 `LinkedList` 都是不同步的，也就是不保证线程安全；
2. **底层数据结构：** `Arraylist` 底层使用的是 **`Object` 数组**；`LinkedList` 底层使用的是 **双向链表** 数据结构（JDK1.6 之前为循环链表，JDK1.7 取消了循环。注意双向链表和双向循环链表的区别，下面有介绍到！）
3. **插入和删除是否受元素位置的影响：** ① **`ArrayList` 采用数组存储，所以插入和删除元素的时间复杂度受元素位置的影响。** 比如：执行`add(E e)`方法的时候， `ArrayList` 会默认在将指定的元素追加到此列表的末尾，这种情况时间复杂度就是 O(1)。但是如果要在指定位置 i 插入和删除元素的话（`add(int index, E element)`）时间复杂度就为 O(n-i)。因为在进行上述操作的时候集合中第 i 和第 i 个元素之后的(n-i)个元素都要执行向后位/向前移一位的操作。 ② **`LinkedList` 采用链表存储，所以对于`add(E e)`方法的插入，删除元素时间复杂度不受元素位置的影响，近似 O(1)，如果是要在指定位置`i`插入和删除元素的话（`(add(int index, E element)`） 时间复杂度近似为`o(n))`因为需要先移动到指定位置再插入。**
4. **是否支持快速随机访问：** `LinkedList` 不支持高效的随机元素访问，而 `ArrayList` 支持。快速随机访问就是通过元素的序号快速获取元素对象(对应于`get(int index)`方法)。
5. **内存空间占用：** `ArrayList` 的空 间浪费主要体现在在 list 列表的结尾会预留一定的容量空间，而 `LinkedList` 的空间花费则体现在它的每一个元素都需要消耗比 `ArrayList` 更多的空间（因为要存放直接后继和直接前驱以及数据）。

#### ArrayList 核心源码解读

```java
package java.util;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    private static final long serialVersionUID = 8683452581122892189L;

    /**
     * 默认初始容量大小
     */
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * 空数组（用于空实例）。
     */
    private static final Object[] EMPTY_ELEMENTDATA = {};

     //用于默认大小空实例的共享空数组实例。
      //我们把它从EMPTY_ELEMENTDATA数组中区分出来，以知道在添加第一个元素时容量需要增加多少。
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     * 保存ArrayList数据的数组
     */
    transient Object[] elementData; // non-private to simplify nested class access

    /**
     * ArrayList 所包含的元素个数
     */
    private int size;

    /**
     * 带初始容量参数的构造函数（用户可以在创建ArrayList对象时自己指定集合的初始大小）
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            //如果传入的参数大于0，创建initialCapacity大小的数组
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            //如果传入的参数等于0，创建空数组
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            //其他情况，抛出异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }

    /**
     *默认无参构造函数
     *DEFAULTCAPACITY_EMPTY_ELEMENTDATA 为0.初始化为10，也就是说初始其实是空数组 当添加第一个元素的时候数组容量才变成10
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    /**
     * 构造一个包含指定集合的元素的列表，按照它们由集合的迭代器返回的顺序。
     */
    public ArrayList(Collection<? extends E> c) {
        //将指定集合转换为数组
        elementData = c.toArray();
        //如果elementData数组的长度不为0
        if ((size = elementData.length) != 0) {
            // 如果elementData不是Object类型数据（c.toArray可能返回的不是Object类型的数组所以加上下面的语句用于判断）
            if (elementData.getClass() != Object[].class)
                //将原来不是Object类型的elementData数组的内容，赋值给新的Object类型的elementData数组
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // 其他情况，用空数组代替
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

    /**
     * 修改这个ArrayList实例的容量是列表的当前大小。 应用程序可以使用此操作来最小化ArrayList实例的存储。
     */
    public void trimToSize() {
        modCount++;
        if (size < elementData.length) {
            elementData = (size == 0)
              ? EMPTY_ELEMENTDATA
              : Arrays.copyOf(elementData, size);
        }
    }
//下面是ArrayList的扩容机制
//ArrayList的扩容机制提高了性能，如果每次只扩充一个，
//那么频繁的插入会导致频繁的拷贝，降低性能，而ArrayList的扩容机制避免了这种情况。
    /**
     * 如有必要，增加此ArrayList实例的容量，以确保它至少能容纳元素的数量
     * @param   minCapacity   所需的最小容量
     */
    public void ensureCapacity(int minCapacity) {
        //如果是true，minExpand的值为0，如果是false,minExpand的值为10
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            // any size if not default element table
            ? 0
            // larger than default for default empty table. It's already
            // supposed to be at default size.
            : DEFAULT_CAPACITY;
        //如果最小容量大于已有的最大容量
        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }
   //得到最小扩容量
    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
              // 获取“默认的容量”和“传入参数”两者之间的最大值
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }
  //判断是否需要扩容
    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            //调用grow方法进行扩容，调用此方法代表已经开始扩容了
            grow(minCapacity);
    }

    /**
     * 要分配的最大数组大小
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * ArrayList扩容的核心方法。
     */
    private void grow(int minCapacity) {
        // oldCapacity为旧容量，newCapacity为新容量
        int oldCapacity = elementData.length;
        //将oldCapacity 右移一位，其效果相当于oldCapacity /2，
        //我们知道位运算的速度远远快于整除运算，整句运算式的结果就是将新容量更新为旧容量的1.5倍，
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        //然后检查新容量是否大于最小需要容量，若还是小于最小需要容量，那么就把最小需要容量当作数组的新容量，
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        //再检查新容量是否超出了ArrayList所定义的最大容量，
        //若超出了，则调用hugeCapacity()来比较minCapacity和 MAX_ARRAY_SIZE，
        //如果minCapacity大于MAX_ARRAY_SIZE，则新容量则为Interger.MAX_VALUE，否则，新容量大小则为 MAX_ARRAY_SIZE。
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    //比较minCapacity和 MAX_ARRAY_SIZE
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     *返回此列表中的元素数。
     */
    public int size() {
        return size;
    }

    /**
     * 如果此列表不包含元素，则返回 true 。
     */
    public boolean isEmpty() {
        //注意=和==的区别
        return size == 0;
    }

    /**
     * 如果此列表包含指定的元素，则返回true 。
     */
    public boolean contains(Object o) {
        //indexOf()方法：返回此列表中指定元素的首次出现的索引，如果此列表不包含此元素，则为-1
        return indexOf(o) >= 0;
    }

    /**
     *返回此列表中指定元素的首次出现的索引，如果此列表不包含此元素，则为-1
     */
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                //equals()方法比较
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回此列表中指定元素的最后一次出现的索引，如果此列表不包含元素，则返回-1。.
     */
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size-1; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = size-1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    /**
     * 返回此ArrayList实例的浅拷贝。 （元素本身不被复制。）
     */
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();
            //Arrays.copyOf功能是实现数组的复制，返回复制后的数组。参数是被复制的数组和复制的长度
            v.elementData = Arrays.copyOf(elementData, size);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // 这不应该发生，因为我们是可以克隆的
            throw new InternalError(e);
        }
    }

    /**
     *以正确的顺序（从第一个到最后一个元素）返回一个包含此列表中所有元素的数组。
     *返回的数组将是“安全的”，因为该列表不保留对它的引用。 （换句话说，这个方法必须分配一个新的数组）。
     *因此，调用者可以自由地修改返回的数组。 此方法充当基于阵列和基于集合的API之间的桥梁。
     */
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    /**
     * 以正确的顺序返回一个包含此列表中所有元素的数组（从第一个到最后一个元素）;
     *返回的数组的运行时类型是指定数组的运行时类型。 如果列表适合指定的数组，则返回其中。
     *否则，将为指定数组的运行时类型和此列表的大小分配一个新数组。
     *如果列表适用于指定的数组，其余空间（即数组的列表数量多于此元素），则紧跟在集合结束后的数组中的元素设置为null 。
     *（这仅在调用者知道列表不包含任何空元素的情况下才能确定列表的长度。）
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            // 新建一个运行时类型的数组，但是ArrayList数组的内容
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
            //调用System提供的arraycopy()方法实现数组之间的复制
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    // Positional Access Operations

    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    /**
     * 返回此列表中指定位置的元素。
     */
    public E get(int index) {
        rangeCheck(index);

        return elementData(index);
    }

    /**
     * 用指定的元素替换此列表中指定位置的元素。
     */
    public E set(int index, E element) {
        //对index进行界限检查
        rangeCheck(index);

        E oldValue = elementData(index);
        elementData[index] = element;
        //返回原来在这个位置的元素
        return oldValue;
    }

    /**
     * 将指定的元素追加到此列表的末尾。
     */
    public boolean add(E e) {
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //这里看到ArrayList添加元素的实质就相当于为数组赋值
        elementData[size++] = e;
        return true;
    }

    /**
     * 在此列表中的指定位置插入指定的元素。
     *先调用 rangeCheckForAdd 对index进行界限检查；然后调用 ensureCapacityInternal 方法保证capacity足够大；
     *再将从index开始之后的所有成员后移一个位置；将element插入index位置；最后size加1。
     */
    public void add(int index, E element) {
        rangeCheckForAdd(index);

        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //arraycopy()这个实现数组之间复制的方法一定要看一下，下面就用到了arraycopy()方法实现数组自己复制自己
        System.arraycopy(elementData, index, elementData, index + 1,
                         size - index);
        elementData[index] = element;
        size++;
    }

    /**
     * 删除该列表中指定位置的元素。 将任何后续元素移动到左侧（从其索引中减去一个元素）。
     */
    public E remove(int index) {
        rangeCheck(index);

        modCount++;
        E oldValue = elementData(index);

        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // clear to let GC do its work
      //从列表中删除的元素
        return oldValue;
    }

    /**
     * 从列表中删除指定元素的第一个出现（如果存在）。 如果列表不包含该元素，则它不会更改。
     *返回true，如果此列表包含指定的元素
     */
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    /*
     * Private remove method that skips bounds checking and does not
     * return the value removed.
     */
    private void fastRemove(int index) {
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        elementData[--size] = null; // clear to let GC do its work
    }

    /**
     * 从列表中删除所有元素。
     */
    public void clear() {
        modCount++;

        // 把数组中所有的元素的值设为null
        for (int i = 0; i < size; i++)
            elementData[i] = null;

        size = 0;
    }

    /**
     * 按指定集合的Iterator返回的顺序将指定集合中的所有元素追加到此列表的末尾。
     */
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * 将指定集合中的所有元素插入到此列表中，从指定的位置开始。
     */
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                             numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
     * 从此列表中删除所有索引为fromIndex （含）和toIndex之间的元素。
     *将任何后续元素移动到左侧（减少其索引）。
     */
    protected void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = size - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                         numMoved);

        // clear to let GC do its work
        int newSize = size - (toIndex-fromIndex);
        for (int i = newSize; i < size; i++) {
            elementData[i] = null;
        }
        size = newSize;
    }

    /**
     * 检查给定的索引是否在范围内。
     */
    private void rangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * add和addAll使用的rangeCheck的一个版本
     */
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * 返回IndexOutOfBoundsException细节信息
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    /**
     * 从此列表中删除指定集合中包含的所有元素。
     */
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        //如果此列表被修改则返回true
        return batchRemove(c, false);
    }

    /**
     * 仅保留此列表中包含在指定集合中的元素。
     *换句话说，从此列表中删除其中不包含在指定集合中的所有元素。
     */
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }


    /**
     * 从列表中的指定位置开始，返回列表中的元素（按正确顺序）的列表迭代器。
     *指定的索引表示初始调用将返回的第一个元素为next 。 初始调用previous将返回指定索引减1的元素。
     *返回的列表迭代器是fail-fast 。
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);
    }

    /**
     *返回列表中的列表迭代器（按适当的顺序）。
     *返回的列表迭代器是fail-fast 。
     */
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    /**
     *以正确的顺序返回该列表中的元素的迭代器。
     *返回的迭代器是fail-fast 。
     */
    public Iterator<E> iterator() {
        return new Itr();
    }


```



#### ArrayList中两个空数组的作用

```java
transient Object[] elementData;
```

由transient修饰的数组，我们所说的ArrayList的底层是个数组就是它了！！！

transient关键字的作用

java的serialization提供了一个非常棒的存储对象状态的机制，说白了serialization就是把对象的状态存储到硬盘上去，等需要的时候就可以再把它读出来使用。有些时候像银行卡号这些字段是不希望在网络上传输的，transient的作用就是把这个字段的生命周期仅存于调用者的内存中而不会写到磁盘里持久化，意思是transient修饰的elementData字段，他的生命周期仅仅在内存中，不会被写到磁盘中。
modCount是什么？ 在成员属性中并没有找到这个东西。那他为什么会出现在这里？
真相只有一个：去他的（ArrayList）的父类找找看。

果不其然在AbstractList.java中找到了modCount.

```java
protected transient int modCount = 0;
```

通过阅读该变量的注释我们可以发现modCount是用来记录ArrayList被修改的次数的。


在数据结构的学习中ArrayList（顺序表）可以说是最早乃至第一个接触的东西，并且在此之前想必也实现过 “MyArrayList”，然而今天由于好奇我就看了看大佬是如何实现“ArrayList”的。

我们从头来看

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```


不难看出ArrayList继承于AbstractList类同时也执行了RandomAccess，Cloneable，java.io.Serializable接口，

不难看出ArrayList继承于AbstractList类同时也执行了RandomAccess，Cloneable，java.io.Serializable接口，

我们先来看看ArrayList执行的三个接口

RandomAccess
RandomAccess接口的作用是只要List集合实现这个接口，就能支持快速随机访问。
具体细节请访问：
ArrayList集合实现RandomAccess接口有何作用？为何LinkedList集合却没实现这接口？

Cloneable
Cloneable 接口是用于ArrayList克隆的。

java.io.Serializable
java.io.Serializable是一个实现序列化的接口
具体细节请访问：
java.io.Serializable（序列化）接口详细总结

其次我们可以看到ArrayList继承于AbstractList类

现在我们看看ArrayList里面的一些成员属性：

```java
private static final long serialVersionUID = 8683452581122892189L;
```


serialVersionUID的作用：
序列化时为了保持版本的兼容性，即在版本升级时反序列化仍保持对象的唯一性。

serialVersionUID的作用：
序列化时为了保持版本的兼容性，即在版本升级时反序列化仍保持对象的唯一性。

```java
 private static final int DEFAULT_CAPACITY = 10;
```


DEFAULT_CAPACITY的作用
字面意思，默认容量大小为10.



```java
private int size;
```


数组大小

数组大小

```java
private static final Object[] EMPTY_ELEMENTDATA = {};
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
```


为了方便观察我将这两个空数组放到了一起。
相信大家也会有跟我一样的困惑，那就是我们虽然知道ArrayList的底层是一个数组，但是这里为什么会出现两个空数组，所以为了搞清楚这个情况。我们继续往下看构造方法。

构造方法

```java
 public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }
```


这段代码的注解是构造具有指定初始容量的空列表。
通过if语句我们可以看到：
该方法传进来一个参数，如果该参数的值大于0则：

这段代码的注解是构造具有指定初始容量的空列表。
通过if语句我们可以看到：
该方法传进来一个参数，如果该参数的值大于0则：

```java
List<Integer> list = new ArrayList<>(1);
this.elementData = new Object[initialCapacity];
```

也就是：

```java
transient Object[] elementData = new Object[initialCapacity];
```

可以看出new了一个Object的数组 数组大小为传进来的值。

可以看出new了一个Object的数组 数组大小为传进来的值。

如果该参数等于0则：

```java
List<Integer> list = new ArrayList<>(0);
this.elementData = EMPTY_ELEMENTDATA;
```

也就是：

```java
transient Object[] elementData = EMPTY_ELEMENTDATA;
```

可以看出如果传进来一个0则底层数组会等于这个短的空数组。

可以看出如果传进来一个0则底层数组会等于这个短的空数组。

否则（也就是传进来的参数小于0）：

```java
List<Integer> list = new ArrayList<>(-1);
```

抛出异常：IllegalArgumentException("Illegal Capacity: "+
initialCapacity）

```java
public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
```


可以看出，如果我们不给ArrayList传参数的话其底层数组会等于长的空数组。

看到这里就突然奇怪了起来！为什么要用两个不同的空数组来区别呢？ 为什么在不传参数的时候不让elementData等于短的空数组呢？？ 为了看清这一点，我决定在底下的代码中找找这两个变量。

ensureCapacity方法

```java
public void ensureCapacity(int minCapacity) {
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA) ? 0 : DEFAULT_CAPACITY;

    if (minCapacity > minExpand) {
       ensureExplicitCapacity(minCapacity);
   }
}
```


首先我在ensureCapacity方法中找到了长的空数组。
在这里可以通过三目运算符发现。如果在new ArrayList时不传参数，则将minExpand的大小设置为10，否则设置为0.
最后通过if语句，如果传进来的数（minCapacity）大于minExpand则调入 ensureExplicitCapacity(minCapacity);函数中。

但是在这个方法中虽然找到较长的空数组，但是发现似乎并没有用到呀…只是做了一个工具人，所以我们继续看下一个方法：

calculateCapacity方法

```java
 private static int calculateCapacity(Object[] elementData, int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        return minCapacity;
    }
```


通过简单的if语句我们能很轻易的看出：
如果new ArrayList的时候没有传参数 则返回DEFAULT_CAPACITY（默认容量：10）与接受到的参数 minCapacity中的最大值。
否则：
返回minCapacity。

通过简单的if语句我们能很轻易的看出：
如果new ArrayList的时候没有传参数 则返回DEFAULT_CAPACITY（默认容量：10）与接受到的参数 minCapacity中的最大值。
否则：
返回minCapacity。

这个方法似乎有点用处了，那么我们就要查看一下是哪位调用了这个方法。他究竟做了些什么：

ensureCapacityInternal方法

```java
private void ensureCapacityInternal(int minCapacity) {
        ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
    }
```


这个方法可能一下给人就看傻了，别急 我给大家写成易懂的写法：

这个方法可能一下给人就看傻了，别急 我给大家写成易懂的写法：

易懂版ensureCapacityInternal方法

```java
private void ensureCapacityInternal(int minCapacity) {
        int a = calculateCapacity(elementData, minCapacity);
        ensureExplicitCapacity(a);
    }
```


这不就好起来了吗。我们不难发现这里把上一个方法所返回的值又传给了ensureExplicitCapacity方法。
接下来我来再来看看ensureExplicitCapacity方法又做了些什么：

这不就好起来了吗。我们不难发现这里把上一个方法所返回的值又传给了ensureExplicitCapacity方法。
接下来我来再来看看ensureExplicitCapacity方法又做了些什么：

ensureExplicitCapacity方法

```java
 private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}
```


modCount是什么？ 在成员属性中并没有找到这个东西。那他为什么会出现在这里？
真相只有一个：去他的（ArrayList）的父类找找看。

果不其然在AbstractList.java中找到了modCount.

```java
protected transient int modCount = 0;
```

通过阅读该变量的注释我们可以发现modCount是用来记录ArrayList被修改的次数的。

接下来看下一条if语句：

```java
if (minCapacity - elementData.length > 0)
            grow(minCapacity);
```


如果传进来的这个参数minCapacity大于elementData数组的长度则调用 grow(minCapacity);

grow方法

```java
private void grow(int minCapacity) {

    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);

    elementData = Arrays.copyOf(elementData, newCapacity);
}
```


这个方法总算是到头了我们来康康这个方法解决了什么问题！
首先将底层数组的长度存储于oldCapacity中，

然后新建一个newCapacity其长度为：oldCapacity + (oldCapacity >> 1);
也就是 oldCapacity + (oldCapacity / 2);
将原来的容量扩大了0.5倍。

接下来进行判断：
如果扩容后的newCapacity小于传进来的参数minCapacity则：
newCapacity = minCapacity;
将minCapacity的值赋值于newCapacity中：

如果newCapacity的值大于MAX_ARRAY_SIZE：
MAX_ARRAY_SIZE（ArrayList的最大长度）：

```java
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
```


则：

```java
newCapacity = hugeCapacity(minCapacity);
```

则：
newCapacity = hugeCapacity(minCapacity);

哦吼 那我们再来看看胡歌方法hugeCapacity。

hugeCapacity方法

```java
 private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) 
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
```


这就明白了
如果形参minCapacity小于0，数组越界异常
否则 若 minCapacity > MAX_ARRAY_SIZE
则返回Integer.MAX_VALUE。也是int的最大值。
否则返回常量MAX_ARRAY_SIZE。

这就明白了
如果形参minCapacity小于0，数组越界异常
否则 若 minCapacity > MAX_ARRAY_SIZE
则返回Integer.MAX_VALUE。也是int的最大值。
否则返回常量MAX_ARRAY_SIZE。

好的继续回到grow方法中：

```java
if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
```


经过if语句判断可以看出这个胡歌方法（hugeCapacity）最终的返回值应该为Integer.MAX_VALUE。

好的走出if语句就剩下最后一条了！！！

数组扩容

```java
 elementData = Arrays.copyOf(elementData, newCapacity);
```


这一条就很明了了 复制一个新的数组,数据还是原来的数据 数组大小变成扩容后的大小 然后将复制出来的数组的覆盖到底层数组中，从而实现ArrayList的扩容。

这一条就很明了了 复制一个新的数组,数据还是原来的数据 数组大小变成扩容后的大小 然后将复制出来的数组的覆盖到底层数组中，从而实现ArrayList的扩容。

呼！是不是晕了！！

那我们将所有方法写入main函数中做一个总结也许就会简单明了！！！
只考虑new ArrayList<>(); 的情况在main函数中实现

```java
public class TestDemo{
    //初始容量
    private static final int DEFAULT_CAPACITY = 10;
    //较长空数组
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    //底层数组
    public static Object[] elementData;
    //神秘参数
    public  static int minCapacity;
    //ArrayList的最大长度
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

public static void main(String[] args) {

    // ArrayList<Integer> list = new ArratList<>(); 不在ArrayList中导入参数
    elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;

    //calculateCapacity 方法 将默认值10传入minCapacity中
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {

    minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
}
    //ensureCapacityInternal 方法 由于该方法只是调用了另一个方法所以只以注释的方式呈现
    //ensureExplicitCapacity(minCapacity);
    //hugeCapacity 胡歌方法
    int Huge;
       if (minCapacity < 0) {
           throw new OutOfMemoryError();
       }else {
           Huge = (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
           
       }

    //ensureExplicitCapacity 方法 if语句内为 grow 方法  ps:由于modCount++; 是用于记录操作次数 所以暂不呈现

    if (minCapacity - elementData.length > 0){        
    //grow方法 该方法用于数组扩容。
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = Huge;
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
}

}  


```

其实已经很容易看出来了当我们不给ArrayList传参数（容量）时calculateCapacity方法会将默认容量DEFAULT_CAPACITY：（10）作为底层数组elementData的初始容量。然而如何判断ArrayList是否传参数呢？就是通过较长空数组来得知的。

那么较短的空数组又是怎么回事呢？
其实我们已经应该猜到一些了，当我们将ArrayList的长度设置为0.

```java
ArrayList list = new Arraylist(0);
```

那不就是为了整了一个空的ArrayList也就是空数组嘛？
可是为什么同样是空数组却要如此分开来呢？
答案在calculateCapacity方法里。
calculateCapacity方法中有一个if语句：

那不就是为了整了一个空的ArrayList也就是空数组嘛？
可是为什么同样是空数组却要如此分开来呢？
答案在calculateCapacity方法里。
calculateCapacity方法中有一个if语句：

```java
if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        }
```


对就是这个这个if语句！！
如果将这两个（new Arraylist(0);或new Arraylist();）表现在一个空数组中，那系统就裂开了（开玩笑）。
试想一下 我本来想整一个容量为0的 ArrayList结果你这一操作 给我把容量 嘎 变成10了。或者说我本来想弄个默认的容量大小结果你一下给我们把容量弄成0了。所以咋想都不合适吧！

对就是这个这个if语句！！
如果将这两个（new Arraylist(0);或new Arraylist();）表现在一个空数组中，那系统就裂开了（开玩笑）。
试想一下 我本来想整一个容量为0的 ArrayList结果你这一操作 给我把容量 嘎 变成10了。或者说我本来想弄个默认的容量大小结果你一下给我们把容量弄成0了。所以咋想都不合适吧！

然而我们是如何将new Arraylist(0);或是new Arraylist();区分开的呢？
那就要看看我们最开始看到的“工具人方法”了：

ensureCapacity方法

```java
public void ensureCapacity(int minCapacity) {
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA) ? 0 : DEFAULT_CAPACITY;
		if (minCapacity > minExpand) {
			ensureExplicitCapacity(minCapacity);
	}
}
```


可以看出如果咱的elementData等于较长空数组则不会直接调用ensureExplicitCapacity方法了

ensureExplicitCapacity方法

```java
 private void ensureExplicitCapacity(int minCapacity) {
        modCount++;
        if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}
```


否则直接就进入了ensureExplicitCapacity方法后进行比较然后扩容

这时候突然恍然大悟：
最开始说的较长空数组在这个方法（ensureCapacity方法）中当工具人原来并不严谨，因为此方法正是区分new ArrayList时候是否传值 如果没传值则底层数组等于较长空数组，然后将较长空数组在该方法中被筛选出来 通过一系列手段将这个未传参的ArrayList的默认容量设置为10！

总结：
1.DEFAULTCAPACITY_EMPTY_ELEMENTDATA 该数组为空数组，用来记录ArrayList在生成时没有进行传参数，随后在操作过程中将elementData（底层数组）的大小设置为默认值10。

2.EMPTY_ELEMENTDATA用来记录ArrayList在生成时传参为0的情况。而为了与没传参数的情况分开所以重新用了一个空数组。

3.ArrayList扩容是通过grow方法实现的且每次扩充的容量为elementData当前长度的一半。

原文链接：https://blog.csdn.net/GUOCHUC/article/details/107324793



#### 分析 ArrayList 扩容机制

#### 先从 ArrayList 的构造函数说起

**（JDK8）ArrayList 有三种方式来初始化，构造方法源码如下：**

```java
   /**
     * 默认初始容量大小
     */
    private static final int DEFAULT_CAPACITY = 10;


    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     *默认构造函数，使用初始容量10构造一个空列表(无参数构造)
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    /**
     * 带初始容量参数的构造函数。（用户自己指定容量）
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {//初始容量大于0
            //创建initialCapacity大小的数组
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {//初始容量等于0
            //创建空数组
            this.elementData = EMPTY_ELEMENTDATA;
        } else {//初始容量小于0，抛出异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }


   /**
    *构造包含指定collection元素的列表，这些元素利用该集合的迭代器按顺序返回
    *如果指定的集合为null，throws NullPointerException。
    */
     public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // replace with empty array.
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }
```

细心的同学一定会发现 ：**以无参数构造方法创建 ArrayList 时，实际上初始化赋值的是一个空数组。当真正对数组进行添加元素操作时，才真正分配容量。即向数组中添加第一个元素时，数组容量扩为 10。** 下面在我们分析 ArrayList 扩容时会讲到这一点内容！

> 补充：JDK7 new无参构造的ArrayList对象时，直接创建了长度是10的Object[]数组elementData 。jdk7中的ArrayList的对象的创建**类似于单例的饿汉式**，而jdk8中的ArrayList的对象的创建**类似于单例的懒汉式**。JDK8的内存优化也值得我们在平时开发中学习。

####  一步一步分析 ArrayList 扩容机制

这里以无参构造函数创建的 ArrayList 为例分析

 先来看 `add` 方法

```
    /**
     * 将指定的元素追加到此列表的末尾。
     */
    public boolean add(E e) {
   //添加元素之前，先调用ensureCapacityInternal方法
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        //这里看到ArrayList添加元素的实质就相当于为数组赋值
        elementData[size++] = e;
        return true;
    }
```

> **注意** ：JDK11 移除了 `ensureCapacityInternal()` 和 `ensureExplicitCapacity()` 方法

再来看看 `ensureCapacityInternal()` 方法

（JDK7）可以看到 `add` 方法 首先调用了`ensureCapacityInternal(size + 1)`

```
   //得到最小扩容量
    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
              // 获取默认的容量和传入参数的较大值
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }
```

**当 要 add 进第 1 个元素时，minCapacity 为 1，在 Math.max()方法比较后，minCapacity 为 10。**

> 此处和后续 JDK8 代码格式化略有不同，核心代码基本一样。

 `ensureExplicitCapacity()` 方法

如果调用 `ensureCapacityInternal()` 方法就一定会进入（执行）这个方法，下面我们来研究一下这个方法的源码！

```
  //判断是否需要扩容
    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            //调用grow方法进行扩容，调用此方法代表已经开始扩容了
            grow(minCapacity);
    }
```

我们来仔细分析一下：

- 当我们要 add 进第 1 个元素到 ArrayList 时，elementData.length 为 0 （因为还是一个空的 list），因为执行了 `ensureCapacityInternal()` 方法 ，所以 minCapacity 此时为 10。此时，`minCapacity - elementData.length > 0`成立，所以会进入 `grow(minCapacity)` 方法。
- 当 add 第 2 个元素时，minCapacity 为 2，此时 e lementData.length(容量)在添加第一个元素后扩容成 10 了。此时，`minCapacity - elementData.length > 0` 不成立，所以不会进入 （执行）`grow(minCapacity)` 方法。
- 添加第 3、4···到第 10 个元素时，依然不会执行 grow 方法，数组容量都为 10。

直到添加第 11 个元素，minCapacity(为 11)比 elementData.length（为 10）要大。进入 grow 方法进行扩容。

`grow()` 方法

```
    /**
     * 要分配的最大数组大小
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * ArrayList扩容的核心方法。
     */
    private void grow(int minCapacity) {
        // oldCapacity为旧容量，newCapacity为新容量
        int oldCapacity = elementData.length;
        //将oldCapacity 右移一位，其效果相当于oldCapacity /2，
        //我们知道位运算的速度远远快于整除运算，整句运算式的结果就是将新容量更新为旧容量的1.5倍，
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        //然后检查新容量是否大于最小需要容量，若还是小于最小需要容量，那么就把最小需要容量当作数组的新容量，
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
       // 如果新容量大于 MAX_ARRAY_SIZE,进入(执行) `hugeCapacity()` 方法来比较 minCapacity 和 MAX_ARRAY_SIZE，
       //如果minCapacity大于最大容量，则新容量则为`Integer.MAX_VALUE`，否则，新容量大小则为 MAX_ARRAY_SIZE 即为 `Integer.MAX_VALUE - 8`。
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
```

**int newCapacity = oldCapacity + (oldCapacity >> 1),所以 ArrayList 每次扩容之后容量都会变为原来的 1.5 倍左右（oldCapacity 为偶数就是 1.5 倍，否则是 1.5 倍左右）！** 奇偶不同，比如 ：10+10/2 = 15, 33+33/2=49。如果是奇数的话会丢掉小数.

> ">>"（移位运算符）：>>1 右移一位相当于除 2，右移 n 位相当于除以 2 的 n 次方。这里 oldCapacity 明显右移了 1 位所以相当于 oldCapacity /2。对于大数据的 2 进制运算,位移运算符比那些普通运算符的运算要快很多,因为程序仅仅移动一下而已,不去计算,这样提高了效率,节省了资源

**我们再来通过例子探究一下`grow()` 方法 ：**

- 当 add 第 1 个元素时，oldCapacity 为 0，经比较后第一个 if 判断成立，newCapacity = minCapacity(为 10)。但是第二个 if 判断不会成立，即 newCapacity 不比 MAX_ARRAY_SIZE 大，则不会进入 `hugeCapacity` 方法。数组容量为 10，add 方法中 return true,size 增为 1。
- 当 add 第 11 个元素进入 grow 方法时，newCapacity 为 15，比 minCapacity（为 11）大，第一个 if 判断不成立。新容量没有大于数组最大 size，不会进入 hugeCapacity 方法。数组容量扩为 15，add 方法中 return true,size 增为 11。
- 以此类推······

**这里补充一点比较重要，但是容易被忽视掉的知识点：**

- java 中的 `length`属性是针对数组说的,比如说你声明了一个数组,想知道这个数组的长度则用到了 length 这个属性.
- java 中的 `length()` 方法是针对字符串说的,如果想看这个字符串的长度则用到 `length()` 这个方法.
- java 中的 `size()` 方法是针对泛型集合说的,如果想看这个泛型有多少个元素,就调用此方法来查看!

`hugeCapacity()` 方法。

从上面 `grow()` 方法源码我们知道： 如果新容量大于 MAX_ARRAY_SIZE,进入(执行) `hugeCapacity()` 方法来比较 minCapacity 和 MAX_ARRAY_SIZE，如果 minCapacity 大于最大容量，则新容量则为`Integer.MAX_VALUE`，否则，新容量大小则为 MAX_ARRAY_SIZE 即为 `Integer.MAX_VALUE - 8`。

```
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        //对minCapacity和MAX_ARRAY_SIZE进行比较
        //若minCapacity大，将Integer.MAX_VALUE作为新数组的大小
        //若MAX_ARRAY_SIZE大，将MAX_ARRAY_SIZE作为新数组的大小
        //MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
```





### HashMap

HashMap 主要用来存放键值对，它基于哈希表的 Map 接口实现，是常用的 Java 集合之一。

JDK1.8 之前 HashMap 由 数组+链表 组成的，数组是 HashMap 的主体，链表则是主要为了解决哈希冲突而存在的（“拉链法”解决冲突）。

JDK1.8 之后 HashMap 的组成多了红黑树，在满足下面两个条件之后，会执行链表转红黑树操作，以此来加快搜索速度。

- 链表长度大于阈值（默认为 8）
- HashMap 数组长度超过 64



#### 红黑树

红黑树是一种自平衡的二叉查找树，是一种高效的查找树。红黑树具有良好的效率，它可在 O(logN) 时间内完成查找、增加、删除等操作。因此，红黑树在业界应用很广泛，比如 Java 中的 TreeMap，JDK 1.8 中的 HashMap均是基于红黑树结构实现的。

红黑树通过如下的性质定义实现自平衡：

> 节点是红色或黑色。
> 根是黑色。
> 所有叶子都是黑色（叶子是NIL节点）。
> 每个红色节点必须有两个黑色的子节点。（从每个叶子到根的所有路径上不能有两个连续的红色节点。）
> 从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点（简称黑高）。





#### 散列表

散列表(哈希表)，其思想主要是基于数组支持按照下标随机访问数据时间复杂度为O(1)的特性。可是说是数组的一种扩展。

**散列函数**

上面的例子中，截取学号后四位的函数即是一个简单的散列函数。

在这里散列函数的作用就是讲key值映射成数组的索引下标。关于散列函数的设计方法有很多，如:直接寻址法、数字分析法、随机数法等等。但即使是再优秀的设计方法也不能避免散列冲突。在散列表中散列函数不应设计太复杂。

散列函数具有确定性和不确定性。

- 确定性:哈希的散列值不同，那么哈希的原始输入也就不同。即:key1=key2,那么hash(key1)=hash(key2)。
- 不确定性:同一个散列值很有可能对应多个不同的原始输入。即:key1≠key2，hash(key1)=hash(key2)。

散列冲突,即key1≠key2，hash(key1)=hash(key2)的情况。散列冲突是不可避免的，如果我们key的长度为100，而数组的索引数量只有50，那么再优秀的算法也无法避免散列冲突。关于散列冲突也有很多解决办法，这里简单复习两种：开放寻址法和链表法。

**1 开放寻址法**

开放寻址法的核心思想是，如果出现了散列冲突，我们就重新探测一一个空闲位置，将其插入。比如，我们可以使用线性探测法。当我们往散列表中插入数据时，如果某个数据经过散列函数散列之后，存储位置已经被占用了，我们就从当前位置开始，依次往后查找，看是否有空闲位置，如果遍历到尾部都没有找到空闲的位置，那么我们就再从表头开始找，直到找到为止。

![img](MarkDown_Java%20SE.assets/v2-7ed00efd4a2e417639f32c0152a497eb_1440w.jpg)

散列表中查找元素的时候，我们通过散列函数求出要查找元素的键值对应的散列值，然后比较数组中下标为散列值的元素和要查找的元素。如果相等，则说明就是我们要找的元素；否则就顺序往后依次查找。如果遍历到数组中的空闲位置还没有找到，就说明要查找的元素并没有在散列表中。

对于删除操作稍微有些特别，不能单纯地把要删除的元素设置为空。因为在查找的时候，一旦我们通过线性探测方法，找到一个空闲位置，我们就可以认定散列表中不存在这个数据。但是，如果这个空闲位置是我们后来删除的，就会导致原来的查找算法失效。这里我们可以将删除的元素，特殊标记为 deleted。当线性探测查找的时候，遇到标记为 deleted 的空间，并不是停下来，而是继续往下探测。

线性探测法存在很大问题。当散列表中插入的数据越来越多时，其散列冲突的可能性就越大，极端情况下甚至要探测整个散列表,因此最坏时间复杂度为O(N)。在开放寻址法中，除了线性探测法，我们还可以二次探测和双重散列等方式。



**2 链表法(拉链法)**

简单来讲就是在冲突的位置拉一条链表来存储数据。

链表法是一种比较常用的散列冲突解决办法,Redis使用的就是链表法来解决散列冲突。链表法的原理是:如果遇到冲突，他就会在原地址新建一个空间，然后以链表结点的形式插入到该空间。当插入的时候，我们只需要通过散列函数计算出对应的散列槽位，将其插入到对应链表中即可。

![img](MarkDown_Java%20SE.assets/v2-a7f9de70b35ecc361c2474c6c22d0a61_1440w.jpg)

 **开放寻址法与链表法比较**

对于开放寻址法解决冲突的散列表，由于数据都存储在数组中，因此可以有效地利用 CPU 缓存加快查询速度(数组占用一块连续的空间)。但是删除数据的时候比较麻烦，需要特殊标记已经删除掉的数据。而且，在开放寻址法中，所有的数据都存储在一个数组中，比起链表法来说，冲突的代价更高。所以，使用开放寻址法解决冲突的散列表，负载因子的上限不能太大。这也导致这种方法比链表法更浪费内存空间。

对于链表法解决冲突的散列表,对内存的利用率比开放寻址法要高。因为链表结点可以在需要的时候再创建，并不需要像开放寻址法那样事先申请好。链表法比起开放寻址法，对大装载因子的容忍度更高。开放寻址法只能适用装载因子小于1的情况。接近1时，就可能会有大量的散列冲突，性能会下降很多。但是对于链表法来说，只要散列函数的值随机均匀，即便装载因子变成10，也就是链表的长度变长了而已，虽然查找效率有所下降，但是比起顺序查找还是快很多。但是，链表因为要存储指针，所以对于比较小的对象的存储，是比较消耗内存的，而且链表中的结点是零散分布在内存中的，不是连续的，所以对CPU缓存是不友好的，这对于执行效率有一定的影响。















#### 底层数据结构分析

#### JDK1.8 之前

JDK1.8 之前 HashMap 底层是 **数组和链表** 结合在一起使用也就是 **链表散列**。

HashMap 通过 key 的 hashCode 经过扰动函数处理过后得到 hash 值，然后通过 `(n - 1) & hash` 判断当前元素存放的位置（这里的 n 指的是数组的长度），如果当前位置存在元素的话，就判断该元素与要存入的元素的 hash 值以及 key 是否相同，如果相同的话，直接覆盖，不相同就通过拉链法解决冲突。

所谓扰动函数指的就是 HashMap 的 hash 方法。使用 hash 方法也就是扰动函数是为了防止一些实现比较差的 hashCode() 方法 换句话说使用扰动函数之后可以减少碰撞。

#### **JDK 1.8 HashMap 的 hash 方法源码:**

JDK 1.8 的 hash 方法 相比于 JDK 1.7 hash 方法更加简化，但是原理不变。

```
    static final int hash(Object key) {
      int h;
      // key.hashCode()：返回散列值也就是hashcode
      // ^ ：按位异或
      // >>>:无符号右移，忽略符号位，空位都以0补齐
      return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
  }
```

对比一下 JDK1.7 的 HashMap 的 hash 方法源码.

```
static int hash(int h) {
    // This function ensures that hashCodes that differ only by
    // constant multiples at each bit position have a bounded
    // number of collisions (approximately 8 at default load factor).

    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}
```

相比于 JDK1.8 的 hash 方法 ，JDK 1.7 的 hash 方法的性能会稍差一点点，因为毕竟扰动了 4 次。

#### HashMap中hash(Object key)的原理，为什么这样做?

1. h >>> 16 是什么，有什么用?
h是hashcode。h >>> 16是用来取出h的高16，(>>>是无符号右移)   如下展示：

```java
0000 0100 1011 0011  1101 1111 1110 0001

>>> 16 

0000 0000 0000 0000  0000 0100 1011 0011
```


2. 为什么 h = key.hashCode()) 与 (h >>> 16) 异或
讲到这里还要看一个方法indexFor，在jdk1.7中有indexFor(int h, int length)方法。jdk1.8里没有，但原理没变。下面看下1.7源码

1.8中用tab[(n - 1) & hash]代替但原理一样。

```java
static int indexFor(int h, int length) {
    return h & (length-1);
}
```


这个方法返回值就是数组下标。我们平时用map大多数情况下map里面的数据不是很多。这里与（length-1）相&,

但由于绝大多数情况下length一般都小于2^16即小于65536。所以return h & (length-1);结果始终是h的低16位与（length-1）进行&运算。
3. 原因总结
由于和（length-1）运算，length 绝大多数情况小于2的16次方。所以始终是hashcode 的低16位（甚至更低）参与运算。要是高16位也参与运算，会让得到的下标更加散列。

所以这样高16位是用不到的，如何让高16也参与运算呢。所以才有hash(Object key)方法。让他的hashCode()和自己的高16位^运算。所以(h >>> 16)得到他的高16位与hashCode()进行^运算。

4. 为什么用^而不用&和|
因为&和|都会使得结果偏向0或者1 ,并不是均匀的概念,所以用^。

这就是为什么有hash(Object key)的原因。



#### JDK1.8 之后

相比于之前的版本，JDK1.8 以后在解决哈希冲突时有了较大的变化。

当链表长度大于阈值（默认为 8）时，会首先调用 `treeifyBin()`方法。这个方法会根据 HashMap 数组来决定是否转换为红黑树。只有当数组长度大于或者等于 64 的情况下，才会执行转换红黑树操作，以减少搜索时间。否则，就是只是执行 `resize()` 方法对数组扩容。相关源码这里就不贴了，重点关注 `treeifyBin()`方法即可！

![img](MarkDown_Java%20SE.assets/68747470733a2f2f6f7363696d672e6f736368696e612e6e65742f6f73636e65742f75702d62626132383332323836393364616537346537386461316566376139613034633638342e706e67)

**类的属性：**

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
    // 序列号
    private static final long serialVersionUID = 362498820763181265L;
    // 默认的初始容量是16
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
    // 最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;
    // 默认的填充因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    // 当桶(bucket)上的结点数大于这个值时会转成红黑树
    static final int TREEIFY_THRESHOLD = 8;
    // 当桶(bucket)上的结点数小于这个值时树转链表
    static final int UNTREEIFY_THRESHOLD = 6;
    // 桶中结构转化为红黑树对应的table的最小大小
    static final int MIN_TREEIFY_CAPACITY = 64;
    // 存储元素的数组，总是2的幂次倍
    transient Node<k,v>[] table;
    // 存放具体元素的集
    transient Set<map.entry<k,v>> entrySet;
    // 存放元素的个数，注意这个不等于数组的长度。
    transient int size;
    // 每次扩容和更改map结构的计数器
    transient int modCount;
    // 临界值 当实际大小(容量*填充因子)超过临界值时，会进行扩容
    int threshold;
    // 加载因子
    final float loadFactor;
}
```

**loadFactor 加载因子**

loadFactor 加载因子是控制数组存放数据的疏密程度，loadFactor 越趋近于 1，那么 数组中存放的数据(entry)也就越多，也就越密，也就是会让链表的长度增加，loadFactor 越小，也就是趋近于 0，数组中存放的数据(entry)也就越少，也就越稀疏。

**loadFactor 太大导致查找元素效率低，太小导致数组的利用率低，存放的数据会很分散。loadFactor 的默认值为 0.75f 是官方给出的一个比较好的临界值**。

给定的默认容量为 16，负载因子为 0.75。Map 在使用过程中不断的往里面存放数据，当数量达到了 16 * 0.75 = 12 就需要将当前 16 的容量进行扩容，而扩容这个过程涉及到 rehash、复制数据等操作，所以非常消耗性能。

**threshold**

**threshold = capacity \* loadFactor**，**当 Size>=threshold**的时候，那么就要考虑对数组的扩增了，也就是说，这个的意思就是 **衡量数组是否需要扩增的一个标准**。



**Node 节点类源码:**

```java
// 继承自 Map.Entry<K,V>
static class Node<K,V> implements Map.Entry<K,V> {
       final int hash;// 哈希值，存放元素到hashmap中时用来与其他元素hash值比较
       final K key;//键
       V value;//值
       // 指向下一个节点
       Node<K,V> next;
       Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }
        // 重写hashCode()方法
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
        // 重写 equals() 方法
        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
}
```

**树节点类源码:**

```java
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // 父
        TreeNode<K,V> left;    // 左
        TreeNode<K,V> right;   // 右
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;           // 判断颜色
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }
        // 返回根节点
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
       }
```





#### 构造方法

HashMap 中有四个构造方法，它们分别如下：

```java
    // 默认构造函数。
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all   other fields defaulted
     }

     // 包含另一个“Map”的构造函数
     public HashMap(Map<? extends K, ? extends V> m) {
         this.loadFactor = DEFAULT_LOAD_FACTOR;
         putMapEntries(m, false);//下面会分析到这个方法
     }

     // 指定“容量大小”的构造函数
     public HashMap(int initialCapacity) {
         this(initialCapacity, DEFAULT_LOAD_FACTOR);
     }

     // 指定“容量大小”和“加载因子”的构造函数
     public HashMap(int initialCapacity, float loadFactor) {
         if (initialCapacity < 0)
             throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
         if (initialCapacity > MAXIMUM_CAPACITY)
             initialCapacity = MAXIMUM_CAPACITY;
         if (loadFactor <= 0 || Float.isNaN(loadFactor))
             throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
         this.loadFactor = loadFactor;
         this.threshold = tableSizeFor(initialCapacity);
     }
```



**putMapEntries 方法：**

```java
final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
    int s = m.size();
    if (s > 0) {
        // 判断table是否已经初始化
        if (table == null) { // pre-size
            // 未初始化，s为m的实际元素个数
            float ft = ((float)s / loadFactor) + 1.0F;
            int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                    (int)ft : MAXIMUM_CAPACITY);
            // 计算得到的t大于阈值，则初始化阈值
            if (t > threshold)
                threshold = tableSizeFor(t);
        }
        // 已初始化，并且m元素个数大于阈值，进行扩容处理
        else if (s > threshold)
            resize();
        // 将m中的所有元素添加至HashMap中
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            K key = e.getKey();
            V value = e.getValue();
            putVal(hash(key), key, value, false, evict);
        }
    }
}
```



#### put 方法

HashMap 只提供了 put 用于添加元素，putVal 方法只是给 put 方法调用的一个方法，并没有提供给用户使用。

**对 putVal 方法添加元素的分析如下：**

1. 如果定位到的数组位置没有元素 就直接插入。
2. 如果定位到的数组位置有元素就和要插入的 key 比较，如果 key 相同就直接覆盖，如果 key 不相同，就判断 p 是否是一个树节点，如果是就调用`e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value)`将元素添加进入。如果不是就遍历链表插入(插入的是链表尾部)。

![ ](MarkDown_Java%20SE.assets/68747470733a2f2f6d792d626c6f672d746f2d7573652e6f73732d636e2d6265696a696e672e616c6979756e63732e636f6d2f323031392d372f7075742545362539362542392545362542332539352e706e67)





```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // table未初始化或者长度为0，进行扩容
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // (n - 1) & hash 确定元素存放在哪个桶中，桶为空，新生成结点放入桶中(此时，这个结点是放在数组中)
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    // 桶中已经存在元素
    else {
        Node<K,V> e; K k;
        // 比较桶中第一个元素(数组中的结点)的hash值相等，key相等
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
                // 将第一个元素赋值给e，用e来记录
                e = p;
        // hash值不相等，即key不相等；为红黑树结点
        else if (p instanceof TreeNode)
            // 放入树中
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        // 为链表结点
        else {
            // 在链表最末插入结点
            for (int binCount = 0; ; ++binCount) {
                // 到达链表的尾部
                if ((e = p.next) == null) {
                    // 在尾部插入新结点
                    p.next = newNode(hash, key, value, null);
                    // 结点数量达到阈值(默认为 8 )，执行 treeifyBin 方法
                    // 这个方法会根据 HashMap 数组来决定是否转换为红黑树。
                    // 只有当数组长度大于或者等于 64 的情况下，才会执行转换红黑树操作，以减少搜索时间。否则，就是只是对数组扩容。
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    // 跳出循环
                    break;
                }
                // 判断链表中结点的key值与插入的元素的key值是否相等
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    // 相等，跳出循环
                    break;
                // 用于遍历桶中的链表，与前面的e = p.next组合，可以遍历链表
                p = e;
            }
        }
        // 表示在桶中找到key值、hash值与插入元素相等的结点
        if (e != null) {
            // 记录e的value
            V oldValue = e.value;
            // onlyIfAbsent为false或者旧值为null
            if (!onlyIfAbsent || oldValue == null)
                //用新值替换旧值
                e.value = value;
            // 访问后回调
            afterNodeAccess(e);
            // 返回旧值
            return oldValue;
        }
    }
    // 结构性修改
    ++modCount;
    // 实际大小大于阈值则扩容
    if (++size > threshold)
        resize();
    // 插入后回调
    afterNodeInsertion(evict);
    return null;
}
```



get 方法

```java
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        // 数组元素相等
        if (first.hash == hash && // always check first node
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        // 桶中不止一个节点
        if ((e = first.next) != null) {
            // 在树中get
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            // 在链表中get
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```





resize 方法

进行扩容，会伴随着一次重新 hash 分配，并且会遍历 hash 表中所有的元素，是非常耗时的。在编写程序中，要尽量避免 resize。

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        // 超过最大值就不再扩充了，就只好随你碰撞去吧
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 没超过最大值，就扩充为原来的2倍
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {
        // signifies using defaults
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    // 计算新的resize上限
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ? (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    if (oldTab != null) {
        // 把每个bucket都移动到新的buckets中
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else {
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        // 原索引
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        // 原索引+oldCap
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    // 原索引放到bucket里
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    // 原索引+oldCap放到bucket里
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```



### ConcurrentHashMap 1.8

#### 1. 存储结构

![Java8 ConcurrentHashMap 存储结构（图片来自 javadoop）](MarkDown_Java%20SE.assets/java8_concurrenthashmap.png)

可以发现 Java8 的 ConcurrentHashMap 相对于 Java7 来说变化比较大，不再是之前的 **Segment 数组 + HashEntry 数组 + 链表**，而是 **Node 数组 + 链表 / 红黑树**。当冲突链表达到一定长度时，链表会转换成红黑树。

#### 2. 初始化 initTable

```java
/**
 * Initializes table, using the size recorded in sizeCtl.
 */
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        ／／　如果 sizeCtl < 0 ,说明另外的线程执行CAS 成功，正在进行初始化。
        if ((sc = sizeCtl) < 0)
            // 让出 CPU 使用权
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```



从源码中可以发现 ConcurrentHashMap 的初始化是通过**自旋和 CAS** 操作完成的。里面需要注意的是变量 `sizeCtl` ，它的值决定着当前的初始化状态。

1. -1 说明正在初始化
2. -N 说明有N-1个线程正在进行扩容
3. 表示 table 初始化大小，如果 table 没有初始化
4. 表示 table 容量，如果 table　已经初始化。



#### 3. put

直接过一遍 put 源码。

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}

/** Implementation for put and putIfAbsent */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // key 和 value 不能为空
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        // f = 目标位置元素
        Node<K,V> f; int n, i, fh;// fh 后面存放目标位置的元素 hash 值
        if (tab == null || (n = tab.length) == 0)
            // 数组桶为空，初始化数组桶（自旋+CAS)
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            // 桶内为空，CAS 放入，不加锁，成功了就直接 break 跳出
            if (casTabAt(tab, i, null,new Node<K,V>(hash, key, value, null)))
                break;  // no lock when adding to empty bin
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            // 使用 synchronized 加锁加入节点
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 说明是链表
                    if (fh >= 0) {
                        binCount = 1;
                        // 循环加入新的或者覆盖节点
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        // 红黑树
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

1. 根据 key 计算出 hashcode 。
2. 判断是否需要进行初始化。
3. 即为当前 key 定位出的 Node，如果为空表示当前位置可以写入数据，利用 CAS 尝试写入，失败则自旋保证成功。
4. 如果当前位置的 `hashcode == MOVED == -1`,则需要进行扩容。
5. 如果都不满足，则利用 synchronized 锁写入数据。
6. 如果数量大于 `TREEIFY_THRESHOLD` 则要转换为红黑树。

#### 4. get

get 流程比较简单，直接过一遍源码。

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    // key 所在的 hash 位置
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        // 如果指定位置元素存在，头结点hash值相同
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                // key hash 值相等，key值相同，直接返回元素 value
                return e.val;
        }
        else if (eh < 0)
            // 头结点hash值小于0，说明正在扩容或者是红黑树，find查找
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            // 是链表，遍历查找
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

总结一下 get 过程：

1. 根据 hash 值计算位置。
2. 查找到指定位置，如果头节点就是要找的，直接返回它的 value.
3. 如果头节点 hash 值小于 0 ，说明正在扩容或者是红黑树，查找之。
4. 如果是链表，遍历查找之。

总结：

总的来说 ConcruuentHashMap 在 Java8 中相对于 Java7 来说变化还是挺大的

#### 总结

Java7 中 ConcruuentHashMap 使用的分段锁，也就是每一个 Segment 上同时只有一个线程可以操作，每一个 Segment 都是一个类似 HashMap 数组的结构，它可以扩容，它的冲突会转化为链表。但是 Segment 的个数一但初始化就不能改变。

Java8 中的 ConcruuentHashMap 使用的 Synchronized 锁加 CAS 的机制。结构也由 Java7 中的 **Segment 数组 + HashEntry 数组 + 链表** 进化成了 **Node 数组 + 链表 / 红黑树**，Node 是类似于一个 HashEntry 的结构。它的冲突再达到一定大小时会转化成红黑树，在冲突小于一定数量时又退回链表。

有些同学可能对 Synchronized 的性能存在疑问，其实 Synchronized 锁自从引入锁升级策略后，性能不再是问题，有兴趣的同学可以自己了解下 Synchronized 的**锁升级**。



## 常用方法

### == vs equals

![image-20210312225203351](MarkDown_Java%20SE.assets/image-20210312225203351.png)

作者：leeon
链接：https://www.zhihu.com/question/26872848/answer/34364603
来源：知乎
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。



Java 语言里的 equals方法其实是交给开发者去覆写的，让开发者**自己去定义**满足什么条件的两个Object是equal的。

所以我们不能单纯的**说equals到底比较的是什么。你想知道一个类的equals方法是什么意思就是要去看定义。**

Java中默认的 equals方法实现如下：

```text
public boolean equals(Object obj) {
    return (this == obj);
}
```

而String类则覆写了这个方法,直观的讲就是比较字符是不是都相同。

```java
public boolean equals(Object anObject) {
    if (this == anObject) {
        return true;
    }
    if (anObject instanceof String) {
        String anotherString = (String)anObject;
        int n = count;
        if (n == anotherString.count) {
            char v1[] = value;
            char v2[] = anotherString.value;
            int i = offset;
            int j = anotherString.offset;
            while (n-- != 0) {
                if (v1[i++] != v2[j++])
                    return false;
            }
            return true;
        }
    }
    return false;
}
```





### `System.arraycopy()` 和 `Arrays.copyOf()`方法

#### `System.arraycopy()` 方法

```java
public static void main(String[] args) {
    int[] a = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    System.arraycopy(a, 2, a, 3, 3);
    //源数组、元素中起始位置、目标数组、目标数组中起始位置、复制的长度
    for (int i = 0; i < a.length; i++) {
        System.out.print(a[i] + " ");
        //0 1 2 2 3 4 6 7 8 9
    }
}
```

#### `Arrays.copyOf()`方法

```java
int[] a = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
int[] b = Arrays.copyOf(a, 3);
for (int i = 0; i < b.length; i++) {
    System.out.print(b[i] + " ");
    //0 1 2 
}
```

####  两者联系和区别

**联系：**

看两者源代码可以发现 `copyOf()`内部实际调用了 `System.arraycopy()` 方法

**区别：**

`arraycopy()` 需要目标数组，将原数组拷贝到你自己定义的数组里或者原数组，而且可以选择拷贝的起点和长度以及放入新数组中的位置 `copyOf()` 是系统自动在内部新建一个数组，并返回该数组。

```java
/**
 * Copies the specified array, truncating or padding with zeros (if necessary)
 * so the copy has the specified length.  For all indices that are
 * valid in both the original array and the copy, the two arrays will
 * contain identical values.  For any indices that are valid in the
 * copy but not the original, the copy will contain {@code 0}.
 * Such indices will exist if and only if the specified length
 * is greater than that of the original array.
 *
 * @param original the array to be copied
 * @param newLength the length of the copy to be returned
 * @return a copy of the original array, truncated or padded with zeros
 *     to obtain the specified length
 * @throws NegativeArraySizeException if {@code newLength} is negative
 * @throws NullPointerException if {@code original} is null
 * @since 1.6
 */
public static int[] copyOf(int[] original, int newLength) {
    int[] copy = new int[newLength];
    System.arraycopy(original, 0, copy, 0,
                     Math.min(original.length, newLength));
    return copy;
}
```



## 关键字

### static

#### 

```java
//TODO:
```







### transient 

java 的transient关键字为我们提供了便利，你只需要实现Serilizable接口，将不需要序列化的属性前添加关键字transient，序列化对象的时候，这个属性就不会序列化到指定的目的地中。

```java
package Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Test_transient implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private transient String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        String path = "E:\\Java_project\\LeetcodeProj\\src\\Test" + File.separator + "testTransient.txt";
        File file = new File(path);
        Test_transient transientDemo = new Test_transient();
        transientDemo.setName("姓名");
        transientDemo.setPassword("密码");

        //将Test_transient类的transientDemo实例对象序列化，传入file
        ObjectOutput output = new ObjectOutputStream(new FileOutputStream(file));
        output.writeObject(transientDemo);

        //从file中读取被序列化的对象
        ObjectInput input = new ObjectInputStream(new FileInputStream(file));
        Test_transient demo = (Test_transient) input.readObject();

        //发现被transient修饰的成员变量不能被序列化
        System.out.println(demo.getName());
        System.out.println(demo.getPassword());
    }

}

```











### final 

**final关键字，意思是最终的、不可修改的，最见不得变化 ，用来修饰类、方法和变量，具有以下特点：**

1. **final修饰的类不能被继承，final类中的所有成员方法都会被隐式的指定为final方法；**
2. **final修饰的方法不能被重写；**
3. **final修饰的变量是常量，如果是基本数据类型的变量，则其数值一旦在初始化之后便不能更改；如果是引用类型的变量，则在对其初始化之后便不能让其指向另一个对象。**

说明：使用final方法的原因有两个。第一个原因是把方法锁定，以防任何继承类修改它的含义；第二个原因是效率。在早期的Java实现版本中，会将final方法转为内嵌调用。但是如果方法过于庞大，可能看不到内嵌调用带来的任何性能提升（现在的Java版本已经不需要使用final方法进行这些优化了）。类中所有的private方法都隐式地指定为final。







### assert

![image-20210401181834764](MarkDown_Java%20SE.assets/image-20210401181834764.png)

![image-20210401181843028](MarkDown_Java%20SE.assets/image-20210401181843028.png)

![image-20210401181857982](MarkDown_Java%20SE.assets/image-20210401181857982.png)

![image-20210401181916976](MarkDown_Java%20SE.assets/image-20210401181916976.png)



```java
package Test;

public class AssertTest {

    public static void main(String args[]) {
        //断言1结果为true，则继续往下执行
        assert true:"断言1失败，此表达式的信息将会在抛出异常的时候输出！";
        System.out.println("断言1没有问题，Go！");

        System.out.println("\n-----------------\n");

        //断言2结果为false,程序终止
        assert false : "断言2失败，此表达式的信息将会在抛出异常的时候输出！";
        System.out.println("断言2没有问题，Go！");
    }

}
```

```java
断言1没有问题，Go！

-----------------

Exception in thread "main" java.lang.AssertionError: 断言2失败，此表达式的信息将会在抛出异常的时候输出！
	at Test.AssertTest.main(AssertTest.java:13)
```

![image-20210401182009614](MarkDown_Java%20SE.assets/image-20210401182009614.png)

```java
断言1没有问题，Go！

-----------------

断言2没有问题，Go！

Process finished with exit code 0

```











### volatile

https://mp.weixin.qq.com/s?__biz=MzI4Njc5NjM1NQ%3D%3D&mid=2247495784&idx=1&sn=a3a0bbc1f9ff28103c6e687c9ac6a38a&scene=45#wechat_redirect

（1）多线程主要围绕可见性和原子性两个特性而展开，使用volatile关键字修饰的变量，保证了其在多线程之间的可见性，即每次读取到volatile变量，一定是最新的数据，volatile保证**可见性(Visibility)**和**有序性（Ordering）**，不能保证**原子性(Atomicity)**。

（2）代码底层执行不像我们看到的高级语言—-Java程序这么简单，它的执行是Java代码–>字节码–>根据字节码执行对应的C/C++代码–>C/C++代码被编译成汇编语言–>和硬件电路交互，现实中，为了获取更好的性能JVM可能会对指令进行重排序，多线程下可能会出现一些意想不到的问题。使用volatile则会对禁止语义重排序，当然这也一定程度上降低了代码执行效率

从实践角度而言，volatile的一个重要作用就是和CAS结合，保证了原子性，详细的可以参见java.util.concurrent.atomic包下的类，比如AtomicInteger。



小demo：永远都不会输出**有点东西**这一段代码，按道理线程改了flag变量，主线程也能访问到的呀？为会出现这个情况呢？

```java
package Test;

public class volatileTest {
    public static void main(String[] args) {
        flagClass flagClass = new flagClass();
        flagClass.start();
        for (; ; ) {
            if(flagClass.isFlag()){
                System.out.println("eh?");
            }
        }
    }
}

class flagClass extends Thread {
    private boolean flag = false;

    public boolean isFlag() {
        return flag;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        flag = true;
        System.out.println("成功更改flag");
    }
}
```

```java
成功更改flag

```

见JMM

#### **总结**

1. volatile修饰符适用于以下场景：某个属性被多个线程共享，其中有一个线程修改了此属性，其他线程可以立即得到修改后的值，比如booleanflag;或者作为触发器，实现轻量级同步。
2. volatile属性的读写操作都是无锁的，它不能替代synchronized，因为它没有提供原子性和互斥性。因为无锁，不需要花费时间在获取锁和释放锁_上，所以说它是低成本的。
3. volatile只能作用于属性，我们用volatile修饰属性，这样compilers就不会对这个属性做指令重排序。
4. volatile提供了可见性，任何一个线程对其的修改将立马对其他线程可见，volatile属性不会被线程缓存，始终从主 存中读取。
5. volatile提供了happens-before保证，对volatile变量v的写入happens-before所有其他线程后续对v的读操作。
6. volatile可以使得long和double的赋值是原子的。
7. volatile可以在单例双重检查中实现可见性和禁止指令重排序，从而保证安全性。





## **JMM**

`JMM`：Java内存模型，是java虚拟机规范中所定义的一种内存模型，描述了**Java程序中各种变量(线程共享变量)的访问规则**，以及在JVM中**将变量存储到内存和从内存中读取变量**这样的底层细节。

Java内存模型是标准化的，屏蔽掉了底层不同计算机的区别（`注意这个跟JVM完全不是一个东西，只有还有小伙伴搞错的`）。

Java内存模型(Java Memory Model，JMM)JMM主要是为了规定了**线程和内存之间**的一些关系。根据JMM的设计，系统存在一个主内存(Main Memory)，Java中所有变量都储存在**主存**中，对于所有线程都是共享的。每条线程都有自己的**工作内存**(Working Memory)，工作内存中保存的是主存中**某些变量的拷贝**，线程对所有变量的操作都是在工作内存中进行，线程之间无法相互直接访问，变量传递均需要通过主存完成。

![image-20210330220554976](MarkDown_Java%20SE.assets/image-20210330220554976.png)

### **jvm和jmm之间的关系**

jmm中的主内存、工作内存与jvm中的Java堆、栈、方法区等并不是同一个层次的内存划分，这两者基本上是没有关系的，如果两者一定要勉强对应起来，那从变量、主内存、工作内存的定义来看，主内存主要对应于Java堆中的对象实例数据部分，而工作内存则对应于虚拟机栈中的部分区域。从更低层次上说，主内存就直接对应于物理硬件的内存，而为了获取更好的运行速度，虚拟机（甚至是硬件系统本身的优化措施）可能会让工作内存优先存储于寄存器和高速缓存中，因为程序运行时主要访问读写的是工作内存。

### **现代计算机的内存模型**

![image-20210330221653312](MarkDown_Java%20SE.assets/image-20210330221653312.png)





### **JMM有以下规定：**

所有的共享变量都存储于主内存，这里所说的变量指的是实例变量和类变量，不包含局部变量，因为局部变量是线程私有的，因此不存在竞争问题。

每一个线程还存在自己的工作内存，线程的工作内存，保留了被线程使用的变量的工作副本。

`线程对变量的所有的操作(读，取)都必须在工作内存中完成，而不能直接读写主内存中的变量`。

不同线程之间也不能直接访问对方工作内存中的变量，线程间变量的值的传递需要通过主内存中转来完成。

### **本地内存和主内存的关系：**

![img](MarkDown_Java%20SE.assets/v2-f0364f6f863d5730e2b962ac6b3387e2_720w.jpg)

正是因为这样的机制，才导致了可见性问题的存在，那我们就讨论下可见性的解决方案。

### **可见性的解决方案**



#### **加锁**

```java
public static void main(String[] args) {
    flagClass flagClass = new flagClass();
    flagClass.start();
    for (; ; ) {
        synchronized (flagClass){
            if(flagClass.isFlag()){
                System.out.println("eh?");
            }
        }
    }
}
```

**为啥加锁可以解决可见性问题呢？**

因为某一个线程进入synchronized代码块前后，线程会获得锁，清空工作内存，从主内存拷贝共享变量最新的值到工作内存成为副本，执行代码，将修改后的副本的值刷新回主内存中，线程释放锁。

而获取不到锁的线程会阻塞等待，所以变量的值肯定一直都是最新的。



#### **Volatile修饰共享变量**

```java
package Test;

public class volatileTest {
    public static void main(String[] args) {
        flagClass flagClass = new flagClass();
        flagClass.start();
        for (; ; ) {
            if(flagClass.isFlag()){
                System.out.println("eh?");
            }
        }
    }
}

class flagClass extends Thread {
    private volatile boolean flag = false;

    public boolean isFlag() {
        return flag;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        flag = true;
        System.out.println("成功更改flag");
    }
}
```

##### **Volatile做了啥？**

每个线程操作数据的时候会把数据从主内存读取到自己的工作内存，如果他操作了数据并且写会了，他其他已经读取的线程的变量副本就会失效了，需要都数据进行操作又要再次去主内存中读取了。

volatile保证不同线程对共享变量操作的可见性，也就是说一个线程修改了volatile修饰的变量，当修改写回主内存时，另外一个线程立即看到最新的值。



##### **禁止指令重排序**

**什么是重排序?**

为了提高性能，编译器和处理器常常会对既定的代码执行顺序进行指令重排序。

**重排序的类型有哪些呢？源码到最终执行会经过哪些重排序呢？**

![img](MarkDown_Java%20SE.assets/v2-8ce58830ca5051bbb5deff4e9bc22599_720w.jpeg)

一个好的内存模型实际上会放松对处理器和编译器规则的束缚，也就是说软件技术和硬件技术都为同一个目标，而进行奋斗：在不改变程序执行结果的前提下，尽可能提高执行效率。

JMM对底层尽量减少约束，使其能够发挥自身优势。

因此，在执行程序时，为了提高性能，编译器和处理器常常会对指令进行重排序。

一般重排序可以分为如下三种：

- 编译器优化的重排序。编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序;
- 指令级并行的重排序。现代处理器采用了指令级并行技术来将多条指令重叠执行。如果不存在数据依赖性，处理器可以改变语句对应机器指令的执行顺序;
- 内存系统的重排序。由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行的。



##### **那Volatile是怎么保证不会被执行重排序的呢？**

**内存屏障**

java编译器会在生成指令系列时在适当的位置会插入`内存屏障`指令来禁止特定类型的处理器重排序。

为了实现volatile的内存语义，JMM会限制特定类型的编译器和处理器重排序，JMM会针对编译器制定volatile重排序规则表：

![preview](MarkDown_Java%20SE.assets/v2-63d3537f6b66726613bf30627c0dbd00_r.jpg)





**as-if-serial**

不管怎么重排序，单线程下的执行结果不能被改变。

编译器、runtime和处理器都必须遵守as-if-serial语义。





JMM具备一些先天的**有序性**,即不需要通过任何手段就可以保证的有序性，通常称为**happens-before**原则。`<<JSR-133：Java Memory Model and Thread Specification>>`定义了如下happens-before规则：

1. **程序顺序规则**： 一个线程中的每个操作，happens-before于该线程中的任意后续操作
2. **监视器锁规则**：对一个线程的解锁，happens-before于随后对这个线程的加锁
3. **volatile变量规则**： 对一个volatile域的写，happens-before于后续对这个volatile域的读
4. **传递性**：如果A happens-before B ,且 B happens-before C, 那么 A happens-before C
5. **start()规则**： 如果线程A执行操作`ThreadB_start()`(启动线程B) , 那么A线程的`ThreadB_start()`happens-before 于B中的任意操作
6. **join()原则**： 如果A执行`ThreadB.join()`并且成功返回，那么线程B中的任意操作happens-before于线程A从`ThreadB.join()`操作成功返回。
7. **interrupt()原则**： 对线程`interrupt()`方法的调用先行发生于被中断线程代码检测到中断事件的发生，可以通过`Thread.interrupted()`方法检测是否有中断发生
8. **finalize()原则**：一个对象的初始化完成先行发生于它的`finalize()`方法的开始



**当写一个volatile变量时，JMM会把该线程对应的本地内存中的共享变量刷新到主内存**

**当读一个volatile变量时，JMM会把该线程对应的本地内存置为无效，线程接下来将从主内存中读取共享变量。**



**面试官：说的还可以，那你知道volatile底层的实现机制？**

如果把加入volatile关键字的代码和未加入volatile关键字的代码都生成汇编代码，会发现加入volatile关键字的代码会多出一个lock前缀指令。

lock前缀指令实际相当于一个内存屏障，内存屏障提供了以下功能：

1 . 重排序时不能把后面的指令重排序到内存屏障之前的位置

2 . 使得本CPU的Cache写入内存

3 . 写入动作也会引起别的CPU或者别的内核无效化其Cache，相当于让新写入的值对别的线程可见。



volatile关键字通过 **“内存屏障”** 的方式来防止指令被重排序，为了实现volatile的内存语义，编译器在生成字节码时，会在指令序列中插入内存屏障来禁止特定类型的处理器重排序。大多数的处理器都支持内存屏障的指令。

对于编译器来说，发现一个最优布置来最小化插入屏障的总数几乎不可能，为此，Java内存模型采取保守策略。下面是基于保守策略的JMM内存屏障插入策略：

- 在每个volatile写操作的前面插入一个StoreStore屏障。
- 在每个volatile写操作的后面插入一个StoreLoad屏障。
- 在每个volatile读操作的后面插入一个LoadLoad屏障。
- 在每个volatile读操作的后面插入一个LoadStore屏障。

知识拓展：**内存屏障**：

内存屏障（Memory Barrier，或有时叫做内存栅栏，Memory Fence）是一种CPU指令，用于控制特定条件下的重排序和内存可见性问题。Java编译器也会根据内存屏障的规则禁止重排序。

内存屏障可以被分为以下几种类型：

- LoadLoad屏障：对于这样的语句Load1; LoadLoad; Load2，在Load2及后续读取操作要读取的数据被访问前，保证Load1要读取的数据被读取完毕。
- StoreStore屏障：对于这样的语句Store1; StoreStore; Store2，在Store2及后续写入操作执行前，保证Store1的写入操作对其它处理器可见。
- LoadStore屏障：对于这样的语句Load1; LoadStore; Store2，在Store2及后续写入操作被刷出前，保证Load1要读取的数据被读取完毕。
- StoreLoad屏障：对于这样的语句Store1; StoreLoad; Load2，在Load2及后续所有读取操作执行前，保证Store1的写入对所有处理器可见。它的开销是四种屏障中最大的。在大多数处理器的实现中，这个屏障是个万能屏障，兼具其它三种内存屏障的功能。













## 修饰符

**private、default（一般省略）、public、protected**

```java
//TODO:
```







## 泛型

Java 泛型（generics）是 JDK 5 中引入的一个新特性, 泛型提供了编译时类型安全检测机制，该机制允许程序员在编译时检测到非法的类型。泛型的本质是参数化类型，也就是说所操作的数据类型被指定为一个参数。

```java
        List<Integer> list = new ArrayList<>();
        list.add(12);
//        list.add("a");//这里直接添加会报错
        Class<? extends List> clazz = list.getClass();
        Method add = clazz.getDeclaredMethod("add", Object.class);
        add.invoke(list, "kl");//但是通过反射添加，是可以的
//        int i = Integer.parseInt(list.get(1));//require String provide Integer
//        int i = (int)list.get(1);//java.lang.ClassCastException: class java.lang.String cannot be cast to class java.lang.Integer

        System.out.println(list.get(1));
        System.out.println(list);
```

### 使用泛型的好处

​		在没有泛型的情况的下，通过对类型 Object 的引用来实现参数的“任意化”，“任意化”带来的缺点是要做显式的强制类型转换，而这种转换是要求开发者对实际参数类型可以预知的情况下进行的。对于强制类型转换错误的情况，编译器可能不提示错误，在运行的时候才出现异常，这是本身就是一个安全隐患。

​		那么泛型的好处就是在编译的时候能够检查类型安全，并且所有的强制转换都是自动和隐式的。

```java
public class TestGeneric<T> {
    private T t;

    public void set(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

    public static void main(String[] args) {
        // do nothing
    }

    /**
     * 不指定类型
     */
    public void noSpecifyType() {
        TestGeneric glmapperGeneric = new TestGeneric();
        glmapperGeneric.set("test");
        // 需要强制类型转换
        String test = (String) glmapperGeneric.get();
        System.out.println(test);
    }

    /**
     * 指定类型
     */
    public void specifyType() {
        TestGeneric<String> glmapperGeneric = new TestGeneric();
        glmapperGeneric.set("test");
        // 不需要强制类型转换
        String test = glmapperGeneric.get();
        System.out.println(test);
    }
}
```

### **泛型类**：

```java
//此处T可以随便写为任意标识，常见的如T、E、K、V等形式的参数常用于表示泛型
//在实例化泛型类时，必须指定T的具体类型
public class Generic<T>{

    private T key;

    public Generic(T key) {
        this.key = key;
    }

    public T getKey(){
        return key;
    }
}
```

如何实例化泛型类：

```java
Generic<Integer> genericInteger = new Generic<Integer>(123456);
```

### **泛型接口** ：

```java
public interface Generator<T> {
    public T method();
}
```

实现泛型接口，不指定类型：

```java
class GeneratorImpl<T> implements Generator<T>{
    @Override
    public T method() {
        return null;
    }
}
```

实现泛型接口，指定类型：

```java
class GeneratorImpl<T> implements Generator<String>{
    @Override
    public String method() {
        return "hello";
    }
}
```

### **泛型方法** ：

```java
public static <E> void printArray(E[] inputArray) {
        for (E element : inputArray) {
//            System.out.printf("%s ", element);
            System.out.print(element + " ");
        }
        System.out.println();
    }
```

使用：

```java
//        int[] intArray = {1, 2, 3};
//        printArray(intArray);  泛型要求包容的是对象类型，而基本数据类型在Java中不属于对象。

        Integer[] Integer = {1, 2, 3};
        printArray(Integer);

        String[] stringArray = {"Hello", "World"};
        printArray(stringArray);
```

### 通配符

**常用的通配符为： T，E，K，V，？**

- ？ 表示不确定的 java 类型
- T (type) 表示具体的一个 java 类型
- K V (key value) 分别代表 java 键值中的 Key Value
- E (element) 代表 Element

#### ？ **无界通配符**

我有一个父类 Animal 和几个子类，如狗、猫等，现在我需要一个动物的列表，我的第一个想法是像这样的：

```java
List<Animal> listAnimals
```

但是老板的想法确实这样的：

```java
List<? extends Animal> listAnimals
```

为什么要使用通配符而不是简单的泛型呢？通配符其实在声明局部变量时是没有什么意义的，但是当你为一个方法声明一个参数时，它是非常重要的。

```java
static int countLegs (List<? extends Animal > animals ) {
    int retVal = 0;
    for ( Animal animal : animals )
    {
        retVal += animal.countLegs();
    }
    return retVal;
}

static int countLegs1 (List< Animal > animals ){
    int retVal = 0;
    for ( Animal animal : animals )
    {
        retVal += animal.countLegs();
    }
    return retVal;
}

public static void main(String[] args) {
    List<Dog> dogs = new ArrayList<>();
 	// 不会报错
    countLegs( dogs );
	// 报错
    countLegs1(dogs);
}
```

当调用 countLegs1 时，就会飘红，提示的错误信息如下：

![img](MarkDown_Java%20SE.assets/16c9df5681ee06f4)

所以，对于不确定或者不关心实际要操作的类型，可以使用无限制通配符（尖括号里一个问号，即 <?> ），表示可以持有任何类型。像 countLegs 方法中，限定了上届，但是不关心具体类型是什么，所以对于传入的 Animal 的所有子类都可以支持，并且不会报错。而 countLegs1 就不行。

#### 下界通配符 < ? super E>

下界: 用 super 进行声明，表示参数化的类型可能是所指定的类型，或者是此类型的父类型，直至 Object

在类型参数中使用 super 表示这个泛型中的参数必须是 E 或者 E 的父类。

```java
private <T> void test(List<? super T> dst, List<T> src){
    for (T t : src) {
        dst.add(t);
    }
}

public static void main(String[] args) {
    List<Dog> dogs = new ArrayList<>();
    List<Animal> animals = new ArrayList<>();
    new Test3().test(animals,dogs);
}

// Dog 是 Animal 的子类
class Dog extends Animal {}
```

#### ？ 和 T 的区别

T 是一个 确定的 类型，通常用于泛型类和泛型方法的定义，？是一个 不确定 的类型，通常用于泛型方法的调用代码和形参，不能用于定义类和泛型方法。

##### 区别1：通过 T 来 确保 泛型参数的一致性

```java
// 通过 T 来 确保 泛型参数的一致性
public <T extends Number> void
test(List<T> dest, List<T> src) {
}

//通配符是 不确定的，所以这个方法不能保证两个 List 具有相同的元素类型
public void
test1(List<? extends Number> dest, List<? extends Number> src) {
}
```

```java
TestGeneric<String> TestGeneric = new TestGeneric<>();
List<String> dest = new ArrayList<>();
List<Number> src = new ArrayList<>();
TestGeneric.test(dest, src);//编译不通过
TestGeneric.test1(dest, src);//编译不通过
```

##### 区别2：类型参数可以多重限定而通配符不行

```java
interface MintefaceA {

}

interface MintefaceB {

}

public class TestGeneric<T> implements MintefaceA, MintefaceB {
    public <T extends MintefaceA & MintefaceB>
    void test(T t) {
        
    }

    public static void main(String[] args) {
        TestGeneric<String> TestGeneric = new TestGeneric<>();
        
    }
}
```

使用 & 符号设定多重边界（Multi Bounds)，指定泛型类型 T 必须是 MultiLimitInterfaceA 和 MultiLimitInterfaceB 的共有子类型，此时变量 t 就具有了所有限定的方法和属性。对于通配符来说，因为它不是一个确定的类型，所以不能进行多重限定。

##### 区别3：通配符可以使用超类限定而类型参数不行

类型参数 T 只具有 一种 类型限定方式：

```java
T extends A
T super A // 编译不通过，不被允许
```

但是通配符 ? 可以进行 两种限定：

```java
? extends A
? super A
```

##### `Class<T>` 和 `Class<?>` 区别

最常见的是在反射场景下的使用，这里以用一段反射的代码来说明下。

对于上述代码，在运行期，如果反射的类型不是 B 类，那么一定会报 java.lang.ClassCastException 错误。

```java
package Test;

import java.lang.reflect.InvocationTargetException;

class A {
}

class B {

}

public class TestGeneric<T> {
    public static <T> T createInstance(Class<T> tClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return tClass.getDeclaredConstructor().newInstance();
    }

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        // 通过反射的方式生成  multiLimit
        // 对象，这里比较明显的是，我们需要使用强制类型转换
        A a = (A) Class.forName("Test.A").getDeclaredConstructor().newInstance();
        B b = (B) Class.forName("Test.A").getDeclaredConstructor().newInstance();//ClassCastException: class Test.A cannot be cast to class Test.B (Test.A and Test.B are in unnamed module of loader 'app')


    }
}
```

对于上述代码，在运行期，如果反射的类型不是 MultiLimit 类，那么一定会报 java.lang.ClassCastException 错误。

```java
import java.lang.reflect.InvocationTargetException;

class A {
}

class B {

}

public class TestGeneric<T> {
    public static <T> T createInstance(Class<T> tClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return tClass.getDeclaredConstructor().newInstance();
    }

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        A a = createInstance(A.class);
        B b = createInstance(A.class); // 编译不通过 需要参数为B类型

    }
}
```

`Class<T>` 在实例化的时候，T 要替换成具体类。`Class<?>` 它是个通配泛型，? 可以代表任何类型，所以主要用于声明时的限制情况。比如，我们可以这样做申明：

```java
package Test;

public class TestGeneric {
    // 可以
    public Class<?> clazz;
    // 不可以，因为 T 需要指定类型
    public Class<T> clazzT;

    public static void main(String[] args) {
    }
}
```





## 反射

Java的反射（reflection）机制是指在程序的运行状态中，可以构造任意一个类的对象，可以了解任意一个对象所属的类，可以了解任意一个类的成员变量和方法，可以调用任意一个对象的属性和方法。这种动态获取程序信息以及动态调用对象的功能称为Java语言的反射机制。反射被视为动态语言的关键。

要正确使用Java反射机制就得使用java.lang.Class这个类。它是Java反射机制的起源。当一个类被加载以后，Java虚拟机就会自动产生一个Class对象。通过这个Class对象我们就能获得加载到虚拟机当中这个Class对象对应的方法、成员以及构造方法的声明和定义等信息。


### Java反射提供的功能.

1.在运行时判断任何一个对象所属的类

2.在运行时构造任何一个类的对象

3.在运行时判断任何一个类所具有的的成员变量和方法

4.在运行时调用任何一个对象的成员变量和方法

5.生成动态代理





### Class类

对于一个字节码文件.class，虽然表面上我们对该字节码文件一无所知，但该文件本身却记录了许多信息。Java在将.class字节码文件载入时，JVM将产生一个java.lang.Class对象代表该.class字节码文件，从该Class对象中可以获得类的许多基本信息，这就是反射机制。所以要想完成反射操作，就必须首先认识Class类。 [1] 

反射机制所需的类主要有java.lang包中的Class类和java.lang.reflect包中的Constructor类、Field类、Method类和Parameter类。Class类是一个比较特殊的类，它是反射机制的基础，Class类的对象表示正在运行的Java程序中的类或接口，也就是任何一个类被加载时，即将类的.class文件（字节码文件）读入内存的同时，都自动为之创建一个java.lang.Class对象。Class类没有公共构造方法，其对象是JVM在加载类时通过调用类加载器中的defineClass（）方法创建的，因此不能显式地创建一个Class对象。通过这个Class对象，才可以获得该对象的其他信息。

![img](MarkDown_Java%20SE.assets/78310a55b319ebc41be002ce8d26cffc1f1716c8)



每个类被加载之后，系统都会为该类生成一个对应的Class对象，通过Class对象就可以访问到JVM中该类的信息，一旦类被加载到JVM中，同一个类将不会被再次载入。被载入JVM的类都有一个唯一标识就是该类的全名，即包括包名和类名。

### 获取 Class 对象的四种方式

如果我们动态获取到这些信息，我们需要依靠 Class 对象。Class 类对象将一个类的方法、变量等信息告诉运行的程序。Java 提供了四种方式获取 Class 对象:

**1.知道具体类的情况下可以使用：**

```
Class alunbarClass = TargetObject.class;
```

但是我们一般是不知道具体类的，基本都是通过遍历包下面的类来获取 Class 对象，通过此方式获取 Class 对象不会进行初始化

**2.通过 `Class.forName()`传入类的路径获取：**

```
Class alunbarClass1 = Class.forName("cn.javaguide.TargetObject");
```

**3.通过对象实例`instance.getClass()`获取：**

```
TargetObject o = new TargetObject();
Class alunbarClass2 = o.getClass();
```

**4.通过类加载器`xxxClassLoader.loadClass()`传入类路径获取:**

```
class clazz = ClassLoader.LoadClass("cn.javaguide.TargetObject");
```

通过类加载器获取 Class 对象不会进行初始化，意味着不进行包括初始化等一些列步骤，静态块和静态对象不会得到执行







### **获取类修饰符**

```java
package Reflection;

import java.lang.reflect.Modifier;

public class ModifiersTest {
    public static void main(String arts[]) {
        Class modifiersTestClass = ModifiersTest.class;
        System.out.println(modifiersTestClass.getModifiers());
        System.out.println(Modifier.isPublic(modifiersTestClass.getModifiers()));

        Class birdClass = Bird.class;
        System.out.println(birdClass.getModifiers());
        System.out.println(Modifier.isPublic(birdClass.getModifiers()));

        Class bird1Class = Bird1.class;
        System.out.println(bird1Class.getModifiers());
        System.out.println(Modifier.isPublic(bird1Class.getModifiers()));

        Class bird2Class = Bird2.class;
        System.out.println(bird2Class.getModifiers());
        System.out.println(Modifier.isPublic(bird2Class.getModifiers()));

    }

    private class Bird {

    }

    class Bird1 {

    }

    protected class Bird2 {

    }
}
```

```java
1
true
2
false
0
false
4
false
```

类修饰符有public、private等类型，getModifiers()可以获取一个类的修饰符，但是返回的结果是int，结合Modifier提供的方法，就可以确认修饰符的类型。

```java
 Modifier.isAbstract(int modifiers)
 Modifier.isFinal(int modifiers)
 Modifier.isInterface(int modifiers)
 Modifier.isNative(int modifiers)
 Modifier.isPrivate(int modifiers)
 Modifier.isProtected(int modifiers)
 Modifier.isPublic(int modifiers)
 Modifier.isStatic(int modifiers)
 Modifier.isStrict(int modifiers)
 Modifier.isSynchronized(int modifiers)
 Modifier.isTransient(int modifiers)
 Modifier.isVolatile(int modifiers)
```



### **获取包信息**

```java
package ReflectionTest;

public class PackageTest {
    public static void main(String arts[]) {

        Class birdClass = Bird.class;
        System.out.println(birdClass.getPackage());

    }

    private class Bird {

    }
}
```

```java
package ReflectionTest

Process finished with exit code 0
```



### **获取父类的Class对象**

```java
package ReflectionTest;

public class SuperClassTest {
    public static void  main(String arts[]){

        Class birdClass = Bird.class;
        Class superclass = birdClass.getSuperclass();
        System.out.println(superclass.getSimpleName());
    }

    private class Bird extends Animal{

    }

    private class Animal{

    }
}
```

```java
Animal
```





### 获取构造函数Constructor

一个类会有多个构造函数，getConstructors()返回的是Constructor[]数组，包含了所有声明的用public修饰的构造函数。

如果你已经知道了某个构造的参数，可以通过下面的方法获取到回应的构造函数对象：

```java
package ReflectionTest;

import java.lang.reflect.Constructor;

public class ConstructorTest {
    public static void main(String arts[]) {

        Class birdClass = Bird.class;
        Constructor[] constructors = birdClass.getConstructors();
        for (Constructor c : constructors) {
            System.out.println(c.getName());
        }

        Class birdClass1 = Bird.class;
        try {
            Constructor constructors1 = birdClass.getConstructor(new Class[]{String.class});
        } catch (NoSuchMethodException e) {

        }
    }

    private class Bird extends Animal {
        private int name;

        public Bird() {
        }

        public Bird(int name) {
            this.name = name;
        }
    }

    private class Animal {

    }
}
```

```java
ReflectionTest.ConstructorTest$Bird
ReflectionTest.ConstructorTest$Bird
```

上面获取构造函数的方式有2点需要注意：
1、只能获取到public修饰的构造函数。
2、需要捕获NoSuchMethodException异常。

#### **获取构造函数的参数**

获取到构造函数的对象之后，可以通过getParameterTypes()获取到构造函数的参数。

```java
Constructor constructors = birdClass.getConstructor(new Class[]{String.class});
            Class[] parameterTypes = constructors.getParameterTypes();
```



### **初始化对象**

通过反射获取到构造器之后，通过newInstance()方法就可以生成类对象。

newinstance()方法接受可选数量的参数，必须为所调用的构造函数提供准确的参数。如果构造函数要求String的参数，在调用newinstance()方法是，必须提供String类型的参数。

```java
package ReflectionTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConstructorTest {
    public static void main(String arts[]) {

        Class birdClass1 = Bird.class;
        try {
//            Constructor constructors = birdClass1.getConstructor(new Class[]{String.class});
            Constructor constructors = birdClass1.getConstructor();
            Class[] parameterTypes = constructors.getParameterTypes();
//            Bird bird = (Bird)constructors.newInstance("eat tea");
            Bird bird = (Bird) constructors.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.out.println("没有对应的构造函数");
        }
    }

    private class Bird extends Animal {
        private String name;

//        public Bird() {
//        }

        public Bird(String name) {
            this.name = name;
        }
    }

    private class Animal {

    }
}
```

```java
没有对应的构造函数
```



### **获取Methods方法信息**

下面代码是通过反射可以获取到该类的声明的成员方法信息：

```java
Method[] metchods = birdClass.getMethods();
Method[] metchods1 = birdClass.getDeclaredMethods();
Method eatMetchod = birdClass.getMethod("eat", new Class[]{int.class});
Method eatMetchod1 = birdClass.getDeclaredMethod("eat", new Class[]{int.class});
```

无参的getMethods()获取到所有public修饰的方法，返回的是Method[]数组。 无参的getDeclaredMethods()方法到的是所有的成员方法，和修饰符无关。 对于有参的getMethods()方法，必须提供要获取的方法名以及方法名的参数。如果要获取的方法没有参数，则用null替代：

```java
Method eatMetchod = birdClass.getMethod("eat", null);
```

无参的getMethods()和getDeclaredMethods()都只能获取到类声明的成员方法，不能获取到继承父类的方法。

```java
package ReflectionTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConstructorTest {
    public static void main(String args[]) {

        Class birdClass = Bird.class;
        try {
            //如果内部类,需要加一个参数
            Constructor constructors = birdClass.getConstructor(ConstructorTest.class, String.class);
            //如果是内部类需要传入外部类的实例
            Bird bird = (Bird) constructors.newInstance(new ConstructorTest(), "eat tea");

        } catch (NoSuchMethodException e) {
            System.out.println("没有对应的构造函数");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    class Bird {
        private String name;

        public Bird() {
        }

        public Bird(String name) {
            this.name = name;
        }
    }


}
```





**获取Methods方法信息**
下面代码是通过反射可以获取到该类的声明的成员方法信息：

```java
Method[] metchods = birdClass.getMethods();
Method[] metchods1 = birdClass.getDeclaredMethods();
Method eatMetchod = birdClass.getMethod("eat", new Class[]{int.class});
Method eatMetchod1 = birdClass.getDeclaredMethod("eat", new Class[]{int.class});
```

无参的getMethods()获取到所有public修饰的方法，返回的是Method[]数组。 无参的getDeclaredMethods()方法到的是所有的成员方法，和修饰符无关。 对于有参的getMethods()方法，必须提供要获取的方法名以及方法名的参数。如果要获取的方法没有参数，则用null替代：

```java
Method eatMetchod = birdClass.getMethod("eat", null);
```

无参的getMethods()和getDeclaredMethods()都只能获取到类声明的成员方法，不能获取到继承父类的方法。



**获取成员方法参数**

```java
Class birdClass = Bird.class;
Class[] parameterTypes = eatMetchod1.getParameterTypes();
```

**获取成员方法返回类型**

```java
Class birdClass = Bird.class;
Class returnType = eatMetchod1.getReturnType();
```



**invoke()方法**
java反射提供invoke()方法，在运行时根据业务需要调用相应的方法，这种情况在运行时非常常见，只要通过反射获取到方法名之后，就可以调用对应的方法：

```java
Class birdClass = Bird.class;
Constructor constructors1 = birdClass.getConstructor();
Method eatMetchod = birdClass.getMethod("eat", new Class[]{int.class});
System.out.println(eatMetchod.invoke(constructors1.newInstance(), 2));
```

invoke方法有两个参数，第一个参数是要调用方法的对象，上面的代码中就是Bird的对象，第二个参数是调用方法要传入的参数。如果有多个参数，则用数组。

如果调用的是static方法，invoke()方法第一个参数就用null代替：

```java
public class Alunbar {
    public static void  main(String arts[]){
        try{
            Class birdClass = Bird.class;
            Constructor constructors1 = birdClass.getConstructor();
            Method eatMetchod = birdClass.getMethod("eat", new Class[]{int.class});
            System.out.println(eatMetchod.invoke(null, 2));
        }catch(Exception  e){
            e.printStackTrace();
            System.out.println("没有对应的构造函数");
        }
    }

}

class Bird{
    public static int eat(int eat){
        return eat;
    }
    public Bird(){

    }

    public Bird(String eat){

    }

    private void talk(){}
 }

 class Animal{
     public void run(){

     }
 }
```





使用反射可以在运行时检查和调用类声明的成员方法，可以用来检测某个类是否有getter和setter方法。getter和setter是java bean必须有的方法。 getter和setter方法有下面的一些规律： getter方法以get为前缀，无参，有返回值 setter方法以set为前缀，有一个参数，返回值可有可无， 下面的代码提供了检测一个类是否有getter和setter方法：

```java
package ReflectionTest;

import java.lang.reflect.Method;

public class printGettersSetters {
    public static void main(String[] args) {
        printGettersSetters(printGettersSetters.class);
    }
    public static void printGettersSetters (Class aClass){
        Method[] methods = aClass.getMethods();

        for (Method method : methods) {
            if (isGetter(method)) System.out.println("getter: " + method);
            if (isSetter(method)) System.out.println("setter: " + method);
        }
    }

    public static boolean isGetter (Method method){
        if (!method.getName().startsWith("get")) return false;
        if (method.getParameterTypes().length != 0) return false;
        if (void.class.equals(method.getReturnType())) return false;
        return true;
    }

    public static boolean isSetter (Method method){
        if (!method.getName().startsWith("set")) return false;
        if (method.getParameterTypes().length != 1) return false;
        return true;
    }
}
```

```java
getter: public final native java.lang.Class java.lang.Object.getClass()

Process finished with exit code 0
```





### **获取成员变量**

通过反射可以在运行时获取到类的所有成员变量，还可以给成员变量赋值和获取成员变量的值。

```java
Class birdClass = Bird.class;
Field[] fields1 = birdClass.getFields();
Field[] fields2 = birdClass.getDeclaredFields();
Field fields3 = birdClass.getField("age");
Field fields4 = birdClass.getDeclaredField("age");
```

getFields()方法获取所有public修饰的成员变量，getField()方法需要传入变量名，并且变量必须是public修饰符修饰。

getDeclaredFields方法获取所有生命的成员变量，不管是public还是private。

**获取成员变量类型**

```java
Field fields4 = birdClass.getDeclaredField("age");
Object fieldType = fields4.getType();
```

**成员变量赋值和取值**
一旦获取到成员变量的Field引用，就可以获取通过get()方法获取变量值，通过set()方法给变量赋值：

```java
Class birdClass = Bird.class;
Field fields3 = birdClass.getField("age");
Bird bird = new Bird();
Object value = fields3.get(bird);
fields3.set(bird, value);
```



**访问私有变量**
有很多文章讨论禁止通过反射访问一个对象的私有变量，但是到目前为止所有的jdk还是允许通过反射访问私有变量。

使用 Class.getDeclaredField(String name)或者Class.getDeclaredFields()才能获取到私有变量。

```java
import java.lang.reflect.Field;

public class PrivateField {
    protected  String name;

    public PrivateField(String name){
        this.name = name;
    }
}

public class PrivateFieldTest {
    public static void main(String args[])throws Exception{
        Class privateFieldClass = PrivateField.class;
        Field privateName = privateFieldClass.getDeclaredField("name");
        privateName.setAccessible(false);
        PrivateField privateField = new PrivateField("Alunbar");
        String privateFieldValue = (String) privateName.get(privateField);
        System.out.println("私有变量值：" + privateFieldValue);
    }
}
```

上面的代码有点需要注意：必须调用**setAccessible(true)**方法，这是针对私有变量而言，public和protected等都不需要。这个方法是允许通过反射访问类的私有变量。

**访问私有方法**
和私有变量一样，私有方法也是不允许其他的类随意调用的，但是通过反射可以饶过这一限制。 使用Class.getDeclaredMethod(String name, Class[] parameterTypes)或者Class.getDeclaredMethods()方法获取到私有方法。

```java
public class PrivateMethod {
    private String accesPrivateMethod(){
        return "成功访问私有方法";
    }
}

public class PrivateMethodTest {
    public static void main(String args[])throws Exception{
        Class privateMethodClass = PrivateMethod.class;

        Method privateStringMethod = privateMethodClass.getDeclaredMethod("accesPrivateMethod", null);
        privateStringMethod.setAccessible(true);
        String returnValue = (String)privateStringMethod.invoke(new PrivateMethod(), null);

        System.out.println("returnValue = " + returnValue);
    }
}
```

和访问私有变量一样，也要调用**setAccessible(true)**方法，允许通过反射访问类的私有方法。



### **访问类注解信息**

通过反射可以在运行时获取到类、方法、变量和参数的注解信息。

访问类的所有注解信息：

```java
Class aClass = TheClass.class;
Annotation[] annotations = aClass.getAnnotations();

for(Annotation annotation : annotations){
    if(annotation instanceof MyAnnotation){
        MyAnnotation myAnnotation = (MyAnnotation) annotation;
        System.out.println("name: " + myAnnotation.name());
        System.out.println("value: " + myAnnotation.value());
    }
}
```

访问类特定的注解信息：

```java
Class aClass = TheClass.class;
Annotation annotation = aClass.getAnnotation(MyAnnotation.class);

if(annotation instanceof MyAnnotation){
    MyAnnotation myAnnotation = (MyAnnotation) annotation;
    System.out.println("name: " + myAnnotation.name());
    System.out.println("value: " + myAnnotation.value());
}
```

访问方法注解信息：

```java
Method method = ... //obtain method object
Annotation[] annotations = method.getDeclaredAnnotations();

for(Annotation annotation : annotations){
    if(annotation instanceof MyAnnotation){
        MyAnnotation myAnnotation = (MyAnnotation) annotation;
        System.out.println("name: " + myAnnotation.name());
        System.out.println("value: " + myAnnotation.value());
    }
}
```

访问特定方法注解信息：

```java
Method method = ... // obtain method object
Annotation annotation = method.getAnnotation(MyAnnotation.class);

if(annotation instanceof MyAnnotation){
    MyAnnotation myAnnotation = (MyAnnotation) annotation;
    System.out.println("name: " + myAnnotation.name());
    System.out.println("value: " + myAnnotation.value());
}
```

访问参数注解信息：

```java
Method method = ... //obtain method object
Annotation[][] parameterAnnotations = method.getParameterAnnotations();
Class[] parameterTypes = method.getParameterTypes();

int i=0;
for(Annotation[] annotations : parameterAnnotations){
  Class parameterType = parameterTypes[i++];

  for(Annotation annotation : annotations){
    if(annotation instanceof MyAnnotation){
        MyAnnotation myAnnotation = (MyAnnotation) annotation;
        System.out.println("param: " + parameterType.getName());
        System.out.println("name : " + myAnnotation.name());
        System.out.println("value: " + myAnnotation.value());
    }
  }
}
```

Method.getParameterAnnotations()方法返回的是一个二维的Annotation数组，其中包含每个方法参数的注解数组。

访问类所有变量注解信息：

```java
Field field = ... //obtain field object
Annotation[] annotations = field.getDeclaredAnnotations();

for(Annotation annotation : annotations){
    if(annotation instanceof MyAnnotation){
        MyAnnotation myAnnotation = (MyAnnotation) annotation;
        System.out.println("name: " + myAnnotation.name());
        System.out.println("value: " + myAnnotation.value());
    }
}
```

访问类某个特定变量的注解信息：

```java
Field field = ... // obtain method object
Annotation annotation = field.getAnnotation(MyAnnotation.class);

if(annotation instanceof MyAnnotation){
    MyAnnotation myAnnotation = (MyAnnotation) annotation;
    System.out.println("name: " + myAnnotation.name());
    System.out.println("value: " + myAnnotation.value());
}
```



### **获取泛型信息**

很多人认为java类在编译的时候会把泛型信息给擦除掉，所以在运行时是无法获取到泛型信息的。其实在某些情况下，还是可以通过反射在运行时获取到泛型信息的。

获取到`java.lang.reflect.Method`对象，就有可能获取到某个方法的泛型返回信息。

**泛型方法返回类型**
下面的类中定义了一个返回值中有泛型的方法：

```java
public class MyClass {

  protected List<String> stringList = ...;

  public List<String> getStringList(){
    return this.stringList;
  }
}
```

下面的代码使用反射检测getStringList()方法返回的是`List<String>`而不是`List`

```java
Method method = MyClass.class.getMethod("getStringList", null);

Type returnType = method.getGenericReturnType();

if(returnType instanceof ParameterizedType){
    ParameterizedType type = (ParameterizedType) returnType;
    Type[] typeArguments = type.getActualTypeArguments();
    for(Type typeArgument : typeArguments){
        Class typeArgClass = (Class) typeArgument;
        System.out.println("typeArgClass = " + typeArgClass);
    }
}
```

上面这段代码会打印：`typeArgClass = java.lang.String`

**泛型方法参数类型**
下面的类定义了一个有泛型参数的方法setStringList():

```java
public class MyClass {
  protected List<String> stringList = ...;

  public void setStringList(List<String> list){
    this.stringList = list;
  }
}
```

Method类提供了getGenericParameterTypes()方法获取方法的泛型参数。

```java
method = Myclass.class.getMethod("setStringList", List.class);

Type[] genericParameterTypes = method.getGenericParameterTypes();

for(Type genericParameterType : genericParameterTypes){
    if(genericParameterType instanceof ParameterizedType){
        ParameterizedType aType = (ParameterizedType) genericParameterType;
        Type[] parameterArgTypes = aType.getActualTypeArguments();
        for(Type parameterArgType : parameterArgTypes){
            Class parameterArgClass = (Class) parameterArgType;
            System.out.println("parameterArgClass = " + parameterArgClass);
        }
    }
}
```

上面的代码会打印出`parameterArgType = java.lang.String`

**泛型变量类型**
通过反射也可以获取到类的成员泛型变量信息——静态变量或实例变量。下面的类定义了一个泛型变量：

```java
public class MyClass {
  public List<String> stringList = ...;
}
```

通过反射的Filed对象获取到泛型变量的类型信息：

```java
Field field = MyClass.class.getField("stringList");

Type genericFieldType = field.getGenericType();

if(genericFieldType instanceof ParameterizedType){
    ParameterizedType aType = (ParameterizedType) genericFieldType;
    Type[] fieldArgTypes = aType.getActualTypeArguments();
    for(Type fieldArgType : fieldArgTypes){
        Class fieldArgClass = (Class) fieldArgType;
        System.out.println("fieldArgClass = " + fieldArgClass);
    }
}
```

Field对象提供了getGenericType()方法获取到泛型变量。 上面的代码会打印出：`fieldArgClass = java.lang.String`





### **动态代理**

使用反射可以在运行时创建接口的动态实现，java.lang.reflect.Proxy类提供了创建动态实现的功能。我们把运行时创建接口的动态实现称为动态代理。

动态代理可以用于许多不同的目的，例如数据库连接和事务管理、用于单元测试的动态模拟对象以及其他类似aop的方法拦截等。



**创建代理**

调用java.lang.reflect.Proxy类的newProxyInstance()方法就可以常见动态代理，newProxyInstance()方法有三个参数： 1、用于“加载”动态代理类的类加载器。
2、要实现的接口数组。 3、将代理上的所有方法调用转发到InvocationHandler的对象。 代码如下：

```java
InvocationHandler handler = new MyInvocationHandler();
MyInterface proxy = (MyInterface) Proxy.newProxyInstance(
                            MyInterface.class.getClassLoader(),
                            new Class[] { MyInterface.class },
                            handler);
```

运行上面代码后，proxy变量包含了MyInterface接口的动态实现。

对代理的所有调用都将由到实现了InvocationHandler接口的handler 对象来处理。

**InvocationHandler**
如上面说的一样，必须将InvocationHandler的实现传递给Proxy.newProxyInstance()方法。对动态代理的所有方法调用都转发到实现接口的InvocationHandler对象。 InvocationHandler代码：

```java
public interface InvocationHandler{
  Object invoke(Object proxy, Method method, Object[] args)
         throws Throwable;
}
```

实现InvocationHandler接口的类：

```java
public class MyInvocationHandler implements InvocationHandler{

  public Object invoke(Object proxy, Method method, Object[] args)
  throws Throwable {
    //do something "dynamic"
  }
}
```

下面详细介绍传递给invoke方法的三个参数。

Object proxy参数，实现接口的动态代理对象。通常不需要这个对象。

Method method参数，表示在动态代理实现的接口上调用的方法。通过Method对象，可以获取到方法名，参数类型，返回类型等信息。

Object[] args参数，包含调用接口中实现的方法时传递给代理的参数值。注意：如果接口中的参数是int、long等基本数据时，这里的args必须使用Integer, Long等包装类型。

上面代码中会生成一个MyInterface接口的对象proxy，通过proxy对象调用的方法都会由MyInvocationHandler类的invoke方法处理。

动态代理使用场景：
1、数据库连接和事务管理。例如Spring框架有一个事务代理，可以启动和提交/回滚事务
2、用于单元测试的动态模拟对象
3、类似AOP的方法拦截。

本文重点介绍了如何通过反射获取到某个类的方法、成员变量、构造函数等信息，同时也介绍动态代理的用法，这些都是反射的基础功能，反射的其他功能里就不一一介绍了。











































### **获取接口信息**

获取接口信息的方法：

```java
Class[] interfaces = birdClass.getInterfaces();
```



一个类可以实现多个接口，所以getInterfaces()方法返回的是Class[]数组。 注意：getInterfaces()只返回指定类实现的接口，不会返父类实现的接口。









### **反射包reflect中的常用类**

反射机制中除了上面介绍的java.lang包中的Class类之外，还需要java.lang.reflect包中的Constructor类、Method类、Field类和Parameter类。Java 8以后在java.lang.reflect包中新增了一个Executable抽象类，该类对象代表可执行的类成员。Executable抽象类派生了Constructor和Method两个子类。

java.lang.reflect.Executable类提供了大量方法用来获取参数、修饰符或注解等信息，其常用方法如下表所示。

![img](MarkDown_Java%20SE.assets/2f738bd4b31c87016242c6b1287f9e2f0608fff5)



getModifiers（）方法返回的是以整数表示的修饰符。此时引入Modifier类，通过调用Modifier.toString（int mod）方法返回修饰符常量所应的字符串。

java.lang.reflect.Constructor类是java.lang.reflect.Executable类的直接子类，用于表示类的构造方法。通过Class对象的getConstructors（）方法可以获得当前运行时类的构造方法。java.lang.reflect.Constructor类的常用方法如下表所示。

![img](MarkDown_Java%20SE.assets/9922720e0cf3d7ca8a004b21fd1fbe096a63a9f7)



java.lang.reflect.Method类是java.lang.reflect.Executable类的直接子类，用于封装成员方法的信息，调用Class对象的getMethod（）方法或getMethods（）方法可以获得当前运行时类的指定方法或所有方法。下表是java.lang.reflect.Method类的常用方法。

![img](MarkDown_Java%20SE.assets/7a899e510fb30f24d5699854c795d143ac4b03f1)



java.lang.reflect.Field类用于封装成员变量信息，调用Class对象的getField（）方法或getFields（）可以获得当前运行时类的指定成员变量或所有成员变量。java.lang.reflect.Field类的常用方法如下表所示。

![img](MarkDown_Java%20SE.assets/9358d109b3de9c82868b8dd86381800a18d843fe)



java.lang.reflect.Parameter类是参数类，每个Parameter对象代表方法的一个参数。java.lang.reflect.Parameter类中提供了许多方法来获取参数信息，下表给出了java.lang.reflect.Parameter类的常用方法。

![img](MarkDown_Java%20SE.assets/728da9773912b31b084aba7e8918367adab4e147)



*说明：使用javac命令编译Java源文件时，默认生成.class字节码文件中不包含方法的形参名信息，因此调用getName（）方法不能得到参数的形参名，调用isNamePresent（）方法将返回false。如果希望javac命令编译Java源文件时保留形参信息，则需要为编译命令指定-parameters选项。*







### 意义

首先，反射机制极大的提高了程序的灵活性和扩展性，降低模块的耦合性，提高自身的适应能力。

其次，通过反射机制可以让程序创建和控制任何类的对象，无需提前硬编码目标类。

再次，使用反射机制能够在运行时构造一个类的对象、判断一个类所具有的成员变量和方法、调用一个对象的方法。

最后，反射机制是构建框架技术的基础所在，使用反射可以避免将代码写死在框架中。

正是反射有以上的特征，所以它能动态编译和创建对象，极大的激发了编程语言的灵活性，强化了多态的特性，进一步提升了面向对象编程的抽象能力，因而受到编程界的青睐。

### 缺点

尽管反射机制带来了极大的灵活性及方便性，但反射也有缺点。反射机制的功能非常强大，但不能滥用。在能不使用反射完成时，尽量不要使用，原因有以下几点：

1、性能问题。

Java反射机制中包含了一些动态类型，所以Java虚拟机不能够对这些动态代码进行优化。因此，反射操作的效率要比正常操作效率低很多。我们应该避免在对性能要求很高的程序或经常被执行的代码中使用反射。而且，如何使用反射决定了性能的高低。如果它作为程序中较少运行的部分，性能将不会成为一个问题。

2、安全限制。

使用反射通常需要程序的运行没有安全方面的限制。如果一个程序对安全性提出要求，则最好不要使用反射。

3、程序健壮性。

反射允许代码执行一些通常不被允许的操作，所以使用反射有可能会导致意想不到的后果。反射代码破坏了Java程序结构的抽象性，所以当程序运行的平台发生变化的时候，由于抽象的逻辑结构不能被识别，代码产生的效果与之前会产生差异。v



## 注解

### @SuppressWarnings

**简介**：java.lang.SuppressWarnings是J2SE5.0中标准的Annotation之一。可以标注在类、字段、方法、参数、构造方法，以及局部变量上。
**作用**：告诉编译器忽略指定的警告，不用在编译完成后出现警告信息。
**使用**：

```java
@SuppressWarnings(“”)
@SuppressWarnings({})
@SuppressWarnings(value={})
```

**根据sun的官方文档描述：**
value -将由编译器在注释的元素中取消显示的警告集。允许使用重复的名称。忽略第二个和后面出现的名称。

出现未被识别的警告名不是错误：编译器必须忽略无法识别的所有警告名。但如果某个注释包含未被识别的警告名，那么编译器可以随意发出一个警告。

各编译器供应商应该将它们所支持的警告名连同注释类型一起记录。鼓励各供应商之间相互合作，确保在多个编译器中使用相同的名称。

示例：

```java
 @SuppressWarnings("unchecked")
```

告诉编译器忽略 unchecked 警告信息，如使用List，ArrayList等未进行参数化产生的警告信息。

```java
@SuppressWarnings("serial")
```

如果编译器出现这样的警告信息：The serializable class WmailCalendar does notdeclare a static final serialVersionUID field of type long使用这个注释将警告信息去掉。

```java
 @SuppressWarnings("deprecation")
```

如果使用了使用@Deprecated注释的方法，编译器将出现警告信息。使用这个注释将警告信息去掉。

```java
@SuppressWarnings("unchecked", "deprecation")
```

告诉编译器同时忽略unchecked和deprecation的警告信息。

```java
@SuppressWarnings(value={"unchecked", "deprecation"})
```

等同于@SuppressWarnings("unchecked", "deprecation")





## 异常(Error与Exception)

### 一、 异常机制的概述

异常机制是指当程序出现错误后，程序如何处理。具体来说，异常机制提供了程序退出的安全通道。当出现错误后，程序执行的流程发生改变，程序的控制权转移到异常处理器。

程序错误分为三种：1.编译错误；2.运行时错误；3.逻辑错误。

（1）编译错误是因为程序没有遵循语法规则，编译程序能够自己发现并且提示我们错误的原因和位置，这个也是大家在刚接触编程语言最常遇到的问题。

（2）运行时错误是因为程序在执行时，运行环境发现了不能执行的操作。

（3）逻辑错误是因为程序没有按照预期的逻辑顺序执行。异常也就是指程序运行时发生错误，而异常处理就是对这些错误进行处理和控制。

### 二、 异常的结构

在 Java 中，所有的异常都有一个共同的祖先 Throwable（可抛出）。Throwable 指定代码中可用异常传播机制通过 Java 应用程序传输的任何问题的共性。

![img](MarkDown_Java%20SE.assets/1e70c56573122a76a57de2054604400c963e5c0f.png@1320w_694h.webp)

Throwable： 有两个重要的子类：Exception（异常）和 Error（错误），二者都是 Java 异常处理的重要子类，各自都包含大量子类。异常和错误的区别是：异常能被程序本身可以处理，错误是无法处理。
      Throwable类中常用方法如下：

1. 返回异常发生时的详细信息public string getMessage();
2. 返回异常发生时的简要描述public string toString();
3. 返回异常对象的本地化信息。使用Throwable的子类覆盖这个方法，可以声称本地化信息。如果子类没有覆盖该方法，则该方法返回的信息与getMessage（）返回的结果相同public string getLocalizedMessage();
4. 在控制台上打印Throwable对象封装的异常信息public void printStackTrace();

Error（错误）:是程序无法处理的错误，表示运行应用程序中较严重问题。大多数错误与代码编写者执行的操作无关，而表示代码运行时 JVM（Java 虚拟机）出现的问题。例如，Java虚拟机运行错误（Virtual MachineError），当 JVM 不再有继续执行操作所需的内存资源时，将出现 OutOfMemoryError。这些异常发生时，Java虚拟机（JVM）一般会选择线程终止。这些错误表示故障发生于虚拟机自身、或者发生在虚拟机试图执行应用时，如Java虚拟机运行错误（Virtual MachineError）、类定义错误（NoClassDefFoundError）等。这些错误是不可查的，因为它们在应用程序的控制和处理能力之 外，而且绝大多数是程序运行时不允许出现的状况。对于设计合理的应用程序来说，即使确实发生了错误，本质上也不应该试图去处理它所引起的异常状况。在 Java中，错误通过Error的子类描述。 

Exception（异常）:是程序本身可以处理的异常。Exception 类有一个重要的子类 RuntimeException。RuntimeException 类及其子类表示“JVM 常用操作”引发的错误。例如，若试图使用空值对象引用、除数为零或数组越界，则分别引发运行时异常（NullPointerException、ArithmeticException）和 ArrayIndexOutOfBoundException。
      Exception（异常）分两大类：运行时异常和非运行时异常(编译异常)。程序中应当尽可能去处理这些异常。
      1.运行时异常：都是RuntimeException类及其子类异常，如NullPointerException(空指针异常)、IndexOutOfBoundsException(下标越界异常)等，这些异常是不检查异常，程序中可以选择捕获处理，也可以不处理。这些异常一般是由程序逻辑错误引起的，程序应该从逻辑角度尽可能避免这类异常的发生。运行时异常的特点是Java编译器不会检查它，也就是说，当程序中可能出现这类异常，即使没有用try-catch语句捕获它，也没有用throws子句声明抛出它，也会编译通过。
      2.非运行时异常 （编译异常）：是RuntimeException以外的异常，类型上都属于Exception类及其子类。从程序语法角度讲是必须进行处理的异常，如果不处理，程序就不能编译通过。如IOException、SQLException等以及用户自定义的Exception异常，一般情况下不自定义检查异常。

​    	通常，Java的异常（Throwable）分为可查的异常（checked exceptions）和不可查的异常（unchecked exceptions）。
![img](MarkDown_Java%20SE.assets/18a4cbf779949d4435312ea7422b9ce66c4b3fdc.jpg@1168w_684h.webp)

1. 可查异常（编译器要求必须处置的异常）：正确的程序在运行中，很容易出现的、情理可容的异常状况。除了Exception中的RuntimeException及RuntimeException的子类以外，其他的Exception类及其子类(例如：IOException和ClassNotFoundException)都属于可查异常。这种异常的特点是Java编译器会检查它，也就是说，当程序中可能出现这类异常，要么用try-catch语句捕获它，要么用throws子句声明抛出它，否则编译不会通过。
2. 不可查异常(编译器不要求强制处置的异常):包括运行时异常（RuntimeException与其子类）和错误（Error）。RuntimeException表示编译器不会检查程序是否对RuntimeException作了处理，在程序中不必捕获RuntimException类型的异常，也不必在方法体声明抛出RuntimeException类。RuntimeException发生的时候，表示程序中出现了编程错误，所以应该找出错误修改程序，而不是去捕获RuntimeException。

- **什么是unchecked异常?**

即RuntimeException（运行时异常）
**不需要try...catch...或throws 机制去处理的异常**



- **列举最常用的五种RuntimeException:**  

这是JAVA认证考试中最常见的题目,事实上,runtime exception中最常见的,经常碰到的,也就5,6种,如下:

| ArithmeticException                                          | int a=0; int b= 3/a;                                      |
| ------------------------------------------------------------ | --------------------------------------------------------- |
| ClassCastException：                                         | Object x = new Integer(0); System.out.println((String)x); |
| IndexOutOfBoundsException   ArrayIndexOutOfBoundsException,   StringIndexOutOfBoundsException | int [] numbers = { 1, 2, 3 }; int sum = numbers[3];       |
| IllegalArgumentException 非法参数异常  NumberFormatException | int a = Interger.parseInt("test");                        |
| NullPointerExceptionextends                                  |                                                           |



- **除了RuntimeException，其他继承自java.lang.Exception得异常统称为Checked Exception,他们有多少种呢?**

下面是JDK API中列出的异常类:
除了**RuntimeException以外的,都是checked Exception
**

java.lang.Object  java.lang.Throwable    java.lang.Exception 所有已实现的接口： Serializable 直接已知子类： AclNotFoundException, ActivationException, AlreadyBoundException, ApplicationException, AWTException, BackingStoreException, BadAttributeValueExpException, BadBinaryOpValueExpException, BadLocationException, BadStringOperationException, BrokenBarrierException, CertificateException, ClassNotFoundException, CloneNotSupportedException, DataFormatException, DatatypeConfigurationException, DestroyFailedException, ExecutionException, ExpandVetoException, FontFormatException, GeneralSecurityException, GSSException, IllegalAccessException, IllegalClassFormatException, InstantiationException, InterruptedException, IntrospectionException, InvalidApplicationException, InvalidMidiDataException, InvalidPreferencesFormatException, InvalidTargetObjectTypeException, InvocationTargetException, IOException, JAXBException, JMException, KeySelectorException, LastOwnerException, LineUnavailableException, MarshalException, MidiUnavailableException, MimeTypeParseException, MimeTypeParseException, NamingException, NoninvertibleTransformException, NoSuchFieldException, NoSuchMethodException, NotBoundException, NotOwnerException, ParseException, ParserConfigurationException, PrinterException, PrintException, PrivilegedActionException, PropertyVetoException, RefreshFailedException, RemarshalException, **RuntimeException**, SAXException, ScriptException, ServerNotActiveException, SOAPException, SQLException, TimeoutException, TooManyListenersException, TransformerException, TransformException, UnmodifiableClassException, UnsupportedAudioFileException, UnsupportedCallbackException, UnsupportedFlavorException, UnsupportedLookAndFeelException, URIReferenceException, URISyntaxException, UserException, XAException, XMLParseException, XMLSignatureException, XMLStreamException, XPathException 





三、 异常处理的机制
      在 Java 应用程序中，异常处理机制为：抛出异常，捕捉异常。
         1. 抛出异常：当一个方法出现错误引发异常时，方法创建异常对象并交付运行时系统，异常对象中包含了异常类型和异常出现时的程序状态等异常信息。运行时系统负责寻找处置异常的代码并执行。详细信息请查看公ZH《java架构宝典》。
         2. 捕获异常：在方法抛出异常之后，运行时系统将转为寻找合适的异常处理器（exception handler）。潜在的异常处理器是异常发生时依次存留在调用栈中的方法的集合。当异常处理器所能处理的异常类型与方法抛出的异常类型相符时，即为合适 的异常处理器。运行时系统从发生异常的方法开始，依次回查调用栈中的方法，直至找到含有合适异常处理器的方法并执行。当运行时系统遍历调用栈而未找到合适 的异常处理器，则运行时系统终止。同时，意味着Java程序的终止。详细信息请查看公ZH《java架构宝典》。



   对于错误、运行时异常、可查异常，Java技术所要求的异常处理方式有所不同。

错误：对于方法运行中可能出现的Error，当运行方法不欲捕捉时，Java允许该方法不做任何抛出声明。因为，大多数Error异常属于永远不能被允许发生的状况，也属于合理的应用程序不该捕捉的异常。

 运行时异常：由于运行时异常的不可查性，为了更合理、更容易地实现应用程序，Java规定，运行时异常将由Java运行时系统自动抛出，允许应用程序忽略运行时异常。

可查异常：对于所有的可查异常，Java规定：一个方法必须捕捉，或者声明抛出方法之外。也就是说，当一个方法选择不捕捉可查异常时，它必须声明将抛出异常。

常的方法，需要提供相符类型的异常处理器。所捕捉的异常，可能是由于自身语句所引发并抛出的异常，也可能是由某个调用的方法或者Java运行时 系统等抛出的异常。也就是说，一个方法所能捕捉的异常，一定是Java代码在某处所抛出的异常。简单地说，异常总是先被抛出，后被捕捉的。

异常抛出：任何Java代码都可以抛出异常，如：自己编写的代码、来自Java开发环境包中代码，或者Java运行时系统。无论是谁，都可以通过Java的throw语句抛出异常。从方法中抛出的任何异常都必须使用throws子句。异常捕获：捕捉异常通过try-catch语句或者try-catch-finally语句实现。总体来说，Java规定：对于可查异常必须捕捉、或者声明抛出。允许忽略不可查的RuntimeException和Error。



### 三、Java常见异常

#### RuntimeException子类:  

1java.lang.ArrayIndexOutOfBoundsException数组索引越界异常。当对数组的索引值为负数或大于等于数组大小时抛出。

2java.lang.ArithmeticException 算术条件异常。譬如：整数除零等。

3java.lang.SecurityException 安全性异常

4java.lang.IllegalArgumentException非法参数异常

5java.lang.ArrayStoreException 数组中包含不兼容的值抛出的异常 

6java.lang.NegativeArraySizeException数组长度为负异常 

7java.lang.NullPointerException空指针异常。当应用试图在要求使用对象的地方使用了null时，抛出该异常。譬如：调用null对象的实例方法、访问null对象的属性、计算null对象的长度、使用throw语句抛出null等等。



####  IOException

1IOException操作输入流和输出流时可能出现的异常

2EOFException文件已结束异常

3FileNotFoundException文件未找到异常



#### 其他 

1ClassCastException类型转换异常类

2ArrayStoreException数组中包含不兼容的值抛出的异常

3SQLException操作数据库异常类

4NoSuchFieldException字段未找到异常

5NoSuchMethodException方法未找到抛出的异常

6NumberFormatException字符串转换为数字抛出的异常

7StringIndexOutOfBoundsException字符串索引超出范围抛出的异常

8IllegalAccessException不允许访问某类异常

9InstantiationException 当应用程序试图使用Class类中的newInstance()方法创建

一个类的实例，而指定的类对象无法被实例化时，抛出该异常

10java.lang.ClassNotFoundException找不到类异常。当应用试图根据字符串形式的类名构造类，而在遍历CLASSPAH之后找不到对应名称的class文件时，抛出该异常。



#### InterruptedException 异常

当一个方法后面声明可能会抛出InterruptedException 异常时，说明该方法是可能会花一点时间，但是可以取消的方法。

 

抛InterruptedException的代表方法有：

1. java.lang.Object 类的 wait 方法

2. java.lang.Thread 类的 sleep 方法

3. java.lang.Thread 类的 join 方法

 

-- 需要花点时间的方法

执行wait方法的线程，会进入等待区等待被notify/notify All。在等待期间，线程不会活动。

执行sleep方法的线程，会暂停执行参数内所设置的时间。

执行join方法的线程，会等待到指定的线程结束为止。

因此，上面的方法都是需要花点时间的方法。

 

-- 可以取消的方法

因为需要花时间的操作会降低程序的响应性，所以可能会取消/中途放弃执行这个方法。

这里主要是通过interrupt方法来取消。

 

1. sleep方法与interrupt方法

interrupt方法是Thread类的实例方法，在执行的时候并不需要获取Thread实例的锁定，任何线程在任何时刻，都可以通过线程实例来调用其他线程的interrupt方法。

当在sleep中的线程被调用interrupt方法时，就会放弃暂停的状态，并抛出InterruptedException异常，这样一来，线程的控制权就交给了捕捉这个异常的catch块了。

 

2. wait方法和interrupt方法

当线程调用wait方法后，线程在进入等待区时，会把锁定接触。当对wait中的线程调用interrupt方法时，会先重新获取锁定，再抛出InterruptedException异常，获取锁定之前，无法抛出InterruptedException异常。

 

3. join方法和interrupt方法

当线程以join方法等待其他线程结束时，一样可以使用interrupt方法取消。因为join方法不需要获取锁定，故而与sleep一样，会马上跳到catch程序块

 

-- interrupt方法干了什么？

interrupt方法其实只是改变了中断状态而已。

而sleep、wait和join这些方法的内部会不断的检查中断状态的值，从而自己抛出InterruptEdException。

所以，如果在线程进行其他处理时，调用了它的interrupt方法，线程也不会抛出InterruptedException的，只有当线程走到了sleep, wait, join这些方法的时候，才会抛出InterruptedException。若是没有调用sleep, wait, join这些方法，或者没有在线程里自己检查中断状态，自己抛出InterruptedException，那InterruptedException是不会抛出来的。

 

isInterrupted方法，可以用来检查中断状态

Thread.interrupted方法，可以用来检查并清除中断状态。







### 四、相关的问题

1. 为什么要创建自己的异常？

   答：当Java内置的异常都不能明确的说明异常情况的时候，需要创建自己的异常。

   \2. 应该在声明方法抛出异常还是在方法中捕获异常？

   答：捕捉并处理知道如何处理的异常，而抛出不知道如何处理的异常。









## 枚举

### 为什么枚举比较用 == 比用 equals 好

分别从运行时和编译时安全考虑

使用equals，运行时会报空指针异常

两个不同的枚举类内的枚举元素进行比较竟然能通过编译！

```java
package Test;

enum Color {
    black,
    white
};

enum Chiral {
    left,
    right
};

public class TestEnum {
    public static void main(String[] args) {
        Color nothing = null;
        if (nothing == Color.black) ;
        if (nothing.equals(Color.black)) ; //NullPointerException

        if (Color.black.equals(Chiral.left)); // compiles fine!!!
        if (Color.black == Chiral.left); //Operator '==' cannot be applied to 'Test.Color', 'Test.Chiral'

    }
}
```









## IO

**java 中 IO 流分为几种？**

按功能来分：输入流（input）、输出流（output）。

按类型来分：字节流和字符流。

字节流和字符流的区别是：字节流按 8 位传输以字节为单位输入输出数据，字符流按 16 位传输以字符为单位输入输出数据。

### IO 概念区分

作者：萧萧
链接：https://www.zhihu.com/question/19732473/answer/241673170
来源：知乎
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。

四个相关概念：

- 同步（Synchronous）
- 异步( Asynchronous)
- 阻塞( Blocking )
- 非阻塞( Nonblocking)

让我们看一下《操作系统概念（第九版）》中有关进程间通信的部分是如何解释的：

![img](MarkDown_Java%20SE.assets/v2-d6729b9e95e8f20c4e53215327596692_720w.jpg)

翻译一下就是：

> 进程间的通信是通过 send() 和 receive() 两种基本操作完成的。具体如何实现这两种基础操作，存在着不同的设计。  消息的传递有可能是**阻塞的**或**非阻塞的** – 也被称为**同步**或**异步**的：

- 阻塞式发送（blocking send）. 发送方进程会被一直阻塞， 直到消息被接受方进程收到。
- 非阻塞式发送（nonblocking send）。 发送方进程调用 send() 后， 立即就可以其他操作。
- 阻塞式接收（blocking receive） 接收方调用 receive() 后一直阻塞， 直到消息到达可用。
- 非阻塞式接受（nonblocking receive） 接收方调用 receive() 函数后， 要么得到一个有效的结果， 要么得到一个空值， 即不会被阻塞。





#### 进程切换



![img](MarkDown_Java%20SE.assets/v2-5672054f97fd77f78420fed6b442536e_hd.jpg)![img]

上图展示了进程切换中几个最重要的步骤：

1. 当一个程序正在执行的过程中， 中断（interrupt） 或 系统调用（system call） 发生可以使得 CPU 的控制权会从当前进程转移到操作系统内核。
2. 操作系统内核负责保存进程 i 在 CPU 中的上下文（程序计数器， 寄存器）到 PCBi （操作系统分配给进程的一个内存块）中。
3. 从 PCBj 取出进程 j 的CPU 上下文， 将 CPU 控制权转移给进程 j ， 开始执行进程 j 的指令。





#### 进程阻塞



![img](MarkDown_Java%20SE.assets/v2-e88514c2e604c4ac538c402f1788862c_hd.jpg)![img]

上图展示了一个进程的不同状态：

- New. 进程正在被创建.
- Running. 进程的指令正在被执行
- Waiting. 进程正在等待一些事件的发生（例如 I/O 的完成或者收到某个信号）
- Ready. 进程在等待被操作系统调度
- Terminated. 进程执行完毕（可能是被强行终止的）

我们所说的 “阻塞”是指进程在**发起了一个系统调用**（System Call） 后， 由于该系统调用的操作不能立即完成，需要等待一段时间，于是内核将进程挂起为**等待 （waiting）**状态， 以确保它不会被调度执行， 占用 CPU 资源。

- 友情提示： **在任意时刻， 一个 CPU 核心上（processor）只可能运行一个进程** 。



#### I/O System Call 的阻塞/非阻塞， 同步/异步

这里再重新审视 **阻塞/非阻塞 IO** 这个概念， 其实**阻塞和非阻塞**描述的是进程的一个操作是否会使得进程转变为“等待”的状态， 但是为什么我们总是把它和 IO 连在一起讨论呢？

原因是， **阻塞**这个词是与系统调用 System Call 紧紧联系在一起的， 因为要让一个进程进入 等待（waiting） 的状态, 要么是它主动调用 wait() 或 sleep() 等挂起自己的操作， 另一种就是它调用 System Call, 而 System Call 因为涉及到了 I/O 操作， 不能立即完成， 于是内核就会先将该进程置为等待状态， 调度其他进程的运行， 等到 它所请求的 I/O 操作完成了以后， 再将其状态更改回 ready 。

操作系统内核在执行 System Call 时， CPU 需要与 IO 设备完成一系列物理通信上的交互， 其实再一次会涉及到阻塞和非阻塞的问题， 例如， 操作系统发起了一个读硬盘的请求后， 其实是向硬盘设备通过总线发出了一个请求，它即可以阻塞式地等待IO 设备的返回结果，也可以非阻塞式的继续其他的操作。 在现代计算机中，这些物理通信操作**基本都是异步完成**的， 即发出请求后， 等待 I/O 设备的中断信号后， 再来读取相应的设备缓冲区。 但是，大部分操作系统默认为用户级应用程序提供的都是**阻塞式的系统调用** （blocking systemcall）接口， 因为阻塞式的调用，使得应用级代码的编写更容易（代码的执行顺序和编写顺序是一致的）。

但同样， 现在的大部分操作系统也会提供非阻塞I/O 系统调用接口（Nonblocking I/O system call）。 一个非阻塞调用不会挂起调用程序， 而是会立即返回一个值， 表示有多少bytes 的数据被成功读取（或写入）。

非阻塞I/O 系统调用( nonblocking system call )的另一个替代品是 **异步I/O系统调用 （asychronous system call）**。 与非阻塞 I/O 系统调用类似，asychronous system call 也是会立即返回， 不会等待 I/O 操作的完成， 应用程序可以继续执行其他的操作， 等到 I/O 操作完成了以后，操作系统会通知调用进程（设置一个用户空间特殊的变量值 或者 触发一个 signal 或者 产生一个软中断 或者 调用应用程序的回调函数）。

此处， **非阻塞I/O 系统调用( nonblocking system call )** 和 **异步I/O系统调用 （asychronous system call）**的区别是：

- 一个**非阻塞I/O 系统调用 read()** 操作立即返回的是任何可以立即拿到的数据， 可以是完整的结果， 也可以是不完整的结果， 还可以是一个空值。
- 而**异步I/O系统调用** read（）结果必须是完整的， 但是这个操作完成的通知可以延迟到将来的一个时间点。

下图展示了同步I/O 与 异步 I/O 的区别 （非阻塞 IO 在下图中没有绘出）.  

![img](MarkDown_Java%20SE.assets/v2-e0180a5ffebd91c480d0ccdc02c6d2a7_hd.jpg)

注意， 上面提到的 **非阻塞I/O 系统调用( nonblocking system call )** 和 **异步I/O系统调用** 都是非阻塞式的行为（non-blocking behavior）。 他们的差异仅仅是返回结果的方式和内容不同。

#### 总结

1. 阻塞/非阻塞， 同步/异步的概念要注意讨论的上下文：

- 在进程通信层面， 阻塞/非阻塞， 同步/异步基本是同义词， 但是需要注意区分讨论的对象是发送方还是接收方。
- 发送方阻塞/非阻塞（同步/异步）和接收方的阻塞/非阻塞（同步/异步） 是互不影响的。
- 在 IO 系统调用层面（ IO system call ）层面， **非阻塞 IO 系统调用** 和 **异步 IO 系统调用**存在着一定的差别， 它们都不会阻塞进程， 但是返回结果的方式和内容有所差别， 但是都属于非阻塞系统调用（ non-blocing system call ）

2. 非阻塞系统调用（non-blocking I/O system call 与 asynchronous I/O system call） 的存在可以用来实现线程级别的 I/O 并发， 与通过多进程实现的 I/O 并发相比可以减少内存消耗以及进程切换的开销。



#### **BIO、NIO、AIO 有什么区别？**

```java
//TODO:待完善
```

https://blog.csdn.net/qq_18297675/article/details/100628025

- BIO：Block IO 同步阻塞式 IO，就是我们平常使用的传统 IO，它的特点是模式简单使用方便，并发处理能力低。
- NIO：New IO 同步非阻塞 IO，是传统 IO 的升级，客户端和服务器端通过 Channel（通道）通讯，实现了多路复用。
- AIO：Asynchronous IO 是 NIO 的升级，也叫 NIO2，实现了异步非堵塞 IO ，异步 IO 的操作基于事件和回调机制。

##### BIO

BIOserver

```java
package BIONIOAIO.BIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BIOserver {
    public static void main(String[] args) throws IOException {
        //创建服务端套接字 & 绑定host:port & 监听client
        ServerSocket serverSocket = new ServerSocket(9999);
        //等待客户端连接到来
        Socket socket = serverSocket.accept();
        //拿到输入流 -- client write to server
        InputStream in = socket.getInputStream();
        //拿到输出流 -- server write to client
        OutputStream out = socket.getOutputStream();
        while (true){
            //将数据读到buf中
            byte[] buf = new byte[32];
            //server read from client
            int len = in.read(buf);
            //如果len == 1，说明client已经断开连接
            if(len == -1){
                throw  new RuntimeException("连接已断开");
            }

            System.out.println("recv:" + new String(buf, 0, len));

            //将读出来的数据写回给client
            //如果不使用偏移量，可能会将buf中的无效数据也写回给client
            out.write(buf, 0, len);
        }
    }



}
```

BIOclient

```java
package BIONIOAIO.BIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class BIOclient {
    public static void main(String[] args) throws IOException, InterruptedException {
        //创建客户端套接字 & 连接服务器
        Socket socket = new Socket("127.0.0.1", 9999);
        //拿到输入流 -- server write to client, client read from server
        InputStream in = socket.getInputStream();
        //拿到输出流 -- client write to server
        OutputStream out = socket.getOutputStream();
        byte[] send = "hello".getBytes();
        while (true){
            //client write to server
            out.write(send);
            byte[] buf = new byte[32];
            //read from server
            int len = in.read(buf, 0 ,send.length);
            //如果len == 1，说明server已经断开连接
            if(len == -1){
                throw  new RuntimeException("连接已断开");
            }
            System.out.println("recv:" + new String(buf, 0, len));
            Thread.sleep(1000);
        }
    }

}
```







##### NIO

Buffer概念
缓冲区，它的内存分配有两种实现，第一种是jvm堆内存分配缓冲区大小，第二种是直接内存分配缓冲区大小。这两种的详细区别，这里不好展开讲，简单说呢，使用jvm堆内存做缓冲区，易于垃圾回收，速度比直接内存更快，但是将数据拷贝到内核空间却需要两次，第一次是拷贝到对外内存，对外内存再到内核空间。如图

![image-20210325145548991](MarkDown_Java%20SE.assets/image-20210325145548991.png)



然后，我们来讲本质上，Buffer是这个什么东西，其实它就是一个数组，然后给你提供各种骚操作，仅此而已。

nio就是非阻塞型I/O（non-blocking I/O），它有三个核心的概念：Selector，Channel，Buffer。
它们的关系就像下图一样

![在这里插入图片描述](MarkDown_Java%20SE.assets/20190424215349475.png)







###### Buffer类

 Buffer 类是 java.nio 的构造基础。一个 Buffer 对象是固定数量的数据的容器，其作用是一个存储器，或者分段运输区，在这里，数据可被存储并在之后用于检索。缓冲区可以被写满或释放。对于每个非布尔原始数据类型都有一个缓冲区类，即 Buffer 的子类有：ByteBuffer、CharBuffer、DoubleBuffer、FloatBuffer、IntBuffer、LongBuffer 和 ShortBuffer，是没有 BooleanBuffer 之说的。尽管缓冲区作用于它们存储的原始数据类型，但缓冲区十分倾向于处理字节。非字节缓冲区可以在后台执行从字节或到字节的转换，这取决于缓冲区是如何创建的。



***\*◇ 缓冲区的四个属性\**** 

https://blog.csdn.net/luming_xml/article/details/51362528

​    所有的缓冲区都具有四个属性来提供关于其所包含的数据元素的信息，这四个属性尽管简单，但其至关重要，需熟记于心：

- **容量(Capacity)**：缓冲区能够容纳的数据元素的最大数量。这一容量在缓冲区创建时被设定，并且永远不能被改变。
- **上界(Limit)：**缓冲区的第一个不能被读或写的元素。缓冲创建时，limit 的值等于 capacity 的值。假设 capacity = 1024，我们在程序中设置了 limit = 512，说明，Buffer 的容量为 1024，但是从 512 之后既不能读也不能写，因此可以理解成，Buffer 的实际可用大小为 512。
- **位置(Position)：**下一个要被读或写的元素的索引。位置会自动由相应的 get() 和 put() 函数更新。 这里需要注意的是positon的位置是从0开始的。
- **标记(Mark)：**一个备忘位置。标记在设定前是未定义的(undefined)。使用场景是，假设缓冲区中有 10 个元素，position 目前的位置为 2(也就是如果get的话是第三个元素)，现在只想发送 6 - 10 之间的缓冲数据，此时我们可以 buffer.mark(buffer.position())，即把当前的 position 记入 mark 中，然后 buffer.postion(6)，此时发送给 channel 的数据就是 6 - 10 的数据。发送完后，我们可以调用 buffer.reset() 使得 position = mark，因此这里的 mark 只是用于临时记录一下位置用的。

请切记，在使用 Buffer 时，我们实际操作的就是这四个属性的值。 我们发现，Buffer 类并没有包括 get() 或 put() 函数。但是，每一个Buffer 的子类都有这两个函数，但它们所采用的参数类型，以及它们返回的数据类型，对每个子类来说都是唯一的，所以它们不能在顶层 Buffer 类中被抽象地声明。它们的定义必须被特定类型的子类所遵从。若不加特殊说明，我们在下面讨论的一些内容，都是以 ByteBuffer 为例，当然，它当然有 get() 和 put() 方法了。 



https://ifeve.com/buffers/

**Buffer的分配**

要想获得一个Buffer对象首先要进行分配。 每一个Buffer类都有一个allocate方法。下面是一个分配48字节capacity的ByteBuffer的例子。

```java
ByteBuffer buf = ByteBuffer.allocate(48);
```

这是分配一个可存储1024个字符的CharBuffer：

```java
CharBuffer buf = CharBuffer.allocate(1024);
```

**向Buffer中写数据**

写数据到Buffer有两种方式：

- 从Channel写到Buffer。
- 通过Buffer的put()方法写到Buffer里。

从Channel写到Buffer的例子

```java
int bytesRead = inChannel.read(buf); //read into buffer.
```

通过put方法写Buffer的例子：

```java
buf.put(127);
```

**相对存取和绝对存取**

```java
public abstract class ByteBuffer extends Buffer implements Comparable { 
// This is a partial API listing
public abstract byte get( );  
public abstract byte get (int index);  
public abstract ByteBuffer put (byte b);  
public abstract ByteBuffer put (int index, byte b); 
} 
```

   来看看上面的代码，有不带索引参数的方法和带索引参数的方法。不带索引的 get 和 put，这些调用执行完后，position 的值会自动前进。当然，对于 put，如果调用多次导致位置超出上界（注意，是 limit 而不是 capacity），则会抛出 BufferOverflowException 异常；对于 get，如果位置不小于上界（同样是 limit 而不是 capacity），则会抛出 BufferUnderflowException 异常。这种不带索引参数的方法，称为相对存取，相对存取会自动影响缓冲区的位置属性。带索引参数的方法，称为绝对存取，绝对存储不会影响缓冲区的位置属性，但如果你提供的索引值超出范围（负数或不小于上界），也将抛出 IndexOutOfBoundsException 异常。 



**flip()方法**

flip方法将Buffer从写模式切换到读模式。调用flip()方法会将position设回0，并将limit设置成之前position的值。

换句话说，position现在用于标记读的位置，limit表示之前写进了多少个byte、char等 —— 现在能读取多少个byte、char等。









**从Buffer中读取数据**

从Buffer中读取数据有两种方式：

1. 从Buffer读取数据到Channel。
2. 使用get()方法从Buffer中读取数据。

从Buffer读取数据到Channel的例子：

```java
//read from buffer into channel.
int bytesWritten = inChannel.write(buf);
```

使用get()方法从Buffer中读取数据的例子

```java
byte aByte = buf.get();
```

```java
public abstract class ByteBuffer extends Buffer implements Comparable { 
// This is a partial API listing
public abstract byte get( );  
public abstract byte get (int index);  
public abstract ByteBuffer put (byte b);  
public abstract ByteBuffer put (int index, byte b); 
} 
```

**rewind()方法**

Buffer.rewind()将position设回0，所以你可以重读Buffer中的所有数据。limit保持不变，仍然表示能从Buffer中读取多少个元素（byte、char等）。

**clear()与compact()方法**

一旦读完Buffer中的数据，需要让Buffer准备好再次被写入。可以通过clear()或compact()方法来完成。

如果调用的是clear()方法，position将被设回0，limit被设置成 capacity的值。换句话说，Buffer 被清空了。Buffer中的数据并未清除，只是这些标记告诉我们可以从哪里开始往Buffer里写数据。

如果Buffer中有一些未读的数据，调用clear()方法，数据将“被遗忘”，意味着不再有任何标记会告诉你哪些数据被读过，哪些还没有。

如果Buffer中仍有未读的数据，且后续还需要这些数据，但是此时想要先先写些数据，那么使用compact()方法。

compact()方法将所有未读的数据拷贝到Buffer起始处。然后将position设到最后一个未读元素正后面。limit属性依然像clear()方法一样，设置成capacity。现在Buffer准备好写数据了，但是不会覆盖未读的数据。

```java
package BIONIOAIO.NIO;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.nio.IntBuffer;
import java.security.SecureRandom;

public class BufferTest {
    public static void main(String[] args) {
        //生成一个长度为10的缓冲区
        IntBuffer intBuffer = IntBuffer.allocate(10);
        for (int i = 0; i < intBuffer.capacity(); ++i){
            int randomNum = new SecureRandom().nextInt(20);
            intBuffer.put(randomNum);
        }
        //状态翻转
        intBuffer.flip();
        while (intBuffer.hasRemaining()){
            //读取数据
            System.out.print(intBuffer.get() + ",");
        }
        //clear方法本质上并不是删除数据
        intBuffer.clear();
        System.out.print("\n");
        System.out.println("-----------------------------");
        while (intBuffer.hasRemaining()){
            System.out.print(intBuffer.get() + ",");
        }
    }


}
```

```java
9,19,18,0,0,19,18,6,11,12,
-----------------------------
9,19,18,0,0,19,18,6,11,12,
Process finished with exit code 0
```

可以看到，调用了clear方法以后，缓冲区里的数据并没有被清除。这是什么原因呢，先不急，先改写一下上面的代码。

```java
package BIONIOAIO.NIO;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.nio.IntBuffer;
import java.security.SecureRandom;

public class BufferTest {
    public static void main(String[] args) {
        IntBuffer intBuffer = IntBuffer.allocate(10);
        System.out.println("初始的Buffer：" + intBuffer);
        for (int i = 0; i < 5; ++i){
            int randomNum = new SecureRandom().nextInt(20);
            intBuffer.put(randomNum);
        }

        System.out.println("flip之前：limit = "+ intBuffer);
        intBuffer.flip();
        System.out.println("flip之后：limit = "+ intBuffer);

        System.out.println("进入读取");
        while (intBuffer.hasRemaining()){
            System.out.println(intBuffer);
            System.out.println(intBuffer.get());
        }
    }
}
```

```java
初始的Buffer：java.nio.HeapIntBuffer[pos=0 lim=10 cap=10]
flip之前：limit = java.nio.HeapIntBuffer[pos=5 lim=10 cap=10]
flip之后：limit = java.nio.HeapIntBuffer[pos=0 lim=5 cap=10]
进入读取
java.nio.HeapIntBuffer[pos=0 lim=5 cap=10]
2
java.nio.HeapIntBuffer[pos=1 lim=5 cap=10]
10
java.nio.HeapIntBuffer[pos=2 lim=5 cap=10]
8
java.nio.HeapIntBuffer[pos=3 lim=5 cap=10]
10
java.nio.HeapIntBuffer[pos=4 lim=5 cap=10]
19

Process finished with exit code 0

```

输出结果中的pos代表的就是`position`属性，lim代表`limit`属性，cap代表`capacity`属性。
在缓冲区刚初始化出来的时候，position指向的是数组的第一个位置，limit和数组的容量一样。
用图片来表示大概就如下图

![image-20210325162516884](MarkDown_Java%20SE.assets/image-20210325162516884.png)

向缓冲区中每写入一个数据，指针就会后移一位

![image-20210325162548078](MarkDown_Java%20SE.assets/image-20210325162548078.png)



当调用了flip()方法后，position又会重新指向数组的第一个位置，而limit会指向原来的position的位置。

![在这里插入图片描述](MarkDown_Java%20SE.assets/20190424232211141.png)

从源码上来看，就是把position赋值给limit，再把position变回0。
如果用图示的话就如下图：

![image-20210325162620226](MarkDown_Java%20SE.assets/image-20210325162620226.png)

然后再调用get方法的时候，每调用读取一个数据，position就会向后移动一位,直到position到达了limit的位置。实际上intBuffer.hasRemaining()方法就是判断position < limit。













**mark()与reset()方法**

通过调用Buffer.mark()方法，可以标记Buffer中的一个特定position。之后可以通过调用Buffer.reset()方法恢复到这个position。例如：

```java
buffer.mark();
//call buffer.get() a couple of times, e.g. during parsing.
buffer.reset();  //set position back to mark.
```



NIOServer

```java
package BIONIOAIO.NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {
    public static void main(String[] args) throws IOException {
        //创建服务端socket通道 & 绑定host:port
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(9999));
        //设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //新创建一个selector（其实可以为每一个channel单独创建一个selector）
        Selector selector = Selector.open();
        //将该通道注册到该selector上，并且注明感兴趣的事件，因为是服务端通道，所以只对accept事件感兴趣
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true){
            //selector会帮我们去轮询，当前是否有我们感兴趣的事件发生，一直阻塞到有为止
            //select还有一个方法，可以指定阻塞时间，超过这个时间就会返回，此时可能返回的key个数为0
            selector.select();
            //若返回的key个数不为0，那么就可以一一处理这些事件
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                //remove是为了下一次select的时候，重复处理这些已经处理过的事件
                //什么意思呢？其实selector.selectedKeys()返回来的set，就是其
                //内部操作的set，引用的是同一个set，所以我们如果不在外面remove已经
                //处理的事件，那么下一次，还会再次出现。需要注意的是，如果在外面对set
                //进行add操作，会抛异常，简单的说就是在外只删不增，在内只增不删。
                iterator.remove();
                //SelectionKey.OP_ACCEPT事件
                if(selectionKey.isAcceptable()){
                    SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    //SelectionKey.OP_READ事件
                } else if(selectionKey.isReadable()){
                    //selectionKey.channel()返回的SelectableChannel是SocketChannel的父类
                    //所以可以直接强转
                    SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                    //NIO规定，必须要用Buffer进行读写
                    ByteBuffer buffer = ByteBuffer.allocate(32);
                    int len = socketChannel.read(buffer);
                    if(len == -1){
                        throw  new RuntimeException("连接已断开");
                    }
                    //上面那一步只是读到缓冲区，这里是从缓冲区真正的拿出数据
                    byte[] buf = new byte[len];
                    //这个操作可以举个例子
                    //例如read(buffer)的时候，其实内部是调用了buffer.put这个方法
                    //那么read结束，position的位置必定等于len
                    //所以我们必须重置一下position为0，才可以从头开始读，但是读到什么地方呢？
                    //那就需要设置limit = position，所以flip后，position=0， limit = len
                    buffer.flip();
                    buffer.get(buf);
                    System.out.println("recv:" + new String(buf, 0, len));
                    //注册写事件
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                    //SelectionKey.OP_WRITE事件
                } else if(selectionKey.isWritable()){
                    SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                    //写数据，也要用Buffer来写
                    int len = socketChannel.write(ByteBuffer.wrap("hello".getBytes()));
                    if(len == -1){
                        throw  new RuntimeException("连接已断开");
                    }
                    //这里为什么要取消写事件呢？因为只要底层的写缓冲区不满，就会一直收到这个事件
                    //所以只有想写数据的时候，才要注册这个写事件
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                }
            }
        }
    }
}
```



NIOClient

```java
package BIONIOAIO.NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        //创建客户端socket通道 & 连接host:port
        SocketChannel socketChannel = SocketChannel.open();
        //设置为非阻塞模式
        socketChannel.configureBlocking(false);
        //非阻塞的形式连接服务器，如果直接使用open带参数的，连接的时候是阻塞连接
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 9999));
        //新创建一个selector
        Selector selector = Selector.open();
        //将该通道注册到该selector上，并且注明感兴趣的事件
        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
        while (true){
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                //连接事件
                if(selectionKey.isConnectable()){
                    //看源码的注释可以知道，如果不使用带参数的open，那么需要手动调用这个方法完成连接
                    //如果是阻塞模式，该方法会阻塞到连接成功，非阻塞模式下，会立刻返回，已连接true，未连接false
                    if(socketChannel.finishConnect()){
                        //需要取消连接事件，否则会一直触发该事件,注册写事件
                        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                    }
                } else if(selectionKey.isReadable()){
                    ByteBuffer buffer = ByteBuffer.allocate(32);
                    int len = socketChannel.read(buffer);
                    if(len == -1){
                        throw  new RuntimeException("连接已断开");
                    }
                    byte[] buf = new byte[len];
                    buffer.flip();
                    buffer.get(buf);
                    System.out.println("recv:" + new String(buf, 0, len));
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                } else if(selectionKey.isWritable()){
                    int len = socketChannel.write(ByteBuffer.wrap("hello".getBytes()));
                    if(len == -1){
                        throw  new RuntimeException("连接已断开");
                    }
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    //这个只是控制一下发送数据的速度
                    Thread.sleep(1000);
                }
            }
        }
    }
}
```



###### chanel概念

chanel和inputstream / outputstream的区别是，前者是双向的，后者是单向的。也就是说，只要你创建一个chanel，你可以用这个chanel进行读写操作。并且chanel是基于Buffer来操作的，不管是读还是写都需要通过Buffer这个东东。画个图，简单理解一下。

![image-20210325162748827](MarkDown_Java%20SE.assets/image-20210325162748827.png)



图中很直观的表现出A和B的chanel都是通过Buffer来读写数据的。上图虽然只画了一个Buffer，但是不止可以有一个Buffer，你可以创建一个读Buffer一个写Buffer都是可以的。
chanel的读写方法，请看定义

```java
public abstract int read(ByteBuffer buffer)
public abstract int write(ByteBuffer buffer)
```

**chanel的两种实现对比**
FileChannel和SocketChannel，读写的实现的区别FileChannel的读写继承至SeekableByteChannel，SocketChannel继承至ReadableByteChannel

SocketChannel的话，就只需要关注Buffer就行，比较简单。FileChannel不能设置非阻塞，并且看实现，也不能使用selector。





![在这里插入图片描述](MarkDown_Java%20SE.assets/20200526114217229.png)



从图中可以看到，我们不再主动的去请求内核，而是让它有主动通知我们。最终，我们都是在读处理和写处理中，用channel发送/接收数据。

**Channel和Buffer的关系？**
可以这么类比
channel.read(buffer)
buffer.read(buf)
相当于
in.read(buf)
抛开stream和channel的差别，其实只是多了一个缓冲区。如图
![在这里插入图片描述](MarkDown_Java%20SE.assets/20190910235156880.png)

**使用Buffer的好处？**
肯定很多人在写demo的时候，觉得Buffer并没有什么用啊？
因为可能demo一般都这么写
ByteBuffer buffer = ByteBuffer.allocate(32);
……
channel.read(buffer);
……
byte[] buf = new byte[32];
buffer.read(buf);
瞧，这样不是脱了裤子放屁吗？为什么不直接内核copy到我的buf呢？没错，如果是这样写，确实太鸡肋了，但是这样写，就可以体现出好处了。
ByteBuffer buffer = ByteBuffer.allocate(1024 * 4);
……
channel.read(buffer);
……
while(true){
byte[] buf = new byte[32];
buffer.read(buf);
//handle
}
如果有1024 * 4个字节，那么没有缓冲区，需要1024 * 4 / 32 次IO操作，
现在有了缓冲区，那么只需要一次IO操作，其它操作都在内存中进行，是不是高效了很多呢？



```java
//TODO:https://blog.csdn.net/qq_18297675/article/details/100628025
```









## 面向对象

类（Class） 和对象（Object） 是面向对象的核心概念。

- 类是一组相关属性和行为的集合。可以看成一类事物的模板；是一类对象的描述，是抽象的，概念上的定义。
- 对象是实际存在的该类事物的每个个体，因而也称为实例（instance）.对象必然具备该类事物的属性和行为。

### 三大特征

#### 继承

#### 封装

#### 多态

**多态的实现方式**

**方式一：重写：**

这个内容已经在上一章节详细讲过，就不再阐述，详细可访问：[Java 重写(Override)与重载(Overload)](https://www.runoob.com/java/java-override-overload.html)。

**方式二：接口**

- \1. 生活中的接口最具代表性的就是插座，例如一个三接头的插头都能接在三孔插座中，因为这个是每个国家都有各自规定的接口规则，有可能到国外就不行，那是因为国外自己定义的接口类型。
- \2. java中的接口类似于生活中的接口，就是一些方法特征的集合，但没有方法的实现。具体可以看 [java接口](https://www.runoob.com/java/java-interfaces.html) 这一章节的内容。

**方式三：抽象类和抽象方法**







## 接口

#### 

```java
//TODO:
```









## lang 与 util 包

#### 

```java
//TODO:
```









## 设计模式

#### 

```java
//TODO:
```





单例模式



代理模式



策略模式



工厂模式



观察者模式









## JVM

《深入理解jvm》

### 运行时数据区域

**JDK 1.8 之前：**

[![img](MarkDown_Java%20SE.assets/JVM%E8%BF%90%E8%A1%8C%E6%97%B6%E6%95%B0%E6%8D%AE%E5%8C%BA%E5%9F%9F.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/JVM运行时数据区域.png)





**JDK 1.8 ：**

[![img](MarkDown_Java%20SE.assets/Java%E8%BF%90%E8%A1%8C%E6%97%B6%E6%95%B0%E6%8D%AE%E5%8C%BA%E5%9F%9FJDK1.8.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/Java运行时数据区域JDK1.8.png)



**线程私有的：**

- 程序计数器
- 虚拟机栈
- 本地方法栈

**线程共享的：**

- 堆
- 方法区
- 直接内存 (非运行时数据区的一部分)



####  程序计数器

程序计数器是一块较小的内存空间，可以看作是当前线程所执行的字节码的行号指示器。**字节码解释器工作时通过改变这个计数器的值来选取下一条需要执行的字节码指令，分支、循环、跳转、异常处理、线程恢复等功能都需要依赖这个计数器来完成。**

另外，**为了线程切换后能恢复到正确的执行位置，每条线程都需要有一个独立的程序计数器，各线程之间计数器互不影响，独立存储，我们称这类内存区域为“线程私有”的内存。**

**从上面的介绍中我们知道程序计数器主要有两个作用：**

1. 字节码解释器通过改变程序计数器来依次读取指令，从而实现代码的流程控制，如：顺序执行、选择、循环、异常处理。
2. 在多线程的情况下，程序计数器用于记录当前线程执行的位置，从而当线程被切换回来的时候能够知道该线程上次运行到哪儿了。

**注意：程序计数器是唯一一个不会出现 `OutOfMemoryError` 的内存区域，它的生命周期随着线程的创建而创建，随着线程的结束而死亡。**因为程序计数器只是记录当前线程正在执行的那条字节码指令的地址，即使出现死循环都不会内存溢出



#### Java 虚拟机栈

**与程序计数器一样，Java 虚拟机栈也是线程私有的，它的生命周期和线程相同，描述的是 Java 方法执行的内存模型，每次方法调用的数据都是通过栈传递的。**

**Java 内存可以粗糙的区分为堆内存（Heap）和栈内存 (Stack),其中栈就是现在说的虚拟机栈，或者说是虚拟机栈中局部变量表部分。** （实际上，Java 虚拟机栈是由一个个栈帧组成，而每个栈帧中都拥有：局部变量表、操作数栈、动态链接、方法出口信息。）

**局部变量表主要存放了编译期可知的各种数据类型**（boolean、byte、char、short、int、float、long、double）、**对象引用**（reference 类型，它不同于对象本身，可能是一个指向对象起始地址的引用指针，也可能是指向一个代表对象的句柄或其他与此对象相关的位置）。

**Java 虚拟机栈会出现两种错误：`StackOverFlowError` 和 `OutOfMemoryError`。**

- **`StackOverFlowError`：** 若 Java 虚拟机栈的内存大小不允许动态扩展，那么当线程请求栈的深度超过当前 Java 虚拟机栈的最大深度的时候，就抛出 StackOverFlowError 错误。
- **`OutOfMemoryError`：** Java 虚拟机栈的内存大小可以动态扩展， 如果虚拟机在动态扩展栈时无法申请到足够的内存空间，则抛出`OutOfMemoryError`异常异常。

[![img](MarkDown_Java%20SE.assets/%E3%80%8A%E6%B7%B1%E5%85%A5%E7%90%86%E8%A7%A3%E8%99%9A%E6%8B%9F%E6%9C%BA%E3%80%8B%E7%AC%AC%E4%B8%89%E7%89%88%E7%9A%84%E7%AC%AC2%E7%AB%A0-%E8%99%9A%E6%8B%9F%E6%9C%BA%E6%A0%88.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/《深入理解虚拟机》第三版的第2章-虚拟机栈.png)

Java 虚拟机栈也是线程私有的，每个线程都有各自的 Java 虚拟机栈，而且随着线程的创建而创建，随着线程的死亡而死亡。

#### StackOverflowError与OutOfMemoryError区别

如果在一个线程计算过程中不允许有更大的本地方法栈，那么JVM就抛出StackOverflowError

```java
package Test;

import java.util.ArrayList;
import java.util.List;

public class TestError {
    public int stackSize = 0;

    public void stackSizeIncrease() {
        stackSize++;
        stackSizeIncrease();
    }

    public static void main(String[] args) {
        TestError testError = new TestError();
        try {
            testError.stackSizeIncrease();
        }catch (Throwable e){
            System.out.println(testError.stackSize);
            e.printStackTrace();
            //31577
            //java.lang.StackOverflowError
            //	at Test.TestError.stackSizeIncrease(TestError.java:11)
        }
    }
}

```



如果本地方法栈可以动态地扩展，并且本地方法栈尝试过扩展了，但是没有足够的内容分配给它，再或者没有足够的内存为线程建立初始化本地方法栈，那么JVM抛出的就是OutOfMemoryError。

```java
package Test;

import java.util.ArrayList;
import java.util.List;

public class TestError {
    List list = new ArrayList();
    int i = 0;

    public void UsedMemoryIncrease() {
        while (true) {
            list.add(i++);
            list.add(list);
        }
    }

    public static void main(String[] args) {
        TestError testError = new TestError();
        try {
            testError.UsedMemoryIncrease();
        } catch (Throwable e) {
            System.out.println(testError.i);
            e.printStackTrace();
            //118278681
            //java.lang.OutOfMemoryError: Java heap space
        }
    }
}

```

堆这里最容易出现的就是 OutOfMemoryError 错误，并且出现这种错误之后的表现形式还会有几种，比如：

1. **`OutOfMemoryError: GC Overhead Limit Exceeded`** ： 当JVM花太多时间执行垃圾回收并且只能回收很少的堆空间时，就会发生此错误。
2. **`java.lang.OutOfMemoryError: Java heap space`** :假如在创建新的对象时, 堆内存中的空间不足以存放新创建的对象, 就会引发`java.lang.OutOfMemoryError: Java heap space` 错误。(和本机物理内存无关，和你配置的内存大小有关！)







**扩展：那么方法/函数如何调用？**

Java 栈可用类比数据结构中栈，Java 栈中保存的主要内容是栈帧，每一次函数调用都会有一个对应的栈帧被压入 Java 栈，每一个函数调用结束后，都会有一个栈帧被弹出。

Java 方法有两种返回方式：

1. return 语句。
2. 抛出异常。

不管哪种返回方式都会导致栈帧被弹出。

#### 本地方法栈

和虚拟机栈所发挥的作用非常相似，区别是： **虚拟机栈为虚拟机执行 Java 方法 （也就是字节码）服务，而本地方法栈则为虚拟机使用到的 Native 方法服务。** 在 HotSpot 虚拟机中和 Java 虚拟机栈合二为一。

本地方法被执行的时候，在本地方法栈也会创建一个栈帧，用于存放该本地方法的局部变量表、操作数栈、动态链接、出口信息。

方法执行完毕后相应的栈帧也会出栈并释放内存空间，也会出现 `StackOverFlowError` 和 `OutOfMemoryError` 两种错误。





#### 栈帧

![image-20210316142233479](MarkDown_Java%20SE.assets/image-20210316142233479.png)







关于「栈帧」，我们在看看《Java虚拟机规范》中的描述：



栈帧是用来存储数据和部分过程结果的数据结构，同时也用来处理动态连接、方法返回值和异常分派。
栈帧随着方法调用而创建，随着方法结束而销毁——无论方法正常完成还是异常完成都算作方法结束。
栈帧的存储空间由创建它的线程分配在Java虚拟机栈之中，每一个栈帧都有自己的本地变量表(局部变量表)、操作数栈和指向当前方法所属的类的运行时常量池的引用。



##### 局部变量表(Local Variable Table)

是一组变量值存储空间，用于存放方法参数和方法内定义的局部变量。一个局部变量可以保存一个类型为boolean、byte、char、short、int、float、reference和returnAddress类型的数据。reference类型表示对一个对象实例的引用。returnAddress类型是为jsr、jsr_w和ret指令服务的，目前已经很少使用了。



##### 操作数栈(Operand Stack)

也常称为操作栈，它是一个后入先出栈(LIFO)。操作数栈的每一个元素可以是任意Java数据类型，32位的数据类型占一个栈容量，64位的数据类型占2个栈容量。当一个方法刚刚开始执行时，其操作数栈是空的，随着方法执行和字节码指令的执行，会从局部变量表或对象实例的字段中复制常量或变量写入到操作数栈，再随着计算的进行将栈中元素出栈到局部变量表或者返回给方法调用者，也就是出栈/入栈操作。一个完整的方法执行期间往往包含多个这样出栈/入栈的过程。



##### 动态链接(Dynamic Linking)

每个栈帧都保存了 一个 可以指向当前方法所在类的 运行时常量池, 目的是: 当前方法中如果需要调用其他方法的时候, 能够从运行时常量池中找到对应的符号引用, 然后将符号引用转换为直接引用,然后就能直接调用对应方法, 这就是动态链接

不是所有方法调用都需要动态链接的, 有一部分符号引用会在 类加载 解析阶段, 将符号引用转换为直接引用, 这部分操作称之为: 静态解析. 就是编译期间就能确定调用的版本, 包括: 调用静态方法, 调用实例的私有构造器, 私有方法, 父类方法

http://www.360doc.com/content/17/0702/18/43088713_668245796.shtml



##### **方法返回**地址

当一个方法开始执行时，可能有两种方式退出该方法：正常完成出口、异常完成出口

正常完成出口是指方法正常完成并退出，没有抛出任何异常(包括Java虚拟机异常以及执行时通过throw语句显示抛出的异常)。如果当前方法正常完成，则根据当前方法返回的字节码指令，这时有可能会有返回值传递给方法调用者(调用它的方法)，或者无返回值。具体是否有返回值以及返回值的数据类型将根据该方法返回的字节码指令确定。

异常完成出口是指方法执行过程中遇到异常，并且这个异常在方法体内部没有得到处理，导致方法退出。

无论是Java虚拟机抛出的异常还是代码中使用throw指令产生的异常，只要在本方法的异常表中没有搜索到相应的异常处理器，就会导致方法退出。
无论方法采用何种方式退出，在方法退出后都需要返回到方法被调用的位置，程序才能继续执行，方法返回时可能需要在当前栈帧中保存一些信息，用来帮他恢复它的上层方法执行状态。

方法退出过程实际上就等同于把当前栈帧出栈，因此退出可以执行的操作有：恢复上层方法的局部变量表和操作数栈，把返回值(如果有的话)压如调用者的操作数栈中，调整PC计数器的值以指向方法调用指令后的下一条指令。
 一般来说，方法正常退出时，调用者的PC计数值可以作为返回地址，栈帧中可能保存此计数值。而方法异常退出时，返回地址是通过异常处理器表确定的，栈帧中一般不会保存此部分信息。



#### 堆

Java 虚拟机所管理的内存中最大的一块，Java 堆是所有线程共享的一块内存区域，在虚拟机启动时创建。**此内存区域的唯一目的就是存放对象实例，几乎所有的对象实例以及数组都在这里分配内存。**

**Java世界中“几乎”所有的对象都在堆中分配，但是，随着JIT编译期的发展与逃逸分析技术逐渐成熟，栈上分配、标量替换优化技术将会导致一些微妙的变化，所有的对象都分配到堆上也渐渐变得不那么“绝对”了。从jdk 1.7开始已经默认开启逃逸分析，如果某些方法中的对象引用没有被返回或者未被外面使用（也就是未逃逸出去），那么对象可以直接在栈上分配内存。**

Java 堆是垃圾收集器管理的主要区域，因此也被称作**GC 堆（Garbage Collected Heap）**.从垃圾回收的角度，由于现在收集器基本都采用分代垃圾收集算法，所以 Java 堆还可以细分为：新生代和老年代：再细致一点有：Eden 空间、From Survivor、To Survivor 空间等。**进一步划分的目的是更好地回收内存，或者更快地分配内存。**



在 JDK 7 版本及JDK 7 版本之前，堆内存被通常被分为下面三部分：

1. 新生代内存(Young Generation)
2. 老生代(Old Generation)
3. 永生代(Permanent Generation)

[![JVM堆内存结构-JDK7](MarkDown_Java%20SE.assets/JVM%E5%A0%86%E5%86%85%E5%AD%98%E7%BB%93%E6%9E%84-JDK7.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/JVM堆内存结构-JDK7.png)

JDK 8 版本之后方法区（HotSpot 的永久代）被彻底移除了（JDK1.7 就已经开始了），取而代之是元空间，元空间使用的是直接内存。

[![JVM堆内存结构-JDK8](MarkDown_Java%20SE.assets/JVM%E5%A0%86%E5%86%85%E5%AD%98%E7%BB%93%E6%9E%84-jdk8.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/JVM堆内存结构-jdk8.png)

**上图所示的 Eden 区、两个 Survivor 区都属于新生代（为了区分，这两个 Survivor 区域按照顺序被命名为 from 和 to），中间一层属于老年代。**

大部分情况，对象都会首先在 Eden 区域分配，在一次新生代垃圾回收后，如果对象还存活，则会进入 s0 或者 s1，并且对象的年龄还会加 1(Eden 区->Survivor 区后对象的初始年龄变为 1)，当它的年龄增加到一定程度（默认为 15 岁），就会被晋升到老年代中。对象晋升到老年代的年龄阈值，可以通过参数 `-XX:MaxTenuringThreshold` 来设置。

> 修正（[issue552](https://github.com/Snailclimb/JavaGuide/issues/552)）：“Hotspot遍历所有对象时，按照年龄从小到大对其所占用的大小进行累积，当累积的某个年龄大小超过了survivor区的一半时，取这个年龄和MaxTenuringThreshold中更小的一个值，作为新的晋升年龄阈值”。
>
> **动态年龄计算的代码如下**
>
> ```
> uint ageTable::compute_tenuring_threshold(size_t survivor_capacity) {
> 	//survivor_capacity是survivor空间的大小
>   size_t desired_survivor_size = (size_t)((((double) survivor_capacity)*TargetSurvivorRatio)/100);
>   size_t total = 0;
>   uint age = 1;
>   while (age < table_size) {
>     total += sizes[age];//sizes数组是每个年龄段对象大小
>     if (total > desired_survivor_size) break;
>     age++;
>   }
>   uint result = age < MaxTenuringThreshold ? age : MaxTenuringThreshold;
> 	...
> }
> ```

堆这里最容易出现的就是 OutOfMemoryError 错误，并且出现这种错误之后的表现形式还会有几种，比如：

1. **`OutOfMemoryError: GC Overhead Limit Exceeded`** ： 当JVM花太多时间执行垃圾回收并且只能回收很少的堆空间时，就会发生此错误。
2. **`java.lang.OutOfMemoryError: Java heap space`** :假如在创建新的对象时, 堆内存中的空间不足以存放新创建的对象, 就会引发`java.lang.OutOfMemoryError: Java heap space` 错误。(和本机物理内存无关，和你配置的内存大小有关！)



#### 方法区

方法区与 Java 堆一样，是各个线程共享的内存区域，它用于存储已被虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码等数据。虽然 **Java 虚拟机规范把方法区描述为堆的一个逻辑部分**，但是它却有一个别名叫做 **Non-Heap（非堆）**，目的应该是与 Java 堆区分开来。

方法区也被称为永久代。很多人都会分不清方法区和永久代的关系，为此我也查阅了文献。

##### 1 方法区和永久代的关系

> 《Java 虚拟机规范》只是规定了有方法区这么个概念和它的作用，并没有规定如何去实现它。那么，在不同的 JVM 上方法区的实现肯定是不同的了。 **方法区和永久代的关系很像 Java 中接口和类的关系，类实现了接口，而永久代就是 HotSpot 虚拟机对虚拟机规范中方法区的一种实现方式。** 也就是说，永久代是 HotSpot 的概念，方法区是 Java 虚拟机规范中的定义，是一种规范，而永久代是一种实现，一个是标准一个是实现，其他的虚拟机实现并没有永久代这一说法。

##### 2 常用参数

JDK 1.8 之前永久代还没被彻底移除的时候通常通过下面这些参数来调节方法区大小

```
-XX:PermSize=N //方法区 (永久代) 初始大小
-XX:MaxPermSize=N //方法区 (永久代) 最大大小,超过这个值将会抛出 OutOfMemoryError 异常:java.lang.OutOfMemoryError: PermGen
```

相对而言，垃圾收集行为在这个区域是比较少出现的，但并非数据进入方法区后就“永久存在”了。

JDK 1.8 的时候，方法区（HotSpot 的永久代）被彻底移除了（JDK1.7 就已经开始了），取而代之是元空间，元空间使用的是直接内存。

下面是一些常用参数：

```
-XX:MetaspaceSize=N //设置 Metaspace 的初始（和最小大小）
-XX:MaxMetaspaceSize=N //设置 Metaspace 的最大大小
```

与永久代很大的不同就是，如果不指定大小的话，随着更多类的创建，虚拟机会耗尽所有可用的系统内存。

##### 3 为什么要将永久代 (PermGen) 替换为元空间 (MetaSpace) 呢?

1. 整个永久代有一个 JVM 本身设置固定大小上限，无法进行调整，而元空间使用的是直接内存，受本机可用内存的限制，虽然元空间仍旧可能溢出，但是比原来出现的几率会更小。

> 当你元空间溢出时会得到如下错误： `java.lang.OutOfMemoryError: MetaSpace`

你可以使用 `-XX：MaxMetaspaceSize` 标志设置最大元空间大小，默认值为 unlimited，这意味着它只受系统内存的限制。`-XX：MetaspaceSize` 调整标志定义元空间的初始大小如果未指定此标志，则 Metaspace 将根据运行时的应用程序需求动态地重新调整大小。元空间里面存放的是类的元数据，这样加载多少类的元数据就不由 `MaxPermSize` 控制了, 而由系统的实际可用空间来控制，这样能加载的类就更多了。



#### 运行时常量池

运行时常量池是方法区的一部分。Class 文件中除了有类的版本、字段、方法、接口等描述信息外，还有常量池表（用于存放编译期生成的各种字面量和符号引用）

既然运行时常量池是方法区的一部分，自然受到方法区内存的限制，当常量池无法再申请到内存时会抛出 OutOfMemoryError 错误。

1. **JDK1.7之前运行时常量池逻辑包含字符串常量池存放在方法区, 此时hotspot虚拟机对方法区的实现为永久代**
2. **JDK1.7 字符串常量池被从方法区拿到了堆中, 这里没有提到运行时常量池,也就是说字符串常量池被单独拿到堆,运行时常量池剩下的东西还在方法区, 也就是hotspot中的永久代** 。
3. **JDK1.8 hotspot移除了永久代用元空间(Metaspace)取而代之, 这时候字符串常量池还在堆, 运行时常量池还在方法区, 只不过方法区的实现从永久代变成了元空间(Metaspace)**

#### 直接内存

**直接内存并不是虚拟机运行时数据区的一部分，也不是虚拟机规范中定义的内存区域，但是这部分内存也被频繁地使用。而且也可能导致 OutOfMemoryError 错误出现。**

JDK1.4 中新加入的 **NIO(New Input/Output) 类**，引入了一种基于**通道（Channel）** 与**缓存区（Buffer）** 的 I/O 方式，它可以直接使用 Native 函数库直接分配堆外内存，然后通过一个存储在 Java 堆中的 DirectByteBuffer 对象作为这块内存的引用进行操作。这样就能在一些场景中显著提高性能，因为**避免了在 Java 堆和 Native 堆之间来回复制数据**。

本机直接内存的分配不会受到 Java 堆的限制，但是，既然是内存就会受到本机总内存大小以及处理器寻址空间的限制。



### HotSpot 虚拟机对象探秘



#### 对象的创建

下图便是 Java 对象的创建过程，我建议最好是能默写出来，并且要掌握每一步在做什么。 [![Java创建对象的过程](MarkDown_Java%20SE.assets/Java%E5%88%9B%E5%BB%BA%E5%AF%B9%E8%B1%A1%E7%9A%84%E8%BF%87%E7%A8%8B.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/Java创建对象的过程.png)

##### Step1:类加载检查

虚拟机遇到一条 new 指令时，首先将去检查这个指令的参数是否能在常量池中定位到这个类的符号引用，并且检查这个符号引用代表的类是否已被加载过、解析和初始化过。如果没有，那必须先执行相应的类加载过程。



##### Step2:分配内存

在**类加载检查**通过后，接下来虚拟机将为新生对象**分配内存**。对象所需的内存大小在类加载完成后便可确定，为对象分配空间的任务等同于把一块确定大小的内存从 Java 堆中划分出来。**分配方式**有 **“指针碰撞”** 和 **“空闲列表”** 两种，**选择哪种分配方式由 Java 堆是否规整决定，而 Java 堆是否规整又由所采用的垃圾收集器是否带有压缩整理功能决定**。

**内存分配的两种方式：（补充内容，需要掌握）**

选择以上两种方式中的哪一种，取决于 Java 堆内存是否规整。而 Java 堆内存是否规整，取决于 GC 收集器的算法是"标记-清除"，还是"标记-整理"（也称作"标记-压缩"），值得注意的是，复制算法内存也是规整的

[![内存分配的两种方式](MarkDown_Java%20SE.assets/%E5%86%85%E5%AD%98%E5%88%86%E9%85%8D%E7%9A%84%E4%B8%A4%E7%A7%8D%E6%96%B9%E5%BC%8F.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/内存分配的两种方式.png)

**内存分配并发问题（补充内容，需要掌握）**

在创建对象的时候有一个很重要的问题，就是线程安全，因为在实际开发过程中，创建对象是很频繁的事情，作为虚拟机来说，必须要保证线程是安全的，通常来讲，虚拟机采用两种方式来保证线程安全：

- **CAS+失败重试：** CAS 是乐观锁的一种实现方式。所谓乐观锁就是，每次不加锁而是假设没有冲突而去完成某项操作，如果因为冲突失败就重试，直到成功为止。**虚拟机采用 CAS 配上失败重试的方式保证更新操作的原子性。**
- **TLAB：** 为每一个线程预先在 Eden 区分配一块儿内存，JVM 在给线程中的对象分配内存时，首先在 TLAB 分配，当对象大于 TLAB 中的剩余内存或 TLAB 的内存已用尽时，再采用上述的 CAS 进行内存分配



##### Step3:初始化零值

内存分配完成后，虚拟机需要将分配到的内存空间都初始化为零值（不包括对象头），这一步操作保证了对象的实例字段在 Java 代码中可以不赋初始值就直接使用，程序能访问到这些字段的数据类型所对应的零值。



##### Step4:设置对象头

初始化零值完成之后，**虚拟机要对对象进行必要的设置**，例如这个对象是哪个类的实例、如何才能找到类的元数据信息、对象的哈希码、对象的 GC 分代年龄等信息。 **这些信息存放在对象头中。** 另外，根据虚拟机当前运行状态的不同，如是否启用偏向锁等，对象头会有不同的设置方式。



##### Step5:执行 init 方法

在上面工作都完成之后，从虚拟机的视角来看，一个新的对象已经产生了，但从 Java 程序的视角来看，对象创建才刚开始，`<init>` 方法还没有执行，所有的字段都还为零。所以一般来说，执行 new 指令之后会接着执行 `<init>` 方法，把对象按照程序员的意愿进行初始化，这样一个真正可用的对象才算完全产生出来。



#### 对象的内存布局

在 Hotspot 虚拟机中，对象在内存中的布局可以分为 3 块区域：**对象头**、**实例数据**和**对齐填充**。

**Hotspot 虚拟机的对象头包括两部分信息**，**第一部分用于存储对象自身的运行时数据**（哈希码、GC 分代年龄、锁状态标志等等），**另一部分是类型指针**，即对象指向它的类元数据的指针，虚拟机通过这个指针来确定这个对象是哪个类的实例。

**实例数据部分是对象真正存储的有效信息**，也是在程序中所定义的各种类型的字段内容。

**对齐填充部分不是必然存在的，也没有什么特别的含义，仅仅起占位作用。** 因为 Hotspot 虚拟机的自动内存管理系统要求对象起始地址必须是 8 字节的整数倍，换句话说就是对象的大小必须是 8 字节的整数倍。而对象头部分正好是 8 字节的倍数（1 倍或 2 倍），因此，当对象实例数据部分没有对齐时，就需要通过对齐填充来补全。



#### 对象的访问定位

建立对象就是为了使用对象，我们的 Java 程序通过栈上的 reference 数据来操作堆上的具体对象。对象的访问方式由虚拟机实现而定，目前主流的访问方式有**①使用句柄**和**②直接指针**两种：

1. **句柄：** 如果使用句柄的话，那么 Java 堆中将会划分出一块内存来作为句柄池，reference 中存储的就是对象的句柄地址，而句柄中包含了对象实例数据与类型数据各自的具体地址信息；

   [![对象的访问定位-使用句柄](MarkDown_Java%20SE.assets/%E5%AF%B9%E8%B1%A1%E7%9A%84%E8%AE%BF%E9%97%AE%E5%AE%9A%E4%BD%8D-%E4%BD%BF%E7%94%A8%E5%8F%A5%E6%9F%84.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/对象的访问定位-使用句柄.png)

2. **直接指针：** 如果使用直接指针访问，那么 Java 堆对象的布局中就必须考虑如何放置访问类型数据的相关信息，而 reference 中存储的直接就是对象的地址。

[![对象的访问定位-直接指针](MarkDown_Java%20SE.assets/%E5%AF%B9%E8%B1%A1%E7%9A%84%E8%AE%BF%E9%97%AE%E5%AE%9A%E4%BD%8D-%E7%9B%B4%E6%8E%A5%E6%8C%87%E9%92%88.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/对象的访问定位-直接指针.png)

**这两种对象访问方式各有优势。使用句柄来访问的最大好处是 reference 中存储的是稳定的句柄地址，在对象被移动时只会改变句柄中的实例数据指针，而 reference 本身不需要修改。使用直接指针访问方式最大的好处就是速度快，它节省了一次指针定位的时间开销。**



### String 类和常量池

**String 对象的两种创建方式：**

```
String str1 = "abcd";//先检查字符串常量池中有没有"abcd"，如果字符串常量池中没有，则创建一个，然后 str1 指向字符串常量池中的对象，如果有，则直接将 str1 指向"abcd""；
String str2 = new String("abcd");//堆中创建一个新的对象
String str3 = new String("abcd");//堆中创建一个新的对象
System.out.println(str1==str2);//false
System.out.println(str2==str3);//false
```

这两种不同的创建方法是有差别的。

- 第一种方式是在常量池中拿对象；
- 第二种方式是直接在堆内存空间创建一个新的对象。

记住一点：**只要使用 new 方法，便需要创建新的对象。**

再给大家一个图应该更容易理解，图片来源：https://www.journaldev.com/797/what-is-java-string-pool：

[![String-Pool-Java](MarkDown_Java%20SE.assets/2019-3String-Pool-Java1-450x249.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/2019-3String-Pool-Java1-450x249.png)

**String 类型的常量池比较特殊。它的主要使用方法有两种：**

- 直接使用双引号声明出来的 String 对象会直接存储在常量池中。
- 如果不是用双引号声明的 String 对象，可以使用 String 提供的 intern 方法。String.intern() 是一个 Native 方法，它的作用是：如果运行时常量池中已经包含一个等于此 String 对象内容的字符串，则返回常量池中该字符串的引用；如果没有，JDK1.7之前（不包含1.7）的处理方式是在常量池中创建与此 String 内容相同的字符串，并返回常量池中创建的字符串的引用，JDK1.7以及之后的处理方式是在常量池中记录此字符串的引用，并返回该引用。

```
	      String s1 = new String("计算机");
	      String s2 = s1.intern();
	      String s3 = "计算机";
	      System.out.println(s2);//计算机
	      System.out.println(s1 == s2);//false，因为一个是堆内存中的 String 对象一个是常量池中的 String 对象，
	      System.out.println(s3 == s2);//true，因为两个都是常量池中的 String 对象
```



**字符串拼接:**

```
		  String str1 = "str";
		  String str2 = "ing";
		 
		  String str3 = "str" + "ing";//常量池中的对象
		  String str4 = str1 + str2; //在堆上创建的新的对象	  
		  String str5 = "string";//常量池中的对象
		  System.out.println(str3 == str4);//false
		  System.out.println(str3 == str5);//true
		  System.out.println(str4 == str5);//false
```

[![字符串拼接](MarkDown_Java%20SE.assets/%E5%AD%97%E7%AC%A6%E4%B8%B2%E6%8B%BC%E6%8E%A5-%E5%B8%B8%E9%87%8F%E6%B1%A02.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/java内存区域/字符串拼接-常量池2.png)

尽量避免多个字符串拼接，因为这样会重新创建对象。如果需要改变字符串的话，可以使用 StringBuilder 或者 StringBuffer。

#### String s1 = new String("abc");这句话创建了几个字符串对象？

将创建 1 或 2 个字符串。如果池中已存在字符串常量“abc”，则只会在堆空间创建一个字符串常量“abc”。如果池中没有字符串常量“abc”，那么它将首先在池中创建，然后在堆空间中创建，因此将创建总共 2 个字符串对象。

**验证：**

```java
		String s1 = new String("abc");// 堆内存的地址值
		String s2 = "abc";
		System.out.println(s1 == s2);// 输出 false,因为一个是堆内存，一个是常量池的内存，故两者是不同的。
		System.out.println(s1.equals(s2));// 输出 true
```

**结果：**

```java
false
true
```



### 8 种基本类型的包装类和常量池

**Java 基本类型的包装类的大部分都实现了常量池技术，即 Byte,Short,Integer,Long,Character,Boolean；前面 4 种包装类默认创建了数值[-128，127] 的相应类型的缓存数据，Character创建了数值在[0,127]范围的缓存数据，Boolean 直接返回True Or False。如果超出对应范围仍然会去创建新的对象。** 为啥把缓存设置为[-128，127]区间？（[参见issue/461](https://github.com/Snailclimb/JavaGuide/issues/461)）性能和资源之间的权衡。

```java
public static Boolean valueOf(boolean b) {
    return (b ? TRUE : FALSE);
}
private static class CharacterCache {         
    private CharacterCache(){}
          
    static final Character cache[] = new Character[127 + 1];          
    static {             
        for (int i = 0; i < cache.length; i++)                 
            cache[i] = new Character((char)i);         
    }   
}
```

两种浮点数类型的包装类 Float,Double 并没有实现常量池技术。

```java
		Integer i1 = 33;
		Integer i2 = 33;
		System.out.println(i1 == i2);// 输出 true
		Integer i11 = 333;
		Integer i22 = 333;
		System.out.println(i11 == i22);// 输出 false
		Double i3 = 1.2;
		Double i4 = 1.2;
		System.out.println(i3 == i4);// 输出 false
```



#### **Integer 缓存源代码：**

```java
/**
*此方法将始终缓存-128 到 127（包括端点）范围内的值，并可以缓存此范围之外的其他值。
*/
    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }
```

**应用场景：**

1. Integer i1=40；Java 在编译的时候会直接将代码封装成 Integer i1=Integer.valueOf(40);，从而使用常量池中的对象。
2. Integer i1 = new Integer(40);这种情况下会创建新的对象。

```java
  Integer i1 = 40;
  Integer i2 = new Integer(40);
  System.out.println(i1==i2);//输出 false
```

**Integer 比较更丰富的一个例子:**

```java
  Integer i1 = 40;
  Integer i2 = 40;
  Integer i3 = 0;
  Integer i4 = new Integer(40);
  Integer i5 = new Integer(40);
  Integer i6 = new Integer(0);
  
  System.out.println("i1=i2   " + (i1 == i2));
  System.out.println("i1=i2+i3   " + (i1 == i2 + i3));
  System.out.println("i1=i4   " + (i1 == i4));
  System.out.println("i4=i5   " + (i4 == i5));
  System.out.println("i4=i5+i6   " + (i4 == i5 + i6));   
  System.out.println("40=i5+i6   " + (40 == i5 + i6));     
```

结果：

```java
i1=i2   true
i1=i2+i3   true
i1=i4   false
i4=i5   false
i4=i5+i6   true
40=i5+i6   true
```

解释：

语句 i4 == i5 + i6，因为+这个操作符不适用于 Integer 对象，首先 i5 和 i6 进行自动拆箱操作，进行数值相加，即 i4 == 40。然后 Integer 对象无法与数值进行直接比较，所以 i4 自动拆箱转为 int 值 40，最终这条语句转为 40 == 40 进行数值比较。







### GC垃圾回收

Java 的自动内存管理主要是针对对象内存的回收和对象内存的分配。同时，Java 自动内存管理最核心的功能是 **堆** 内存中对象的分配与回收。

Java 堆是垃圾收集器管理的主要区域，因此也被称作**GC 堆（Garbage Collected Heap）**.从垃圾回收的角度，由于现在收集器基本都采用分代垃圾收集算法，所以 Java 堆还可以细分为：新生代和老年代：再细致一点有：Eden 空间、From Survivor、To Survivor 空间等。**进一步划分的目的是更好地回收内存，或者更快地分配内存。**

**堆空间的基本结构：**

[![img](MarkDown_Java%20SE.assets/01d330d8-2710-4fad-a91c-7bbbfaaefc0e.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/01d330d8-2710-4fad-a91c-7bbbfaaefc0e.png)

上图所示的 Eden 区、From Survivor0("From") 区、To Survivor1("To") 区都属于新生代，Old Memory 区属于老年代。

大部分情况，对象都会首先在 Eden 区域分配，在一次新生代垃圾回收后，如果对象还存活，则会进入 s0 或者 s1，并且对象的年龄还会加 1(Eden 区->Survivor 区后对象的初始年龄变为 1)，当它的年龄增加到一定程度（默认为 15 岁），就会被晋升到老年代中。对象晋升到老年代的年龄阈值，可以通过参数 `-XX:MaxTenuringThreshold` 来设置。

> 修正（[issue552](https://github.com/Snailclimb/JavaGuide/issues/552)）：“Hotspot 遍历所有对象时，按照年龄从小到大对其所占用的大小进行累积，当累积的某个年龄大小超过了 survivor 区的一半时，取这个年龄和 MaxTenuringThreshold 中更小的一个值，作为新的晋升年龄阈值”。
>
> **动态年龄计算的代码如下**
>
> ```java
> uint ageTable::compute_tenuring_threshold(size_t survivor_capacity) {
>     //survivor_capacity是survivor空间的大小
>     size_t desired_survivor_size = (size_t)((((double)survivor_capacity)*TargetSurvivorRatio)/100);
>     size_t total = 0;
>     uint age = 1;
>     while (age < table_size) {
>         //sizes数组是每个年龄段对象大小
>         total += sizes[age];
>         if (total > desired_survivor_size) {
>             break;
>         }
>         age++;
>     }
>     uint result = age < MaxTenuringThreshold ? age : MaxTenuringThreshold;
>     ...
> }
> ```

经过这次 GC 后，Eden 区和"From"区已经被清空。这个时候，"From"和"To"会交换他们的角色，也就是新的"To"就是上次 GC 前的“From”，新的"From"就是上次 GC 前的"To"。不管怎样，都会保证名为 To 的 Survivor 区域是空的。Minor GC 会一直重复这样的过程，直到“To”区被填满，"To"区被填满之后，会将所有对象移动到老年代中。

[![堆内存常见分配策略 ](MarkDown_Java%20SE.assets/%E5%A0%86%E5%86%85%E5%AD%98.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/堆内存.png)



#### 1.1 对象优先在 eden 区分配

目前主流的垃圾收集器都会采用分代回收算法，因此需要将堆内存分为新生代和老年代，这样我们就可以根据各个年代的特点选择合适的垃圾收集算法。

大多数情况下，对象在新生代中 eden 区分配。当 eden 区没有足够空间进行分配时，虚拟机将发起一次 Minor GC.



#### 1.2 大对象直接进入老年代

大对象就是需要大量连续内存空间的对象（比如：字符串、数组）。

**为什么要这样呢？**

为了避免为大对象分配内存时由于分配担保机制带来的复制而降低效率。



#### 1.3 长期存活的对象将进入老年代

既然虚拟机采用了分代收集的思想来管理内存，那么内存回收时就必须能识别哪些对象应放在新生代，哪些对象应放在老年代中。为了做到这一点，虚拟机给每个对象一个对象年龄（Age）计数器。

如果对象在 Eden 出生并经过第一次 Minor GC 后仍然能够存活，并且能被 Survivor 容纳的话，将被移动到 Survivor 空间中，并将对象年龄设为 1.对象在 Survivor 中每熬过一次 MinorGC,年龄就增加 1 岁，当它的年龄增加到一定程度（默认为 15 岁），就会被晋升到老年代中。对象晋升到老年代的年龄阈值，可以通过参数 `-XX:MaxTenuringThreshold` 来设置。



#### 1.4 动态对象年龄判定

大部分情况，对象都会首先在 Eden 区域分配，在一次新生代垃圾回收后，如果对象还存活，则会进入 s0 或者 s1，并且对象的年龄还会加 1(Eden 区->Survivor 区后对象的初始年龄变为 1)，当它的年龄增加到一定程度（默认为 15 岁），就会被晋升到老年代中。对象晋升到老年代的年龄阈值，可以通过参数 `-XX:MaxTenuringThreshold` 来设置。

> 修正（[issue552](https://github.com/Snailclimb/JavaGuide/issues/552)）：“Hotspot 遍历所有对象时，按照年龄从小到大对其所占用的大小进行累积，当累积的某个年龄大小超过了 survivor 区的一半时，取这个年龄和 MaxTenuringThreshold 中更小的一个值，作为新的晋升年龄阈值”。
>
> **动态年龄计算的代码如下**
>
> ```
> uint ageTable::compute_tenuring_threshold(size_t survivor_capacity) {
>     //survivor_capacity是survivor空间的大小
>     size_t desired_survivor_size = (size_t)((((double)survivor_capacity)*TargetSurvivorRatio)/100);
>     size_t total = 0;
>     uint age = 1;
>     while (age < table_size) {
>         //sizes数组是每个年龄段对象大小
>         total += sizes[age];
>         if (total > desired_survivor_size) {
>             break;
>         }
>         age++;
>     }
>     uint result = age < MaxTenuringThreshold ? age : MaxTenuringThreshold;
>     ...
> }
> ```
>
> 额外补充说明([issue672](https://github.com/Snailclimb/JavaGuide/issues/672))：**关于默认的晋升年龄是 15，这个说法的来源大部分都是《深入理解 Java 虚拟机》这本书。** 如果你去 Oracle 的官网阅读[相关的虚拟机参数](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html)，你会发现`-XX:MaxTenuringThreshold=threshold`这里有个说明
>
> **Sets the maximum tenuring threshold for use in adaptive GC sizing. The largest value is 15. The default value is 15 for the parallel (throughput) collector, and 6 for the CMS collector.默认晋升年龄并不都是 15，这个是要区分垃圾收集器的，CMS 就是 6.**



#### 1.5 主要进行 gc 的区域

周志明先生在《深入理解 Java 虚拟机》第二版中 P92 如是写道：

![image-20210316161642557](MarkDown_Java%20SE.assets/image-20210316161642557.png)



#### 2 对象已经死亡？

堆中几乎放着所有的对象实例，对堆垃圾回收前的第一步就是要判断哪些对象已经死亡（即不能再被任何途径使用的对象）。

[![img](MarkDown_Java%20SE.assets/11034259.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/11034259.png)



#### 2.1 引用计数法

给对象中添加一个引用计数器，每当有一个地方引用它，计数器就加 1；当引用失效，计数器就减 1；任何时候计数器为 0 的对象就是不可能再被使用的。

**这个方法实现简单，效率高，但是目前主流的虚拟机中并没有选择这个算法来管理内存，其最主要的原因是它很难解决对象之间相互循环引用的问题。** 所谓对象之间的相互引用问题，如下面代码所示：除了对象 objA 和 objB 相互引用着对方之外，这两个对象之间再无任何引用。但是他们因为互相引用对方，导致它们的引用计数器都不为 0，于是引用计数算法无法通知 GC 回收器回收他们。

```
public class ReferenceCountingGc {
    Object instance = null;
	public static void main(String[] args) {
		ReferenceCountingGc objA = new ReferenceCountingGc();
		ReferenceCountingGc objB = new ReferenceCountingGc();
		objA.instance = objB;
		objB.instance = objA;
		objA = null;
		objB = null;

	}
}
```

#### 2.2 可达性分析算法

这个算法的基本思想就是通过一系列的称为 **“GC Roots”** 的对象作为起点，从这些节点开始向下搜索，节点所走过的路径称为引用链，当一个对象到 GC Roots 没有任何引用链相连的话，则证明此对象是不可用的。

[![可达性分析算法 ](MarkDown_Java%20SE.assets/72762049.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/72762049.png)

可作为 GC Roots 的对象包括下面几种:

- 虚拟机栈(栈帧中的本地变量表)中引用的对象
- 本地方法栈(Native 方法)中引用的对象
- 方法区中类静态属性引用的对象
- 方法区中常量引用的对象
- 所有被同步锁持有的对象



#### 2.3 再谈引用

无论是通过引用计数法判断对象引用数量，还是通过可达性分析法判断对象的引用链是否可达，判定对象的存活都与“引用”有关。

JDK1.2 之前，Java 中引用的定义很传统：如果 reference 类型的数据存储的数值代表的是另一块内存的起始地址，就称这块内存代表一个引用。

JDK1.2 以后，Java 对引用的概念进行了扩充，将引用分为强引用、软引用、弱引用、虚引用四种（引用强度逐渐减弱）

**1．强引用（StrongReference）**

以前我们使用的大部分引用实际上都是强引用，这是使用最普遍的引用。如果一个对象具有强引用，那就类似于**必不可少的生活用品**，垃圾回收器绝不会回收它。当内存空间不足，Java 虚拟机宁愿抛出 OutOfMemoryError 错误，使程序异常终止，也不会靠随意回收具有强引用的对象来解决内存不足问题。

**2．软引用（SoftReference）**

如果一个对象只具有软引用，那就类似于**可有可无的生活用品**。如果内存空间足够，垃圾回收器就不会回收它，如果内存空间不足了，就会回收这些对象的内存。只要垃圾回收器没有回收它，该对象就可以被程序使用。软引用可用来实现内存敏感的高速缓存。

软引用可以和一个引用队列（ReferenceQueue）联合使用，如果软引用所引用的对象被垃圾回收，JAVA 虚拟机就会把这个软引用加入到与之关联的引用队列中。

**3．弱引用（WeakReference）**

如果一个对象只具有弱引用，那就类似于**可有可无的生活用品**。弱引用与软引用的区别在于：只具有弱引用的对象拥有更短暂的生命周期。在垃圾回收器线程扫描它所管辖的内存区域的过程中，一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存。不过，由于垃圾回收器是一个优先级很低的线程， 因此不一定会很快发现那些只具有弱引用的对象。

弱引用可以和一个引用队列（ReferenceQueue）联合使用，如果弱引用所引用的对象被垃圾回收，Java 虚拟机就会把这个弱引用加入到与之关联的引用队列中。

**4．虚引用（PhantomReference）**

"虚引用"顾名思义，就是形同虚设，与其他几种引用都不同，虚引用并不会决定对象的生命周期。如果一个对象仅持有虚引用，那么它就和没有任何引用一样，在任何时候都可能被垃圾回收。

**虚引用主要用来跟踪对象被垃圾回收的活动**。

**虚引用与软引用和弱引用的一个区别在于：** 虚引用必须和引用队列（ReferenceQueue）联合使用。当垃圾回收器准备回收一个对象时，如果发现它还有虚引用，就会在回收对象的内存之前，把这个虚引用加入到与之关联的引用队列中。程序可以通过判断引用队列中是否已经加入了虚引用，来了解被引用的对象是否将要被垃圾回收。程序如果发现某个虚引用已经被加入到引用队列，那么就可以在所引用的对象的内存被回收之前采取必要的行动。

特别注意，在程序设计中一般很少使用弱引用与虚引用，使用软引用的情况较多，这是因为**软引用可以加速 JVM 对垃圾内存的回收速度，可以维护系统的运行安全，防止内存溢出（OutOfMemory）等问题的产生**。

#### 2.4 不可达的对象并非“非死不可”

即使在可达性分析法中不可达的对象，也并非是“非死不可”的，这时候它们暂时处于“缓刑阶段”，要真正宣告一个对象死亡，至少要经历两次标记过程；可达性分析法中不可达的对象被第一次标记并且进行一次筛选，筛选的条件是此对象是否有必要执行 finalize 方法。当对象没有覆盖 finalize 方法，或 finalize 方法已经被虚拟机调用过时，虚拟机将这两种情况视为没有必要执行。

被判定为需要执行的对象将会被放在一个队列中进行第二次标记，除非这个对象与引用链上的任何一个对象建立关联，否则就会被真的回收。



#### 2.5 如何判断一个常量是废弃常量？

运行时常量池主要回收的是废弃的常量。那么，我们如何判断一个常量是废弃常量呢？

1. **JDK1.7 之前运行时常量池逻辑包含字符串常量池存放在方法区, 此时 hotspot 虚拟机对方法区的实现为永久代**
2. **JDK1.7 字符串常量池被从方法区拿到了堆中, 这里没有提到运行时常量池,也就是说字符串常量池被单独拿到堆,运行时常量池剩下的东西还在方法区, 也就是 hotspot 中的永久代** 。
3. **JDK1.8 hotspot 移除了永久代用元空间(Metaspace)取而代之, 这时候字符串常量池还在堆, 运行时常量池还在方法区, 只不过方法区的实现从永久代变成了元空间(Metaspace)**

假如在字符串常量池中存在字符串 "abc"，如果当前没有任何 String 对象引用该字符串常量的话，就说明常量 "abc" 就是废弃常量，如果这时发生内存回收的话而且有必要的话，"abc" 就会被系统清理出常量池了。



#### 2.6 如何判断一个类是无用的类

方法区主要回收的是无用的类，那么如何判断一个类是无用的类的呢？

判定一个常量是否是“废弃常量”比较简单，而要判定一个类是否是“无用的类”的条件则相对苛刻许多。类需要同时满足下面 3 个条件才能算是 **“无用的类”** ：

- 该类所有的实例都已经被回收，也就是 Java 堆中不存在该类的任何实例。
- 加载该类的 `ClassLoader` 已经被回收。
- 该类对应的 `java.lang.Class` 对象没有在任何地方被引用，无法在任何地方通过反射访问该类的方法。

虚拟机可以对满足上述 3 个条件的无用类进行回收，这里说的仅仅是“可以”，而并不是和对象一样不使用了就会必然被回收。





#### 3 垃圾收集算法

[![垃圾收集算法分类](MarkDown_Java%20SE.assets/%E5%9E%83%E5%9C%BE%E6%94%B6%E9%9B%86%E7%AE%97%E6%B3%95.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/垃圾收集算法.png)





#### 3.1 标记-清除算法

该算法分为“标记”和“清除”阶段：首先标记出所有不需要回收的对象，在标记完成后统一回收掉所有没有被标记的对象。它是最基础的收集算法，后续的算法都是对其不足进行改进得到。这种垃圾收集算法会带来两个明显的问题：

1. **效率问题**
2. **空间问题（标记清除后会产生大量不连续的碎片）**

[![img](MarkDown_Java%20SE.assets/%E6%A0%87%E8%AE%B0-%E6%B8%85%E9%99%A4%E7%AE%97%E6%B3%95.jpeg)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/标记-清除算法.jpeg)

#### 3.2 标记-复制算法

为了解决效率问题，“标记-复制”收集算法出现了。它可以将内存分为大小相同的两块，每次使用其中的一块。当这一块的内存使用完后，就将还存活的对象复制到另一块去，然后再把使用的空间一次清理掉。这样就使每次的内存回收都是对内存区间的一半进行回收。

[![复制算法](MarkDown_Java%20SE.assets/90984624.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/90984624.png)

#### 3.3 标记-整理算法

根据老年代的特点提出的一种标记算法，标记过程仍然与“标记-清除”算法一样，但后续步骤不是直接对可回收对象回收，而是让所有存活的对象向一端移动，然后直接清理掉端边界以外的内存。

[![标记-整理算法 ](MarkDown_Java%20SE.assets/94057049.png)](https://github.com/Snailclimb/JavaGuide/blob/master/docs/java/jvm/pictures/jvm垃圾回收/94057049.png)

#### 3.4 分代收集算法

当前虚拟机的垃圾收集都采用分代收集算法，这种算法没有什么新的思想，只是根据对象存活周期的不同将内存分为几块。一般将 java 堆分为新生代和老年代，这样我们就可以根据各个年代的特点选择合适的垃圾收集算法。

**比如在新生代中，每次收集都会有大量对象死去，所以可以选择”标记-复制“算法，只需要付出少量对象的复制成本就可以完成每次垃圾收集。而老年代的对象存活几率是比较高的，而且没有额外的空间对它进行分配担保，所以我们必须选择“标记-清除”或“标记-整理”算法进行垃圾收集。**

**延伸面试问题：** 

##### HotSpot 为什么要分为新生代和老年代？

因为有的对象寿命长，有的对象寿命短。应该将寿命长的对象放在一个区，寿命短的对象放在一个区。不同的区采用不同的垃圾收集算法。寿命短的区清理频次高一点，寿命长的区清理频次低一点。提高效率。



根据上面的对分代收集算法的介绍回答。

那么，在JVM的新生代内存中，为什么除了Eden区，还要设置两个Survivor区？

##### 1 为什么要有Survivor区

如果没有Survivor，Eden区每进行一次Minor GC，存活的对象就会被送到老年代。老年代很快被填满，触发Major GC（因为Major GC一般伴随着Minor GC，也可以看做触发了Full GC）。老年代的内存空间远大于新生代，进行一次Full GC消耗的时间比Minor GC长得多。你也许会问，执行时间长有什么坏处？频发的Full GC消耗的时间是非常可观的，这一点会影响大型程序的执行和响应速度，更不要说某些连接会因为超时发生连接错误了。

我们可以得到第一条结论：Survivor的存在意义，就是减少被送到老年代的对象，进而减少Full GC的发生，Survivor的预筛选保证，只有经历16次Minor GC还能在新生代中存活的对象，才会被送到老年代。

##### 2 为什么要设置两个Survivor区

设置两个Survivor区最大的好处就是解决了碎片化，下面我们来分析一下。

![image-20210316163620737](MarkDown_Java%20SE.assets/image-20210316163620737.png)

为什么一个Survivor区不行？第一部分中，我们知道了必须设置Survivor区。假设现在只有一个survivor区，我们来模拟一下流程：

刚刚新建的对象在Eden中，一旦Eden满了，触发一次Minor GC，Eden中的存活对象就会被移动到Survivor区。这样继续循环下去，下一次Eden满了的时候，问题来了，此时进行Minor GC，Eden和Survivor各有一些存活对象，如果此时把Eden区的存活对象硬放到Survivor区，很明显这两部分对象所占有的内存是不连续的，也就导致了内存碎片化。

我绘制了一幅图来表明这个过程。其中色块代表对象，白色框分别代表Eden区（大）和Survivor区（小）。Eden区理所当然大一些，否则新建对象很快就导致Eden区满，进而触发Minor GC，有悖于初衷。

碎片化带来的风险是极大的，严重影响JAVA程序的性能。堆空间被散布的对象占据不连续的内存，最直接的结果就是，堆中没有足够大的连续内存空间，接下去如果程序需要给一个内存需求很大的对象分配内存。。。画面太美不敢看。。。这就好比我们爬山的时候，背包里所有东西紧挨着放，最后就可能省出一块完整的空间放相机。如果每件行李之间隔一点空隙乱放，很可能最后就要一路把相机挂在脖子上了。

那么，顺理成章的，应该建立两块Survivor区，刚刚新建的对象在Eden中，经历一次Minor GC，Eden中的存活对象就会被移动到第一块survivor space S0，Eden被清空；等Eden区再满了，就再触发一次Minor GC，Eden和S0中的存活对象又会被复制送入第二块survivor space S1（这个过程非常重要，因为这种复制算法保证了S1中来自S0和Eden两部分的存活对象占用连续的内存空间，避免了碎片化的发生）。S0和Eden被清空，然后下一轮S0与S1交换角色，如此循环往复。如果对象的复制次数达到16次，该对象就会被送到老年代中。下图中每部分的意义和上一张图一样，就不加注释了。

![两块Survivor避免碎片化](MarkDown_Java%20SE.assets/20160516174938778)

那么，Survivor为什么不分更多块呢？比方说分成三个、四个、五个?显然，如果Survivor区再细分下去，每一块的空间就会比较小，很容易导致Survivor区满，因此，我认为两块Survivor区是经过权衡之后的最佳方案。



#### 常见的垃圾回收器

```java
//TODO:
```











### **类文件结构**

#### 一 概述

在 Java 中，JVM 可以理解的代码就叫做`字节码`（即扩展名为 `.class` 的文件），它不面向任何特定的处理器，只面向虚拟机。Java 语言通过字节码的方式，在一定程度上解决了传统解释型语言执行效率低的问题，同时又保留了解释型语言可移植的特点。所以 Java 程序运行时比较高效，而且，由于字节码并不针对一种特定的机器，因此，Java 程序无须重新编译便可在多种不同操作系统的计算机上运行。

Clojure（Lisp 语言的一种方言）、Groovy、Scala 等语言都是运行在 Java 虚拟机之上。下图展示了不同的语言被不同的编译器编译成`.class`文件最终运行在 Java 虚拟机之上。`.class`文件的二进制格式可以使用 [WinHex](https://www.x-ways.net/winhex/) 查看。

![image-20210321151500011](MarkDown_Java%20SE.assets/image-20210321151500011.png)

**可以说`.class`文件是不同的语言在 Java 虚拟机之间的重要桥梁，同时也是支持 Java 跨平台很重要的一个原因。**





#### 二 Class 文件结构总结

根据 Java 虚拟机规范，类文件由单个 ClassFile 结构组成：

```
ClassFile {
    u4             magic; //Class 文件的标志
    u2             minor_version;//Class 的小版本号
    u2             major_version;//Class 的大版本号
    u2             constant_pool_count;//常量池的数量
    cp_info        constant_pool[constant_pool_count-1];//常量池
    u2             access_flags;//Class 的访问标记
    u2             this_class;//当前类
    u2             super_class;//父类
    u2             interfaces_count;//接口
    u2             interfaces[interfaces_count];//一个类可以实现多个接口
    u2             fields_count;//Class 文件的字段属性
    field_info     fields[fields_count];//一个类会可以有多个字段
    u2             methods_count;//Class 文件的方法数量
    method_info    methods[methods_count];//一个类可以有个多个方法
    u2             attributes_count;//此类的属性表中的属性数
    attribute_info attributes[attributes_count];//属性表集合
}
```

下面详细介绍一下 Class 文件结构涉及到的一些组件。

**Class文件字节码结构组织示意图** （之前在网上保存的，非常不错，原出处不明）：

![image-20210321151531252](MarkDown_Java%20SE.assets/image-20210321151531252.png)



##### 2.1 魔数

```
    u4             magic; //Class 文件的标志
```

每个 Class 文件的头四个字节称为魔数（Magic Number）,它的唯一作用是**确定这个文件是否为一个能被虚拟机接收的 Class 文件**。

程序设计者很多时候都喜欢用一些特殊的数字表示固定的文件类型或者其它特殊的含义。

##### 2.2 Class 文件版本

```
    u2             minor_version;//Class 的小版本号
    u2             major_version;//Class 的大版本号
```

紧接着魔数的四个字节存储的是 Class 文件的版本号：第五和第六是**次版本号**，第七和第八是**主版本号**。

高版本的 Java 虚拟机可以执行低版本编译器生成的 Class 文件，但是低版本的 Java 虚拟机不能执行高版本编译器生成的 Class 文件。所以，我们在实际开发的时候要确保开发的的 JDK 版本和生产环境的 JDK 版本保持一致。



##### 2.3 常量池

```
    u2             constant_pool_count;//常量池的数量
    cp_info        constant_pool[constant_pool_count-1];//常量池
```

紧接着主次版本号之后的是常量池，常量池的数量是 constant_pool_count-1（**常量池计数器是从1开始计数的，将第0项常量空出来是有特殊考虑的，索引值为0代表“不引用任何一个常量池项”**）。

常量池主要存放两大常量：字面量和符号引用。字面量比较接近于 Java 语言层面的的常量概念，如文本字符串、声明为 final 的常量值等。而符号引用则属于编译原理方面的概念。包括下面三类常量：

- 类和接口的全限定名
- 字段的名称和描述符
- 方法的名称和描述符

常量池中每一项常量都是一个表，这14种表有一个共同的特点：**开始的第一位是一个 u1 类型的标志位 -tag 来标识常量的类型，代表当前这个常量属于哪种常量类型．**

| 类型                             | 标志（tag） | 描述                   |
| -------------------------------- | ----------- | ---------------------- |
| CONSTANT_utf8_info               | 1           | UTF-8编码的字符串      |
| CONSTANT_Integer_info            | 3           | 整形字面量             |
| CONSTANT_Float_info              | 4           | 浮点型字面量           |
| CONSTANT_Long_info               | ５          | 长整型字面量           |
| CONSTANT_Double_info             | ６          | 双精度浮点型字面量     |
| CONSTANT_Class_info              | ７          | 类或接口的符号引用     |
| CONSTANT_String_info             | ８          | 字符串类型字面量       |
| CONSTANT_Fieldref_info           | ９          | 字段的符号引用         |
| CONSTANT_Methodref_info          | 10          | 类中方法的符号引用     |
| CONSTANT_InterfaceMethodref_info | 11          | 接口中方法的符号引用   |
| CONSTANT_NameAndType_info        | 12          | 字段或方法的符号引用   |
| CONSTANT_MothodType_info         | 16          | 标志方法类型           |
| CONSTANT_MethodHandle_info       | 15          | 表示方法句柄           |
| CONSTANT_InvokeDynamic_info      | 18          | 表示一个动态方法调用点 |

`.class` 文件可以通过`javap -v class类名` 指令来看一下其常量池中的信息(`javap -v class类名-> temp.txt` ：将结果输出到 temp.txt 文件)。





##### 2.4 访问标志

在常量池结束之后，紧接着的两个字节代表访问标志，这个标志用于识别一些类或者接口层次的访问信息，包括：这个 Class 是类还是接口，是否为 public 或者 abstract 类型，如果是类的话是否声明为 final 等等。

类访问和属性修饰符:

![image-20210322002211721](MarkDown_Java%20SE.assets/image-20210322002211721.png)

我们定义了一个 Employee 类

```java
package top.snailclimb.bean;
public class Employee {
   ...
}
```

通过`javap -v class类名` 指令来看一下类的访问标志。

![image-20210321151617113](MarkDown_Java%20SE.assets/image-20210321151617113.png)





##### 2.5 当前类索引,父类索引与接口索引集合

```
    u2             this_class;//当前类
    u2             super_class;//父类
    u2             interfaces_count;//接口
    u2             interfaces[interfaces_count];//一个类可以实现多个接口
```

**类索引用于确定这个类的全限定名，父类索引用于确定这个类的父类的全限定名，由于 Java 语言的单继承，所以父类索引只有一个，除了 `java.lang.Object` 之外，所有的 java 类都有父类，因此除了 `java.lang.Object` 外，所有 Java 类的父类索引都不为 0。**

**接口索引集合用来描述这个类实现了那些接口，这些被实现的接口将按 `implements` (如果这个类本身是接口的话则是`extends`) 后的接口顺序从左到右排列在接口索引集合中。**





##### 2.6 字段表集合

```
    u2             fields_count;//Class 文件的字段的个数
    field_info     fields[fields_count];//一个类会可以有个字段
```

字段表（field info）用于描述接口或类中声明的变量。字段包括类级变量以及实例变量，但不包括在方法内部声明的局部变量。

**field info(字段表) 的结构:**

![image-20210321152013264](MarkDown_Java%20SE.assets/image-20210321152013264.png)

- **access_flags:** 字段的作用域（`public` ,`private`,`protected`修饰符），是实例变量还是类变量（`static`修饰符）,可否被序列化（transient 修饰符）,可变性（final）,可见性（volatile 修饰符，是否强制从主内存读写）。
- **name_index:** 对常量池的引用，表示的字段的名称；
- **descriptor_index:** 对常量池的引用，表示字段和方法的描述符；
- **attributes_count:** 一个字段还会拥有一些额外的属性，attributes_count 存放属性的个数；
- **attributes[attributes_count]:** 存放具体属性具体内容。

上述这些信息中，各个修饰符都是布尔值，要么有某个修饰符，要么没有，很适合使用标志位来表示。而字段叫什么名字、字段被定义为什么数据类型这些都是无法固定的，只能引用常量池中常量来描述。

**字段的 access_flag 的取值:**

![image-20210322002301252](MarkDown_Java%20SE.assets/image-20210322002301252.png)







##### 2.7 方法表集合

```
    u2             methods_count;//Class 文件的方法的数量
    method_info    methods[methods_count];//一个类可以有个多个方法
```

methods_count 表示方法的数量，而 method_info 表示方法表。

Class 文件存储格式中对方法的描述与对字段的描述几乎采用了完全一致的方式。方法表的结构如同字段表一样，依次包括了访问标志、名称索引、描述符索引、属性表集合几项。

**method_info(方法表的) 结构:**

[![方法表的结构](https://camo.githubusercontent.com/0af5b65285b8a41644a5ddd447cac7c43c1f4eacd0a66bed29f201b5e086f94a/68747470733a2f2f6d792d626c6f672d746f2d7573652e6f73732d636e2d6265696a696e672e616c6979756e63732e636f6d2f323031392d362f2545362539362542392545362542332539352545382541312541382545372539412538342545372542422539332545362539452538342e706e67)](https://camo.githubusercontent.com/0af5b65285b8a41644a5ddd447cac7c43c1f4eacd0a66bed29f201b5e086f94a/68747470733a2f2f6d792d626c6f672d746f2d7573652e6f73732d636e2d6265696a696e672e616c6979756e63732e636f6d2f323031392d362f2545362539362542392545362542332539352545382541312541382545372539412538342545372542422539332545362539452538342e706e67)

**方法表的 access_flag 取值：**

![image-20210322002339549](MarkDown_Java%20SE.assets/image-20210322002339549.png)

注意：因为`volatile`修饰符和`transient`修饰符不可以修饰方法，所以方法表的访问标志中没有这两个对应的标志，但是增加了`synchronized`、`native`、`abstract`等关键字修饰方法，所以也就多了这些关键字对应的标志。



##### 2.8 属性表集合

```
   u2             attributes_count;//此类的属性表中的属性数
   attribute_info attributes[attributes_count];//属性表集合
```

在 Class 文件，字段表，方法表中都可以携带自己的属性表集合，以用于描述某些场景专有的信息。与 Class 文件中其它的数据项目要求的顺序、长度和内容不同，属性表集合的限制稍微宽松一些，不再要求各个属性表具有严格的顺序，并且只要不与已有的属性名重复，任何人实现的编译器都可以向属性表中写 入自己定义的属性信息，Java 虚拟机运行时会忽略掉它不认识的属性。











### **类加载过程**

#### 类的生命周期

一个类的完整生命周期如下：

![image-20210321160945179](MarkDown_Java%20SE.assets/image-20210321160945179.png)





#### 类加载过程

Class 文件需要加载到虚拟机中之后才能运行和使用，那么虚拟机是如何加载这些 Class 文件呢？

系统加载 Class 类型的文件主要三步:**加载->连接->初始化**。连接过程又可分为三步:**验证->准备->解析**。

![image-20210321161140706](MarkDown_Java%20SE.assets/image-20210321161140706.png)



#### 加载

类加载过程的第一步，主要完成下面3件事情：

1. 通过全类名获取定义此类的二进制字节流
2. 将字节流所代表的静态存储结构转换为方法区的运行时数据结构
3. 在内存中生成一个代表该类的 Class 对象,作为方法区这些数据的访问入口

虚拟机规范上面这3点并不具体，因此是非常灵活的。比如："通过全类名获取定义此类的二进制字节流" 并没有指明具体从哪里获取、怎样获取。比如：比较常见的就是从 ZIP 包中读取（日后出现的JAR、EAR、WAR格式的基础）、其他文件生成（典型应用就是JSP）等等。

**一个非数组类的加载阶段（加载阶段获取类的二进制字节流的动作）是可控性最强的阶段，这一步我们可以去完成还可以自定义类加载器去控制字节流的获取方式（重写一个类加载器的 `loadClass()` 方法）。数组类型不通过类加载器创建，它由 Java 虚拟机直接创建。**

类加载器、双亲委派模型也是非常重要的知识点，这部分内容会在后面的文章中单独介绍到。

加载阶段和连接阶段的部分内容是交叉进行的，加载阶段尚未结束，连接阶段可能就已经开始了。



#### 验证

![image-20210322002448158](MarkDown_Java%20SE.assets/image-20210322002448158.png)



#### 准备

**准备阶段是正式为类变量分配内存并设置类变量初始值的阶段**，这些内存都将在方法区中分配。对于该阶段有以下几点需要注意：

1. 这时候进行内存分配的仅包括类变量（static），而不包括实例变量，实例变量会在对象实例化时随着对象一块分配在 Java 堆中。
2. 这里所设置的初始值"通常情况"下是数据类型默认的零值（如0、0L、null、false等），比如我们定义了`public static int value=111` ，那么 value 变量在准备阶段的初始值就是 0 而不是111（初始化阶段才会赋值）。特殊情况：比如给 value 变量加上了 fianl 关键字`public static final int value=111` ，那么准备阶段 value 的值就被赋值为 111。

**基本数据类型的零值：**

![image-20210321163009717](MarkDown_Java%20SE.assets/image-20210321163009717.png)





#### 解析

解析阶段是虚拟机将常量池内的符号引用替换为直接引用的过程。解析动作主要针对类或接口、字段、类方法、接口方法、方法类型、方法句柄和调用限定符7类符号引用进行。

符号引用就是一组符号来描述目标，可以是任何字面量。**直接引用**就是直接指向目标的指针、相对偏移量或一个间接定位到目标的句柄。在程序实际运行时，只有符号引用是不够的，举个例子：在程序执行方法时，系统需要明确知道这个方法所在的位置。Java 虚拟机为每个类都准备了一张方法表来存放类中所有的方法。当需要调用一个类的方法的时候，只要知道这个方法在方法表中的偏移量就可以直接调用该方法了。通过解析操作符号引用就可以直接转变为目标方法在类中方法表的位置，从而使得方法可以被调用。

综上，解析阶段是虚拟机将常量池内的符号引用替换为直接引用的过程，也就是得到类或者字段、方法在内存中的指针或者偏移量。





#### 初始化

初始化是类加载的最后一步，也是真正执行类中定义的 Java 程序代码(字节码)，初始化阶段是执行初始化方法 `<clinit> ()`方法的过程。

对于`<clinit>（）` 方法的调用，虚拟机会自己确保其在多线程环境中的安全性。因为 `<clinit>（）` 方法是带锁线程安全，所以在多线程环境下进行类初始化的话可能会引起死锁，并且这种死锁很难被发现。

对于初始化阶段，虚拟机严格规范了有且只有5种情况下，必须对类进行初始化(只有主动去使用类才会初始化类)：

1. 当遇到 new 、 getstatic、putstatic或invokestatic 这4条直接码指令时，比如 new 一个类，读取一个静态字段(未被 final 修饰)、或调用一个类的静态方法时。
   - 当jvm执行new指令时会初始化类。即当程序创建一个类的实例对象。
   - 当jvm执行getstatic指令时会初始化类。即程序访问类的静态变量(不是静态常量，常量会被加载到运行时常量池)。
   - 当jvm执行putstatic指令时会初始化类。即程序给类的静态变量赋值。
   - 当jvm执行invokestatic指令时会初始化类。即程序调用类的静态方法。
2. 使用 `java.lang.reflect` 包的方法对类进行反射调用时如Class.forname("..."),newInstance()等等。 ，如果类没初始化，需要触发其初始化。
3. 初始化一个类，如果其父类还未初始化，则先触发该父类的初始化。
4. 当虚拟机启动时，用户需要定义一个要执行的主类 (包含 main 方法的那个类)，虚拟机会先初始化这个类。
5. MethodHandle和VarHandle可以看作是轻量级的反射调用机制，而要想使用这2个调用， 就必须先使用findStaticVarHandle来初始化要调用的类。
6. **「补充，来自[issue745](https://github.com/Snailclimb/JavaGuide/issues/745)」** 当一个接口中定义了JDK8新加入的默认方法（被default关键字修饰的接口方法）时，如果有这个接口的实现类发生了初始化，那该接口要在其之前被初始化。



#### 卸载

> 卸载这部分内容来自 [issue#662](https://github.com/Snailclimb/JavaGuide/issues/662)由 **[guang19](https://github.com/guang19)** 补充完善。

卸载类即该类的Class对象被GC。

卸载类需要满足3个要求:

1. 该类的所有的实例对象都已被GC，也就是说堆不存在该类的实例对象。
2. 该类没有在其他任何地方被引用
3. 该类的类加载器的实例已被GC

所以，在JVM生命周期类，由jvm自带的类加载器加载的类是不会被卸载的。但是由我们自定义的类加载器加载的类是可能被卸载的。

只要想通一点就好了，jdk自带的BootstrapClassLoader,ExtClassLoader,AppClassLoader负责加载jdk提供的类，所以它们(类加载器的实例)肯定不会被回收。而我们自定义的类加载器的实例是可以被回收的，所以使用我们自定义加载器加载的类是可以被卸载掉的。

**参考**

- 《深入理解Java虚拟机》
- 《实战Java虚拟机》
- https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-5.html







### 类加载器

JVM 中内置了三个重要的 ClassLoader，除了 BootstrapClassLoader 其他类加载器均由 Java 实现且全部继承自`java.lang.ClassLoader`：

1. **BootstrapClassLoader(启动类加载器)** ：最顶层的加载类，由C++实现，负责加载 `%JAVA_HOME%/lib`目录下的jar包和类或者或被 `-Xbootclasspath`参数指定的路径中的所有类。
2. **ExtensionClassLoader(扩展类加载器)** ：主要负责加载目录 `%JRE_HOME%/lib/ext` 目录下的jar包和类，或被 `java.ext.dirs` 系统变量所指定的路径下的jar包。
3. **AppClassLoader(应用程序类加载器)** :面向我们用户的加载器，负责加载当前应用classpath下的所有jar包和类。



#### 双亲委派模型

##### 双亲委派模型介绍

每一个类都有一个对应它的类加载器。系统中的 ClassLoder 在协同工作的时候会默认使用 **双亲委派模型** 。即在类加载的时候，系统会首先判断当前类是否被加载过。已经被加载的类会直接返回，否则才会尝试加载。加载的时候，首先会把该请求委派该父类加载器的 `loadClass()` 处理，因此所有的请求最终都应该传送到顶层的启动类加载器 `BootstrapClassLoader` 中。当父类加载器无法处理时，才由自己来处理。当父类加载器为null时，会使用启动类加载器 `BootstrapClassLoader` 作为父类加载器。

![image-20210321165004481](MarkDown_Java%20SE.assets/image-20210321165004481.png)

每个类加载都有一个父类加载器，我们通过下面的程序来验证。

```java
public class ClassLoaderDemo {
    public static void main(String[] args) {
        System.out.println("ClassLodarDemo's ClassLoader is " + ClassLoaderDemo.class.getClassLoader());
        System.out.println("The Parent of ClassLodarDemo's ClassLoader is " + ClassLoaderDemo.class.getClassLoader().getParent());
        System.out.println("The GrandParent of ClassLodarDemo's ClassLoader is " + ClassLoaderDemo.class.getClassLoader().getParent().getParent());
    }
}
```

Output

```java
ClassLodarDemo's ClassLoader is jdk.internal.loader.ClassLoaders$AppClassLoader@1f89ab83
The Parent of ClassLodarDemo's ClassLoader is jdk.internal.loader.ClassLoaders$PlatformClassLoader@edf4efb
The GrandParent of ClassLodarDemo's ClassLoader is null
```

`AppClassLoader`的父类加载器为`ExtClassLoader` `ExtClassLoader`(java1.9之前，现已变为PlatFromClassLoader)的父类加载器为null，**null并不代表`ExtClassLoader`没有父类加载器，而是 `BootstrapClassLoader`** 。



其实这个双亲翻译的容易让别人误解，我们一般理解的双亲都是父母，这里的双亲更多地表达的是“父母这一辈”的人而已，并不是说真的有一个 Mother ClassLoader 和一个 Father ClassLoader 。另外，类加载器之间的“父子”关系也不是通过继承来体现的，是由“优先级”来决定。官方API文档对这部分的描述如下:

> The Java platform uses a delegation model for loading classes. **The basic idea is that every class loader has a "parent" class loader.** When loading a class, a class loader first "delegates" the search for the class to its parent class loader before attempting to find the class itself.





#### 双亲委派模型实现源码分析

双亲委派模型的实现代码非常简单，逻辑非常清晰，都集中在 `java.lang.ClassLoader` 的 `loadClass()` 中，相关代码如下所示。

```java
private final ClassLoader parent; 
protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        synchronized (getClassLoadingLock(name)) {
            // 首先，检查请求的类是否已经被加载过
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    if (parent != null) {//父加载器不为空，调用父加载器loadClass()方法处理
                        c = parent.loadClass(name, false);
                    } else {//父加载器为空，使用启动类加载器 BootstrapClassLoader 加载
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                   //抛出异常说明父类加载器无法完成加载请求
                }
                
                if (c == null) {
                    long t1 = System.nanoTime();
                    //自己尝试加载
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
```







#### 双亲委派模型的好处

双亲委派模型保证了Java程序的稳定运行，可以避免类的重复加载（JVM 区分不同类的方式不仅仅根据类名，相同的类文件被不同的类加载器加载产生的是两个不同的类），也保证了 Java 的核心 API 不被篡改。如果没有使用双亲委派模型，而是每个类加载器加载自己的话就会出现一些问题，比如我们编写一个称为 `java.lang.Object` 类的话，那么程序运行的时候，系统就会出现多个不同的 `Object` 类。

#### 如果我们不想用双亲委派模型怎么办？

~~为了避免双亲委托机制，我们可以自己定义一个类加载器，然后重写 `loadClass()` 即可。~~

完善修正（[issue871](https://github.com/Snailclimb/JavaGuide/issues/871)：类加载器一问的补充说明）：

**自定义加载器的话，需要继承 `ClassLoader` 。如果我们不想打破双亲委派模型，就重写 `ClassLoader` 类中的 `findClass()` 方法即可，无法被父类加载器加载的类最终会通过这个方法被加载。但是，如果想打破双亲委派模型则需要重写 `loadClass()` 方法**



```java
//TODO:双亲依赖
```

https://juejin.cn/post/6844903729435508750



#### 哪些操作会破坏双亲依赖?

【面试题】jvm中如何打破双亲委托机制？ - wuxinliulei的回答 - 知乎
https://www.zhihu.com/question/45022217/answer/425141928







#### 自定义类加载器

除了 `BootstrapClassLoader` 其他类加载器均由 Java 实现且全部继承自`java.lang.ClassLoader`。如果我们要自定义自己的类加载器，很明显需要继承 `ClassLoader`。

推荐阅读

- https://blog.csdn.net/xyang81/article/details/7292380
- https://juejin.im/post/5c04892351882516e70dcc9b
- http://gityuan.com/2016/01/24/java-classloader/







### Java七个可能存在的内存泄露风险

https://mp.weixin.qq.com/s/SKhAzV4DXf6hBvneQxxnRg

```java
//TODO:
```





### IDE-idea 调整虚拟机选项 

VM Options





## JAVA中的锁

通俗易懂 悲观锁、乐观锁、可重入锁、自旋锁、偏向锁、轻量/重量级锁、读写锁、各种锁及其Java实现！ - Pickle Pee的文章 - 知乎 https://zhuanlan.zhihu.com/p/71156910



Java中有两种加锁的方式：一种是用**synchronized关键字**，另一种是用**Lock接口**的实现类。

形象地说，synchronized关键字是**自动档**，可以满足一切日常驾驶需求。但是如果你想要玩漂移或者各种骚操作，就需要**手动档**了——各种Lock的实现类。

所以如果你只是想要简单的加个锁，对性能也没什么特别的要求，用synchronized关键字就足够了。自Java 5之后，才在java.util.concurrent.locks包下有了另外一种方式来实现锁，那就是Lock。也就是说，**synchronized是Java语言内置的关键字，而Lock是一个接口**，这个接口的实现类在代码层面实现了锁的功能，具体细节不在本文展开，有兴趣可以研究下AbstractQueuedSynchronizer类，写得可以说是牛逼爆了。

![img](MarkDown_Java%20SE.assets/v2-ddb71ab0b68d65ae70244bfdeb0d6704_1440w.jpg)其实只需要关注三个类就可以了：ReentrantLock类、ReadLock类、WriteLock类。

**ReentrantLock、ReadLock、WriteLock** 是Lock接口最重要的三个实现类。对应了“可重入锁”、“读锁”和“写锁”，后面会讲它们的用途。

ReadWriteLock其实是一个工厂接口，而ReentrantReadWriteLock是ReadWriteLock的实现类，它包含两个静态内部类ReadLock和WriteLock。这两个静态内部类又分别实现了Lock接口。

我们停止深究源码，仅从使用的角度看，Lock与synchronized的区别是什么？在接下来的几个小节中，我将梳理各种锁分类的概念，以及synchronized关键字、各种Lock实现类之间的区别与联系。

### 一、悲观锁与乐观锁

锁的一种宏观分类方式是**悲观锁**和**乐观锁**。悲观锁与乐观锁**并不是特指某个锁**（Java中没有哪个Lock实现类就叫PessimisticLock或OptimisticLock），而是在并发情况下的两种不同策略。

悲观锁（Pessimistic Lock）, 就是很悲观，每次去拿数据的时候都认为别人会修改。所以每次在拿数据的时候都会上锁。这样别人想拿数据就被挡住，直到悲观锁被释放。

乐观锁（Optimistic Lock）, 就是很乐观，每次去拿数据的时候都认为别人不会修改。所以**不会上锁，不会上锁！**但是如果想要更新数据，则会在**更新前检查在读取至更新这段时间别人有没有修改过这个数据**。如果修改过，则重新读取，再次尝试更新，循环上述步骤直到更新成功（当然也允许更新失败的线程放弃操作）。

**悲观锁阻塞事务，乐观锁回滚重试**，它们各有优缺点，不要认为一种一定好于另一种。像乐观锁适用于写比较少的情况下，即冲突真的很少发生的时候，这样可以省去锁的开销，加大了系统的整个吞吐量。但如果经常产生冲突，上层应用会不断的进行重试，这样反倒是降低了性能，所以这种情况下用悲观锁就比较合适。







### 二、乐观锁的基础——CAS

什么是CAS呢？Compare-and-Swap，即**比较并替换，**也有叫做Compare-and-Set的，**比较并设置**。

1、比较：读取到了一个值A，在将其更新为B之前，检查原值是否仍为A（未被其他线程改动）。

2、设置：如果是，将A更新为B，结束。[[1\]](https://zhuanlan.zhihu.com/p/71156910#ref_1)如果不是，则什么都不做。

上面的两步操作是原子性的，可以简单地理解为瞬间完成，在CPU看来就是一步操作。

有了CAS，就可以实现一个**乐观锁**：



- synchronized是悲观锁，这种线程一旦得到锁，其他需要锁的线程就挂起的情况就是悲观锁。
- CAS操作的就是乐观锁，每次不加锁而是假设没有冲突而去完成某项操作，如果因为冲突失败就重试，直到成功为止。

在进入正题之前，我们先理解下下面的代码:

```java
private static int count = 0;

public static void main(String[] args) {
    for (int i = 0; i < 5; i++) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //每个线程让count自增100次
                for (int i = 0; i < 100; i++) {
                    count++;
                }
            }
        }).start();
    }

    try {
        Thread.sleep(1000);
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println(count);//408
}
```

请问cout的输出值是否为500？答案是否定的，因为这个程序是线程不安全的，所以造成的结果count值可能小于500;

那么如何改造成线程安全的呢，其实我们可以使用上`Synchronized`同步锁,我们只需要在count++的位置添加同步锁，代码如下:

```java
package Test;

public class TestCAS {
    private static int count = 0;

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //每个线程让count自增100次
                    for (int i = 0; i < 100; i++) {
                        synchronized (TestCAS.class) {
                            count++;
                        }
                    }
                }
            }).start();
        }

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(count);//500
    }
}
```

加了同步锁之后，count自增的操作变成了原子性操作，所以最终的输出一定是count=500，代码实现了线程安全。

但是`Synchronized`虽然确保了线程的安全，但是在性能上却不是最优的，`Synchronized`关键字会让没有得到锁资源的线程进入`BLOCKED`状态，而后在争夺到锁资源后恢复为`RUNNABLE`状态，这个过程中涉及到操作系统用户模式和内核模式的转换，代价比较高。

尽管Java1.6为`Synchronized`做了优化，增加了从偏向锁到轻量级锁再到重量级锁的过度，但是在最终转变为重量级锁之后，性能仍然较低。

所谓原子操作类，指的是java.util.concurrent.atomic包下，一系列以Atomic开头的包装类。例如`AtomicBoolean`，`AtomicInteger`，`AtomicLong`。它们分别用于`Boolean`，`Integer`，`Long`类型的原子性操作。

```java
import java.util.concurrent.atomic.AtomicInteger;

public class TestCAS {
    private static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //每个线程让count自增100次
                    for (int i = 0; i < 100; i++) {
                        count.incrementAndGet();
                    }
                }
            }).start();
        }

        try{
            Thread.sleep(1000);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(count);//500
    }
}
```

使用AtomicInteger之后，最终的输出结果同样可以保证是200。并且在某些情况下，代码的性能会比Synchronized更好。

而Atomic操作的底层实现正是利用的CAS机制，好的，我们切入到这个博客的正点。





#### 什么是CAS机制

CAS是英文单词Compare And Swap的缩写，翻译过来就是比较并替换。

CAS机制当中使用了3个基本操作数：内存地址V，旧的预期值A，要修改的新值B。

更新一个变量的时候，只有当变量的预期值A和内存地址V当中的实际值相同时，才会将内存地址V对应的值修改为B。

CAS是英文单词Compare And Swap的缩写，翻译过来就是比较并替换。

CAS机制当中使用了3个基本操作数：内存地址V，旧的预期值A，要修改的新值B。

更新一个变量(想要变成B)的时候，只有当变量的预期值A和内存地址V当中的实际值相同时，才会将内存地址V对应的值修改为B。

这样说或许有些抽象，我们来看一个例子：

1.在内存地址V当中，存储着值为10的变量。

![img](MarkDown_Java%20SE.assets/5630287-350bc3c474eef0e8.jpg)

2.此时线程1想要把变量的值增加1。对线程1来说，旧的预期值A=10，要修改的新值B=11。

![img](MarkDown_Java%20SE.assets/5630287-eb7709492f262c25.jpg)

3.在线程1要提交更新之前，另一个线程2抢先一步，把内存地址V中的变量值率先更新成了11。

![img](MarkDown_Java%20SE.assets/5630287-cab4d45aa3e06369.jpg)





4.线程1开始提交更新，首先进行A和地址V的实际值比较（Compare），发现A不等于V的实际值，提交失败。

![img](MarkDown_Java%20SE.assets/5630287-a250c3f723b73be0.jpg)

5.线程1重新获取内存地址V的当前值，并重新计算想要修改的新值。此时对线程1来说，A=11，B=12。这个重新尝试的过程被称为自旋。

![img](MarkDown_Java%20SE.assets/5630287-f638cadea7b6cb96.jpg)

6.这一次比较幸运，没有其他线程改变地址V的值。线程1进行Compare，发现A和地址V的实际值是相等的。

![img](MarkDown_Java%20SE.assets/5630287-0a3d0b3926366d63.jpg)

7.线程1进行SWAP，把地址V的值替换为B，也就是12。

![img](MarkDown_Java%20SE.assets/5630287-f6c83ad3ca4f3294.jpg)

从思想上来说，Synchronized属于悲观锁，悲观地认为程序中的并发情况严重，所以严防死守。CAS属于乐观锁，乐观地认为程序中的并发情况不那么严重，所以让线程不断去尝试更新。

#### CAS乐观锁小demo

看到上面的解释是不是索然无味，查找了很多资料也没完全弄明白，通过几次验证后，终于明白，最终可以理解成一个无阻塞多线程争抢资源的模型。先上代码

```java
package Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanTest implements Runnable {

    private static AtomicBoolean flag = new AtomicBoolean(true);

    public static void main(String[] args) {
        AtomicBooleanTest ast = new AtomicBooleanTest();
        Thread thread1 = new Thread(ast);
        Thread thread = new Thread(ast);
        thread1.start();
        thread.start();
    }
    @Override
    public void run() {
        System.out.println("thread:"+Thread.currentThread().getName()+";flag:"+flag.get());
        if (flag.compareAndSet(true,false)){
            System.out.println(Thread.currentThread().getName()+""+flag.get());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            flag.set(true);
        }else{
            System.out.println("重试机制thread:"+Thread.currentThread().getName()+";flag:"+flag.get());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            run();//继续调用自己
        }

    }
}
```

程序运行结果：

```java
thread:Thread-1;flag:true
thread:Thread-0;flag:true
重试机制thread:Thread-0;flag:false
Thread-1false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:false
重试机制thread:Thread-0;flag:false
thread:Thread-0;flag:true
Thread-0false

Process finished with exit code 0
```

这里无论怎么运行，Thread-1、Thread-0都会执行if=true条件，而且还不会产生线程脏读脏写，这是如何做到的了，这就用到了我们的compareAndSet(boolean expect,boolean update)方法
 我们看到当Thread-1在进行操作的时候，Thread一直在进行重试机制，程序原理图:

![img](MarkDown_Java%20SE.assets/5630287-98b1979eb2f85998.png)

这个图中重最要的是compareAndSet(true,false)方法要拆开成compare(true)方法和Set(false)方法理解，是compare(true)是等于true后，就马上设置共享内存为false，这个时候，其它线程无论怎么走都无法走到只有得到共享内存为true时的程序隔离方法区。

看到这里，这种CAS机制就是完美的吗？这个程序其实存在一个问题，不知道大家注意到没有？

但是这种得不到状态为true时使用递归算法是很耗cpu资源的，所以一般情况下，都会有线程sleep。

CAS的缺点：

1.CPU开销较大
 在并发量比较高的情况下，如果许多线程反复尝试更新某一个变量，却又一直更新不成功，循环往复，会给CPU带来很大的压力。

2.不能保证代码块的原子性
 CAS机制所保证的只是一个变量的原子性操作，而不能保证整个代码块的原子性。比如需要保证3个变量共同进行原子性的更新，就不得不使用Synchronized了。











### 三、自旋锁

有一种锁叫**自旋锁**。所谓自旋，说白了就是一个 while(true) 无限循环。

刚刚的乐观锁就有类似的无限循环操作，那么它是自旋锁吗？

> 感谢评论区[养猫的虾](https://www.zhihu.com/people/zhao-chen-77-90)的指正。

不是。尽管自旋与 while(true) 的操作是一样的，但还是应该将这两个术语分开。“自旋”这两个字，特指自旋锁的自旋。

然而在JDK中并没有自旋锁（SpinLock）这个类，那什么才是自旋锁呢？读完下个小节就知道了。











### 四、synchronized锁升级：偏向锁 → 轻量级锁 → 重量级锁

前面提到，synchronized关键字就像是汽车的**自动档，**现在详细讲这个过程。一脚油门踩下去，synchronized会从**无锁**升级为**偏向锁**，再升级为**轻量级锁**，最后升级为**重量级锁**，就像自动换挡一样。那么自旋锁在哪里呢？这里的轻量级锁就是一种**自旋锁**。

初次执行到synchronized代码块的时候，锁对象变成**偏向锁**（通过CAS修改对象头里的锁标志位），字面意思是“偏向于第一个获得它的线程”的锁。执行完同步代码块后，线程并**不会主动释放偏向锁**。当第二次到达同步代码块时，线程会判断此时持有锁的线程是否就是自己（持有锁的线程ID也在对象头里），如果是则正常往下执行。**由于之前没有释放锁，这里也就不需要重新加锁。**如果自始至终使用锁的线程只有一个，很明显偏向锁几乎没有额外开销，性能极高。

一旦有第二个线程加入**锁竞争**，偏向锁就升级为**轻量级锁（自旋锁）**。这里要明确一下什么是锁竞争：如果多个线程轮流获取一个锁，但是每次获取锁的时候都很顺利，没有发生阻塞，那么就不存在锁竞争。只有当某线程尝试获取锁的时候，发现该锁已经被占用，只能等待其释放，这才发生了锁竞争。

在轻量级锁状态下继续锁竞争，没有抢到锁的线程将**自旋**，即不停地循环判断锁是否能够被成功获取。获取锁的操作，其实就是通过CAS修改对象头里的锁标志位。先**比较**当前锁标志位是否为“释放”，如果是则将其**设置**为“锁定”，比较并设置是**原子性**发生的。这就算抢到锁了，然后线程将当前锁的持有者信息修改为自己。

长时间的自旋操作是非常消耗资源的，一个线程持有锁，其他线程就只能在原地空耗CPU，执行不了任何有效的任务，这种现象叫做**忙等（busy-waiting）**。如果多个线程用一个锁，但是没有发生锁竞争，或者发生了很轻微的锁竞争，那么synchronized就用轻量级锁，允许短时间的忙等现象。这是一种折衷的想法，**短时间的忙等，换取线程在用户态和内核态之间切换的开销。**

显然，此忙等是有限度的（有个计数器记录自旋次数，默认允许循环10次，可以通过虚拟机参数更改）。如果锁竞争情况严重，某个达到最大自旋次数的线程，会将轻量级锁升级为**重量级锁**（依然是CAS修改锁标志位，但不修改持有锁的线程ID）。当后续线程尝试获取锁时，发现被占用的锁是重量级锁，则直接将自己挂起（而不是忙等），等待将来被唤醒。在JDK1.6之前，synchronized直接加重量级锁，很明显现在得到了很好的优化。

一个锁只能按照 偏向锁、轻量级锁、重量级锁的顺序逐渐升级（也有叫**锁膨胀**的），不允许降级。

> 感谢评论区[酷帅俊靓美](https://www.zhihu.com/people/ding-yi-51-99)的问题：
> 偏向锁的一个特性是，持有锁的线程在执行完同步代码块时不会释放锁。那么当第二个线程执行到这个synchronized代码块时是否一定会发生锁竞争然后升级为轻量级锁呢？
> 线程A第一次执行完同步代码块后，当线程B尝试获取锁的时候，发现是偏向锁，会判断线程A是否仍然存活。**如果线程A仍然存活，**将线程A暂停，此时偏向锁升级为轻量级锁，之后线程A继续执行，线程B自旋。但是**如果判断结果是线程A不存在了**，则线程B持有此偏向锁，锁不升级。
> 还有人对此有疑惑，我之前确实没有描述清楚，但如果要展开讲，涉及到太多新概念，可以新开一篇了。更何况有些太底层的东西，我没读过源码，没有自信说自己一定是对的。其实在升级为轻量级锁之前，虚拟机会让线程A尽快在安全点挂起，然后在它的栈中“伪造”一些信息，让线程A在被唤醒之后，认为自己一直持有的是轻量级锁。如果线程A之前正在同步代码块中，那么线程B自旋等待即可。如果线程A之前不在同步代码块中，它会在被唤醒后检查到这一情况并立即释放锁，让线程B可以拿到。这部分内容我之前也没有深入研究过，如果有说的不对的，请多多指教啊！









### 五、可重入锁（递归锁）

#### 1、可重入锁的概念

**允许同一个线程多次获取同一把锁**。指的是同一个线程外层函数获得锁之后，内层递归函数仍然能获取该锁的代码，在同一个线程在外层方法获取锁的时候，在进入内层方法会自动获取锁。

也就是说，线程可以进入任何一个他已经拥有锁的所有同步代码块。

Java里只要以Reentrant开头命名的锁都是可重入锁，而且**JDK提供的所有现成的Lock实现类，包括synchronized关键字锁都是可重入的。**

#### 2 . 为什么要可重入

如果线程A继续再次获得这个锁呢?比如一个方法是synchronized,递归调用自己,那么第一次已经获得了锁,第二次调用的时候还能进入吗? 直观上当然需要能进入.这就要求必须是可重入的.可重入锁又叫做递归锁,再举个例子.

```java
public class Widget {
        public synchronized void doSomething() {
            ...
        }
}
     
public class LoggingWidget extends Widget {
        public synchronized void doSomething() {
            System.out.println(toString() + ": calling doSomething");
            super.doSomething();//若内置锁是不可重入的，则发生死锁
        }
}
```

这个例子是java并发编程实战中的例 子.synchronized 是父类Widget的内置锁,当执行子 类的方法的时候,先获取了一次Widget的锁,然后在执行super的时候,就要获取一次,如果不可重入,那么就跪了.

#### 3 . 如何实现可重入锁

为每个锁关联一个获取计数器和一个所有者线程,当计数值为0的时候,这个所就没有被任何线程只有.当线程请求一个未被持有的锁时,JVM将记下锁的持有者,并且将获取计数值置为1,如果同一个线程再次获取这个锁,技术值将递增,退出一次同步代码块,计算值递减,当计数值为0时,这个锁就被释放.ReentrantLock里面有实现

#### 4 . 有不可重入锁吗

这个还真有.Linux下的pthread_mutex_t锁是默认是非递归的。可以通过设置PTHREAD_MUTEX_RECURSIVE属性，将pthread_mutex_t锁设置为递归锁。如果要自己实现不可重入锁,同可重入锁,这个计数器只能为1.或者0,再次进入的时候,发现已经是1了,就进行阻塞.jdk里面没有默认的实现类.



#### 5 . demo代码展示

**5.1 内置锁的可重入**

```java
package syn.Lock.ReentrantLock;

public class ReentrantTest {
    public void method1() {
        synchronized (ReentrantTest.class) {
            System.out.println("方法1获得ReentrantTest的内置锁运行了");
            method2();
        }
    }

    public void method2() {
        synchronized (ReentrantTest.class) {
            System.out.println("方法1里面调用的方法2重入内置锁,也正常运行了");
        }
    }

    public static void main(String[] args) {
        new ReentrantTest().method1();
    }
}
```

```java
方法1获得ReentrantTest的内置锁运行了
方法1里面调用的方法2重入内置锁,也正常运行了
```

**5.2 lock对象的可重入**

```java
package syn.Lock.ReentrantLock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantTest2 {
        private Lock lock = new ReentrantLock();
    public void method2() {
        lock.lock();
        System.out.println("method2");
        lock.unlock();
    }

    public void method1() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        lock.lock();
//        method2();
        System.out.println("method1");
//        Method method = ReentrantTest2.class.getMethod("method2");
//        method.invoke(this, null);
        method2();
        lock.unlock();
    }


    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        ReentrantTest2 reentrantTest2 = new ReentrantTest2();
        reentrantTest2.method1();
    }
}
```

```java
method1
method2
```











### 六、公平锁、非公平锁

如果多个线程申请一把**公平锁**，那么当锁释放的时候，先申请的先得到，非常公平。显然如果是**非公平锁**，后申请的线程可能先获取到锁，是随机或者按照其他优先级排序的。

对ReentrantLock类而言，通过构造函数传参**可以指定该锁是否是公平锁，默认是非公平锁**。一般情况下，非公平锁的吞吐量比公平锁大，如果没有特殊要求，优先使用非公平锁。

![img](MarkDown_Java%20SE.assets/v2-7a4a72fe7ace46095cd3ca2e6c5212d9_1440w.jpg)ReentrantLock构造器可以指定为公平或非公平

对于synchronized而言，它也是一种**非公平锁**，但是并没有任何办法使其变成公平锁。











### 七、可中断锁

可中断锁，字面意思是“可以**响应中断**的锁”。

这里的关键是理解什么是**中断**。Java并没有提供任何直接中断某线程的方法，只提供了**中断机制**。何谓“中断机制”？线程A向线程B发出“请你停止运行”的请求（线程B也可以自己给自己发送此请求），但线程B并不会立刻停止运行，而是自行选择合适的时机以自己的方式响应中断，也可以直接忽略此中断。也就是说，Java的**中断不能直接终止线程**，而是需要被中断的线程自己决定怎么处理。这好比是父母叮嘱在外的子女要注意身体，但子女是否注意身体，怎么注意身体则完全取决于自己。[[2\]](https://zhuanlan.zhihu.com/p/71156910#ref_2)

回到锁的话题上来，如果线程A持有锁，线程B等待获取该锁。由于线程A持有锁的时间过长，线程B不想继续等待了，我们可以让线程B中断自己或者在别的线程里中断它，这种就是**可中断锁**。

在Java中，synchronized就是**不可中断锁**，而Lock的实现类都是**可中断锁，**可以简单看下Lock接口。

```java
/* Lock接口 */
public interface Lock {

    void lock(); // 拿不到锁就一直等，拿到马上返回。

    void lockInterruptibly() throws InterruptedException; // 拿不到锁就一直等，如果等待时收到中断请求，则需要处理InterruptedException。

    boolean tryLock(); // 无论拿不拿得到锁，都马上返回。拿到返回true，拿不到返回false。

    boolean tryLock(long time, TimeUnit unit) throws InterruptedException; // 同上，可以自定义等待的时间。

    void unlock();

    Condition newCondition();
}
```









### 八、读写锁、共享锁、互斥锁

读写锁其实是一对锁，一个读锁（共享锁）和一个写锁（互斥锁、排他锁）。

看下Java里的ReadWriteLock接口，它只规定了两个方法，一个返回读锁，一个返回写锁。

![img](MarkDown_Java%20SE.assets/v2-5ec6ed066c75e59c4f3829ca51db8148_1440w.jpg)

记得之前的乐观锁策略吗？所有线程随时都可以读，仅在写之前判断值有没有被更改。

读写锁其实做的事情是一样的，但是策略稍有不同。很多情况下，线程知道自己读取数据后，是否是为了更新它。那么何不在加锁的时候直接明确这一点呢？如果我读取值是为了更新它（SQL的for update就是这个意思），那么加锁的时候就直接加**写锁**，我持有写锁的时候别的线程无论读还是写都需要等待；如果我读取数据仅为了前端展示，那么加锁时就明确地加一个**读锁，**其他线程如果也要加读锁，不需要等待，可以直接获取（读锁计数器+1）。

虽然读写锁感觉与乐观锁有点像，但是**读写锁是悲观锁策略**。因为读写锁并没有在**更新前**判断值有没有被修改过，而是在**加锁前**决定应该用读锁还是写锁。乐观锁特指无锁编程，如果仍有疑惑可以再回到第一、二小节，看一下什么是“乐观锁”。

JDK提供的唯一一个ReadWriteLock接口实现类是ReentrantReadWriteLock。看名字就知道，它不仅提供了读写锁，而是都是可重入锁。 除了两个接口方法以外，ReentrantReadWriteLock还提供了一些便于外界监控其内部工作状态的方法，这里就不一一展开。



### 九、回到悲观锁和乐观锁

> 这篇文章经历过一次修改，我之前认为偏向锁和轻量级锁是乐观锁，重量级锁和Lock实现类为悲观锁，网上很多资料对这些概念的表述也很模糊，各执一词。

先抛出我的结论：

我们在Java里使用的各种锁，**几乎全都是悲观锁**。synchronized从偏向锁、轻量级锁到重量级锁，全是悲观锁。JDK提供的Lock实现类全是悲观锁。其实只要有“锁对象”出现，那么就一定是悲观锁。因为**乐观锁不是锁，而是一个在循环里尝试CAS的算法。**

那JDK并发包里到底有没有乐观锁呢？

有。java.util.concurrent.atomic包里面的**原子类**都是利用乐观锁实现的。

![img](MarkDown_Java%20SE.assets/v2-98cd919fe09521bac03aa66d6968aeb2_1440w.jpg)

原子类AtomicInteger的自增方法为乐观锁策略

为什么网上有些资料认为偏向锁、轻量级锁是乐观锁？理由是它们底层用到了CAS？或者是把“乐观/悲观”与“轻量/重量”搞混了？其实，线程在抢占这些锁的时候，确实是循环+CAS的操作，感觉好像是乐观锁。但问题的关键是，我们说一个锁是悲观锁还是乐观锁，总是应该站在应用层，看它们是如何锁住应用数据的，而不是站在底层看抢占锁的过程。如果一个线程尝试获取锁时，发现已经被占用，它是否继续读取数据，等后续要更新时再决定要不要重试？对于偏向锁、轻量级锁来说，显然答案是否定的。无论是挂起还是忙等，对应用数据的读取操作都被“挡住”了。从这个角度看，它们确实是悲观锁。





### AQS











## 多线程

```java
//TODO:3.28
```



### 线程的状态

![img](MarkDown_Java%20SE.assets/v2-1319f27379e4745d02b40ea12b9307cb_720w.jpg)



**初始(NEW)**：新创建了一个线程对象，但还没有调用start()方法。



**运行(RUNNABLE)**：Java线程中将就绪（ready）和运行中（running）两种状态笼统的称为“运行”。
线程对象创建后，其他线程(比如main线程）调用了该对象的start()方法。该状态的线程位于可运行线程池中，等待被线程调度选中，获取CPU的使用权，此时处于就绪状态（ready）。就绪状态的线程在获得CPU时间片后变为运行中状态（running）。



**阻塞(BLOCKED)**：表示线程阻塞于锁。阻塞状态是线程阻塞在进入synchronized关键字修饰的方法或代码块(获取锁)时的状态。



**等待(WAITING)**：进入该状态的线程需要等待其他线程做出一些特定动作（通知或中断）。处于这种状态的线程不会被分配CPU执行时间，它们要等待被显式地唤醒，否则会处于无限期等待的状态。



**超时等待(TIMED_WAITING)**：该状态不同于WAITING，它可以在指定的时间后自行返回。处于这种状态的线程不会被分配CPU执行时间，不过无须无限期等待被其他线程显示地唤醒，在达到一定时间后它们会自动唤醒。https://maokun.blog.csdn.net/article/details/99697007

带指定的等待时间的等待线程所处的状态。一个线程处于这一状态是因为用一个指定的正的等待时间（为参数）调用了以下方法中的其一：

- Thread.sleep
- 带时限（timeout）的 Object.wait
- 带时限（timeout）的 Thread.join
- LockSupport.parkNanos
- LockSupport.parkUntil

那么 wait(1000）相当于自带设有倒计时 1000 毫秒的闹钟，换言之，她在同时等待两个通知，并取决于哪个先到：

- 如果在1000毫秒内，她就收到了乘务员线程的通知从而唤醒，闹钟也随之失效；
- 反之，超过1000毫秒，还没收到通知，则闹钟响起，此时她则被闹钟唤醒。

换言之，wait 应该总是在循环中调用（waits should always occur in loops），javadoc 中给出了样板代码：

```java
synchronized(obj){
	while(<condition doesnothold>) {
	obj.wait(timeout);
	...// Perform action appropriate to condition
	}
}
```

简单讲，要避免使用 if 的方式来判断条件，否则一旦线程恢复，就继续往下执行，不会再次检测条件。由于可能存在的“虚假唤醒”，并不意味着条件是满足的，这点甚至对简单的“二人转”的两个线程的 wait/notify 情况也需要注意。



**终止(TERMINATED)**：表示该线程已经执行完毕。



#### 多线程常见指令

首先，wait()和notify()，notifyAll()是**Object**类的方法，sleep()和yield()是**Thread**类的方法。

**sleep()**

Thread.sleep(long millis)，一定是当前线程调用此方法，当前线程进入TIMED_WAITING状态，但**不释放对象锁**，millis后线程自动苏醒进入就绪状态。作用：给其它线程执行机会的最佳方式。

操作系统中，CPU竞争有很多种策略。Unix系统使用的是时间片算法，而Windows则属于抢占式的。在时间片算法中，所有的进程排成一个队列。操作系统按照他们的顺序，给每个进程分配一段时间，即该进程允许运行的时间。如果在 时间片结束时进程还在运行，则CPU将被剥夺并分配给另一个进程。如果进程在时间片结束前阻塞或结束，则CPU当即进行切换。调度程 序所要做的就是维护一张就绪进程列表，当进程用完它的时间片后，它被移到队列的末尾。
所谓抢占式操作系统，就是说如果一个进程得到了 CPU 时间，除非它自己放弃使用 CPU ，否则将完全霸占 CPU 。当进程执行完毕或者自己主动挂起后，操作系统就会重新计算一 次所有进程的总优先级，然后再挑一个优先级最高的把 CPU 控制权交给他。

Sleep函数就是干这事的，他告诉操作系统“在未来的多少毫秒内我不参与CPU竞争”。

Thread.Sleep(0)的作用，就是“触发操作系统立刻重新进行一次CPU竞争”。竞争的结果也许是当前线程仍然获得CPU控制权，也许会换成别的线程获得CPU控制权。这也是我们在大循环里面经常会写一句Thread.Sleep(0) ，因为这样就给了其他线程比如Paint线程获得CPU控制权的权力，这样界面就不会假死在那里。


**wait**()

obj.wait()，当前线程调用对象的wait()方法，wait()的作用是让当前线程进入等待状态，同时，**wait()也会让当前线程释放它所持有的锁**，进入等待队列。而notify()和notifyAll()的作用，则是唤醒当前对象上的等待线程；notify()是唤醒单个线程，而notifyAll()是唤醒所有的线程，或者wait(long timeout) timeout时间到自动唤醒。

没有参数的 wait() 等价于 wait(0)，而 wait(0) 它不是等0毫秒，恰恰相反，它的意思是永久的等下去，到天荒地老，除非收到通知。





**notify()**

唤醒在此对象锁上等待的单个线程。



**notifyAll()**

唤醒在此对象锁上等待的所有线程。

synchronized, wait, notify 是任何对象都具有的同步工具。让我们先来了解他们

![img](MarkDown_Java%20SE.assets/v2-d7068850e10b7784451da6f34d7e0e24_720w.jpg)

他们是应用于同步问题的人工线程调度工具。讲其本质，首先就要明确monitor的概念，Java中的每个对象都有一个监视器，来监测并发代码的重入。在非多线程编码时该监视器不发挥作用，反之如果在synchronized 范围内，监视器发挥作用。

wait/notify必须存在于synchronized块中。并且，这三个关键字针对的是同一个监视器（某对象的监视器）。这意味着wait之后，其他线程可以进入同步块执行。

**当某代码并不持有监视器的使用权时（如图中5的状态，即脱离同步块）去wait或notify，会抛出java.lang.IllegalMonitorStateException。也包括在synchronized块中去调用另一个对象的wait/notify，因为不同对象的监视器不同，同样会抛出此异常。**





**join()**

thread.join()/thread.join(long millis)，当前线程里调用其它线程t的join方法，当前线程进入WAITING/TIMED_WAITING状态，当前线程不会释放已经持有的对象锁。线程t执行完毕或者millis时间到，当前线程一般情况下进入RUNNABLE状态，也有可能进入BLOCKED状态（因为join是基于wait实现的）。

插队

```java
package ThreadTest;

public class testJoin implements Runnable {


    @Override
    public void run() {
        for (int i = 0; i < 20; ++i) {
            System.out.println("线程vip来了" + i);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        testJoin testJoin = new testJoin();
        Thread thread = new Thread(testJoin);
        thread.start();

        for (int i = 0; i < 100; i++) {
            if (i == 10) {
                thread.join();
            }
            System.out.println("main" + i);

        }
    }
}

```

```java
main0
线程vip来了0
main1
线程vip来了1
main2
线程vip来了2
main3
线程vip来了3
main4
线程vip来了4
main5
main6
线程vip来了5
main7
线程vip来了6
main8
线程vip来了7
main9
线程vip来了8
线程vip来了9
线程vip来了10
线程vip来了11
线程vip来了12
线程vip来了13
线程vip来了14
线程vip来了15
线程vip来了16
线程vip来了17
线程vip来了18
线程vip来了19
main10
main11
main12
main13
main14
main15
main16
main17
main18
main19
main20
main21
main22
main23
main24
main25
main26
main27
main28
main29
main30
main31
main32
main33
main34
main35
main36
main37
main38
main39
main40
main41
main42
main43
main44
main45
main46
main47
main48
main49
main50
main51
main52
main53
main54
main55
main56
main57
main58
main59
main60
main61
main62
main63
main64
main65
main66
main67
main68
main69
main70
main71
main72
main73
main74
main75
main76
main77
main78
main79
main80
main81
main82
main83
main84
main85
main86
main87
main88
main89
main90
main91
main92
main93
main94
main95
main96
main97
main98
main99

Process finished with exit code 0

```







**yield()**

Thread.yield()，一定是当前线程调用此方法，当前线程放弃获取的CPU时间片，但不释放锁资源，由运行状态变为就绪状态，让OS再次选择线程。作用：让相同优先级的线程轮流执行，但并不保证一定会轮流执行。实际中无法保证yield()达到让步目的，因为让步的线程还有可能被线程调度程序再次选中。Thread.yield()不会导致阻塞。该方法与sleep()类似，只是不能由用户指定暂停多长时间。

实际上，yield()方法对应了如下操作： 先检测当前是否有相同优先级的线程处于同可运行状态，如有，则把 CPU 的占有权交给此线程，否则继续运行原来的线程。所以yield()方法称为"退让",它把运行机会让给了同等优先级的其他线程。

1、sleep()方法会给其他线程运行的机会，而不考虑其他线程的优先级，因此会给较低线程一个运行的机会;yield()方法只会给相同优先级或者更高优先级的线程一个运行的机会。

2、当线程执行了sleep(long millis)方法后，将转到阻塞状态，参数millis指定睡眠时间;当线程执行了yield()方法后，将转到就绪状态。

3、sleep()方法声明抛出InterruptedException异常，而yield()方法没有声明抛出任何异常



#### [1114. 按序打印](https://leetcode-cn.com/problems/print-in-order/)

```java
class Foo {

    boolean a = false;
    boolean b = false;
    public Foo() {
        
    }

    public void first(Runnable printFirst) throws InterruptedException {
        synchronized(this) {
            if (!a) {
                // printFirst.run() outputs "first". Do not change or remove this line.
                printFirst.run();
                a = true;
                this.notifyAll();
            }
        }

    }

    public void second(Runnable printSecond) throws InterruptedException {
        synchronized(this) {
            while (!a) {
                this.wait();
            }
            // printSecond.run() outputs "second". Do not change or remove this line.
            printSecond.run();
            b = true;
            this.notifyAll();
        }

    }

    public void third(Runnable printThird) throws InterruptedException {
        synchronized(this) {
            while (!b) {
                this.wait();
            }
            // printThird.run() outputs "third". Do not change or remove this line.
            printThird.run();
        }
    }
}
```



ReentrantLock

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Foo {

    int num;
    Lock lock;
    //精确的通知和唤醒线程
    Condition condition1, condition2, condition3;

    public Foo() {
        num = 1;
        lock = new ReentrantLock();
        condition1 = lock.newCondition();
        condition2 = lock.newCondition();
        condition3 = lock.newCondition();
    }

    public void first(Runnable printFirst) throws InterruptedException {
        lock.lock();
        try {
            while (num != 1) {
                condition1.await();
            }
            // printFirst.run() outputs "first". Do not change or remove this line.
            printFirst.run();
            num = 2;
            condition2.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void second(Runnable printSecond) throws InterruptedException {
        lock.lock();
        try {
            while (num != 2) {
                condition2.await();
            }
            // printSecond.run() outputs "second". Do not change or remove this line.
            printSecond.run();
            num = 3;
            condition3.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void third(Runnable printThird) throws InterruptedException {
        lock.lock();
        try {
            while (num != 3) {
                condition3.await();
            }
            // printThird.run() outputs "third". Do not change or remove this line.
            printThird.run();
            // num = 4;
            // condition1.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

```



ThreadLocal

https://mp.weixin.qq.com/s/__TDcasN5wWsuogTkRNFaA









### Java的Future机制

https://www.jianshu.com/p/43dab9b7c25bv

#### 一、为什么出现Future机制

常见的两种创建线程的方式。一种是直接继承Thread，另外一种就是实现Runnable接口。

这两种方式都有一个缺陷就是：在执行完任务之后无法获取执行结果。

从Java 1.5开始，就提供了Callable和Future，通过它们可以在任务执行完毕之后得到任务执行结果。

Future模式的核心思想是能够让主线程将原来需要同步等待的这段时间用来做其他的事情。（因为可以异步获得执行结果，所以不用一直同步等待去获得执行结果）

![img](MarkDown_Java%20SE.assets/5679451-9c2a775b0599a2df.png)

上图简单描述了不使用Future和使用Future的区别，不使用Future模式，主线程在invoke完一些耗时逻辑之后需要等待，这个耗时逻辑在实际应用中可能是一次RPC调用，可能是一个本地IO操作等。B图表达的是使用Future模式之后，我们主线程在invoke之后可以立即返回，去做其他的事情，回头再来看看刚才提交的invoke有没有结果。



#### 二、Future的相关类图

##### 2.1 Future 接口

首先，我们需要清楚，Futrue是个接口。Future就是对于具体的Runnable或者Callable任务的执行结果进行取消、查询是否完成、获取结果。必要时可以通过get方法获取执行结果，该方法会阻塞直到任务返回结果。

![img](MarkDown_Java%20SE.assets/5679451-62f48ec8755546e4.png)

接口定义行为，我们通过上图可以看到实现Future接口的子类会具有哪些行为：

- 我们可以取消这个执行逻辑，如果这个逻辑已经正在执行，提供可选的参数来控制是否取消已经正在执行的逻辑。
- 我们可以判断执行逻辑是否已经被取消。
- 我们可以判断执行逻辑是否已经执行完成。
- 我们可以获取执行逻辑的执行结果。
- 我们可以允许在一定时间内去等待获取执行结果，如果超过这个时间，抛`TimeoutException`。

##### 2.2 FutureTask 类

类图如下：

<img src="MarkDown_Java%20SE.assets/image-20210328161750119.png" alt="image-20210328161750119" style="zoom: 50%;" />

FutureTask是Future的具体实现。`FutureTask`实现了`RunnableFuture`接口。`RunnableFuture`接口又同时继承了`Future` 和 `Runnable` 接口。所以`FutureTask`既可以作为Runnable被线程执行，又可以作为Future得到Callable的返回值。



#### 三、FutureTask的使用方法

举个例子，假设我们要执行一个算法，算法需要两个输入 `input1` 和 `input2`, 但是`input1`和`input2`需要经过一个非常耗时的运算才能得出。由于算法必须要两个输入都存在，才能给出输出，所以我们必须等待两个输入的产生。接下来就模仿一下这个过程。

```java
package ThreadPool;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class FutureTaskTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        long starttime = System.currentTimeMillis();

        //input2生成， 需要耗费3秒
        FutureTask<Integer> input2_futuretask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(3000);
                return 5;
            }
        });

        new Thread(input2_futuretask).start();

        //input1生成，需要耗费2秒
        FutureTask<Integer> input1_futuretask = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(2000);
                return 3;
            }
        });
        new Thread(input1_futuretask).start();

        Integer integer2 = input2_futuretask.get();
        Integer integer1 = input1_futuretask.get();
        System.out.println(algorithm(integer1, integer2));
        long endtime = System.currentTimeMillis();
        System.out.println("用时：" + String.valueOf(endtime - starttime));
    }

    //这是我们要执行的算法
    public static int algorithm(int input, int input2) {
        return input + input2;
    }
}
```

```java
8
用时：3005
```

我们可以看到用时3001毫秒，与最费时的input2生成时间差不多。
注意，我们在程序中生成input1时，也让线程休眠了2秒，但是结果不是3+2。说明FutureTask是被异步执行了。



#### 四、FutureTask源码分析

##### 4.1 state字段

volatile修饰的state字段；表示FutureTask当前所处的状态。可能的转换过程见注释。



```java
/**
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;
```



##### 4.2 其他变量

```java
    /** 任务 */
    private Callable<V> callable;
    /** 储存结果*/
    private Object outcome; // non-volatile, protected by state reads/writes
    /** 执行任务的线程*/
    private volatile Thread runner;
    /** get方法阻塞的线程队列 */
    private volatile WaitNode waiters;

    //FutureTask的内部类，get方法的等待队列
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }
```





##### 4.3 CAS工具初始化



```java
    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
```

这段代码是为了后面使用CAS而准备的。可以这么理解：
 一个java对象可以看成是一段内存，各个字段都得按照一定的顺序放在这段内存里，同时考虑到对齐要求，可能这些字段不是连续放置的，用这个`UNSAFE.objectFieldOffset（）`方法能准确地告诉你某个字段相对于对象的起始内存地址的字节偏移量，因为是相对偏移量，所以它其实跟某个具体对象又没什么太大关系，跟class的定义和虚拟机的内存模型的实现细节更相关。



##### 4.4 构造函数

FutureTask有两个构造函数：

```java
public FutureTask(Callable<V> callable) {
    if (callable == null)
        throw new NullPointerException();
    this.callable = callable;
    this.state = NEW;       // ensure visibility of callable
}
    
public FutureTask(Runnable runnable, V result) {
    this.callable = Executors.callable(runnable, result);
    this.state = NEW;       // ensure visibility of callable
}
```

这两个构造函数区别在于，如果使用第一个构造函数最后获取线程实行结果就是callable的执行的返回结果；而如果使用第二个构造函数那么最后获取线程实行结果就是参数中的result，接下来让我们看一下FutureTask的run方法。

同时两个构造函数都把当前状态设置为NEW。





##### 4.5  run方法及其他

构造完FutureTask后，会把它当做线程的参数传进去，然后线程就会运行它的run方法。所以我们先来看一下run方法：



```java
public void run() {
        //如果状态不是new，或者runner旧值不为null(已经启动过了)，就结束
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable; // 这里的callable是从构造方法里面传人的
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call(); //执行任务，并将结果保存在result字段里。
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex); // 保存call方法抛出的异常
                }
                if (ran)
                    set(result); // 保存call方法的执行结果
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }
```

其中，catch语句中的setException(ex)如下：



```java
//发生异常时设置state和outcome
protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); 
            finishCompletion();// 唤醒get()方法阻塞的线程
        }
    }
```

而正常完成时，set(result);方法如下：



```java
//正常完成时，设置state和outcome
protected void set(V v) {
//正常完成时，NEW->COMPLETING->NORMAL
 if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
     outcome = v;
     UNSAFE.putOrderedInt(this, stateOffset, NORMAL); 
            finishCompletion(); // 唤醒get方法阻塞的线程
        }
    }
```

这两个set方法中，都是用到了`finishCompletion()`去唤醒get方法阻塞的线程。下面来看看这个方法：



```java
//移除并唤醒所有等待的线程，调用done，并清空callable
private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null;) {
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                for (;;) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t); //唤醒线程
                    }
                    //接下来的这几句代码是将当前节点剥离出队列，然后将q指向下一个等待节点。被剥离的节点由于thread和next都为null，所以会被GC回收。
                    WaitNode next = q.next;
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done(); //这个是空的方法，子类可以覆盖，实现回调的功能。
        callable = null;        // to reduce footprint
    }
```

好，到这里我们把运行以及设置结果的流程分析完了。那接下来看一下怎么获得执行结果把。也就是`get`方法。

get方法有两个，一个是有超时时间设置，另一个没有超时时间设置。



```java
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }
```



```java
public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // get(timeout, unit) 也很简单, 主要还是在 awaitDone里面
        if(unit == null){
            throw new NullPointerException();
        }
        int s = state;
        // 判断state状态是否 <= Completing, 调用awaitDone进行自旋等待
        if(s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING){
            throw new TimeoutException();
        }
        // 根据state的值进行返回结果或抛出异常
        return report(s);
    }
```

两个get方法都用到了`awaitDone()`。这个方法的作用是： 等待任务执行完成、被中断或超时。看一下源码：



```java
    //等待完成，可能是是中断、异常、正常完成，timed:true，考虑等待时长，false:不考虑等待时长
    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L; //如果设置了超时时间
        WaitNode q = null;
        boolean queued = false;
        for (;;) {
         /**
         *  有优先级顺序
         *  1、如果线程已中断，则直接将当前节点q从waiters中移出
         *  2、如果state已经是最终状态了，则直接返回state
         *  3、如果state是中间状态(COMPLETING),意味很快将变更过成最终状态，让出cpu时间片即可
         *  4、如果发现尚未有节点，则创建节点
         *  5、如果当前节点尚未入队，则将当前节点放到waiters中的首节点，并替换旧的waiters
         *  6、线程被阻塞指定时间后再唤醒
         *  7、线程一直被阻塞直到被其他线程唤醒
         *
         */
            if (Thread.interrupted()) {// 1
                removeWaiter(q);
                throw new InterruptedException();
            }

            int s = state;
            if (s > COMPLETING) {// 2
                if (q != null)
                    q.thread = null;
                return s; 
            }
            else if (s == COMPLETING) // 3
                Thread.yield();
            else if (q == null) // 4
                q = new WaitNode();
            else if (!queued) // 5
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            else if (timed) {// 6
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q); //从waiters中移出节点q
                    return state; 
                }
                LockSupport.parkNanos(this, nanos); 
            }
            else // 7
                LockSupport.park(this);
        }
    }
```

接下来看下`removeWaiter()`移除等待节点的源码：



```java
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null; // 将移除的节点的thread＝null, 为移除做标示
            retry:
            for (;;) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    //通过 thread 判断当前 q 是否是需要移除的 q节点，因为我们刚才标示过了
                    if (q.thread != null) 
                        pred = q; //当不是我们要移除的节点，就往下走
                    else if (pred != null) {
                        //当p.thread==null时，到这里。下面这句话，相当于把q从队列移除。
                        pred.next = s;
                        //pred.thread == null 这种情况是在多线程进行并发 removeWaiter 时产生的
                        //此时正好移除节点 node 和 pred, 所以loop跳到retry, 从新进行这个过程。想象一下，如果在并发的情况下，其他线程把pred的线程置为空了。那说明这个链表不应该包含pred了。所以我们需要跳到retry从新开始。
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    //到这步说明p.thread==null 并且 pred==null。说明node是头结点。
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                                                          q, s))
                        continue retry;
                }
                break;
            }
        }
    }
```

最后在get方法中调用`report(s)`，根据状态s的不同进行返回结果或抛出异常。



```java
    private V report(int s) throws ExecutionException {
        Object x = outcome;  //之前我们set的时候，已经设置过这个值了。所以直接用。
        if (s == NORMAL)
            return (V)x;  //正常执行结束，返回结果
        if (s >= CANCELLED)
            throw new CancellationException(); //被取消或中断了，就抛异常。
        throw new ExecutionException((Throwable)x);
    }
```

以上就是FutureTask的源码分析。经过了一天的折腾，算是弄明白了。
 最后总结一下：

FutureTask既可以当做Runnable也可以当做Future。线程通过执行FutureTask的run方法，将正常运行的结果放入FutureTask类的result变量中。使用get方法可以阻塞直到获得结果。





### 线程池

https://zhuanlan.zhihu.com/p/73990200

Java线程池实现原理及其在美团业务中的实践 - 美团技术团队的文章 - 知乎 https://zhuanlan.zhihu.com/p/123328822

#### 线程池特点

**线程池：** 简单理解，它就是一个管理线程的池子。

- **降低资源消耗**：通过池化技术重复利用已创建的线程，降低线程创建和销毁造成的损耗。
- **提高响应速度**：任务到达时，无需等待线程创建即可立即执行。
- **提高线程的可管理性**：线程是稀缺资源，如果无限制创建，不仅会消耗系统资源，还会因为线程的不合理分布导致资源调度失衡，降低系统的稳定性。使用线程池可以进行统一的分配、调优和监控。
- **提供更多更强大的功能**：线程池具备可拓展性，允许开发人员向其中增加更多的功能。比如延时定时线程池ScheduledThreadPoolExecutor，就允许任务延期执行或定期执行。



#### 池化思想

为解决资源分配这个问题，线程池采用了“池化”（Pooling）思想。池化，顾名思义，是为了最大化收益并最小化风险，而将资源统一在一起管理的一种思想。

> Pooling is the grouping together of resources (assets, equipment, personnel, effort, etc.) for the purposes of maximizing advantage or minimizing risk to the users. The term is used in finance, computing and equipment management.——wikipedia

“池化”思想不仅仅能应用在计算机领域，在金融、设备、人员管理、工作管理等领域也有相关的应用。

在计算机领域中的表现为：统一管理IT资源，包括服务器、存储、和网络资源等等。通过共享资源，使用户在低投入中获益。除去线程池，还有其他比较典型的几种使用策略包括：

1. 内存池(Memory Pooling)：预先申请内存，提升申请内存速度，减少内存碎片。
2. 连接池(Connection Pooling)：预先申请数据库连接，提升申请连接的速度，降低系统的开销。
3. 实例池(Object Pooling)：循环使用对象，减少资源在初始化和释放时的昂贵损耗。



**2.1 总体设计**

Java中的线程池核心实现类是ThreadPoolExecutor，本章基于JDK 1.8的源码来分析Java线程池的核心设计与实现。我们首先来看一下ThreadPoolExecutor的UML类图，了解下ThreadPoolExecutor的继承关系。

![img](MarkDown_Java%20SE.assets/v2-e3ba513194a1f918b0abfc42b6fecd0a_1440w.jpg)

ThreadPoolExecutor实现的顶层接口是Executor，顶层接口Executor提供了一种思想：将任务提交和任务执行进行解耦。用户无需关注如何创建线程，如何调度线程来执行任务，用户只需提供Runnable对象，将任务的运行逻辑提交到执行器(Executor)中，由Executor框架完成线程的调配和任务的执行部分。

ExecutorService接口增加了一些能力：（1）扩充执行任务的能力，补充可以为一个或一批异步任务生成Future的方法；（2）提供了管控线程池的方法，比如停止线程池的运行。

AbstractExecutorService则是上层的抽象类，将执行任务的流程串联了起来，保证下层的实现只需关注一个执行任务的方法即可。

最下层的实现类ThreadPoolExecutor实现最复杂的运行部分，ThreadPoolExecutor将会一方面维护自身的生命周期，另一方面同时管理线程和任务，使两者良好的结合从而执行并行任务。

![img](MarkDown_Java%20SE.assets/v2-4e788c3de25c337889e31ca0e77ceabd_1440w.jpg)













#### 构造函数

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

##### 几个核心参数的作用：

**corePoolSize：** 线程池核心线程数最大值

**maximumPoolSize：** 线程池最大线程数大小

**keepAliveTime：** 线程池中非核心线程空闲的存活时间大小

**unit：** 线程空闲存活时间单位

**workQueue：** 存放任务的阻塞队列

**threadFactory：** 用于设置创建线程的工厂，可以给创建的线程设置有意义的名字，可方便排查问题。

**handler：** 线城池的饱和策略事件，主要有四种类型。



#### 任务执行

#### 线程池执行流程，即对应execute()方法：

![image-20210328000924642](MarkDown_Java%20SE.assets/image-20210328000924642.png)



- 提交一个任务，线程池里存活的核心线程数小于线程数corePoolSize时，线程池会创建一个核心线程去处理提交的任务。
- 如果线程池核心线程数已满，即线程数已经等于corePoolSize，一个新提交的任务，会被放进任务队列workQueue排队等待执行。
- 当线程池里面存活的线程数已经等于corePoolSize了,并且任务队列workQueue也满，判断线程数是否达到maximumPoolSize，即最大线程数是否已满，如果没到达，创建一个非核心线程执行提交的任务。
- 如果当前的线程数达到了maximumPoolSize，还有新的任务过来的话，直接采用拒绝策略处理。



```java
//TODO:4.3
```



#### 四种拒绝策略

- AbortPolicy(抛出一个异常，默认的)
- DiscardPolicy(直接丢弃任务)
- DiscardOldestPolicy（丢弃队列里最老的任务，将当前这个任务继续提交给线程池）
- CallerRunsPolicy（交给线程池调用所在的线程进行处理)



#### 为了形象描述线程池执行，我打个比喻：

- 核心线程比作公司正式员工
- 非核心线程比作外包员工
- 阻塞队列比作需求池
- 提交任务比作提需求

![image-20210328001320952](MarkDown_Java%20SE.assets/image-20210328001320952.png)

- 当产品提个需求，正式员工（核心线程）先接需求（执行任务）
- 如果正式员工都有需求在做，即核心线程数已满），产品就把需求先放需求池（阻塞队列）。
- 如果需求池(阻塞队列)也满了，但是这时候产品继续提需求,怎么办呢？那就请外包（非核心线程）来做。
- 如果所有员工（最大线程数也满了）都有需求在做了，那就执行拒绝策略。
- 如果外包员工把需求做完了，它经过一段（keepAliveTime）空闲时间，就离开公司了。





#### 线程池异常处理

在使用线程池处理任务的时候，任务代码可能抛出RuntimeException，抛出异常后，线程池可能捕获它，也可能创建一个新的线程来代替异常的线程，我们可能无法感知任务出现了异常，因此我们需要考虑线程池异常情况。

**当提交新任务时，异常如何处理?**

我们先来看一段代码：

```java
//不加声明的话，无法感知任务出现了异常
ExecutorService threadPool = Executors.newFixedThreadPool(5);
for (int i = 0; i < 5; i++) {
    threadPool.submit(() -> {
        System.out.println("current thread name" + Thread.currentThread().getName());
        Object object = null;
        System.out.print("result## "+object.toString());
    });
}
```

```java
current thread namepool-1-thread-1
current thread namepool-1-thread-5
current thread namepool-1-thread-4
current thread namepool-1-thread-2
current thread namepool-1-thread-3
```

##### 方式一：try-catch

虽然没有结果输出，但是没有抛出异常，所以我们无法感知任务出现了异常，所以需要添加try/catch。 如下图：

```java
//        方式一：通过try/catch捕获异常
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            threadPool.submit(() -> {
                System.out.println("current thread name" + Thread.currentThread().getName());
                try {
                    Object object = null;
                    System.out.print("result## "+object.toString());
                }catch (NullPointerException e) {
                    System.out.println(e.toString());
                }
            });
        }
```

```java
current thread namepool-1-thread-2
java.lang.NullPointerException
current thread namepool-1-thread-5
current thread namepool-1-thread-4
java.lang.NullPointerException
current thread namepool-1-thread-3
java.lang.NullPointerException
java.lang.NullPointerException
current thread namepool-1-thread-1
java.lang.NullPointerException
```





##### 方式二：通过Future对象的get方法接收抛出的异常，再进行处理

**submit执行的任务，可以通过Future对象的get方法接收抛出的异常，再进行处理。** 我们再通过一个demo，看一下Future对象的get方法处理异常的姿势

```java
//        方式二：通过Future对象的get方法接收抛出的异常，再处理
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            Future future = threadPool.submit(() -> {
                System.out.println("current thread name" + Thread.currentThread().getName());
                Object object = null;
                System.out.print("result## " + object.toString());
            });
            try {
                future.get();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
```

```java
current thread namepool-1-thread-1
java.util.concurrent.ExecutionException: java.lang.NullPointerException
current thread namepool-1-thread-2
java.util.concurrent.ExecutionException: java.lang.NullPointerException
current thread namepool-1-thread-3
java.util.concurrent.ExecutionException: java.lang.NullPointerException
current thread namepool-1-thread-4
java.util.concurrent.ExecutionException: java.lang.NullPointerException
current thread namepool-1-thread-5
java.util.concurrent.ExecutionException: java.lang.NullPointerException
```



#####  方式三：UncaughtExceptionHandler 

为工作者线程设置UncaughtExceptionHandler，在uncaughtException方法中处理异常

```java
//        3.为工作者线程设置UncaughtExceptionHandler，在uncaughtException方法中处理异常
        ExecutorService threadPool = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler(
                    (t1, e) -> {
                        System.out.println(t1.getName() + "线程抛出的异常" + e);
                    });
            return t;
        });
        threadPool.execute(() -> {
            Object object = null;
            System.out.print("result## " + object.toString());
        });

```

```java
Thread-0线程抛出的异常java.lang.NullPointerException
```



##### 方式四：重写线程池ThreadPoolExecutor的afterExecute方法

重写ThreadPoolExecutor https://www.jianshu.com/p/f030aa5d7a28

重写ThreadPoolExecutor的afterExecute方法，处理传递的异常引用

这是jdk文档的一个demo：

```java
class ExtendedExecutor extends ThreadPoolExecutor {
    // 这可是jdk文档里面给的例子。。
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            try {
                Object result = ((Future<?>) r).get();
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // ignore/reset
            }
        }
        if (t != null)
            System.out.println(t);
    }
}}
```

![image-20210328195157145](MarkDown_Java%20SE.assets/image-20210328195157145.png)

##### 因此，被问到线程池异常处理，如何回答？

![img](MarkDown_Java%20SE.assets/v2-4a46342775c6f9ad8543654890833b6b_720w.jpg)



#### 线程池的工作队列

##### **线程池都有哪几种工作队列？**

- ArrayBlockingQueue

- LinkedBlockingQueue

- DelayQueue

- PriorityBlockingQueue

- SynchronousQueue

  
  
  
  
  ![img](MarkDown_Java%20SE.assets/v2-2a502a31771713d4f631034485b8b796_1440w.jpg)



###### ArrayBlockingQueue

ArrayBlockingQueue（有界队列）是一个用数组实现的有界阻塞队列，按FIFO排序量。

###### LinkedBlockingQueue

LinkedBlockingQueue（可设置容量队列）基于链表结构的阻塞队列，按FIFO排序任务，容量可以选择进行设置，不设置的话，将是一个无边界的阻塞队列，最大长度为Integer.MAX_VALUE，吞吐量通常要高于ArrayBlockingQuene；**newFixedThreadPool和newSingleThreadExecutor线程池**使用了这个队列

###### DelayQueue

DelayQueue（延迟队列）是一个任务定时周期的延迟执行的队列。根据指定的执行时间从小到大排序，否则根据插入到队列的先后排序。**newScheduledThreadPool线程池**使用了这个队列。

###### PriorityBlockingQueue

PriorityBlockingQueue（优先级队列）是具有优先级的无界阻塞队列；

###### SynchronousQueue

SynchronousQueue（同步队列）一个不存储元素的阻塞队列，每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态，吞吐量通常要高于LinkedBlockingQuene，**newCachedThreadPool线程池**使用了这个队列。

针对面试题：**线程池都有哪几种工作队列？** 我觉得，**回答以上几种ArrayBlockingQueue，LinkedBlockingQueue，SynchronousQueue等，说出它们的特点，并结合使用到对应队列的常用线程池(如newFixedThreadPool线程池使用LinkedBlockingQueue)，进行展开阐述，** 就可以啦。





##### 几种常用的线程池

- newFixedThreadPool (固定数目线程的线程池)
- newCachedThreadPool(可缓存线程的线程池)
- newSingleThreadExecutor(单线程的线程池)
- newScheduledThreadPool(定时及周期执行的线程池)



###### newFixedThreadPool

```java
public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>(),
                                  threadFactory);
}
```

**线程池特点：**

- 核心线程数和最大线程数大小一样
- 没有所谓的非空闲时间，即keepAliveTime为0
- 阻塞队列为无界队列**LinkedBlockingQueue**



**工作机制：**

![img](MarkDown_Java%20SE.assets/v2-c79663b197b24233f1e0c0a15099b5dd_720w.jpg)

- 提交任务
- 如果线程数少于核心线程，创建核心线程执行任务
- 如果线程数等于核心线程，把任务添加到LinkedBlockingQueue阻塞队列
- 如果线程执行完任务，去阻塞队列取任务，继续执行。

实例代码

```java
package ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class newFixedThreadPoolTest {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(100);
                    System.out.println(Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    //do nothing
                }
            });
        }
    }
}
```





IDE指定JVM参数：-Xmx8m -Xms8m

![image-20210328205442695](MarkDown_Java%20SE.assets/image-20210328205442695.png)

run以上代码，会抛出OOM：

```java
pool-1-thread-2
pool-1-thread-9
pool-1-thread-3
pool-1-thread-7
pool-1-thread-1
pool-1-thread-10
pool-1-thread-5
pool-1-thread-6
pool-1-thread-4
pool-1-thread-8

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "main"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "pool-1-thread-10"

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "pool-1-thread-7"

Process finished with exit code 1
```

**面试题：使用无界队列的线程池会导致内存飙升吗？**

答案 **：会的，newFixedThreadPool使用了无界的阻塞队列LinkedBlockingQueue，如果线程获取一个任务后，任务的执行时间比较长(比如，上面demo设置了10秒)，会导致队列的任务越积越多，导致机器内存使用不停飙升，** 最终导致OOM。

**使用场景**

FixedThreadPool 适用于处理**CPU密集型**的任务，确保CPU在长期被工作线程使用的情况下，尽可能的少的分配线程，即适用执行长期的任务。





###### newCachedThreadPool

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}
```

**线程池特点：**

- 核心线程数为0
- 最大线程数为Integer.MAX_VALUE
- 阻塞队列是**SynchronousQueue**
- 非核心线程空闲存活时间为60秒

当提交任务的速度大于处理任务的速度时，每次提交一个任务，就必然会创建一个线程。极端情况下会创建过多的线程，耗尽 CPU 和内存资源。由于空闲 60 秒的线程会被终止，长时间保持空闲的 CachedThreadPool 不会占用任何资源。

![img](MarkDown_Java%20SE.assets/v2-ebd1b37b1baaf31854af0de28340238f_720w.jpg)

实例代码

```java
package ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class newCachedThreadPoolTest {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                System.out.println(Thread.currentThread().getName()+"正在执行");
            });
        }
    }
}
```

```java
pool-1-thread-3正在执行
pool-1-thread-1正在执行
pool-1-thread-4正在执行
pool-1-thread-2正在执行
pool-1-thread-5正在执行

Process finished with exit code 0
```

**使用场景**

用于并发执行大量短期的小任务。





###### newSingleThreadExecutor

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}
```

**线程池特点**

- 核心线程数为1
- 最大线程数也为1
- 阻塞队列是**LinkedBlockingQueue**
- keepAliveTime为0

![img](MarkDown_Java%20SE.assets/v2-a18e8b8a21853de2295d43ec10f843a6_720w.jpg)

- 提交任务
- 线程池是否有一条线程在，如果没有，新建线程执行任务
- 如果有，讲任务加到阻塞队列
- 当前的唯一线程，从队列取任务，执行完一个，再继续取，一个人（一条线程）夜以继日地干活。

```java
package ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class newSingleThreadExecutorTest {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                System.out.println(Thread.currentThread().getName() + "正在执行");
            });
        }
    }
}
```

```java
pool-1-thread-1正在执行
pool-1-thread-1正在执行
pool-1-thread-1正在执行
pool-1-thread-1正在执行
pool-1-thread-1正在执行
```

**使用场景**

适用于串行执行任务的场景，一个任务一个任务地执行。



###### newScheduledThreadPool

```java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
    return new ScheduledThreadPoolExecutor(corePoolSize);
}
```

**线程池特点**

- 最大线程数为Integer.MAX_VALUE
- 阻塞队列是DelayedWorkQueue
- keepAliveTime为0
- scheduleAtFixedRate() ：按某种速率周期执行
- scheduleWithFixedDelay()：在某个延迟后执行



**工作机制**

- 添加一个任务
- 线程池中的线程从 DelayQueue 中取任务
- 线程从 DelayQueue 中获取 time 大于等于当前时间的task
- 执行完后修改这个 task 的 time 为下次被执行的时间
- 这个 task 放回DelayQueue队列中



实例代码

```java
package ThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class newScheduledThreadPoolTest {
    public static void main(String[] args) {
        /**
         创建一个给定初始延迟的间隔性的任务，之后的下次执行时间是上一次任务从执行到结束所需要的时间+* 给定的间隔时间
         */
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            System.out.println("current Time" + System.currentTimeMillis());
            System.out.println(Thread.currentThread().getName() + "正在执行");
        }, 1, 3, TimeUnit.SECONDS);
    }
}
```

```java
current Time1616937268431
pool-1-thread-1正在执行
current Time1616937271467
pool-1-thread-1正在执行
current Time1616937274480
pool-1-thread-1正在执行
current Time1616937277490
pool-1-thread-1正在执行
current Time1616937280503
pool-1-thread-1正在执行
```

**使用场景**

周期性执行任务的场景，需要限制线程数量的场景



###### 回到面试题：**说说几种常见的线程池及使用场景？**

回答这四种经典线程池 **：newFixedThreadPool，newSingleThreadExecutor，newCachedThreadPool，newScheduledThreadPool，分线程池特点，工作机制，使用场景分开描述，再分析可能存在的问题，比如newFixedThreadPool内存飙升问题** 即可





#### 线程池状态（**生命周期**）

线程池运行的状态，并不是用户显式设置的，而是伴随着线程池的运行，由内部来维护。线程池内部使用一个变量维护两个值：运行状态(runState)和线程数量 (workerCount)。在具体实现中，线程池将运行状态(runState)、线程数量 (workerCount)两个关键参数的维护放在了一起，如下代码所示：

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```

`ctl`这个AtomicInteger类型，是对线程池的运行状态和线程池中有效线程的数量进行控制的一个字段， 它同时包含两部分的信息：线程池的运行状态 (runState) 和线程池内有效线程的数量 (workerCount)，高3位保存runState，低29位保存workerCount，两个变量之间互不干扰。用一个变量去存储两个值，可避免在做相关决策时，出现不一致的情况，不必为了维护两者的一致，而占用锁资源。通过阅读线程池源代码也可以发现，经常出现要同时判断线程池运行状态和线程数量的情况。线程池也提供了若干方法去供用户获得线程池当前的运行状态、线程个数。这里都使用的是位运算的方式，相比于基本运算，速度也会快很多。

线程池有这几个状态：RUNNING,SHUTDOWN,STOP,TIDYING,TERMINATED。

```java
// runState is stored in the high-order bits
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;
```



##### 线程池各个状态切换图：

![img](MarkDown_Java%20SE.assets/v2-b5783bcc607e99e365c16f791c4dfedd_720w.jpg)



**RUNNING**

- 该状态的线程池会接收新任务，并处理阻塞队列中的任务;
- 调用线程池的shutdown()方法，可以切换到SHUTDOWN状态;
- 调用线程池的shutdownNow()方法，可以切换到STOP状态;



**SHUTDOWN**

- 该状态的线程池不会接收新任务，但会处理阻塞队列中的任务；
- 队列为空，并且线程池中执行的任务也为空,进入TIDYING状态;



**STOP**

- 该状态的线程不会接收新任务，也不会处理阻塞队列中的任务，而且会中断正在运行的任务；
- 线程池中执行的任务为空,进入TIDYING状态;



**TIDYING**

- 该状态表明所有的任务已经运行终止，记录的任务数量为0。
- terminated()执行完毕，进入TERMINATED状态



**TERMINATED**

- 该状态表示线程池彻底终止





### AQS



## JAVA 8 新特性





### Lambda表达式(Lambda expressions)

匿名内部类并不是一个好的选择，因为：

- 语法过于冗余
- 匿名类中的this和变量名容易使人产生误解
- 类型载入和实例创建语义不够灵活
- 无法捕获非final的局部变量
- 无法对控制流进行抽象



#### **Lambda 变量类型如何识别**

在Lambda表达式中无需指定类型,程序依然可以编译。这是因为 javac 根据程序的上下文(方法的签名)在后台推断出了参数的类型。这意味着如果参数类型不言而明,则无需显式指定。即Lambda 表达式的类型依赖于上下文环境,是由编译器推断出来的。



#### **Lambda 引用值,而不是变量**

如果你曾使用过匿名内部类,也许遇到过这样的情况:需要引用它所在方法里的变量。这时,需要将变量声明为final。将变量声明为final,意味着不能为其重复赋值。同时也意味着在使用final变量时,实际上是在使用赋给该变量的一个特定的值。

Java 8虽然放松了这一限制,可以引用非final变量,但是该变量在既成事实上必须是 final。虽然无需将变量声明为final,但在Lambda表达式中,也无法用作非终态变量。如果坚持用作非终态变量,编译器就会报错。（local variables referenced from a lambda expression must be final or effectively final.）

既成事实上的 final 是指只能给该变量赋值一次。换句话说,Lambda 表达式引用的是值, 而不是变量。



#### **Lambda用在哪里**

我们知道Lambda表达式的目标类型是**函数性接口**——每一个Lambda都能通过一个特定的函数式接口与一个给定的类型进行匹配。因此一个Lambda表达式能被应用在与其目标类型匹配的任何地方，lambda表达式必须和函数式接口的抽象函数描述一样的参数类型，它的返回类型也必须和抽象函数的返回类型兼容，并且他能抛出的异常也仅限于在函数的描述范围中。



