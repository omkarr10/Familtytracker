import { create } from 'zustand'
import { Device, Location, Alert } from '../types/database'

interface DeviceWithLocation extends Device {
  latestLocation?: Location
}

interface AppState {
  devices: DeviceWithLocation[]
  selectedDevice: DeviceWithLocation | null
  alerts: Alert[]
  setDevices: (devices: DeviceWithLocation[]) => void
  setSelectedDevice: (device: DeviceWithLocation | null) => void
  updateDeviceLocation: (deviceId: string, location: Location) => void
  setAlerts: (alerts: Alert[]) => void
  addAlert: (alert: Alert) => void
  markAlertRead: (alertId: string) => void
}

export const useAppStore = create<AppState>((set) => ({
  devices: [],
  selectedDevice: null,
  alerts: [],
  
  setDevices: (devices) => set({ devices }),
  
  setSelectedDevice: (device) => set({ selectedDevice: device }),
  
  updateDeviceLocation: (deviceId, location) =>
    set((state) => ({
      devices: state.devices.map((d) =>
        d.id === deviceId
          ? { ...d, latestLocation: location, last_seen: location.created_at }
          : d
      ),
    })),
  
  setAlerts: (alerts) => set({ alerts }),
  
  addAlert: (alert) =>
    set((state) => ({
      alerts: [alert, ...state.alerts],
    })),
  
  markAlertRead: (alertId) =>
    set((state) => ({
      alerts: state.alerts.map((a) =>
        a.id === alertId ? { ...a, is_read: true } : a
      ),
    })),
}))
