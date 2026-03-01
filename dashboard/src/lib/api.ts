// API client that uses Vercel proxy to bypass ISP blocking
const API_BASE = '/api'

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
  },

  // Generic table operations via proxy
  from: (table: string) => ({
    select: async (columns = '*', options: { eq?: [string, any], order?: [string, boolean], limit?: number } = {}) => {
      let url = `${API_BASE}/proxy/${table}?select=${columns}`
      if (options.eq) {
        url += `&${options.eq[0]}=eq.${options.eq[1]}`
      }
      if (options.order) {
        url += `&order=${options.order[0]}.${options.order[1] ? 'asc' : 'desc'}`
      }
      if (options.limit) {
        url += `&limit=${options.limit}`
      }

      const res = await fetchWithTimeout(url)
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Fetch failed')
      return { data, error: null }
    },

    insert: async (record: any) => {
      const res = await fetchWithTimeout(`${API_BASE}/proxy/${table}`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Prefer': 'return=representation',
        },
        body: JSON.stringify(record),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Insert failed')
      return { data: Array.isArray(data) ? data[0] : data, error: null }
    },

    update: async (record: any, eq: [string, any]) => {
      const url = `${API_BASE}/proxy/${table}?${eq[0]}=eq.${eq[1]}`
      const res = await fetchWithTimeout(url, {
        method: 'PATCH',
        headers: { 
          'Content-Type': 'application/json',
          'Prefer': 'return=representation',
        },
        body: JSON.stringify(record),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Update failed')
      return { data, error: null }
    },

    delete: async (eq: [string, any]) => {
      const url = `${API_BASE}/proxy/${table}?${eq[0]}=eq.${eq[1]}`
      const res = await fetchWithTimeout(url, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      })
      if (!res.ok) {
        const data = await res.json()
        throw new Error(data.error || 'Delete failed')
      }
      return { data: null, error: null }
    },
  }),
}

// Session storage helper
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
