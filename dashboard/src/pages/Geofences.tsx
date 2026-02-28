import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Circle, Marker, Popup, useMapEvents } from 'react-leaflet'
import { Icon } from 'leaflet'
import { supabase } from '../lib/supabase'
import { useAuthStore } from '../store/authStore'
import { Geofence } from '../types/database'
import { Shield, Plus, Trash2, MapPin, Bell, BellOff } from 'lucide-react'

const centerIcon = new Icon({
  iconUrl: `data:image/svg+xml;base64,${btoa(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#22c55e" width="24" height="24">
      <circle cx="12" cy="12" r="8" fill="#22c55e" stroke="#fff" stroke-width="2"/>
    </svg>
  `)}`,
  iconSize: [24, 24],
  iconAnchor: [12, 12],
})

function LocationPicker({ onLocationSelect }: { onLocationSelect: (lat: number, lng: number) => void }) {
  useMapEvents({
    click: (e) => {
      onLocationSelect(e.latlng.lat, e.latlng.lng)
    },
  })
  return null
}

export default function Geofences() {
  const { user } = useAuthStore()
  const [geofences, setGeofences] = useState<Geofence[]>([])
  const [loading, setLoading] = useState(true)
  const [showAddModal, setShowAddModal] = useState(false)
  const [isSelectingLocation, setIsSelectingLocation] = useState(false)
  const [newGeofence, setNewGeofence] = useState({
    name: '',
    latitude: 0,
    longitude: 0,
    radius_meters: 100,
    alert_on_enter: true,
    alert_on_exit: true,
  })

  useEffect(() => {
    if (!user) return
    fetchGeofences()
  }, [user])

  const fetchGeofences = async () => {
    const { data, error } = await supabase
      .from('geofences')
      .select('*')
      .eq('user_id', user!.id)
      .order('created_at', { ascending: false })

    if (error) {
      console.error('Error fetching geofences:', error)
    } else {
      setGeofences(data || [])
    }
    setLoading(false)
  }

  const addGeofence = async () => {
    if (!newGeofence.name.trim() || newGeofence.latitude === 0) return

    const { data, error } = await supabase
      .from('geofences')
      .insert({
        user_id: user!.id,
        name: newGeofence.name.trim(),
        latitude: newGeofence.latitude,
        longitude: newGeofence.longitude,
        radius_meters: newGeofence.radius_meters,
        alert_on_enter: newGeofence.alert_on_enter,
        alert_on_exit: newGeofence.alert_on_exit,
      })
      .select()
      .single()

    if (error) {
      console.error('Error adding geofence:', error)
    } else {
      setGeofences([data, ...geofences])
      setShowAddModal(false)
      setNewGeofence({
        name: '',
        latitude: 0,
        longitude: 0,
        radius_meters: 100,
        alert_on_enter: true,
        alert_on_exit: true,
      })
      setIsSelectingLocation(false)
    }
  }

  const deleteGeofence = async (id: string) => {
    if (!confirm('Are you sure you want to delete this geofence?')) return

    const { error } = await supabase
      .from('geofences')
      .delete()
      .eq('id', id)

    if (error) {
      console.error('Error deleting geofence:', error)
    } else {
      setGeofences(geofences.filter((g) => g.id !== id))
    }
  }

  const handleLocationSelect = (lat: number, lng: number) => {
    setNewGeofence((prev) => ({
      ...prev,
      latitude: lat,
      longitude: lng,
    }))
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-800">Geofences</h1>
        <button
          onClick={() => {
            setShowAddModal(true)
            setIsSelectingLocation(true)
          }}
          className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition"
        >
          <Plus className="w-5 h-5" />
          Add Geofence
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Geofence List */}
        <div className="space-y-4">
          {geofences.length === 0 ? (
            <div className="bg-white rounded-lg p-8 text-center">
              <Shield className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">No geofences created</p>
              <p className="text-sm text-gray-400 mt-1">
                Create safe zones to get alerts when devices enter or exit
              </p>
            </div>
          ) : (
            geofences.map((geofence) => (
              <div key={geofence.id} className="bg-white rounded-lg p-4 shadow-sm">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                      <Shield className="w-5 h-5 text-green-600" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-800">{geofence.name}</h3>
                      <p className="text-sm text-gray-500">
                        Radius: {geofence.radius_meters}m
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => deleteGeofence(geofence.id)}
                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
                <div className="flex gap-2">
                  <span
                    className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs ${
                      geofence.alert_on_enter
                        ? 'bg-blue-100 text-blue-700'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {geofence.alert_on_enter ? <Bell className="w-3 h-3" /> : <BellOff className="w-3 h-3" />}
                    Enter
                  </span>
                  <span
                    className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs ${
                      geofence.alert_on_exit
                        ? 'bg-orange-100 text-orange-700'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {geofence.alert_on_exit ? <Bell className="w-3 h-3" /> : <BellOff className="w-3 h-3" />}
                    Exit
                  </span>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Map */}
        <div className="lg:col-span-2 h-[500px] bg-white rounded-lg overflow-hidden shadow-sm">
          <MapContainer
            center={[20.5937, 78.9629]}
            zoom={5}
            scrollWheelZoom={true}
            className="h-full w-full"
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            
            {isSelectingLocation && (
              <LocationPicker onLocationSelect={handleLocationSelect} />
            )}

            {/* Existing geofences */}
            {geofences.map((geofence) => (
              <Circle
                key={geofence.id}
                center={[geofence.latitude, geofence.longitude]}
                radius={geofence.radius_meters}
                pathOptions={{
                  color: '#22c55e',
                  fillColor: '#22c55e',
                  fillOpacity: 0.2,
                }}
              >
                <Popup>
                  <div className="font-medium">{geofence.name}</div>
                  <div className="text-sm text-gray-500">
                    Radius: {geofence.radius_meters}m
                  </div>
                </Popup>
              </Circle>
            ))}

            {/* New geofence preview */}
            {isSelectingLocation && newGeofence.latitude !== 0 && (
              <>
                <Circle
                  center={[newGeofence.latitude, newGeofence.longitude]}
                  radius={newGeofence.radius_meters}
                  pathOptions={{
                    color: '#3b82f6',
                    fillColor: '#3b82f6',
                    fillOpacity: 0.2,
                    dashArray: '5, 10',
                  }}
                />
                <Marker
                  position={[newGeofence.latitude, newGeofence.longitude]}
                  icon={centerIcon}
                />
              </>
            )}
          </MapContainer>
        </div>
      </div>

      {/* Add Geofence Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl w-full max-w-md p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Add New Geofence</h2>
            
            {isSelectingLocation && newGeofence.latitude === 0 ? (
              <div className="mb-4 p-4 bg-blue-50 rounded-lg">
                <div className="flex items-center gap-2 text-blue-700">
                  <MapPin className="w-5 h-5" />
                  <span className="font-medium">Click on the map to select location</span>
                </div>
              </div>
            ) : (
              <>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Name
                    </label>
                    <input
                      type="text"
                      value={newGeofence.name}
                      onChange={(e) =>
                        setNewGeofence((prev) => ({ ...prev, name: e.target.value }))
                      }
                      placeholder="e.g., Home, School, Office"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Radius (meters)
                    </label>
                    <input
                      type="range"
                      min={50}
                      max={1000}
                      step={50}
                      value={newGeofence.radius_meters}
                      onChange={(e) =>
                        setNewGeofence((prev) => ({
                          ...prev,
                          radius_meters: parseInt(e.target.value),
                        }))
                      }
                      className="w-full"
                    />
                    <div className="text-center text-sm text-gray-600">
                      {newGeofence.radius_meters}m
                    </div>
                  </div>

                  <div className="flex gap-4">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newGeofence.alert_on_enter}
                        onChange={(e) =>
                          setNewGeofence((prev) => ({
                            ...prev,
                            alert_on_enter: e.target.checked,
                          }))
                        }
                        className="w-4 h-4 text-primary-600 rounded"
                      />
                      <span className="text-sm text-gray-700">Alert on enter</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={newGeofence.alert_on_exit}
                        onChange={(e) =>
                          setNewGeofence((prev) => ({
                            ...prev,
                            alert_on_exit: e.target.checked,
                          }))
                        }
                        className="w-4 h-4 text-primary-600 rounded"
                      />
                      <span className="text-sm text-gray-700">Alert on exit</span>
                    </label>
                  </div>
                </div>
              </>
            )}

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => {
                  setShowAddModal(false)
                  setIsSelectingLocation(false)
                  setNewGeofence({
                    name: '',
                    latitude: 0,
                    longitude: 0,
                    radius_meters: 100,
                    alert_on_enter: true,
                    alert_on_exit: true,
                  })
                }}
                className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
              >
                Cancel
              </button>
              {newGeofence.latitude !== 0 && (
                <button
                  onClick={addGeofence}
                  disabled={!newGeofence.name.trim()}
                  className="flex-1 px-4 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition disabled:opacity-50"
                >
                  Create Geofence
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
