<template>
  <section class="mobile-workbench">
    <div class="phone-frame">
      <div class="phone-status">
        <span>ParkVision</span>
        <span><i class="fa-solid fa-wifi"></i> 5G <i class="fa-solid fa-battery-full"></i></span>
      </div>
      <div class="phone-screen" style="background: #0f172a;">
        
        <div style="padding: 2rem 1.5rem 1rem; color: #fff;">
            <h2 style="font-size:22px; font-weight:700;"><i class="fa-solid fa-location-arrow" style="color:var(--brand); margin-right:8px;"></i>室内接续导航</h2>
            <p style="color:#94a3b8; font-size:13px; margin-top:5px;">距 AGV 自动交接区还有 120 米</p>
        </div>

        <div style="flex:1; position:relative; margin: 0 1rem 1rem; border-radius: 20px; background: rgba(30,41,59,0.5); border: 1px solid rgba(255,255,255,0.1); overflow:hidden;">
            <!-- Radar sweep effect -->
            <div style="position:absolute; width:200%; height:200%; background:conic-gradient(from 0deg, transparent 70%, rgba(56,189,248,0.3) 100%); top:-50%; left:-50%; animation: spin 4s linear infinite; pointer-events:none;"></div>
            
            <!-- Path SVG -->
            <svg width="100%" height="100%" style="position:absolute; top:0; left:0;">
                <path d="M 50 350 Q 50 200 150 200 T 250 50" fill="transparent" stroke="rgba(56,189,248,0.2)" stroke-width="12" stroke-linecap="round"/>
                <path d="M 50 350 Q 50 200 150 200 T 250 50" fill="transparent" stroke="var(--brand)" stroke-width="4" stroke-dasharray="10, 10" class="dash-path"/>
            </svg>
            
            <!-- Nodes -->
            <div style="position:absolute; bottom:40px; left:35px; width:30px; height:30px; background:#fff; border-radius:50%; display:grid; place-items:center; color:#0f172a; box-shadow:0 0 15px #fff;"><i class="fa-solid fa-car"></i></div>
            <div style="position:absolute; top:35px; right:35px; width:40px; height:40px; background:var(--safety-green); border-radius:50%; display:grid; place-items:center; color:#fff; box-shadow:0 0 20px var(--safety-green);"><i class="fa-solid fa-flag-checkered"></i></div>
            <div style="position:absolute; top:15px; right:20px; color:var(--safety-green); font-size:11px; font-weight:700;">A区 交接点</div>
        </div>

        <div style="padding: 1.5rem; background:rgba(255,255,255,0.05); border-top-left-radius: 24px; border-top-right-radius: 24px; backdrop-filter:blur(10px);">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1rem;">
                <div style="color:#fff; font-size:18px; font-weight:700;">直行后右转</div>
                <div style="color:var(--brand); font-size:24px; font-weight:800; font-family:'Orbitron', sans-serif;">15 km/h</div>
            </div>
            <div style="display:flex; gap:10px;">
                <div style="flex:1; height:6px; background:var(--brand); border-radius:3px;"></div>
                <div style="flex:1; height:6px; background:var(--brand); border-radius:3px;"></div>
                <div style="flex:1; height:6px; background:rgba(255,255,255,0.1); border-radius:3px;"></div>
            </div>
        </div>

      </div>
    </div>

    <div class="surface owner-detail" style="border:none; box-shadow:none; background:transparent;">
      <div class="section-head">
        <div>
          <h2 style="font-size:24px; margin-bottom:10px;">室内外接续泊位导航</h2>
          <p style="font-size:15px; max-width:500px;">无缝融合室外 GPS 与室内蓝牙/UWB 定位，引导车主精准驶入 AGV 机械交接区。</p>
        </div>
      </div>
      <div class="module-row" style="grid-template-columns: 1fr;">
        <div style="background:rgba(56,189,248,0.1); border-color:var(--brand);">
          <b><i class="fa-solid fa-satellite-dish"></i> 无盲区定位切换</b>
          <span>进入车库瞬间完成 GPS 到信标的握手，结合数字孪生地图下发实时矢量路径，避免车主在错综复杂的地下空间迷失。</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
@keyframes spin { 100% { transform: rotate(360deg); } }
.dash-path { animation: dashAnim 2s linear infinite; }
@keyframes dashAnim { to { stroke-dashoffset: -20; } }
</style>
