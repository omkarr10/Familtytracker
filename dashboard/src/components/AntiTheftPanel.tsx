import { useState } from 'react'
import { api } from '../lib/api'
import { Device } from '../types/database'
import {
  Lock,
  Unlock,
  Volume2,
  VolumeX,
  Camera,
  MapPin,
  AlertTriangle,
  Shield,
  Loader2,
  Smartphone,
  Image,
  X,
  Power,
} from 'lucide-react'
import clsx from 'clsx'
import { toast } from './Toast'

interface AntiTheftPanelProps {
  device: Device
  onClose: () => void
}

interface TheftPhoto {
  id: string
  device_id: string
  photo_base64: string
  camera_type: string  // 'front' or 'back'
  trigger_event: string
  latitude?: number
  longitude?: number
  captured_at: string
  created_at: string
}

export function AntiTheftPanel({ device, onClose }: AntiTheftPanelProps) {
  const [loading, setLoading] = useState<string | null>(null)
  const [theftMode, setTheftMode] = useState(false)
  const [photos, setPhotos] = useState<TheftPhoto[]>([])
  const [showPhotos, setShowPhotos] = useState(false)
  const [selectedPhoto, setSelectedPhoto] = useState<TheftPhoto | null>(null)

  const sendCommand = async (command: string, label: string, parameters: Record<string, any> = {}) => {
    setLoading(command)
    try {
      // Minimal payload - let database handle defaults
      const payload: any = {
        device_id: device.id,
        command: command,
        status: 'pending',
      }
      // Only add parameters if not empty
      if (Object.keys(parameters).length > 0) {
        payload.parameters = parameters
      }
      
      await api.from('remote_commands').insert(payload)
      toast.success(`${label} command sent!`)
      
      if (command === 'activate_theft_mode') {
        setTheftMode(true)
      } else if (command === 'deactivate_theft_mode') {
        setTheftMode(false)
      }
    } catch (error: any) {
      toast.error(`Failed: ${error.message}`)
    } finally {
      setLoading(null)
    }
  }

  const fetchTheftPhotos = async () => {
    setShowPhotos(true)
    setLoading('photos')
    try {
      const { data } = await api.from('theft_photos').select('*', {
        eq: ['device_id', device.id],
        order: ['created_at', false],
        limit: 20,
      })
      setPhotos(data || [])
    } catch (error: any) {
      toast.error(`Failed to load photos: ${error.message}`)
    } finally {
      setLoading(null)
    }
  }

  const CommandButton = ({
    command,
    label,
    icon: Icon,
    variant = 'default',
    disabled = false,
    parameters = {},
  }: {
    command: string
    label: string
    icon: any
    variant?: 'default' | 'danger' | 'warning' | 'success'
    disabled?: boolean
    parameters?: Record<string, any>
  }) => (
    <button
      onClick={() => sendCommand(command, label, parameters)}
      disabled={loading !== null || disabled}
      className={clsx(
        'flex flex-col items-center justify-center p-4 rounded-xl transition-all',
        'border-2 hover:scale-105 active:scale-95',
        variant === 'default' && 'border-gray-200 dark:border-gray-700 hover:border-blue-500 dark:hover:border-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20',
        variant === 'danger' && 'border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20 hover:bg-red-100 dark:hover:bg-red-900/40',
        variant === 'warning' && 'border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-900/20 hover:bg-orange-100 dark:hover:bg-orange-900/40',
        variant === 'success' && 'border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 hover:bg-green-100 dark:hover:bg-green-900/40',
        (loading !== null || disabled) && 'opacity-50 cursor-not-allowed'
      )}
    >
      {loading === command ? (
        <Loader2 className="w-6 h-6 animate-spin text-gray-500" />
      ) : (
        <Icon className={clsx(
          'w-6 h-6',
          variant === 'default' && 'text-gray-600 dark:text-gray-400',
          variant === 'danger' && 'text-red-600 dark:text-red-400',
          variant === 'warning' && 'text-orange-600 dark:text-orange-400',
          variant === 'success' && 'text-green-600 dark:text-green-400'
        )} />
      )}
      <span className={clsx(
        'mt-2 text-xs font-medium',
        variant === 'default' && 'text-gray-700 dark:text-gray-300',
        variant === 'danger' && 'text-red-700 dark:text-red-300',
        variant === 'warning' && 'text-orange-700 dark:text-orange-300',
        variant === 'success' && 'text-green-700 dark:text-green-300'
      )}>
        {label}
      </span>
    </button>
  )

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-red-100 dark:bg-red-900/30 rounded-lg">
              <Shield className="w-5 h-5 text-red-600 dark:text-red-400" />
            </div>
            <div>
              <h2 className="font-semibold text-gray-900 dark:text-gray-100">
                Anti-Theft Controls
              </h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {device.device_name}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-500" />
          </button>
        </div>

        {/* Theft Mode Toggle */}
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between p-4 rounded-xl bg-gradient-to-r from-red-500 to-orange-500">
            <div className="flex items-center gap-3">
              <AlertTriangle className="w-6 h-6 text-white" />
              <div>
                <p className="font-semibold text-white">Theft Mode</p>
                <p className="text-xs text-white/80">
                  {theftMode ? 'Active - High frequency tracking' : 'Inactive'}
                </p>
              </div>
            </div>
            <button
              onClick={() => sendCommand(
                theftMode ? 'deactivate_theft_mode' : 'activate_theft_mode',
                theftMode ? 'Deactivate' : 'Activate'
              )}
              disabled={loading !== null}
              className={clsx(
                'px-4 py-2 rounded-lg font-medium transition-colors',
                theftMode
                  ? 'bg-white text-red-600 hover:bg-gray-100'
                  : 'bg-white/20 text-white hover:bg-white/30'
              )}
            >
              {loading === 'activate_theft_mode' || loading === 'deactivate_theft_mode' ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : theftMode ? (
                'Deactivate'
              ) : (
                'Activate'
              )}
            </button>
          </div>
        </div>

        {/* Command Grid */}
        <div className="p-4 grid grid-cols-3 gap-3">
          <CommandButton
            command="lock"
            label="Lock Phone"
            icon={Lock}
            variant="warning"
          />
          <CommandButton
            command="stealth_lock"
            label="Stealth Lock"
            icon={Power}
            variant="danger"
          />
          <CommandButton
            command="unlock"
            label="Unlock Phone"
            icon={Unlock}
            variant="success"
          />
          <CommandButton
            command="alarm"
            label="Sound Alarm"
            icon={Volume2}
            variant="danger"
          />
          <CommandButton
            command="stop_alarm"
            label="Stop Alarm"
            icon={VolumeX}
          />
          <CommandButton
            command="capture"
            label="Take Photos"
            icon={Camera}
            parameters={{ continuous: true, count: 5 }}
          />
          <CommandButton
            command="locate"
            label="Get Location"
            icon={MapPin}
            variant="success"
          />
          <button
            onClick={fetchTheftPhotos}
            disabled={loading !== null}
            className={clsx(
              'flex flex-col items-center justify-center p-4 rounded-xl transition-all',
              'border-2 border-purple-200 dark:border-purple-800 bg-purple-50 dark:bg-purple-900/20',
              'hover:bg-purple-100 dark:hover:bg-purple-900/40 hover:scale-105 active:scale-95',
              loading !== null && 'opacity-50 cursor-not-allowed'
            )}
          >
            {loading === 'photos' ? (
              <Loader2 className="w-6 h-6 animate-spin text-purple-500" />
            ) : (
              <Image className="w-6 h-6 text-purple-600 dark:text-purple-400" />
            )}
            <span className="mt-2 text-xs font-medium text-purple-700 dark:text-purple-300">
              View Photos
            </span>
          </button>
        </div>

        {/* SMS Commands Info */}
        <div className="px-4 pb-4">
          <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-xl">
            <div className="flex items-start gap-3">
              <Smartphone className="w-5 h-5 text-blue-600 dark:text-blue-400 mt-0.5" />
              <div>
                <p className="font-medium text-blue-900 dark:text-blue-100">
                  SMS Commands (No Internet)
                </p>
                <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">
                  If phone has no internet, send SMS to the device:
                </p>
                <div className="space-y-3 font-mono text-sm">
                  <p>• TRACKIT LOCATE - Get location</p>
                  <p>• TRACKIT ALARM - Sound alarm</p>
                  <p>• TRACKIT CAPTURE - Take photos</p>
                  <p>• TRACKIT THEFT - Activate theft mode</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Photos Modal */}
        {showPhotos && (
          <div className="border-t border-gray-200 dark:border-gray-700 p-4">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                Captured Photos
              </h3>
              <button
                onClick={() => setShowPhotos(false)}
                className="text-sm text-gray-500 hover:text-gray-700"
              >
                Hide
              </button>
            </div>
            
            {photos.length === 0 ? (
              <p className="text-center text-gray-500 py-8">
                No photos captured yet
              </p>
            ) : (
              <div className="grid grid-cols-3 gap-2">
                {photos.map((photo) => (
                  <button
                    key={photo.id}
                    onClick={() => setSelectedPhoto(photo)}
                    className="aspect-square rounded-lg overflow-hidden hover:ring-2 hover:ring-blue-500 transition-all"
                  >
                    <img
                      src={`data:image/jpeg;base64,${photo.photo_base64}`}
                      alt={photo.camera_type === 'front' ? 'Front camera' : 'Back camera'}
                      className="w-full h-full object-cover"
                    />
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Full Photo Modal */}
        {selectedPhoto && (
          <div
            className="fixed inset-0 bg-black/90 flex items-center justify-center z-60"
            onClick={() => setSelectedPhoto(null)}
          >
            <div className="relative max-w-3xl max-h-[90vh] p-4">
              <img
                src={`data:image/jpeg;base64,${selectedPhoto.photo_base64}`}
                alt="Captured photo"
                className="max-w-full max-h-full rounded-lg"
              />
              <div className="absolute bottom-6 left-1/2 -translate-x-1/2 bg-black/70 text-white px-4 py-2 rounded-full text-sm">
                {selectedPhoto.camera_type === 'front' ? '📸 Front Camera' : '📷 Back Camera'} •{' '}
                {new Date(selectedPhoto.captured_at).toLocaleString()}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
