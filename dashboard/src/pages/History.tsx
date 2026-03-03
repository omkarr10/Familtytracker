import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet'
import { Icon, LatLngBounds } from 'leaflet'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { useSettingsStore, mapTileLayers } from '../store/settingsStore'
import { Device, Location } from '../types/database'
import { format, subDays, startOfDay, endOfDay } from 'date-fns'
import { Calendar, Clock, MapPin, Play, Pause, SkipForward, SkipBack, Map, List } from 'lucide-react'
import { SkeletonCard, SkeletonMap } from '../components/Skeleton'
import { Timeline } from '../components/Timeline'
import { MapThemeSelector } from '../components/MapThemeSelector'
import clsx from 'clsx'

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
  const { mapTheme } = useSettingsStore()
  const [devices, setDevices] = useState<Device[]>([])
  const [selectedDeviceId, setSelectedDeviceId] = useState<string>('')
  const [selectedDate, setSelectedDate] = useState(format(new Date(), 'yyyy-MM-dd'))
  const [locations, setLocations] = useState<Location[]>([])
  const [loading, setLoading] = useState(false)
  const [playbackIndex, setPlaybackIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [viewMode, setViewMode] = useState<'map' | 'timeline'>('map')

  // Fetch devices
  useEffect(() => {
    if (!user) return

    const fetchDevices = async () => {
      try {
        const { data } = await api.from('devices').select('*', {
          eq: ['user_id', user.id]
        })

        if (data && data.length > 0) {
          setDevices(data)
          setSelectedDeviceId(data[0].id)
        }
      } catch (err) {
        console.error('Error fetching devices:', err)
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

      try {
        // Fetch more locations and filter by date client-side
        const { data } = await api.from('locations').select('*', {
          eq: ['device_id', selectedDeviceId],
          limit: 1000
        })

        // Filter by date range and sort
        const filtered = (data || [])
          .filter((loc: Location) => {
            const locDate = new Date(loc.created_at)
            return locDate >= startDate && locDate <= endDate
          })
          .sort((a: Location, b: Location) => 
            new Date(a.created_at).getTime() - new Date(b.created_at).getTime()
          )

        setLocations(filtered)
        setPlaybackIndex(0)
        setIsPlaying(false)
      } catch (err) {
        console.error('Error fetching locations:', err)
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
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Location History</h1>
      </div>

      {/* Controls */}
      <div className="card p-4">
        <div className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 dark:text-dark-200 mb-1">
              Device
            </label>
            <select
              value={selectedDeviceId}
              onChange={(e) => setSelectedDeviceId(e.target.value)}
              className="input w-full"
            >
              {devices.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.device_name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex-1 min-w-[200px]">
            <label className="block text-sm font-medium text-gray-700 dark:text-dark-200 mb-1">
              Date
            </label>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              max={format(new Date(), 'yyyy-MM-dd')}
              className="input w-full"
            />
          </div>

          <div className="flex items-end gap-2">
            <button
              onClick={() => setSelectedDate(format(new Date(), 'yyyy-MM-dd'))}
              className="btn-secondary"
            >
              Today
            </button>
            <button
              onClick={() => setSelectedDate(format(subDays(new Date(), 1), 'yyyy-MM-dd'))}
              className="btn-secondary"
            >
              Yesterday
            </button>
          </div>

          {/* View Toggle */}
          <div className="flex items-end">
            <div className="flex bg-gray-100 dark:bg-dark-700 rounded-lg p-1">
              <button
                onClick={() => setViewMode('map')}
                className={clsx(
                  'flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-all',
                  viewMode === 'map'
                    ? 'bg-white dark:bg-dark-600 text-primary-600 dark:text-primary-400 shadow'
                    : 'text-gray-600 dark:text-dark-300 hover:text-gray-800 dark:hover:text-white'
                )}
              >
                <Map className="w-4 h-4" />
                Map
              </button>
              <button
                onClick={() => setViewMode('timeline')}
                className={clsx(
                  'flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-all',
                  viewMode === 'timeline'
                    ? 'bg-white dark:bg-dark-600 text-primary-600 dark:text-primary-400 shadow'
                    : 'text-gray-600 dark:text-dark-300 hover:text-gray-800 dark:hover:text-white'
                )}
              >
                <List className="w-4 h-4" />
                Timeline
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="card p-4">
          <div className="flex items-center gap-2 text-gray-500 dark:text-dark-400 mb-1">
            <MapPin className="w-4 h-4" />
            <span className="text-sm">Total Points</span>
          </div>
          <p className="text-2xl font-bold text-gray-800 dark:text-white">{locations.length}</p>
        </div>
        <div className="card p-4">
          <div className="flex items-center gap-2 text-gray-500 dark:text-dark-400 mb-1">
            <Clock className="w-4 h-4" />
            <span className="text-sm">First Activity</span>
          </div>
          <p className="text-lg font-semibold text-gray-800 dark:text-white">
            {locations.length > 0
              ? format(new Date(locations[0].created_at), 'HH:mm')
              : '--:--'}
          </p>
        </div>
        <div className="card p-4">
          <div className="flex items-center gap-2 text-gray-500 dark:text-dark-400 mb-1">
            <Clock className="w-4 h-4" />
            <span className="text-sm">Last Activity</span>
          </div>
          <p className="text-lg font-semibold text-gray-800 dark:text-white">
            {locations.length > 0
              ? format(new Date(locations[locations.length - 1].created_at), 'HH:mm')
              : '--:--'}
          </p>
        </div>
        <div className="card p-4">
          <div className="flex items-center gap-2 text-gray-500 dark:text-dark-400 mb-1">
            <Calendar className="w-4 h-4" />
            <span className="text-sm">Date</span>
          </div>
          <p className="text-lg font-semibold text-gray-800 dark:text-white">
            {format(new Date(selectedDate), 'MMM dd, yyyy')}
          </p>
        </div>
      </div>

      {/* Map and Playback */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Map Section */}
        <div className={clsx('card overflow-hidden p-0', viewMode === 'map' ? 'lg:col-span-3' : 'lg:col-span-2')}>
          {/* Playback Controls */}
          {locations.length > 0 && viewMode === 'map' && (
          <div className="flex items-center justify-center gap-4 p-3 border-b border-gray-200 dark:border-dark-700 bg-gray-50 dark:bg-dark-800">
            <button
              onClick={() => setPlaybackIndex(0)}
              className="btn-ghost p-2"
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
              className="btn-ghost p-2"
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
                className="w-full accent-primary-600"
              />
            </div>
            <span className="text-sm text-gray-600 dark:text-dark-300 w-32 text-right">
              {currentLocation
                ? format(new Date(currentLocation.created_at), 'HH:mm:ss')
                : '--:--:--'}
            </span>
          </div>
        )}

        {/* Map */}
        <div className="h-[500px] relative">
          {viewMode === 'map' && <MapThemeSelector />}
          {loading ? (
            <div className="h-full skeleton" />
          ) : locations.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-gray-500 dark:text-dark-400 bg-gray-50 dark:bg-dark-800">
              <MapPin className="w-16 h-16 text-gray-300 dark:text-dark-600 mb-4" />
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
                key={mapTheme}
                attribution={mapTileLayers[mapTheme].attribution}
                url={mapTileLayers[mapTheme].url}
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

        {/* Timeline Panel */}
        {viewMode === 'timeline' && (
          <div className="card p-4 lg:col-span-1 max-h-[600px] overflow-y-auto">
            <h3 className="font-semibold text-gray-800 dark:text-white mb-4">Activity Timeline</h3>
            <Timeline
              locations={locations}
              currentIndex={playbackIndex}
              onLocationClick={(_, index) => setPlaybackIndex(index)}
            />
          </div>
        )}
      </div>
    </div>
  )
}
