import { useState, useEffect } from 'react'
import { useAuthStore } from '../store/authStore'
import { useSettingsStore } from '../store/settingsStore'
import { api } from '../lib/api'
import { notificationService, NotificationPermissionStatus } from '../lib/notifications'
import { User, Lock, Shield, Info, Bell, Volume2, Battery, Map } from 'lucide-react'
import clsx from 'clsx'

export default function Settings() {
  const { user } = useAuthStore()
  const {
    soundAlerts,
    setSoundAlerts,
    batteryAlertThreshold,
    setBatteryAlertThreshold,
    mapTheme,
    setMapTheme,
  } = useSettingsStore()
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [notificationPermission, setNotificationPermission] = useState<NotificationPermissionStatus>('default')

  useEffect(() => {
    // Initialize notification service and get permission status
    notificationService.init().then(setNotificationPermission)
  }, [])

  const requestNotificationPermission = async () => {
    const permission = await notificationService.requestPermission()
    setNotificationPermission(permission)
  }

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (newPassword !== confirmPassword) {
      setMessage({ type: 'error', text: 'Passwords do not match' })
      return
    }

    if (newPassword.length < 6) {
      setMessage({ type: 'error', text: 'Password must be at least 6 characters' })
      return
    }

    setLoading(true)
    setMessage(null)

    try {
      await api.auth.updatePassword(newPassword)
      setMessage({ type: 'success', text: 'Password updated successfully' })
      setNewPassword('')
      setConfirmPassword('')
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to update password' })
    }

    setLoading(false)
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Settings</h1>

      {/* Profile Section */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex items-center justify-center">
            <User className="w-5 h-5 text-primary-600 dark:text-primary-400" />
          </div>
          <h2 className="text-lg font-semibold text-gray-800 dark:text-white">Profile</h2>
        </div>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-dark-200 mb-1">
              Email
            </label>
            <input
              type="email"
              value={user?.email || ''}
              disabled
              className="input w-full bg-gray-50 dark:bg-dark-800 text-gray-500 dark:text-dark-400 cursor-not-allowed"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-dark-200 mb-1">
              Account ID
            </label>
            <input
              type="text"
              value={user?.id || ''}
              disabled
              className="input w-full bg-gray-50 dark:bg-dark-800 text-gray-500 dark:text-dark-400 font-mono text-sm cursor-not-allowed"
            />
          </div>
        </div>
      </div>

      {/* Change Password */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-orange-100 dark:bg-orange-900/30 rounded-lg flex items-center justify-center">
            <Lock className="w-5 h-5 text-orange-600 dark:text-orange-400" />
          </div>
          <h2 className="text-lg font-semibold text-gray-800 dark:text-white">Change Password</h2>
        </div>
        <form onSubmit={handlePasswordChange} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-dark-200 mb-1">
              New Password
            </label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="input w-full"
              placeholder="Enter new password"
              minLength={6}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-dark-200 mb-1">
              Confirm New Password
            </label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="input w-full"
              placeholder="Confirm new password"
              minLength={6}
            />
          </div>
          {message && (
            <div
              className={`p-3 rounded-lg text-sm ${
                message.type === 'success'
                  ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                  : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400'
              }`}
            >
              {message.text}
            </div>
          )}
          <button
            type="submit"
            disabled={loading || !newPassword || !confirmPassword}
            className="btn-primary"
          >
            {loading ? 'Updating...' : 'Update Password'}
          </button>
        </form>
      </div>

      {/* Notification Settings */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-purple-100 dark:bg-purple-900/30 rounded-lg flex items-center justify-center">
            <Bell className="w-5 h-5 text-purple-600 dark:text-purple-400" />
          </div>
          <h2 className="text-lg font-semibold text-gray-800 dark:text-white">Notifications</h2>
        </div>
        <div className="space-y-4">
          {/* Browser Notifications */}
          <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-dark-800 rounded-lg">
            <div>
              <p className="font-medium text-gray-800 dark:text-white">Push Notifications</p>
              <p className="text-sm text-gray-500 dark:text-dark-400">
                Get alerts even when the app is in background
              </p>
            </div>
            {notificationPermission === 'granted' ? (
              <span className="px-3 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 text-sm rounded-full">
                Enabled
              </span>
            ) : notificationPermission === 'denied' ? (
              <span className="px-3 py-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 text-sm rounded-full">
                Blocked
              </span>
            ) : notificationPermission === 'unsupported' ? (
              <span className="px-3 py-1 bg-gray-100 dark:bg-dark-700 text-gray-600 dark:text-dark-400 text-sm rounded-full">
                Not Supported
              </span>
            ) : (
              <button
                onClick={requestNotificationPermission}
                className="btn-primary text-sm px-3 py-1"
              >
                Enable
              </button>
            )}
          </div>

          {/* Sound Alerts */}
          <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-dark-800 rounded-lg">
            <div className="flex items-center gap-3">
              <Volume2 className="w-5 h-5 text-gray-500 dark:text-dark-400" />
              <div>
                <p className="font-medium text-gray-800 dark:text-white">Sound Alerts</p>
                <p className="text-sm text-gray-500 dark:text-dark-400">
                  Play sound for SOS and critical alerts
                </p>
              </div>
            </div>
            <button
              onClick={() => setSoundAlerts(!soundAlerts)}
              className={clsx(
                'relative w-12 h-6 rounded-full transition-colors',
                soundAlerts ? 'bg-primary-600' : 'bg-gray-300 dark:bg-dark-600'
              )}
            >
              <span
                className={clsx(
                  'absolute top-1 w-4 h-4 bg-white rounded-full transition-transform',
                  soundAlerts ? 'left-7' : 'left-1'
                )}
              />
            </button>
          </div>

          {/* Battery Alert Threshold */}
          <div className="p-3 bg-gray-50 dark:bg-dark-800 rounded-lg">
            <div className="flex items-center gap-3 mb-3">
              <Battery className="w-5 h-5 text-gray-500 dark:text-dark-400" />
              <div>
                <p className="font-medium text-gray-800 dark:text-white">Low Battery Alert</p>
                <p className="text-sm text-gray-500 dark:text-dark-400">
                  Alert when device battery falls below {batteryAlertThreshold}%
                </p>
              </div>
            </div>
            <input
              type="range"
              min="5"
              max="50"
              step="5"
              value={batteryAlertThreshold}
              onChange={(e) => setBatteryAlertThreshold(parseInt(e.target.value))}
              className="w-full accent-primary-600"
            />
            <div className="flex justify-between text-xs text-gray-500 dark:text-dark-400 mt-1">
              <span>5%</span>
              <span className="font-medium text-primary-600">{batteryAlertThreshold}%</span>
              <span>50%</span>
            </div>
          </div>
        </div>
      </div>

      {/* App Info */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 bg-green-100 dark:bg-green-900/30 rounded-lg flex items-center justify-center">
            <Shield className="w-5 h-5 text-green-600 dark:text-green-400" />
          </div>
          <h2 className="text-lg font-semibold text-gray-800 dark:text-white">Android App Setup</h2>
        </div>
        <div className="text-gray-600 dark:text-dark-300">
          <p>To start tracking a device:</p>
          <ol className="list-decimal list-inside space-y-2 mt-3">
            <li>Go to <strong className="text-gray-800 dark:text-white">Devices</strong> page and add a new device</li>
            <li>Copy the Device ID</li>
            <li>Install the TrackIt app on the Android phone</li>
            <li>Open the app and paste the Device ID</li>
            <li>Grant location permissions</li>
            <li>The device will start sending location updates!</li>
          </ol>
        </div>
      </div>

      {/* About */}
      <div className="card p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
            <Info className="w-5 h-5 text-blue-600 dark:text-blue-400" />
          </div>
          <h2 className="text-lg font-semibold text-gray-800 dark:text-white">About</h2>
        </div>
        <div className="text-sm text-gray-600 dark:text-dark-300 space-y-2">
          <p><strong className="text-gray-800 dark:text-white">TrackIt</strong> v1.0.0</p>
          <p>A secure family location tracking system.</p>
          <p className="text-gray-400 dark:text-dark-500">
            Built with React, Supabase, and Leaflet
          </p>
        </div>
      </div>
    </div>
  )
}
