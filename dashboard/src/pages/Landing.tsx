import { Link } from 'react-router-dom'
import { 
  MapPin, 
  Shield, 
  Bell, 
  Smartphone, 
  Lock, 
  Camera, 
  MessageSquare, 
  WifiOff,
  ChevronRight,
  Github,
  ArrowRight,
  Map,
  Users,
  AlertTriangle,
  Volume2
} from 'lucide-react'

export default function Landing() {
  const features = [
    {
      icon: MapPin,
      title: 'Real-Time Location Tracking',
      description: 'Track your family members in real-time on an interactive map with accurate GPS positioning.',
      color: 'from-blue-500 to-blue-600'
    },
    {
      icon: Shield,
      title: 'Anti-Theft Protection',
      description: 'Advanced theft detection with instant photo capture, loud alarms, and remote device lock.',
      color: 'from-red-500 to-red-600'
    },
    {
      icon: Bell,
      title: 'Smart Alerts',
      description: 'Get instant notifications when family members enter or leave designated safe zones.',
      color: 'from-amber-500 to-amber-600'
    },
    {
      icon: Camera,
      title: 'Thief Photo Capture',
      description: 'Automatically capture front and back camera photos when theft is detected.',
      color: 'from-purple-500 to-purple-600'
    },
    {
      icon: MessageSquare,
      title: 'SMS Fallback',
      description: 'Control the device via SMS when internet is unavailable. Works even offline.',
      color: 'from-green-500 to-green-600'
    },
    {
      icon: Volume2,
      title: 'Remote Alarm',
      description: 'Trigger a loud alarm remotely that works even when the phone is on silent mode.',
      color: 'from-orange-500 to-orange-600'
    }
  ]

  const howItWorks = [
    {
      step: 1,
      title: 'Install the App',
      description: 'Download and install TrackIt on family members\' Android devices.',
      icon: Smartphone
    },
    {
      step: 2,
      title: 'Add Emergency Contacts',
      description: 'Set up to 3 emergency contacts who will receive SMS alerts with location.',
      icon: Users
    },
    {
      step: 3,
      title: 'Start Tracking',
      description: 'View locations in real-time on the web dashboard from anywhere.',
      icon: Map
    },
    {
      step: 4,
      title: 'Stay Protected',
      description: 'Anti-theft features kick in automatically when suspicious activity is detected.',
      icon: Shield
    }
  ]

  const antiTheftFeatures = [
    { icon: Camera, text: 'Captures thief\'s photo silently' },
    { icon: AlertTriangle, text: 'SIM removal detection' },
    { icon: MessageSquare, text: 'SMS location alerts' },
    { icon: Lock, text: 'Remote device lock' },
    { icon: Volume2, text: 'Loud alarm (even on silent)' },
    { icon: WifiOff, text: 'Works offline' }
  ]

  return (
    <div className="min-h-screen bg-white dark:bg-dark-950">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-white/80 dark:bg-dark-900/80 backdrop-blur-xl border-b border-gray-100 dark:border-dark-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                <MapPin className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-bold text-gray-900 dark:text-white">TrackIt</span>
            </div>
            <div className="flex items-center gap-4">
              <a 
                href="https://github.com/yourusername/trackit" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
              >
                <Github className="w-5 h-5" />
              </a>
              <Link 
                to="/login" 
                className="px-4 py-2 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors"
              >
                Get Started
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-primary-50 via-white to-blue-50 dark:from-dark-900 dark:via-dark-950 dark:to-primary-950/30" />
        <div className="absolute top-1/4 right-0 w-96 h-96 bg-primary-500/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl" />
        
        <div className="max-w-7xl mx-auto relative">
          <div className="text-center max-w-4xl mx-auto">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 text-sm font-medium mb-6">
              <Shield className="w-4 h-4" />
              Open Source Family Safety App
            </div>
            
            <h1 className="text-5xl sm:text-6xl lg:text-7xl font-bold text-gray-900 dark:text-white leading-tight mb-6">
              Keep Your Family{' '}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-600 to-blue-600">
                Safe & Connected
              </span>
            </h1>
            
            <p className="text-xl text-gray-600 dark:text-gray-400 mb-8 max-w-2xl mx-auto">
              Real-time location tracking with powerful anti-theft protection. 
              Know where your loved ones are and protect their devices from theft.
            </p>
            
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link 
                to="/login" 
                className="w-full sm:w-auto px-8 py-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-700 text-white font-semibold text-lg shadow-xl shadow-primary-500/25 hover:shadow-2xl hover:shadow-primary-500/30 transition-all hover:-translate-y-0.5 flex items-center justify-center gap-2"
              >
                Start Tracking Free
                <ArrowRight className="w-5 h-5" />
              </Link>
              <a 
                href="#features" 
                className="w-full sm:w-auto px-8 py-4 rounded-xl bg-gray-100 dark:bg-dark-800 text-gray-700 dark:text-gray-300 font-semibold text-lg hover:bg-gray-200 dark:hover:bg-dark-700 transition-colors flex items-center justify-center gap-2"
              >
                Learn More
                <ChevronRight className="w-5 h-5" />
              </a>
            </div>
          </div>

          {/* Hero Image/Dashboard Preview */}
          <div className="mt-16 relative">
            <div className="absolute inset-0 bg-gradient-to-t from-white dark:from-dark-950 to-transparent z-10 pointer-events-none h-32 bottom-0 top-auto" />
            <div className="bg-white dark:bg-dark-800 rounded-2xl shadow-2xl border border-gray-200 dark:border-dark-700 overflow-hidden">
              <div className="h-8 bg-gray-100 dark:bg-dark-700 flex items-center px-4 gap-2">
                <div className="w-3 h-3 rounded-full bg-red-500" />
                <div className="w-3 h-3 rounded-full bg-yellow-500" />
                <div className="w-3 h-3 rounded-full bg-green-500" />
              </div>
              <div className="p-4 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-dark-800 dark:to-dark-900">
                <div className="aspect-video bg-gray-200 dark:bg-dark-700 rounded-lg flex items-center justify-center">
                  <div className="text-center">
                    <Map className="w-16 h-16 text-primary-500 mx-auto mb-4" />
                    <p className="text-gray-500 dark:text-gray-400">Interactive Map Dashboard</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 px-4 bg-gray-50 dark:bg-dark-900">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
              Everything You Need to Keep Family Safe
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              Comprehensive location tracking and anti-theft features in one powerful app.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature, index) => (
              <div 
                key={index}
                className="bg-white dark:bg-dark-800 rounded-2xl p-6 shadow-sm hover:shadow-lg transition-shadow border border-gray-100 dark:border-dark-700"
              >
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4`}>
                  <feature.icon className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                  {feature.title}
                </h3>
                <p className="text-gray-600 dark:text-gray-400">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Anti-Theft Section */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 text-sm font-medium mb-4">
                <Shield className="w-4 h-4" />
                Anti-Theft Protection
              </div>
              <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
                Powerful Theft Detection & Response
              </h2>
              <p className="text-lg text-gray-600 dark:text-gray-400 mb-8">
                When someone tries to steal the phone, TrackIt instantly captures photos, 
                sends SMS alerts to your emergency contacts, and gives you remote control 
                to lock, alarm, or wipe the device.
              </p>

              <div className="grid sm:grid-cols-2 gap-4">
                {antiTheftFeatures.map((item, index) => (
                  <div key={index} className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-dark-800 rounded-lg">
                    <div className="w-10 h-10 rounded-lg bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                      <item.icon className="w-5 h-5 text-red-600 dark:text-red-400" />
                    </div>
                    <span className="text-gray-700 dark:text-gray-300 font-medium">{item.text}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 to-orange-500/20 rounded-3xl blur-3xl" />
              <div className="relative bg-gradient-to-br from-gray-900 to-gray-800 rounded-3xl p-8 text-white">
                <div className="flex items-center gap-3 mb-6">
                  <AlertTriangle className="w-8 h-8 text-red-500" />
                  <div>
                    <p className="font-semibold">Theft Detected!</p>
                    <p className="text-sm text-gray-400">SIM card removed</p>
                  </div>
                </div>
                
                <div className="space-y-4">
                  <div className="bg-white/10 rounded-lg p-4">
                    <p className="text-sm text-gray-400 mb-2">SMS sent to emergency contacts</p>
                    <p className="font-mono text-sm">
                      "ALERT: SIM removed from TrackIt device. Last location: 40.7128°N, 74.0060°W"
                    </p>
                  </div>
                  
                  <div className="flex gap-3">
                    <div className="flex-1 bg-red-500 rounded-lg p-3 text-center">
                      <Volume2 className="w-5 h-5 mx-auto mb-1" />
                      <p className="text-xs">Alarm</p>
                    </div>
                    <div className="flex-1 bg-orange-500 rounded-lg p-3 text-center">
                      <Lock className="w-5 h-5 mx-auto mb-1" />
                      <p className="text-xs">Lock</p>
                    </div>
                    <div className="flex-1 bg-purple-500 rounded-lg p-3 text-center">
                      <Camera className="w-5 h-5 mx-auto mb-1" />
                      <p className="text-xs">Capture</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works */}
      <section className="py-20 px-4 bg-gray-50 dark:bg-dark-900">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
              How It Works
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              Get started in minutes with our simple setup process.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-8">
            {howItWorks.map((item, index) => (
              <div key={index} className="relative">
                {index < howItWorks.length - 1 && (
                  <div className="hidden lg:block absolute top-12 left-full w-full h-0.5 bg-gradient-to-r from-primary-500 to-transparent" />
                )}
                <div className="text-center">
                  <div className="w-24 h-24 mx-auto rounded-2xl bg-gradient-to-br from-primary-500 to-primary-600 flex items-center justify-center mb-4 shadow-lg shadow-primary-500/25">
                    <item.icon className="w-10 h-10 text-white" />
                  </div>
                  <div className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 text-sm font-bold mb-3">
                    {item.step}
                  </div>
                  <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                    {item.title}
                  </h3>
                  <p className="text-gray-600 dark:text-gray-400">
                    {item.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SMS Commands */}
      <section className="py-20 px-4">
        <div className="max-w-7xl mx-auto">
          <div className="bg-gradient-to-br from-primary-600 to-primary-800 rounded-3xl p-8 sm:p-12 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl" />
            <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary-400/20 rounded-full blur-3xl" />
            
            <div className="relative grid lg:grid-cols-2 gap-8 items-center">
              <div>
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/20 text-white text-sm font-medium mb-4">
                  <WifiOff className="w-4 h-4" />
                  Works Without Internet
                </div>
                <h2 className="text-3xl sm:text-4xl font-bold text-white mb-4">
                  SMS Command Fallback
                </h2>
                <p className="text-lg text-primary-100 mb-6">
                  Even if the thief turns off data, you can still control the device via SMS. 
                  Send commands to locate, alarm, or capture photos.
                </p>
              </div>

              <div className="bg-white/10 backdrop-blur rounded-2xl p-6">
                <p className="text-primary-100 text-sm mb-4">Available SMS Commands:</p>
                <div className="space-y-3 font-mono text-sm">
                  <div className="flex items-center gap-3 text-white">
                    <MapPin className="w-4 h-4 text-primary-300" />
                    <code>TRACKIT LOCATE</code>
                    <span className="text-primary-200 text-xs">- Get location</span>
                  </div>
                  <div className="flex items-center gap-3 text-white">
                    <Volume2 className="w-4 h-4 text-primary-300" />
                    <code>TRACKIT ALARM</code>
                    <span className="text-primary-200 text-xs">- Sound alarm</span>
                  </div>
                  <div className="flex items-center gap-3 text-white">
                    <Camera className="w-4 h-4 text-primary-300" />
                    <code>TRACKIT CAPTURE</code>
                    <span className="text-primary-200 text-xs">- Take photos</span>
                  </div>
                  <div className="flex items-center gap-3 text-white">
                    <Shield className="w-4 h-4 text-primary-300" />
                    <code>TRACKIT THEFT</code>
                    <span className="text-primary-200 text-xs">- Theft mode</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Tech Stack */}
      <section className="py-20 px-4 bg-gray-50 dark:bg-dark-900">
        <div className="max-w-7xl mx-auto text-center">
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
            Built with Modern Tech
          </h2>
          <p className="text-lg text-gray-600 dark:text-gray-400 mb-12 max-w-2xl mx-auto">
            Open source, self-hostable, and built with technologies you know and trust.
          </p>

          <div className="flex flex-wrap justify-center gap-6">
            {['React', 'TypeScript', 'Kotlin', 'Supabase', 'Tailwind CSS', 'Vite'].map((tech) => (
              <div key={tech} className="px-6 py-3 bg-white dark:bg-dark-800 rounded-xl shadow-sm border border-gray-100 dark:border-dark-700">
                <span className="font-semibold text-gray-700 dark:text-gray-300">{tech}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
            Start Protecting Your Family Today
          </h2>
          <p className="text-lg text-gray-600 dark:text-gray-400 mb-8">
            TrackIt is free, open source, and gives you complete control over your data.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link 
              to="/login" 
              className="w-full sm:w-auto px-8 py-4 rounded-xl bg-gradient-to-r from-primary-600 to-primary-700 text-white font-semibold text-lg shadow-xl shadow-primary-500/25 hover:shadow-2xl transition-all flex items-center justify-center gap-2"
            >
              Get Started Free
              <ArrowRight className="w-5 h-5" />
            </Link>
            <a 
              href="https://github.com/yourusername/trackit"
              target="_blank"
              rel="noopener noreferrer"
              className="w-full sm:w-auto px-8 py-4 rounded-xl bg-gray-900 dark:bg-white text-white dark:text-gray-900 font-semibold text-lg hover:bg-gray-800 dark:hover:bg-gray-100 transition-colors flex items-center justify-center gap-2"
            >
              <Github className="w-5 h-5" />
              View on GitHub
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-4 border-t border-gray-100 dark:border-dark-800">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                <MapPin className="w-4 h-4 text-white" />
              </div>
              <span className="font-semibold text-gray-900 dark:text-white">TrackIt</span>
            </div>
            <p className="text-gray-500 dark:text-gray-400 text-sm">
              Open source family location tracker with anti-theft protection.
            </p>
            <div className="flex items-center gap-4">
              <a 
                href="https://github.com/yourusername/trackit" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
              >
                <Github className="w-5 h-5" />
              </a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
