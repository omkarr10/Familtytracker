import { Location } from '../types/database'
import { format } from 'date-fns'
import { MapPin, Clock, Navigation, Activity } from 'lucide-react'
import clsx from 'clsx'
import { LocationAddress } from './LocationAddress'

interface TimelineProps {
  locations: Location[]
  onLocationClick?: (location: Location, index: number) => void
  currentIndex?: number
}

interface TimelineEvent {
  location: Location
  index: number
  type: 'start' | 'stop' | 'moving' | 'idle'
  duration?: number // minutes spent at this location
}

function detectStops(locations: Location[]): TimelineEvent[] {
  if (locations.length === 0) return []
  
  const events: TimelineEvent[] = []
  const STOP_THRESHOLD = 50 // meters - consider stopped if within this radius
  const MIN_STOP_DURATION = 5 // minutes
  
  let i = 0
  while (i < locations.length) {
    const current = locations[i]
    
    // Check if this is the start
    if (i === 0) {
      events.push({ location: current, index: i, type: 'start' })
      i++
      continue
    }
    
    // Check if device stopped here (consecutive locations within threshold)
    let stopEnd = i
    for (let j = i + 1; j < locations.length; j++) {
      const distance = getDistance(current, locations[j])
      if (distance < STOP_THRESHOLD) {
        stopEnd = j
      } else {
        break
      }
    }
    
    if (stopEnd > i) {
      const startTime = new Date(locations[i].created_at)
      const endTime = new Date(locations[stopEnd].created_at)
      const duration = Math.round((endTime.getTime() - startTime.getTime()) / 60000)
      
      if (duration >= MIN_STOP_DURATION) {
        events.push({
          location: current,
          index: i,
          type: 'stop',
          duration,
        })
      }
      i = stopEnd + 1
    } else {
      // Moving point - add occasionally to show route
      if (events.length === 0 || i - events[events.length - 1].index >= 10) {
        events.push({ location: current, index: i, type: 'moving' })
      }
      i++
    }
  }
  
  // Mark last location
  if (locations.length > 0) {
    const lastEvent = events[events.length - 1]
    if (lastEvent && lastEvent.index !== locations.length - 1) {
      events.push({
        location: locations[locations.length - 1],
        index: locations.length - 1,
        type: 'idle',
      })
    }
  }
  
  return events
}

function getDistance(loc1: Location, loc2: Location): number {
  const R = 6371000 // Earth's radius in meters
  const dLat = (loc2.latitude - loc1.latitude) * Math.PI / 180
  const dLon = (loc2.longitude - loc1.longitude) * Math.PI / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(loc1.latitude * Math.PI / 180) * Math.cos(loc2.latitude * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

function calculateTotalDistance(locations: Location[]): number {
  let total = 0
  for (let i = 1; i < locations.length; i++) {
    total += getDistance(locations[i - 1], locations[i])
  }
  return total
}

const eventIcons = {
  start: <Navigation className="w-4 h-4 text-green-500" />,
  stop: <MapPin className="w-4 h-4 text-red-500" />,
  moving: <Activity className="w-4 h-4 text-blue-500" />,
  idle: <Clock className="w-4 h-4 text-gray-500" />,
}

const eventColors = {
  start: 'border-green-500 bg-green-50 dark:bg-green-900/20',
  stop: 'border-red-500 bg-red-50 dark:bg-red-900/20',
  moving: 'border-blue-500 bg-blue-50 dark:bg-blue-900/20',
  idle: 'border-gray-500 bg-gray-50 dark:bg-gray-900/20',
}

const eventLabels = {
  start: 'Started',
  stop: 'Stopped',
  moving: 'Moving',
  idle: 'Idle',
}

export function Timeline({ locations, onLocationClick, currentIndex }: TimelineProps) {
  const events = detectStops(locations)
  const totalDistance = calculateTotalDistance(locations)
  
  if (locations.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500 dark:text-dark-400">
        <Clock className="w-12 h-12 mx-auto mb-2 opacity-50" />
        <p>No location data for this day</p>
      </div>
    )
  }
  
  return (
    <div className="space-y-4">
      {/* Summary Stats */}
      <div className="grid grid-cols-3 gap-3 mb-4">
        <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3">
          <p className="text-xs text-blue-600 dark:text-blue-400 font-medium">Total Distance</p>
          <p className="text-lg font-bold text-blue-700 dark:text-blue-300">
            {totalDistance >= 1000
              ? `${(totalDistance / 1000).toFixed(1)} km`
              : `${Math.round(totalDistance)} m`}
          </p>
        </div>
        <div className="bg-green-50 dark:bg-green-900/20 rounded-lg p-3">
          <p className="text-xs text-green-600 dark:text-green-400 font-medium">Points</p>
          <p className="text-lg font-bold text-green-700 dark:text-green-300">{locations.length}</p>
        </div>
        <div className="bg-purple-50 dark:bg-purple-900/20 rounded-lg p-3">
          <p className="text-xs text-purple-600 dark:text-purple-400 font-medium">Stops</p>
          <p className="text-lg font-bold text-purple-700 dark:text-purple-300">
            {events.filter((e) => e.type === 'stop').length}
          </p>
        </div>
      </div>
      
      {/* Timeline */}
      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gray-200 dark:bg-dark-600" />
        
        <div className="space-y-3">
          {events.map((event, i) => (
            <div
              key={`${event.index}-${i}`}
              onClick={() => onLocationClick?.(event.location, event.index)}
              className={clsx(
                'relative pl-10 cursor-pointer transition-all duration-200',
                currentIndex === event.index && 'scale-[1.02]'
              )}
            >
              {/* Dot on line */}
              <div
                className={clsx(
                  'absolute left-2 w-5 h-5 rounded-full flex items-center justify-center',
                  'border-2 bg-white dark:bg-dark-800 transition-all',
                  currentIndex === event.index
                    ? 'ring-2 ring-primary-500 ring-offset-2 dark:ring-offset-dark-800'
                    : '',
                  event.type === 'start' && 'border-green-500',
                  event.type === 'stop' && 'border-red-500',
                  event.type === 'moving' && 'border-blue-500',
                  event.type === 'idle' && 'border-gray-500'
                )}
              >
                {eventIcons[event.type]}
              </div>
              
              {/* Event Card */}
              <div
                className={clsx(
                  'p-3 rounded-lg border-l-4 transition-colors',
                  eventColors[event.type],
                  currentIndex === event.index && 'ring-2 ring-primary-500'
                )}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium text-gray-800 dark:text-white">
                    {eventLabels[event.type]}
                  </span>
                  <span className="text-sm text-gray-500 dark:text-dark-400">
                    {format(new Date(event.location.created_at), 'HH:mm')}
                  </span>
                </div>
                
                <LocationAddress
                  latitude={event.location.latitude}
                  longitude={event.location.longitude}
                  className="text-sm"
                />
                
                {event.duration && (
                  <div className="mt-1 flex items-center gap-1 text-sm text-red-600 dark:text-red-400">
                    <Clock className="w-3 h-3" />
                    <span>
                      {event.duration >= 60
                        ? `${Math.floor(event.duration / 60)}h ${event.duration % 60}m`
                        : `${event.duration} min`}
                    </span>
                  </div>
                )}
                
                {event.location.speed !== null && event.location.speed > 0 && (
                  <p className="text-xs text-gray-500 dark:text-dark-400 mt-1">
                    Speed: {Math.round(event.location.speed * 3.6)} km/h
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
