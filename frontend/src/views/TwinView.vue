<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { simulateEntry, state, refreshCore, refreshAdminData } from "../stores/parkingStore";
import { parkvisionApi } from "../api/parkvisionApi";
import { zhText } from "../utils/localize";

/* ------------------------------------------------------------------ *
 * Vertical rotary tower garage (垂直升降塔库), no roads.                *
 * Each level has 3 FULL concentric rings (inner 6 / mid 8 / outer 10), *
 * each on its own turntable (回转盘). To fetch a car the target ring     *
 * turns it to the FRONT and the other rings turn a GAP to the front,   *
 * so the fork pulls the car straight in to the central lift through a  *
 * clear path — no car is ever crossed. Every bay has a numbered tag    *
 * and a charging post, and everything is clickable.                    *
 * ------------------------------------------------------------------ */
const LEVELS = [
  { key: "Shallow", label: "L1 浅层", y: 0 },
  { key: "Mid", label: "L2 中层", y: 6 },
  { key: "Deep", label: "L3 深层", y: 12 },
];
const LEVEL_H = 6;
// Upper bound of bays drawn per level. Real placement comes from each slot's
// `layer` field (see computePlacement), so a level renders exactly as many bays
// as the backend reports for that layer (24 each in the seeded dataset).
const BAYS_PER_LEVEL = 24;
// 3 full rings; counts sum to 24 to match each level's real slot count
const RINGS = [
  { count: 6, R: 9 }, // 内环
  { count: 8, R: 13.5 }, // 中环
  { count: 10, R: 18 }, // 外环
];
const CORRIDOR_HALF = 0.42; // front exit-corridor half-angle (~24°) kept clear of cars
const RING_LABEL = ["内环", "中环", "外环"];
const LAYER_INDEX = { Shallow: 0, Mid: 1, Deep: 2 };

// Map every backend slot to a (level, within-level) position using its real
// `layer` field, so the 3D tower matches the authoritative slot list one-to-one.
let slotPlacement = []; // [globalIdx] -> { level, within }
let placementToSlot = []; // [level][within] -> globalIdx
function computePlacement() {
  slotPlacement = [];
  placementToSlot = LEVELS.map(() => []);
  const counters = LEVELS.map(() => 0);
  state.slots.forEach((slot, idx) => {
    let lvl = LAYER_INDEX[slot.layer];
    if (lvl == null) lvl = Math.min(LEVELS.length - 1, Math.floor(idx / BAYS_PER_LEVEL));
    const within = counters[lvl]++;
    slotPlacement[idx] = { level: lvl, within };
    placementToSlot[lvl][within] = idx;
  });
}
function levelOf(idx) {
  return slotPlacement[idx] ? slotPlacement[idx].level : 0;
}
function withinOf(idx) {
  return slotPlacement[idx] ? slotPlacement[idx].within : 0;
}
const RIDE = 0.45;
const EXIT_Z = RINGS[2].R + 8;
const HANDOFF_Z = RINGS[2].R + 4;

const STATUS_COLOR = {
  empty: 0x94a3b8,
  occupied: 0x3b82f6,
  buffer: 0xf59e0b,
  charging: 0x10b981,
  reserved: 0x8b5cf6,
  maintenance: 0xef4444,
};
const CAR_COLORS = [0xe2474c, 0x3b82f6, 0x64748b, 0xeab308, 0x8b5cf6, 0x14b8a6];

const DEMO_SCENARIOS = [
  {
    id: "retrieve",
    label: "取车出库",
    icon: "fa-car-side",
    accent: "#c2410c",
    carColor: 0x3b82f6,
    summary: "目标环把车位转到正前方取车口，其余环转出空档让出通道；搬运叉把车直接取入中央梯笼，下行到地面后转向，传送带送出、抬杆放行。每条通道都被让空，全程不经过其它车。",
    slotIndex: 52,
    steps: [
      { key: "fetch", label: "回转对位取车", duration: 4.0, note: "目标环把车位转到正前方，其余环转出空档；搬运叉沿空出的径向把车取入中央梯笼。" },
      { key: "lower", label: "升降下行", duration: 3.0, note: "梯笼载车垂直下行至地面层。" },
      { key: "rotate", label: "回转朝向", duration: 2.4, note: "地面回转把车辆转向出场方向。" },
      { key: "release", label: "传送放行", duration: 3.0, note: "传送带把车送出通道，道闸抬杆放行，车位释放。" },
    ],
  },
  {
    id: "storage",
    label: "入库存车",
    icon: "fa-arrow-right-to-bracket",
    accent: "#4f46e5",
    carColor: 0xe2474c,
    summary: "车主在入口交车后，传送带把车送入中央梯笼，升降梯升至目标层，对应环把空车位转到取车口，搬运叉把车送入车位。",
    slotIndex: 61,
    steps: [
      { key: "intake", label: "入口交车", duration: 2.6, note: "车主在入口交车，系统创建存车任务。" },
      { key: "to-cage", label: "送入梯笼", duration: 3.0, note: "传送带把车送入中央梯笼。" },
      { key: "lift-up", label: "升降上行", duration: 3.0, note: "梯笼载车上行至目标层，对应环把空车位转到取车口。" },
      { key: "rack-in", label: "送入车位", duration: 3.2, note: "搬运叉把车辆推入车位，完成立体存车。" },
    ],
  },
  {
    id: "touch",
    label: "临停取物",
    icon: "fa-box-open",
    accent: "#0f766e",
    carColor: 0xeab308,
    summary: "把车取到地面交接区交给车主取物，取完后自动原路送回原车位，计费会话保持开启。",
    slotIndex: 33,
    steps: [
      { key: "fetch", label: "回转对位取车", duration: 3.8, note: "回转对位后搬运叉把车取入梯笼。" },
      { key: "lower", label: "升降下行", duration: 2.8, note: "梯笼载车下行至地面交接层。" },
      { key: "handoff", label: "交接取物", duration: 3.0, note: "车辆转向交接区，车主到位取物。" },
      { key: "return-up", label: "回送上行", duration: 3.0, note: "取物完成，车辆回到梯笼并上行返回原层。" },
      { key: "rack-back", label: "复位入库", duration: 2.8, note: "搬运叉把车送回原车位，计费会话保持开启。" },
    ],
  },
];

/* ------------------------------------------------------------------ *
 * Reactive UI                                                         *
 * ------------------------------------------------------------------ */
const canvasHost = ref(null);
const azimuthDeg = ref(45);
const autoRotate = ref(false);
const initError = ref("");
const liftLevel = ref(1);
const demoScenarioId = ref("retrieve");
const demoStepIndex = ref(0);
const demoStepElapsed = ref(0);
const demoPlaying = ref(false);
const gateManual = ref(false);
const activeSlotOverride = ref(null);
const selected = ref(null);
const selectedScreen = ref({ x: 0, y: 0, visible: false });
// Charging is read straight from the authoritative slot list so the twin, the
// dashboard overview and the backend always agree.
function isCharging(idx) {
  return state.slots[idx]?.status === "charging";
}

const freeSlots = computed(() => state.slots.filter((s) => s.status === "empty").length);
const occupiedSlots = computed(() => state.slots.filter((s) => s.status !== "empty").length);
const chargingSlots = computed(() => state.slots.reduce((n, _s, i) => n + (isCharging(i) ? 1 : 0), 0));
const safetyGate = computed(() => state.devices.gates.find((g) => g.gateId.includes("OUT")) || null);
const demoScenario = computed(() => DEMO_SCENARIOS.find((s) => s.id === demoScenarioId.value) || DEMO_SCENARIOS[0]);
const demoStep = computed(() => demoScenario.value.steps[demoStepIndex.value] || demoScenario.value.steps[0]);
const demoProgress = computed(() => {
  const steps = demoScenario.value.steps.length || 1;
  return ((demoStepIndex.value + Math.min(demoStepElapsed.value / (demoStep.value?.duration || 1), 1)) / steps) * 100;
});
const focusSlotId = computed(() => {
  const idx = activeSlotOverride.value ?? demoScenario.value.slotIndex;
  return state.slots[idx]?.id || "--";
});

/* ------------------------------------------------------------------ *
 * Three.js handles                                                    *
 * ------------------------------------------------------------------ */
let renderer, scene, camera, controls, clock, frameId, resizeObserver;
let accentLightA, ambient;
let liftCage, turntable, payloadCar, humanMarker, gateBar, forkPivot, forkArm;
const cameras3d = [];
const conveyorRollers = [];
const ringCar = []; // ringCar[level][ringIdx] -> turntable group
const ringRot = []; // ringRot[level][ringIdx] -> { rot, tRot }
const slotChargers = new Map(); // index -> { screenMat, label }
const pulseLeds = [];
const slotContent = new Map();
const slotPads = new Map();
let lastStatusKey = "";
let emergencyMix = 0;
const raycaster = new THREE.Raycaster();
const pointer = new THREE.Vector2();

const DEFAULT_CAM = new THREE.Vector3(48, 40, 58);
const DEFAULT_TARGET = new THREE.Vector3(0, 6, 0);
let tween = null;

/* ------------------------------------------------------------------ *
 * Helpers                                                             *
 * ------------------------------------------------------------------ */
function clamp(v, a, b) {
  return Math.min(Math.max(v, a), b);
}
function V3(x, y, z) {
  return new THREE.Vector3(x, y, z);
}
function easeInOut(p) {
  const c = clamp(p, 0, 1);
  return c < 0.5 ? 2 * c * c : 1 - Math.pow(-2 * c + 2, 2) / 2;
}
function lerpAngle(a, b, t) {
  let d = b - a;
  while (d > Math.PI) d -= Math.PI * 2;
  while (d < -Math.PI) d += Math.PI * 2;
  return a + d * t;
}
function nearestEquiv(current, target) {
  let t = target;
  while (t - current > Math.PI) t -= Math.PI * 2;
  while (t - current < -Math.PI) t += Math.PI * 2;
  return t;
}
function nearestGapRot(current, step) {
  const k = Math.round((current - step / 2) / step);
  return k * step + step / 2;
}
// within-level index (0..23) -> { ring, R, count, step, angle }
function bayLayout(within) {
  let acc = 0;
  for (let ri = 0; ri < RINGS.length; ri++) {
    if (within < acc + RINGS[ri].count) {
      const j = within - acc;
      const step = (Math.PI * 2) / RINGS[ri].count;
      return { ring: ri, R: RINGS[ri].R, count: RINGS[ri].count, step, angle: j * step };
    }
    acc += RINGS[ri].count;
  }
  const step = (Math.PI * 2) / RINGS[2].count;
  return { ring: 2, R: RINGS[2].R, count: RINGS[2].count, step, angle: 0 };
}

function makeLabel(text, { color = "#1e293b", weight = 700, px = 46, bg = "rgba(255,255,255,0.86)" } = {}) {
  const c = document.createElement("canvas");
  c.width = 256;
  c.height = 128;
  const ctx = c.getContext("2d");
  ctx.font = `${weight} ${px}px Orbitron, "Noto Sans SC", sans-serif`;
  const w = Math.min(ctx.measureText(text).width + 40, 244);
  const h = 64;
  const x = (256 - w) / 2;
  const y = (128 - h) / 2;
  const r = 24;
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
  ctx.fillStyle = bg;
  ctx.fill();
  ctx.lineWidth = 2;
  ctx.strokeStyle = "rgba(99, 102, 241, 0.3)";
  ctx.stroke();
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillStyle = color;
  ctx.fillText(text, 128, 64);
  const tex = new THREE.CanvasTexture(c);
  tex.anisotropy = 4;
  const mat = new THREE.SpriteMaterial({ map: tex, transparent: true, depthWrite: false });
  const sprite = new THREE.Sprite(mat);
  sprite.scale.set(4, 2, 1);
  return sprite;
}

/* ------------------------------------------------------------------ *
 * Model factories                                                     *
 * ------------------------------------------------------------------ */
function makeCar(colorHex) {
  const g = new THREE.Group();
  const skirt = new THREE.Mesh(
    new THREE.BoxGeometry(2.5, 0.45, 4.4),
    new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.4, roughness: 0.6 }),
  );
  skirt.position.y = 0.42;
  skirt.castShadow = true;
  g.add(skirt);
  const body = new THREE.Mesh(
    new THREE.BoxGeometry(2.4, 0.7, 4.2),
    new THREE.MeshStandardMaterial({ color: colorHex, metalness: 0.45, roughness: 0.34 }),
  );
  body.position.y = 0.85;
  body.castShadow = true;
  g.add(body);
  const cabin = new THREE.Mesh(
    new THREE.BoxGeometry(2.0, 0.68, 2.1),
    new THREE.MeshStandardMaterial({ color: 0x9fc3e6, metalness: 0.1, roughness: 0.08, transparent: true, opacity: 0.62 }),
  );
  cabin.position.set(0, 1.42, -0.15);
  g.add(cabin);
  const roof = new THREE.Mesh(
    new THREE.BoxGeometry(1.9, 0.12, 1.7),
    new THREE.MeshStandardMaterial({ color: colorHex, metalness: 0.45, roughness: 0.36 }),
  );
  roof.position.set(0, 1.78, -0.2);
  g.add(roof);
  const wheelGeo = new THREE.CylinderGeometry(0.42, 0.42, 0.32, 18);
  const wheelMat = new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.9 });
  const hubMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.9, roughness: 0.3 });
  for (const [wx, wz] of [[-1.15, 1.35], [1.15, 1.35], [-1.15, -1.35], [1.15, -1.35]]) {
    const w = new THREE.Mesh(wheelGeo, wheelMat);
    w.rotation.z = Math.PI / 2;
    w.position.set(wx, 0.42, wz);
    const hub = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.16, 0.34, 12), hubMat);
    hub.rotation.z = Math.PI / 2;
    hub.position.set(wx, 0.42, wz);
    g.add(w, hub);
  }
  const headMat = new THREE.MeshStandardMaterial({ color: 0xfffdf2, emissive: 0xfff4d0, emissiveIntensity: 0.6 });
  const tailMat = new THREE.MeshStandardMaterial({ color: 0xff4455, emissive: 0xff2233, emissiveIntensity: 0.55 });
  const lampGeo = new THREE.BoxGeometry(0.45, 0.22, 0.08);
  for (const lx of [-0.7, 0.7]) {
    const h = new THREE.Mesh(lampGeo, headMat);
    h.position.set(lx, 0.85, 2.12);
    const tl = new THREE.Mesh(lampGeo, tailMat);
    tl.position.set(lx, 0.85, -2.12);
    g.add(h, tl);
  }
  g.scale.setScalar(0.78);
  return g;
}

function makeCharger() {
  const g = new THREE.Group();
  const post = new THREE.Group();
  post.position.set(1.7, 0, 1.0); // tangential side, clear of the radial fetch path
  const base = new THREE.Mesh(
    new THREE.BoxGeometry(0.46, 0.18, 0.46),
    new THREE.MeshStandardMaterial({ color: 0x475569, roughness: 0.8 }),
  );
  base.position.y = 0.09;
  post.add(base);
  const pillar = new THREE.Mesh(
    new THREE.BoxGeometry(0.3, 1.4, 0.26),
    new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.4, roughness: 0.5 }),
  );
  pillar.position.y = 0.85;
  post.add(pillar);
  const screenMat = new THREE.MeshStandardMaterial({ color: 0x64748b, emissive: 0x10b981, emissiveIntensity: 0.0 });
  const screen = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.4, 0.06), screenMat);
  screen.position.set(0, 1.12, 0.16);
  post.add(screen);
  const arm = new THREE.Mesh(
    new THREE.BoxGeometry(0.06, 0.06, 0.7),
    new THREE.MeshStandardMaterial({ color: 0x334155, roughness: 0.6 }),
  );
  arm.position.set(0, 0.95, -0.42);
  post.add(arm);
  const label = makeLabel("充电中", { color: "#059669", px: 34 });
  label.position.set(0, 2.3, 0);
  label.scale.set(2.3, 1.15, 1);
  label.visible = false;
  post.add(label);
  g.add(post);
  return { group: g, screenMat, label };
}

function makeCone() {
  const g = new THREE.Group();
  const cone = new THREE.Mesh(
    new THREE.ConeGeometry(0.5, 1.4, 20),
    new THREE.MeshStandardMaterial({ color: 0xff7a1a, emissive: 0xff5500, emissiveIntensity: 0.35, roughness: 0.6 }),
  );
  cone.position.y = 0.8;
  cone.castShadow = true;
  g.add(cone);
  return g;
}

function makeHumanMarker() {
  const group = new THREE.Group();
  const body = new THREE.Mesh(
    new THREE.CapsuleGeometry(0.34, 1.3, 6, 12),
    new THREE.MeshStandardMaterial({ color: 0x0f172a, roughness: 0.5 }),
  );
  body.position.y = 1.1;
  body.castShadow = true;
  group.add(body);
  const vest = new THREE.Mesh(
    new THREE.BoxGeometry(0.85, 0.68, 0.5),
    new THREE.MeshStandardMaterial({ color: 0xf59e0b, emissive: 0xf59e0b, emissiveIntensity: 0.22 }),
  );
  vest.position.y = 1.1;
  group.add(vest);
  const head = new THREE.Mesh(
    new THREE.SphereGeometry(0.27, 18, 18),
    new THREE.MeshStandardMaterial({ color: 0xf8c9a4, roughness: 0.8 }),
  );
  head.position.y = 2.0;
  group.add(head);
  const halo = new THREE.Mesh(
    new THREE.RingGeometry(0.5, 0.72, 28),
    new THREE.MeshBasicMaterial({ color: 0xf59e0b, transparent: true, opacity: 0.5, side: THREE.DoubleSide }),
  );
  halo.rotation.x = -Math.PI / 2;
  halo.position.y = 0.05;
  group.add(halo);
  const label = makeLabel("车主", { color: "#9a3412", px: 34 });
  label.position.set(0, 3.0, 0);
  label.scale.set(2.2, 1.1, 1);
  group.add(label);
  group.visible = false;
  group.userData.halo = halo;
  return group;
}

function makeCamera3d(camData, angle) {
  const g = new THREE.Group();
  const pole = new THREE.Mesh(
    new THREE.CylinderGeometry(0.1, 0.1, 4.2, 10),
    new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.5, roughness: 0.5 }),
  );
  pole.position.y = 2.1;
  g.add(pole);
  const head = new THREE.Mesh(
    new THREE.BoxGeometry(0.7, 0.5, 1.0),
    new THREE.MeshStandardMaterial({ color: 0x334155, metalness: 0.5, roughness: 0.4 }),
  );
  head.position.set(0, 4.1, 0.2);
  g.add(head);
  const lens = new THREE.Mesh(
    new THREE.CylinderGeometry(0.18, 0.2, 0.3, 16),
    new THREE.MeshStandardMaterial({ color: 0x0ea5e9, emissive: 0x0ea5e9, emissiveIntensity: 0.8 }),
  );
  lens.rotation.x = Math.PI / 2;
  lens.position.set(0, 4.1, 0.85);
  g.add(lens);
  const tag = makeLabel(camData.cameraId || "CAM", { color: "#0369a1", px: 30 });
  tag.position.set(0, 5.0, 0);
  tag.scale.set(3.0, 1.1, 1);
  g.add(tag);
  g.rotation.y = angle + Math.PI;
  g.userData.pick = { kind: "camera", data: camData };
  return g;
}

function makeBoomGate(gateData) {
  const g = new THREE.Group();
  const post = new THREE.Mesh(
    new THREE.BoxGeometry(0.4, 2.6, 0.4),
    new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.45, roughness: 0.5 }),
  );
  post.position.set(-2.4, 1.3, 0);
  g.add(post);
  gateBar = new THREE.Group();
  gateBar.position.set(-2.4, 2.4, 0);
  const bar = new THREE.Mesh(
    new THREE.BoxGeometry(4.6, 0.18, 0.18),
    new THREE.MeshStandardMaterial({ color: 0xf59e0b, emissive: 0xf59e0b, emissiveIntensity: 0.25, roughness: 0.5 }),
  );
  bar.position.set(2.3, 0, 0);
  for (let i = 0; i < 4; i++) {
    const stripe = new THREE.Mesh(
      new THREE.BoxGeometry(0.5, 0.2, 0.2),
      new THREE.MeshStandardMaterial({ color: i % 2 ? 0xffffff : 0xdc2626 }),
    );
    stripe.position.set(0.6 + i * 1.1, 0, 0);
    gateBar.add(stripe);
  }
  gateBar.add(bar);
  g.add(gateBar);
  const label = makeLabel("出场闸机", { color: "#9a3412", px: 32 });
  label.position.set(0, 3.4, 0);
  label.scale.set(3.0, 1.2, 1);
  g.add(label);
  g.position.set(0, 0, EXIT_Z);
  g.userData.pick = { kind: "gate", data: gateData };
  return g;
}

function makePallet() {
  const g = new THREE.Group();
  const tray = new THREE.Mesh(
    new THREE.BoxGeometry(2.9, 0.18, 4.3),
    new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.55, roughness: 0.4 }),
  );
  tray.position.y = -0.09;
  tray.castShadow = true;
  g.add(tray);
  const rollerMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.7, roughness: 0.3 });
  for (let k = -1; k <= 1; k++) {
    const r = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.1, 2.7, 10), rollerMat);
    r.rotation.z = Math.PI / 2;
    r.position.set(0, 0.02, k * 1.2);
    g.add(r);
  }
  const ledMat = new THREE.MeshStandardMaterial({ color: 0x38bdf8, emissive: 0x38bdf8, emissiveIntensity: 1.0 });
  pulseLeds.push(ledMat);
  for (const sx of [-1.42, 1.42]) {
    const s = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.16, 4.1), ledMat);
    s.position.set(sx, 0.02, 0);
    g.add(s);
  }
  return g;
}

function makeLiftAndTurntable() {
  const g = new THREE.Group();
  const topY = LEVELS[LEVELS.length - 1].y;
  const railMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.6, roughness: 0.4 });
  const shaftH = topY + 5;
  for (const [rx, rz] of [[-3.0, -3.0], [3.0, -3.0], [-3.0, 3.0], [3.0, 3.0]]) {
    const rail = new THREE.Mesh(new THREE.CylinderGeometry(0.14, 0.14, shaftH, 12), railMat);
    rail.position.set(rx, shaftH / 2 - 0.6, rz);
    g.add(rail);
  }
  LEVELS.forEach((lvl) => {
    const mk = new THREE.Mesh(
      new THREE.TorusGeometry(3.4, 0.06, 8, 32),
      new THREE.MeshBasicMaterial({ color: 0x818cf8, transparent: true, opacity: 0.5 }),
    );
    mk.rotation.x = Math.PI / 2;
    mk.position.y = lvl.y - 0.1;
    g.add(mk);
  });

  liftCage = new THREE.Group();
  const deck = new THREE.Mesh(
    new THREE.CylinderGeometry(3.0, 3.0, 0.3, 40),
    new THREE.MeshStandardMaterial({ color: 0x6366f1, metalness: 0.5, roughness: 0.4, emissive: 0x6366f1, emissiveIntensity: 0.22 }),
  );
  deck.position.y = 0.15;
  deck.castShadow = true;
  liftCage.add(deck);
  turntable = new THREE.Group();
  const disc = new THREE.Mesh(
    new THREE.CylinderGeometry(2.7, 2.7, 0.12, 40),
    new THREE.MeshStandardMaterial({ color: 0xc7d2fe, metalness: 0.4, roughness: 0.5, emissive: 0x818cf8, emissiveIntensity: 0.12 }),
  );
  disc.position.y = 0.34;
  turntable.add(disc);
  const arrow = new THREE.Mesh(
    new THREE.ConeGeometry(0.3, 0.8, 4),
    new THREE.MeshStandardMaterial({ color: 0xfbbf24, emissive: 0xf59e0b, emissiveIntensity: 0.6 }),
  );
  arrow.rotation.x = Math.PI / 2;
  arrow.position.set(0, 0.5, 2.2);
  turntable.add(arrow);
  liftCage.add(turntable);

  forkPivot = new THREE.Group();
  forkPivot.position.y = 0.4;
  forkArm = new THREE.Mesh(
    new THREE.BoxGeometry(0.5, 0.22, RINGS[2].R),
    new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.6, roughness: 0.4 }),
  );
  forkArm.position.set(0, 0, RINGS[2].R / 2);
  forkPivot.add(forkArm);
  forkPivot.visible = false;
  liftCage.add(forkPivot);

  liftCage.position.y = 0;
  liftCage.userData.pick = { kind: "lift", data: null };
  g.add(liftCage);

  const label = makeLabel("升降梯 / 回转盘", { color: "#4f46e5", px: 32 });
  label.position.set(0, topY + 4.6, 0);
  label.scale.set(6, 2.0, 1);
  g.add(label);
  return g;
}

/* ------------------------------------------------------------------ *
 * Slot content (car colour stable by index; charging shown by post)   *
 * ------------------------------------------------------------------ */
function slotType(status) {
  if (status === "empty" || status === "reserved") return "empty";
  return status === "maintenance" ? "cone" : "car";
}
function buildSlotContent(type, index, status) {
  const g = new THREE.Group();
  if (type === "car") g.add(makeCar(CAR_COLORS[index % CAR_COLORS.length]));
  else if (type === "cone") g.add(makeCone());
  if (status === "reserved") {
    const lbl = makeLabel("已预约", { color: "#7c3aed", px: 36, bg: "rgba(237,233,254,0.92)" });
    lbl.position.set(0, 1.8, 0);
    lbl.scale.set(2.6, 1.3, 1);
    g.add(lbl);
  }
  return g;
}
function setChargerState(idx) {
  const ch = slotChargers.get(idx);
  if (!ch) return;
  const charging = isCharging(idx);
  ch.screenMat.emissiveIntensity = charging ? 1.5 : 0.0;
  ch.screenMat.color.setHex(charging ? 0x10b981 : 0x64748b);
  ch.label.visible = charging;
}
/* ------------------------------------------------------------------ *
 * Scene                                                               *
 * ------------------------------------------------------------------ */
// Build (or rebuild) the ring turntables and their bays from the authoritative
// slot list. Called on init and whenever the slot set changes structurally
// (count / id / layer), so the tower stays in lock-step with the backend.
function buildLevels() {
  for (let L = 0; L < ringCar.length; L++) {
    const ringsAtLevel = ringCar[L] || [];
    for (let ri = 0; ri < ringsAtLevel.length; ri++) {
      const grp = ringsAtLevel[ri];
      if (grp) {
        scene.remove(grp);
        disposeGroup(grp);
      }
    }
  }
  ringCar.length = 0;
  ringRot.length = 0;
  slotPads.clear();
  slotChargers.clear();
  slotContent.clear();
  lastStatusKey = "";
  computePlacement();

  for (let level = 0; level < LEVELS.length; level++) {
    const ly = LEVELS[level].y;
    ringCar[level] = [];
    ringRot[level] = [];
    for (let ri = 0; ri < RINGS.length; ri++) {
      const grp = new THREE.Group();
      scene.add(grp);
      ringCar[level][ri] = grp;
      ringRot[level][ri] = { rot: 0, tRot: 0 };
    }

    const idsForLevel = placementToSlot[level] || [];
    idsForLevel.forEach((globalIdx, within) => {
      const b = bayLayout(within);
      const grp = ringCar[level][b.ring];
      const x = Math.sin(b.angle) * b.R;
      const z = Math.cos(b.angle) * b.R;

      const pad = new THREE.Mesh(
        new THREE.BoxGeometry(2.9, 0.1, 3.9),
        new THREE.MeshStandardMaterial({ color: 0xf1f5fb, metalness: 0.05, roughness: 0.9 }),
      );
      pad.position.set(x, ly - 0.02, z);
      pad.rotation.y = b.angle;
      pad.receiveShadow = true;
      pad.userData.pick = { kind: "slot", index: globalIdx };
      grp.add(pad);

      const borderMat = new THREE.LineBasicMaterial({ color: STATUS_COLOR.empty, transparent: true, opacity: 0.9 });
      const border = new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.PlaneGeometry(2.9, 3.9)), borderMat);
      border.rotation.x = -Math.PI / 2;
      border.rotation.z = b.angle;
      border.position.set(x, ly + 0.05, z);
      grp.add(border);
      slotPads.set(globalIdx, borderMat);

      // bay-number tag floating above the spot
      const id = state.slots[globalIdx]?.id || "--";
      const tag = makeLabel(id, { color: "#1e293b", px: 44, bg: "rgba(255,255,255,0.9)" });
      tag.position.set(x, ly + 2.6, z);
      tag.scale.set(2.0, 1.0, 1);
      tag.userData.pick = { kind: "slot", index: globalIdx };
      grp.add(tag);

      // every bay has a charging post (state shown read-only from the slot list)
      const ch = makeCharger();
      ch.group.position.set(x, ly, z);
      ch.group.rotation.y = b.angle;
      ch.group.userData.pick = { kind: "charger", index: globalIdx };
      grp.add(ch.group);
      slotChargers.set(globalIdx, ch);
    });
  }
}

function buildStaticScene() {
  const ground = new THREE.Mesh(
    new THREE.CircleGeometry(RINGS[2].R + 14, 64),
    new THREE.MeshStandardMaterial({ color: 0xdfe6f1, metalness: 0.0, roughness: 0.96 }),
  );
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -0.55;
  ground.receiveShadow = true;
  scene.add(ground);

  scene.add(makeLiftAndTurntable());

  const postMat = new THREE.MeshStandardMaterial({ color: 0xaab4c6, metalness: 0.5, roughness: 0.45 });
  const topY = LEVELS[LEVELS.length - 1].y;
  for (let k = 0; k < 8; k++) {
    const a = (k / 8) * Math.PI * 2;
    const post = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.12, topY + 4, 10), postMat);
    post.position.set(Math.sin(a) * (RINGS[2].R + 2.6), (topY + 4) / 2 - 0.6, Math.cos(a) * (RINGS[2].R + 2.6));
    scene.add(post);
  }

  for (let level = 0; level < LEVELS.length; level++) {
    const ly = LEVELS[level].y;
    const deck = new THREE.Mesh(
      new THREE.RingGeometry(RINGS[0].R - 2.6, RINGS[2].R + 2.6, 64),
      new THREE.MeshStandardMaterial({ color: 0xeaf0f8, metalness: 0.05, roughness: 0.9, side: THREE.DoubleSide, transparent: true, opacity: 0.92 }),
    );
    deck.rotation.x = -Math.PI / 2;
    deck.position.y = ly - 0.08;
    deck.receiveShadow = true;
    scene.add(deck);

    const lvlLabel = makeLabel(LEVELS[level].label, { color: "#4f46e5", px: 38 });
    lvlLabel.position.set(0, ly + 1.2, -(RINGS[2].R + 4));
    lvlLabel.scale.set(4.4, 2.2, 1);
    scene.add(lvlLabel);
  }

  buildLevels();

  const camList = state.devices.cameras || [];
  for (let k = 0; k < 3; k++) {
    const a = (k / 3) * Math.PI * 2 + 0.4;
    const cam = makeCamera3d(camList[k] || { cameraId: `CAM-0${k + 1}` }, a);
    cam.position.set(Math.sin(a) * (RINGS[2].R + 5), 0, Math.cos(a) * (RINGS[2].R + 5));
    scene.add(cam);
    cameras3d.push(cam);
  }

  const gateData = state.devices.gates?.find((gt) => gt.gateId?.includes("OUT")) || { gateId: "OUT-GATE" };
  scene.add(makeBoomGate(gateData));

  // ===== permanent exit corridor (出口通道) + conveyor — always visible =====
  const lane = new THREE.Mesh(
    new THREE.BoxGeometry(4.4, 0.05, EXIT_Z + 2),
    new THREE.MeshStandardMaterial({ color: 0xfde68a, emissive: 0xf59e0b, emissiveIntensity: 0.12, roughness: 0.9, transparent: true, opacity: 0.55 }),
  );
  lane.position.set(0, -0.52, EXIT_Z / 2);
  scene.add(lane);
  const belt = new THREE.Mesh(
    new THREE.BoxGeometry(3.2, 0.28, EXIT_Z - 1.5),
    new THREE.MeshStandardMaterial({ color: 0x334155, metalness: 0.45, roughness: 0.55 }),
  );
  belt.position.set(0, -0.18, EXIT_Z / 2);
  belt.receiveShadow = true;
  scene.add(belt);
  const rollerMat = new THREE.MeshStandardMaterial({ color: 0xcbd5e1, metalness: 0.7, roughness: 0.3 });
  for (let z = 1.5; z <= EXIT_Z - 1; z += 1.0) {
    const roller = new THREE.Mesh(new THREE.CylinderGeometry(0.24, 0.24, 3.1, 16), rollerMat);
    roller.rotation.z = Math.PI / 2;
    roller.position.set(0, -0.02, z);
    scene.add(roller);
    conveyorRollers.push(roller);
  }
  const corridorLabel = makeLabel("出口通道", { color: "#9a3412", px: 34, bg: "rgba(254,243,199,0.92)" });
  corridorLabel.position.set(0, 0.7, EXIT_Z - 4.5);
  corridorLabel.scale.set(3.6, 1.5, 1);
  scene.add(corridorLabel);

  humanMarker = makeHumanMarker();
  humanMarker.position.set(2.6, 0, EXIT_Z - 1.5);
  scene.add(humanMarker);
}

function setupLights() {
  scene.add(new THREE.HemisphereLight(0xffffff, 0xdbe3f0, 1.0));
  ambient = new THREE.AmbientLight(0xffffff, 0.42);
  scene.add(ambient);
  const dir = new THREE.DirectionalLight(0xffffff, 2.3);
  dir.position.set(30, 60, 34);
  dir.castShadow = true;
  dir.shadow.mapSize.set(2048, 2048);
  dir.shadow.camera.near = 5;
  dir.shadow.camera.far = 220;
  dir.shadow.camera.left = -RINGS[2].R - 18;
  dir.shadow.camera.right = RINGS[2].R + 18;
  dir.shadow.camera.top = RINGS[2].R + 22;
  dir.shadow.camera.bottom = -RINGS[2].R - 22;
  dir.shadow.bias = -0.0004;
  scene.add(dir);
  const fill = new THREE.DirectionalLight(0xe3ebf8, 0.7);
  fill.position.set(-28, 32, -26);
  scene.add(fill);
  accentLightA = new THREE.PointLight(0xbcd0ff, 0.25, 200, 1.6);
  accentLightA.position.set(0, 26, 0);
  scene.add(accentLightA);
}

/* ------------------------------------------------------------------ *
 * Carousel control — target ring to front, other rings present a gap  *
 * ------------------------------------------------------------------ */
function setCarouselTargets(slotIdx) {
  const L = levelOf(slotIdx);
  const b = bayLayout(withinOf(slotIdx));
  const rr = ringRot[L];
  if (!rr) return;
  for (let ri = 0; ri < RINGS.length; ri++) {
    const step = (Math.PI * 2) / RINGS[ri].count;
    rr[ri].tRot = ri === b.ring ? nearestEquiv(rr[ri].rot, -b.angle) : nearestGapRot(rr[ri].rot, step);
  }
}

function applyCarousels(dt) {
  for (let L = 0; L < LEVELS.length; L++) {
    const rr = ringRot[L];
    if (!rr) continue;
    for (let ri = 0; ri < RINGS.length; ri++) {
      rr[ri].rot += (rr[ri].tRot - rr[ri].rot) * Math.min(dt * 2.2, 1);
      if (ringCar[L] && ringCar[L][ri]) ringCar[L][ri].rotation.y = rr[ri].rot;
    }
  }
}

/* ------------------------------------------------------------------ *
 * Choreography                                                        *
 * ------------------------------------------------------------------ */
function buildCtx() {
  const idx = activeSlotOverride.value ?? demoScenario.value.slotIndex;
  const L = levelOf(idx);
  const b = bayLayout(withinOf(idx));
  const ly = LEVELS[L].y;
  return {
    idx,
    L,
    ly,
    R: b.R,
    front: V3(0, ly, b.R),
    center: (y) => V3(0, y + RIDE, 0),
    headIn: Math.PI,
    headExit: 0,
    gate: V3(0, RIDE, EXIT_Z - 1.5),
    handoff: V3(0, RIDE, HANDOFF_Z),
    personGate: V3(2.6, 0, EXIT_Z - 1.5),
    personHandoff: V3(2.4, 0, HANDOFF_Z),
  };
}

function towerFrame(step, pr, c) {
  const f = { liftY: 0, carPos: null, carYaw: c.headIn, gateOpen: 0, turn: 0, human: null, bayHidden: true, carrier: "none" };
  const sid = demoScenario.value.id;

  if ((sid === "retrieve" || sid === "touch") && step === 0) {
    f.liftY = c.ly * easeInOut(Math.min(pr / 0.55, 1));
    if (pr < 0.55) {
      f.carPos = null;
      f.bayHidden = false;
    } else {
      const e = easeInOut((pr - 0.55) / 0.45);
      f.carPos = c.front.clone().lerp(c.center(c.ly), e);
      f.bayHidden = true;
      f.carrier = "fork";
    }
    return f;
  }
  if ((sid === "retrieve" || sid === "touch") && step === 1) {
    const e = easeInOut(pr);
    f.liftY = c.ly * (1 - e);
    f.carPos = c.center(c.ly).lerp(c.center(0), e);
    f.carrier = "lift";
    return f;
  }

  if (sid === "retrieve") {
    if (step === 2) {
      const e = easeInOut(pr);
      f.carPos = c.center(0);
      f.carYaw = lerpAngle(c.headIn, c.headExit, e);
      f.turn = e * Math.PI;
      f.gateOpen = e * 0.5;
      f.carrier = "conveyor";
      return f;
    }
    const e = easeInOut(pr);
    f.carPos = c.center(0).lerp(c.gate, e);
    f.carYaw = c.headExit;
    f.gateOpen = 1;
    f.human = c.personGate;
    f.carrier = "conveyor";
    return f;
  }

  if (sid === "storage") {
    if (step === 0) {
      f.carPos = c.gate.clone();
      f.carYaw = Math.PI;
      f.gateOpen = 1;
      f.human = c.personGate;
      f.carrier = "conveyor";
      return f;
    }
    if (step === 1) {
      const e = easeInOut(pr);
      f.carPos = c.gate.clone().lerp(c.center(0), e);
      f.carYaw = Math.PI;
      f.gateOpen = 1 - e * 0.6;
      f.carrier = "conveyor";
      return f;
    }
    if (step === 2) {
      const e = easeInOut(pr);
      f.liftY = c.ly * e;
      f.carPos = c.center(0).lerp(c.center(c.ly), e);
      f.carYaw = c.headIn;
      f.carrier = "lift";
      return f;
    }
    const e = easeInOut(pr);
    f.liftY = c.ly;
    f.carPos = c.center(c.ly).lerp(c.front, e);
    f.carYaw = c.headIn;
    f.carrier = "fork";
    return f;
  }

  // touch steps 2..4
  if (step === 2) {
    if (pr < 0.4) {
      const e = easeInOut(pr / 0.4);
      f.carPos = c.center(0);
      f.carYaw = lerpAngle(c.headIn, c.headExit, e);
      f.turn = e * Math.PI;
    } else {
      const e = easeInOut((pr - 0.4) / 0.6);
      f.carPos = c.center(0).lerp(c.handoff, e);
      f.carYaw = c.headExit;
    }
    f.human = c.personHandoff;
    f.carrier = "conveyor";
    return f;
  }
  if (step === 3) {
    if (pr < 0.4) {
      const e = easeInOut(pr / 0.4);
      f.carPos = c.handoff.clone().lerp(c.center(0), e);
      f.carYaw = c.headExit;
      f.human = c.personHandoff;
      f.carrier = "conveyor";
    } else if (pr < 0.6) {
      const e = easeInOut((pr - 0.4) / 0.2);
      f.carPos = c.center(0);
      f.carYaw = lerpAngle(c.headExit, c.headIn, e);
      f.turn = (1 - e) * Math.PI;
    } else {
      const e = easeInOut((pr - 0.6) / 0.4);
      f.liftY = c.ly * e;
      f.carPos = c.center(0).lerp(c.center(c.ly), e);
      f.carYaw = c.headIn;
      f.carrier = "lift";
    }
    return f;
  }
  const e = easeInOut(pr);
  f.liftY = c.ly;
  f.carPos = c.center(c.ly).lerp(c.front, e);
  f.carYaw = c.headIn;
  f.carrier = "fork";
  return f;
}

function advanceDemo(dt) {
  const steps = demoScenario.value.steps;
  if (!steps.length) return;
  demoStepElapsed.value += dt;
  const duration = demoStep.value?.duration || 3;
  if (demoStepElapsed.value >= duration) {
    if (demoStepIndex.value + 1 >= steps.length) {
      demoStepElapsed.value = duration;
      demoPlaying.value = false;
      lastStatusKey = "";
    } else {
      demoStepElapsed.value = 0;
      demoStepIndex.value += 1;
    }
  }
}

function playScenario(id, slotOverride = null) {
  const sc = DEMO_SCENARIOS.find((s) => s.id === id) || DEMO_SCENARIOS[0];
  demoScenarioId.value = sc.id;
  activeSlotOverride.value = slotOverride;
  demoStepIndex.value = 0;
  demoStepElapsed.value = 0;
  demoPlaying.value = true;
  const idx = slotOverride ?? sc.slotIndex;
  setCarouselTargets(idx);
  const color = (id === "retrieve" || id === "touch") ? CAR_COLORS[idx % CAR_COLORS.length] : sc.carColor;
  if (scene) rebuildPayload(color);
}

async function playScenarioWithApi(id, slotOverride = null) {
  const sc = DEMO_SCENARIOS.find((s) => s.id === id) || DEMO_SCENARIOS[0];

  if (id === "retrieve") {
    const idx = slotOverride ?? sc.slotIndex;
    const slot = state.slots[idx];
    playScenario(id, slotOverride);
    setTimeout(async () => {
      if (slot && slot.status !== "empty") {
        const order = state.orders.find((o) => o.slotId === slot.id && o.status !== "FINISHED");
        try {
          if (order) {
            await parkvisionApi.retrieveOrder(order.orderNo);
            await parkvisionApi.payOrder(order.orderNo);
          } else {
            await parkvisionApi.clearSlot(slot.id);
          }
        } catch {}
      }
      void refreshCore();
      void refreshAdminData();
    }, 6000);
  } else if (id === "storage") {
    try {
      const order = await parkvisionApi.simulateEntry();
      const slotIdx = state.slots.findIndex((s) => s.id === order.slotId);
      playScenario(id, slotIdx >= 0 ? slotIdx : slotOverride);
      void refreshCore();
      void refreshAdminData();
    } catch {
      playScenario(id, slotOverride);
    }
  } else {
    playScenario(id, slotOverride);
  }
}

function rebuildPayload(color) {
  if (payloadCar) {
    scene.remove(payloadCar);
    disposeGroup(payloadCar);
  }
  payloadCar = new THREE.Group();
  const pallet = makePallet();
  const car = makeCar(color);
  car.position.y = 0.12;
  payloadCar.add(pallet, car);
  payloadCar.visible = false;
  scene.add(payloadCar);
}

watch(
  () => state.twinSignal.seq,
  (seq, prev) => {
    if (seq && seq !== prev && state.twinSignal.scenario && !demoPlaying.value) {
      const slotIdx = state.twinSignal.slotId
        ? state.slots.findIndex((s) => s.id === state.twinSignal.slotId)
        : null;
      playScenario(state.twinSignal.scenario, slotIdx >= 0 ? slotIdx : null);
    }
  },
);

// Rebuild the tower bays when the slot set changes structurally (count / id /
// layer) — e.g. when the live backend list replaces the initial fallback data.
// Pure status changes don't alter this key, so they stay on the lightweight path.
watch(
  () => state.slots.map((s) => `${s.id}:${s.layer}`).join("|"),
  () => {
    if (!scene) return;
    buildLevels();
    syncSlots();
  },
);

function runTower(dt, t) {
  if (!demoPlaying.value) {
    if (payloadCar) payloadCar.visible = false;
    if (liftCage) liftCage.position.y += (0 - liftCage.position.y) * Math.min(dt * 3, 1);
    if (humanMarker) humanMarker.visible = false;
    if (forkPivot) forkPivot.visible = false;
    if (gateBar) gateBar.rotation.z = (gateManual.value ? 1 : 0) * (Math.PI / 2);
    // keep the front exit corridor clear of cars while idle
    for (let L = 0; L < LEVELS.length; L++) {
      const rr = ringRot[L];
      if (!rr) continue;
      for (let ri = 0; ri < RINGS.length; ri++) rr[ri].tRot = nearestGapRot(rr[ri].rot, (Math.PI * 2) / RINGS[ri].count);
    }
    applyCarousels(dt);
    conveyorRollers.forEach((r) => (r.rotation.x += dt * 0.8));
    const lvl0 = Math.round(liftCage ? liftCage.position.y / LEVEL_H : 0) + 1;
    if (lvl0 !== liftLevel.value) liftLevel.value = clamp(lvl0, 1, LEVELS.length);
    return -1;
  }

  advanceDemo(dt);
  applyCarousels(dt);
  const c = buildCtx();
  const dur = demoStep.value?.duration || 3;
  const pr = clamp(demoStepElapsed.value / dur, 0, 1);
  const f = towerFrame(demoStepIndex.value, pr, c);

  if (liftCage) liftCage.position.y = f.liftY;
  if (turntable) turntable.rotation.y = f.turn;

  if (payloadCar && f.carPos) {
    payloadCar.visible = true;
    payloadCar.position.copy(f.carPos);
    payloadCar.rotation.y = f.carYaw;
  } else if (payloadCar) {
    payloadCar.visible = false;
  }

  if (forkPivot) {
    const show = f.carrier === "fork" && !!f.carPos;
    forkPivot.visible = show;
    if (show) {
      forkPivot.rotation.y = Math.atan2(f.carPos.x, f.carPos.z);
      forkPivot.position.y = f.carPos.y - liftCage.position.y - 0.22;
    }
  }
  conveyorRollers.forEach((r) => (r.rotation.x += f.carrier === "conveyor" ? dt * 6 : dt * 0.7));

  if (gateBar) {
    const open = Math.max(f.gateOpen, gateManual.value ? 1 : 0);
    gateBar.rotation.z = open * (Math.PI / 2);
  }

  if (humanMarker) {
    humanMarker.visible = !!f.human;
    if (f.human) {
      humanMarker.position.copy(f.human);
      humanMarker.userData.halo.material.opacity = 0.38 + 0.3 * (0.5 + 0.5 * Math.sin(t * 4));
    }
  }

  const lvl = Math.round(f.liftY / LEVEL_H) + 1;
  if (lvl !== liftLevel.value) liftLevel.value = clamp(lvl, 1, LEVELS.length);

  return f.bayHidden ? c.idx : -1;
}

/* ------------------------------------------------------------------ *
 * Interactivity                                                       *
 * ------------------------------------------------------------------ */
function findPick(object) {
  let o = object;
  while (o) {
    if (o.userData && o.userData.pick) return o.userData.pick;
    o = o.parent;
  }
  return null;
}

function onCanvasPick(ev) {
  if (!renderer || !camera) return;
  const rect = renderer.domElement.getBoundingClientRect();
  pointer.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1;
  pointer.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1;
  raycaster.setFromCamera(pointer, camera);
  const hits = raycaster.intersectObjects(scene.children, true);
  for (const hit of hits) {
    const pick = findPick(hit.object);
    if (pick) {
      selectPick(pick, hit.point);
      return;
    }
  }
  selected.value = null;
}

function slotMeta(index) {
  const slot = state.slots[index];
  const level = LEVELS[levelOf(index)]?.label || "L1";
  const ring = RING_LABEL[bayLayout(withinOf(index)).ring];
  const order = state.orders.find((o) => o.slotId === slot?.id);
  return { slot, level, ring, order };
}

function selectPick(pick, point) {
  if (pick.kind === "slot") {
    const { slot, level, ring, order } = slotMeta(pick.index);
    if (!slot) return;
    const occupied = slot.status !== "empty";
    const fields = [
      ["层级", level],
      ["环位", ring],
      ["状态", zhText(slot.status)],
    ];
    if (occupied && order) {
      fields.push(["车牌", order.plateNo], ["订单", order.orderNo]);
    }
    selected.value = {
      kind: "slot",
      title: `车位 ${slot.id}`,
      badge: zhText(slot.status),
      badgeClass: slot.status,
      fields,
      actions: occupied
        ? [{ label: "取车出库", icon: "fa-car-side", fn: () => playScenarioWithApi("retrieve", pick.index) },
           { label: "临停取物", icon: "fa-box-open", fn: () => playScenario("touch", pick.index) }]
        : [{ label: "存车入库", icon: "fa-arrow-right-to-bracket", fn: () => playScenarioWithApi("storage", pick.index) }],
      anchor: point.clone(),
    };
  } else if (pick.kind === "charger") {
    const { slot } = slotMeta(pick.index);
    const charging = isCharging(pick.index);
    selected.value = {
      kind: "charger",
      title: `充电桩 ${slot?.id || ""}`,
      badge: charging ? "充电中" : "待机",
      badgeClass: charging ? "charging" : "empty",
      fields: [
        ["关联车位", slot?.id || "--"],
        ["状态", charging ? "充电中" : slot?.status === "empty" ? "无车" : "已就绪"],
        ["功率", "7 kW 交流"],
        ["本次电量", charging ? "12.4 kWh" : "—"],
      ],
      actions: [],
      anchor: point.clone(),
    };
  } else if (pick.kind === "camera") {
    const cam = pick.data || {};
    selected.value = {
      kind: "camera",
      title: cam.cameraId || "摄像头",
      badge: cam.intrusionState ? "入侵告警" : "正常",
      badgeClass: cam.intrusionState ? "maintenance" : "charging",
      fields: [
        ["最近车牌", cam.lastPlate || state.activePlate || "--"],
        ["入侵检测", cam.intrusionState ? "是" : "否"],
        ["状态", cam.status || "在线"],
      ],
      actions: [{ label: "查看 AI 感知", icon: "fa-eye", fn: () => {} }],
      anchor: point.clone(),
    };
  } else if (pick.kind === "gate") {
    const gate = pick.data || {};
    selected.value = {
      kind: "gate",
      title: gate.gateId || "出场闸机",
      badge: gateManual.value ? "已抬杆" : "落杆",
      badgeClass: gateManual.value ? "charging" : "occupied",
      fields: [
        ["闸机状态", gateManual.value ? "抬杆放行" : zhText(gate.gateState, "就绪")],
        ["急停", gate.estopArmed ? "已锁定" : "正常"],
      ],
      actions: [{ label: gateManual.value ? "落杆" : "抬杆放行", icon: "fa-up-down", fn: () => (gateManual.value = !gateManual.value) }],
      anchor: point.clone(),
    };
  } else if (pick.kind === "lift") {
    selected.value = {
      kind: "lift",
      title: "升降梯 / 回转盘",
      badge: `运行至 L${liftLevel.value}`,
      badgeClass: "occupied",
      fields: [
        ["当前层", `L${liftLevel.value}`],
        ["作业", demoPlaying.value ? demoStep.value?.label || "运行" : "空闲待命"],
      ],
      actions: [],
      anchor: point.clone(),
    };
  }
}

function closePanel() {
  selected.value = null;
}

/* ------------------------------------------------------------------ *
 * Data sync                                                           *
 * ------------------------------------------------------------------ */
function syncSlots() {
  const key = state.slots.map((s) => s.status).join("|");
  if (key === lastStatusKey) return;
  lastStatusKey = key;

  const animIdx = demoPlaying.value ? (activeSlotOverride.value ?? demoScenario.value.slotIndex) : -1;

  state.slots.forEach((slot, idx) => {
    if (!slotPlacement[idx]) return;
    if (idx === animIdx) return;
    const status = slot.status || "empty";
    const level = levelOf(idx);
    const within = withinOf(idx);
    const b = bayLayout(within);

    const mat = slotPads.get(idx);
    if (mat) mat.color.setHex(STATUS_COLOR[status] ?? STATUS_COLOR.empty);
    setChargerState(idx);

    const type = slotType(status);
    const prev = slotContent.get(idx);
    if (prev && prev.type === type && prev.status === status) return;
    if (prev) {
      prev.group.parent?.remove(prev.group);
      disposeGroup(prev.group);
    }
    const content = buildSlotContent(type, idx, status);
    if (content.children.length) {
      const x = Math.sin(b.angle) * b.R;
      const z = Math.cos(b.angle) * b.R;
      content.position.set(x, LEVELS[level].y, z);
      content.rotation.y = b.angle + Math.PI;
      content.userData.pick = { kind: "slot", index: idx };
      content.userData.spawn = 0;
      content.scale.setScalar(0.01);
      ringCar[level][b.ring].add(content);
    }
    slotContent.set(idx, { group: content, type, status });
  });
}

function disposeGroup(group) {
  group.traverse((o) => {
    if (o.geometry) o.geometry.dispose();
    if (o.material) {
      const mats = Array.isArray(o.material) ? o.material : [o.material];
      mats.forEach((m) => {
        if (m.map) m.map.dispose();
        m.dispose();
      });
    }
  });
}

/* ------------------------------------------------------------------ *
 * Render loop                                                         *
 * ------------------------------------------------------------------ */
function animate() {
  frameId = requestAnimationFrame(animate);
  const dt = Math.min(clock.getDelta(), 0.05);
  const t = clock.elapsedTime;

  if (tween) {
    tween.t = Math.min(tween.t + dt / 0.8, 1);
    const e = 1 - Math.pow(1 - tween.t, 3);
    camera.position.lerpVectors(tween.from, tween.to, e);
    controls.target.lerpVectors(tween.fromT, tween.toT, e);
    if (tween.t >= 1) {
      tween = null;
      controls.enabled = true;
    }
  }
  controls.autoRotate = autoRotate.value && !tween;
  if (!tween) controls.update();

  syncSlots();
  const hiddenSlot = runTower(dt, t);

  slotContent.forEach((entry, idx) => {
    const g = entry.group;
    g.visible = idx !== hiddenSlot;
    if (g.userData.spawn != null && g.userData.spawn < 1) {
      g.userData.spawn = Math.min(g.userData.spawn + dt * 2.4, 1);
      const s = g.userData.spawn;
      g.scale.setScalar(0.01 + (1 - 0.01) * (1 - Math.pow(1 - s, 3)));
    }
  });

  const pulse = 0.8 + Math.sin(t * 4) * 0.6;
  pulseLeds.forEach((m) => (m.emissiveIntensity = pulse));

  if (selected.value && selected.value.anchor && canvasHost.value) {
    const v = selected.value.anchor.clone().project(camera);
    const w = canvasHost.value.clientWidth;
    const h = canvasHost.value.clientHeight;
    selectedScreen.value = { x: (v.x * 0.5 + 0.5) * w, y: (-v.y * 0.5 + 0.5) * h, visible: v.z < 1 };
  }

  const targetMix = state.emergency ? 1 : 0;
  emergencyMix += (targetMix - emergencyMix) * Math.min(dt * 3, 1);
  if (emergencyMix > 0.001) {
    const flash = 0.5 + Math.sin(t * 8) * 0.5;
    ambient.color.setRGB(1, 1 - 0.5 * emergencyMix * flash, 1 - 0.55 * emergencyMix * flash);
    accentLightA.color.setRGB(1, 0.45 * (1 - emergencyMix), 0.45 * (1 - emergencyMix));
  } else {
    ambient.color.setHex(0xffffff);
    accentLightA.color.setHex(0xbcd0ff);
  }

  azimuthDeg.value = THREE.MathUtils.radToDeg(controls.getAzimuthalAngle());
  renderer.render(scene, camera);
}

/* ------------------------------------------------------------------ *
 * View controls                                                       *
 * ------------------------------------------------------------------ */
function zoomBy(factor) {
  const dir = camera.position.clone().sub(controls.target);
  const len = clamp(dir.length() * factor, controls.minDistance, controls.maxDistance);
  dir.setLength(len);
  camera.position.copy(controls.target.clone().add(dir));
}
function rotateAzimuth(deltaDeg) {
  const offset = camera.position.clone().sub(controls.target);
  const ang = THREE.MathUtils.degToRad(deltaDeg);
  const cos = Math.cos(ang);
  const sin = Math.sin(ang);
  const x = offset.x * cos - offset.z * sin;
  const z = offset.x * sin + offset.z * cos;
  offset.x = x;
  offset.z = z;
  camera.position.copy(controls.target.clone().add(offset));
}
function startTween(toPos, toTarget) {
  controls.enabled = false;
  tween = { from: camera.position.clone(), to: toPos.clone(), fromT: controls.target.clone(), toT: toTarget.clone(), t: 0 };
}
function resetView() {
  autoRotate.value = false;
  startTween(DEFAULT_CAM, DEFAULT_TARGET);
}
function topView() {
  autoRotate.value = false;
  startTween(new THREE.Vector3(0.01, 140, 0.01), new THREE.Vector3(0, 6, 0));
}
function toggleAutoRotate() {
  autoRotate.value = !autoRotate.value;
}

/* ------------------------------------------------------------------ *
 * Lifecycle                                                           *
 * ------------------------------------------------------------------ */
function onResize() {
  if (!renderer || !canvasHost.value) return;
  const w = canvasHost.value.clientWidth;
  const h = canvasHost.value.clientHeight;
  if (!w || !h) return;
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h, false);
}

onMounted(() => {
  try {
    const host = canvasHost.value;
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0xeaf0f8);
    scene.fog = new THREE.Fog(0xeaf0f8, 130, 320);
    camera = new THREE.PerspectiveCamera(46, 1, 0.1, 700);
    camera.position.copy(DEFAULT_CAM);
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    host.appendChild(renderer.domElement);

    controls = new OrbitControls(camera, renderer.domElement);
    controls.target.copy(DEFAULT_TARGET);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.minDistance = 22;
    controls.maxDistance = 220;
    controls.maxPolarAngle = THREE.MathUtils.degToRad(89);
    controls.autoRotateSpeed = 0.7;
    controls.update();

    setupLights();
    buildStaticScene();
    syncSlots();
    rebuildPayload(demoScenario.value.carColor);

    clock = new THREE.Clock();
    onResize();
    animate();

    resizeObserver = new ResizeObserver(onResize);
    resizeObserver.observe(host);
    window.addEventListener("resize", onResize);
    renderer.domElement.addEventListener("pointerdown", onCanvasPick);
  } catch (err) {
    initError.value = String(err?.message || err);
    console.error("[TwinView] init failed", err);
  }
});

onBeforeUnmount(() => {
  cancelAnimationFrame(frameId);
  window.removeEventListener("resize", onResize);
  if (renderer) renderer.domElement.removeEventListener("pointerdown", onCanvasPick);
  if (resizeObserver) resizeObserver.disconnect();
  if (controls) controls.dispose();
  if (renderer) {
    renderer.dispose();
    renderer.domElement?.remove();
  }
  if (scene) {
    scene.traverse((o) => {
      if (o.geometry) o.geometry.dispose();
      if (o.material) {
        const mats = Array.isArray(o.material) ? o.material : [o.material];
        mats.forEach((m) => {
          if (m.map) m.map.dispose();
          m.dispose();
        });
      }
    });
  }
  slotContent.clear();
  slotPads.clear();
  slotChargers.clear();
  cameras3d.length = 0;
  conveyorRollers.length = 0;
  ringCar.length = 0;
  ringRot.length = 0;
  pulseLeds.length = 0;
  liftCage = turntable = payloadCar = humanMarker = gateBar = forkPivot = forkArm = null;
});
</script>

<template>
  <section class="twin-layout" :class="{ 'twin-layout-emergency': state.emergency }">
    <article class="surface wide twin-stage">
      <div ref="canvasHost" class="twin-canvas" :class="{ emergency: state.emergency }">
        <div class="hud-card top-left">
          <div class="hud-title"><i class="fa-solid fa-cubes"></i> 立体车库孪生</div>
          <div class="hud-row"><span>车位空闲</span><strong class="ok">{{ freeSlots }}</strong></div>
          <div class="hud-row"><span>车位占用</span><strong>{{ occupiedSlots }}</strong></div>
          <div class="hud-row"><span>充电中</span><strong class="charge">{{ chargingSlots }}</strong></div>
          <div class="hud-row"><span>升降梯</span><strong class="lift">运行至 L{{ liftLevel }}</strong></div>
          <div class="hud-row">
            <span>系统状态</span>
            <strong :class="state.emergency ? 'danger' : 'ok'">{{ state.emergency ? "急停锁定" : "自动运行" }}</strong>
          </div>
        </div>

        <div v-if="demoPlaying" class="demo-strip">
          <span class="demo-strip-head">
            <i class="fa-solid" :class="demoScenario.icon"></i>
            {{ demoScenario.label }} · 车位 {{ focusSlotId }} · {{ demoStep.label }}
          </span>
          <span class="demo-strip-bar"><i :style="{ width: `${demoProgress}%`, background: demoScenario.accent }"></i></span>
        </div>

        <div class="hud-compass">
          <div class="compass-dial" :style="{ transform: `rotate(${-azimuthDeg}deg)` }">
            <span class="tick n">N</span><span class="tick e">E</span><span class="tick s">S</span><span class="tick w">W</span>
          </div>
        </div>

        <div class="hud-legend">
          <span><i class="dot empty"></i>空闲</span>
          <span><i class="dot occupied"></i>占用</span>
          <span><i class="dot charging"></i>充电</span>
          <span><i class="dot buffer"></i>缓冲</span>
          <span><i class="dot maintenance"></i>维护</span>
          <span class="level-hint"><i class="fa-solid fa-tower-observation"></i> 三环回转塔库 · 每位带充电桩 · 点击可控</span>
        </div>

        <div class="view-tools">
          <button @click="zoomBy(0.82)" title="放大"><i class="fa-solid fa-magnifying-glass-plus"></i></button>
          <button @click="zoomBy(1.22)" title="缩小"><i class="fa-solid fa-magnifying-glass-minus"></i></button>
          <button @click="rotateAzimuth(-20)" title="向左旋转"><i class="fa-solid fa-rotate-left"></i></button>
          <button @click="rotateAzimuth(20)" title="向右旋转"><i class="fa-solid fa-rotate-right"></i></button>
          <button @click="topView" title="俯视视角"><i class="fa-solid fa-table-cells"></i></button>
          <button :class="{ active: autoRotate }" @click="toggleAutoRotate" title="自动环绕"><i class="fa-solid fa-arrows-spin"></i></button>
          <button @click="resetView" title="重置视角"><i class="fa-solid fa-arrows-to-eye"></i></button>
        </div>

        <div class="hud-hint"><i class="fa-solid fa-hand-pointer"></i> 点击车位 / 车辆 / 充电桩 / 摄像头 / 闸机查看与操控</div>

        <div
          v-if="selected"
          class="inspect-panel"
          :style="{ left: `${selectedScreen.x}px`, top: `${selectedScreen.y}px` }"
        >
          <div class="inspect-head">
            <strong>{{ selected.title }}</strong>
            <span v-if="selected.badge" class="inspect-badge" :class="selected.badgeClass">{{ selected.badge }}</span>
            <button class="inspect-close" @click="closePanel"><i class="fa-solid fa-xmark"></i></button>
          </div>
          <div class="inspect-fields">
            <div v-for="(f, k) in selected.fields" :key="k"><span>{{ f[0] }}</span><b>{{ f[1] }}</b></div>
          </div>
          <div v-if="selected.actions.length" class="inspect-actions">
            <button v-for="(a, k) in selected.actions" :key="k" @click="a.fn(); closePanel()">
              <i class="fa-solid" :class="a.icon"></i>{{ a.label }}
            </button>
          </div>
        </div>

        <div v-if="initError" class="twin-init-error">3D 场景初始化失败：{{ initError }}</div>
        <div v-if="state.emergency" class="twin-emergency-banner"><i class="fa-solid fa-triangle-exclamation"></i> 紧急停机已激活</div>
      </div>
    </article>

    <aside class="twin-side-panel">
      <article class="surface twin-demo-card">
        <div class="section-head compact">
          <h2>立体调度联动</h2>
          <span class="demo-state" :class="{ live: demoPlaying }">
            <i class="fa-solid" :class="demoPlaying ? 'fa-circle-play' : 'fa-circle-pause'"></i>
            {{ demoPlaying ? "运行中" : "待触发" }}
          </span>
        </div>
        <div class="demo-switcher">
          <button
            v-for="scenario in DEMO_SCENARIOS"
            :key="scenario.id"
            type="button"
            :class="{ active: demoPlaying && scenario.id === demoScenarioId }"
            @click="playScenarioWithApi(scenario.id)"
          >
            <i class="fa-solid" :class="scenario.icon"></i>{{ scenario.label }}
          </button>
        </div>
        <p class="demo-summary">{{ demoScenario.summary }}</p>
        <div class="demo-progress"><span :style="{ width: `${demoProgress}%`, background: demoScenario.accent }"></span></div>
        <div class="demo-steps">
          <div
            v-for="(step, index) in demoScenario.steps"
            :key="step.key"
            class="demo-step-chip"
            :class="{ active: demoPlaying && index === demoStepIndex, done: demoPlaying && index < demoStepIndex }"
          >
            <span>{{ index + 1 }}</span>{{ step.label }}
          </div>
        </div>
        <div class="demo-note">
          <i class="fa-solid" :class="demoPlaying ? demoScenario.icon : 'fa-hand-pointer'"></i>
          <div>
            <strong>{{ demoPlaying ? demoStep.label : "点击场景或场景中的车位播放" }}</strong>
            <span>{{ demoPlaying ? demoStep.note : "也会随车主端取车 / 临停取物、登记入场等真实操作自动播放一次。" }}</span>
          </div>
        </div>
      </article>

      <article class="surface twin-safety-shell">
        <div class="safety-card" :class="{ danger: state.emergency }">
          <strong>
            <i class="fa-solid" :class="state.emergency ? 'fa-triangle-exclamation' : 'fa-shield-check'"></i>
            {{ state.emergency ? "安全停机生效" : "安全边界正常" }}
          </strong>
          <span>
            {{
              state.emergency
                ? "后端安全锁已经生效，PLC 放行输出会在复核通过前保持禁止。"
                : `交接区未检测到入侵，${safetyGate?.gateId || "出场闸机"} 当前状态为 ${zhText(safetyGate?.gateState, "就绪")}。`
            }}
          </span>
        </div>
      </article>
    </aside>
  </section>
</template>

<style scoped>
.twin-canvas {
  position: relative;
  width: 100%;
  height: 640px;
  border-radius: 16px;
  overflow: hidden;
  background: radial-gradient(circle at 50% 30%, #ffffff 0%, #e6edf6 72%);
  border: 1px solid rgba(99, 102, 241, 0.18);
  box-shadow: inset 0 0 60px rgba(99, 102, 241, 0.05), 0 16px 40px -18px rgba(15, 23, 42, 0.18);
  cursor: grab;
}
.twin-canvas:active { cursor: grabbing; }
.twin-canvas.emergency { border-color: rgba(239, 68, 68, 0.45); }
.twin-canvas :deep(canvas) { display: block; width: 100% !important; height: 100% !important; }

.hud-card {
  position: absolute; top: 18px; left: 18px; z-index: 10; min-width: 196px;
  padding: 14px 16px; border-radius: 12px; background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(99, 102, 241, 0.16); box-shadow: 0 10px 30px -14px rgba(15, 23, 42, 0.25);
  color: var(--text-main); font-size: 13px;
}
.hud-title { font-family: "Orbitron", sans-serif; font-size: 13px; color: var(--brand); margin-bottom: 10px; display: flex; align-items: center; gap: 8px; }
.hud-row { display: flex; justify-content: space-between; gap: 16px; padding: 4px 0; }
.hud-row span { color: var(--text-muted); }
.hud-row strong { font-family: "Orbitron", sans-serif; color: var(--text-main); }
.hud-row strong.ok { color: var(--safety-green); }
.hud-row strong.charge { color: #059669; }
.hud-row strong.lift { color: var(--brand); font-size: 12px; }
.hud-row strong.danger { color: var(--danger-red); }

.demo-strip {
  position: absolute; bottom: 52px; left: 50%; transform: translateX(-50%); z-index: 10;
  width: min(470px, calc(100% - 230px)); display: flex; flex-direction: column; gap: 6px;
  padding: 9px 16px; border-radius: 12px; background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(99, 102, 241, 0.16); backdrop-filter: blur(10px);
  box-shadow: 0 10px 26px -14px rgba(15, 23, 42, 0.32);
}
.demo-strip-head { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 600; color: var(--text-main); }
.demo-strip-head i { color: var(--brand); }
.demo-strip-bar { height: 4px; border-radius: 999px; background: rgba(148, 163, 184, 0.2); overflow: hidden; }
.demo-strip-bar i { display: block; height: 100%; border-radius: inherit; transition: width 0.3s ease; }

.inspect-panel {
  position: absolute; z-index: 14; transform: translate(14px, -50%);
  min-width: 200px; max-width: 244px; padding: 12px 14px; border-radius: 12px;
  background: rgba(255, 255, 255, 0.94); border: 1px solid rgba(99, 102, 241, 0.22);
  box-shadow: 0 16px 40px -16px rgba(15, 23, 42, 0.4); backdrop-filter: blur(12px);
  color: var(--text-main); font-size: 13px;
}
.inspect-head { display: flex; align-items: center; gap: 8px; }
.inspect-head strong { font-size: 14px; flex: 1; }
.inspect-badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; color: #fff; background: #3b82f6; }
.inspect-badge.empty { background: #94a3b8; }
.inspect-badge.occupied { background: #3b82f6; }
.inspect-badge.charging { background: #10b981; }
.inspect-badge.buffer { background: #f59e0b; }
.inspect-badge.maintenance { background: #ef4444; }
.inspect-close { background: transparent; color: var(--text-muted); width: 22px; height: 22px; border-radius: 6px; cursor: pointer; }
.inspect-close:hover { background: rgba(148, 163, 184, 0.16); color: var(--text-main); }
.inspect-fields { margin: 10px 0; display: grid; gap: 6px; }
.inspect-fields div { display: flex; justify-content: space-between; gap: 10px; }
.inspect-fields span { color: var(--text-muted); font-size: 12px; }
.inspect-fields b { color: var(--text-main); font-size: 12px; }
.inspect-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.inspect-actions button {
  flex: 1 1 auto; min-height: 32px; padding: 0 10px; border-radius: 8px; cursor: pointer;
  font-size: 12px; color: #fff; background: var(--brand); display: inline-flex; align-items: center; justify-content: center; gap: 6px; border: 0;
}
.inspect-actions button:hover { filter: brightness(1.05); }

.hud-compass {
  position: absolute; top: 18px; right: 18px; z-index: 10; width: 64px; height: 64px; border-radius: 50%;
  background: rgba(255, 255, 255, 0.82); border: 1px solid rgba(99, 102, 241, 0.18); backdrop-filter: blur(10px);
}
.compass-dial { position: absolute; inset: 0; }
.compass-dial .tick { position: absolute; font-family: "Orbitron", sans-serif; font-size: 11px; color: var(--text-muted); left: 50%; transform: translateX(-50%); }
.compass-dial .tick.n { top: 5px; color: var(--danger-red); font-weight: 700; }
.compass-dial .tick.s { bottom: 5px; }
.compass-dial .tick.e { right: 6px; top: 50%; left: auto; transform: translateY(-50%); }
.compass-dial .tick.w { left: 6px; top: 50%; transform: translateY(-50%); }

.hud-legend {
  position: absolute; bottom: 16px; left: 18px; z-index: 10; display: flex; flex-wrap: wrap; align-items: center; gap: 12px;
  padding: 10px 14px; border-radius: 10px; background: rgba(255, 255, 255, 0.82); border: 1px solid rgba(99, 102, 241, 0.14);
  backdrop-filter: blur(10px); font-size: 12px; color: var(--text-muted);
}
.hud-legend span { display: flex; align-items: center; gap: 6px; }
.hud-legend .dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
.hud-legend .dot.empty { background: #94a3b8; }
.hud-legend .dot.occupied { background: #3b82f6; }
.hud-legend .dot.charging { background: #10b981; }
.hud-legend .dot.buffer { background: #f59e0b; }
.hud-legend .dot.maintenance { background: #ef4444; }
.hud-legend .level-hint { color: var(--brand); font-weight: 600; border-left: 1px solid rgba(99, 102, 241, 0.2); padding-left: 12px; }

.view-tools { position: absolute; top: 92px; right: 18px; z-index: 10; display: flex; flex-direction: column; gap: 8px; }
.view-tools button {
  width: 38px; height: 38px; border-radius: 10px; background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(99, 102, 241, 0.18); color: var(--brand); display: grid; place-items: center; font-size: 14px;
  cursor: pointer; backdrop-filter: blur(10px); box-shadow: 0 4px 12px -6px rgba(15, 23, 42, 0.2); transition: all 0.18s ease;
}
.view-tools button:hover { color: #fff; background: var(--brand); border-color: var(--brand); transform: translateY(-1px); }
.view-tools button.active { color: #fff; background: var(--brand); border-color: var(--brand); }

.hud-hint {
  position: absolute; bottom: 16px; right: 18px; z-index: 10; font-size: 11px; color: var(--text-muted);
  background: rgba(255, 255, 255, 0.78); border: 1px solid rgba(99, 102, 241, 0.12); border-radius: 8px; padding: 6px 10px; backdrop-filter: blur(8px);
}

.twin-init-error { position: absolute; inset: auto 18px 60px 18px; z-index: 12; padding: 12px 16px; border-radius: 10px; background: rgba(220, 38, 38, 0.92); color: #fff; font-size: 13px; }
.twin-emergency-banner {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); z-index: 12; padding: 12px 26px; border-radius: 12px;
  background: rgba(239, 68, 68, 0.92); color: #fff; font-family: "Orbitron", sans-serif; letter-spacing: 1px;
  box-shadow: 0 0 40px rgba(239, 68, 68, 0.5); animation: twinPulse 1s ease-in-out infinite;
}
@keyframes twinPulse { 0%, 100% { opacity: 0.92; } 50% { opacity: 0.55; } }

.twin-demo-card .section-head.compact { align-items: center; justify-content: space-between; }
.demo-state { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 999px; font-size: 12px; color: var(--text-muted); background: rgba(148, 163, 184, 0.16); }
.demo-state.live { color: #fff; background: var(--brand); }
.demo-summary { margin: 14px 0 0; color: var(--text-muted); font-size: 13px; line-height: 1.55; }
.demo-progress { margin-top: 12px; height: 6px; border-radius: 999px; background: rgba(148, 163, 184, 0.16); overflow: hidden; }
.demo-progress span { display: block; height: 100%; border-radius: inherit; transition: width 0.4s ease; }
.demo-steps { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 12px; }
.demo-step-chip {
  min-height: 42px; padding: 8px 10px; border: 1px solid rgba(99, 102, 241, 0.12); border-radius: 10px;
  background: rgba(248, 250, 252, 0.9); color: var(--text-muted); font-size: 12px; text-align: left; transition: all 0.2s ease;
}
.demo-step-chip span {
  display: inline-grid; place-items: center; width: 18px; height: 18px; margin-right: 8px; border-radius: 999px;
  background: rgba(148, 163, 184, 0.16); color: var(--text-main); font-size: 11px; font-family: "Orbitron", sans-serif;
}
.demo-step-chip.active { background: rgba(79, 70, 229, 0.08); border-color: rgba(79, 70, 229, 0.24); color: var(--text-main); }
.demo-step-chip.active span { background: var(--brand); color: #fff; }
.demo-step-chip.done span { background: var(--safety-green); color: #fff; }
.demo-note { display: grid; grid-template-columns: 28px 1fr; gap: 10px; margin-top: 12px; padding: 12px; border-radius: 12px; background: rgba(241, 245, 249, 0.9); color: var(--text-muted); font-size: 12px; }
.demo-note i { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 10px; background: rgba(79, 70, 229, 0.1); color: var(--brand); }
.demo-note strong { display: block; color: var(--text-main); margin-bottom: 4px; }
.demo-note span { line-height: 1.45; }
.demo-switcher { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.demo-switcher button {
  height: 34px; padding: 0 12px; border-radius: 999px; border: 1px solid rgba(99, 102, 241, 0.14); background: rgba(255, 255, 255, 0.88);
  color: var(--text-muted); display: inline-flex; align-items: center; gap: 8px; font-size: 12px; cursor: pointer; transition: all 0.18s ease;
}
.demo-switcher button.active, .demo-switcher button:hover { background: rgba(79, 70, 229, 0.08); border-color: rgba(79, 70, 229, 0.24); color: var(--brand); }

.safety-card { display: flex; flex-direction: column; gap: 8px; padding: 16px; border-radius: 12px; background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.3); color: var(--text-muted); font-size: 13px; }
.safety-card strong { color: #059669; display: flex; align-items: center; gap: 8px; }
.safety-card.danger { background: rgba(239, 68, 68, 0.08); border-color: rgba(239, 68, 68, 0.35); }
.safety-card.danger strong { color: #dc2626; }

@media (max-width: 1100px) {
  .twin-layout { grid-template-columns: 1fr; }
  .twin-canvas { height: 520px; }
}
</style>
