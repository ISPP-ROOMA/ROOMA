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

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  let publicRoutes = <></>
  let privateRoutes = <></>
  let customerRoutes = <></>
  let adminRoutes = <></>

  switch (role) {
    case 'ADMIN':
      adminRoutes = (
        <>
          <NavLink className="p-2" to="/users">
            Users
          </NavLink>
        </>
      )
      break
    case 'CUSTOMER':
      customerRoutes = (
        <>
          <NavLink className="p-2" to="/reservations">
            Reservations
          </NavLink>
        </>
      )
      break
    default:
      break
  }

  if (!token) {
    publicRoutes = (
      <>
        <NavLink className="p-2" to="/login">
          Login
        </NavLink>
        <NavLink className="p-2" to="/register">
          Register
        </NavLink>
      </>
    )
  } else {
    privateRoutes = (
      <>
        <NavLink className="p-2" to="/mis-solicitudes">
          Mis solicitudes
        </NavLink>
        <NavLink className="p-2" to="/profile">
          Profile
        </NavLink>
        <button onClick={handleLogout} className="btn btn-ghost btn-sm text-error">
          Logout
        </button>
      </>
    )
  }

  return (
    <div className="navbar bg-base-100 shadow sticky top-0 z-50">
      <div className="navbar-start">
        <NavLink to="/" className="btn btn-ghost normal-case text-xl">
          Enterprise
        </NavLink>
      </div>
      <div className="navbar-center hidden lg:flex">
        <div className="menu menu-horizontal px-1 items-center gap-2">
          <NavLink className="btn btn-ghost" to="/">
            Feed
          </NavLink>

          {token && (
            <button
              onClick={() => setShowReviewsAlert(!show_reviews_alert)}
              className={`btn btn-xs ${show_reviews_alert ? 'btn-error' : 'btn-success'} text-white`}
            >
              {show_reviews_alert ? 'Disable review alerts' : 'Enable review alerts'}
            </button>
          )}

          {adminRoutes}
          {customerRoutes}
        </div>
      </div>
      <div className="navbar-end">
        <div className="flex gap-2 items-center">
          {privateRoutes}
          {publicRoutes}
        </div>
      </div>
    </div>
  )
}
