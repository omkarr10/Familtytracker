import { useEffect, useState, useRef } from 'react'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { useAppStore } from '../store/appStore'
import { useSettingsStore } from '../store/settingsStore'
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
import { SkeletonAlert } from '../components/Skeleton'
import { toast } from '../components/Toast'

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
  sos: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400',
  low_battery: 'bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400',
  sim_change: 'bg-purple-100 text-purple-600 dark:bg-purple-900/30 dark:text-purple-400',
  geofence_enter: 'bg-green-100 text-green-600 dark:bg-green-900/30 dark:text-green-400',
  geofence_exit: 'bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-400',
  default: 'bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400',
}

export default function Alerts() {
  const { user } = useAuthStore()
  const { setAlerts: setGlobalAlerts } = useAppStore()
  const { soundAlerts } = useSettingsStore()
  const [alerts, setAlerts] = useState<AlertWithDevice[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<'all' | 'unread'>('all')
  const notifiedAlertsRef = useRef<Set<string>>(new Set())

  useEffect(() => {
    if (!user) return
    fetchData()
  }, [user])

  const fetchData = async () => {
    try {
      // Fetch devices first
      const { data: devicesData } = await api.from('devices').select('*', {
        eq: ['user_id', user!.id]
      })

      const devices: Device[] = devicesData || []

      // Fetch alerts for each device
      if (devices.length > 0) {
        const allAlerts: Alert[] = []
        for (const device of devices) {
          const { data: alertsData } = await api.from('alerts').select('*', {
            eq: ['device_id', device.id],
            order: ['created_at', false],
            limit: 50
          })
          if (alertsData) allAlerts.push(...alertsData)
        }

        // Sort by created_at descending
        allAlerts.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime())

        const alertsWithDevices = allAlerts.map((alert) => ({
          ...alert,
          device: devices.find((d) => d.id === alert.device_id),
        }))
        setAlerts(alertsWithDevices)
        setGlobalAlerts(allAlerts)

        // Notify for new unread SOS alerts
        if (soundAlerts) {
          alertsWithDevices.forEach((alert) => {
            if (
              !alert.is_read &&
              alert.alert_type === 'sos' &&
              !notifiedAlertsRef.current.has(alert.id)
            ) {
              notifiedAlertsRef.current.add(alert.id)
              toast.sos(alert.device?.device_name || 'Unknown Device', alert.message || undefined)
            } else if (
              !alert.is_read &&
              (alert.alert_type === 'geofence_enter' || alert.alert_type === 'geofence_exit') &&
              !notifiedAlertsRef.current.has(alert.id)
            ) {
              notifiedAlertsRef.current.add(alert.id)
              toast.geofence(
                alert.device?.device_name || 'Device',
                'Geofence',
                alert.alert_type === 'geofence_enter' ? 'entered' : 'exited'
              )
            }
          })
        }
      }
    } catch (err) {
      console.error('Error fetching alerts:', err)
    }

    setLoading(false)
  }

  const markAsRead = async (alertId: string) => {
    try {
      await api.from('alerts').update({ is_read: true }, ['id', alertId])
      setAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, is_read: true } : a))
      )
    } catch (err) {
      console.error('Error marking alert as read:', err)
    }
  }

  const markAllAsRead = async () => {
    const unreadIds = alerts.filter((a) => !a.is_read).map((a) => a.id)
    if (unreadIds.length === 0) return

    try {
      // Mark each alert as read individually through proxy
      for (const id of unreadIds) {
        await api.from('alerts').update({ is_read: true }, ['id', id])
      }
      setAlerts((prev) => prev.map((a) => ({ ...a, is_read: true })))
    } catch (err) {
      console.error('Error marking all as read:', err)
    }
  }

  const filteredAlerts = filter === 'unread' ? alerts.filter((a) => !a.is_read) : alerts
  const unreadCount = alerts.filter((a) => !a.is_read).length

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="h-8 w-20 skeleton rounded" />
        </div>
        <div className="space-y-3">
          <SkeletonAlert />
          <SkeletonAlert />
          <SkeletonAlert />
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Alerts</h1>
          {unreadCount > 0 && (
            <span className="badge-danger">
              {unreadCount} unread
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllAsRead}
            className="btn-ghost flex items-center gap-2"
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
            'px-4 py-2 rounded-lg transition font-medium',
            filter === 'all'
              ? 'bg-primary-600 text-white'
              : 'bg-white dark:bg-dark-800 text-gray-600 dark:text-dark-300 hover:bg-gray-50 dark:hover:bg-dark-700 border border-gray-200 dark:border-dark-700'
          )}
        >
          All
        </button>
        <button
          onClick={() => setFilter('unread')}
          className={clsx(
            'px-4 py-2 rounded-lg transition font-medium',
            filter === 'unread'
              ? 'bg-primary-600 text-white'
              : 'bg-white dark:bg-dark-800 text-gray-600 dark:text-dark-300 hover:bg-gray-50 dark:hover:bg-dark-700 border border-gray-200 dark:border-dark-700'
          )}
        >
          Unread
        </button>
      </div>

      {/* Alerts List */}
      {filteredAlerts.length === 0 ? (
        <div className="card p-12 text-center">
          <Bell className="w-16 h-16 text-gray-300 dark:text-dark-600 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-gray-700 dark:text-dark-200 mb-2">
            {filter === 'unread' ? 'No unread alerts' : 'No alerts yet'}
          </h2>
          <p className="text-gray-500 dark:text-dark-400">
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
                  'card p-4 transition',
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
                        <h3 className="font-semibold text-gray-800 dark:text-white">
                          {alert.alert_type.replace('_', ' ').replace(/\b\w/g, (l) => l.toUpperCase())}
                        </h3>
                        <p className="text-sm text-gray-600 dark:text-dark-400">
                          {alert.device?.device_name || 'Unknown device'}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm text-gray-500 dark:text-dark-400">
                          {formatDistanceToNow(new Date(alert.created_at), { addSuffix: true })}
                        </span>
                        {!alert.is_read && (
                          <button
                            onClick={() => markAsRead(alert.id)}
                            className="btn-ghost p-1"
                            title="Mark as read"
                          >
                            <Check className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                    {alert.message && (
                      <p className="text-sm text-gray-600 dark:text-dark-300 mt-2">{alert.message}</p>
                    )}
                    {alert.latitude !== null && alert.longitude !== null && (
                      <a
                        href={`https://www.google.com/maps?q=${alert.latitude},${alert.longitude}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-sm text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 mt-2"
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
