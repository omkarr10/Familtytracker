import type { VercelRequest, VercelResponse } from '@vercel/node'

// Generic proxy to forward requests to Supabase REST API
export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, apikey, Prefer')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  try {
    const { path } = req.query
    const pathArray = Array.isArray(path) ? path : [path]
    const supabasePath = pathArray.join('/')
    
    const supabaseUrl = process.env.SUPABASE_URL!
    const supabaseKey = process.env.SUPABASE_ANON_KEY!

    // Build the target URL
    const url = new URL(`${supabaseUrl}/rest/v1/${supabasePath}`)
    
    // Forward query params (except 'path')
    Object.entries(req.query).forEach(([key, value]) => {
      if (key !== 'path' && value) {
        url.searchParams.set(key, String(value))
      }
    })

    // Forward the request to Supabase
    const response = await fetch(url.toString(), {
      method: req.method,
      headers: {
        'Content-Type': 'application/json',
        'apikey': supabaseKey,
        'Authorization': req.headers.authorization || `Bearer ${supabaseKey}`,
        'Prefer': req.headers.prefer as string || '',
      },
      body: req.method !== 'GET' && req.method !== 'HEAD' ? JSON.stringify(req.body) : undefined,
    })

    const data = await response.json()
    return res.status(response.status).json(data)
  } catch (err: any) {
    console.error('Proxy error:', err)
    return res.status(500).json({ error: err.message })
  }
}
