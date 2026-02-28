import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet'
import { Icon, LatLngBounds } from 'leaflet'
import { supabase } from '../lib/supabase'
import { useAuthStore } from '../store/authStore'
import { Device, Location } from '../types/database'
import { format, subDays, startOfDay, endOfDay } from 'date-fns'
import { Calendar, Clock, MapPin, Play, Pause, SkipForward, SkipBack } from 'lucide-react'

const markerIcon = new Icon({
  iconUrl: `data:image/svg+xml;base64,${btoa(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#3b82f6" width="32" height="32">
      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
    </svg>
  `)}`,
  iconSize: [32, 32],
  iconAnchor: [16, 32],
  popupAnchor: [0, -32],
})

function MapBounds({ locations }: { locations: Location[] }) {
  const map = useMap()

  useEffect(() => {
    if (locations.length === 0) return

    if (locations.length === 1) {
      map.setView([locations[0].latitude, locations[0].longitude], 15)
    } else {
      const bounds = new LatLngBounds(
        locations.map((l) => [l.latitude, l.longitude])
      )
      map.fitBounds(bounds, { padding: [50, 50] })
    }
  }, [locations, map])

  return null
}

export default function History() {
  const { user } = useAuthStore()
  const [devices, setDevices] = useState<Device[]>([])
  const [selectedDeviceId, setSelectedDeviceId] = useState<string>('')
  const [selectedDate, setSelectedDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [locations, setLocations] = useState<Location[]>([])
  const [loading, setLoading] = useState(false)
  const [playbackIndex, setPlaybackIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)

  // Fetch devices
  useEffect(() => {
    if (!user) return

    const fetchDevices = async () => {
      const { data } = await supabase
        .from('devices')
        .select('*')
        .eq('user_id', user.id)

      if (data && data.length > 0) {
        setDevices(data)
        setSelectedDeviceId(data[0].id)
      }
    }

    fetchDevices()
  }, [user])

  // Fetch locations when device or date changes
  useEffect(() => {
    if (!selectedDeviceId || !selectedDate) return

    const fetchLocations = async () => {
      setLoading(true)
      const startDate = startOfDay(new Date(selectedDate))
      const endDate = endOfDay(new Date(selectedDate))

      const { data, error } = await supabase
        .from('locations')
        .select('*')
        .eq('device_id', selectedDeviceId)
        .gte('created_at', startDate.toISOString())
        .lte('created_at', endDate.toISOString())
        .order('created_at', { ascending: true })

      if (error) {
        console.error('Error fetching locations:', error)
      } else {
        setLocations(data || [])
        setPlaybackIndex(0)
        setIsPlaying(false)
      }
      setLoading(false)
    }

    fetchLocations()
  }, [selectedDeviceId, selectedDate])

  // Playback animation
  useEffect(() => {
    if (!isPlaying || locations.length === 0) return

    const interval = setInterval(() => {
      setPlaybackIndex((prev) => {
        if (prev >= locations.length - 1) {
          setIsPlaying(false)
          return prev
        }
        return prev + 1
      })
    }, 500)

    return () => clearInterval(interval)
  }, [isPlaying, locations.length])

  const pathCoordinates: [number, number][] = locations.map((l) => [l.latitude, l.longitude])
  const currentLocation = locations[playbackIndex]

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">Location History</h1>
      </div>

      {/* Controls */}
      <div className="bg-white rounded-lg p-4 shadow-sm">
        <div className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Device
            </label>
            <select
              value={selectedDeviceId}
              onChange={(e) => setSelectedDeviceId(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none"
            >
              {devices.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.device_name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Date
            </label>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              max={format(new Date(), 'yyyy-MM-dd')}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none"
            />
          </div>

          <div className="flex items-end gap-2">
            <button
              onClick={() => setSelectedDate(format(new Date(), 'yyyy-MM-dd'))}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Today
            </button>
            <button
              onClick={() => setSelectedDate(format(subDays(new Date(), 1), 'yyyy-MM-dd'))}
              className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition"
            >
              Yesterday
            </button>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-2 text-gray-500 mb-1">
            <MapPin className="w-4 h-4" />
            <span className="text-sm">Total Points</span>
          </div>
          <p className="text-2xl font-bold text-gray-800">{locations.length}</p>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-2 text-gray-500 mb-1">
            <Clock className="w-4 h-4" />
            <span className="text-sm">First Activity</span>
          </div>
          <p className="text-lg font-semibold text-gray-800">
            {locations.length > 0
              ? format(new Date(locations[0].created_at), 'HH:mm')
              : '--:--'}
          </p>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-2 text-gray-500 mb-1">
            <Clock className="w-4 h-4" />
            <span className="text-sm">Last Activity</span>
          </div>
          <p className="text-lg font-semibold text-gray-800">
            {locations.length > 0
              ? format(new Date(locations[locations.length - 1].created_at), 'HH:mm')
              : '--:--'}
          </p>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-2 text-gray-500 mb-1">
            <Calendar className="w-4 h-4" />
            <span className="text-sm">Date</span>
          </div>
          <p className="text-lg font-semibold text-gray-800">
            {format(new Date(selectedDate), 'MMM dd, yyyy')}
          </p>
        </div>
      </div>

      {/* Map and Playback */}
      <div className="bg-white rounded-lg shadow-sm overflow-hidden">
        {/* Playback Controls */}
        {locations.length > 0 && (
          <div className="flex items-center justify-center gap-4 p-3 border-b bg-gray-50">
            <button
              onClick={() => setPlaybackIndex(0)}
              className="p-2 hover:bg-gray-200 rounded-lg transition"
              disabled={playbackIndex === 0}
            >
              <SkipBack className="w-5 h-5" />
            </button>
            <button
              onClick={() => setIsPlaying(!isPlaying)}
              className="p-3 bg-primary-600 text-white rounded-full hover:bg-primary-700 transition"
            >
              {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5" />}
            </button>
            <button
              onClick={() => setPlaybackIndex(locations.length - 1)}
              className="p-2 hover:bg-gray-200 rounded-lg transition"
              disabled={playbackIndex === locations.length - 1}
            >
              <SkipForward className="w-5 h-5" />
            </button>
            <div className="flex-1 max-w-md">
              <input
                type="range"
                min={0}
                max={locations.length - 1}
                value={playbackIndex}
                onChange={(e) => setPlaybackIndex(parseInt(e.target.value))}
                className="w-full"
              />
            </div>
            <span className="text-sm text-gray-600 w-32 text-right">
              {currentLocation
                ? format(new Date(currentLocation.created_at), 'HH:mm:ss')
                : '--:--:--'}
            </span>
          </div>
        )}

        {/* Map */}
        <div className="h-[500px]">
          {loading ? (
            <div className="flex items-center justify-center h-full">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
          ) : locations.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-500">
              <MapPin className="w-16 h-16 text-gray-300 mb-4" />
              <p>No location data for this date</p>
            </div>
          ) : (
            <MapContainer
              center={[locations[0].latitude, locations[0].longitude]}
              zoom={13}
              scrollWheelZoom={true}
              className="h-full w-full"
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <MapBounds locations={locations} />

              {/* Path */}
              <Polyline
                positions={pathCoordinates}
                color="#3b82f6"
                weight={3}
                opacity={0.7}
              />

              {/* Current position marker */}
              {currentLocation && (
                <Marker
                  position={[currentLocation.latitude, currentLocation.longitude]}
                  icon={markerIcon}
                >
                  <Popup>
                    <div className="text-sm">
                      <p className="font-medium">
                        {format(new Date(currentLocation.created_at), 'HH:mm:ss')}
                      </p>
                      {currentLocation.speed && (
                        <p>Speed: {Math.round(currentLocation.speed * 3.6)} km/h</p>
                      )}
                    </div>
                  </Popup>
                </Marker>
              )}
            </MapContainer>
          )}
        </div>
      </div>
    </div>
  )
}
