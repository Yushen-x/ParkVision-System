<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { state } from "../stores/parkingStore";
import { zhText } from "../utils/localize";

/* ------------------------------------------------------------------ *
 * Multi-level (立体) garage layout                                     *
 * 72 store slots -> 3 vertical levels x 24 bays (4 bay-columns x 6     *
 * rows around a central aisle). Levels map to the store's layer field: *
 * Shallow -> L1 (bottom), Mid -> L2, Deep -> L3 (top).                 *
 * ------------------------------------------------------------------ */
const LEVELS = [
  { key: "Shallow", label: "L1 浅层", y: 0 },
  { key: "Mid", label: "L2 中层", y: 6.4 },
  { key: "Deep", label: "L3 深层", y: 12.8 },
];
const LEVEL_H = 6.4;
const BAYS_PER_LEVEL = 24;
const BAY_COLS_X = [-7.6, -3.6, 3.6, 7.6]; // 2 left + 2 right of the aisle
const BAY_ROWS = 6;
const ROW_DZ = 4.4;
const HALF_Z = ((BAY_ROWS - 1) / 2) * ROW_DZ; // ~11
const FOOT_X = 9.8;
const FOOT_Z = 13.6;
const LIFT_Z = 16.6; // lift shaft sits in front of the racks

/* AGV motion — constant-speed glide along the aisle rail only */
const AGV_SPEED = 9;
const AGV_MIN_SEP = 4.6;
const AGV_LEVEL_MAP = [0, 1, 2, 0]; // stable per-AGV level assignment

/* status -> accent colour, tuned for the light scene */
const STATUS_COLOR = {
  empty: 0x94a3b8,
  occupied: 0x3b82f6,
  buffer: 0xf59e0b,
  charging: 0x10b981,
  maintenance: 0xef4444,
};
const CAR_COLORS = [0xe2474c, 0x3b82f6, 0x64748b, 0xeab308, 0x8b5cf6, 0x14b8a6];

/* ------------------------------------------------------------------ *
 * Reactive UI bits (HUD overlay)                                      *
 * ------------------------------------------------------------------ */
const canvasHost = ref(null);
const azimuthDeg = ref(45);
const autoRotate = ref(false);
const initError = ref("");
const liftLevel = ref(1);

const freeSlots = computed(() => state.slots.filter((s) => s.status === "empty").length);
const occupiedSlots = computed(() => state.slots.filter((s) => s.status !== "empty").length);
const chargingSlots = computed(() => state.slots.filter((s) => s.status === "charging").length);
const safetyGate = computed(
  () => state.devices.gates.find((g) => g.gateId.includes("OUT")) || null,
);

/* ------------------------------------------------------------------ *
 * Three.js handles (non-reactive on purpose)                          *
 * ------------------------------------------------------------------ */
let renderer, scene, camera, controls, clock, frameId, resizeObserver;
let accentLightA, accentLightB, ambient;
let sweep; // scanning laser plane on the ground level
let liftPlatform; // moving elevator deck
const lidars = []; // spinning sensor heads
const pulseLeds = []; // AGV emissive strips
const slotContent = new Map(); // index -> { group, status }
const slotPads = new Map(); // index -> border material
const agvObjects = new Map(); // id -> { group, ring, ringMat, level, target, sepZ }
let lastStatusKey = "";
let emergencyMix = 0;

const DEFAULT_CAM = new THREE.Vector3(42, 33, 48);
const DEFAULT_TARGET = new THREE.Vector3(0, 7, 0);
let tween = null;

/* ------------------------------------------------------------------ *
 * Small helpers                                                       *
 * ------------------------------------------------------------------ */
function clamp(v, a, b) {
  return Math.min(Math.max(v, a), b);
}

function bayWorld(level, localIdx) {
  const col = localIdx % 4;
  const row = Math.floor(localIdx / 4); // 0..5
  return {
    x: BAY_COLS_X[col],
    y: LEVELS[level].y,
    z: (row - (BAY_ROWS - 1) / 2) * ROW_DZ,
    right: col >= 2,
  };
}

function makeLabel(text, { color = "#1e293b", weight = 700, px = 46 } = {}) {
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
  ctx.fillStyle = "rgba(255, 255, 255, 0.86)";
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
function makeCar(colorHex, { ev = false } = {}) {
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
    new THREE.MeshStandardMaterial({
      color: 0x9fc3e6,
      metalness: 0.1,
      roughness: 0.08,
      transparent: true,
      opacity: 0.62,
    }),
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
  for (const [wx, wz] of [
    [-1.15, 1.35],
    [1.15, 1.35],
    [-1.15, -1.35],
    [1.15, -1.35],
  ]) {
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

  if (ev) {
    const port = new THREE.Mesh(
      new THREE.CircleGeometry(0.18, 16),
      new THREE.MeshStandardMaterial({ color: 0x10b981, emissive: 0x10b981, emissiveIntensity: 1.2 }),
    );
    port.position.set(1.21, 0.95, -1.0);
    port.rotation.y = Math.PI / 2;
    g.add(port);
  }

  g.scale.setScalar(0.78);
  return g;
}

function makeChargingStation() {
  const g = new THREE.Group();
  const base = new THREE.Mesh(
    new THREE.BoxGeometry(0.9, 0.25, 0.9),
    new THREE.MeshStandardMaterial({ color: 0x475569, roughness: 0.8 }),
  );
  base.position.y = 0.12;
  g.add(base);

  const pillar = new THREE.Mesh(
    new THREE.BoxGeometry(0.7, 2.0, 0.5),
    new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.4, roughness: 0.5 }),
  );
  pillar.position.y = 1.1;
  pillar.castShadow = true;
  g.add(pillar);

  const screen = new THREE.Mesh(
    new THREE.BoxGeometry(0.5, 0.6, 0.06),
    new THREE.MeshStandardMaterial({ color: 0x10b981, emissive: 0x10b981, emissiveIntensity: 1.4 }),
  );
  screen.position.set(0, 1.45, 0.27);
  g.add(screen);

  const top = new THREE.Mesh(
    new THREE.SphereGeometry(0.16, 16, 16),
    new THREE.MeshStandardMaterial({ color: 0x6ee7b7, emissive: 0x10b981, emissiveIntensity: 1.6 }),
  );
  top.position.y = 2.2;
  g.add(top);

  g.position.set(-1.35, 0, -1.35);
  return g;
}

function makeCone() {
  const g = new THREE.Group();
  const cone = new THREE.Mesh(
    new THREE.ConeGeometry(0.55, 1.5, 20),
    new THREE.MeshStandardMaterial({ color: 0xff7a1a, emissive: 0xff5500, emissiveIntensity: 0.35, roughness: 0.6 }),
  );
  cone.position.y = 0.85;
  cone.castShadow = true;
  const foot = new THREE.Mesh(
    new THREE.BoxGeometry(1.1, 0.12, 1.1),
    new THREE.MeshStandardMaterial({ color: 0x475569, roughness: 0.9 }),
  );
  foot.position.y = 0.06;
  g.add(foot, cone);
  return g;
}

/* Animated charge indicator: a fill bar + pulsing ground ring + label */
function makeChargeIndicator() {
  const g = new THREE.Group();

  const track = new THREE.Mesh(
    new THREE.BoxGeometry(0.26, 1.4, 0.12),
    new THREE.MeshStandardMaterial({ color: 0xcbd5e1, roughness: 0.85 }),
  );
  track.position.set(1.2, 1.7, 1.2);
  g.add(track);

  const fill = new THREE.Mesh(
    new THREE.BoxGeometry(0.3, 1.4, 0.16),
    new THREE.MeshStandardMaterial({ color: 0x10b981, emissive: 0x10b981, emissiveIntensity: 1.0 }),
  );
  fill.position.set(1.2, 1.0, 1.2);
  fill.scale.y = 0.02;
  g.add(fill);

  const ring = new THREE.Mesh(
    new THREE.TorusGeometry(1.5, 0.08, 10, 36),
    new THREE.MeshBasicMaterial({ color: 0x10b981, transparent: true, opacity: 0.6 }),
  );
  ring.rotation.x = -Math.PI / 2;
  ring.position.y = 0.08;
  g.add(ring);

  const label = makeLabel("充电中", { color: "#059669", px: 36 });
  label.position.set(0, 3.0, 0);
  label.scale.set(2.7, 1.35, 1);
  g.add(label);

  g.userData.charge = { fill, ring, phase: Math.random() };
  return g;
}

function makeAgv(id) {
  const g = new THREE.Group();

  const platform = new THREE.Mesh(
    new THREE.BoxGeometry(2.6, 0.5, 3.4),
    new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.6, roughness: 0.35 }),
  );
  platform.position.y = 0.4;
  platform.castShadow = true;
  g.add(platform);

  const deck = new THREE.Mesh(
    new THREE.BoxGeometry(2.3, 0.16, 3.0),
    new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.5, roughness: 0.45 }),
  );
  deck.position.y = 0.7;
  g.add(deck);

  const ledMat = new THREE.MeshStandardMaterial({ color: 0x38bdf8, emissive: 0x38bdf8, emissiveIntensity: 1.0 });
  pulseLeds.push(ledMat);
  for (const sx of [-1.2, 1.2]) {
    const strip = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.3, 2.9), ledMat);
    strip.position.set(sx, 0.42, 0);
    g.add(strip);
  }

  const wMat = new THREE.MeshStandardMaterial({ color: 0x1e293b, roughness: 0.9 });
  const wGeo = new THREE.CylinderGeometry(0.3, 0.3, 0.28, 14);
  for (const [wx, wz] of [
    [-1.15, 1.25],
    [1.15, 1.25],
    [-1.15, -1.25],
    [1.15, -1.25],
  ]) {
    const w = new THREE.Mesh(wGeo, wMat);
    w.rotation.z = Math.PI / 2;
    w.position.set(wx, 0.3, wz);
    g.add(w);
  }

  const tower = new THREE.Mesh(
    new THREE.CylinderGeometry(0.2, 0.24, 0.5, 16),
    new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.5, roughness: 0.4 }),
  );
  tower.position.y = 1.0;
  g.add(tower);
  const head = new THREE.Mesh(
    new THREE.CylinderGeometry(0.25, 0.25, 0.22, 18),
    new THREE.MeshStandardMaterial({ color: 0x67e8f9, emissive: 0x22d3ee, emissiveIntensity: 1.2 }),
  );
  head.position.y = 1.3;
  g.add(head);
  lidars.push(head);

  const ringMat = new THREE.MeshStandardMaterial({ color: 0x38bdf8, emissive: 0x38bdf8, emissiveIntensity: 1.1 });
  const ring = new THREE.Mesh(new THREE.TorusGeometry(1.8, 0.08, 10, 36), ringMat);
  ring.rotation.x = Math.PI / 2;
  ring.position.y = 0.12;
  g.add(ring);

  const label = makeLabel(id.replace("AGV-", "#"), { color: "#1e293b", px: 56 });
  label.position.set(0, 2.3, 0);
  label.scale.set(3.0, 1.5, 1);
  g.add(label);

  return { group: g, ring, ringMat };
}

function applyAgvMode(entry, mode, loaded) {
  let c = 0x38bdf8;
  if (loaded) c = 0xf59e0b;
  else if (String(mode || "").toUpperCase() === "CHARGING") c = 0x10b981;
  else if (String(mode || "").toUpperCase() === "TRANSIT") c = 0x818cf8;
  entry.ringMat.color.setHex(c);
  entry.ringMat.emissive.setHex(c);
  const hasCargo = entry.group.getObjectByName("cargo");
  if (loaded && !hasCargo) {
    const car = makeCar(0xcbd5e1);
    car.name = "cargo";
    car.scale.setScalar(0.56);
    car.position.y = 0.78;
    entry.group.add(car);
  } else if (!loaded && hasCargo) {
    entry.group.remove(hasCargo);
  }
}

/* ------------------------------------------------------------------ *
 * Slot content build                                                  *
 * ------------------------------------------------------------------ */
function buildSlotContent(status, index) {
  const g = new THREE.Group();
  if (status === "occupied" || status === "buffer") {
    g.add(makeCar(CAR_COLORS[index % CAR_COLORS.length]));
  } else if (status === "charging") {
    g.add(makeChargingStation());
    g.add(makeCar(0x34d399, { ev: true }));
    const indicator = makeChargeIndicator();
    g.add(indicator);
    g.userData.charge = indicator.userData.charge;
  } else if (status === "maintenance") {
    g.add(makeCone());
  }
  return g;
}

/* ------------------------------------------------------------------ *
 * Scene assembly                                                      *
 * ------------------------------------------------------------------ */
function makeRackFrame() {
  const group = new THREE.Group();
  const postMat = new THREE.MeshStandardMaterial({ color: 0xaab4c6, metalness: 0.55, roughness: 0.45 });
  const beamMat = new THREE.LineBasicMaterial({ color: 0x6366f1, transparent: true, opacity: 0.34 });
  const topY = LEVELS[LEVELS.length - 1].y;

  // 4 corner posts spanning all levels
  for (const [px, pz] of [
    [-FOOT_X, -FOOT_Z],
    [FOOT_X, -FOOT_Z],
    [-FOOT_X, FOOT_Z],
    [FOOT_X, FOOT_Z],
  ]) {
    const post = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.16, topY + 4.2, 12), postMat);
    post.position.set(px, (topY + 4.2) / 2 - 0.6, pz);
    group.add(post);
  }

  // per-level open frame + aisle rail + level label
  LEVELS.forEach((lvl, li) => {
    const ring = new THREE.LineSegments(
      new THREE.EdgesGeometry(new THREE.BoxGeometry(FOOT_X * 2, 0.18, FOOT_Z * 2)),
      beamMat,
    );
    ring.position.set(0, lvl.y - 0.18, 0);
    group.add(ring);

    // aisle rail strip the AGV rides on
    const rail = new THREE.Mesh(
      new THREE.BoxGeometry(2.8, 0.08, FOOT_Z * 2 - 1),
      new THREE.MeshStandardMaterial({
        color: 0xd7dee9,
        emissive: 0x6366f1,
        emissiveIntensity: 0.08,
        roughness: 0.9,
      }),
    );
    rail.position.set(0, lvl.y - 0.05, 0);
    rail.receiveShadow = true;
    group.add(rail);

    const label = makeLabel(lvl.label, { color: "#4f46e5", px: 40 });
    label.position.set(-FOOT_X - 0.6, lvl.y + 1.4, -FOOT_Z + 1.2);
    label.scale.set(4.6, 2.3, 1);
    group.add(label);
  });

  return group;
}

function makeLift() {
  const g = new THREE.Group();
  const topY = LEVELS[LEVELS.length - 1].y;
  const railMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.65, roughness: 0.35 });
  const shaftH = topY + 4.6;

  for (const [rx, rz] of [
    [-2.0, -2.0],
    [2.0, -2.0],
    [-2.0, 2.0],
    [2.0, 2.0],
  ]) {
    const rail = new THREE.Mesh(new THREE.CylinderGeometry(0.14, 0.14, shaftH, 12), railMat);
    rail.position.set(rx, shaftH / 2 - 0.6, rz);
    g.add(rail);
  }

  // level stop markers
  LEVELS.forEach((lvl) => {
    const mk = new THREE.Mesh(
      new THREE.TorusGeometry(2.3, 0.06, 8, 28),
      new THREE.MeshBasicMaterial({ color: 0x818cf8, transparent: true, opacity: 0.5 }),
    );
    mk.rotation.x = Math.PI / 2;
    mk.position.y = lvl.y - 0.1;
    g.add(mk);
  });

  // moving platform with a car
  liftPlatform = new THREE.Group();
  const deck = new THREE.Mesh(
    new THREE.BoxGeometry(3.6, 0.26, 4.0),
    new THREE.MeshStandardMaterial({ color: 0x6366f1, metalness: 0.5, roughness: 0.4, emissive: 0x6366f1, emissiveIntensity: 0.25 }),
  );
  deck.castShadow = true;
  liftPlatform.add(deck);
  const liftCar = makeCar(0x3b82f6);
  liftCar.scale.setScalar(0.7);
  liftCar.position.y = 0.5;
  liftPlatform.add(liftCar);
  liftPlatform.position.y = 0;
  g.add(liftPlatform);

  const label = makeLabel("升降梯 LIFT", { color: "#4f46e5", px: 34 });
  label.position.set(0, topY + 4.4, 0);
  label.scale.set(5, 2.2, 1);
  g.add(label);

  g.position.set(0, 0, LIFT_Z);
  return g;
}

function buildStaticScene() {
  // ground plane
  const ground = new THREE.Mesh(
    new THREE.PlaneGeometry(FOOT_X * 2 + 30, FOOT_Z * 2 + 30),
    new THREE.MeshStandardMaterial({ color: 0xdfe6f1, metalness: 0.0, roughness: 0.96 }),
  );
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -0.6;
  ground.receiveShadow = true;
  scene.add(ground);

  const grid = new THREE.GridHelper(FOOT_X * 2 + 26, 26, 0x9aa6bb, 0xc6cfde);
  grid.position.y = -0.58;
  grid.material.transparent = true;
  grid.material.opacity = 0.45;
  scene.add(grid);

  scene.add(makeRackFrame());
  scene.add(makeLift());

  // bay pads (thin) + status border, for every level/bay
  for (let level = 0; level < LEVELS.length; level++) {
    for (let local = 0; local < BAYS_PER_LEVEL; local++) {
      const globalIdx = level * BAYS_PER_LEVEL + local;
      const { x, y, z } = bayWorld(level, local);

      const pad = new THREE.Mesh(
        new THREE.BoxGeometry(3.4, 0.12, 3.9),
        new THREE.MeshStandardMaterial({ color: 0xeaf0f8, metalness: 0.05, roughness: 0.9 }),
      );
      pad.position.set(x, y - 0.06, z);
      pad.receiveShadow = true;
      scene.add(pad);

      const borderMat = new THREE.LineBasicMaterial({ color: STATUS_COLOR.empty, transparent: true, opacity: 0.9 });
      const border = new THREE.LineSegments(
        new THREE.EdgesGeometry(new THREE.PlaneGeometry(3.4, 3.9)),
        borderMat,
      );
      border.rotation.x = -Math.PI / 2;
      border.position.set(x, y + 0.02, z);
      scene.add(border);
      slotPads.set(globalIdx, borderMat);
    }
  }

  // scanning laser sweep on the ground level
  sweep = new THREE.Mesh(
    new THREE.PlaneGeometry(FOOT_X * 2, 1.2),
    new THREE.MeshBasicMaterial({
      color: 0x6366f1,
      transparent: true,
      opacity: 0.16,
      side: THREE.DoubleSide,
      depthWrite: false,
    }),
  );
  sweep.rotation.x = -Math.PI / 2;
  sweep.position.y = 0.06;
  scene.add(sweep);
}

function setupLights() {
  scene.add(new THREE.HemisphereLight(0xffffff, 0xdbe3f0, 1.0));
  ambient = new THREE.AmbientLight(0xffffff, 0.42);
  scene.add(ambient);

  const dir = new THREE.DirectionalLight(0xffffff, 2.3);
  dir.position.set(36, 60, 30);
  dir.castShadow = true;
  dir.shadow.mapSize.set(2048, 2048);
  dir.shadow.camera.near = 5;
  dir.shadow.camera.far = 200;
  dir.shadow.camera.left = -FOOT_X - 14;
  dir.shadow.camera.right = FOOT_X + 14;
  dir.shadow.camera.top = FOOT_Z + 22;
  dir.shadow.camera.bottom = -FOOT_Z - 22;
  dir.shadow.bias = -0.0004;
  scene.add(dir);

  const fill = new THREE.DirectionalLight(0xe3ebf8, 0.7);
  fill.position.set(-30, 34, -26);
  scene.add(fill);

  accentLightA = new THREE.PointLight(0xbcd0ff, 0.25, 160, 1.6);
  accentLightA.position.set(-26, 22, -26);
  scene.add(accentLightA);
  accentLightB = new THREE.PointLight(0xc7d2fe, 0.2, 160, 1.6);
  accentLightB.position.set(28, 20, 26);
  scene.add(accentLightB);
}

/* ------------------------------------------------------------------ *
 * Data sync                                                           *
 * ------------------------------------------------------------------ */
function syncSlots() {
  const key = state.slots.map((s) => s.status).join("|");
  if (key === lastStatusKey) return;
  lastStatusKey = key;

  state.slots.forEach((slot, idx) => {
    if (idx >= LEVELS.length * BAYS_PER_LEVEL) return;
    const status = slot.status || "empty";
    const level = Math.floor(idx / BAYS_PER_LEVEL);
    const local = idx % BAYS_PER_LEVEL;
    const bay = bayWorld(level, local);

    const mat = slotPads.get(idx);
    if (mat) mat.color.setHex(STATUS_COLOR[status] ?? STATUS_COLOR.empty);

    const prev = slotContent.get(idx);
    if (prev && prev.status === status) return;
    if (prev) {
      scene.remove(prev.group);
      disposeGroup(prev.group);
    }

    const content = buildSlotContent(status, idx);
    if (content.children.length) {
      content.position.set(bay.x, bay.y, bay.z);
      content.rotation.y = bay.right ? -Math.PI / 2 : Math.PI / 2;
      content.userData.spawn = 0;
      content.scale.setScalar(0.01);
      scene.add(content);
    }
    slotContent.set(idx, { group: content, status });
  });
}

function snapAgvZ(agv) {
  const rawY = clamp(Number(agv.y ?? 0), 0, 100);
  return clamp((rawY / 100 - 0.5) * (2 * HALF_Z), -HALF_Z, HALF_Z);
}

function syncAgvs() {
  state.agvs.forEach((agv, idx) => {
    let entry = agvObjects.get(agv.id);
    if (!entry) {
      entry = makeAgv(agv.id);
      entry.level = AGV_LEVEL_MAP[idx % AGV_LEVEL_MAP.length];
      const z = snapAgvZ(agv);
      entry.group.position.set(0, LEVELS[entry.level].y, z);
      entry.target = new THREE.Vector3(0, LEVELS[entry.level].y, z);
      entry.sepZ = z;
      scene.add(entry.group);
      agvObjects.set(agv.id, entry);
    }
    entry.target.set(0, LEVELS[entry.level].y, snapAgvZ(agv));
    applyAgvMode(entry, agv.mode, Boolean(agv.loaded ?? agv.load));
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
  syncAgvs();

  // slot spawn animation + charging indicators
  slotContent.forEach((entry) => {
    const g = entry.group;
    if (g.userData.spawn != null && g.userData.spawn < 1) {
      g.userData.spawn = Math.min(g.userData.spawn + dt * 2.4, 1);
      const s = g.userData.spawn;
      g.scale.setScalar(0.01 + (1 - 0.01) * (1 - Math.pow(1 - s, 3)));
    }
    if (g.userData.charge) {
      const ch = g.userData.charge;
      ch.phase = (ch.phase + dt / 4) % 1; // ~4s charge cycle
      const frac = Math.max(0.02, ch.phase);
      ch.fill.scale.y = frac;
      ch.fill.position.y = 1.0 + 0.7 * frac;
      ch.ring.material.opacity = 0.45 + 0.35 * (0.5 + 0.5 * Math.sin(t * 5));
      ch.ring.scale.setScalar(1 + 0.04 * Math.sin(t * 5));
    }
  });

  // AGV motion — separate within each level along z, then glide (aisle only)
  const byLevel = [[], [], []];
  agvObjects.forEach((e) => {
    e.sepZ = e.target.z;
    byLevel[e.level].push(e);
  });
  byLevel.forEach((list) => {
    for (let pass = 0; pass < 4; pass++) {
      for (let i = 0; i < list.length; i++) {
        for (let j = i + 1; j < list.length; j++) {
          const a = list[i];
          const b = list[j];
          const d = b.sepZ - a.sepZ;
          const ad = Math.abs(d);
          if (ad < AGV_MIN_SEP) {
            const push = (AGV_MIN_SEP - ad) / 2 || 0.4;
            const dir = d >= 0 ? 1 : -1;
            a.sepZ = clamp(a.sepZ - dir * push, -HALF_Z, HALF_Z);
            b.sepZ = clamp(b.sepZ + dir * push, -HALF_Z, HALF_Z);
          }
        }
      }
    }
  });
  agvObjects.forEach((entry) => {
    const g = entry.group;
    const ty = entry.target.y;
    g.position.y += (ty - g.position.y) * Math.min(dt * 4, 1);
    const dz = entry.sepZ - g.position.z;
    const dist = Math.abs(dz);
    if (dist > 0.002) {
      const step = Math.min(AGV_SPEED * dt, dist);
      g.position.z += Math.sign(dz) * step;
      const targetYaw = dz >= 0 ? 0 : Math.PI;
      let diff = targetYaw - g.rotation.y;
      while (diff > Math.PI) diff -= Math.PI * 2;
      while (diff < -Math.PI) diff += Math.PI * 2;
      g.rotation.y += diff * Math.min(dt * 5, 1);
    }
    entry.ring.scale.setScalar(1 + Math.sin(t * 3) * 0.04);
  });

  // lift platform travels up/down through the levels
  if (liftPlatform) {
    const topY = LEVELS[LEVELS.length - 1].y;
    const cycle = (Math.sin(t * 0.5) * 0.5 + 0.5); // 0..1 smooth
    liftPlatform.position.y = cycle * topY;
    const lvl = Math.round(liftPlatform.position.y / LEVEL_H) + 1;
    if (lvl !== liftLevel.value) liftLevel.value = clamp(lvl, 1, LEVELS.length);
  }

  lidars.forEach((h) => (h.rotation.y += dt * 4.5));
  const pulse = 0.8 + Math.sin(t * 4) * 0.6;
  pulseLeds.forEach((m) => (m.emissiveIntensity = pulse));

  if (sweep) {
    sweep.position.z = ((t * 8) % (FOOT_Z * 2 + 8)) - FOOT_Z - 4;
  }

  const targetMix = state.emergency ? 1 : 0;
  emergencyMix += (targetMix - emergencyMix) * Math.min(dt * 3, 1);
  if (emergencyMix > 0.001) {
    const flash = 0.5 + Math.sin(t * 8) * 0.5;
    ambient.color.setRGB(1, 1 - 0.5 * emergencyMix * flash, 1 - 0.55 * emergencyMix * flash);
    accentLightA.color.setRGB(1, 0.45 * (1 - emergencyMix), 0.45 * (1 - emergencyMix));
    sweep.material.color.setRGB(0.9, 0.2 + 0.3 * (1 - emergencyMix), 0.25 * (1 - emergencyMix));
  } else {
    ambient.color.setHex(0xffffff);
    accentLightA.color.setHex(0xbcd0ff);
    sweep.material.color.setHex(0x6366f1);
  }

  azimuthDeg.value = THREE.MathUtils.radToDeg(controls.getAzimuthalAngle());
  renderer.render(scene, camera);
}

/* ------------------------------------------------------------------ *
 * View controls (HUD buttons)                                         *
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
  tween = {
    from: camera.position.clone(),
    to: toPos.clone(),
    fromT: controls.target.clone(),
    toT: toTarget.clone(),
    t: 0,
  };
}
function resetView() {
  autoRotate.value = false;
  startTween(DEFAULT_CAM, DEFAULT_TARGET);
}
function topView() {
  autoRotate.value = false;
  startTween(new THREE.Vector3(0.01, 120, 0.01), new THREE.Vector3(0, 6, 0));
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
    scene.fog = new THREE.Fog(0xeaf0f8, 120, 300);

    camera = new THREE.PerspectiveCamera(46, 1, 0.1, 600);
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
    controls.minDistance = 24;
    controls.maxDistance = 180;
    controls.maxPolarAngle = THREE.MathUtils.degToRad(89);
    controls.autoRotateSpeed = 0.8;
    controls.update();

    setupLights();
    buildStaticScene();
    syncSlots();
    syncAgvs();

    clock = new THREE.Clock();
    onResize();
    animate();

    resizeObserver = new ResizeObserver(onResize);
    resizeObserver.observe(host);
    window.addEventListener("resize", onResize);
  } catch (err) {
    initError.value = String(err?.message || err);
    console.error("[TwinView] init failed", err);
  }
});

onBeforeUnmount(() => {
  cancelAnimationFrame(frameId);
  window.removeEventListener("resize", onResize);
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
  agvObjects.clear();
  lidars.length = 0;
  pulseLeds.length = 0;
  liftPlatform = null;
});
</script>

<template>
  <section class="twin-layout" :class="{ 'twin-layout-emergency': state.emergency }">
    <article class="surface wide twin-stage">
      <div ref="canvasHost" class="twin-canvas" :class="{ emergency: state.emergency }">
        <!-- HUD: live stats -->
        <div class="hud-card top-left">
          <div class="hud-title"><i class="fa-solid fa-cubes"></i> 立体车库孪生</div>
          <div class="hud-row"><span>车位空闲</span><strong class="ok">{{ freeSlots }}</strong></div>
          <div class="hud-row"><span>车位占用</span><strong>{{ occupiedSlots }}</strong></div>
          <div class="hud-row"><span>充电中</span><strong class="charge">{{ chargingSlots }}</strong></div>
          <div class="hud-row"><span>AGV 在线</span><strong>{{ state.agvs.length }} 台</strong></div>
          <div class="hud-row"><span>升降梯</span><strong class="lift">运行至 L{{ liftLevel }}</strong></div>
          <div class="hud-row">
            <span>系统状态</span>
            <strong :class="state.emergency ? 'danger' : 'ok'">
              {{ state.emergency ? "急停锁定" : "自动运行" }}
            </strong>
          </div>
        </div>

        <!-- HUD: compass -->
        <div class="hud-compass">
          <div class="compass-dial" :style="{ transform: `rotate(${-azimuthDeg}deg)` }">
            <span class="tick n">N</span>
            <span class="tick e">E</span>
            <span class="tick s">S</span>
            <span class="tick w">W</span>
          </div>
        </div>

        <!-- HUD: legend -->
        <div class="hud-legend">
          <span><i class="dot empty"></i>空闲</span>
          <span><i class="dot occupied"></i>占用</span>
          <span><i class="dot charging"></i>充电</span>
          <span><i class="dot buffer"></i>缓冲</span>
          <span><i class="dot maintenance"></i>维护</span>
          <span class="level-hint"><i class="fa-solid fa-layer-group"></i> 三层立体库 · 升降梯传送</span>
        </div>

        <!-- HUD: view controls -->
        <div class="view-tools">
          <button @click="zoomBy(0.82)" title="放大"><i class="fa-solid fa-magnifying-glass-plus"></i></button>
          <button @click="zoomBy(1.22)" title="缩小"><i class="fa-solid fa-magnifying-glass-minus"></i></button>
          <button @click="rotateAzimuth(-20)" title="向左旋转"><i class="fa-solid fa-rotate-left"></i></button>
          <button @click="rotateAzimuth(20)" title="向右旋转"><i class="fa-solid fa-rotate-right"></i></button>
          <button @click="topView" title="俯视视角"><i class="fa-solid fa-table-cells"></i></button>
          <button :class="{ active: autoRotate }" @click="toggleAutoRotate" title="自动环绕">
            <i class="fa-solid fa-arrows-spin"></i>
          </button>
          <button @click="resetView" title="重置视角"><i class="fa-solid fa-arrows-to-eye"></i></button>
        </div>

        <div class="hud-hint"><i class="fa-solid fa-arrows-up-down-left-right"></i> 拖拽旋转 · 滚轮缩放 · 右键平移</div>

        <div v-if="initError" class="twin-init-error">
          3D 场景初始化失败：{{ initError }}
        </div>
        <div v-if="state.emergency" class="twin-emergency-banner">
          <i class="fa-solid fa-triangle-exclamation"></i> 紧急停机已激活
        </div>
      </div>
    </article>

    <aside class="twin-side-panel">
      <article class="surface" style="flex: 1;">
        <div class="section-head compact">
          <h2>AGV 车队</h2>
        </div>
        <div class="agv-list" style="margin-top: 16px;">
          <div v-for="agv in state.agvs" :key="agv.id" class="agv-card">
            <b><i class="fa-solid fa-robot" style="color: var(--brand); margin-right: 8px;"></i>{{ agv.id }} | {{ zhText(agv.mode) }}</b>
            <span>坐标 [{{ Math.round(agv.x) }}, {{ Math.round(agv.y) }}] | {{ zhText(agv.task) }}</span>
            <span>电量 {{ agv.batteryPct }}% | 速度 {{ Number(agv.velocityMps || 0).toFixed(2) }} m/s | 指令 {{ zhText(agv.lastCommand) }}</span>
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
.twin-canvas.emergency {
  border-color: rgba(239, 68, 68, 0.45);
  box-shadow: inset 0 0 80px rgba(239, 68, 68, 0.12), 0 16px 40px -18px rgba(15, 23, 42, 0.2);
}
.twin-canvas :deep(canvas) { display: block; width: 100% !important; height: 100% !important; }

/* HUD cards — light frosted glass to match the rest of the app */
.hud-card {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 10;
  min-width: 196px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(99, 102, 241, 0.16);
  box-shadow: 0 10px 30px -14px rgba(15, 23, 42, 0.25);
  color: var(--text-main);
  font-size: 13px;
}
.hud-title {
  font-family: "Orbitron", sans-serif;
  font-size: 13px;
  letter-spacing: 0.5px;
  color: var(--brand);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.hud-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 0;
}
.hud-row span { color: var(--text-muted); }
.hud-row strong { font-family: "Orbitron", sans-serif; color: var(--text-main); }
.hud-row strong.ok { color: var(--safety-green); }
.hud-row strong.charge { color: #059669; }
.hud-row strong.lift { color: var(--brand); font-size: 12px; }
.hud-row strong.danger { color: var(--danger-red); }

/* compass */
.hud-compass {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 10;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(99, 102, 241, 0.18);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}
.compass-dial { position: absolute; inset: 0; }
.compass-dial .tick {
  position: absolute;
  font-family: "Orbitron", sans-serif;
  font-size: 11px;
  color: var(--text-muted);
  left: 50%;
  transform: translateX(-50%);
}
.compass-dial .tick.n { top: 5px; color: var(--danger-red); font-weight: 700; }
.compass-dial .tick.s { bottom: 5px; }
.compass-dial .tick.e { right: 6px; top: 50%; left: auto; transform: translateY(-50%); }
.compass-dial .tick.w { left: 6px; top: 50%; transform: translateY(-50%); }

/* legend */
.hud-legend {
  position: absolute;
  bottom: 16px;
  left: 18px;
  z-index: 10;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(99, 102, 241, 0.14);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  font-size: 12px;
  color: var(--text-muted);
}
.hud-legend span { display: flex; align-items: center; gap: 6px; }
.hud-legend .dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
.hud-legend .dot.empty { background: #94a3b8; }
.hud-legend .dot.occupied { background: #3b82f6; }
.hud-legend .dot.charging { background: #10b981; }
.hud-legend .dot.buffer { background: #f59e0b; }
.hud-legend .dot.maintenance { background: #ef4444; }
.hud-legend .level-hint { color: var(--brand); font-weight: 600; border-left: 1px solid rgba(99,102,241,0.2); padding-left: 12px; }

/* view tools */
.view-tools {
  position: absolute;
  top: 92px;
  right: 18px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.view-tools button {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(99, 102, 241, 0.18);
  color: var(--brand);
  display: grid;
  place-items: center;
  font-size: 14px;
  cursor: pointer;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px -6px rgba(15, 23, 42, 0.2);
  transition: all 0.18s ease;
}
.view-tools button:hover {
  color: #fff;
  background: var(--brand);
  border-color: var(--brand);
  box-shadow: 0 6px 16px -6px rgba(79, 70, 229, 0.5);
  transform: translateY(-1px);
}
.view-tools button.active {
  color: #fff;
  background: var(--brand);
  border-color: var(--brand);
}

.hud-hint {
  position: absolute;
  bottom: 16px;
  right: 18px;
  z-index: 10;
  font-size: 11px;
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 8px;
  padding: 6px 10px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.twin-init-error {
  position: absolute;
  inset: auto 18px 60px 18px;
  z-index: 12;
  padding: 12px 16px;
  border-radius: 10px;
  background: rgba(220, 38, 38, 0.92);
  color: #fff;
  font-size: 13px;
}
.twin-emergency-banner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 12;
  padding: 12px 26px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.92);
  color: #fff;
  font-family: "Orbitron", sans-serif;
  letter-spacing: 1px;
  box-shadow: 0 0 40px rgba(239, 68, 68, 0.5);
  animation: twinPulse 1s ease-in-out infinite;
}
@keyframes twinPulse {
  0%, 100% { opacity: 0.92; }
  50% { opacity: 0.55; }
}

.agv-list { display: flex; flex-direction: column; gap: 12px; }
.agv-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--panel-2);
  border: 1px solid var(--line);
  font-size: 12px;
  color: var(--text-muted);
}
.agv-card b { color: var(--text-main); font-size: 13px; }
.safety-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: var(--text-muted);
  font-size: 13px;
}
.safety-card strong { color: #059669; display: flex; align-items: center; gap: 8px; }
.safety-card.danger { background: rgba(239, 68, 68, 0.08); border-color: rgba(239, 68, 68, 0.35); }
.safety-card.danger strong { color: #dc2626; }

@media (max-width: 1100px) {
  .twin-layout { grid-template-columns: 1fr; }
  .twin-canvas { height: 520px; }
}
</style>
