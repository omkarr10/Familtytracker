// Service Worker for TrackIt PWA
const CACHE_NAME = 'family-tracker-v1'
const urlsToCache = [
  '/',
  '/index.html',
  '/manifest.json',
  '/logo.jpeg'
]

// Install event - cache resources
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(urlsToCache))
  )
  self.skipWaiting()
})

// Activate event - clean old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) => name !== CACHE_NAME)
          .map((name) => caches.delete(name))
      )
    })
  )
  self.clients.claim()
})

// Fetch event - serve from cache, fallback to network
self.addEventListener('fetch', (event) => {
  // Skip non-GET requests
  if (event.request.method !== 'GET') return

  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        // Return cached version or fetch from network
        return response || fetch(event.request)
      })
      .catch(() => {
        // Offline fallback
        if (event.request.mode === 'navigate') {
          return caches.match('/')
        }
      })
  )
})

// Push notification event
self.addEventListener('push', (event) => {
  const data = event.data?.json() ?? {}
  
  const options = {
    body: data.message || 'New notification from TrackIt',
    icon: '/logo.jpeg',
    badge: '/logo.jpeg',
    vibrate: [200, 100, 200],
    tag: data.tag || 'default',
    renotify: true,
    data: {
      url: data.url || '/',
      type: data.type || 'default'
    },
    actions: [
      { action: 'view', title: 'View' },
      { action: 'dismiss', title: 'Dismiss' }
    ]
  }

  // For SOS alerts, use different vibration and urgency
  if (data.type === 'sos') {
    options.vibrate = [500, 200, 500, 200, 500]
    options.requireInteraction = true
    options.urgency = 'high'
  }

  event.waitUntil(
    self.registration.showNotification(data.title || 'TrackIt', options)
  )
})

// Notification click handler
self.addEventListener('notificationclick', (event) => {
  event.notification.close()

  if (event.action === 'dismiss') return

  const urlToOpen = event.notification.data?.url || '/'

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((clients) => {
        // Check if app is already open
        for (const client of clients) {
          if (client.url.includes(self.location.origin) && 'focus' in client) {
            client.navigate(urlToOpen)
            return client.focus()
          }
        }
        // If not open, open new window
        if (self.clients.openWindow) {
          return self.clients.openWindow(urlToOpen)
        }
      })
  )
})
