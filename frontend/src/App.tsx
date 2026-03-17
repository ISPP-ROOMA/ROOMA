import { useEffect, useRef, useState } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar'
import PrivateRoute from './components/PrivateRoute'
import ReviewModal from './components/ReviewModal'
import Grainient from './components/ui/Grainient'
import { ToastProvider } from './context/ToastContext'
import User from './pages/admin/User'
import Users from './pages/admin/Users'
import ApartmentDetail from './pages/apartments/ApartmentDetail'
import Apartments from './pages/apartments/Apartments'
import ApartmentBills from './pages/apartments/billing/ApartmentBills'
import LandlordBillDetail from './pages/apartments/billing/LandlordBillDetail'
import NewBill from './pages/apartments/billing/NewBill'
import PublishFlowContainer from './pages/apartments/publish/PublishFlowContainer'
import Home from './pages/Home'
import Login from './pages/Login'
import LeaveReview from './pages/private/LeaveReview'
import MyHome from './pages/private/MyHome'
import FavoritesPage from './pages/private/FavoritesPage'
import DebtDetail from './pages/private/payments/DebtDetail'
import Invoices from './pages/private/payments/Invoices'
import ApartmentIncidences from './pages/private/incidences/ApartmentIncidences'
import IncidentDetail from './pages/private/incidences/IncidentDetail'
import PaymentSuccess from './pages/private/payments/PaymentSuccess'
import MyReviews from './pages/private/MyReviews'
import Profile from './pages/private/Profile'
import ProfileEdit from './pages/private/ProfileEdit'
import ReviewContractFinished from './pages/private/ReviewContractFinished'
import SelectReviewTarget from './pages/private/SelectReviewTarget'
import LandlordMatchDetailPage from './pages/private/requests/LandlordMatchDetailPage'
import LandlordRequestDetailPage from './pages/private/requests/LandlordRequestDetailPage'
import LandlordRequestsPage from './pages/private/requests/LandlordRequestsPage'
import TenantRequestsPage from './pages/private/requests/TenantRequestsPage'
import Register from './pages/Register'
import { hasSessionHint, refreshToken } from './service/auth.service'
import { getPendingReviewApartments } from './service/review.service'
import { useAuthStore } from './store/authStore'

function App() {
  const { token, role } = useAuthStore()
  const location = useLocation()

  const showBackground =
    location.pathname === '/login' ||
    location.pathname === '/register' ||
    (location.pathname === '/' && !token)
  const didTryRefresh = useRef(false)

  const [pendingContract, setPendingContract] = useState<{
    contractId: number
    apartmentAddress: string
    endDate: string
  } | null>(null)

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

  useEffect(() => {
    if (token) {
      void getPendingReviewApartments()
        .then((data) => {
          if (data && data.length > 0) {
            const apt = data.find((a) => a.pendingUsers.some((u) => !u.youReviewedThem))
            if (apt) {
              setPendingContract({
                contractId: apt.apartmentId,
                apartmentAddress: apt.apartmentUbication ?? apt.apartmentTitle,
                endDate: '',
              })
            }
          }
        })
        .catch(console.error)
    }
  }, [token])

  let publicRoutes = <></>
  let privateRoutes = <></>
  const adminRoutes = (
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
          path="/profile/edit"
          element={
            <PrivateRoute>
              <ProfileEdit />
            </PrivateRoute>
          }
        />
        <Route
          path="/mis-solicitudes"
          element={
            <PrivateRoute>
              <Navigate
                to={
                  role === 'LANDLORD'
                    ? '/mis-solicitudes/recibidas'
                    : role === 'TENANT'
                      ? '/mis-solicitudes/enviadas'
                      : '/profile'
                }
                replace
              />
            </PrivateRoute>
          }
        />
        <Route
          path="/mis-solicitudes/enviadas"
          element={
            <PrivateRoute allowedRoles={['TENANT']}>
              <TenantRequestsPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/mis-solicitudes/recibidas"
          element={
            <PrivateRoute allowedRoles={['LANDLORD']}>
              <LandlordRequestsPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/mis-solicitudes/recibidas/:apartmentMatchId"
          element={
            <PrivateRoute allowedRoles={['LANDLORD']}>
              <LandlordRequestDetailPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/mis-solicitudes/recibidas/:apartmentMatchId/match"
          element={
            <PrivateRoute allowedRoles={['LANDLORD']}>
              <LandlordMatchDetailPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/reviews/new/:contractId"
          element={
            <PrivateRoute>
              <ReviewContractFinished />
            </PrivateRoute>
          }
        />
        <Route
          path="/reviews/new/:contractId/select"
          element={
            <PrivateRoute>
              <SelectReviewTarget />
            </PrivateRoute>
          }
        />
        <Route
          path="/reviews/new/:contractId/form/:userId"
          element={
            <PrivateRoute>
              <LeaveReview />
            </PrivateRoute>
          }
        />
        <Route
          path="/my-reviews"
          element={
            <PrivateRoute>
              <MyReviews />
            </PrivateRoute>
          }
        />
      </>
    )
  }

  return (
    <ToastProvider>
      <div className="flex flex-col min-h-screen relative overflow-hidden text-base-content font-sans">
        {/* Animated Background */}
        {showBackground && (
          <div className="fixed inset-0 z-[-1] pointer-events-none">
            <Grainient
              color1="#f0ebe3" // base-200
              color2="#0d9488" // primary (teal)
              color3="#c4a97d" // secondary (beige)
              timeSpeed={0.25}
              colorBalance={-0.17}
              warpStrength={0.75}
              warpFrequency={5}
              warpSpeed={2}
              warpAmplitude={65}
              blendAngle={0}
              blendSoftness={0.05}
              rotationAmount={500}
              noiseScale={2.3}
              grainAmount={0.02}
              grainScale={2}
              grainAnimated={true}
              contrast={1.1}
              gamma={1}
              saturation={1.2}
              zoom={0.9}
            />
          </div>
        )}

        {/* Only show Navbar if not on Home screen unauthenticated */}
        {(token || location.pathname !== '/') && <Navbar />}

        <main className="mx-auto flex-grow w-full pb-20 md:pb-0 relative z-0">
          <Routes>
            <Route path="/" element={<Home />} />

            {!token && (
              <>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
              </>
            )}

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
                    <PrivateRoute allowedRoles={['ADMIN']}>
                      <Users />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/users/:id"
                  element={
                    <PrivateRoute allowedRoles={['ADMIN']}>
                      <User />
                    </PrivateRoute>
                  }
                />
                {adminRoutes}
              </>
            )}

            {/* Rutas de Landlord */}
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
                <Route
                  path="/apartments/:id"
                  element={
                    <PrivateRoute allowedRoles={['LANDLORD']}>
                      <ApartmentDetail />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/new-bill"
                  element={
                    <PrivateRoute allowedRoles={['LANDLORD']}>
                      <NewBill />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/bills"
                  element={
                    <PrivateRoute allowedRoles={['LANDLORD']}>
                      <ApartmentBills />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/bills/:billId"
                  element={
                    <PrivateRoute allowedRoles={['LANDLORD']}>
                      <LandlordBillDetail />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/incidences"
                  element={
                    <PrivateRoute allowedRoles={['LANDLORD']}>
                      <ApartmentIncidences />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/incidences/:incidentId"
                  element={
                    <PrivateRoute allowedRoles={['LANDLORD']}>
                      <IncidentDetail />
                    </PrivateRoute>
                  }
                />
              </>
            )}

            {/* Rutas de Tenant (Inquilino) */}
            {role === 'TENANT' && (
              <>
                <Route
                  path="/my-home"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <MyHome />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/favorites"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <FavoritesPage />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/invoices"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <Invoices />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/invoices/:debtId"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <DebtDetail />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/invoices/:debtId/success"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <PaymentSuccess />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/incidences"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <ApartmentIncidences />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/apartments/:id/incidences/:incidentId"
                  element={
                    <PrivateRoute allowedRoles={['TENANT']}>
                      <IncidentDetail />
                    </PrivateRoute>
                  }
                />
              </>
            )}

            {privateRoutes}
            {publicRoutes}
          </Routes>
        </main>

        {/* Modales globales */}
        <ReviewModal
          contract={pendingContract}
          onClose={() => {
            setPendingContract(null)
          }}
        />
      </div>
    </ToastProvider>
  )
}

export default App
