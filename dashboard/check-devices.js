import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://vcdqijuwsqkmvwceizdu.supabase.co'
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZjZHFpanV3c3FrbXZ3Y2VpemR1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIyNzY3OTIsImV4cCI6MjA4Nzg1Mjc5Mn0.0ygd1r5cozra1aT3oRNfff2Ei8bsz7hI5_sqzJwhN1M'

const supabase = createClient(supabaseUrl, supabaseKey)

async function insertDevice() {
  const deviceId = '396b7562-5e2d-496b-96d8-797aa1122bcc'
  
  // Check if it exists
  const { data: existing } = await supabase.from('devices').select('*').eq('id', deviceId)
  
  if (existing && existing.length > 0) {
    console.log("Device already exists")
    // Let's update it to make sure it's active
    await supabase.from('devices').update({ device_name: "My Android Phone", is_online: true }).eq('id', deviceId)
    console.log("Updated device name to 'My Android Phone'")
  } else {
    // Insert it
    const { data, error } = await supabase.from('devices').insert({
      id: deviceId,
      device_name: "My Android Phone",
      is_online: true,
      battery_level: 100
    })
    
    if (error) {
      console.error("Error inserting:", error)
    } else {
      console.log("Successfully inserted device!")
    }
  }
  
  // Also check existing devices to rename the ghost one so user doesn't use it
  await supabase.from('devices').update({ device_name: "OLD/GHOST DEVICE" }).eq('id', '1ce54707-f014-4f54-a57f-51f623f8517c')
  console.log("Renamed old ghost device.")
}

insertDevice()
