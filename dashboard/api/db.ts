import type { VercelRequest, VercelResponse } from '@vercel/node'

// Simple proxy to forward requests to Supabase REST API
export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, apikey, Prefer')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  const supabaseUrl = process.env.SUPABASE_URL
  const supabaseKey = process.env.SUPABASE_ANON_KEY

  if (!supabaseUrl || !supabaseKey) {
    return res.status(500).json({ error: 'Missing Supabase configuration' })
  }

  try {
    const { table, ...queryParams } = req.query
    
    if (!table || typeof table !== 'string') {
      return res.status(400).json({ error: 'Missing table parameter' })
    }

    // Build the target URL
    const url = new URL(`${supabaseUrl}/rest/v1/${table}`)
    
    // Forward query params
    Object.entries(queryParams).forEach(([key, value]) => {
      if (value) {
        url.searchParams.set(key, String(value))
      }
    })

    console.log('DB Proxy:', req.method, url.toString())

    // Build headers
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
