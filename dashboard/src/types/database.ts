export interface Database {
  public: {
    Tables: {
      users: {
        Row: {
          id: string
          email: string
          name: string | null
          created_at: string
        }
        Insert: {
          id?: string
          email: string
          name?: string | null
          created_at?: string
        }
        Update: {
          id?: string
          email?: string
          name?: string | null
          created_at?: string
        }
        Relationships: []
      }
      devices: {
        Row: {
          id: string
          user_id: string
          device_name: string
          device_token: string | null
          last_seen: string | null
          battery_level: number | null
          is_online: boolean
          created_at: string
        }
        Insert: {
          id?: string
          user_id: string
          device_name: string
          device_token?: string | null
          last_seen?: string | null
          battery_level?: number | null
          is_online?: boolean
          created_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          device_name?: string
          device_token?: string | null
          last_seen?: string | null
          battery_level?: number | null
          is_online?: boolean
          created_at?: string
        }
        Relationships: []
      }
      locations: {
        Row: {
          id: string
          device_id: string
          latitude: number
          longitude: number
          accuracy: number | null
          speed: number | null
          event_type: string
          created_at: string
        }
        Insert: {
          id?: string
          device_id: string
          latitude: number
          longitude: number
          accuracy?: number | null
          speed?: number | null
          event_type?: string
          created_at?: string
        }
        Update: {
          id?: string
          device_id?: string
          latitude?: number
          longitude?: number
          accuracy?: number | null
          speed?: number | null
          event_type?: string
          created_at?: string
        }
        Relationships: []
      }
      geofences: {
        Row: {
          id: string
          user_id: string
          name: string
          latitude: number
          longitude: number
          radius_meters: number
          alert_on_enter: boolean
          alert_on_exit: boolean
          created_at: string
        }
        Insert: {
          id?: string
          user_id: string
          name: string
          latitude: number
          longitude: number
          radius_meters: number
          alert_on_enter?: boolean
          alert_on_exit?: boolean
          created_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          name?: string
          latitude?: number
          longitude?: number
          radius_meters?: number
          alert_on_enter?: boolean
          alert_on_exit?: boolean
          created_at?: string
        }
        Relationships: []
      }
      alerts: {
        Row: {
          id: string
          device_id: string
          alert_type: string
          message: string | null
          latitude: number | null
          longitude: number | null
          is_read: boolean
          created_at: string
        }
        Insert: {
          id?: string
          device_id: string
          alert_type: string
          message?: string | null
          latitude?: number | null
          longitude?: number | null
          is_read?: boolean
          created_at?: string
        }
        Update: {
          id?: string
          device_id?: string
          alert_type?: string
          message?: string | null
          latitude?: number | null
          longitude?: number | null
          is_read?: boolean
          created_at?: string
        }
        Relationships: []
      }
    }
    Views: Record<string, never>
    Functions: Record<string, never>
    Enums: Record<string, never>
    CompositeTypes: Record<string, never>
  }
}

export type Device = Database['public']['Tables']['devices']['Row']
export type Location = Database['public']['Tables']['locations']['Row']
export type Geofence = Database['public']['Tables']['geofences']['Row']
export type Alert = Database['public']['Tables']['alerts']['Row']
