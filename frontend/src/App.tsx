import { useEffect, useRef } from 'react'
import { Route, Routes } from 'react-router-dom'
import Home from './pages/Home'
import Navbar from './components/Navbar'
import Login from './pages/Login'
import Profile from './pages/private/Profile'
import Footer from './components/Footer'
import PrivateRoute from './components/PrivateRoute'
import { useAuthStore } from './store/authStore'
import Users from './pages/admin/Users'
import User from './pages/admin/User'
import { hasSessionHint, refreshToken } from './service/auth.service'
import Register from './pages/Register'
import Apartments from './pages/apartments/Apartments'
import ApartmentDetail from './pages/apartments/ApartmentDetail'
import PublishFlowContainer from './pages/apartments/publish/PublishFlowContainer'

function App() {
  const { token, role } = useAuthStore()
  const didTryRefresh = useRef(false)

  useEffect(() => {
    if (didTryRefresh.current) {
      return
    }

    if (token || !hasSessionHint()) {
      return
    }

    didTryRefresh.current = true
    refreshToken()
  }, [token])

  return (
    <div>
      <Navbar />
      <main className="mx-auto min-h-dvh flex flex-col">
        <Routes>
          <Route path="/" element={<Home />} />

          {!token && <Route path="/login" element={<Login />} />}
          {!token && <Route path="/register" element={<Register />} />}

          {token && (
            <Route
              path="/profile"
              element={
                <PrivateRoute>
                  <Profile />
                </PrivateRoute>
              }
            />
          )}

          {role === 'ADMIN' && (
            <>
              <Route
                path="/users"
                element={
                  <PrivateRoute>
                    <Users />
                  </PrivateRoute>
                }
              />
              <Route
                path="/users/:id"
                element={
                  <PrivateRoute>
                    <User />
                  </PrivateRoute>
                }
              />
            </>
          )}

          {role === 'LANDLORD' && (
            <>
              <Route
                path="/apartments/my"
                element={
                  <PrivateRoute allowedRoles={['LANDLORD']}>
                    <Apartments />
                  </PrivateRoute>
                }
              />
              <Route
                path="/apartments/publish"
                element={
                  <PrivateRoute allowedRoles={['LANDLORD']}>
                    <PublishFlowContainer />
                  </PrivateRoute>
                }
              />
            </>
          )}

          {role === 'LANDLORD' && (
            <Route
              path="/apartments/:id"
              element={
                <PrivateRoute allowedRoles={['LANDLORD']}>
                  <ApartmentDetail />
                </PrivateRoute>
              }
            />
          )}
        </Routes>
      </main>
      <Footer />
    </div>
  )
}

export default App
