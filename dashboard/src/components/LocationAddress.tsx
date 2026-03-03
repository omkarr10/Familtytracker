import { useReverseGeocode } from '../lib/geocoding'
import { MapPin } from 'lucide-react'

interface LocationAddressProps {
  latitude: number
  longitude: number
  className?: string
  showIcon?: boolean
}

export function LocationAddress({ latitude, longitude, className = '', showIcon = true }: LocationAddressProps) {
  const { address, loading } = useReverseGeocode(latitude, longitude)

  if (loading) {
    return (
      <div className={`flex items-center gap-1 ${className}`}>
        {showIcon && <MapPin className="w-3 h-3 text-gray-400 dark:text-dark-500" />}
        <span className="h-3 w-24 skeleton rounded" />
      </div>
    )
  }

  if (!address) {
    return (
      <div className={`flex items-center gap-1 ${className}`}>
        {showIcon && <MapPin className="w-3 h-3 text-gray-400 dark:text-dark-500" />}
        <span className="text-gray-500 dark:text-dark-400 text-xs">
          {latitude.toFixed(4)}, {longitude.toFixed(4)}
        </span>
      </div>
    )
  }

  return (
    <div className={`flex items-start gap-1 ${className}`} title={address.address}>
      {showIcon && <MapPin className="w-3 h-3 text-primary-500 flex-shrink-0 mt-0.5" />}
      <span className="text-gray-600 dark:text-dark-300 text-xs truncate">
        {address.shortAddress}
      </span>
    </div>
  )
}
