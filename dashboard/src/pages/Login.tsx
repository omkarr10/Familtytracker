import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api, sessionStore } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { MapPin, Mail, Lock, Eye, EyeOff, Loader2, ArrowLeft } from 'lucide-react'

export default function Login() {
  const navigate = useNavigate()
  const { setUser } = useAuthStore()
  const [isSignUp, setIsSignUp] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    try {
      if (isSignUp) {
        const data = await api.auth.signUp(email, password)
        if (data.session) {
          sessionStore.save(data.session)
          setUser(data.user)
          navigate('/dashboard')
        } else if (data.user) {
          setError('Check your email for the confirmation link!')
        }
      } else {
        const data = await api.auth.signIn(email, password)
        if (data.session) {
          sessionStore.save(data.session)
          setUser(data.user)
          navigate('/dashboard')
        }
      }
    } catch (err: any) {
      console.error('Auth error:', err)
      setError(err.message || 'Login failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-600 via-primary-700 to-primary-900 dark:from-dark-950 dark:via-dark-900 dark:to-primary-950 flex items-center justify-center p-4">
      {/* Back to home */}
      <Link 
        to="/" 
        className="absolute top-4 left-4 flex items-center gap-2 text-white/80 hover:text-white transition-colors"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to home
      </Link>
      
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-bl from-white/5 to-transparent rounded-full" />
        <div className="absolute -bottom-1/4 -left-1/4 w-1/2 h-1/2 bg-gradient-to-tr from-primary-500/20 to-transparent rounded-full blur-3xl" />
      </div>
      
      <div className="relative bg-white/95 dark:bg-dark-800/95 backdrop-blur-xl rounded-3xl shadow-2xl w-full max-w-md p-8 border border-white/20 dark:border-dark-700">
        {/* Logo & Header */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-xl shadow-primary-500/30 mb-4">
            <MapPin className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-800 dark:text-white">TrackIt</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">
            {isSignUp ? 'Create your account' : 'Welcome back'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Email
            </label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input pl-12"
                placeholder="you@example.com"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="input pl-12 pr-12"
                placeholder="••••••••"
                required
                minLength={6}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {error && (
            <div
              className={`p-4 rounded-xl text-sm font-medium ${
                error.includes('Check your email')
                  ? 'bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-800'
                  : 'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-800'
              }`}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full py-3.5 text-base"
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <Loader2 className="w-5 h-5 animate-spin" />
                Processing...
              </span>
            ) : isSignUp ? (
              'Create Account'
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        <div className="mt-8 text-center">
          <span className="text-gray-500 dark:text-gray-400 text-sm">
            {isSignUp ? 'Already have an account?' : "Don't have an account?"}
          </span>
          <button
            onClick={() => setIsSignUp(!isSignUp)}
            className="ml-2 text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 text-sm font-semibold transition-colors"
          >
            {isSignUp ? 'Sign in' : 'Sign up'}
          </button>
        </div>
        
        {/* Feature highlights */}
        <div className="mt-8 pt-6 border-t border-gray-100 dark:border-dark-700">
          <div className="grid grid-cols-3 gap-4 text-center">
            {[
              { label: 'Live', desc: 'Tracking' },
              { label: 'Secure', desc: 'Data' },
              { label: '24/7', desc: 'Updates' },
            ].map(({ label, desc }) => (
              <div key={label}>
                <div className="text-lg font-bold text-primary-600 dark:text-primary-400">{label}</div>
                <div className="text-xs text-gray-500 dark:text-gray-400">{desc}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
