<script setup>
import DataTable from "../components/DataTable.vue";
const listData = [
  ["沪A·P7686", "月卡白名单", "业主", "2027-12-31", "直接放行，免检"],
  ["苏E·M9021", "内部测试车", "系统管理", "长期有效", "无视拥堵杠杆"],
  ["浙D·F4422", "黑名单", "逃逸记录", "永久拦截", "禁止入场闸机抬杆"],
];
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2><i class="fa-solid fa-shield-halved" style="color:var(--brand); margin-right:8px;"></i>物理鉴权与黑白名单网关</h2>
          <p>控制系统出入准许原则与违规拦截机制。</p>
        </div>
        <button class="primary-button small"><i class="fa-solid fa-plus"></i> 添加名单</button>
      </div>
      <div class="table-wrap" style="margin-top:16px;">
        <DataTable :headers="['车牌号', '名单类型', '用户身份', '有效期', '执行策略']" :rows="listData" />
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>分布式拓扑节点监控</h2>
        </div>
        <div style="margin-top:16px; display:flex; flex-direction:column; gap:12px;">
            <div class="strategy-card" style="border-left: 4px solid var(--safety-green);">
                <div style="display:flex; justify-content:space-between;">
                    <strong>Edge-Cam-01</strong>
                    <span class="status-pill stable" style="min-height:auto; padding:2px 8px;">98ms</span>
                </div>
                <span>南门入口视觉预处理节点，正常运行中。</span>
            </div>
            <div class="strategy-card" style="border-left: 4px solid var(--safety-green);">
                <div style="display:flex; justify-content:space-between;">
                    <strong>PLC-Master-Controller</strong>
                    <span class="status-pill stable" style="min-height:auto; padding:2px 8px;">12ms</span>
                </div>
                <span>底层 AGV 车队网关，心跳同步正常。</span>
            </div>
            <div class="strategy-card" style="border-left: 4px solid var(--danger-red); background: rgba(239,68,68,0.05);">
                <div style="display:flex; justify-content:space-between;">
                    <strong style="color:var(--danger-red);">Redis-Sync-Cluster</strong>
                    <span class="status-pill warning" style="min-height:auto; padding:2px 8px;">TIMEOUT</span>
                </div>
                <span style="color:var(--danger-red);">缓存同步节点超时，已启动熔断降级策略，主库顶替中！</span>
            </div>
        </div>
      </article>
    </aside>
  </section>
</template>
