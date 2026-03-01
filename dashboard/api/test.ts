import type { VercelRequest, VercelResponse } from '@vercel/node'

// Simple test endpoint to check if API routes work
export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  
  const supabaseUrl = process.env.SUPABASE_URL
  const supabaseKey = process.env.SUPABASE_ANON_KEY
  
  return res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    env: {
      SUPABASE_URL: supabaseUrl ? 'SET' : 'MISSING',
      SUPABASE_ANON_KEY: supabaseKey ? 'SET' : 'MISSING'
    }
  })
}
