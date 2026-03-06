import type { VercelRequest, VercelResponse } from '@vercel/node'
import { createClient } from '@supabase/supabase-js'

const supabaseUrl = process.env.VITE_SUPABASE_URL || process.env.SUPABASE_URL
const supabaseKey = process.env.VITE_SUPABASE_ANON_KEY || process.env.SUPABASE_ANON_KEY

export default async function handler(req: VercelRequest, res: VercelResponse) {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')

  if (req.method === 'OPTIONS') {
    return res.status(200).end()
  }

  if (!supabaseUrl || !supabaseKey) {
    return res.status(500).json({ error: 'Supabase configuration missing' })
  }

  const supabase = createClient(supabaseUrl, supabaseKey)

  try {
    if (req.method === 'GET') {
      // Get user preferences
      const { user_id } = req.query as { user_id: string }
      
      if (!user_id) {
        return res.status(400).json({ error: 'user_id is required' })
      }

      const { data, error } = await supabase
        .from('users')
        .select('selected_antitheft_device_id')
        .eq('id', user_id)
        .single()

      if (error) {
        // User might not have a record yet - that's ok
        if (error.code === 'PGRST116') {
          return res.status(200).json({ preferences: { selected_antitheft_device_id: null } })
        }
        throw error
      }

      return res.status(200).json({ 
        preferences: { 
          selected_antitheft_device_id: data?.selected_antitheft_device_id || null 
        } 
      })
    }

    if (req.method === 'PUT' || req.method === 'POST') {
      // Update user preferences
      const { user_id, selected_antitheft_device_id } = req.body

      if (!user_id) {
        return res.status(400).json({ error: 'user_id is required' })
      }

      const { error } = await supabase
        .from('users')
        .update({ selected_antitheft_device_id: selected_antitheft_device_id || null })
        .eq('id', user_id)

      if (error) {
        throw error
      }

      return res.status(200).json({ success: true })
    }

    return res.status(405).json({ error: 'Method not allowed' })
  } catch (err: any) {
    console.error('User preferences error:', err)
    return res.status(500).json({ error: err.message || 'Failed to process request' })
  }
}
