import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import { Icon, LatLngBounds } from 'leaflet'
import { api } from '../lib/api'
import { useAppStore } from '../store/appStore'
import { useAuthStore } from '../store/authStore'
import { Device, Location } from '../types/database'
import { formatDistanceToNow } from 'date-fns'
import {
  Battery,
  BatteryLow,
  BatteryMedium,
  BatteryFull,
  Wifi,
  WifiOff,
  Navigation,
  RefreshCw,
  AlertCircle,
} from 'lucide-react'
import clsx from 'clsx'
import { SkeletonDeviceCard, SkeletonMap } from '../components/Skeleton'

// Custom marker icon
const createMarkerIcon = (color: string, isOnline: boolean) =>
  new Icon({
    iconUrl: `data:image/svg+xml;base64,${btoa(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${color}" width="36" height="36">
        <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
        ${isOnline ? '<circle cx="18" cy="6" r="4" fill="#22c55e"/>' : ''}
      </svg>
    `)}`,
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36],
  })

const deviceColors = [
  '#3b82f6', // blue
  '#ef4444', // red
  '#22c55e', // green
  '#f59e0b', // amber
  '#8b5cf6', // violet
  '#ec4899', // pink
]

interface DeviceWithLocation extends Device {
  latestLocation?: Location
}

// Auto-fit map to markers
function MapBounds({ devices }: { devices: DeviceWithLocation[] }) {
  const map = useMap()
  
  useEffect(() => {
    const validDevices = devices.filter((d) => d.latestLocation)
    if (validDevices.length === 0) return
    
    if (validDevices.length === 1) {
      const loc = validDevices[0].latestLocation!
      map.setView([loc.latitude, loc.longitude], 15)
    } else {
      const bounds = new LatLngBounds(
        validDevices.map((d) => [d.latestLocation!.latitude, d.latestLocation!.longitude])
      )
      map.fitBounds(bounds, { padding: [50, 50] })
    }
  }, [devices, map])
  
  return null
}

export default function Dashboard() {
  const { user } = useAuthStore()
  const { devices, setDevices, selectedDevice, setSelectedDevice, updateDeviceLocation } = useAppStore()
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mapCenter] = useState<[number, number]>([20.5937, 78.9629]) // India center

  const fetchDevices = async () => {
    if (!user) {
      setError('No user logged in')
      setLoading(false)
      return
    }
    
    setError(null)
    
    try {
      console.log('Fetching devices for user:', user.id)
      const { data: devicesData } = await api.from('devices').select('*', {
        eq: ['user_id', user.id]
      })

      console.log('Devices response:', devicesData)

      if (!devicesData) {
        setLoading(false)
        return
      }

      // Get latest location for each device
      const devicesWithLocations: DeviceWithLocation[] = await Promise.all(
        (devicesData || []).map(async (device: Device) => {
          try {
            const { data: locationData } = await api.from('locations').select('*', {
              eq: ['device_id', device.id],
              order: ['created_at', false],
              limit: 1
            })
            return {
              ...device,
              latestLocation: locationData?.[0] || undefined,
            }
          } catch {
            return { ...device, latestLocation: undefined }
          }
        })
      )

      setDevices(devicesWithLocations)
    } catch (err: any) {
      console.error('Error fetching devices:', err)
      setError(err.message || 'Failed to fetch devices')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  // Fetch devices and their latest locations
  useEffect(() => {
    fetchDevices()
    
    // Poll for updates every 30 seconds (since realtime doesn't work through proxy)
    const interval = setInterval(fetchDevices, 30000)
    return () => clearInterval(interval)
  }, [user])

  const handleRefresh = () => {
    setRefreshing(true)
    fetchDevices()
  }

  const getBatteryIcon = (level: number | null) => {
    if (level === null) return <Battery className="w-4 h-4 text-gray-400 dark:text-dark-400" />
    if (level <= 20) return <BatteryLow className="w-4 h-4 text-red-500" />
    if (level <= 50) return <BatteryMedium className="w-4 h-4 text-yellow-500" />
    return <BatteryFull className="w-4 h-4 text-green-500" />
  }

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="h-8 w-32 skeleton rounded" />
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 skeleton rounded-lg" />
            <div className="h-4 w-20 skeleton rounded" />
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
          <div className="lg:col-span-1 space-y-3">
            <SkeletonDeviceCard />
            <SkeletonDeviceCard />
            <SkeletonDeviceCard />
          </div>
          <div className="lg:col-span-3">
            <SkeletonMap />
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-120px)]">
        <div className="card border border-red-200 dark:border-red-900/50 p-6 max-w-md text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-3" />
          <p className="text-red-700 dark:text-red-400 font-medium mb-2">Error loading data</p>
          <p className="text-red-600 dark:text-red-300 text-sm mb-4">{error}</p>
          <button 
            onClick={() => { setLoading(true); fetchDevices(); }}
            className="btn-primary bg-red-600 hover:bg-red-700"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Live Tracking</h1>
        <div className="flex items-center gap-3">
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="btn-ghost p-2"
          >
            <RefreshCw className={clsx('w-5 h-5', refreshing && 'animate-spin')} />
          </button>
          <span className="text-sm text-gray-500 dark:text-dark-400">{devices.length} device(s)</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* Device List */}
        <div className="lg:col-span-1 space-y-3">
          {devices.length === 0 ? (
            <div className="card p-6 text-center">
              <Navigation className="w-12 h-12 text-gray-300 dark:text-dark-600 mx-auto mb-3" />
              <p className="text-gray-500 dark:text-dark-300">No devices registered</p>
              <p className="text-sm text-gray-400 dark:text-dark-400 mt-1">
                Install the app on family phones to start tracking
              </p>
            </div>
          ) : (
            devices.map((device, index) => (
              <div
                key={device.id}
                onClick={() => setSelectedDevice(device)}
                className={clsx(
                  'card p-4 cursor-pointer transition-all border-2',
                  selectedDevice?.id === device.id
                    ? 'border-primary-500 shadow-lg ring-2 ring-primary-500/20'
                    : 'border-transparent hover:border-gray-200 dark:hover:border-dark-600'
                )}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold shadow-lg"
                    style={{ backgroundColor: deviceColors[index % deviceColors.length] }}
                  >
                    {device.device_name.charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-800 dark:text-white truncate">
                      {device.device_name}
                    </h3>
                    <p className="text-sm text-gray-500 dark:text-dark-400">
                      {device.last_seen
                        ? formatDistanceToNow(new Date(device.last_seen), {
                            addSuffix: true,
                          })
                        : 'Never seen'}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    {device.is_online ? (
                      <Wifi className="w-4 h-4 text-green-500" />
                    ) : (
                      <WifiOff className="w-4 h-4 text-gray-400 dark:text-dark-500" />
                    )}
                    <div className="flex items-center gap-1">
                      {getBatteryIcon(device.battery_level)}
                      {device.battery_level !== null && (
                        <span className="text-xs text-gray-500 dark:text-dark-400">
                          {device.battery_level}%
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Map */}
        <div className="lg:col-span-3 h-[calc(100vh-200px)] min-h-[400px] card overflow-hidden p-0">
          <MapContainer
            center={mapCenter}
            zoom={5}
            scrollWheelZoom={true}
            className="h-full w-full"
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <MapBounds devices={devices} />
            
            {devices.map((device, index) => {
              if (!device.latestLocation) return null
              const color = deviceColors[index % deviceColors.length]
              
              return (
                <Marker
                  key={device.id}
                  position={[device.latestLocation.latitude, device.latestLocation.longitude]}
                  icon={createMarkerIcon(color, device.is_online)}
                >
                  <Popup>
                    <div className="p-2">
                      <h3 className="font-bold text-gray-900">{device.device_name}</h3>
                      <p className="text-sm text-gray-600">
                        Last update:{' '}
                        {formatDistanceToNow(new Date(device.latestLocation.created_at), {
                          addSuffix: true,
                        })}
                      </p>
                      {device.latestLocation.speed && (
                        <p className="text-sm text-gray-600">
                          Speed: {Math.round(device.latestLocation.speed * 3.6)} km/h
                        </p>
                      )}
                      {device.latestLocation.accuracy && (
                        <p className="text-sm text-gray-600">
                          Accuracy: ±{Math.round(device.latestLocation.accuracy)}m
                        </p>
                      )}
                    </div>
                  </Popup>
                </Marker>
              )
            })}
          </MapContainer>
        </div>
      </div>
    </div>
  )
}
