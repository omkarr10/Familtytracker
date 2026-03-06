import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { sessionStore } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { useAppStore } from '../store/appStore'
import { useThemeStore, applyTheme } from '../store/themeStore'
import {
  MapPin,
  Smartphone,
  History,
  Shield,
  Bell,
  Settings,
  LogOut,
  Menu,
  X,
  Sun,
  Moon,
  Monitor,
} from 'lucide-react'
import { useState } from 'react'
import clsx from 'clsx'

const navItems = [
  { to: '/dashboard', icon: MapPin, label: 'Live Tracking' },
  { to: '/dashboard/devices', icon: Smartphone, label: 'Devices' },
  { to: '/dashboard/history', icon: History, label: 'History' },
  { to: '/dashboard/geofences', icon: Shield, label: 'Geofences' },
  { to: '/dashboard/alerts', icon: Bell, label: 'Alerts' },
  { to: '/dashboard/settings', icon: Settings, label: 'Settings' },
]

function ThemeToggle() {
  const { theme, setTheme } = useThemeStore()
  const [showMenu, setShowMenu] = useState(false)
  
  const themes = [
    { value: 'light', icon: Sun, label: 'Light' },
    { value: 'dark', icon: Moon, label: 'Dark' },
    { value: 'system', icon: Monitor, label: 'System' },
  ] as const
  
  const currentTheme = themes.find(t => t.value === theme) || themes[2]
  const CurrentIcon = currentTheme.icon
  
  return (
    <div className="relative">
      <button
        onClick={() => setShowMenu(!showMenu)}
        className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-dark-700 transition-colors"
        title={`Theme: ${currentTheme.label}`}
      >
        <CurrentIcon className="w-5 h-5 text-gray-600 dark:text-gray-300" />
      </button>
      
      {showMenu && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setShowMenu(false)} />
          <div className="absolute right-0 mt-2 w-36 card shadow-lg z-50 py-1">
            {themes.map(({ value, icon: Icon, label }) => (
              <button
                key={value}
                onClick={() => {
                  setTheme(value)
                  applyTheme(value)
                  setShowMenu(false)
                }}
                className={clsx(
                  'flex items-center gap-2 w-full px-3 py-2 text-sm transition-colors',
                  theme === value
                    ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-dark-700'
                )}
              >
                <Icon className="w-4 h-4" />
                {label}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

// Bottom nav items for mobile (fewer items, most important)
const bottomNavItems = [
  { to: '/dashboard', icon: MapPin, label: 'Track' },
  { to: '/dashboard/devices', icon: Smartphone, label: 'Devices' },
  { to: '/dashboard/alerts', icon: Bell, label: 'Alerts' },
  { to: '/dashboard/settings', icon: Settings, label: 'Settings' },
]

export default function Layout() {
  const navigate = useNavigate()
  const { setUser } = useAuthStore()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const { alerts } = useAppStore()
  const unreadCount = alerts.filter((a) => !a.is_read).length

  const handleLogout = () => {
    sessionStore.clear()
    setUser(null)
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-dark-950 pb-16 lg:pb-0">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={clsx(
          'fixed top-0 left-0 z-50 h-full w-64 bg-white dark:bg-dark-900 shadow-xl transform transition-transform duration-300 lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center justify-between p-4 border-b border-gray-100 dark:border-dark-700">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-lg shadow-primary-500/30">
              <MapPin className="w-5 h-5 text-white" />
            </div>
            <span className="text-lg font-bold text-gray-800 dark:text-white">TrackIt</span>
          </div>
          <button
            className="lg:hidden p-2 hover:bg-gray-100 dark:hover:bg-dark-700 rounded-lg transition-colors"
            onClick={() => setSidebarOpen(false)}
          >
            <X className="w-5 h-5 text-gray-600 dark:text-gray-300" />
          </button>
        </div>

        <nav className="p-3 space-y-1">
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200',
                  isActive
                    ? 'bg-primary-500 text-white shadow-lg shadow-primary-500/30'
                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-dark-800 hover:text-gray-900 dark:hover:text-white'
                )
              }
            >
              <Icon className="w-5 h-5" />
              <span className="font-medium">{label}</span>
              {label === 'Alerts' && unreadCount > 0 && (
                <span className="ml-auto bg-red-500 text-white text-xs px-2 py-0.5 rounded-full animate-pulse">
                  {unreadCount}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="absolute bottom-0 left-0 right-0 p-3 border-t border-gray-100 dark:border-dark-700">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-4 py-3 w-full text-gray-600 dark:text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-600 dark:hover:text-red-400 rounded-xl transition-all duration-200"
          >
            <LogOut className="w-5 h-5" />
            <span className="font-medium">Logout</span>
          </button>
        </div>
      </aside>

      {/* Main content */}
      <div className="lg:ml-64">
        {/* Top bar */}
        <header className="bg-white/80 dark:bg-dark-900/80 backdrop-blur-xl border-b border-gray-100 dark:border-dark-700 sticky top-0 z-30">
          <div className="flex items-center justify-between px-4 py-3">
            <button
              className="lg:hidden p-2 hover:bg-gray-100 dark:hover:bg-dark-700 rounded-lg transition-colors"
              onClick={() => setSidebarOpen(true)}
            >
              <Menu className="w-6 h-6 text-gray-700 dark:text-gray-300" />
            </button>
            
            <div className="flex items-center gap-4 ml-auto">
              <div className="flex items-center gap-2 px-3 py-1.5 bg-green-50 dark:bg-green-900/30 rounded-full">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                </span>
                <span className="text-sm font-medium text-green-700 dark:text-green-400">Live</span>
              </div>
              
              <ThemeToggle />
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="p-3 sm:p-4 lg:p-6">
          <Outlet />
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white dark:bg-dark-900 border-t border-gray-200 dark:border-dark-700 z-40 lg:hidden safe-area-bottom">
        <div className="flex items-center justify-around h-16">
          {bottomNavItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/dashboard'}
              className={({ isActive }) =>
                clsx(
                  'flex flex-col items-center justify-center w-full h-full transition-colors relative',
                  isActive
                    ? 'text-primary-600 dark:text-primary-400'
                    : 'text-gray-500 dark:text-gray-400'
                )
              }
            >
              <Icon className="w-5 h-5" />
              <span className="text-xs mt-1 font-medium">{label}</span>
              {label === 'Alerts' && unreadCount > 0 && (
                <span className="absolute top-1 right-1/4 bg-red-500 text-white text-[10px] w-4 h-4 rounded-full flex items-center justify-center">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </NavLink>
          ))}
        </div>
      </nav>
    </div>
  )
}
