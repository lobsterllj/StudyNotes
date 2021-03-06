# 线性结构

线性结构是一个有序数据元素的集合。

1．集合中必存在唯一的一个"第一个元素"；

2．集合中必存在唯一的一个"最后的元素"；

3．除最后元素之外，其它数据元素均有唯一的"后继"；

4．除第一元素之外，其它数据元素均有唯一的"前驱"。

数据结构中线性结构指的是数据元素之间存在着“一对一”的线性关系的数据结构。

如（a0,a1,a2,.....,an）,a0为第一个元素，an为最后一个元素，此集合即为一个线性结构的集合。

相对应于线性结构，非线性结构的逻辑特征是一个结点元素可能对应多个直接前驱和多个后继。

常用的线性结构有：[线性表](https://baike.baidu.com/item/线性表/3228081)，栈，[队列](https://baike.baidu.com/item/队列/14580481)，双队列，串(一维数组)。







## 循环队列

为充分利用向量空间，克服"[假溢出](https://baike.baidu.com/item/假溢出/11050937)"现象的方法是：将向量空间想象为一个首尾相接的圆环，并称这种向量为循环向量。存储在其中的队列称为循环队列（Circular Queue）。循环队列是把[顺序队列](https://baike.baidu.com/item/顺序队列/20832734)首尾相连，把存储队列元素的表从逻辑上看成一个环，成为循环队列。

循环队列就是将[队列](https://baike.baidu.com/item/队列/14580481)存储空间的最后一个位置绕到第一个位置，形成逻辑上的环状空间，供队列循环使用。在循环队列结构中，当存储空间的最后一个位置已被使用而再要进入队运算时，只需要存储空间的第一个位置空闲，便可将元素加入到第一个位置，即将存储空间的第一个位置作为队尾。 [1] 循环队列可以更简单防止伪溢出的发生，但队列大小是固定的。

在循环队列中，当队列为空时，有front=rear，而当所有队列空间全占满时，也有front=rear。为了区别这两种情况，规定循环队列最多只能有MaxSize-1个队列元素，当循环队列中只剩下一个空存储单元时，队列就已经满了。因此，队列判空的条件是front=rear，而队列判满的条件是front=（rear+1)%MaxSize。



# 非线性数据结构

关于广义表、数组(高维)，是一种非线性的数据结构。

常见的非线性结构有：二维数组，多维数组，广义表，树(二叉树等)，图

### 什么是广义表、广义表及定义详解

 [矩阵加法（基于十字链表）](http://data.biancheng.net/view/vip_231.html)[广义表的存储结构](http://data.biancheng.net/view/190.html) 

前面讲过，

[数组](http://data.biancheng.net/view/181.html)

即可以存储不可再分的数据元素（如数字 5、字符 'a'），也可以继续存储数组（即 n 维数组）。

但需要注意的是，以上两种数据存储形式绝不会出现在同一个数组中。例如，我们可以创建一个整形数组去存储 {1,2,3}，我们也可以创建一个二维整形数组去存储 {{1,2,3},{4,5,6}}，但数组不适合用来存储类似 {1,{1,2,3}} 这样的数据。

有人可能会说，创建一个二维数组来存储{1,{1,2,3}}。在存储上确实可以实现，但无疑会造成存储空间的浪费。

对于存储 {1,{1,2,3}} 这样的数据，更适合用广义表结构来存储。

#### 什么是广义表

广义表，又称列表，也是一种线性存储结构。

同数组类似，广义表中既可以存储不可再分的元素，也可以存储广义表，记作：

LS = (a1,a2,…,an)

其中，LS 代表广义表的名称，an 表示广义表存储的数据。广义表中每个 ai 既可以代表单个元素，也可以代表另一个广义表。

#### 原子和子表

通常，广义表中存储的单个元素称为 "原子"，而存储的广义表称为 "子表"。

例如创建一个广义表 LS = {1,{1,2,3}}，我们可以这样解释此广义表的构成：广义表 LS 存储了一个原子 1 和子表 {1,2,3}。

以下是广义表存储数据的一些常用形式：

- A = ()：A 表示一个广义表，只不过表是空的。
- B = (e)：广义表 B 中只有一个原子 e。
- C = (a,(b,c,d)) ：广义表 C 中有两个元素，原子 a 和子表 (b,c,d)。
- D = (A,B,C)：广义表 D 中存有 3 个子表，分别是A、B和C。这种表示方式等同于 D = ((),(e),(b,c,d)) 。
- E = (a,E)：广义表 E 中有两个元素，原子 a 和它本身。这是一个递归广义表，等同于：E = (a,(a,(a,…)))。


注意，A = () 和 A = (()) 是不一样的。前者是空表，而后者是包含一个子表的广义表，只不过这个子表是空表。

#### 广义表的表头和表尾

当广义表不是空表时，称第一个数据（原子或子表）为"表头"，剩下的数据构成的新广义表为"表尾"。

强调一下，除非广义表为空表，否则广义表一定具有表头和表尾，且广义表的表尾一定是一个广义表。

例如在广义表中 LS={1,{1,2,3},5} 中，表头为原子 1，表尾为子表 {1,2,3} 和原子 5 构成的广义表，即 {{1,2,3},5}。

再比如，在广义表 LS = {1} 中，表头为原子 1 ，但由于广义表中无表尾元素，因此该表的表尾是一个空表，用 {} 表示。



### 

