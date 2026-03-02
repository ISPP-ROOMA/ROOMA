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

  const isTenant = role === 'TENANT'
  const isLandlord = role === 'LANDLORD'
  const isAdmin = role === 'ADMIN'

  return (
    <div className="navbar bg-base-100 px-4 md:px-8 shadow-sm sticky top-0 z-50">
      <div className="navbar-start gap-2 items-center flex">
        <NavLink className="btn btn-ghost" to="/">
          {isTenant ? 'Feed' : 'Home'}
        </NavLink>

        {isTenant && (
          <>
            <NavLink className="p-2 hidden md:block" to="/mis-solicitudes">
              Mis solicitudes
            </NavLink>
            <NavLink className="btn btn-ghost" to="/my-home">
              Mi piso
            </NavLink>
          </>
        )}

        {isLandlord && (
          <NavLink className="btn btn-ghost hidden md:block" to="/mis-solicitudes/recibidas">
            Solicitudes recibidas
          </NavLink>
        )}

        {isAdmin && (
          <NavLink className="p-2" to="/users">
            Users
          </NavLink>
        )}

        {token && (
          <NavLink className="btn btn-ghost" to="/profile">
            Perfil
          </NavLink>
        )}

        {token && role && (
          <span className="badge badge-outline badge-sm self-center">
            {isLandlord ? 'Propietario' : isTenant ? 'Inquilino' : role}
          </span>
        )}

        {token && isTenant && (
          <button
            onClick={() => setShowReviewsAlert(!show_reviews_alert)}
            className={`btn btn-xs ${show_reviews_alert ? 'btn-error' : 'btn-success'} text-white`}
          >
            {show_reviews_alert ? 'Disable review alerts' : 'Enable review alerts'}
          </button>
        )}
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
            <button className="btn btn-error btn-sm" onClick={handleLogout}>
              Logout
            </button>
          )}
        </div>
      </div>
    </div>
  )
}