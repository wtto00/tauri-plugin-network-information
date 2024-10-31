import { addPluginListener, invoke } from "@tauri-apps/api/core";

export enum Connection {
  UNKNOWN = 0,
  ETHERNET = 1,
  WIFI = 2,
  CELL_2G = 3,
  CELL_3G = 4,
  CELL_4G = 5,
  CELL_5G = 6,
  CELL = 7,
  NONE = 8,
}

export async function connection(): Promise<Connection> {
  return await invoke<Connection>("plugin:network-information|connection");
}
export async function isOnline(params?: { hostname?: string; port?: number }): Promise<boolean> {
  return await invoke<boolean>("plugin:network-information|is_online", { payload: params });
}

let _availableCallback: (available: boolean) => void;
function availableCallback(available: boolean) {
  _availableCallback?.(available);
}
export function onNetworkAvailable(callback: (available: boolean) => void = () => {}) {
  if (!_availableCallback) {
    addPluginListener("network-information", "available", availableCallback);
  }
  _availableCallback = callback;
}
