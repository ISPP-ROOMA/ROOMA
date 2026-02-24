import { Route, Routes, useLocation } from 'react-router-dom'
import Home from './pages/Home'
import Navbar from './components/Navbar'
import Login from './pages/Login'
import Profile from './pages/private/Profile'
import Footer from './components/Footer'
import PrivateRoute from './components/PrivateRoute'
import { useAuthStore } from './store/authStore'
import Users from './pages/admin/Users'
import User from './pages/admin/User'
import { useEffect } from 'react'
import { refreshToken } from './service/auth.service'
import Register from './pages/Register'
import MyRequests from './pages/private/MyRequests'

function App() {
  const location = useLocation()

  const { token, role } = useAuthStore()

  useEffect(() => {
    refreshToken()
  }, [])

  let publicRoutes = <></>
  let privateRoutes = <></>
  let customerRoutes = <></>
  let adminRoutes = <></>

  switch (role) {
    case "ADMIN":
      adminRoutes = (
        <>
          <Route path='/users' element={<PrivateRoute><Users /></PrivateRoute>} />
          <Route path='/users/:id' element={<PrivateRoute><User /></PrivateRoute>} />
        </>
      )
      break
    default:
      break
  }

  if (!token) {
    publicRoutes = (
      <>
        <Route path='/login' element={<Login />} />
        <Route path='/register' element={<Register />} />
      </>
    )
  } else {
    privateRoutes = (
      <>
        <Route path='/profile' element={<PrivateRoute><Profile /></PrivateRoute>} />
        <Route path='/mis-solicitudes' element={<PrivateRoute><MyRequests /></PrivateRoute>} />
      </>
    )
  }

  const usesMobileLayout = location.pathname === '/mis-solicitudes'

  return (
    <div>
      {!usesMobileLayout && <Navbar />}
      <main className='mx-auto min-h-dvh flex flex-col'>
        <Routes>
          <Route path='/' element={<Home />} />
          {adminRoutes}
          {customerRoutes}
          {privateRoutes}
          {publicRoutes}
        </Routes>
      </main>
      {!usesMobileLayout && <Footer />}
    </div>
  )
}

export default App
