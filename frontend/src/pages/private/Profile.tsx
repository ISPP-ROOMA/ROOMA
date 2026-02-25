import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getUserProfile } from '../../service/users.service'
import type { User } from '../../service/users.service'

export default function Profile() {
  const [userData, setUserData] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const profile = async () => {
      try {
        const res = await getUserProfile()
        setUserData(res || null)
      } catch (error) {
        console.error(error)
      } finally {
        setIsLoading(false)
      }
    }
    profile()
  }, [])

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Loading...</p>

  if (!userData) return <p className="text-center mt-10 text-red-500">Error loading profile</p>

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] p-4">
      <div className="card w-full max-w-sm bg-base-100 shadow-lg">
        <div className="card-body items-center text-center">
          <div className="avatar">
            <div className="w-24 h-24 rounded-full bg-primary text-white flex items-center justify-center text-3xl font-bold">
              {userData.name ? userData.name[0].toUpperCase() : '?'}
            </div>
          </div>
          <h1 className="text-2xl font-semibold">{userData.name}</h1>
          <p className="text-muted mb-4">{userData.email}</p>

          <div className="flex flex-col gap-2 mt-4 w-full">
            <Link to="/edit-profile" className="btn btn-primary">
              Edit Profile
            </Link>
            <Link to="/" className="link">
              ← Volver al inicio
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
