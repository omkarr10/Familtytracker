import { Map, Satellite, Mountain, Moon } from 'lucide-react'
import { useSettingsStore, MapTheme } from '../store/settingsStore'
import clsx from 'clsx'

const themes: { id: MapTheme; label: string; icon: React.ReactNode }[] = [
  { id: 'standard', label: 'Standard', icon: <Map className="w-4 h-4" /> },
  { id: 'satellite', label: 'Satellite', icon: <Satellite className="w-4 h-4" /> },
  { id: 'terrain', label: 'Terrain', icon: <Mountain className="w-4 h-4" /> },
  { id: 'dark', label: 'Dark', icon: <Moon className="w-4 h-4" /> },
]

export function MapThemeSelector() {
  const { mapTheme, setMapTheme } = useSettingsStore()

  return (
    <div className="absolute top-4 right-4 z-[1000] bg-white dark:bg-dark-800 rounded-lg shadow-lg border border-gray-200 dark:border-dark-600 p-1 flex gap-1">
      {themes.map((theme) => (
        <button
          key={theme.id}
          onClick={() => setMapTheme(theme.id)}
          title={theme.label}
          className={clsx(
            'p-2 rounded-md transition-all duration-200',
            mapTheme === theme.id
              ? 'bg-primary-500 text-white shadow-md'
              : 'text-gray-600 dark:text-dark-300 hover:bg-gray-100 dark:hover:bg-dark-700'
          )}
        >
          {theme.icon}
        </button>
      ))}
    </div>
  )
}
