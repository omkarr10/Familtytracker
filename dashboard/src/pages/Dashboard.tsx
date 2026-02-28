import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import { Icon, LatLngBounds } from 'leaflet'
import { supabase } from '../lib/supabase'
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
} from 'lucide-react'
import clsx from 'clsx'

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
  const [mapCenter] = useState<[number, number]>([20.5937, 78.9629]) // India center

  // Fetch devices and their latest locations
  useEffect(() => {
    if (!user) return

    const fetchDevices = async () => {
      const { data: devicesData, error: devicesError } = await supabase
        .from('devices')
        .select('*')
        .eq('user_id', user.id)

      if (devicesError) {
        console.error('Error fetching devices:', devicesError)
        return
      }

      // Get latest location for each device
      const devicesWithLocations: DeviceWithLocation[] = await Promise.all(
        (devicesData || []).map(async (device) => {
          const { data: locationData } = await supabase
            .from('locations')
            .select('*')
            .eq('device_id', device.id)
            .order('created_at', { ascending: false })
            .limit(1)
            .single()

          return {
            ...device,
            latestLocation: locationData || undefined,
          }
        })
      )

      setDevices(devicesWithLocations)
      setLoading(false)
    }

    fetchDevices()

    // Subscribe to real-time location updates
    const channel = supabase
      .channel('location-updates')
      .on(
        'postgres_changes',
        { event: 'INSERT', schema: 'public', table: 'locations' },
        (payload) => {
          const newLocation = payload.new as Location
          updateDeviceLocation(newLocation.device_id, newLocation)
        }
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [user, setDevices, updateDeviceLocation])

  const getBatteryIcon = (level: number | null) => {
    if (level === null) return <Battery className="w-4 h-4 text-gray-400" />
    if (level <= 20) return <BatteryLow className="w-4 h-4 text-red-500" />
    if (level <= 50) return <BatteryMedium className="w-4 h-4 text-yellow-500" />
    return <BatteryFull className="w-4 h-4 text-green-500" />
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[calc(100vh-120px)]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">Live Tracking</h1>
        <span className="text-sm text-gray-500">{devices.length} device(s)</span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* Device List */}
        <div className="lg:col-span-1 space-y-3">
          {devices.length === 0 ? (
            <div className="bg-white rounded-lg p-6 text-center">
              <Navigation className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">No devices registered</p>
              <p className="text-sm text-gray-400 mt-1">
                Install the app on family phones to start tracking
              </p>
            </div>
          ) : (
            devices.map((device, index) => (
              <div
                key={device.id}
                onClick={() => setSelectedDevice(device)}
                className={clsx(
                  'bg-white rounded-lg p-4 cursor-pointer transition-all border-2',
                  selectedDevice?.id === device.id
                    ? 'border-primary-500 shadow-lg'
                    : 'border-transparent hover:border-gray-200'
                )}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold"
                    style={{ backgroundColor: deviceColors[index % deviceColors.length] }}
                  >
                    {device.device_name.charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-800 truncate">
                      {device.device_name}
                    </h3>
                    <p className="text-sm text-gray-500">
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
                      <WifiOff className="w-4 h-4 text-gray-400" />
                    )}
                    <div className="flex items-center gap-1">
                      {getBatteryIcon(device.battery_level)}
                      {device.battery_level !== null && (
                        <span className="text-xs text-gray-500">
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
        <div className="lg:col-span-3 h-[calc(100vh-200px)] min-h-[400px] bg-white rounded-lg overflow-hidden shadow-lg">
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
                      <h3 className="font-bold">{device.device_name}</h3>
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
