import { useEffect, useState, useCallback } from 'react'
import { create } from 'zustand'
import { AlertTriangle, CheckCircle, Info, X, AlertCircle, Battery, MapPin } from 'lucide-react'
import clsx from 'clsx'

export type ToastType = 'success' | 'error' | 'warning' | 'info' | 'sos' | 'battery' | 'geofence'

interface Toast {
  id: string
  type: ToastType
  title: string
  message?: string
  duration?: number
  sound?: boolean
}

interface ToastStore {
  toasts: Toast[]
  addToast: (toast: Omit<Toast, 'id'>) => void
  removeToast: (id: string) => void
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  addToast: (toast) => {
    const id = crypto.randomUUID()
    set((state) => ({
      toasts: [...state.toasts, { ...toast, id }],
    }))
    
    // Play sound for critical alerts
    if (toast.sound && (toast.type === 'sos' || toast.type === 'warning')) {
      playAlertSound(toast.type)
    }
  },
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    })),
}))

// Play alert sounds
function playAlertSound(type: ToastType) {
  const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()
  const oscillator = audioContext.createOscillator()
  const gainNode = audioContext.createGain()

  oscillator.connect(gainNode)
  gainNode.connect(audioContext.destination)

  if (type === 'sos') {
    // Urgent SOS sound - high pitched beeps
    oscillator.frequency.value = 1000
    oscillator.type = 'square'
    gainNode.gain.value = 0.3

    oscillator.start()
    setTimeout(() => oscillator.stop(), 200)
    setTimeout(() => {
      const osc2 = audioContext.createOscillator()
      osc2.connect(gainNode)
      osc2.frequency.value = 1000
      osc2.type = 'square'
      osc2.start()
      setTimeout(() => osc2.stop(), 200)
    }, 300)
    setTimeout(() => {
      const osc3 = audioContext.createOscillator()
      osc3.connect(gainNode)
      osc3.frequency.value = 1000
      osc3.type = 'square'
      osc3.start()
      setTimeout(() => osc3.stop(), 200)
    }, 600)
  } else {
    // Regular alert sound
    oscillator.frequency.value = 600
    oscillator.type = 'sine'
    gainNode.gain.value = 0.2
    oscillator.start()
    setTimeout(() => oscillator.stop(), 150)
  }
}

// Helper function to show toasts easily
export const toast = {
  success: (title: string, message?: string) =>
    useToastStore.getState().addToast({ type: 'success', title, message, duration: 4000 }),
  error: (title: string, message?: string) =>
    useToastStore.getState().addToast({ type: 'error', title, message, duration: 6000 }),
  warning: (title: string, message?: string, sound = true) =>
    useToastStore.getState().addToast({ type: 'warning', title, message, duration: 5000, sound }),
  info: (title: string, message?: string) =>
    useToastStore.getState().addToast({ type: 'info', title, message, duration: 4000 }),
  sos: (deviceName: string, message?: string) =>
    useToastStore.getState().addToast({
      type: 'sos',
      title: `🚨 SOS Alert - ${deviceName}`,
      message: message || 'Emergency alert triggered!',
      duration: 0, // Don't auto-dismiss SOS
      sound: true,
    }),
  battery: (deviceName: string, level: number) =>
    useToastStore.getState().addToast({
      type: 'battery',
      title: `Low Battery - ${deviceName}`,
      message: `Battery at ${level}%`,
      duration: 8000,
      sound: true,
    }),
  geofence: (deviceName: string, geofenceName: string, action: 'entered' | 'exited') =>
    useToastStore.getState().addToast({
      type: 'geofence',
      title: `Geofence ${action === 'entered' ? 'Entry' : 'Exit'}`,
      message: `${deviceName} ${action} ${geofenceName}`,
      duration: 6000,
      sound: true,
    }),
}

const iconMap: Record<ToastType, React.ReactNode> = {
  success: <CheckCircle className="w-5 h-5" />,
  error: <AlertCircle className="w-5 h-5" />,
  warning: <AlertTriangle className="w-5 h-5" />,
  info: <Info className="w-5 h-5" />,
  sos: <AlertTriangle className="w-6 h-6 animate-pulse" />,
  battery: <Battery className="w-5 h-5" />,
  geofence: <MapPin className="w-5 h-5" />,
}

const styleMap: Record<ToastType, string> = {
  success: 'bg-green-50 dark:bg-green-900/30 border-green-500 text-green-800 dark:text-green-200',
  error: 'bg-red-50 dark:bg-red-900/30 border-red-500 text-red-800 dark:text-red-200',
  warning: 'bg-amber-50 dark:bg-amber-900/30 border-amber-500 text-amber-800 dark:text-amber-200',
  info: 'bg-blue-50 dark:bg-blue-900/30 border-blue-500 text-blue-800 dark:text-blue-200',
  sos: 'bg-red-100 dark:bg-red-900/50 border-red-600 text-red-900 dark:text-red-100 animate-pulse ring-2 ring-red-500',
  battery: 'bg-orange-50 dark:bg-orange-900/30 border-orange-500 text-orange-800 dark:text-orange-200',
  geofence: 'bg-purple-50 dark:bg-purple-900/30 border-purple-500 text-purple-800 dark:text-purple-200',
}

function ToastItem({ toast: t, onClose }: { toast: Toast; onClose: () => void }) {
  useEffect(() => {
    if (t.duration && t.duration > 0) {
      const timer = setTimeout(onClose, t.duration)
      return () => clearTimeout(timer)
    }
  }, [t.duration, onClose])

  return (
    <div
      className={clsx(
        'flex items-start gap-3 p-4 rounded-lg border-l-4 shadow-lg backdrop-blur-sm',
        'transform transition-all duration-300 ease-out',
        'animate-in slide-in-from-right',
        styleMap[t.type]
      )}
    >
      <div className="flex-shrink-0">{iconMap[t.type]}</div>
      <div className="flex-1 min-w-0">
        <p className="font-semibold">{t.title}</p>
        {t.message && <p className="text-sm opacity-90 mt-0.5">{t.message}</p>}
      </div>
      <button
        onClick={onClose}
        className="flex-shrink-0 p-1 rounded hover:bg-black/10 dark:hover:bg-white/10 transition-colors"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  )
}

export function ToastContainer() {
  const { toasts, removeToast } = useToastStore()

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      {toasts.map((t) => (
        <div key={t.id} className="pointer-events-auto">
          <ToastItem toast={t} onClose={() => removeToast(t.id)} />
        </div>
      ))}
    </div>
  )
}
