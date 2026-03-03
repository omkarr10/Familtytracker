import { useState, useRef } from 'react'
import { Camera, User } from 'lucide-react'
import clsx from 'clsx'

interface DeviceAvatarProps {
  name: string
  avatarUrl?: string | null
  color?: string
  size?: 'sm' | 'md' | 'lg'
  editable?: boolean
  onAvatarChange?: (dataUrl: string | null) => void
}

const sizeClasses = {
  sm: 'w-8 h-8 text-sm',
  md: 'w-10 h-10 text-base',
  lg: 'w-16 h-16 text-xl',
}

export function DeviceAvatar({
  name,
  avatarUrl,
  color = '#3b82f6',
  size = 'md',
  editable = false,
  onAvatarChange,
}: DeviceAvatarProps) {
  const [isHovering, setIsHovering] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    // Validate file type
    if (!file.type.startsWith('image/')) {
      alert('Please select an image file')
      return
    }

    // Validate file size (max 500KB)
    if (file.size > 500 * 1024) {
      alert('Image too large. Please select an image under 500KB')
      return
    }

    // Read and resize the image
    const reader = new FileReader()
    reader.onload = (event) => {
      const img = new Image()
      img.onload = () => {
        // Create a canvas to resize the image
        const canvas = document.createElement('canvas')
        const maxSize = 128
        let width = img.width
        let height = img.height

        // Scale down if necessary
        if (width > height) {
          if (width > maxSize) {
            height = (height / width) * maxSize
            width = maxSize
          }
        } else {
          if (height > maxSize) {
            width = (width / height) * maxSize
            height = maxSize
          }
        }

        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)

        // Convert to base64
        const dataUrl = canvas.toDataURL('image/jpeg', 0.8)
        onAvatarChange?.(dataUrl)
      }
      img.src = event.target?.result as string
    }
    reader.readAsDataURL(file)
  }

  const handleRemoveAvatar = (e: React.MouseEvent) => {
    e.stopPropagation()
    onAvatarChange?.(null)
  }

  const initial = name.charAt(0).toUpperCase()

  return (
    <div
      className={clsx(
        'relative rounded-full flex items-center justify-center text-white font-bold shadow-lg overflow-hidden',
        sizeClasses[size],
        editable && 'cursor-pointer'
      )}
      style={{ backgroundColor: avatarUrl ? undefined : color }}
      onMouseEnter={() => editable && setIsHovering(true)}
      onMouseLeave={() => editable && setIsHovering(false)}
      onClick={() => editable && fileInputRef.current?.click()}
    >
      {avatarUrl ? (
        <img
          src={avatarUrl}
          alt={name}
          className="w-full h-full object-cover"
        />
      ) : (
        <span>{initial}</span>
      )}

      {/* Hover overlay for editable avatars */}
      {editable && isHovering && (
        <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
          <Camera className="w-5 h-5 text-white" />
        </div>
      )}

      {/* Hidden file input */}
      {editable && (
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleFileSelect}
          className="hidden"
        />
      )}
    </div>
  )
}

// Preset avatar colors based on device index
export const avatarColors = [
  '#3b82f6', // blue
  '#ef4444', // red
  '#22c55e', // green
  '#f59e0b', // amber
  '#8b5cf6', // violet
  '#ec4899', // pink
  '#06b6d4', // cyan
  '#84cc16', // lime
]

export function getAvatarColor(index: number): string {
  return avatarColors[index % avatarColors.length]
}
