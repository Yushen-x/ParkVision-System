/** Canonical vehicle energy types used across the app and backend. */
export const ENERGY_FUEL = "FUEL";
export const ENERGY_EV = "EV";

export function normalizeEnergyType(value, fallback = ENERGY_FUEL) {
  if (value === undefined || value === null || value === "") return fallback;
  const text = String(value).trim().toUpperCase();
  if (text === ENERGY_EV || text.includes("EV") || text.includes("ELECTRIC") || text.includes("电") || text.includes("新能源")) {
    return ENERGY_EV;
  }
  return ENERGY_FUEL;
}

export function isEvEnergyType(value) {
  return normalizeEnergyType(value, ENERGY_FUEL) === ENERGY_EV;
}

export function slotStatusForEnergy(energyType, plateNo = "") {
  if (isEvEnergyType(energyType)) return "charging";
  const plate = String(plateNo || "").toUpperCase();
  if (/[A-Z]-?D\d|D\d{3,}/.test(plate) || plate.includes("D5") || plate.includes("D3")) {
    return "charging";
  }
  return "occupied";
}
