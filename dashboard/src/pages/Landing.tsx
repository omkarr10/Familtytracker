import { Link } from 'react-router-dom'
import { 
  MapPin, Shield, Camera, ArrowRight, Github, Volume2, 
  WifiOff, AlertTriangle, Download, ChevronDown, Check
} from 'lucide-react'

export default function Landing() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950">
      {/* Nav */}
      <nav className="fixed top-0 w-full z-50 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-100 dark:border-slate-800">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center">
              <MapPin className="w-5 h-5 text-white" />
            </div>
            <span className="text-lg font-semibold text-slate-900 dark:text-white">TrackIt</span>
          </Link>
          <div className="flex items-center gap-3">
            <a 
              href="https://github.com/omkarr10/Familtytracker" 
              target="_blank" 
              rel="noopener noreferrer"
              className="p-2 text-slate-500 hover:text-slate-900 dark:hover:text-white transition-colors"
              aria-label="GitHub"
            >
              <Github className="w-5 h-5" />
            </a>
            <Link 
              to="/login" 
              className="px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
            >
              Open Dashboard
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="pt-28 pb-16 px-6">
        <div className="max-w-3xl mx-auto text-center">
          <h1 className="text-4xl sm:text-5xl font-bold text-slate-900 dark:text-white leading-tight tracking-tight">
            Family location tracking
            <br />
            <span className="text-emerald-600">that actually works offline</span>
          </h1>
          
          <p className="mt-6 text-lg text-slate-600 dark:text-slate-400 max-w-xl mx-auto">
            Track your family's location. Get alerts if a phone is stolen. 
            Works via SMS when there's no internet. Free and open source.
          </p>

          <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
            <a 
              href="/TrackIt.apk" 
              download="TrackIt.apk"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 text-base font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
            >
              <Download className="w-5 h-5" />
              Download for Android
            </a>
            <Link 
              to="/login" 
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 text-base font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg hover:border-slate-300 dark:hover:border-slate-600 transition-colors"
            >
              Open Web Dashboard
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <a href="#install" className="mt-8 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-emerald-600 transition-colors">
            See installation guide
            <ChevronDown className="w-4 h-4" />
          </a>
        </div>
      </section>

      {/* App Preview */}
      <section className="pb-20 px-6">
        <div className="max-w-5xl mx-auto">
          <div className="relative bg-slate-900 rounded-2xl overflow-hidden shadow-2xl shadow-slate-900/20">
            {/* Browser bar */}
            <div className="h-10 bg-slate-800 flex items-center px-4 gap-2">
              <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-red-500" />
                <div className="w-3 h-3 rounded-full bg-amber-500" />
                <div className="w-3 h-3 rounded-full bg-emerald-500" />
              </div>
              <div className="ml-4 flex-1 max-w-md">
                <div className="h-6 bg-slate-700 rounded-md px-3 flex items-center">
                  <span className="text-xs text-slate-400">trackit.yourserver.com/dashboard</span>
                </div>
              </div>
            </div>
            {/* Screenshot */}
            <img 
              src="/dashboard.png" 
              alt="TrackIt Dashboard showing family locations on a map" 
              className="w-full"
            />
          </div>
        </div>
      </section>

      {/* What You Get */}
      <section className="py-20 px-6 bg-white dark:bg-slate-900">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white text-center">
            What you get
          </h2>
          
          <div className="mt-12 grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[
              {
                icon: MapPin,
                title: 'Real-time Location',
                desc: 'See where family members are on a live map. Location updates every few minutes.',
                color: 'bg-blue-500'
              },
              {
                icon: Camera,
                title: 'Intruder Selfie',
                desc: 'Wrong PIN entered? The front camera silently captures a photo and sends it to you.',
                color: 'bg-purple-500'
              },
              {
                icon: AlertTriangle,
                title: 'SIM Removal Alert',
                desc: 'If someone removes the SIM card, you get an SMS with the last known location.',
                color: 'bg-red-500'
              },
              {
                icon: Volume2,
                title: 'Remote Alarm',
                desc: 'Lost your phone under the couch? Trigger a loud alarm from the dashboard.',
                color: 'bg-orange-500'
              },
              {
                icon: WifiOff,
                title: 'SMS Fallback',
                desc: 'No WiFi? No data? Send SMS commands to locate, lock, or alarm the device.',
                color: 'bg-emerald-500'
              },
              {
                icon: Shield,
                title: 'Shake to SOS',
                desc: 'Shake the phone 3 times to send an emergency alert with location to contacts.',
                color: 'bg-slate-700'
              }
            ].map((feature, i) => (
              <div key={i} className="p-5 bg-slate-50 dark:bg-slate-800 rounded-xl border border-slate-100 dark:border-slate-700">
                <div className={`w-10 h-10 ${feature.color} rounded-lg flex items-center justify-center mb-4`}>
                  <feature.icon className="w-5 h-5 text-white" />
                </div>
                <h3 className="font-semibold text-slate-900 dark:text-white mb-2">{feature.title}</h3>
                <p className="text-sm text-slate-600 dark:text-slate-400">{feature.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SMS Commands */}
      <section className="py-20 px-6">
        <div className="max-w-4xl mx-auto">
          <div className="bg-slate-900 rounded-2xl p-8 sm:p-12">
            <div className="flex items-center gap-3 mb-6">
              <WifiOff className="w-6 h-6 text-emerald-400" />
              <h2 className="text-xl sm:text-2xl font-bold text-white">
                Works even without internet
              </h2>
            </div>
            
            <p className="text-slate-400 mb-8 max-w-xl">
              Thief turned off WiFi and mobile data? No problem. 
              Send these SMS commands to control the device:
            </p>

            <div className="grid sm:grid-cols-2 gap-4">
              {[
                { cmd: 'TRACKIT LOCATE', desc: 'Get current GPS location' },
                { cmd: 'TRACKIT ALARM', desc: 'Sound loud alarm' },
                { cmd: 'TRACKIT CAPTURE', desc: 'Take front & back photos' },
                { cmd: 'TRACKIT LOCK', desc: 'Lock the device' }
              ].map((item, i) => (
                <div key={i} className="flex items-center gap-3 p-3 bg-slate-800 rounded-lg">
                  <code className="text-emerald-400 text-sm font-mono">{item.cmd}</code>
                  <span className="text-slate-500 text-sm">— {item.desc}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Installation Guide */}
      <section id="install" className="py-20 px-6 bg-white dark:bg-slate-900">
        <div className="max-w-3xl mx-auto">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white text-center mb-4">
            How to install
          </h2>
          <p className="text-center text-slate-600 dark:text-slate-400 mb-10">
            Since this isn't on the Play Store, you'll need to allow installation from unknown sources.
          </p>

          <div className="space-y-6">
            <div className="flex gap-4">
              <div className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                1
              </div>
              <div>
                <h3 className="font-semibold text-slate-900 dark:text-white mb-1">
                  Temporarily disable Play Protect
                </h3>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Open Play Store → Profile icon → Play Protect → Settings gear → Turn off "Scan apps with Play Protect"
                </p>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                2
              </div>
              <div>
                <h3 className="font-semibold text-slate-900 dark:text-white mb-1">
                  Download and install the APK
                </h3>
                <p className="text-sm text-slate-600 dark:text-slate-400 mb-3">
                  Tap the download button, then open the downloaded file and tap Install.
                </p>
                <a 
                  href="/TrackIt.apk" 
                  download="TrackIt.apk"
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-emerald-600 rounded-lg hover:bg-emerald-700 transition-colors"
                >
                  <Download className="w-4 h-4" />
                  Download TrackIt.apk
                </a>
              </div>
            </div>

            <div className="flex gap-4">
              <div className="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                3
              </div>
              <div>
                <h3 className="font-semibold text-slate-900 dark:text-white mb-1">
                  Grant permissions & set up
                </h3>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Allow location, camera, and SMS permissions. Add your emergency contacts. 
                  Then re-enable Play Protect.
                </p>
              </div>
            </div>
          </div>

          <div className="mt-8 p-4 bg-slate-100 dark:bg-slate-800 rounded-lg">
            <p className="text-sm text-slate-600 dark:text-slate-400">
              <span className="font-medium text-slate-900 dark:text-white">Open source & safe.</span>{' '}
              This app is fully open source. You can review the code on{' '}
              <a 
                href="https://github.com/omkarr10/Familtytracker" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-emerald-600 hover:underline"
              >
                GitHub
              </a>{' '}
              or even self-host everything.
            </p>
          </div>
        </div>
      </section>

      {/* Anti-Theft Detail */}
      <section className="py-20 px-6">
        <div className="max-w-5xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 text-sm font-medium rounded-full mb-4">
                <Shield className="w-4 h-4" />
                Anti-Theft
              </div>
              <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white mb-4">
                What happens when a phone is stolen
              </h2>
              <p className="text-slate-600 dark:text-slate-400 mb-6">
                The moment someone enters the wrong PIN, removes the SIM, 
                or tries to turn off the phone, TrackIt kicks into action.
              </p>

              <ul className="space-y-3">
                {[
                  'Front camera silently takes a photo',
                  'SMS sent to your emergency contacts',
                  'Last known location shared',
                  'You can remotely lock or alarm the device',
                  'Everything works even without internet'
                ].map((item, i) => (
                  <li key={i} className="flex items-start gap-3">
                    <Check className="w-5 h-5 text-emerald-500 mt-0.5 flex-shrink-0" />
                    <span className="text-slate-700 dark:text-slate-300">{item}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Mock notification */}
            <div className="bg-slate-100 dark:bg-slate-800 rounded-2xl p-6">
              <div className="bg-white dark:bg-slate-900 rounded-xl shadow-lg overflow-hidden">
                <div className="p-4 border-b border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-red-500 rounded-full flex items-center justify-center">
                      <AlertTriangle className="w-5 h-5 text-white" />
                    </div>
                    <div>
                      <p className="font-semibold text-slate-900 dark:text-white text-sm">TrackIt Alert</p>
                      <p className="text-xs text-slate-500">Just now</p>
                    </div>
                  </div>
                </div>
                <div className="p-4">
                  <p className="text-sm text-slate-700 dark:text-slate-300 mb-3">
                    <span className="font-semibold">Wrong PIN entered</span> on Mom's Phone. 
                    Intruder photo captured.
                  </p>
                  <div className="aspect-video bg-slate-200 dark:bg-slate-700 rounded-lg flex items-center justify-center">
                    <Camera className="w-8 h-8 text-slate-400" />
                  </div>
                  <p className="text-xs text-slate-500 mt-3">
                    Last location: 40.7128° N, 74.0060° W
                  </p>
                </div>
                <div className="p-3 bg-slate-50 dark:bg-slate-800 flex gap-2">
                  <button className="flex-1 py-2 px-3 text-sm font-medium text-white bg-red-500 rounded-lg">
                    Sound Alarm
                  </button>
                  <button className="flex-1 py-2 px-3 text-sm font-medium text-slate-700 dark:text-slate-200 bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 rounded-lg">
                    Lock Device
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-6 bg-emerald-600 dark:bg-emerald-700">
        <div className="max-w-2xl mx-auto text-center">
          <h2 className="text-2xl sm:text-3xl font-bold text-white mb-4">
            Ready to try it?
          </h2>
          <p className="text-emerald-100 mb-8">
            Download the Android app and set up the web dashboard in under 5 minutes. 
            It's free and always will be.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <a 
              href="/TrackIt.apk" 
              download="TrackIt.apk"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 text-base font-medium text-emerald-700 bg-white rounded-lg hover:bg-emerald-50 transition-colors"
            >
              <Download className="w-5 h-5" />
              Download APK
            </a>
            <Link 
              to="/login" 
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 text-base font-medium text-white border-2 border-white/30 rounded-lg hover:bg-white/10 transition-colors"
            >
              Open Dashboard
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-8 px-6 bg-slate-900">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-emerald-600 flex items-center justify-center">
              <MapPin className="w-4 h-4 text-white" />
            </div>
            <span className="font-semibold text-white">TrackIt</span>
          </div>
          <p className="text-sm text-slate-400">
            Open source family safety app. Free forever.
          </p>
          <a 
            href="https://github.com/omkarr10/Familtytracker" 
            target="_blank" 
            rel="noopener noreferrer"
            className="text-slate-400 hover:text-white transition-colors"
          >
            <Github className="w-5 h-5" />
          </a>
        </div>
      </footer>
    </div>
  )
}
