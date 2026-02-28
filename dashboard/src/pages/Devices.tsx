import { useEffect, useState } from 'react'
import { supabase } from '../lib/supabase'
import { useAuthStore } from '../store/authStore'
import { Device } from '../types/database'
import { formatDistanceToNow } from 'date-fns'
import {
  Smartphone,
  Plus,
  Trash2,
  Battery,
  Wifi,
  WifiOff,
  Copy,
  Check,
} from 'lucide-react'
import clsx from 'clsx'

export default function Devices() {
  const { user } = useAuthStore()
  const [devices, setDevices] = useState<Device[]>([])
  const [loading, setLoading] = useState(true)
  const [showAddModal, setShowAddModal] = useState(false)
  const [newDeviceName, setNewDeviceName] = useState('')
  const [addingDevice, setAddingDevice] = useState(false)
  const [copiedId, setCopiedId] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    fetchDevices()
  }, [user])

  const fetchDevices = async () => {
    const { data, error } = await supabase
      .from('devices')
      .select('*')
      .eq('user_id', user!.id)
      .order('created_at', { ascending: false })

    if (error) {
      console.error('Error fetching devices:', error)
    } else {
      setDevices(data || [])
    }
    setLoading(false)
  }

  const addDevice = async () => {
    if (!newDeviceName.trim()) return
    setAddingDevice(true)

    const { data, error } = await supabase
      .from('devices')
      .insert({
        user_id: user!.id,
        device_name: newDeviceName.trim(),
      })
      .select()
      .single()

    if (error) {
      console.error('Error adding device:', error)
    } else {
      setDevices([data, ...devices])
      setNewDeviceName('')
      setShowAddModal(false)
    }
    setAddingDevice(false)
  }

  const deleteDevice = async (deviceId: string) => {
    if (!confirm('Are you sure you want to delete this device? All location history will be lost.')) return

    const { error } = await supabase
      .from('devices')
      .delete()
      .eq('id', deviceId)

    if (error) {
      console.error('Error deleting device:', error)
    } else {
      setDevices(devices.filter((d) => d.id !== deviceId))
    }
  }

  const copyDeviceId = (id: string) => {
    navigator.clipboard.writeText(id)
    setCopiedId(id)
    setTimeout(() => setCopiedId(null), 2000)
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
        <h1 className="text-2xl font-bold text-gray-800">Devices</h1>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition"
        >
          <Plus className="w-5 h-5" />
          Add Device
        </button>
      </div>

      {devices.length === 0 ? (
        <div className="bg-white rounded-lg p-12 text-center">
          <Smartphone className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">No devices yet</h2>
          <p className="text-gray-500 mb-6">
            Add a device to get started with tracking
          </p>
          <button
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center gap-2 px-6 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition"
          >
            <Plus className="w-5 h-5" />
            Add Your First Device
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {devices.map((device) => (
            <div
              key={device.id}
              className="bg-white rounded-lg p-6 shadow-sm hover:shadow-md transition"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
                    <Smartphone className="w-6 h-6 text-primary-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-800">{device.device_name}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      {device.is_online ? (
                        <>
                          <Wifi className="w-4 h-4 text-green-500" />
                          <span className="text-sm text-green-600">Online</span>
                        </>
                      ) : (
                        <>
                          <WifiOff className="w-4 h-4 text-gray-400" />
                          <span className="text-sm text-gray-500">Offline</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => deleteDevice(device.id)}
                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="space-y-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="text-gray-500">Battery</span>
                  <div className="flex items-center gap-2">
                    <Battery className="w-4 h-4 text-gray-400" />
                    <span className={clsx(
                      device.battery_level !== null && device.battery_level <= 20 && 'text-red-500',
                      device.battery_level !== null && device.battery_level > 20 && 'text-gray-700'
                    )}>
                      {device.battery_level !== null ? `${device.battery_level}%` : 'Unknown'}
                    </span>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-500">Last seen</span>
                  <span className="text-gray-700">
                    {device.last_seen
                      ? formatDistanceToNow(new Date(device.last_seen), { addSuffix: true })
                      : 'Never'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-500">Device ID</span>
                  <button
                    onClick={() => copyDeviceId(device.id)}
                    className="flex items-center gap-1 text-gray-600 hover:text-primary-600 transition"
                  >
                    <span className="font-mono text-xs truncate max-w-[100px]">
                      {device.id.slice(0, 8)}...
                    </span>
                    {copiedId === device.id ? (
                      <Check className="w-4 h-4 text-green-500" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Device Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl w-full max-w-md p-6">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Add New Device</h2>
            <p className="text-gray-600 mb-4">
              Enter a name for the device (e.g., "Dad's Phone", "Mom's Phone")
            </p>
            <input
              type="text"
              value={newDeviceName}
              onChange={(e) => setNewDeviceName(e.target.value)}
              placeholder="Device name"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none mb-4"
            />
            <div className="flex gap-3">
              <button
                onClick={() => setShowAddModal(false)}
                className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
              >
                Cancel
              </button>
              <button
                onClick={addDevice}
                disabled={addingDevice || !newDeviceName.trim()}
                className="flex-1 px-4 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition disabled:opacity-50"
              >
                {addingDevice ? 'Adding...' : 'Add Device'}
              </button>
            </div>
            
            <div className="mt-6 p-4 bg-gray-50 rounded-lg">
              <h3 className="font-medium text-gray-800 mb-2">Next Steps</h3>
              <ol className="text-sm text-gray-600 space-y-2 list-decimal list-inside">
                <li>Install the Family Tracker app on the device</li>
                <li>Copy the Device ID and enter it in the app</li>
                <li>Grant location permissions</li>
                <li>The device will appear on your map!</li>
              </ol>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
