import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://vcdqijuwsqkmvwceizdu.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZjZHFpanV3c3FrbXZ3Y2VpemR1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyNzY3OTIsImV4cCI6MjA4Nzg1Mjc5Mn0.0ygd1r5cozra1aT3oRNfff2Ei8bsz7hI5_sqzJwhN1M'

const supabase = createClient(supabaseUrl, supabaseKey)

async function checkCommands() {
  const { data, error } = await supabase
    .from('remote_commands')
    .select('*')
    .order('created_at', { ascending: false })
    .limit(10)
    
  if (error) {
    console.error(error)
  } else {
    console.log(JSON.stringify(data, null, 2))
  }
}

checkCommands()
