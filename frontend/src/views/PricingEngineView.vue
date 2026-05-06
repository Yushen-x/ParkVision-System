<template>
  <section class="admin-grid">
    <article class="surface wide" style="background: linear-gradient(135deg, rgba(30,41,59,0.8), rgba(15,23,42,0.9)); border-color:var(--brand);">
      <div class="section-head compact">
        <h2>多维度动态策略定价引擎 (Pricing Engine)</h2>
      </div>
      <div style="padding: 20px; background: rgba(0,0,0,0.4); border-radius: 12px; margin-top: 16px; border: 1px solid rgba(255,255,255,0.05);">
        <p style="color:var(--text-muted); font-size:13px; margin-bottom:10px; text-transform:uppercase;">实时结算演算公式</p>
        <div style="font-size: 28px; font-family:'Orbitron', sans-serif; color: #fff; display:flex; align-items:center; gap:15px; flex-wrap:wrap;">
            <span style="color:var(--brand);">TOTAL</span> <span>=</span>
            <span style="background:rgba(255,255,255,0.1); padding:4px 12px; border-radius:8px;">Base</span>
            <span style="color:var(--text-muted);">×</span>
            <span style="background:rgba(239,68,68,0.2); color:var(--danger-red); padding:4px 12px; border-radius:8px;">Peak(1.5x)</span>
            <span style="color:var(--text-muted);">+</span>
            <span style="background:rgba(16,185,129,0.2); color:var(--safety-green); padding:4px 12px; border-radius:8px;">EV(+¥1.2/KWh)</span>
            <span style="color:var(--text-muted);">+</span>
            <span style="background:rgba(245,158,11,0.2); color:var(--warning-yellow); padding:4px 12px; border-radius:8px;">VIP(+¥5.0)</span>
        </div>
      </div>
    </article>

    <div style="display:grid; grid-template-columns: repeat(2, 1fr); gap:20px; grid-column: 1/-1;">
        <article class="surface">
            <div class="section-head">
                <div>
                    <h2><i class="fa-solid fa-clock"></i> 基础阶梯费率</h2>
                    <p>依据工作日/节假日及停放时长设定。</p>
                </div>
                <button class="text-button"><i class="fa-solid fa-pen"></i> 编辑</button>
            </div>
            <div class="metric-list" style="grid-template-columns: 1fr;">
                <div><span>首小时基础费</span><strong>¥ 8.00</strong></div>
                <div><span>超首小时每30分钟</span><strong>¥ 2.50</strong></div>
                <div><span>24小时封顶</span><strong>¥ 80.00</strong></div>
            </div>
        </article>
        
        <article class="surface">
            <div class="section-head">
                <div>
                    <h2><i class="fa-solid fa-bolt"></i> 附加服务与杠杆</h2>
                    <p>新能源充电、插队等复合附加池。</p>
                </div>
                <button class="primary-button small"><i class="fa-solid fa-plus"></i> 新增策略</button>
            </div>
            <div class="queue-list">
                <div class="queue-item" style="grid-template-columns: 1fr auto;">
                    <div><b style="color:var(--warning-yellow);">VIP 加急插队包</b><span>优先调用闲置 AGV，订单追加固定费用。</span></div>
                    <strong style="color:#fff;">+ ¥ 5.00 /次</strong>
                </div>
                <div class="queue-item" style="grid-template-columns: 1fr auto;">
                    <div><b style="color:var(--safety-green);">新能源超级快充</b><span>按实时输送电量计算，与停车费合并账单。</span></div>
                    <strong style="color:#fff;">+ ¥ 1.20 /度</strong>
                </div>
                <div class="queue-item" style="grid-template-columns: 1fr auto;">
                    <div><b style="color:var(--danger-red);">晚高峰拥堵杠杆</b><span>17:00-19:00 入库订单费率膨胀，控制流量。</span></div>
                    <strong style="color:#fff;">× 1.5 倍数</strong>
                </div>
            </div>
        </article>
    </div>
  </section>
</template>
