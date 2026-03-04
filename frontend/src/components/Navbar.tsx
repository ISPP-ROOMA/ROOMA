import { Building2, Home, LayoutList, LogOut, Star, UserCircle, Users } from 'lucide-react'
import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

export default function Navbar() {
  const navigate = useNavigate()
  const { token, role } = useAuthStore()

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
        { to: '/mis-solicitudes', label: 'Solicitudes', icon: <LayoutList size={22} /> },
        { to: '/', label: 'Inicio', icon: <Home size={22} />, end: true },
        { to: '/my-reviews', label: 'Valoraciones', icon: <Star size={22} /> },
        { to: '/profile', label: 'Perfil', icon: <UserCircle size={22} /> },
      ]
    }
    if (role === 'LANDLORD') {
      return [
        { to: '/apartments/my', label: 'Inmuebles', icon: <Building2 size={22} /> },
        { to: '/', label: 'Inicio', icon: <Home size={22} />, end: true },
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

  const activeCls = 'text-primary'
  const inactiveCls = 'text-base-content/50 hover:text-base-content'

  const desktopNav = (
    <header className="hidden md:flex sticky top-0 z-50 w-full items-center justify-between px-8 py-3 bg-base-100/80 backdrop-blur-md border-b border-base-200 shadow-sm">
      {/* Brand (Left) */}
      <NavLink to="/" className="flex items-center gap-2 font-bold text-xl tracking-tight text-primary select-none z-10 w-32">
        <img src="/Logo Rooma.jpeg" alt="Rooma" className="h-8 w-8 rounded-xl object-cover" />
        Rooma
      </NavLink>

      {/* Center links (Absolute Center) */}
      <nav className="absolute left-1/2 -translate-x-1/2 flex items-center gap-1">
        {navItems.map((item) => (
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
          <button
            onClick={handleLogout}
            className="btn btn-ghost btn-sm gap-1.5 text-error hover:bg-error/10"
          >
            <LogOut size={16} />
            Salir
          </button>
        ) : null}
      </div>
    </header>
  )

  const mobileNav = (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 flex items-center justify-around px-2 py-2 bg-base-100/90 backdrop-blur-xl border-t border-base-200 shadow-[0_-4px_24px_rgba(0,0,0,0.08)]">
      {navItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            `flex flex-col items-center gap-0.5 px-3 py-1 rounded-2xl transition-all duration-200 ${isActive ? activeCls : inactiveCls}`
          }
        >
          {({ isActive }) => (
            <>
              <span className={`transition-transform duration-200 ${isActive ? 'scale-110' : ''}`}>
                {item.icon}
              </span>
              <span className={`text-[10px] font-semibold ${isActive ? 'text-primary' : 'text-base-content/40'}`}>
                {item.label}
              </span>
              {isActive && (
                <span className="w-1 h-1 rounded-full bg-primary block" />
              )}
            </>
          )}
        </NavLink>
      ))}

      {token && (
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
