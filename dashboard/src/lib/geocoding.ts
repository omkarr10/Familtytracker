// Reverse geocoding using OpenStreetMap Nominatim API (free, no API key required)

interface GeocodingResult {
  address: string
  shortAddress: string
  city?: string
  state?: string
  country?: string
}

// Cache to avoid repeated API calls for same coordinates
const cache = new Map<string, GeocodingResult>()

// Round coordinates to reduce cache misses for nearby locations
const roundCoord = (coord: number, precision = 4) => 
  Math.round(coord * Math.pow(10, precision)) / Math.pow(10, precision)

export async function reverseGeocode(
  latitude: number,
  longitude: number
): Promise<GeocodingResult | null> {
  const roundedLat = roundCoord(latitude)
  const roundedLng = roundCoord(longitude)
  const cacheKey = `${roundedLat},${roundedLng}`

  // Check cache first
  if (cache.has(cacheKey)) {
    return cache.get(cacheKey)!
  }

  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1`,
      {
        headers: {
          'Accept-Language': 'en',
          'User-Agent': 'FamilyTracker/1.0',
        },
      }
    )

    if (!response.ok) {
      console.warn('Geocoding API error:', response.status)
      return null
    }

    const data = await response.json()
    
    if (!data.address) {
      return null
    }

    const addr = data.address
    
    // Build short address (road/locality + city)
    const road = addr.road || addr.pedestrian || addr.street || addr.locality || ''
    const suburb = addr.suburb || addr.neighbourhood || addr.quarter || ''
    const city = addr.city || addr.town || addr.village || addr.municipality || ''
    const state = addr.state || addr.region || ''
    const country = addr.country || ''

    // Create a concise short address
    let shortAddress = ''
    if (road) {
      shortAddress = road
      if (suburb) shortAddress += `, ${suburb}`
    } else if (suburb) {
      shortAddress = suburb
    }
    if (city && shortAddress) {
      shortAddress += `, ${city}`
    } else if (city) {
      shortAddress = city
    }
    
    // Fallback to display name if we couldn't build a short address
    if (!shortAddress) {
      shortAddress = data.display_name?.split(',').slice(0, 2).join(',') || 'Unknown location'
    }

    const result: GeocodingResult = {
      address: data.display_name || 'Unknown location',
      shortAddress,
      city,
      state,
      country,
    }

    // Store in cache
    cache.set(cacheKey, result)
    
    return result
  } catch (error) {
    console.error('Geocoding error:', error)
    return null
  }
}

// Hook for using geocoding in components
import { useState, useEffect } from 'react'

export function useReverseGeocode(latitude: number | null, longitude: number | null) {
  const [address, setAddress] = useState<GeocodingResult | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (latitude === null || longitude === null) {
      setAddress(null)
      return
    }

    setLoading(true)
    reverseGeocode(latitude, longitude)
      .then(result => {
        setAddress(result)
        setLoading(false)
      })
      .catch(() => {
        setLoading(false)
      })
  }, [latitude, longitude])

  return { address, loading }
}

// Batch geocode multiple locations with rate limiting
export async function batchReverseGeocode(
  locations: Array<{ lat: number; lng: number; id: string }>
): Promise<Map<string, GeocodingResult>> {
  const results = new Map<string, GeocodingResult>()
  
  // Process in batches of 3 with delay to respect rate limits
  for (let i = 0; i < locations.length; i++) {
    const { lat, lng, id } = locations[i]
    const result = await reverseGeocode(lat, lng)
    if (result) {
      results.set(id, result)
    }
    
    // Add delay between requests to avoid rate limiting (1 request per second)
    if (i < locations.length - 1) {
      await new Promise(resolve => setTimeout(resolve, 1000))
    }
  }
  
  return results
}
