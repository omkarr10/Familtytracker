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
    const { device_id, limit = 100 } = req.query

    if (!device_id) {
      return res.status(400).json({ error: 'device_id required' })
    }

    const { data, error } = await supabase
      .from('locations')
      .select('*')
      .eq('device_id', device_id)
      .order('timestamp', { ascending: false })
      .limit(Number(limit))

    if (error) throw error
    return res.status(200).json(data)
  } catch (err: any) {
    console.error('Locations error:', err)
    return res.status(500).json({ error: err.message })
  }
}
