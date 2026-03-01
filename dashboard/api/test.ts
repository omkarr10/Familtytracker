import type { VercelRequest, VercelResponse } from '@vercel/node'

// Simple test endpoint to check if API routes work
export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  
  const supabaseUrl = process.env.SUPABASE_URL
  const supabaseKey = process.env.SUPABASE_ANON_KEY
  
  // Test actual Supabase connection
  let supabaseTest = { status: 'not_tested', message: '' }
  
  if (supabaseUrl && supabaseKey) {
    try {
      const response = await fetch(`${supabaseUrl}/rest/v1/devices?select=id&limit=1`, {
        headers: {
          'apikey': supabaseKey,
          'Authorization': `Bearer ${supabaseKey}`,
        }
      })
      supabaseTest = {
        status: response.ok ? 'connected' : 'error',
        message: response.ok ? 'Supabase reachable' : `HTTP ${response.status}`
      }
    } catch (err: any) {
      supabaseTest = {
        status: 'failed',
        message: err.message
      }
    }
  }
  
  return res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    userAgent: req.headers['user-agent'],
    ip: req.headers['x-forwarded-for'] || req.headers['x-real-ip'] || 'unknown',
    env: {
      SUPABASE_URL: supabaseUrl ? 'SET' : 'MISSING',
      SUPABASE_ANON_KEY: supabaseKey ? 'SET' : 'MISSING'
    },
    supabaseTest
  })
}
