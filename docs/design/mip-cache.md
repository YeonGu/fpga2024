# MIP Cache Design Notes

## 控制信号
- `pipeline_stall`. 流水线暂停. 只对`Cache Lookup`之前的流水线寄存器有效 (考虑到清空该流水)