<template>
  <section class="admin-grid">
    <article class="surface wide" style="background: linear-gradient(135deg, rgba(30,41,59,0.8), rgba(15,23,42,0.9)); border-color:var(--brand);">
      <div class="section-head compact">
        <h2>Dynamic pricing engine</h2>
      </div>
      <div style="padding: 20px; background: rgba(0,0,0,0.4); border-radius: 12px; margin-top: 16px; border: 1px solid rgba(255,255,255,0.05);">
        <p style="color:var(--text-muted); font-size:13px; margin-bottom:10px; text-transform:uppercase;">Reference pricing formula</p>
        <div style="font-size: 28px; font-family:'Orbitron', sans-serif; color: #fff; display:flex; align-items:center; gap:15px; flex-wrap:wrap;">
          <span style="color:var(--brand);">TOTAL</span> <span>=</span>
          <span style="background:rgba(255,255,255,0.1); padding:4px 12px; border-radius:8px;">Base</span>
          <span style="color:var(--text-muted);">x</span>
          <span style="background:rgba(239,68,68,0.2); color:var(--danger-red); padding:4px 12px; border-radius:8px;">Peak (1.5x)</span>
          <span style="color:var(--text-muted);">+</span>
          <span style="background:rgba(16,185,129,0.2); color:var(--safety-green); padding:4px 12px; border-radius:8px;">EV (+1.2/kWh)</span>
          <span style="color:var(--text-muted);">+</span>
          <span style="background:rgba(245,158,11,0.2); color:var(--warning-yellow); padding:4px 12px; border-radius:8px;">VIP (+5.0)</span>
        </div>
      </div>
    </article>

    <div style="display:grid; grid-template-columns: repeat(2, 1fr); gap:20px; grid-column: 1/-1;">
      <article class="surface">
        <div class="section-head">
          <div>
            <h2><i class="fa-solid fa-clock"></i> Base rate ladder</h2>
            <p>Hourly and cap logic shared with the backend pricing-rule dataset.</p>
          </div>
        </div>
        <div class="metric-list" style="grid-template-columns: 1fr;">
          <div><span>First hour</span><strong>CNY 8.00</strong></div>
          <div><span>Every additional 30 min</span><strong>CNY 2.50</strong></div>
          <div><span>24h cap</span><strong>CNY 80.00</strong></div>
        </div>
      </article>

      <article class="surface">
        <div class="section-head">
          <div>
            <h2><i class="fa-solid fa-bolt"></i> Service surcharges</h2>
            <p>Priority retrieval, EV charging, and peak shaping combine into a single pricing decision.</p>
          </div>
        </div>
        <div class="queue-list">
          <div class="queue-item" style="grid-template-columns: 1fr auto;">
            <div><b style="color:var(--warning-yellow);">VIP retrieval package</b><span>Priority dispatch with a fixed surcharge added to the order.</span></div>
            <strong style="color:#fff;">+ CNY 5.00</strong>
          </div>
          <div class="queue-item" style="grid-template-columns: 1fr auto;">
            <div><b style="color:var(--safety-green);">EV charging premium</b><span>Charged by delivered energy and merged into the same final invoice.</span></div>
            <strong style="color:#fff;">+ CNY 1.20 / kWh</strong>
          </div>
          <div class="queue-item" style="grid-template-columns: 1fr auto;">
            <div><b style="color:var(--danger-red);">Peak congestion multiplier</b><span>Applied during the evening surge to smooth incoming traffic.</span></div>
            <strong style="color:#fff;">x 1.5</strong>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
