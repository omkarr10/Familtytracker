import type { VercelRequest, VercelResponse } from '@vercel/node'
import { createClient } from '@supabase/supabase-js'

export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' })
  }

  const supabaseUrl = process.env.SUPABASE_URL
  const supabaseKey = process.env.SUPABASE_ANON_KEY

  if (!supabaseUrl || !supabaseKey) {
    return res.status(500).json({ error: 'Missing Supabase configuration' })
  }

  try {
    const { password, accessToken } = req.body

    if (!password) {
      return res.status(400).json({ error: 'Password is required' })
    }

    if (!accessToken) {
      return res.status(401).json({ error: 'Access token required' })
    }

    // Create client with user's access token
    const supabase = createClient(supabaseUrl, supabaseKey, {
      global: {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    })

    const { data, error } = await supabase.auth.updateUser({
      password,
    })

    if (error) {
      return res.status(400).json({ error: error.message })
    }

    return res.status(200).json({ success: true, user: data.user })
  } catch (err: any) {
    console.error('Update password error:', err)
    return res.status(500).json({ error: 'Internal server error' })
  }
}
