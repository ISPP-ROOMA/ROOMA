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
  Compass,
} from 'lucide-react'
import type { ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

type NavItem = {
  to: string
  label: string
  icon: ReactNode
  end?: boolean
}

export default function Navbar() {
  const navigate = useNavigate()
  const location = useLocation()
  const { token, role } = useAuthStore()
  const [moreOpen, setMoreOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const moreRef = useRef<HTMLDivElement>(null)
  const profileRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const id = requestAnimationFrame(() => {
      setMoreOpen(false)
      setProfileOpen(false)
    })
    return () => cancelAnimationFrame(id)
  }, [location.pathname])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (moreOpen && moreRef.current && !moreRef.current.contains(e.target as Node)) {
        setMoreOpen(false)
      }
      if (profileOpen && profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [moreOpen, profileOpen])

  const handleLogout = () => {
    logout()
      .then(() => navigate('/login'))
      .catch((err) => console.error('Logout failed', err))
  }

  const { mainItems, profileItems } = (() => {
    if (!token) {
      return {
        mainItems: [
          { to: '/login', label: 'Entrar', icon: <UserCircle size={22} /> },
          { to: '/register', label: 'Registrarse', icon: <Home size={22} /> },
        ] as NavItem[],
        profileItems: [] as NavItem[],
      }
    }

    if (role === 'TENANT') {
      return {
        mainItems: [
          { to: '/', label: 'Explorar', icon: <Compass size={22} />, end: true },
          { to: '/my-home', label: 'Mi Casa', icon: <Home size={22} /> },
          { to: '/mis-solicitudes', label: 'Solicitudes', icon: <LayoutList size={22} /> },
        ] as NavItem[],
        profileItems: [
          { to: '/profile', label: 'Mi Perfil', icon: <UserCircle size={20} /> },
          { to: '/favorites', label: 'Favoritos', icon: <Bookmark size={20} /> },
          { to: '/my-reviews', label: 'Valoraciones', icon: <Star size={20} /> },
        ] as NavItem[],
      }
    }

    if (role === 'LANDLORD') {
      return {
        mainItems: [
          { to: '/', label: 'Explorar', icon: <Compass size={22} />, end: true },
          { to: '/apartments/my', label: 'Inmuebles', icon: <Building2 size={22} /> },
          { to: '/mis-solicitudes', label: 'Solicitudes', icon: <LayoutList size={22} /> },
          { to: '/apartments/publish', label: 'Publicar', icon: <LayoutList size={22} /> },
        ] as NavItem[],
        profileItems: [
          { to: '/profile', label: 'Mi Perfil', icon: <UserCircle size={20} /> },
          { to: '/my-reviews', label: 'Valoraciones', icon: <Star size={20} /> },
        ] as NavItem[],
      }
    }

    if (role === 'ADMIN') {
      return {
        mainItems: [
          { to: '/', label: 'Explorar', icon: <Compass size={22} />, end: true },
          { to: '/users', label: 'Usuarios', icon: <Users size={22} /> },
        ] as NavItem[],
        profileItems: [
          { to: '/profile', label: 'Mi Perfil', icon: <UserCircle size={20} /> },
        ] as NavItem[],
      }
    }

    return {
      mainItems: [{ to: '/', label: 'Explorar', icon: <Compass size={22} />, end: true }] as NavItem[],
      profileItems: [] as NavItem[],
    }
  })()

  const checkActive = (to: string, exact: boolean = false) => {
    return exact ? location.pathname === to : location.pathname.startsWith(to)
  }

  const MOBILE_MAX_ITEMS = 4
  const primaryItems = mainItems.slice(0, MOBILE_MAX_ITEMS)
  const secondaryMainItems = mainItems.slice(MOBILE_MAX_ITEMS)

  const mobileOverflowMenu = [...secondaryMainItems, ...profileItems]
  const isMobileMoreActive = mobileOverflowMenu.some((item) => checkActive(item.to, item.end))
  const hasMobileMore = mobileOverflowMenu.length > 0 || !!token

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

      {/* Center Links */}
      <nav className="absolute left-1/2 -translate-x-1/2 flex flex-1 justify-center items-center gap-1">
        {mainItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `flex items-center gap-1.5 px-6 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${isActive ? 'bg-primary/10 text-primary' : 'text-base-content/60 hover:bg-base-200 hover:text-base-content'}`
            }
          >
            {item.icon}
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* Right Actions (Profile Dropdown) */}
      <div className="flex items-center gap-2 relative z-20">
        {token ? (
          <div ref={profileRef} className="relative">
            <button
              onClick={() => setProfileOpen((prev) => !prev)}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${
                profileOpen
                  ? 'bg-base-200 text-base-content'
                  : 'text-base-content/60 hover:bg-base-200 hover:text-base-content'
              }`}
            >
              <UserCircle size={24} />
              Perfil
            </button>

            {/* Desktop Profile Dropdown */}
            {profileOpen && (
              <div className="absolute right-0 top-full mt-2 w-48 bg-base-100 rounded-2xl shadow-xl border border-base-200 py-1 flex flex-col overflow-hidden">
                {profileItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `flex items-center gap-3 px-4 py-2.5 text-sm font-medium transition-colors ${isActive ? 'bg-primary/5 text-primary' : 'text-base-content/80 hover:bg-base-200 hover:text-base-content'}`
                    }
                  >
                    {item.icon}
                    {item.label}
                  </NavLink>
                ))}
                <div className="border-t border-base-200 my-1" />
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-3 w-full text-left px-4 py-2.5 text-sm font-medium text-error/80 hover:bg-error/5 hover:text-error transition-colors"
                >
                  <LogOut size={20} />
                  Salir
                </button>
              </div>
            )}
          </div>
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

      {/* "mas" / Profile overflow button */}
      {hasMobileMore && (
        <div ref={moreRef} className="relative">
          {moreOpen && (
            <div className="absolute bottom-full mb-3 right-0 min-w-[160px] bg-base-100 border border-base-200 rounded-2xl shadow-xl py-1 flex flex-col">
              {mobileOverflowMenu.map((item) => (
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
            className={`flex flex-col items-center gap-0.5 px-3 py-1 rounded-2xl transition-all duration-200 ${moreOpen || isMobileMoreActive ? 'text-primary' : 'text-base-content/50 hover:text-base-content'}`}
          >
            {token ? <UserCircle size={22} /> : <MoreHorizontal size={22} />}
            <span className="text-[10px] font-semibold">{token ? 'Perfil' : 'Más'}</span>
            {isMobileMoreActive && !moreOpen && (
              <span className="w-1 h-1 rounded-full bg-primary block" />
            )}
          </button>
        </div>
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
