import type { VercelRequest, VercelResponse } from '@vercel/node'

// Generic proxy to forward requests to Supabase REST API
export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, apikey, Prefer')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  // Check for required env vars
  const supabaseUrl = process.env.SUPABASE_URL
  const supabaseKey = process.env.SUPABASE_ANON_KEY

  if (!supabaseUrl || !supabaseKey) {
    console.error('Missing env vars:', { supabaseUrl: !!supabaseUrl, supabaseKey: !!supabaseKey })
    return res.status(500).json({ 
      error: 'Server configuration error: Missing Supabase credentials.' 
    })
  }

  try {
    const { path } = req.query
    const pathArray = Array.isArray(path) ? path : [path]
    const supabasePath = pathArray.join('/')

    // Build the target URL
    const url = new URL(`${supabaseUrl}/rest/v1/${supabasePath}`)
    
    // Forward query params (except 'path')
    Object.entries(req.query).forEach(([key, value]) => {
      if (key !== 'path' && value) {
        url.searchParams.set(key, String(value))
      }
    })

    console.log('Proxy request:', req.method, url.toString())

    // Build headers - only include Prefer if it has a value
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'apikey': supabaseKey,
      'Authorization': `Bearer ${supabaseKey}`,
    }
    
    if (req.headers.prefer) {
      headers['Prefer'] = req.headers.prefer as string
    }

    // Forward the request to Supabase
    const response = await fetch(url.toString(), {
      method: req.method,
      headers,
      body: req.method !== 'GET' && req.method !== 'HEAD' ? JSON.stringify(req.body) : undefined,
    })

    const text = await response.text()
    console.log('Supabase response:', response.status, text.substring(0, 500))

    // Try to parse as JSON
    let data
    try {
      data = JSON.parse(text)
    } catch {
      data = { rawResponse: text }
    }

    return res.status(response.status).json(data)
  } catch (err: any) {
    console.error('Proxy error:', err)
    return res.status(500).json({ error: err.message })
  }
}
