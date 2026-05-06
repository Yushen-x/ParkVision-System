import os

base_dir = r"D:\Desktop\G3-2course\专业综合项目\ParkVision-System\frontend\src"

files = {}

# 1. base.css
files["assets/base.css"] = """@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Orbitron:wght@400;500;700&display=swap');
:root {
  --primary-blue: #3b82f6; --primary-blue-hover: #2563eb;
  --safety-green: #10b981; --warning-yellow: #f59e0b; --danger-red: #ef4444;
  --bg-dark: #0f172a; --bg-card: rgba(30, 41, 59, 0.7);
  --text-main: #f8fafc; --text-muted: #94a3b8;
  --border-color: rgba(255, 255, 255, 0.1);
  --radius-md: 12px; --radius-lg: 20px;
  --shadow-sm: 0 4px 6px -1px rgba(0, 0, 0, 0.1); --shadow-md: 0 10px 30px -10px rgba(96, 165, 250, 0.5);
  
  --panel: rgba(15, 23, 42, 0.6); --panel-2: rgba(30, 41, 59, 0.6);
  --line: rgba(56, 189, 248, 0.2);
  --brand: #38bdf8; --brand-2: #0284c7; --accent: #f59e0b;
}

* { box-sizing: border-box; }
html, body, #app { margin: 0; min-height: 100%; height: 100%; }
body {
  font-family: 'Inter', -apple-system, sans-serif;
  color: var(--text-main);
  background: linear-gradient(135deg, #020617 0%, #0f172a 100%);
}

button, input, textarea { font: inherit; outline: none; }
button { border: 0; cursor: pointer; transition: all 0.2s; }
a { color: inherit; text-decoration: none; }
h1, h2, h3, p { margin-top: 0; }
h1 { margin-bottom: 0; font-size: 30px; letter-spacing: 1px; }
h2 { margin-bottom: 0; font-size: 18px; letter-spacing: 0; color: #e2e8f0; }

.app-shell { min-height: 100vh; display: grid; grid-template-columns: 260px minmax(0, 1fr); background: transparent; }

/* Scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.3); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(148, 163, 184, 0.5); }

/* Sidebar */
.sidebar {
  position: sticky; top: 0; height: 100vh; padding: 24px 18px;
  background: rgba(15, 23, 42, 0.8); border-right: 1px solid var(--border-color);
  backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  display: flex; flex-direction: column; gap: 24px; z-index: 100;
}
.brand { display: flex; align-items: center; gap: 12px; min-height: 52px; margin-bottom: 10px; }
.brand-mark {
  width: 48px; height: 48px; display: grid; place-items: center; border-radius: 12px;
  background: linear-gradient(135deg, var(--brand), var(--brand-2)); color: #fff; font-weight: 800;
  box-shadow: 0 0 15px rgba(56, 189, 248, 0.4);
}
.brand strong { font-family: 'Orbitron', sans-serif; font-size: 18px; color: #fff; display: block; }
.brand span { color: var(--brand); font-size: 11px; margin-top: 4px; display: block; letter-spacing: 1px;}
.nav-list { display: grid; gap: 8px; }
.nav-item {
  width: 100%; min-height: 48px; padding: 0 14px; display: flex; align-items: center; gap: 12px;
  color: #94a3b8; background: transparent; border-radius: 10px; text-align: left; font-weight: 500;
  transition: all 0.3s;
}
.nav-item:hover { color: #fff; background: rgba(255, 255, 255, 0.05); transform: translateX(4px); }
.nav-item.router-link-active { color: #fff; background: linear-gradient(90deg, rgba(56,189,248,0.2) 0%, transparent 100%); border-left: 3px solid var(--brand); }
.nav-icon { width: 24px; display: grid; place-items: center; font-size: 16px; color: inherit; }

.sidebar-panel {
  margin-top: auto; padding: 16px; border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.03); border-radius: 12px; backdrop-filter: blur(4px);
}
.sidebar-panel strong { display: block; margin-top: 6px; font-size: 16px; color: #fff; }
.sidebar-panel p { color: var(--text-muted); font-size: 12px; line-height: 1.7; margin: 8px 0 0; }
.panel-label { color: var(--brand); font-size: 12px; font-weight: 600;}

/* Main Area */
.main { min-width: 0; padding: 24px 32px; height: 100vh; overflow-y: auto; }
.topbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.eyebrow { margin: 0 0 6px; color: var(--brand); font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; }
.topbar h1 { font-family: 'Orbitron', sans-serif; font-size: 28px; background: linear-gradient(to right, #e2e8f0, #94a3b8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.topbar-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; justify-content: flex-end; }

/* Buttons */
.primary-button, .ghost-button, .danger-button, .text-button {
  min-height: 40px; padding: 0 16px; border-radius: 8px; font-weight: 600; font-size: 14px;
}
.primary-button { color: #fff; background: linear-gradient(135deg, var(--brand-2), var(--brand)); box-shadow: 0 4px 15px rgba(56, 189, 248, 0.3); }
.primary-button:hover { box-shadow: 0 4px 20px rgba(56, 189, 248, 0.5); transform: translateY(-1px); }
.ghost-button { color: var(--text-main); background: rgba(255,255,255,0.05); border: 1px solid var(--line); }
.ghost-button:hover { background: rgba(255,255,255,0.1); }
.danger-button { color: #fff; background: var(--danger-red); box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3); }
.danger-button:hover { background: #dc2626; box-shadow: 0 4px 20px rgba(239, 68, 68, 0.5); }
.text-button { min-height: 34px; color: var(--brand); background: transparent; padding: 0 8px; }
.text-button:hover { color: #fff; }
.small { min-height: 32px; padding: 0 12px; font-size: 13px; }

/* Common Surface / Panel */
.surface { background: var(--panel); border: 1px solid var(--line); border-radius: 12px; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2); backdrop-filter: blur(10px); padding: 20px; }
.section-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 12px;}
.section-head p { color: var(--text-muted); margin: 6px 0 0; line-height: 1.6; font-size: 13px; }
.section-head.compact { align-items: center; border-bottom: none; padding-bottom: 0; margin-bottom: 16px;}

.status-pill {
  white-space: nowrap; min-height: 26px; padding: 4px 10px; border-radius: 999px; background: rgba(255,255,255,0.1);
  color: #fff; font-size: 12px; font-weight: 600; border: 1px solid rgba(255,255,255,0.1);
}
.status-pill.stable { color: var(--safety-green); background: rgba(16, 185, 129, 0.1); border-color: rgba(16, 185, 129, 0.3); }
.status-pill.warning { color: var(--danger-red); background: rgba(239, 68, 68, 0.1); border-color: rgba(239, 68, 68, 0.3); animation: pulseRed 2s infinite;}

@keyframes pulseRed { 0% { box-shadow: 0 0 0 0 rgba(239,68,68,0.4); } 70% { box-shadow: 0 0 0 6px rgba(239,68,68,0); } 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); } }

/* Grids & Layouts */
.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 20px;}
.kpi-card { padding: 20px; position: relative; overflow: hidden; background: linear-gradient(145deg, rgba(30, 41, 59, 0.8), rgba(15, 23, 42, 0.8)); }
.kpi-card::after { content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle, rgba(56,189,248,0.05) 0%, transparent 60%); z-index: 0; pointer-events: none;}
.kpi-card > * { position: relative; z-index: 1; }
.kpi-card span { display: block; color: var(--text-muted); font-size: 13px; font-weight: 500;}
.kpi-card strong { display: block; margin: 12px 0 8px; font-size: 32px; font-family: 'Orbitron', sans-serif; color: #fff; text-shadow: 0 0 10px rgba(255,255,255,0.2); }
.kpi-card small { display: block; color: var(--brand); font-size: 11px; }
.kpi-card.alert strong { color: var(--danger-red); text-shadow: 0 0 10px rgba(239, 68, 68, 0.5); }
.kpi-card.alert small { color: var(--danger-red); }

.dashboard-layout { display: grid; grid-template-columns: minmax(0, 2fr) minmax(320px, 0.9fr); gap: 20px; margin-top: 20px; }
.dashboard-layout.lower { grid-template-columns: minmax(420px, 1fr) minmax(380px, 0.9fr); }

/* Flow Stack */
.flow-stack { display: grid; gap: 14px; }
.flow-step { position: relative; padding: 16px 16px 16px 20px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); }
.flow-step::before { content: ""; position: absolute; left: 0; top: 12px; bottom: 12px; width: 4px; background: rgba(255,255,255,0.1); border-radius: 4px; }
.flow-step.done::before { background: var(--safety-green); box-shadow: 0 0 8px var(--safety-green); }
.flow-step.active::before { background: var(--brand); box-shadow: 0 0 8px var(--brand); }
.flow-step b { display: block; font-size: 15px; color: #fff; }
.flow-step span { display: block; color: var(--text-muted); margin-top: 6px; font-size: 13px; }

/* Grid (Slots) */
.mini-grid { display: grid; grid-template-columns: repeat(15, minmax(18px, 1fr)); gap: 8px; }
.slot-dot { aspect-ratio: 1; border-radius: 4px; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.1); transition: all 0.3s;}
.slot-dot.empty, .legend.empty { background: rgba(56, 189, 248, 0.1); border-color: rgba(56, 189, 248, 0.3); }
.slot-dot.occupied, .legend.occupied { background: rgba(56, 189, 248, 0.6); box-shadow: 0 0 8px rgba(56, 189, 248, 0.4); }
.slot-dot.charging, .legend.charging { background: rgba(16, 185, 129, 0.6); box-shadow: 0 0 8px rgba(16, 185, 129, 0.4); }
.slot-dot.buffer, .legend.buffer { background: rgba(245, 158, 11, 0.6); box-shadow: 0 0 8px rgba(245, 158, 11, 0.4); }
.slot-dot.maintenance, .legend.maintenance { background: rgba(239, 68, 68, 0.4); border-color: rgba(239, 68, 68, 0.6); box-shadow: inset 0 0 5px rgba(239, 68, 68, 0.5); }
.legend-row { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 16px; color: var(--text-muted); font-size: 12px; }
.legend-row span { display: inline-flex; align-items: center; gap: 8px; }
.legend { width: 14px; height: 14px; display: inline-block; border-radius: 4px; }

/* Lists & Tables */
.event-list { display: grid; gap: 12px; max-height: 280px; overflow-y: auto; padding-right: 8px; }
.event-item { padding: 14px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); }
.event-item b { display: block; font-size: 14px; color: #e2e8f0; margin-bottom: 4px;}
.event-item span { color: var(--brand); font-size: 12px; }

/* Admin Grid */
.admin-grid { display: grid; grid-template-columns: minmax(0, 1fr); gap: 20px; }
.admin-tabs { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.tab { min-height: 36px; padding: 0 16px; border-radius: 8px; color: var(--text-muted); background: rgba(255,255,255,0.05); font-weight: 500; border: 1px solid transparent; transition: all 0.3s; }
.tab:hover { background: rgba(255,255,255,0.1); color: #fff; }
.tab.active { color: #fff; background: rgba(56, 189, 248, 0.2); border-color: rgba(56, 189, 248, 0.5); box-shadow: 0 0 15px rgba(56,189,248,0.2); }
.table-wrap { overflow-x: auto; border: 1px solid var(--border-color); border-radius: 10px; background: rgba(15, 23, 42, 0.4); }
table { width: 100%; border-collapse: collapse; min-width: 760px; color: #f8fafc; }
th, td { padding: 14px 16px; border-bottom: 1px solid var(--border-color); text-align: left; font-size: 14px; }
th { color: var(--brand); background: rgba(255,255,255,0.02); font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; }
tr:hover td { background: rgba(255,255,255,0.03); }
tr:last-child td { border-bottom: 0; }

.metric-list { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); margin-bottom: 20px;}
.metric-list div { display: flex; justify-content: space-between; gap: 12px; padding: 16px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); align-items: center;}
.metric-list span { color: var(--text-muted); font-size: 13px; }
.metric-list strong { font-size: 20px; font-family: 'Orbitron', sans-serif; color: #fff; }

.query-box { padding: 20px; border-radius: 12px; background: rgba(56, 189, 248, 0.05); border: 1px solid rgba(56, 189, 248, 0.2); margin-bottom: 20px;}
.query-box p { margin-bottom: 16px; line-height: 1.6; color: #e2e8f0; font-size: 15px; }
.query-box .fake-input { display: flex; align-items: center; gap: 10px; background: rgba(15,23,42,0.8); padding: 10px 16px; border-radius: 8px; border: 1px solid var(--border-color); margin-bottom: 16px; }
.query-box .fake-input input { flex: 1; background: transparent; border: none; color: #fff; width: 100%; font-size: 14px; }

.report-output { padding: 20px; border-radius: 12px; color: #bae6fd; background: rgba(2, 132, 199, 0.1); border: 1px solid rgba(2, 132, 199, 0.3); line-height: 1.7; font-size: 14px; margin-top: 20px; border-left: 4px solid var(--brand-2); animation: fadeIn 0.5s;}

/* AiVision & Dispatch */
.ai-layout, .dispatch-layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr); gap: 20px; }
.ai-layout .wide, .dispatch-layout .wide { grid-column: 1 / -1; }

.camera-panel { min-height: 520px; }
.camera-view {
  position: relative; min-height: 450px; border-radius: 10px; overflow: hidden;
  background: linear-gradient(0deg, rgba(2, 6, 23, 0.9), rgba(2, 6, 23, 0.6)), repeating-linear-gradient(90deg, rgba(56,189,248,0.05) 0 74px, rgba(56,189,248,0.02) 74px 148px);
  border: 1px solid var(--border-color);
}
.lane-line { position: absolute; top: 0; bottom: 0; width: 4px; background: rgba(56, 189, 248, 0.3); box-shadow: 0 0 10px rgba(56, 189, 248, 0.5); }
.lane-line.left { left: 34%; }
.lane-line.right { right: 34%; }
.vehicle-shape {
  position: absolute; left: 50%; top: 54%; width: 210px; height: 125px; transform: translate(-50%, -50%);
  border-radius: 42px 42px 26px 26px; background: rgba(255,255,255,0.1); box-shadow: inset 0 -22px 0 rgba(0,0,0,0.5), 0 18px 38px rgba(0, 0, 0, 0.5); backdrop-filter: blur(5px); border: 1px solid rgba(255,255,255,0.2);
}
.vehicle-shape::before, .vehicle-shape::after { content: ""; position: absolute; bottom: -14px; width: 38px; height: 38px; border-radius: 50%; background: #020617; box-shadow: inset 0 2px 5px rgba(255,255,255,0.2); }
.vehicle-shape::before { left: 28px; }
.vehicle-shape::after { right: 28px; }
.plate { position: absolute; left: 50%; bottom: 20px; transform: translateX(-50%); padding: 6px 16px; border-radius: 6px; color: #fff; background: #2563eb; font-weight: 800; border: 1px solid #60a5fa; box-shadow: 0 0 15px rgba(37,99,235,0.5); letter-spacing: 1px;}
.detection-box { position: absolute; border: 2px solid var(--safety-green); color: var(--safety-green); font-weight: 800; font-size: 12px; padding: 4px; background: rgba(16,185,129,0.1); box-shadow: 0 0 10px rgba(16,185,129,0.2); backdrop-filter: blur(2px);}
.plate-box { left: calc(50% - 68px); top: calc(54% + 30px); width: 136px; height: 40px; border-color: #f59e0b; color: #f59e0b; background: rgba(245,158,11,0.1); box-shadow: 0 0 10px rgba(245,158,11,0.2);}
.vehicle-box { left: calc(50% - 122px); top: calc(54% - 78px); width: 244px; height: 154px; }
.intruder { position: absolute; right: 15%; top: 50%; display: none; width: 70px; height: 120px; place-items: center; border: 2px solid var(--danger-red); color: var(--danger-red); font-size: 12px; font-weight: 800; background: rgba(239,68,68,0.1); box-shadow: 0 0 15px rgba(239,68,68,0.4); animation: blink 1s infinite alternate;}
.intruder.show { display: grid; }
@keyframes blink { from { opacity: 0.5; } to { opacity: 1; } }

.json-output { min-height: 450px; margin: 0; padding: 20px; overflow: auto; color: #34d399; background: rgba(0,0,0,0.5); border-radius: 10px; line-height: 1.6; font-size: 13px; font-family: monospace; border: 1px solid var(--border-color); }
.module-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.module-row div { padding: 20px; border-radius: 12px; border: 1px solid var(--border-color); background: rgba(255,255,255,0.02); transition: transform 0.3s;}
.module-row div:hover { transform: translateY(-5px); background: rgba(255,255,255,0.05); }
.module-row b { display: block; color: #fff; font-size: 16px; margin-bottom: 8px;}
.module-row span { display: block; color: var(--text-muted); line-height: 1.6; font-size: 13px; }

/* Dispatch */
.queue-list, .task-timeline { display: grid; gap: 12px; }
.queue-item, .task-item { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 16px; padding: 16px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); }
.queue-rank { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 8px; background: rgba(255,255,255,0.1); font-weight: 800; color: #94a3b8; font-family: 'Orbitron', sans-serif;}
.queue-item.vip { border-color: rgba(245, 158, 11, 0.4); background: linear-gradient(90deg, rgba(245, 158, 11, 0.1) 0%, transparent 100%); }
.queue-item.vip .queue-rank { color: #fff; background: var(--warning-yellow); box-shadow: 0 0 10px rgba(245,158,11,0.5);}
.queue-item b, .task-item b { display: block; color: #e2e8f0; font-size: 15px;}
.queue-item span, .task-item span { display: block; color: var(--text-muted); font-size: 13px; margin-top: 4px; }
.queue-tag { padding: 6px 10px; border-radius: 999px; background: rgba(56, 189, 248, 0.1); color: var(--brand); font-size: 12px; font-weight: 700; border: 1px solid rgba(56, 189, 248, 0.2);}
.strategy-card { padding: 16px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); margin-bottom: 12px; }
.strategy-card strong { display: block; color: #fff; margin-bottom: 6px;}
.strategy-card span { display: block; color: var(--text-muted); line-height: 1.6; font-size: 13px;}

/* Twin 3D View */
.twin-layout { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 20px; }
.view-3d-container { perspective: 1200px; width: 100%; height: 600px; display: flex; justify-content: center; align-items: center; overflow: hidden; background: radial-gradient(circle at center, rgba(15,23,42,0.8) 0%, #020617 100%); border-radius: 12px; border: 1px solid var(--line); box-shadow: inset 0 0 50px rgba(0,0,0,0.5);}
.grid-3d { width: 500px; height: 500px; display: grid; grid-template-columns: repeat(12, 1fr); grid-template-rows: repeat(12, 1fr); gap: 4px; transform: rotateX(60deg) rotateZ(-45deg); transform-style: preserve-3d; transition: all 1s ease; }
.cell { background: rgba(56, 189, 248, 0.05); border: 1px solid rgba(56, 189, 248, 0.2); position: relative; transition: all 0.3s; }
.cell.occupied { background: rgba(56, 189, 248, 0.3); box-shadow: 0 0 10px rgba(56, 189, 248, 0.4); }
.cell.agv { background: var(--safety-green); box-shadow: 0 0 20px var(--safety-green), inset 0 0 10px white; transform: translateZ(15px); z-index: 10; border-radius: 2px;}
.cell.agv.loaded { background: var(--warning-yellow); box-shadow: 0 0 20px var(--warning-yellow), inset 0 0 10px white; }
.cell::after { content: ''; position: absolute; bottom: -10px; left: 0; width: 100%; height: 10px; background: rgba(56, 189, 248, 0.1); transform-origin: top; transform: rotateX(-90deg); }
.agv-card { padding: 16px; border-radius: 10px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); margin-bottom: 12px;}
.agv-card b { display: block; color: #fff; font-size: 14px;}
.agv-card span { display: block; color: var(--text-muted); font-size: 13px; margin-top: 6px; }
.safety-card { padding: 16px; border-radius: 10px; background: rgba(16, 185, 129, 0.1); border: 1px solid rgba(16, 185, 129, 0.3); }
.safety-card.danger { background: rgba(239, 68, 68, 0.1); border-color: rgba(239, 68, 68, 0.4); box-shadow: inset 0 0 20px rgba(239,68,68,0.2); animation: pulseRed 2s infinite;}
.safety-card strong { display: block; color: #fff; margin-bottom: 8px;}
.safety-card span { display: block; color: var(--text-muted); line-height: 1.6; font-size: 13px; }

/* OwnerPortal C-End */
.mobile-workbench { display: flex; justify-content: center; gap: 40px; align-items: stretch; padding: 20px 0;}
.phone-frame { width: 414px; height: 896px; padding: 12px; border-radius: 40px; background: #0f172a; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); border: 2px solid #334155; display: flex; flex-direction: column; position: relative;}
.phone-status { display: flex; justify-content: space-between; color: #fff; font-size: 13px; padding: 6px 16px 12px; font-weight: 600;}
.phone-screen { flex: 1; border-radius: 30px; background: #f1f5f9; overflow: hidden; position: relative; display: flex; flex-direction: column; }
.owner-detail { flex: 1; max-width: 600px; display: flex; flex-direction: column; justify-content: center;}

/* C-End Content specific */
.c-app-header { background: linear-gradient(135deg, #3b82f6, #60a5fa); color: white; padding: 2.5rem 1.5rem 3.5rem 1.5rem; border-bottom-left-radius: 24px; border-bottom-right-radius: 24px; }
.c-header-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.c-plate { font-weight: 600; font-size: 1.1rem; background: rgba(255,255,255,0.2); padding: 4px 12px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.4); }
.c-app-header h2 { font-size: 24px; font-weight: 700; color: #fff;}
.c-status-card { background: white; margin: -2.5rem 1.5rem 1rem 1.5rem; border-radius: 16px; padding: 1.5rem; box-shadow: 0 4px 20px rgba(0,0,0,0.05); display: flex; flex-direction: column; align-items: center; position: relative; }
.c-status-indicator { position: relative; width: 100px; height: 100px; display: flex; justify-content: center; align-items: center; margin-bottom: 1.5rem; }
.c-pulse-ring { position: absolute; width: 100%; height: 100%; border-radius: 50%; background: rgba(16, 185, 129, 0.2); animation: cPulse 2s infinite; }
@keyframes cPulse { 0% { transform: scale(0.9); opacity: 1; } 100% { transform: scale(1.4); opacity: 0; } }
.c-inner-circle { width: 75px; height: 75px; background: var(--safety-green); border-radius: 50%; display: flex; justify-content: center; align-items: center; color: white; font-weight: 700; font-size: 1rem; z-index: 2; box-shadow: 0 0 15px rgba(16, 185, 129, 0.4); }
.c-location { text-align: center; color: #64748b; font-size: 13px; margin-bottom: 1rem; }
.c-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; background: #f8fafc; padding: 1rem; border-radius: 12px; width: 100%;}
.c-info-item { display: flex; flex-direction: column; text-align: left; }
.c-info-item span { font-size: 11px; color: #64748b; margin-bottom: 4px; }
.c-info-item strong { font-size: 16px; color: #0f172a; }
.c-actions { padding: 0 1.5rem; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 1.5rem; }
.c-btn { border: none; font-weight: 600; border-radius: 12px; padding: 1rem; display: flex; flex-direction: column; align-items: center; gap: 8px; font-size: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.c-btn-primary { background: #3b82f6; color: white; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3); }
.c-btn-secondary { background: white; color: #334155; position: relative;}
.c-badge { position: absolute; top: 4px; right: 4px; background: var(--safety-green); color: white; font-size: 10px; padding: 2px 6px; border-radius: 4px; }
.c-vip-card { margin: 0 1.5rem; background: linear-gradient(135deg, #1e293b, #0f172a); color: white; border-radius: 12px; padding: 1rem; display: flex; align-items: center; position: relative; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1);}
.c-vip-card::after { content: ''; position: absolute; top: 0; right: 0; width: 60px; height: 100%; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent); transform: skewX(-20deg) translateX(-150px); animation: cShimmer 3s infinite; }
@keyframes cShimmer { 100% { transform: skewX(-20deg) translateX(400px); } }
.c-vip-icon { width: 36px; height: 36px; border-radius: 50%; background: rgba(245, 158, 11, 0.2); color: #f59e0b; display: flex; justify-content: center; align-items: center; font-size: 16px; margin-right: 12px; }
.c-vip-text h4 { margin: 0 0 4px 0; font-size: 14px; color: #fff;}
.c-vip-text p { margin: 0; font-size: 11px; color: #94a3b8; }
.c-vip-price { margin-left: auto; font-weight: 700; color: #f59e0b; font-size: 16px; }

/* Demo Guide */
.demo-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }
.demo-step { padding: 20px; border-radius: 12px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); position: relative; overflow: hidden;}
.demo-step::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 4px; background: linear-gradient(90deg, var(--brand), var(--brand-2)); }
.demo-step span { display: inline-flex; width: 32px; height: 32px; justify-content: center; align-items: center; border-radius: 8px; color: #fff; background: var(--brand); font-weight: 800; margin-bottom: 16px; font-family: 'Orbitron', sans-serif;}
.demo-step b { display: block; font-size: 18px; color: #fff; margin-bottom: 8px;}
.demo-step p { color: var(--text-muted); line-height: 1.6; margin: 0; font-size: 14px;}

@media (max-width: 1180px) {
  .app-shell { grid-template-columns: 1fr; }
  .sidebar { position: static; height: auto; border-right: none; border-bottom: 1px solid var(--border-color); }
  .nav-list { grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); }
  .mobile-workbench { flex-direction: column; align-items: center;}
}
"""

# 2. OwnerPortalView.vue
files["views/OwnerPortalView.vue"] = """<script setup>
import { computed, ref } from "vue";
import { getters, handleOwnerAction, state } from "../stores/parkingStore";

const duration = ref(156);
const fee = computed(() => (18 + Math.floor(duration.value / 30) * 2).toFixed(2));
const orderStatus = ref("存放中");
const freeCount = getters.freeCount;

const showOverlay = ref(false);
const timer = ref(180);
let timerId = null;

function doAction(action) {
  if (action === 'touch') {
    showOverlay.value = true;
    timer.value = 180;
    timerId = setInterval(() => {
      timer.value--;
      if(timer.value <= 0) {
        clearInterval(timerId);
      }
    }, 1000);
    orderStatus.value = "取物倒计时";
  } else {
    const statusMap = { reserve: "已预约", retrieve: "取车中", pay: "已支付" };
    orderStatus.value = statusMap[action];
  }
  handleOwnerAction(action);
}

function finishTouch() {
  showOverlay.value = false;
  if(timerId) clearInterval(timerId);
  orderStatus.value = "存放中";
}

const formatTime = (seconds) => {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0');
  const s = (seconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};
</script>

<template>
  <section class="mobile-workbench">
    <div class="phone-frame">
      <div class="phone-status">
        <span>ParkVision</span>
        <span><i class="fa-solid fa-wifi"></i> 5G <i class="fa-solid fa-battery-full"></i></span>
      </div>
      <div class="phone-screen">
        
        <div class="c-app-header">
            <div class="c-header-top">
                <span class="c-plate">浙A·12345</span>
                <i class="fa-regular fa-bell" style="font-size: 18px;"></i>
            </div>
            <h2>我的车辆</h2>
        </div>

        <div class="c-status-card">
            <div class="c-status-indicator">
                <div class="c-pulse-ring"></div>
                <div class="c-inner-circle">{{ orderStatus }}</div>
            </div>
            <div class="c-location"><i class="fa-solid fa-location-dot"></i> B2层 深库区 - A12位</div>
            <div class="c-info-grid">
                <div class="c-info-item"><span>停放时长</span><strong>02:36</strong></div>
                <div class="c-info-item"><span>当前电量</span><strong style="color: #10b981;">82% <i class="fa-solid fa-bolt"></i></strong></div>
            </div>
        </div>

        <div class="c-actions">
            <button class="c-btn c-btn-primary" @click="doAction('retrieve')">
                <i class="fa-solid fa-truck-ramp-box" style="font-size: 20px;"></i> 呼叫出库
            </button>
            <button class="c-btn c-btn-secondary" @click="doAction('touch')">
                <i class="fa-solid fa-box-open" style="font-size: 20px; color: #f59e0b;"></i> 临时取物
                <span class="c-badge">免扣费</span>
            </button>
        </div>

        <div class="c-vip-card" @click="doAction('retrieve')">
            <div class="c-vip-icon"><i class="fa-solid fa-bolt-lightning"></i></div>
            <div class="c-vip-text">
                <h4>VIP 加急取车</h4>
                <p>优先调度 AGV，预计省时 8 分钟</p>
            </div>
            <div class="c-vip-price">+¥5.00</div>
        </div>
        
        <!-- Timer Overlay -->
        <div v-if="showOverlay" style="position: absolute; top:0; left:0; width:100%; height:100%; background:rgba(15,23,42,0.85); backdrop-filter:blur(5px); z-index:50; display:flex; flex-direction:column; justify-content:center; align-items:center;">
            <div style="background: rgba(30, 41, 59, 0.95); width: 85%; border-radius: 20px; padding: 2.5rem 1.5rem; text-align: center; color: white; border: 1px solid rgba(255,255,255,0.1);">
                <h3 style="margin:0 0 10px; font-size:18px;">临时取物已到达</h3>
                <p style="color:#94a3b8; font-size:13px; margin:0;">请在倒计时结束前完成取物</p>
                <div style="font-size: 48px; font-weight:700; font-family:'Orbitron', sans-serif; color: #10b981; margin: 30px 0;">
                    {{ formatTime(timer) }}
                </div>
                <button class="primary-button full" @click="finishTouch">完成并入库</button>
            </div>
        </div>

      </div>
    </div>

    <div class="surface owner-detail" style="border:none; box-shadow:none; background:transparent;">
      <div class="section-head">
        <div>
          <h2 style="font-size:24px; margin-bottom:10px;">车主端核心高保真原型</h2>
          <p style="font-size:15px; max-width:500px;">展示融合了倒计时微动效的 Touch-and-Go 与加急取车功能，无缝契合 Vue 状态管理逻辑。</p>
        </div>
      </div>
      <div class="module-row" style="grid-template-columns: 1fr 1fr;">
        <div><b>实时车位与无感支付</b><span>打通底层数据，展示空闲数、充电位，自动生成订单，离场时扣费放行。</span></div>
        <div><b>Touch-and-Go 交互</b><span>临时取物倒计时，UI 状态实时切换，计费不中断，结束后自动回库。</span></div>
        <div><b style="color: #f59e0b;">VIP 柔性加急</b><span>高亮卡片诱导，点击即触发最高优先级 AGV 调度序列。</span></div>
        <div><b>AI 对话助手入口</b><span>预留自然语言转 API 的智能客服接口。</span></div>
      </div>
    </div>
  </section>
</template>
"""

# 3. AdminConsoleView.vue
files["views/AdminConsoleView.vue"] = """<script setup>
import { computed, ref, onMounted } from "vue";
import DataTable from "../components/DataTable.vue";
import { accessList, pricingRules } from "../data/mockData";
import { state } from "../stores/parkingStore";
import * as echarts from 'echarts';

const tab = ref("report");
const report = ref("");
const isGenerating = ref(false);

const tableConfig = computed(() => {
  const configs = {
    orders: { headers: ["订单号", "车牌", "事件", "位置", "状态", "费用"], rows: state.orders },
    pricing: { headers: ["规则名", "适用时段", "计费方式", "附加策略", "状态"], rows: pricingRules },
    alerts: { headers: ["告警号", "类型", "内容", "处理状态", "级别"], rows: state.alerts },
    access: { headers: ["车牌", "名单类型", "用户类型", "有效期", "备注"], rows: accessList },
  };
  return configs[tab.value] || configs['orders'];
});

function generateReport() {
  isGenerating.value = true;
  setTimeout(() => {
    report.value = "AI 摘要：根据数据分析，过去7天 VIP加急服务总收益达 ¥3,250，周对比增长 24.5%。主要高峰集中在周五和周日晚间。建议提前 25 分钟执行 8 辆车的深浅库位迁移。";
    isGenerating.value = false;
    renderChart();
  }, 1500);
}

function renderChart() {
  const dom = document.getElementById('echartsMain');
  if(!dom) return;
  const myChart = echarts.init(dom, 'dark', {background: 'transparent'});
  myChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['上周收益', '本周收益'], textStyle: { color: '#94a3b8' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } } },
    yAxis: { type: 'value', axisLabel: { formatter: '¥ {value}' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } } },
    series: [
        { name: '上周收益', type: 'line', data: [120, 132, 101, 134, 290, 430, 410], smooth: true, itemStyle: {color: '#94a3b8'} },
        { name: '本周收益', type: 'line', data: [220, 182, 191, 234, 490, 530, 610], smooth: true, areaStyle: { opacity: 0.2 }, itemStyle: {color: '#38bdf8'} }
    ]
  });
  window.addEventListener('resize', () => myChart.resize());
}
</script>

<template>
  <section class="admin-grid">
    <article class="surface wide" style="padding: 0; background: transparent; border: none; box-shadow: none; backdrop-filter: none;">
      <div class="admin-tabs">
        <button class="tab" :class="{ active: tab === 'report' }" @click="tab = 'report'"><i class="fa-solid fa-wand-magic-sparkles" style="margin-right:6px;"></i>AI 智能报表</button>
        <button class="tab" :class="{ active: tab === 'orders' }" @click="tab = 'orders'">出入场记录</button>
        <button class="tab" :class="{ active: tab === 'pricing' }" @click="tab = 'pricing'">计费规则</button>
        <button class="tab" :class="{ active: tab === 'alerts' }" @click="tab = 'alerts'">异常告警</button>
        <button class="tab" :class="{ active: tab === 'access' }" @click="tab = 'access'">名单管理</button>
      </div>
    </article>

    <!-- Report View -->
    <template v-if="tab === 'report'">
        <article class="surface wide">
            <div class="section-head compact">
                <h2>AI 数据中台 (Text-to-SQL)</h2>
                <div style="display:flex; align-items:center; gap:8px;"><i class="fa-solid fa-circle-user" style="color:var(--brand); font-size:24px;"></i> Admin</div>
            </div>
            <div class="query-box">
                <p><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i> 用自然语言查询业务数据并生成可视化报表</p>
                <div class="fake-input">
                    <input type="text" value="对比过去七天插队服务(VIP加急)带来的额外收益趋势" readonly>
                    <button class="primary-button" @click="generateReport" :disabled="isGenerating">
                        <i class="fa-solid" :class="isGenerating ? 'fa-spinner fa-spin' : 'fa-bolt'"></i> {{ isGenerating ? '生成中...' : '生成报表' }}
                    </button>
                </div>
            </div>
            
            <div v-if="report" class="report-output" style="animation: fadeIn 0.5s;">
                <strong><i class="fa-solid fa-chart-line"></i> 查询结果分析</strong>
                <p style="margin-top:10px; margin-bottom: 20px; color: #e2e8f0;">{{ report }}</p>
                <div id="echartsMain" style="width: 100%; height: 350px;"></div>
            </div>
        </article>
    </template>

    <!-- Table View -->
    <template v-else>
        <article class="surface wide">
            <div class="section-head">
                <div>
                <h2 style="text-transform: capitalize;">{{ tab }} 管理</h2>
                <p>实时同步底层业务流数据，支持高级筛选与导出。</p>
                </div>
                <button class="primary-button small">导出数据</button>
            </div>
            <div class="table-wrap">
                <DataTable :headers="tableConfig.headers" :rows="tableConfig.rows" />
            </div>
        </article>
    </template>
  </section>
</template>
"""

# 4. TwinView.vue
files["views/TwinView.vue"] = """<script setup>
import { state } from "../stores/parkingStore";
import { computed } from "vue";

const gridCells = computed(() => {
    // Generate a 12x12 grid representing slots, mostly empty but mapping to state.slots if possible
    const cells = [];
    for(let i=0; i<144; i++) {
        let isAgv = false;
        let isLoaded = false;
        let status = 'empty';
        
        // Map 1D to 2D roughly for visual, state.slots has 150 items
        if(state.slots[i]) {
            status = state.slots[i].status;
        }
        
        // Map AGVs
        state.agvs.forEach(agv => {
            // roughly map agv.x, agv.y (0-100) to 12x12
            const ax = Math.floor((agv.x / 100) * 12);
            const ay = Math.floor((agv.y / 100) * 12);
            const idx = ay * 12 + ax;
            if(idx === i) {
                isAgv = true;
                isLoaded = agv.load;
            }
        });
        
        cells.push({ id: i, status, isAgv, isLoaded });
    }
    return cells;
});
</script>

<template>
  <section class="twin-layout" :style="state.emergency ? 'box-shadow: inset 0 0 100px rgba(239,68,68,0.3); border-radius:12px; transition: all 0.5s;' : ''">
    <article class="surface wide" style="grid-column: 1 / 2; padding: 0; background: transparent; border: none; box-shadow: none;">
      <div class="view-3d-container" :style="state.emergency ? 'border-color: #ef4444;' : ''">
        <div class="grid-3d">
            <div v-for="cell in gridCells" :key="cell.id" 
                 class="cell" 
                 :class="[cell.status, { 'agv': cell.isAgv, 'loaded': cell.isLoaded }]">
            </div>
        </div>
        
        <div v-if="state.emergency" style="position:absolute; top:20px; left:20px; background:rgba(239,68,68,0.2); padding:10px 20px; border-radius:8px; border:1px solid #ef4444; color:#ef4444; font-weight:700; animation:blink 1s infinite alternate; font-family:'Orbitron', sans-serif; font-size: 20px;">
            <i class="fa-solid fa-triangle-exclamation"></i> EMERGENCY STOP
        </div>
      </div>
    </article>
    
    <aside style="display:flex; flex-direction:column; gap:20px;">
      <article class="surface" style="flex:1;">
        <div class="section-head compact">
          <h2>AGV 车队集群</h2>
        </div>
        <div class="agv-list" style="margin-top:16px;">
          <div v-for="agv in state.agvs" :key="agv.id" class="agv-card">
            <b><i class="fa-solid fa-robot" style="color:var(--brand); margin-right:8px;"></i> {{ agv.id }} · {{ agv.load ? "载车" : "空载" }}</b>
            <span>坐标 [{{ Math.round(agv.x) }}, {{ Math.round(agv.y) }}] · {{ agv.task }}</span>
          </div>
        </div>
      </article>

      <article class="surface" style="padding:0; background:transparent; border:none; box-shadow:none;">
        <div class="safety-card" :class="{ danger: state.emergency }">
          <strong><i class="fa-solid" :class="state.emergency ? 'fa-triangle-exclamation' : 'fa-shield-check'"></i> {{ state.emergency ? "安全急停已触发" : "边缘网关正常" }}</strong>
          <span>{{ state.emergency ? "检测到交接区异常闯入，YOLOv8 视觉告警。AGV 队列已物理冻结，等待人工复核解锁。" : "未发现人员闯入，系统心跳正常，闸机允许放行。" }}</span>
        </div>
      </article>
    </aside>
  </section>
</template>
"""

# 5. DashboardView.vue
files["views/DashboardView.vue"] = """<script setup>
import EventStream from "../components/EventStream.vue";
import MetricCard from "../components/MetricCard.vue";
import SlotGrid from "../components/SlotGrid.vue";
import TrafficChart from "../components/TrafficChart.vue";
import { state } from "../stores/parkingStore";
</script>

<template>
  <section>
    <div class="kpi-grid">
      <MetricCard label="库容占用率" :value="`${state.summary.occupancyRate}%`" hint="车位状态由视觉实时同步" />
      <MetricCard label="今日车辆流量" :value="state.summary.trafficTotal" hint="入场、出场、预约累计" />
      <MetricCard label="AGV 在线数" :value="state.summary.agvOnline" hint="沙盘坐标毫秒级刷新" />
      <MetricCard label="异常告警" :value="state.summary.alertCount" hint="安全、设备链路可追溯" tone="alert" />
    </div>

    <div class="dashboard-layout">
      <article class="surface">
        <div class="section-head compact">
          <div>
            <h2>时序流量与空闲预测</h2>
            <p>基于时序模型预测未来出入库压力</p>
          </div>
          <span class="status-pill stable">AI ONLINE</span>
        </div>
        <TrafficChart :history="state.forecast.history" :prediction="state.forecast.prediction" />
      </article>

      <article class="surface">
        <div class="section-head compact">
          <h2>系统闭环链路</h2>
        </div>
        <div class="flow-stack" style="margin-top:16px;">
          <div class="flow-step done"><b>边缘 AI 感知</b><span>端侧 YOLOv8 车牌/人员识别</span></div>
          <div class="flow-step done"><b>业务中台流转</b><span>柔性计费、订单、C端交互</span></div>
          <div class="flow-step active"><b>大模型智能调度</b><span>Pre-Dispatch 深度前置调度决策</span></div>
          <div class="flow-step"><b>数字孪生与控制</b><span>底层 PLC 下发与 3D 实时反馈</span></div>
        </div>
      </article>
    </div>

    <div class="dashboard-layout lower">
      <article class="surface">
        <div class="section-head compact">
          <h2>实时库区俯视图</h2>
          <RouterLink class="text-button" to="/twin">3D 沙盘 ></RouterLink>
        </div>
        <SlotGrid :slots="state.slots" />
      </article>
      <article class="surface">
        <div class="section-head compact">
          <h2>系统事件总线</h2>
          <span class="status-pill">STREAM</span>
        </div>
        <EventStream :events="state.events" />
      </article>
    </div>
  </section>
</template>
"""

# 6. AiVisionView.vue
files["views/AiVisionView.vue"] = """<script setup>
import { computed } from "vue";
import { getters, runVision, state } from "../stores/parkingStore";

const freeCount = getters.freeCount;
const occupiedCount = getters.occupiedCount;
const visionJson = computed(() =>
  JSON.stringify(
    {
      requestId: `edge-${Date.now().toString().slice(-6)}`,
      cameraId: "gate-A-01",
      plate: state.visionResult.plate,
      confidence: state.visionResult.confidence,
      slotOccupancy: { free: freeCount.value, occupied: occupiedCount.value },
      intrusion: state.visionResult.intrusion,
      action: state.visionResult.action,
    },
    null,
    2,
  ),
);
</script>

<template>
  <section class="ai-layout">
    <article class="surface camera-panel">
      <div class="section-head">
        <div>
          <h2>边缘视觉模型实时渲染</h2>
          <p>YOLOv8 + OCR 推理节点展示，结构化 JSON 下发至调度中枢。</p>
        </div>
        <button class="primary-button small" @click="runVision"><i class="fa-solid fa-play"></i> 捕获视频帧</button>
      </div>
      <div class="camera-view">
        <div class="lane-line left"></div>
        <div class="lane-line right"></div>
        <div class="vehicle-shape">
          <span class="plate">{{ state.visionResult.plate }}</span>
        </div>
        <div class="detection-box plate-box">PLATE CONF {{ state.visionResult.confidence }}</div>
        <div class="detection-box vehicle-box">VEHICLE 0.96</div>
        <div class="intruder" :class="{ show: state.visionResult.intrusion }">PERSON</div>
        <div style="position:absolute; top:20px; right:20px; color:#ef4444; font-family:'Orbitron', sans-serif; font-size:14px; animation:blink 1s infinite;"><i class="fa-solid fa-circle"></i> REC</div>
      </div>
    </article>

    <aside style="display:flex; flex-direction:column; gap:20px;">
        <article class="surface" style="flex:1;">
        <div class="section-head compact">
            <h2>结构化输出 payload</h2>
            <span class="status-pill" :class="state.visionResult.intrusion ? 'warning' : 'stable'">
            {{ state.visionResult.intrusion ? "INTERVENTION REQUIRED" : "VERIFIED" }}
            </span>
        </div>
        <pre class="json-output">{{ visionJson }}</pre>
        </article>
    </aside>

    <article class="surface wide" style="margin-top:-5px;">
      <div class="module-row">
        <div><b><i class="fa-solid fa-car-side" style="color:var(--brand); margin-right:8px;"></i>双流车牌识别</b><span>端侧 YOLO 定位 + HyperLPR 识别号码，输出极高置信度。</span></div>
        <div><b><i class="fa-solid fa-border-none" style="color:var(--safety-green); margin-right:8px;"></i>空余泊位检测</b><span>基于车库俯视 ROI 区域遮挡判定，更新孪生系统的车位图谱。</span></div>
        <div><b><i class="fa-solid fa-person-falling-burst" style="color:var(--danger-red); margin-right:8px;"></i>活体闯入告警</b><span>交接区严格执行防夹防闯入，毫秒级下发硬件底层急停指令。</span></div>
      </div>
    </article>
  </section>
</template>
"""

# 7. DispatchCenterView.vue
files["views/DispatchCenterView.vue"] = """<script setup>
import { state } from "../stores/parkingStore";
</script>

<template>
  <section class="dispatch-layout">
    <article class="surface">
      <div class="section-head">
        <div>
          <h2>大模型柔性调度引擎</h2>
          <p>将自然语言策略（如“让 VIP 优先”、“充电满 80% 挪车”）转化为 AGV 任务队列。</p>
        </div>
      </div>
      <div class="queue-list" style="margin-top:16px;">
        <div v-for="(task, index) in state.dispatchQueue" :key="index" class="queue-item" :class="{ vip: task.isVip }">
          <div class="queue-rank">{{ index + 1 }}</div>
          <div>
            <b>{{ task.action }} - {{ task.plate }}</b>
            <span>由 {{ task.source }} 调度策略触发</span>
          </div>
          <span class="queue-tag">{{ task.status }}</span>
        </div>
      </div>
    </article>

    <aside>
      <article class="surface" style="height: 100%;">
        <div class="section-head compact">
          <h2>生效中的策略规则</h2>
        </div>
        <div style="margin-top:16px;">
            <div class="strategy-card">
            <strong>VIP 优先级穿透</strong>
            <span>针对用户购买的加急取车订单，直接插入 AGV 调度队列首位。</span>
            </div>
            <div class="strategy-card">
            <strong>充电桩潮汐释放</strong>
            <span>检测到新能源车电量达到阈值且排队增多时，自动下发移库任务至浅缓存区。</span>
            </div>
            <div class="strategy-card">
            <strong>Pre-Dispatch 前置搬运</strong>
            <span>基于流量预测，在高峰期提前 20 分钟将热门深库位车辆移出。</span>
            </div>
        </div>
      </article>
    </aside>
  </section>
</template>
"""

# Write all files
for rel_path, content in files.items():
    full_path = os.path.join(base_dir, rel_path.replace("/", "\\"))
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Vue Frontend Rewrite Completed Successfully.")
