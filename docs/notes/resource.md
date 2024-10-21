# Resource Utilization

KV260板卡资源分配与计划.

## 基础情况

- Ultraram x64. $288Kbit × 64 = 18.432Mbit = 2.29MB$
    - 9bit/port. 考虑`1byte->9bits`, 为 $2048KB$.

- Block RAM x144. $36Kbit × 144 = 0.632KB$.


- VRAM. 8位单色. 
  - `1080p -> 2025KB`
  - `720p (1280*720) -> 900KB`
  - `960*720 (4:3) -> 675KB`
  - `640*480 -> 300KB`

- 原始数据RAM. $8bit/voxel$. 
  - $64^3$: `256KB` -> **8 Ultraram Blocks**
  - $128^3$: `2048KB` (基本撑满全部uram容量)


# Resource Utilization

- Realtime MIP VRAM `960*720`. **24 Ultraram Blocks**
  - ULTRA RAM 960*720 (4:3) `675KB` `10 + 10 = 20bits address`
  - 考虑把9bit当成8bit 使用, 一个Ultraram可以放置`32KB`数据. 从而需要`21.09`个Ultraram. 
  - 取整. 需要24个Ultraram. 8通道 * 3blocks/channel. 屏幕高位交叉编址.  

  ```plain text
  AAAA AAAA AAAA A      | CCC
  address in uram block | uram channel sel
  ```

  - 考虑到之后的读取需求, VRAM采用 `8bit write + 64bit read` 模式.