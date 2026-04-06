import {
  Building2,
  Bookmark,
  Home,
  LayoutList,
  LogOut,
  MoreHorizontal,
  Star,
  UserCircle,
  Users,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

const MOBILE_MAX_ITEMS = 4

export default function Navbar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { token, role } = useAuthStore()
  const [moreOpen, setMoreOpen] = useState(false)
  const moreRef = useRef<HTMLDivElement>(null)

  // Close "mas" menu on route change or outside click
  useEffect(() => {
    const id = requestAnimationFrame(() => {
      setMoreOpen(false)
    })
    return () => {
      cancelAnimationFrame(id)
    }
  }, [location.pathname])

  useEffect(() => {
    if (!moreOpen) return
    const handler = (e: MouseEvent) => {
      if (moreRef.current && !moreRef.current.contains(e.target as Node)) {
        setMoreOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => {
      document.removeEventListener('mousedown', handler)
    }
  }, [moreOpen])

  const handleLogout = () => {
    logout()
      .then(() => navigate('/login'))
      .catch((err) => console.error('Logout failed', err))
  }

  const navItems = (() => {
    if (!token) {
      return [
        { to: '/login', label: 'Entrar', icon: <UserCircle size={22} /> },
        { to: '/register', label: 'Registrarse', icon: <Home size={22} /> },
      ]
    }
    if (role === 'TENANT') {
      return [
        { to: '/', label: 'Inicio', icon: <Home size={22} />, end: true },
        { to: '/mis-solicitudes', label: 'Solicitudes', icon: <LayoutList size={22} /> },
        { to: '/favorites', label: 'Favoritos', icon: <Bookmark size={22} /> },
        { to: '/my-reviews', label: 'Valoraciones', icon: <Star size={22} /> },
        { to: '/profile', label: 'Perfil', icon: <UserCircle size={22} /> },
      ]
    }
    if (role === 'LANDLORD') {
      return [
        { to: '/', label: 'Inicio', icon: <Home size={22} />, end: true },
        { to: '/apartments/my', label: 'Inmuebles', icon: <Building2 size={22} /> },
        { to: '/mis-solicitudes', label: 'Solicitudes', icon: <LayoutList size={22} /> },
        { to: '/my-reviews', label: 'Valoraciones', icon: <Star size={22} /> },
        { to: '/apartments/publish', label: 'Publicar', icon: <LayoutList size={22} /> },
        { to: '/profile', label: 'Perfil', icon: <UserCircle size={22} /> },
      ]
    }
    if (role === 'ADMIN') {
      return [
        { to: '/', label: 'Inicio', icon: <Home size={22} />, end: true },
        { to: '/users', label: 'Usuarios', icon: <Users size={22} /> },
        { to: '/profile', label: 'Perfil', icon: <UserCircle size={22} /> },
      ]
    }
    return [{ to: '/', label: 'Inicio', icon: <Home size={22} />, end: true }]
  })()

  // Split items for mobile: first N go in the bar, the rest go in "mas"
  const primaryItems = navItems.slice(0, MOBILE_MAX_ITEMS)
  const secondaryItems = navItems.slice(MOBILE_MAX_ITEMS)
  const hasMore = secondaryItems.length > 0

  // Whether any secondary item is currently active (to highlight the "mas" button)
  const isMoreActive = secondaryItems.some((item) =>
    item.end ? location.pathname === item.to : location.pathname.startsWith(item.to)
  )

  const desktopNav = (
    <header className="hidden md:flex sticky top-0 z-50 w-full items-center justify-between px-8 py-3 bg-base-100/80 backdrop-blur-md border-b border-base-200 shadow-sm">
      {/* Brand (Left) */}
      <NavLink
        to="/"
        className="flex items-center gap-2 font-bold text-xl tracking-tight text-primary select-none z-10 w-32"
      >
        <img src="/Logo Rooma.jpeg" alt="Rooma" className="h-8 w-8 rounded-xl object-cover" />
        Rooma
      </NavLink>

      {/* Center links (Absolute Center) */}
      <nav className="absolute left-1/2 -translate-x-1/2 flex items-center gap-1">
        {navItems.filter(item => item.to !== '/profile').map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `flex items-center gap-1.5 px-8 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${isActive ? 'bg-primary/10 text-primary' : 'text-base-content/60 hover:bg-base-200 hover:text-base-content'}`
            }
          >
            {item.icon}
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* Right actions */}
      <div className="flex items-center gap-2">
        {token ? (
          <>
            <NavLink
              to="/profile"
              className={({ isActive }) =>
                `flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${isActive ? 'bg-primary/10 text-primary' : 'text-base-content/60 hover:bg-base-200 hover:text-base-content'}`
              }
            >
              <UserCircle size={20} />
              Perfil
            </NavLink>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 text-error/80 hover:bg-error/10 hover:text-error"
            >
              <LogOut size={20} />
              Salir
            </button>
          </>
        ) : null}
      </div>
    </header>
  )

  const mobileNav = (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 flex items-center justify-around px-2 py-2 bg-base-100/90 backdrop-blur-xl border-t border-base-200 shadow-[0_-4px_24px_rgba(0,0,0,0.08)]">
      {primaryItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 px-3 py-1 rounded-2xl transition-all duration-200 ${isActive ? 'text-primary' : 'text-base-content/50 hover:text-base-content'}`
          }
        >
          {({ isActive }) => (
            <>
              <span className={`transition-transform duration-200 ${isActive ? 'scale-110' : ''}`}>
                {item.icon}
              </span>
              <span
                className={`text-[10px] font-semibold ${isActive ? 'text-primary' : 'text-base-content/40'}`}
              >
                {item.label}
              </span>
              {isActive && <span className="w-1 h-1 rounded-full bg-primary block" />}
            </>
          )}
        </NavLink>
      ))}

      {/* "mas" overflow button */}
      {hasMore && (
        <div ref={moreRef} className="relative">
          {/* Popup menu */}
          {moreOpen && (
            <div className="absolute bottom-full mb-3 right-0 min-w-[160px] bg-base-100 border border-base-200 rounded-2xl shadow-xl py-1 flex flex-col">
              {secondaryItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  onClick={() => setMoreOpen(false)}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-4 py-3 text-sm font-medium transition-colors ${isActive ? 'text-primary bg-primary/5' : 'text-base-content/70 hover:bg-base-200 hover:text-base-content'}`
                  }
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              ))}
              {token && (
                <>
                  <div className="border-t border-base-200 my-1" />
                  <button
                    onClick={() => {
                      setMoreOpen(false)
                      handleLogout()
                    }}
                    className="flex items-center gap-3 px-4 py-3 text-sm font-medium text-error/80 hover:bg-error/5 hover:text-error transition-colors w-full text-left"
                  >
                    <LogOut size={18} />
                    Salir
                  </button>
                </>
              )}
            </div>
          )}
          <button
            onClick={() => setMoreOpen((v) => !v)}
            className={`flex flex-col items-center gap-0.5 px-3 py-1 rounded-2xl transition-all duration-200 ${moreOpen || isMoreActive ? 'text-primary' : 'text-base-content/50 hover:text-base-content'}`}
          >
            <MoreHorizontal size={22} />
            <span className="text-[10px] font-semibold">mas</span>
            {isMoreActive && !moreOpen && (
              <span className="w-1 h-1 rounded-full bg-primary block" />
            )}
          </button>
        </div>
      )}

      {/* Logout in bar when no overflow menu */}
      {!hasMore && token && (
        <button
          onClick={handleLogout}
          className="flex flex-col items-center gap-0.5 px-3 py-1 rounded-2xl text-error/70 hover:text-error transition-colors duration-200"
        >
          <LogOut size={22} />
          <span className="text-[10px] font-semibold">Salir</span>
        </button>
      )}
    </nav>
  )

  return (
    <>
      {desktopNav}
      {mobileNav}
    </>
  )
}
