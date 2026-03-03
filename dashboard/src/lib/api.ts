// API client that uses Vercel proxy to bypass ISP blocking
const API_BASE = '/api'

// Session storage helper (defined first so it can be used by api)
export const sessionStore = {
  save: (session: any) => {
    if (session) {
      localStorage.setItem('ft_session', JSON.stringify(session))
      localStorage.setItem('ft_user', JSON.stringify(session.user))
    }
  },
  
  getSession: () => {
    const session = localStorage.getItem('ft_session')
    return session ? JSON.parse(session) : null
  },
  
  getUser: () => {
    const user = localStorage.getItem('ft_user')
    return user ? JSON.parse(user) : null
  },
  
  clear: () => {
    localStorage.removeItem('ft_session')
    localStorage.removeItem('ft_user')
  },
}

// Get auth headers with user's access token for RLS
function getAuthHeaders(): Record<string, string> {
  const session = sessionStore.getSession()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  if (session?.access_token) {
    headers['x-access-token'] = session.access_token
  }
  return headers
}

// Helper for fetch with timeout
async function fetchWithTimeout(url: string, options: RequestInit = {}, timeout = 15000) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeout)

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    })
    clearTimeout(timeoutId)
    return response
  } catch (err: any) {
    clearTimeout(timeoutId)
    if (err.name === 'AbortError') {
      throw new Error('Request timed out. Please try again.')
    }
    throw err
  }
}

// Auth functions
export const api = {
  auth: {
    signIn: async (email: string, password: string) => {
      const res = await fetchWithTimeout(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Login failed')
      return data
    },

    signUp: async (email: string, password: string) => {
      const res = await fetchWithTimeout(`${API_BASE}/auth/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Signup failed')
      return data
    },

    updatePassword: async (password: string) => {
      const session = sessionStore.getSession()
      if (!session?.access_token) {
        throw new Error('Not logged in')
      }
      
      const res = await fetchWithTimeout(`${API_BASE}/auth/update-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password, accessToken: session.access_token }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Password update failed')
      return data
    },
  },

  // Generic table operations via proxy
  from: (table: string) => ({
    select: async (columns = '*', options: { eq?: [string, any], order?: [string, boolean], limit?: number } = {}) => {
      let url = `${API_BASE}/db?table=${table}`
      if (columns && columns !== '*') {
        url += `&select=${encodeURIComponent(columns)}`
      }
      if (options.eq) {
        url += `&${options.eq[0]}=eq.${options.eq[1]}`
      }
      if (options.order) {
        url += `&order=${options.order[0]}.${options.order[1] ? 'asc' : 'desc'}`
      }
      if (options.limit) {
        url += `&limit=${options.limit}`
      }

      console.log('API call:', url)
      const res = await fetchWithTimeout(url, {
        headers: getAuthHeaders(),
      })
      const data = await res.json()
      if (!res.ok) {
        console.error('API error:', res.status, data)
        throw new Error(data.message || data.error || data.details || `API error: ${res.status}`)
      }
      return { data, error: null }
    },

    insert: async (record: any) => {
      const headers = getAuthHeaders()
      headers['Prefer'] = 'return=representation'
      
      const res = await fetchWithTimeout(`${API_BASE}/db?table=${table}`, {
        method: 'POST',
        headers,
        body: JSON.stringify(record),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Insert failed')
      return { data: Array.isArray(data) ? data[0] : data, error: null }
    },

    update: async (record: any, eq: [string, any]) => {
      const url = `${API_BASE}/db?table=${table}&${eq[0]}=eq.${eq[1]}`
      const headers = getAuthHeaders()
      headers['Prefer'] = 'return=representation'
      
      const res = await fetchWithTimeout(url, {
        method: 'PATCH',
        headers,
        body: JSON.stringify(record),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Update failed')
      return { data, error: null }
    },

    delete: async (eq: [string, any]) => {
      const url = `${API_BASE}/db?table=${table}&${eq[0]}=eq.${eq[1]}`
      const res = await fetchWithTimeout(url, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      })
      if (!res.ok) {
        const data = await res.json()
        throw new Error(data.error || 'Delete failed')
      }
      return { data: null, error: null }
    },
  }),
}
