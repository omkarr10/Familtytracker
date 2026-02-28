import { useEffect, useState } from 'react'
import { supabase } from '../lib/supabase'
import { useAuthStore } from '../store/authStore'
import { useAppStore } from '../store/appStore'
import { Alert, Device } from '../types/database'
import { formatDistanceToNow } from 'date-fns'
import {
  Bell,
  AlertTriangle,
  MapPin,
  Battery,
  Smartphone,
  Shield,
  Check,
  CheckCheck,
} from 'lucide-react'
import clsx from 'clsx'

interface AlertWithDevice extends Alert {
  device?: Device
}

const alertIcons: Record<string, typeof AlertTriangle> = {
  sos: AlertTriangle,
  low_battery: Battery,
  sim_change: Smartphone,
  geofence_enter: Shield,
  geofence_exit: Shield,
  default: Bell,
}

const alertColors: Record<string, string> = {
  sos: 'bg-red-100 text-red-600',
  low_battery: 'bg-orange-100 text-orange-600',
  sim_change: 'bg-purple-100 text-purple-600',
  geofence_enter: 'bg-green-100 text-green-600',
  geofence_exit: 'bg-yellow-100 text-yellow-600',
  default: 'bg-blue-100 text-blue-600',
}

export default function Alerts() {
  const { user } = useAuthStore()
  const { setAlerts: setGlobalAlerts } = useAppStore()
  const [alerts, setAlerts] = useState<AlertWithDevice[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<'all' | 'unread'>('all')

  useEffect(() => {
    if (!user) return
    fetchData()
  }, [user])

  const fetchData = async () => {
    // Fetch devices first
    const { data: devicesData } = await supabase
      .from('devices')
      .select('*')
      .eq('user_id', user!.id)

    const devices: Device[] = devicesData || []

    // Fetch alerts
    if (devices.length > 0) {
      const deviceIds = devices.map((d) => d.id)
      const { data: alertsData, error } = await supabase
        .from('alerts')
        .select('*')
        .in('device_id', deviceIds)
        .order('created_at', { ascending: false })
        .limit(100)

      if (error) {
        console.error('Error fetching alerts:', error)
      } else {
        const alertsWithDevices = (alertsData || []).map((alert) => ({
          ...alert,
          device: devices.find((d) => d.id === alert.device_id),
        }))
        setAlerts(alertsWithDevices)
        setGlobalAlerts(alertsData || [])
      }
    }

    setLoading(false)
  }

  const markAsRead = async (alertId: string) => {
    const { error } = await supabase
      .from('alerts')
      .update({ is_read: true })
      .eq('id', alertId)

    if (!error) {
      setAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, is_read: true } : a))
      )
    }
  }

  const markAllAsRead = async () => {
    const unreadIds = alerts.filter((a) => !a.is_read).map((a) => a.id)
    if (unreadIds.length === 0) return

    const { error } = await supabase
      .from('alerts')
      .update({ is_read: true })
      .in('id', unreadIds)

    if (!error) {
      setAlerts((prev) => prev.map((a) => ({ ...a, is_read: true })))
    }
  }

  const filteredAlerts = filter === 'unread' ? alerts.filter((a) => !a.is_read) : alerts
  const unreadCount = alerts.filter((a) => !a.is_read).length

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
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-gray-800">Alerts</h1>
          {unreadCount > 0 && (
            <span className="px-3 py-1 bg-red-500 text-white text-sm rounded-full">
              {unreadCount} unread
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllAsRead}
            className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition"
          >
            <CheckCheck className="w-5 h-5" />
            Mark all as read
          </button>
        )}
      </div>

      {/* Filter */}
      <div className="flex gap-2">
        <button
          onClick={() => setFilter('all')}
          className={clsx(
            'px-4 py-2 rounded-lg transition',
            filter === 'all'
              ? 'bg-primary-600 text-white'
              : 'bg-white text-gray-600 hover:bg-gray-50'
          )}
        >
          All
        </button>
        <button
          onClick={() => setFilter('unread')}
          className={clsx(
            'px-4 py-2 rounded-lg transition',
            filter === 'unread'
              ? 'bg-primary-600 text-white'
              : 'bg-white text-gray-600 hover:bg-gray-50'
          )}
        >
          Unread
        </button>
      </div>

      {/* Alerts List */}
      {filteredAlerts.length === 0 ? (
        <div className="bg-white rounded-lg p-12 text-center">
          <Bell className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 mb-2">
            {filter === 'unread' ? 'No unread alerts' : 'No alerts yet'}
          </h2>
          <p className="text-gray-500">
            Alerts will appear here when triggered by device events
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredAlerts.map((alert) => {
            const Icon = alertIcons[alert.alert_type] || alertIcons.default
            const colorClass = alertColors[alert.alert_type] || alertColors.default

            return (
              <div
                key={alert.id}
                className={clsx(
                  'bg-white rounded-lg p-4 shadow-sm transition',
                  !alert.is_read && 'border-l-4 border-primary-500'
                )}
              >
                <div className="flex items-start gap-4">
                  <div className={clsx('w-10 h-10 rounded-full flex items-center justify-center', colorClass)}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="font-semibold text-gray-800">
                          {alert.alert_type.replace('_', ' ').replace(/\b\w/g, (l) => l.toUpperCase())}
                        </h3>
                        <p className="text-sm text-gray-600">
                          {alert.device?.device_name || 'Unknown device'}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm text-gray-500">
                          {formatDistanceToNow(new Date(alert.created_at), { addSuffix: true })}
                        </span>
                        {!alert.is_read && (
                          <button
                            onClick={() => markAsRead(alert.id)}
                            className="p-1 text-gray-400 hover:text-green-500 hover:bg-green-50 rounded transition"
                            title="Mark as read"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                    {alert.message && (
                      <p className="text-sm text-gray-600 mt-2">{alert.message}</p>
                    )}
                    {alert.latitude !== null && alert.longitude !== null && (
                      <a
                        href={`https://www.google.com/maps?q=${alert.latitude},${alert.longitude}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-sm text-primary-600 hover:text-primary-700 mt-2"
                      >
                        <MapPin className="w-4 h-4" />
                        View location
                      </a>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
