import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { logout } from '../service/auth.service'

interface NavbarProps {
  show_reviews_alert: boolean
  setShowReviewsAlert: (value: boolean) => void
}

export default function Navbar({ show_reviews_alert, setShowReviewsAlert }: NavbarProps) {
  const navigate = useNavigate()
  const { token, role } = useAuthStore()

  const handleLogout = () => {
    logout()
      .then(() => navigate('/login'))
      .catch((err) => console.error('Logout failed', err))
  }

  let adminRoutes = <></>
  let tenantRoutes = <></>

  switch (role) {
    case 'ADMIN':
      adminRoutes = (
        <NavLink className="p-2" to="/users">
          Users
        </NavLink>
      )
      break
    case 'TENANT':
      tenantRoutes = (
        <NavLink className="p-2" to="/reservations">
          Reservations
        </NavLink>
      )
      break
    default:
      break
  }

  return (
    <div className="navbar bg-base-100 px-4 md:px-8 shadow-sm sticky top-0 z-50">
      <div className="navbar-start gap-2">
        <NavLink to="/" className="btn btn-ghost normal-case text-xl">
          Rooma
        </NavLink>

        {role === 'ADMIN' && (
          <NavLink className="btn btn-ghost hidden lg:flex" to="/users">
            Usuarios
          </NavLink>
        )}

        {role === 'LANDLORD' && (
          <NavLink className="btn btn-ghost" to="/apartments/my">
            Mis Inmuebles
          </NavLink>
        )}
      </div>

      <div className="navbar-center hidden lg:flex">
        <div className="menu menu-horizontal px-1 items-center gap-2">
          <NavLink className="btn btn-ghost" to="/">
            {role === 'TENANT' ? 'Feed' : 'Home'}
          </NavLink>

          {token && role === 'TENANT' && (
            <button
              onClick={() => setShowReviewsAlert(!show_reviews_alert)}
              className={`btn btn-xs ${show_reviews_alert ? 'btn-error' : 'btn-success'} text-white`}
            >
              {show_reviews_alert ? 'Disable review alerts' : 'Enable review alerts'}
            </button>
          )}

          {adminRoutes}
          {tenantRoutes}
        </div>
      </div>

      <div className="navbar-end">
        <div className="flex gap-2 items-center">
          {!token ? (
            <>
              <NavLink className="btn btn-ghost" to="/login">
                Login
              </NavLink>
              <NavLink className="btn btn-primary btn-sm" to="/register">
                Register
              </NavLink>
            </>
          ) : (
            <>
              {role === 'TENANT' && (
                <NavLink className="p-2 hidden md:block" to="/mis-solicitudes">
                  Mis solicitudes
                </NavLink>
              )}
              <NavLink className="btn btn-ghost" to="/profile">
                Perfil
              </NavLink>
              {role && (
                <span className="badge badge-outline badge-sm self-center">
                  {role === 'LANDLORD' ? 'Propietario' : role === 'TENANT' ? 'Inquilino' : role}
                </span>
              )}
              <button className="btn btn-error btn-sm" onClick={handleLogout}>
                Logout
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}