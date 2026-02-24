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
import { useEffect } from 'react'
import { refreshToken } from './service/auth.service'
import Register from './pages/Register'
import Apartments from './pages/apartments/Apartments'
import ApartmentDetail from './pages/apartments/ApartmentDetail'
import PublishFlowContainer from './pages/apartments/publish/PublishFlowContainer'
import BrowseApartments from './pages/apartments/BrowseApartments'

function App() {

  const { token, role } = useAuthStore()

  useEffect(() => {
    refreshToken()
  }, [])

  let publicRoutes = <></>
  let privateRoutes = <></>
  let landlordRoutes = <></>
  let tenantRoutes = <></>
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
    case "LANDLORD":
      landlordRoutes = (
        <>
          <Route path='/apartments' element={<PrivateRoute><Apartments /></PrivateRoute>} />
          <Route path='/apartments/publish' element={<PrivateRoute><PublishFlowContainer /></PrivateRoute>} />
          <Route path='/apartments/:id' element={<PrivateRoute><ApartmentDetail /></PrivateRoute>} />
        </>
      )
      break
    case "TENANT":
      tenantRoutes = (
        <>
          <Route path='/explore' element={<PrivateRoute><BrowseApartments /></PrivateRoute>} />
          <Route path='/apartments/:id' element={<PrivateRoute><ApartmentDetail /></PrivateRoute>} />
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
      </>
    )
  }

  return (
    <div>
      <Navbar />
      <main className='mx-auto min-h-dvh flex flex-col'>
        <Routes>
          <Route path='/' element={<Home />} />
          {adminRoutes}
          {landlordRoutes}
          {tenantRoutes}
          {privateRoutes}
          {publicRoutes}
        </Routes>
      </main>
      <Footer />
    </div>
  )
}

export default App
