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
import { useEffect, useState } from 'react'
import { refreshToken } from './service/auth.service'
import Register from './pages/Register'
import MyRequests from './pages/private/MyRequests'
import PropertyDetails from './pages/PropertyDetails'
import { ToastProvider } from './context/ToastContext'
import ReviewModal from './components/ReviewModal'
import { getPendingReviews } from './service/review.service'
import LeaveReview from './pages/private/LeaveReview'

function App() {
  const location = useLocation()

  const { token, role } = useAuthStore()

  const [pendingContract, setPendingContract] = useState<{
    contractId: number
    apartmentAddress: string
    endDate: string
  } | null>(null)

  useEffect(() => {
    refreshToken()
  }, [])

  useEffect(() => {
    if (token) {
      getPendingReviews().then((data) => {
        if (data && data.length > 0) {
          setPendingContract(data[0])
        }
      })
    }
  }, [token])

  let publicRoutes = <></>
  let privateRoutes = <></>
  const customerRoutes = <></>
  let adminRoutes = <></>

  switch (role) {
    case 'ADMIN':
      adminRoutes = (
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
      )
      break
    default:
      break
  }

  if (!token) {
    publicRoutes = (
      <>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </>
    )
  } else {
    privateRoutes = (
      <>
        <Route
          path="/profile"
          element={
            <PrivateRoute>
              <Profile />
            </PrivateRoute>
          }
        />
        <Route
          path="/mis-solicitudes"
          element={
            <PrivateRoute>
              <MyRequests />
            </PrivateRoute>
          }
        />
        <Route
          path="/reviews/new/:contractId"
          element={
            <PrivateRoute>
              <LeaveReview />
            </PrivateRoute>
          }
        />
      </>
    )
  }

  const usesMobileLayout = location.pathname === '/mis-solicitudes'

  return (
    <ToastProvider>
      <div>
        {!usesMobileLayout && <Navbar />}
        <main className="mx-auto min-h-dvh flex flex-col">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/properties/:id" element={<PropertyDetails />} />
            {adminRoutes}
            {customerRoutes}
            {privateRoutes}
            {publicRoutes}
          </Routes>
        </main>
        {!usesMobileLayout && <Footer />}
        <ReviewModal contract={pendingContract} onClose={() => setPendingContract(null)} />
      </div>
    </ToastProvider>
  )
}

export default App
