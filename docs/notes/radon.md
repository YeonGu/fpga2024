# 滤波反投影算法原理

## Radon变换
X光在穿过介质时会产生衰减. 衰减的强度和穿透距离的关系可以表示为
$$
I = I_0 \exp(-\mu s)
$$
其中$\mu$是透射系数, 和介质材料相关, $s$是穿透距离. 如果$\mu$是随介质位置的函数, 那么上式可以改写为
$$
- \log \frac{I}{I_0} = \int_{-\infty}^{+\infty} \mu(x, y)\mathrm{d}s
$$
也就是$\mu(x,y)$沿着传播路径的积分.

考虑最简单的平行束照射情况. X光沿着直线$x\cos\theta + y\sin \theta = t$传播, 线积分可以改写为
$$
- \log \frac{I}{I_0} = \int_{-\infty}^{+\infty}\int_{-\infty}^{+\infty} \mu(x, y)\delta(x\cos\theta + y\sin\theta - t)\mathrm{d}x \mathrm{d}y
$$

现在我们从CT仪上直接采集到的数据是入射X光强度和出射X光强度之间的关系, 也就是上式等号左侧. 而重建图像需要计算出原图像的$\mu(x,y)$, 进而得到物质在空间中的分布情况.

给定一个函数$f(x,y)$, 定义其Radon变换结果为
$$
R(\theta, t) = \int_{-\infty}^{+\infty}\int_{-\infty}^{+\infty} f(x,y)\delta(x\cos \theta + y\sin \theta - t) \mathrm{d}x \mathrm{d}y
$$
根据傅里叶切片定理, 设$S_\theta(\omega)$为$R(\theta, t)$关于$t$的一维傅里叶变换
$$
S_\theta(\omega) = \int_{-\infty}^{+\infty}R(\theta, t)e^{-2\pi i \omega t} \mathrm{d}t
$$
那么就有
$$
S_\theta(\omega) = F(\omega\cos\theta, \omega\sin\theta)
$$
其中$F(u,v)$是$f(x,y)$的二维傅里叶变换. 换句话讲, 一个二维函数沿某个方向的平行线积分, 其一维傅里叶变换等于该二维函数的二维傅里叶变换在与投影方向垂直的直线上的值.

对$F(x,y)$做二维傅里叶逆变换
$$
f(x,y) = \int_{-\infty}^{+\infty}\int_{-\infty}^{+\infty}F(u, v)e^{2\pi i(ux+vy)} \mathrm{d}u\mathrm{d}v
$$
换成极坐标, 并利用Jacobi行列式
$$
f(x,y) = \int_{0}^{\pi}\int_{-\infty}^{+\infty}F(\omega\cos\theta, \omega\sin\theta)|\omega|e^{2\pi i\omega(x\cos\theta + y\sin\theta)} \mathrm{d}\omega\mathrm{d}\theta
$$
注意到线积分是在$x\cos\theta+y\sin\theta - t= 0$直线上进行的, 代入$S_\theta(\omega)$可得
$$
\begin{gathered}
f(x,y) = \int_{0}^{\pi}Q_\theta(x\cos\theta, y\sin\theta)\mathrm{d}\theta \\
Q_\theta(t) = \int_{-\infty}^{\infty}S_\theta(t)|\omega|e^{2\pi i \omega t} \mathrm{d}\omega
\end{gathered}
$$
第二式就是对Radon变换结果做滤波后再做傅里叶逆变换.

## FBP重建算法
FBP重建就是根据上面的数学原理进行重建. 重建需要得到物质在空间中的分布情况, 而这可以通过穿透系数的分布情况表现出来. 设穿透系数的分布为$f(x,y)$. CT探测结果能直接得到X光透射强度和入射强度的关系, 根据X光穿透的物理规律, 最终透射强度和入射强度的比值的负对数恰好就是对$f(x,y)$做Radon变换. 相当于从CT探测结果能直接得到$R(\theta, t)$.

CT图的探测结果通常是个正弦图(Sinogram), 它的横轴是$\theta$, 也就是探测仪绕着目标旋转的角度; 纵轴是$t$, 也就是探测仪发出的不同平行束相对于中心轴的偏移量. 根据正弦图可以直接描述$R(\theta, t)$.

那么重建的步骤就很显然了
1. 根据CT探测结果描述透射X光强度分布, 然后根据图中显然没有物体的部分获取背景X光强度(物体越少的位置探测结果上越黑). 两者相比再取负对数即可得到$R(\theta, t)$.
2. 对$R(\theta, t)$做关于$t$方向上的一维傅里叶变换(离散形式)得到$S_\theta(\omega)$
3. 对一维傅里叶变换结果进行滤波, 如Ram-Lak滤波器, Hamming滤波器. 
4. 对滤波后的结果关于变量$t$做一维傅里叶逆变换, 得到$Q_\theta(t)$.
5. 对正弦图上的不同方向(即不同$\theta$)均做以上操作, 也就是得到$Q_\theta$在不同方向上的取值, 最后将得到的所有结果叠加起来得到重建的图像.

需要注意的是以上的公式积分范围是$[0,\pi]$, 也就是CT探测仪在探测的时候只转半圈, 输出的正弦图$\theta\in[0, \pi]$. 有些CT机会转一整圈, 需要注意对数据的处理.

## 滤波器
FBP里面的滤波是高通滤波器, 如果没有滤波器直接叠加的话得到的图像会非常模糊. 下图是叠加了120个不同方向的结果, 图像看起来非常模糊. 从上面的数学推导也可以看出, 在频域中进行高通滤波是非常重要的.       

![](./fig1.png)

常用的滤波器有
1. Ram-Lak滤波器: $H(f) = |f|$
2. Shepp-Logan滤波器: $H(f) = |f| \cdot \frac{\sin(\pi f / (2f_m))}{\pi f / (2f_m)}$, 其中$f_m$是最大频率.
3. Cosine滤波器: $H(f) = |f| \cdot \cos(\pi f / (2f_m))$
4. Hamming滤波器: $H(f) = |f| \cdot (0.54 + 0.46 \cos(\pi f / f_m))$
5. Hann滤波器: $H(f) = |f| \cdot (0.5 + 0.5 \cos(\pi f / f_m))$
6. 高斯滤波器: $H(f) = |f| \cdot e^{-(\pi f / (2f_m))^2}$

## 非平行束情况
现代CT机通常会发射扇形束或者锥形束, 但FBP算法是基于平行束设计的, 因此需要对CT机采集到的数据做预处理, 将其转换为等效的平行束, 再使用FBP算法.

### 扇形束

