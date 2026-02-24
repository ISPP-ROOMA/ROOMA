import { NavLink, useNavigate } from "react-router-dom"
import { useAuthStore } from "../store/authStore"
import { logout } from "../service/auth.service"

export default function Navbar() {
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
    case "ADMIN":
      adminRoutes = (
        <>
          <NavLink className="p-2" to="/users">Users</NavLink>
        </>
      )
      break
    case "CUSTOMER":
      customerRoutes = (
        <>
          <NavLink className="p-2" to="/reservations">Reservations</NavLink>
        </>
      )
      break
    default:
      break
  }

  if (!token) {
    publicRoutes = (
      <>
        <NavLink className="p-2" to="/login">Login</NavLink>
        <NavLink className="p-2" to="/register">Register</NavLink>
      </>
    )
  } else {
    privateRoutes = (
      <>
        <NavLink className="p-2" to="/mis-solicitudes">Mis solicitudes</NavLink>
        <NavLink className="p-2" to="/profile">Profile</NavLink>
        <button onClick={handleLogout} className="p-2 text-white">
          Logout
        </button>
      </>
    )
  }

  return (
    <div className="navbar bg-base-100 shadow sticky top-0 z-50">
      <div className="navbar-start">
        <NavLink to="/" className="btn btn-ghost normal-case text-xl">Enterprise</NavLink>
      </div>
      <div className="navbar-center hidden lg:flex">
        <div className="menu menu-horizontal px-1">
          <NavLink className="btn btn-ghost" to="/">Home</NavLink>
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
