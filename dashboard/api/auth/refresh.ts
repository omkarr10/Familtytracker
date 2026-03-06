import type { VercelRequest, VercelResponse } from '@vercel/node'
import { createClient } from '@supabase/supabase-js'

const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_ANON_KEY!
)

export default async function handler(req: VercelRequest, res: VercelResponse) {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' })
  }

  try {
    const { refresh_token } = req.body

    if (!refresh_token) {
      return res.status(400).json({ error: 'Refresh token required' })
    }

    // Use the refresh token to get a new session
    const { data, error } = await supabase.auth.refreshSession({
      refresh_token,
    })

    if (error) {
      console.error('Refresh error:', error)
      return res.status(401).json({ error: error.message, expired: true })
    }

    if (!data.session) {
      return res.status(401).json({ error: 'Session refresh failed', expired: true })
    }

    return res.status(200).json({
      user: data.user,
      session: data.session,
    })
  } catch (err: any) {
    console.error('Refresh error:', err)
    return res.status(500).json({ error: 'Internal server error' })
  }
}
