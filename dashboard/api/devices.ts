import type { VercelRequest, VercelResponse } from '@vercel/node'
import { createClient } from '@supabase/supabase-js'

const supabase = createClient(
  process.env.SUPABASE_URL!,
  process.env.SUPABASE_ANON_KEY!
)

export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  try {
    const { action, userId } = req.query

    if (action === 'list') {
      // Get user's devices
      const { data, error } = await supabase
        .from('devices')
        .select('*')
        .eq('user_id', userId)
        .order('created_at', { ascending: false })

      if (error) throw error
      return res.status(200).json(data)
    }

    if (action === 'add' && req.method === 'POST') {
      const { name, device_id, user_id } = req.body
      const { data, error } = await supabase
        .from('devices')
        .insert({ name, device_id, user_id })
        .select()
        .single()

      if (error) throw error
      return res.status(200).json(data)
    }

    if (action === 'delete' && req.method === 'POST') {
      const { device_id } = req.body
      const { error } = await supabase
        .from('devices')
        .delete()
        .eq('id', device_id)

      if (error) throw error
      return res.status(200).json({ success: true })
    }

    return res.status(400).json({ error: 'Invalid action' })
  } catch (err: any) {
    console.error('Devices error:', err)
    return res.status(500).json({ error: err.message })
  }
}
