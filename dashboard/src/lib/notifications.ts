// Browser Notification Utilities

export type NotificationPermissionStatus = 'granted' | 'denied' | 'default' | 'unsupported'

interface NotificationOptions {
  title: string
  body?: string
  icon?: string
  tag?: string
  requireInteraction?: boolean
  vibrate?: number[]
  data?: Record<string, unknown>
}

class NotificationService {
  private swRegistration: ServiceWorkerRegistration | null = null

  async init(): Promise<NotificationPermissionStatus> {
    // Check if notifications are supported
    if (!('Notification' in window)) {
      console.log('This browser does not support notifications')
      return 'unsupported'
    }

    // Register service worker
    if ('serviceWorker' in navigator) {
      try {
        this.swRegistration = await navigator.serviceWorker.register('/sw.js')
        console.log('Service Worker registered:', this.swRegistration)
      } catch (error) {
        console.error('Service Worker registration failed:', error)
      }
    }

    return Notification.permission as NotificationPermissionStatus
  }

  async requestPermission(): Promise<NotificationPermissionStatus> {
    if (!('Notification' in window)) {
      return 'unsupported'
    }

    const permission = await Notification.requestPermission()
    return permission as NotificationPermissionStatus
  }

  getPermission(): NotificationPermissionStatus {
    if (!('Notification' in window)) {
      return 'unsupported'
    }
    return Notification.permission as NotificationPermissionStatus
  }

  async showNotification(options: NotificationOptions): Promise<void> {
    const permission = this.getPermission()
    
    if (permission !== 'granted') {
      console.log('Notification permission not granted')
      return
    }

    // Use service worker notification if available (works in background)
    if (this.swRegistration) {
      await this.swRegistration.showNotification(options.title, {
        body: options.body,
        icon: options.icon || '/logo.jpeg',
        tag: options.tag,
        requireInteraction: options.requireInteraction,
        vibrate: options.vibrate,
        data: options.data,
      })
    } else {
      // Fallback to regular notification
      new Notification(options.title, {
        body: options.body,
        icon: options.icon || '/logo.jpeg',
        tag: options.tag,
        requireInteraction: options.requireInteraction,
      })
    }
  }

  // Convenience methods for different alert types
  async sosAlert(deviceName: string, message?: string): Promise<void> {
    await this.showNotification({
      title: `🚨 SOS Alert - ${deviceName}`,
      body: message || 'Emergency alert triggered!',
      tag: 'sos-alert',
      requireInteraction: true,
      vibrate: [500, 200, 500, 200, 500],
      data: { type: 'sos', url: '/alerts' },
    })
  }

  async lowBatteryAlert(deviceName: string, level: number): Promise<void> {
    await this.showNotification({
      title: `🔋 Low Battery - ${deviceName}`,
      body: `Battery at ${level}%`,
      tag: `battery-${deviceName}`,
      data: { type: 'battery', url: '/' },
    })
  }

  async geofenceAlert(
    deviceName: string,
    geofenceName: string,
    action: 'entered' | 'exited'
  ): Promise<void> {
    await this.showNotification({
      title: `📍 Geofence ${action === 'entered' ? 'Entry' : 'Exit'}`,
      body: `${deviceName} ${action} ${geofenceName}`,
      tag: `geofence-${geofenceName}`,
      data: { type: 'geofence', url: '/geofences' },
    })
  }
}

export const notificationService = new NotificationService()
