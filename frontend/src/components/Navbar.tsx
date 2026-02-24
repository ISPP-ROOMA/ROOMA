import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { logout } from '../service/auth.service'

export default function Navbar() {
  const navigate = useNavigate()
  const { token, role } = useAuthStore()

  const handleLogout = () => {
    logout()
      .then(() => navigate('/login'))
      .catch((err) => console.error('Logout failed', err))
  }

  return (
    <div className="navbar bg-base-100 px-4 md:px-8 shadow-sm">
      <div className="navbar-start gap-2">
        <NavLink to="/" className="btn btn-ghost normal-case text-xl">
          Rooma
        </NavLink>

        {role === 'ADMIN' && (
          <NavLink className="btn btn-ghost" to="/users">
            Usuarios
          </NavLink>
        )}

        {role === 'LANDLORD' && (
          <NavLink className="btn btn-ghost" to="/apartments/my">
            Mis Inmuebles
          </NavLink>
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
            <>
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
