import type { VercelRequest, VercelResponse } from '@vercel/node'

// Simple proxy to forward requests to Supabase REST API
export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, apikey, Prefer, x-access-token')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  const supabaseUrl = process.env.SUPABASE_URL
  const supabaseKey = process.env.SUPABASE_ANON_KEY

  if (!supabaseUrl || !supabaseKey) {
    console.error('Missing env vars:', { 
      SUPABASE_URL: supabaseUrl ? 'SET' : 'MISSING', 
      SUPABASE_ANON_KEY: supabaseKey ? 'SET' : 'MISSING' 
    })
    return res.status(500).json({ 
      error: 'Missing Supabase configuration',
      details: 'Please add SUPABASE_URL and SUPABASE_ANON_KEY to Vercel Environment Variables'
    })
  }

  try {
    const { table, select, ...otherParams } = req.query
    
    if (!table || typeof table !== 'string') {
      return res.status(400).json({ error: 'Missing table parameter' })
    }

    // Build the target URL - use select=* directly without encoding
    const selectValue = select || '*'
    let targetUrl = `${supabaseUrl}/rest/v1/${table}?select=${selectValue}`
    
    // Append other query params
    Object.entries(otherParams).forEach(([key, value]) => {
      if (value) {
        targetUrl += `&${key}=${encodeURIComponent(String(value))}`
      }
    })

    console.log('DB Proxy:', req.method, targetUrl)

    // Get user's access token from header (for RLS)
    const userToken = req.headers['x-access-token'] as string | undefined

    // Build headers - use user token if available, otherwise anon key
    const authToken = userToken || supabaseKey
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'apikey': supabaseKey,
      'Authorization': `Bearer ${authToken}`,
    }
    
    if (req.headers.prefer) {
      headers['Prefer'] = req.headers.prefer as string
    }

    // Forward the request to Supabase
    const response = await fetch(targetUrl, {
      method: req.method,
      headers,
      body: req.method !== 'GET' && req.method !== 'HEAD' ? JSON.stringify(req.body) : undefined,
    })

    const text = await response.text()
    console.log('Supabase response:', response.status, text.substring(0, 200))
    
    // Try to parse as JSON
    let data
    try {
      data = JSON.parse(text)
    } catch {
      data = { rawResponse: text }
    }

    return res.status(response.status).json(data)
  } catch (err: any) {
    console.error('DB Proxy error:', err)
    return res.status(500).json({ error: err.message })
  }
}
