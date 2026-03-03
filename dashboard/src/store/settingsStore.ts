import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type MapTheme = 'standard' | 'satellite' | 'terrain' | 'dark'

export const mapTileLayers: Record<MapTheme, { url: string; attribution: string }> = {
  standard: {
    url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  },
  satellite: {
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    attribution: '&copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping',
  },
  terrain: {
    url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://opentopomap.org">OpenTopoMap</a>',
  },
  dark: {
    url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
  },
}

interface SettingsState {
  mapTheme: MapTheme
  soundAlerts: boolean
  batteryAlertThreshold: number
  speedAlertEnabled: boolean
  speedAlertThreshold: number // km/h
  setMapTheme: (theme: MapTheme) => void
  setSoundAlerts: (enabled: boolean) => void
  setBatteryAlertThreshold: (threshold: number) => void
  setSpeedAlert: (enabled: boolean, threshold?: number) => void
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      mapTheme: 'standard',
      soundAlerts: true,
      batteryAlertThreshold: 20,
      speedAlertEnabled: false,
      speedAlertThreshold: 80,

      setMapTheme: (mapTheme) => set({ mapTheme }),
      setSoundAlerts: (soundAlerts) => set({ soundAlerts }),
      setBatteryAlertThreshold: (batteryAlertThreshold) => set({ batteryAlertThreshold }),
      setSpeedAlert: (enabled, threshold) =>
        set((state) => ({
          speedAlertEnabled: enabled,
          speedAlertThreshold: threshold ?? state.speedAlertThreshold,
        })),
    }),
    {
      name: 'ft-settings',
    }
  )
)
